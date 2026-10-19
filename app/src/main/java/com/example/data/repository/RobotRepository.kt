package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.AutoRobotState
import com.example.data.model.CalibrationConfig
import com.example.data.model.DeviceNetworkConfig
import com.example.data.model.Esp32CamTelemetry
import com.example.data.model.Esp32MainTelemetry
import com.example.data.model.NodeMcuSensorTelemetry
import com.example.data.model.OperatingMode
import com.example.data.model.SavedWeedRecord
import com.example.data.model.WaterLevelState
import com.example.data.model.YoloDetection
import com.example.data.network.RobotApiClient
import com.example.vision.YoloVisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RobotRepository(
    private val context: Context,
    private val apiClient: RobotApiClient = RobotApiClient(),
    private val visionEngine: YoloVisionEngine = YoloVisionEngine(context)
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Authoritative Operating Mode (Starts strictly in STOPPED)
    private val _operatingMode = MutableStateFlow(OperatingMode.STOPPED)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode.asStateFlow()

    // Device Network Configuration
    private val _networkConfig = MutableStateFlow(DeviceNetworkConfig())
    val networkConfig: StateFlow<DeviceNetworkConfig> = _networkConfig.asStateFlow()

    // Calibration Configuration
    private val _calibration = MutableStateFlow(CalibrationConfig())
    val calibration: StateFlow<CalibrationConfig> = _calibration.asStateFlow()

    // Real Hardware Telemetry (No fake simulations)
    private val _esp32Telemetry = MutableStateFlow(Esp32MainTelemetry())
    val esp32Telemetry: StateFlow<Esp32MainTelemetry> = _esp32Telemetry.asStateFlow()

    private val _nodeMcuTelemetry = MutableStateFlow(NodeMcuSensorTelemetry())
    val nodeMcuTelemetry: StateFlow<NodeMcuSensorTelemetry> = _nodeMcuTelemetry.asStateFlow()

    private val _esp32CamTelemetry = MutableStateFlow(Esp32CamTelemetry())
    val esp32CamTelemetry: StateFlow<Esp32CamTelemetry> = _esp32CamTelemetry.asStateFlow()

    // Camera Frame & YOLO Detections
    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame: StateFlow<Bitmap?> = _currentFrame.asStateFlow()

    private val _latestDetections = MutableStateFlow<List<YoloDetection>>(emptyList())
    val latestDetections: StateFlow<List<YoloDetection>> = _latestDetections.asStateFlow()

    // Autonomous Control State
    private val _autoState = MutableStateFlow(AutoRobotState.STOPPED)
    val autoState: StateFlow<AutoRobotState> = _autoState.asStateFlow()

    // 5-Minute Soil Test Countdown (seconds remaining until next test)
    private val _soilTestCountdown = MutableStateFlow(300)
    val soilTestCountdown: StateFlow<Int> = _soilTestCountdown.asStateFlow()

    // Strict UI Flag: Temporary Soil gauges visible ONLY during active test!
    private val _isSoilTestWindowActive = MutableStateFlow(false)
    val isSoilTestWindowActive: StateFlow<Boolean> = _isSoilTestWindowActive.asStateFlow()

    // Saved Weed Captures
    private val _savedWeeds = MutableStateFlow<List<SavedWeedRecord>>(emptyList())
    val savedWeeds: StateFlow<List<SavedWeedRecord>> = _savedWeeds.asStateFlow()

    // Operational Logs
    private val _systemLogs = MutableStateFlow<List<String>>(emptyList())
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    // Background jobs
    private var telemetryPollingJob: Job? = null
    private var cameraStreamJob: Job? = null
    private var autoRoutineJob: Job? = null

    init {
        log("System initialized in STOPPED mode. Hardware safety interlock active.")
        startTelemetryPolling()
        loadSavedWeedCaptures()
    }

    fun updateNetworkConfig(config: DeviceNetworkConfig) {
        _networkConfig.value = config
        log("Updated device addresses: ESP32=${config.esp32MainIp}, NodeMCU=${config.nodeMcuIp}, CAM=${config.esp32CamIp}")
        restartCameraStream()
    }

    fun updateCalibration(calibrationConfig: CalibrationConfig) {
        _calibration.value = calibrationConfig
        log("Saved updated calibration settings.")
    }

    fun log(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $msg"
        val current = _systemLogs.value.toMutableList()
        current.add(0, formatted)
        if (current.size > 50) current.removeLast()
        _systemLogs.value = current
    }

    // =========================================================================
    // MODE TRANSITION STATE MACHINE (Strict Separation)
    // =========================================================================

    suspend fun requestModeChange(targetMode: OperatingMode): Result<Boolean> = withContext(Dispatchers.Default) {
        val current = _operatingMode.value
        if (current == targetMode) return@withContext Result.success(true)

        log("Requesting mode switch from $current to $targetMode...")

        // Step 1: ALWAYS Stop previous mode first and halt motors/actuators
        val config = _networkConfig.value
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "STOP", 0)
        autoRoutineJob?.cancel()
        _autoState.value = AutoRobotState.STOPPED
        _isSoilTestWindowActive.value = false

        if (targetMode == OperatingMode.STOPPED) {
            val stopAck = apiClient.setOperatingMode(config.esp32MainIp, config.esp32MainPort, OperatingMode.STOPPED)
            _operatingMode.value = OperatingMode.STOPPED
            log("System successfully switched to STOPPED mode.")
            return@withContext Result.success(true)
        }

        // Step 2: Request controller acknowledgement for target mode
        val ackResult = apiClient.setOperatingMode(config.esp32MainIp, config.esp32MainPort, targetMode)

        if (ackResult.isSuccess) {
            _operatingMode.value = targetMode
            log("Mode transition acknowledged by ESP32: Active mode is now $targetMode.")

            if (targetMode == OperatingMode.AUTO) {
                startAutoRoutine()
            } else if (targetMode == OperatingMode.MANUAL) {
                // Ensure camera stream is ready
                restartCameraStream()
            }
            Result.success(true)
        } else {
            // Stay strictly STOPPED if acknowledgement or communication fails
            _operatingMode.value = OperatingMode.STOPPED
            val error = ackResult.exceptionOrNull()?.message ?: "Acknowledgement timeout"
            log("ERROR: Mode transition to $targetMode failed: $error. Staying in STOPPED mode for safety.")
            Result.failure(Exception("Controller rejected transition: $error"))
        }
    }

    // =========================================================================
    // EMERGENCY STOP (Available in all modes, overrides everything)
    // =========================================================================

    suspend fun triggerEmergencyStop() = withContext(Dispatchers.Default) {
        log("EMERGENCY STOP TRIGGERED! Shutting down all motors, cutter, and arm.")
        autoRoutineJob?.cancel()
        _autoState.value = AutoRobotState.STOPPED
        _isSoilTestWindowActive.value = false
        _operatingMode.value = OperatingMode.STOPPED

        val config = _networkConfig.value
        // Fire estop to both controllers immediately
        apiClient.triggerEmergencyStop(config.esp32MainIp, config.esp32MainPort)
        apiClient.triggerEmergencyStop(config.nodeMcuIp, config.nodeMcuPort)
    }

    // =========================================================================
    // MANUAL ROBOT CONTROLS (Only in MANUAL mode)
    // =========================================================================

    suspend fun sendManualMove(action: String, speed: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_operatingMode.value != OperatingMode.MANUAL) {
            return@withContext Result.failure(IllegalStateException("Movement permitted ONLY in MANUAL mode!"))
        }
        val config = _networkConfig.value
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, action, speed)
    }

    suspend fun sendCameraPanTilt(pan: Int, tilt: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_operatingMode.value != OperatingMode.MANUAL) {
            return@withContext Result.failure(IllegalStateException("Pan/Tilt permitted ONLY in MANUAL mode!"))
        }
        val config = _networkConfig.value
        apiClient.sendCameraPanTilt(config.esp32MainIp, config.esp32MainPort, pan, tilt)
    }

    // =========================================================================
    // AUTONOMOUS ROUTINE (Only in AUTO mode)
    // =========================================================================

    private fun startAutoRoutine() {
        autoRoutineJob?.cancel()
        autoRoutineJob = scope.launch {
            log("Autonomous weed detection & soil management loop started.")
            _autoState.value = AutoRobotState.SEARCHING
            _soilTestCountdown.value = _calibration.value.soilTestIntervalSeconds

            var secondsPassed = 0

            while (isActive && _operatingMode.value == OperatingMode.AUTO) {
                delay(1000)
                secondsPassed++
                val interval = _calibration.value.soilTestIntervalSeconds
                val remaining = (interval - (secondsPassed % interval)).coerceAtLeast(0)
                _soilTestCountdown.value = remaining

                // Check front obstacle sensor telemetry
                if (_esp32Telemetry.value.isObstacleDetected && _autoState.value == AutoRobotState.SEARCHING) {
                    executeObstacleAvoidance()
                    continue
                }

                // Check 5-minute soil test trigger
                if (remaining == 0) {
                    executeSoilTestCycle()
                    secondsPassed = 0
                    _soilTestCountdown.value = interval
                    continue
                }

                // Normal weed search evaluation
                if (_autoState.value == AutoRobotState.SEARCHING) {
                    val eligibleWeed = _latestDetections.value.firstOrNull { it.isCutterEligible }
                    if (eligibleWeed != null) {
                        executeWeedCutCycle(eligibleWeed)
                    }
                }
            }
        }
    }

    private suspend fun executeObstacleAvoidance() = withContext(Dispatchers.IO) {
        log("HC-SR04 Obstacle detected (<20cm). Stopping and performing avoidance maneuver...")
        _autoState.value = AutoRobotState.OBSTACLE_AVOIDING
        val config = _networkConfig.value

        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "STOP", 0)
        delay(400)
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "BACKWARD", 150)
        delay(1200)
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "RIGHT", 160)
        delay(800)
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "STOP", 0)
        delay(400)

        _autoState.value = AutoRobotState.SEARCHING
        log("Path clear. Resumed autonomous search.")
    }

    private suspend fun executeWeedCutCycle(weed: YoloDetection) = withContext(Dispatchers.IO) {
        log("CONFIRMED WEED DETECTED at X=${weed.targetX_cm}cm, Y=${weed.targetY_cm}cm (Conf: ${(weed.confidence * 100).toInt()}%)")
        _autoState.value = AutoRobotState.WEED_CONFIRMED
        val config = _networkConfig.value

        // 1. Stop robot chassis
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "STOP", 0)
        delay(300)

        // 2. Capture and save real image
        val frame = _currentFrame.value
        if (frame != null) {
            val record = visionEngine.saveWeedDetectionImage(frame, weed)
            _savedWeeds.value = listOf(record) + _savedWeeds.value
            log("Saved weed capture image to local storage: ${record.id}")
        }

        // 3. Move arm servos to target
        _autoState.value = AutoRobotState.ARM_POSITIONING
        log("Sending validated target to ESP32: X=${weed.targetX_cm}cm, Y=${weed.targetY_cm}cm")

        val targetResult = apiClient.sendAutoWeedTarget(
            config.esp32MainIp,
            config.esp32MainPort,
            weed.targetX_cm,
            weed.targetY_cm,
            weed.confidence
        )

        if (targetResult.isSuccess) {
            _autoState.value = AutoRobotState.CUTTER_ACTIVE
            log("ESP32 arm positioned. Cutter active (Hard safety cutoff <= 7s)...")
            // Wait while cutter cuts weed (controlled safely by ESP32 firmware)
            delay(4000)
            log("Cutter cycle complete. Arm returning HOME.")
        } else {
            log("ERROR: Arm target command failed: ${targetResult.exceptionOrNull()?.message}")
        }

        _autoState.value = AutoRobotState.SEARCHING
    }

    private suspend fun executeSoilTestCycle() = withContext(Dispatchers.IO) {
        log("5-Minute scheduled soil test initiated. Stopping robot chassis...")
        _autoState.value = AutoRobotState.SOIL_TEST_IN_PROGRESS
        val config = _networkConfig.value

        // Stop robot motors
        apiClient.sendManualMove(config.esp32MainIp, config.esp32MainPort, "STOP", 0)
        delay(400)

        // Show temporary gauges during test window
        _isSoilTestWindowActive.value = true

        // Check water availability
        val nodeStatus = apiClient.getNodeMcuStatus(config.nodeMcuIp, config.nodeMcuPort).getOrNull()
        val isWaterOk = nodeStatus?.isWaterAvailable ?: false

        log("Deploying soil probe servo via NodeMCU...")
        val testResult = apiClient.triggerSoilTest(config.nodeMcuIp, config.nodeMcuPort)

        if (testResult.isSuccess) {
            val resultJson = testResult.getOrNull()
            val moisture = resultJson?.optDouble("moisture", 45.0)?.toFloat() ?: 45f
            log("Soil test complete: Moisture=${moisture}%, WaterAvailable=$isWaterOk")

            if (moisture < _calibration.value.soilDryThresholdPercent && isWaterOk) {
                log("Soil is DRY (<${_calibration.value.soilDryThresholdPercent}%) & Water is AVAILABLE -> Pump activated for <= 5s.")
            } else if (!isWaterOk) {
                log("Water NOT available in tank -> Pump inhibited.")
            } else {
                log("Soil moisture adequate (${moisture}%) -> No watering needed.")
            }
        } else {
            log("WARNING: Soil test communication with NodeMCU failed: ${testResult.exceptionOrNull()?.message}")
        }

        // Keep temporary gauges visible for 8 seconds after probe finishes for user inspection, then hide
        delay(8000)
        _isSoilTestWindowActive.value = false
        _autoState.value = AutoRobotState.SEARCHING
        log("Soil-test gauges closed. Resumed slow autonomous search.")
    }

    // =========================================================================
    // BACKGROUND TELEMETRY & CAMERA
    // =========================================================================

    private fun startTelemetryPolling() {
        telemetryPollingJob?.cancel()
        telemetryPollingJob = scope.launch {
            while (isActive) {
                val config = _networkConfig.value

                // Poll ESP32
                val espRes = apiClient.getEsp32Status(config.esp32MainIp, config.esp32MainPort)
                if (espRes.isSuccess) {
                    _esp32Telemetry.value = espRes.getOrThrow()
                } else {
                    _esp32Telemetry.value = _esp32Telemetry.value.copy(
                        connected = false,
                        errorMessage = espRes.exceptionOrNull()?.message
                    )
                }

                // Poll NodeMCU
                val nodeRes = apiClient.getNodeMcuStatus(config.nodeMcuIp, config.nodeMcuPort)
                if (nodeRes.isSuccess) {
                    _nodeMcuTelemetry.value = nodeRes.getOrThrow()
                } else {
                    _nodeMcuTelemetry.value = _nodeMcuTelemetry.value.copy(
                        connected = false,
                        waterState = WaterLevelState.DISCONNECTED,
                        errorMessage = nodeRes.exceptionOrNull()?.message
                    )
                }

                delay(1500)
            }
        }
    }

    fun restartCameraStream() {
        cameraStreamJob?.cancel()
        cameraStreamJob = scope.launch {
            val config = _networkConfig.value
            log("Connecting to ESP32-CAM at ${config.esp32CamIp}:${config.esp32CamPort}...")

            while (isActive) {
                val frameRes = apiClient.captureCameraFrame(config.esp32CamIp, config.esp32CamPort, config.esp32CamCapturePath)
                if (frameRes.isSuccess) {
                    val bitmap = frameRes.getOrThrow()
                    _currentFrame.value = bitmap
                    _esp32CamTelemetry.value = Esp32CamTelemetry(
                        connected = true,
                        streamFps = 10f,
                        resolution = "${bitmap.width}x${bitmap.height}",
                        lastFrameTimestamp = System.currentTimeMillis()
                    )

                    // If in AUTO mode, run YOLO vision analysis on frame
                    if (_operatingMode.value == OperatingMode.AUTO) {
                        val detections = visionEngine.analyzeFrame(bitmap, _calibration.value)
                        _latestDetections.value = detections
                    } else {
                        _latestDetections.value = emptyList()
                    }
                    delay(150)
                } else {
                    _esp32CamTelemetry.value = Esp32CamTelemetry(
                        connected = false,
                        errorMessage = "Camera disconnected (${frameRes.exceptionOrNull()?.message})"
                    )
                    delay(2000)
                }
            }
        }
    }

    private fun loadSavedWeedCaptures() {
        scope.launch {
            _savedWeeds.value = visionEngine.getSavedWeedRecords()
        }
    }
}
