package com.example.robolab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.data.JsonHelper
import com.example.robolab.data.ProjectEntity
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.viewmodel.ActiveScreen
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen(
    viewModel: RoboLabViewModel,
    onNavigate: (ActiveScreen) -> Unit
) {
    val allProjects by viewModel.allProjects.collectAsState()
    val activeId by viewModel.activeProjectId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var showExportDialog by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("projects_screen")
    ) {
        EngineeringHeader(
            title = "Robot Projects",
            subtitle = "Persisted Local Assemblies & Logic",
            badgeText = "${allProjects.size} SAVED",
            actions = {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_new_project")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("NEW", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(allProjects) { project ->
                val isActive = project.id == activeId
                val componentCount = JsonHelper.deserializeComponents(project.assemblyJson).size
                val programBlockCount = JsonHelper.deserializeProgram(project.programJson).size
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(project.updatedAt))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.loadProject(project)
                            onNavigate(ActiveScreen.WORKSHOP)
                        }
                        .testTag("project_card_${project.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) DarkSurfaceVariant else DarkSurface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isActive) ElectricCyan else DarkOutline
                        ),
                        width = if (isActive) 1.5.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                if (isActive) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "[CURRENT]",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { showExportDialog = project },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export JSON", tint = TechBlue, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.duplicateCurrentProject()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { projectToDelete = project },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "$componentCount Components",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "$programBlockCount Logic Blocks",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberAmber
                                )
                            }
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Create New Project Dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurface,
            title = { Text("Create Robot Project", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Robot Name") },
                        placeholder = { Text("e.g. HexaRover Mark III") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Engineering Description") },
                        placeholder = { Text("Obstacle-detecting mobile platform") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(name, desc)
                        showCreateDialog = false
                        onNavigate(ActiveScreen.WORKSHOP)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Delete Project?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = HazardRed) },
            text = {
                Text("Are you sure you want to delete '${projectToDelete!!.name}'? This action cannot be undone.", color = Color.White)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(projectToDelete!!.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HazardRed, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // Export Project JSON Dialog
    if (showExportDialog != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            containerColor = DarkSurface,
            title = { Text("Export Project JSON", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan) },
            text = {
                Column {
                    Text("Raw project definition in native RoboLab format:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ID: ${showExportDialog!!.id}\nFormat: v${showExportDialog!!.formatVersion}\nAssembly Parts: ${showExportDialog!!.assemblyJson.take(120)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
