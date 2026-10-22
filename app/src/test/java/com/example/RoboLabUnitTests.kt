package com.example

import com.example.robolab.data.JsonHelper
import com.example.robolab.model.*
import com.example.robolab.physics.ArticulatedArmKinematics
import com.example.robolab.physics.DifferentialDriveKinematics
import com.example.robolab.physics.RaycastSensorEngine
import com.example.robolab.validator.AssemblyValidator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoboLabUnitTests {

    @Test
    fun testStraightLineKinematics() {
        val omegaL = 10f // rad/s
        val omegaR = 10f // rad/s
        val radius = 0.05f // 5cm
        val wheelBase = 0.30f // 30cm

        val (linearVel, angularVel) = DifferentialDriveKinematics.computeChassisVelocity(
            omegaL, omegaR, radius, wheelBase
        )

        // v = (0.5 + 0.5) / 2 = 0.5 m/s
        assertEquals(0.5f, linearVel, 1e-4f)
        // omega = (0.5 - 0.5) / 0.30 = 0 rad/s
        assertEquals(0.0f, angularVel, 1e-4f)
    }

    @Test
    fun testDifferentialSpinInPlace() {
        val omegaL = -10f // rad/s (reverse)
        val omegaR = 10f  // rad/s (forward)
        val radius = 0.05f
        val wheelBase = 0.25f

        val (linearVel, angularVel) = DifferentialDriveKinematics.computeChassisVelocity(
            omegaL, omegaR, radius, wheelBase
        )

        // Linear velocity should be zero (spin in place)
        assertEquals(0.0f, linearVel, 1e-4f)
        // Angular velocity = (0.5 - (-0.5)) / 0.25 = 1.0 / 0.25 = 4.0 rad/s
        assertEquals(4.0f, angularVel, 1e-4f)
    }

    @Test
    fun testOdometryIntegration() {
        val (x1, z1, heading1) = DifferentialDriveKinematics.integratePose(
            currentX = 0f,
            currentZ = 0f,
            currentHeadingRad = 0f,
            linearVelocity = 1.0f,
            angularVelocityRad = 0f,
            dtSec = 1.0f
        )

        assertEquals(0.0f, x1, 1e-4f)
        assertEquals(1.0f, z1, 1e-4f) // Moved 1m forward along Z
        assertEquals(0.0f, heading1, 1e-4f)
    }

    @Test
    fun testAssemblyValidationReportsMissingWheels() {
        val chassisOnly = listOf(
            RobotComponent(name = "Chassis", type = ComponentType.CHASSIS)
        )
        val issues = AssemblyValidator.validateAssembly(chassisOnly)
        assertTrue(issues.any { it.severity == ValidationSeverity.ERROR && it.title.contains("No Drive Wheels") })
    }

    @Test
    fun testAssemblyValidationPassesForValidDifferentialRobot() {
        val validAssembly = listOf(
            RobotComponent(name = "Chassis", type = ComponentType.CHASSIS),
            RobotComponent(name = "Left Wheel", type = ComponentType.LEFT_WHEEL, wheelRadius = 0.045f),
            RobotComponent(name = "Right Wheel", type = ComponentType.RIGHT_WHEEL, wheelRadius = 0.045f),
            RobotComponent(name = "Caster", type = ComponentType.CASTER_WHEEL),
            RobotComponent(name = "MCU", type = ComponentType.CONTROLLER),
            RobotComponent(name = "Sensor", type = ComponentType.DISTANCE_SENSOR)
        )
        val issues = AssemblyValidator.validateAssembly(validAssembly)
        val errors = issues.filter { it.severity == ValidationSeverity.ERROR }
        assertEquals(0, errors.size)
    }

    @Test
    fun testJsonSerializationRoundTrip() {
        val components = listOf(
            RobotComponent(name = "Chassis Test", type = ComponentType.CHASSIS, massKg = 2.5f),
            RobotComponent(name = "Left Wheel Test", type = ComponentType.LEFT_WHEEL, wheelRadius = 0.05f)
        )
        val json = JsonHelper.serializeComponents(components)
        val restored = JsonHelper.deserializeComponents(json)

        assertEquals(2, restored.size)
        assertEquals("Chassis Test", restored[0].name)
        assertEquals(2.5f, restored[0].massKg, 1e-3f)
        assertEquals(0.05f, restored[1].wheelRadius, 1e-3f)
    }

    @Test
    fun testRaycastDetectionDirectHit() {
        val obstacle = Obstacle(
            name = "Front Wall",
            posX = 0f,
            posZ = 2.0f,
            width = 1.0f,
            length = 0.2f
        )
        // Cast ray from (0, 0) forward along +Z (angle = 0)
        val hitDistance = RaycastSensorEngine.castRay(
            originX = 0f,
            originZ = 0f,
            directionRad = 0f,
            maxRangeMeters = 5.0f,
            obstacles = listOf(obstacle)
        )

        // Obstacle is at Z=2.0 with length=0.2, front face is at Z = 2.0 - 0.1 = 1.9m
        assertEquals(1.9f, hitDistance, 0.05f)
    }

    @Test
    fun testArmForwardKinematics() {
        // Arm straight out along X axis: theta1=0, theta2=0
        val (endX, endY) = ArticulatedArmKinematics.forwardKinematics2D(
            length1 = 0.20f,
            length2 = 0.15f,
            theta1Rad = 0f,
            theta2Rad = 0f
        )

        assertEquals(0.35f, endX, 1e-4f)
        assertEquals(0.0f, endY, 1e-4f)
    }
}
