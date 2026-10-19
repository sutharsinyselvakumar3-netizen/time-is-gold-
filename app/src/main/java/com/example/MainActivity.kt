package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.OperatingMode
import com.example.ui.RobotViewModel
import com.example.ui.components.HeaderBar
import com.example.ui.screens.AutoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ManualScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OnionBotApp()
            }
        }
    }
}

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    MANUAL("Manual", Icons.Default.Agriculture),
    AUTO("Auto Vision", Icons.Default.AutoAwesome),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun OnionBotApp(viewModel: RobotViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    val operatingMode by viewModel.operatingMode.collectAsState()
    val esp32Telemetry by viewModel.esp32Telemetry.collectAsState()
    val nodeMcuTelemetry by viewModel.nodeMcuTelemetry.collectAsState()
    val esp32CamTelemetry by viewModel.esp32CamTelemetry.collectAsState()
    val pendingModeChange by viewModel.pendingModeChange.collectAsState()

    // Dynamic Screen & Navigation Bar Colors
    val isAutoScreen = currentDestination == AppDestination.AUTO
    val isManualScreen = currentDestination == AppDestination.MANUAL

    val navBarBg = when {
        isAutoScreen -> Color(0xFF0B132B)
        isManualScreen -> Color(0xFFFAF9F6)
        else -> Color.White
    }

    val selectedColor = when {
        isAutoScreen -> Color(0xFF48CAE4)
        isManualScreen -> Color(0xFF1B4D3E)
        else -> Color(0xFF1B4D3E)
    }

    val unselectedColor = when {
        isAutoScreen -> Color(0xFF64748B)
        else -> Color(0xFF94A3B8)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                operatingMode = operatingMode,
                isEsp32Connected = esp32Telemetry.connected,
                isNodeMcuConnected = nodeMcuTelemetry.connected,
                isCamConnected = esp32CamTelemetry.connected,
                onEmergencyStop = { viewModel.triggerEmergencyStop() },
                pendingModeChange = pendingModeChange,
                onConfirmModeChange = { viewModel.executeConfirmedModeSwitch() },
                onDismissModeChange = { viewModel.dismissModeConfirmation() },
                isDarkTheme = isAutoScreen
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = navBarBg,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isAutoScreen) Color(0xFF0B132B) else Color.White,
                            selectedTextColor = selectedColor,
                            indicatorColor = selectedColor,
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        ),
                        modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.HOME -> HomeScreen(viewModel = viewModel)
                AppDestination.MANUAL -> ManualScreen(viewModel = viewModel)
                AppDestination.AUTO -> AutoScreen(viewModel = viewModel)
                AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
