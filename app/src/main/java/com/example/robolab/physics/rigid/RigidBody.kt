package com.example.robolab.physics.rigid

import com.example.robolab.model.assembly.AssemblyPart
import com.example.robolab.model.assembly.ShapeType
import com.example.robolab.renderer3d.Vector3
import kotlin.math.*

class RigidBody(
    val part: AssemblyPart
) {
    val id: String = part.id
    val isStatic: Boolean = part.isStatic
    val mass: Float = if (isStatic) Float.POSITIVE_INFINITY else part.massKg.coerceAtLeast(0.01f)
    val invMass: Float = if (isStatic) 0f else 1f / mass

    var position: Vector3 = Vector3(part.posX, part.posY, part.posZ)
    var orientation: Quaternion = Quaternion.fromEulerDeg(part.rotYawDeg, part.rotPitchDeg, part.rotRollDeg)
    var rotationMatrix: Matrix3 = orientation.toMatrix3()

    var linearVelocity: Vector3 = Vector3(0f, 0f, 0f)
    var angularVelocity: Vector3 = Vector3(0f, 0f, 0f)

    var forceAccumulator: Vector3 = Vector3(0f, 0f, 0f)
    var torqueAccumulator: Vector3 = Vector3(0f, 0f, 0f)

    private val invInertiaBody: Matrix3 = computeBodyInertiaInverse()
    var invInertiaWorld: Matrix3 = computeWorldInertiaInverse()

    fun updateWorldInertia() {
        rotationMatrix = orientation.toMatrix3()
        invInertiaWorld = computeWorldInertiaInverse()
    }

    private fun computeWorldInertiaInverse(): Matrix3 {
        if (isStatic) return Matrix3.ZERO
        return rotationMatrix * invInertiaBody * rotationMatrix.transposed()
    }

    private fun computeBodyInertiaInverse(): Matrix3 {
        if (isStatic) return Matrix3.ZERO
        val m = mass

        val (ixx, iyy, izz) = when (part.shapeType) {
            ShapeType.BOX -> {
                val dx = part.dimX
                val dy = part.dimY
                val dz = part.dimZ
                Triple(
                    (m / 12f) * (dy * dy + dz * dz),
                    (m / 12f) * (dx * dx + dz * dz),
                    (m / 12f) * (dx * dx + dy * dy)
                )
            }
            ShapeType.CYLINDER -> {
                val r = part.radius
                val h = part.dimY
                Triple(
                    (m / 12f) * (3f * r * r + h * h),
                    (0.5f * m * r * r),
                    (m / 12f) * (3f * r * r + h * h)
                )
            }
            ShapeType.SPHERE -> {
                val r = part.radius
                val i = (2f / 5f) * m * r * r
                Triple(i, i, i)
            }
            ShapeType.CAPSULE -> {
                val r = part.radius
                val h = part.dimY
                val iLateral = (m / 12f) * (3f * r * r + h * h) + (0.4f * m * r * r)
                val iLongitudinal = 0.5f * m * r * r
                Triple(iLateral, iLongitudinal, iLateral)
            }
        }

        return Matrix3.diagonal(
            if (ixx > 1e-6f) 1f / ixx else 0f,
            if (iyy > 1e-6f) 1f / iyy else 0f,
            if (izz > 1e-6f) 1f / izz else 0f
        )
    }

    fun applyForce(f: Vector3) {
        if (!isStatic) forceAccumulator += f
    }

    fun applyTorque(t: Vector3) {
        if (!isStatic) torqueAccumulator += t
    }

    fun applyImpulse(impulse: Vector3, contactPointWorld: Vector3) {
        if (isStatic) return
        linearVelocity += impulse * invMass
        val r = contactPointWorld - position
        val angularImpulse = r.cross(impulse)
        angularVelocity += invInertiaWorld * angularImpulse
    }

    fun getVelocityAtWorldPoint(pointWorld: Vector3): Vector3 {
        val r = pointWorld - position
        return linearVelocity + angularVelocity.cross(r)
    }

    /**
     * Returns characteristic boundary sample points in world space for ground/collision contact testing.
     */
    fun getWorldSamplePoints(): List<Vector3> {
        val points = mutableListOf<Vector3>()
        when (part.shapeType) {
            ShapeType.BOX -> {
                val hx = part.dimX * 0.5f
                val hy = part.dimY * 0.5f
                val hz = part.dimZ * 0.5f
                val localCorners = listOf(
                    Vector3(-hx, -hy, -hz), Vector3(hx, -hy, -hz),
                    Vector3(-hx, hy, -hz), Vector3(hx, hy, -hz),
                    Vector3(-hx, -hy, hz), Vector3(hx, -hy, hz),
                    Vector3(-hx, hy, hz), Vector3(hx, hy, hz)
                )
                for (lc in localCorners) {
                    points.add(position + rotationMatrix * lc)
                }
            }
            ShapeType.CYLINDER -> {
                val r = part.radius
                val hy = part.dimY * 0.5f
                val segments = 8
                for (s in 0 until segments) {
                    val theta = (s * 2.0 * Math.PI / segments).toFloat()
                    val cx = r * cos(theta)
                    val cz = r * sin(theta)
                    points.add(position + rotationMatrix * Vector3(cx, -hy, cz))
                    points.add(position + rotationMatrix * Vector3(cx, hy, cz))
                }
            }
            ShapeType.SPHERE -> {
                val r = part.radius
                // Lowest point
                points.add(position - Vector3(0f, r, 0f))
            }
            ShapeType.CAPSULE -> {
                val r = part.radius
                val hy = part.dimY * 0.5f
                points.add(position + rotationMatrix * Vector3(0f, -hy - r, 0f))
                points.add(position + rotationMatrix * Vector3(0f, hy + r, 0f))
            }
        }
        return points
    }
}
