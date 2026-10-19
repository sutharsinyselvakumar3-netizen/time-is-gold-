package com.example.data.model

enum class OperatingMode {
    STOPPED,
    MANUAL,
    AUTO
}

enum class DeviceType {
    ESP32_MAIN,
    NODEMCU_SENSOR,
    ESP32_CAM
}

enum class DeviceConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class WaterLevelState {
    AVAILABLE,
    LOW_NOT_AVAILABLE,
    SENSOR_ERROR,
    DISCONNECTED
}

enum class ArmStatus {
    HOME,
    POSITIONING,
    CUTTING,
    RETRACTING,
    FAULT,
    IDLE
}

enum class AutoRobotState {
    STOPPED,
    SEARCHING,
    OBSTACLE_AVOIDING,
    WEED_CONFIRMED,
    ARM_POSITIONING,
    CUTTER_ACTIVE,
    SOIL_TEST_IN_PROGRESS,
    SAFETY_PAUSE
}

enum class WeedClass {
    WEED,
    ONION,
    UNKNOWN
}

data class YoloDetection(
    val id: String,
    val weedClass: WeedClass,
    val confidence: Float,
    val boundingBox: BoundingBox, // Normalized 0..1
    val pixelCenterX: Float,
    val pixelCenterY: Float,
    val targetX_cm: Float,
    val targetY_cm: Float,
    val isWithinSafeWorkspace: Boolean,
    val isCutterEligible: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

data class Esp32MainTelemetry(
    val connected: Boolean = false,
    val currentMode: OperatingMode = OperatingMode.STOPPED,
    val batteryVoltage: Float? = null,
    val obstacleDistanceCm: Float? = null,
    val isObstacleDetected: Boolean = false,
    val armStatus: ArmStatus = ArmStatus.HOME,
    val isCutterActive: Boolean = false,
    val cutterSecondsElapsed: Float = 0f,
    val panAngle: Int = 90,
    val tiltAngle: Int = 90,
    val motorLeftSpeed: Int = 0,
    val motorRightSpeed: Int = 0,
    val emergencyStopTriggered: Boolean = false,
    val lastHeartbeatTimestamp: Long = 0L,
    val errorMessage: String? = null
)

data class NodeMcuSensorTelemetry(
    val connected: Boolean = false,
    val waterState: WaterLevelState = WaterLevelState.DISCONNECTED,
    val waterReadingRaw: Int? = null, // Raw analog/digital value
    val isWaterAvailable: Boolean = false,
    val isSoilTestActive: Boolean = false,
    val soilMoisturePercent: Float? = null,
    val temperatureC: Float? = null,
    val humidityPercent: Float? = null,
    val isPumpActive: Boolean = false,
    val pumpSecondsElapsed: Float = 0f,
    val isSoilServoDeployed: Boolean = false,
    val lastSoilTestTimestamp: Long = 0L,
    val lastHeartbeatTimestamp: Long = 0L,
    val errorMessage: String? = null
)

data class Esp32CamTelemetry(
    val connected: Boolean = false,
    val streamFps: Float = 0f,
    val resolution: String = "640x480",
    val lastFrameTimestamp: Long = 0L,
    val errorMessage: String? = null
)

data class SavedWeedRecord(
    val id: String,
    val timestamp: Long,
    val confidence: Float,
    val xCm: Float,
    val yCm: Float,
    val imageFilePath: String,
    val wasCutSuccessfully: Boolean = false
)

data class WorkspaceLimits(
    val xMinCm: Float = -15f,
    val xMaxCm: Float = 15f,
    val yMinCm: Float = 10f,
    val yMaxCm: Float = 35f
)

data class CalibrationConfig(
    val cameraHeightCm: Float = 25f,
    val cameraTiltAngleDeg: Float = 45f,
    val cameraFocalLengthPx: Float = 380f,
    val workspace: WorkspaceLimits = WorkspaceLimits(),
    val yoloConfidenceThreshold: Float = 0.70f,
    val soilDryThresholdPercent: Float = 35f,
    val soilTestIntervalSeconds: Int = 300, // 5 minutes
    val maxCutterDurationSeconds: Int = 7,
    val maxPumpDurationSeconds: Int = 5,
    val isAnalogWaterSensor: Boolean = false,
    val waterAnalogThreshold: Int = 400
)

data class DeviceNetworkConfig(
    val esp32MainIp: String = "192.168.4.1",
    val esp32MainPort: Int = 80,
    val nodeMcuIp: String = "192.168.4.2",
    val nodeMcuPort: Int = 80,
    val esp32CamIp: String = "192.168.4.3",
    val esp32CamPort: Int = 81,
    val esp32CamStreamPath: String = "/stream",
    val esp32CamCapturePath: String = "/capture"
)
