package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.ChannelBAmber
import com.example.ui.theme.FaderCapShadow
import com.example.ui.theme.FaderCapSilver
import com.example.ui.theme.FaderTrack
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.util.HapticFeedbackHelper
import kotlin.math.roundToInt

@Composable
fun StudioRackFader(
    bandLabel: String,
    levelDb: Int, // -15 to +15
    onLevelChanged: (Int) -> Unit,
    isChannelA: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isChannelA) ChannelACyan else ChannelBAmber
    val view = LocalView.current
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(56.dp)
            .testTag("fader_${bandLabel.replace(" ", "_")}_${if (isChannelA) "A" else "B"}")
    ) {
        // dB readout badge
        val formattedDb = if (levelDb > 0) "+$levelDb" else "$levelDb"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (enabled && levelDb != 0) accentColor.copy(alpha = 0.15f) else RackDarkBackground)
                .drawBehind {
                    if (enabled && levelDb != 0) {
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.4f),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                }
        ) {
            Text(
                text = "$formattedDb dB",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (!enabled) TextMuted else if (levelDb != 0) accentColor else TextSilver
            )
        }

        // Vertical Fader Track Area
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(52.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RackDarkBackground)
        ) {
            val totalHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            val knobHeightPx = with(density) { 36.dp.toPx() }
            val travelRangePx = totalHeightPx - knobHeightPx

            // Map levelDb (-15 to +15) to fader knob offset (0 to travelRangePx)
            // +15 is top (offset 0), -15 is bottom (offset travelRangePx), 0 is middle
            val normalizedRatio = 1.0f - ((levelDb + 15f) / 30f)
            val currentKnobY = normalizedRatio * travelRangePx

            var dragAccumulator by remember(levelDb) { mutableFloatStateOf(currentKnobY) }

            // Draw track slot and dB scale ticks
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val centerX = size.width / 2f

                // Draw side scale tick marks
                val tickMarks = listOf(
                    15 to "+15",
                    10 to "+10",
                    5 to "+5",
                    0 to "0",
                    -5 to "-5",
                    -10 to "-10",
                    -15 to "-15"
                )

                tickMarks.forEach { (tickDb, _) ->
                    val ratio = 1.0f - ((tickDb + 15f) / 30f)
                    val y = (knobHeightPx / 2f) + ratio * travelRangePx
                    val isZero = tickDb == 0
                    val tickColor = if (isZero) {
                        if (enabled) accentColor.copy(alpha = 0.8f) else TextMuted
                    } else {
                        RackBorder.copy(alpha = 0.7f)
                    }
                    val tickWidth = if (isZero) 12.dp.toPx() else 6.dp.toPx()

                    // Left ticks
                    drawLine(
                        color = tickColor,
                        start = Offset(centerX - 12.dp.toPx(), y),
                        end = Offset(centerX - 12.dp.toPx() + tickWidth, y),
                        strokeWidth = if (isZero) 2.dp.toPx() else 1.dp.toPx()
                    )

                    // Right ticks
                    drawLine(
                        color = tickColor,
                        start = Offset(centerX + 12.dp.toPx() - tickWidth, y),
                        end = Offset(centerX + 12.dp.toPx(), y),
                        strokeWidth = if (isZero) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Vertical metallic groove / fader slot
                drawRoundRect(
                    color = FaderTrack,
                    topLeft = Offset(centerX - 3.dp.toPx(), knobHeightPx / 2f),
                    size = Size(6.dp.toPx(), travelRangePx),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                // Center line groove
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX, knobHeightPx / 2f),
                    end = Offset(centerX, (knobHeightPx / 2f) + travelRangePx),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draggable Tactile Fader Knob
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                dragAccumulator = (offset.y - knobHeightPx / 2f).coerceIn(0f, travelRangePx)
                                val fraction = 1.0f - (dragAccumulator / travelRangePx)
                                val newDb = ((fraction * 30f) - 15f).roundToInt().coerceIn(-15, 15)
                                if (newDb != levelDb) {
                                    HapticFeedbackHelper.performFaderHaptic(context, newDb, levelDb, view)
                                    onLevelChanged(newDb)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator = (dragAccumulator + dragAmount).coerceIn(0f, travelRangePx)
                                val fraction = 1.0f - (dragAccumulator / travelRangePx)
                                val newDb = ((fraction * 30f) - 15f).roundToInt().coerceIn(-15, 15)
                                if (newDb != levelDb) {
                                    HapticFeedbackHelper.performFaderHaptic(context, newDb, levelDb, view)
                                    onLevelChanged(newDb)
                                }
                            }
                        )
                    }
            ) {
                // The physical metallic cap
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = currentKnobY.roundToInt()) }
                        .align(Alignment.TopCenter)
                        .width(42.dp)
                        .height(34.dp)
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FaderCapSilver,
                                    FaderCapShadow,
                                    FaderCapSilver.copy(alpha = 0.85f),
                                    Color(0xFF263238)
                                )
                            )
                        )
                        .drawBehind {
                            // Metallic side bevels
                            drawRect(
                                color = Color.White.copy(alpha = 0.35f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, 2.dp.toPx())
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(0f, size.height - 2.dp.toPx()),
                                size = Size(size.width, 2.dp.toPx())
                            )

                            // Horizontal grip ridges
                            val ridgeY1 = size.height * 0.28f
                            val ridgeY2 = size.height * 0.72f
                            drawLine(
                                color = Color.Black.copy(alpha = 0.4f),
                                start = Offset(6.dp.toPx(), ridgeY1),
                                end = Offset(size.width - 6.dp.toPx(), ridgeY1),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = Color.Black.copy(alpha = 0.4f),
                                start = Offset(6.dp.toPx(), ridgeY2),
                                end = Offset(size.width - 6.dp.toPx(), ridgeY2),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Illuminated Center Indicator Line (Neon Cyan / Amber)
                            val centerIndicatorY = size.height / 2f
                            drawLine(
                                color = if (enabled) accentColor else Color.Gray,
                                start = Offset(2.dp.toPx(), centerIndicatorY),
                                end = Offset(size.width - 2.dp.toPx(), centerIndicatorY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                )
            }
        }

        // Frequency Label
        Text(
            text = bandLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (bandLabel.contains("60")) Color(0xFFFF5252) else TextSilver,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
