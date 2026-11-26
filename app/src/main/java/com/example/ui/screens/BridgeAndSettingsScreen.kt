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
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.MemberAccessStatus
import com.example.ui.components.AdminDeckCard
import com.example.ui.components.LotSizeCalculatorCard
import com.example.ui.components.MutedPairsManagerCard
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.TradingSessionStrip
import com.example.ui.theme.DeskThemeId
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TerminalTheme
import com.example.ui.theme.paletteForTheme
import com.example.viewmodel.TradingDeskUiState
import java.util.Locale

private const val EXPECTED_SIGNAL_JSON = """{
  "id": "sig_20261125_xau_01",
  "symbol": "XAUUSD",
  "timeframe": "M15",
  "side": "BUY",
  "strategy": "3-Module PA",
  "entry": 2642.50,
  "sl": 2635.00,
  "tp1": 2652.50,
  "tp2": 2662.50,
  "tp3": 2675.00,
  "lotSize": "0.02",
  "rr": "1:2.6",
  "status": "ACTIVE",
  "timestamp": 1763673400000
}"""

private fun buildPythonBridgeSnippet(databaseUrl: String): String {
    val effectiveUrl = databaseUrl.ifBlank { "https://YOUR-PROJECT-default-rtdb.firebaseio.com" }
    return """import time, requests

# Digital Vertex Free RTDB Endpoint (Muhammad Zain VPS Bot):
DATABASE_URL = "$effectiveUrl"

def push_signal(symbol, timeframe, side, strategy, entry, sl, tp1, tp2, rr, tp3=0.0, lot_size=""):
    payload = {
        "symbol": symbol.upper(),
        "timeframe": timeframe,
        "side": side,            # "BUY" or "SELL"
        "strategy": strategy,    # e.g. "3-Module PA", "RSI+EMA"
        "entry": float(entry),
        "sl": float(sl),
        "tp1": float(tp1),
        "tp2": float(tp2),
        "rr": rr,
        "status": "ACTIVE",
        "timestamp": int(time.time() * 1000)
    }
    if tp3 and float(tp3) > 0:
        payload["tp3"] = float(tp3)
    if lot_size:
        payload["lotSize"] = str(lot_size)
    res = requests.post(f"{DATABASE_URL}/signals.json", json=payload, timeout=10)
    res.raise_for_status()
    return res.json()["name"]

def update_signal_status(signal_key, new_status, pnl_percent=None):
    # Bot-authoritative updates: "TP1_HIT", "TP2_HIT", "SL_HIT", "CLOSED"
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
    onSetLightMode: (Boolean) -> Unit = {},
    onUpdateMemberName: (String) -> Unit,
    onRefreshTeamSync: () -> Unit,
    onToggleMutedPair: (String) -> Unit,
    onLockAdmin: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onSendAdminDiagnostic: () -> Unit,
    onClearAllFirebaseSignals: () -> Unit,
    onCloseStaleOpenSignals: () -> Unit,
    onPruneOldSignals: () -> Unit,
    onRefreshTeamMembers: () -> Unit,
    onUpdateMemberStatus: (String, MemberAccessStatus) -> Unit,
    onRemoveTeamMember: (String) -> Unit,
    onPublishAnnouncement: (String) -> Unit,
    onSetRequireMemberApproval: (Boolean) -> Unit,
    onToggleAutoTrackTpSl: (Boolean) -> Unit = {},
    onToggleAutoSaveCards: (Boolean) -> Unit = {},
    onToggleShowManualButtons: (Boolean) -> Unit = {},
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val clipboard = LocalClipboardManager.current
    val knownSymbols = remember(uiState.signals) {
        uiState.signals.map { it.symbol.uppercase(Locale.US) }.distinct()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        // Hero Banner Card (Adapts cleanly between Client View and Owner Admin View)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .height(144.dp)
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
                            text = if (uiState.isAdminUnlocked) {
                                "Digital Vertex · Owner Command"
                            } else {
                                "Digital Vertex · Trading Tools"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (uiState.isAdminUnlocked) {
                                "Owner: Muhammad Zain · Team Roster, Spark Quota & VPS Bridge"
                            } else {
                                "By Muhammad Zain · Risk Calculator, Alerts & Themes"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // Hidden Owner Command Center (Rendered ONLY when Admin Unlocked!)
        if (uiState.isAdminUnlocked) {
            item {
                AdminDeckCard(
                    uiState = uiState,
                    onLockAdmin = onLockAdmin,
                    onUpdateAdminPin = onUpdateAdminPin,
                    onOpenFirebaseSetup = onOpenFirebaseSetup,
                    onSendAdminDiagnostic = onSendAdminDiagnostic,
                    onClearAllFirebaseSignals = onClearAllFirebaseSignals,
                    onCloseStaleOpenSignals = onCloseStaleOpenSignals,
                    onPruneOldSignals = onPruneOldSignals,
                    onRefreshTeamMembers = onRefreshTeamMembers,
                    onUpdateMemberStatus = onUpdateMemberStatus,
                    onRemoveTeamMember = onRemoveTeamMember,
                    onPublishAnnouncement = onPublishAnnouncement,
                    onSetRequireMemberApproval = onSetRequireMemberApproval,
                    onToggleAutoTrackTpSl = onToggleAutoTrackTpSl,
                    onToggleAutoSaveCards = onToggleAutoSaveCards,
                    onToggleShowManualButtons = onToggleShowManualButtons,
                    onShowMessage = onShowMessage
                )
            }
        }

        // Client Membership Card (Name, Device ID, Live Access Status)
        item {
            MemberProfileCard(
                uiState = uiState,
                onSaveMemberName = onUpdateMemberName,
                onRefreshSync = onRefreshTeamSync
            )
        }

        // Live Session Clock + Risk & Lot Size Calculator
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TradingSessionStrip()
                LotSizeCalculatorCard()
            }
        }

        // Per-Pair Notification Mute / Filter
        item {
            MutedPairsManagerCard(
                knownSymbols = knownSymbols,
                mutedPairs = uiState.mutedPairs,
                onToggleMutePair = onToggleMutedPair
            )
        }

        // Theme Picker + Dark / Light Mode Card
        item {
            ThemeSelectorCard(
                selectedTheme = uiState.themeId,
                isLightMode = uiState.isLightMode,
                onSelectTheme = onSelectTheme,
                onSetLightMode = onSetLightMode
            )
        }

        // Connection & Alerts Card (Zero raw Firebase URL leak for non-admin clients!)
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
                                "Active Source: Digital Vertex Team Live Stream"
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

        // Admin-only Bridge Docs & Python Sender Guide
        if (uiState.isAdminUnlocked) {
            item {
                CodeGuideCard(
                    icon = Icons.Rounded.Code,
                    title = "Expected Signal JSON Format",
                    subtitle = "Push objects under `/signals` with these fields (tp3 and lotSize are optional):",
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
                    title = "VPS Python Bot Sender",
                    subtitle = "100% Free Firebase RTDB REST push (Bot-authoritative TP/SL updates):",
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
                            text = "Spark Free-Tier Bandwidth Protection",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Foreground app streams only the latest 35 signals (`limitToLast=35`), background listener uses `limitToLast=20`.\n" +
                                "• Pair stops: Turning off a pair in your VPS bot stops notifications automatically, and clients can mute individual pairs locally.\n" +
                                "• Zero Firebase Storage: Signal cards render locally in RAM and save PNGs only on user click.\n" +
                                "• Use 'Prune Keep 35' periodically to keep your Spark RTDB tiny and ultra-fast for all friends.",
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
private fun MemberProfileCard(
    uiState: TradingDeskUiState,
    onSaveMemberName: (String) -> Unit,
    onRefreshSync: () -> Unit
) {
    val colors = TerminalTheme.colors
    var editableName by remember(uiState.memberName) { mutableStateOf(uiState.memberName) }
    val statusColor = when (uiState.memberAccessStatus) {
        MemberAccessStatus.ACTIVE -> colors.emeraldBuy
        MemberAccessStatus.PENDING, MemberAccessStatus.PAUSED -> colors.goldAccent
        MemberAccessStatus.BLOCKED -> colors.crimsonSell
    }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "My Desk Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Device ID: ${uiState.deviceId.ifBlank { "Generating..." }}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            ),
                            color = colors.textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = uiState.memberAccessStatus.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(onClick = onRefreshSync) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sync status",
                            tint = colors.emeraldBuy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it },
                    label = { Text("Display Name", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("member_profile_name_input")
                )

                OutlinedButton(
                    onClick = { onSaveMemberName(editableName) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldBuy),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Save Name")
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedTheme: DeskThemeId,
    isLightMode: Boolean,
    onSelectTheme: (DeskThemeId) -> Unit,
    onSetLightMode: (Boolean) -> Unit
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
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Switch between Dark / Light Mode and pick your palette",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dark Mode / Light Mode Toggle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val darkSelected = !isLightMode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (darkSelected) colors.emeraldContainer else Color.Transparent,
                        border = if (darkSelected) BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.55f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetLightMode(false) }
                            .testTag("appearance_dark_mode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = if (darkSelected) colors.emeraldBuy else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (darkSelected) colors.emeraldBuy else colors.textSecondary
                            )
                        }
                    }

                    val lightSelected = isLightMode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (lightSelected) colors.goldContainer else Color.Transparent,
                        border = if (lightSelected) BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.65f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetLightMode(true) }
                            .testTag("appearance_light_mode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = if (lightSelected) colors.goldAccent else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Light Mode",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (lightSelected) colors.goldAccent else colors.textSecondary
                            )
                        }
                    }
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
                            val preview = paletteForTheme(themeId, isLightMode = isLightMode)
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
