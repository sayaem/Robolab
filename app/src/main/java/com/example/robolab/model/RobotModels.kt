package com.example.robolab.model

import java.util.UUID

enum class ComponentType(val displayName: String, val category: ComponentCategory) {
    CHASSIS("Chassis Frame", ComponentCategory.STRUCTURAL),
    LEFT_WHEEL("Left Drive Wheel", ComponentCategory.MOTION),
    RIGHT_WHEEL("Right Drive Wheel", ComponentCategory.MOTION),
    CASTER_WHEEL("Support Caster", ComponentCategory.MOTION),
    DISTANCE_SENSOR("Ultrasonic Sensor", ComponentCategory.SENSOR),
    LIDAR_2D("2D LiDAR Scanner", ComponentCategory.SENSOR),
    IMU_SENSOR("6-DOF IMU", ComponentCategory.SENSOR),
    ARM_BASE("Arm Turret Joint", ComponentCategory.ARM),
    ARM_LINK_1("Shoulder Link", ComponentCategory.ARM),
    ARM_LINK_2("Forearm Link", ComponentCategory.ARM),
    CONTROLLER("MCU Controller", ComponentCategory.CONTROL),
    BATTERY_PACK("LiPo Battery Pack", ComponentCategory.CONTROL)
}

enum class ComponentCategory(val title: String) {
    STRUCTURAL("Structural"),
    MOTION("Actuators & Wheels"),
    SENSOR("Sensors & Telemetry"),
    ARM("Manipulators"),
    CONTROL("Power & Logic")
}

data class RobotComponent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ComponentType,
    val parentId: String? = null,
    // Spatial placement relative to chassis center (in meters)
    val posX: Float = 0f,
    val posY: Float = 0f, // vertical offset in CAD space
    val posZ: Float = 0f, // forward/backward
    val rotYaw: Float = 0f, // rotation around vertical axis (deg)
    val rotPitch: Float = 0f,
    val rotRoll: Float = 0f,
    // Physical attributes
    val dimWidth: Float = 0.28f, // meters
    val dimLength: Float = 0.38f,
    val dimHeight: Float = 0.12f,
    val massKg: Float = 1.2f,
    // Wheel & joint characteristics
    val wheelRadius: Float = 0.045f, // 45mm
    val motorMaxRpm: Float = 180f,
    val motorTorqueNm: Float = 1.5f,
    val jointAngleDeg: Float = 0f,
    val jointLimitMinDeg: Float = -135f,
    val jointLimitMaxDeg: Float = 135f,
    // Sensor characteristics
    val sensorRangeM: Float = 3.0f,
    val sensorFovDeg: Float = 35f,
    val isEnabled: Boolean = true
)

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class ValidationIssue(
    val severity: ValidationSeverity,
    val title: String,
    val message: String,
    val recommendation: String,
    val componentId: String? = null
)

enum class BlockType(val label: String, val category: String) {
    START("When Started", "Control"),
    DRIVE_FORWARD("Drive Forward", "Motion"),
    ROTATE("Turn In Place", "Motion"),
    SET_WHEEL_SPEEDS("Set Wheel Speeds", "Motion"),
    WAIT("Wait Duration", "Timing"),
    STOP("Emergency Stop", "Motion"),
    IF_OBSTACLE("If Distance < Threshold", "Condition"),
    WHILE_CLEAR("While Distance > Threshold", "Loop"),
    REPEAT("Repeat Times", "Loop"),
    SET_ARM_ANGLES("Set Arm Joint Angles", "Arm"),
    LOG_MESSAGE("Telemetry Log", "Debug")
}

data class ProgramBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val speed: Float = 0.5f,          // 0.0 .. 1.0
    val leftSpeed: Float = 0.5f,      // -1.0 .. 1.0
    val rightSpeed: Float = 0.5f,     // -1.0 .. 1.0
    val durationSec: Float = 1.5f,
    val angleDeg: Float = 90f,
    val thresholdM: Float = 0.40f,
    val repeatCount: Int = 3,
    val armAngle1Deg: Float = 30f,
    val armAngle2Deg: Float = -45f,
    val message: String = "Scanning environment...",
    val childBlocks: List<ProgramBlock> = emptyList(),
    val elseBlocks: List<ProgramBlock> = emptyList()
)

data class Obstacle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val posX: Float, // in meters
    val posZ: Float, // in meters
    val width: Float,
    val length: Float,
    val height: Float = 0.35f,
    val colorHex: Long = 0xFF374151
)

enum class GraphicsProfile(val label: String, val description: String) {
    PERFORMANCE("Performance", "Fast rasterization, 16 sensor rays, simple lighting"),
    BALANCED("Balanced", "Smooth 3D shaders, 32 sensor rays, contact shadows"),
    HIGH_QUALITY("High Quality", "Detailed mesh bevels, specular lighting, 64 rays")
}

data class SimulationTelemetry(
    val simTimeSec: Float = 0f,
    val realFps: Int = 60,
    val posX: Float = 0f,
    val posZ: Float = 0f,
    val headingDeg: Float = 0f,
    val linearVelocity: Float = 0f, // m/s
    val angularVelocityDeg: Float = 0f, // deg/s
    val leftWheelRps: Float = 0f,
    val rightWheelRps: Float = 0f,
    val distanceSensorReadingM: Float = 3.0f,
    val hasCollision: Boolean = false,
    val collisionObstacleName: String? = null,
    val batteryPercent: Float = 98.5f,
    val lidarReadings: List<LidarPoint> = emptyList(),
    val activeProgramBlockId: String? = null,
    val logMessages: List<String> = emptyList()
)

data class LidarPoint(
    val angleDeg: Float,
    val distanceM: Float,
    val hitX: Float,
    val hitZ: Float
)
