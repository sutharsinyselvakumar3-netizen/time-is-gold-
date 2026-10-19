package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Esp32CamTelemetry
import com.example.data.model.WeedClass
import com.example.data.model.YoloDetection

@Composable
fun CameraStreamView(
    frame: Bitmap?,
    camTelemetry: Esp32CamTelemetry,
    detections: List<YoloDetection>,
    showYoloOverlay: Boolean,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = camTelemetry.connected && frame != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .testTag("camera_stream_container"),
        contentAlignment = Alignment.Center
    ) {
        if (isConnected && frame != null) {
            // Render Live Camera Frame
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "ESP32-CAM Live Stream",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("live_camera_image"),
                contentScale = ContentScale.Crop
            )

            // Auto Mode YOLO Bounding Box Overlay
            if (showYoloOverlay) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("yolo_overlay_canvas")) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    detections.forEach { det ->
                        val box = det.boundingBox
                        val leftPx = box.left * canvasWidth
                        val topPx = box.top * canvasHeight
                        val boxWidthPx = box.width * canvasWidth
                        val boxHeightPx = box.height * canvasHeight

                        val strokeColor = when (det.weedClass) {
                            WeedClass.WEED -> Color(0xFFFF3B30) // Red for weed target
                            WeedClass.ONION -> Color(0xFF00E5FF) // Bright cyan for protected onion
                            WeedClass.UNKNOWN -> Color(0xFFFFB300) // Amber for unclassified
                        }

                        // Draw bounding box rectangle
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(leftPx, topPx),
                            size = Size(boxWidthPx, boxHeightPx),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw pixel center target crosshair
                        val centerPxX = det.pixelCenterX * (canvasWidth / frame.width.coerceAtLeast(1))
                        val centerPxY = det.pixelCenterY * (canvasHeight / frame.height.coerceAtLeast(1))
                        drawLine(
                            color = strokeColor,
                            start = Offset(centerPxX - 10f, centerPxY),
                            end = Offset(centerPxX + 10f, centerPxY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(centerPxX, centerPxY - 10f),
                            end = Offset(centerPxX, centerPxY + 10f),
                            strokeWidth = 2f
                        )

                        // Draw text label on canvas
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            isFakeBoldText = true
                            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                        }

                        val bgPaint = android.graphics.Paint().apply {
                            color = when (det.weedClass) {
                                WeedClass.WEED -> android.graphics.Color.argb(220, 230, 57, 70)
                                WeedClass.ONION -> android.graphics.Color.argb(220, 0, 180, 216)
                                WeedClass.UNKNOWN -> android.graphics.Color.argb(220, 100, 116, 139)
                            }
                        }

                        val labelText = when (det.weedClass) {
                            WeedClass.WEED -> "WEED ${(det.confidence * 100).toInt()}% [${det.targetX_cm}, ${det.targetY_cm}cm]"
                            WeedClass.ONION -> "ONION ${(det.confidence * 100).toInt()}% (PROTECTED)"
                            WeedClass.UNKNOWN -> "UNKNOWN ${(det.confidence * 100).toInt()}%"
                        }

                        val textWidth = paint.measureText(labelText)
                        val textHeight = 36f

                        drawContext.canvas.nativeCanvas.drawRect(
                            leftPx,
                            (topPx - textHeight).coerceAtLeast(0f),
                            leftPx + textWidth + 16f,
                            topPx.coerceAtLeast(textHeight),
                            bgPaint
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            leftPx + 8f,
                            (topPx - 8f).coerceAtLeast(24f),
                            paint
                        )
                    }
                }
            }

            // Top-left Stream Stats Pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ESP32-CAM ${camTelemetry.resolution} | LIVE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Genuine Camera Disconnected State (NO fake placeholder image)
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .testTag("camera_disconnected_banner"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Camera Disconnected",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = camTelemetry.errorMessage ?: "Check AI-Thinker ESP32-CAM power and Wi-Fi endpoint",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onReconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("camera_reconnect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconnect Stream", fontSize = 13.sp)
                }
            }
        }
    }
}
