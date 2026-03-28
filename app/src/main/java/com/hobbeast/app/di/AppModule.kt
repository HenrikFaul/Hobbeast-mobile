package com.hobbeast.app.di

import android.content.Context
import androidx.room.Room
import com.hobbeast.app.BuildConfig
import com.hobbeast.app.data.local.*
import com.hobbeast.app.data.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── OkHttp ───────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    // ─── Supabase ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    // ─── Retrofit instances ───────────────────────────────────────────────────

    private fun buildRetrofit(client: OkHttpClient, baseUrl: String) =
        Retrofit.Builder().baseUrl(baseUrl).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun provideTicketmasterApi(client: OkHttpClient): TicketmasterApi =
        buildRetrofit(client, "https://app.ticketmaster.com/").create(TicketmasterApi::class.java)

    @Provides @Singleton
    fun provideSeatGeekApi(client: OkHttpClient): SeatGeekApi =
        buildRetrofit(client, "https://api.seatgeek.com/2/").create(SeatGeekApi::class.java)

    @Provides @Singleton
    fun provideGeoapifyApi(client: OkHttpClient): GeoapifyApi =
        buildRetrofit(client, "https://api.geoapify.com/").create(GeoapifyApi::class.java)

    @Provides @Singleton
    fun provideMapyApi(client: OkHttpClient): MapyApi =
        buildRetrofit(client, "https://api.mapy.cz/v1/").create(MapyApi::class.java)

    @Provides @Singleton
    fun provideMapyService(mapy: MapyApi): MapyTripPlanningService =
        MapyTripPlanningService(mapy, BuildConfig.MAPS_API_KEY)

    // ─── Room ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HobbeastDatabase =
        Room.databaseBuilder(context, HobbeastDatabase::class.java, "hobbeast.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideEventDao(db: HobbeastDatabase): EventDao = db.eventDao()
    @Provides fun provideProfileDao(db: HobbeastDatabase): ProfileDao = db.profileDao()
    @Provides fun provideReminderDao(db: HobbeastDatabase): ReminderDao = db.reminderDao()
}
