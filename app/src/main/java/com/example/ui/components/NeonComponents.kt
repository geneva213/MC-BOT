package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CandlePoint
import com.example.data.model.MarketDirection
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCoralDim
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldDim
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusPill(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bg = accentColor.copy(alpha = 0.14f)
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NeonPulseOrb(
    isListening: Boolean,
    isWaking: Boolean,
    size: Dp = 76.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mc_orb_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (isListening || isWaking) 1.16f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening || isWaking) 650 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringColor = when {
        isWaking -> NeonEmerald
        isListening -> NeonCyan
        else -> NeonCyan.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .size(size + 18.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("mc_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (this.size.minDimension / 2f) * (pulseScale / 1.16f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ringColor.copy(alpha = 0.45f), Color.Transparent)
                ),
                radius = radius
            )
            drawCircle(
                color = ringColor,
                radius = (this.size.minDimension / 2f) - 6f,
                style = Stroke(width = if (isListening || isWaking) 4.dp.toPx() else 2.dp.toPx())
            )
        }

        Image(
            painter = painterResource(id = R.drawable.img_mc_avatar),
            contentDescription = "MC AI Assistant Core Orb",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun BullBearOrderFlowBar(
    buyerPercent: Int,
    sellerPercent: Int,
    buyersLabel: String,
    sellersLabel: String,
    direction: MarketDirection,
    upText: String,
    downText: String,
    modifier: Modifier = Modifier
) {
    val isUp = direction == MarketDirection.UP
    val dirColor = if (isUp) NeonEmerald else NeonCoral
    val dirBg = if (isUp) NeonEmeraldDim else NeonCoralDim

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$buyersLabel $buyerPercent%",
                style = MaterialTheme.typography.labelLarge,
                color = NeonEmerald,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = dirBg,
                border = BorderStroke(1.dp, dirColor.copy(alpha = 0.7f))
            ) {
                Text(
                    text = if (isUp) upText else downText,
                    style = MaterialTheme.typography.labelMedium,
                    color = dirColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = "$sellersLabel $sellerPercent%",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCoral,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(ObsidianVoid)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(50))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val buyWeight = buyerPercent.coerceIn(5, 95).toFloat()
                val sellWeight = sellerPercent.coerceIn(5, 95).toFloat()
                Box(
                    modifier = Modifier
                        .weight(buyWeight)
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonCyan, NeonEmerald)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(sellWeight)
                        .fillMaxSize()
                        .background(NeonCoral)
                )
            }
        }
    }
}

@Composable
fun MiniCandleSparkline(
    candles: List<CandlePoint>,
    isBullish: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isBullish) NeonEmerald else NeonCoral
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianVoid.copy(alpha = 0.65f))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        if (candles.size < 2) return@Canvas
        val minLow = candles.minOf { it.low }
        val maxHigh = candles.maxOf { it.high }
        val range = (maxHigh - minLow).coerceAtLeast(1e-8)
        val stepX = size.width / (candles.size - 1).coerceAtLeast(1)

        // Draw subtle candle bodies
        val candleWidth = (stepX * 0.48f).coerceIn(3f, 12f)
        candles.forEachIndexed { i, c ->
            val x = i * stepX
            val yHigh = size.height - (((c.high - minLow) / range) * size.height).toFloat()
            val yLow = size.height - (((c.low - minLow) / range) * size.height).toFloat()
            val yOpen = size.height - (((c.open - minLow) / range) * size.height).toFloat()
            val yClose = size.height - (((c.close - minLow) / range) * size.height).toFloat()
            val cColor = if (c.close >= c.open) NeonEmerald.copy(alpha = 0.45f) else NeonCoral.copy(alpha = 0.45f)

            drawLine(
                color = cColor,
                start = Offset(x, yHigh),
                end = Offset(x, yLow),
                strokeWidth = 1.5f
            )
            val top = minOf(yOpen, yClose)
            val bottom = maxOf(yOpen, yClose).coerceAtLeast(top + 2f)
            drawRect(
                color = cColor,
                topLeft = Offset(x - candleWidth / 2f, top),
                size = androidx.compose.ui.geometry.Size(candleWidth, bottom - top)
            )
        }

        // Draw crisp close price trend line on top
        val path = Path()
        candles.forEachIndexed { i, c ->
            val x = i * stepX
            val y = size.height - (((c.close - minLow) / range) * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SectionTerminalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ObsidianBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
