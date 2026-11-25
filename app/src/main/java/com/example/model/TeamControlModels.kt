package com.example.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class MemberAccessStatus(val label: String) {
    ACTIVE("ACTIVE"),
    PENDING("PENDING"),
    PAUSED("PAUSED"),
    BLOCKED("BLOCKED");

    val canReceiveSignals: Boolean
        get() = this == ACTIVE

    companion object {
        fun fromRaw(raw: String?): MemberAccessStatus {
            val upper = raw?.trim()?.uppercase(Locale.US) ?: return ACTIVE
            return entries.firstOrNull { it.name == upper } ?: ACTIVE
        }
    }
}

data class TeamMember(
    val deviceId: String = "",
    val name: String = "Trader",
    val status: String = MemberAccessStatus.ACTIVE.name,
    val joinedAt: Long = 0L,
    val lastSeenAt: Long = 0L,
    val appVersion: String = "1.0"
) {
    val parsedStatus: MemberAccessStatus
        get() = MemberAccessStatus.fromRaw(status)

    val isRecentlyOnline: Boolean
        get() = lastSeenAt > 0L && (System.currentTimeMillis() - lastSeenAt) <= 10 * 60_000L
}

data class DeskConfig(
    val announcement: String = "",
    val requireApproval: Boolean = false,
    val updatedAt: Long = 0L
)

data class TradingSessionStatus(
    val activeSessionsLabel: String,
    val isHighLiquidityOverlap: Boolean
)

fun computeActiveTradingSession(nowMillis: Long = System.currentTimeMillis()): TradingSessionStatus {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = nowMillis
    }
    val hourUtc = utcCal.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = utcCal.get(Calendar.DAY_OF_WEEK)

    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
        return TradingSessionStatus(
            activeSessionsLabel = "Crypto 24/7 · Forex Closed",
            isHighLiquidityOverlap = false
        )
    }

    val active = mutableListOf<String>()
    if (hourUtc in 0..8) active.add("Tokyo")
    if (hourUtc in 7..15) active.add("London")
    if (hourUtc in 12..20) active.add("New York")
    if (hourUtc >= 22 || hourUtc <= 6) active.add("Sydney")

    val overlap = hourUtc in 12..15
    val label = when {
        overlap -> "London + NY Overlap (Peak)"
        active.isEmpty() -> "Off-Peak · Crypto Live"
        else -> "${active.joinToString(" + ")} Session"
    }
    return TradingSessionStatus(
        activeSessionsLabel = label,
        isHighLiquidityOverlap = overlap
    )
}
