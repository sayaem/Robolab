package com.example.robolab.physics.rigid

import com.example.robolab.renderer3d.Vector3
import kotlin.math.*

data class Quaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f
) {
    fun normalized(): Quaternion {
        val len = sqrt(x * x + y * y + z * z + w * w)
        return if (len > 1e-6f) Quaternion(x / len, y / len, z / len, w / len) else IDENTITY
    }

    operator fun plus(other: Quaternion) = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)

    operator fun times(scale: Float) = Quaternion(x * scale, y * scale, z * scale, w * scale)

    operator fun times(q: Quaternion) = Quaternion(
        w * q.x + x * q.w + y * q.z - z * q.y,
        w * q.y - x * q.z + y * q.w + z * q.x,
        w * q.z + x * q.y - y * q.x + z * q.w,
        w * q.w - x * q.x - y * q.y - z * q.z
    )

    fun conjugate() = Quaternion(-x, -y, -z, w)

    fun rotate(v: Vector3): Vector3 {
        val qv = Quaternion(v.x, v.y, v.z, 0f)
        val res = (this * qv) * this.conjugate()
        return Vector3(res.x, res.y, res.z)
    }

    fun toMatrix3(): Matrix3 {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        return Matrix3(
            1f - 2f * (yy + zz), 2f * (xy - wz), 2f * (xz + wy),
            2f * (xy + wz), 1f - 2f * (xx + zz), 2f * (yz - wx),
            2f * (xz - wy), 2f * (yz + wx), 1f - 2f * (xx + yy)
        )
    }

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        fun fromAxisAngle(axis: Vector3, angleRad: Float): Quaternion {
            val halfAngle = angleRad * 0.5f
            val s = sin(halfAngle)
            val normAxis = axis.normalized()
            return Quaternion(normAxis.x * s, normAxis.y * s, normAxis.z * s, cos(halfAngle)).normalized()
        }

        fun fromEulerDeg(yawDeg: Float, pitchDeg: Float, rollDeg: Float): Quaternion {
            val qYaw = fromAxisAngle(Vector3(0f, 1f, 0f), Math.toRadians(yawDeg.toDouble()).toFloat())
            val qPitch = fromAxisAngle(Vector3(1f, 0f, 0f), Math.toRadians(pitchDeg.toDouble()).toFloat())
            val qRoll = fromAxisAngle(Vector3(0f, 0f, 1f), Math.toRadians(rollDeg.toDouble()).toFloat())
            return (qYaw * qPitch * qRoll).normalized()
        }
    }
}

data class Matrix3(
    val m00: Float, val m01: Float, val m02: Float,
    val m10: Float, val m11: Float, val m12: Float,
    val m20: Float, val m21: Float, val m22: Float
) {
    operator fun times(v: Vector3): Vector3 = Vector3(
        m00 * v.x + m01 * v.y + m02 * v.z,
        m10 * v.x + m11 * v.y + m12 * v.z,
        m20 * v.x + m21 * v.y + m22 * v.z
    )

    operator fun times(m: Matrix3): Matrix3 = Matrix3(
        m00 * m.m00 + m01 * m.m10 + m02 * m.m20,
        m00 * m.m01 + m01 * m.m11 + m02 * m.m21,
        m00 * m.m02 + m01 * m.m12 + m02 * m.m22,

        m10 * m.m00 + m11 * m.m10 + m12 * m.m20,
        m10 * m.m01 + m11 * m.m11 + m12 * m.m21,
        m10 * m.m02 + m11 * m.m12 + m12 * m.m22,

        m20 * m.m00 + m21 * m.m10 + m22 * m.m20,
        m20 * m.m01 + m21 * m.m11 + m22 * m.m21,
        m20 * m.m02 + m21 * m.m12 + m22 * m.m22
    )

    fun transposed(): Matrix3 = Matrix3(
        m00, m10, m20,
        m01, m11, m21,
        m02, m12, m22
    )

    companion object {
        val ZERO = Matrix3(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val IDENTITY = Matrix3(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

        fun diagonal(d0: Float, d1: Float, d2: Float) = Matrix3(
            d0, 0f, 0f,
            0f, d1, 0f,
            0f, 0f, d2
        )
    }
}
