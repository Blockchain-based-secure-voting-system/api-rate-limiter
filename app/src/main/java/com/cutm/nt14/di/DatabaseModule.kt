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
        ).addCallback(object : RoomDatabase.Callback() {
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
            // 1. Endpoints
            val ep1 = Endpoint("ep_auth", "User Auth API", "/api/v1/auth/login", "POST", "ACTIVE", "admin@cutm.nt14.com")
            val ep2 = Endpoint("ep_profile", "User Profile API", "/api/v1/users/profile", "GET", "ACTIVE", "admin@cutm.nt14.com")
            val ep3 = Endpoint("ep_checkout", "Payments Checkout API", "/api/v1/payments/checkout", "POST", "ACTIVE", "admin@cutm.nt14.com")
            val ep4 = Endpoint("ep_search", "Product Search API", "/api/v1/products/search", "GET", "ACTIVE", "admin@cutm.nt14.com")

            db.endpointDao().insertEndpoint(ep1)
            db.endpointDao().insertEndpoint(ep2)
            db.endpointDao().insertEndpoint(ep3)
            db.endpointDao().insertEndpoint(ep4)

            // 2. Request Logs
            val logs = listOf(
                RequestLog("log_1", "ep_auth", now - 5000, "192.168.1.105", "usr_1", 200, 85, SyncStatus.SYNCED),
                RequestLog("log_2", "ep_auth", now - 4000, "192.168.1.105", "usr_1", 429, 40, SyncStatus.SYNCED),
                RequestLog("log_3", "ep_auth", now - 3000, "192.168.1.105", "usr_1", 429, 35, SyncStatus.SYNCED),
                RequestLog("log_4", "ep_checkout", now - 2000, "10.0.0.52", "usr_2", 200, 120, SyncStatus.SYNCED),
                RequestLog("log_5", "ep_checkout", now - 1000, "10.0.0.53", "usr_3", 500, 850, SyncStatus.SYNCED),
                RequestLog("log_6", "ep_search", now - 500, "172.16.0.8", "usr_4", 200, 45, SyncStatus.SYNCED)
            )
            logs.forEach { db.requestLogDao().insertLog(it) }

            // 3. Rate Limits
            val rl1 = RateLimit("rl_1", "ep_auth", 60, 10, "THROTTLE", now, SyncStatus.SYNCED)
            val rl2 = RateLimit("rl_2", "ep_checkout", 120, 20, "BLOCK", now, SyncStatus.SYNCED)
            val rl3 = RateLimit("rl_3", "ep_search", 300, 50, "THROTTLE", now, SyncStatus.SYNCED)
            db.rateLimitDao().insertRule(rl1)
            db.rateLimitDao().insertRule(rl2)
            db.rateLimitDao().insertRule(rl3)

            // 4. Abuse Events
            val ae1 = AbuseEvent("ae_1", "log_2", "Credential Stuffing", 85, "THROTTLE_IP", now - 3000, SyncStatus.SYNCED)
            db.abuseEventDao().insertEvent(ae1)

            // 5. DDoS Incidents
            val ddos1 = DDoSIncident("ddos_1", "ep_checkout", now - 10000, 450, "HIGH", "ACTIVE", SyncStatus.SYNCED)
            db.ddosIncidentDao().insertIncident(ddos1)

            // 6. Reports
            val rpt1 = Report("rpt_1", "Daily Summary", "High traffic spike on /api/v1/payments/checkout detected.", "Increase rate limit for /api/v1/products/search to 300 req/min. Throttle IP 192.168.1.105.", now - 3600000, SyncStatus.SYNCED)
            db.reportDao().insertReport(rpt1)
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
