package com.cutm.nt14.data.remote

import com.cutm.nt14.BuildConfig
import com.cutm.nt14.data.local.entities.Endpoint
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SheetsDataSource @Inject constructor() {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    
    // In a real app, this should be initialized with proper OAuth2 credentials.
    // For now, we use a placeholder setup.
    private val sheetsService: Sheets by lazy {
        Sheets.Builder(transport, jsonFactory, null)
            .setApplicationName("NT14 API Optimizer")
            .build()
    }

    suspend fun pushEndpoints(endpoints: List<Endpoint>) {
        val spreadsheetId = BuildConfig.SHEETS_SPREADSHEET_ID
        if (spreadsheetId.isEmpty()) return

        val values = endpoints.map { 
            listOf(it.endpointId, it.name, it.baseUrl, it.method, it.status, it.ownerEmail)
        }

        val body = ValueRange().setValues(values)
        
        // Use API Key if provided, or the service will fail if auth is missing
        val request = sheetsService.spreadsheets().values()
            .update(spreadsheetId, "Endpoints!A2", body)
            .setValueInputOption("RAW")
            
        if (BuildConfig.SHEETS_API_KEY.isNotEmpty()) {
            request.setKey(BuildConfig.SHEETS_API_KEY)
        }
        
        request.execute()
    }

    suspend fun pushLogs(logs: List<com.cutm.nt14.data.local.entities.RequestLog>) {
        val spreadsheetId = BuildConfig.SHEETS_SPREADSHEET_ID
        if (spreadsheetId.isEmpty()) return
        val values = logs.map { listOf(it.logId, it.endpointId, it.timestamp, it.sourceIp, it.userId, it.statusCode, it.latencyMs) }
        val body = ValueRange().setValues(values)
        sheetsService.spreadsheets().values().update(spreadsheetId, "RequestLogs!A2", body).setValueInputOption("RAW").execute()
    }

    suspend fun pushRateLimits(rules: List<com.cutm.nt14.data.local.entities.RateLimit>) {
        val spreadsheetId = BuildConfig.SHEETS_SPREADSHEET_ID
        if (spreadsheetId.isEmpty()) return
        val values = rules.map { listOf(it.ruleId, it.endpointId, it.limitPerMin, it.burstLimit, it.action, it.updatedAt) }
        val body = ValueRange().setValues(values)
        sheetsService.spreadsheets().values().update(spreadsheetId, "RateLimits!A2", body).setValueInputOption("RAW").execute()
    }
}
