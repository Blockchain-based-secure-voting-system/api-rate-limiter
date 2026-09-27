package com.cutm.nt14.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(reports) { report ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(report.generatedAt))
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Report: ${report.period} ($date)", style = MaterialTheme.typography.titleMedium)
                        Text(text = report.summary, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Recommendation: ${report.recommendation}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
