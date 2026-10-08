package com.cutm.nt14.ui.abuse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IncidentScreen(
    viewModel: IncidentViewModel = hiltViewModel()
) {
    val abuseEvents by viewModel.abuseEvents.collectAsState()
    val ddosIncidents by viewModel.ddosIncidents.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var ipToBlacklist by remember { mutableStateOf<String?>(null) }

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
                            text = "SECURITY CENTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyDanger,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Threat Incidents",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyTextPrimary
                        )
                    }

                    GlassBadge(
                        text = if (ddosIncidents.isEmpty() && abuseEvents.isEmpty()) "ALL SECURE" else "THREATS DETECTED",
                        color = if (ddosIncidents.isEmpty() && abuseEvents.isEmpty()) PolySuccess else PolyDanger
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp
                )
            ) {
                // DDoS Incidents
                item {
                    Text(
                        text = "LIVE DDOS SPIKE DETECTIONS (${ddosIncidents.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyTextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                if (ddosIncidents.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.85f),
                            elevation = 1.dp
                        ) {
                            Text(
                                text = "No active DDoS attacks detected across gateway endpoints.",
                                color = PolyTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(ddosIncidents) { incident ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.92f),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = PolyDanger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "DDoS: ${incident.endpointId}",
                                            color = PolyTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Spike: ${incident.requestSpike} reqs • Severity: ${incident.severity}",
                                        color = PolyTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PolyDangerBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = incident.status,
                                        color = PolyDanger,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Abuse Events
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ABUSE & IP BLOCK LOGS (${abuseEvents.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyTextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                if (abuseEvents.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.85f),
                            elevation = 1.dp
                        ) {
                            Text(
                                text = "Zero rate limit breaches or abusive IPs reported.",
                                color = PolyTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(abuseEvents) { event ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.createdAt))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.92f),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.eventType,
                                        color = PolyTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Risk: ${event.riskScore}/100 • Action: ${event.action} • $timeStr",
                                        color = PolyTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                if (isAdmin) {
                                    GlassButton(
                                        text = "Blacklist",
                                        accentColor = PolyDanger,
                                        onClick = { ipToBlacklist = "IP from ${event.logId}" }
                                    )
                                } else {
                                    Text(
                                        text = event.action,
                                        color = PolyWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }

        ipToBlacklist?.let { ip ->
            AlertDialog(
                onDismissRequest = { ipToBlacklist = null },
                containerColor = Color.White,
                title = { Text("Blacklist IP Address", color = PolyTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to permanently blacklist $ip on the gateway?",
                        color = PolyTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { ipToBlacklist = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PolyDanger)
                    ) {
                        Text("Confirm Blacklist", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { ipToBlacklist = null }) {
                        Text("Cancel", color = PolyTextSecondary)
                    }
                }
            )
        }
    }
}
