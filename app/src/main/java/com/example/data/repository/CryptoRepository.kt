package com.example.data.repository

import com.example.data.local.CryptoSnapshotDao
import com.example.data.local.CryptoSnapshotEntity
import com.example.data.model.CandlePoint
import com.example.data.model.CryptoAsset
import com.example.data.model.CryptoTickerAnalysis
import com.example.data.model.CryptoTimeframe
import com.example.data.model.MarketDirection
import com.example.data.remote.CryptoMarketService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CryptoRepository(
    private val service: CryptoMarketService,
    private val snapshotDao: CryptoSnapshotDao
) {

    suspend fun fetchLiveMarket(
        timeframe: CryptoTimeframe,
        offlineMode: Boolean
    ): Result<List<CryptoTickerAnalysis>> {
        if (offlineMode) {
            val cached = loadCached(timeframe)
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("Offline mode active and no cached snapshot yet."))
            }
        }

        return try {
            val results = coroutineScope {
                CryptoAsset.entries.map { asset ->
                    async {
                        runCatching { service.fetchAssetAnalysis(asset, timeframe) }.getOrNull()
                    }
                }.awaitAll().filterNotNull()
            }

            if (results.isNotEmpty()) {
                snapshotDao.upsertAll(results.map { it.toEntity() })
                Result.success(results)
            } else {
                val cached = loadCached(timeframe)
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(IllegalStateException("Unable to reach live crypto exchanges."))
                }
            }
        } catch (e: Exception) {
            val cached = loadCached(timeframe)
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchBitcoin5mLive(offlineMode: Boolean): CryptoTickerAnalysis? {
        if (offlineMode) {
            return loadCached(CryptoTimeframe.M5).firstOrNull { it.asset == CryptoAsset.BTC }
        }
        return runCatching {
            val btc = service.fetchAssetAnalysis(CryptoAsset.BTC, CryptoTimeframe.M5)
            snapshotDao.upsertAll(listOf(btc.toEntity()))
            btc
        }.getOrElse {
            loadCached(CryptoTimeframe.M5).firstOrNull { it.asset == CryptoAsset.BTC }
        }
    }

    private suspend fun loadCached(timeframe: CryptoTimeframe): List<CryptoTickerAnalysis> {
        return snapshotDao.getByTimeframe(timeframe.apiInterval).mapNotNull { entity ->
            val asset = CryptoAsset.entries.firstOrNull { it.symbol == entity.symbol } ?: return@mapNotNull null
            val tf = CryptoTimeframe.entries.firstOrNull { it.apiInterval == entity.timeframe } ?: timeframe
            val closes = entity.sparklineClosesCsv
                .split(",")
                .mapNotNull { it.toDoubleOrNull() }
            val candles = closes.mapIndexed { idx, close ->
                CandlePoint(
                    openTime = entity.updatedAtMillis - ((closes.size - idx) * 300_000L),
                    open = close,
                    high = close,
                    low = close,
                    close = close,
                    volume = 1.0,
                    takerBuyVolume = entity.buyerPercent / 100.0
                )
            }
            CryptoTickerAnalysis(
                asset = asset,
                timeframe = tf,
                priceUsd = entity.priceUsd,
                change24hPercent = entity.change24hPercent,
                buyerPercent = entity.buyerPercent,
                sellerPercent = entity.sellerPercent,
                direction = if (entity.direction == MarketDirection.UP.name) MarketDirection.UP else MarketDirection.DOWN,
                confidencePercent = entity.confidencePercent,
                rsi14 = entity.rsi14,
                emaFast = entity.emaFast,
                emaSlow = entity.emaSlow,
                candles = candles,
                dataSource = entity.dataSource,
                updatedAtMillis = entity.updatedAtMillis,
                isCachedOffline = true
            )
        }
    }

    private fun CryptoTickerAnalysis.toEntity(): CryptoSnapshotEntity {
        val closesCsv = candles.takeLast(24).joinToString(",") { it.close.toString() }
        return CryptoSnapshotEntity(
            symbol = asset.symbol,
            timeframe = timeframe.apiInterval,
            priceUsd = priceUsd,
            change24hPercent = change24hPercent,
            buyerPercent = buyerPercent,
            sellerPercent = sellerPercent,
            direction = direction.name,
            confidencePercent = confidencePercent,
            rsi14 = rsi14,
            emaFast = emaFast,
            emaSlow = emaSlow,
            sparklineClosesCsv = closesCsv,
            dataSource = dataSource,
            updatedAtMillis = updatedAtMillis
        )
    }
}
