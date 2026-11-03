package com.cutm.nt14.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.remote.model.MetricsSeriesDto
import com.cutm.nt14.data.remote.model.TrafficReportDto
import com.cutm.nt14.data.repository.GatewayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: GatewayRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow("1h")
    val selectedRange: StateFlow<String> = _selectedRange.asStateFlow()

    private val _selectedSource = MutableStateFlow("all") // "all" | "real" | "demo"
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _report = MutableStateFlow<TrafficReportDto?>(null)
    val report: StateFlow<TrafficReportDto?> = _report.asStateFlow()

    private val _metricsSeries = MutableStateFlow<MetricsSeriesDto?>(null)
    val metricsSeries: StateFlow<MetricsSeriesDto?> = _metricsSeries.asStateFlow()

    val lastDataSyncMs: StateFlow<Long> = repository.lastDataSyncMs
    val persistenceMode: StateFlow<String> = repository.persistenceMode

    init {
        loadReport("1h", "all")
    }

    fun selectRange(range: String) {
        _selectedRange.value = range
        loadReport(range, _selectedSource.value)
    }

    fun selectSource(source: String) {
        _selectedSource.value = source
        loadReport(_selectedRange.value, source)
    }

    fun loadReport(
        range: String = _selectedRange.value,
        source: String = _selectedSource.value
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val src = source.takeIf { it != "all" }
            _report.value = repository.getReport(range, src)
            _metricsSeries.value = repository.getMetricsSeries(range, src)
            _isLoading.value = false
        }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val src = _selectedSource.value.takeIf { it != "all" }
            val serverCsv = repository.downloadCsvExport(src)
            onReady(serverCsv ?: exportCsvText())
        }
    }

    fun exportCsvText(): String {
        val rep = _report.value ?: return "No report data available"
        val sb = StringBuilder()
        sb.appendLine("NT14 Rate Limiter Traffic Audit Report")
        sb.appendLine("Range,${rep.range}")
        sb.appendLine("Source,${rep.source}")
        sb.appendLine("Storage,${rep.persistence}")
        sb.appendLine("Total Requests,${rep.totalRequests}")
        sb.appendLine("Allowed Requests,${rep.allowedRequests}")
        sb.appendLine("Blocked Requests,${rep.blockedRequests}")
        sb.appendLine("Server Errors (5xx),${rep.serverErrors}")
        sb.appendLine("Error Rate,${rep.errorRate}")
        sb.appendLine("Avg Latency (ms),${rep.avgLatencyMs}")
        sb.appendLine("P95 Latency (ms),${rep.p95LatencyMs}")
        sb.appendLine("Peak RPS,${rep.peakRps}")
        sb.appendLine()
        sb.appendLine("Top Endpoints:")
        rep.topEndpoints.forEach { (ep, count) ->
            sb.appendLine("$ep,$count")
        }
        sb.appendLine()
        sb.appendLine("Top Clients:")
        rep.topClients.forEach { (client, count) ->
            sb.appendLine("$client,$count")
        }
        return sb.toString()
    }
}
