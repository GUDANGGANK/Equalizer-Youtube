package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PresetEntity
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.HoregCrimson
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver

@Composable
fun PresetBar(
    presets: List<PresetEntity>,
    activePresetName: String,
    onSelectPreset: (PresetEntity) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (PresetEntity) -> Unit,
    onResetFlat: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameToSave by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<PresetEntity?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Preset header
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Presets",
                    tint = ChannelACyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PRESET AUDIO HOREG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSilver,
                    letterSpacing = 1.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Flat button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RackSurfaceElevated)
                        .border(1.dp, RackBorder, RoundedCornerShape(4.dp))
                        .clickable(enabled = enabled, onClick = onResetFlat)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("btn_reset_flat")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Flat",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "FLAT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSilver
                        )
                    }
                }

                // Save button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ChannelACyan.copy(alpha = 0.15f))
                        .border(1.dp, ChannelACyan, RoundedCornerShape(4.dp))
                        .clickable(enabled = enabled) {
                            presetNameToSave = "My Horeg EQ"
                            showSaveDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("btn_save_preset")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Simpan",
                            tint = ChannelACyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SIMPAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChannelACyan
                        )
                    }
                }
            }
        }

        // Horizontal preset chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isActive = preset.name.equals(activePresetName, ignoreCase = true)
                val isHoreg = preset.name.contains("HOREG", ignoreCase = true)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isActive && isHoreg -> HoregCrimson.copy(alpha = 0.25f)
                                isActive -> ChannelACyan.copy(alpha = 0.2f)
                                else -> RackSurface
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isActive && isHoreg -> HoregCrimson
                                isActive -> ChannelACyan
                                else -> RackBorder
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = enabled) { onSelectPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("preset_chip_${preset.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = preset.name,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.SemiBold,
                            color = when {
                                isActive && isHoreg -> HoregCrimson
                                isActive -> ChannelACyan
                                isHoreg -> Color(0xFFFF8A80)
                                else -> TextSilver
                            }
                        )

                        if (!preset.isSystemPreset) {
                            IconButton(
                                onClick = { presetToDelete = preset },
                                modifier = Modifier.size(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Preset",
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = RackSurface,
            title = {
                Text(
                    text = "Simpan Preset Custom",
                    color = TextSilver,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Simpan setting fader, 60Hz Sub Horeg, dan Gain saat ini:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = presetNameToSave,
                        onValueChange = { presetNameToSave = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextSilver,
                            unfocusedTextColor = TextSilver,
                            focusedBorderColor = ChannelACyan,
                            unfocusedBorderColor = RackBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_preset_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameToSave.isNotBlank()) {
                            onSavePreset(presetNameToSave)
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChannelACyan, contentColor = Color.Black)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            containerColor = RackSurface,
            title = { Text("Hapus Preset?", color = TextSilver) },
            text = { Text("Yakin ingin menghapus '${preset.name}'?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePreset(preset)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HoregCrimson)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }
}
