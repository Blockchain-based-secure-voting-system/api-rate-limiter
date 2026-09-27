package com.cutm.nt14.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.ui.components.*
import com.cutm.nt14.ui.dashboard.WhiteLogItem

@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()

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
                            text = "TRAFFIC MONITOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Request Logs",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyTextPrimary
                        )
                    }

                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PolyDangerBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Logs",
                                tint = PolyDanger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White.copy(alpha = 0.85f),
                        elevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PolyTextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Logs Recorded",
                                color = PolyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Real-time traffic events will appear here dynamically as incoming requests hit the gateway.",
                                color = PolyTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${logs.size} TOTAL LOGGED REQUESTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolyTextSecondary,
                                letterSpacing = 1.sp
                            )
                            GlassBadge(text = "LIVE FEED", color = PolySuccess)
                        }
                    }

                    items(logs) { log ->
                        WhiteLogItem(log)
                    }

                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }
    }
}
