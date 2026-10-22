package com.example.robolab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.example.robolab.model.GraphicsProfile
import com.example.robolab.model.Obstacle
import com.example.robolab.model.RobotComponent
import com.example.robolab.model.SimulationTelemetry
import com.example.robolab.renderer3d.Camera3D
import com.example.robolab.renderer3d.Renderer3D
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan

@Composable
fun InteractiveViewport3D(
    camera: Camera3D,
    components: List<RobotComponent>,
    robotPoseX: Float = 0f,
    robotPoseZ: Float = 0f,
    robotHeadingDeg: Float = 0f,
    obstacles: List<Obstacle> = emptyList(),
    telemetry: SimulationTelemetry? = null,
    selectedComponentId: String? = null,
    graphicsProfile: GraphicsProfile = GraphicsProfile.BALANCED,
    isWorkshopMode: Boolean = false,
    trajectoryHistory: List<Pair<Float, Float>> = emptyList(),
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    var azimuth by remember { mutableFloatStateOf(camera.azimuthDeg) }
    var elevation by remember { mutableFloatStateOf(camera.elevationDeg) }
    var distance by remember { mutableFloatStateOf(camera.distance) }
    var isOrtho by remember { mutableStateOf(camera.isOrthographic) }

    // Keep camera synced
    LaunchedEffect(azimuth, elevation, distance, isOrtho) {
        camera.azimuthDeg = azimuth
        camera.elevationDeg = elevation
        camera.distance = distance
        camera.isOrthographic = isOrtho
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Rotate orbit with drag
                    azimuth = (azimuth - pan.x * 0.45f) % 360f
                    elevation = (elevation + pan.y * 0.35f).coerceIn(8f, 85f)
                    // Zoom with pinch
                    distance = (distance / zoom).coerceIn(1.0f, 10.0f)
                }
            }
            .testTag("viewport_3d_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            Renderer3D.renderScene(
                drawScope = this,
                camera = camera,
                robotComponents = components,
                robotPoseX = robotPoseX,
                robotPoseZ = robotPoseZ,
                robotHeadingDeg = robotHeadingDeg,
                obstacles = obstacles,
                telemetry = telemetry,
                selectedComponentId = selectedComponentId,
                graphicsProfile = graphicsProfile,
                isWorkshopMode = isWorkshopMode,
                trajectoryHistory = trajectoryHistory
            )
        }

        // Viewport Overlay Controls (Camera reset, Zoom, Perspective)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    azimuth = 45f
                    elevation = 30f
                    distance = if (isWorkshopMode) 2.2f else 4.5f
                },
                containerColor = DarkSurface.copy(alpha = 0.85f),
                contentColor = ElectricCyan,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).testTag("btn_reset_camera")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Camera", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    distance = (distance - 0.6f).coerceAtLeast(1.0f)
                },
                containerColor = DarkSurface.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).testTag("btn_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    distance = (distance + 0.6f).coerceAtMost(10.0f)
                },
                containerColor = DarkSurface.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).testTag("btn_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    isOrtho = !isOrtho
                },
                containerColor = if (isOrtho) ElectricCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                contentColor = if (isOrtho) ElectricCyan else Color.White,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).testTag("btn_toggle_projection")
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = "Toggle Orthographic", modifier = Modifier.size(18.dp))
            }
        }

        // Additional user-provided overlays (telemetry, status, joystick)
        overlayContent()
    }
}
