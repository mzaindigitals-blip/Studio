package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Calendar
import java.util.TimeZone

@Keep
@IgnoreExtraProperties
data class TradeStats(
    val userId: String = "",
    val todayWins: Long = 0L,
    val todayLosses: Long = 0L,
    val todayNetPnl: Double = 0.0,
    val yesterdayWins: Long = 0L,
    val yesterdayLosses: Long = 0L,
    val yesterdayNetPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val activeCount: Long = 0L,
    val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "todayWins" to todayWins,
        "todayLosses" to todayLosses,
        "todayNetPnl" to todayNetPnl,
        "yesterdayWins" to yesterdayWins,
        "yesterdayLosses" to yesterdayLosses,
        "yesterdayNetPnl" to yesterdayNetPnl,
        "winRate" to winRate,
        "activeCount" to activeCount,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class HistoryDayBucket {
    TODAY,
    YESTERDAY,
    OLDER
}

fun classifyDayBucket(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): HistoryDayBucket {
    val tz = TimeZone.getDefault()
    val todayStart = Calendar.getInstance(tz).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = Calendar.getInstance(tz).apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    return when {
        timestampMillis >= todayStart -> HistoryDayBucket.TODAY
        timestampMillis >= yesterdayStart -> HistoryDayBucket.YESTERDAY
        else -> HistoryDayBucket.OLDER
    }
}

fun calculateTradeStats(
    signals: List<TradingSignal>,
    nowMillis: Long = System.currentTimeMillis()
): TradeStats {
    var todayWins = 0L
    var todayLosses = 0L
    var todayNetPnl = 0.0
    var yesterdayWins = 0L
    var yesterdayLosses = 0L
    var yesterdayNetPnl = 0.0
    var totalWins = 0L
    var totalLosses = 0L
    var activeCount = 0L

    for (signal in signals) {
        val status = signal.parsedStatus
        if (status.isOpen) {
            activeCount++
        }
        val bucket = classifyDayBucket(signal.effectiveTimestampMillis(), nowMillis)
        val pnl = if (signal.pnlPercent != 0.0 || status == SignalStatus.CLOSED) {
            signal.pnlPercent
        } else {
            signal.calculatePnlForStatus(status)
        }

        if (status.isWin) totalWins++
        if (status.isLoss) totalLosses++

        when (bucket) {
            HistoryDayBucket.TODAY -> {
                if (status.isWin) todayWins++
                if (status.isLoss) todayLosses++
                if (status != SignalStatus.ACTIVE) todayNetPnl += pnl
            }
            HistoryDayBucket.YESTERDAY -> {
                if (status.isWin) yesterdayWins++
                if (status.isLoss) yesterdayLosses++
                if (status != SignalStatus.ACTIVE) yesterdayNetPnl += pnl
            }
            HistoryDayBucket.OLDER -> Unit
        }
    }

    val decided = totalWins + totalLosses
    val winRate = if (decided > 0L) {
        ((totalWins.toDouble() / decided.toDouble()) * 100.0).roundToTwoDecimals()
    } else {
        0.0
    }

    return TradeStats(
        todayWins = todayWins,
        todayLosses = todayLosses,
        todayNetPnl = todayNetPnl.roundToTwoDecimals(),
        yesterdayWins = yesterdayWins,
        yesterdayLosses = yesterdayLosses,
        yesterdayNetPnl = yesterdayNetPnl.roundToTwoDecimals(),
        winRate = winRate,
        activeCount = activeCount
    )
}
