package com.example.data

import android.util.Log
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 100% Free, zero-API-key live market price fetcher & automatic TP1 / TP2 / SL evaluator.
 * - Crypto (*USDT) -> Binance public spot ticker API
 * - Gold (XAUUSD) -> Gold-API / Yahoo Finance GC=F fallback
 * - Forex (EURUSD, GBPUSD, etc.) -> Yahoo Finance public chart API
 */
class MarketPriceMonitor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLivePrices(symbols: Set<String>): Map<String, Double> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Double>()
        symbols.map { it.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { sym ->
                fetchSinglePrice(sym)?.let { price ->
                    if (price > 0.0) {
                        result[sym] = price
                    }
                }
            }
        result
    }

    private fun fetchSinglePrice(symbol: String): Double? {
        return when {
            symbol.endsWith("USDT") || symbol.endsWith("USDC") || symbol.endsWith("BUSD") -> {
                fetchBinancePrice(symbol) ?: fetchYahooChartPrice(symbol.replace("USDT", "-USD"))
            }
            symbol == "XAUUSD" || symbol == "GOLD" -> {
                fetchGoldApiPrice() ?: fetchYahooChartPrice("GC=F")
            }
            symbol == "XAGUSD" || symbol == "SILVER" -> {
                fetchYahooChartPrice("SI=F")
            }
            symbol.length == 6 && symbol.all { it.isLetter() } -> {
                fetchYahooChartPrice("${symbol}=X")
            }
            else -> fetchYahooChartPrice(symbol)
        }
    }

    private fun fetchBinancePrice(symbol: String): Double? = runCatching {
        val req = Request.Builder()
            .url("https://api.binance.com/api/v3/ticker/price?symbol=$symbol")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            val json = JSONObject(body)
            json.optString("price").toDoubleOrNull()
        }
    }.onFailure {
        Log.d(TAG, "Binance price unavailable for $symbol: ${it.message}")
    }.getOrNull()

    private fun fetchGoldApiPrice(): Double? = runCatching {
        val req = Request.Builder()
            .url("https://api.gold-api.com/price/XAU")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            val json = JSONObject(body)
            json.optDouble("price", -1.0).takeIf { it > 0.0 }
        }
    }.onFailure {
        Log.d(TAG, "Gold-API unavailable: ${it.message}")
    }.getOrNull()

    private fun fetchYahooChartPrice(yahooSymbol: String): Double? = runCatching {
        val req = Request.Builder()
            .url("https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=1m&range=1d")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            val json = JSONObject(body)
            val meta = json.optJSONObject("chart")
                ?.optJSONArray("result")
                ?.optJSONObject(0)
                ?.optJSONObject("meta")
                ?: return null
            meta.optDouble("regularMarketPrice", -1.0).takeIf { it > 0.0 }
        }
    }.onFailure {
        Log.d(TAG, "Yahoo chart price unavailable for $yahooSymbol: ${it.message}")
    }.getOrNull()

    companion object {
        private const val TAG = "MarketPriceMonitor"

        /**
         * Evaluates whether live market price has reached TP2, TP1, or SL for an open signal.
         * Guards against stale/unrealistic prices (>25% distance from entry) to prevent false triggers.
         */
        fun evaluateAutoStatus(signal: TradingSignal, livePrice: Double): SignalStatus? {
            val status = signal.parsedStatus
            if (!status.isOpen || livePrice <= 0.0 || signal.entry <= 0.0) return null

            val relativeGap = abs(livePrice - signal.entry) / signal.entry
            if (relativeGap > 0.25) return null

            return when (signal.parsedSide) {
                SignalSide.BUY -> {
                    when {
                        signal.tp2 > signal.entry && livePrice >= signal.tp2 -> SignalStatus.TP2_HIT
                        status == SignalStatus.ACTIVE && signal.tp1 > signal.entry && livePrice >= signal.tp1 ->
                            SignalStatus.TP1_HIT
                        status == SignalStatus.ACTIVE && signal.sl > 0.0 && signal.sl < signal.entry && livePrice <= signal.sl ->
                            SignalStatus.SL_HIT
                        else -> null
                    }
                }
                SignalSide.SELL -> {
                    when {
                        signal.tp2 > 0.0 && signal.tp2 < signal.entry && livePrice <= signal.tp2 -> SignalStatus.TP2_HIT
                        status == SignalStatus.ACTIVE && signal.tp1 > 0.0 && signal.tp1 < signal.entry && livePrice <= signal.tp1 ->
                            SignalStatus.TP1_HIT
                        status == SignalStatus.ACTIVE && signal.sl > signal.entry && livePrice >= signal.sl ->
                            SignalStatus.SL_HIT
                        else -> null
                    }
                }
            }
        }
    }
}
