package com.example.robolab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.robolab.model.assembly.AssemblyJoint
import com.example.robolab.model.assembly.AssemblyPart
import com.example.robolab.model.assembly.JointType
import com.example.robolab.model.assembly.ShapeType
import com.example.robolab.renderer3d.AssemblyRenderer3D
import com.example.robolab.renderer3d.Vector3
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopScreen(
    viewModel: RoboLabViewModel
) {
    val parts by viewModel.assemblyParts.collectAsState()
    val joints by viewModel.assemblyJoints.collectAsState()
    val selectedPartId by viewModel.selectedAssemblyPartId.collectAsState()
    val selectedJointId by viewModel.selectedAssemblyJointId.collectAsState()
    val dynamicPoses by viewModel.dynamicPoses.collectAsState()
    val activeProjectName by viewModel.activeProjectName.collectAsState()
    val graphicsProfile by viewModel.graphicsProfile.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: 3D CAD & Inspector, 1: Add Part / Joint, 2: Assembly Tree
    var showAddJointDialog by remember { mutableStateOf(false) }

    val selectedPart = parts.find { it.id == selectedPartId }
    val selectedJoint = joints.find { it.id == selectedJointId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("workshop_screen")
    ) {
        EngineeringHeader(
            title = "3D Workshop",
            subtitle = activeProjectName,
            badgeText = "${parts.size} PARTS | ${joints.size} JOINTS",
            actions = {
                // Clear to empty workspace
                TextButton(
                    onClick = { viewModel.clearToEmptyWorkspace() },
                    modifier = Modifier.testTag("btn_empty_workspace")
                ) {
                    Text("Empty", color = HazardRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Milestone 1 vertical slice preset
                TextButton(
                    onClick = { viewModel.loadMilestone1Slice() },
                    modifier = Modifier.testTag("btn_load_m1_slice")
                ) {
                    Text("Revolute Slice", color = ElectricCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        )

        // Workshop Navigation Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurface,
            contentColor = ElectricCyan,
            divider = { HorizontalDivider(color = DarkOutline) }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("3D Workspace", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_3d_workspace")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Add Geometry", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_add_geometry")
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Hierarchy (${parts.size})", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_hierarchy")
            )
        }

        when (activeTab) {
            0 -> {
                // 3D CAD Canvas and Part/Joint Property Inspector
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
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
                            .testTag("assembly_3d_viewport")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            AssemblyRenderer3D.renderAssemblyScene(
                                drawScope = this,
                                camera = viewModel.camera,
                                parts = parts,
                                joints = joints,
                                dynamicPoses = dynamicPoses,
                                selectedPartId = selectedPartId,
                                selectedJointId = selectedJointId,
                                graphicsProfile = graphicsProfile,
                                isWorkshopMode = true
                            )
                        }

                        // Viewport overlay controls (Reset camera, Zoom)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.camera.azimuthDeg = 45f
                                    viewModel.camera.elevationDeg = 30f
                                    viewModel.camera.distance = 2.5f
                                },
                                containerColor = DarkSurface.copy(alpha = 0.85f),
                                contentColor = ElectricCyan,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Camera", modifier = Modifier.size(16.dp))
                            }
                        }

                        if (parts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant.copy(alpha = 0.9f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("EMPTY 3D WORKSPACE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 12.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Construct any robot from scratch using 3D primitives and joints.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { activeTab = 1 },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Add 3D Part", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.loadMilestone1Slice() },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Load M1 Slice", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Parts & Joints Ribbon Selector
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(parts) { part ->
                            val isSelected = part.id == selectedPartId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectAssemblyPart(if (isSelected) null else part.id) },
                                label = { Text(part.name, fontSize = 11.sp, maxLines = 1) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(part.colorHex))
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }

                        items(joints) { joint ->
                            val isSelected = joint.id == selectedJointId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectAssemblyJoint(if (isSelected) null else joint.id) },
                                label = { Text(joint.name, fontSize = 11.sp, maxLines = 1) },
                                leadingIcon = {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = if (joint.enableMotor) SensorGreen else CyberAmber, modifier = Modifier.size(12.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TechBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = TechBlue
                                )
                            )
                        }
                    }

                    // Inspector Drawer (Part or Joint)
                    if (selectedPart != null) {
                        AssemblyPartInspector(
                            part = selectedPart,
                            onUpdate = { viewModel.updateCustomPart(it) },
                            onDelete = { viewModel.removeCustomPart(selectedPart.id) },
                            onDuplicate = { viewModel.duplicateCustomPart(selectedPart.id) },
                            onClose = { viewModel.selectAssemblyPart(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .padding(8.dp)
                        )
                    } else if (selectedJoint != null) {
                        AssemblyJointInspector(
                            joint = selectedJoint,
                            onUpdate = { viewModel.updateCustomJoint(it) },
                            onDelete = { viewModel.removeCustomJoint(selectedJoint.id) },
                            onClose = { viewModel.selectAssemblyJoint(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            1 -> {
                // Add Geometry & Connect Joints Tab
                AddGeometryAndJointsView(
                    parts = parts,
                    onAddPart = { shape ->
                        viewModel.addCustomPart(shape)
                        activeTab = 0
                    },
                    onOpenAddJoint = { showAddJointDialog = true }
                )
            }

            2 -> {
                // Assembly Scene Graph Hierarchy View
                AssemblyHierarchyView(
                    parts = parts,
                    joints = joints,
                    selectedPartId = selectedPartId,
                    selectedJointId = selectedJointId,
                    onSelectPart = {
                        viewModel.selectAssemblyPart(it)
                        activeTab = 0
                    },
                    onSelectJoint = {
                        viewModel.selectAssemblyJoint(it)
                        activeTab = 0
                    },
                    onDeletePart = { viewModel.removeCustomPart(it) },
                    onDeleteJoint = { viewModel.removeCustomJoint(it) }
                )
            }
        }
    }

    if (showAddJointDialog && parts.size >= 2) {
        CreateJointDialog(
            parts = parts,
            onDismiss = { showAddJointDialog = false },
            onCreate = { parentId, childId, type, axis, motor, speed, torque ->
                viewModel.addCustomJoint(
                    parentPartId = parentId,
                    childPartId = childId,
                    type = type,
                    axis = axis,
                    enableMotor = motor,
                    motorTargetVelocity = speed,
                    motorMaxTorque = torque
                )
                showAddJointDialog = false
                activeTab = 0
            }
        )
    }
}

@Composable
fun AddGeometryAndJointsView(
    parts: List<AssemblyPart>,
    onAddPart: (ShapeType) -> Unit,
    onOpenAddJoint: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("CREATE 3D PRIMITIVE PART", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 12.sp)
            Text("Spawn editable 3D primitive geometry in the workspace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(ShapeType.values()) { shape ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddPart(shape) }
                    .testTag("btn_add_shape_${shape.name}"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = ElectricCyan)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(shape.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(getShapeDesc(shape), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { onAddPart(shape) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Add", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("MECHANICAL JOINTS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TechBlue, fontSize = 12.sp)
            Text("Connect two 3D parts with a revolute hinge or fixed weld.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onOpenAddJoint,
                enabled = parts.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = TechBlue, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_connect_joint_open")
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (parts.size >= 2) "CONNECT TWO PARTS WITH JOINT" else "NEED AT LEAST 2 PARTS TO CONNECT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun AssemblyPartInspector(
    part: AssemblyPart,
    onUpdate: (AssemblyPart) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("part_inspector_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(part.colorHex))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(part.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("[${part.shapeType.name}]", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                }
                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TechBlue, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                // Dimensions
                item {
                    Text("Dimensions: ${part.getDimensionsSummary()}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                    if (part.shapeType == ShapeType.BOX) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("W: ${"%.2f".format(part.dimX)}m", fontSize = 10.sp)
                                Slider(value = part.dimX, onValueChange = { onUpdate(part.copy(dimX = it)) }, valueRange = 0.05f..1.0f)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("H: ${"%.2f".format(part.dimY)}m", fontSize = 10.sp)
                                Slider(value = part.dimY, onValueChange = { onUpdate(part.copy(dimY = it)) }, valueRange = 0.05f..1.0f)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("D: ${"%.2f".format(part.dimZ)}m", fontSize = 10.sp)
                                Slider(value = part.dimZ, onValueChange = { onUpdate(part.copy(dimZ = it)) }, valueRange = 0.05f..1.0f)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Radius: ${"%.2f".format(part.radius)}m", fontSize = 10.sp)
                                Slider(value = part.radius, onValueChange = { onUpdate(part.copy(radius = it)) }, valueRange = 0.03f..0.50f)
                            }
                            if (part.shapeType != ShapeType.SPHERE) {
                                Column(Modifier.weight(1f)) {
                                    Text("Height: ${"%.2f".format(part.dimY)}m", fontSize = 10.sp)
                                    Slider(value = part.dimY, onValueChange = { onUpdate(part.copy(dimY = it)) }, valueRange = 0.05f..1.0f)
                                }
                            }
                        }
                    }
                }

                // Spatial Position X, Y, Z
                item {
                    Text("Position (X, Y, Z)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("X: ${"%.2f".format(part.posX)}", fontSize = 10.sp)
                            Slider(value = part.posX, onValueChange = { onUpdate(part.copy(posX = it)) }, valueRange = -1.5f..1.5f)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Y: ${"%.2f".format(part.posY)}", fontSize = 10.sp)
                            Slider(value = part.posY, onValueChange = { onUpdate(part.copy(posY = it)) }, valueRange = 0.0f..2.0f)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Z: ${"%.2f".format(part.posZ)}", fontSize = 10.sp)
                            Slider(value = part.posZ, onValueChange = { onUpdate(part.copy(posZ = it)) }, valueRange = -1.5f..1.5f)
                        }
                    }
                }

                // Physical properties: Mass & Static toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anchored (Static Base):", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Switch(
                            checked = part.isStatic,
                            onCheckedChange = { onUpdate(part.copy(isStatic = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                        )
                    }
                    if (!part.isStatic) {
                        Text("Mass: ${"%.1f".format(part.massKg)} kg", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Slider(value = part.massKg, onValueChange = { onUpdate(part.copy(massKg = it)) }, valueRange = 0.1f..10.0f)
                    }
                }
            }
        }
    }
}

@Composable
fun AssemblyJointInspector(
    joint: AssemblyJoint,
    onUpdate: (AssemblyJoint) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("joint_inspector_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TechBlue.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = TechBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(joint.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Row {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("Joint Type: ${joint.type.displayName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TechBlue)
                }

                if (joint.type == JointType.REVOLUTE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Joint Motor:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Switch(
                                checked = joint.enableMotor,
                                onCheckedChange = { onUpdate(joint.copy(enableMotor = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = SensorGreen)
                            )
                        }

                        if (joint.enableMotor) {
                            Text("Target Velocity: ${"%.1f".format(joint.motorTargetVelocityRadPerSec)} rad/s", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(
                                value = joint.motorTargetVelocityRadPerSec,
                                onValueChange = { onUpdate(joint.copy(motorTargetVelocityRadPerSec = it)) },
                                valueRange = -10.0f..10.0f
                            )
                            Text("Max Torque: ${"%.1f".format(joint.motorMaxTorqueNm)} Nm", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(
                                value = joint.motorMaxTorqueNm,
                                onValueChange = { onUpdate(joint.copy(motorMaxTorqueNm = it)) },
                                valueRange = 1.0f..50.0f
                            )
                        }
                    }

                    item {
                        Text("Hinge Axis: [${joint.axisX.toInt()}, ${joint.axisY.toInt()}, ${joint.axisZ.toInt()}]", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = joint.axisX == 1f,
                                onClick = { onUpdate(joint.copy(axisX = 1f, axisY = 0f, axisZ = 0f)) },
                                label = { Text("Pitch (X)") }
                            )
                            FilterChip(
                                selected = joint.axisY == 1f,
                                onClick = { onUpdate(joint.copy(axisX = 0f, axisY = 1f, axisZ = 0f)) },
                                label = { Text("Yaw (Y)") }
                            )
                            FilterChip(
                                selected = joint.axisZ == 1f,
                                onClick = { onUpdate(joint.copy(axisX = 0f, axisY = 0f, axisZ = 1f)) },
                                label = { Text("Roll (Z)") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssemblyHierarchyView(
    parts: List<AssemblyPart>,
    joints: List<AssemblyJoint>,
    selectedPartId: String?,
    selectedJointId: String?,
    onSelectPart: (String) -> Unit,
    onSelectJoint: (String) -> Unit,
    onDeletePart: (String) -> Unit,
    onDeleteJoint: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("PARTS (${parts.size})", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 11.sp)
        }
        items(parts) { part ->
            val isSelected = part.id == selectedPartId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPart(part.id) }
                    .testTag("hierarchy_part_${part.id}"),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) ElectricCyan else DarkOutline)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(part.colorHex))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(part.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("${part.shapeType.name} | ${if (part.isStatic) "Anchored" else "${part.massKg}kg"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { onDeletePart(part.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("JOINTS (${joints.size})", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TechBlue, fontSize = 11.sp)
        }
        items(joints) { joint ->
            val isSelected = joint.id == selectedJointId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectJoint(joint.id) }
                    .testTag("hierarchy_joint_${joint.id}"),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) TechBlue else DarkOutline)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = if (joint.enableMotor) SensorGreen else CyberAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(joint.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("${joint.type.displayName} ${if (joint.enableMotor) "| Motor On" else ""}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { onDeleteJoint(joint.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CreateJointDialog(
    parts: List<AssemblyPart>,
    onDismiss: () -> Unit,
    onCreate: (String, String, JointType, Vector3, Boolean, Float, Float) -> Unit
) {
    var parentId by remember { mutableStateOf(parts.first().id) }
    var childId by remember { mutableStateOf(parts.getOrNull(1)?.id ?: parts.first().id) }
    var jointType by remember { mutableStateOf(JointType.REVOLUTE) }
    var axis by remember { mutableStateOf(Vector3(1f, 0f, 0f)) }
    var motorOn by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(2.5f) }
    var torque by remember { mutableFloatStateOf(12.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Connect Parts with Joint", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Parent Part:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                parts.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = parentId == p.id, onClick = { parentId = p.id })
                        Text(p.name, fontSize = 11.sp, color = Color.White)
                    }
                }

                Text("Child Part:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                parts.filter { it.id != parentId }.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = childId == p.id, onClick = { childId = p.id })
                        Text(p.name, fontSize = 11.sp, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Joint Motor:", fontSize = 11.sp)
                    Switch(checked = motorOn, onCheckedChange = { motorOn = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(parentId, childId, jointType, axis, motorOn, speed, torque) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

fun getShapeDesc(shape: ShapeType): String {
    return when (shape) {
        ShapeType.BOX -> "Rectangular cuboid with custom width, height, and depth"
        ShapeType.CYLINDER -> "Rotational cylinder with radius and height"
        ShapeType.SPHERE -> "Omnidirectional sphere primitive"
        ShapeType.CAPSULE -> "Cylinder body with rounded hemisphere caps"
    }
}
