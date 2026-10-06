package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.BandInfo
import com.example.ui.ChannelSelection
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.ChannelBAmber
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackScrew
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.ui.theme.VuLedGreen

@Composable
fun FaderConsole(
    bands: List<BandInfo>,
    channelABands: List<Int>,
    channelBBands: List<Int>,
    isStereoLinked: Boolean,
    selectedChannel: ChannelSelection,
    onSelectChannel: (ChannelSelection) -> Unit,
    onToggleStereoLink: () -> Unit,
    onBandAChanged: (Int, Int) -> Unit,
    onBandBChanged: (Int, Int) -> Unit,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RackSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(RackBorder, RackDarkBackground))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fader_console_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rack Faceplate Top Bar: Power + Screws + Master Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Hex Screw + Power Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RackScrewGraphic()

                    // Power Button
                    IconButton(
                        onClick = onTogglePower,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) VuLedGreen.copy(alpha = 0.2f) else RackSurfaceElevated)
                            .border(
                                1.5.dp,
                                if (isEnabled) VuLedGreen else TextMuted,
                                CircleShape
                            )
                            .testTag("btn_power_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (isEnabled) VuLedGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "HOREG EQUALIZER RACK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextSilver,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isEnabled) "DSP ACTIVE • 60Hz TUNED" else "BYPASS (STANDBY)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) VuLedGreen else TextMuted
                        )
                    }
                }

                // Stereo Link Switch + Right Screw
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stereo Link Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isStereoLinked) ChannelACyan.copy(alpha = 0.2f) else RackDarkBackground)
                            .border(
                                1.dp,
                                if (isStereoLinked) ChannelACyan else RackBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = isEnabled, onClick = onToggleStereoLink)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("btn_stereo_link")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isStereoLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = "Stereo Link",
                                tint = if (isStereoLinked) ChannelACyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isStereoLinked) "LINK A/B" else "UNLINK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isStereoLinked) ChannelACyan else TextMuted
                            )
                        }
                    }

                    RackScrewGraphic()
                }
            }

            // Channel Tabs Selector (Channel A / Channel B / Dual View)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RackDarkBackground)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Channel A
                ChannelTabButton(
                    title = "CHANNEL A (LEFT)",
                    accentColor = ChannelACyan,
                    isSelected = selectedChannel == ChannelSelection.CHANNEL_A,
                    onClick = { onSelectChannel(ChannelSelection.CHANNEL_A) },
                    modifier = Modifier.weight(1f),
                    testTag = "tab_channel_a"
                )

                // Tab Channel B
                ChannelTabButton(
                    title = "CHANNEL B (RIGHT)",
                    accentColor = ChannelBAmber,
                    isSelected = selectedChannel == ChannelSelection.CHANNEL_B,
                    onClick = { onSelectChannel(ChannelSelection.CHANNEL_B) },
                    modifier = Modifier.weight(1f),
                    testTag = "tab_channel_b"
                )

                // Tab Dual View
                ChannelTabButton(
                    title = "DUAL A+B",
                    accentColor = Color.White,
                    isSelected = selectedChannel == ChannelSelection.DUAL_VIEW,
                    onClick = { onSelectChannel(ChannelSelection.DUAL_VIEW) },
                    modifier = Modifier.weight(0.8f),
                    testTag = "tab_dual_view"
                )
            }

            // Fader Bank Area
            when (selectedChannel) {
                ChannelSelection.CHANNEL_A -> {
                    FaderStripRow(
                        bands = bands,
                        bandLevels = channelABands,
                        onLevelChanged = onBandAChanged,
                        isChannelA = true,
                        enabled = isEnabled,
                        channelBadge = "CH A (LEFT) - 60Hz HOREG"
                    )
                }
                ChannelSelection.CHANNEL_B -> {
                    FaderStripRow(
                        bands = bands,
                        bandLevels = channelBBands,
                        onLevelChanged = onBandBChanged,
                        isChannelA = false,
                        enabled = isEnabled,
                        channelBadge = "CH B (RIGHT) - 60Hz HOREG"
                    )
                }
                ChannelSelection.DUAL_VIEW -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Channel A Strip
                        FaderStripRow(
                            bands = bands,
                            bandLevels = channelABands,
                            onLevelChanged = onBandAChanged,
                            isChannelA = true,
                            enabled = isEnabled,
                            channelBadge = "CH A (LEFT / MONO 1)"
                        )

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(RackBorder)
                        )

                        // Channel B Strip
                        FaderStripRow(
                            bands = bands,
                            bandLevels = channelBBands,
                            onLevelChanged = onBandBChanged,
                            isChannelA = false,
                            enabled = isEnabled,
                            channelBadge = "CH B (RIGHT / MONO 2)"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaderStripRow(
    bands: List<BandInfo>,
    bandLevels: List<Int>,
    onLevelChanged: (Int, Int) -> Unit,
    isChannelA: Boolean,
    enabled: Boolean,
    channelBadge: String,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isChannelA) ChannelACyan else ChannelBAmber

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Strip Header with Channel Badge
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
                        .background(if (enabled) accentColor else TextMuted)
                )
                Text(
                    text = channelBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (enabled) accentColor else TextMuted
                )
            }
            Text(
                text = "+15 dB / -15 dB",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        // Horizontal Faders Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEachIndexed { index, bandInfo ->
                val level = bandLevels.getOrElse(index) { 0 }
                StudioRackFader(
                    bandLabel = bandInfo.label,
                    levelDb = level,
                    onLevelChanged = { newDb -> onLevelChanged(index, newDb) },
                    isChannelA = isChannelA,
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ChannelTabButton(
    title: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) accentColor else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) accentColor else TextMuted
        )
    }
}

@Composable
fun RackScrewGraphic(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(RackScrew, Color(0xFF1E2430), Color.Black)
                )
            )
            .border(0.5.dp, Color(0xFF6B7280), CircleShape)
    ) {
        // Hex / slot cutout
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(1.5.dp)
                .background(Color.Black.copy(alpha = 0.8f))
        )
    }
}
