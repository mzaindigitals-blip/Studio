package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.ui.components.SignalCard
import com.example.ui.theme.TerminalTheme
import com.example.viewmodel.SideFilter
import com.example.viewmodel.SignalsStreamState
import com.example.viewmodel.TradingDeskUiState

@Composable
fun LiveSignalsScreen(
    uiState: TradingDeskUiState,
    onSelectSideFilter: (SideFilter) -> Unit,
    onSelectSymbolFilter: (String) -> Unit,
    onUpdateStatus: (TradingSignal, SignalStatus) -> Unit,
    onDismissNew: (String) -> Unit,
    onClearLocalFeed: () -> Unit,
    onRestoreClearedFeed: () -> Unit,
    onOpenFirebaseSetup: () -> Unit,
    onLoadDemoMode: () -> Unit,
    onShareCard: (TradingSignal) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val defaultSymbols = remember { listOf("XAUUSD", "BTCUSDT", "BNBUSDT") }
    val availableSymbols = remember(uiState.visibleLiveSignals) {
        val dynamic = uiState.visibleLiveSignals.map { it.symbol.uppercase() }.distinct()
        listOf("ALL") + (defaultSymbols + dynamic).distinct()
    }

    val filteredSignals = remember(
        uiState.visibleLiveSignals,
        uiState.sideFilter,
        uiState.symbolFilter
    ) {
        uiState.visibleLiveSignals.filter { sig ->
            val sideMatches = when (uiState.sideFilter) {
                SideFilter.ALL -> true
                SideFilter.BUY -> sig.parsedSide == SignalSide.BUY
                SideFilter.SELL -> sig.parsedSide == SignalSide.SELL
            }
            val symbolMatches = uiState.symbolFilter == "ALL" ||
                sig.symbol.equals(uiState.symbolFilter, ignoreCase = true)
            sideMatches && symbolMatches
        }
    }

    val showAdminCardActions = uiState.isAdminUnlocked || uiState.connectionMode == ConnectionMode.DEMO

    Column(modifier = modifier.fillMaxSize()) {
        // Filter Bar + Clear / Restore Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideFilter.entries.forEach { sideOption ->
                    val selected = uiState.sideFilter == sideOption
                    val chipAccent = when (sideOption) {
                        SideFilter.ALL -> colors.goldAccent
                        SideFilter.BUY -> colors.emeraldBuy
                        SideFilter.SELL -> colors.crimsonSell
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSideFilter(sideOption) },
                        label = {
                            Text(
                                text = sideOption.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipAccent.copy(alpha = 0.16f),
                            selectedLabelColor = chipAccent,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.border,
                            selectedBorderColor = chipAccent.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("filter_side_${sideOption.name.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                availableSymbols.forEach { symbolOption ->
                    val selected = uiState.symbolFilter.equals(symbolOption, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSymbolFilter(symbolOption) },
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
                        modifier = Modifier.testTag("filter_symbol_${symbolOption.lowercase()}")
                    )
                }
            }

            if (uiState.visibleLiveSignals.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onClearLocalFeed,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("clear_live_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = "Clear signals screen",
                        tint = colors.goldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            } else if (uiState.clearedSignalsCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onRestoreClearedFeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("restore_cleared_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = "Restore cleared signals",
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore (${uiState.clearedSignalsCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.emeraldBuy
                    )
                }
            }
        }

        // Feed Content
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.connectionStatus != ConnectionStatus.FIREBASE_LIVE) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ConnectionBannerCard(
                        uiState = uiState,
                        onOpenFirebaseSetup = onOpenFirebaseSetup,
                        onLoadDemoMode = onLoadDemoMode
                    )
                }
            }

            when (val stream = uiState.signalsState) {
                is SignalsStreamState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = colors.emeraldBuy)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to Digital Vertex stream...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                is SignalsStreamState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ErrorEmptyStateCard(
                            title = "Firebase Stream Interrupted",
                            message = stream.message,
                            onOpenSetup = onOpenFirebaseSetup,
                            onLoadDemo = onLoadDemoMode
                        )
                    }
                }

                is SignalsStreamState.Success -> {
                    if (filteredSignals.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptySignalsCard(
                                hasActiveFilters = uiState.sideFilter != SideFilter.ALL || uiState.symbolFilter != "ALL",
                                clearedCount = uiState.clearedSignalsCount,
                                onClearFilters = {
                                    onSelectSideFilter(SideFilter.ALL)
                                    onSelectSymbolFilter("ALL")
                                },
                                onRestoreCleared = onRestoreClearedFeed
                            )
                        }
                    } else {
                        items(
                            items = filteredSignals,
                            key = { it.id.ifBlank { "${it.symbol}_${it.timestamp}" } }
                        ) { signal ->
                            SignalCard(
                                signal = signal,
                                isNew = signal.id in uiState.highlightedNewIds,
                                isUpdating = signal.id in uiState.updatingSignalIds,
                                showAdminActions = showAdminCardActions,
                                onUpdateStatus = { newStatus -> onUpdateStatus(signal, newStatus) },
                                onDismissNew = { onDismissNew(signal.id) },
                                onShareCard = { onShareCard(signal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBannerCard(
    uiState: TradingDeskUiState,
    onOpenFirebaseSetup: () -> Unit,
    onLoadDemoMode: () -> Unit
) {
    val colors = TerminalTheme.colors
    val isDisconnected = uiState.connectionStatus == ConnectionStatus.DISCONNECTED
    val accent = if (isDisconnected) colors.crimsonSell else colors.goldAccent
    val bg = if (isDisconnected) {
        colors.crimsonContainer.copy(alpha = 0.65f)
    } else {
        colors.goldContainer.copy(alpha = 0.55f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudQueue,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isDisconnected) {
                        uiState.statusError ?: "Disconnected from Firebase"
                    } else {
                        "Demo Mode active — tap Connect Firebase or paste Team Code"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenFirebaseSetup,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = colors.background
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("banner_connect_firebase")
                ) {
                    Text(
                        text = "Connect / Team Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDisconnected) {
                    OutlinedButton(
                        onClick = onLoadDemoMode,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Load Demo Mode", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySignalsCard(
    hasActiveFilters: Boolean,
    clearedCount: Int,
    onClearFilters: () -> Unit,
    onRestoreCleared: () -> Unit
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasActiveFilters) Icons.Rounded.FilterListOff else Icons.Rounded.Sensors,
                contentDescription = null,
                tint = colors.goldAccent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    hasActiveFilters -> "No signals match current filters"
                    clearedCount > 0 -> "Signal feed cleared — ready for new alerts"
                    else -> "Listening for laptop bot signals"
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    hasActiveFilters -> "Reset BUY/SELL or pair filters to view available setups."
                    clearedCount > 0 -> "New signals from your Python bot will appear here immediately with sound & notification alerts."
                    else -> "Signals pushed by Muhammad Zain's Python bot will stream here live in real time."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    border = BorderStroke(1.dp, colors.goldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent)
                ) {
                    Text("Reset Filters")
                }
            } else if (clearedCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onRestoreCleared,
                    border = BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldBuy)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore $clearedCount Cleared Signals")
                }
            }
        }
    }
}

@Composable
private fun ErrorEmptyStateCard(
    title: String,
    message: String,
    onOpenSetup: () -> Unit,
    onLoadDemo: () -> Unit
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.crimsonContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, colors.crimsonSell.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.crimsonSell
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenSetup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.crimsonSell,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text("Connection Setup")
                }
                OutlinedButton(
                    onClick = onLoadDemo,
                    border = BorderStroke(1.dp, colors.goldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent)
                ) {
                    Text("Load Demo Mode")
                }
            }
        }
    }
}
