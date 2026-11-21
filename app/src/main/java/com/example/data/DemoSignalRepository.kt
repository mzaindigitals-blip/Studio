package com.example.data

import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class DemoSignalRepository {
    private val _signals = MutableStateFlow(buildInitialDemoSignals())
    val signals: Flow<List<TradingSignal>> = _signals.asStateFlow()

    fun resetDemoData() {
        _signals.value = buildInitialDemoSignals()
    }

    fun updateStatus(signalId: String, newStatus: SignalStatus) {
        _signals.update { current ->
            current.map { signal ->
                if (signal.id == signalId) {
                    val newPnl = signal.calculatePnlForStatus(newStatus)
                    signal.copy(status = newStatus.name, pnlPercent = newPnl)
                } else {
                    signal
                }
            }
        }
    }

    fun injectTestSignal(): TradingSignal {
        val base = BuiltInFirestoreRepository.buildSampleTestSignal("demo")
        val newSignal = base.copy(
            id = "demo_${UUID.randomUUID().toString().take(8)}",
            timestamp = System.currentTimeMillis()
        )
        _signals.update { current -> listOf(newSignal) + current }
        return newSignal
    }

    private fun buildInitialDemoSignals(): List<TradingSignal> {
        val now = System.currentTimeMillis()
        val min = 60_000L
        val hour = 3_600_000L
        val day = 86_400_000L

        return listOf(
            // Today - Active & TP1_HIT & Closed
            TradingSignal(
                id = "demo_xau_1",
                symbol = "XAUUSD",
                timeframe = "M15",
                side = SignalSide.BUY.name,
                strategy = "3-Module PA",
                entry = 2644.50,
                sl = 2637.00,
                tp1 = 2655.00,
                tp2 = 2666.50,
                rr = "1:2.9",
                status = SignalStatus.ACTIVE.name,
                timestamp = now - 8 * min,
                pnlPercent = 0.0
            ),
            TradingSignal(
                id = "demo_btc_1",
                symbol = "BTCUSDT",
                timeframe = "M5",
                side = SignalSide.SELL.name,
                strategy = "Bollinger Bands",
                entry = 92140.00,
                sl = 92680.00,
                tp1 = 91400.00,
                tp2 = 90650.00,
                rr = "1:2.8",
                status = SignalStatus.TP1_HIT.name,
                timestamp = now - 42 * min,
                pnlPercent = 1.37
            ),
            TradingSignal(
                id = "demo_bnb_1",
                symbol = "BNBUSDT",
                timeframe = "H1",
                side = SignalSide.BUY.name,
                strategy = "RSI+EMA",
                entry = 618.20,
                sl = 611.00,
                tp1 = 628.00,
                tp2 = 638.50,
                rr = "1:2.8",
                status = SignalStatus.TP2_HIT.name,
                timestamp = now - 3 * hour,
                pnlPercent = 2.82
            ),
            TradingSignal(
                id = "demo_xau_2",
                symbol = "XAUUSD",
                timeframe = "M5",
                side = SignalSide.SELL.name,
                strategy = "Candlestick PA",
                entry = 2651.80,
                sl = 2657.80,
                tp1 = 2643.80,
                tp2 = 2636.80,
                rr = "1:2.5",
                status = SignalStatus.SL_HIT.name,
                timestamp = now - 5 * hour,
                pnlPercent = -1.00
            ),
            TradingSignal(
                id = "demo_btc_2",
                symbol = "BTCUSDT",
                timeframe = "H1",
                side = SignalSide.BUY.name,
                strategy = "H1 Candle",
                entry = 90420.00,
                sl = 89800.00,
                tp1 = 91300.00,
                tp2 = 92200.00,
                rr = "1:2.9",
                status = SignalStatus.TP2_HIT.name,
                timestamp = now - 7 * hour,
                pnlPercent = 2.87
            ),
            // Yesterday
            TradingSignal(
                id = "demo_xau_y1",
                symbol = "XAUUSD",
                timeframe = "H1",
                side = SignalSide.BUY.name,
                strategy = "H1 Candle",
                entry = 2622.00,
                sl = 2614.00,
                tp1 = 2634.00,
                tp2 = 2646.00,
                rr = "1:3.0",
                status = SignalStatus.TP2_HIT.name,
                timestamp = now - day - 2 * hour,
                pnlPercent = 3.00
            ),
            TradingSignal(
                id = "demo_bnb_y1",
                symbol = "BNBUSDT",
                timeframe = "M15",
                side = SignalSide.SELL.name,
                strategy = "3-Module PA",
                entry = 632.00,
                sl = 638.00,
                tp1 = 624.00,
                tp2 = 616.00,
                rr = "1:2.7",
                status = SignalStatus.TP1_HIT.name,
                timestamp = now - day - 4 * hour,
                pnlPercent = 1.33
            ),
            TradingSignal(
                id = "demo_btc_y1",
                symbol = "BTCUSDT",
                timeframe = "M15",
                side = SignalSide.BUY.name,
                strategy = "RSI+EMA",
                entry = 89600.00,
                sl = 89050.00,
                tp1 = 90400.00,
                tp2 = 91200.00,
                rr = "1:2.9",
                status = SignalStatus.SL_HIT.name,
                timestamp = now - day - 6 * hour,
                pnlPercent = -1.00
            )
        )
    }
}
