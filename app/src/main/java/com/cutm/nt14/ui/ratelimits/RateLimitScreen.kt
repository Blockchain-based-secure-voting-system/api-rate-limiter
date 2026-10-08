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
                            color = PolyPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Rate Limits",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolyTextPrimary
                        )
                    }

                    GlassBadge(
                        text = if (isAdmin) "ADMIN" else "VIEWER",
                        color = if (isAdmin) PolyPurple else PolyPrimary
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp
                )
            ) {
                item {
                    Text(
                        text = "${rules.size} ACTIVE GATEWAY ALGORITHMS (TOKEN BUCKET + SLIDING WINDOW)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }

                if (rules.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White.copy(alpha = 0.90f),
                            elevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PolyTextMuted,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Rate Limiting Rules Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PolyTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Standard gateway default threshold (60 req/min) is currently active across all endpoints.",
                                    fontSize = 12.sp,
                                    color = PolyTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(rules) { rule ->
                    val (actionColor, actionBg) = if (rule.action == "BLOCK") {
                        PolyDanger to PolyDangerBg
                    } else {
                        PolyWarning to PolyWarningBg
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White.copy(alpha = 0.90f),
                        elevation = 2.dp
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(PolyPrimaryLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = PolyPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.endpointId,
                                        color = PolyTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            text = "RATE LIMIT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolyTextMuted
                                        )
                                        Text(
                                            text = "${rule.limitPerMin} req/min",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolyTextPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "BURST TOKENS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolyTextMuted
                                        )
                                        Text(
                                            text = "${rule.burstLimit} burst",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolyPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "ACTION",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolyTextMuted
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(actionBg)
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
                                        .background(PolyDangerBg)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Rule",
                                        tint = PolyDanger,
                                        modifier = Modifier.size(16.dp)
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

        ruleToDelete?.let { rule ->
            AlertDialog(
                onDismissRequest = { ruleToDelete = null },
                containerColor = Color.White,
                title = { Text("Disable Rate Limit", color = PolyTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to disable rate limiting for ${rule.endpointId}?",
                        color = PolyTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { ruleToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PolyDanger)
                    ) {
                        Text("Disable", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { ruleToDelete = null }) {
                        Text("Cancel", color = PolyTextSecondary)
                    }
                }
            )
        }
    }
}
