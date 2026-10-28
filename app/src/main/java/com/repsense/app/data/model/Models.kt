package com.repsense.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseType(val displayName: String, val icon: String, val difficulty: String, val defaultActive: Boolean) {
    BICEP_CURL("Bicep Curl", "fitness", "Beginner", true),
    SQUAT("Squat", "body", "Beginner", false),
    PUSH_UP("Push-up", "fitness", "Beginner", false),
    SHOULDER_PRESS("Shoulder Press", "fitness", "Intermediate", false),
    LATERAL_RAISE("Lateral Raise", "fitness", "Beginner", false),
    TRICEP_EXTENSION("Tricep Extension", "fitness", "Beginner", false);

    val muscleGroups: List<String>
        get() = when (this) {
            BICEP_CURL -> listOf("Biceps", "Forearms")
            SQUAT -> listOf("Quadriceps", "Glutes", "Hamstrings")
            PUSH_UP -> listOf("Chest", "Shoulders", "Triceps")
            SHOULDER_PRESS -> listOf("Shoulders", "Triceps", "Upper Chest")
            LATERAL_RAISE -> listOf("Shoulders", "Deltoids")
            TRICEP_EXTENSION -> listOf("Triceps", "Arms")
        }

    val formCues: List<String>
        get() = when (this) {
            BICEP_CURL -> listOf("Keep elbows stationary", "Don't swing upper arm", "Reach 130°+ flexion", "Full extension at bottom")
            SQUAT -> listOf("Knees don't cave inward", "Weight in heels", "Chest proud", "Hip crease below knee")
            PUSH_UP -> listOf("Elbows at 45 degrees", "Core engaged", "Full range of motion", "Don't sag hips")
            SHOULDER_PRESS -> listOf("Don't arch lower back", "Press straight up", "Lock out at top", "Control descent")
            LATERAL_RAISE -> listOf("Slight bend in elbows", "Lead with elbows", "Don't shrug shoulders", "Control both phases")
            TRICEP_EXTENSION -> listOf("Keep elbows close to head", "Don't flare elbows", "Full extension at top", "Control negative")
        }
}

@Serializable
enum class FormStatus(val label: String, val feedbackMessage: String) {
    CORRECT_CURL("Correct Form", "Great job! Clean rep through full range of motion."),
    PARTIAL_CURL("Incomplete ROM", "Incomplete curl. Flex past 120° at the top."),
    FAST_CURL("Fast Tempo", "Slow down! Control the 2-second eccentric lowering."),
    INCORRECT_FORM("Excessive Arm Swing", "Elbow drift detected! Pin your upper arm to your ribs."),
    NO_EXERCISE("Waiting", "Waiting for movement...");
}

@Serializable
data class SensorData(
    val timestamp: Long = System.currentTimeMillis(),
    val sequence: Long = 0L,

    // Upper-Arm MPU6050
    val upperArmGyroX: Float = 0f, // °/s
    val upperArmGyroY: Float = 0f, // °/s
    val upperArmGyroZ: Float = 0f, // °/s
    val upperArmAccelX: Float = 0f,
    val upperArmAccelY: Float = 0f,
    val upperArmAccelZ: Float = 9.81f,
    val upperArmAngle: Float = 0f, // degrees
    val upperArmAngleValid: Boolean = true,

    // Forearm MPU6050
    val forearmGyroX: Float = 0f, // °/s
    val forearmGyroY: Float = 0f, // °/s
    val forearmGyroZ: Float = 0f, // °/s
    val forearmAccelX: Float = 0f,
    val forearmAccelY: Float = 0f,
    val forearmAccelZ: Float = 9.81f,
    val forearmAngle: Float = 0f, // degrees
    val forearmAngleValid: Boolean = true,

    // Relative Elbow Biomechanics
    val elbowAngle: Float = 25f, // degrees (0° = straight arm extension, 145° = peak flexion)
    val angleValid: Boolean = true,
    val peakElbowAngle: Float = 25f,
    val rangeOfMotion: Float = 0f,

    // MAX30102 Physiological Telemetry
    val heartRate: Int = 80,
    val heartRateValid: Boolean = true,
    val spo2: Int = 98,
    val spo2Valid: Boolean = true,
    val sensorQuality: String = "GOOD",

    // Session status
    val reps: Int = 0,
    val form: FormStatus = FormStatus.NO_EXERCISE,
    val exercise: ExerciseType = ExerciseType.BICEP_CURL,
    val calibrationStatus: String = "calibrated", // "uncalibrated", "calibrating", "calibrated"

    // Backward-compatibility aliases
    val ax: Float = upperArmAccelX,
    val ay: Float = upperArmAccelY,
    val az: Float = upperArmAccelZ,
    val gx: Float = upperArmGyroX,
    val gy: Float = upperArmGyroY,
    val gz: Float = upperArmGyroZ
)

@Serializable
data class Workout(
    val id: String,
    val userId: String = "user_1",
    val exerciseType: ExerciseType,
    val startTime: Long,
    val endTime: Long,
    val duration: Int, // seconds
    val totalReps: Int,
    val correctReps: Int,
    val partialReps: Int,
    val fastReps: Int,
    val incorrectReps: Int,
    val formAccuracy: Float, // percentage e.g. 85.5f
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val averageSpO2: Int,
    val estimatedCalories: Int,
    val peakElbowAngle: Float = 138f, // degrees
    val minElbowAngle: Float = 22f, // degrees
    val rangeOfMotion: Float = 116f, // degrees (peak - min)
    val upperArmStability: Float = 92f, // % time upper arm remained within +/- 15°
    val avgAngularVelocity: Float = 64f, // °/s
    val synced: Boolean = true
)

@Serializable
data class User(
    val id: String = "user_1",
    val name: String = "Alex Morgan",
    val email: String = "alex.morgan@repsense.fit",
    val age: Int = 26,
    val gender: String = "Male",
    val height: Int = 178, // cm
    val weight: Float = 74.5f, // kg
    val fitnessGoal: String = "Build upper-body strength and improve bicep curl form consistency."
)

@Serializable
data class Device(
    val id: String = "mock-repsense-001",
    val name: String = "RepSense Band Pro",
    val batteryLevel: Int = 88,
    val signalStrength: Int = -42,
    val isConnected: Boolean = true,
    val lastConnectedAt: Long = System.currentTimeMillis(),
    val isHardwareDevice: Boolean = false,
    val firmwareVersion: String = "ESP32-v2.1-DualIMU"
)
