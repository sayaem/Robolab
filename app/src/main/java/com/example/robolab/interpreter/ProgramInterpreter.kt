package com.example.robolab.interpreter

import com.example.robolab.model.BlockType
import com.example.robolab.model.ProgramBlock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

interface RobotMotorController {
    fun setMotorSpeeds(left: Float, right: Float)
    fun getDistanceSensorReading(): Float
    fun getHeadingDeg(): Float
    fun stopMotors()
    fun logMessage(msg: String)
    fun setArmAngles(angle1Deg: Float, angle2Deg: Float)
}

enum class InterpreterStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR
}

class ProgramInterpreter(
    private val motorController: RobotMotorController
) {
    private val _status = MutableStateFlow(InterpreterStatus.IDLE)
    val status: StateFlow<InterpreterStatus> = _status.asStateFlow()

    private val _activeBlockId = MutableStateFlow<String?>(null)
    val activeBlockId: StateFlow<String?> = _activeBlockId.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    private var executionJob: Job? = null

    fun start(blocks: List<ProgramBlock>, scope: CoroutineScope) {
        stop()
        _status.value = InterpreterStatus.RUNNING
        _executionLogs.value = listOf("[SYS] Program execution started.")
        executionJob = scope.launch(Dispatchers.Default) {
            try {
                executeBlockList(blocks)
                motorController.stopMotors()
                _status.value = InterpreterStatus.COMPLETED
                log("[SYS] Program execution finished.")
            } catch (e: CancellationException) {
                motorController.stopMotors()
                _status.value = InterpreterStatus.IDLE
                log("[SYS] Program cancelled by user.")
            } catch (e: Exception) {
                motorController.stopMotors()
                _status.value = InterpreterStatus.ERROR
                log("[ERR] Runtime error: ${e.message}")
            } finally {
                _activeBlockId.value = null
            }
        }
    }

    fun stop() {
        executionJob?.cancel()
        executionJob = null
        motorController.stopMotors()
        _activeBlockId.value = null
        _status.value = InterpreterStatus.IDLE
    }

    private suspend fun executeBlockList(blocks: List<ProgramBlock>) {
        for (block in blocks) {
            if (!coroutineContext.isActive) break
            executeBlock(block)
        }
    }

    private suspend fun executeBlock(block: ProgramBlock) {
        _activeBlockId.value = block.id

        when (block.type) {
            BlockType.START -> {
                log("Executing: Start")
                delay(100)
                executeBlockList(block.childBlocks)
            }
            BlockType.DRIVE_FORWARD -> {
                log("Drive Forward (speed=${block.speed}, duration=${block.durationSec}s)")
                motorController.setMotorSpeeds(block.speed, block.speed)
                delay((block.durationSec * 1000).toLong())
                motorController.stopMotors()
            }
            BlockType.ROTATE -> {
                log("Rotate (${block.angleDeg}°)")
                // For differential drive: left = -speed, right = +speed
                val turnSpeed = block.speed.coerceAtLeast(0.3f)
                val isClockwise = block.angleDeg >= 0
                val left = if (isClockwise) turnSpeed else -turnSpeed
                val right = if (isClockwise) -turnSpeed else turnSpeed
                motorController.setMotorSpeeds(left, right)
                // Approximate time to complete rotation
                val turnDuration = (abs(block.angleDeg) / 90.0f) * 0.9f
                delay((turnDuration * 1000).toLong())
                motorController.stopMotors()
            }
            BlockType.SET_WHEEL_SPEEDS -> {
                log("Set Wheel Speeds: L=${block.leftSpeed}, R=${block.rightSpeed}")
                motorController.setMotorSpeeds(block.leftSpeed, block.rightSpeed)
                if (block.durationSec > 0.05f) {
                    delay((block.durationSec * 1000).toLong())
                    motorController.stopMotors()
                }
            }
            BlockType.WAIT -> {
                log("Wait (${block.durationSec}s)")
                delay((block.durationSec * 1000).toLong())
            }
            BlockType.STOP -> {
                log("Emergency Stop")
                motorController.stopMotors()
                delay(150)
            }
            BlockType.IF_OBSTACLE -> {
                val distance = motorController.getDistanceSensorReading()
                log("Sensor Distance: ${"%.2f".format(distance)}m vs threshold ${block.thresholdM}m")
                if (distance < block.thresholdM) {
                    log("-> Obstacle detected! Executing THEN branch")
                    executeBlockList(block.childBlocks)
                } else if (block.elseBlocks.isNotEmpty()) {
                    log("-> Clear. Executing ELSE branch")
                    executeBlockList(block.elseBlocks)
                }
            }
            BlockType.WHILE_CLEAR -> {
                log("While Clear (distance > ${block.thresholdM}m)")
                var iterations = 0
                while (coroutineContext.isActive && iterations < 50) {
                    val distance = motorController.getDistanceSensorReading()
                    if (distance <= block.thresholdM) {
                        log("Obstacle reached (${"%.2f".format(distance)}m). Exiting loop.")
                        break
                    }
                    executeBlockList(block.childBlocks)
                    iterations++
                    delay(50)
                }
            }
            BlockType.REPEAT -> {
                log("Repeat ${block.repeatCount} times")
                for (i in 1..block.repeatCount) {
                    if (!coroutineContext.isActive) break
                    log("Iteration $i/${block.repeatCount}")
                    executeBlockList(block.childBlocks)
                }
            }
            BlockType.SET_ARM_ANGLES -> {
                log("Set Arm: joint1=${block.armAngle1Deg}°, joint2=${block.armAngle2Deg}°")
                motorController.setArmAngles(block.armAngle1Deg, block.armAngle2Deg)
                delay(300)
            }
            BlockType.LOG_MESSAGE -> {
                log("[USER] ${block.message}")
                delay(100)
            }
        }
    }

    private fun log(text: String) {
        val current = _executionLogs.value.toMutableList()
        current.add(text)
        if (current.size > 100) current.removeAt(0)
        _executionLogs.value = current
        motorController.logMessage(text)
    }
}
