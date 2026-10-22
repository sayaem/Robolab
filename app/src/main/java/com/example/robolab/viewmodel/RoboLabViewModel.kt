package com.example.robolab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.robolab.data.*
import com.example.robolab.experiments.ExperimentCatalog
import com.example.robolab.experiments.RoboticsExperiment
import com.example.robolab.interpreter.InterpreterStatus
import com.example.robolab.interpreter.ProgramInterpreter
import com.example.robolab.interpreter.RobotMotorController
import com.example.robolab.model.*
import com.example.robolab.physics.CollisionEngine
import com.example.robolab.physics.DifferentialDriveKinematics
import com.example.robolab.physics.RaycastSensorEngine
import com.example.robolab.renderer3d.Camera3D
import com.example.robolab.validator.AssemblyValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.PI
import kotlin.math.abs

enum class ActiveScreen {
    HOME,
    WORKSHOP,
    SIMULATOR,
    PROGRAMMING,
    PROJECTS,
    EXPERIMENTS
}

class RoboLabViewModel(application: Application) : AndroidViewModel(application), RobotMotorController {

    private val db = RoboLabDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())

    // All persisted projects
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation state
    private val _currentScreen = MutableStateFlow(ActiveScreen.HOME)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    // Active project state
    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _activeProjectName = MutableStateFlow("Rover One (2WD Differential)")
    val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

    private val _components = MutableStateFlow<List<RobotComponent>>(emptyList())
    val components: StateFlow<List<RobotComponent>> = _components.asStateFlow()

    private val _programBlocks = MutableStateFlow<List<ProgramBlock>>(emptyList())
    val programBlocks: StateFlow<List<ProgramBlock>> = _programBlocks.asStateFlow()

    private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
    val obstacles: StateFlow<List<Obstacle>> = _obstacles.asStateFlow()

    // Workshop UI state
    private val _selectedComponentId = MutableStateFlow<String?>(null)
    val selectedComponentId: StateFlow<String?> = _selectedComponentId.asStateFlow()

    val validationIssues: StateFlow<List<ValidationIssue>> = _components
        .map { AssemblyValidator.validateAssembly(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3D Camera
    val camera = Camera3D()

    // Graphics profile
    private val _graphicsProfile = MutableStateFlow(GraphicsProfile.BALANCED)
    val graphicsProfile: StateFlow<GraphicsProfile> = _graphicsProfile.asStateFlow()

    // Simulation state
    private val _isSimRunning = MutableStateFlow(false)
    val isSimRunning: StateFlow<Boolean> = _isSimRunning.asStateFlow()

    private val _isSimPaused = MutableStateFlow(false)
    val isSimPaused: StateFlow<Boolean> = _isSimPaused.asStateFlow()

    private val _simSpeedMultiplier = MutableStateFlow(1f)
    val simSpeedMultiplier: StateFlow<Float> = _simSpeedMultiplier.asStateFlow()

    // Robot physical state in simulation arena
    private var robotX = 0f
    private var robotZ = 0f
    private var robotHeadingRad = 0f
    private var motorLeftSpeed = 0f
    private var motorRightSpeed = 0f
    private var armAngle1 = 0f
    private var armAngle2 = 0f

    private val _telemetry = MutableStateFlow(SimulationTelemetry())
    val telemetry: StateFlow<SimulationTelemetry> = _telemetry.asStateFlow()

    private val _trajectoryHistory = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val trajectoryHistory: StateFlow<List<Pair<Float, Float>>> = _trajectoryHistory.asStateFlow()

    // Visual Program Interpreter
    val interpreter = ProgramInterpreter(this)
    val interpreterStatus: StateFlow<InterpreterStatus> = interpreter.status
    val activeProgramBlockId: StateFlow<String?> = interpreter.activeBlockId
    val executionLogs: StateFlow<List<String>> = interpreter.executionLogs

    // Active Experiment tracking
    private val _activeExperiment = MutableStateFlow<RoboticsExperiment?>(null)
    val activeExperiment: StateFlow<RoboticsExperiment?> = _activeExperiment.asStateFlow()

    private val _experimentCompleted = MutableStateFlow(false)
    val experimentCompleted: StateFlow<Boolean> = _experimentCompleted.asStateFlow()

    private var simulationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultProjectsIfEmpty()
            // Load the first available project
            allProjects.filter { it.isNotEmpty() }.first().let { list ->
                loadProject(list.first())
            }
        }
    }

    fun navigateTo(screen: ActiveScreen) {
        _currentScreen.value = screen
        if (screen == ActiveScreen.WORKSHOP) {
            camera.distance = 2.2f
            camera.elevationDeg = 25f
        } else if (screen == ActiveScreen.SIMULATOR) {
            camera.distance = 5.0f
            camera.elevationDeg = 35f
        }
    }

    fun setGraphicsProfile(profile: GraphicsProfile) {
        _graphicsProfile.value = profile
    }

    fun selectComponent(componentId: String?) {
        _selectedComponentId.value = componentId
    }

    fun addComponent(type: ComponentType) {
        val count = _components.value.count { it.type == type }
        val newComp = when (type) {
            ComponentType.CHASSIS -> RobotComponent(
                name = "Chassis Base ${count + 1}",
                type = type,
                posX = 0f, posY = 0.04f, posZ = 0f
            )
            ComponentType.LEFT_WHEEL -> RobotComponent(
                name = "Left Wheel ${count + 1}",
                type = type,
                posX = -0.16f, posY = 0f, posZ = 0.05f
            )
            ComponentType.RIGHT_WHEEL -> RobotComponent(
                name = "Right Wheel ${count + 1}",
                type = type,
                posX = 0.16f, posY = 0f, posZ = 0.05f
            )
            ComponentType.CASTER_WHEEL -> RobotComponent(
                name = "Caster Wheel ${count + 1}",
                type = type,
                posX = 0f, posY = 0f, posZ = -0.14f,
                wheelRadius = 0.025f
            )
            ComponentType.DISTANCE_SENSOR -> RobotComponent(
                name = "Ultrasonic Sensor ${count + 1}",
                type = type,
                posX = 0f, posY = 0.07f, posZ = 0.19f
            )
            ComponentType.LIDAR_2D -> RobotComponent(
                name = "2D LiDAR Scanner ${count + 1}",
                type = type,
                posX = 0f, posY = 0.12f, posZ = 0.05f
            )
            ComponentType.IMU_SENSOR -> RobotComponent(
                name = "IMU Sensor ${count + 1}",
                type = type,
                posX = 0f, posY = 0.07f, posZ = 0f
            )
            ComponentType.ARM_BASE -> RobotComponent(
                name = "Arm Base Joint",
                type = type,
                posX = 0f, posY = 0.08f, posZ = 0.05f
            )
            ComponentType.ARM_LINK_1 -> RobotComponent(
                name = "Shoulder Link",
                type = type,
                posX = 0f, posY = 0.12f, posZ = 0.05f,
                jointAngleDeg = 45f
            )
            ComponentType.ARM_LINK_2 -> RobotComponent(
                name = "Forearm Link",
                type = type,
                posX = 0f, posY = 0.24f, posZ = 0.08f,
                jointAngleDeg = -30f
            )
            ComponentType.CONTROLLER -> RobotComponent(
                name = "RoboCore MCU",
                type = type,
                posX = 0f, posY = 0.09f, posZ = 0f
            )
            ComponentType.BATTERY_PACK -> RobotComponent(
                name = "LiPo Battery",
                type = type,
                posX = 0f, posY = 0.05f, posZ = -0.06f
            )
        }

        val updated = _components.value.toMutableList()
        updated.add(newComp)
        _components.value = updated
        _selectedComponentId.value = newComp.id
        autoSave()
    }

    fun updateComponent(updated: RobotComponent) {
        val list = _components.value.map { if (it.id == updated.id) updated else it }
        _components.value = list
        autoSave()
    }

    fun removeComponent(componentId: String) {
        val list = _components.value.filter { it.id != componentId && it.parentId != componentId }
        _components.value = list
        if (_selectedComponentId.value == componentId) {
            _selectedComponentId.value = null
        }
        autoSave()
    }

    // Program block manipulation
    fun addProgramBlock(block: ProgramBlock) {
        val list = _programBlocks.value.toMutableList()
        list.add(block)
        _programBlocks.value = list
        autoSave()
    }

    fun updateProgramBlocks(blocks: List<ProgramBlock>) {
        _programBlocks.value = blocks
        autoSave()
    }

    fun removeProgramBlock(blockId: String) {
        _programBlocks.value = _programBlocks.value.filter { it.id != blockId }
        autoSave()
    }

    // Projects CRUD
    fun loadProject(entity: ProjectEntity) {
        stopSimulation()
        _activeProjectId.value = entity.id
        _activeProjectName.value = entity.name
        _components.value = JsonHelper.deserializeComponents(entity.assemblyJson)
        _programBlocks.value = JsonHelper.deserializeProgram(entity.programJson)
        _obstacles.value = JsonHelper.deserializeObstacles(entity.obstaclesJson)
        _selectedComponentId.value = null
        resetSimulationState()
    }

    fun createNewProject(name: String, desc: String) {
        viewModelScope.launch {
            val entity = repository.createEmptyProject(name, desc)
            repository.saveProject(entity)
            loadProject(entity)
            navigateTo(ActiveScreen.WORKSHOP)
        }
    }

    fun duplicateCurrentProject() {
        val currentId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val current = repository.getProject(currentId) ?: return@launch
            val dup = repository.duplicateProject(current)
            repository.saveProject(dup)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_activeProjectId.value == id) {
                val remaining = allProjects.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    loadProject(remaining.first())
                }
            }
        }
    }

    private fun autoSave() {
        val currentId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val entity = ProjectEntity(
                id = currentId,
                name = _activeProjectName.value,
                description = "RoboLab Project",
                assemblyJson = JsonHelper.serializeComponents(_components.value),
                programJson = JsonHelper.serializeProgram(_programBlocks.value),
                obstaclesJson = JsonHelper.serializeObstacles(_obstacles.value),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(entity)
        }
    }

    // Experiments
    fun launchExperiment(experiment: RoboticsExperiment) {
        _activeExperiment.value = experiment
        _experimentCompleted.value = false
        _programBlocks.value = experiment.defaultBlocks
        navigateTo(ActiveScreen.SIMULATOR)
        resetSimulationState()
        // If experiment is maze or obstacle, set appropriate obstacles
        if (experiment.id == "exp_maze_escape") {
            _obstacles.value = ProjectRepository.createMazeObstacles()
        } else if (experiment.id == "exp_obstacle_stop") {
            _obstacles.value = listOf(
                Obstacle(name = "Direct Barrier", posX = 0f, posZ = 1.6f, width = 0.8f, length = 0.4f)
            )
        }
    }

    // Simulation Engine Controls
    fun startSimulation() {
        if (_isSimRunning.value && !_isSimPaused.value) return
        _isSimRunning.value = true
        _isSimPaused.value = false

        if (simulationJob == null || !simulationJob!!.isActive) {
            startPhysicsLoop()
        }
    }

    fun pauseSimulation() {
        _isSimPaused.value = true
    }

    fun resumeSimulation() {
        _isSimPaused.value = false
    }

    fun stopSimulation() {
        _isSimRunning.value = false
        _isSimPaused.value = false
        simulationJob?.cancel()
        simulationJob = null
        interpreter.stop()
        motorLeftSpeed = 0f
        motorRightSpeed = 0f
    }

    fun resetSimulationState() {
        stopSimulation()
        robotX = 0f
        robotZ = 0f
        robotHeadingRad = 0f
        motorLeftSpeed = 0f
        motorRightSpeed = 0f
        _trajectoryHistory.value = emptyList()
        _experimentCompleted.value = false
        updateTelemetry(0f, 0f, 0f, 0f)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        _simSpeedMultiplier.value = multiplier
    }

    fun runProgramInSimulator() {
        startSimulation()
        interpreter.start(_programBlocks.value, viewModelScope)
    }

    fun stopProgram() {
        interpreter.stop()
    }

    // Manual joystick overrides
    fun manualDrive(forward: Float, turn: Float) {
        if (interpreterStatus.value == InterpreterStatus.RUNNING) {
            interpreter.stop()
        }
        motorLeftSpeed = (forward + turn).coerceIn(-1.0f, 1.0f)
        motorRightSpeed = (forward - turn).coerceIn(-1.0f, 1.0f)
        startSimulation()
    }

    // Implementation of RobotMotorController interface for ProgramInterpreter
    override fun setMotorSpeeds(left: Float, right: Float) {
        motorLeftSpeed = left.coerceIn(-1.0f, 1.0f)
        motorRightSpeed = right.coerceIn(-1.0f, 1.0f)
    }

    override fun getDistanceSensorReading(): Float {
        return _telemetry.value.distanceSensorReadingM
    }

    override fun getHeadingDeg(): Float {
        return _telemetry.value.headingDeg
    }

    override fun stopMotors() {
        motorLeftSpeed = 0f
        motorRightSpeed = 0f
    }

    override fun logMessage(msg: String) {
        // Appended automatically in telemetry
    }

    override fun setArmAngles(angle1Deg: Float, angle2Deg: Float) {
        armAngle1 = angle1Deg
        armAngle2 = angle2Deg
        val updated = _components.value.map { comp ->
            when (comp.type) {
                ComponentType.ARM_LINK_1 -> comp.copy(jointAngleDeg = angle1Deg)
                ComponentType.ARM_LINK_2 -> comp.copy(jointAngleDeg = angle2Deg)
                else -> comp
            }
        }
        _components.value = updated
    }

    private fun startPhysicsLoop() {
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            val dt = 0.02f // 50 Hz physics step
            var simTime = _telemetry.value.simTimeSec
            var lastHistoryRecord = 0L

            while (isActive && _isSimRunning.value) {
                if (!_isSimPaused.value) {
                    val effectiveDt = dt * _simSpeedMultiplier.value

                    // 1. Gather kinematics parameters from active components
                    val leftWheels = _components.value.filter { it.type == ComponentType.LEFT_WHEEL && it.isEnabled }
                    val rightWheels = _components.value.filter { it.type == ComponentType.RIGHT_WHEEL && it.isEnabled }
                    val wheelRadius = leftWheels.firstOrNull()?.wheelRadius ?: 0.045f
                    val wheelBase = 0.32f

                    // Angular velocity per wheel (rad/s)
                    val maxRps = (leftWheels.firstOrNull()?.motorMaxRpm ?: 180f) / 60.0f
                    val maxRadPerSec = maxRps * 2.0f * PI.toFloat()
                    val omegaL = motorLeftSpeed * maxRadPerSec
                    val omegaR = motorRightSpeed * maxRadPerSec

                    // 2. Differential Drive Kinematics
                    val (linearVel, angularVelRad) = DifferentialDriveKinematics.computeChassisVelocity(
                        omegaL, omegaR, wheelRadius, wheelBase
                    )

                    // 3. Integrate Pose
                    val (newX, newZ, newHeadingRad) = DifferentialDriveKinematics.integratePose(
                        robotX, robotZ, robotHeadingRad, linearVel, angularVelRad, effectiveDt
                    )

                    // 4. Collision Detection
                    val collision = CollisionEngine.checkCollision(newX, newZ, 0.22f, _obstacles.value)
                    if (collision.hasCollision) {
                        // Collision response: push out and halt forward motion
                        robotX = newX + collision.pushoutX
                        robotZ = newZ + collision.pushoutZ
                        motorLeftSpeed = 0f
                        motorRightSpeed = 0f
                    } else {
                        robotX = newX
                        robotZ = newZ
                        robotHeadingRad = newHeadingRad
                    }

                    simTime += effectiveDt

                    // 5. Sensor Calculations
                    val sensorDist = RaycastSensorEngine.castRay(
                        originX = robotX + 0.15f * kotlin.math.sin(robotHeadingRad),
                        originZ = robotZ + 0.15f * kotlin.math.cos(robotHeadingRad),
                        directionRad = robotHeadingRad,
                        maxRangeMeters = 3.5f,
                        obstacles = _obstacles.value
                    )

                    // LiDAR Sweep
                    val lidarPoints = if (_graphicsProfile.value != GraphicsProfile.PERFORMANCE) {
                        RaycastSensorEngine.performLidarSweep(
                            originX = robotX,
                            originZ = robotZ,
                            baseHeadingRad = robotHeadingRad,
                            numRays = if (_graphicsProfile.value == GraphicsProfile.HIGH_QUALITY) 32 else 16,
                            maxRangeM = 4.0f,
                            obstacles = _obstacles.value
                        )
                    } else emptyList()

                    // Record trajectory point every 150ms
                    val now = System.currentTimeMillis()
                    if (now - lastHistoryRecord > 150) {
                        lastHistoryRecord = now
                        val hist = _trajectoryHistory.value.toMutableList()
                        hist.add(Pair(robotX, robotZ))
                        if (hist.size > 200) hist.removeAt(0)
                        _trajectoryHistory.value = hist
                    }

                    // 6. Check Active Experiment Completion
                    checkExperimentConditions(robotX, robotZ, robotHeadingRad, sensorDist, collision.hasCollision)

                    // 7. Update Telemetry State
                    val headingDeg = Math.toDegrees(robotHeadingRad.toDouble()).toFloat()
                    val angularVelDeg = Math.toDegrees(angularVelRad.toDouble()).toFloat()

                    _telemetry.value = SimulationTelemetry(
                        simTimeSec = simTime,
                        realFps = 50,
                        posX = robotX,
                        posZ = robotZ,
                        headingDeg = headingDeg,
                        linearVelocity = linearVel,
                        angularVelocityDeg = angularVelDeg,
                        leftWheelRps = omegaL / (2f * PI.toFloat()),
                        rightWheelRps = omegaR / (2f * PI.toFloat()),
                        distanceSensorReadingM = sensorDist,
                        hasCollision = collision.hasCollision,
                        collisionObstacleName = collision.obstacleName,
                        lidarReadings = lidarPoints,
                        activeProgramBlockId = activeProgramBlockId.value,
                        logMessages = executionLogs.value.takeLast(5)
                    )
                }
                delay(20) // 50 Hz
            }
        }
    }

    private fun checkExperimentConditions(
        rx: Float, rz: Float, headingRad: Float, distanceM: Float, hasCollision: Boolean
    ) {
        val exp = _activeExperiment.value ?: return
        if (_experimentCompleted.value) return

        when (exp.id) {
            "exp_straight_line" -> {
                val dist = kotlin.math.sqrt(rx * rx + rz * rz)
                val headingDev = abs(Math.toDegrees(headingRad.toDouble()).toFloat())
                if (dist >= 1.5f && headingDev < 3.0f) {
                    _experimentCompleted.value = true
                }
            }
            "exp_controlled_turn" -> {
                val deg = abs(Math.toDegrees(headingRad.toDouble()).toFloat())
                if (deg in 85f..95f && !hasCollision) {
                    _experimentCompleted.value = true
                }
            }
            "exp_obstacle_stop" -> {
                if (distanceM in 0.15f..0.45f && motorLeftSpeed == 0f && motorRightSpeed == 0f && !hasCollision) {
                    _experimentCompleted.value = true
                }
            }
            "exp_maze_escape" -> {
                val dist = kotlin.math.sqrt(rx * rx + rz * rz)
                if (dist >= 2.5f && !hasCollision) {
                    _experimentCompleted.value = true
                }
            }
            "exp_arm_kinematics" -> {
                if (armAngle1 != 0f && armAngle2 != 0f) {
                    _experimentCompleted.value = true
                }
            }
        }
    }

    private fun updateTelemetry(x: Float, z: Float, headingDeg: Float, sensorDist: Float) {
        _telemetry.value = _telemetry.value.copy(
            posX = x,
            posZ = z,
            headingDeg = headingDeg,
            linearVelocity = 0f,
            angularVelocityDeg = 0f,
            leftWheelRps = 0f,
            rightWheelRps = 0f,
            distanceSensorReadingM = sensorDist,
            hasCollision = false,
            collisionObstacleName = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}
