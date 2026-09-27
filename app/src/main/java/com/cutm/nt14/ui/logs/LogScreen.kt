package com.cutm.nt14.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.RequestLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Request Logs") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(logs) { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
fun LogItem(log: RequestLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = sdf.format(Date(log.timestamp))
    
    val isError = log.statusCode >= 400
    val color = if (isError) MaterialTheme.colorScheme.error else Color.Unspecified

    ListItem(
        headlineContent = { Text("Status: ${log.statusCode}", color = color) },
        supportingContent = { Text("${log.sourceIp} | ${log.latencyMs}ms") },
        trailingContent = { Text(time) }
    )
}
