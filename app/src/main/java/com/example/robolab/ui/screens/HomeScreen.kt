package com.example.robolab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.robolab.experiments.ExperimentCatalog
import com.example.robolab.model.GraphicsProfile
import com.example.robolab.model.ValidationSeverity
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.ui.components.InteractiveViewport3D
import com.example.robolab.ui.components.TelemetryBadge
import com.example.robolab.viewmodel.ActiveScreen
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: RoboLabViewModel,
    onNavigate: (ActiveScreen) -> Unit
) {
    val activeName by viewModel.activeProjectName.collectAsState()
    val components by viewModel.components.collectAsState()
    val programBlocks by viewModel.programBlocks.collectAsState()
    val validationIssues by viewModel.validationIssues.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val graphicsProfile by viewModel.graphicsProfile.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showInteractive3DHero by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            EngineeringHeader(
                title = "RoboLab",
                subtitle = "Virtual Robotics Engineering & Physics Simulation Platform",
                badgeText = "v1.0 OFFLINE"
            )
        }

        // Hero Preview Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkOutline, RoundedCornerShape(16.dp))
            ) {
                if (showInteractive3DHero) {
                    InteractiveViewport3D(
                        camera = viewModel.camera,
                        components = components,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.robolab_hero),
                        contentDescription = "RoboLab Engineering Workshop",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Toggle 3D Interactive Hero Overlay
                Button(
                    onClick = { showInteractive3DHero = !showInteractive3DHero },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .testTag("btn_toggle_hero_3d")
                ) {
                    Icon(
                        imageVector = if (showInteractive3DHero) Icons.Default.Image else Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (showInteractive3DHero) "Hero Photo" else "Interactive 3D",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // Primary Action Cards: Continue Project & Create Robot
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ActiveScreen.WORKSHOP) }
                        .testTag("card_resume_workshop"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.4f)))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("WORKSHOP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(activeName, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("${components.size} components attached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ActiveScreen.SIMULATOR) }
                        .testTag("card_enter_simulator"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TechBlue.copy(alpha = 0.4f)))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TechBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SIMULATOR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TechBlue)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Real-Time Physics", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("Differential kinematics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Telemetry & Validation Bar
        item {
            val errorCount = validationIssues.count { it.severity == ValidationSeverity.ERROR }
            val warningCount = validationIssues.count { it.severity == ValidationSeverity.WARNING }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "ASSEMBLY",
                    value = if (errorCount == 0) "READY" else "$errorCount ERRORS",
                    accentColor = if (errorCount == 0) SensorGreen else HazardRed,
                    icon = if (errorCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning
                )
                TelemetryBadge(
                    label = "PROGRAM",
                    value = "${programBlocks.size} BLOCKS",
                    accentColor = CyberAmber,
                    icon = Icons.Default.Code
                )
                TelemetryBadge(
                    label = "PROFILE",
                    value = graphicsProfile.label.uppercase(),
                    accentColor = TechBlue,
                    icon = Icons.Default.Speed
                )
            }
        }

        // Quick Creation Action
        item {
            Button(
                onClick = { showNewProjectDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp)
                    .testTag("btn_create_robot_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("CREATE NEW ROBOT ASSEMBLY", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        // Featured Experiments Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FEATURED EXPERIMENTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onNavigate(ActiveScreen.EXPERIMENTS) }) {
                        Text("View All (5)", color = ElectricCyan, fontSize = 12.sp)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ExperimentCatalog.experiments.take(3)) { exp ->
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .clickable { viewModel.launchExperiment(exp) }
                                .testTag("card_experiment_${exp.id}"),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(exp.category, fontSize = 10.sp, color = TechBlue, fontFamily = FontFamily.Monospace)
                                    Text(exp.difficulty, fontSize = 10.sp, color = CyberAmber, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(exp.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(exp.objective, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Launch Lab", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Projects Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED PROJECTS (${allProjects.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onNavigate(ActiveScreen.PROJECTS) }) {
                        Text("Manage", color = ElectricCyan, fontSize = 12.sp)
                    }
                }

                for (p in allProjects.take(3)) {
                    val isActive = p.name == activeName
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.loadProject(p) }
                            .testTag("project_item_${p.id}"),
                        colors = CardDefaults.cardColors(containerColor = if (isActive) DarkSurfaceVariant else DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isActive) ElectricCyan.copy(alpha = 0.5f) else DarkOutline)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    if (isActive) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("[ACTIVE]", fontSize = 10.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "Open Project", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Graphics Profile Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("GRAPHICS PROFILE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TechBlue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Select simulation rendering precision and ray density", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphicsProfile.values().forEach { profile ->
                            val isSelected = graphicsProfile == profile
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGraphicsProfile(profile) },
                                label = { Text(profile.label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("New Robot Assembly", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Robot Name") },
                        placeholder = { Text("e.g. Explorer Bot v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Design Notes") },
                        placeholder = { Text("Differential drive autonomous platform") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(newName, newDesc)
                        showNewProjectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary)
                ) {
                    Text("Create & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
