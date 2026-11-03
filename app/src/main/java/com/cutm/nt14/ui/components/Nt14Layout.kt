package com.cutm.nt14.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutm.nt14.data.remote.EndpointType
import com.cutm.nt14.data.remote.GatewayConnectionState
import com.cutm.nt14.data.remote.ResolverState
import com.cutm.nt14.data.repository.GatewayRepository
import com.cutm.nt14.domain.model.UserRole

/**
 * Nt14 Standard Spaced Screen container.
 * Rules: 20dp edge margin, 24dp gaps, 112dp bottom clearance.
 */
@Composable
fun SpacedScreen(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        content = content
    )
}

/**
 * Standard ScreenHeader.
 * Rule:
 * - Eyebrow + icon actions (QR scanner, Sign out; no share icon, no duplicate pills)
 * - Title on its own row
 * - Role chip + identity on the next row (chip must NEVER wrap)
 */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    userRole: UserRole,
    userEmail: String?,
    onScanQr: () -> Unit,
    onSignOut: () -> Unit,
    onOpenHost: (() -> Unit)? = null,
    onToggleRole: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Eyebrow + Icon Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = eyebrow.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Nt14Ui.Accent,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenHost != null) {
                    IconButton(
                        onClick = onOpenHost,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Gateway Host Settings",
                            tint = Nt14Ui.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onScanQr,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Gateway QR",
                        tint = Nt14Ui.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Nt14Ui.Blocked,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Row 2: Title on its own row
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = Nt14Ui.TextPrimary
        )

        // Row 3: Role Chip + Identity (chip never wraps)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val roleText = if (userRole == UserRole.ADMIN) "ADMIN" else "Guest (read-only)"
            val roleBg = if (userRole == UserRole.ADMIN) Nt14Ui.Accent.copy(alpha = 0.12f) else Nt14Ui.TileFill
            val roleTextColor = if (userRole == UserRole.ADMIN) Nt14Ui.Accent else Nt14Ui.TextMuted

            Surface(
                onClick = { onToggleRole?.invoke() },
                enabled = onToggleRole != null,
                color = roleBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (userRole == UserRole.ADMIN) Nt14Ui.Accent.copy(alpha = 0.3f) else Nt14Ui.Outline)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = roleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = roleTextColor,
                        maxLines = 1
                    )
                    if (onToggleRole != null) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Switch Role",
                            tint = roleTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            if (!userEmail.isNullOrBlank()) {
                Text(
                    text = userEmail,
                    fontSize = 12.sp,
                    color = Nt14Ui.TextMuted,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

/**
 * ONE global ConnectionStatusCard.
 * State comes strictly from GatewayEndpointResolver.
 * Rules: Never show an address that isn't the active target. No 10.0.2.2 text on a real phone.
 */
@Composable
fun ConnectionStatusCard(
    repository: GatewayRepository,
    modifier: Modifier = Modifier,
    onOpenHost: (() -> Unit)? = null
) {
    val connectionState by repository.connectionState.collectAsState()
    val resolverState by repository.resolverState.collectAsState()
    val endpointType by repository.endpointType.collectAsState()
    val activeHost by repository.connectedHost.collectAsState()

    // Determine status info based strictly on resolverState
    val (title, detail, bgCol, borderCol, statusCol) = when (val resState = resolverState) {
        is ResolverState.WakingCloud -> Quint(
            "Waking Cloud Gateway...",
            "Cold start probe in flight (~30-60s) • $activeHost",
            Color(0xFFEFF6FF),
            Color(0xFFBFDBFE),
            Nt14Ui.Accent
        )
        is ResolverState.Probing -> Quint(
            "Probing Endpoints...",
            "Testing candidate • ${resState.candidate}",
            Color(0xFFFFFBEB),
            Color(0xFFFDE68A),
            Nt14Ui.Warn
        )
        is ResolverState.Unreachable -> Quint(
            "Gateways Unreachable",
            resState.lastError,
            Color(0xFFFEF2F2),
            Color(0xFFFECACA),
            Nt14Ui.Blocked
        )
        is ResolverState.Connected -> {
            when (val conn = connectionState) {
                is GatewayConnectionState.Connected -> Quint(
                    "Connected to Gateway",
                    activeHost,
                    Color(0xFFF0FDF4),
                    Color(0xFFBBF7D0),
                    Nt14Ui.Ok
                )
                is GatewayConnectionState.Connecting -> Quint(
                    "Connecting to Gateway...",
                    activeHost,
                    Color(0xFFFFFBEB),
                    Color(0xFFFDE68A),
                    Nt14Ui.Warn
                )
                is GatewayConnectionState.WakingServer -> Quint(
                    "Waking Gateway...",
                    activeHost,
                    Color(0xFFEFF6FF),
                    Color(0xFFBFDBFE),
                    Nt14Ui.Accent
                )
                is GatewayConnectionState.Failed -> Quint(
                    "Gateway Offline • ${conn.message}",
                    activeHost,
                    Color(0xFFFEF2F2),
                    Color(0xFFFECACA),
                    Nt14Ui.Blocked
                )
                is GatewayConnectionState.Idle -> Quint(
                    "Gateway Disconnected",
                    activeHost,
                    Color(0xFFF8FAFC),
                    Color(0xFFE2E8F0),
                    Nt14Ui.TextMuted
                )
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenHost?.invoke() },
        color = bgCol,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LOCAL or CLOUD chip
                Surface(
                    color = if (endpointType == EndpointType.LOCAL) Color(0xFFDCFCE7) else Color(0xFFE0E7FF),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (endpointType == EndpointType.LOCAL) Color(0xFF86EFAC) else Color(0xFFA5B4FC))
                ) {
                    Text(
                        text = if (endpointType == EndpointType.LOCAL) "LOCAL" else "CLOUD",
                        color = if (endpointType == EndpointType.LOCAL) Color(0xFF166534) else Color(0xFF3730A3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Nt14Ui.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detail,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Nt14Ui.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { repository.reResolve() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Connection",
                        tint = statusCol,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onOpenHost != null) {
                    IconButton(
                        onClick = onOpenHost,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Host Settings",
                            tint = Nt14Ui.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

/**
 * InfoCard: replaces long paragraphs.
 * Rules: Title + max 3 lines of crisp text.
 */
@Composable
fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Nt14Ui.TileFill,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Nt14Ui.Outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = Nt14Ui.Accent, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Nt14Ui.TextPrimary
                    )
                }
                if (badge != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Nt14Ui.Outline)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Nt14Ui.Accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = body,
                fontSize = 12.sp,
                color = Nt14Ui.TextMuted,
                fontFamily = FontFamily.SansSerif,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }
    }
}

/**
 * Metrics 2-column Grid with 12dp gaps.
 */
@Composable
fun MetricsGrid(
    metrics: List<MetricTileData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val pairs = metrics.chunked(2)
        for (pair in pairs) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(pair[0], modifier = Modifier.weight(1f))
                if (pair.size > 1) {
                    MetricTile(pair[1], modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class MetricTileData(
    val title: String,
    val value: String,
    val subtext: String = "",
    val accentColor: Color = Nt14Ui.Accent,
    val icon: ImageVector? = null
)

@Composable
fun MetricTile(
    data: MetricTileData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 96.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Nt14Ui.Outline.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    color = Nt14Ui.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (data.icon != null) {
                    Icon(
                        data.icon,
                        contentDescription = null,
                        tint = data.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = data.value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Nt14Ui.TextPrimary,
                maxLines = 1
            )

            if (data.subtext.isNotBlank()) {
                Text(
                    text = data.subtext,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = data.accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
