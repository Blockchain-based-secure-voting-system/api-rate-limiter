package com.cutm.nt14.ui.abuse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentScreen(
    viewModel: IncidentViewModel = hiltViewModel()
) {
    val abuseEvents by viewModel.abuseEvents.collectAsState()
    val ddosIncidents by viewModel.ddosIncidents.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var ipToBlacklist by remember { mutableStateOf<String?>(null) }

    val isAdmin = userRole == UserRole.ADMIN

    Scaffold(
        topBar = { TopAppBar(title = { Text("Security Incidents") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item { Text("DDoS Incidents", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
            items(ddosIncidents) { incident ->
                ListItem(
                    headlineContent = { Text("DDoS: ${incident.endpointId}") },
                    supportingContent = { Text("Severity: ${incident.severity} | Spike: ${incident.requestSpike}") },
                    trailingContent = { Text(incident.status) }
                )
            }
            
            item { HorizontalDivider() }
            
            item { Text("Abuse Events", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
            items(abuseEvents) { event ->
                ListItem(
                    headlineContent = { Text("${event.eventType}") },
                    supportingContent = { Text("Risk Score: ${event.riskScore} | Log ID: ${event.logId}") },
                    trailingContent = {
                        if (isAdmin) {
                            TextButton(onClick = { ipToBlacklist = "IP from ${event.logId}" }) {
                                Text("Blacklist")
                            }
                        } else {
                            Text(event.action)
                        }
                    }
                )
            }
        }

        ipToBlacklist?.let { ip ->
            AlertDialog(
                onDismissRequest = { ipToBlacklist = null },
                title = { Text("Blacklist IP") },
                text = { Text("Are you sure you want to blacklist $ip?") },
                confirmButton = {
                    Button(onClick = { ipToBlacklist = null }) { Text("Blacklist") }
                },
                dismissButton = {
                    TextButton(onClick = { ipToBlacklist = null }) { Text("Cancel") }
                }
            )
        }
    }
}
