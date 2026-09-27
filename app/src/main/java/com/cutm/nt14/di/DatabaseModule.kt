package com.cutm.nt14.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cutm.nt14.data.local.NT14Database
import com.cutm.nt14.data.local.SyncStatus
import com.cutm.nt14.data.local.entities.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NT14Database {
        lateinit var database: NT14Database
        database = Room.databaseBuilder(
            context,
            NT14Database::class.java,
            "nt14_database"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Executors.newSingleThreadExecutor().execute {
                    seedDatabase(database)
                }
            }
        }).build()
        return database
    }

    private fun seedDatabase(db: NT14Database) {
        val now = System.currentTimeMillis()

        runBlocking {
            // Seed ONLY real gateway endpoints and active rules.
            // NO MOCK REQUEST LOGS OR INCIDENTS — all data is fed in real-time from the live Gateway WebSocket!
            val ep1 = Endpoint("ep_users", "Users API", "/api/users", "GET", "ACTIVE", "admin@cutm.nt14.com")
            val ep2 = Endpoint("ep_orders", "Orders API", "/api/orders", "GET", "ACTIVE", "admin@cutm.nt14.com")
            val ep3 = Endpoint("ep_products", "Products API", "/api/products", "GET", "ACTIVE", "admin@cutm.nt14.com")
            val ep4 = Endpoint("ep_rules", "Rules API", "/api/rules", "GET", "ACTIVE", "admin@cutm.nt14.com")

            db.endpointDao().insertEndpoint(ep1)
            db.endpointDao().insertEndpoint(ep2)
            db.endpointDao().insertEndpoint(ep3)
            db.endpointDao().insertEndpoint(ep4)

            val rl1 = RateLimit("rl_users", "/api/users", 10, 5, "THROTTLE", now, SyncStatus.SYNCED)
            val rl2 = RateLimit("rl_orders", "/api/orders", 5, 3, "BLOCK", now, SyncStatus.SYNCED)
            val rl3 = RateLimit("rl_products", "/api/products", 20, 10, "THROTTLE", now, SyncStatus.SYNCED)

            db.rateLimitDao().insertRule(rl1)
            db.rateLimitDao().insertRule(rl2)
            db.rateLimitDao().insertRule(rl3)
        }
    }

    @Provides
    fun provideEndpointDao(db: NT14Database) = db.endpointDao()

    @Provides
    fun provideRequestLogDao(db: NT14Database) = db.requestLogDao()

    @Provides
    fun provideRateLimitDao(db: NT14Database) = db.rateLimitDao()

    @Provides
    fun provideAbuseEventDao(db: NT14Database) = db.abuseEventDao()

    @Provides
    fun provideFingerprintDao(db: NT14Database) = db.fingerprintDao()

    @Provides
    fun provideDDoSIncidentDao(db: NT14Database) = db.ddosIncidentDao()

    @Provides
    fun provideReportDao(db: NT14Database) = db.reportDao()
}
