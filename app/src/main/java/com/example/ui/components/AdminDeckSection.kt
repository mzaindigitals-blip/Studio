package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TerminalTheme
import com.example.viewmodel.TradingDeskUiState

@Composable
fun AdminDeckCard(
    uiState: TradingDeskUiState,
    onUnlockAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onOpenFirebaseSetup: () -> Unit,
    onSendAdminDiagnostic: () -> Unit,
    onClearAllFirebaseSignals: () -> Unit,
    onToggleAutoTrackTpSl: (Boolean) -> Unit = {},
    onToggleAutoSaveCards: (Boolean) -> Unit = {},
    onToggleShowManualButtons: (Boolean) -> Unit = {},
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    val clipboard = LocalClipboardManager.current

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var newPinInput by remember { mutableStateOf("") }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (uiState.isAdminUnlocked) colors.goldAccent.copy(alpha = 0.55f) else colors.border
        ),
        modifier = modifier
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
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = colors.goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (uiState.isAdminUnlocked) {
                                "Admin Deck · Muhammad Zain"
                            } else {
                                "Owner Admin Access"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (uiState.isAdminUnlocked) {
                                "Manage Team Access Codes, Firebase DB & Python Bridge"
                            } else {
                                "Clients receive signals only. Unlock Admin to manage setup & team sharing."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                if (uiState.isAdminUnlocked) {
                    TextButton(
                        onClick = onLockAdmin,
                        modifier = Modifier.testTag("lock_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock Admin Mode",
                            tint = colors.goldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock", color = colors.goldAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.isAdminUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = null
                        },
                        label = { Text("Admin PIN", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("••••", color = colors.textMuted) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.goldAccent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.goldAccent,
                            unfocusedLabelColor = colors.textSecondary,
                            cursorColor = colors.goldAccent,
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_pin_input")
                    )

                    Button(
                        onClick = {
                            val ok = onUnlockAdmin(pinInput)
                            if (ok) {
                                pinInput = ""
                                pinError = null
                            } else {
                                pinError = "Incorrect Admin PIN"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.goldAccent,
                            contentColor = colors.background
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("unlock_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pinError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.crimsonSell
                    )
                }
            } else {
                // Team Join Code section (Free APK + Team Code distribution)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = colors.emeraldBuy,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Team & Friends Access Code (100% Free)",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share the APK via WhatsApp or Bluetooth, then send this Team Code so friends connect in 1 tap:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.customConfig.isValid) {
                            val teamCode = remember(uiState.customConfig) {
                                uiState.customConfig.toTeamJoinCode()
                            }
                            Text(
                                text = teamCode,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = colors.emeraldBuy,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(teamCode))
                                        onShowMessage("Team Access Code copied — paste on WhatsApp!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.emeraldBuy,
                                        contentColor = colors.background
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("copy_team_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Team Code", maxLines = 1, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val invite = buildString {
                                            appendLine("Digital Vertex — Live Trading Signals (By Muhammad Zain)")
                                            appendLine("1. Install the Digital Vertex APK")
                                            appendLine("2. Tap 'Connect / Team Code' & paste this code:")
                                            appendLine(teamCode)
                                            append("3. Turn ON the top Bell icon for live background notifications.")
                                        }
                                        clipboard.setText(AnnotatedString(invite))
                                        onShowMessage("WhatsApp invite message copied!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Copy Invite", maxLines = 1)
                                }
                            }
                        } else {
                            Text(
                                text = "Connect your Firebase RTDB URL first to generate a Team Access Code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onOpenFirebaseSetup,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.goldAccent,
                                    contentColor = colors.background
                                )
                            ) {
                                Text("Configure Firebase URL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Admin Firebase Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendAdminDiagnostic,
                        enabled = !uiState.isSendingTestSignal,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.cyanInfo.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.cyanInfo),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_send_diagnostic_button")
                    ) {
                        if (uiState.isSendingTestSignal) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = colors.cyanInfo,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Diagnostic Push", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showConfirmClearDialog = true },
                        enabled = !uiState.isClearingFirebase,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.crimsonSell.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.crimsonSell),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_clear_firebase_button")
                    ) {
                        if (uiState.isClearingFirebase) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = colors.crimsonSell,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Firebase", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.borderSubtle)
                Spacer(modifier = Modifier.height(10.dp))

                // Change Admin PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("New Admin PIN (4+ digits)", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.goldAccent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.goldAccent,
                            unfocusedLabelColor = colors.textSecondary,
                            cursorColor = colors.goldAccent,
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            onUpdateAdminPin(newPinInput)
                            if (newPinInput.trim().length >= 4) {
                                newPinInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Save PIN")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.borderSubtle)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Automation & Card Engine (100% Free)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.goldAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminToggleRow(
                        icon = Icons.Rounded.AutoGraph,
                        title = "Auto TP/SL Market Tracker",
                        subtitle = "Monitors Binance & Gold/Forex prices every 20s and auto-updates TP1/TP2/SL",
                        checked = uiState.autoTrackTpSlEnabled,
                        onCheckedChange = onToggleAutoTrackTpSl,
                        testTag = "toggle_auto_track_tp_sl"
                    )
                    AdminToggleRow(
                        icon = Icons.Rounded.Image,
                        title = "Auto-Save Order & TP Cards",
                        subtitle = "Automatically saves Order & Result PNG cards to Pictures/DigitalVertex",
                        checked = uiState.autoSaveCardImages,
                        onCheckedChange = onToggleAutoSaveCards,
                        testTag = "toggle_auto_save_cards"
                    )
                    AdminToggleRow(
                        icon = Icons.Rounded.TouchApp,
                        title = "Show Manual TP/SL Buttons",
                        subtitle = "Keep OFF for clean 100% auto mode; turn ON only if manual override needed",
                        checked = uiState.showManualStatusButtons,
                        onCheckedChange = onToggleShowManualButtons,
                        testTag = "toggle_show_manual_buttons"
                    )
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Clear Firebase Signals?") },
            text = {
                Text(
                    "This deletes all signals under `/signals` in your Firebase Realtime Database for all connected phones. Use Client 'Clear' on the Live tab if you only want to clean your own phone screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllFirebaseSignals()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.crimsonSell,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AdminToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) colors.emeraldBuy else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.emeraldBuy,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surface
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
