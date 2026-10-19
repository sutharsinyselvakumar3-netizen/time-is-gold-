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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OperatingMode
import com.example.ui.RobotViewModel
import com.example.ui.components.WaterTankIndicator

@Composable
fun HomeScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val operatingMode by viewModel.operatingMode.collectAsState()
    val esp32Telemetry by viewModel.esp32Telemetry.collectAsState()
    val nodeMcuTelemetry by viewModel.nodeMcuTelemetry.collectAsState()
    val esp32CamTelemetry by viewModel.esp32CamTelemetry.collectAsState()
    val networkConfig by viewModel.networkConfig.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF9))
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("home_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Control Hub Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mode_control_hub_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Operational Mode Control",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D3B2E)
                        )
                        Text(
                            text = "Strict safety separation: Manual and Auto never run together",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Mode Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (operatingMode) {
                                    OperatingMode.STOPPED -> Color(0xFFFEE2E2)
                                    OperatingMode.MANUAL -> Color(0xFFDCFCE7)
                                    OperatingMode.AUTO -> Color(0xFFE0F2FE)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = operatingMode.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (operatingMode) {
                                OperatingMode.STOPPED -> Color(0xFFDC2626)
                                OperatingMode.MANUAL -> Color(0xFF15803D)
                                OperatingMode.AUTO -> Color(0xFF0284C7)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selection Buttons (Triggers safety confirmation modal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // STOPPED Button
                    Button(
                        onClick = { viewModel.requestModeSwitch(OperatingMode.STOPPED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_mode_stopped_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (operatingMode == OperatingMode.STOPPED) Color(0xFFDC2626) else Color(0xFFF1F5F9),
                            contentColor = if (operatingMode == OperatingMode.STOPPED) Color.White else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // MANUAL Button
                    Button(
                        onClick = { viewModel.requestModeSwitch(OperatingMode.MANUAL) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_mode_manual_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (operatingMode == OperatingMode.MANUAL) Color(0xFF16A34A) else Color(0xFFF1F5F9),
                            contentColor = if (operatingMode == OperatingMode.MANUAL) Color.White else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MANUAL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // AUTO Button
                    Button(
                        onClick = { viewModel.requestModeSwitch(OperatingMode.AUTO) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_mode_auto_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (operatingMode == OperatingMode.AUTO) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                            contentColor = if (operatingMode == OperatingMode.AUTO) Color.White else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AUTO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Real Water Tank Display
        WaterTankIndicator(
            telemetry = nodeMcuTelemetry,
            isDarkTheme = false
        )

        // Real Hardware Controllers Section Header
        Text(
            text = "HARDWARE CONTROLLERS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )

        // 1. ESP32 Main Controller Card
        ControllerCard(
            title = "ESP32 38-Pin Main Controller",
            ipAddress = "${networkConfig.esp32MainIp}:${networkConfig.esp32MainPort}",
            isConnected = esp32Telemetry.connected,
            icon = Icons.Default.DeveloperBoard,
            details = listOf(
                "Chassis Motors" to if (esp32Telemetry.motorLeftSpeed > 0 || esp32Telemetry.motorRightSpeed > 0) "RUNNING (${esp32Telemetry.motorLeftSpeed} PWM)" else "STOPPED",
                "Obstacle (HC-SR04)" to (esp32Telemetry.obstacleDistanceCm?.let { "${it.toInt()} cm" } ?: "Unavailable"),
                "Arm Status" to esp32Telemetry.armStatus.name,
                "Cutter Relay" to if (esp32Telemetry.isCutterActive) "ACTIVE (Safety limit: 7s)" else "OFF",
                "Camera Servos" to "Pan ${esp32Telemetry.panAngle}° / Tilt ${esp32Telemetry.tiltAngle}°",
                "Battery Sensor" to (esp32Telemetry.batteryVoltage?.let { "${it}V" } ?: "Not fitted / N/A")
            ),
            errorMessage = esp32Telemetry.errorMessage
        )

        // 2. NodeMCU Sensor Controller Card
        ControllerCard(
            title = "NodeMCU ESP8266 Sensor Controller",
            ipAddress = "${networkConfig.nodeMcuIp}:${networkConfig.nodeMcuPort}",
            isConnected = nodeMcuTelemetry.connected,
            icon = Icons.Default.Sensors,
            details = listOf(
                "PCB Water Sensor" to if (nodeMcuTelemetry.isWaterAvailable) "WATER DETECTED" else "LOW / EMPTY",
                "Soil-Test Servo" to if (nodeMcuTelemetry.isSoilServoDeployed) "DEPLOYED IN SOIL" else "HOME (Safe)",
                "Water Pump Driver" to if (nodeMcuTelemetry.isPumpActive) "PUMPING (Max: 5s)" else "OFF",
                "Scheduled Routine" to "5-minute interval (AUTO only)"
            ),
            errorMessage = nodeMcuTelemetry.errorMessage
        )

        // 3. AI-Thinker ESP32-CAM Card
        ControllerCard(
            title = "AI-Thinker ESP32-CAM (OV2640)",
            ipAddress = "${networkConfig.esp32CamIp}:${networkConfig.esp32CamPort}${networkConfig.esp32CamStreamPath}",
            isConnected = esp32CamTelemetry.connected,
            icon = Icons.Default.CameraAlt,
            details = listOf(
                "Video Resolution" to esp32CamTelemetry.resolution,
                "Streaming Rate" to if (esp32CamTelemetry.connected) "10 FPS (Active)" else "Offline",
                "Vision Tasks" to if (operatingMode == OperatingMode.AUTO) "On-Device YOLO Active" else "Raw Feed Only"
            ),
            errorMessage = esp32CamTelemetry.errorMessage
        )

        // System Logs Activity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Event & Safety Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0D3B2E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    if (systemLogs.isEmpty()) {
                        Text(
                            text = "No recent events",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    } else {
                        systemLogs.take(6).forEach { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.contains("ERROR", true) || logLine.contains("EMERGENCY", true)) Color(0xFFFF5252)
                                else if (logLine.contains("CONFIRMED", true)) Color(0xFF4ADE80)
                                else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControllerCard(
    title: String,
    ipAddress: String,
    isConnected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    details: List<Pair<String, String>>,
    errorMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isConnected) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF15803D) else Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0D3B2E)
                        )
                        Text(
                            text = ipAddress,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Connection status dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) "ONLINE" else "OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF15803D) else Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                details.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }
                }
            }

            if (!isConnected && errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connection issue: $errorMessage",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}
