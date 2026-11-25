package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MemberAccessStatus
import com.example.model.computeActiveTradingSession
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import java.util.Locale
import kotlin.math.abs

enum class CalculatorInstrument(val label: String) {
    XAUUSD("XAUUSD Gold"),
    FOREX_MAJOR("Forex Major"),
    FOREX_JPY("Forex JPY"),
    CRYPTO("Crypto (USDT)")
}

data class RiskCalculationResult(
    val riskAmountUsd: Double,
    val slDistance: Double,
    val distanceLabel: String,
    val primarySizeLabel: String,
    val primarySizeTitle: String,
    val secondaryInfo: String
)

fun calculateRiskAndLotSize(
    accountBalance: Double,
    riskPercent: Double,
    entry: Double,
    sl: Double,
    instrument: CalculatorInstrument
): RiskCalculationResult? {
    if (accountBalance <= 0.0 || riskPercent <= 0.0 || entry <= 0.0 || sl <= 0.0) return null
    val dist = abs(entry - sl)
    if (dist <= 0.0) return null

    val riskUsd = accountBalance * (riskPercent / 100.0)
    return when (instrument) {
        CalculatorInstrument.XAUUSD -> {
            val lots = riskUsd / (dist * 100.0)
            RiskCalculationResult(
                riskAmountUsd = riskUsd,
                slDistance = dist,
                distanceLabel = String.format(Locale.US, "%.2f pts ($%.2f/pt)", dist, lots * 100.0),
                primarySizeTitle = "RECOMMENDED LOT",
                primarySizeLabel = String.format(Locale.US, "%.2f lots", lots.coerceAtLeast(0.01)),
                secondaryInfo = String.format(Locale.US, "Raw: %.3f lots · Micro: %.1f", lots, lots * 100.0)
            )
        }
        CalculatorInstrument.FOREX_MAJOR -> {
            val pips = dist / 0.0001
            val lots = riskUsd / (dist * 100_000.0)
            RiskCalculationResult(
                riskAmountUsd = riskUsd,
                slDistance = dist,
                distanceLabel = String.format(Locale.US, "%.1f pips", pips),
                primarySizeTitle = "RECOMMENDED LOT",
                primarySizeLabel = String.format(Locale.US, "%.2f lots", lots.coerceAtLeast(0.01)),
                secondaryInfo = String.format(Locale.US, "Raw: %.3f lots · %.0f units", lots, lots * 100_000.0)
            )
        }
        CalculatorInstrument.FOREX_JPY -> {
            val pips = dist / 0.01
            val usdPerPriceUnitPerLot = 100_000.0 / entry
            val lots = riskUsd / (dist * usdPerPriceUnitPerLot)
            RiskCalculationResult(
                riskAmountUsd = riskUsd,
                slDistance = dist,
                distanceLabel = String.format(Locale.US, "%.1f pips", pips),
                primarySizeTitle = "RECOMMENDED LOT",
                primarySizeLabel = String.format(Locale.US, "%.2f lots", lots.coerceAtLeast(0.01)),
                secondaryInfo = String.format(Locale.US, "Raw: %.3f lots · %.0f units", lots, lots * 100_000.0)
            )
        }
        CalculatorInstrument.CRYPTO -> {
            val units = riskUsd / dist
            val notional = units * entry
            RiskCalculationResult(
                riskAmountUsd = riskUsd,
                slDistance = dist,
                distanceLabel = String.format(Locale.US, "$%.2f move", dist),
                primarySizeTitle = "POSITION UNITS",
                primarySizeLabel = if (units >= 1.0) {
                    String.format(Locale.US, "%.3f units", units)
                } else {
                    String.format(Locale.US, "%.5f units", units)
                },
                secondaryInfo = String.format(Locale.US, "Notional value: $%.2f", notional)
            )
        }
    }
}

@Composable
fun TradingSessionStrip(
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val session = remember { computeActiveTradingSession() }
    val accent = if (session.isHighLiquidityOverlap) colors.emeraldBuy else colors.goldAccent

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = session.activeSessionsLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
            }
            Text(
                text = if (session.isHighLiquidityOverlap) "HIGH VOLUME" else "MARKET CLOCK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

@Composable
fun AnnouncementBannerCard(
    announcement: String,
    modifier: Modifier = Modifier
) {
    if (announcement.isBlank()) return
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.goldContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = "Admin announcement",
                tint = colors.goldAccent,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DESK NOTICE · MUHAMMAD ZAIN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = colors.goldAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = announcement,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
fun MemberAccessGateCard(
    accessStatus: MemberAccessStatus,
    memberName: String,
    deviceId: String,
    onRefreshSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val (icon, accent, title, description) = when (accessStatus) {
        MemberAccessStatus.PENDING -> Quadruple(
            Icons.Rounded.HourglassTop,
            colors.goldAccent,
            "Waiting for Admin Approval",
            "Your join request ($memberName · $deviceId) is pending Muhammad Zain's approval. Once approved in Admin Team Roster, live signals will unlock automatically."
        )
        MemberAccessStatus.PAUSED -> Quadruple(
            Icons.Rounded.PauseCircle,
            colors.goldAccent,
            "Signal Access Paused",
            "Live signal notifications and stream have been temporarily paused for your device ($deviceId) by Digital Vertex Admin."
        )
        MemberAccessStatus.BLOCKED -> Quadruple(
            Icons.Rounded.Block,
            colors.crimsonSell,
            "Access Restricted",
            "This device ($deviceId) has been blocked from receiving signals. Contact Muhammad Zain if this was unintentional."
        )
        MemberAccessStatus.ACTIVE -> return
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_access_gate_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRefreshSync,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = colors.background
                ),
                modifier = Modifier.testTag("gate_refresh_sync_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check Access Status", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MutedPairsManagerCard(
    knownSymbols: List<String>,
    mutedPairs: Set<String>,
    onToggleMutePair: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val allPairs = remember(knownSymbols) {
        (listOf("XAUUSD", "BTCUSDT", "BNBUSDT", "ETHUSDT", "EURUSD", "GBPUSD", "USDJPY") +
            knownSymbols.map { it.trim().uppercase(Locale.US) })
            .filter { it.isNotBlank() }
            .distinct()
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (mutedPairs.isEmpty()) {
                        Icons.Rounded.NotificationsActive
                    } else {
                        Icons.Rounded.NotificationsOff
                    },
                    contentDescription = null,
                    tint = colors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Pair Notification Filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tap any pair to mute/unmute sound & push notifications for that symbol on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allPairs.forEach { pair ->
                    val isMuted = pair in mutedPairs
                    FilterChip(
                        selected = !isMuted,
                        onClick = { onToggleMutePair(pair) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isMuted) {
                                    Icons.Rounded.NotificationsOff
                                } else {
                                    Icons.Rounded.NotificationsActive
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (isMuted) "$pair (Muted)" else pair,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.emeraldContainer,
                            selectedLabelColor = colors.emeraldBuy,
                            selectedLeadingIconColor = colors.emeraldBuy,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textMuted,
                            iconColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = !isMuted,
                            borderColor = colors.border,
                            selectedBorderColor = colors.emeraldBuy.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("mute_pair_chip_${pair.lowercase(Locale.US)}")
                    )
                }
            }
        }
    }
}

@Composable
fun LotSizeCalculatorCard(
    initialEntry: String = "2650.00",
    initialSl: String = "2642.00",
    initialInstrument: CalculatorInstrument = CalculatorInstrument.XAUUSD,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    var balanceText by remember { mutableStateOf("500") }
    var riskPercentText by remember { mutableStateOf("1.0") }
    var entryText by remember(initialEntry) { mutableStateOf(initialEntry) }
    var slText by remember(initialSl) { mutableStateOf(initialSl) }
    var instrument by remember(initialInstrument) { mutableStateOf(initialInstrument) }

    val balance = balanceText.toDoubleOrNull() ?: 0.0
    val riskPct = riskPercentText.toDoubleOrNull() ?: 0.0
    val entry = entryText.toDoubleOrNull() ?: 0.0
    val sl = slText.toDoubleOrNull() ?: 0.0
    val calcResult = remember(balance, riskPct, entry, sl, instrument) {
        calculateRiskAndLotSize(balance, riskPct, entry, sl, instrument)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("lot_size_calculator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = colors.emeraldBuy,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Lot Size & Risk Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Protect your capital — calculate exact lot size before entering any signal",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Instrument selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalculatorInstrument.entries.forEach { inst ->
                    val selected = inst == instrument
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) colors.emeraldContainer else colors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.emeraldBuy.copy(alpha = 0.6f) else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                instrument = inst
                                when (inst) {
                                    CalculatorInstrument.XAUUSD -> {
                                        entryText = "2650.00"
                                        slText = "2642.00"
                                    }
                                    CalculatorInstrument.FOREX_MAJOR -> {
                                        entryText = "1.08500"
                                        slText = "1.08250"
                                    }
                                    CalculatorInstrument.FOREX_JPY -> {
                                        entryText = "154.500"
                                        slText = "154.000"
                                    }
                                    CalculatorInstrument.CRYPTO -> {
                                        entryText = "95000.00"
                                        slText = "93800.00"
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = when (inst) {
                                CalculatorInstrument.XAUUSD -> "GOLD"
                                CalculatorInstrument.FOREX_MAJOR -> "FX"
                                CalculatorInstrument.FOREX_JPY -> "JPY"
                                CalculatorInstrument.CRYPTO -> "CRYPTO"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) colors.emeraldBuy else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorNumberField(
                    label = "Account ($)",
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    modifier = Modifier.weight(1f)
                )
                CalculatorNumberField(
                    label = "Risk (%)",
                    value = riskPercentText,
                    onValueChange = { riskPercentText = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("0.5", "1.0", "2.0", "5.0").forEach { preset ->
                    val active = riskPercentText == preset
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) colors.goldContainer else colors.surfaceElevated,
                        border = BorderStroke(1.dp, if (active) colors.goldAccent else colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { riskPercentText = preset }
                    ) {
                        Text(
                            text = "$preset% Risk",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (active) colors.goldAccent else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorNumberField(
                    label = "Entry Price",
                    value = entryText,
                    onValueChange = { entryText = it },
                    modifier = Modifier.weight(1f)
                )
                CalculatorNumberField(
                    label = "Stop Loss",
                    value = slText,
                    onValueChange = { slText = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (calcResult != null) colors.emeraldBuy.copy(alpha = 0.45f) else colors.borderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (calcResult != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = calcResult.primarySizeTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Text(
                                text = calcResult.primarySizeLabel,
                                style = TabularMonoStyle.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.emeraldBuy
                            )
                            Text(
                                text = calcResult.secondaryInfo,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "MAX RISK",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", calcResult.riskAmountUsd),
                                style = TabularMonoStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.crimsonSell
                            )
                            Text(
                                text = calcResult.distanceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.goldAccent
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Enter valid Balance, Risk %, Entry, and Stop Loss prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotSizeCalculatorBottomSheet(
    onDismiss: () -> Unit,
    initialEntry: String = "2650.00",
    initialSl: String = "2642.00",
    initialInstrument: CalculatorInstrument = CalculatorInstrument.XAUUSD
) {
    val colors = TerminalTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Risk & Lot Calculator",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close calculator",
                        tint = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LotSizeCalculatorCard(
                initialEntry = initialEntry,
                initialSl = initialSl,
                initialInstrument = initialInstrument
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalculatorNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.emeraldBuy,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.emeraldBuy,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.emeraldBuy,
            focusedContainerColor = colors.surfaceElevated,
            unfocusedContainerColor = colors.surfaceElevated
        ),
        modifier = modifier
    )
}
