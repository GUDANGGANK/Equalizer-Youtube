package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.ChannelBAmber
import com.example.ui.theme.HoregCrimson
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.ui.theme.VuLedGreen
import com.example.ui.theme.VuLedOff
import com.example.ui.theme.VuLedYellow

enum class SpectrumMode {
    BARS,
    CURVE
}

@Composable
fun RealtimeSpectrumVisualizer(
    bands: List<Float>, // 24 frequency levels (0.0 to 1.0)
    peaks: List<Float>, // 24 peak hold points
    isProcessing: Boolean = true,
    hasAudioPermission: Boolean = true,
    activeMediaSessionId: Int? = null,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(SpectrumMode.BARS) }

    // Identify dominant peak frequency
    val maxBandIndex = remember(bands) {
        val maxVal = bands.maxOrNull() ?: 0f
        if (maxVal > 0.05f) bands.indexOf(maxVal) else -1
    }

    val dominantFrequencyLabel = remember(maxBandIndex) {
        when (maxBandIndex) {
            -1 -> "STANDBY"
            0 -> "30 Hz [SUB]"
            1 -> "45 Hz [SUB]"
            2 -> "60 Hz [SUB HOREG GLERR]"
            3 -> "80 Hz [BASS PUNCH]"
            4 -> "120 Hz [LOW-BASS]"
            5 -> "180 Hz [MID-BASS]"
            6 -> "250 Hz [WARMTH]"
            7 -> "350 Hz [BODY]"
            8 -> "500 Hz [LOW-MID]"
            9 -> "700 Hz [MID]"
            10 -> "1.0 kHz [VOCAL CORE]"
            11 -> "1.4 kHz [VOCAL MID]"
            12 -> "2.0 kHz [ATTACK]"
            13 -> "2.8 kHz [CLARITY]"
            14 -> "4.0 kHz [PRESENCE]"
            15 -> "5.6 kHz [DETAIL]"
            16 -> "8.0 kHz [BRILLIANCE]"
            17 -> "11.0 kHz [AIR]"
            18 -> "14.0 kHz [SPARKLE]"
            19 -> "16.0 kHz [SHIMMER]"
            else -> "20.0 kHz [ULTRA-HIGH]"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RackSurface)
            .border(1.dp, RackBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("realtime_spectrum_visualizer")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Title + Live Peak Readout + Mode Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProcessing && hasAudioPermission) VuLedGreen else VuLedYellow
                            )
                    )
                    Text(
                        text = "LIVE FREQUENCY SPECTRUM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = TextSilver
                    )
                }

                // Mode switch: BARS vs CURVE
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeChip(
                        label = "BARS",
                        icon = Icons.Default.GraphicEq,
                        isSelected = displayMode == SpectrumMode.BARS,
                        onClick = { displayMode = SpectrumMode.BARS }
                    )
                    ModeChip(
                        label = "CURVE",
                        icon = Icons.Default.ShowChart,
                        isSelected = displayMode == SpectrumMode.CURVE,
                        onClick = { displayMode = SpectrumMode.CURVE }
                    )
                }
            }

            // Real-time Peak Readout & Session Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(RackDarkBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DOMINANT PEAK:",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Text(
                        text = dominantFrequencyLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (maxBandIndex in 0..3) HoregCrimson else ChannelACyan
                    )
                }

                Text(
                    text = if (activeMediaSessionId != null)
                        "SESSION #$activeMediaSessionId [MEDIA OUT]"
                    else
                        "24-BAND FFT • LIVE",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = VuLedGreen
                )
            }

            // Main Canvas Graphic Analyzer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RackDarkBackground)
                    .border(0.5.dp, RackBorder, RoundedCornerShape(8.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 1. Draw dB Grid lines (-36dB, -24dB, -12dB, -6dB, 0dB)
                    val dbLevels = listOf(0.15f, 0.35f, 0.60f, 0.80f, 0.98f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)

                    for (fraction in dbLevels) {
                        val y = canvasHeight * (1f - fraction)
                        drawLine(
                            color = RackBorder.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    val totalBands = 24
                    val clampedBands = List(totalBands) { idx ->
                        if (isProcessing && hasAudioPermission) {
                            bands.getOrElse(idx) { 0f }.coerceIn(0f, 1f)
                        } else 0f
                    }
                    val clampedPeaks = List(totalBands) { idx ->
                        if (isProcessing && hasAudioPermission) {
                            peaks.getOrElse(idx) { 0f }.coerceIn(0f, 1f)
                        } else 0f
                    }

                    if (displayMode == SpectrumMode.BARS) {
                        // Render 24-band gradient neon bars with floating peak caps
                        val gap = 2.5.dp.toPx()
                        val barWidth = (canvasWidth - (totalBands - 1) * gap) / totalBands

                        for (i in 0 until totalBands) {
                            val level = clampedBands[i]
                            val peak = clampedPeaks[i]
                            val x = i * (barWidth + gap)
                            val barHeight = (level * canvasHeight).coerceAtLeast(2.dp.toPx())
                            val y = canvasHeight - barHeight

                            // Color selection based on audio frequency character
                            val (colorBottom, colorTop) = when {
                                i <= 2 -> Pair(HoregCrimson.copy(alpha = 0.5f), HoregCrimson) // Sub-Bass Horeg
                                i in 3..6 -> Pair(ChannelBAmber.copy(alpha = 0.4f), ChannelBAmber) // Bass
                                i in 7..13 -> Pair(ChannelACyan.copy(alpha = 0.4f), ChannelACyan) // Mids
                                i in 14..18 -> Pair(VuLedGreen.copy(alpha = 0.4f), VuLedGreen) // Presence
                                else -> Pair(Color(0xFF7C4DFF).copy(alpha = 0.4f), Color(0xFFB388FF)) // Air / Highs
                            }

                            val barBrush = Brush.verticalGradient(
                                colors = listOf(colorTop, colorBottom),
                                startY = y,
                                endY = canvasHeight
                            )

                            // Draw rounded bar
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )

                            // Draw floating peak hold cap
                            if (peak > 0.05f) {
                                val peakY = (canvasHeight * (1f - peak)).coerceIn(1f, canvasHeight - 2f)
                                drawRoundRect(
                                    color = colorTop,
                                    topLeft = Offset(x, peakY - 1.5.dp.toPx()),
                                    size = Size(barWidth, 2.5.dp.toPx()),
                                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                )
                            }
                        }
                    } else {
                        // Render continuous spline area curve
                        val points = mutableListOf<Offset>()
                        val stepX = canvasWidth / (totalBands - 1)

                        for (i in 0 until totalBands) {
                            val x = i * stepX
                            val level = clampedBands[i]
                            val y = canvasHeight - (level * canvasHeight).coerceAtLeast(2.dp.toPx())
                            points.add(Offset(x, y))
                        }

                        // Build smooth path
                        val curvePath = Path()
                        val fillPath = Path()

                        if (points.isNotEmpty()) {
                            curvePath.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(points.first().x, canvasHeight)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val ctrlX1 = p0.x + (p1.x - p0.x) / 2f
                                val ctrlY1 = p0.y
                                val ctrlX2 = p0.x + (p1.x - p0.x) / 2f
                                val ctrlY2 = p1.y

                                curvePath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, p1.x, p1.y)
                                fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, p1.x, p1.y)
                            }

                            fillPath.lineTo(points.last().x, canvasHeight)
                            fillPath.close()

                            // Fill under the curve with glowing gradient
                            val areaBrush = Brush.verticalGradient(
                                colors = listOf(
                                    HoregCrimson.copy(alpha = 0.55f),
                                    ChannelACyan.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = canvasHeight
                            )
                            drawPath(fillPath, brush = areaBrush)

                            // Stroke along the top
                            val strokeBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    HoregCrimson,
                                    ChannelBAmber,
                                    ChannelACyan,
                                    VuLedGreen,
                                    Color(0xFFB388FF)
                                )
                            )
                            drawPath(
                                curvePath,
                                brush = strokeBrush,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw peak circles on active peaks
                            for (p in points) {
                                if (canvasHeight - p.y > 10.dp.toPx()) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = p
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Axis scale markers along the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FreqAxisLabel("30Hz")
                FreqAxisLabel("60Hz [SUB]", isHighlighted = true)
                FreqAxisLabel("120Hz")
                FreqAxisLabel("250Hz")
                FreqAxisLabel("500Hz")
                FreqAxisLabel("1kHz")
                FreqAxisLabel("2kHz")
                FreqAxisLabel("4kHz")
                FreqAxisLabel("8kHz")
                FreqAxisLabel("16kHz")
            }
        }
    }
}

@Composable
private fun FreqAxisLabel(
    label: String,
    isHighlighted: Boolean = false
) {
    Text(
        text = label,
        fontSize = 7.5.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Normal,
        color = if (isHighlighted) HoregCrimson else TextMuted
    )
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ChannelACyan.copy(alpha = 0.25f) else RackDarkBackground,
        label = "modeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ChannelACyan else RackBorder,
        label = "modeBorder"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ChannelACyan else TextMuted,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) ChannelACyan else TextMuted
        )
    }
}
