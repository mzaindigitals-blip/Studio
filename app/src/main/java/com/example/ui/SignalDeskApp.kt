package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.BuiltInFirestoreRepository
import com.example.data.ConfigPreferencesStore
import com.example.data.CustomFirebaseRepository
import com.example.data.DemoSignalRepository
import com.example.data.SoundAlertPlayer
import com.example.data.authStateFlow
import com.example.model.ConnectionMode
import com.example.model.TradingSignal
import com.example.service.SignalMonitorService
import com.example.ui.components.FirebaseSetupModal
import com.example.ui.components.LotSizeCalculatorBottomSheet
import com.example.ui.components.OwnerPinUnlockDialog
import com.example.ui.components.ShareSignalCardDialog
import com.example.ui.components.TerminalHeaderAndStats
import com.example.ui.screens.ActiveHistoryScreen
import com.example.ui.screens.BridgeAndSettingsScreen
import com.example.ui.screens.LiveSignalsScreen
import com.example.ui.theme.SignalDeskTheme
import com.example.ui.theme.TerminalTheme
import com.example.ui.theme.paletteForTheme
import com.example.util.ShareCardType
import com.example.util.SignalCardImageGenerator
import com.example.viewmodel.TradingDeskUiState
import com.example.viewmodel.TradingDeskViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DeskTab(
    val clientTitle: String,
    val adminTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    LIVE_SIGNALS(
        clientTitle = "Live Signals",
        adminTitle = "Live Signals",
        selectedIcon = Icons.Rounded.CandlestickChart,
        unselectedIcon = Icons.Outlined.CandlestickChart,
        testTag = "nav_tab_live_signals"
    ),
    ACTIVE_HISTORY(
        clientTitle = "History",
        adminTitle = "History",
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_active_history"
    ),
    DESK_ADMIN(
        clientTitle = "Tools",
        adminTitle = "Admin",
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_bridge_settings"
    );

    fun labelFor(isAdmin: Boolean): String = if (isAdmin) adminTitle else clientTitle
}

@Composable
fun SignalDeskApp() {
    val context = LocalContext.current
    val isDefaultFirebaseReady = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty()
    }
    val auth = remember(isDefaultFirebaseReady) {
        if (isDefaultFirebaseReady) Firebase.auth else null
    }
    val currentUser by (auth?.authStateFlow() ?: flowOf(null))
        .collectAsStateWithLifecycle(initialValue = auth?.currentUser)
    val authKey = currentUser?.uid ?: "guest"

    key(authKey) {
        val deskViewModel: TradingDeskViewModel = viewModel(
            key = authKey,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val builtInRepo = if (auth != null) {
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        BuiltInFirestoreRepository(db, auth)
                    } else {
                        null
                    }
                    val appContext = app.applicationContext
                    val autoSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    TradingDeskViewModel(
                        builtInRepo = builtInRepo,
                        customRepo = CustomFirebaseRepository(appContext),
                        demoRepo = DemoSignalRepository(),
                        prefsStore = ConfigPreferencesStore(appContext),
                        soundPlayer = SoundAlertPlayer(),
                        authenticatedUserId = currentUser?.uid,
                        onAutoSaveCard = { signal, cardType, themeId, isLightMode ->
                            autoSaveScope.launch {
                                val palette = paletteForTheme(themeId, isLightMode = isLightMode)
                                val bitmap = SignalCardImageGenerator.getOrRenderPreviewBitmap(
                                    signal = signal,
                                    cardType = cardType,
                                    palette = palette,
                                    themeKey = "${themeId.name}_$isLightMode"
                                )
                                SignalCardImageGenerator.saveSignalCardToGallery(
                                    context = appContext,
                                    bitmap = bitmap,
                                    signal = signal,
                                    cardType = cardType
                                )
                            }
                        }
                    )
                }
            }
        )

        val uiState by deskViewModel.uiState.collectAsStateWithLifecycle()
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = uiState.isLightMode
                controller.isAppearanceLightNavigationBars = uiState.isLightMode
            }
        }

        SignalDeskTheme(
            themeId = uiState.themeId,
            isLightMode = uiState.isLightMode
        ) {
            SignalDeskScaffold(
                viewModel = deskViewModel,
                uiState = uiState
            )
        }
    }
}

@Composable
private fun SignalDeskScaffold(
    viewModel: TradingDeskViewModel,
    uiState: TradingDeskUiState
) {
    val colors = TerminalTheme.colors
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DeskTab.LIVE_SIGNALS) }
    var showFirebaseModal by remember { mutableStateOf(false) }
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var showRiskCalculatorSheet by remember { mutableStateOf(false) }
    var shareCardSignal by remember { mutableStateOf<TradingSignal?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setBackgroundAlertsEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Allow Notifications permission in Android Settings for background alerts")
            }
        }
    }

    val handleToggleBackgroundAlerts: () -> Unit = {
        if (uiState.backgroundAlertsEnabled) {
            viewModel.setBackgroundAlertsEnabled(false)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    viewModel.setBackgroundAlertsEnabled(true)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                viewModel.setBackgroundAlertsEnabled(true)
            }
        }
    }

    LaunchedEffect(uiState.backgroundAlertsEnabled, uiState.connectionMode, uiState.customConfig) {
        val canMonitorCustom = uiState.connectionMode == ConnectionMode.CUSTOM_RTDB ||
            uiState.connectionMode == ConnectionMode.CUSTOM_FIRESTORE
        if (uiState.backgroundAlertsEnabled && canMonitorCustom) {
            runCatching { SignalMonitorService.start(context) }
        } else {
            runCatching { SignalMonitorService.stop(context) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentTab != DeskTab.LIVE_SIGNALS) {
        BackHandler {
            currentTab = DeskTab.LIVE_SIGNALS
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val useNavigationRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        DeskTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val label = tab.labelFor(uiState.isAdminUnlocked)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.emeraldBuy,
                                    selectedTextColor = colors.emeraldBuy,
                                    indicatorColor = colors.emeraldContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        DeskTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val label = tab.labelFor(uiState.isAdminUnlocked)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.emeraldBuy,
                                    selectedTextColor = colors.emeraldBuy,
                                    indicatorColor = colors.emeraldContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    TerminalHeaderAndStats(
                        connectionStatus = uiState.connectionStatus,
                        isSoundMuted = uiState.isSoundMuted,
                        backgroundAlertsEnabled = uiState.backgroundAlertsEnabled,
                        isAdminUnlocked = uiState.isAdminUnlocked,
                        isLightMode = uiState.isLightMode,
                        stats = uiState.stats,
                        onOpenConnectionModal = { showFirebaseModal = true },
                        onToggleMute = { viewModel.toggleSoundMute() },
                        onToggleBackgroundAlerts = handleToggleBackgroundAlerts,
                        onToggleLightMode = viewModel::toggleLightMode,
                        onTriggerOwnerUnlock = {
                            if (uiState.isAdminUnlocked) {
                                currentTab = DeskTab.DESK_ADMIN
                            } else {
                                showOwnerPinDialog = true
                            }
                        }
                    )

                    val saveCardDirectly: (TradingSignal, ShareCardType) -> Unit = { sig, cardType ->
                        scope.launch {
                            val palette = paletteForTheme(uiState.themeId, isLightMode = uiState.isLightMode)
                            val saveResult = withContext(Dispatchers.IO) {
                                val bitmap = SignalCardImageGenerator.getOrRenderPreviewBitmap(
                                    signal = sig,
                                    cardType = cardType,
                                    palette = palette,
                                    themeKey = "${uiState.themeId.name}_${uiState.isLightMode}"
                                )
                                SignalCardImageGenerator.saveSignalCardToGallery(
                                    context = context,
                                    bitmap = bitmap,
                                    signal = sig,
                                    cardType = cardType
                                )
                            }
                            saveResult.onSuccess { msg ->
                                snackbarHostState.showSnackbar("${sig.symbol} ${cardType.label}: $msg")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to save image")
                            }
                        }
                    }

                    val copyPriceSnackbar: (String, String) -> Unit = { label, value ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Copied $label: $value")
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            DeskTab.LIVE_SIGNALS -> {
                                LiveSignalsScreen(
                                    uiState = uiState,
                                    onSelectSideFilter = viewModel::setSideFilter,
                                    onSelectSymbolFilter = viewModel::setSymbolFilter,
                                    onSetLiveTodayOnly = viewModel::setLiveTodayOnly,
                                    onUpdateStatus = viewModel::updateSignalStatus,
                                    onDismissNew = viewModel::dismissNewBadge,
                                    onClearLocalFeed = viewModel::clearLocalFeed,
                                    onRestoreClearedFeed = viewModel::restoreClearedFeed,
                                    onOpenFirebaseSetup = { showFirebaseModal = true },
                                    onLoadDemoMode = { viewModel.loadDemoMode(resetData = true) },
                                    onOpenRiskCalculator = { showRiskCalculatorSheet = true },
                                    onRefreshTeamSync = viewModel::refreshTeamSync,
                                    onSaveCard = saveCardDirectly,
                                    onShareCard = { shareCardSignal = it },
                                    onCopyPrice = copyPriceSnackbar
                                )
                            }
                            DeskTab.ACTIVE_HISTORY -> {
                                ActiveHistoryScreen(
                                    uiState = uiState,
                                    onSelectSegment = viewModel::setActiveHistorySegment,
                                    onSelectHistoryRange = viewModel::setHistoryRange,
                                    onSelectHistorySymbolFilter = viewModel::setHistorySymbolFilter,
                                    onUpdateStatus = viewModel::updateSignalStatus,
                                    onDismissNew = viewModel::dismissNewBadge,
                                    onClearLocalHistoryFeed = viewModel::clearLocalHistoryFeed,
                                    onRestoreClearedHistoryFeed = viewModel::restoreClearedHistoryFeed,
                                    onRefreshTeamSync = viewModel::refreshTeamSync,
                                    onSaveCard = saveCardDirectly,
                                    onShareCard = { shareCardSignal = it },
                                    onCopyPrice = copyPriceSnackbar
                                )
                            }
                            DeskTab.DESK_ADMIN -> {
                                BridgeAndSettingsScreen(
                                    uiState = uiState,
                                    onOpenFirebaseSetup = { showFirebaseModal = true },
                                    onLoadDemoMode = { viewModel.loadDemoMode(resetData = true) },
                                    onTestSound = viewModel::testSoundAlert,
                                    onToggleBackgroundAlerts = handleToggleBackgroundAlerts,
                                    onRestoreClearedFeed = viewModel::restoreClearedFeed,
                                    onSelectTheme = viewModel::setThemeId,
                                    onSetLightMode = viewModel::setLightMode,
                                    onUpdateMemberName = viewModel::updateMemberName,
                                    onRefreshTeamSync = viewModel::refreshTeamSync,
                                    onToggleMutedPair = viewModel::toggleMutedPair,
                                    onLockAdmin = viewModel::lockAdmin,
                                    onUpdateAdminPin = viewModel::updateAdminPin,
                                    onSendAdminDiagnostic = viewModel::sendAdminDiagnosticSignal,
                                    onClearAllFirebaseSignals = viewModel::clearAllFirebaseSignals,
                                    onCloseStaleOpenSignals = viewModel::closeStaleOpenSignals,
                                    onPruneOldSignals = { viewModel.pruneOldSignals(35) },
                                    onRefreshTeamMembers = viewModel::refreshTeamMembers,
                                    onUpdateMemberStatus = viewModel::updateTeamMemberStatus,
                                    onRemoveTeamMember = viewModel::removeTeamMember,
                                    onPublishAnnouncement = viewModel::publishDeskAnnouncement,
                                    onSetRequireMemberApproval = viewModel::setRequireMemberApproval,
                                    onToggleAutoTrackTpSl = viewModel::setAutoTrackTpSlEnabled,
                                    onToggleAutoSaveCards = viewModel::setAutoSaveCardImages,
                                    onToggleShowManualButtons = viewModel::setShowManualStatusButtons,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFirebaseModal) {
        FirebaseSetupModal(
            uiState = uiState,
            onDismiss = { showFirebaseModal = false },
            onSaveAndConnectCustom = viewModel::saveAndConnectCustomConfig,
            onLoadDemoMode = { viewModel.loadDemoMode(resetData = true) },
            onConnectBuiltInCloud = viewModel::connectBuiltInCloud,
            onSendAdminDiagnostic = viewModel::sendAdminDiagnosticSignal
        )
    }

    if (showOwnerPinDialog) {
        OwnerPinUnlockDialog(
            onDismiss = { showOwnerPinDialog = false },
            onUnlockAdmin = { pin ->
                val unlocked = viewModel.unlockAdmin(pin)
                if (unlocked) {
                    currentTab = DeskTab.DESK_ADMIN
                }
                unlocked
            }
        )
    }

    if (showRiskCalculatorSheet) {
        LotSizeCalculatorBottomSheet(
            onDismiss = { showRiskCalculatorSheet = false }
        )
    }

    shareCardSignal?.let { targetSignal ->
        ShareSignalCardDialog(
            signal = targetSignal,
            onDismiss = { shareCardSignal = null },
            onShowMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }
}
