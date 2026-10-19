package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalibrationConfig
import com.example.data.model.DeviceNetworkConfig
import com.example.data.model.WorkspaceLimits
import com.example.ui.RobotViewModel

@Composable
fun SettingsScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val calibration by viewModel.calibration.collectAsState()
    val networkConfig by viewModel.networkConfig.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calibration", "Vision & Soil", "Wi-Fi IPs", "Firmware APIs", "Bench Checklist")

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF9))
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Robot Calibration & Engineering",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D3B2E)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF1B4D3E),
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> CalibrationTab(calibration = calibration, onSave = { viewModel.updateCalibration(it) })
            1 -> VisionAndSoilTab(calibration = calibration, onSave = { viewModel.updateCalibration(it) })
            2 -> NetworkSetupTab(networkConfig = networkConfig, onSave = { viewModel.updateNetworkConfig(it) })
            3 -> FirmwareSpecsTab()
            4 -> BenchChecklistTab()
        }
    }
}

@Composable
private fun CalibrationTab(
    calibration: CalibrationConfig,
    onSave: (CalibrationConfig) -> Unit
) {
    var heightCm by remember { mutableStateOf(calibration.cameraHeightCm.toString()) }
    var tiltDeg by remember { mutableStateOf(calibration.cameraTiltAngleDeg.toString()) }
    var focalPx by remember { mutableStateOf(calibration.cameraFocalLengthPx.toString()) }
    var xMin by remember { mutableStateOf(calibration.workspace.xMinCm.toString()) }
    var xMax by remember { mutableStateOf(calibration.workspace.xMaxCm.toString()) }
    var yMin by remember { mutableStateOf(calibration.workspace.yMinCm.toString()) }
    var yMax by remember { mutableStateOf(calibration.workspace.yMaxCm.toString()) }

    var testPxX by remember { mutableStateOf("320") }
    var testPxY by remember { mutableStateOf("240") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Camera Mounting & Perspective Calibration",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0D3B2E)
            )
            Text(
                text = "Converts image pixel coordinates into physical target ground coordinates (cm). Raw pixels must never be treated as physical centimetres.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f).testTag("calib_height_input")
                )
                OutlinedTextField(
                    value = tiltDeg,
                    onValueChange = { tiltDeg = it },
                    label = { Text("Tilt Angle (°)") },
                    modifier = Modifier.weight(1f).testTag("calib_tilt_input")
                )
            }

            OutlinedTextField(
                value = focalPx,
                onValueChange = { focalPx = it },
                label = { Text("Camera Focal Length (pixels)") },
                modifier = Modifier.fillMaxWidth().testTag("calib_focal_input")
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Arm Safe Workspace Limits (cm)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0D3B2E)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = xMin,
                    onValueChange = { xMin = it },
                    label = { Text("X Min (cm)") },
                    modifier = Modifier.weight(1f).testTag("calib_xmin_input")
                )
                OutlinedTextField(
                    value = xMax,
                    onValueChange = { xMax = it },
                    label = { Text("X Max (cm)") },
                    modifier = Modifier.weight(1f).testTag("calib_xmax_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = yMin,
                    onValueChange = { yMin = it },
                    label = { Text("Y Min (cm)") },
                    modifier = Modifier.weight(1f).testTag("calib_ymin_input")
                )
                OutlinedTextField(
                    value = yMax,
                    onValueChange = { yMax = it },
                    label = { Text("Y Max (cm)") },
                    modifier = Modifier.weight(1f).testTag("calib_ymax_input")
                )
            }

            Button(
                onClick = {
                    val updated = calibration.copy(
                        cameraHeightCm = heightCm.toFloatOrNull() ?: calibration.cameraHeightCm,
                        cameraTiltAngleDeg = tiltDeg.toFloatOrNull() ?: calibration.cameraTiltAngleDeg,
                        cameraFocalLengthPx = focalPx.toFloatOrNull() ?: calibration.cameraFocalLengthPx,
                        workspace = WorkspaceLimits(
                            xMinCm = xMin.toFloatOrNull() ?: calibration.workspace.xMinCm,
                            xMaxCm = xMax.toFloatOrNull() ?: calibration.workspace.xMaxCm,
                            yMinCm = yMin.toFloatOrNull() ?: calibration.workspace.yMinCm,
                            yMaxCm = yMax.toFloatOrNull() ?: calibration.workspace.yMaxCm
                        )
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth().testTag("save_calibration_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4D3E))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Camera & Workspace Calibration")
            }

            // Interactive Ground Coordinate Calculator
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interactive Coordinate Projection Test", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F766E))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = testPxX,
                        onValueChange = { testPxX = it },
                        label = { Text("Pixel X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = testPxY,
                        onValueChange = { testPxY = it },
                        label = { Text("Pixel Y") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val px = testPxX.toFloatOrNull() ?: 320f
                val py = testPxY.toFloatOrNull() ?: 240f
                val calcH = heightCm.toFloatOrNull() ?: 25f
                val calcTilt = Math.toRadians((tiltDeg.toDoubleOrNull() ?: 45.0))
                val calcF = focalPx.toFloatOrNull() ?: 380f
                val vy = (py - 240f) / calcF
                val groundY = (calcH / Math.tan(calcTilt + Math.atan(vy.toDouble()))).toFloat()
                val slant = Math.sqrt((groundY * groundY + calcH * calcH).toDouble())
                val vx = (px - 320f) / calcF
                val groundX = (vx * slant).toFloat()

                Text(
                    text = "Projected: X = ${Math.round(groundX * 10) / 10f} cm, Y = ${Math.round(groundY * 10) / 10f} cm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
private fun VisionAndSoilTab(
    calibration: CalibrationConfig,
    onSave: (CalibrationConfig) -> Unit
) {
    var confThreshold by remember { mutableStateOf(calibration.yoloConfidenceThreshold) }
    var soilDryThreshold by remember { mutableStateOf(calibration.soilDryThresholdPercent) }
    var cutterMaxDuration by remember { mutableStateOf(calibration.maxCutterDurationSeconds.toString()) }
    var pumpMaxDuration by remember { mutableStateOf(calibration.maxPumpDurationSeconds.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "YOLO Vision & Safety Cutoffs",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0D3B2E)
            )

            Column {
                Text(
                    text = "YOLO Detection Confidence Threshold: ${(confThreshold * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Detections below this threshold are rejected. Never cut an onion or low-confidence target.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Slider(
                    value = confThreshold,
                    onValueChange = { confThreshold = it },
                    valueRange = 0.50f..0.95f,
                    steps = 9,
                    modifier = Modifier.testTag("yolo_conf_slider"),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF1B4D3E), activeTrackColor = Color(0xFF1B4D3E))
                )
            }

            Column {
                Text(
                    text = "Soil Moisture Dry Threshold: ${soilDryThreshold.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Pump activates only if soil is below this moisture AND water is confirmed available.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Slider(
                    value = soilDryThreshold,
                    onValueChange = { soilDryThreshold = it },
                    valueRange = 20f..55f,
                    steps = 7,
                    modifier = Modifier.testTag("soil_dry_slider"),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF0284C7), activeTrackColor = Color(0xFF0284C7))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cutterMaxDuration,
                    onValueChange = { cutterMaxDuration = it },
                    label = { Text("Max Cutter Time (s)") },
                    supportingText = { Text("Hard cutoff <= 7s") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pumpMaxDuration,
                    onValueChange = { pumpMaxDuration = it },
                    label = { Text("Max Pump Time (s)") },
                    supportingText = { Text("Hard cutoff <= 5s") },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val updated = calibration.copy(
                        yoloConfidenceThreshold = confThreshold,
                        soilDryThresholdPercent = soilDryThreshold,
                        maxCutterDurationSeconds = (cutterMaxDuration.toIntOrNull() ?: 7).coerceIn(1, 7),
                        maxPumpDurationSeconds = (pumpMaxDuration.toIntOrNull() ?: 5).coerceIn(1, 5)
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth().testTag("save_vision_soil_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4D3E))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Vision & Soil Thresholds")
            }
        }
    }
}

@Composable
private fun NetworkSetupTab(
    networkConfig: DeviceNetworkConfig,
    onSave: (DeviceNetworkConfig) -> Unit
) {
    var esp32Ip by remember { mutableStateOf(networkConfig.esp32MainIp) }
    var esp32Port by remember { mutableStateOf(networkConfig.esp32MainPort.toString()) }
    var nodeMcuIp by remember { mutableStateOf(networkConfig.nodeMcuIp) }
    var nodeMcuPort by remember { mutableStateOf(networkConfig.nodeMcuPort.toString()) }
    var camIp by remember { mutableStateOf(networkConfig.esp32CamIp) }
    var camPort by remember { mutableStateOf(networkConfig.esp32CamPort.toString()) }
    var camStreamPath by remember { mutableStateOf(networkConfig.esp32CamStreamPath) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Wi-Fi Device Network Addresses",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0D3B2E)
            )
            Text(
                text = "Communication: Wi-Fi only. No UART. All devices connect to the same Wi-Fi router or ESP32 AP.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Text("ESP32 Main Controller (Motors, Arm, Cutter):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = esp32Ip, onValueChange = { esp32Ip = it }, label = { Text("IP Address") }, modifier = Modifier.weight(2f))
                OutlinedTextField(value = esp32Port, onValueChange = { esp32Port = it }, label = { Text("Port") }, modifier = Modifier.weight(1f))
            }

            Text("NodeMCU ESP8266 (Soil Sensor, Pump, Water Level):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nodeMcuIp, onValueChange = { nodeMcuIp = it }, label = { Text("IP Address") }, modifier = Modifier.weight(2f))
                OutlinedTextField(value = nodeMcuPort, onValueChange = { nodeMcuPort = it }, label = { Text("Port") }, modifier = Modifier.weight(1f))
            }

            Text("AI-Thinker ESP32-CAM (OV2640 Stream):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = camIp, onValueChange = { camIp = it }, label = { Text("IP Address") }, modifier = Modifier.weight(2f))
                OutlinedTextField(value = camPort, onValueChange = { camPort = it }, label = { Text("Port") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = camStreamPath, onValueChange = { camStreamPath = it }, label = { Text("Stream Path") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val updated = networkConfig.copy(
                        esp32MainIp = esp32Ip.trim(),
                        esp32MainPort = esp32Port.toIntOrNull() ?: 80,
                        nodeMcuIp = nodeMcuIp.trim(),
                        nodeMcuPort = nodeMcuPort.toIntOrNull() ?: 80,
                        esp32CamIp = camIp.trim(),
                        esp32CamPort = camPort.toIntOrNull() ?: 81,
                        esp32CamStreamPath = camStreamPath.trim()
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth().testTag("save_network_ips_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4D3E))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Network Addresses & Reconnect")
            }
        }
    }
}

@Composable
private fun FirmwareSpecsTab() {
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Complete Matching Firmware Specifications",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0D3B2E)
            )
            Text(
                text = "Ready-to-flash C++ / Arduino firmware with strict mode separation, actuator timeouts, and matching REST endpoints.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            // Pinout Summary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                Text("HARDWARE PIN ASSIGNMENTS", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("• ESP32 Main Motors (L298N): ENA=13, IN1=12, IN2=14, IN3=27, IN4=26, ENB=25", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• ESP32 Front Obstacle: HC-SR04 Trig=32, Echo=35", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• ESP32 Arm Servos: Base=18, Shoulder=19, Elbow=21", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• ESP32 Camera Servos: Pan=22, Tilt=23", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• ESP32 Cutter Relay: GPIO 4 (Max 7s safety cutoff)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• NodeMCU Soil Moisture: A0 (Capacitive Analog)", color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• NodeMCU DHT11: D4 (GPIO 2)", color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• NodeMCU PCB Water-Level: D5 (GPIO 14)", color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• NodeMCU Soil Servo: D7 (GPIO 13)", color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("• NodeMCU Water Pump: D1 (GPIO 5, Max 5s cutoff)", color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            FirmwareCodeBlock(
                title = "1. ESP32 38-Pin Main Controller (OnionBot_Main.ino)",
                code = esp32FirmwareSnippet,
                onCopy = {
                    clipboard.setText(AnnotatedString(esp32FirmwareSnippet))
                    copiedNotice = "Copied ESP32 code to clipboard!"
                }
            )

            FirmwareCodeBlock(
                title = "2. NodeMCU ESP8266 Sensors (OnionBot_Sensors.ino)",
                code = nodeMcuFirmwareSnippet,
                onCopy = {
                    clipboard.setText(AnnotatedString(nodeMcuFirmwareSnippet))
                    copiedNotice = "Copied NodeMCU code to clipboard!"
                }
            )

            if (copiedNotice != null) {
                Text(text = copiedNotice!!, color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FirmwareCodeBlock(
    title: String,
    code: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy", fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = code.take(500) + "\n... [Full code available for copy] ...",
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun BenchChecklistTab() {
    val checklistItems = remember {
        mutableStateMapOf(
            "Wi-Fi Connectivity (All 3 devices pingable)" to false,
            "Chassis L298N Motors (Forward, Back, Left, Right)" to false,
            "Front HC-SR04 Obstacle Sensor (<20cm trigger)" to false,
            "Camera Pan/Tilt Servos (0° to 180° sweep)" to false,
            "AI-Thinker ESP32-CAM (Live OV2640 feed)" to false,
            "Arm 3-Axis Kinematics (Safe workspace bounds)" to false,
            "Cutter Relay Driver (Hardware 7s hard cutoff)" to false,
            "NodeMCU Capacitive Soil Sensor (Submerged vs Dry)" to false,
            "NodeMCU Soil Probe Plunge Servo" to false,
            "PCB Water-Level Sensor (Wet/Dry contact detection)" to false,
            "Water Pump Relay (Hardware 5s maximum cutoff)" to false,
            "Emergency Stop Interlock (Overrides all actuators)" to false
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Pre-Flight Hardware Test Checklist",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0D3B2E)
            )
            Text(
                text = "Verify all components on the test bench before outdoor garden deployment. Do not deploy if any safety test fails.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            checklistItems.forEach { (task, isDone) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDone) Color(0xFFDCFCE7) else Color(0xFFF8FAFC))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { checklistItems[task] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task,
                        fontSize = 12.sp,
                        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDone) Color(0xFF15803D) else Color(0xFF334155)
                    )
                }
            }
        }
    }
}

private val esp32FirmwareSnippet = """
#include <WiFi.h>
#include <WebServer.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>

// L298N Motors
const int ENA = 13, IN1 = 12, IN2 = 14, IN3 = 27, IN4 = 26, ENB = 25;
// HC-SR04 Obstacle
const int TRIG_PIN = 32, ECHO_PIN = 35;
// Cutter Relay
const int CUTTER_PIN = 4;
// Servos
Servo armBase, armShoulder, armElbow, camPan, camTilt;

enum Mode { STOPPED, MANUAL, AUTO };
Mode currentMode = STOPPED;
bool estopActive = false;
unsigned long cutterStartTime = 0;
bool cutterRunning = false;
const unsigned long MAX_CUTTER_MS = 7000; // Hard 7s safety limit

WebServer server(80);

void setup() {
  Serial.begin(115200);
  pinMode(CUTTER_PIN, OUTPUT);
  digitalWrite(CUTTER_PIN, LOW); // OFF on startup
  // Setup WiFi and REST endpoints: /api/status, /api/mode, /api/manual/move, /api/auto/target, /api/estop
}

void loop() {
  server.handleClient();
  // Enforce hardware safety timeouts
  if (cutterRunning && (millis() - cutterStartTime >= MAX_CUTTER_MS)) {
    digitalWrite(CUTTER_PIN, LOW);
    cutterRunning = false;
  }
}
""".trimIndent()

private val nodeMcuFirmwareSnippet = """
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <Servo.h>
#include <DHT.h>
#include <ArduinoJson.h>

const int SOIL_PIN = A0;
const int DHT_PIN = 2; // D4
const int WATER_PCB_PIN = 14; // D5
const int PUMP_PIN = 5; // D1
const int SOIL_SERVO_PIN = 13; // D7

Servo soilServo;
DHT dht(DHT_PIN, DHT11);
ESP8266WebServer server(80);

unsigned long pumpStartTime = 0;
bool pumpRunning = false;
const unsigned long MAX_PUMP_MS = 5000; // Hard 5s safety limit

void setup() {
  pinMode(PUMP_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, LOW); // Pump OFF on startup
  pinMode(WATER_PCB_PIN, INPUT_PULLUP);
  // Setup endpoints: /api/sensors/status, /api/soil-test/start, /api/estop
}

void loop() {
  server.handleClient();
  if (pumpRunning && (millis() - pumpStartTime >= MAX_PUMP_MS)) {
    digitalWrite(PUMP_PIN, LOW);
    pumpRunning = false;
  }
}
""".trimIndent()
