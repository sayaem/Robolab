package com.example.robolab.physics

import com.example.robolab.model.LidarPoint
import com.example.robolab.model.Obstacle
import kotlin.math.*

object DifferentialDriveKinematics {

    /**
     * Calculates the chassis linear velocity (m/s) and angular velocity (rad/s)
     * from individual wheel angular velocities (rad/s), wheel radius (m), and wheelbase separation (m).
     */
    fun computeChassisVelocity(
        omegaLeftRadPerSec: Float,
        omegaRightRadPerSec: Float,
        wheelRadiusMeters: Float,
        wheelBaseSeparationMeters: Float
    ): Pair<Float, Float> {
        val vLeft = wheelRadiusMeters * omegaLeftRadPerSec
        val vRight = wheelRadiusMeters * omegaRightRadPerSec
        val linearVelocity = (vRight + vLeft) / 2.0f
        val angularVelocityRad = if (wheelBaseSeparationMeters > 0.001f) {
            (vRight - vLeft) / wheelBaseSeparationMeters
        } else {
            0.0f
        }
        return Pair(linearVelocity, angularVelocityRad)
    }

    /**
     * Odometry state integration given dt, current pose (x, z, headingRad),
     * and velocities (v, omega).
     */
    fun integratePose(
        currentX: Float,
        currentZ: Float,
        currentHeadingRad: Float,
        linearVelocity: Float,
        angularVelocityRad: Float,
        dtSec: Float
    ): Triple<Float, Float, Float> {
        val newHeadingRad = normalizeAngle(currentHeadingRad + angularVelocityRad * dtSec)
        // In our coordinate space: Z is forward when heading is 0, or standard 2D plane:
        // Heading 0 rad points along positive Z (or X depending on frame).
        // Let's standardise: Heading 0 points in +Z direction, turning positive rotates towards +X.
        val deltaDist = linearVelocity * dtSec
        val newX = currentX + deltaDist * sin(newHeadingRad)
        val newZ = currentZ + deltaDist * cos(newHeadingRad)
        return Triple(newX, newZ, newHeadingRad)
    }

    fun normalizeAngle(rad: Float): Float {
        var angle = rad
        while (angle > Math.PI.toFloat()) angle -= (2.0f * Math.PI.toFloat())
        while (angle < -Math.PI.toFloat()) angle += (2.0f * Math.PI.toFloat())
        return angle
    }
}

object RaycastSensorEngine {

    /**
     * Casts a single sensor ray from (originX, originZ) in direction (angleRad)
     * against rectangular obstacles and environment bounds.
     * Returns hit distance (clamped to maxRangeMeters).
     */
    fun castRay(
        originX: Float,
        originZ: Float,
        directionRad: Float,
        maxRangeMeters: Float,
        obstacles: List<Obstacle>,
        arenaBoundaryRadius: Float = 5.0f
    ): Float {
        val rayDx = sin(directionRad)
        val rayDz = cos(directionRad)

        var closestDistance = maxRangeMeters

        // 1. Raycast against each obstacle bounding box
        for (obs in obstacles) {
            val halfW = obs.width / 2f
            val halfL = obs.length / 2f
            val minX = obs.posX - halfW
            val maxX = obs.posX + halfW
            val minZ = obs.posZ - halfL
            val maxZ = obs.posZ + halfL

            val dist = intersectRayAABB(originX, originZ, rayDx, rayDz, minX, maxX, minZ, maxZ)
            if (dist != null && dist in 0.001f..closestDistance) {
                closestDistance = dist
            }
        }

        // 2. Raycast against circular perimeter boundary
        val boundaryDist = intersectRayCircle(originX, originZ, rayDx, rayDz, arenaBoundaryRadius)
        if (boundaryDist != null && boundaryDist in 0.001f..closestDistance) {
            closestDistance = boundaryDist
        }

        return closestDistance
    }

    /**
     * Intersects ray with Axis-Aligned Bounding Box (AABB) in 2D.
     */
    private fun intersectRayAABB(
        ox: Float, oz: Float,
        dx: Float, dz: Float,
        minX: Float, maxX: Float,
        minZ: Float, maxZ: Float
    ): Float? {
        var tmin = Float.NEGATIVE_INFINITY
        var tmax = Float.POSITIVE_INFINITY

        if (abs(dx) > 1e-6f) {
            val t1 = (minX - ox) / dx
            val t2 = (maxX - ox) / dx
            tmin = max(tmin, min(t1, t2))
            tmax = min(tmax, max(t1, t2))
        } else if (ox < minX || ox > maxX) {
            return null
        }

        if (abs(dz) > 1e-6f) {
            val t1 = (minZ - oz) / dz
            val t2 = (maxZ - oz) / dz
            tmin = max(tmin, min(t1, t2))
            tmax = min(tmax, max(t1, t2))
        } else if (oz < minZ || oz > maxZ) {
            return null
        }

        return if (tmax >= tmin && tmax >= 0f) {
            if (tmin >= 0f) tmin else tmax
        } else {
            null
        }
    }

    private fun intersectRayCircle(
        ox: Float, oz: Float,
        dx: Float, dz: Float,
        radius: Float
    ): Float? {
        val a = dx * dx + dz * dz
        val b = 2 * (ox * dx + oz * dz)
        val c = ox * ox + oz * oz - radius * radius
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return null
        val sqrtD = sqrt(discriminant)
        val t1 = (-b - sqrtD) / (2 * a)
        val t2 = (-b + sqrtD) / (2 * a)
        return when {
            t1 > 0f -> t1
            t2 > 0f -> t2
            else -> null
        }
    }

    /**
     * Performs a 2D LiDAR sweep with configurable ray count and scan angle range.
     */
    fun performLidarSweep(
        originX: Float,
        originZ: Float,
        baseHeadingRad: Float,
        numRays: Int = 32,
        sweepFovDeg: Float = 360f,
        maxRangeM: Float = 4.0f,
        obstacles: List<Obstacle>
    ): List<LidarPoint> {
        val points = mutableListOf<LidarPoint>()
        val startAngleDeg = -sweepFovDeg / 2.0f
        val stepDeg = sweepFovDeg / numRays.coerceAtLeast(1)

        for (i in 0 until numRays) {
            val relativeAngleDeg = startAngleDeg + i * stepDeg
            val absoluteAngleRad = baseHeadingRad + Math.toRadians(relativeAngleDeg.toDouble()).toFloat()
            val dist = castRay(originX, originZ, absoluteAngleRad, maxRangeM, obstacles)
            val hitX = originX + dist * sin(absoluteAngleRad)
            val hitZ = originZ + dist * cos(absoluteAngleRad)
            points.add(LidarPoint(relativeAngleDeg, dist, hitX, hitZ))
        }
        return points
    }
}

object CollisionEngine {

    data class CollisionResult(
        val hasCollision: Boolean,
        val obstacleName: String? = null,
        val pushoutX: Float = 0f,
        val pushoutZ: Float = 0f
    )

    /**
     * Checks collision between the robot's circular footprint and obstacles/arena boundaries.
     */
    fun checkCollision(
        robotX: Float,
        robotZ: Float,
        robotRadiusM: Float,
        obstacles: List<Obstacle>,
        arenaRadiusM: Float = 5.0f
    ): CollisionResult {
        // 1. Check against obstacles
        for (obs in obstacles) {
            val halfW = obs.width / 2f
            val halfL = obs.length / 2f
            val closestX = robotX.coerceIn(obs.posX - halfW, obs.posX + halfW)
            val closestZ = robotZ.coerceIn(obs.posZ - halfL, obs.posZ + halfL)
            val distX = robotX - closestX
            val distZ = robotZ - closestZ
            val distanceSq = distX * distX + distZ * distZ

            if (distanceSq < robotRadiusM * robotRadiusM) {
                val distance = sqrt(distanceSq).coerceAtLeast(0.001f)
                val penetration = robotRadiusM - distance
                val nx = distX / distance
                val nz = distZ / distance
                return CollisionResult(
                    hasCollision = true,
                    obstacleName = obs.name,
                    pushoutX = nx * penetration,
                    pushoutZ = nz * penetration
                )
            }
        }

        // 2. Check arena perimeter
        val distFromCenter = sqrt(robotX * robotX + robotZ * robotZ)
        if (distFromCenter + robotRadiusM > arenaRadiusM) {
            val penetration = (distFromCenter + robotRadiusM) - arenaRadiusM
            val nx = -robotX / distFromCenter.coerceAtLeast(0.001f)
            val nz = -robotZ / distFromCenter.coerceAtLeast(0.001f)
            return CollisionResult(
                hasCollision = true,
                obstacleName = "Perimeter Barrier",
                pushoutX = nx * penetration,
                pushoutZ = nz * penetration
            )
        }

        return CollisionResult(hasCollision = false)
    }
}

object ArticulatedArmKinematics {

    /**
     * Forward kinematics for a 2-link planar robotic arm.
     * Joint 1 (shoulder): angle theta1 (rad)
     * Joint 2 (elbow): relative angle theta2 (rad)
     * Returns end-effector coordinates (x, y) in the arm plane.
     */
    fun forwardKinematics2D(
        length1: Float,
        length2: Float,
        theta1Rad: Float,
        theta2Rad: Float
    ): Pair<Float, Float> {
        val elbowX = length1 * cos(theta1Rad)
        val elbowY = length1 * sin(theta1Rad)
        val endX = elbowX + length2 * cos(theta1Rad + theta2Rad)
        val endY = elbowY + length2 * sin(theta1Rad + theta2Rad)
        return Pair(endX, endY)
    }

    /**
     * Inverse kinematics for 2-link planar arm to reach target (targetX, targetY).
     * Returns theta1 and theta2 in radians, or null if target is unreachable.
     */
    fun inverseKinematics2D(
        length1: Float,
        length2: Float,
        targetX: Float,
        targetY: Float
    ): Pair<Float, Float>? {
        val distSq = targetX * targetX + targetY * targetY
        val dist = sqrt(distSq)
        if (dist > (length1 + length2) || dist < abs(length1 - length2)) {
            return null // Unreachable
        }

        val cosTheta2 = (distSq - length1 * length1 - length2 * length2) / (2f * length1 * length2)
        val theta2 = acos(cosTheta2.coerceIn(-1.0f, 1.0f))

        val k1 = length1 + length2 * cos(theta2)
        val k2 = length2 * sin(theta2)
        val theta1 = atan2(targetY, targetX) - atan2(k2, k1)

        return Pair(theta1, theta2)
    }
}
