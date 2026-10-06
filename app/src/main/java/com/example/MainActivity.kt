package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.EqualizerViewModel
import com.example.ui.components.BluetoothTwsOptimizationSheet
import com.example.ui.components.DeviceStatusBar
import com.example.ui.components.DualVuMeter
import com.example.ui.components.FaderConsole
import com.example.ui.components.HoregControlPanel
import com.example.ui.components.PresetBar
import com.example.ui.components.RealtimeSpectrumVisualizer
import com.example.ui.theme.ChannelACyan
import com.example.ui.theme.HoregCrimson
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RackDarkBackground
import com.example.ui.theme.RackSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSilver
import com.example.ui.theme.VuLedGreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: EqualizerViewModel by viewModels {
        EqualizerViewModel.Factory(application as HoregApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce standard 8-bit RGBA_8888 window format and default colorMode
        // to prevent HWUI 101010-2 HDR probing and unknown dataspace warnings
        window.setFormat(PixelFormat.RGBA_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSessionIntent(intent)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSessionIntent(intent)
    }

    private fun handleSessionIntent(intent: Intent?) {
        val sessionId = intent?.getIntExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, -1) ?: -1
        val pkg = intent?.getStringExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME)
        if (sessionId != -1) {
            viewModel.openSession(sessionId, pkg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EqualizerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle ON_RESUME to refresh permissions when user returns from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher for Record Audio (required for Visualizer sync with YouTube)
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        coroutineScope.launch {
            if (granted) {
                snackbarHostState.showSnackbar("Visualizer berhasil disinkronkan dengan audio YouTube!")
            } else {
                snackbarHostState.showSnackbar("Izin audio dibutuhkan agar visualizer bergerak mengikuti suara YouTube.")
            }
        }
    }

    // Multiple permissions launcher for initial launch
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (perms.isNotEmpty()) {
            multiplePermissionsLauncher.launch(perms.toTypedArray())
        }
    }

    // State observation
    val isEnabled by viewModel.isEnabled.collectAsState()
    val isStereoLinked by viewModel.isStereoLinked.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val channelABands by viewModel.channelABands.collectAsState()
    val channelBBands by viewModel.channelBBands.collectAsState()
    val subBoost by viewModel.subBoost.collectAsState()
    val masterGain by viewModel.masterGain.collectAsState()
    val midHighBoost by viewModel.midHighBoost.collectAsState()
    val lowLatencyTwsEnabled by viewModel.lowLatencyTwsEnabled.collectAsState()
    val bluetoothStatus by viewModel.bluetoothStatus.collectAsState()
    val vuLevelA by viewModel.vuLevelA.collectAsState()
    val vuLevelB by viewModel.vuLevelB.collectAsState()
    val peakLevelA by viewModel.peakLevelA.collectAsState()
    val peakLevelB by viewModel.peakLevelB.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val activePresetName by viewModel.activePresetName.collectAsState()
    val isTestingAudio by viewModel.isTestingAudio.collectAsState()
    val testAudioMode by viewModel.testAudioMode.collectAsState()

    val hasAudioPermission by viewModel.hasAudioPermission.collectAsState()
    val isBatteryExempt by viewModel.isBatteryExempt.collectAsState()
    val isYouTubeAudioActive by viewModel.isYouTubeAudioActive.collectAsState()
    val activeMediaSessionId by viewModel.activeMediaSessionId.collectAsState()
    val spectrumLevels by viewModel.spectrumLevels.collectAsState()
    val spectrumBands24 by viewModel.spectrumBands24.collectAsState()
    val spectrumPeaks24 by viewModel.spectrumPeaks24.collectAsState()

    val twsLatencyProfile by viewModel.twsLatencyProfile.collectAsState()
    val audioSyncOffsetMs by viewModel.audioSyncOffsetMs.collectAsState()
    val monoSummingEnabled by viewModel.monoSummingEnabled.collectAsState()

    var showTwsOptimizationMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RackDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RackSurface,
                    titleContentColor = TextSilver
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) VuLedGreen else TextMuted)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "HOREG EQUALIZER",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = TextSilver
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(HoregCrimson.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "60Hz",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = HoregCrimson
                                    )
                                }
                            }
                            Text(
                                text = "YouTube Background & TWS Audio Engine",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTwsOptimizationMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Optimisasi TWS & Lip-Sync",
                            tint = if (bluetoothStatus.isLowLatencyActive) ChannelACyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Horeg EQ berjalan di latar belakang menyaring audio YouTube bawaan & TWS Bluetooth."
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = ChannelACyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = RackDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Stereo Dual VU Meter with Real-Time YouTube Audio Sync & Spectrum Bars
                DualVuMeter(
                    levelA = vuLevelA,
                    levelB = vuLevelB,
                    peakA = peakLevelA,
                    peakB = peakLevelB,
                    spectrumLevels = spectrumLevels,
                    isYouTubeSynced = isYouTubeAudioActive,
                    hasAudioPermission = hasAudioPermission,
                    enabled = isEnabled
                )

                // 2. High-Resolution Live Frequency Spectrum Visualizer (Canvas Real-Time Feedback)
                RealtimeSpectrumVisualizer(
                    bands = spectrumBands24,
                    peaks = spectrumPeaks24,
                    isProcessing = isEnabled && (isYouTubeAudioActive || isTestingAudio),
                    hasAudioPermission = hasAudioPermission,
                    activeMediaSessionId = activeMediaSessionId
                )

                // 3. Bluetooth Device Status, Background Sync Status, and Permission Banner
                DeviceStatusBar(
                    bluetoothStatus = bluetoothStatus,
                    hasAudioPermission = hasAudioPermission,
                    isBatteryExempt = isBatteryExempt,
                    isYouTubeAudioActive = isYouTubeAudioActive,
                    activeMediaSessionId = activeMediaSessionId,
                    isTestingAudio = isTestingAudio,
                    testAudioMode = testAudioMode,
                    onRequestAudioPermission = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRequestBatteryExemption = {
                        viewModel.requestBatteryExemption(context)
                    },
                    onToggleTestAudio = { mode -> viewModel.toggleTestAudio(mode) },
                    onOpenTwsOptimization = { showTwsOptimizationMenu = true }
                )

                // 3. Preset Selector Bar (Horeg Glerr, Jedag-Jedug, etc.)
                PresetBar(
                    presets = presets,
                    activePresetName = activePresetName,
                    onSelectPreset = { preset -> viewModel.selectPreset(preset) },
                    onSavePreset = { name -> viewModel.saveCustomPreset(name) },
                    onDeletePreset = { preset -> viewModel.deletePreset(preset) },
                    onResetFlat = { viewModel.resetToFlat() },
                    enabled = isEnabled
                )

                // 4. Dual Channel A & B Physical Fader Console
                FaderConsole(
                    bands = bands,
                    channelABands = channelABands,
                    channelBBands = channelBBands,
                    isStereoLinked = isStereoLinked,
                    selectedChannel = selectedChannel,
                    onSelectChannel = { viewModel.setSelectedChannel(it) },
                    onToggleStereoLink = { viewModel.toggleStereoLink() },
                    onBandAChanged = { index, level -> viewModel.updateBand(true, index, level) },
                    onBandBChanged = { index, level -> viewModel.updateBand(false, index, level) },
                    isEnabled = isEnabled,
                    onTogglePower = { viewModel.togglePower() }
                )

                // 5. 60Hz Sub Horeg Boost, Gain Pre-amp, Mid-High, and Anti-Delay TWS
                HoregControlPanel(
                    subBoost = subBoost,
                    onSubBoostChanged = { viewModel.setSubBoost(it) },
                    masterGain = masterGain,
                    onMasterGainChanged = { viewModel.setMasterGain(it) },
                    midHighBoost = midHighBoost,
                    onMidHighBoostChanged = { viewModel.setMidHighBoost(it) },
                    lowLatencyTwsEnabled = lowLatencyTwsEnabled,
                    onToggleLowLatencyTws = { viewModel.toggleLowLatencyTws() },
                    enabled = isEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTwsOptimizationMenu) {
        BluetoothTwsOptimizationSheet(
            bluetoothStatus = bluetoothStatus,
            isLowLatencyEnabled = bluetoothStatus.isLowLatencyActive,
            latencyProfile = twsLatencyProfile,
            audioSyncOffsetMs = audioSyncOffsetMs,
            monoSummingEnabled = monoSummingEnabled,
            onToggleLowLatency = { viewModel.setLowLatencyTwsEnabled(it) },
            onSelectLatencyProfile = { viewModel.setTwsLatencyProfile(it) },
            onUpdateSyncOffset = { viewModel.setAudioSyncOffsetMs(it) },
            onToggleMonoSumming = { viewModel.setMonoSummingEnabled(it) },
            onPlayChannelTest = { viewModel.playTwsChannelTest(it) },
            onRefreshBluetooth = { viewModel.refreshBluetoothStatus() },
            onOpenBluetoothSettings = { viewModel.openBluetoothSettings(context) },
            onDismiss = { showTwsOptimizationMenu = false }
        )
    }
}
