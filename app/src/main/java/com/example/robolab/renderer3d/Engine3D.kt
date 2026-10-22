package com.example.robolab.renderer3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.robolab.model.*
import kotlin.math.*

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vector3 {
        val len = length()
        return if (len > 1e-6f) Vector3(x / len, y / len, z / len) else Vector3(0f, 1f, 0f)
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

data class Camera3D(
    var azimuthDeg: Float = 45f,
    var elevationDeg: Float = 30f,
    var distance: Float = 3.5f,
    var targetX: Float = 0f,
    var targetY: Float = 0.1f,
    var targetZ: Float = 0f,
    var isOrthographic: Boolean = false
) {
    fun project(point: Vector3, screenWidth: Float, screenHeight: Float): ProjectedPoint? {
        val radAzimuth = Math.toRadians(azimuthDeg.toDouble()).toFloat()
        val radElevation = Math.toRadians(elevationDeg.coerceIn(5f, 88f).toDouble()).toFloat()

        // Camera position relative to target
        val camX = targetX + distance * cos(radElevation) * sin(radAzimuth)
        val camY = targetY + distance * sin(radElevation)
        val camZ = targetZ + distance * cos(radElevation) * cos(radAzimuth)

        // Translation relative to camera
        val rx = point.x - camX
        val ry = point.y - camY
        val rz = point.z - camZ

        // Camera orientation vectors
        val forward = Vector3(targetX - camX, targetY - camY, targetZ - camZ).normalized()
        val worldUp = Vector3(0f, 1f, 0f)
        val right = forward.cross(worldUp).normalized()
        val up = right.cross(forward).normalized()

        // Transform into camera coordinates
        val viewX = rx * right.x + ry * right.y + rz * right.z
        val viewY = rx * up.x + ry * up.y + rz * up.z
        val viewZ = rx * forward.x + ry * forward.y + rz * forward.z

        if (!isOrthographic && viewZ <= 0.05f) return null // Behind near clipping plane

        val fovFactor = min(screenWidth, screenHeight) * 0.9f
        val (screenX, screenY) = if (isOrthographic) {
            val orthoScale = fovFactor / 2.5f
            Pair(
                screenWidth / 2f + viewX * orthoScale,
                screenHeight / 2f - viewY * orthoScale
            )
        } else {
            Pair(
                screenWidth / 2f + (viewX / viewZ) * fovFactor,
                screenHeight / 2f - (viewY / viewZ) * fovFactor
            )
        }

        return ProjectedPoint(screenX, screenY, depth = viewZ)
    }
}

data class ProjectedPoint(val x: Float, val y: Float, val depth: Float)

data class Polygon3D(
    val vertices: List<Vector3>,
    val baseColor: Color,
    val outlineColor: Color? = null,
    val normal: Vector3? = null
)

object Renderer3D {

    private val lightDir = Vector3(0.5f, 1.0f, 0.3f).normalized()

    /**
     * Renders ground grid, coordinate axes, obstacles, robot, sensor telemetry, and trajectory.
     */
    fun renderScene(
        drawScope: DrawScope,
        camera: Camera3D,
        robotComponents: List<RobotComponent>,
        robotPoseX: Float,
        robotPoseZ: Float,
        robotHeadingDeg: Float,
        obstacles: List<Obstacle>,
        telemetry: SimulationTelemetry?,
        selectedComponentId: String? = null,
        graphicsProfile: GraphicsProfile = GraphicsProfile.BALANCED,
        isWorkshopMode: Boolean = false,
        trajectoryHistory: List<Pair<Float, Float>> = emptyList()
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Draw 3D Ground Grid
        drawGroundGrid(drawScope, camera, width, height, isWorkshopMode, graphicsProfile)

        // 2. Draw Trajectory Trail
        if (trajectoryHistory.size > 1) {
            drawTrajectoryTrail(drawScope, camera, width, height, trajectoryHistory)
        }

        // 3. Draw Drop Shadow under Robot
        drawContactShadow(drawScope, camera, width, height, robotPoseX, robotPoseZ)

        // 4. Collect Polygons from Obstacles and Robot Components
        val polygonsToRender = mutableListOf<RenderablePolygon>()

        // Add Obstacles
        for (obs in obstacles) {
            val obsPolys = createBoxPolygons(
                center = Vector3(obs.posX, obs.height / 2f, obs.posZ),
                width = obs.width,
                height = obs.height,
                length = obs.length,
                color = Color(obs.colorHex)
            )
            for (p in obsPolys) {
                polygonsToRender.add(preparePolygon(p, camera, width, height))
            }
        }

        // Add Robot Components (transformed into world coordinates)
        val headingRad = Math.toRadians(robotHeadingDeg.toDouble()).toFloat()
        val cosH = cos(headingRad)
        val sinH = sin(headingRad)

        for (comp in robotComponents) {
            if (!comp.isEnabled) continue
            // Transform local component offset by robot heading + world pose
            val worldCompX = robotPoseX + (comp.posX * cosH - comp.posZ * sinH)
            val worldCompY = 0.05f + comp.posY // slightly above ground
            val worldCompZ = robotPoseZ + (comp.posX * sinH + comp.posZ * cosH)

            val isSelected = isWorkshopMode && comp.id == selectedComponentId

            val compPolys = generateComponentPolygons(
                comp = comp,
                worldX = worldCompX,
                worldY = worldCompY,
                worldZ = worldCompZ,
                robotHeadingDeg = robotHeadingDeg,
                isSelected = isSelected,
                graphicsProfile = graphicsProfile
            )

            for (p in compPolys) {
                polygonsToRender.add(preparePolygon(p, camera, width, height))
            }
        }

        // 5. Depth Sort (Painter's Algorithm: farthest depth first)
        polygonsToRender.sortByDescending { it.avgDepth }

        // 6. Draw Polygons
        for (rp in polygonsToRender) {
            if (rp.screenPoints.size >= 3) {
                val path = Path().apply {
                    moveTo(rp.screenPoints[0].x, rp.screenPoints[0].y)
                    for (i in 1 until rp.screenPoints.size) {
                        lineTo(rp.screenPoints[i].x, rp.screenPoints[i].y)
                    }
                    close()
                }
                drawScope.drawPath(path, rp.shadedColor)
                if (rp.outlineColor != null) {
                    drawScope.drawPath(path, rp.outlineColor, style = Stroke(width = 1.2f))
                }
            }
        }

        // 7. Sensor Ray Visualizations
        if (telemetry != null) {
            drawSensorRays(drawScope, camera, width, height, robotPoseX, robotPoseZ, headingRad, telemetry, graphicsProfile)
        }

        // 8. Transform Gizmos in Workshop Mode
        if (isWorkshopMode && selectedComponentId != null) {
            val selected = robotComponents.find { it.id == selectedComponentId }
            if (selected != null) {
                val gx = robotPoseX + (selected.posX * cosH - selected.posZ * sinH)
                val gy = 0.05f + selected.posY
                val gz = robotPoseZ + (selected.posX * sinH + selected.posZ * cosH)
                drawTransformGizmo(drawScope, camera, width, height, Vector3(gx, gy, gz))
            }
        }
    }

    private data class RenderablePolygon(
        val screenPoints: List<ProjectedPoint>,
        val avgDepth: Float,
        val shadedColor: Color,
        val outlineColor: Color?
    )

    private fun preparePolygon(
        poly: Polygon3D,
        camera: Camera3D,
        screenWidth: Float,
        screenHeight: Float
    ): RenderablePolygon {
        val proj = poly.vertices.mapNotNull { camera.project(it, screenWidth, screenHeight) }
        val avgDepth = if (proj.isNotEmpty()) proj.map { it.depth }.average().toFloat() else 0f

        // Lighting calculation
        val normal = poly.normal ?: calculateNormal(poly.vertices)
        val diffuse = max(0.18f, normal.dot(lightDir) * 0.85f)
        val r = (poly.baseColor.red * diffuse).coerceIn(0f, 1f)
        val g = (poly.baseColor.green * diffuse).coerceIn(0f, 1f)
        val b = (poly.baseColor.blue * diffuse).coerceIn(0f, 1f)
        val shaded = Color(r, g, b, poly.baseColor.alpha)

        return RenderablePolygon(proj, avgDepth, shaded, poly.outlineColor)
    }

    private fun calculateNormal(vertices: List<Vector3>): Vector3 {
        if (vertices.size < 3) return Vector3(0f, 1f, 0f)
        val v0 = vertices[0]
        val v1 = vertices[1]
        val v2 = vertices[2]
        return (v1 - v0).cross(v2 - v0).normalized()
    }

    private fun drawGroundGrid(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        isWorkshop: Boolean,
        profile: GraphicsProfile
    ) {
        val gridStep = if (isWorkshop) 0.5f else 1.0f
        val range = if (profile == GraphicsProfile.PERFORMANCE) 3 else 5
        val gridColor = Color(0x33485563)
        val axisOrigin = Vector3(0f, 0f, 0f)

        for (i in -range..range) {
            val z = i * gridStep
            val p1 = camera.project(Vector3(-range * gridStep, 0f, z), width, height)
            val p2 = camera.project(Vector3(range * gridStep, 0f, z), width, height)
            if (p1 != null && p2 != null) {
                drawScope.drawLine(gridColor, Offset(p1.x, p1.y), Offset(p2.x, p2.y), strokeWidth = 1f)
            }

            val x = i * gridStep
            val p3 = camera.project(Vector3(x, 0f, -range * gridStep), width, height)
            val p4 = camera.project(Vector3(x, 0f, range * gridStep), width, height)
            if (p3 != null && p4 != null) {
                drawScope.drawLine(gridColor, Offset(p3.x, p3.y), Offset(p4.x, p4.y), strokeWidth = 1f)
            }
        }

        // Draw Coordinate Axes (Origin marker: X=Red, Z=Blue, Y=Green)
        val originP = camera.project(axisOrigin, width, height)
        val xP = camera.project(Vector3(0.5f, 0f, 0f), width, height)
        val yP = camera.project(Vector3(0f, 0.5f, 0f), width, height)
        val zP = camera.project(Vector3(0f, 0f, 0.5f), width, height)

        if (originP != null) {
            val o = Offset(originP.x, originP.y)
            if (xP != null) drawScope.drawLine(Color(0xFFEF4444), o, Offset(xP.x, xP.y), strokeWidth = 3f)
            if (yP != null) drawScope.drawLine(Color(0xFF10B981), o, Offset(yP.x, yP.y), strokeWidth = 3f)
            if (zP != null) drawScope.drawLine(Color(0xFF38BDF8), o, Offset(zP.x, zP.y), strokeWidth = 3f)
        }
    }

    private fun drawContactShadow(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        robotX: Float,
        robotZ: Float
    ) {
        val segments = 12
        val shadowRadius = 0.26f
        val points = mutableListOf<ProjectedPoint>()
        for (i in 0 until segments) {
            val theta = (i * 2.0 * Math.PI / segments).toFloat()
            val sx = robotX + shadowRadius * cos(theta)
            val sz = robotZ + shadowRadius * sin(theta)
            val pt = camera.project(Vector3(sx, 0.002f, sz), width, height)
            if (pt != null) points.add(pt)
        }
        if (points.size >= 3) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (j in 1 until points.size) lineTo(points[j].x, points[j].y)
                close()
            }
            drawScope.drawPath(path, Color(0x77000000))
        }
    }

    private fun drawTrajectoryTrail(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        history: List<Pair<Float, Float>>
    ) {
        for (i in 0 until history.size - 1) {
            val (x1, z1) = history[i]
            val (x2, z2) = history[i + 1]
            val p1 = camera.project(Vector3(x1, 0.005f, z1), width, height)
            val p2 = camera.project(Vector3(x2, 0.005f, z2), width, height)
            if (p1 != null && p2 != null) {
                val alpha = (i.toFloat() / history.size).coerceIn(0.2f, 0.9f)
                drawScope.drawLine(
                    Color(0xFF00F0FF).copy(alpha = alpha),
                    Offset(p1.x, p1.y),
                    Offset(p2.x, p2.y),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun drawSensorRays(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        rx: Float,
        rz: Float,
        headingRad: Float,
        telemetry: SimulationTelemetry,
        profile: GraphicsProfile
    ) {
        // Distance sensor forward beam
        val sensorOrigin = Vector3(
            rx + 0.15f * sin(headingRad),
            0.12f,
            rz + 0.15f * cos(headingRad)
        )
        val hitDist = telemetry.distanceSensorReadingM
        val hitPoint = Vector3(
            sensorOrigin.x + hitDist * sin(headingRad),
            sensorOrigin.y,
            sensorOrigin.z + hitDist * cos(headingRad)
        )

        val pOrigin = camera.project(sensorOrigin, width, height)
        val pHit = camera.project(hitPoint, width, height)

        if (pOrigin != null && pHit != null) {
            val beamColor = if (hitDist < 0.45f) Color(0xFFEF4444) else Color(0xFF00F0FF)
            drawScope.drawLine(
                beamColor.copy(alpha = 0.85f),
                Offset(pOrigin.x, pOrigin.y),
                Offset(pHit.x, pHit.y),
                strokeWidth = 3f
            )
            // Hit marker point
            drawScope.drawCircle(beamColor, radius = 5f, center = Offset(pHit.x, pHit.y))
        }

        // 2D LiDAR Scan Points & Fans (if available)
        if (profile != GraphicsProfile.PERFORMANCE && telemetry.lidarReadings.isNotEmpty()) {
            val step = if (profile == GraphicsProfile.HIGH_QUALITY) 1 else 2
            for (idx in telemetry.lidarReadings.indices step step) {
                val lp = telemetry.lidarReadings[idx]
                val pt = camera.project(Vector3(lp.hitX, 0.08f, lp.hitZ), width, height)
                if (pt != null) {
                    val ptColor = if (lp.distanceM < 0.6f) Color(0xFFF59E0B) else Color(0xAA38BDF8)
                    drawScope.drawCircle(ptColor, radius = 3f, center = Offset(pt.x, pt.y))
                }
            }
        }
    }

    private fun drawTransformGizmo(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        origin: Vector3
    ) {
        val o = camera.project(origin, width, height) ?: return
        val xArr = camera.project(origin + Vector3(0.25f, 0f, 0f), width, height)
        val yArr = camera.project(origin + Vector3(0f, 0.25f, 0f), width, height)
        val zArr = camera.project(origin + Vector3(0f, 0f, 0.25f), width, height)

        val c = Offset(o.x, o.y)
        if (xArr != null) drawScope.drawLine(Color(0xFFEF4444), c, Offset(xArr.x, xArr.y), strokeWidth = 4f)
        if (yArr != null) drawScope.drawLine(Color(0xFF10B981), c, Offset(yArr.x, yArr.y), strokeWidth = 4f)
        if (zArr != null) drawScope.drawLine(Color(0xFF38BDF8), c, Offset(zArr.x, zArr.y), strokeWidth = 4f)
        drawScope.drawCircle(Color.White, radius = 4f, center = c)
    }

    private fun createBoxPolygons(
        center: Vector3,
        width: Float,
        height: Float,
        length: Float,
        color: Color,
        outlineColor: Color? = Color(0x66FFFFFF),
        headingDeg: Float = 0f
    ): List<Polygon3D> {
        val hw = width / 2f
        val hh = height / 2f
        val hl = length / 2f

        val hRad = Math.toRadians(headingDeg.toDouble()).toFloat()
        val cosH = cos(hRad)
        val sinH = sin(hRad)

        fun rotatePoint(lx: Float, ly: Float, lz: Float): Vector3 {
            val rx = lx * cosH - lz * sinH
            val rz = lx * sinH + lz * cosH
            return Vector3(center.x + rx, center.y + ly, center.z + rz)
        }

        // 8 box vertices
        val v000 = rotatePoint(-hw, -hh, -hl)
        val v100 = rotatePoint(hw, -hh, -hl)
        val v110 = rotatePoint(hw, hh, -hl)
        val v010 = rotatePoint(-hw, hh, -hl)
        val v001 = rotatePoint(-hw, -hh, hl)
        val v101 = rotatePoint(hw, -hh, hl)
        val v111 = rotatePoint(hw, hh, hl)
        val v011 = rotatePoint(-hw, hh, hl)

        return listOf(
            // Top (+Y)
            Polygon3D(listOf(v010, v110, v111, v011), color, outlineColor),
            // Bottom (-Y)
            Polygon3D(listOf(v000, v001, v101, v100), color, outlineColor),
            // Front (+Z)
            Polygon3D(listOf(v001, v011, v111, v101), color, outlineColor),
            // Back (-Z)
            Polygon3D(listOf(v100, v110, v010, v000), color, outlineColor),
            // Left (-X)
            Polygon3D(listOf(v000, v010, v011, v001), color, outlineColor),
            // Right (+X)
            Polygon3D(listOf(v101, v111, v110, v100), color, outlineColor)
        )
    }

    private fun generateComponentPolygons(
        comp: RobotComponent,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        robotHeadingDeg: Float,
        isSelected: Boolean,
        graphicsProfile: GraphicsProfile
    ): List<Polygon3D> {
        val outline = if (isSelected) Color(0xFF00F0FF) else Color(0x448B949E)
        val center = Vector3(worldX, worldY, worldZ)

        return when (comp.type) {
            ComponentType.CHASSIS -> {
                val chassisColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF21262D)
                createBoxPolygons(
                    center = center + Vector3(0f, comp.dimHeight / 2f, 0f),
                    width = comp.dimWidth,
                    height = comp.dimHeight,
                    length = comp.dimLength,
                    color = chassisColor,
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.LEFT_WHEEL, ComponentType.RIGHT_WHEEL -> {
                val wheelColor = Color(0xFF111827)
                // Draw wheel as a thick disc/box oriented with the chassis
                val wheelWidth = 0.035f
                createBoxPolygons(
                    center = center + Vector3(0f, comp.wheelRadius, 0f),
                    width = wheelWidth,
                    height = comp.wheelRadius * 2f,
                    length = comp.wheelRadius * 2f,
                    color = wheelColor,
                    outlineColor = if (isSelected) Color(0xFF00F0FF) else Color(0xFF4B5563),
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.CASTER_WHEEL -> {
                createBoxPolygons(
                    center = center + Vector3(0f, comp.wheelRadius * 0.7f, 0f),
                    width = 0.03f,
                    height = 0.03f,
                    length = 0.03f,
                    color = Color(0xFF9CA3AF),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.DISTANCE_SENSOR -> {
                // Ultrasonic sensor module
                val bodyColor = Color(0xFF1E293B)
                val base = createBoxPolygons(
                    center = center + Vector3(0f, 0.03f, 0f),
                    width = 0.06f,
                    height = 0.03f,
                    length = 0.025f,
                    color = bodyColor,
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
                base
            }
            ComponentType.LIDAR_2D -> {
                // Cylindrical LiDAR turret
                val turretColor = Color(0xFF0F172A)
                createBoxPolygons(
                    center = center + Vector3(0f, 0.04f, 0f),
                    width = 0.08f,
                    height = 0.05f,
                    length = 0.08f,
                    color = turretColor,
                    outlineColor = if (isSelected) Color(0xFF00F0FF) else Color(0xFF00E5FF),
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.ARM_BASE -> {
                createBoxPolygons(
                    center = center + Vector3(0f, 0.03f, 0f),
                    width = 0.08f,
                    height = 0.04f,
                    length = 0.08f,
                    color = Color(0xFF334155),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.ARM_LINK_1 -> {
                val linkLength = 0.16f
                createBoxPolygons(
                    center = center + Vector3(0f, linkLength / 2f + 0.04f, 0f),
                    width = 0.03f,
                    height = linkLength,
                    length = 0.03f,
                    color = Color(0xFF0284C7),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.ARM_LINK_2 -> {
                val linkLength = 0.14f
                createBoxPolygons(
                    center = center + Vector3(0f, linkLength / 2f + 0.20f, 0.04f),
                    width = 0.025f,
                    height = linkLength,
                    length = 0.025f,
                    color = Color(0xFF38BDF8),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.CONTROLLER -> {
                createBoxPolygons(
                    center = center + Vector3(0f, 0.08f, 0f),
                    width = 0.12f,
                    height = 0.02f,
                    length = 0.10f,
                    color = Color(0xFF065F46),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.BATTERY_PACK -> {
                createBoxPolygons(
                    center = center + Vector3(0f, 0.04f, -0.05f),
                    width = 0.14f,
                    height = 0.035f,
                    length = 0.07f,
                    color = Color(0xFF7C2D12),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
            ComponentType.IMU_SENSOR -> {
                createBoxPolygons(
                    center = center + Vector3(0f, 0.05f, 0f),
                    width = 0.03f,
                    height = 0.015f,
                    length = 0.03f,
                    color = Color(0xFF6B21A8),
                    outlineColor = outline,
                    headingDeg = robotHeadingDeg
                )
            }
        }
    }
}
