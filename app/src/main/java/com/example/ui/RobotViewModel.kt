package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalibrationConfig
import com.example.data.model.DeviceNetworkConfig
import com.example.data.model.OperatingMode
import com.example.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RobotViewModel(application: Application) : AndroidViewModel(application) {
    val repository = RobotRepository(application.applicationContext)

    val operatingMode = repository.operatingMode
    val esp32Telemetry = repository.esp32Telemetry
    val nodeMcuTelemetry = repository.nodeMcuTelemetry
    val esp32CamTelemetry = repository.esp32CamTelemetry

    val currentFrame = repository.currentFrame
    val latestDetections = repository.latestDetections
    val autoState = repository.autoState
    val soilTestCountdown = repository.soilTestCountdown
    val isSoilTestWindowActive = repository.isSoilTestWindowActive

    val savedWeeds = repository.savedWeeds
    val networkConfig = repository.networkConfig
    val calibration = repository.calibration
    val systemLogs = repository.systemLogs

    // Mode Confirmation Modal State
    private val _pendingModeChange = MutableStateFlow<OperatingMode?>(null)
    val pendingModeChange: StateFlow<OperatingMode?> = _pendingModeChange.asStateFlow()

    private val _modeSwitchError = MutableStateFlow<String?>(null)
    val modeSwitchError: StateFlow<String?> = _modeSwitchError.asStateFlow()

    // Manual Mode Speed Selection: Slow (120), Medium (185), Fast (255)
    private val _manualSpeed = MutableStateFlow(ManualSpeed.MEDIUM)
    val manualSpeed: StateFlow<ManualSpeed> = _manualSpeed.asStateFlow()

    // Camera Pan/Tilt sliders in Manual mode
    private val _manualPan = MutableStateFlow(90)
    val manualPan: StateFlow<Int> = _manualPan.asStateFlow()

    private val _manualTilt = MutableStateFlow(90)
    val manualTilt: StateFlow<Int> = _manualTilt.asStateFlow()

    fun requestModeSwitch(targetMode: OperatingMode) {
        if (targetMode == operatingMode.value) return
        _pendingModeChange.value = targetMode
    }

    fun dismissModeConfirmation() {
        _pendingModeChange.value = null
    }

    fun executeConfirmedModeSwitch() {
        val target = _pendingModeChange.value ?: return
        _pendingModeChange.value = null
        _modeSwitchError.value = null

        viewModelScope.launch {
            val result = repository.requestModeChange(target)
            if (result.isFailure) {
                _modeSwitchError.value = result.exceptionOrNull()?.message ?: "Failed to switch mode"
            }
        }
    }

    fun triggerEmergencyStop() {
        viewModelScope.launch {
            repository.triggerEmergencyStop()
        }
    }

    fun setManualSpeed(speed: ManualSpeed) {
        _manualSpeed.value = speed
    }

    fun sendManualMove(action: String) {
        val speedVal = when (_manualSpeed.value) {
            ManualSpeed.SLOW -> 120
            ManualSpeed.MEDIUM -> 185
            ManualSpeed.FAST -> 255
        }
        viewModelScope.launch {
            repository.sendManualMove(action, speedVal)
        }
    }

    fun stopManualMove() {
        viewModelScope.launch {
            repository.sendManualMove("STOP", 0)
        }
    }

    fun setPanTilt(pan: Int, tilt: Int) {
        _manualPan.value = pan
        _manualTilt.value = tilt
        viewModelScope.launch {
            repository.sendCameraPanTilt(pan, tilt)
        }
    }

    fun centerCamera() {
        setPanTilt(90, 90)
    }

    fun updateNetworkConfig(config: DeviceNetworkConfig) {
        repository.updateNetworkConfig(config)
    }

    fun updateCalibration(config: CalibrationConfig) {
        repository.updateCalibration(config)
    }

    fun retryCameraStream() {
        repository.restartCameraStream()
    }
}

enum class ManualSpeed(val label: String, val pwm: Int) {
    SLOW("Slow (30%)", 120),
    MEDIUM("Medium (65%)", 185),
    FAST("Fast (100%)", 255)
}
