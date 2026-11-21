package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.data.ConfigPreferencesStore
import com.example.data.CustomFirebaseRepository
import com.example.model.ConnectionMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Lightweight 100% free background listener service that keeps listening to Firebase
 * RTDB / Custom Firestore when the app is minimized or screen is locked, popping
 * high-priority Android notifications for newly arriving signals.
 */
class SignalMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    private val seenIds = mutableSetOf<String>()
    private var initialSnapshotProcessed = false
    private var currentConfigKey = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        SignalNotificationHelper.ensureChannels(this)
        val notification = SignalNotificationHelper.buildMonitorNotification(
            context = this,
            statusText = "Listening for live trading signals · By Muhammad Zain"
        )
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            SignalNotificationHelper.MONITOR_NOTIFICATION_ID,
            notification,
            serviceType
        )

        startMonitoring()
        return START_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        val prefsStore = ConfigPreferencesStore(applicationContext)
        val customRepo = CustomFirebaseRepository(applicationContext)

        monitorJob = serviceScope.launch {
            prefsStore.preferencesFlow
                .flatMapLatest { prefs ->
                    val nextKey = "${prefs.connectionMode}:${prefs.customConfig.databaseURL}:${prefs.customConfig.projectId}"
                    if (nextKey != currentConfigKey) {
                        currentConfigKey = nextKey
                        seenIds.clear()
                        initialSnapshotProcessed = false
                    }

                    if (!prefs.backgroundAlertsEnabled) {
                        stopSelf()
                        return@flatMapLatest emptyFlow()
                    }

                    when (prefs.connectionMode) {
                        ConnectionMode.CUSTOM_RTDB -> {
                            val url = prefs.customConfig.databaseURL
                            if (url.isBlank()) emptyFlow()
                            else customRepo.observeRtdbSignals(
                                databaseUrl = url,
                                onConnected = {},
                                onDisconnected = {}
                            )
                        }
                        ConnectionMode.CUSTOM_FIRESTORE -> {
                            if (prefs.customConfig.hasFirestoreConfig) {
                                customRepo.observeCustomFirestoreSignals(prefs.customConfig)
                            } else {
                                emptyFlow()
                            }
                        }
                        else -> emptyFlow()
                    }
                }
                .collectLatest { signals ->
                    val ids = signals.mapNotNull { it.id.takeIf(String::isNotBlank) }
                    if (!initialSnapshotProcessed) {
                        seenIds.addAll(ids)
                        initialSnapshotProcessed = true
                        return@collectLatest
                    }
                    val newlyArrived = signals.filter { it.id.isNotBlank() && it.id !in seenIds }
                    if (newlyArrived.isNotEmpty()) {
                        seenIds.addAll(newlyArrived.map { it.id })
                        newlyArrived.forEach { signal ->
                            SignalNotificationHelper.showNewSignalNotification(applicationContext, signal)
                        }
                    }
                }
        }.also { job ->
            job.invokeOnCompletion { err ->
                if (err != null && err !is CancellationException) {
                    Log.w("SignalMonitorService", "Monitor stopped: ${err.message}", err)
                }
            }
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.example.action.START_MONITOR"
        private const val ACTION_STOP = "com.example.action.STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, SignalMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SignalMonitorService::class.java))
        }
    }
}
