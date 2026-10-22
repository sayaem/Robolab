package com.example

import com.example.robolab.data.JsonHelper
import com.example.robolab.model.assembly.*
import com.example.robolab.physics.rigid.*
import com.example.robolab.renderer3d.Vector3
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RigidPhysicsUnitTests {

    @Test
    fun testFreeFallUnderGravity() {
        val world = RigidPhysicsWorld()
        val fallingBox = AssemblyPart(
            id = "part_fall",
            name = "Falling Sphere",
            shapeType = ShapeType.SPHERE,
            dimX = 0.2f, dimY = 0.2f, dimZ = 0.2f,
            posX = 0f, posY = 2.0f, posZ = 0f,
            massKg = 1.0f,
            isStatic = false,
            restitution = 0.5f,
            friction = 0.4f
        )
        world.loadAssembly(listOf(fallingBox), emptyList())

        val body = world.bodies.first { it.id == "part_fall" }
        assertEquals(2.0f, body.position.y, 0.001f)
        assertEquals(0.0f, body.linearVelocity.y, 0.001f)

        // Step for 0.5 seconds (25 steps of 0.02s)
        for (i in 0 until 25) {
            world.step(0.02f)
        }

        // Under g = -9.81 m/s^2, v_y should be approx -9.81 * 0.5 = -4.905 m/s
        assertTrue("Body should accelerate downwards", body.linearVelocity.y < -4.0f)
        // Position y = y0 - 0.5 * g * t^2 = 2.0 - 0.5 * 9.81 * 0.25 = ~0.77 m
        assertTrue("Body should fall towards ground", body.position.y < 1.5f && body.position.y > 0.0f)
    }

    @Test
    fun testGroundCollisionImpulse() {
        val world = RigidPhysicsWorld()
        val droppingBox = AssemblyPart(
            id = "part_drop",
            name = "Dropping Box",
            shapeType = ShapeType.BOX,
            dimX = 0.2f, dimY = 0.2f, dimZ = 0.2f,
            posX = 0f, posY = 0.3f, posZ = 0f,
            massKg = 2.0f,
            isStatic = false,
            restitution = 0.2f,
            friction = 0.5f
        )
        world.loadAssembly(listOf(droppingBox), emptyList())
        val body = world.bodies.first { it.id == "part_drop" }

        // Step for 2.0 seconds (100 steps)
        for (i in 0 until 100) {
            world.step(0.02f)
        }

        // Body bottom face is at posY - 0.1. Ground is at y=0.
        // It must rest on or above ground, not sink down to -inf!
        assertTrue("Body should not sink below ground plane", body.position.y >= 0.08f)
        assertTrue("Vertical speed should be neutralized at rest", Math.abs(body.linearVelocity.y) < 0.2f)
    }

    @Test
    fun testRevoluteJointAnchorConstraint() {
        val world = RigidPhysicsWorld()

        val basePart = AssemblyPart(
            id = "base",
            name = "Anchored Base",
            shapeType = ShapeType.BOX,
            dimX = 0.4f, dimY = 0.4f, dimZ = 0.4f,
            posX = 0f, posY = 1.0f, posZ = 0f,
            massKg = 5.0f,
            isStatic = true
        )

        val armPart = AssemblyPart(
            id = "arm",
            name = "Pendulum Arm",
            shapeType = ShapeType.CYLINDER,
            dimX = 0.1f, dimY = 0.6f, dimZ = 0.1f,
            posX = 0f, posY = 0.6f, posZ = 0f,
            massKg = 1.0f,
            isStatic = false
        )

        // Revolute joint connecting base bottom (0, -0.2, 0) to arm top (0, 0.3, 0)
        val joint = AssemblyJoint(
            id = "joint_hinge",
            name = "Hinge Joint",
            type = JointType.REVOLUTE,
            parentPartId = "base",
            childPartId = "arm",
            anchorParentX = 0f, anchorParentY = -0.2f, anchorParentZ = 0f,
            anchorChildX = 0f, anchorChildY = 0.3f, anchorChildZ = 0f,
            axisX = 1f, axisY = 0f, axisZ = 0f,
            enableLimits = false,
            enableMotor = false
        )

        world.loadAssembly(listOf(basePart, armPart), listOf(joint))

        val bodyBase = world.bodies.first { it.id == "base" }
        val bodyArm = world.bodies.first { it.id == "arm" }

        // Step through simulation
        for (i in 0 until 50) {
            world.step(0.02f)
        }

        // Anchor world points
        val anchorAWorld = bodyBase.position + bodyBase.rotationMatrix * Vector3(0f, -0.2f, 0f)
        val anchorBWorld = bodyArm.position + bodyArm.rotationMatrix * Vector3(0f, 0.3f, 0f)
        val distance = (anchorAWorld - anchorBWorld).length()

        assertTrue("Anchor distance error should remain tiny: $distance m", distance < 0.05f)
    }

    @Test
    fun testRevoluteJointMotorDriven() {
        val world = RigidPhysicsWorld()

        val basePart = AssemblyPart(
            id = "base",
            name = "Anchored Motor Mount",
            shapeType = ShapeType.BOX,
            dimX = 0.4f, dimY = 0.4f, dimZ = 0.4f,
            posX = 0f, posY = 1.0f, posZ = 0f,
            massKg = 10.0f,
            isStatic = true
        )

        val rotorPart = AssemblyPart(
            id = "rotor",
            name = "Rotating Rotor",
            shapeType = ShapeType.CYLINDER,
            dimX = 0.1f, dimY = 0.5f, dimZ = 0.1f,
            posX = 0f, posY = 0.6f, posZ = 0f,
            massKg = 0.5f,
            isStatic = false
        )

        val motorJoint = AssemblyJoint(
            id = "joint_motor",
            name = "Drive Motor",
            type = JointType.REVOLUTE,
            parentPartId = "base",
            childPartId = "rotor",
            anchorParentX = 0f, anchorParentY = -0.2f, anchorParentZ = 0f,
            anchorChildX = 0f, anchorChildY = 0.25f, anchorChildZ = 0f,
            axisX = 1f, axisY = 0f, axisZ = 0f,
            enableMotor = true,
            motorTargetVelocityRadPerSec = 3.14f, // ~3.14 rad/s
            motorMaxTorqueNm = 20.0f
        )

        world.loadAssembly(listOf(basePart, rotorPart), listOf(motorJoint))
        val bodyRotor = world.bodies.first { it.id == "rotor" }

        // Step for 0.4s
        for (i in 0 until 20) {
            world.step(0.02f)
        }

        val jointObj = world.joints.first { it.id == "joint_motor" }
        assertTrue("Motor should generate torque", jointObj.currentMotorTorqueNm != 0.0f)
        assertTrue("Rotor should achieve angular velocity in direction of motor", Math.abs(bodyRotor.angularVelocity.x) > 0.5f)
    }

    @Test
    fun testAssemblySerializationRoundTrip() {
        val part1 = AssemblyPart(
            id = "part_1",
            name = "Chassis Base",
            shapeType = ShapeType.BOX,
            dimX = 0.6f, dimY = 0.2f, dimZ = 0.4f,
            posX = 0f, posY = 0.5f, posZ = 0f,
            massKg = 4.5f,
            isStatic = false,
            friction = 0.65f,
            restitution = 0.15f
        )
        val part2 = AssemblyPart(
            id = "part_2",
            name = "Turret Head",
            shapeType = ShapeType.SPHERE,
            dimX = 0.3f, dimY = 0.3f, dimZ = 0.3f,
            posX = 0f, posY = 0.75f, posZ = 0f,
            massKg = 1.2f,
            isStatic = false
        )
        val joint1 = AssemblyJoint(
            id = "joint_1",
            name = "Turret Yaw Joint",
            type = JointType.REVOLUTE,
            parentPartId = "part_1",
            childPartId = "part_2",
            axisX = 0f, axisY = 1f, axisZ = 0f,
            enableMotor = true,
            motorTargetVelocityRadPerSec = 2.0f,
            motorMaxTorqueNm = 15.0f
        )

        val assembly = RoboLabAssembly(
            id = "assembly_test",
            name = "Test Assembly",
            parts = listOf(part1, part2),
            joints = listOf(joint1)
        )

        val json = JsonHelper.serializeRoboLabAssembly(assembly)
        assertNotNull(json)
        assertTrue(json.contains("Chassis Base"))
        assertTrue(json.contains("Turret Yaw Joint"))

        val restored = JsonHelper.deserializeRoboLabAssembly(json)
        assertNotNull(restored)
        assertEquals("assembly_test", restored.id)
        assertEquals(2, restored.parts.size)
        assertEquals(1, restored.joints.size)
        assertEquals(ShapeType.BOX, restored.parts[0].shapeType)
        assertEquals(ShapeType.SPHERE, restored.parts[1].shapeType)
        assertEquals(JointType.REVOLUTE, restored.joints[0].type)
        assertEquals(2.0f, restored.joints[0].motorTargetVelocityRadPerSec, 0.001f)
    }
}
