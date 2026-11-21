package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.SignalSide
import com.example.model.TradingSignal
import com.example.model.formatPrice

object SignalNotificationHelper {
    const val CHANNEL_MONITOR = "digital_vertex_monitor"
    const val CHANNEL_SIGNALS = "digital_vertex_signals"
    const val MONITOR_NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            "Live Signal Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Digital Vertex connected to receive real-time signals in background."
            setShowBadge(false)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val signalChannel = NotificationChannel(
            CHANNEL_SIGNALS,
            "Trading Signal Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts when a new BUY or SELL signal arrives."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 180L, 80L, 220L)
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(signalChannel)
    }

    fun buildMonitorNotification(context: Context, statusText: String) =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Digital Vertex · Live Monitor")
            .setContentText(statusText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createLaunchIntent(context))
            .build()

    fun showNewSignalNotification(context: Context, signal: TradingSignal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannels(context)
        val isBuy = signal.parsedSide == SignalSide.BUY
        val sideBadge = if (isBuy) "BUY" else "SELL"
        val title = "$sideBadge ${signal.symbol} (${signal.timeframe}) · ${signal.strategy}"
        val body = "Entry ${formatPrice(signal.entry, signal.symbol)} | " +
            "SL ${formatPrice(signal.sl, signal.symbol)} | " +
            "TP1 ${formatPrice(signal.tp1, signal.symbol)} | R:R ${signal.rr}"

        val notif = NotificationCompat.Builder(context, CHANNEL_SIGNALS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createLaunchIntent(context))
            .build()

        val notifId = (signal.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(2000)
        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    private fun createLaunchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
