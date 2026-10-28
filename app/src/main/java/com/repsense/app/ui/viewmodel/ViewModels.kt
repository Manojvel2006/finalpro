package com.repsense.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repsense.app.data.ble.BleManager
import com.repsense.app.data.ble.ConnectionState
import com.repsense.app.data.model.Device
import com.repsense.app.data.model.ExerciseType
import com.repsense.app.data.model.FormStatus
import com.repsense.app.data.model.SensorData
import com.repsense.app.data.model.User
import com.repsense.app.data.model.Workout
import com.repsense.app.data.repository.AnalyticsSummary
import com.repsense.app.data.repository.UserRepository
import com.repsense.app.data.repository.WorkoutRepository
import com.repsense.app.utils.VoiceGuidanceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class LiveWorkoutUiState(
    val isWorkoutActive: Boolean = false,
    val isPaused: Boolean = false,
    val exercise: ExerciseType = ExerciseType.BICEP_CURL,
    val reps: Int = 0,
    val correctReps: Int = 0,
    val partialReps: Int = 0,
    val fastReps: Int = 0,
    val incorrectReps: Int = 0,
    val heartRate: Int = 82,
    val heartRateValid: Boolean = true,
    val spo2: Int = 98,
    val spo2Valid: Boolean = true,
    val sensorQuality: String = "OPTIMAL",
    val durationSeconds: Int = 0,
    val calories: Float = 0f,
    val formStatus: FormStatus = FormStatus.NO_EXERCISE,
    val formAccuracy: Float = 100f,
    val latestSensorData: SensorData? = null,
    val upperArmAngle: Float = 0f,
    val forearmAngle: Float = 25f,
    val elbowAngle: Float = 25f,
    val peakElbowAngle: Float = 25f,
    val minElbowAngle: Float = 180f,
    val rangeOfMotion: Float = 0f,
    val upperArmStability: Float = 95f,
    val recentAccelWave: List<Float> = emptyList(),
    val feedbackMessage: String = "Perform steady, controlled reps.",
    val isVoiceGuidanceEnabled: Boolean = true,
    val calibrationStatus: String = "calibrated"
)

class LiveWorkoutViewModel(
    private val bleManager: BleManager = BleManager.instance,
    private val workoutRepository: WorkoutRepository = WorkoutRepository.instance,
    private val voiceGuidance: VoiceGuidanceManager = VoiceGuidanceManager.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveWorkoutUiState())
    val uiState: StateFlow<LiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sensorJob: Job? = null
    private var sessionStartTime = 0L
    private var lastRecordedRep = 0
    private var peakElbowSession = 25f
    private var minElbowSession = 180f
    private var stableUpperArmCount = 0
    private var totalSampleCount = 0

    fun startWorkout(exercise: ExerciseType) {
        sessionStartTime = System.currentTimeMillis()
        lastRecordedRep = 0
        peakElbowSession = 25f
        minElbowSession = 180f
        stableUpperArmCount = 0
        totalSampleCount = 0

        _uiState.value = LiveWorkoutUiState(
            isWorkoutActive = true,
            isPaused = false,
            exercise = exercise,
            reps = 0,
            durationSeconds = 0,
            calories = 0f,
            formStatus = FormStatus.NO_EXERCISE,
            feedbackMessage = "Ready. Begin your first repetition!",
            isVoiceGuidanceEnabled = voiceGuidance.isEnabled,
            calibrationStatus = bleManager.calibrationStatus.value
        )

        bleManager.startWorkoutSession(exercise)
        voiceGuidance.speak("Starting ${exercise.displayName}. Begin your set.")
        startTimer()
        observeSensorData()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_uiState.value.isPaused && _uiState.value.isWorkoutActive) {
                    val newSecs = _uiState.value.durationSeconds + 1
                    val hr = _uiState.value.heartRate
                    val addedCal = (hr * 0.6309f * 72f / 3600f)
                    _uiState.value = _uiState.value.copy(
                        durationSeconds = newSecs,
                        calories = _uiState.value.calories + addedCal
                    )
                }
            }
        }
    }

    private fun observeSensorData() {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch {
            bleManager.sensorFlow.collect { sensor ->
                if (!_uiState.value.isWorkoutActive || _uiState.value.isPaused) return@collect

                val prev = _uiState.value
                val wave = (prev.recentAccelWave + sensor.forearmGyroZ).takeLast(28)

                var newReps = prev.reps
                var correct = prev.correctReps
                var partial = prev.partialReps
                var fast = prev.fastReps
                var incorrect = prev.incorrectReps
                var feedbackMsg = prev.feedbackMessage

                peakElbowSession = max(peakElbowSession, sensor.elbowAngle)
                if (sensor.elbowAngle > 10f) {
                    minElbowSession = min(minElbowSession, sensor.elbowAngle)
                }
                val currentRom = max(0f, peakElbowSession - minElbowSession)

                totalSampleCount++
                if (kotlin.math.abs(sensor.upperArmAngle) < 15f) {
                    stableUpperArmCount++
                }
                val stability = if (totalSampleCount > 0) {
                    (stableUpperArmCount.toFloat() / totalSampleCount.toFloat()) * 100f
                } else 95f

                // Detect Rep Completion event
                if (sensor.reps > lastRecordedRep) {
                    newReps = sensor.reps
                    lastRecordedRep = sensor.reps

                    when (sensor.form) {
                        FormStatus.CORRECT_CURL -> {
                            correct++
                            feedbackMsg = "Great curl! Full extension & contraction."
                            bleManager.triggerVibration("SHORT")
                            voiceGuidance.speak("$newReps, great form.")
                        }
                        FormStatus.PARTIAL_CURL -> {
                            partial++
                            feedbackMsg = "Incomplete ROM: flex past 120° at the top."
                            bleManager.triggerVibration("WARNING")
                            voiceGuidance.speak("Complete full range of motion.")
                        }
                        FormStatus.FAST_CURL -> {
                            fast++
                            feedbackMsg = "Tempo too fast! Control the 2-second descent."
                            bleManager.triggerVibration("WARNING")
                            voiceGuidance.speak("Slow down the movement.")
                        }
                        FormStatus.INCORRECT_FORM -> {
                            incorrect++
                            feedbackMsg = "Elbow drift detected: pin upper arm to torso."
                            bleManager.triggerVibration("DOUBLE")
                            voiceGuidance.speak("Keep upper arm stationary.")
                        }
                        FormStatus.NO_EXERCISE -> {}
                    }
                }

                val accuracy = if (newReps > 0) {
                    (correct.toFloat() / newReps.toFloat()) * 100f
                } else {
                    100f
                }

                _uiState.value = prev.copy(
                    reps = newReps,
                    correctReps = correct,
                    partialReps = partial,
                    fastReps = fast,
                    incorrectReps = incorrect,
                    formAccuracy = accuracy,
                    heartRate = sensor.heartRate,
                    heartRateValid = sensor.heartRateValid,
                    spo2 = sensor.spo2,
                    spo2Valid = sensor.spo2Valid,
                    sensorQuality = sensor.sensorQuality,
                    formStatus = sensor.form,
                    latestSensorData = sensor,
                    upperArmAngle = sensor.upperArmAngle,
                    forearmAngle = sensor.forearmAngle,
                    elbowAngle = sensor.elbowAngle,
                    peakElbowAngle = peakElbowSession,
                    minElbowAngle = minElbowSession,
                    rangeOfMotion = currentRom,
                    upperArmStability = stability,
                    recentAccelWave = wave,
                    feedbackMessage = feedbackMsg,
                    calibrationStatus = sensor.calibrationStatus
                )
            }
        }
    }

    fun toggleVoiceGuidance() {
        val current = _uiState.value.isVoiceGuidanceEnabled
        voiceGuidance.isEnabled = !current
        _uiState.value = _uiState.value.copy(isVoiceGuidanceEnabled = !current)
    }

    fun calibrateSensors() {
        bleManager.calibrateSensors()
        voiceGuidance.speak("Sensors calibrated to starting angle.")
    }

    fun togglePause() {
        val current = _uiState.value.isPaused
        _uiState.value = _uiState.value.copy(isPaused = !current)
        if (!current) {
            voiceGuidance.speak("Workout paused.")
        } else {
            voiceGuidance.speak("Workout resumed.")
        }
    }

    fun endWorkout(): Workout? {
        val state = _uiState.value
        if (!state.isWorkoutActive) return null

        timerJob?.cancel()
        sensorJob?.cancel()
        bleManager.stopWorkoutSession()

        val workout = Workout(
            id = UUID.randomUUID().toString(),
            exerciseType = state.exercise,
            startTime = sessionStartTime,
            endTime = System.currentTimeMillis(),
            duration = state.durationSeconds,
            totalReps = state.reps,
            correctReps = state.correctReps,
            partialReps = state.partialReps,
            fastReps = state.fastReps,
            incorrectReps = state.incorrectReps,
            formAccuracy = state.formAccuracy,
            averageHeartRate = state.heartRate,
            maxHeartRate = (state.heartRate + 18),
            averageSpO2 = state.spo2,
            estimatedCalories = state.calories.roundToInt(),
            peakElbowAngle = state.peakElbowAngle,
            minElbowAngle = if (state.minElbowAngle == 180f) 25f else state.minElbowAngle,
            rangeOfMotion = state.rangeOfMotion,
            upperArmStability = state.upperArmStability,
            avgAngularVelocity = 72f
        )

        workoutRepository.addWorkout(workout)
        voiceGuidance.speak("Workout finished! You completed ${state.reps} reps.")
        _uiState.value = _uiState.value.copy(isWorkoutActive = false)
        return workout
    }

    fun manualRep(form: FormStatus = FormStatus.CORRECT_CURL) {
        bleManager.triggerManualRep(form)
    }
}

class HomeViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository.instance,
    private val userRepository: UserRepository = UserRepository.instance,
    private val bleManager: BleManager = BleManager.instance
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = workoutRepository.workouts
    val user: StateFlow<User> = userRepository.currentUser
    val connectedDevice: StateFlow<Device?> = bleManager.connectedDevice
    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val weeklyGoal: StateFlow<Int> = workoutRepository.weeklyRepGoal
    val lastDataReceivedTime: StateFlow<Long> = bleManager.lastDataReceivedTime
}

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository.instance
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = workoutRepository.workouts

    fun deleteWorkout(id: String) {
        workoutRepository.deleteWorkout(id)
    }

    fun getWorkout(id: String): Workout? {
        return workoutRepository.getWorkoutById(id)
    }
}

class AnalyticsViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository.instance
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = workoutRepository.workouts
    val weeklyGoal: StateFlow<Int> = workoutRepository.weeklyRepGoal

    fun setGoal(goal: Int) {
        workoutRepository.setWeeklyGoal(goal)
    }

    fun getSummary(): AnalyticsSummary = workoutRepository.getSummary()

    fun getWeeklyAnalytics() = workoutRepository.getWeeklyAnalytics()

    fun getDailyMetrics() = workoutRepository.getDailyMetrics()

    fun generateCsvExport(): String {
        val list = workoutRepository.workouts.value
        val sb = StringBuilder()
        sb.append("Workout ID,Exercise,Date,Duration (s),Total Reps,Correct,Partial,Fast,Incorrect,Accuracy %,Peak Angle (°),Min Angle (°),ROM (°),Upper Arm Stability %,Avg HR,Calories\n")
        list.forEach { w ->
            sb.append("${w.id},${w.exerciseType.name},${w.startTime},${w.duration},${w.totalReps},${w.correctReps},${w.partialReps},${w.fastReps},${w.incorrectReps},${w.formAccuracy},${w.peakElbowAngle},${w.minElbowAngle},${w.rangeOfMotion},${w.upperArmStability},${w.averageHeartRate},${w.estimatedCalories}\n")
        }
        return sb.toString()
    }
}

class DeviceViewModel(
    private val bleManager: BleManager = BleManager.instance
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val connectedDevice: StateFlow<Device?> = bleManager.connectedDevice
    val availableDevices: StateFlow<List<Device>> = bleManager.availableDevices
    val lastDataReceivedTime: StateFlow<Long> = bleManager.lastDataReceivedTime
    val calibrationStatus: StateFlow<String> = bleManager.calibrationStatus

    fun startScan() = bleManager.startScan()
    fun connect(device: Device) = bleManager.connect(device)
    fun disconnect() = bleManager.disconnect()
    fun calibrateSensors() = bleManager.calibrateSensors()
    fun testVibration() = bleManager.triggerVibration("SHORT")
    fun toggleSimulationMode(enabled: Boolean) {
        bleManager.isSimulationMode = enabled
    }
}

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository.instance
) : ViewModel() {

    val user: StateFlow<User> = userRepository.currentUser

    fun updateProfile(name: String, age: Int, gender: String, height: Int, weight: Float, goal: String) {
        userRepository.updateProfile(
            User(
                name = name,
                age = age,
                gender = gender,
                height = height,
                weight = weight,
                fitnessGoal = goal
            )
        )
    }
}
