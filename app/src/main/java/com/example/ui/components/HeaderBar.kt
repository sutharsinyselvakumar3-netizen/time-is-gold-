package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OperatingMode

@Composable
fun HeaderBar(
    operatingMode: OperatingMode,
    isEsp32Connected: Boolean,
    isNodeMcuConnected: Boolean,
    isCamConnected: Boolean,
    onEmergencyStop: () -> Unit,
    pendingModeChange: OperatingMode?,
    onConfirmModeChange: () -> Unit,
    onDismissModeChange: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0D3B2E)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = barBg,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: App branding & Active mode pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ONIONBOT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Mode Pill
                    val (modeBg, modeText, modeBorder) = when (operatingMode) {
                        OperatingMode.STOPPED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFFEF4444))
                        OperatingMode.MANUAL -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF22C55E))
                        OperatingMode.AUTO -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Color(0xFF38BDF8))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(modeBg)
                            .border(1.dp, modeBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("operating_mode_pill")
                    ) {
                        Text(
                            text = operatingMode.name,
                            color = modeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: Emergency Stop Button
                Button(
                    onClick = onEmergencyStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("emergency_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = "Emergency Stop",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "E-STOP",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hardware Connection Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeviceStatusBadge(label = "ESP32 MAIN", isConnected = isEsp32Connected, isDarkTheme = isDarkTheme)
                DeviceStatusBadge(label = "NODEMCU SENSORS", isConnected = isNodeMcuConnected, isDarkTheme = isDarkTheme)
                DeviceStatusBadge(label = "ESP32-CAM", isConnected = isCamConnected, isDarkTheme = isDarkTheme)
            }
        }
    }

    // Confirmation Modal for Mode Switching
    if (pendingModeChange != null) {
        AlertDialog(
            onDismissRequest = onDismissModeChange,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Mode Switch")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Switch operating mode to ${pendingModeChange.name}?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SAFETY ENFORCEMENT:\n• MANUAL and AUTO must never run simultaneously.\n• The active mode will be stopped first and acknowledged by the ESP32 controller.\n• If communication fails, the robot will remain safely in STOPPED mode.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmModeChange,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pendingModeChange == OperatingMode.STOPPED) Color(0xFFDC2626) else Color(0xFF16A34A)
                    ),
                    modifier = Modifier.testTag("confirm_mode_switch_btn")
                ) {
                    Text("Confirm Switch")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissModeChange,
                    modifier = Modifier.testTag("cancel_mode_switch_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceStatusBadge(
    label: String,
    isConnected: Boolean,
    isDarkTheme: Boolean
) {
    val dotColor = if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444)
    val textColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
