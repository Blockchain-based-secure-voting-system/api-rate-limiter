package com.cutm.nt14.ui.endpoints

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.Endpoint
import com.cutm.nt14.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndpointScreen(
    viewModel: EndpointViewModel = hiltViewModel()
) {
    val endpoints by viewModel.endpoints.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var endpointToDelete by remember { mutableStateOf<Endpoint?>(null) }

    val isAdmin = userRole == UserRole.ADMIN

    Scaffold(
        topBar = { TopAppBar(title = { Text("Endpoints") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(endpoints) { endpoint ->
                ListItem(
                    headlineContent = { Text(endpoint.name) },
                    supportingContent = { Text("${endpoint.method} ${endpoint.baseUrl}") },
                    trailingContent = {
                        if (isAdmin) {
                            IconButton(onClick = { endpointToDelete = endpoint }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
            }
        }

        if (showAddDialog) {
            AddEndpointDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, url, method ->
                    viewModel.addEndpoint(name, url, method)
                    showAddDialog = false
                }
            )
        }

        endpointToDelete?.let { endpoint ->
            AlertDialog(
                onDismissRequest = { endpointToDelete = null },
                title = { Text("Delete Endpoint") },
                text = { Text("Are you sure you want to delete ${endpoint.name}?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteEndpoint(endpoint)
                        endpointToDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { endpointToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun AddEndpointDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("GET") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Endpoint") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
                // Simple method selector
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, url, method) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
