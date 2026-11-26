package com.tuneurlradio.app.data.ksal

import android.util.Log
import com.google.gson.JsonParser
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sponsor ads from the station's own ad server (Revive Adserver at
 * adserver.radiosalina.com), the same server and zones that ksal.com uses.
 * Impressions and clicks are counted by Revive. Android sends
 * `loc=https://www.ksal.com/app/android/<placement>` so the station can tell
 * the platforms apart in its reports. Port of iOS Flow/Ads/Ads.swift.
 */
object AdZone {
    const val TOP_BANNER = 17     // 728x90, top of every ksal.com page
    const val HOME_BOX = 20       // 300x250, home page
    const val MOBILE_BANNER = 5   // 288x100, home page
    const val SPORTS_BANNER = 16  // 1110x180, before Sports News on the home page
    const val STORY_BANNER = 6    // 728x90, after the story on story pages
    const val STORY_BOX = 24      // 300x250, after the story on story pages
}

enum class AdPlacement(val key: String) {
    HOME_TOP("home-top"),             // under the weather strip
    HOME_FEED("home-feed"),           // after the first five stories
    ARTICLE_BANNER("article-banner"), // after the story text
    ARTICLE_BOX("article-box"),       // under the story banner
    SPORTS("sports"),                 // under My teams
    LOCAL("local"),                   // under the weather card
    LISTEN("listen");                 // under the stations

    fun zone(tablet: Boolean): Int = when (this) {
        HOME_TOP -> AdZone.TOP_BANNER
        HOME_FEED -> AdZone.HOME_BOX
        ARTICLE_BANNER -> AdZone.STORY_BANNER
        ARTICLE_BOX -> AdZone.STORY_BOX
        SPORTS -> AdZone.SPORTS_BANNER
        LOCAL, LISTEN -> if (tablet) AdZone.TOP_BANNER else AdZone.MOBILE_BANNER
    }
}

data class AdCreative(
    val imageUrl: String,
    val clickUrl: String,
    val beaconUrl: String?,
    val altText: String,
    val width: Float,
    val height: Float
)

@Singleton
class AdService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    /** One banner for a zone, or null when the zone has nothing to show. */
    suspend fun fetch(zone: Int, placement: AdPlacement): AdCreative? = withContext(Dispatchers.IO) {
        val label = "Ad ${placement.key} (zone $zone)"
        val url = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("zones", zone.toString())
            .addQueryParameter("prefix", "revive-0-")
            .addQueryParameter("loc", "https://www.ksal.com/app/android/${placement.key}")
            .addQueryParameter("id", REVIVE_ID)
            .build()

        val body = try {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use {
                if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
                it.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "$label: request failed: ${e.message}")
            return@withContext null
        }

        val entry = runCatching {
            JsonParser.parseString(body).asJsonObject.entrySet().firstOrNull()?.value?.asJsonObject
        }.getOrNull()
        val html = entry?.str("html")
        if (entry == null || html.isNullOrEmpty()) {
            Log.i(TAG, "$label: no ad from the server")
            return@withContext null
        }

        val width = entry.get("width")?.asString?.toFloatOrNull() ?: 0f
        val height = entry.get("height")?.asString?.toFloatOrNull() ?: 0f
        val click = firstMatch("<a[^>]+href='([^']+)'", html)?.let(::clean)
        val image = firstMatch("<img[^>]+src='([^']+)'", html)?.let(::clean)
        if (width <= 0f || height <= 0f || click == null || image == null) {
            Log.e(TAG, "$label: could not read the ad (size ${width}x$height)")
            return@withContext null
        }
        val beacon = firstMatch("src='([^']*lg\\.php[^']*)'", html)?.let(::clean)
        val alt = firstMatch("alt='([^']*)'", html)?.let { Html.decodeEntities(it) }.orEmpty()

        Log.i(TAG, "$label: showing \"$alt\" ${width.toInt()}x${height.toInt()}")
        AdCreative(image, click, beacon, alt, width, height)
    }

    /** Counts the impression. Call once, when the ad is on screen. */
    fun sendBeacon(url: String) {
        okHttpClient.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Impression beacon failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun clean(raw: String) = raw.replace("\\/", "/").replace("&amp;", "&")

    private fun firstMatch(pattern: String, text: String): String? =
        Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)

    private companion object {
        const val TAG = "Ads"
        const val ENDPOINT = "https://adserver.radiosalina.com/mm-adsrv/www/delivery/asyncspc.php"
        const val REVIVE_ID = "617ad54ec7b76eee24d2cc5f6121ca87"
    }
}

/** Lets composables reach the singleton [AdService] without a ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdServiceEntryPoint {
    fun adService(): AdService
}
