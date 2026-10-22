package com.example.robolab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.robolab.model.BlockType
import com.example.robolab.model.ProgramBlock
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun ProgrammingScreen(
    viewModel: RoboLabViewModel
) {
    val programBlocks by viewModel.programBlocks.collectAsState()
    val interpreterStatus by viewModel.interpreterStatus.collectAsState()
    val activeBlockId by viewModel.activeProgramBlockId.collectAsState()
    val executionLogs by viewModel.executionLogs.collectAsState()
    val activeProjectName by viewModel.activeProjectName.collectAsState()

    var showAddBlockDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("programming_screen")
    ) {
        EngineeringHeader(
            title = "Programming Lab",
            subtitle = activeProjectName,
            badgeText = if (interpreterStatus == InterpreterStatus.RUNNING) "EXECUTING" else "${programBlocks.size} BLOCKS",
            actions = {
                // Run / Halt Button
                Button(
                    onClick = {
                        if (interpreterStatus == InterpreterStatus.RUNNING) {
                            viewModel.stopProgram()
                        } else {
                            viewModel.runProgramInSimulator()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (interpreterStatus == InterpreterStatus.RUNNING) HazardRed else ElectricCyan,
                        contentColor = if (interpreterStatus == InterpreterStatus.RUNNING) Color.White else DarkOnPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_run_program")
                ) {
                    Icon(
                        imageVector = if (interpreterStatus == InterpreterStatus.RUNNING) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (interpreterStatus == InterpreterStatus.RUNNING) "HALT" else "RUN CODE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        )

        // Block Workspace & Terminal split
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Top Toolbar: Add Block Button & Quick templates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddBlockDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.5f))),
                    modifier = Modifier.testTag("btn_add_block_open")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADD LOGIC BLOCK", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "${programBlocks.size} instructions",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Blocks List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (programBlocks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No program blocks added yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { showAddBlockDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary)
                                ) {
                                    Text("Add First Block")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(programBlocks) { index, block ->
                        val isActive = block.id == activeBlockId
                        ProgramBlockCard(
                            block = block,
                            isActive = isActive,
                            onUpdate = { updated ->
                                val list = programBlocks.toMutableList()
                                list[index] = updated
                                viewModel.updateProgramBlocks(list)
                            },
                            onDelete = { viewModel.removeProgramBlock(block.id) },
                            onMoveUp = if (index > 0) {
                                {
                                    val list = programBlocks.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index - 1]
                                    list[index - 1] = temp
                                    viewModel.updateProgramBlocks(list)
                                }
                            } else null,
                            onMoveDown = if (index < programBlocks.size - 1) {
                                {
                                    val list = programBlocks.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index + 1]
                                    list[index + 1] = temp
                                    viewModel.updateProgramBlocks(list)
                                }
                            } else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Embedded Terminal / Debug Output
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("EXECUTION CONSOLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        }
                        Text(
                            text = if (interpreterStatus == InterpreterStatus.RUNNING) "ACTIVE" else "IDLE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (interpreterStatus == InterpreterStatus.RUNNING) SensorGreen else Color.Gray
                        )
                    }
                    HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (executionLogs.isEmpty()) {
                            item {
                                Text("Waiting for program execution...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        } else {
                            items(executionLogs) { log ->
                                Text(log, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFC9D1D9))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Block Dialog
    if (showAddBlockDialog) {
        val categories = listOf("All", "Motion", "Condition", "Loop", "Timing", "Arm", "Debug")
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Select Logic Block", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    val filteredTypes = BlockType.values().filter {
                        selectedCategoryFilter == "All" || it.category == selectedCategoryFilter
                    }

                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTypes) { type ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addProgramBlock(ProgramBlock(id = UUID.randomUUID().toString(), type = type))
                                        showAddBlockDialog = false
                                    }
                                    .testTag("add_block_option_${type.name}"),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(type.label, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        Text(type.category, fontSize = 10.sp, color = TechBlue, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProgramBlockCard(
    block: ProgramBlock,
    isActive: Boolean,
    onUpdate: (ProgramBlock) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val blockColor = when (block.type.category) {
        "Motion" -> TechBlue
        "Condition" -> CyberAmber
        "Loop" -> VioletJoint
        "Timing" -> Color(0xFF14B8A6)
        "Arm" -> SensorGreen
        else -> ElectricCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("program_block_${block.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ElectricCyan.copy(alpha = 0.15f) else DarkSurfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) ElectricCyan else blockColor.copy(alpha = 0.5f)
            ),
            width = if (isActive) 2.dp else 1.dp
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Block Type Label & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(blockColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = block.type.label.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = blockColor
                    )
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Text("[EXECUTING]", fontSize = 10.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Block", tint = HazardRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 6.dp))

            // Inline Parameter Controls based on block type
            when (block.type) {
                BlockType.DRIVE_FORWARD -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speed: ${"%.1f".format(block.speed * 100)}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(value = block.speed, onValueChange = { onUpdate(block.copy(speed = it)) }, valueRange = 0.1f..1.0f)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration: ${"%.1f".format(block.durationSec)}s", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(value = block.durationSec, onValueChange = { onUpdate(block.copy(durationSec = it)) }, valueRange = 0.2f..5.0f)
                        }
                    }
                }
                BlockType.ROTATE -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Angle: ${block.angleDeg.toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(value = block.angleDeg, onValueChange = { onUpdate(block.copy(angleDeg = it)) }, valueRange = -180f..180f, steps = 11)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speed: ${"%.1f".format(block.speed * 100)}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(value = block.speed, onValueChange = { onUpdate(block.copy(speed = it)) }, valueRange = 0.2f..1.0f)
                        }
                    }
                }
                BlockType.SET_WHEEL_SPEEDS -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Left: ${"%.1f".format(block.leftSpeed)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(value = block.leftSpeed, onValueChange = { onUpdate(block.copy(leftSpeed = it)) }, valueRange = -1f..1f)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Right: ${"%.1f".format(block.rightSpeed)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(value = block.rightSpeed, onValueChange = { onUpdate(block.copy(rightSpeed = it)) }, valueRange = -1f..1f)
                        }
                    }
                }
                BlockType.WAIT -> {
                    Text("Duration: ${"%.1f".format(block.durationSec)}s", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Slider(value = block.durationSec, onValueChange = { onUpdate(block.copy(durationSec = it)) }, valueRange = 0.2f..5.0f)
                }
                BlockType.IF_OBSTACLE, BlockType.WHILE_CLEAR -> {
                    Text("Proximity Threshold: ${"%.2f".format(block.thresholdM)}m", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberAmber)
                    Slider(value = block.thresholdM, onValueChange = { onUpdate(block.copy(thresholdM = it)) }, valueRange = 0.20f..1.50f)
                }
                BlockType.REPEAT -> {
                    Text("Iterations: ${block.repeatCount}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = VioletJoint)
                    Slider(value = block.repeatCount.toFloat(), onValueChange = { onUpdate(block.copy(repeatCount = it.toInt())) }, valueRange = 1f..10f, steps = 8)
                }
                BlockType.SET_ARM_ANGLES -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shoulder: ${block.armAngle1Deg.toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(value = block.armAngle1Deg, onValueChange = { onUpdate(block.copy(armAngle1Deg = it)) }, valueRange = -90f..90f)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Forearm: ${block.armAngle2Deg.toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(value = block.armAngle2Deg, onValueChange = { onUpdate(block.copy(armAngle2Deg = it)) }, valueRange = -90f..90f)
                        }
                    }
                }
                BlockType.LOG_MESSAGE -> {
                    OutlinedTextField(
                        value = block.message,
                        onValueChange = { onUpdate(block.copy(message = it)) },
                        label = { Text("Log Text", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BlockType.START, BlockType.STOP -> {
                    Text(
                        text = if (block.type == BlockType.START) "Execution entrypoint." else "Halts all drive wheel PWM signals immediately.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
