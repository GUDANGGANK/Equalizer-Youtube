package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.HoregApplication
import com.example.audio.AudioEffectManager
import com.example.audio.BandInfo
import com.example.audio.BluetoothStatus
import com.example.audio.TestAudioMode
import com.example.audio.TwsLatencyProfile
import com.example.data.PresetDao
import com.example.data.PresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChannelSelection {
    CHANNEL_A,
    CHANNEL_B,
    DUAL_VIEW
}

class EqualizerViewModel(
    private val audioManager: AudioEffectManager,
    private val presetDao: PresetDao
) : ViewModel() {

    val isEnabled: StateFlow<Boolean> = audioManager.isEnabled
    val isStereoLinked: StateFlow<Boolean> = audioManager.isStereoLinked
    val bands: StateFlow<List<BandInfo>> = audioManager.bands
    val channelABands: StateFlow<List<Int>> = audioManager.channelABands
    val channelBBands: StateFlow<List<Int>> = audioManager.channelBBands
    val subBoost: StateFlow<Int> = audioManager.subBoost
    val masterGain: StateFlow<Int> = audioManager.masterGain
    val midHighBoost: StateFlow<Int> = audioManager.midHighBoost
    val lowLatencyTwsEnabled: StateFlow<Boolean> = audioManager.lowLatencyTwsEnabled
    val bluetoothStatus: StateFlow<BluetoothStatus> = audioManager.bluetoothStatus

    val vuLevelA: StateFlow<Float> = audioManager.vuLevelA
    val vuLevelB: StateFlow<Float> = audioManager.vuLevelB
    val peakLevelA: StateFlow<Float> = audioManager.peakLevelA
    val peakLevelB: StateFlow<Float> = audioManager.peakLevelB
    val hasAudioPermission: StateFlow<Boolean> = audioManager.hasAudioPermission
    val isBatteryExempt: StateFlow<Boolean> = audioManager.isBatteryExempt
    val isYouTubeAudioActive: StateFlow<Boolean> = audioManager.isYouTubeAudioActive
    val activeMediaSessionId: StateFlow<Int?> = audioManager.activeMediaSessionId
    val spectrumLevels: StateFlow<List<Float>> = audioManager.spectrumLevels
    val spectrumBands24: StateFlow<List<Float>> = audioManager.spectrumBands24
    val spectrumPeaks24: StateFlow<List<Float>> = audioManager.spectrumPeaks24

    val twsLatencyProfile: StateFlow<TwsLatencyProfile> = audioManager.twsLatencyProfile
    val audioSyncOffsetMs: StateFlow<Int> = audioManager.audioSyncOffsetMs
    val monoSummingEnabled: StateFlow<Boolean> = audioManager.monoSummingEnabled

    fun setLowLatencyTwsEnabled(enabled: Boolean) {
        audioManager.setLowLatencyTwsEnabled(enabled)
    }

    fun setTwsLatencyProfile(profile: TwsLatencyProfile) {
        audioManager.setTwsLatencyProfile(profile)
    }

    fun setAudioSyncOffsetMs(offset: Int) {
        audioManager.setAudioSyncOffsetMs(offset)
    }

    fun setMonoSummingEnabled(enabled: Boolean) {
        audioManager.setMonoSummingEnabled(enabled)
    }

    fun playTwsChannelTest(isLeft: Boolean) {
        audioManager.playTwsChannelTest(isLeft)
    }

    fun refreshBluetoothStatus() {
        audioManager.refreshBluetoothStatus()
    }

    fun openSession(sessionId: Int, packageName: String? = null) {
        audioManager.openSession(sessionId, packageName)
    }

    fun closeSession(sessionId: Int) {
        audioManager.closeSession(sessionId)
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ignored: Exception) {}
        }
    }

    fun refreshPermissions() {
        audioManager.checkPermissions()
    }

    fun requestBatteryExemption(context: Context) {
        audioManager.requestIgnoreBatteryOptimizations(context)
    }

    private val _selectedChannel = MutableStateFlow(ChannelSelection.DUAL_VIEW)
    val selectedChannel: StateFlow<ChannelSelection> = _selectedChannel.asStateFlow()

    private val _activePresetName = MutableStateFlow("HOREG BASS GLERR (60Hz)")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    val presets: StateFlow<List<PresetEntity>> = presetDao.getAllPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isTestingAudio = MutableStateFlow(false)
    val isTestingAudio: StateFlow<Boolean> = _isTestingAudio.asStateFlow()

    private val _testAudioMode = MutableStateFlow(TestAudioMode.SUB_60HZ_PULSE)
    val testAudioMode: StateFlow<TestAudioMode> = _testAudioMode.asStateFlow()

    fun togglePower() {
        val newState = !isEnabled.value
        audioManager.setEnabled(newState)
    }

    fun toggleStereoLink() {
        audioManager.setStereoLinked(!isStereoLinked.value)
    }

    fun setSelectedChannel(channel: ChannelSelection) {
        _selectedChannel.value = channel
    }

    fun updateBand(isChannelA: Boolean, bandIndex: Int, levelDb: Int) {
        if (isChannelA) {
            audioManager.updateChannelABand(bandIndex, levelDb)
        } else {
            audioManager.updateChannelBBand(bandIndex, levelDb)
        }
        _activePresetName.value = "Custom EQ"
    }

    fun setSubBoost(value: Int) {
        audioManager.setSubBoost(value)
        _activePresetName.value = "Custom EQ"
    }

    fun setMasterGain(value: Int) {
        audioManager.setMasterGain(value)
        _activePresetName.value = "Custom EQ"
    }

    fun setMidHighBoost(value: Int) {
        audioManager.setMidHighBoost(value)
        _activePresetName.value = "Custom EQ"
    }

    fun toggleLowLatencyTws() {
        audioManager.setLowLatencyTwsEnabled(!lowLatencyTwsEnabled.value)
    }

    fun selectPreset(preset: PresetEntity) {
        val bandsA = preset.channelABands.split(",").mapNotNull { it.trim().toIntOrNull() }
        val bandsB = preset.channelBBands.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (bandsA.isNotEmpty() && bandsB.isNotEmpty()) {
            audioManager.applyPreset(
                bandsA = bandsA,
                bandsB = bandsB,
                sub = preset.subBoost,
                gain = preset.masterGain,
                midHigh = preset.midHighBoost
            )
            _activePresetName.value = preset.name
        }
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val aStr = channelABands.value.joinToString(",")
            val bStr = channelBBands.value.joinToString(",")
            val entity = PresetEntity(
                name = name.ifBlank { "Custom Horeg ${System.currentTimeMillis() % 1000}" },
                channelABands = aStr,
                channelBBands = bStr,
                subBoost = subBoost.value,
                masterGain = masterGain.value,
                midHighBoost = midHighBoost.value,
                isSystemPreset = false
            )
            presetDao.insertPreset(entity)
            _activePresetName.value = entity.name
        }
    }

    fun deletePreset(preset: PresetEntity) {
        if (!preset.isSystemPreset) {
            viewModelScope.launch {
                presetDao.deletePreset(preset)
            }
        }
    }

    fun resetToFlat() {
        audioManager.resetToFlat()
        _activePresetName.value = "FLAT MONITOR"
    }

    fun toggleTestAudio(mode: TestAudioMode? = null) {
        val targetMode = mode ?: _testAudioMode.value
        _testAudioMode.value = targetMode

        if (_isTestingAudio.value && mode == null) {
            audioManager.toneGenerator.stop()
            _isTestingAudio.value = false
        } else {
            audioManager.toneGenerator.start(targetMode, audioManager.defaultSessionId)
            _isTestingAudio.value = true
        }
    }

    fun stopTestAudio() {
        if (_isTestingAudio.value) {
            audioManager.toneGenerator.stop()
            _isTestingAudio.value = false
        }
    }

    fun launchYouTube(context: Context) {
        try {
            // First try native YouTube app
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Membuka browser YouTube...", Toast.LENGTH_SHORT).show()
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    override fun onCleared() {
        audioManager.toneGenerator.stop()
        super.onCleared()
    }

    class Factory(
        private val application: HoregApplication
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EqualizerViewModel::class.java)) {
                return EqualizerViewModel(
                    application.audioEffectManager,
                    application.database.presetDao()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
