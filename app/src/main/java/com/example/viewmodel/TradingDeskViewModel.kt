package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BuiltInFirestoreRepository
import com.example.data.ConfigPreferencesStore
import com.example.data.CustomFirebaseRepository
import com.example.data.DemoSignalRepository
import com.example.data.MarketPriceMonitor
import com.example.data.PersistedDeskPreferences
import com.example.data.SoundAlertPlayer
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.CustomFirebaseConfig
import com.example.model.DeskConfig
import com.example.model.HistoryDayBucket
import com.example.model.MemberAccessStatus
import com.example.model.SignalStatus
import com.example.model.TeamMember
import com.example.model.TradeStats
import com.example.model.TradingSignal
import com.example.model.calculateTradeStats
import com.example.model.classifyDayBucket
import com.example.model.formatPnlPercent
import com.example.ui.theme.DeskThemeId
import com.example.util.ShareCardType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class SideFilter(val label: String) {
    ALL("ALL"),
    BUY("BUY"),
    SELL("SELL")
}

enum class HistoryRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    ALL_TIME("All Time")
}

sealed interface SignalsStreamState {
    data object Loading : SignalsStreamState
    data class Success(val signals: List<TradingSignal>) : SignalsStreamState
    data class Error(val message: String) : SignalsStreamState
}

data class TradingDeskUiState(
    val connectionMode: ConnectionMode = ConnectionMode.DEMO,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DEMO_MODE,
    val customConfig: CustomFirebaseConfig = CustomFirebaseConfig(),
    val isSoundMuted: Boolean = false,
    val themeId: DeskThemeId = DeskThemeId.OBSIDIAN_GOLD,
    val clearedBeforeTimestamp: Long = 0L,
    val historyClearedBeforeTimestamp: Long = 0L,
    val isAdminUnlocked: Boolean = false,
    val isOwnerDevice: Boolean = false,
    val adminPin: String = "2026",
    val backgroundAlertsEnabled: Boolean = false,
    val autoTrackTpSlEnabled: Boolean = false,
    val autoSaveCardImages: Boolean = false,
    val showManualStatusButtons: Boolean = false,
    val liveTodayOnly: Boolean = true,
    val mutedPairs: Set<String> = emptySet(),
    val deviceId: String = "",
    val memberName: String = "Trader",
    val memberAccessStatus: MemberAccessStatus = MemberAccessStatus.ACTIVE,
    val deskConfig: DeskConfig = DeskConfig(),
    val teamMembers: List<TeamMember> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val isProcessingCleanup: Boolean = false,
    val livePrices: Map<String, Double> = emptyMap(),
    val signalsState: SignalsStreamState = SignalsStreamState.Loading,
    val signals: List<TradingSignal> = emptyList(),
    val visibleLiveSignals: List<TradingSignal> = emptyList(),
    val hiddenOlderLiveCount: Int = 0,
    val clearedSignalsCount: Int = 0,
    val visibleHistorySignals: List<TradingSignal> = emptyList(),
    val clearedHistoryCount: Int = 0,
    val stats: TradeStats = TradeStats(),
    val highlightedNewIds: Set<String> = emptySet(),
    val sideFilter: SideFilter = SideFilter.ALL,
    val symbolFilter: String = "ALL",
    val historySymbolFilter: String = "ALL",
    val historyRange: HistoryRange = HistoryRange.TODAY,
    val activeHistorySegment: Int = 0,
    val statusError: String? = null,
    val isSendingTestSignal: Boolean = false,
    val isClearingFirebase: Boolean = false,
    val updatingSignalIds: Set<String> = emptySet(),
    val authenticatedUserId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TradingDeskViewModel(
    private val builtInRepo: BuiltInFirestoreRepository?,
    private val customRepo: CustomFirebaseRepository,
    private val demoRepo: DemoSignalRepository,
    private val prefsStore: ConfigPreferencesStore,
    private val soundPlayer: SoundAlertPlayer,
    private val authenticatedUserId: String?,
    private val marketPriceMonitor: MarketPriceMonitor = MarketPriceMonitor(),
    private val onAutoSaveCard: (TradingSignal, ShareCardType, DeskThemeId) -> Unit = { _, _, _ -> }
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DEMO_MODE)
    private val _statusError = MutableStateFlow<String?>(null)
    private val _highlightedNewIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sideFilter = MutableStateFlow(SideFilter.ALL)
    private val _symbolFilter = MutableStateFlow("ALL")
    private val _historySymbolFilter = MutableStateFlow("ALL")
    private val _historyRange = MutableStateFlow(HistoryRange.TODAY)
    private val _activeHistorySegment = MutableStateFlow(0)
    private val _isSendingTestSignal = MutableStateFlow(false)
    private val _isClearingFirebase = MutableStateFlow(false)
    private val _isAdminUnlocked = MutableStateFlow(false)
    private val _updatingSignalIds = MutableStateFlow<Set<String>>(emptySet())
    private val _livePrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    private val _memberAccessStatus = MutableStateFlow(MemberAccessStatus.ACTIVE)
    private val _deskConfig = MutableStateFlow(DeskConfig())
    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    private val _isLoadingMembers = MutableStateFlow(false)
    private val _isProcessingCleanup = MutableStateFlow(false)

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val knownSignalStatuses = mutableMapOf<String, SignalStatus>()
    private val autoTriggeredKeys = mutableSetOf<String>()
    private val autoSavedCardKeys = mutableSetOf<String>()
    @Volatile
    private var latestSignalsSnapshot: List<TradingSignal> = emptyList()
    private var initialEmissionCaptured = false
    private var currentPrefsSnapshot = PersistedDeskPreferences()

    init {
        viewModelScope.launch {
            prefsStore.ensureDeviceId()
        }
        startTeamSyncLoop()
        startMarketPriceWatcher()
    }

    private val streamFlow: Flow<SignalsStreamState> = prefsStore.preferencesFlow
        .onEach { prefs ->
            if (prefs.connectionMode != currentPrefsSnapshot.connectionMode ||
                prefs.customConfig != currentPrefsSnapshot.customConfig
            ) {
                knownSignalStatuses.clear()
                autoTriggeredKeys.clear()
                autoSavedCardKeys.clear()
                latestSignalsSnapshot = emptyList()
                _livePrices.value = emptyMap()
                initialEmissionCaptured = false
                _highlightedNewIds.value = emptySet()
            }
            if (prefs.isOwnerDevice && !_isAdminUnlocked.value) {
                _isAdminUnlocked.value = true
            }
            currentPrefsSnapshot = prefs
        }
        .flatMapLatest { prefs ->
            when (prefs.connectionMode) {
                ConnectionMode.DEMO -> {
                    _connectionStatus.value = ConnectionStatus.DEMO_MODE
                    _statusError.value = null
                    demoRepo.signals.map<List<TradingSignal>, SignalsStreamState> { list ->
                        SignalsStreamState.Success(list)
                    }
                }
                ConnectionMode.BUILT_IN_FIRESTORE -> {
                    if (builtInRepo == null || authenticatedUserId.isNullOrBlank()) {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _statusError.value = if (builtInRepo == null) {
                            "Use Team Code / RTDB URL tab to connect your live bot."
                        } else {
                            "Sign in with Google in Admin Panel to connect Cloud Firestore."
                        }
                        flowOf(SignalsStreamState.Success(emptyList()))
                    } else {
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        _statusError.value = null
                        builtInRepo.observeSignals(authenticatedUserId)
                            .map<List<TradingSignal>, SignalsStreamState> { list ->
                                _connectionStatus.value = ConnectionStatus.FIREBASE_LIVE
                                _statusError.value = null
                                SignalsStreamState.Success(list)
                            }
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                val rootError = firestoreError ?: error
                                Log.w(TAG, "Error observing built-in Firestore signals", rootError)
                                val msg = rootError.message ?: "Failed to observe signals"
                                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                                _statusError.value = msg
                                emit(SignalsStreamState.Error(msg))
                            }
                    }
                }
                ConnectionMode.CUSTOM_RTDB -> {
                    val dbUrl = prefs.customConfig.databaseURL
                    if (dbUrl.isBlank()) {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _statusError.value = "No Firebase URL configured. Enter Team Code or setup in Desk & Admin."
                        flowOf(SignalsStreamState.Error("Missing databaseURL"))
                    } else {
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        _statusError.value = null
                        customRepo.observeRtdbSignals(
                            databaseUrl = dbUrl,
                            onConnected = {
                                _connectionStatus.value = ConnectionStatus.FIREBASE_LIVE
                                _statusError.value = null
                            },
                            onDisconnected = { errMsg ->
                                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                                _statusError.value = errMsg
                            }
                        ).map<List<TradingSignal>, SignalsStreamState> { list ->
                            _connectionStatus.value = ConnectionStatus.FIREBASE_LIVE
                            SignalsStreamState.Success(list)
                        }.catch { error ->
                            if (error is CancellationException) throw error
                            val msg = error.message ?: "Realtime Database error"
                            _connectionStatus.value = ConnectionStatus.DISCONNECTED
                            _statusError.value = msg
                            emit(SignalsStreamState.Error(msg))
                        }
                    }
                }
                ConnectionMode.CUSTOM_FIRESTORE -> {
                    val cfg = prefs.customConfig
                    if (!cfg.hasFirestoreConfig) {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _statusError.value = "Incomplete Firestore configuration."
                        flowOf(SignalsStreamState.Error("Incomplete Firestore config"))
                    } else {
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        _statusError.value = null
                        customRepo.observeCustomFirestoreSignals(cfg)
                            .map<List<TradingSignal>, SignalsStreamState> { list ->
                                _connectionStatus.value = ConnectionStatus.FIREBASE_LIVE
                                _statusError.value = null
                                SignalsStreamState.Success(list)
                            }
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                val rootError = firestoreError ?: error
                                val msg = rootError.message ?: "Custom Firestore connection failed"
                                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                                _statusError.value = msg
                                emit(SignalsStreamState.Error(msg))
                            }
                    }
                }
            }
        }
        .onEach { state ->
            if (state is SignalsStreamState.Success) {
                latestSignalsSnapshot = state.signals
                detectIncomingSignals(state.signals, currentPrefsSnapshot.isSoundMuted)
            }
        }

    private val externalStatsFlow: Flow<TradeStats?> = prefsStore.preferencesFlow.flatMapLatest { prefs ->
        when (prefs.connectionMode) {
            ConnectionMode.BUILT_IN_FIRESTORE -> {
                if (builtInRepo == null || authenticatedUserId.isNullOrBlank()) {
                    flowOf(null)
                } else {
                    builtInRepo.observeStats(authenticatedUserId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            emit(null)
                        }
                }
            }
            else -> flowOf(null)
        }
    }

    private val filtersFlow = combine(
        _sideFilter,
        _symbolFilter,
        _historySymbolFilter,
        _historyRange,
        _activeHistorySegment
    ) { side, symbol, histSym, range, seg ->
        FilterBundle(side, symbol, histSym, range, seg)
    }

    private val statusBundleFlow = combine(
        _connectionStatus,
        _statusError,
        _highlightedNewIds,
        _isSendingTestSignal,
        _isClearingFirebase
    ) { status, err, newIds, sending, clearing ->
        StatusBundle(status, err, newIds, sending, clearing)
    }

    private val teamBundleFlow = combine(
        _memberAccessStatus,
        _deskConfig,
        _teamMembers,
        _isLoadingMembers,
        _isProcessingCleanup
    ) { memberStatus, deskCfg, members, loadingMembers, cleaning ->
        TeamBundle(memberStatus, deskCfg, members, loadingMembers, cleaning)
    }

    private val adminBundleFlow = combine(
        _isAdminUnlocked,
        _updatingSignalIds,
        _livePrices,
        teamBundleFlow
    ) { adminUnlocked, updating, prices, teamBundle ->
        AdminBundle(adminUnlocked, updating, prices, teamBundle)
    }

    val uiState: StateFlow<TradingDeskUiState> = combine(
        prefsStore.preferencesFlow,
        streamFlow,
        externalStatsFlow,
        filtersFlow,
        statusBundleFlow,
        adminBundleFlow
    ) { values ->
        val prefs = values[0] as PersistedDeskPreferences
        val stream = values[1] as SignalsStreamState
        val syncedStats = values[2] as TradeStats?
        val filters = values[3] as FilterBundle
        val statusBundle = values[4] as StatusBundle
        val adminBundle = values[5] as AdminBundle
        val teamBundle = adminBundle.teamBundle

        val effectiveAdminUnlocked = adminBundle.isAdminUnlocked || prefs.isOwnerDevice
        val effectiveMemberStatus = if (effectiveAdminUnlocked || prefs.connectionMode != ConnectionMode.CUSTOM_RTDB) {
            MemberAccessStatus.ACTIVE
        } else {
            teamBundle.memberAccessStatus
        }
        val canReceiveSignals = effectiveAdminUnlocked || effectiveMemberStatus.canReceiveSignals

        val rawSignals = (stream as? SignalsStreamState.Success)?.signals.orEmpty()
        val signalList = if (canReceiveSignals) rawSignals else emptyList()

        val afterClearLive = signalList.filter { it.effectiveTimestampMillis() > prefs.clearedBeforeTimestamp }
        val clearedCount = (signalList.size - afterClearLive.size).coerceAtLeast(0)

        val now = System.currentTimeMillis()
        val visibleLive = if (prefs.liveTodayOnly) {
            afterClearLive.filter { classifyDayBucket(it.effectiveTimestampMillis(), now) == HistoryDayBucket.TODAY }
        } else {
            afterClearLive
        }
        val hiddenOlderLive = (afterClearLive.size - visibleLive.size).coerceAtLeast(0)

        val visibleHistory = signalList.filter { it.effectiveTimestampMillis() > prefs.historyClearedBeforeTimestamp }
        val clearedHistoryCnt = (signalList.size - visibleHistory.size).coerceAtLeast(0)
        val computedStats = calculateTradeStats(signalList, nowMillis = now)
        val effectiveStats = if (signalList.isNotEmpty()) computedStats else (syncedStats ?: computedStats)

        TradingDeskUiState(
            connectionMode = prefs.connectionMode,
            connectionStatus = statusBundle.connectionStatus,
            customConfig = prefs.customConfig,
            isSoundMuted = prefs.isSoundMuted,
            themeId = prefs.themeId,
            clearedBeforeTimestamp = prefs.clearedBeforeTimestamp,
            historyClearedBeforeTimestamp = prefs.historyClearedBeforeTimestamp,
            isAdminUnlocked = effectiveAdminUnlocked,
            isOwnerDevice = prefs.isOwnerDevice,
            adminPin = prefs.adminPin,
            backgroundAlertsEnabled = prefs.backgroundAlertsEnabled,
            autoTrackTpSlEnabled = prefs.autoTrackTpSlEnabled,
            autoSaveCardImages = prefs.autoSaveCardImages,
            showManualStatusButtons = prefs.showManualStatusButtons,
            liveTodayOnly = prefs.liveTodayOnly,
            mutedPairs = prefs.mutedPairs,
            deviceId = prefs.deviceId,
            memberName = prefs.memberName,
            memberAccessStatus = effectiveMemberStatus,
            deskConfig = teamBundle.deskConfig,
            teamMembers = teamBundle.teamMembers,
            isLoadingMembers = teamBundle.isLoadingMembers,
            isProcessingCleanup = teamBundle.isProcessingCleanup,
            livePrices = adminBundle.livePrices,
            signalsState = stream,
            signals = signalList,
            visibleLiveSignals = visibleLive,
            hiddenOlderLiveCount = hiddenOlderLive,
            clearedSignalsCount = clearedCount,
            visibleHistorySignals = visibleHistory,
            clearedHistoryCount = clearedHistoryCnt,
            stats = effectiveStats,
            highlightedNewIds = statusBundle.highlightedNewIds,
            sideFilter = filters.sideFilter,
            symbolFilter = filters.symbolFilter,
            historySymbolFilter = filters.historySymbolFilter,
            historyRange = filters.historyRange,
            activeHistorySegment = filters.activeHistorySegment,
            statusError = statusBundle.statusError,
            isSendingTestSignal = statusBundle.isSendingTestSignal,
            isClearingFirebase = statusBundle.isClearingFirebase,
            updatingSignalIds = adminBundle.updatingSignalIds,
            authenticatedUserId = authenticatedUserId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TradingDeskUiState(authenticatedUserId = authenticatedUserId)
    )

    private fun startTeamSyncLoop() {
        viewModelScope.launch {
            delay(1_200L)
            while (isActive) {
                syncTeamAndDeskNow(silent = true)
                delay(45_000L)
            }
        }
    }

    private suspend fun syncTeamAndDeskNow(silent: Boolean) {
        val prefs = currentPrefsSnapshot
        if (prefs.connectionMode != ConnectionMode.CUSTOM_RTDB || !prefs.customConfig.hasDatabaseUrl) {
            _memberAccessStatus.value = MemberAccessStatus.ACTIVE
            return
        }
        val devId = prefs.deviceId.ifBlank { prefsStore.ensureDeviceId() }
        val isOwner = prefs.isOwnerDevice || _isAdminUnlocked.value
        customRepo.syncMemberAndDeskConfig(
            databaseUrl = prefs.customConfig.databaseURL,
            deviceId = devId,
            memberName = prefs.memberName,
            isOwnerDevice = isOwner
        ).onSuccess { (status, config) ->
            _memberAccessStatus.value = status
            _deskConfig.value = config
            prefsStore.setCachedMemberStatus(status)
        }.onFailure { err ->
            if (!silent) {
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Sync failed")
            }
        }
        if (isOwner) {
            customRepo.fetchAllTeamMembers(prefs.customConfig.databaseURL).onSuccess { list ->
                _teamMembers.value = list
            }
        }
    }

    private fun startMarketPriceWatcher() {
        viewModelScope.launch {
            delay(3_000L)
            while (isActive) {
                val prefs = currentPrefsSnapshot
                val isLiveStream = prefs.connectionMode == ConnectionMode.CUSTOM_RTDB ||
                    prefs.connectionMode == ConnectionMode.CUSTOM_FIRESTORE
                val isOwnerOrAdmin = prefs.isOwnerDevice || _isAdminUnlocked.value
                if (isLiveStream && prefs.autoTrackTpSlEnabled && isOwnerOrAdmin) {
                    val now = System.currentTimeMillis()
                    val cutoff = now - 36L * 3600_000L
                    val openSignals = latestSignalsSnapshot.filter {
                        it.parsedStatus.isOpen && it.effectiveTimestampMillis() >= cutoff
                    }
                    if (openSignals.isNotEmpty()) {
                        val symbols = openSignals.map { it.symbol.uppercase(Locale.US) }.toSet()
                        val fetchedPrices = runCatching { marketPriceMonitor.fetchLivePrices(symbols) }
                            .getOrDefault(emptyMap())
                        if (fetchedPrices.isNotEmpty()) {
                            _livePrices.update { existing -> existing + fetchedPrices }
                            openSignals.forEach { sig ->
                                val livePrice = fetchedPrices[sig.symbol.uppercase(Locale.US)] ?: return@forEach
                                val hitStatus = MarketPriceMonitor.evaluateAutoStatus(sig, livePrice)
                                    ?: return@forEach
                                val guardKey = "${sig.id}_${hitStatus.name}"
                                if (sig.parsedStatus != hitStatus && autoTriggeredKeys.add(guardKey)) {
                                    performStatusUpdate(sig, hitStatus, isAutoTriggered = true)
                                }
                            }
                        }
                    }
                }
                delay(25_000L)
            }
        }
    }

    private fun detectIncomingSignals(signals: List<TradingSignal>, isMuted: Boolean) {
        val validSignals = signals.filter { it.id.isNotBlank() }
        if (!initialEmissionCaptured) {
            validSignals.forEach { knownSignalStatuses[it.id] = it.parsedStatus }
            initialEmissionCaptured = true
            return
        }

        val prefs = currentPrefsSnapshot
        val canReceive = prefs.isOwnerDevice || _isAdminUnlocked.value || _memberAccessStatus.value.canReceiveSignals
        if (!canReceive) {
            validSignals.forEach { knownSignalStatuses[it.id] = it.parsedStatus }
            return
        }

        val now = System.currentTimeMillis()
        val maxAgeMs = 24L * 3600_000L
        val newlyArrivedIds = mutableSetOf<String>()
        val statusChanged = mutableListOf<TradingSignal>()
        val newlyArrivedSignals = mutableListOf<TradingSignal>()

        validSignals.forEach { sig ->
            val prevStatus = knownSignalStatuses[sig.id]
            val currentStatus = sig.parsedStatus
            val symUpper = sig.symbol.trim().uppercase(Locale.US)
            val isAllowedPair = symUpper !in prefs.mutedPairs
            val isFresh = (now - sig.effectiveTimestampMillis()) <= maxAgeMs

            if (prevStatus == null) {
                knownSignalStatuses[sig.id] = currentStatus
                if (isAllowedPair && isFresh) {
                    newlyArrivedIds.add(sig.id)
                    newlyArrivedSignals.add(sig)
                }
            } else if (prevStatus != currentStatus) {
                knownSignalStatuses[sig.id] = currentStatus
                if (currentStatus != SignalStatus.ACTIVE && isAllowedPair && isFresh) {
                    newlyArrivedIds.add(sig.id)
                    statusChanged.add(sig)
                }
            }
        }

        if (prefs.autoSaveCardImages) {
            val activeTheme = prefs.themeId
            newlyArrivedSignals.forEach { arrived ->
                val cardType = if (arrived.parsedStatus == SignalStatus.ACTIVE) {
                    ShareCardType.ORDER_ENTRY
                } else {
                    ShareCardType.RESULT_HIT
                }
                val saveKey = "${arrived.id}_${cardType.name}_${arrived.status}"
                if (autoSavedCardKeys.add(saveKey)) {
                    onAutoSaveCard(arrived, cardType, activeTheme)
                }
            }
            statusChanged.forEach { changed ->
                val saveKey = "${changed.id}_RESULT_${changed.parsedStatus.name}"
                if (autoSavedCardKeys.add(saveKey)) {
                    onAutoSaveCard(changed, ShareCardType.RESULT_HIT, activeTheme)
                }
            }
        }

        if (newlyArrivedIds.isNotEmpty()) {
            _highlightedNewIds.update { it + newlyArrivedIds }
            if (!isMuted) {
                soundPlayer.playSignalAlert()
            }
            statusChanged.firstOrNull()?.let { changed ->
                val pnl = if (changed.pnlPercent != 0.0 || changed.parsedStatus == SignalStatus.CLOSED) {
                    changed.pnlPercent
                } else {
                    changed.calculatePnlForStatus(changed.parsedStatus)
                }
                _snackbarEvents.tryEmit("${changed.symbol} ${changed.parsedStatus.label} (${formatPnlPercent(pnl)})")
            }
        }
    }

    fun dismissNewBadge(signalId: String) {
        _highlightedNewIds.update { it - signalId }
    }

    fun setSideFilter(filter: SideFilter) {
        _sideFilter.value = filter
    }

    fun setSymbolFilter(symbol: String) {
        _symbolFilter.value = symbol
    }

    fun setHistoryRange(range: HistoryRange) {
        _historyRange.value = range
    }

    fun setHistorySymbolFilter(symbol: String) {
        _historySymbolFilter.value = symbol
    }

    fun setActiveHistorySegment(segment: Int) {
        _activeHistorySegment.value = segment
    }

    fun setLiveTodayOnly(todayOnly: Boolean) {
        viewModelScope.launch {
            prefsStore.setLiveTodayOnly(todayOnly)
        }
    }

    fun toggleMutedPair(symbol: String) {
        val clean = symbol.trim().uppercase(Locale.US)
        if (clean.isBlank()) return
        viewModelScope.launch {
            val current = currentPrefsSnapshot.mutedPairs
            val next = if (clean in current) current - clean else current + clean
            prefsStore.setMutedPairs(next)
            _snackbarEvents.tryEmit(
                if (clean in next) "$clean alerts muted on this phone" else "$clean alerts enabled"
            )
        }
    }

    fun updateMemberName(name: String) {
        viewModelScope.launch {
            prefsStore.setMemberName(name)
            syncTeamAndDeskNow(silent = true)
            _snackbarEvents.tryEmit("Profile name saved")
        }
    }

    fun refreshTeamSync() {
        viewModelScope.launch {
            syncTeamAndDeskNow(silent = false)
            _snackbarEvents.tryEmit("Synced with Digital Vertex server")
        }
    }

    fun setThemeId(themeId: DeskThemeId) {
        viewModelScope.launch {
            prefsStore.setThemeId(themeId)
            _snackbarEvents.tryEmit("Theme switched to ${themeId.displayName}")
        }
    }

    fun clearLocalFeed() {
        viewModelScope.launch {
            val latestTs = (uiState.value.signals.maxOfOrNull { it.effectiveTimestampMillis() }
                ?: System.currentTimeMillis())
            prefsStore.setClearedBeforeTimestamp(latestTs)
            _highlightedNewIds.value = emptySet()
            _snackbarEvents.tryEmit("Live signal screen cleared — ready for new signals")
        }
    }

    fun restoreClearedFeed() {
        viewModelScope.launch {
            prefsStore.setClearedBeforeTimestamp(0L)
            _snackbarEvents.tryEmit("Cleared signals restored")
        }
    }

    fun clearLocalHistoryFeed() {
        viewModelScope.launch {
            val latestTs = (uiState.value.signals.maxOfOrNull { it.effectiveTimestampMillis() }
                ?: System.currentTimeMillis())
            prefsStore.setHistoryClearedBeforeTimestamp(latestTs)
            _snackbarEvents.tryEmit("Active & History cleared locally — ready for new trades")
        }
    }

    fun restoreClearedHistoryFeed() {
        viewModelScope.launch {
            prefsStore.setHistoryClearedBeforeTimestamp(0L)
            _snackbarEvents.tryEmit("Cleared Active & History restored")
        }
    }

    fun unlockAdmin(enteredPin: String): Boolean {
        return if (enteredPin.trim() == currentPrefsSnapshot.adminPin) {
            _isAdminUnlocked.value = true
            viewModelScope.launch {
                prefsStore.setIsOwnerDevice(true)
                syncTeamAndDeskNow(silent = true)
            }
            _snackbarEvents.tryEmit("Owner Admin unlocked — Muhammad Zain")
            true
        } else {
            false
        }
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        viewModelScope.launch {
            prefsStore.setIsOwnerDevice(false)
        }
        _snackbarEvents.tryEmit("Switched to Client View (Admin hidden)")
    }

    fun refreshTeamMembers() {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            _isLoadingMembers.value = true
            customRepo.fetchAllTeamMembers(dbUrl).onSuccess { list ->
                _teamMembers.value = list
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to load team list")
            }
            _isLoadingMembers.value = false
        }
    }

    fun updateTeamMemberStatus(deviceId: String, newStatus: MemberAccessStatus) {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            customRepo.updateMemberStatus(dbUrl, deviceId, newStatus).onSuccess {
                _teamMembers.update { list ->
                    list.map { if (it.deviceId == deviceId) it.copy(status = newStatus.name) else it }
                }
                _snackbarEvents.tryEmit("Member set to ${newStatus.label}")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update member")
            }
        }
    }

    fun removeTeamMember(deviceId: String) {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            customRepo.removeTeamMember(dbUrl, deviceId).onSuccess {
                _teamMembers.update { list -> list.filterNot { it.deviceId == deviceId } }
                _snackbarEvents.tryEmit("Member removed from roster")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to remove member")
            }
        }
    }

    fun publishDeskAnnouncement(announcement: String) {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            val next = _deskConfig.value.copy(announcement = announcement.trim())
            customRepo.updateDeskConfig(dbUrl, next).onSuccess {
                _deskConfig.value = next
                _snackbarEvents.tryEmit(
                    if (next.announcement.isBlank()) "Broadcast notice cleared" else "Notice sent to all active phones"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update notice")
            }
        }
    }

    fun setRequireMemberApproval(requireApproval: Boolean) {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            val next = _deskConfig.value.copy(requireApproval = requireApproval)
            customRepo.updateDeskConfig(dbUrl, next).onSuccess {
                _deskConfig.value = next
                _snackbarEvents.tryEmit(
                    if (requireApproval) "New members now wait for your Approval" else "Auto-join enabled for Team Code"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update approval mode")
            }
        }
    }

    fun closeStaleOpenSignals() {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            _isProcessingCleanup.value = true
            customRepo.closeStaleOpenSignals(dbUrl, latestSignalsSnapshot).onSuccess { closedCount ->
                _snackbarEvents.tryEmit(
                    if (closedCount > 0) "Closed $closedCount old open trades (>24h)" else "No stale open trades found"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Cleanup failed")
            }
            _isProcessingCleanup.value = false
        }
    }

    fun pruneOldSignals(keepLatest: Int = 35) {
        if (!_isAdminUnlocked.value && !currentPrefsSnapshot.isOwnerDevice) return
        val dbUrl = currentPrefsSnapshot.customConfig.databaseURL
        if (dbUrl.isBlank()) return
        viewModelScope.launch {
            _isProcessingCleanup.value = true
            customRepo.pruneOldSignals(dbUrl, keepLatest).onSuccess { deleted ->
                _snackbarEvents.tryEmit(
                    if (deleted > 0) "Pruned $deleted old signals (kept latest $keepLatest for Spark quota)" else "Already lean (<= $keepLatest signals)"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Prune failed")
            }
            _isProcessingCleanup.value = false
        }
    }

    fun updateAdminPin(newPin: String) {
        val clean = newPin.trim()
        if (clean.length < 4) {
            _snackbarEvents.tryEmit("PIN must be at least 4 digits")
            return
        }
        viewModelScope.launch {
            prefsStore.setAdminPin(clean)
            _snackbarEvents.tryEmit("Admin PIN updated")
        }
    }

    fun setBackgroundAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setBackgroundAlertsEnabled(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Background push monitor enabled (100% Free)" else "Background push monitor turned off"
            )
        }
    }

    fun setAutoTrackTpSlEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setAutoTrackTpSlEnabled(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Experimental live price tracker enabled" else "Bot-authoritative TP/SL mode active (Recommended)"
            )
        }
    }

    fun setAutoSaveCardImages(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setAutoSaveCardImages(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Auto-save PNG cards to Gallery enabled" else "Auto-save turned off (Storage saver)"
            )
        }
    }

    fun setShowManualStatusButtons(show: Boolean) {
        viewModelScope.launch {
            prefsStore.setShowManualStatusButtons(show)
            _snackbarEvents.tryEmit(
                if (show) "Manual TP/SL override buttons shown" else "Manual TP/SL buttons hidden"
            )
        }
    }

    fun toggleSoundMute() {
        viewModelScope.launch {
            val nextMuted = !currentPrefsSnapshot.isSoundMuted
            prefsStore.setSoundMuted(nextMuted)
            if (!nextMuted) {
                soundPlayer.playSignalAlert()
            }
            _snackbarEvents.tryEmit(if (nextMuted) "Signal sound muted" else "Signal sound enabled")
        }
    }

    fun testSoundAlert() {
        soundPlayer.playSignalAlert()
        _snackbarEvents.tryEmit("Played Digital Vertex alert chime")
    }

    fun loadDemoMode(resetData: Boolean = false) {
        viewModelScope.launch {
            if (resetData) {
                demoRepo.resetDemoData()
            }
            prefsStore.setClearedBeforeTimestamp(0L)
            prefsStore.setHistoryClearedBeforeTimestamp(0L)
            prefsStore.setConnectionMode(ConnectionMode.DEMO)
            _snackbarEvents.tryEmit("Demo Mode active with sample signals")
        }
    }

    fun connectBuiltInCloud() {
        viewModelScope.launch {
            prefsStore.setClearedBeforeTimestamp(0L)
            prefsStore.setHistoryClearedBeforeTimestamp(0L)
            prefsStore.setConnectionMode(ConnectionMode.BUILT_IN_FIRESTORE)
            _snackbarEvents.tryEmit("Switched to Built-in Cloud Firestore")
        }
    }

    fun saveAndConnectCustomConfig(config: CustomFirebaseConfig, memberName: String? = null) {
        viewModelScope.launch {
            if (!config.isValid) {
                _snackbarEvents.tryEmit("Enter a valid Team Code, databaseURL, or firebaseConfig")
                return@launch
            }
            if (!memberName.isNullOrBlank()) {
                prefsStore.setMemberName(memberName)
            }
            val targetMode = if (config.hasDatabaseUrl) {
                ConnectionMode.CUSTOM_RTDB
            } else {
                ConnectionMode.CUSTOM_FIRESTORE
            }
            prefsStore.setClearedBeforeTimestamp(0L)
            prefsStore.setHistoryClearedBeforeTimestamp(0L)
            prefsStore.saveCustomConfig(config, targetMode)
            delay(250L)
            syncTeamAndDeskNow(silent = true)
            _snackbarEvents.tryEmit(
                if (targetMode == ConnectionMode.CUSTOM_RTDB) {
                    "Connected to Digital Vertex Live Stream"
                } else {
                    "Connected to Custom Firestore"
                }
            )
        }
    }

    fun updateSignalStatus(signal: TradingSignal, newStatus: SignalStatus) {
        if (!_isAdminUnlocked.value && currentPrefsSnapshot.connectionMode != ConnectionMode.DEMO) {
            _snackbarEvents.tryEmit("Unlock Admin Mode to update Firebase signal status")
            return
        }
        performStatusUpdate(signal, newStatus, isAutoTriggered = false)
    }

    private fun performStatusUpdate(
        signal: TradingSignal,
        newStatus: SignalStatus,
        isAutoTriggered: Boolean
    ) {
        if (signal.id.isBlank()) return
        viewModelScope.launch {
            _updatingSignalIds.update { it + signal.id }
            _highlightedNewIds.update { it - signal.id }
            val result: Result<Unit> = when (currentPrefsSnapshot.connectionMode) {
                ConnectionMode.DEMO -> {
                    demoRepo.updateStatus(signal.id, newStatus)
                    Result.success(Unit)
                }
                ConnectionMode.BUILT_IN_FIRESTORE -> {
                    builtInRepo?.updateSignalStatus(signal, newStatus)
                        ?: Result.failure(IllegalStateException("Built-in Cloud Firestore is unavailable in this build."))
                }
                ConnectionMode.CUSTOM_RTDB -> {
                    customRepo.updateRtdbSignalStatus(
                        currentPrefsSnapshot.customConfig.databaseURL,
                        signal,
                        newStatus
                    )
                }
                ConnectionMode.CUSTOM_FIRESTORE -> {
                    customRepo.updateCustomFirestoreStatus(
                        currentPrefsSnapshot.customConfig,
                        signal,
                        newStatus
                    )
                }
            }
            _updatingSignalIds.update { it - signal.id }
            result.onSuccess {
                val prefix = if (isAutoTriggered) "Auto-Detected: " else ""
                val pnlStr = if (isAutoTriggered) {
                    " (${formatPnlPercent(signal.calculatePnlForStatus(newStatus))})"
                } else {
                    ""
                }
                _snackbarEvents.tryEmit("$prefix${signal.symbol} marked ${newStatus.label}$pnlStr")
            }.onFailure { err ->
                if (!isAutoTriggered) {
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update ${signal.symbol}")
                } else {
                    autoTriggeredKeys.remove("${signal.id}_${newStatus.name}")
                }
            }
        }
    }

    fun clearAllFirebaseSignals() {
        if (!_isAdminUnlocked.value) return
        viewModelScope.launch {
            _isClearingFirebase.value = true
            val result: Result<Unit> = when (currentPrefsSnapshot.connectionMode) {
                ConnectionMode.CUSTOM_RTDB -> {
                    customRepo.clearAllRtdbSignals(currentPrefsSnapshot.customConfig.databaseURL)
                }
                ConnectionMode.DEMO -> {
                    clearLocalFeed()
                    Result.success(Unit)
                }
                else -> {
                    clearLocalFeed()
                    Result.success(Unit)
                }
            }
            _isClearingFirebase.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Firebase signals cleared cleanly")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to clear Firebase signals")
            }
        }
    }

    fun sendAdminDiagnosticSignal() {
        if (!_isAdminUnlocked.value) return
        viewModelScope.launch {
            _isSendingTestSignal.value = true
            val result: Result<TradingSignal> = when (currentPrefsSnapshot.connectionMode) {
                ConnectionMode.DEMO -> Result.success(demoRepo.injectTestSignal())
                ConnectionMode.BUILT_IN_FIRESTORE -> {
                    if (builtInRepo == null || authenticatedUserId.isNullOrBlank()) {
                        Result.failure(IllegalStateException("Sign in with Google first (requires google-services.json)."))
                    } else {
                        builtInRepo.sendTestSignal()
                    }
                }
                ConnectionMode.CUSTOM_RTDB -> {
                    customRepo.sendTestSignalToRtdb(currentPrefsSnapshot.customConfig.databaseURL)
                }
                ConnectionMode.CUSTOM_FIRESTORE -> {
                    customRepo.sendTestSignalToCustomFirestore(currentPrefsSnapshot.customConfig)
                }
            }
            _isSendingTestSignal.value = false
            result.onSuccess { sig ->
                _snackbarEvents.tryEmit("Diagnostic ${sig.side} signal pushed: ${sig.symbol}")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Diagnostic push failed")
            }
        }
    }

    private data class FilterBundle(
        val sideFilter: SideFilter,
        val symbolFilter: String,
        val historySymbolFilter: String,
        val historyRange: HistoryRange,
        val activeHistorySegment: Int
    )

    private data class StatusBundle(
        val connectionStatus: ConnectionStatus,
        val statusError: String?,
        val highlightedNewIds: Set<String>,
        val isSendingTestSignal: Boolean,
        val isClearingFirebase: Boolean
    )

    private data class TeamBundle(
        val memberAccessStatus: MemberAccessStatus,
        val deskConfig: DeskConfig,
        val teamMembers: List<TeamMember>,
        val isLoadingMembers: Boolean,
        val isProcessingCleanup: Boolean
    )

    private data class AdminBundle(
        val isAdminUnlocked: Boolean,
        val updatingSignalIds: Set<String>,
        val livePrices: Map<String, Double>,
        val teamBundle: TeamBundle
    )

    private companion object {
        const val TAG = "TradingDeskVM"
    }
}
