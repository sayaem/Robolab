package com.example.robolab.validator

import com.example.robolab.model.ComponentType
import com.example.robolab.model.RobotComponent
import com.example.robolab.model.ValidationIssue
import com.example.robolab.model.ValidationSeverity
import kotlin.math.abs

object AssemblyValidator {

    fun validateAssembly(components: List<RobotComponent>): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // 1. Check for Chassis Frame
        val chassis = components.find { it.type == ComponentType.CHASSIS }
        if (chassis == null) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    title = "Missing Structural Chassis",
                    message = "Every robot requires a main Chassis frame to mount wheels, actuators, and controllers.",
                    recommendation = "Add a Chassis Frame from the Structural component library."
                )
            )
        }

        // 2. Check for Drive Wheels
        val leftWheels = components.filter { it.type == ComponentType.LEFT_WHEEL }
        val rightWheels = components.filter { it.type == ComponentType.RIGHT_WHEEL }

        if (leftWheels.isEmpty() && rightWheels.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    title = "No Drive Wheels Attached",
                    message = "Differential-drive simulation requires at least one left wheel and one right wheel.",
                    recommendation = "Attach both a Left Drive Wheel and a Right Drive Wheel."
                )
            )
        } else if (leftWheels.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    title = "Missing Left Drive Wheel",
                    message = "Chassis has a right wheel but no left drive wheel. Differential kinematics cannot compute forward velocity.",
                    recommendation = "Add a Left Drive Wheel with matched radius."
                )
            )
        } else if (rightWheels.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    title = "Missing Right Drive Wheel",
                    message = "Chassis has a left wheel but no right drive wheel. Differential kinematics cannot compute forward velocity.",
                    recommendation = "Add a Right Drive Wheel with matched radius."
                )
            )
        } else {
            // Check wheel radius mismatch
            val leftRadius = leftWheels.first().wheelRadius
            val rightRadius = rightWheels.first().wheelRadius
            if (abs(leftRadius - rightRadius) > 0.005f) {
                issues.add(
                    ValidationIssue(
                        severity = ValidationSeverity.WARNING,
                        title = "Wheel Radius Mismatch",
                        message = "Left wheel radius (${(leftRadius * 1000).toInt()}mm) differs from right wheel (${(rightRadius * 1000).toInt()}mm). This causes mechanical drift.",
                        recommendation = "Equalize wheel radii in the property inspector for symmetric motion."
                    )
                )
            }
        }

        // 3. Check for Stability Support (Caster or 4WD)
        val hasCaster = components.any { it.type == ComponentType.CASTER_WHEEL }
        val totalDriveWheels = leftWheels.size + rightWheels.size
        if (totalDriveWheels == 2 && !hasCaster) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.WARNING,
                    title = "Two-Wheel Instability Warning",
                    message = "A 2-wheel robot without a passive caster wheel or secondary contact point will pitch forward or backward under torque.",
                    recommendation = "Add a Support Caster wheel at the rear or front of the chassis."
                )
            )
        }

        // 4. Check for MCU Controller
        val hasController = components.any { it.type == ComponentType.CONTROLLER }
        if (!hasController) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.WARNING,
                    title = "Missing Microcontroller",
                    message = "No MCU Controller board is mounted to execute program logic and modulate motor PWM.",
                    recommendation = "Add an MCU Controller from the Power & Logic drawer."
                )
            )
        }

        // 5. Check for Distance Sensor
        val hasDistanceSensor = components.any { it.type == ComponentType.DISTANCE_SENSOR || it.type == ComponentType.LIDAR_2D }
        if (!hasDistanceSensor) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.INFO,
                    title = "No Proximity Sensor Attached",
                    message = "Robot currently lacks an ultrasonic or LiDAR sensor. Obstacle avoidance programs will receive max distance.",
                    recommendation = "Mount an Ultrasonic Sensor or 2D LiDAR scanner on the chassis front."
                )
            )
        }

        // 6. Check Arm Hierarchy
        val armLink1 = components.find { it.type == ComponentType.ARM_LINK_1 }
        val armLink2 = components.find { it.type == ComponentType.ARM_LINK_2 }
        if (armLink2 != null && armLink1 == null) {
            issues.add(
                ValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    title = "Disconnected Forearm Link",
                    message = "Forearm Link requires a parent Shoulder Link in the kinematic chain.",
                    recommendation = "Add a Shoulder Link before attaching Forearm."
                )
            )
        }

        return issues
    }
}
