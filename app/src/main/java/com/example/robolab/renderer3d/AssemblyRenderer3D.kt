package com.example.robolab.renderer3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.robolab.model.GraphicsProfile
import com.example.robolab.model.assembly.AssemblyJoint
import com.example.robolab.model.assembly.AssemblyPart
import com.example.robolab.model.assembly.JointType
import com.example.robolab.model.assembly.ShapeType
import com.example.robolab.physics.rigid.Matrix3
import com.example.robolab.physics.rigid.Quaternion
import kotlin.math.*

object AssemblyRenderer3D {

    private val lightDir = Vector3(0.5f, 1.0f, 0.4f).normalized()

    private data class RenderablePoly(
        val screenPoints: List<ProjectedPoint>,
        val avgDepth: Float,
        val shadedColor: Color,
        val outlineColor: Color?
    )

    fun renderAssemblyScene(
        drawScope: DrawScope,
        camera: Camera3D,
        parts: List<AssemblyPart>,
        joints: List<AssemblyJoint>,
        dynamicPoses: Map<String, Pair<Vector3, Matrix3>> = emptyMap(),
        selectedPartId: String? = null,
        selectedJointId: String? = null,
        graphicsProfile: GraphicsProfile = GraphicsProfile.BALANCED,
        isWorkshopMode: Boolean = true
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Draw 3D Ground Grid
        drawGroundGrid(drawScope, camera, width, height, isWorkshopMode)

        // 2. Draw Drop Shadows under parts
        for (part in parts) {
            val pose = dynamicPoses[part.id]
            val px = pose?.first?.x ?: part.posX
            val pz = pose?.first?.z ?: part.posZ
            val shadowR = max(part.dimX, part.dimZ) * 0.6f
            drawContactShadow(drawScope, camera, width, height, px, pz, shadowR)
        }

        // 3. Generate Polygons for all parts
        val polysToRender = mutableListOf<RenderablePoly>()

        for (part in parts) {
            val isSelected = isWorkshopMode && part.id == selectedPartId
            val pose = dynamicPoses[part.id]
            val center = pose?.first ?: Vector3(part.posX, part.posY, part.posZ)
            val rotMat = pose?.second ?: Quaternion.fromEulerDeg(part.rotYawDeg, part.rotPitchDeg, part.rotRollDeg).toMatrix3()

            val baseColor = Color(part.colorHex)
            val outline = if (isSelected) Color(0xFF00F0FF) else Color(0x55FFFFFF)

            val partPolys = when (part.shapeType) {
                ShapeType.BOX -> createBoxPolygons(center, part.dimX, part.dimY, part.dimZ, rotMat, baseColor, outline)
                ShapeType.CYLINDER -> createCylinderPolygons(center, part.radius, part.dimY, rotMat, baseColor, outline)
                ShapeType.SPHERE -> createSpherePolygons(center, part.radius, baseColor, outline)
                ShapeType.CAPSULE -> createCapsulePolygons(center, part.radius, part.dimY, rotMat, baseColor, outline)
            }

            for (p in partPolys) {
                val proj = p.vertices.mapNotNull { camera.project(it, width, height) }
                if (proj.size >= 3) {
                    val avgDepth = proj.map { it.depth }.average().toFloat()
                    val norm = p.normal ?: calculateNormal(p.vertices)
                    val diffuse = max(0.20f, norm.dot(lightDir) * 0.85f)
                    val shaded = Color(
                        (baseColor.red * diffuse).coerceIn(0f, 1f),
                        (baseColor.green * diffuse).coerceIn(0f, 1f),
                        (baseColor.blue * diffuse).coerceIn(0f, 1f),
                        baseColor.alpha
                    )
                    polysToRender.add(RenderablePoly(proj, avgDepth, shaded, p.outlineColor))
                }
            }
        }

        // 4. Depth sort (Painter's algorithm: farthest first)
        polysToRender.sortByDescending { it.avgDepth }

        // 5. Draw Polygons
        for (rp in polysToRender) {
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

        // 6. Draw Joint Visualizations (Pivots, Anchors, and Axes)
        drawJointGizmos(drawScope, camera, width, height, joints, parts, dynamicPoses, selectedJointId)

        // 7. Draw Transform Gizmo on selected part
        if (isWorkshopMode && selectedPartId != null) {
            val sel = parts.find { it.id == selectedPartId }
            if (sel != null) {
                val pose = dynamicPoses[sel.id]
                val origin = pose?.first ?: Vector3(sel.posX, sel.posY, sel.posZ)
                drawTransformGizmo(drawScope, camera, width, height, origin)
            }
        }
    }

    private fun calculateNormal(vertices: List<Vector3>): Vector3 {
        if (vertices.size < 3) return Vector3(0f, 1f, 0f)
        val v0 = vertices[0]
        val v1 = vertices[1]
        val v2 = vertices[2]
        return (v1 - v0).cross(v2 - v0).normalized()
    }

    private fun createBoxPolygons(
        center: Vector3,
        width: Float,
        height: Float,
        depth: Float,
        rot: Matrix3,
        color: Color,
        outline: Color?
    ): List<Polygon3D> {
        val hx = width * 0.5f
        val hy = height * 0.5f
        val hz = depth * 0.5f

        fun p(x: Float, y: Float, z: Float) = center + rot * Vector3(x, y, z)

        val v000 = p(-hx, -hy, -hz)
        val v100 = p(hx, -hy, -hz)
        val v110 = p(hx, hy, -hz)
        val v010 = p(-hx, hy, -hz)
        val v001 = p(-hx, -hy, hz)
        val v101 = p(hx, -hy, hz)
        val v111 = p(hx, hy, hz)
        val v011 = p(-hx, hy, hz)

        return listOf(
            Polygon3D(listOf(v010, v110, v111, v011), color, outline), // Top (+Y)
            Polygon3D(listOf(v000, v001, v101, v100), color, outline), // Bottom (-Y)
            Polygon3D(listOf(v001, v011, v111, v101), color, outline), // Front (+Z)
            Polygon3D(listOf(v100, v110, v010, v000), color, outline), // Back (-Z)
            Polygon3D(listOf(v000, v010, v011, v001), color, outline), // Left (-X)
            Polygon3D(listOf(v101, v111, v110, v100), color, outline)  // Right (+X)
        )
    }

    private fun createCylinderPolygons(
        center: Vector3,
        radius: Float,
        height: Float,
        rot: Matrix3,
        color: Color,
        outline: Color?
    ): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        val segments = 12
        val hy = height * 0.5f

        val topVerts = mutableListOf<Vector3>()
        val bottomVerts = mutableListOf<Vector3>()

        for (i in 0 until segments) {
            val theta = (i * 2.0 * Math.PI / segments).toFloat()
            val cx = radius * cos(theta)
            val cz = radius * sin(theta)
            topVerts.add(center + rot * Vector3(cx, hy, cz))
            bottomVerts.add(center + rot * Vector3(cx, -hy, cz))
        }

        // Top Disc
        polys.add(Polygon3D(topVerts, color, outline))
        // Bottom Disc (reverse for correct face normal)
        polys.add(Polygon3D(bottomVerts.reversed(), color, outline))

        // Side Quads
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            val quad = listOf(bottomVerts[i], topVerts[i], topVerts[next], bottomVerts[next])
            polys.add(Polygon3D(quad, color, outline))
        }

        return polys
    }

    private fun createSpherePolygons(
        center: Vector3,
        radius: Float,
        color: Color,
        outline: Color?
    ): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        val stacks = 6
        val slices = 8

        for (i in 0 until stacks) {
            val phi1 = (i * Math.PI / stacks).toFloat()
            val phi2 = ((i + 1) * Math.PI / stacks).toFloat()

            val y1 = radius * cos(phi1)
            val r1 = radius * sin(phi1)
            val y2 = radius * cos(phi2)
            val r2 = radius * sin(phi2)

            for (j in 0 until slices) {
                val theta1 = (j * 2.0 * Math.PI / slices).toFloat()
                val theta2 = ((j + 1) * 2.0 * Math.PI / slices).toFloat()

                val v1 = center + Vector3(r1 * cos(theta1), y1, r1 * sin(theta1))
                val v2 = center + Vector3(r1 * cos(theta2), y1, r1 * sin(theta2))
                val v3 = center + Vector3(r2 * cos(theta2), y2, r2 * sin(theta2))
                val v4 = center + Vector3(r2 * cos(theta1), y2, r2 * sin(theta1))

                polys.add(Polygon3D(listOf(v1, v2, v3, v4), color, outline))
            }
        }
        return polys
    }

    private fun createCapsulePolygons(
        center: Vector3,
        radius: Float,
        height: Float,
        rot: Matrix3,
        color: Color,
        outline: Color?
    ): List<Polygon3D> {
        return createCylinderPolygons(center, radius, height, rot, color, outline)
    }

    private fun drawJointGizmos(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        joints: List<AssemblyJoint>,
        parts: List<AssemblyPart>,
        dynamicPoses: Map<String, Pair<Vector3, Matrix3>>,
        selectedJointId: String?
    ) {
        val partMap = parts.associateBy { it.id }

        for (joint in joints) {
            val parentPart = partMap[joint.parentPartId] ?: continue
            val childPart = partMap[joint.childPartId] ?: continue

            val parentPose = dynamicPoses[parentPart.id]
            val parentPos = parentPose?.first ?: Vector3(parentPart.posX, parentPart.posY, parentPart.posZ)
            val parentRot = parentPose?.second ?: Quaternion.fromEulerDeg(parentPart.rotYawDeg, parentPart.rotPitchDeg, parentPart.rotRollDeg).toMatrix3()

            val anchorLocal = Vector3(joint.anchorParentX, joint.anchorParentY, joint.anchorParentZ)
            val anchorWorld = parentPos + parentRot * anchorLocal

            val axisLocal = Vector3(joint.axisX, joint.axisY, joint.axisZ).normalized()
            val axisWorld = (parentRot * axisLocal).normalized()

            val pAnchor = camera.project(anchorWorld, width, height)
            val pAxisEnd = camera.project(anchorWorld + axisWorld * 0.15f, width, height)

            if (pAnchor != null) {
                val isSelected = joint.id == selectedJointId
                val jointColor = if (isSelected) Color(0xFF00F0FF) else if (joint.enableMotor) Color(0xFF10B981) else Color(0xFFF59E0B)

                // Joint anchor center disc
                drawScope.drawCircle(
                    color = jointColor,
                    radius = if (isSelected) 8f else 6f,
                    center = Offset(pAnchor.x, pAnchor.y)
                )

                // Hinge axis arrow
                if (pAxisEnd != null) {
                    drawScope.drawLine(
                        color = jointColor,
                        start = Offset(pAnchor.x, pAnchor.y),
                        end = Offset(pAxisEnd.x, pAxisEnd.y),
                        strokeWidth = 3.5f
                    )
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
        val xArr = camera.project(origin + Vector3(0.3f, 0f, 0f), width, height)
        val yArr = camera.project(origin + Vector3(0f, 0.3f, 0f), width, height)
        val zArr = camera.project(origin + Vector3(0f, 0f, 0.3f), width, height)

        val c = Offset(o.x, o.y)
        if (xArr != null) drawScope.drawLine(Color(0xFFEF4444), c, Offset(xArr.x, xArr.y), strokeWidth = 4f)
        if (yArr != null) drawScope.drawLine(Color(0xFF10B981), c, Offset(yArr.x, yArr.y), strokeWidth = 4f)
        if (zArr != null) drawScope.drawLine(Color(0xFF38BDF8), c, Offset(zArr.x, zArr.y), strokeWidth = 4f)
        drawScope.drawCircle(Color.White, radius = 5f, center = c)
    }

    private fun drawGroundGrid(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        isWorkshop: Boolean
    ) {
        val gridStep = if (isWorkshop) 0.5f else 1.0f
        val range = 4
        val gridColor = Color(0x33485563)

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

        // Coordinate axes at origin (X=Red, Y=Green, Z=Blue)
        val o = camera.project(Vector3(0f, 0f, 0f), width, height)
        val xp = camera.project(Vector3(0.5f, 0f, 0f), width, height)
        val yp = camera.project(Vector3(0f, 0.5f, 0f), width, height)
        val zp = camera.project(Vector3(0f, 0f, 0.5f), width, height)

        if (o != null) {
            val c = Offset(o.x, o.y)
            if (xp != null) drawScope.drawLine(Color(0xFFEF4444), c, Offset(xp.x, xp.y), strokeWidth = 3f)
            if (yp != null) drawScope.drawLine(Color(0xFF10B981), c, Offset(yp.x, yp.y), strokeWidth = 3f)
            if (zp != null) drawScope.drawLine(Color(0xFF38BDF8), c, Offset(zp.x, zp.y), strokeWidth = 3f)
        }
    }

    private fun drawContactShadow(
        drawScope: DrawScope,
        camera: Camera3D,
        width: Float,
        height: Float,
        px: Float,
        pz: Float,
        radius: Float
    ) {
        val segments = 10
        val points = mutableListOf<ProjectedPoint>()
        for (i in 0 until segments) {
            val theta = (i * 2.0 * Math.PI / segments).toFloat()
            val sx = px + radius * cos(theta)
            val sz = pz + radius * sin(theta)
            val pt = camera.project(Vector3(sx, 0.002f, sz), width, height)
            if (pt != null) points.add(pt)
        }
        if (points.size >= 3) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (j in 1 until points.size) lineTo(points[j].x, points[j].y)
                close()
            }
            drawScope.drawPath(path, Color(0x66000000))
        }
    }
}
