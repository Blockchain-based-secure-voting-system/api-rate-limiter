package com.cutm.nt14.ui.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.remote.model.TargetStatusDto
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.ui.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DataFreshnessBanner(
    lastSyncMs: Long,
    persistenceMode: String,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val ageText = if (lastSyncMs <= 0L) {
        "Syncing from REST…"
    } else {
        val sec = ((nowMs - lastSyncMs) / 1000L).coerceAtLeast(0L)
        if (sec < 2L) "Live just now" else "Updated ${sec}s ago"
    }
    val dbLabel = if (persistenceMode.equals("postgres", true)) "PostgreSQL Durable" else "In-Memory Cache"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (lastSyncMs > 0L) PolySuccess else PolyWarning)
        )
        Text(
            text = "$dbLabel • $ageText",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PolyTextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SourceTagBadge(source: String) {
    val isReal = source.equals("real", ignoreCase = true)
    val bg = if (isReal) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val fg = if (isReal) Color(0xFF15803D) else Color(0xFFB45309)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isReal) "REAL" else "DEMO",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LatencySparkline(
    latencies: List<Long>,
    upFlags: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (upFlags.lastOrNull() == false) PolyDanger else PolyPrimary
    Canvas(modifier = modifier) {
        if (latencies.isEmpty()) return@Canvas
        val pts = if (latencies.size == 1) listOf(latencies[0], latencies[0]) else latencies
        val maxVal = (pts.maxOrNull() ?: 100L).coerceAtLeast(10L).toFloat()
        val minVal = (pts.minOrNull() ?: 0L).coerceAtLeast(0L).toFloat()
        val span = (maxVal - minVal).coerceAtLeast(10f)
        val stepX = size.width / (pts.size - 1).coerceAtLeast(1)

        val path = Path()
        pts.forEachIndexed { idx, v ->
            val x = idx * stepX
            val norm = ((v - minVal) / span).coerceIn(0f, 1f)
            val y = size.height - (norm * (size.height * 0.78f) + size.height * 0.11f)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        pts.forEachIndexed { idx, v ->
            val x = idx * stepX
            val norm = ((v - minVal) / span).coerceIn(0f, 1f)
            val y = size.height - (norm * (size.height * 0.78f) + size.height * 0.11f)
            val isPointUp = upFlags.getOrNull(idx) ?: true
            drawCircle(
                color = if (isPointUp) PolySuccess else PolyDanger,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun MonitoringScreen(
    viewModel: MonitoringViewModel = hiltViewModel()
) {
    val targets by viewModel.targets.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val role by viewModel.userRole.collectAsState()
    val lastSyncMs by viewModel.lastDataSyncMs.collectAsState()
    val persistenceMode by viewModel.persistenceMode.collectAsState()
    val isAdmin = role == UserRole.ADMIN

    var showAddDialog by remember { mutableStateOf(false) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "UPSTREAM SYNTHETIC PROBES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolyPrimary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "API Monitoring",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolyTextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdmin) {
                                PillActionButton(
                                    text = "Add Target",
                                    icon = Icons.Default.Add,
                                    onClick = { showAddDialog = true }
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refresh() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PolyPrimaryLight)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PolyPrimary)
                            }
                        }
                    }

                    DataFreshnessBanner(lastSyncMs = lastSyncMs, persistenceMode = persistenceMode)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (targets.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.94f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No Monitored Targets Loaded", fontWeight = FontWeight.Bold, color = PolyTextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Tap Refresh to query /api/targets/status from PostgreSQL.", fontSize = 12.sp, color = PolyTextSecondary)
                            }
                        }
                    }
                } else {
                    items(targets, key = { it.id }) { t ->
                        TargetStatusCard(
                            target = t,
                            isAdmin = isAdmin,
                            onProbeNow = { viewModel.probeNow(t.id) },
                            onDelete = { viewModel.deleteTarget(t.id) }
                        )
                    }
                }

                val targetIncidents = incidents.filter { it.type.contains("Target", true) || it.targetId != null }
                if (targetIncidents.isNotEmpty()) {
                    item {
                        Text(
                            text = "DOWNTIME INCIDENTS (3+ CONSECUTIVE PROBE FAILURES)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyDanger,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(targetIncidents, key = { it.id }) { inc ->
                        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (inc.resolvedAt == null) PolyDangerBg else Color.White.copy(alpha = 0.94f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (inc.resolvedAt == null) "OPEN • ${inc.type}" else "RESOLVED • ${inc.type}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (inc.resolvedAt == null) PolyDanger else PolySuccess
                                    )
                                    Text(
                                        text = fmt.format(Date(inc.timestamp)),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = PolyTextSecondary
                                    )
                                }
                                Text(text = inc.detail, fontSize = 12.sp, color = PolyTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        var expected by remember { mutableStateOf("200") }
        var interval by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Monitored Target", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Target URL") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expected,
                            onValueChange = { expected = it },
                            label = { Text("Expected HTTP") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it },
                            label = { Text("Interval (s)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && url.startsWith("http")) {
                            viewModel.addTarget(
                                name = name.trim(),
                                url = url.trim(),
                                method = "GET",
                                expectedStatus = expected.toIntOrNull() ?: 200,
                                intervalSec = interval.toIntOrNull() ?: 30
                            )
                            showAddDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TargetStatusCard(
    target: TargetStatusDto,
    isAdmin: Boolean,
    onProbeNow: () -> Unit,
    onDelete: () -> Unit
) {
    val isUp = target.up == true
    val statusText = when (target.up) {
        true -> "UP (${target.lastStatusCode ?: 200})"
        false -> "DOWN (${target.lastStatusCode ?: "ERR"})"
        null -> "PENDING"
    }
    val statusColor = when (target.up) {
        true -> PolySuccess
        false -> PolyDanger
        null -> PolyWarning
    }
    val statusBg = when (target.up) {
        true -> PolySuccessBg
        false -> PolyDangerBg
        null -> PolyWarningBg
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color.White.copy(alpha = 0.95f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PolyTextPrimary
                    )
                    Text(
                        text = "${target.method} ${target.url}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PolyTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Latency Sparkline
            if (target.recentLatencies.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LATENCY SPARKLINE (${target.recentLatencies.size} probes)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyTextMuted
                        )
                        Text(
                            text = "Last: ${target.lastLatencyMs ?: 0}ms • 24h Uptime: ${target.uptimePct24h?.let { "$it%" } ?: "100%"}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = PolyTextSecondary
                        )
                    }
                    LatencySparkline(
                        latencies = target.recentLatencies,
                        upFlags = target.recentUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            if (!isUp && target.lastError != null) {
                Text(
                    text = "Failure (${target.consecutiveFailures}x): ${target.lastError}",
                    fontSize = 11.sp,
                    color = PolyDanger,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onProbeNow) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Probe Now", fontSize = 12.sp)
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = PolyDanger)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
