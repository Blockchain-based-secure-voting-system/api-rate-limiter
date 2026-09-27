package com.cutm.nt14.ui.ratelimits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
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
import com.cutm.nt14.data.local.entities.RateLimit
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.ui.components.*

@Composable
fun RateLimitScreen(
    viewModel: RateLimitViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var ruleToDelete by remember { mutableStateOf<RateLimit?>(null) }

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
                            text = "TRAFFIC POLICIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleCyan,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Rate Limits",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    GlassBadge(
                        text = if (isAdmin) "ADMIN" else "VIEWER",
                        color = if (isAdmin) ApplePurple else AppleCyan
                    )
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
                    Text(
                        text = "${rules.size} ACTIVE GATEWAY ALGORITHMS (TOKEN BUCKET + SLIDING WINDOW)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp
                    )
                }

                items(rules) { rule ->
                    val actionColor = if (rule.action == "BLOCK") AppleRed else AppleOrange

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = GlassSurfaceDark,
                        borderBrush = Brush.horizontalGradient(
                            listOf(AppleCyan.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AppleCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rule.endpointId,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column {
                                        Text(
                                            text = "RATE LIMIT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                        Text(
                                            text = "${rule.limitPerMin} req/min",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "BURST CAPACITY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                        Text(
                                            text = "${rule.burstLimit} tokens",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleCyan
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "THROTTLE ACTION",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(actionColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = rule.action,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = actionColor
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAdmin) {
                                IconButton(
                                    onClick = { ruleToDelete = rule },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(GlassSurfaceLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Rule",
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

        ruleToDelete?.let { rule ->
            AlertDialog(
                onDismissRequest = { ruleToDelete = null },
                containerColor = Color(0xFF161A28),
                title = { Text("Disable Rate Limit", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to disable rate limiting for ${rule.endpointId}?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { ruleToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                    ) {
                        Text("Disable", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { ruleToDelete = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}
