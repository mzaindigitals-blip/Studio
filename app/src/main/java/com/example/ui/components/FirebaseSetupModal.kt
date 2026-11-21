package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.onGoogleSignInClicked
import com.example.data.signOutFromFirebase
import com.example.model.ConnectionMode
import com.example.model.CustomFirebaseConfig
import com.example.model.parseFirebaseConfigInput
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TerminalTheme
import com.example.viewmodel.TradingDeskUiState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseSetupModal(
    uiState: TradingDeskUiState,
    onDismiss: () -> Unit,
    onSaveAndConnectCustom: (CustomFirebaseConfig) -> Unit,
    onLoadDemoMode: () -> Unit,
    onConnectBuiltInCloud: () -> Unit,
    onSendAdminDiagnostic: () -> Unit
) {
    val colors = TerminalTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember {
        mutableIntStateOf(
            if (uiState.connectionMode == ConnectionMode.BUILT_IN_FIRESTORE) 1 else 0
        )
    }

    var rawConfigText by remember { mutableStateOf("") }
    var apiKey by remember(uiState.customConfig) { mutableStateOf(uiState.customConfig.apiKey) }
    var authDomain by remember(uiState.customConfig) { mutableStateOf(uiState.customConfig.authDomain) }
    var databaseURL by remember(uiState.customConfig) { mutableStateOf(uiState.customConfig.databaseURL) }
    var projectId by remember(uiState.customConfig) { mutableStateOf(uiState.customConfig.projectId) }
    var appId by remember(uiState.customConfig) { mutableStateOf(uiState.customConfig.appId) }
    var showManualFields by remember(uiState.isAdminUnlocked) { mutableStateOf(uiState.isAdminUnlocked) }
    var parseError by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Connect Digital Vertex",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (uiState.isAdminUnlocked) {
                            "Admin setup · Paste RTDB URL, firebaseConfig, or Team Code"
                        } else {
                            "Paste your Team Access Code (DVX1:...) from Muhammad Zain"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close connection setup",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.emeraldContainer,
                        activeContentColor = colors.emeraldBuy,
                        inactiveContainerColor = colors.surfaceElevated,
                        inactiveContentColor = colors.textSecondary
                    )
                ) {
                    Text("Team Code / URL")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.goldContainer,
                        activeContentColor = colors.goldAccent,
                        inactiveContainerColor = colors.surfaceElevated,
                        inactiveContentColor = colors.textSecondary
                    )
                ) {
                    Text("Cloud Account Sync")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                Text(
                    text = "Paste Team Code (DVX1:...), RTDB URL, or firebaseConfig",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = rawConfigText,
                    onValueChange = { input ->
                        rawConfigText = input
                        parseError = null
                        parseFirebaseConfigInput(input).onSuccess { parsed ->
                            if (parsed.apiKey.isNotBlank()) apiKey = parsed.apiKey
                            if (parsed.authDomain.isNotBlank()) authDomain = parsed.authDomain
                            if (parsed.databaseURL.isNotBlank()) databaseURL = parsed.databaseURL
                            if (parsed.projectId.isNotBlank()) projectId = parsed.projectId
                            if (parsed.appId.isNotBlank()) appId = parsed.appId
                        }
                    },
                    placeholder = {
                        Text(
                            text = "DVX1:eyJkYXRhYmFzZVVSTCI6...  OR  https://your-app-default-rtdb.firebaseio.com",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colors.textMuted
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    ),
                    minLines = 3,
                    maxLines = 5,
                    colors = terminalTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_config_paste_input")
                )

                if (parseError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.crimsonSell
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showManualFields = !showManualFields },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = if (showManualFields) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showManualFields) "Hide manual Firebase fields" else "Show manual Firebase fields",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }

                if (showManualFields) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ConfigField(
                        label = "databaseURL (Realtime Database)",
                        value = databaseURL,
                        onValueChange = { databaseURL = it },
                        placeholder = "https://your-project-default-rtdb.firebaseio.com"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ConfigField(
                        label = "projectId",
                        value = projectId,
                        onValueChange = { projectId = it },
                        placeholder = "my-trading-desk"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfigField(
                            label = "apiKey",
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = "AIzaSy...",
                            modifier = Modifier.weight(1f)
                        )
                        ConfigField(
                            label = "authDomain",
                            value = authDomain,
                            onValueChange = { authDomain = it },
                            placeholder = "app.firebaseapp.com",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ConfigField(
                        label = "appId",
                        value = appId,
                        onValueChange = { appId = it },
                        placeholder = "1:123456:web:abcdef"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val candidate = CustomFirebaseConfig(
                            apiKey = apiKey.trim(),
                            authDomain = authDomain.trim(),
                            databaseURL = databaseURL.trim().trimEnd('/'),
                            projectId = projectId.trim(),
                            appId = appId.trim()
                        )
                        if (rawConfigText.isNotBlank()) {
                            parseFirebaseConfigInput(rawConfigText).fold(
                                onSuccess = { parsed ->
                                    onSaveAndConnectCustom(parsed)
                                    onDismiss()
                                },
                                onFailure = { err ->
                                    if (candidate.isValid) {
                                        onSaveAndConnectCustom(candidate)
                                        onDismiss()
                                    } else {
                                        parseError = err.localizedMessage
                                    }
                                }
                            )
                        } else if (!candidate.isValid) {
                            parseError = "Enter a valid Team Code (DVX1:...), databaseURL, or Firestore config."
                        } else {
                            onSaveAndConnectCustom(candidate)
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldBuy,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_and_connect_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Connect", fontWeight = FontWeight.Bold)
                }
            } else {
                BuiltInCloudSyncPanel(
                    uiState = uiState,
                    onConnectBuiltInCloud = onConnectBuiltInCloud,
                    onDismiss = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.border)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onLoadDemoMode()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("modal_load_demo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Demo Mode", maxLines = 1)
                }

                if (uiState.isAdminUnlocked) {
                    OutlinedButton(
                        onClick = onSendAdminDiagnostic,
                        enabled = !uiState.isSendingTestSignal,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.cyanInfo.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.cyanInfo),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("modal_send_test_signal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Diagnostic Push", maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun BuiltInCloudSyncPanel(
    uiState: TradingDeskUiState,
    onConnectBuiltInCloud: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TerminalTheme.colors
    val context = LocalContext.current
    val isFirebaseAvailable = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty()
    }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser = remember(isFirebaseAvailable) {
        if (isFirebaseAvailable) Firebase.auth.currentUser else null
    }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Provisioned Cloud Firestore",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isFirebaseAvailable) {
                    "Sync your personal signals & stats across Android devices securely using your Google account."
                } else {
                    "This APK uses Team Code / RTDB URL mode (100% Free). Use the 'Team Code / URL' tab to connect your Python bot."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isFirebaseAvailable) {
                Text(
                    text = "Tip: Paste your DVX1 Team Code or Firebase RTDB URL in the first tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.goldAccent
                )
            } else if (currentUser == null) {
                Button(
                    onClick = {
                        isAuthLoading = true
                        authError = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isAuthLoading = false
                                onConnectBuiltInCloud()
                            },
                            onAuthError = { msg ->
                                isAuthLoading = false
                                authError = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isAuthLoading = false }
                        )
                    },
                    enabled = !isAuthLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldBuy,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign in with Google", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.emeraldBuy,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.email ?: "Signed in with Google",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "UID: ${currentUser.uid.take(12)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    TextButton(
                        onClick = {
                            signOutFromFirebase(credentialManager, scope)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign out",
                            tint = colors.crimsonSell,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", color = colors.crimsonSell)
                    }
                }

                if (uiState.connectionMode != ConnectionMode.BUILT_IN_FIRESTORE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onConnectBuiltInCloud()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldBuy,
                            contentColor = colors.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Built-in Cloud Firestore", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.crimsonSell
                )
            }
        }
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = TerminalTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = { Text(placeholder, color = colors.textMuted, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = JetBrainsMonoFontFamily
        ),
        colors = terminalTextFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun terminalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalTheme.colors.emeraldBuy,
    unfocusedBorderColor = TerminalTheme.colors.border,
    focusedTextColor = TerminalTheme.colors.textPrimary,
    unfocusedTextColor = TerminalTheme.colors.textPrimary,
    focusedLabelColor = TerminalTheme.colors.emeraldBuy,
    unfocusedLabelColor = TerminalTheme.colors.textSecondary,
    cursorColor = TerminalTheme.colors.emeraldBuy,
    focusedContainerColor = TerminalTheme.colors.surfaceElevated,
    unfocusedContainerColor = TerminalTheme.colors.surfaceElevated
)
