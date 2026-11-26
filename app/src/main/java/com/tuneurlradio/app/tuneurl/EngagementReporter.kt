package com.tuneurlradio.app.tuneurl

import android.content.Context
import android.util.Log
import android.provider.Settings
import com.dekidea.tuneurl.api.TuneUrlApi
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tuneurlradio.app.data.local.entity.HistoryEngagementEntity
import com.tuneurlradio.app.data.local.entity.SavedEngagementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that sends Interest_action reports ("heard", "interested",
 * "uninterested", "acted", "shared") to the TuneURL interests API, through
 * [TuneUrlApi.addRecordOfInterest].
 *
 * Mirrors the iOS [ReportAction] enum. The wire format is shared between
 * platforms — same endpoint, same lowercase action strings. The SDK already
 * handles `"heard"`, `"interested"`, `"uninterested"` from elsewhere in the
 * app; this helper adds `"acted"` and `"shared"` for the detail-screen flow
 * without disturbing the existing call sites.
 *
 * Fire-and-forget by design: reporting must never block UI, and a failure to
 * report is logged but does not surface to the user.
 */
@Singleton
class EngagementReporter @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: TuneUrlApi
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "EngagementReporter"

    /** Acted = user invoked the engagement (Open Website / View Coupon / Call / Send Message). */
    fun acted(context: Context, engagementId: Int, heardDateIso: String?) {
        post(context, engagementId, ACTION_ACTED, heardDateIso)
    }

    /** Shared = user tapped Share (fires when the share sheet opens). */
    fun shared(context: Context, engagementId: Int, heardDateIso: String?) {
        post(context, engagementId, ACTION_SHARED, heardDateIso)
    }

    /** Convenience overloads for the two entity types. */
    fun acted(context: Context, entity: SavedEngagementEntity) =
        acted(context, entity.engagementId, TimeUtils.formatAsIso(entity.heardAt))

    fun acted(context: Context, entity: HistoryEngagementEntity) =
        acted(context, entity.engagementId, TimeUtils.formatAsIso(entity.heardAt))

    fun shared(context: Context, entity: SavedEngagementEntity) =
        shared(context, entity.engagementId, TimeUtils.formatAsIso(entity.heardAt))

    fun shared(context: Context, entity: HistoryEngagementEntity) =
        shared(context, entity.engagementId, TimeUtils.formatAsIso(entity.heardAt))

    private fun post(@Suppress("UNUSED_PARAMETER") context: Context, id: Int, action: String, date: String?) {
        report(id.toString(), action, date)
    }

    /**
     * Report [action] for the Turl [tuneUrlId]. Fire-and-forget: runs in the
     * background, never throws, and logs the result.
     */
    fun report(tuneUrlId: String, action: String, date: String?) {
        scope.launch {
            api.addRecordOfInterest(userId(), tuneUrlId, action, date.orEmpty())
                .onSuccess { Log.d(TAG, "Reported $action for engagement id=$tuneUrlId") }
                .onFailure { Log.w(TAG, "Failed to report $action for engagement id=$tuneUrlId: ${it.message}") }
        }
    }

    /** Same user id the Java SDK sent (Settings.Secure.ANDROID_ID). */
    private fun userId(): String =
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    companion object {
        const val ACTION_HEARD = "heard"
        const val ACTION_INTERESTED = "interested"
        const val ACTION_UNINTERESTED = "uninterested"
        const val ACTION_ACTED = "acted"
        const val ACTION_SHARED = "shared"
    }
}
