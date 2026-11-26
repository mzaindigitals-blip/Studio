package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.TradeStats
import com.example.model.formatPnlPercent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TerminalHeaderAndStats(
    connectionStatus: ConnectionStatus,
    isSoundMuted: Boolean,
    backgroundAlertsEnabled: Boolean,
    isAdminUnlocked: Boolean,
    isLightMode: Boolean = false,
    stats: TradeStats,
    onOpenConnectionModal: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleBackgroundAlerts: () -> Unit,
    onToggleLightMode: () -> Unit = {},
    onTriggerOwnerUnlock: () -> Unit = {}
) {
    val colors = TerminalTheme.colors
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapMs by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.surface, colors.background)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Header Bar
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.emeraldContainer)
                        .border(1.dp, colors.emeraldBuy.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastLogoTapMs > 2_000L) {
                                logoTapCount = 1
                            } else {
                                logoTapCount += 1
                            }
                            lastLogoTapMs = now
                            if (logoTapCount >= 5) {
                                logoTapCount = 0
                                onTriggerOwnerUnlock()
                            }
                        }
                        .testTag("header_logo_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CandlestickChart,
                        contentDescription = "Digital Vertex logo",
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onTriggerOwnerUnlock
                    )
                ) {
                    Text(
                        text = "Digital Vertex",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("header_title_text")
                    )
                    if (isAdminUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.goldContainer,
                            border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = "Admin mode active",
                                    tint = colors.goldAccent,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "ADMIN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.goldAccent
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onToggleLightMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("light_dark_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isLightMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = if (isLightMode) "Switch to Dark Mode" else "Switch to Light Mode",
                        tint = colors.goldAccent
                    )
                }

                IconButton(
                    onClick = onToggleBackgroundAlerts,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("background_alerts_toggle_button")
                ) {
                    Icon(
                        imageVector = if (backgroundAlertsEnabled) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.NotificationsOff
                        },
                        contentDescription = if (backgroundAlertsEnabled) {
                            "Disable background push alerts"
                        } else {
                            "Enable background push alerts"
                        },
                        tint = if (backgroundAlertsEnabled) colors.goldAccent else colors.textMuted
                    )
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSoundMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = if (isSoundMuted) "Unmute signal alerts" else "Mute signal alerts",
                        tint = if (isSoundMuted) colors.textMuted else colors.emeraldBuy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusBadgePill(
                status = connectionStatus,
                onClick = onOpenConnectionModal
            )
            Text(
                text = "By Muhammad Zain",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textMuted,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Performance Bar
        PerformanceMetricsBar(stats = stats)
    }
}

@Composable
fun StatusBadgePill(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    val colors = TerminalTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (dotColor, containerColor, textColor) = when (status) {
        ConnectionStatus.FIREBASE_LIVE -> Triple(colors.emeraldBuy, colors.emeraldContainer, colors.emeraldBuy)
        ConnectionStatus.DEMO_MODE -> Triple(colors.goldAccent, colors.goldContainer, colors.goldAccent)
        ConnectionStatus.CONNECTING -> Triple(colors.cyanInfo, colors.surfaceElevated, colors.cyanInfo)
        ConnectionStatus.DISCONNECTED -> Triple(colors.crimsonSell, colors.crimsonContainer, colors.crimsonSell)
    }

    val animatedBg by animateColorAsState(targetValue = containerColor, label = "status_bg")

    Surface(
        shape = RoundedCornerShape(50),
        color = animatedBg,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag("top_status_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(
                        if (status == ConnectionStatus.FIREBASE_LIVE || status == ConnectionStatus.CONNECTING) {
                            pulseAlpha
                        } else {
                            1f
                        }
                    )
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = textColor
            )
            if (status == ConnectionStatus.FIREBASE_LIVE) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun PerformanceMetricsBar(stats: TradeStats) {
    val colors = TerminalTheme.colors
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 600.dp
        val todayPnlColor = if (stats.todayNetPnl >= 0.0) colors.emeraldBuy else colors.crimsonSell
        val yesterdayPnlColor = if (stats.yesterdayNetPnl >= 0.0) colors.emeraldBuy else colors.crimsonSell
        val winRateColor = when {
            stats.winRate >= 60.0 -> colors.emeraldBuy
            stats.winRate >= 45.0 -> colors.goldAccent
            stats.todayWins + stats.todayLosses + stats.yesterdayWins + stats.yesterdayLosses == 0L -> colors.textSecondary
            else -> colors.crimsonSell
        }

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricCard(
                    title = "TODAY",
                    primaryValue = formatPnlPercent(stats.todayNetPnl),
                    primaryColor = todayPnlColor,
                    subtitle = "${stats.todayWins}W / ${stats.todayLosses}L",
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "YESTERDAY",
                    primaryValue = formatPnlPercent(stats.yesterdayNetPnl),
                    primaryColor = yesterdayPnlColor,
                    subtitle = "${stats.yesterdayWins}W / ${stats.yesterdayLosses}L",
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "WIN RATE",
                    primaryValue = String.format(Locale.US, "%.1f%%", stats.winRate),
                    primaryColor = winRateColor,
                    subtitle = "${stats.todayWins + stats.yesterdayWins} wins total",
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "ACTIVE",
                    primaryValue = stats.activeCount.toString(),
                    primaryColor = colors.goldAccent,
                    subtitle = "Open positions",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMetricCard(
                        title = "TODAY",
                        primaryValue = formatPnlPercent(stats.todayNetPnl),
                        primaryColor = todayPnlColor,
                        subtitle = "${stats.todayWins}W / ${stats.todayLosses}L",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "YESTERDAY",
                        primaryValue = formatPnlPercent(stats.yesterdayNetPnl),
                        primaryColor = yesterdayPnlColor,
                        subtitle = "${stats.yesterdayWins}W / ${stats.yesterdayLosses}L",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMetricCard(
                        title = "WIN RATE",
                        primaryValue = String.format(Locale.US, "%.1f%%", stats.winRate),
                        primaryColor = winRateColor,
                        subtitle = "${stats.todayWins + stats.yesterdayWins} wins",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "ACTIVE",
                        primaryValue = stats.activeCount.toString(),
                        primaryColor = colors.goldAccent,
                        subtitle = "Open desk",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    primaryValue: String,
    primaryColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = primaryValue,
                style = TabularMonoStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = primaryColor,
                maxLines = 1
            )
        }
    }
}
