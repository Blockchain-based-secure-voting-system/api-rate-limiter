package com.cutm.nt14.ui.dashboard

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.data.remote.GatewayConnectionState
import com.cutm.nt14.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showHostDialog by remember { mutableStateOf(false) }

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
                            text = "NT14 OPTIMIZER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleCyan,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Live Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassSurfaceDark)
                                .border(BorderStroke(1.dp, GlassBorderSubtle), CircleShape)
                                .clickable { showHostDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Gateway Host Settings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassSurfaceDark)
                                .border(BorderStroke(1.dp, GlassBorderSubtle), CircleShape)
                                .clickable {
                                    viewModel.logout()
                                    onLogout()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = AppleRed,
                                modifier = Modifier.size(18.dp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Live Gateway Connection Pill
                item {
                    GatewayConnectionCard(
                        connectionState = uiState.connectionState,
                        connectedHost = uiState.connectedHost,
                        onClick = { showHostDialog = true }
                    )
                }

                // 2. Action Message Banner (if any)
                if (uiState.actionMessage != null) {
                    item {
                        GlassCard(
                            backgroundColor = Color(0xFF1E2746).copy(alpha = 0.85f),
                            borderBrush = Brush.horizontalGradient(listOf(AppleBlue, AppleCyan))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.actionMessage!!,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "DISMISS",
                                    color = AppleCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.clearActionMessage() }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Real-Time Action Control Deck
                item {
                    Text(
                        text = "LIVE GATEWAY ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            text = "⚡ Test GET",
                            accentColor = AppleBlue,
                            onClick = { viewModel.fireTestRequest("/api/users") },
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "🔥 Blast (18)",
                            accentColor = AppleOrange,
                            onClick = { viewModel.simulateAttackBurst() },
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "🗑️ Clear",
                            accentColor = AppleRed,
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                // 4. iOS Glass Metric Cards (2x2 Grid)
                item {
                    Text(
                        text = "REAL-TIME METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassMetricTile(
                                title = "Endpoints",
                                value = uiState.endpointCount.toString(),
                                subtitle = "Active API routes",
                                accentColor = ApplePurple,
                                icon = Icons.Default.Place,
                                modifier = Modifier.weight(1f)
                            )
                            GlassMetricTile(
                                title = "Total Requests",
                                value = uiState.totalRequests.toString(),
                                subtitle = "Processed live",
                                accentColor = AppleCyan,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassMetricTile(
                                title = "Error / Throttle",
                                value = "%.1f%%".format(uiState.errorRate * 100),
                                subtitle = "Rate limit breaches",
                                accentColor = if (uiState.errorRate > 0) AppleOrange else AppleGreen,
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f)
                            )
                            GlassMetricTile(
                                title = "Active Incidents",
                                value = uiState.activeIncidents.toString(),
                                subtitle = if (uiState.activeIncidents > 0) "Immediate attention" else "Zero threats",
                                accentColor = if (uiState.activeIncidents > 0) AppleRed else AppleGreen,
                                icon = Icons.Default.Notifications,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. Live Traffic Stream Feed
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE TRAFFIC STREAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        GlassBadge(
                            text = if (uiState.recentLogs.isEmpty()) "IDLE" else "REALTIME STREAM",
                            color = if (uiState.recentLogs.isEmpty()) Color.Gray else AppleGreen
                        )
                    }
                }

                if (uiState.recentLogs.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = GlassSurfaceLight
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No Traffic",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No traffic recorded yet",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '⚡ Test GET' or '🔥 Blast' above to trigger live requests!",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentLogs) { log ->
                        LiveLogGlassItem(log)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // padding for floating glass bottom dock
                }
            }
        }

        // Host Switcher / Configuration Dialog
        if (showHostDialog) {
            GatewayHostDialog(
                currentHost = uiState.connectedHost,
                onDismiss = { showHostDialog = false },
                onSave = { newHost ->
                    viewModel.updateGatewayHost(newHost)
                    showHostDialog = false
                }
            )
        }
    }
}

@Composable
fun GatewayConnectionCard(
    connectionState: GatewayConnectionState,
    connectedHost: String,
    onClick: () -> Unit
) {
    val isConnected = connectionState == GatewayConnectionState.CONNECTED
    val isConnecting = connectionState == GatewayConnectionState.CONNECTING
    val statusColor = when {
        isConnected -> AppleGreen
        isConnecting -> AppleOrange
        else -> AppleRed
    }
    val statusText = when {
        isConnected -> "GATEWAY ONLINE (LIVE)"
        isConnecting -> "CONNECTING TO GATEWAY..."
        else -> "DISCONNECTED (TAP TO CONFIGURE)"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = Color(0xFF131724).copy(alpha = 0.82f),
        borderBrush = Brush.horizontalGradient(
            listOf(statusColor.copy(alpha = 0.45f), Color.White.copy(alpha = 0.1f))
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "ws://$connectedHost/ws/events",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Host",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GlassMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(130.dp),
        backgroundColor = GlassSurfaceDark,
        borderBrush = Brush.linearGradient(
            listOf(accentColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun LiveLogGlassItem(log: RequestLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = sdf.format(Date(log.timestamp))
    val isBlocked = log.statusCode >= 400
    val badgeColor = if (isBlocked) AppleRed else AppleGreen
    val statusLabel = if (isBlocked) "${log.statusCode} BLOCKED" else "${log.statusCode} OK"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassSurfaceDark,
        borderBrush = Brush.horizontalGradient(
            listOf(badgeColor.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
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
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.endpointId,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${log.sourceIp} • ${log.latencyMs} ms",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Text(
                text = time,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GatewayHostDialog(
    currentHost: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var hostInput by remember { mutableStateOf(currentHost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161A28),
        title = {
            Text("Gateway Server Host", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select preset or enter IP of host PC running Ktor gateway:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Host:Port") },
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
                    text = "Quick Presets:",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("Wi-Fi (192.168.29.231)") {
                        hostInput = "192.168.29.231:8000"
                    }
                    PresetChip("ADB (127.0.0.1)") {
                        hostInput = "127.0.0.1:8000"
                    }
                    PresetChip("Emulator") {
                        hostInput = "10.0.2.2:8000"
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(hostInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
            ) {
                Text("Connect", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = AppleCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
