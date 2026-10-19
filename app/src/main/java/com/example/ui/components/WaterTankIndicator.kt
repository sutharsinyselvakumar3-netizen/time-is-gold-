package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NodeMcuSensorTelemetry
import com.example.data.model.WaterLevelState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@Composable
fun WaterTankIndicator(
    telemetry: NodeMcuSensorTelemetry,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state = if (!telemetry.connected) WaterLevelState.DISCONNECTED else telemetry.waterState

    val statusColor = when (state) {
        WaterLevelState.AVAILABLE -> Color(0xFF00B4D8)
        WaterLevelState.LOW_NOT_AVAILABLE -> Color(0xFFE63946)
        WaterLevelState.SENSOR_ERROR -> Color(0xFFF77F00)
        WaterLevelState.DISCONNECTED -> Color(0xFF6C757D)
    }

    val statusLabel = when (state) {
        WaterLevelState.AVAILABLE -> "WATER AVAILABLE"
        WaterLevelState.LOW_NOT_AVAILABLE -> "LOW / NOT AVAILABLE"
        WaterLevelState.SENSOR_ERROR -> "SENSOR ERROR"
        WaterLevelState.DISCONNECTED -> "DISCONNECTED"
    }

    val cardBg = if (isDarkTheme) Color(0xFF1C2541) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF0D3B2E)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_tank_indicator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Graphic Tank representation
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, if (isDarkTheme) Color(0xFF4A5568) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            ) {
                TankGraphic(
                    waterState = state,
                    fluidColor = statusColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right: Real telemetry details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Water tank status",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Water Tank Monitor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (state) {
                            WaterLevelState.AVAILABLE -> Icons.Default.CheckCircle
                            WaterLevelState.LOW_NOT_AVAILABLE -> Icons.Default.Warning
                            WaterLevelState.SENSOR_ERROR -> Icons.Default.Warning
                            WaterLevelState.DISCONNECTED -> Icons.Default.WifiOff
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real sensor data breakdown (No fake percentages)
                val rawInfo = if (telemetry.connected && telemetry.waterReadingRaw != null) {
                    "PCB Sensor: ${if (telemetry.isWaterAvailable) "WET (Submerged)" else "DRY (Exposed)"} [Raw: ${telemetry.waterReadingRaw}]"
                } else if (telemetry.connected) {
                    "PCB Sensor: ${if (telemetry.isWaterAvailable) "Contact Closed (Water Detected)" else "Contact Open (Empty)"}"
                } else {
                    "Sensor offline — Check NodeMCU Wi-Fi"
                }

                Text(
                    text = rawInfo,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.8f)
                )

                val lastReadTime = if (telemetry.lastHeartbeatTimestamp > 0) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(telemetry.lastHeartbeatTimestamp))
                } else {
                    "Never"
                }

                Text(
                    text = "Last updated: $lastReadTime",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TankGraphic(
    waterState: WaterLevelState,
    fluidColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Measurement ticks on the side
        for (i in 1..4) {
            val tickY = height * (i / 5f)
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(width - 8f, tickY),
                end = Offset(width, tickY),
                strokeWidth = 2f
            )
        }

        when (waterState) {
            WaterLevelState.AVAILABLE -> {
                // High water level with subtle animated wave
                val fillHeight = height * 0.75f
                val waterTop = height - fillHeight

                val path = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waterTop)
                    // Wave curve
                    for (x in 0..width.toInt() step 4) {
                        val xF = x.toFloat()
                        val waveY = waterTop + sin(xF / 12f + waveOffset) * 2.5f
                        lineTo(xF, waveY)
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(path, color = fluidColor.copy(alpha = 0.85f))
            }
            WaterLevelState.LOW_NOT_AVAILABLE -> {
                // Depleted water level (only bottom residue)
                val fillHeight = height * 0.12f
                val waterTop = height - fillHeight
                drawRect(
                    color = fluidColor.copy(alpha = 0.8f),
                    topLeft = Offset(0f, waterTop),
                    size = Size(width, fillHeight)
                )
            }
            WaterLevelState.SENSOR_ERROR -> {
                // Diagonal stripes for error
                drawRoundRect(
                    color = fluidColor.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            WaterLevelState.DISCONNECTED -> {
                // Empty dimmed tank
            }
        }
    }
}
