package com.cutm.nt14.ui.dashboard

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
import androidx.compose.ui.draw.shadow
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
                            color = PolyPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Live Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyTextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(2.dp, CircleShape, ambientColor = Color(0x10000000))
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape)
                                .clickable { showHostDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Gateway Host Settings",
                                tint = PolyTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(2.dp, CircleShape, ambientColor = Color(0x10000000))
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape)
                                .clickable {
                                    viewModel.logout()
                                    onLogout()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = PolyDanger,
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
                // 1. Live Gateway Connection Card
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
                            backgroundColor = PolyPrimaryLight,
                            borderBrush = GlassBorderSubtle
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.actionMessage!!,
                                    color = PolyPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "DISMISS",
                                    color = PolyPrimary,
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
                        fontWeight = FontWeight.Bold,
                        color = PolyTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            text = "Test GET",
                            accentColor = PolyPrimary,
                            onClick = { viewModel.fireTestRequest("/api/polylance/escrows") },
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "Simulate Burst",
                            accentColor = PolyWarning,
                            onClick = { viewModel.simulateAttackBurst() },
                            modifier = Modifier.weight(1.1f)
                        )
                        GlassButton(
                            text = "Clear Logs",
                            accentColor = PolyDanger,
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                // 4. Transparent White Metric Tiles (2x2 Grid)
                item {
                    Text(
                        text = "REAL-TIME METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WhiteMetricTile(
                                title = "Endpoints",
                                value = uiState.endpointCount.toString(),
                                subtitle = "Active routes",
                                accentColor = PolyPurple,
                                icon = Icons.Default.Place,
                                modifier = Modifier.weight(1f)
                            )
                            WhiteMetricTile(
                                title = "Total Requests",
                                value = uiState.totalRequests.toString(),
                                subtitle = "Processed live",
                                accentColor = PolyCyan,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WhiteMetricTile(
                                title = "Error / Throttle",
                                value = "%.1f%%".format(uiState.errorRate * 100),
                                subtitle = "429 rate throttled",
                                accentColor = if (uiState.errorRate > 0) PolyWarning else PolySuccess,
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f)
                            )
                            WhiteMetricTile(
                                title = "Active Incidents",
                                value = uiState.activeIncidents.toString(),
                                subtitle = if (uiState.activeIncidents > 0) "Immediate attention" else "Zero threats",
                                accentColor = if (uiState.activeIncidents > 0) PolyDanger else PolySuccess,
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
                            fontWeight = FontWeight.Bold,
                            color = PolyTextSecondary,
                            letterSpacing = 1.sp
                        )
                        GlassBadge(
                            text = if (uiState.recentLogs.isEmpty()) "IDLE" else "REALTIME STREAM",
                            color = if (uiState.recentLogs.isEmpty()) PolyTextMuted else PolySuccess
                        )
                    }
                }

                if (uiState.recentLogs.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.75f)
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
                                    tint = PolyTextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No traffic recorded yet",
                                    color = PolyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Test GET' or 'Simulate Burst' above to trigger live requests.",
                                    color = PolyTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentLogs) { log ->
                        WhiteLogItem(log)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }

        // Host Switcher / Configuration Dialog
        if (showHostDialog) {
            GatewayHostWhiteDialog(
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
        isConnected -> PolySuccess
        isConnecting -> PolyWarning
        else -> PolyDanger
    }
    val statusText = when {
        isConnected -> "GATEWAY ONLINE (LIVE)"
        isConnecting -> "CONNECTING TO GATEWAY..."
        else -> "DISCONNECTED (TAP TO CONFIGURE)"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = Color.White.copy(alpha = 0.92f),
        elevation = 3.dp
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
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "ws://$connectedHost/ws/events",
                        fontSize = 13.sp,
                        color = PolyTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Host",
                tint = PolyTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WhiteMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(126.dp),
        backgroundColor = Color.White.copy(alpha = 0.88f),
        elevation = 2.dp
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
                    fontWeight = FontWeight.Bold,
                    color = PolyTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PolyTextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = PolyTextMuted
            )
        }
    }
}

@Composable
fun WhiteLogItem(log: RequestLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = sdf.format(Date(log.timestamp))
    val isBlocked = log.statusCode >= 400
    val badgeColor = if (isBlocked) PolyDanger else PolySuccess
    val badgeBg = if (isBlocked) PolyDangerBg else PolySuccessBg
    val statusLabel = if (isBlocked) "${log.statusCode} BLOCKED" else "${log.statusCode} OK"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White.copy(alpha = 0.90f),
        elevation = 1.dp
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
                            .background(badgeBg)
                            .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
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
                        color = PolyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${log.sourceIp} • ${log.latencyMs} ms",
                    color = PolyTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = time,
                color = PolyTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GatewayHostWhiteDialog(
    currentHost: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var hostInput by remember { mutableStateOf(currentHost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Gateway Server Host", color = PolyTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select preset or enter IP of host machine running Ktor gateway:",
                    color = PolyTextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Host:Port") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolyTextPrimary,
                        unfocusedTextColor = PolyTextPrimary,
                        focusedBorderColor = PolyPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = PolyPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick Presets:",
                    color = PolyTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WhitePresetChip("Wi-Fi (192.168.29.231)") {
                        hostInput = "192.168.29.231:8000"
                    }
                    WhitePresetChip("ADB (127.0.0.1)") {
                        hostInput = "127.0.0.1:8000"
                    }
                    WhitePresetChip("Emulator") {
                        hostInput = "10.0.2.2:8000"
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(hostInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = PolyPrimary)
            ) {
                Text("Connect", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PolyTextSecondary)
            }
        }
    )
}

@Composable
fun WhitePresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PolyPrimaryLight)
            .border(BorderStroke(1.dp, PolyPrimary.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = PolyPrimaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
