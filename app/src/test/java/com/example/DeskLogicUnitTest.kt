package com.example

import com.example.model.HistoryDayBucket
import com.example.model.MemberAccessStatus
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.calculateTradeStats
import com.example.model.classifyDayBucket
import com.example.model.computeActiveTradingSession
import com.example.model.formatPrice
import com.example.ui.components.CalculatorInstrument
import com.example.ui.components.calculateRiskAndLotSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DeskLogicUnitTest {

    @Test
    fun memberAccessStatus_controlsSignalReception() {
        assertTrue(MemberAccessStatus.fromRaw("active").canReceiveSignals)
        assertFalse(MemberAccessStatus.fromRaw("PAUSED").canReceiveSignals)
        assertFalse(MemberAccessStatus.fromRaw("PENDING").canReceiveSignals)
        assertFalse(MemberAccessStatus.fromRaw("BLOCKED").canReceiveSignals)
        assertEquals(MemberAccessStatus.ACTIVE, MemberAccessStatus.fromRaw("unknown_fallback"))
    }

    @Test
    fun formatPrice_rendersFullDecimalsWithoutTruncation() {
        assertEquals("1.08452", formatPrice(1.08452, "EURUSD"))
        assertEquals("154.325", formatPrice(154.325, "USDJPY"))
        assertEquals("2642.55", formatPrice(2642.55, "XAUUSD"))
        assertEquals("94850.50", formatPrice(94850.5, "BTCUSDT"))
        assertEquals("0.5842", formatPrice(0.5842, "SOLUSDT"))
    }

    @Test
    fun tradingSessionClock_detectsLondonNyOverlapAndWeekend() {
        val wednesday13Utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 25, 13, 30, 0)
        }.timeInMillis
        val overlap = computeActiveTradingSession(wednesday13Utc)
        assertTrue(overlap.isHighLiquidityOverlap)
        assertTrue(overlap.activeSessionsLabel.contains("Overlap"))

        val sundayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 22, 14, 0, 0)
        }.timeInMillis
        val weekend = computeActiveTradingSession(sundayUtc)
        assertFalse(weekend.isHighLiquidityOverlap)
        assertTrue(weekend.activeSessionsLabel.contains("Crypto"))
    }

    @Test
    fun tradeStats_separatesTodayAndYesterdayRolloverCleanly() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = now - 24L * 3600_000L

        assertEquals(HistoryDayBucket.TODAY, classifyDayBucket(now, now))
        assertEquals(HistoryDayBucket.YESTERDAY, classifyDayBucket(yesterday, now))

        val signals = listOf(
            TradingSignal(
                id = "1",
                symbol = "XAUUSD",
                side = SignalSide.BUY.name,
                entry = 2640.0,
                sl = 2630.0,
                tp1 = 2650.0,
                tp2 = 2665.0,
                status = SignalStatus.TP2_HIT.name,
                pnlPercent = 2.5,
                timestamp = now
            ),
            TradingSignal(
                id = "2",
                symbol = "BTCUSDT",
                side = SignalSide.SELL.name,
                entry = 95000.0,
                sl = 96000.0,
                tp1 = 93500.0,
                tp2 = 92000.0,
                status = SignalStatus.SL_HIT.name,
                pnlPercent = -1.0,
                timestamp = yesterday
            ),
            TradingSignal(
                id = "3",
                symbol = "ETHUSDT",
                side = SignalSide.BUY.name,
                entry = 3200.0,
                sl = 3150.0,
                tp1 = 3300.0,
                tp2 = 3400.0,
                status = SignalStatus.ACTIVE.name,
                timestamp = now
            )
        )

        val stats = calculateTradeStats(signals, nowMillis = now)
        assertEquals(1L, stats.todayWins)
        assertEquals(0L, stats.todayLosses)
        assertEquals(2.5, stats.todayNetPnl, 0.01)
        assertEquals(0L, stats.yesterdayWins)
        assertEquals(1L, stats.yesterdayLosses)
        assertEquals(-1.0, stats.yesterdayNetPnl, 0.01)
        assertEquals(1L, stats.activeCount)
        assertEquals(50.0, stats.winRate, 0.01)
    }

    @Test
    fun lotSizeCalculator_computesGoldForexAndCryptoAccurately() {
        val goldRes = calculateRiskAndLotSize(
            accountBalance = 1000.0,
            riskPercent = 1.0,
            entry = 2650.0,
            sl = 2645.0,
            instrument = CalculatorInstrument.XAUUSD
        )
        assertNotNull(goldRes)
        assertEquals(10.0, goldRes!!.riskAmountUsd, 0.01)
        assertEquals("0.02 lots", goldRes.primarySizeLabel)

        val fxRes = calculateRiskAndLotSize(
            accountBalance = 500.0,
            riskPercent = 2.0,
            entry = 1.0850,
            sl = 1.0825,
            instrument = CalculatorInstrument.FOREX_MAJOR
        )
        assertNotNull(fxRes)
        assertEquals(10.0, fxRes!!.riskAmountUsd, 0.01)
        assertEquals("0.04 lots", fxRes.primarySizeLabel)

        val cryptoRes = calculateRiskAndLotSize(
            accountBalance = 1000.0,
            riskPercent = 2.0,
            entry = 95000.0,
            sl = 94000.0,
            instrument = CalculatorInstrument.CRYPTO
        )
        assertNotNull(cryptoRes)
        assertEquals("0.02000 units", cryptoRes!!.primarySizeLabel)
    }
}
