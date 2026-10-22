package com.example.robolab.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.model.*
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.ui.components.InteractiveViewport3D
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopScreen(
    viewModel: RoboLabViewModel
) {
    val components by viewModel.components.collectAsState()
    val selectedId by viewModel.selectedComponentId.collectAsState()
    val validationIssues by viewModel.validationIssues.collectAsState()
    val activeProjectName by viewModel.activeProjectName.collectAsState()
    val graphicsProfile by viewModel.graphicsProfile.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: 3D View & Inspector, 1: Component Library, 2: Assembly Tree, 3: Validation
    var showAddDrawer by remember { mutableStateOf(false) }

    val selectedComponent = components.find { it.id == selectedId }
    val errorCount = validationIssues.count { it.severity == ValidationSeverity.ERROR }
    val warningCount = validationIssues.count { it.severity == ValidationSeverity.WARNING }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("workshop_screen")
    ) {
        EngineeringHeader(
            title = "Robot Workshop",
            subtitle = activeProjectName,
            badgeText = "${components.size} PARTS",
            actions = {
                // Validation Status Chip
                Surface(
                    onClick = { activeTab = 3 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (errorCount == 0) SensorGreen.copy(alpha = 0.15f) else HazardRed.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (errorCount == 0) SensorGreen else HazardRed)
                    ),
                    modifier = Modifier.testTag("chip_validation_status")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (errorCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (errorCount == 0) SensorGreen else HazardRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (errorCount == 0) "VALID" else "$errorCount ERRORS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (errorCount == 0) SensorGreen else HazardRed
                        )
                    }
                }
            }
        )

        // Workshop Tabs (3D Canvas, Component Library, Hierarchy, Validation)
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurface,
            contentColor = ElectricCyan,
            divider = { HorizontalDivider(color = DarkOutline) }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("3D CAD", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_3d_cad")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Library", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_component_library")
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Hierarchy", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_hierarchy")
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = {
                    Text(
                        if (errorCount > 0) "Issues ($errorCount)" else "Validate",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                icon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_validation")
            )
        }

        when (activeTab) {
            0 -> {
                // 3D Viewport with Bottom Inspector Sheet
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        InteractiveViewport3D(
                            camera = viewModel.camera,
                            components = components,
                            selectedComponentId = selectedId,
                            graphicsProfile = graphicsProfile,
                            isWorkshopMode = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp)
                        )

                        // Quick Horizontal Component Selector Ribbon
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(components) { comp ->
                                val isSelected = comp.id == selectedId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectComponent(if (isSelected) null else comp.id) },
                                    label = { Text(comp.name, fontSize = 11.sp, maxLines = 1) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getComponentIcon(comp.type),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = ElectricCyan
                                    )
                                )
                            }
                        }

                        // Inspector Panel (if a component is selected)
                        if (selectedComponent != null) {
                            ComponentInspectorPanel(
                                component = selectedComponent,
                                onUpdate = { viewModel.updateComponent(it) },
                                onDelete = { viewModel.removeComponent(selectedComponent.id) },
                                onClose = { viewModel.selectComponent(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .padding(8.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a component in the ribbon or Hierarchy tab to inspect parameters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Component Library Tab
                ComponentLibraryView(
                    onAddComponent = { type ->
                        viewModel.addComponent(type)
                        activeTab = 0
                    }
                )
            }

            2 -> {
                // Assembly Hierarchy Tree View
                HierarchyTreeView(
                    components = components,
                    selectedId = selectedId,
                    onSelect = {
                        viewModel.selectComponent(it)
                        activeTab = 0
                    },
                    onDelete = { viewModel.removeComponent(it) }
                )
            }

            3 -> {
                // Validation Checklist View
                ValidationReportView(
                    issues = validationIssues,
                    onResolveIssue = { issue ->
                        activeTab = 1 // Switch to library to add missing part
                    }
                )
            }
        }
    }
}

@Composable
fun ComponentLibraryView(
    onAddComponent: (ComponentType) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ComponentCategory.STRUCTURAL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ADD ROBOT COMPONENT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        // Category Pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ComponentCategory.values()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.title, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        val matchingTypes = ComponentType.values().filter { it.category == selectedCategory }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(matchingTypes) { type ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddComponent(type) }
                        .testTag("lib_item_${type.name}"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getComponentIcon(type), contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(type.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Text(getComponentDescription(type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            onClick = { onAddComponent(type) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Mount", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentInspectorPanel(
    component: RobotComponent,
    onUpdate: (RobotComponent) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("component_inspector_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.4f))),
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
                    Icon(getComponentIcon(component.type), contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(component.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HazardRed, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    // Position X Slider
                    Text("Lateral Offset X: ${"%.2f".format(component.posX)}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = component.posX,
                        onValueChange = { onUpdate(component.copy(posX = it)) },
                        valueRange = -0.30f..0.30f,
                        steps = 12
                    )
                }
                item {
                    // Position Z Slider (Forward/Back)
                    Text("Longitudinal Offset Z: ${"%.2f".format(component.posZ)}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = component.posZ,
                        onValueChange = { onUpdate(component.copy(posZ = it)) },
                        valueRange = -0.30f..0.30f,
                        steps = 12
                    )
                }

                if (component.type == ComponentType.LEFT_WHEEL || component.type == ComponentType.RIGHT_WHEEL) {
                    item {
                        Text("Wheel Radius: ${(component.wheelRadius * 1000).toInt()}mm", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                        Slider(
                            value = component.wheelRadius,
                            onValueChange = { onUpdate(component.copy(wheelRadius = it)) },
                            valueRange = 0.025f..0.090f,
                            steps = 13
                        )
                    }
                }

                if (component.type == ComponentType.DISTANCE_SENSOR || component.type == ComponentType.LIDAR_2D) {
                    item {
                        Text("Sensor Max Range: ${"%.1f".format(component.sensorRangeM)}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SensorGreen)
                        Slider(
                            value = component.sensorRangeM,
                            onValueChange = { onUpdate(component.copy(sensorRangeM = it)) },
                            valueRange = 1.0f..6.0f,
                            steps = 10
                        )
                    }
                }

                if (component.type == ComponentType.ARM_LINK_1 || component.type == ComponentType.ARM_LINK_2) {
                    item {
                        Text("Joint Angle: ${component.jointAngleDeg.toInt()}°", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = VioletJoint)
                        Slider(
                            value = component.jointAngleDeg,
                            onValueChange = { onUpdate(component.copy(jointAngleDeg = it)) },
                            valueRange = -90f..90f,
                            steps = 18
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HierarchyTreeView(
    components: List<RobotComponent>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "ROBOT SCENE GRAPH (${components.size} COMPONENTS)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(components) { comp ->
            val isSelected = comp.id == selectedId
            val isChild = comp.parentId != null
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isChild) 24.dp else 0.dp)
                    .clickable { onSelect(comp.id) }
                    .testTag("hierarchy_item_${comp.id}"),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) ElectricCyan else DarkOutline)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getComponentIcon(comp.type), contentDescription = null, tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(comp.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(comp.type.displayName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                    }
                    IconButton(onClick = { onDelete(comp.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationReportView(
    issues: List<ValidationIssue>,
    onResolveIssue: (ValidationIssue) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ASSEMBLY VALIDATION REPORT",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (issues.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SensorGreen.copy(alpha = 0.1f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SensorGreen.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SensorGreen, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Assembly Fully Validated", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("All mechanical constraints and differential kinematic requirements are satisfied.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(issues) { issue ->
                val badgeColor = when (issue.severity) {
                    ValidationSeverity.ERROR -> HazardRed
                    ValidationSeverity.WARNING -> CyberAmber
                    ValidationSeverity.INFO -> TechBlue
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(badgeColor.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (issue.severity == ValidationSeverity.ERROR) Icons.Default.Cancel else Icons.Default.Warning,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(issue.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(issue.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Recommendation: ${issue.recommendation}", fontSize = 11.sp, color = badgeColor, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

fun getComponentIcon(type: ComponentType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        ComponentType.CHASSIS -> Icons.Default.SquareFoot
        ComponentType.LEFT_WHEEL, ComponentType.RIGHT_WHEEL, ComponentType.CASTER_WHEEL -> Icons.Default.Circle
        ComponentType.DISTANCE_SENSOR -> Icons.Default.Sensors
        ComponentType.LIDAR_2D -> Icons.Default.Radar
        ComponentType.IMU_SENSOR -> Icons.Default.ScreenRotation
        ComponentType.ARM_BASE, ComponentType.ARM_LINK_1, ComponentType.ARM_LINK_2 -> Icons.Default.PrecisionManufacturing
        ComponentType.CONTROLLER -> Icons.Default.Memory
        ComponentType.BATTERY_PACK -> Icons.Default.BatteryChargingFull
    }
}

fun getComponentDescription(type: ComponentType): String {
    return when (type) {
        ComponentType.CHASSIS -> "Main rigid body platform with mounting holes"
        ComponentType.LEFT_WHEEL -> "High traction rubber drive wheel for left motor shaft"
        ComponentType.RIGHT_WHEEL -> "High traction rubber drive wheel for right motor shaft"
        ComponentType.CASTER_WHEEL -> "Passive omnidirectional swivel ball caster for balance"
        ComponentType.DISTANCE_SENSOR -> "Ultrasonic pulse transducer for frontal collision detection"
        ComponentType.LIDAR_2D -> "High-speed 360-degree laser range scanner"
        ComponentType.IMU_SENSOR -> "3-axis gyroscope and 3-axis accelerometer sensor"
        ComponentType.ARM_BASE -> "Revolute servo turntable mounting joint"
        ComponentType.ARM_LINK_1 -> "160mm aluminum shoulder link for arm reach"
        ComponentType.ARM_LINK_2 -> "140mm articulated forearm segment"
        ComponentType.CONTROLLER -> "Embedded ARM microcontroller running control loop"
        ComponentType.BATTERY_PACK -> "High-current lithium-polymer power pack"
    }
}
