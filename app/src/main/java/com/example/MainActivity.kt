package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.McLocalization
import com.example.core.McWakeService
import com.example.core.configureAppCheck
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CodingStudioScreen
import com.example.ui.screens.CryptoTradingScreen
import com.example.ui.screens.DeviceHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.McAssistantTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.McViewModel

const val ROUTE_ASSISTANT = "assistant"
const val ROUTE_CRYPTO = "crypto"
const val ROUTE_CONTROL = "control"
const val ROUTE_CODE = "code"
const val ROUTE_SETTINGS = "settings"

private data class McNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val launchedFromWake = intent?.getBooleanExtra(McWakeService.EXTRA_FROM_WAKE_SERVICE, false) == true

        setContent {
            McAssistantTheme {
                McAppRoot(launchedFromWake = launchedFromWake)
            }
        }
    }
}

@Composable
private fun McAppRoot(
    launchedFromWake: Boolean,
    viewModel: McViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { McLocalization.get(settings.language) }

    LaunchedEffect(launchedFromWake) {
        if (launchedFromWake) {
            viewModel.triggerMcWakeResponse()
        }
    }

    LaunchedEffect(toastMsg) {
        if (!toastMsg.isNullOrBlank()) {
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val tabs = remember(strings) {
        listOf(
            McNavTab(ROUTE_ASSISTANT, strings.navAssistant, Icons.Filled.GraphicEq),
            McNavTab(ROUTE_CRYPTO, strings.navCrypto, Icons.AutoMirrored.Filled.ShowChart),
            McNavTab(ROUTE_CONTROL, strings.navControl, Icons.Filled.Devices),
            McNavTab(ROUTE_CODE, strings.navCode, Icons.Filled.Code),
            McNavTab(ROUTE_SETTINGS, strings.navSettings, Icons.Filled.Settings)
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_ASSISTANT

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_ASSISTANT) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = ObsidianElevated
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_ASSISTANT) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = ObsidianElevated
                                ),
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_ASSISTANT,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_ASSISTANT) {
                        AssistantScreen(
                            viewModel = viewModel,
                            onNavigateToCrypto = {
                                navController.navigate(ROUTE_CRYPTO) {
                                    popUpTo(ROUTE_ASSISTANT) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(ROUTE_CRYPTO) {
                        CryptoTradingScreen(viewModel = viewModel)
                    }
                    composable(ROUTE_CONTROL) {
                        DeviceHubScreen(viewModel = viewModel)
                    }
                    composable(ROUTE_CODE) {
                        CodingStudioScreen(viewModel = viewModel)
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
