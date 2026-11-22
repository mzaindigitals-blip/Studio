package com.example

import com.example.data.MarketPriceMonitor
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarketPriceEvaluatorTest {

    @Test
    fun buySignal_triggersTp1Tp2AndSlAccurately() {
        val buyActive = TradingSignal(
            id = "sig_buy_1",
            symbol = "XAUUSD",
            side = SignalSide.BUY.name,
            entry = 2640.0,
            sl = 2630.0,
            tp1 = 2650.0,
            tp2 = 2665.0,
            status = SignalStatus.ACTIVE.name
        )

        assertNull(MarketPriceMonitor.evaluateAutoStatus(buyActive, 2645.0))
        assertEquals(SignalStatus.TP1_HIT, MarketPriceMonitor.evaluateAutoStatus(buyActive, 2650.0))
        assertEquals(SignalStatus.TP2_HIT, MarketPriceMonitor.evaluateAutoStatus(buyActive, 2666.0))
        assertEquals(SignalStatus.SL_HIT, MarketPriceMonitor.evaluateAutoStatus(buyActive, 2629.5))

        val buyTp1 = buyActive.copy(status = SignalStatus.TP1_HIT.name)
        assertNull(MarketPriceMonitor.evaluateAutoStatus(buyTp1, 2651.0))
        assertEquals(SignalStatus.TP2_HIT, MarketPriceMonitor.evaluateAutoStatus(buyTp1, 2665.0))
    }

    @Test
    fun sellSignal_triggersTp1Tp2AndSlAccurately() {
        val sellActive = TradingSignal(
            id = "sig_sell_1",
            symbol = "BTCUSDT",
            side = SignalSide.SELL.name,
            entry = 90000.0,
            sl = 91200.0,
            tp1 = 88500.0,
            tp2 = 87000.0,
            status = SignalStatus.ACTIVE.name
        )

        assertNull(MarketPriceMonitor.evaluateAutoStatus(sellActive, 89200.0))
        assertEquals(SignalStatus.TP1_HIT, MarketPriceMonitor.evaluateAutoStatus(sellActive, 88400.0))
        assertEquals(SignalStatus.TP2_HIT, MarketPriceMonitor.evaluateAutoStatus(sellActive, 86900.0))
        assertEquals(SignalStatus.SL_HIT, MarketPriceMonitor.evaluateAutoStatus(sellActive, 91250.0))
    }

    @Test
    fun outlierMarketPrice_isIgnoredBySanityGuard() {
        val signal = TradingSignal(
            id = "sig_guard",
            symbol = "XAUUSD",
            side = SignalSide.BUY.name,
            entry = 2640.0,
            sl = 2630.0,
            tp1 = 2650.0,
            tp2 = 2665.0,
            status = SignalStatus.ACTIVE.name
        )
        // More than 25% away from entry should be rejected safely
        assertNull(MarketPriceMonitor.evaluateAutoStatus(signal, 5000.0))
        assertNull(MarketPriceMonitor.evaluateAutoStatus(signal, 100.0))
    }
}
