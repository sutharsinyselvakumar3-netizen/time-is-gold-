package com.example.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.model.ArmStatus
import com.example.data.model.Esp32MainTelemetry
import com.example.data.model.NodeMcuSensorTelemetry
import com.example.data.model.OperatingMode
import com.example.data.model.WaterLevelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class RobotApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(1500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ==========================================
    // ESP32 MAIN CONTROLLER API
    // ==========================================

    suspend fun getEsp32Status(host: String, port: Int): Result<Esp32MainTelemetry> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val modeStr = json.optString("mode", "STOPPED")
                val mode = when (modeStr.uppercase()) {
                    "MANUAL" -> OperatingMode.MANUAL
                    "AUTO" -> OperatingMode.AUTO
                    else -> OperatingMode.STOPPED
                }
                val armStr = json.optString("arm_status", "HOME")
                val armStatus = when (armStr.uppercase()) {
                    "POSITIONING" -> ArmStatus.POSITIONING
                    "CUTTING" -> ArmStatus.CUTTING
                    "RETRACTING" -> ArmStatus.RETRACTING
                    "FAULT" -> ArmStatus.FAULT
                    else -> ArmStatus.HOME
                }
                val battery = if (json.has("battery_voltage")) json.optDouble("battery_voltage", 0.0).toFloat() else null
                val distance = if (json.has("obstacle_cm")) json.optDouble("obstacle_cm", 999.0).toFloat() else null
                val obstacleDetected = json.optBoolean("obstacle_detected", (distance != null && distance < 20f))

                val telemetry = Esp32MainTelemetry(
                    connected = true,
                    currentMode = mode,
                    batteryVoltage = battery,
                    obstacleDistanceCm = distance,
                    isObstacleDetected = obstacleDetected,
                    armStatus = armStatus,
                    isCutterActive = json.optBoolean("cutter_active", false),
                    cutterSecondsElapsed = json.optDouble("cutter_seconds", 0.0).toFloat(),
                    panAngle = json.optInt("pan_angle", 90),
                    tiltAngle = json.optInt("tilt_angle", 90),
                    motorLeftSpeed = json.optInt("motor_left", 0),
                    motorRightSpeed = json.optInt("motor_right", 0),
                    emergencyStopTriggered = json.optBoolean("estop", false),
                    lastHeartbeatTimestamp = System.currentTimeMillis()
                )
                Result.success(telemetry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setOperatingMode(host: String, port: Int, targetMode: OperatingMode): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/mode"
            val payload = JSONObject().apply {
                put("mode", targetMode.name)
            }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val json = JSONObject(bodyStr)
                    val acknowledged = json.optBoolean("acknowledged", true)
                    val returnedMode = json.optString("mode", targetMode.name)
                    if (acknowledged && returnedMode.equals(targetMode.name, ignoreCase = true)) {
                        Result.success(true)
                    } else {
                        Result.failure(Exception("Mode transition rejected by ESP32: $bodyStr"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code} setting mode"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerEmergencyStop(host: String, port: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/estop"
            val payload = JSONObject().apply { put("estop", true) }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendManualMove(host: String, port: Int, action: String, speed: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/manual/move"
            val payload = JSONObject().apply {
                put("action", action)
                put("speed", speed)
            }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code} - Move rejected (Ensure MANUAL mode)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendCameraPanTilt(host: String, port: Int, pan: Int, tilt: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/manual/camera"
            val payload = JSONObject().apply {
                put("pan", pan.coerceIn(0, 180))
                put("tilt", tilt.coerceIn(0, 180))
            }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendAutoWeedTarget(
        host: String,
        port: Int,
        targetX_cm: Float,
        targetY_cm: Float,
        confidence: Float
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/auto/target"
            val payload = JSONObject().apply {
                put("targetX_cm", targetX_cm)
                put("targetY_cm", targetY_cm)
                put("confidence", confidence)
                put("class", "WEED")
            }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(JSONObject(bodyStr))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // NODEMCU SENSOR CONTROLLER API
    // ==========================================

    suspend fun getNodeMcuStatus(host: String, port: Int): Result<NodeMcuSensorTelemetry> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/sensors/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)

                val waterAvailable = json.optBoolean("water_available", false)
                val rawWater = if (json.has("water_raw")) json.optInt("water_raw") else null
                val waterStateStr = json.optString("water_state", if (waterAvailable) "AVAILABLE" else "LOW_NOT_AVAILABLE")
                val waterState = when (waterStateStr.uppercase()) {
                    "AVAILABLE" -> WaterLevelState.AVAILABLE
                    "LOW_NOT_AVAILABLE", "LOW", "EMPTY" -> WaterLevelState.LOW_NOT_AVAILABLE
                    "ERROR" -> WaterLevelState.SENSOR_ERROR
                    else -> if (waterAvailable) WaterLevelState.AVAILABLE else WaterLevelState.LOW_NOT_AVAILABLE
                }

                val soilMoisture = if (json.has("soil_moisture")) json.optDouble("soil_moisture").toFloat() else null
                val temp = if (json.has("temperature")) json.optDouble("temperature").toFloat() else null
                val humidity = if (json.has("humidity")) json.optDouble("humidity").toFloat() else null

                val telemetry = NodeMcuSensorTelemetry(
                    connected = true,
                    waterState = waterState,
                    waterReadingRaw = rawWater,
                    isWaterAvailable = waterAvailable,
                    isSoilTestActive = json.optBoolean("soil_test_active", false),
                    soilMoisturePercent = soilMoisture,
                    temperatureC = temp,
                    humidityPercent = humidity,
                    isPumpActive = json.optBoolean("pump_active", false),
                    pumpSecondsElapsed = json.optDouble("pump_seconds", 0.0).toFloat(),
                    isSoilServoDeployed = json.optBoolean("soil_servo_deployed", false),
                    lastSoilTestTimestamp = json.optLong("last_test_timestamp", 0L),
                    lastHeartbeatTimestamp = System.currentTimeMillis()
                )
                Result.success(telemetry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerSoilTest(host: String, port: Int): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/api/soil-test/start"
            val payload = JSONObject().apply { put("mode", "AUTO") }.toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(JSONObject(bodyStr))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ESP32-CAM STREAM & SNAPSHOT API
    // ==========================================

    suspend fun captureCameraFrame(host: String, port: Int, path: String = "/capture"): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port$path"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} capturing frame"))
                }
                val bytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Empty frame"))
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    Result.success(bitmap)
                } else {
                    Result.failure(Exception("Failed to decode JPEG frame"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * MJPEG stream decoder flow. Yields Bitmaps directly from multipart stream.
     */
    fun streamMjpegFrames(host: String, port: Int, streamPath: String = "/stream"): Flow<Bitmap> = flow {
        val streamClient = client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for stream
            .build()
        val url = "http://$host:$port$streamPath"
        val request = Request.Builder().url(url).build()

        var inputStream: InputStream? = null
        try {
            val response = streamClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Stream failed HTTP ${response.code}")
            }
            inputStream = response.body?.byteStream() ?: throw Exception("Null stream body")

            val buffer = ByteArray(4096)
            val byteAccumulator = ByteArrayOutputStream()
            var prevByte = -1

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                for (i in 0 until bytesRead) {
                    val currByte = buffer[i].toInt() and 0xFF
                    byteAccumulator.write(currByte)

                    // Check for JPEG SOI (0xFF, 0xD8) and EOI (0xFF, 0xD9)
                    if (prevByte == 0xFF && currByte == 0xD9) {
                        val jpegBytes = byteAccumulator.toByteArray()
                        val startIndex = findJpegStart(jpegBytes)
                        if (startIndex != -1) {
                            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, startIndex, jpegBytes.size - startIndex)
                            if (bitmap != null) {
                                emit(bitmap)
                            }
                        }
                        byteAccumulator.reset()
                    }
                    prevByte = currByte
                }
            }
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun findJpegStart(data: ByteArray): Int {
        for (i in 0 until data.size - 1) {
            if ((data[i].toInt() and 0xFF) == 0xFF && (data[i + 1].toInt() and 0xFF) == 0xD8) {
                return i
            }
        }
        return -1
    }
}
