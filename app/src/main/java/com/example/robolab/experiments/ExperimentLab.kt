package com.example.robolab.experiments

import com.example.robolab.model.BlockType
import com.example.robolab.model.ProgramBlock

data class RoboticsExperiment(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val objective: String,
    val engineeringTheory: String,
    val successCondition: String,
    val defaultBlocks: List<ProgramBlock>
)

object ExperimentCatalog {
    val experiments = listOf(
        RoboticsExperiment(
            id = "exp_straight_line",
            title = "1. Straight-Line Kinematics",
            category = "Kinematics",
            difficulty = "Beginner",
            objective = "Command both wheels at identical angular velocity and observe symmetric linear translation without rotational drift.",
            engineeringTheory = "In differential drive kinematics, when v_L = v_R, angular velocity omega = (v_R - v_L) / b = 0. The robot executes pure forward translation at velocity v = (v_R + v_L) / 2.",
            successCondition = "Robot travels at least 1.5 meters forward with heading deviation < 2.0 degrees.",
            defaultBlocks = listOf(
                ProgramBlock(type = BlockType.START),
                ProgramBlock(type = BlockType.DRIVE_FORWARD, speed = 0.6f, durationSec = 2.0f),
                ProgramBlock(type = BlockType.STOP),
                ProgramBlock(type = BlockType.LOG_MESSAGE, message = "Straight line run complete.")
            )
        ),
        RoboticsExperiment(
            id = "exp_controlled_turn",
            title = "2. Differential Spin & Turn",
            category = "Kinematics",
            difficulty = "Beginner",
            objective = "Execute an exact 90-degree in-place turn by applying equal and opposite wheel velocities (v_L = -v_R).",
            engineeringTheory = "When wheels rotate in opposite directions with identical speed, linear velocity v = (v_R + v_L)/2 = 0, while angular velocity omega = (v_R - (-v_R))/b = 2*v_R / b is maximized around the robot's midpoint.",
            successCondition = "Robot turns 90 degrees (+/- 5 deg) without significant center displacement.",
            defaultBlocks = listOf(
                ProgramBlock(type = BlockType.START),
                ProgramBlock(type = BlockType.ROTATE, angleDeg = 90f, speed = 0.5f),
                ProgramBlock(type = BlockType.STOP),
                ProgramBlock(type = BlockType.LOG_MESSAGE, message = "Completed 90-degree pivot.")
            )
        ),
        RoboticsExperiment(
            id = "exp_obstacle_stop",
            title = "3. Proximity Safety Stop",
            category = "Sensors & Safety",
            difficulty = "Intermediate",
            objective = "Drive toward a solid obstacle and trigger an emergency stop when the ultrasonic ray detects distance < 0.40m.",
            engineeringTheory = "Ultrasonic sensors emit acoustic pulse packets and measure Time-Of-Flight (TOF): distance = (time * speedOfSound) / 2. Real-time safety loops poll TOF sensors to prevent kinetic impact.",
            successCondition = "Robot halts within 0.20m to 0.45m of the barrier without colliding.",
            defaultBlocks = listOf(
                ProgramBlock(type = BlockType.START),
                ProgramBlock(
                    type = BlockType.WHILE_CLEAR,
                    thresholdM = 0.40f,
                    childBlocks = listOf(
                        ProgramBlock(type = BlockType.DRIVE_FORWARD, speed = 0.5f, durationSec = 0.5f)
                    )
                ),
                ProgramBlock(type = BlockType.STOP),
                ProgramBlock(type = BlockType.LOG_MESSAGE, message = "Emergency safety stop engaged.")
            )
        ),
        RoboticsExperiment(
            id = "exp_maze_escape",
            title = "4. Autonomous Obstacle Avoidance",
            category = "Autonomous Systems",
            difficulty = "Intermediate",
            objective = "Program a state machine: cruise forward while path is clear; upon encountering a barrier, halt, pivot 90 degrees, and resume navigation.",
            engineeringTheory = "Reactive navigation couples sensor telemetry directly to motor commands via condition branches, forming the basis of bug algorithms and early autonomous rovers.",
            successCondition = "Robot avoids at least 2 consecutive obstacles and covers > 3.0 meters of arena path.",
            defaultBlocks = listOf(
                ProgramBlock(type = BlockType.START),
                ProgramBlock(
                    type = BlockType.REPEAT,
                    repeatCount = 3,
                    childBlocks = listOf(
                        ProgramBlock(
                            type = BlockType.WHILE_CLEAR,
                            thresholdM = 0.45f,
                            childBlocks = listOf(
                                ProgramBlock(type = BlockType.DRIVE_FORWARD, speed = 0.6f, durationSec = 0.8f)
                            )
                        ),
                        ProgramBlock(type = BlockType.STOP),
                        ProgramBlock(type = BlockType.ROTATE, angleDeg = 90f, speed = 0.5f),
                        ProgramBlock(type = BlockType.LOG_MESSAGE, message = "Avoided barrier, resuming course.")
                    )
                )
            )
        ),
        RoboticsExperiment(
            id = "exp_arm_kinematics",
            title = "5. Articulated Arm Forward Kinematics",
            category = "Manipulators",
            difficulty = "Advanced",
            objective = "Set shoulder and elbow joint angles to position the end-effector at specific Cartesian coordinates (X, Y).",
            engineeringTheory = "Forward kinematics maps joint angles (theta_1, theta_2) to Cartesian coordinates using trigonometry: x = L1*cos(theta_1) + L2*cos(theta_1+theta_2), y = L1*sin(theta_1) + L2*sin(theta_1+theta_2).",
            successCondition = "End-effector reaches the target quadrant with valid joint constraints.",
            defaultBlocks = listOf(
                ProgramBlock(type = BlockType.START),
                ProgramBlock(type = BlockType.SET_ARM_ANGLES, armAngle1Deg = 45f, armAngle2Deg = 30f),
                ProgramBlock(type = BlockType.WAIT, durationSec = 1.0f),
                ProgramBlock(type = BlockType.SET_ARM_ANGLES, armAngle1Deg = 60f, armAngle2Deg = -45f),
                ProgramBlock(type = BlockType.LOG_MESSAGE, message = "Joint sequence executed successfully.")
            )
        )
    )
}
