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

    fun serializeAssemblyParts(parts: List<com.example.robolab.model.assembly.AssemblyPart>): String {
        val array = JSONArray()
        for (p in parts) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("shapeType", p.shapeType.name)
                put("dimX", p.dimX.toDouble())
                put("dimY", p.dimY.toDouble())
                put("dimZ", p.dimZ.toDouble())
                put("radius", p.radius.toDouble())
                put("massKg", p.massKg.toDouble())
                put("isStatic", p.isStatic)
                put("friction", p.friction.toDouble())
                put("restitution", p.restitution.toDouble())
                put("colorHex", p.colorHex)
                put("posX", p.posX.toDouble())
                put("posY", p.posY.toDouble())
                put("posZ", p.posZ.toDouble())
                put("rotYawDeg", p.rotYawDeg.toDouble())
                put("rotPitchDeg", p.rotPitchDeg.toDouble())
                put("rotRollDeg", p.rotRollDeg.toDouble())
                put("parentId", p.parentId ?: JSONObject.NULL)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeAssemblyParts(json: String): List<com.example.robolab.model.assembly.AssemblyPart> {
        val list = mutableListOf<com.example.robolab.model.assembly.AssemblyPart>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val shapeName = obj.optString("shapeType", "BOX")
                val shapeType = try { com.example.robolab.model.assembly.ShapeType.valueOf(shapeName) } catch (_: Exception) { com.example.robolab.model.assembly.ShapeType.BOX }
                list.add(
                    com.example.robolab.model.assembly.AssemblyPart(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Part"),
                        shapeType = shapeType,
                        dimX = obj.optDouble("dimX", 0.2).toFloat(),
                        dimY = obj.optDouble("dimY", 0.2).toFloat(),
                        dimZ = obj.optDouble("dimZ", 0.2).toFloat(),
                        radius = obj.optDouble("radius", 0.1).toFloat(),
                        massKg = obj.optDouble("massKg", 1.0).toFloat(),
                        isStatic = obj.optBoolean("isStatic", false),
                        friction = obj.optDouble("friction", 0.6).toFloat(),
                        restitution = obj.optDouble("restitution", 0.2).toFloat(),
                        colorHex = obj.optLong("colorHex", 0xFF00B4D8),
                        posX = obj.optDouble("posX", 0.0).toFloat(),
                        posY = obj.optDouble("posY", 0.5).toFloat(),
                        posZ = obj.optDouble("posZ", 0.0).toFloat(),
                        rotYawDeg = obj.optDouble("rotYawDeg", 0.0).toFloat(),
                        rotPitchDeg = obj.optDouble("rotPitchDeg", 0.0).toFloat(),
                        rotRollDeg = obj.optDouble("rotRollDeg", 0.0).toFloat(),
                        parentId = if (obj.isNull("parentId")) null else obj.optString("parentId")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeAssemblyJoints(joints: List<com.example.robolab.model.assembly.AssemblyJoint>): String {
        val array = JSONArray()
        for (j in joints) {
            val obj = JSONObject().apply {
                put("id", j.id)
                put("name", j.name)
                put("type", j.type.name)
                put("parentPartId", j.parentPartId)
                put("childPartId", j.childPartId)
                put("anchorParentX", j.anchorParentX.toDouble())
                put("anchorParentY", j.anchorParentY.toDouble())
                put("anchorParentZ", j.anchorParentZ.toDouble())
                put("anchorChildX", j.anchorChildX.toDouble())
                put("anchorChildY", j.anchorChildY.toDouble())
                put("anchorChildZ", j.anchorChildZ.toDouble())
                put("axisX", j.axisX.toDouble())
                put("axisY", j.axisY.toDouble())
                put("axisZ", j.axisZ.toDouble())
                put("enableLimits", j.enableLimits)
                put("lowerLimitDeg", j.lowerLimitDeg.toDouble())
                put("upperLimitDeg", j.upperLimitDeg.toDouble())
                put("enableMotor", j.enableMotor)
                put("motorTargetVelocityRadPerSec", j.motorTargetVelocityRadPerSec.toDouble())
                put("motorMaxTorqueNm", j.motorMaxTorqueNm.toDouble())
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeAssemblyJoints(json: String): List<com.example.robolab.model.assembly.AssemblyJoint> {
        val list = mutableListOf<com.example.robolab.model.assembly.AssemblyJoint>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", "REVOLUTE")
                val type = try { com.example.robolab.model.assembly.JointType.valueOf(typeName) } catch (_: Exception) { com.example.robolab.model.assembly.JointType.REVOLUTE }
                list.add(
                    com.example.robolab.model.assembly.AssemblyJoint(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Joint"),
                        type = type,
                        parentPartId = obj.optString("parentPartId"),
                        childPartId = obj.optString("childPartId"),
                        anchorParentX = obj.optDouble("anchorParentX", 0.0).toFloat(),
                        anchorParentY = obj.optDouble("anchorParentY", 0.0).toFloat(),
                        anchorParentZ = obj.optDouble("anchorParentZ", 0.0).toFloat(),
                        anchorChildX = obj.optDouble("anchorChildX", 0.0).toFloat(),
                        anchorChildY = obj.optDouble("anchorChildY", 0.0).toFloat(),
                        anchorChildZ = obj.optDouble("anchorChildZ", 0.0).toFloat(),
                        axisX = obj.optDouble("axisX", 0.0).toFloat(),
                        axisY = obj.optDouble("axisY", 1.0).toFloat(),
                        axisZ = obj.optDouble("axisZ", 0.0).toFloat(),
                        enableLimits = obj.optBoolean("enableLimits", false),
                        lowerLimitDeg = obj.optDouble("lowerLimitDeg", -90.0).toFloat(),
                        upperLimitDeg = obj.optDouble("upperLimitDeg", 90.0).toFloat(),
                        enableMotor = obj.optBoolean("enableMotor", false),
                        motorTargetVelocityRadPerSec = obj.optDouble("motorTargetVelocityRadPerSec", 2.0).toFloat(),
                        motorMaxTorqueNm = obj.optDouble("motorMaxTorqueNm", 15.0).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeRoboLabAssembly(assembly: com.example.robolab.model.assembly.RoboLabAssembly): String {
        val obj = JSONObject()
        obj.put("id", assembly.id)
        obj.put("name", assembly.name)
        obj.put("parts", JSONArray(serializeAssemblyParts(assembly.parts)))
        obj.put("joints", JSONArray(serializeAssemblyJoints(assembly.joints)))
        obj.put("gravityY", assembly.gravityY.toDouble())
        return obj.toString()
    }

    fun deserializeRoboLabAssembly(json: String): com.example.robolab.model.assembly.RoboLabAssembly {
        try {
            if (json.startsWith("{")) {
                val obj = JSONObject(json)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val name = obj.optString("name", "Custom Assembly")
                val partsJson = obj.optJSONArray("parts")?.toString() ?: "[]"
                val jointsJson = obj.optJSONArray("joints")?.toString() ?: "[]"
                val grav = obj.optDouble("gravityY", -9.81).toFloat()
                return com.example.robolab.model.assembly.RoboLabAssembly(
                    id = id,
                    name = name,
                    parts = deserializeAssemblyParts(partsJson),
                    joints = deserializeAssemblyJoints(jointsJson),
                    gravityY = grav
                )
            }
        } catch (_: Exception) {}
        return com.example.robolab.model.assembly.RoboLabAssembly()
    }
}
