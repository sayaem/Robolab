package com.example.robolab.physics.rigid

import com.example.robolab.model.assembly.RoboLabAssembly
import com.example.robolab.renderer3d.Vector3
import kotlin.math.abs
import kotlin.math.max

data class ContactPoint(
    val body: RigidBody,
    val worldPoint: Vector3,
    val normal: Vector3, // points out of the ground / obstacle into the body
    val penetration: Float,
    val friction: Float,
    val restitution: Float
)

class RigidPhysicsWorld(
    val gravity: Vector3 = Vector3(0f, -9.81f, 0f)
) {
    val bodies = mutableListOf<RigidBody>()
    val joints = mutableListOf<RigidJoint>()

    fun loadAssembly(assembly: RoboLabAssembly) {
        bodies.clear()
        joints.clear()

        val bodyMap = mutableMapOf<String, RigidBody>()
        for (part in assembly.parts) {
            val body = RigidBody(part)
            bodies.add(body)
            bodyMap[part.id] = body
        }

        for (jointData in assembly.joints) {
            val parentBody = bodyMap[jointData.parentPartId]
            val childBody = bodyMap[jointData.childPartId]
            if (parentBody != null && childBody != null) {
                joints.add(RigidJoint(jointData, parentBody, childBody))
            }
        }
    }

    fun loadAssembly(parts: List<com.example.robolab.model.assembly.AssemblyPart>, jointsList: List<com.example.robolab.model.assembly.AssemblyJoint> = emptyList()) {
        loadAssembly(RoboLabAssembly(parts = parts, joints = jointsList))
    }

    fun step(dt: Float) {
        if (dt <= 0f) return

        // 1. Apply gravity and external forces to dynamic bodies
        for (body in bodies) {
            if (!body.isStatic) {
                body.applyForce(gravity * body.mass)
                // Linear velocity integration prediction
                body.linearVelocity += (body.forceAccumulator * body.invMass) * dt
                // Angular velocity integration prediction
                body.angularVelocity += (body.invInertiaWorld * body.torqueAccumulator) * dt

                // Damping to prevent floating precision blowup
                body.linearVelocity = body.linearVelocity * 0.998f
                body.angularVelocity = body.angularVelocity * 0.995f
            }
            body.forceAccumulator = Vector3(0f, 0f, 0f)
            body.torqueAccumulator = Vector3(0f, 0f, 0f)
        }

        // 2. Generate Ground Contact Manifold (Ground at Y = 0)
        val contacts = generateGroundContacts()

        // 3. Sequential Impulse Constraint Solver (10 iterations)
        val solverIterations = 10
        for (iter in 0 until solverIterations) {
            // Solve Joints & Motors
            for (joint in joints) {
                joint.solveVelocity(dt)
            }

            // Solve Ground Contacts & Friction
            for (contact in contacts) {
                solveContact(contact, dt)
            }
        }

        // 4. Position & Orientation Integration
        for (body in bodies) {
            if (!body.isStatic) {
                // Integrate Position
                body.position += body.linearVelocity * dt

                // Integrate Orientation Quaternion: q += 0.5 * dt * (w * q)
                val w = body.angularVelocity
                val qOmega = Quaternion(w.x, w.y, w.z, 0f)
                val dq = (qOmega * body.orientation) * (0.5f * dt)
                body.orientation = (body.orientation + dq).normalized()

                // Update transformed inertia tensor and rotation matrix
                body.updateWorldInertia()
            }
        }

        // 5. Update Joint telemetry (angles)
        for (joint in joints) {
            joint.updateTelemetry(dt)
        }
    }

    private fun generateGroundContacts(): List<ContactPoint> {
        val contacts = mutableListOf<ContactPoint>()
        val groundNormal = Vector3(0f, 1f, 0f)

        for (body in bodies) {
            if (body.isStatic) continue
            val samplePoints = body.getWorldSamplePoints()
            for (p in samplePoints) {
                if (p.y < 0.005f) {
                    val penetration = 0.005f - p.y
                    contacts.add(
                        ContactPoint(
                            body = body,
                            worldPoint = p,
                            normal = groundNormal,
                            penetration = penetration,
                            friction = body.part.friction,
                            restitution = body.part.restitution
                        )
                    )
                }
            }
        }
        return contacts
    }

    private fun solveContact(contact: ContactPoint, dt: Float) {
        val body = contact.body
        val n = contact.normal
        val r = contact.worldPoint - body.position
        val vAtPoint = body.getVelocityAtWorldPoint(contact.worldPoint)
        val vNormal = vAtPoint.dot(n)

        // Baumgarte position penetration resolution
        val beta = 0.25f
        val bias = (beta / dt) * contact.penetration.coerceAtLeast(0f)

        if (vNormal < 0.01f || contact.penetration > 0f) {
            val rCrossN = r.cross(n)
            val invMassNormal = body.invMass + (body.invInertiaWorld * rCrossN).dot(rCrossN)

            if (invMassNormal > 1e-6f) {
                val impulseMag = (-vNormal + bias) / invMassNormal
                val clampedImpulse = max(0f, impulseMag)
                val normalImpulse = n * clampedImpulse
                body.applyImpulse(normalImpulse, contact.worldPoint)

                // Friction constraint along tangent
                val vTangent = vAtPoint - (n * vNormal)
                val tangentSpeed = vTangent.length()
                if (tangentSpeed > 1e-4f) {
                    val t = (vTangent * (-1f / tangentSpeed)).normalized()
                    val rCrossT = r.cross(t)
                    val invMassTangent = body.invMass + (body.invInertiaWorld * rCrossT).dot(rCrossT)
                    if (invMassTangent > 1e-6f) {
                        val maxFriction = clampedImpulse * contact.friction
                        val frictionImpulseMag = (tangentSpeed / invMassTangent).coerceIn(-maxFriction, maxFriction)
                        body.applyImpulse(t * frictionImpulseMag, contact.worldPoint)
                    }
                }
            }
        }
    }
}
