package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
import com.example.util.ShareCardType
import com.example.util.SignalCardImageGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalCard(
    signal: TradingSignal,
    isNew: Boolean,
    isUpdating: Boolean,
    showAdminActions: Boolean,
    onUpdateStatus: (SignalStatus) -> Unit,
    onDismissNew: () -> Unit,
    themeKey: String = "OBSIDIAN_GOLD",
    livePrice: Double? = null,
    showLiveReferencePrice: Boolean = false,
    onSaveCard: (ShareCardType) -> Unit = {},
    onShareCard: () -> Unit = {},
    onCopyPrice: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val clipboard = LocalClipboardManager.current
    val side = signal.parsedSide
    val status = signal.parsedStatus
    val isBuy = side == SignalSide.BUY
    val sideAccent = if (isBuy) colors.emeraldBuy else colors.crimsonSell
    val sideContainer = if (isBuy) colors.emeraldContainer else colors.crimsonContainer

    val copyValue: (String, String) -> Unit = { label, formatted ->
        if (formatted != "—") {
            clipboard.setText(AnnotatedString(formatted))
            onCopyPrice(label, formatted)
        }
    }

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

                // Strategy, R:R, Lot, P&L, Timestamp (never breaks R:R mid-number)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                softWrap = false,
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
                            color = colors.textSecondary,
                            maxLines = 1,
                            softWrap = false
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
                                color = pnlColor,
                                maxLines = 1,
                                softWrap = false
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
                            color = colors.textMuted,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Spacious 2-Column Full-Precision Price Matrix (Tap any cell to copy)
                val entryFormatted = formatPrice(signal.entry, signal.symbol)
                val slFormatted = formatPrice(signal.sl, signal.symbol)
                val tp1Formatted = formatPrice(signal.tp1, signal.symbol)
                val tp2Formatted = formatPrice(signal.tp2, signal.symbol)
                val tp3Formatted = formatPrice(signal.tp3, signal.symbol)

                val slDelta = formatDistanceLabel(signal.entry, signal.sl, signal.symbol, prefix = "Risk ")
                val tp1Delta = formatDistanceLabel(signal.entry, signal.tp1, signal.symbol, prefix = "+")
                val tp2Delta = formatDistanceLabel(signal.entry, signal.tp2, signal.symbol, prefix = "+")
                val tp3Delta = formatDistanceLabel(signal.entry, signal.tp3, signal.symbol, prefix = "+")

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FullPrecisionPriceCell(
                                label = "ENTRY",
                                value = entryFormatted,
                                delta = if (signal.lotSize.isNotBlank()) "Lot: ${signal.lotSize}" else "Tap to copy",
                                valueColor = colors.textPrimary,
                                accentBg = colors.surface,
                                onClick = { copyValue("Entry", entryFormatted) },
                                modifier = Modifier.weight(1f)
                            )
                            FullPrecisionPriceCell(
                                label = "STOP LOSS",
                                value = slFormatted,
                                delta = slDelta,
                                valueColor = colors.crimsonSell,
                                accentBg = colors.crimsonContainer.copy(alpha = 0.25f),
                                onClick = { copyValue("SL", slFormatted) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FullPrecisionPriceCell(
                                label = "TP 1",
                                value = tp1Formatted,
                                delta = tp1Delta,
                                valueColor = colors.emeraldBuyDim,
                                accentBg = colors.emeraldContainer.copy(alpha = 0.22f),
                                onClick = { copyValue("TP1", tp1Formatted) },
                                modifier = Modifier.weight(1f)
                            )
                            FullPrecisionPriceCell(
                                label = "TP 2",
                                value = tp2Formatted,
                                delta = tp2Delta,
                                valueColor = colors.emeraldBuy,
                                accentBg = colors.emeraldContainer.copy(alpha = 0.32f),
                                onClick = { copyValue("TP2", tp2Formatted) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (signal.tp3 > 0.0 || signal.lotSize.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (signal.tp3 > 0.0) {
                                    FullPrecisionPriceCell(
                                        label = "TP 3 (EXT)",
                                        value = tp3Formatted,
                                        delta = tp3Delta,
                                        valueColor = colors.goldAccent,
                                        accentBg = colors.goldContainer.copy(alpha = 0.35f),
                                        onClick = { copyValue("TP3", tp3Formatted) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (signal.lotSize.isNotBlank()) {
                                    FullPrecisionPriceCell(
                                        label = "BOT LOT SIZE",
                                        value = signal.lotSize,
                                        delta = "R:R ${signal.rr}",
                                        valueColor = colors.cyanInfo,
                                        accentBg = colors.cyanContainer.copy(alpha = 0.25f),
                                        onClick = { copyValue("Lot Size", signal.lotSize) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLiveReferencePrice && status.isOpen && livePrice != null && livePrice > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.cyanContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, colors.cyanInfo.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(colors.emeraldBuy)
                                )
                                Text(
                                    text = "LIVE REFERENCE TRACKER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.cyanInfo
                                )
                            }
                            Text(
                                text = "MKT: ${formatPrice(livePrice, signal.symbol)}",
                                style = TabularMonoStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                        }
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

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-Generated Order / TP Result Card Image Strip (In-Memory Preview, Save only on button tap)
                val autoCardType = if (status == SignalStatus.ACTIVE) {
                    ShareCardType.ORDER_ENTRY
                } else {
                    ShareCardType.RESULT_HIT
                }
                val previewBitmap by produceState<Bitmap?>(
                    initialValue = null,
                    signal.id,
                    signal.status,
                    signal.pnlPercent,
                    autoCardType,
                    themeKey
                ) {
                    value = withContext(Dispatchers.Default) {
                        SignalCardImageGenerator.getOrRenderPreviewBitmap(
                            signal = signal,
                            cardType = autoCardType,
                            palette = colors,
                            themeKey = themeKey
                        )
                    }
                }
                val cardAccent = when {
                    autoCardType == ShareCardType.ORDER_ENTRY -> colors.goldAccent
                    status == SignalStatus.SL_HIT -> colors.crimsonSell
                    else -> colors.emeraldBuy
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, cardAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, cardAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(52.dp)
                                .height(65.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { onShareCard() }
                                .testTag("card_thumbnail_${signal.id}")
                        ) {
                            val bmp = previewBitmap
                            if (bmp != null && !bmp.isRecycled) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "${signal.symbol} auto card preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = cardAccent,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (autoCardType == ShareCardType.ORDER_ENTRY) {
                                    "Order Card Ready"
                                } else {
                                    "${status.label} Card Ready"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = cardAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Preview in memory · Tap Save PNG only when needed",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = colors.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveCard(autoCardType) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.goldContainer.copy(alpha = 0.25f),
                                        contentColor = colors.goldAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("btn_save_card_${signal.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download PNG card",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Save PNG",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = onShareCard,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.emeraldBuy,
                                        contentColor = colors.background
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("btn_share_card_${signal.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share card to group",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Share",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
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
private fun FullPrecisionPriceCell(
    label: String,
    value: String,
    delta: String,
    valueColor: Color,
    accentBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accentBg,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textMuted,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = colors.textMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(11.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TabularMonoStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
            if (delta.isNotBlank()) {
                Text(
                    text = delta,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDistanceLabel(entry: Double, target: Double, symbol: String, prefix: String): String {
    if (entry <= 0.0 || target <= 0.0) return ""
    val diff = abs(target - entry)
    val upper = symbol.uppercase(Locale.US)
    return when {
        upper.contains("JPY") -> String.format(Locale.US, "%s%.1f pips", prefix, diff / 0.01)
        upper.contains("XAU") || upper.contains("XAG") -> String.format(Locale.US, "%s%.2f pts", prefix, diff)
        upper.contains("BTC") || upper.contains("ETH") || upper.contains("BNB") || upper.contains("SOL") ->
            if (entry >= 100.0) String.format(Locale.US, "%s%.2f", prefix, diff)
            else String.format(Locale.US, "%s%.4f", prefix, diff)
        entry < 20.0 -> String.format(Locale.US, "%s%.1f pips", prefix, diff / 0.0001)
        else -> String.format(Locale.US, "%s%.2f pts", prefix, diff)
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
    if (timestampMillis <= 0L) return "Live"
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
