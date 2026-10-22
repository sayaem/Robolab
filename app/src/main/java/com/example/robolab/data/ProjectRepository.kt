package com.example.robolab.data

import com.example.robolab.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getProject(id: String): ProjectEntity? = dao.getProjectById(id)

    suspend fun saveProject(project: ProjectEntity) = dao.insertProject(project)

    suspend fun deleteProject(id: String) = dao.deleteProjectById(id)

    suspend fun initializeDefaultProjectsIfEmpty() {
        val existing = dao.getAllProjects().first()
        if (existing.isEmpty()) {
            // Seed Project 1: Rover One
            val roverComponents = createRoverOneComponents()
            val roverProgram = createRoverOneProgram()
            val roverObstacles = createDefaultObstacles()

            val p1 = ProjectEntity(
                id = "proj_rover_one",
                name = "Rover One (2WD Differential)",
                description = "Standard two-wheel differential drive robot equipped with ultrasonic proximity sensor and obstacle avoidance program.",
                assemblyJson = JsonHelper.serializeComponents(roverComponents),
                programJson = JsonHelper.serializeProgram(roverProgram),
                obstaclesJson = JsonHelper.serializeObstacles(roverObstacles)
            )
            dao.insertProject(p1)

            // Seed Project 2: Scout LiDAR Explorer
            val scoutComponents = createScoutLidarComponents()
            val scoutProgram = createScoutLidarProgram()
            val scoutObstacles = createMazeObstacles()

            val p2 = ProjectEntity(
                id = "proj_scout_lidar",
                name = "Scout LiDAR Explorer",
                description = "Autonomous mobile scout equipped with a 360-degree 2D LiDAR scanner turret and perimeter navigation.",
                assemblyJson = JsonHelper.serializeComponents(scoutComponents),
                programJson = JsonHelper.serializeProgram(scoutProgram),
                obstaclesJson = JsonHelper.serializeObstacles(scoutObstacles)
            )
            dao.insertProject(p2)

            // Seed Project 3: Articulated Arm Explorer
            val armComponents = createArmExplorerComponents()
            val armProgram = createArmExplorerProgram()

            val p3 = ProjectEntity(
                id = "proj_arm_explorer",
                name = "RoboArm Kinematics Lab",
                description = "Two-link planar robotic arm demonstrating forward and inverse kinematics, joint constraints, and end-effector reach.",
                assemblyJson = JsonHelper.serializeComponents(armComponents),
                programJson = JsonHelper.serializeProgram(armProgram),
                obstaclesJson = JsonHelper.serializeObstacles(emptyList())
            )
            dao.insertProject(p3)
        }
    }

    fun createEmptyProject(name: String, desc: String): ProjectEntity {
        val defaultComponents = createRoverOneComponents()
        val defaultProgram = listOf(
            ProgramBlock(type = BlockType.START, message = "System online"),
            ProgramBlock(type = BlockType.DRIVE_FORWARD, speed = 0.5f, durationSec = 2.0f),
            ProgramBlock(type = BlockType.STOP)
        )
        val defaultObstacles = createDefaultObstacles()
        return ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "New Robot Project" },
            description = desc.ifBlank { "Custom mobile robotics design and simulation" },
            assemblyJson = JsonHelper.serializeComponents(defaultComponents),
            programJson = JsonHelper.serializeProgram(defaultProgram),
            obstaclesJson = JsonHelper.serializeObstacles(defaultObstacles)
        )
    }

    fun duplicateProject(original: ProjectEntity): ProjectEntity {
        return ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            description = original.description,
            assemblyJson = original.assemblyJson,
            programJson = original.programJson,
            obstaclesJson = original.obstaclesJson,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun createRoverOneComponents(): List<RobotComponent> {
            val chassisId = "comp_chassis"
            return listOf(
                RobotComponent(
                    id = chassisId,
                    name = "Aluminum Chassis Base",
                    type = ComponentType.CHASSIS,
                    posX = 0f, posY = 0.04f, posZ = 0f,
                    dimWidth = 0.28f, dimLength = 0.38f, dimHeight = 0.08f,
                    massKg = 1.6f
                ),
                RobotComponent(
                    id = "comp_wheel_left",
                    name = "Left Drive Wheel",
                    type = ComponentType.LEFT_WHEEL,
                    parentId = chassisId,
                    posX = -0.16f, posY = 0.0f, posZ = 0.05f,
                    wheelRadius = 0.045f,
                    motorMaxRpm = 180f
                ),
                RobotComponent(
                    id = "comp_wheel_right",
                    name = "Right Drive Wheel",
                    type = ComponentType.RIGHT_WHEEL,
                    parentId = chassisId,
                    posX = 0.16f, posY = 0.0f, posZ = 0.05f,
                    wheelRadius = 0.045f,
                    motorMaxRpm = 180f
                ),
                RobotComponent(
                    id = "comp_caster",
                    name = "Rear Support Caster",
                    type = ComponentType.CASTER_WHEEL,
                    parentId = chassisId,
                    posX = 0.0f, posY = 0.0f, posZ = -0.14f,
                    wheelRadius = 0.025f
                ),
                RobotComponent(
                    id = "comp_distance_sensor",
                    name = "Front Ultrasonic Sensor",
                    type = ComponentType.DISTANCE_SENSOR,
                    parentId = chassisId,
                    posX = 0.0f, posY = 0.07f, posZ = 0.19f,
                    sensorRangeM = 3.5f,
                    sensorFovDeg = 35f
                ),
                RobotComponent(
                    id = "comp_mcu",
                    name = "RoboCore ARM MCU",
                    type = ComponentType.CONTROLLER,
                    parentId = chassisId,
                    posX = 0.0f, posY = 0.09f, posZ = 0.0f
                ),
                RobotComponent(
                    id = "comp_battery",
                    name = "3S 2200mAh LiPo",
                    type = ComponentType.BATTERY_PACK,
                    parentId = chassisId,
                    posX = 0.0f, posY = 0.05f, posZ = -0.06f
                )
            )
        }

        fun createRoverOneProgram(): List<ProgramBlock> {
            return listOf(
                ProgramBlock(
                    id = "block_start",
                    type = BlockType.START,
                    childBlocks = listOf(
                        ProgramBlock(
                            id = "block_loop",
                            type = BlockType.WHILE_CLEAR,
                            thresholdM = 0.45f,
                            childBlocks = listOf(
                                ProgramBlock(
                                    id = "block_drive",
                                    type = BlockType.DRIVE_FORWARD,
                                    speed = 0.6f,
                                    durationSec = 0.8f
                                )
                            )
                        ),
                        ProgramBlock(
                            id = "block_stop",
                            type = BlockType.STOP
                        ),
                        ProgramBlock(
                            id = "block_rotate",
                            type = BlockType.ROTATE,
                            angleDeg = 90f,
                            speed = 0.5f
                        ),
                        ProgramBlock(
                            id = "block_log",
                            type = BlockType.LOG_MESSAGE,
                            message = "Obstacle cleared. Continuing autonomous patrol."
                        )
                    )
                )
            )
        }

        fun createScoutLidarComponents(): List<RobotComponent> {
            val chassisId = "scout_chassis"
            return listOf(
                RobotComponent(
                    id = chassisId,
                    name = "Heavy Scout Chassis",
                    type = ComponentType.CHASSIS,
                    posX = 0f, posY = 0.05f, posZ = 0f,
                    dimWidth = 0.32f, dimLength = 0.42f, dimHeight = 0.10f,
                    massKg = 2.4f
                ),
                RobotComponent(
                    id = "scout_wheel_l",
                    name = "Left Track Wheel",
                    type = ComponentType.LEFT_WHEEL,
                    parentId = chassisId,
                    posX = -0.18f, posY = 0.0f, posZ = 0f,
                    wheelRadius = 0.055f
                ),
                RobotComponent(
                    id = "scout_wheel_r",
                    name = "Right Track Wheel",
                    type = ComponentType.RIGHT_WHEEL,
                    parentId = chassisId,
                    posX = 0.18f, posY = 0.0f, posZ = 0f,
                    wheelRadius = 0.055f
                ),
                RobotComponent(
                    id = "scout_caster",
                    name = "Stabilizer Caster",
                    type = ComponentType.CASTER_WHEEL,
                    parentId = chassisId,
                    posX = 0f, posY = 0f, posZ = -0.16f,
                    wheelRadius = 0.03f
                ),
                RobotComponent(
                    id = "scout_lidar",
                    name = "360° LiDAR Scanner Turret",
                    type = ComponentType.LIDAR_2D,
                    parentId = chassisId,
                    posX = 0f, posY = 0.12f, posZ = 0.05f,
                    sensorRangeM = 5.0f,
                    sensorFovDeg = 360f
                ),
                RobotComponent(
                    id = "scout_imu",
                    name = "High Precision IMU",
                    type = ComponentType.IMU_SENSOR,
                    parentId = chassisId,
                    posX = 0f, posY = 0.07f, posZ = 0f
                ),
                RobotComponent(
                    id = "scout_ctrl",
                    name = "Dual MCU Pilot",
                    type = ComponentType.CONTROLLER,
                    parentId = chassisId,
                    posX = 0f, posY = 0.11f, posZ = -0.08f
                )
            )
        }

        fun createScoutLidarProgram(): List<ProgramBlock> {
            return listOf(
                ProgramBlock(
                    id = "scout_start",
                    type = BlockType.START,
                    childBlocks = listOf(
                        ProgramBlock(
                            id = "scout_repeat",
                            type = BlockType.REPEAT,
                            repeatCount = 4,
                            childBlocks = listOf(
                                ProgramBlock(
                                    id = "scout_fwd",
                                    type = BlockType.DRIVE_FORWARD,
                                    speed = 0.5f,
                                    durationSec = 1.2f
                                ),
                                ProgramBlock(
                                    id = "scout_rot",
                                    type = BlockType.ROTATE,
                                    angleDeg = 90f,
                                    speed = 0.5f
                                ),
                                ProgramBlock(
                                    id = "scout_log",
                                    type = BlockType.LOG_MESSAGE,
                                    message = "Completed perimeter leg scan."
                                )
                            )
                        ),
                        ProgramBlock(id = "scout_stop", type = BlockType.STOP)
                    )
                )
            )
        }

        fun createArmExplorerComponents(): List<RobotComponent> {
            val chassisId = "arm_chassis"
            return listOf(
                RobotComponent(
                    id = chassisId,
                    name = "Robotic Base Platform",
                    type = ComponentType.CHASSIS,
                    posX = 0f, posY = 0.04f, posZ = 0f,
                    dimWidth = 0.28f, dimLength = 0.32f, dimHeight = 0.08f
                ),
                RobotComponent(
                    id = "arm_base",
                    name = "Revolute Base Joint",
                    type = ComponentType.ARM_BASE,
                    parentId = chassisId,
                    posX = 0f, posY = 0.08f, posZ = 0.05f
                ),
                RobotComponent(
                    id = "arm_shoulder",
                    name = "Shoulder Link (L1=160mm)",
                    type = ComponentType.ARM_LINK_1,
                    parentId = "arm_base",
                    posX = 0f, posY = 0.12f, posZ = 0.05f,
                    jointAngleDeg = 45f
                ),
                RobotComponent(
                    id = "arm_forearm",
                    name = "Forearm Link (L2=140mm)",
                    type = ComponentType.ARM_LINK_2,
                    parentId = "arm_shoulder",
                    posX = 0f, posY = 0.24f, posZ = 0.08f,
                    jointAngleDeg = -30f
                ),
                RobotComponent(
                    id = "arm_mcu",
                    name = "Servo Motion Controller",
                    type = ComponentType.CONTROLLER,
                    parentId = chassisId,
                    posX = 0f, posY = 0.08f, posZ = -0.06f
                )
            )
        }

        fun createArmExplorerProgram(): List<ProgramBlock> {
            return listOf(
                ProgramBlock(
                    id = "arm_start",
                    type = BlockType.START,
                    childBlocks = listOf(
                        ProgramBlock(
                            id = "arm_pose1",
                            type = BlockType.SET_ARM_ANGLES,
                            armAngle1Deg = 30f,
                            armAngle2Deg = 45f
                        ),
                        ProgramBlock(id = "arm_wait1", type = BlockType.WAIT, durationSec = 1.0f),
                        ProgramBlock(
                            id = "arm_pose2",
                            type = BlockType.SET_ARM_ANGLES,
                            armAngle1Deg = 60f,
                            armAngle2Deg = -45f
                        ),
                        ProgramBlock(
                            id = "arm_log",
                            type = BlockType.LOG_MESSAGE,
                            message = "End-effector trajectory verified."
                        )
                    )
                )
            )
        }

        fun createDefaultObstacles(): List<Obstacle> {
            return listOf(
                Obstacle(id = "obs_1", name = "Concrete Barrier Alpha", posX = 0.0f, posZ = 1.8f, width = 0.6f, length = 0.6f, colorHex = 0xFF475569),
                Obstacle(id = "obs_2", name = "Test Pillar Bravo", posX = -1.2f, posZ = 2.4f, width = 0.4f, length = 0.4f, colorHex = 0xFF334155),
                Obstacle(id = "obs_3", name = "Test Pillar Charlie", posX = 1.3f, posZ = 2.2f, width = 0.45f, length = 0.45f, colorHex = 0xFF334155)
            )
        }

        fun createMazeObstacles(): List<Obstacle> {
            return listOf(
                Obstacle(id = "wall_1", name = "North Wall", posX = 0f, posZ = 2.8f, width = 3.5f, length = 0.3f, colorHex = 0xFF1E293B),
                Obstacle(id = "wall_2", name = "South Wall", posX = 0f, posZ = -2.8f, width = 3.5f, length = 0.3f, colorHex = 0xFF1E293B),
                Obstacle(id = "wall_3", name = "East Wall", posX = 2.0f, posZ = 0f, width = 0.3f, length = 4.0f, colorHex = 0xFF1E293B),
                Obstacle(id = "wall_4", name = "West Wall", posX = -2.0f, posZ = 0f, width = 0.3f, length = 4.0f, colorHex = 0xFF1E293B),
                Obstacle(id = "core_obs", name = "Center Block", posX = 0.3f, posZ = 0.8f, width = 0.7f, length = 0.7f, colorHex = 0xFF475569)
            )
        }
    }
}
