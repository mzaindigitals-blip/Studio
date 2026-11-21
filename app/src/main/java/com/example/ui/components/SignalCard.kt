package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.formatPnlPercent
import com.example.model.formatPrice
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignalCard(
    signal: TradingSignal,
    isNew: Boolean,
    isUpdating: Boolean,
    showAdminActions: Boolean,
    onUpdateStatus: (SignalStatus) -> Unit,
    onDismissNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val side = signal.parsedSide
    val status = signal.parsedStatus
    val isBuy = side == SignalSide.BUY
    val sideAccent = if (isBuy) colors.emeraldBuy else colors.crimsonSell
    val sideContainer = if (isBuy) colors.emeraldContainer else colors.crimsonContainer

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isNew -> colors.goldAccent
            status == SignalStatus.ACTIVE -> sideAccent.copy(alpha = 0.35f)
            else -> colors.border
        },
        label = "card_border"
    )

    val (statusTextColor, statusBgColor) = when (status) {
        SignalStatus.ACTIVE -> colors.cyanInfo to colors.cyanContainer
        SignalStatus.TP1_HIT -> colors.emeraldBuy to colors.emeraldContainer
        SignalStatus.TP2_HIT -> colors.emeraldBuy to colors.emeraldContainer
        SignalStatus.SL_HIT -> colors.crimsonSell to colors.crimsonContainer
        SignalStatus.CLOSED -> colors.textSecondary to colors.surfaceHighlight
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(if (isNew) 1.5.dp else 1.dp, cardBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (isNew) onDismissNew()
            }
            .testTag("signal_card_${signal.id}")
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(sideAccent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sideContainer,
                            border = BorderStroke(1.dp, sideAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBuy) {
                                        Icons.AutoMirrored.Rounded.TrendingUp
                                    } else {
                                        Icons.AutoMirrored.Rounded.TrendingDown
                                    },
                                    contentDescription = side.name,
                                    tint = sideAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = side.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = sideAccent
                                )
                            }
                        }

                        Text(
                            text = signal.symbol,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = signal.timeframe,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        AnimatedVisibility(visible = isNew) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.goldContainer,
                                border = BorderStroke(1.dp, colors.goldAccent)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "New signal",
                                        tint = colors.goldAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "NEW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.goldAccent
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBgColor,
                        border = BorderStroke(1.dp, statusTextColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strategy, R:R, P&L, Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.goldContainer
                        ) {
                            Text(
                                text = signal.strategy,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.goldAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "R:R ${signal.rr}",
                            style = TabularMonoStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textSecondary
                        )

                        if (status != SignalStatus.ACTIVE) {
                            val effectivePnl = if (signal.pnlPercent != 0.0 || status == SignalStatus.CLOSED) {
                                signal.pnlPercent
                            } else {
                                signal.calculatePnlForStatus(status)
                            }
                            val pnlColor = when {
                                effectivePnl > 0.0 -> colors.emeraldBuy
                                effectivePnl < 0.0 -> colors.crimsonSell
                                else -> colors.textSecondary
                            }
                            Text(
                                text = formatPnlPercent(effectivePnl),
                                style = TabularMonoStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = pnlColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatRelativeAndClockTime(signal.effectiveTimestampMillis()),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Matrix (Entry, SL, TP1, TP2)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PriceColumn(
                            label = "ENTRY",
                            value = formatPrice(signal.entry, signal.symbol),
                            valueColor = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        PriceColumn(
                            label = "STOP LOSS",
                            value = formatPrice(signal.sl, signal.symbol),
                            valueColor = colors.crimsonSell,
                            modifier = Modifier.weight(1f)
                        )
                        PriceColumn(
                            label = "TP 1",
                            value = formatPrice(signal.tp1, signal.symbol),
                            valueColor = colors.emeraldBuyDim,
                            modifier = Modifier.weight(1f)
                        )
                        PriceColumn(
                            label = "TP 2",
                            value = formatPrice(signal.tp2, signal.symbol),
                            valueColor = colors.emeraldBuy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!signal.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = signal.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Actions visible ONLY in Admin Mode (or Demo Mode) for Open Trades
                if (showAdminActions && status.isOpen) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isUpdating) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.emeraldBuy,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Syncing update...",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickStatusButton(
                                label = "TP1 Hit",
                                accent = colors.emeraldBuyDim,
                                enabled = status == SignalStatus.ACTIVE,
                                onClick = { onUpdateStatus(SignalStatus.TP1_HIT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_tp1_${signal.id}")
                            )
                            QuickStatusButton(
                                label = "TP2 Hit",
                                accent = colors.emeraldBuy,
                                enabled = true,
                                onClick = { onUpdateStatus(SignalStatus.TP2_HIT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_tp2_${signal.id}")
                            )
                            QuickStatusButton(
                                label = "SL Hit",
                                accent = colors.crimsonSell,
                                enabled = true,
                                onClick = { onUpdateStatus(SignalStatus.SL_HIT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sl_${signal.id}")
                            )
                            QuickStatusButton(
                                label = "Close",
                                accent = colors.textSecondary,
                                enabled = true,
                                onClick = { onUpdateStatus(SignalStatus.CLOSED) },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("btn_close_${signal.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = colors.textMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularMonoStyle.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickStatusButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accent.copy(alpha = 0.45f) else colors.borderSubtle
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) accent.copy(alpha = 0.10f) else Color.Transparent,
            contentColor = accent,
            disabledContentColor = colors.textMuted
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            maxLines = 1
        )
    }
}

fun formatRelativeAndClockTime(timestampMillis: Long): String {
    val diff = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val mins = diff / 60_000L
    val hours = diff / 3_600_000L
    val rel = when {
        mins < 1L -> "just now"
        mins < 60L -> "${mins}m ago"
        hours < 24L -> "${hours}h ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(timestampMillis))
    }
    val clock = SimpleDateFormat("HH:mm", Locale.US).format(Date(timestampMillis))
    return "$rel · $clock"
}
