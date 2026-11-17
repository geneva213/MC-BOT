package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AppLanguage
import com.example.core.McLocalization
import com.example.data.model.CryptoTickerAnalysis
import com.example.data.model.CryptoTimeframe
import com.example.data.model.MarketDirection
import com.example.ui.components.BullBearOrderFlowBar
import com.example.ui.components.MiniCandleSparkline
import com.example.ui.components.StatusPill
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CryptoUiState
import com.example.ui.viewmodel.McViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CryptoTradingScreen(
    viewModel: McViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isEffectiveOnline.collectAsStateWithLifecycle()
    val timeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val cryptoState by viewModel.cryptoState.collectAsStateWithLifecycle()
    val btc5mLive by viewModel.btc5mLive.collectAsStateWithLifecycle()
    val analysisPopup by viewModel.marketAnalysisPopup.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isMarketAnalyzing.collectAsStateWithLifecycle()

    val strings = remember(settings.language) { McLocalization.get(settings.language) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Header + Live BTC 5-Min Signal Card
            item {
                FeaturedBitcoin5mCard(
                    btc5m = btc5mLive,
                    title = strings.btc5mSignalTitle,
                    buyersLabel = strings.buyersLabel,
                    sellersLabel = strings.sellersLabel,
                    upText = strings.upDirection,
                    downText = strings.downDirection,
                    isOnline = isOnline,
                    onAnalyzeBtc = { btc5mLive?.let { viewModel.analyzeMarketWithMc(it) } },
                    onShareBtc = { btc5mLive?.let { viewModel.shareCryptoAnalysis(it) } }
                )
            }

            // Offline Banner if applicable
            if (!isOnline) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, NeonAmber)
                    ) {
                        Text(
                            text = strings.cachedOfflineBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonAmber,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }

            // Controls: Timeframe Selector (5m, 15m, 1h) + Ask MC Full Market + Share + Refresh
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.liveMarketTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.refreshCryptoMarket(showLoader = true) },
                                modifier = Modifier.testTag("refresh_crypto_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.refreshMarket,
                                    tint = NeonCyan
                                )
                            }
                            IconButton(
                                onClick = { viewModel.shareAllMarketSnapshot() },
                                modifier = Modifier.testTag("share_all_market_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = strings.shareSignal,
                                    tint = NeonEmerald
                                )
                            }
                        }
                    }

                    // 5m / 15m / 1h Timeframe Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CryptoTimeframe.entries.forEach { tf ->
                            val selected = tf == timeframe
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) NeonCyan else ObsidianCard,
                                border = BorderStroke(1.dp, if (selected) NeonCyan else ObsidianBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectTimeframe(tf) }
                                    .testTag("timeframe_${tf.apiInterval}")
                            ) {
                                Text(
                                    text = if (settings.language == AppLanguage.BANGLA) tf.labelBn else tf.labelEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ObsidianVoid else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Ask MC Full Market Bull/Bear Analysis Button
                    Button(
                        onClick = { viewModel.analyzeMarketWithMc(null) },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonViolet,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_full_market_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = strings.askMcAnalysis,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${strings.askMcAnalysis} (${timeframe.apiInterval.uppercase(Locale.US)})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Asset Cards (BTC, ETH, XRP, BNB, DOGE, SOL)
            when (val state = cryptoState) {
                is CryptoUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonCyan)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Fetching live order-flow & candles...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                is CryptoUiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NeonCoral.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, NeonCoral)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NeonCoral
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { viewModel.refreshCryptoMarket(true) }) {
                                    Text("Retry Live Feed")
                                }
                            }
                        }
                    }
                }
                is CryptoUiState.Ready -> {
                    items(state.tickers, key = { "${it.asset.symbol}_${it.timeframe.apiInterval}" }) { ticker ->
                        CryptoAssetSignalCard(
                            ticker = ticker,
                            buyersLabel = strings.buyersLabel,
                            sellersLabel = strings.sellersLabel,
                            upText = strings.upDirection,
                            downText = strings.downDirection,
                            askMcLabel = strings.askMcAnalysis,
                            shareLabel = strings.shareSignal,
                            onAskMc = { viewModel.analyzeMarketWithMc(ticker) },
                            onShare = { viewModel.shareCryptoAnalysis(ticker) }
                        )
                    }
                }
            }
        }
    }

    // MC Market Analysis Popup Dialog
    if (analysisPopup != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMarketAnalysisPopup() },
            containerColor = ObsidianCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                    Text(
                        text = "MC Live Market Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = analysisPopup.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissMarketAnalysisPopup() }) {
                    Text("OK", color = NeonCyan)
                }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = { viewModel.voiceEngine.speak(analysisPopup.orEmpty()) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Analysis",
                            tint = NeonEmerald
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.deviceControl.shareText(
                                "MC Crypto Market Analysis",
                                analysisPopup.orEmpty()
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Analysis",
                            tint = NeonCyan
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FeaturedBitcoin5mCard(
    btc5m: CryptoTickerAnalysis?,
    title: String,
    buyersLabel: String,
    sellersLabel: String,
    upText: String,
    downText: String,
    isOnline: Boolean,
    onAnalyzeBtc: () -> Unit,
    onShareBtc: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_btc_5m_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_trading_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.22f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianVoid.copy(alpha = 0.45f),
                                ObsidianCard.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = if (btc5m?.isCachedOffline == true || !isOnline) "CACHED" else "5M LIVE",
                        accentColor = if (btc5m?.isCachedOffline == true || !isOnline) NeonAmber else NeonEmerald
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (btc5m == null) {
                    Text(
                        text = "Syncing live Bitcoin 5-minute order flow...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    val isUp = btc5m.direction == MarketDirection.UP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", btc5m.priceUsd)}",
                                style = MaterialTheme.typography.displaySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "24h ${String.format(Locale.US, "%+.2f%%", btc5m.change24hPercent)} • RSI ${String.format(Locale.US, "%.1f", btc5m.rsi14)} • Conf ${btc5m.confidencePercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (btc5m.change24hPercent >= 0) NeonEmerald else NeonCoral
                            )
                        }

                        Icon(
                            imageVector = if (isUp) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = "BTC Direction",
                            tint = if (isUp) NeonEmerald else NeonCoral,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MiniCandleSparkline(
                        candles = btc5m.candles,
                        isBullish = isUp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BullBearOrderFlowBar(
                        buyerPercent = btc5m.buyerPercent,
                        sellerPercent = btc5m.sellerPercent,
                        buyersLabel = buyersLabel,
                        sellersLabel = sellersLabel,
                        direction = btc5m.direction,
                        upText = upText,
                        downText = downText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAnalyzeBtc,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "MC 5m Analysis",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onShareBtc,
                            border = BorderStroke(1.dp, NeonEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share BTC Signal",
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoAssetSignalCard(
    ticker: CryptoTickerAnalysis,
    buyersLabel: String,
    sellersLabel: String,
    upText: String,
    downText: String,
    askMcLabel: String,
    shareLabel: String,
    onAskMc: () -> Unit,
    onShare: () -> Unit
) {
    val isUp = ticker.direction == MarketDirection.UP
    val timeText = remember(ticker.updatedAtMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ticker.updatedAtMillis))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (isUp) NeonEmerald.copy(alpha = 0.45f) else NeonCoral.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crypto_card_${ticker.asset.symbol}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ticker.asset.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "${ticker.asset.displayName} (${ticker.timeframe.apiInterval.uppercase(Locale.US)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${ticker.dataSource} • $timeText • RSI ${String.format(Locale.US, "%.1f", ticker.rsi14)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val priceStr = when {
                        ticker.priceUsd >= 1000.0 -> String.format(Locale.US, "$%,.2f", ticker.priceUsd)
                        ticker.priceUsd >= 1.0 -> String.format(Locale.US, "$%.4f", ticker.priceUsd)
                        else -> String.format(Locale.US, "$%.5f", ticker.priceUsd)
                    }
                    Text(
                        text = priceStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%+.2f%% (24h)", ticker.change24hPercent),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ticker.change24hPercent >= 0) NeonEmerald else NeonCoral
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MiniCandleSparkline(
                candles = ticker.candles,
                isBullish = isUp
            )

            Spacer(modifier = Modifier.height(10.dp))

            BullBearOrderFlowBar(
                buyerPercent = ticker.buyerPercent,
                sellerPercent = ticker.sellerPercent,
                buyersLabel = buyersLabel,
                sellersLabel = sellersLabel,
                direction = ticker.direction,
                upText = upText,
                downText = downText
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAskMc,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(askMcLabel, style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                }

                OutlinedButton(
                    onClick = onShare,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = shareLabel,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(shareLabel, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
            }
        }
    }
}
