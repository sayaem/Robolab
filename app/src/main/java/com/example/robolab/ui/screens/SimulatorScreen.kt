package com.example.robolab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.interpreter.InterpreterStatus
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.ui.components.InteractiveViewport3D
import com.example.robolab.ui.components.TelemetryBadge
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    viewModel: RoboLabViewModel
) {
    val components by viewModel.components.collectAsState()
    val obstacles by viewModel.obstacles.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val trajectory by viewModel.trajectoryHistory.collectAsState()
    val isRunning by viewModel.isSimRunning.collectAsState()
    val isPaused by viewModel.isSimPaused.collectAsState()
    val speedMultiplier by viewModel.simSpeedMultiplier.collectAsState()
    val graphicsProfile by viewModel.graphicsProfile.collectAsState()
    val interpreterStatus by viewModel.interpreterStatus.collectAsState()
    val activeExperiment by viewModel.activeExperiment.collectAsState()
    val experimentCompleted by viewModel.experimentCompleted.collectAsState()

    var showTelemetryDrawer by remember { mutableStateOf(false) }
    var controlMode by remember { mutableStateOf(0) } // 0: Autonomous Program, 1: Manual Joystick

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("simulator_screen")
    ) {
        EngineeringHeader(
            title = "Physics Simulator",
            subtitle = if (activeExperiment != null) activeExperiment!!.title else "Real-Time Arena",
            badgeText = if (isRunning) "RUNNING" else "STANDBY",
            actions = {
                // Speed selection button
                IconButton(onClick = {
                    val next = when (speedMultiplier) {
                        1f -> 2f
                        2f -> 4f
                        else -> 1f
                    }
                    viewModel.setSpeedMultiplier(next)
                }) {
                    Text("${speedMultiplier.toInt()}X", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 12.sp)
                }

                IconButton(onClick = { showTelemetryDrawer = !showTelemetryDrawer }) {
                    Icon(Icons.Default.QueryStats, contentDescription = "Telemetry", tint = ElectricCyan)
                }
            }
        )

        // Active Experiment Goal Banner (if launched)
        if (activeExperiment != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (experimentCompleted) SensorGreen.copy(alpha = 0.15f) else DarkSurfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (experimentCompleted) SensorGreen else ElectricCyan.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (experimentCompleted) "LAB OBJECTIVE COMPLETE!" else "EXPERIMENT CHALLENGE:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (experimentCompleted) SensorGreen else CyberAmber
                        )
                        Text(
                            text = activeExperiment!!.successCondition,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                    if (experimentCompleted) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Success", tint = CyberAmber, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // 3D Simulation Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            InteractiveViewport3D(
                camera = viewModel.camera,
                components = components,
                robotPoseX = telemetry.posX,
                robotPoseZ = telemetry.posZ,
                robotHeadingDeg = telemetry.headingDeg,
                obstacles = obstacles,
                telemetry = telemetry,
                graphicsProfile = graphicsProfile,
                isWorkshopMode = false,
                trajectoryHistory = trajectory,
                modifier = Modifier.fillMaxSize()
            )

            // Top-left HUD Overlay on Viewport: Speed & Distance
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TelemetryBadge(
                    label = "POS",
                    value = "X:${"%.1f".format(telemetry.posX)} Z:${"%.1f".format(telemetry.posZ)}",
                    accentColor = ElectricCyan
                )
                TelemetryBadge(
                    label = "VEL",
                    value = "${"%.2f".format(telemetry.linearVelocity)} m/s",
                    accentColor = TechBlue
                )
                TelemetryBadge(
                    label = "SENSOR",
                    value = "${"%.2f".format(telemetry.distanceSensorReadingM)} m",
                    accentColor = if (telemetry.distanceSensorReadingM < 0.45f) HazardRed else SensorGreen
                )

                if (telemetry.hasCollision) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HazardRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COLLISION: ${telemetry.collisionObstacleName ?: "OBSTACLE"}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Radar Mini Scope (LiDAR Sweep preview) in bottom corner
            if (telemetry.lidarReadings.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = 0.85f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Radar,
                        contentDescription = "LiDAR Active",
                        tint = ElectricCyan,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // Bottom Controls Deck
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Mode Toggle & Simulation Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Simulation Transport Controls
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Play/Pause
                        IconButton(
                            onClick = {
                                if (isRunning && !isPaused) viewModel.pauseSimulation()
                                else if (isRunning && isPaused) viewModel.resumeSimulation()
                                else viewModel.startSimulation()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricCyan)
                                .testTag("btn_sim_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isRunning && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = DarkOnPrimary
                            )
                        }

                        // Reset
                        IconButton(
                            onClick = { viewModel.resetSimulationState() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .testTag("btn_sim_reset")
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "Reset Simulation", tint = Color.White)
                        }

                        // Run Visual Program
                        Button(
                            onClick = {
                                if (interpreterStatus == InterpreterStatus.RUNNING) {
                                    viewModel.stopProgram()
                                } else {
                                    viewModel.runProgramInSimulator()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (interpreterStatus == InterpreterStatus.RUNNING) HazardRed else TechBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("btn_run_program_sim")
                        ) {
                            Icon(
                                imageVector = if (interpreterStatus == InterpreterStatus.RUNNING) Icons.Default.Stop else Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (interpreterStatus == InterpreterStatus.RUNNING) "HALT CODE" else "EXECUTE CODE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mode switch (Program vs Manual D-Pad)
                    Row {
                        IconButton(onClick = { controlMode = 0 }) {
                            Icon(Icons.Default.Code, contentDescription = "Program Mode", tint = if (controlMode == 0) ElectricCyan else Color.Gray)
                        }
                        IconButton(onClick = { controlMode = 1 }) {
                            Icon(Icons.Default.Gamepad, contentDescription = "Manual D-Pad", tint = if (controlMode == 1) ElectricCyan else Color.Gray)
                        }
                    }
                }

                // If Manual D-Pad Mode is active
                if (controlMode == 1) {
                    Spacer(Modifier.height(8.dp))
                    ManualDPadControls(
                        onDrive = { forward, turn ->
                            viewModel.manualDrive(forward, turn)
                        },
                        onStop = {
                            viewModel.manualDrive(0f, 0f)
                        }
                    )
                }
            }
        }
    }

    // Telemetry Drawer Sheet
    if (showTelemetryDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showTelemetryDrawer = false },
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "REAL-TIME ROBOTICS TELEMETRY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricCyan
                )
                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryBadge(label = "TIME", value = "${"%.1f".format(telemetry.simTimeSec)}s", modifier = Modifier.weight(1f))
                    TelemetryBadge(label = "HEADING", value = "${telemetry.headingDeg.toInt()}°", modifier = Modifier.weight(1f))
                    TelemetryBadge(label = "ANGULAR VEL", value = "${"%.1f".format(telemetry.angularVelocityDeg)}°/s", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryBadge(label = "WHEEL L", value = "${"%.1f".format(telemetry.leftWheelRps)} rps", modifier = Modifier.weight(1f))
                    TelemetryBadge(label = "WHEEL R", value = "${"%.1f".format(telemetry.rightWheelRps)} rps", modifier = Modifier.weight(1f))
                    TelemetryBadge(label = "BATTERY", value = "${telemetry.batteryPercent.toInt()}%", accentColor = SensorGreen, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Text("EXECUTION LOGS:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(8.dp)
                ) {
                    items(telemetry.logMessages) { log ->
                        Text(log, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFC9D1D9))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TelemetryBadge(label: String, value: String, accentColor: Color = ElectricCyan, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
            Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun ManualDPadControls(
    onDrive: (Float, Float) -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Forward
        IconButton(
            onClick = { onDrive(0.6f, 0f) },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .testTag("dpad_up")
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Forward", tint = ElectricCyan)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left
            IconButton(
                onClick = { onDrive(0f, -0.6f) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("dpad_left")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = ElectricCyan)
            }
            // Stop Center
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HazardRed.copy(alpha = 0.2f))
                    .testTag("dpad_stop")
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = HazardRed)
            }
            // Right
            IconButton(
                onClick = { onDrive(0f, 0.6f) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("dpad_right")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = ElectricCyan)
            }
        }
        // Reverse
        IconButton(
            onClick = { onDrive(-0.5f, 0f) },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .testTag("dpad_down")
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Reverse", tint = ElectricCyan)
        }
    }
}
