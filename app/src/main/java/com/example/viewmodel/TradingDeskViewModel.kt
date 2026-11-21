package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BuiltInFirestoreRepository
import com.example.data.ConfigPreferencesStore
import com.example.data.CustomFirebaseRepository
import com.example.data.DemoSignalRepository
import com.example.data.PersistedDeskPreferences
import com.example.data.SoundAlertPlayer
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.CustomFirebaseConfig
import com.example.model.SignalStatus
import com.example.model.TradeStats
import com.example.model.TradingSignal
import com.example.model.calculateTradeStats
import com.example.ui.theme.DeskThemeId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.launch

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
    val isAdminUnlocked: Boolean = false,
    val adminPin: String = "2026",
    val backgroundAlertsEnabled: Boolean = false,
    val signalsState: SignalsStreamState = SignalsStreamState.Loading,
    val signals: List<TradingSignal> = emptyList(),
    val visibleLiveSignals: List<TradingSignal> = emptyList(),
    val clearedSignalsCount: Int = 0,
    val stats: TradeStats = TradeStats(),
    val highlightedNewIds: Set<String> = emptySet(),
    val sideFilter: SideFilter = SideFilter.ALL,
    val symbolFilter: String = "ALL",
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
    private val authenticatedUserId: String?
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DEMO_MODE)
    private val _statusError = MutableStateFlow<String?>(null)
    private val _highlightedNewIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sideFilter = MutableStateFlow(SideFilter.ALL)
    private val _symbolFilter = MutableStateFlow("ALL")
    private val _historyRange = MutableStateFlow(HistoryRange.TODAY)
    private val _activeHistorySegment = MutableStateFlow(0)
    private val _isSendingTestSignal = MutableStateFlow(false)
    private val _isClearingFirebase = MutableStateFlow(false)
    private val _isAdminUnlocked = MutableStateFlow(false)
    private val _updatingSignalIds = MutableStateFlow<Set<String>>(emptySet())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val knownSignalIds = mutableSetOf<String>()
    private var initialEmissionCaptured = false
    private var currentPrefsSnapshot = PersistedDeskPreferences()

    private val streamFlow: Flow<SignalsStreamState> = prefsStore.preferencesFlow
        .onEach { prefs ->
            if (prefs.connectionMode != currentPrefsSnapshot.connectionMode ||
                prefs.customConfig != currentPrefsSnapshot.customConfig
            ) {
                knownSignalIds.clear()
                initialEmissionCaptured = false
                _highlightedNewIds.value = emptySet()
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
        _historyRange,
        _activeHistorySegment
    ) { side, symbol, range, seg ->
        FilterBundle(side, symbol, range, seg)
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

    private val adminBundleFlow = combine(
        _isAdminUnlocked,
        _updatingSignalIds
    ) { adminUnlocked, updating ->
        adminUnlocked to updating
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
        @Suppress("UNCHECKED_CAST")
        val adminBundle = values[5] as Pair<Boolean, Set<String>>

        val signalList = (stream as? SignalsStreamState.Success)?.signals.orEmpty()
        val visibleLive = signalList.filter { it.effectiveTimestampMillis() > prefs.clearedBeforeTimestamp }
        val clearedCount = (signalList.size - visibleLive.size).coerceAtLeast(0)
        val computedStats = calculateTradeStats(signalList)
        val effectiveStats = if (signalList.isNotEmpty()) computedStats else (syncedStats ?: computedStats)

        TradingDeskUiState(
            connectionMode = prefs.connectionMode,
            connectionStatus = statusBundle.connectionStatus,
            customConfig = prefs.customConfig,
            isSoundMuted = prefs.isSoundMuted,
            themeId = prefs.themeId,
            clearedBeforeTimestamp = prefs.clearedBeforeTimestamp,
            isAdminUnlocked = adminBundle.first,
            adminPin = prefs.adminPin,
            backgroundAlertsEnabled = prefs.backgroundAlertsEnabled,
            signalsState = stream,
            signals = signalList,
            visibleLiveSignals = visibleLive,
            clearedSignalsCount = clearedCount,
            stats = effectiveStats,
            highlightedNewIds = statusBundle.highlightedNewIds,
            sideFilter = filters.sideFilter,
            symbolFilter = filters.symbolFilter,
            historyRange = filters.historyRange,
            activeHistorySegment = filters.activeHistorySegment,
            statusError = statusBundle.statusError,
            isSendingTestSignal = statusBundle.isSendingTestSignal,
            isClearingFirebase = statusBundle.isClearingFirebase,
            updatingSignalIds = adminBundle.second,
            authenticatedUserId = authenticatedUserId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TradingDeskUiState(authenticatedUserId = authenticatedUserId)
    )

    private fun detectIncomingSignals(signals: List<TradingSignal>, isMuted: Boolean) {
        val currentIds = signals.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
        if (!initialEmissionCaptured) {
            knownSignalIds.addAll(currentIds)
            initialEmissionCaptured = true
            return
        }
        val arrived = currentIds - knownSignalIds
        if (arrived.isNotEmpty()) {
            knownSignalIds.addAll(arrived)
            _highlightedNewIds.update { it + arrived }
            if (!isMuted) {
                soundPlayer.playSignalAlert()
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

    fun setActiveHistorySegment(segment: Int) {
        _activeHistorySegment.value = segment
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

    fun unlockAdmin(enteredPin: String): Boolean {
        return if (enteredPin.trim() == currentPrefsSnapshot.adminPin) {
            _isAdminUnlocked.value = true
            _snackbarEvents.tryEmit("Admin Mode unlocked — Muhammad Zain")
            true
        } else {
            false
        }
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        _snackbarEvents.tryEmit("Admin Mode locked (Client View)")
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
            prefsStore.setConnectionMode(ConnectionMode.DEMO)
            _snackbarEvents.tryEmit("Demo Mode active with sample signals")
        }
    }

    fun connectBuiltInCloud() {
        viewModelScope.launch {
            prefsStore.setClearedBeforeTimestamp(0L)
            prefsStore.setConnectionMode(ConnectionMode.BUILT_IN_FIRESTORE)
            _snackbarEvents.tryEmit("Switched to Built-in Cloud Firestore")
        }
    }

    fun saveAndConnectCustomConfig(config: CustomFirebaseConfig) {
        viewModelScope.launch {
            if (!config.isValid) {
                _snackbarEvents.tryEmit("Enter a valid Team Code, databaseURL, or firebaseConfig")
                return@launch
            }
            val targetMode = if (config.hasDatabaseUrl) {
                ConnectionMode.CUSTOM_RTDB
            } else {
                ConnectionMode.CUSTOM_FIRESTORE
            }
            prefsStore.setClearedBeforeTimestamp(0L)
            prefsStore.saveCustomConfig(config, targetMode)
            _snackbarEvents.tryEmit(
                if (targetMode == ConnectionMode.CUSTOM_RTDB) {
                    "Connected to Digital Vertex Realtime Database"
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
                _snackbarEvents.tryEmit("${signal.symbol} marked ${newStatus.label}")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update ${signal.symbol}")
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

    private companion object {
        const val TAG = "TradingDeskVM"
    }
}
