package com.example.data.remote

import com.example.data.model.CandlePoint
import com.example.data.model.CryptoAsset
import com.example.data.model.CryptoTickerAnalysis
import com.example.data.model.CryptoTimeframe
import com.example.data.model.MarketDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class CryptoMarketService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAssetAnalysis(
        asset: CryptoAsset,
        timeframe: CryptoTimeframe
    ): CryptoTickerAnalysis = withContext(Dispatchers.IO) {
        // Tier 1: Binance Global (includes takerBuyBaseAssetVolume per candle)
        runCatching {
            fetchFromBinance("https://api.binance.com", "Binance Live", asset, timeframe)
        }.getOrElse {
            // Tier 2: Binance US
            runCatching {
                fetchFromBinance("https://api.binance.us", "Binance US Live", asset, timeframe)
            }.getOrElse {
                // Tier 3: Kraken Public API (OHLC + real buy/sell trade stream)
                fetchFromKraken(asset, timeframe)
            }
        }
    }

    private fun fetchFromBinance(
        baseUrl: String,
        sourceName: String,
        asset: CryptoAsset,
        timeframe: CryptoTimeframe
    ): CryptoTickerAnalysis {
        val klineUrl = "$baseUrl/api/v3/klines?symbol=${asset.binancePair}&interval=${timeframe.apiInterval}&limit=30"
        val tickerUrl = "$baseUrl/api/v3/ticker/24hr?symbol=${asset.binancePair}"

        val klineJson = getJson(klineUrl)
        val tickerJson = getJson(tickerUrl)

        val klines = JSONArray(klineJson)
        if (klines.length() == 0) error("Empty klines for ${asset.symbol}")

        val candles = ArrayList<CandlePoint>(klines.length())
        for (i in 0 until klines.length()) {
            val row = klines.getJSONArray(i)
            candles.add(
                CandlePoint(
                    openTime = row.getLong(0),
                    open = row.getString(1).toDouble(),
                    high = row.getString(2).toDouble(),
                    low = row.getString(3).toDouble(),
                    close = row.getString(4).toDouble(),
                    volume = row.getString(5).toDouble(),
                    takerBuyVolume = row.getString(9).toDouble()
                )
            )
        }

        val tickerObj = JSONObject(tickerJson)
        val livePrice = tickerObj.optString("lastPrice").toDoubleOrNull() ?: candles.last().close
        val change24h = tickerObj.optString("priceChangePercent").toDoubleOrNull() ?: 0.0

        return computeAnalysis(
            asset = asset,
            timeframe = timeframe,
            livePrice = livePrice,
            change24h = change24h,
            candles = candles,
            explicitBuyerRatio = null,
            sourceName = sourceName
        )
    }

    private fun fetchFromKraken(
        asset: CryptoAsset,
        timeframe: CryptoTimeframe
    ): CryptoTickerAnalysis {
        val intervalMinutes = when (timeframe) {
            CryptoTimeframe.M5 -> 5
            CryptoTimeframe.M15 -> 15
            CryptoTimeframe.H1 -> 60
        }
        val ohlcRaw = getJson("https://api.kraken.com/0/public/OHLC?pair=${asset.krakenPair}&interval=$intervalMinutes")
        val tickerRaw = getJson("https://api.kraken.com/0/public/Ticker?pair=${asset.krakenPair}")
        val tradesRaw = getJson("https://api.kraken.com/0/public/Trades?pair=${asset.krakenPair}")

        val ohlcResult = JSONObject(ohlcRaw).getJSONObject("result")
        val pairKey = ohlcResult.keys().asSequence().first { it != "last" }
        val rows = ohlcResult.getJSONArray(pairKey)
        val startIdx = (rows.length() - 30).coerceAtLeast(0)
        val candles = ArrayList<CandlePoint>()
        for (i in startIdx until rows.length()) {
            val r = rows.getJSONArray(i)
            val open = r.getString(1).toDouble()
            val high = r.getString(2).toDouble()
            val low = r.getString(3).toDouble()
            val close = r.getString(4).toDouble()
            val vol = r.getString(6).toDouble()
            val range = (high - low).coerceAtLeast(1e-9)
            val estBuy = vol * ((close - low) / range).coerceIn(0.05, 0.95)
            candles.add(
                CandlePoint(
                    openTime = r.getLong(0) * 1000L,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol,
                    takerBuyVolume = estBuy
                )
            )
        }

        val tickerResult = JSONObject(tickerRaw).getJSONObject("result")
        val tickerKey = tickerResult.keys().asSequence().first()
        val tickerObj = tickerResult.getJSONObject(tickerKey)
        val livePrice = tickerObj.getJSONArray("c").getString(0).toDouble()
        val openToday = tickerObj.optString("o").toDoubleOrNull() ?: candles.first().open
        val change24h = if (openToday > 0) ((livePrice - openToday) / openToday) * 100.0 else 0.0

        // Calculate actual buy vs sell taker ratio from Kraken recent real trades
        var buyVol = 0.0
        var sellVol = 0.0
        runCatching {
            val tradesResult = JSONObject(tradesRaw).getJSONObject("result")
            val tradesKey = tradesResult.keys().asSequence().first { it != "last" }
            val tradesArr = tradesResult.getJSONArray(tradesKey)
            val fromIdx = (tradesArr.length() - 250).coerceAtLeast(0)
            for (i in fromIdx until tradesArr.length()) {
                val t = tradesArr.getJSONArray(i)
                val vol = t.getString(1).toDouble()
                val side = t.getString(3)
                if (side == "b") buyVol += vol else sellVol += vol
            }
        }
        val explicitRatio = if (buyVol + sellVol > 0.0) buyVol / (buyVol + sellVol) else null

        return computeAnalysis(
            asset = asset,
            timeframe = timeframe,
            livePrice = livePrice,
            change24h = change24h,
            candles = candles,
            explicitBuyerRatio = explicitRatio,
            sourceName = "Kraken Live"
        )
    }

    private fun computeAnalysis(
        asset: CryptoAsset,
        timeframe: CryptoTimeframe,
        livePrice: Double,
        change24h: Double,
        candles: List<CandlePoint>,
        explicitBuyerRatio: Double?,
        sourceName: String
    ): CryptoTickerAnalysis {
        // Use the most recent 6 candles weighted toward the latest candle for real-time order-flow pressure
        val recent = candles.takeLast(6)
        var weightedBuy = 0.0
        var weightedTotal = 0.0
        recent.forEachIndexed { idx, c ->
            val weight = (idx + 1).toDouble()
            weightedBuy += c.takerBuyVolume * weight
            weightedTotal += c.volume * weight
        }
        val klineBuyerRatio = if (weightedTotal > 0.0) (weightedBuy / weightedTotal) else 0.5
        val finalBuyerRatio = if (explicitBuyerRatio != null) {
            (klineBuyerRatio * 0.5 + explicitBuyerRatio * 0.5)
        } else {
            klineBuyerRatio
        }

        val buyerPct = (finalBuyerRatio * 100.0).roundToInt().coerceIn(5, 95)
        val sellerPct = 100 - buyerPct

        val closes = candles.map { it.close }
        val emaFast = calculateEma(closes, 9)
        val emaSlow = calculateEma(closes, 21)
        val rsi14 = calculateRsi(closes, 14)

        // Combined directional score from real order-flow + EMA crossover + RSI
        var bullScore = 0.0
        bullScore += (buyerPct - 50) * 1.4
        if (emaFast >= emaSlow) bullScore += 14.0 else bullScore -= 14.0
        if (livePrice >= emaFast) bullScore += 10.0 else bullScore -= 10.0
        bullScore += (rsi14 - 50.0) * 0.45

        val direction = if (bullScore >= 0) MarketDirection.UP else MarketDirection.DOWN
        val confidence = (52 + abs(bullScore).roundToInt()).coerceIn(52, 96)

        return CryptoTickerAnalysis(
            asset = asset,
            timeframe = timeframe,
            priceUsd = livePrice,
            change24hPercent = change24h,
            buyerPercent = buyerPct,
            sellerPercent = sellerPct,
            direction = direction,
            confidencePercent = confidence,
            rsi14 = rsi14,
            emaFast = emaFast,
            emaSlow = emaSlow,
            candles = candles,
            dataSource = sourceName,
            updatedAtMillis = System.currentTimeMillis(),
            isCachedOffline = false
        )
    }

    private fun calculateEma(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = values.first()
        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
        }
        return ema
    }

    private fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size < 2) return 50.0
        val start = (closes.size - period - 1).coerceAtLeast(0)
        var gains = 0.0
        var losses = 0.0
        var count = 0
        for (i in (start + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gains += diff else losses += abs(diff)
            count++
        }
        if (count == 0) return 50.0
        val avgGain = gains / count
        val avgLoss = losses / count
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    private fun getJson(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "MC-Assistant-Android/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} for $url")
            }
            return response.body?.string() ?: error("Empty response body for $url")
        }
    }
}
