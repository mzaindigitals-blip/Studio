package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

enum class SignalSide {
    BUY,
    SELL;

    companion object {
        fun fromRaw(raw: String?): SignalSide {
            val upper = raw?.trim()?.uppercase(Locale.US) ?: return BUY
            return entries.firstOrNull { it.name == upper } ?: BUY
        }
    }
}

enum class SignalStatus(val label: String) {
    ACTIVE("ACTIVE"),
    TP1_HIT("TP1 HIT"),
    TP2_HIT("TP2 HIT"),
    SL_HIT("SL HIT"),
    CLOSED("CLOSED");

    val isOpen: Boolean
        get() = this == ACTIVE || this == TP1_HIT

    val isClosed: Boolean
        get() = !isOpen

    val isWin: Boolean
        get() = this == TP1_HIT || this == TP2_HIT

    val isLoss: Boolean
        get() = this == SL_HIT

    companion object {
        fun fromRaw(raw: String?): SignalStatus {
            val upper = raw?.trim()?.uppercase(Locale.US) ?: return ACTIVE
            return entries.firstOrNull { it.name == upper } ?: ACTIVE
        }
    }
}

enum class Timeframe {
    M1,
    M5,
    M15,
    M30,
    H1,
    H4,
    D1;

    companion object {
        fun fromRaw(raw: String?): Timeframe {
            val upper = raw?.trim()?.uppercase(Locale.US) ?: return M15
            return entries.firstOrNull { it.name == upper } ?: M15
        }
    }
}

@Keep
@IgnoreExtraProperties
data class TradingSignal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "XAUUSD",
    val timeframe: String = "M15",
    val side: String = "BUY",
    val strategy: String = "3-Module PA",
    val entry: Double = 0.0,
    val sl: Double = 0.0,
    val tp1: Double = 0.0,
    val tp2: Double = 0.0,
    val tp3: Double = 0.0,
    val rr: String = "1:2.0",
    val lotSize: String = "",
    val status: String = "ACTIVE",
    val timestamp: Long = 0L,
    val pnlPercent: Double = 0.0,
    val notes: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val parsedSide: SignalSide
        get() = SignalSide.fromRaw(side)

    val parsedStatus: SignalStatus
        get() = SignalStatus.fromRaw(status)

    val parsedTimeframe: Timeframe
        get() = Timeframe.fromRaw(timeframe)

    fun effectiveTimestampMillis(): Long {
        if (timestamp > 0L) return timestamp
        return createdAt?.toDate()?.time ?: System.currentTimeMillis()
    }

    fun calculatePnlForStatus(targetStatus: SignalStatus): Double {
        val risk = abs(entry - sl)
        return when (targetStatus) {
            SignalStatus.ACTIVE -> 0.0
            SignalStatus.TP1_HIT -> {
                if (risk > 0.0) ((abs(tp1 - entry) / risk) * 1.0).roundToTwoDecimals()
                else 1.2
            }
            SignalStatus.TP2_HIT -> {
                if (risk > 0.0) ((abs(tp2 - entry) / risk) * 1.0).roundToTwoDecimals()
                else 2.4
            }
            SignalStatus.SL_HIT -> -1.0
            SignalStatus.CLOSED -> {
                if (parsedStatus == SignalStatus.TP1_HIT) pnlPercent else 0.0
            }
        }
    }

    fun toFirestoreCreateMap(uid: String): Map<String, Any> {
        val rawMap = mutableMapOf<String, Any?>(
            "userId" to uid,
            "symbol" to symbol.trim().uppercase(Locale.US).take(20),
            "timeframe" to Timeframe.fromRaw(timeframe).name,
            "side" to SignalSide.fromRaw(side).name,
            "strategy" to strategy.trim().ifBlank { "Price Action" }.take(60),
            "entry" to entry,
            "sl" to sl,
            "tp1" to tp1,
            "tp2" to tp2,
            "tp3" to tp3.takeIf { it > 0.0 },
            "rr" to rr.trim().ifBlank { "1:2.0" }.take(20),
            "lotSize" to lotSize.takeIf { it.isNotBlank() }?.take(20),
            "status" to SignalStatus.fromRaw(status).name,
            "timestamp" to effectiveTimestampMillis(),
            "pnlPercent" to pnlPercent,
            "notes" to notes?.takeIf { it.isNotBlank() }?.take(200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return rawMap.filterValues { it != null } as Map<String, Any>
    }

    fun toRtdbJson(): JSONObject {
        return JSONObject().apply {
            if (id.isNotBlank()) put("id", id)
            put("symbol", symbol.trim().uppercase(Locale.US))
            put("timeframe", Timeframe.fromRaw(timeframe).name)
            put("side", SignalSide.fromRaw(side).name)
            put("strategy", strategy.trim().ifBlank { "Price Action" })
            put("entry", entry)
            put("sl", sl)
            put("tp1", tp1)
            put("tp2", tp2)
            if (tp3 > 0.0) put("tp3", tp3)
            put("rr", rr)
            if (lotSize.isNotBlank()) put("lotSize", lotSize)
            put("status", SignalStatus.fromRaw(status).name)
            put("timestamp", effectiveTimestampMillis())
            put("pnlPercent", pnlPercent)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }
    }
}

fun Double.roundToTwoDecimals(): Double =
    kotlin.math.round(this * 100.0) / 100.0

fun formatPrice(value: Double, symbol: String): String {
    if (value == 0.0) return "—"
    val upper = symbol.uppercase(Locale.US)
    val decimals = when {
        upper.contains("JPY") -> 3
        upper.contains("XAU") || upper.contains("XAG") -> 2
        upper.contains("BTC") || upper.contains("ETH") || upper.contains("BNB") || upper.contains("SOL") -> {
            if (value < 10.0) 4 else 2
        }
        value < 20.0 -> 5
        else -> 2
    }
    return String.format(Locale.US, "%.${decimals}f", value)
}

fun formatPnlPercent(pnl: Double): String {
    val rounded = pnl.roundToTwoDecimals()
    return when {
        rounded > 0.0 -> String.format(Locale.US, "+%.2f%%", rounded)
        rounded < 0.0 -> String.format(Locale.US, "%.2f%%", rounded)
        else -> "0.00%"
    }
}
