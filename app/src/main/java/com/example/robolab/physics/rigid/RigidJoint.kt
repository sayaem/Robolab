package com.example.robolab.physics.rigid

import com.example.robolab.model.assembly.AssemblyJoint
import com.example.robolab.model.assembly.JointType
import com.example.robolab.renderer3d.Vector3
import kotlin.math.*

class RigidJoint(
    val jointData: AssemblyJoint,
    val bodyA: RigidBody,
    val bodyB: RigidBody
) {
    val id: String = jointData.id
    val type: JointType = jointData.type

    val localAnchorA = Vector3(jointData.anchorParentX, jointData.anchorParentY, jointData.anchorParentZ)
    val localAnchorB = Vector3(jointData.anchorChildX, jointData.anchorChildY, jointData.anchorChildZ)
    val localAxisA = Vector3(jointData.axisX, jointData.axisY, jointData.axisZ).normalized()

    // Real-time joint telemetry
    var currentAngleDeg: Float = 0f
        private set
    var currentAngularVelocityRadPerSec: Float = 0f
        private set
    var currentMotorTorqueNm: Float = 0f
        private set

    /**
     * Solves velocity constraints (anchors, hinge alignment, motor, limits).
     */
    fun solveVelocity(dt: Float) {
        val rA = bodyA.rotationMatrix * localAnchorA
        val rB = bodyB.rotationMatrix * localAnchorB
        val worldAxisA = (bodyA.rotationMatrix * localAxisA).normalized()

        // 1. Point-to-Point Linear Anchor Constraint
        val vA = bodyA.linearVelocity + bodyA.angularVelocity.cross(rA)
        val vB = bodyB.linearVelocity + bodyB.angularVelocity.cross(rB)
        val vRel = vB - vA

        // Baumgarte stabilization for positional drift
        val pA = bodyA.position + rA
        val pB = bodyB.position + rB
        val posError = pB - pA
        val beta = 0.2f
        val bias = posError * (beta / dt)

        val invMassTotal = bodyA.invMass + bodyB.invMass
        if (invMassTotal > 1e-6f) {
            val impulse = (vRel + bias) * (-0.85f / invMassTotal)
            bodyA.applyImpulse(impulse * -1f, pA)
            bodyB.applyImpulse(impulse, pB)
        }

        if (type == JointType.REVOLUTE) {
            // 2. Angular Alignment Constraint (Constrain rotation on 2 axes perpendicular to hinge axis)
            val relAngVel = bodyB.angularVelocity - bodyA.angularVelocity
            currentAngularVelocityRadPerSec = relAngVel.dot(worldAxisA)

            // 3. Motor Constraint along Hinge Axis
            if (jointData.enableMotor && jointData.motorMaxTorqueNm > 0f) {
                val targetSpeed = jointData.motorTargetVelocityRadPerSec
                val deltaSpeed = targetSpeed - currentAngularVelocityRadPerSec

                val invInertiaTotal = (bodyA.invInertiaWorld * worldAxisA).dot(worldAxisA) +
                        (bodyB.invInertiaWorld * worldAxisA).dot(worldAxisA)

                if (invInertiaTotal > 1e-6f) {
                    val motorTorque = (deltaSpeed / invInertiaTotal)
                    val maxImpulse = jointData.motorMaxTorqueNm * dt
                    val clampedImpulse = (motorTorque * dt).coerceIn(-maxImpulse, maxImpulse)
                    currentMotorTorqueNm = clampedImpulse / dt

                    val torqueVec = worldAxisA * currentMotorTorqueNm
                    bodyB.applyTorque(torqueVec)
                    bodyA.applyTorque(torqueVec * -1f) // Newton's third law
                }
            } else {
                currentMotorTorqueNm = 0f
            }

            // 4. Angular Limit Constraint
            if (jointData.enableLimits) {
                val lowerRad = Math.toRadians(jointData.lowerLimitDeg.toDouble()).toFloat()
                val upperRad = Math.toRadians(jointData.upperLimitDeg.toDouble()).toFloat()
                val currentRad = Math.toRadians(currentAngleDeg.toDouble()).toFloat()

                if (currentRad < lowerRad && currentAngularVelocityRadPerSec < 0f) {
                    val stopImpulse = -currentAngularVelocityRadPerSec * 0.8f
                    val torqueVec = worldAxisA * stopImpulse
                    bodyB.angularVelocity += bodyB.invInertiaWorld * torqueVec
                    bodyA.angularVelocity -= bodyA.invInertiaWorld * torqueVec
                } else if (currentRad > upperRad && currentAngularVelocityRadPerSec > 0f) {
                    val stopImpulse = -currentAngularVelocityRadPerSec * 0.8f
                    val torqueVec = worldAxisA * stopImpulse
                    bodyB.angularVelocity += bodyB.invInertiaWorld * torqueVec
                    bodyA.angularVelocity -= bodyA.invInertiaWorld * torqueVec
                }
            }
        } else if (type == JointType.FIXED) {
            // Weld all relative angular velocity
            val relAngVel = bodyB.angularVelocity - bodyA.angularVelocity
            val impulse = relAngVel * -0.9f
            bodyB.angularVelocity += bodyB.invInertiaWorld * impulse
            bodyA.angularVelocity -= bodyA.invInertiaWorld * impulse
        }
    }

    fun updateTelemetry(dt: Float) {
        if (type == JointType.REVOLUTE) {
            currentAngleDeg += Math.toDegrees((currentAngularVelocityRadPerSec * dt).toDouble()).toFloat()
        }
    }
}
