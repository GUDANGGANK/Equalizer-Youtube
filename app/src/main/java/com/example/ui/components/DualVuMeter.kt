package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VuLedGreen
import com.example.ui.theme.VuLedOff
import com.example.ui.theme.VuLedRed
import com.example.ui.theme.VuLedYellow

@Composable
fun DualVuMeter(
    levelA: Float, // 0.0 to 1.0
    levelB: Float,
    peakA: Float,
    peakB: Float,
    spectrumLevels: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    isYouTubeSynced: Boolean = false,
    hasAudioPermission: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RackDarkBackground)
            .border(1.dp, RackBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("dual_vu_meter")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header / Scale + Real-time YouTube Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusDotColor by animateColorAsState(
                        targetValue = when {
                            !enabled -> TextMuted
                            !hasAudioPermission -> VuLedYellow
                            isYouTubeSynced -> VuLedGreen
                            else -> ChannelACyan
                        },
                        label = "dotColor"
                    )

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )

                    Text(
                        text = when {
                            !enabled -> "EQUALIZER STANDBY"
                            !hasAudioPermission -> "PERLU IZIN AUDIO"
                            isYouTubeSynced -> "YOUTUBE SYNCED [LIVE]"
                            else -> "MONITOR SISTEM AKTIF"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusDotColor,
                        letterSpacing = 0.5.sp
                    )
                }

                // dB tick marks header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("-20", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("-12", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("-6", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("0 dB", fontSize = 8.sp, color = VuLedYellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("+3 PEAK", fontSize = 8.sp, color = VuLedRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Real-time FFT Frequency Spectrum Indicator Bar
            SpectrumMiniBars(
                levels = spectrumLevels,
                enabled = enabled && hasAudioPermission
            )

            // Channel A LED bar
            VuChannelRow(
                channelLabel = "A",
                labelColor = ChannelACyan,
                level = if (enabled) levelA else 0f,
                peak = if (enabled) peakA else 0f
            )

            // Channel B LED bar
            VuChannelRow(
                channelLabel = "B",
                labelColor = ChannelBAmber,
                level = if (enabled) levelB else 0f,
                peak = if (enabled) peakB else 0f
            )
        }
    }
}

@Composable
private fun SpectrumMiniBars(
    levels: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val labels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
    val colors = listOf(HoregCrimson, ChannelBAmber, ChannelACyan, VuLedGreen, ChannelACyan)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(RackSurfaceElevated)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { i, label ->
            val level = if (enabled) levels.getOrElse(i) { 0f }.coerceIn(0f, 1f) else 0f
            val color = colors.getOrElse(i) { ChannelACyan }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (label == "60Hz") HoregCrimson else TextMuted
                )
                // Mini 4-dot ladder
                Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    val activeDots = (level * 4).toInt()
                    for (dot in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 8.dp)
                                .clip(RoundedCornerShape(0.5.dp))
                                .background(
                                    if (dot < activeDots) color else VuLedOff
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VuChannelRow(
    channelLabel: String,
    labelColor: Color,
    level: Float,
    peak: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Channel Tag
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(22.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(labelColor.copy(alpha = 0.2f))
                .border(0.5.dp, labelColor, RoundedCornerShape(2.dp))
        ) {
            Text(
                text = channelLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = labelColor
            )
        }

        // 20-segment LED ladder
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            val totalSegments = 20
            val gap = 2.dp.toPx()
            val segmentWidth = (size.width - (totalSegments - 1) * gap) / totalSegments
            val activeSegments = (level.coerceIn(0f, 1f) * totalSegments).toInt()
            val peakSegment = (peak.coerceIn(0f, 1f) * totalSegments).toInt().coerceAtMost(totalSegments - 1)

            for (i in 0 until totalSegments) {
                val left = i * (segmentWidth + gap)
                val isActive = i < activeSegments
                val isPeakHold = i == peakSegment && peakSegment > 0

                val segmentColor = when {
                    i >= 17 -> VuLedRed       // Red Clip
                    i >= 13 -> VuLedYellow    // Amber -6 to 0 dB
                    else -> VuLedGreen        // Green Normal
                }

                val fill = when {
                    isActive -> segmentColor
                    isPeakHold -> segmentColor.copy(alpha = 0.95f)
                    else -> VuLedOff
                }

                drawRoundRect(
                    color = fill,
                    topLeft = Offset(left, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }
    }
}
