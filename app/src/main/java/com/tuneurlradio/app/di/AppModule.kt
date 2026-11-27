package com.tuneurlradio.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tuneurlradio.app.data.local.AppDatabase
import com.tuneurlradio.app.data.local.dao.HistoryEngagementDao
import com.tuneurlradio.app.data.local.dao.SavedEngagementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.dekidea.tuneurl.api.TuneUrlApi
import com.dekidea.tuneurl.api.TuneUrlEndpoints
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tuneurlradio.db"
        ).build()
    }

    @Provides
    fun provideSavedEngagementDao(database: AppDatabase): SavedEngagementDao {
        return database.savedEngagementDao()
    }

    @Provides
    fun provideHistoryEngagementDao(database: AppDatabase): HistoryEngagementDao {
        return database.historyEngagementDao()
    }

    /**
     * TuneURL server endpoints (were written into SDK SharedPreferences by
     * the old MainActivity.initializeTuneURLSDK()). The poll endpoint is still plain
     * HTTP; it is allowed in network_security_config.xml until it has HTTPS.
     */
    @Provides
    @Singleton
    fun provideTuneUrlEndpoints(): TuneUrlEndpoints = TuneUrlEndpoints(
        searchFingerprintUrl = "https://pnz3vadc52.execute-api.us-east-2.amazonaws.com/dev/search-fingerprint",
        interestsUrl = "https://65neejq3c9.execute-api.us-east-2.amazonaws.com/interests",
        pollUrl = "http://pollapiwebservice.us-east-2.elasticbeanstalk.com/api/pollapi",
        cyoaUrl = "https://pnz3vadc52.execute-api.us-east-2.amazonaws.com/dev/get-cyoa-mp3"
    )

    @Provides
    @Singleton
    fun provideTuneUrlApi(endpoints: TuneUrlEndpoints, okHttpClient: OkHttpClient): TuneUrlApi =
        TuneUrlApi(endpoints, okHttpClient)
}
