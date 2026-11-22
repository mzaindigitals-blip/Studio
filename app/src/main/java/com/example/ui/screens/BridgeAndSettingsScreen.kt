package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionMode
import com.example.ui.components.AdminDeckCard
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.DeskThemeId
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TerminalTheme
import com.example.ui.theme.paletteForTheme
import com.example.viewmodel.TradingDeskUiState

private const val EXPECTED_SIGNAL_JSON = """{
  "id": "sig_20261120_xau_01",
  "symbol": "XAUUSD",
  "timeframe": "M15",
  "side": "BUY",
  "strategy": "3-Module PA",
  "entry": 2642.50,
  "sl": 2635.00,
  "tp1": 2652.50,
  "tp2": 2662.50,
  "rr": "1:2.6",
  "status": "ACTIVE",
  "timestamp": 1763673400000
}"""

private fun buildPythonBridgeSnippet(databaseUrl: String): String {
    val effectiveUrl = databaseUrl.ifBlank { "https://YOUR-PROJECT-default-rtdb.firebaseio.com" }
    return """import time, requests

# Digital Vertex Free RTDB Endpoint (Muhammad Zain):
DATABASE_URL = "$effectiveUrl"

def push_signal(symbol, timeframe, side, strategy, entry, sl, tp1, tp2, rr):
    payload = {
        "symbol": symbol,
        "timeframe": timeframe,
        "side": side,          # "BUY" or "SELL"
        "strategy": strategy,  # e.g. "3-Module PA", "RSI+EMA"
        "entry": float(entry),
        "sl": float(sl),
        "tp1": float(tp1),
        "tp2": float(tp2),
        "rr": rr,
        "status": "ACTIVE",
        "timestamp": int(time.time() * 1000)
    }
    res = requests.post(f"{DATABASE_URL}/signals.json", json=payload, timeout=10)
    res.raise_for_status()
    return res.json()["name"]

def update_signal_status(signal_key, new_status, pnl_percent=None):
    # new_status: "TP1_HIT", "TP2_HIT", "SL_HIT", "CLOSED"
    patch_data = {"status": new_status}
    if pnl_percent is not None:
        patch_data["pnlPercent"] = float(pnl_percent)
    res = requests.patch(f"{DATABASE_URL}/signals/{signal_key}.json", json=patch_data, timeout=10)
    res.raise_for_status()"""
}

@Composable
fun BridgeAndSettingsScreen(
    uiState: TradingDeskUiState,
    onOpenFirebaseSetup: () -> Unit,
    onLoadDemoMode: () -> Unit,
    onTestSound: () -> Unit,
    onToggleBackgroundAlerts: () -> Unit,
    onRestoreClearedFeed: () -> Unit,
    onSelectTheme: (DeskThemeId) -> Unit,
    onUnlockAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onSendAdminDiagnostic: () -> Unit,
    onClearAllFirebaseSignals: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        // Hero Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .height(148.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_terminal_banner_1795238681215),
                        contentDescription = "Digital Vertex Trading Desk Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.background.copy(alpha = 0.35f),
                                        colors.background.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        StatusBadgePill(
                            status = uiState.connectionStatus,
                            onClick = onOpenFirebaseSetup
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Digital Vertex · Desk & Admin",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Owner: Muhammad Zain · 100% Free Live Bot Receiver",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // Theme Picker Card (Client & Admin)
        item {
            ThemeSelectorCard(
                selectedTheme = uiState.themeId,
                onSelectTheme = onSelectTheme
            )
        }

        // Client Alerts, Connection & Feed Controls
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsEthernet,
                            contentDescription = null,
                            tint = colors.emeraldBuy,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Connection & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val modeDescription = when (uiState.connectionMode) {
                        ConnectionMode.DEMO -> "Active Source: Demo Mode (sample signals)"
                        ConnectionMode.BUILT_IN_FIRESTORE -> {
                            if (uiState.authenticatedUserId != null) {
                                "Active Source: Cloud Firestore Sync (Signed in)"
                            } else {
                                "Active Source: Cloud Firestore Sync (Sign-in required)"
                            }
                        }
                        ConnectionMode.CUSTOM_RTDB -> {
                            if (uiState.isAdminUnlocked) {
                                "Active Source: RTDB (${uiState.customConfig.databaseURL})"
                            } else {
                                "Active Source: Digital Vertex Live Stream (Connected)"
                            }
                        }
                        ConnectionMode.CUSTOM_FIRESTORE -> "Active Source: Custom Firestore (${uiState.customConfig.projectId})"
                    }
                    Text(
                        text = modeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Free Background Push Notifications Switch
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (uiState.backgroundAlertsEnabled) colors.goldAccent else colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Background Push Alerts (Free)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Receive status-bar alerts & sound even when app is minimized",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = uiState.backgroundAlertsEnabled,
                                onCheckedChange = { onToggleBackgroundAlerts() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.background,
                                    checkedTrackColor = colors.emeraldBuy,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surface
                                ),
                                modifier = Modifier.testTag("settings_background_alerts_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenFirebaseSetup,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emeraldBuy,
                                contentColor = colors.background
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_firebase_modal")
                        ) {
                            Text(
                                text = if (uiState.isAdminUnlocked) "Firebase Setup" else "Team Code / Connect",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onLoadDemoMode,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_load_demo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Demo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestSound,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_test_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = colors.goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Sound")
                        }

                        if (uiState.clearedSignalsCount > 0) {
                            OutlinedButton(
                                onClick = onRestoreClearedFeed,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldBuy),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore (${uiState.clearedSignalsCount})")
                            }
                        }
                    }
                }
            }
        }

        // PIN-Gated Admin Deck
        item {
            AdminDeckCard(
                uiState = uiState,
                onUnlockAdmin = onUnlockAdmin,
                onLockAdmin = onLockAdmin,
                onUpdateAdminPin = onUpdateAdminPin,
                onOpenFirebaseSetup = onOpenFirebaseSetup,
                onSendAdminDiagnostic = onSendAdminDiagnostic,
                onClearAllFirebaseSignals = onClearAllFirebaseSignals,
                onShowMessage = onShowMessage
            )
        }

        // Admin-only Bridge Docs & Python Sender Guide
        if (uiState.isAdminUnlocked) {
            item {
                CodeGuideCard(
                    icon = Icons.Rounded.Code,
                    title = "Expected Signal JSON Format",
                    subtitle = "Push objects under the `signals` node/collection with these fields:",
                    code = EXPECTED_SIGNAL_JSON,
                    accentColor = colors.goldAccent,
                    onCopy = {
                        clipboard.setText(AnnotatedString(EXPECTED_SIGNAL_JSON))
                        onShowMessage("Signal JSON schema copied to clipboard")
                    }
                )
            }

            item {
                val pythonCode = buildPythonBridgeSnippet(uiState.customConfig.databaseURL)
                CodeGuideCard(
                    icon = Icons.Rounded.Terminal,
                    title = "Laptop / VPS Python Bot Sender",
                    subtitle = "100% Free Firebase RTDB REST push (no Firebase Admin SDK required):",
                    code = pythonCode,
                    accentColor = colors.emeraldBuy,
                    onCopy = {
                        clipboard.setText(AnnotatedString(pythonCode))
                        onShowMessage("Python bot snippet copied to clipboard")
                    }
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Supported Strategies & Free-Tier Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Sides: BUY, SELL\n" +
                                "• Timeframes: M1, M5, M15, M30, H1, H4, D1\n" +
                                "• Strategies: 3-Module PA, Bollinger Bands, RSI+EMA, H1 Candle, Candlestick PA\n" +
                                "• Statuses: ACTIVE, TP1_HIT, TP2_HIT, SL_HIT, CLOSED\n" +
                                "• Spark Free Tier: App streams only the latest 50 signals (`limitToLast=50`) to save bandwidth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedTheme: DeskThemeId,
    onSelectTheme: (DeskThemeId) -> Unit
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = colors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Terminal Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tap any style to customize your Digital Vertex desk",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val themes = DeskThemeId.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.chunked(2).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowThemes.forEach { themeId ->
                            val preview = paletteForTheme(themeId)
                            val isSelected = selectedTheme == themeId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = preview.surfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.emeraldBuy else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectTheme(themeId) }
                                    .testTag("theme_option_${themeId.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(preview.emeraldBuy)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(preview.goldAccent)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(preview.crimsonSell)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Active theme",
                                                tint = colors.emeraldBuy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = themeId.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = preview.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = themeId.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = preview.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeGuideCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    code: String,
    accentColor: Color,
    onCopy: () -> Unit
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy snippet",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = accentColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
