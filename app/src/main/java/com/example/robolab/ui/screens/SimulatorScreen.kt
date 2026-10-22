package com.example.robolab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.interpreter.InterpreterStatus
import com.example.robolab.model.assembly.JointType
import com.example.robolab.renderer3d.AssemblyRenderer3D
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.ui.components.InteractiveViewport3D
import com.example.robolab.viewmodel.JointTelemetry
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    viewModel: RoboLabViewModel
) {
    val assemblyParts by viewModel.assemblyParts.collectAsState()
    val assemblyJoints by viewModel.assemblyJoints.collectAsState()
    val dynamicPoses by viewModel.dynamicPoses.collectAsState()
    val jointTelemetry by viewModel.jointTelemetry.collectAsState()

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
    var controlMode by remember { mutableStateOf(0) } // 0: Program/Controls, 1: D-Pad
    val is3DAssemblyMode = assemblyParts.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("simulator_screen")
    ) {
        EngineeringHeader(
            title = if (is3DAssemblyMode) "3D Physics Simulator" else "Physics Simulator",
            subtitle = if (is3DAssemblyMode) "Multi-Body Rigid Dynamics Engine" else if (activeExperiment != null) activeExperiment!!.title else "Real-Time Arena",
            badgeText = if (isRunning) "SIM ACTIVE" else "STANDBY",
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
                    Text(
                        "${speedMultiplier.toInt()}X",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { showTelemetryDrawer = !showTelemetryDrawer }) {
                    Icon(Icons.Default.QueryStats, contentDescription = "Telemetry", tint = ElectricCyan)
                }
            }
        )

        // Experiment Goal Banner (if launched)
        if (activeExperiment != null && !is3DAssemblyMode) {
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

        // Viewport Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            if (is3DAssemblyMode) {
                // Genuine 3D Rigid-Body Assembly Viewport
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                viewModel.camera.azimuthDeg = (viewModel.camera.azimuthDeg - pan.x * 0.45f) % 360f
                                viewModel.camera.elevationDeg = (viewModel.camera.elevationDeg + pan.y * 0.35f).coerceIn(8f, 85f)
                                viewModel.camera.distance = (viewModel.camera.distance / zoom).coerceIn(1.0f, 10.0f)
                            }
                        }
                        .testTag("sim_3d_rigid_viewport")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        AssemblyRenderer3D.renderAssemblyScene(
                            drawScope = this,
                            camera = viewModel.camera,
                            parts = assemblyParts,
                            joints = assemblyJoints,
                            dynamicPoses = dynamicPoses,
                            selectedPartId = null,
                            selectedJointId = null,
                            graphicsProfile = graphicsProfile,
                            isWorkshopMode = false
                        )
                    }

                    // HUD Overlay on 3D Viewport
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TelemetryBadge(
                            label = "PARTS / JOINTS",
                            value = "${assemblyParts.size} parts | ${assemblyJoints.size} joints",
                            accentColor = ElectricCyan
                        )
                        TelemetryBadge(
                            label = "PHYSICS SOLVER",
                            value = "50Hz PGS (10 iter)",
                            accentColor = TechBlue
                        )
                        TelemetryBadge(
                            label = "GRAVITY",
                            value = "-9.81 m/s² (Y)",
                            accentColor = SensorGreen
                        )

                        val hasGroundContact = dynamicPoses.any { (_, pose) -> pose.first.y <= 0.05f }
                        if (hasGroundContact) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberAmber)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "GROUND CONTACT / FRICTION ACTIVE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Viewport Hint
                    Text(
                        text = "Touch & Drag: Orbit Camera | Pinch: Zoom",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            } else {
                // Fallback Legacy Mobile Robot Viewport
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
                // Simulation Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Play / Pause Button
                        IconButton(
                            onClick = {
                                if (is3DAssemblyMode) {
                                    if (isRunning && !isPaused) viewModel.pauseRigidSimulation()
                                    else if (isRunning && isPaused) viewModel.resumeRigidSimulation()
                                    else viewModel.startRigidSimulation()
                                } else {
                                    if (isRunning && !isPaused) viewModel.pauseSimulation()
                                    else if (isRunning && isPaused) viewModel.resumeSimulation()
                                    else viewModel.startSimulation()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
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

                        // Reset Button
                        IconButton(
                            onClick = {
                                if (is3DAssemblyMode) {
                                    viewModel.resetRigidSimulation()
                                } else {
                                    viewModel.resetSimulationState()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .testTag("btn_sim_reset")
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "Reset Simulation", tint = Color.White)
                        }

                        if (is3DAssemblyMode) {
                            Text(
                                text = if (isRunning && !isPaused) "SIMULATING DYNAMICS" else if (isPaused) "PAUSED" else "READY TO SIMULATE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning && !isPaused) SensorGreen else Color.Gray
                            )
                        } else {
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
                    }

                    if (is3DAssemblyMode) {
                        Button(
                            onClick = { viewModel.loadMilestone1Slice() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_reload_revolute_slice")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Revolute Slice", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        // Mode switch for legacy car
                        Row {
                            IconButton(onClick = { controlMode = 0 }) {
                                Icon(Icons.Default.Code, contentDescription = "Program Mode", tint = if (controlMode == 0) ElectricCyan else Color.Gray)
                            }
                            IconButton(onClick = { controlMode = 1 }) {
                                Icon(Icons.Default.Gamepad, contentDescription = "Manual D-Pad", tint = if (controlMode == 1) ElectricCyan else Color.Gray)
                            }
                        }
                    }
                }

                // If 3D Multi-Body Assembly Mode: Interactive Joint Motors Control Deck!
                if (is3DAssemblyMode) {
                    val revoluteJoints = assemblyJoints.filter { it.type == JointType.REVOLUTE }
                    if (revoluteJoints.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "ACTIVE REVOLUTE MOTORS & CONSTRAINTS:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Spacer(Modifier.height(4.dp))

                        revoluteJoints.forEach { joint ->
                            val tele = jointTelemetry[joint.id]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = joint.name,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Angle: ${"%.1f".format(tele?.angleDeg ?: 0f)}° | Speed: ${"%.2f".format(tele?.angularVelocityRad ?: 0f)} rad/s | Torque: ${"%.2f".format(tele?.motorTorqueNm ?: 0f)} N·m",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberAmber
                                        )
                                    }

                                    // Live Motor Target Velocity Control
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Motor Target: ${"%.1f".format(joint.motorTargetVelocityRadPerSec)} rad/s",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.LightGray,
                                            modifier = Modifier.width(140.dp)
                                        )
                                        Slider(
                                            value = joint.motorTargetVelocityRadPerSec,
                                            onValueChange = { newVel ->
                                                viewModel.setJointMotorVelocity(joint.id, newVel)
                                            },
                                            valueRange = -8f..8f,
                                            steps = 16,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("slider_motor_vel_${joint.id}")
                                        )
                                    }

                                    // Quick Motor Speed Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.setJointMotorVelocity(joint.id, -3.14f) },
                                            modifier = Modifier.weight(1f).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                                        ) {
                                            Text("-π rad/s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.setJointMotorVelocity(joint.id, 0f) },
                                            modifier = Modifier.weight(1f).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = HazardRed.copy(alpha = 0.3f))
                                        ) {
                                            Text("Brake (0)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = HazardRed)
                                        }
                                        Button(
                                            onClick = { viewModel.setJointMotorVelocity(joint.id, 3.14f) },
                                            modifier = Modifier.weight(1f).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                                        ) {
                                            Text("+π rad/s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.setJointMotorVelocity(joint.id, 6.28f) },
                                            modifier = Modifier.weight(1f).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                                        ) {
                                            Text("+2π rad/s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "No motorized joints in current assembly. Dynamic bodies fall freely under gravity (-9.81 m/s²) and collide with the ground.",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                } else if (controlMode == 1) {
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
                    text = if (is3DAssemblyMode) "RIGID-BODY DYNAMICS TELEMETRY" else "REAL-TIME ROBOTICS TELEMETRY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricCyan
                )
                Spacer(Modifier.height(14.dp))

                if (is3DAssemblyMode) {
                    Text(
                        text = "Rigid Bodies (${assemblyParts.size}):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(8.dp)
                    ) {
                        items(assemblyParts) { part ->
                            val pose = dynamicPoses[part.id]
                            val posY = pose?.first?.y ?: part.posY
                            val posX = pose?.first?.x ?: part.posX
                            val posZ = pose?.first?.z ?: part.posZ
                            Text(
                                text = "• [${part.shapeType.displayName}] ${part.name}: Pos=(${"%.2f".format(posX)}, ${"%.2f".format(posY)}, ${"%.2f".format(posZ)}) | Mass=${if (part.isStatic) "STATIC" else "${part.massKg}kg"}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (part.isStatic) Color.Gray else SensorGreen
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Joint Constraints & Motors (${assemblyJoints.size}):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(8.dp)
                    ) {
                        items(assemblyJoints) { joint ->
                            val tele = jointTelemetry[joint.id]
                            Text(
                                text = "• [${joint.type}] ${joint.name}: θ=${"%.1f".format(tele?.angleDeg ?: 0f)}° | ω=${"%.2f".format(tele?.angularVelocityRad ?: 0f)} rad/s | τ=${"%.2f".format(tele?.motorTorqueNm ?: 0f)} N·m",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAmber
                            )
                        }
                    }
                } else {
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
