package com.example.data.model

enum class CryptoTimeframe(val apiInterval: String, val labelEn: String, val labelBn: String) {
    M5("5m", "5m Live", "৫মি লাইভ"),
    M15("15m", "15m", "১৫মি"),
    H1("1h", "1h", "১ঘণ্টা")
}

enum class CryptoAsset(
    val symbol: String,
    val displayName: String,
    val binancePair: String,
    val krakenPair: String
) {
    BTC("BTC", "Bitcoin", "BTCUSDT", "XBTUSD"),
    ETH("ETH", "Ethereum", "ETHUSDT", "ETHUSD"),
    XRP("XRP", "XRP", "XRPUSDT", "XRPUSD"),
    BNB("BNB", "BNB", "BNBUSDT", "BNBUSD"),
    DOGE("DOGE", "Dogecoin", "DOGEUSDT", "XDGUSD"),
    SOL("SOL", "Solana", "SOLUSDT", "SOLUSD")
}

enum class MarketDirection {
    UP,
    DOWN
}

data class CandlePoint(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val takerBuyVolume: Double
)

data class CryptoTickerAnalysis(
    val asset: CryptoAsset,
    val timeframe: CryptoTimeframe,
    val priceUsd: Double,
    val change24hPercent: Double,
    val buyerPercent: Int,
    val sellerPercent: Int,
    val direction: MarketDirection,
    val confidencePercent: Int,
    val rsi14: Double,
    val emaFast: Double,
    val emaSlow: Double,
    val candles: List<CandlePoint>,
    val dataSource: String,
    val updatedAtMillis: Long,
    val isCachedOffline: Boolean = false
)

enum class MessageSender {
    USER,
    MC
}

data class AssistantMessage(
    val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineReply: Boolean = false,
    val hasAttachment: Boolean = false
)

data class SavedCodeSnippet(
    val id: Long = 0,
    val title: String,
    val language: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)
