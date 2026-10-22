package com.example.robolab.data

import com.example.robolab.model.*
import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {

    fun serializeComponents(components: List<RobotComponent>): String {
        val array = JSONArray()
        for (c in components) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type.name)
                put("parentId", c.parentId ?: JSONObject.NULL)
                put("posX", c.posX.toDouble())
                put("posY", c.posY.toDouble())
                put("posZ", c.posZ.toDouble())
                put("rotYaw", c.rotYaw.toDouble())
                put("dimWidth", c.dimWidth.toDouble())
                put("dimLength", c.dimLength.toDouble())
                put("dimHeight", c.dimHeight.toDouble())
                put("massKg", c.massKg.toDouble())
                put("wheelRadius", c.wheelRadius.toDouble())
                put("motorMaxRpm", c.motorMaxRpm.toDouble())
                put("sensorRangeM", c.sensorRangeM.toDouble())
                put("sensorFovDeg", c.sensorFovDeg.toDouble())
                put("jointAngleDeg", c.jointAngleDeg.toDouble())
                put("isEnabled", c.isEnabled)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeComponents(json: String): List<RobotComponent> {
        val list = mutableListOf<RobotComponent>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", ComponentType.CHASSIS.name)
                val type = try { ComponentType.valueOf(typeName) } catch (_: Exception) { ComponentType.CHASSIS }
                list.add(
                    RobotComponent(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Component"),
                        type = type,
                        parentId = if (obj.isNull("parentId")) null else obj.optString("parentId"),
                        posX = obj.optDouble("posX", 0.0).toFloat(),
                        posY = obj.optDouble("posY", 0.0).toFloat(),
                        posZ = obj.optDouble("posZ", 0.0).toFloat(),
                        rotYaw = obj.optDouble("rotYaw", 0.0).toFloat(),
                        dimWidth = obj.optDouble("dimWidth", 0.28).toFloat(),
                        dimLength = obj.optDouble("dimLength", 0.38).toFloat(),
                        dimHeight = obj.optDouble("dimHeight", 0.12).toFloat(),
                        massKg = obj.optDouble("massKg", 1.2).toFloat(),
                        wheelRadius = obj.optDouble("wheelRadius", 0.045).toFloat(),
                        motorMaxRpm = obj.optDouble("motorMaxRpm", 180.0).toFloat(),
                        sensorRangeM = obj.optDouble("sensorRangeM", 3.0).toFloat(),
                        sensorFovDeg = obj.optDouble("sensorFovDeg", 35.0).toFloat(),
                        jointAngleDeg = obj.optDouble("jointAngleDeg", 0.0).toFloat(),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
        } catch (_: Exception) {
            // Return fallback default components if parsing fails
        }
        return list
    }

    fun serializeProgram(blocks: List<ProgramBlock>): String {
        val array = JSONArray()
        for (b in blocks) {
            array.put(serializeBlock(b))
        }
        return array.toString()
    }

    private fun serializeBlock(block: ProgramBlock): JSONObject {
        val obj = JSONObject().apply {
            put("id", block.id)
            put("type", block.type.name)
            put("speed", block.speed.toDouble())
            put("leftSpeed", block.leftSpeed.toDouble())
            put("rightSpeed", block.rightSpeed.toDouble())
            put("durationSec", block.durationSec.toDouble())
            put("angleDeg", block.angleDeg.toDouble())
            put("thresholdM", block.thresholdM.toDouble())
            put("repeatCount", block.repeatCount)
            put("armAngle1Deg", block.armAngle1Deg.toDouble())
            put("armAngle2Deg", block.armAngle2Deg.toDouble())
            put("message", block.message)
            val children = JSONArray()
            for (c in block.childBlocks) children.put(serializeBlock(c))
            put("childBlocks", children)
            val elseArr = JSONArray()
            for (e in block.elseBlocks) elseArr.put(serializeBlock(e))
            put("elseBlocks", elseArr)
        }
        return obj
    }

    fun deserializeProgram(json: String): List<ProgramBlock> {
        val list = mutableListOf<ProgramBlock>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(deserializeBlock(array.getJSONObject(i)))
            }
        } catch (_: Exception) {
            // Fallback
        }
        return list
    }

    private fun deserializeBlock(obj: JSONObject): ProgramBlock {
        val typeName = obj.optString("type", BlockType.START.name)
        val type = try { BlockType.valueOf(typeName) } catch (_: Exception) { BlockType.START }
        val children = mutableListOf<ProgramBlock>()
        val childArr = obj.optJSONArray("childBlocks")
        if (childArr != null) {
            for (i in 0 until childArr.length()) {
                children.add(deserializeBlock(childArr.getJSONObject(i)))
            }
        }
        val elses = mutableListOf<ProgramBlock>()
        val elseArr = obj.optJSONArray("elseBlocks")
        if (elseArr != null) {
            for (i in 0 until elseArr.length()) {
                elses.add(deserializeBlock(elseArr.getJSONObject(i)))
            }
        }
        return ProgramBlock(
            id = obj.optString("id"),
            type = type,
            speed = obj.optDouble("speed", 0.5).toFloat(),
            leftSpeed = obj.optDouble("leftSpeed", 0.5).toFloat(),
            rightSpeed = obj.optDouble("rightSpeed", 0.5).toFloat(),
            durationSec = obj.optDouble("durationSec", 1.5).toFloat(),
            angleDeg = obj.optDouble("angleDeg", 90.0).toFloat(),
            thresholdM = obj.optDouble("thresholdM", 0.40).toFloat(),
            repeatCount = obj.optInt("repeatCount", 3),
            armAngle1Deg = obj.optDouble("armAngle1Deg", 30.0).toFloat(),
            armAngle2Deg = obj.optDouble("armAngle2Deg", -45.0).toFloat(),
            message = obj.optString("message", ""),
            childBlocks = children,
            elseBlocks = elses
        )
    }

    fun serializeObstacles(obstacles: List<Obstacle>): String {
        val array = JSONArray()
        for (obs in obstacles) {
            val obj = JSONObject().apply {
                put("id", obs.id)
                put("name", obs.name)
                put("posX", obs.posX.toDouble())
                put("posZ", obs.posZ.toDouble())
                put("width", obs.width.toDouble())
                put("length", obs.length.toDouble())
                put("height", obs.height.toDouble())
                put("colorHex", obs.colorHex)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeObstacles(json: String): List<Obstacle> {
        val list = mutableListOf<Obstacle>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Obstacle(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Obstacle"),
                        posX = obj.optDouble("posX", 0.0).toFloat(),
                        posZ = obj.optDouble("posZ", 1.5).toFloat(),
                        width = obj.optDouble("width", 0.5).toFloat(),
                        length = obj.optDouble("length", 0.5).toFloat(),
                        height = obj.optDouble("height", 0.35).toFloat(),
                        colorHex = obj.optLong("colorHex", 0xFF374151)
                    )
                )
            }
        } catch (_: Exception) {
            // Return empty
        }
        return list
    }
}
