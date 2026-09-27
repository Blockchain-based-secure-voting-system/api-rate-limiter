package com.cutm.nt14.ui.ratelimits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.RateLimit
import com.cutm.nt14.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateLimitScreen(
    viewModel: RateLimitViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var ruleToDelete by remember { mutableStateOf<RateLimit?>(null) }

    val isAdmin = userRole == UserRole.ADMIN

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rate Limits") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(rules) { rule ->
                RateLimitItem(rule, isAdmin, onDelete = { ruleToDelete = it })
            }
        }

        ruleToDelete?.let { rule ->
            AlertDialog(
                onDismissRequest = { ruleToDelete = null },
                title = { Text("Disable Rule") },
                text = { Text("Are you sure you want to disable/delete this rate limit rule for ${rule.endpointId}?") },
                confirmButton = {
                    Button(onClick = {
                        ruleToDelete = null
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { ruleToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun RateLimitItem(rule: RateLimit, isAdmin: Boolean, onDelete: (RateLimit) -> Unit) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Endpoint: ${rule.endpointId}", style = MaterialTheme.typography.labelSmall)
                Text(text = "Limit: ${rule.limitPerMin} req/min", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Burst: ${rule.burstLimit}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Action: ${rule.action}", style = MaterialTheme.typography.bodySmall)
            }
            if (isAdmin) {
                IconButton(onClick = { onDelete(rule) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
