package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
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
import com.example.model.MemberAccessStatus
import com.example.model.TeamMember
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TerminalTheme
import com.example.viewmodel.TradingDeskUiState

@Composable
fun OwnerPinUnlockDialog(
    onDismiss: () -> Unit,
    onUnlockAdmin: (String) -> Boolean
) {
    val colors = TerminalTheme.colors
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = colors.goldAccent
                )
                Text("Owner Admin Access", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter your Owner PIN to access Muhammad Zain's Command Center.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = null
                    },
                    label = { Text("Owner PIN", style = MaterialTheme.typography.bodySmall) },
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
                        .fillMaxWidth()
                        .testTag("owner_pin_dialog_input")
                )
                if (pinError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pinError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.crimsonSell
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onUnlockAdmin(pinInput)) {
                        onDismiss()
                    } else {
                        pinError = "Incorrect Owner PIN"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.goldAccent,
                    contentColor = colors.background
                ),
                modifier = Modifier.testTag("owner_pin_unlock_confirm")
            ) {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Unlock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun AdminDeckCard(
    uiState: TradingDeskUiState,
    onLockAdmin: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onOpenFirebaseSetup: () -> Unit,
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
    if (!uiState.isAdminUnlocked) return

    val colors = TerminalTheme.colors
    val clipboard = LocalClipboardManager.current

    var newPinInput by remember { mutableStateOf("") }
    var announcementInput by remember(uiState.deskConfig.announcement) {
        mutableStateOf(uiState.deskConfig.announcement)
    }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val activeMembers = uiState.teamMembers.count { it.parsedStatus == MemberAccessStatus.ACTIVE }
    val pendingMembers = uiState.teamMembers.count { it.parsedStatus == MemberAccessStatus.PENDING }
    val pausedMembers = uiState.teamMembers.count {
        it.parsedStatus == MemberAccessStatus.PAUSED || it.parsedStatus == MemberAccessStatus.BLOCKED
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("admin_command_center_card")
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
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Owner Command Center",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Muhammad Zain · Full Bot, Team & Spark Quota Control",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

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
                    Text("Client View", color = colors.goldAccent, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.adminPin == "2026") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Security Tip: You are using the default PIN (2026). Update your Owner PIN below so only you can unlock Admin Mode.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.goldAccent,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Telemetry Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminMiniStat(
                    label = "SIGNALS",
                    value = uiState.signals.size.toString(),
                    accent = colors.emeraldBuy,
                    modifier = Modifier.weight(1f)
                )
                AdminMiniStat(
                    label = "OPEN",
                    value = uiState.stats.activeCount.toString(),
                    accent = colors.cyanInfo,
                    modifier = Modifier.weight(1f)
                )
                AdminMiniStat(
                    label = "TEAM ACTIVE",
                    value = "$activeMembers/${uiState.teamMembers.size}",
                    accent = colors.goldAccent,
                    modifier = Modifier.weight(1f)
                )
                AdminMiniStat(
                    label = "PAUSED/WAIT",
                    value = "${pendingMembers + pausedMembers}",
                    accent = if (pendingMembers > 0) colors.crimsonSell else colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Team Access & Broadcast Controls
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
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Team Roster & Signal Access",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
                IconButton(
                    onClick = onRefreshTeamMembers,
                    enabled = !uiState.isLoadingMembers,
                    modifier = Modifier.testTag("refresh_team_members_button")
                ) {
                    if (uiState.isLoadingMembers) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.emeraldBuy,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh team roster",
                            tint = colors.emeraldBuy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AdminToggleRow(
                icon = Icons.Rounded.Shield,
                title = "Require Approval for New Joins",
                subtitle = if (uiState.deskConfig.requireApproval) {
                    "New friends join as PENDING until you tap Approve"
                } else {
                    "Friends with Team Code join directly as ACTIVE (1-tap Pause anytime)"
                },
                checked = uiState.deskConfig.requireApproval,
                onCheckedChange = onSetRequireMemberApproval,
                testTag = "toggle_require_approval"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Broadcast Notice Editor
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = colors.goldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Broadcast Desk Notice (Shown on all friends' Live tab)",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = announcementInput,
                        onValueChange = { announcementInput = it },
                        placeholder = {
                            Text(
                                text = "e.g. Gold volatility high today — use 0.01 lot only",
                                color = colors.textMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = adminTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_announcement_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onPublishAnnouncement(announcementInput) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldAccent,
                                contentColor = colors.background
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("publish_announcement_button")
                        ) {
                            Text("Publish Notice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        if (uiState.deskConfig.announcement.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    announcementInput = ""
                                    onPublishAnnouncement("")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.border),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear Notice", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Team Members List
            if (uiState.teamMembers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No team members synced yet. Once your friends install the APK and connect using your Team Code, their names and live statuses will appear here for 1-tap Approve / Pause / Remove control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.teamMembers.forEach { member ->
                        TeamMemberRosterRow(
                            member = member,
                            isCurrentDevice = member.deviceId == uiState.deviceId,
                            onApprove = { onUpdateMemberStatus(member.deviceId, MemberAccessStatus.ACTIVE) },
                            onPause = { onUpdateMemberStatus(member.deviceId, MemberAccessStatus.PAUSED) },
                            onBlock = { onUpdateMemberStatus(member.deviceId, MemberAccessStatus.BLOCKED) },
                            onRemove = { onRemoveTeamMember(member.deviceId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Team Join Code section
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
                        text = "Share the APK + Team Code. Clients only see Signals, History & Calculator — never your Admin tools or Firebase URL.",
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
                                        appendLine("2. Tap 'Connect / Team Code', enter your Name & paste:")
                                        appendLine(teamCode)
                                        append("3. Turn ON the top Bell icon for live alerts.")
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Spark Free Tier & Bot Maintenance Tools
            Text(
                text = "Firebase Spark Quota & Cleanup Tools",
                style = MaterialTheme.typography.labelLarge,
                color = colors.cyanInfo
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCloseStaleOpenSignals,
                    enabled = !uiState.isProcessingCleanup,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_close_stale_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close >24h", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onPruneOldSignals,
                    enabled = !uiState.isProcessingCleanup,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.emeraldBuy.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldBuy),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_prune_old_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoDelete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prune Keep 35", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    label = { Text("Change Owner PIN (4+ digits)", style = MaterialTheme.typography.bodySmall) },
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
                    colors = adminTextFieldColors(),
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
                text = "Bot Engine & Storage Behavior",
                style = MaterialTheme.typography.labelLarge,
                color = colors.goldAccent
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminToggleRow(
                    icon = Icons.Rounded.AutoGraph,
                    title = "Experimental Live Price Auto-TP/SL",
                    subtitle = "Keep OFF so only your VPS Python bot marks TP/SL hits accurately (prevents early Futures spread triggers)",
                    checked = uiState.autoTrackTpSlEnabled,
                    onCheckedChange = onToggleAutoTrackTpSl,
                    testTag = "toggle_auto_track_tp_sl"
                )
                AdminToggleRow(
                    icon = Icons.Rounded.Image,
                    title = "Auto-Save Order & TP PNGs to Gallery",
                    subtitle = "Keep OFF to save phone storage — cards render in-memory and save only when you tap Save PNG",
                    checked = uiState.autoSaveCardImages,
                    onCheckedChange = onToggleAutoSaveCards,
                    testTag = "toggle_auto_save_cards"
                )
                AdminToggleRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Show Manual TP/SL Override Buttons",
                    subtitle = "Shows TP1/TP2/SL/Close buttons on cards for your Admin phone only",
                    checked = uiState.showManualStatusButtons,
                    onCheckedChange = onToggleShowManualButtons,
                    testTag = "toggle_show_manual_buttons"
                )
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
                    "This deletes all signals under `/signals` in your Firebase Realtime Database for all connected phones. Use 'Prune Keep 35' above if you only want to clean old records while keeping today's trades."
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
private fun TeamMemberRosterRow(
    member: TeamMember,
    isCurrentDevice: Boolean,
    onApprove: () -> Unit,
    onPause: () -> Unit,
    onBlock: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = TerminalTheme.colors
    val status = member.parsedStatus
    val statusColor = when (status) {
        MemberAccessStatus.ACTIVE -> colors.emeraldBuy
        MemberAccessStatus.PENDING -> colors.goldAccent
        MemberAccessStatus.PAUSED -> colors.goldAccent
        MemberAccessStatus.BLOCKED -> colors.crimsonSell
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (member.isRecentlyOnline) colors.emeraldBuy else colors.textMuted)
                    )
                    Column {
                        Text(
                            text = if (isCurrentDevice) "${member.name} (You)" else member.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${member.deviceId} · ${formatRelativeLastSeen(member.lastSeenAt)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            ),
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!isCurrentDevice) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (status != MemberAccessStatus.ACTIVE) {
                        SmallRosterActionBtn(
                            label = "Approve",
                            icon = Icons.Rounded.CheckCircle,
                            accent = colors.emeraldBuy,
                            onClick = onApprove,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        SmallRosterActionBtn(
                            label = "Pause",
                            icon = Icons.Rounded.PauseCircle,
                            accent = colors.goldAccent,
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (status != MemberAccessStatus.BLOCKED) {
                        SmallRosterActionBtn(
                            label = "Block",
                            icon = Icons.Rounded.Block,
                            accent = colors.crimsonSell,
                            onClick = onBlock,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SmallRosterActionBtn(
                        label = "Remove",
                        icon = Icons.Rounded.DeleteOutline,
                        accent = colors.textSecondary,
                        onClick = onRemove,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallRosterActionBtn(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = accent.copy(alpha = 0.08f),
            contentColor = accent
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = modifier.height(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun AdminMiniStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = colors.textMuted,
                maxLines = 1
            )
            Text(
                text = value,
                style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalTheme.colors.goldAccent,
    unfocusedBorderColor = TerminalTheme.colors.border,
    focusedTextColor = TerminalTheme.colors.textPrimary,
    unfocusedTextColor = TerminalTheme.colors.textPrimary,
    focusedLabelColor = TerminalTheme.colors.goldAccent,
    unfocusedLabelColor = TerminalTheme.colors.textSecondary,
    cursorColor = TerminalTheme.colors.goldAccent,
    focusedContainerColor = TerminalTheme.colors.surfaceElevated,
    unfocusedContainerColor = TerminalTheme.colors.surfaceElevated
)

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

private fun formatRelativeLastSeen(lastSeenAt: Long): String {
    if (lastSeenAt <= 0L) return "never"
    val diff = (System.currentTimeMillis() - lastSeenAt).coerceAtLeast(0L)
    val mins = diff / 60_000L
    val hours = diff / 3_600_000L
    return when {
        mins < 2L -> "online"
        mins < 60L -> "${mins}m ago"
        hours < 24L -> "${hours}h ago"
        else -> "${hours / 24L}d ago"
    }
}
