package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Comma-separated list of dB values for Channel A, e.g. "12,8,2,6,10"
    val channelABands: String,
    // Comma-separated list of dB values for Channel B
    val channelBBands: String,
    val subBoost: Int = 800, // 0 to 1000
    val masterGain: Int = 400, // 0 to 1000 (maps to 0..1200 mB)
    val midHighBoost: Int = 600, // 0 to 1000
    val isSystemPreset: Boolean = false
)
