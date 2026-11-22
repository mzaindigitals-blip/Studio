package com.example

import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.calculateTradeStats
import com.example.model.parseFirebaseConfigInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignalDeskUnitTest {

    @Test
    fun parseFirebaseConfig_acceptsJsonAndJsSnippet() {
        val jsSnippet = """
            const firebaseConfig = {
              apiKey: "AIzaSyTestKey123",
              authDomain: "desk-app.firebaseapp.com",
              databaseURL: "https://desk-app-default-rtdb.firebaseio.com/",
              projectId: "desk-app",
              appId: "1:999:web:abc123",
            };
        """.trimIndent()

        val result = parseFirebaseConfigInput(jsSnippet)
        assertTrue(result.isSuccess)
        val cfg = result.getOrThrow()
        assertEquals("AIzaSyTestKey123", cfg.apiKey)
        assertEquals("desk-app.firebaseapp.com", cfg.authDomain)
        assertEquals("https://desk-app-default-rtdb.firebaseio.com", cfg.databaseURL)
        assertEquals("desk-app", cfg.projectId)
        assertEquals("1:999:web:abc123", cfg.appId)
        assertTrue(cfg.hasDatabaseUrl)
    }

    @Test
    fun calculateTradeStats_computesTodayYesterdayWinRateAndActive() {
        val now = 1_763_673_600_000L // Fixed timestamp
        val hour = 3_600_000L
        val day = 86_400_000L

        val signals = listOf(
            TradingSignal(
                id = "1",
                symbol = "XAUUSD",
                side = SignalSide.BUY.name,
                entry = 2640.0,
                sl = 2630.0,
                tp1 = 2650.0,
                tp2 = 2665.0,
                status = SignalStatus.ACTIVE.name,
                timestamp = now - hour,
                pnlPercent = 0.0
            ),
            TradingSignal(
                id = "2",
                symbol = "BTCUSDT",
                side = SignalSide.BUY.name,
                entry = 90000.0,
                sl = 89000.0,
                tp1 = 91500.0,
                tp2 = 93000.0,
                status = SignalStatus.TP2_HIT.name,
                timestamp = now - 2 * hour,
                pnlPercent = 3.0
            ),
            TradingSignal(
                id = "3",
                symbol = "BNBUSDT",
                side = SignalSide.SELL.name,
                entry = 620.0,
                sl = 630.0,
                tp1 = 605.0,
                tp2 = 590.0,
                status = SignalStatus.SL_HIT.name,
                timestamp = now - 3 * hour,
                pnlPercent = -1.0
            ),
            TradingSignal(
                id = "4",
                symbol = "XAUUSD",
                side = SignalSide.BUY.name,
                entry = 2610.0,
                sl = 2600.0,
                tp1 = 2625.0,
                tp2 = 2640.0,
                status = SignalStatus.TP1_HIT.name,
                timestamp = now - day - hour,
                pnlPercent = 1.5
            )
        )

        val stats = calculateTradeStats(signals, nowMillis = now)
        assertEquals(2L, stats.activeCount) // ACTIVE + TP1_HIT
        assertEquals(1L, stats.todayWins)
        assertEquals(1L, stats.todayLosses)
        assertEquals(2.0, stats.todayNetPnl, 0.01)
        assertEquals(1L, stats.yesterdayWins)
        assertEquals(0L, stats.yesterdayLosses)
        assertEquals(1.5, stats.yesterdayNetPnl, 0.01)
        assertEquals(66.67, stats.winRate, 0.01)
    }

    @Test
    fun calculatePnlForStatus_estimatesRiskRewardAccurately() {
        val signal = TradingSignal(
            symbol = "XAUUSD",
            side = SignalSide.BUY.name,
            entry = 2640.0,
            sl = 2630.0,
            tp1 = 2655.0,
            tp2 = 2670.0
        )
        assertEquals(1.5, signal.calculatePnlForStatus(SignalStatus.TP1_HIT), 0.01)
        assertEquals(3.0, signal.calculatePnlForStatus(SignalStatus.TP2_HIT), 0.01)
        assertEquals(-1.0, signal.calculatePnlForStatus(SignalStatus.SL_HIT), 0.01)
    }

    @Test
    fun teamJoinCode_roundTripsCleanly() {
        val original = com.example.model.CustomFirebaseConfig(
            databaseURL = "https://digital-vertex-default-rtdb.firebaseio.com",
            projectId = "digital-vertex"
        )
        val code = original.toTeamJoinCode()
        assertTrue(code.startsWith("DVX1:"))

        val parsed = parseFirebaseConfigInput(code)
        assertTrue(parsed.isSuccess)
        val cfg = parsed.getOrThrow()
        assertEquals("https://digital-vertex-default-rtdb.firebaseio.com", cfg.databaseURL)
        assertEquals("digital-vertex", cfg.projectId)
    }

    @Test
    fun parseFirebaseConfig_acceptsDirectRtdbUrl() {
        val res = parseFirebaseConfigInput("https://my-bot-rtdb.firebaseio.com/")
        assertTrue(res.isSuccess)
        assertEquals("https://my-bot-rtdb.firebaseio.com", res.getOrThrow().databaseURL)
    }

    @Test
    fun signalCardImageGenerator_rendersOrderAndResultBitmaps() {
        val palette = com.example.ui.theme.paletteForTheme(com.example.ui.theme.DeskThemeId.OBSIDIAN_GOLD)
        val signal = TradingSignal(
            id = "sig_test",
            symbol = "XAUUSD",
            timeframe = "M15",
            side = SignalSide.BUY.name,
            strategy = "3-Module PA",
            entry = 2642.50,
            sl = 2635.00,
            tp1 = 2652.50,
            tp2 = 2662.50,
            rr = "1:2.6",
            status = SignalStatus.TP2_HIT.name,
            pnlPercent = 2.67
        )

        val orderBitmap = com.example.util.SignalCardImageGenerator.renderSignalCardBitmap(
            signal = signal,
            cardType = com.example.util.ShareCardType.ORDER_ENTRY,
            palette = palette
        )
        assertEquals(1080, orderBitmap.width)
        assertEquals(1350, orderBitmap.height)

        val resultBitmap = com.example.util.SignalCardImageGenerator.renderSignalCardBitmap(
            signal = signal,
            cardType = com.example.util.ShareCardType.RESULT_HIT,
            palette = palette
        )
        assertEquals(1080, resultBitmap.width)
        assertEquals(1350, resultBitmap.height)
    }
}
