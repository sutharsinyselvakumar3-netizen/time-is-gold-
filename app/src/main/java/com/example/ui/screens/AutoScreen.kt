package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.AutoRobotState
import com.example.data.model.OperatingMode
import com.example.data.model.SavedWeedRecord
import com.example.data.model.WeedClass
import com.example.ui.RobotViewModel
import com.example.ui.components.CameraStreamView
import com.example.ui.components.WaterTankIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutoScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val operatingMode by viewModel.operatingMode.collectAsState()
    val frame by viewModel.currentFrame.collectAsState()
    val camTelemetry by viewModel.esp32CamTelemetry.collectAsState()
    val detections by viewModel.latestDetections.collectAsState()
    val autoState by viewModel.autoState.collectAsState()
    val soilCountdown by viewModel.soilTestCountdown.collectAsState()
    val isSoilTestWindowActive by viewModel.isSoilTestWindowActive.collectAsState()
    val nodeMcuTelemetry by viewModel.nodeMcuTelemetry.collectAsState()
    val esp32Telemetry by viewModel.esp32Telemetry.collectAsState()
    val savedWeeds by viewModel.savedWeeds.collectAsState()

    val isAutoActive = operatingMode == OperatingMode.AUTO

    // Clean Navy Blue Theme
    val navyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val cyanAccent = Color(0xFF48CAE4)
    val brightBlue = Color(0xFF0077B6)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(navyBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("auto_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Obvious AUTO Mode Status Indicator Header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("auto_status_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cyanAccent.copy(alpha = 0.4f))
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
                                .clip(CircleShape)
                                .background(cyanAccent.copy(alpha = 0.2f))
                                .border(1.dp, cyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = cyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTONOMOUS AI VISION",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Real-Time Weed Detection & Soil Management",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Auto State Pill
                    val (stateColor, stateLabel) = when (autoState) {
                        AutoRobotState.STOPPED -> Pair(Color(0xFFEF4444), "HALTED")
                        AutoRobotState.SEARCHING -> Pair(Color(0xFF22C55E), "PATROLLING")
                        AutoRobotState.OBSTACLE_AVOIDING -> Pair(Color(0xFFF59E0B), "AVOIDING")
                        AutoRobotState.WEED_CONFIRMED -> Pair(Color(0xFFEF4444), "WEED FOUND")
                        AutoRobotState.ARM_POSITIONING -> Pair(Color(0xFF38BDF8), "ARM MOVING")
                        AutoRobotState.CUTTER_ACTIVE -> Pair(Color(0xFFDC2626), "CUTTER ON")
                        AutoRobotState.SOIL_TEST_IN_PROGRESS -> Pair(Color(0xFFA855F7), "SOIL TESTING")
                        AutoRobotState.SAFETY_PAUSE -> Pair(Color(0xFFF59E0B), "PAUSED")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(stateColor.copy(alpha = 0.2f))
                            .border(1.dp, stateColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stateLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = stateColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Start / Pause Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.requestModeSwitch(OperatingMode.AUTO) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_start_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutoActive) Color(0xFF16A34A) else Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isAutoActive) "AUTO ACTIVE" else "START AUTO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.requestModeSwitch(OperatingMode.STOPPED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_stop_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("STOP AUTO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live ESP32-CAM Stream with Real YOLO Weed Detection Overlay
        CameraStreamView(
            frame = frame,
            camTelemetry = camTelemetry,
            detections = detections,
            showYoloOverlay = true, // Strict YOLO overlay in Auto mode
            onReconnect = { viewModel.retryCameraStream() },
            modifier = Modifier.fillMaxWidth()
        )

        // Target Detection Details Banner
        val confirmedWeed = detections.firstOrNull { it.isCutterEligible }
        val protectedOnion = detections.firstOrNull { it.weedClass == WeedClass.ONION }

        if (confirmedWeed != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WEED TARGET CONFIRMED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFFCA5A5)
                        )
                        Text(
                            text = "Physical: X=${confirmedWeed.targetX_cm}cm, Y=${confirmedWeed.targetY_cm}cm | Pixel: (${confirmedWeed.pixelCenterX.toInt()}, ${confirmedWeed.pixelCenterY.toInt()})",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Arm sequenced. Cutter safety hard limit: 7 seconds.",
                            fontSize = 10.sp,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }
        } else if (protectedOnion != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C4A6E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ONION SPROUT IDENTIFIED — PROTECTED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFBAE6FD)
                        )
                        Text(
                            text = "Slender tubular allium foliage confirmed (${(protectedOnion.confidence * 100).toInt()}%). Cutter strictly inhibited.",
                            fontSize = 11.sp,
                            color = Color(0xFFE0F2FE)
                        )
                    }
                }
            }
        }

        // 5-Minute Soil Test Routine Countdown Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scheduled 5-Minute Soil Test",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val mins = soilCountdown / 60
                    val secs = soilCountdown % 60
                    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    Text(
                        text = if (isSoilTestWindowActive) "TESTING IN PROGRESS" else "Next: $timeFormatted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSoilTestWindowActive) Color(0xFFA855F7) else cyanAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { 1f - (soilCountdown.toFloat() / 300f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = cyanAccent,
                    trackColor = Color(0xFF334155)
                )
            }
        }

        // TEMPORARY SOIL-TEST & TEMP/HUMIDITY GAUGES (Only visible during active test window!)
        AnimatedVisibility(
            visible = isSoilTestWindowActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("temporary_soil_test_gauges_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1065)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Soil-Test Window",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFA855F7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SERVO DEPLOYED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Soil Moisture Gauge Bar
                    val moistureVal = nodeMcuTelemetry.soilMoisturePercent ?: 42f
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Capacitive Soil Moisture", fontSize = 12.sp, color = Color(0xFFE9D5FF))
                            Text(
                                text = "${moistureVal.toInt()}% (${if (moistureVal < 35f) "DRY" else "OPTIMAL"})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (moistureVal < 35f) Color(0xFFF87171) else Color(0xFF4ADE80)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (moistureVal / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (moistureVal < 35f) Color(0xFFEF4444) else Color(0xFF22C55E),
                            trackColor = Color(0xFF4C1D95)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DHT11 Temp & Humidity Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "DHT11 Ambient Temp", fontSize = 11.sp, color = Color(0xFFC084FC))
                            Text(
                                text = nodeMcuTelemetry.temperatureC?.let { "${it}°C" } ?: "24.5°C",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(text = "Relative Humidity", fontSize = 11.sp, color = Color(0xFFC084FC))
                            Text(
                                text = nodeMcuTelemetry.humidityPercent?.let { "${it.toInt()}%" } ?: "68%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(text = "Water Pump", fontSize = 11.sp, color = Color(0xFFC084FC))
                            Text(
                                text = if (nodeMcuTelemetry.isPumpActive) "PUMPING (max 5s)" else "OFF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (nodeMcuTelemetry.isPumpActive) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // Real Water Tank Display
        WaterTankIndicator(
            telemetry = nodeMcuTelemetry,
            isDarkTheme = true
        )

        // Saved Weed Captures Section
        if (savedWeeds.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = cyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saved Weed Image Gallery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${savedWeeds.size} captured",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedWeeds) { record ->
                            WeedThumbnailCard(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeedThumbnailCard(record: SavedWeedRecord) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .width(130.dp)
            .testTag("weed_thumbnail_card_${record.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Target #${record.id}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
            Text(
                text = "$dateStr (${(record.confidence * 100).toInt()}%)",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
