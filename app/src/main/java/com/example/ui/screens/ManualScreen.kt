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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.ui.ManualSpeed
import com.example.ui.RobotViewModel
import com.example.ui.components.CameraStreamView
import com.example.ui.components.WaterTankIndicator

@Composable
fun ManualScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val operatingMode by viewModel.operatingMode.collectAsState()
    val frame by viewModel.currentFrame.collectAsState()
    val camTelemetry by viewModel.esp32CamTelemetry.collectAsState()
    val esp32Telemetry by viewModel.esp32Telemetry.collectAsState()
    val nodeMcuTelemetry by viewModel.nodeMcuTelemetry.collectAsState()
    val manualSpeed by viewModel.manualSpeed.collectAsState()
    val manualPan by viewModel.manualPan.collectAsState()
    val manualTilt by viewModel.manualTilt.collectAsState()

    val isManualActive = operatingMode == OperatingMode.MANUAL

    // Theme: Milk-white background, dark-green text & accents
    val milkWhiteBg = Color(0xFFFAF9F6)
    val darkGreenText = Color(0xFF0D3B2E)
    val forestGreenPrimary = Color(0xFF1B4D3E)
    val emeraldAccent = Color(0xFF2D6A4F)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(milkWhiteBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("manual_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Header & Safety Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(forestGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MANUAL OPERATING MODE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = darkGreenText
                    )
                    Text(
                        text = "Movement & Camera Servos Only. Arm, cutter, and auto soil testing are locked OFF.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // 1. Live ESP32-CAM Video Stream (Normal stream without auto YOLO overlay)
        CameraStreamView(
            frame = frame,
            camTelemetry = camTelemetry,
            detections = emptyList(),
            showYoloOverlay = false, // Strictly false in manual mode
            onReconnect = { viewModel.retryCameraStream() },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Real Telemetry Quick Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TelemetryItem(
                    label = "Front Obstacle",
                    value = esp32Telemetry.obstacleDistanceCm?.let { "${it.toInt()} cm" } ?: "Clear",
                    isWarning = esp32Telemetry.isObstacleDetected
                )
                TelemetryItem(
                    label = "Speed Setting",
                    value = manualSpeed.label,
                    isWarning = false
                )
                TelemetryItem(
                    label = "Chassis State",
                    value = if (esp32Telemetry.motorLeftSpeed > 0 || esp32Telemetry.motorRightSpeed > 0) "MOVING" else "HALTED",
                    isWarning = false
                )
            }
        }

        // 3. Speed Selection (Slow, Medium, Fast)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = forestGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Speed Selection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = darkGreenText
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ManualSpeed.values().forEach { speedOption ->
                        val isSelected = manualSpeed == speedOption
                        Button(
                            onClick = { viewModel.setManualSpeed(speedOption) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speed_${speedOption.name.lowercase()}_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) forestGreenPrimary else Color(0xFFF1F5F9),
                                contentColor = if (isSelected) Color.White else darkGreenText
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = speedOption.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Directional Movement Control Pad
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chassis Movement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = darkGreenText
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Forward Button
                Button(
                    onClick = { viewModel.sendManualMove("FORWARD") },
                    enabled = isManualActive,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("manual_move_forward_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = forestGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Forward", modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Left, Stop, Right Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left
                    Button(
                        onClick = { viewModel.sendManualMove("LEFT") },
                        enabled = isManualActive,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("manual_move_left_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = forestGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Turn Left", modifier = Modifier.size(32.dp))
                    }

                    // Stop
                    Button(
                        onClick = { viewModel.stopManualMove() },
                        enabled = isManualActive,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("manual_move_stop_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(36.dp))
                    }

                    // Right
                    Button(
                        onClick = { viewModel.sendManualMove("RIGHT") },
                        enabled = isManualActive,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("manual_move_right_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = forestGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Turn Right", modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reverse Button
                Button(
                    onClick = { viewModel.sendManualMove("BACKWARD") },
                    enabled = isManualActive,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("manual_move_reverse_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = forestGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Reverse", modifier = Modifier.size(32.dp))
                }
            }
        }

        // 5. Camera Pan & Tilt Position Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = forestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Camera Pan / Tilt Position",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = darkGreenText
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.centerCamera() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("recenter_camera_btn")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Center (90°)", fontSize = 11.sp, color = darkGreenText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pan Slider (Horizontal 0° - 180°)
                Text(
                    text = "Camera Pan: $manualPan° (Left / Right)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = darkGreenText
                )
                Slider(
                    value = manualPan.toFloat(),
                    onValueChange = { viewModel.setPanTilt(it.toInt(), manualTilt) },
                    valueRange = 0f..180f,
                    steps = 17,
                    modifier = Modifier.testTag("camera_pan_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = forestGreenPrimary,
                        activeTrackColor = forestGreenPrimary,
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tilt Slider (Vertical 0° - 180°)
                Text(
                    text = "Camera Tilt: $manualTilt° (Up / Down)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = darkGreenText
                )
                Slider(
                    value = manualTilt.toFloat(),
                    onValueChange = { viewModel.setPanTilt(manualPan, it.toInt()) },
                    valueRange = 0f..180f,
                    steps = 17,
                    modifier = Modifier.testTag("camera_tilt_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = forestGreenPrimary,
                        activeTrackColor = forestGreenPrimary,
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }

        // 6. Real Water Tank Display
        WaterTankIndicator(
            telemetry = nodeMcuTelemetry,
            isDarkTheme = false
        )
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    isWarning: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) Color(0xFFDC2626) else Color(0xFF0D3B2E)
        )
    }
}
