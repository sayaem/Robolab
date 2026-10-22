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
import com.example.robolab.model.assembly.*
import com.example.robolab.physics.CollisionEngine
import com.example.robolab.physics.DifferentialDriveKinematics
import com.example.robolab.physics.RaycastSensorEngine
import com.example.robolab.physics.rigid.Matrix3
import com.example.robolab.physics.rigid.RigidPhysicsWorld
import com.example.robolab.renderer3d.Camera3D
import com.example.robolab.renderer3d.Vector3
import com.example.robolab.validator.AssemblyValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
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

data class JointTelemetry(
    val jointId: String,
    val angleDeg: Float,
    val angularVelocityRad: Float,
    val motorTorqueNm: Float
)

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

    private val _activeProjectName = MutableStateFlow("3D Sandbox: Revolute Motor Mechanism")
    val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

    // --- General-Purpose 3D Assembly State ---
    private val _assemblyParts = MutableStateFlow<List<AssemblyPart>>(emptyList())
    val assemblyParts: StateFlow<List<AssemblyPart>> = _assemblyParts.asStateFlow()

    private val _assemblyJoints = MutableStateFlow<List<AssemblyJoint>>(emptyList())
    val assemblyJoints: StateFlow<List<AssemblyJoint>> = _assemblyJoints.asStateFlow()

    private val _selectedAssemblyPartId = MutableStateFlow<String?>(null)
    val selectedAssemblyPartId: StateFlow<String?> = _selectedAssemblyPartId.asStateFlow()

    private val _selectedAssemblyJointId = MutableStateFlow<String?>(null)
    val selectedAssemblyJointId: StateFlow<String?> = _selectedAssemblyJointId.asStateFlow()

    // 3D Rigid-Body Physics World & Real-Time Dynamic Poses
    val rigidPhysicsWorld = RigidPhysicsWorld()

    private val _dynamicPoses = MutableStateFlow<Map<String, Pair<Vector3, Matrix3>>>(emptyMap())
    val dynamicPoses: StateFlow<Map<String, Pair<Vector3, Matrix3>>> = _dynamicPoses.asStateFlow()

    private val _jointTelemetry = MutableStateFlow<Map<String, JointTelemetry>>(emptyMap())
    val jointTelemetry: StateFlow<Map<String, JointTelemetry>> = _jointTelemetry.asStateFlow()

    // Legacy components (maintained for backward compatibility with existing tests/curriculum)
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
    private var rigidSimJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultProjectsIfEmpty()
            allProjects.filter { it.isNotEmpty() }.first().let { list ->
                // Prefer opening the 3D sandbox project if available
                val sandboxProj = list.find { it.id == "proj_revolute_sandbox" } ?: list.first()
                loadProject(sandboxProj)
            }
        }
    }

    fun navigateTo(screen: ActiveScreen) {
        _currentScreen.value = screen
        if (screen == ActiveScreen.WORKSHOP) {
            camera.distance = 2.5f
            camera.elevationDeg = 30f
        } else if (screen == ActiveScreen.SIMULATOR) {
            camera.distance = 4.0f
            camera.elevationDeg = 35f
        }
    }

    fun setGraphicsProfile(profile: GraphicsProfile) {
        _graphicsProfile.value = profile
    }

    // --- 3D Assembly Operations (Milestone 1) ---

    fun addCustomPart(shape: ShapeType) {
        val count = _assemblyParts.value.size
        val newPart = when (shape) {
            ShapeType.BOX -> AssemblyPart(
                name = "Box Part ${count + 1}",
                shapeType = shape,
                dimX = 0.25f, dimY = 0.20f, dimZ = 0.25f,
                massKg = 1.0f,
                colorHex = 0xFF00B4D8,
                posX = 0f, posY = 0.35f, posZ = 0f
            )
            ShapeType.CYLINDER -> AssemblyPart(
                name = "Cylinder Part ${count + 1}",
                shapeType = shape,
                radius = 0.08f, dimY = 0.30f,
                massKg = 1.2f,
                colorHex = 0xFF38BDF8,
                posX = 0f, posY = 0.35f, posZ = 0f
            )
            ShapeType.SPHERE -> AssemblyPart(
                name = "Sphere Part ${count + 1}",
                shapeType = shape,
                radius = 0.10f,
                massKg = 0.8f,
                colorHex = 0xFFF59E0B,
                posX = 0f, posY = 0.35f, posZ = 0f
            )
            ShapeType.CAPSULE -> AssemblyPart(
                name = "Capsule Part ${count + 1}",
                shapeType = shape,
                radius = 0.06f, dimY = 0.24f,
                massKg = 1.0f,
                colorHex = 0xFFA855F7,
                posX = 0f, posY = 0.35f, posZ = 0f
            )
        }

        val list = _assemblyParts.value.toMutableList()
        list.add(newPart)
        _assemblyParts.value = list
        _selectedAssemblyPartId.value = newPart.id
        autoSave()
    }

    fun updateCustomPart(part: AssemblyPart) {
        _assemblyParts.value = _assemblyParts.value.map { if (it.id == part.id) part else it }
        autoSave()
    }

    fun removeCustomPart(partId: String) {
        _assemblyParts.value = _assemblyParts.value.filter { it.id != partId }
        _assemblyJoints.value = _assemblyJoints.value.filter { it.parentPartId != partId && it.childPartId != partId }
        if (_selectedAssemblyPartId.value == partId) _selectedAssemblyPartId.value = null
        autoSave()
    }

    fun duplicateCustomPart(partId: String) {
        val orig = _assemblyParts.value.find { it.id == partId } ?: return
        val copy = orig.copy(
            id = UUID.randomUUID().toString(),
            name = "${orig.name} (Copy)",
            posX = orig.posX + 0.15f,
            posY = orig.posY + 0.05f
        )
        val list = _assemblyParts.value.toMutableList()
        list.add(copy)
        _assemblyParts.value = list
        _selectedAssemblyPartId.value = copy.id
        autoSave()
    }

    fun selectAssemblyPart(partId: String?) {
        _selectedAssemblyPartId.value = partId
        if (partId != null) _selectedAssemblyJointId.value = null
    }

    fun selectAssemblyJoint(jointId: String?) {
        _selectedAssemblyJointId.value = jointId
        if (jointId != null) _selectedAssemblyPartId.value = null
    }

    fun addCustomJoint(
        parentPartId: String,
        childPartId: String,
        type: JointType = JointType.REVOLUTE,
        axis: Vector3 = Vector3(1f, 0f, 0f),
        enableMotor: Boolean = true,
        motorTargetVelocity: Float = 2.0f,
        motorMaxTorque: Float = 15.0f
    ) {
        val count = _assemblyJoints.value.size
        val newJoint = AssemblyJoint(
            name = "Joint ${count + 1} (${type.displayName})",
            type = type,
            parentPartId = parentPartId,
            childPartId = childPartId,
            axisX = axis.x, axisY = axis.y, axisZ = axis.z,
            enableMotor = enableMotor,
            motorTargetVelocityRadPerSec = motorTargetVelocity,
            motorMaxTorqueNm = motorMaxTorque
        )
        val list = _assemblyJoints.value.toMutableList()
        list.add(newJoint)
        _assemblyJoints.value = list
        _selectedAssemblyJointId.value = newJoint.id
        autoSave()
    }

    fun updateCustomJoint(joint: AssemblyJoint) {
        _assemblyJoints.value = _assemblyJoints.value.map { if (it.id == joint.id) joint else it }
        autoSave()
    }

    fun removeCustomJoint(jointId: String) {
        _assemblyJoints.value = _assemblyJoints.value.filter { it.id != jointId }
        if (_selectedAssemblyJointId.value == jointId) _selectedAssemblyJointId.value = null
        autoSave()
    }

    fun clearToEmptyWorkspace() {
        stopSimulation()
        _assemblyParts.value = emptyList()
        _assemblyJoints.value = emptyList()
        _selectedAssemblyPartId.value = null
        _selectedAssemblyJointId.value = null
        _dynamicPoses.value = emptyMap()
        autoSave()
    }

    fun loadMilestone1Slice() {
        stopSimulation()
        val m1 = ProjectRepository.createMilestone1Assembly()
        _assemblyParts.value = m1.parts
        _assemblyJoints.value = m1.joints
        _selectedAssemblyPartId.value = m1.parts.getOrNull(1)?.id
        _dynamicPoses.value = emptyMap()
        autoSave()
    }

    // --- Genuine 3D Rigid-Body Physics Simulation (Milestone 1) ---

    fun startRigidSimulation() {
        if (_isSimRunning.value && !_isSimPaused.value) return
        _isSimRunning.value = true
        _isSimPaused.value = false

        val currentAssembly = RoboLabAssembly(
            id = _activeProjectId.value ?: "curr",
            name = _activeProjectName.value,
            parts = _assemblyParts.value,
            joints = _assemblyJoints.value
        )
        rigidPhysicsWorld.loadAssembly(currentAssembly)

        startRigidPhysicsLoop()
    }

    fun pauseRigidSimulation() {
        _isSimPaused.value = true
    }

    fun resumeRigidSimulation() {
        _isSimPaused.value = false
    }

    fun resetRigidSimulation() {
        stopSimulation()
        _dynamicPoses.value = emptyMap()
        _jointTelemetry.value = emptyMap()
    }

    fun setJointMotorVelocity(jointId: String, targetVelRadPerSec: Float) {
        _assemblyJoints.value = _assemblyJoints.value.map {
            if (it.id == jointId) it.copy(motorTargetVelocityRadPerSec = targetVelRadPerSec) else it
        }
        // If simulation running, update active physics joint
        rigidPhysicsWorld.joints.find { it.id == jointId }?.let { liveJoint ->
            // Modulate live motor
            val updatedData = liveJoint.jointData.copy(motorTargetVelocityRadPerSec = targetVelRadPerSec)
            val idx = rigidPhysicsWorld.joints.indexOf(liveJoint)
            if (idx != -1) {
                rigidPhysicsWorld.joints[idx] = com.example.robolab.physics.rigid.RigidJoint(
                    updatedData, liveJoint.bodyA, liveJoint.bodyB
                )
            }
        }
    }

    private fun startRigidPhysicsLoop() {
        rigidSimJob?.cancel()
        rigidSimJob = viewModelScope.launch(Dispatchers.Default) {
            val dt = 0.02f // 50 Hz physics step
            while (isActive && _isSimRunning.value) {
                if (!_isSimPaused.value) {
                    val effectiveDt = dt * _simSpeedMultiplier.value
                    rigidPhysicsWorld.step(effectiveDt)

                    // Extract real-time world poses for 3D renderer
                    val poses = mutableMapOf<String, Pair<Vector3, Matrix3>>()
                    for (body in rigidPhysicsWorld.bodies) {
                        poses[body.id] = Pair(body.position, body.rotationMatrix)
                    }
                    _dynamicPoses.value = poses

                    // Extract joint telemetry (angles, speed, torque)
                    val tele = mutableMapOf<String, JointTelemetry>()
                    for (joint in rigidPhysicsWorld.joints) {
                        tele[joint.id] = JointTelemetry(
                            jointId = joint.id,
                            angleDeg = joint.currentAngleDeg,
                            angularVelocityRad = joint.currentAngularVelocityRadPerSec,
                            motorTorqueNm = joint.currentMotorTorqueNm
                        )
                    }
                    _jointTelemetry.value = tele
                }
                delay(20)
            }
        }
    }

    // --- Legacy Component & Differential Simulation (Kept for compatibility) ---

    fun selectComponent(componentId: String?) {
        _selectedComponentId.value = componentId
    }

    fun addComponent(type: ComponentType) {
        val count = _components.value.count { it.type == type }
        val newComp = RobotComponent(
            name = "${type.displayName} ${count + 1}",
            type = type,
            posX = 0f, posY = 0.04f, posZ = 0f
        )
        val updated = _components.value.toMutableList()
        updated.add(newComp)
        _components.value = updated
        _selectedComponentId.value = newComp.id
        autoSave()
    }

    fun updateComponent(updated: RobotComponent) {
        _components.value = _components.value.map { if (it.id == updated.id) updated else it }
        autoSave()
    }

    fun removeComponent(componentId: String) {
        _components.value = _components.value.filter { it.id != componentId && it.parentId != componentId }
        if (_selectedComponentId.value == componentId) _selectedComponentId.value = null
        autoSave()
    }

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

    // --- Project Persistence ---

    fun loadProject(entity: ProjectEntity) {
        stopSimulation()
        _activeProjectId.value = entity.id
        _activeProjectName.value = entity.name

        val assembly = JsonHelper.deserializeRoboLabAssembly(entity.assemblyJson)
        if (assembly.parts.isNotEmpty()) {
            _assemblyParts.value = assembly.parts
            _assemblyJoints.value = assembly.joints
        } else {
            // Load legacy components
            val legacy = JsonHelper.deserializeComponents(entity.assemblyJson)
            _components.value = legacy
            if (_assemblyParts.value.isEmpty()) {
                loadMilestone1Slice()
            }
        }

        _programBlocks.value = JsonHelper.deserializeProgram(entity.programJson)
        _obstacles.value = JsonHelper.deserializeObstacles(entity.obstaclesJson)
        _selectedComponentId.value = null
        _selectedAssemblyPartId.value = _assemblyParts.value.firstOrNull()?.id
        _selectedAssemblyJointId.value = null
        resetSimulationState()
        resetRigidSimulation()
    }

    fun createNewProject(name: String, desc: String) {
        viewModelScope.launch {
            val emptyAssembly = RoboLabAssembly(
                name = name.ifBlank { "Custom 3D Assembly" },
                parts = emptyList(),
                joints = emptyList()
            )
            val entity = ProjectEntity(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Custom 3D Assembly" },
                description = desc.ifBlank { "General-purpose 3D multi-body robotics sandbox" },
                assemblyJson = JsonHelper.serializeRoboLabAssembly(emptyAssembly),
                programJson = "[]",
                obstaclesJson = "[]"
            )
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
            val assembly = RoboLabAssembly(
                id = currentId,
                name = _activeProjectName.value,
                parts = _assemblyParts.value,
                joints = _assemblyJoints.value
            )
            val entity = ProjectEntity(
                id = currentId,
                name = _activeProjectName.value,
                description = "General-Purpose 3D Robotics Assembly",
                assemblyJson = JsonHelper.serializeRoboLabAssembly(assembly),
                programJson = JsonHelper.serializeProgram(_programBlocks.value),
                obstaclesJson = JsonHelper.serializeObstacles(_obstacles.value),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(entity)
        }
    }

    // --- Experiments ---

    fun launchExperiment(experiment: RoboticsExperiment) {
        _activeExperiment.value = experiment
        _experimentCompleted.value = false
        _programBlocks.value = experiment.defaultBlocks
        navigateTo(ActiveScreen.SIMULATOR)
        resetSimulationState()
    }

    // --- Simulation Transport Controls ---

    fun startSimulation() {
        if (_assemblyParts.value.isNotEmpty()) {
            startRigidSimulation()
            return
        }
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
        rigidSimJob?.cancel()
        rigidSimJob = null
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
        _dynamicPoses.value = emptyMap()
        _jointTelemetry.value = emptyMap()
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

    fun manualDrive(forward: Float, turn: Float) {
        if (interpreterStatus.value == InterpreterStatus.RUNNING) {
            interpreter.stop()
        }
        motorLeftSpeed = (forward + turn).coerceIn(-1.0f, 1.0f)
        motorRightSpeed = (forward - turn).coerceIn(-1.0f, 1.0f)
        startSimulation()
    }

    override fun setMotorSpeeds(left: Float, right: Float) {
        motorLeftSpeed = left.coerceIn(-1.0f, 1.0f)
        motorRightSpeed = right.coerceIn(-1.0f, 1.0f)
    }

    override fun getDistanceSensorReading(): Float = _telemetry.value.distanceSensorReadingM

    override fun getHeadingDeg(): Float = _telemetry.value.headingDeg

    override fun stopMotors() {
        motorLeftSpeed = 0f
        motorRightSpeed = 0f
    }

    override fun logMessage(msg: String) {}

    override fun setArmAngles(angle1Deg: Float, angle2Deg: Float) {
        armAngle1 = angle1Deg
        armAngle2 = angle2Deg
    }

    private fun startPhysicsLoop() {
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            val dt = 0.02f
            var simTime = _telemetry.value.simTimeSec

            while (isActive && _isSimRunning.value) {
                if (!_isSimPaused.value) {
                    val effectiveDt = dt * _simSpeedMultiplier.value
                    val leftWheels = _components.value.filter { it.type == ComponentType.LEFT_WHEEL && it.isEnabled }
                    val wheelRadius = leftWheels.firstOrNull()?.wheelRadius ?: 0.045f
                    val wheelBase = 0.32f

                    val maxRps = (leftWheels.firstOrNull()?.motorMaxRpm ?: 180f) / 60.0f
                    val maxRadPerSec = maxRps * 2.0f * PI.toFloat()
                    val omegaL = motorLeftSpeed * maxRadPerSec
                    val omegaR = motorRightSpeed * maxRadPerSec

                    val (linearVel, angularVelRad) = DifferentialDriveKinematics.computeChassisVelocity(
                        omegaL, omegaR, wheelRadius, wheelBase
                    )

                    val (newX, newZ, newHeadingRad) = DifferentialDriveKinematics.integratePose(
                        robotX, robotZ, robotHeadingRad, linearVel, angularVelRad, effectiveDt
                    )

                    val collision = CollisionEngine.checkCollision(newX, newZ, 0.22f, _obstacles.value)
                    if (collision.hasCollision) {
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
                    val headingDeg = Math.toDegrees(robotHeadingRad.toDouble()).toFloat()

                    _telemetry.value = SimulationTelemetry(
                        simTimeSec = simTime,
                        posX = robotX,
                        posZ = robotZ,
                        headingDeg = headingDeg,
                        linearVelocity = linearVel,
                        angularVelocityDeg = Math.toDegrees(angularVelRad.toDouble()).toFloat()
                    )
                }
                delay(20)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}
