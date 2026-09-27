package com.cutm.nt14.ui.endpoints

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.Endpoint
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.ui.components.*

@Composable
fun EndpointScreen(
    viewModel: EndpointViewModel = hiltViewModel()
) {
    val endpoints by viewModel.endpoints.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var endpointToDelete by remember { mutableStateOf<Endpoint?>(null) }

    val isAdmin = userRole == UserRole.ADMIN

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROUTING GATEWAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleCyan,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "API Endpoints",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppleBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Endpoint",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${endpoints.size} REGISTERED GATEWAY ROUTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        GlassBadge(
                            text = if (isAdmin) "ADMIN ACCESS" else "VIEWER MODE",
                            color = if (isAdmin) ApplePurple else AppleCyan
                        )
                    }
                }

                items(endpoints) { endpoint ->
                    val methodColor = when (endpoint.method.uppercase()) {
                        "GET" -> AppleGreen
                        "POST" -> AppleBlue
                        "PUT" -> AppleOrange
                        "DELETE" -> AppleRed
                        else -> AppleCyan
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = GlassSurfaceDark,
                        borderBrush = Brush.horizontalGradient(
                            listOf(methodColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(methodColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = endpoint.method,
                                            color = methodColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = endpoint.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = endpoint.baseUrl,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Status: ${endpoint.status} • Owner: ${endpoint.ownerEmail}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }

                            if (isAdmin) {
                                IconButton(
                                    onClick = { endpointToDelete = endpoint },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(GlassSurfaceLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = AppleRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        if (showAddDialog) {
            AddEndpointGlassDialog(
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
                containerColor = Color(0xFF161A28),
                title = { Text("Delete Endpoint", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to remove ${endpoint.name} (${endpoint.baseUrl}) from gateway routing?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEndpoint(endpoint)
                            endpointToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { endpointToDelete = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
fun AddEndpointGlassDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("/api/") }
    var method by remember { mutableStateOf("GET") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161A28),
        title = { Text("Add Gateway Endpoint", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route Name (e.g. Invoices API)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppleCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = AppleCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Path (e.g. /api/invoices)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppleCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = AppleCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "HTTP Method:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                        val isSel = method == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AppleBlue else Color.White.copy(alpha = 0.08f))
                                .border(
                                    BorderStroke(1.dp, if (isSel) AppleCyan else Color.White.copy(alpha = 0.15f)),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { method = m }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = m,
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onConfirm(name.trim(), url.trim(), method)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
            ) {
                Text("Add Route", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
