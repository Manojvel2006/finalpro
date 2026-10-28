package com.repsense.app.data.repository

import com.repsense.app.data.model.ExerciseType
import com.repsense.app.data.model.User
import com.repsense.app.data.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AnalyticsSummary(
    val totalWorkouts: Int,
    val totalReps: Int,
    val totalDuration: Int,
    val totalCalories: Int,
    val averageFormAccuracy: Float,
    val averageRangeOfMotion: Float,
    val averageUpperArmStability: Float
)

data class WeeklyAnalyticsItem(
    val week: String,
    val totalReps: Int,
    val totalCalories: Int,
    val totalDuration: Int,
    val workoutCount: Int,
    val averageFormAccuracy: Float,
    val averageROM: Float
)

data class DailyMetric(
    val dateLabel: String,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val formAccuracy: Float,
    val rangeOfMotion: Float
)

class WorkoutRepository private constructor() {

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    private val _weeklyRepGoal = MutableStateFlow(80)
    val weeklyRepGoal: StateFlow<Int> = _weeklyRepGoal.asStateFlow()

    init {
        seedInitialWorkouts()
    }

    private fun seedInitialWorkouts() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val initialList = listOf(
            Workout(
                id = UUID.randomUUID().toString(),
                exerciseType = ExerciseType.BICEP_CURL,
                startTime = now - (day * 0) - 3_600_000L,
                endTime = now - (day * 0) - 3_600_000L + 720_000L,
                duration = 720,
                totalReps = 36,
                correctReps = 29,
                partialReps = 4,
                fastReps = 2,
                incorrectReps = 1,
                formAccuracy = 80.5f,
                averageHeartRate = 124,
                maxHeartRate = 148,
                averageSpO2 = 98,
                estimatedCalories = 108,
                peakElbowAngle = 138f,
                minElbowAngle = 24f,
                rangeOfMotion = 114f,
                upperArmStability = 94f,
                avgAngularVelocity = 68f
            ),
            Workout(
                id = UUID.randomUUID().toString(),
                exerciseType = ExerciseType.BICEP_CURL,
                startTime = now - (day * 1) - 7_200_000L,
                endTime = now - (day * 1) - 7_200_000L + 900_000L,
                duration = 900,
                totalReps = 45,
                correctReps = 39,
                partialReps = 3,
                fastReps = 2,
                incorrectReps = 1,
                formAccuracy = 86.7f,
                averageHeartRate = 128,
                maxHeartRate = 152,
                averageSpO2 = 98,
                estimatedCalories = 135,
                peakElbowAngle = 142f,
                minElbowAngle = 22f,
                rangeOfMotion = 120f,
                upperArmStability = 96f,
                avgAngularVelocity = 72f
            ),
            Workout(
                id = UUID.randomUUID().toString(),
                exerciseType = ExerciseType.SHOULDER_PRESS,
                startTime = now - (day * 2) - 10_000_000L,
                endTime = now - (day * 2) - 10_000_000L + 600_000L,
                duration = 600,
                totalReps = 30,
                correctReps = 24,
                partialReps = 3,
                fastReps = 2,
                incorrectReps = 1,
                formAccuracy = 80.0f,
                averageHeartRate = 132,
                maxHeartRate = 158,
                averageSpO2 = 97,
                estimatedCalories = 95,
                peakElbowAngle = 168f,
                minElbowAngle = 72f,
                rangeOfMotion = 96f,
                upperArmStability = 88f,
                avgAngularVelocity = 54f
            ),
            Workout(
                id = UUID.randomUUID().toString(),
                exerciseType = ExerciseType.SQUAT,
                startTime = now - (day * 4) - 15_000_000L,
                endTime = now - (day * 4) - 15_000_000L + 1200_000L,
                duration = 1200,
                totalReps = 50,
                correctReps = 44,
                partialReps = 4,
                fastReps = 1,
                incorrectReps = 1,
                formAccuracy = 88.0f,
                averageHeartRate = 142,
                maxHeartRate = 166,
                averageSpO2 = 98,
                estimatedCalories = 190,
                peakElbowAngle = 90f,
                minElbowAngle = 30f,
                rangeOfMotion = 60f,
                upperArmStability = 95f,
                avgAngularVelocity = 40f
            ),
            Workout(
                id = UUID.randomUUID().toString(),
                exerciseType = ExerciseType.BICEP_CURL,
                startTime = now - (day * 6) - 5_000_000L,
                endTime = now - (day * 6) - 5_000_000L + 850_000L,
                duration = 850,
                totalReps = 40,
                correctReps = 35,
                partialReps = 3,
                fastReps = 1,
                incorrectReps = 1,
                formAccuracy = 87.5f,
                averageHeartRate = 125,
                maxHeartRate = 150,
                averageSpO2 = 98,
                estimatedCalories = 125,
                peakElbowAngle = 139f,
                minElbowAngle = 23f,
                rangeOfMotion = 116f,
                upperArmStability = 93f,
                avgAngularVelocity = 69f
            )
        )
        _workouts.value = initialList
    }

    fun addWorkout(workout: Workout) {
        val current = _workouts.value.toMutableList()
        current.add(0, workout)
        _workouts.value = current
    }

    fun deleteWorkout(workoutId: String) {
        _workouts.value = _workouts.value.filter { it.id != workoutId }
    }

    fun getWorkoutById(id: String): Workout? {
        return _workouts.value.find { it.id == id }
    }

    fun setWeeklyGoal(goal: Int) {
        _weeklyRepGoal.value = goal
    }

    fun getSummary(): AnalyticsSummary {
        val list = _workouts.value
        if (list.isEmpty()) {
            return AnalyticsSummary(0, 0, 0, 0, 0f, 0f, 0f)
        }
        val totalWorkouts = list.size
        val totalReps = list.sumOf { it.totalReps }
        val totalDuration = list.sumOf { it.duration }
        val totalCalories = list.sumOf { it.estimatedCalories }
        val avgAccuracy = list.map { it.formAccuracy }.average().toFloat()
        val avgROM = list.map { it.rangeOfMotion }.average().toFloat()
        val avgStability = list.map { it.upperArmStability }.average().toFloat()

        return AnalyticsSummary(
            totalWorkouts = totalWorkouts,
            totalReps = totalReps,
            totalDuration = totalDuration,
            totalCalories = totalCalories,
            averageFormAccuracy = avgAccuracy,
            averageRangeOfMotion = avgROM,
            averageUpperArmStability = avgStability
        )
    }

    fun getWeeklyAnalytics(): List<WeeklyAnalyticsItem> {
        val list = _workouts.value
        return listOf(
            WeeklyAnalyticsItem("W1", 45, 135, 900, 1, 86.7f, 120f),
            WeeklyAnalyticsItem("W2", 50, 190, 1200, 1, 88.0f, 60f),
            WeeklyAnalyticsItem("W3", 40, 125, 850, 1, 87.5f, 116f),
            WeeklyAnalyticsItem(
                "W4",
                list.sumOf { it.totalReps },
                list.sumOf { it.estimatedCalories },
                list.sumOf { it.duration },
                list.size,
                85.2f,
                115f
            )
        )
    }

    fun getDailyMetrics(): List<DailyMetric> {
        return listOf(
            DailyMetric("Mon", 122, 145, 84f, 112f),
            DailyMetric("Tue", 128, 152, 87f, 118f),
            DailyMetric("Wed", 132, 158, 80f, 96f),
            DailyMetric("Thu", 118, 140, 89f, 115f),
            DailyMetric("Fri", 142, 166, 88f, 60f),
            DailyMetric("Sat", 125, 150, 88f, 114f),
            DailyMetric("Sun", 124, 148, 81f, 114f)
        )
    }

    companion object {
        val instance: WorkoutRepository by lazy { WorkoutRepository() }
    }
}

class UserRepository private constructor() {
    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    fun updateProfile(user: User) {
        _currentUser.value = user
    }

    companion object {
        val instance: UserRepository by lazy { UserRepository() }
    }
}
