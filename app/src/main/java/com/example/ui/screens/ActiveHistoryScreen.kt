package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.MemberAccessStatus
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.classifyDayBucket
import com.example.model.formatPnlPercent
import com.example.model.roundToTwoDecimals
import com.example.ui.components.MemberAccessGateCard
import com.example.ui.components.SignalCard
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import com.example.util.ShareCardType
import com.example.viewmodel.HistoryRange
import com.example.viewmodel.TradingDeskUiState
import java.util.Locale

@Composable
fun ActiveHistoryScreen(
    uiState: TradingDeskUiState,
    onSelectSegment: (Int) -> Unit,
    onSelectHistoryRange: (HistoryRange) -> Unit,
    onSelectHistorySymbolFilter: (String) -> Unit = {},
    onUpdateStatus: (TradingSignal, SignalStatus) -> Unit,
    onDismissNew: (String) -> Unit,
    onClearLocalHistoryFeed: () -> Unit = {},
    onRestoreClearedHistoryFeed: () -> Unit = {},
    onRefreshTeamSync: () -> Unit = {},
    onSaveCard: (TradingSignal, ShareCardType) -> Unit = { _, _ -> },
    onShareCard: (TradingSignal) -> Unit = {},
    onCopyPrice: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors

    val availableSymbols = remember(uiState.visibleHistorySignals) {
        val dynamic = uiState.visibleHistorySignals
            .map { it.symbol.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("ALL") + dynamic
    }

    val symbolScopedSignals = remember(uiState.visibleHistorySignals, uiState.historySymbolFilter) {
        if (uiState.historySymbolFilter == "ALL") {
            uiState.visibleHistorySignals
        } else {
            uiState.visibleHistorySignals.filter {
                it.symbol.equals(uiState.historySymbolFilter, ignoreCase = true)
            }
        }
    }

    val openTrades = remember(symbolScopedSignals) {
        symbolScopedSignals.filter { it.parsedStatus.isOpen }
    }

    val closedTradesAll = remember(symbolScopedSignals) {
        symbolScopedSignals.filter { it.parsedStatus.isClosed }
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
    val showAdminCardActions = uiState.showManualStatusButtons &&
        (uiState.isAdminUnlocked || uiState.connectionMode == ConnectionMode.DEMO)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
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
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Active (${openTrades.size})", maxLines = 1)
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
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("History (${closedTradesAll.size})", maxLines = 1)
                    }
                }
            }

            if (uiState.visibleHistorySignals.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearLocalHistoryFeed,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("clear_history_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = "Clear Active & History",
                        tint = colors.goldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            } else if (uiState.clearedHistoryCount > 0) {
                TextButton(
                    onClick = onRestoreClearedHistoryFeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("restore_cleared_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = "Restore cleared Active & History",
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore (${uiState.clearedHistoryCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.emeraldBuy
                    )
                }
            }
        }

        // Date Range + Pair Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.activeHistorySegment == 1) {
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

            if (availableSymbols.size > 1) {
                availableSymbols.forEach { symbolOption ->
                    val selected = uiState.historySymbolFilter.equals(symbolOption, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectHistorySymbolFilter(symbolOption) },
                        label = {
                            Text(
                                text = if (symbolOption == "ALL") "ALL PAIRS" else symbolOption,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.emeraldContainer,
                            selectedLabelColor = colors.emeraldBuy,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.emeraldBuy.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.testTag("history_symbol_${symbolOption.lowercase()}")
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
            if (uiState.memberAccessStatus != MemberAccessStatus.ACTIVE && !uiState.isAdminUnlocked) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MemberAccessGateCard(
                        accessStatus = uiState.memberAccessStatus,
                        memberName = uiState.memberName,
                        deviceId = uiState.deviceId,
                        onRefreshSync = onRefreshTeamSync
                    )
                }
            } else {
                if (uiState.activeHistorySegment == 1 && filteredClosedTrades.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HistorySummaryStrip(
                            rangeLabel = uiState.historyRange.label,
                            symbolLabel = uiState.historySymbolFilter,
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
                                    text = when {
                                        uiState.clearedHistoryCount > 0 && uiState.visibleHistorySignals.isEmpty() ->
                                            "Active & History cleared locally"
                                        uiState.historySymbolFilter != "ALL" ->
                                            "No ${uiState.historySymbolFilter} trades in this view"
                                        uiState.activeHistorySegment == 0 ->
                                            "No open trades right now"
                                        else ->
                                            "No closed trades for ${uiState.historyRange.label}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        uiState.clearedHistoryCount > 0 && uiState.visibleHistorySignals.isEmpty() ->
                                            "New trades and TP/SL updates will appear here automatically."
                                        uiState.historySymbolFilter != "ALL" ->
                                            "Switch pair filter back to ALL PAIRS or pick All Time."
                                        uiState.activeHistorySegment == 0 ->
                                            "Active and TP1_HIT positions appear here automatically."
                                        else ->
                                            "Completed TP2, SL, and closed signals will be logged here."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.clearedHistoryCount > 0) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = onRestoreClearedHistoryFeed,
                                        border = BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldBuy)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore ${uiState.clearedHistoryCount} Cleared Trades")
                                    }
                                }
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
                            onDismissNew = { onDismissNew(signal.id) },
                            themeKey = uiState.themeId.name,
                            livePrice = uiState.livePrices[signal.symbol.uppercase(Locale.US)],
                            showLiveReferencePrice = uiState.autoTrackTpSlEnabled,
                            onSaveCard = { cardType -> onSaveCard(signal, cardType) },
                            onShareCard = { onShareCard(signal) },
                            onCopyPrice = onCopyPrice
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryStrip(
    rangeLabel: String,
    symbolLabel: String,
    trades: List<TradingSignal>
) {
    val colors = TerminalTheme.colors
    val wins = trades.count { it.parsedStatus.isWin }
    val losses = trades.count { it.parsedStatus.isLoss }
    val decided = wins + losses
    val winRate = if (decided > 0) ((wins * 100.0) / decided).roundToTwoDecimals() else 0.0
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
                    text = if (symbolLabel == "ALL") "$rangeLabel Summary" else "$rangeLabel · $symbolLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = String.format(Locale.US, "%d trades · %dW / %dL · %.0f%% WR", trades.size, wins, losses, winRate),
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
