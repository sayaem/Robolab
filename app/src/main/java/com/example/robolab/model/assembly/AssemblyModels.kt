package com.example.robolab.model.assembly

import com.example.robolab.renderer3d.Vector3
import java.util.UUID

enum class ShapeType(val displayName: String) {
    BOX("Box"),
    CYLINDER("Cylinder"),
    SPHERE("Sphere"),
    CAPSULE("Capsule")
}

data class AssemblyPart(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shapeType: ShapeType = ShapeType.BOX,
    // Dimensions in meters
    val dimX: Float = 0.20f, // Width / Diameter
    val dimY: Float = 0.20f, // Height
    val dimZ: Float = 0.20f, // Depth / Length
    val radius: Float = 0.10f, // Used for Cylinder, Sphere, Capsule
    // Physical attributes
    val massKg: Float = 1.0f,
    val isStatic: Boolean = false, // Anchored to world or dynamic
    val friction: Float = 0.6f,
    val restitution: Float = 0.2f, // Bounciness
    val colorHex: Long = 0xFF00B4D8,
    // Spatial transform relative to root or parent
    val posX: Float = 0f,
    val posY: Float = 0.5f, // spawn slightly above ground
    val posZ: Float = 0f,
    val rotYawDeg: Float = 0f,
    val rotPitchDeg: Float = 0f,
    val rotRollDeg: Float = 0f,
    val parentId: String? = null
) {
    fun getDimensionsSummary(): String {
        return when (shapeType) {
            ShapeType.BOX -> "${(dimX * 100).toInt()}x${(dimY * 100).toInt()}x${(dimZ * 100).toInt()}cm"
            ShapeType.CYLINDER -> "r:${(radius * 100).toInt()}cm h:${(dimY * 100).toInt()}cm"
            ShapeType.SPHERE -> "r:${(radius * 100).toInt()}cm"
            ShapeType.CAPSULE -> "r:${(radius * 100).toInt()}cm h:${(dimY * 100).toInt()}cm"
        }
    }
}

enum class JointType(val displayName: String) {
    REVOLUTE("Revolute (Hinge)"),
    FIXED("Fixed (Weld)"),
    PRISMATIC("Prismatic (Slider)")
}

data class AssemblyJoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: JointType = JointType.REVOLUTE,
    val parentPartId: String,
    val childPartId: String,
    // Anchor positions in local body coordinate frames
    val anchorParentX: Float = 0f,
    val anchorParentY: Float = 0f,
    val anchorParentZ: Float = 0f,
    val anchorChildX: Float = 0f,
    val anchorChildY: Float = 0f,
    val anchorChildZ: Float = 0f,
    // Joint rotation axis in local frame
    val axisX: Float = 0f,
    val axisY: Float = 1f, // Default vertical axis (yaw)
    val axisZ: Float = 0f,
    // Joint angle limits
    val enableLimits: Boolean = false,
    val lowerLimitDeg: Float = -90f,
    val upperLimitDeg: Float = 90f,
    // Motor configuration
    val enableMotor: Boolean = false,
    val motorTargetVelocityRadPerSec: Float = 2.0f,
    val motorMaxTorqueNm: Float = 15.0f
)

data class RoboLabAssembly(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom Assembly",
    val parts: List<AssemblyPart> = emptyList(),
    val joints: List<AssemblyJoint> = emptyList(),
    val gravityY: Float = -9.81f
)
