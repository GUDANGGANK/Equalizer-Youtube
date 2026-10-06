package com.example.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.BluetoothStatus
import com.example.audio.TwsLatencyProfile
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
import com.example.ui.theme.VuLedYellow
import com.example.util.HapticFeedbackHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothTwsOptimizationSheet(
    bluetoothStatus: BluetoothStatus,
    isLowLatencyEnabled: Boolean,
    latencyProfile: TwsLatencyProfile,
    audioSyncOffsetMs: Int,
    monoSummingEnabled: Boolean,
    onToggleLowLatency: (Boolean) -> Unit,
    onSelectLatencyProfile: (TwsLatencyProfile) -> Unit,
    onUpdateSyncOffset: (Int) -> Unit,
    onToggleMonoSumming: (Boolean) -> Unit,
    onPlayChannelTest: (Boolean) -> Unit,
    onRefreshBluetooth: () -> Unit,
    onOpenBluetoothSettings: (Context) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RackSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = Modifier.testTag("bluetooth_tws_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChannelACyan.copy(alpha = 0.15f))
                            .border(1.dp, ChannelACyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth TWS",
                            tint = ChannelACyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "OPTIMISASI TWS & AUDIO SYNC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextSilver
                        )
                        Text(
                            text = "Low-Latency Gaming Mode & Lip-Sync Buffer",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            // 1. Current Output Device & Route Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackDarkBackground)
                    .border(1.dp, if (bluetoothStatus.isConnected) ChannelACyan.copy(alpha = 0.5f) else RackBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (bluetoothStatus.isConnected) Icons.Default.Headphones else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (bluetoothStatus.isConnected) ChannelACyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = bluetoothStatus.deviceName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSilver
                                )
                                Text(
                                    text = "${bluetoothStatus.audioType} • ${if (bluetoothStatus.isConnected) "Tersambung" else "Standby"}",
                                    fontSize = 10.sp,
                                    color = if (bluetoothStatus.isConnected) VuLedGreen else TextMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (bluetoothStatus.isConnected) VuLedGreen.copy(alpha = 0.15f) else RackSurfaceElevated)
                                .border(0.5.dp, if (bluetoothStatus.isConnected) VuLedGreen else RackBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (bluetoothStatus.isConnected) "BT ACTIVE" else "INTERNAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (bluetoothStatus.isConnected) VuLedGreen else TextMuted
                            )
                        }
                    }

                    // Device Actions: Open BT Settings + Refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenBluetoothSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_open_bt_settings"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChannelACyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ChannelACyan.copy(alpha = 0.6f))),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pairing HP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onRefreshBluetooth,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_refresh_bt"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSilver),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RackBorder)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pindai Ulang", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. TWS Low-Latency Engine & Buffer Profiles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackDarkBackground)
                    .border(1.dp, RackBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = ChannelACyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Anti-Delay TWS Engine",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSilver
                                )
                            }
                            Text(
                                text = "Bypass buffer DSP berat untuk audio-video lip-sync akurat",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isLowLatencyEnabled,
                            onCheckedChange = onToggleLowLatency,
                            modifier = Modifier.testTag("tws_low_latency_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ChannelACyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = RackSurfaceElevated
                            )
                        )
                    }

                    // Profile options
                    Text(
                        text = "PROFIL LATENSI BUFFER:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TwsLatencyProfile.values().forEach { profile ->
                            val isSelected = latencyProfile == profile && isLowLatencyEnabled
                            val borderCol by animateColorAsState(
                                targetValue = if (isSelected) ChannelACyan else RackBorder,
                                label = "profileBorder"
                            )
                            val bgCol by animateColorAsState(
                                targetValue = if (isSelected) ChannelACyan.copy(alpha = 0.12f) else RackSurface,
                                label = "profileBg"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgCol)
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onToggleLowLatency(true)
                                        onSelectLatencyProfile(profile)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ChannelACyan else TextSilver
                                        )
                                        if (profile == TwsLatencyProfile.ULTRA_LOW) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(HoregCrimson.copy(alpha = 0.2f))
                                                    .border(0.5.dp, HoregCrimson, RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "VIDEO SYNC",
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HoregCrimson
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = profile.desc,
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Terpilih",
                                        tint = ChannelACyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Manual Lip-Sync Delay Fine-Tuning Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackDarkBackground)
                    .border(1.dp, RackBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kompensasi Manual Lip-Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSilver
                            )
                            Text(
                                text = "Geser untuk menyamakan gerak bibir video jika TWS lambat",
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RackSurfaceElevated)
                                .border(0.5.dp, ChannelACyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .clickable { onUpdateSyncOffset(0) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (audioSyncOffsetMs > 0) "+$audioSyncOffsetMs ms" else "$audioSyncOffsetMs ms",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (audioSyncOffsetMs == 0) VuLedGreen else ChannelACyan
                            )
                        }
                    }

                    Slider(
                        value = audioSyncOffsetMs.toFloat(),
                        onValueChange = {
                            val newOffset = it.roundToInt()
                            if (newOffset != audioSyncOffsetMs) {
                                HapticFeedbackHelper.performGainHaptic(context, newOffset + 100, audioSyncOffsetMs + 100, view)
                                onUpdateSyncOffset(newOffset)
                            }
                        },
                        valueRange = -100f..200f,
                        steps = 29, // 10ms increments
                        colors = SliderDefaults.colors(
                            thumbColor = ChannelACyan,
                            activeTrackColor = ChannelACyan,
                            inactiveTrackColor = RackBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-100ms (Maju)", fontSize = 8.5.sp, color = TextMuted)
                        Text("0ms (Netral)", fontSize = 8.5.sp, color = VuLedGreen)
                        Text("+200ms (Tunda)", fontSize = 8.5.sp, color = TextMuted)
                    }
                }
            }

            // 4. TWS Channel Calibration & Earbud Check
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RackDarkBackground)
                    .border(1.dp, RackBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "KALIBRASI EARBUD TWS (L / R):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlayChannelTest(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_test_left_channel"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChannelACyan.copy(alpha = 0.2f),
                                contentColor = ChannelACyan
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ChannelACyan)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Tes Kiri (L)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onPlayChannelTest(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_test_right_channel"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChannelBAmber.copy(alpha = 0.2f),
                                contentColor = ChannelBAmber
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ChannelBAmber)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Tes Kanan (R)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Mono Summing Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mono Summing (1 Earbud)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSilver
                            )
                            Text(
                                text = "Gabungkan sinyal L dan R jika hanya memakai 1 earbud",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = monoSummingEnabled,
                            onCheckedChange = onToggleMonoSumming,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ChannelBAmber,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = RackSurfaceElevated
                            )
                        )
                    }
                }
            }

            // Close Action
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_close_tws_sheet"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChannelACyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SIMPAN & SELESAI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
