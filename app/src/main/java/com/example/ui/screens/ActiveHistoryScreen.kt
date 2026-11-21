package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.HistoryDayBucket
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.classifyDayBucket
import com.example.model.formatPnlPercent
import com.example.model.roundToTwoDecimals
import com.example.ui.components.SignalCard
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import com.example.viewmodel.HistoryRange
import com.example.viewmodel.TradingDeskUiState

@Composable
fun ActiveHistoryScreen(
    uiState: TradingDeskUiState,
    onSelectSegment: (Int) -> Unit,
    onSelectHistoryRange: (HistoryRange) -> Unit,
    onUpdateStatus: (TradingSignal, SignalStatus) -> Unit,
    onDismissNew: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors

    val openTrades = remember(uiState.signals) {
        uiState.signals.filter { it.parsedStatus.isOpen }
    }

    val closedTradesAll = remember(uiState.signals) {
        uiState.signals.filter { it.parsedStatus.isClosed }
    }

    val filteredClosedTrades = remember(closedTradesAll, uiState.historyRange) {
        val now = System.currentTimeMillis()
        when (uiState.historyRange) {
            HistoryRange.TODAY -> closedTradesAll.filter {
                classifyDayBucket(it.effectiveTimestampMillis(), now) == HistoryDayBucket.TODAY
            }
            HistoryRange.YESTERDAY -> closedTradesAll.filter {
                classifyDayBucket(it.effectiveTimestampMillis(), now) == HistoryDayBucket.YESTERDAY
            }
            HistoryRange.ALL_TIME -> closedTradesAll
        }
    }

    val displayedTrades = if (uiState.activeHistorySegment == 0) openTrades else filteredClosedTrades
    val showAdminCardActions = uiState.isAdminUnlocked || uiState.connectionMode == ConnectionMode.DEMO

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = uiState.activeHistorySegment == 0,
                onClick = { onSelectSegment(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.emeraldContainer,
                    activeContentColor = colors.emeraldBuy,
                    inactiveContainerColor = colors.surface,
                    inactiveContentColor = colors.textSecondary
                ),
                modifier = Modifier.testTag("segment_active_trades")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Active (${openTrades.size})")
                }
            }

            SegmentedButton(
                selected = uiState.activeHistorySegment == 1,
                onClick = { onSelectSegment(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.goldContainer,
                    activeContentColor = colors.goldAccent,
                    inactiveContainerColor = colors.surface,
                    inactiveContentColor = colors.textSecondary
                ),
                modifier = Modifier.testTag("segment_closed_history")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History (${closedTradesAll.size})")
                }
            }
        }

        if (uiState.activeHistorySegment == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryRange.entries.forEach { range ->
                    val selected = uiState.historyRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectHistoryRange(range) },
                        label = {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.goldContainer,
                            selectedLabelColor = colors.goldAccent,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.goldAccent.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.testTag("history_range_${range.name.lowercase()}")
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.activeHistorySegment == 1 && filteredClosedTrades.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HistorySummaryStrip(
                        rangeLabel = uiState.historyRange.label,
                        trades = filteredClosedTrades
                    )
                }
            }

            if (displayedTrades.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (uiState.activeHistorySegment == 0) {
                                    Icons.Rounded.PlayArrow
                                } else {
                                    Icons.Rounded.History
                                },
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.activeHistorySegment == 0) {
                                    "No open trades right now"
                                } else {
                                    "No closed trades for ${uiState.historyRange.label}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.activeHistorySegment == 0) {
                                    "Active and TP1_HIT positions appear here automatically."
                                } else {
                                    "Completed TP2, SL, and closed signals will be logged here."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayedTrades,
                    key = { it.id.ifBlank { "${it.symbol}_${it.timestamp}" } }
                ) { signal ->
                    SignalCard(
                        signal = signal,
                        isNew = signal.id in uiState.highlightedNewIds,
                        isUpdating = signal.id in uiState.updatingSignalIds,
                        showAdminActions = showAdminCardActions,
                        onUpdateStatus = { newStatus -> onUpdateStatus(signal, newStatus) },
                        onDismissNew = { onDismissNew(signal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryStrip(
    rangeLabel: String,
    trades: List<TradingSignal>
) {
    val colors = TerminalTheme.colors
    val wins = trades.count { it.parsedStatus.isWin }
    val losses = trades.count { it.parsedStatus.isLoss }
    val netPnl = trades.sumOf {
        if (it.pnlPercent != 0.0 || it.parsedStatus == SignalStatus.CLOSED) it.pnlPercent
        else it.calculatePnlForStatus(it.parsedStatus)
    }.roundToTwoDecimals()
    val pnlColor = if (netPnl >= 0.0) colors.emeraldBuy else colors.crimsonSell

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$rangeLabel Summary",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = "${trades.size} trades · ${wins}W / ${losses}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            Text(
                text = formatPnlPercent(netPnl),
                style = TabularMonoStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = pnlColor
            )
        }
    }
}
