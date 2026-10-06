package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.BluetoothStatus
import com.example.audio.TestAudioMode
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.HoregCrimson
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.ui.theme.VuLedGreen
import com.example.ui.theme.VuLedYellow

@Composable
fun DeviceStatusBar(
    bluetoothStatus: BluetoothStatus,
    hasAudioPermission: Boolean,
    isBatteryExempt: Boolean,
    isYouTubeAudioActive: Boolean,
    activeMediaSessionId: Int? = null,
    isTestingAudio: Boolean,
    testAudioMode: TestAudioMode,
    onRequestAudioPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onToggleTestAudio: (TestAudioMode?) -> Unit,
    onOpenTwsOptimization: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Row 1: Audio Route Card + Background & YouTube Status Tile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Output Device (TWS Bluetooth / Speaker)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackSurface)
                    .border(
                        1.dp,
                        if (bluetoothStatus.isLowLatencyActive) ChannelACyan.copy(alpha = 0.5f) else RackBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onOpenTwsOptimization)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isBt = bluetoothStatus.isConnected
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isBt) ChannelACyan.copy(alpha = 0.2f) else RackSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = if (isBt) Icons.Default.BluetoothConnected else Icons.Default.Headphones,
                            contentDescription = "Output Device",
                            tint = if (isBt) ChannelACyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bluetoothStatus.deviceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSilver,
                            maxLines = 1
                        )
                        Text(
                            text = if (bluetoothStatus.isLowLatencyActive)
                                "Anti-Delay Aktif • Ketuk Atur"
                            else
                                "${bluetoothStatus.audioType} • Ketuk Atur",
                            fontSize = 9.sp,
                            color = if (bluetoothStatus.isLowLatencyActive) VuLedGreen else TextMuted
                        )
                    }
                }
            }

            // YouTube Background Sync Live Status Card
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackSurface)
                    .border(
                        1.dp,
                        if (isYouTubeAudioActive) VuLedGreen.copy(alpha = 0.6f) else RackBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isYouTubeAudioActive) VuLedGreen.copy(alpha = 0.2f) else RackSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = if (isYouTubeAudioActive) Icons.Default.Radio else Icons.Default.Sync,
                            contentDescription = "YouTube Audio Sync",
                            tint = if (isYouTubeAudioActive) VuLedGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isYouTubeAudioActive) VuLedGreen else TextMuted)
                            )
                            Text(
                                text = if (activeMediaSessionId != null)
                                    "Media Session #$activeMediaSessionId"
                                else if (isYouTubeAudioActive)
                                    "YouTube Media Output"
                                else
                                    "YouTube Audio Sync",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isYouTubeAudioActive) VuLedGreen else TextSilver
                            )
                        }
                        Text(
                            text = if (activeMediaSessionId != null)
                                "AudioEffect aktif pada media player"
                            else if (isYouTubeAudioActive)
                                "Equalizer media player aktif"
                            else
                                "Menunggu media player dimulai",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Row 2: Permissions Check Banner (if permissions are needed for real-time visualizer or background stability)
        if (!hasAudioPermission || !isBatteryExempt) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RackSurfaceElevated)
                    .border(1.dp, VuLedYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (!hasAudioPermission) Icons.Default.Mic else Icons.Default.BatterySaver,
                            contentDescription = "Permission Alert",
                            tint = VuLedYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (!hasAudioPermission)
                                    "Izin Audio Diperlukan untuk Sinkron Visualizer"
                                else
                                    "Izin Latar Belakang (Baterai) Tanpa Batas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSilver
                            )
                            Text(
                                text = if (!hasAudioPermission)
                                    "Klik tombol untuk sinkronkan visualizer dengan suara YouTube"
                                else
                                    "Cegah YouTube equalizer dihentikan oleh sistem saat layar mati",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VuLedYellow.copy(alpha = 0.2f))
                            .border(1.dp, VuLedYellow, RoundedCornerShape(6.dp))
                            .clickable {
                                if (!hasAudioPermission) {
                                    onRequestAudioPermission()
                                } else {
                                    onRequestBatteryExemption()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("btn_grant_permission")
                    ) {
                        Text(
                            text = if (!hasAudioPermission) "Beri Izin" else "Izinkan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VuLedYellow
                        )
                    }
                }
            }
        }

        // Row 3: Built-in Live Sound Tester (To audition Horeg 60Hz and EQ live)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RackSurfaceElevated)
                .border(1.dp, if (isTestingAudio) HoregCrimson.copy(alpha = 0.5f) else RackBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onToggleTestAudio(null) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isTestingAudio) HoregCrimson else ChannelACyan)
                            .testTag("btn_toggle_test_audio")
                    ) {
                        Icon(
                            imageVector = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Tes Suara",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isTestingAudio) "AUDITION LIVE TEST SUARA..." else "TES SUARA EQUALIZER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isTestingAudio) HoregCrimson else TextSilver
                        )
                        Text(
                            text = "Uji langsung bass 60Hz & clarity",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // Modes: 60Hz Sub | Beat | Sweep
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TestModeChip(
                        label = "60Hz Sub",
                        isSelected = testAudioMode == TestAudioMode.SUB_60HZ_PULSE,
                        onClick = { onToggleTestAudio(TestAudioMode.SUB_60HZ_PULSE) }
                    )
                    TestModeChip(
                        label = "Kick Beat",
                        isSelected = testAudioMode == TestAudioMode.HOREG_KICK_BEAT,
                        onClick = { onToggleTestAudio(TestAudioMode.HOREG_KICK_BEAT) }
                    )
                    TestModeChip(
                        label = "Sweep",
                        isSelected = testAudioMode == TestAudioMode.FULL_SWEEP_NOISE,
                        onClick = { onToggleTestAudio(TestAudioMode.FULL_SWEEP_NOISE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TestModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) ChannelACyan.copy(alpha = 0.25f) else RackDarkBackground)
            .border(
                0.5.dp,
                if (isSelected) ChannelACyan else RackBorder,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ChannelACyan else TextMuted
        )
    }
}
