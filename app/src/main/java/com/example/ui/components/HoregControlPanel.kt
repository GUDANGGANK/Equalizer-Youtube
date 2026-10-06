package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.HapticFeedbackHelper
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.ChannelBAmber
import com.example.ui.theme.HoregCrimson
import com.example.ui.theme.HoregRed
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.ui.theme.VuLedGreen

@Composable
fun HoregControlPanel(
    subBoost: Int, // 0..1000
    onSubBoostChanged: (Int) -> Unit,
    masterGain: Int, // 0..1000
    onMasterGainChanged: (Int) -> Unit,
    midHighBoost: Int, // 0..1000
    onMidHighBoostChanged: (Int) -> Unit,
    lowLatencyTwsEnabled: Boolean,
    onToggleLowLatencyTws: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RackSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(RackBorder, RackDarkBackground))),
        modifier = modifier
            .fillMaxWidth()
            .testTag("horeg_control_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Title
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (enabled && subBoost > 500) HoregCrimson else TextMuted)
                    )
                    Text(
                        text = "HOREG BASS & PRE-AMP DSP ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (enabled) HoregCrimson else TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                // 60Hz Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(HoregRed.copy(alpha = 0.2f))
                        .border(1.dp, HoregRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SUB 60 Hz",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HoregCrimson,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 1. 60Hz SUB BASS HOREG BOOST SLIDER
            ControlSliderRow(
                icon = Icons.Default.GraphicEq,
                iconColor = HoregCrimson,
                title = "60Hz SUB BASS HOREG",
                subtitle = "Bass Glerr & Hentakan Dada",
                valuePercent = subBoost / 10f,
                valueText = "${(subBoost / 10)}%",
                accentColor = HoregCrimson,
                sliderValue = subBoost.toFloat(),
                onValueChange = {
                    val intVal = it.toInt()
                    HapticFeedbackHelper.performSubBassHaptic(context, intVal, subBoost, view)
                    onSubBoostChanged(intVal)
                },
                enabled = enabled,
                testTag = "slider_sub_boost"
            )

            // 2. MASTER GAIN PRE-AMP (+0 to +12 dB)
            val gainDb = (masterGain / 1000f) * 12f
            val gainText = "+%.1f dB".format(gainDb)
            ControlSliderRow(
                icon = Icons.Default.VolumeUp,
                iconColor = ChannelBAmber,
                title = "GAIN PRE-AMP LOUDNESS",
                subtitle = "Tenaga Nendang Anti Distorsi",
                valuePercent = masterGain / 10f,
                valueText = gainText,
                accentColor = ChannelBAmber,
                sliderValue = masterGain.toFloat(),
                onValueChange = {
                    val intVal = it.toInt()
                    HapticFeedbackHelper.performGainHaptic(context, intVal, masterGain, view)
                    onMasterGainChanged(intVal)
                },
                enabled = enabled,
                testTag = "slider_master_gain"
            )

            // 3. MID-HIGH CLARITY & PRESENCE
            ControlSliderRow(
                icon = Icons.Default.Equalizer,
                iconColor = ChannelACyan,
                title = "MID-HIGH CLARITY",
                subtitle = "Vokal Jernih & Treble Renyah",
                valuePercent = midHighBoost / 10f,
                valueText = "${(midHighBoost / 10)}%",
                accentColor = ChannelACyan,
                sliderValue = midHighBoost.toFloat(),
                onValueChange = {
                    val intVal = it.toInt()
                    HapticFeedbackHelper.performGainHaptic(context, intVal, midHighBoost, view)
                    onMidHighBoostChanged(intVal)
                },
                enabled = enabled,
                testTag = "slider_mid_high"
            )

            // 4. ANTI-DELAY BLUETOOTH TWS LOW LATENCY SWITCH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RackSurfaceElevated)
                    .border(1.dp, if (lowLatencyTwsEnabled) VuLedGreen.copy(alpha = 0.4f) else RackBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val badgeColor by animateColorAsState(
                            targetValue = if (lowLatencyTwsEnabled) VuLedGreen else TextMuted,
                            label = "twsColor"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Low Latency TWS",
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ANTI-DELAY TWS BLUETOOTH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSilver
                                )
                                if (lowLatencyTwsEnabled) {
                                    Text(
                                        text = "< 10ms",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = VuLedGreen
                                    )
                                }
                            }
                            Text(
                                text = if (lowLatencyTwsEnabled)
                                    "Bypass delay filter • Sinkron suara video YouTube"
                                else
                                    "Mode normal (standar buffer audio)",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = lowLatencyTwsEnabled,
                        onCheckedChange = { onToggleLowLatencyTws() },
                        enabled = enabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VuLedGreen,
                            checkedTrackColor = VuLedGreen.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = RackDarkBackground
                        ),
                        modifier = Modifier.testTag("switch_low_latency_tws")
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    valuePercent: Float,
    valueText: String,
    accentColor: Color,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    testTag: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) iconColor else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) TextSilver else TextMuted
                    )
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(RackDarkBackground)
                    .border(0.5.dp, if (enabled) accentColor.copy(alpha = 0.5f) else RackBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (enabled) accentColor else TextMuted
                )
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = 0f..1000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = RackDarkBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag(testTag)
        )
    }
}
