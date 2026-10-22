package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.ui.screens.*
import com.example.robolab.viewmodel.ActiveScreen
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RoboLabTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RoboLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoboLabTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                BackHandler(enabled = currentScreen != ActiveScreen.HOME) {
                    viewModel.navigateTo(ActiveScreen.HOME)
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.HOME,
                                onClick = { viewModel.navigateTo(ActiveScreen.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_home")
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.WORKSHOP,
                                onClick = { viewModel.navigateTo(ActiveScreen.WORKSHOP) },
                                icon = { Icon(Icons.Default.Build, contentDescription = "Workshop") },
                                label = { Text("Workshop", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_workshop")
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.SIMULATOR,
                                onClick = { viewModel.navigateTo(ActiveScreen.SIMULATOR) },
                                icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Simulator") },
                                label = { Text("Simulator", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_simulator")
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.PROGRAMMING,
                                onClick = { viewModel.navigateTo(ActiveScreen.PROGRAMMING) },
                                icon = { Icon(Icons.Default.Code, contentDescription = "Coding") },
                                label = { Text("Coding", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_programming")
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.PROJECTS,
                                onClick = { viewModel.navigateTo(ActiveScreen.PROJECTS) },
                                icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                                label = { Text("Projects", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_projects")
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.EXPERIMENTS,
                                onClick = { viewModel.navigateTo(ActiveScreen.EXPERIMENTS) },
                                icon = { Icon(Icons.Default.Science, contentDescription = "Labs") },
                                label = { Text("Labs", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_item_experiments")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            ActiveScreen.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            ActiveScreen.WORKSHOP -> WorkshopScreen(
                                viewModel = viewModel
                            )
                            ActiveScreen.SIMULATOR -> SimulatorScreen(
                                viewModel = viewModel
                            )
                            ActiveScreen.PROGRAMMING -> ProgrammingScreen(
                                viewModel = viewModel
                            )
                            ActiveScreen.PROJECTS -> ProjectsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            ActiveScreen.EXPERIMENTS -> ExperimentsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
