package com.repsense.app.data.ble

import com.repsense.app.data.model.ExerciseType
import com.repsense.app.data.model.FormStatus
import com.repsense.app.data.model.SensorData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.UUID

/**
 * RepSense Hardware-to-Mobile BLE Data Protocol Specification.
 *
 * Configured 128-bit standard GATT UUIDs matching ESP32 firmware:
 * - Service:        0000FFF0-0000-1000-8000-00805F9B34FB
 * - Sensor Data:    0000FFF1-0000-1000-8000-00805F9B34FB (Notify)
 * - Control / Vibe: 0000FFF2-0000-1000-8000-00805F9B34FB (Write / Write Without Response)
 * - Calibration:    0000FFF3-0000-1000-8000-00805F9B34FB (Write / Notify)
 */
object BleProtocol {

    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val SENSOR_DATA_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val CONTROL_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    val CALIBRATION_CHAR_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")

    const val PROTOCOL_VERSION = 1
    const val DEVICE_NAME_PREFIX = "RepSense"

    private val jsonEngine = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses incoming BLE characteristic payload (supports both JSON development format
     * and compact CSV/delimited telemetry format).
     *
     * Validates bounds, timestamps, and sensor ranges:
     * - Gyroscope: -2000.0 to +2000.0 °/s
     * - Angles: 0.0° to 180.0°
     * - Heart Rate: 35 to 220 BPM
     * - SpO2: 70% to 100%
     */
    fun parseSensorPacket(rawBytes: ByteArray): SensorData? {
        if (rawBytes.isEmpty()) return null
        val rawString = try {
            String(rawBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            return null
        }

        return if (rawString.startsWith("{") && rawString.endsWith("}")) {
            parseJsonPayload(rawString)
        } else if (rawString.contains(",")) {
            parseDelimitedPayload(rawString)
        } else {
            null
        }
    }

    private fun parseJsonPayload(jsonStr: String): SensorData? {
        return try {
            val root = jsonEngine.parseToJsonElement(jsonStr).jsonObject

            fun optDouble(key: String, default: Double): Double =
                root[key]?.jsonPrimitive?.doubleOrNull ?: default

            fun optLong(key: String, default: Long): Long =
                root[key]?.jsonPrimitive?.longOrNull ?: default

            fun optInt(key: String, default: Int): Int =
                root[key]?.jsonPrimitive?.intOrNull ?: default

            fun optBoolean(key: String, default: Boolean): Boolean =
                root[key]?.jsonPrimitive?.booleanOrNull ?: default

            fun optString(key: String, default: String): String =
                root[key]?.jsonPrimitive?.contentOrNull ?: default

            val seq = optLong("sequence", System.currentTimeMillis())
            val ts = optLong("timestamp", System.currentTimeMillis())

            // Upper arm MPU6050
            val uaGx = optDouble("upperArmGyroX", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val uaGy = optDouble("upperArmGyroY", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val uaGz = optDouble("upperArmGyroZ", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val uaAx = optDouble("upperArmAccelX", 0.0).toFloat().coerceIn(-16f, 16f)
            val uaAy = optDouble("upperArmAccelY", 0.0).toFloat().coerceIn(-16f, 16f)
            val uaAz = optDouble("upperArmAccelZ", 9.81).toFloat().coerceIn(-16f, 16f)

            // Forearm MPU6050
            val faGx = optDouble("forearmGyroX", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val faGy = optDouble("forearmGyroY", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val faGz = optDouble("forearmGyroZ", 0.0).toFloat().coerceIn(-2000f, 2000f)
            val faAx = optDouble("forearmAccelX", 0.0).toFloat().coerceIn(-16f, 16f)
            val faAy = optDouble("forearmAccelY", 0.0).toFloat().coerceIn(-16f, 16f)
            val faAz = optDouble("forearmAccelZ", 9.81).toFloat().coerceIn(-16f, 16f)

            // Angles
            val uaAngle = optDouble("upperArmAngle", 0.0).toFloat().coerceIn(-180f, 180f)
            val faAngle = optDouble("forearmAngle", 0.0).toFloat().coerceIn(-180f, 180f)
            val elbowAngle = optDouble("elbowAngle", 0.0).toFloat().coerceIn(0f, 180f)
            val angleValid = optBoolean("angleValid", true)
            val uaAngleValid = optBoolean("upperArmAngleValid", angleValid)
            val faAngleValid = optBoolean("forearmAngleValid", angleValid)

            // MAX30102
            val hr = optInt("heartRate", 80).coerceIn(35, 220)
            val spo2 = optInt("spo2", 98).coerceIn(70, 100)
            val hrValid = optBoolean("heartRateValid", true)
            val spo2Valid = optBoolean("spo2Valid", true)
            val quality = optString("sensorQuality", "GOOD")

            // Exercise & Reps
            val reps = if (root.containsKey("repCount")) optInt("repCount", 0) else optInt("reps", 0)
            val formStr = optString("form", "NO_EXERCISE")
            val form = try {
                FormStatus.valueOf(formStr)
            } catch (e: Exception) {
                FormStatus.NO_EXERCISE
            }

            val exStr = optString("exercise", "BICEP_CURL")
            val exercise = try {
                ExerciseType.valueOf(exStr.uppercase())
            } catch (e: Exception) {
                ExerciseType.BICEP_CURL
            }

            val calibStatus = optString("calibrationStatus", "calibrated")

            SensorData(
                timestamp = ts,
                sequence = seq,
                upperArmGyroX = uaGx,
                upperArmGyroY = uaGy,
                upperArmGyroZ = uaGz,
                upperArmAccelX = uaAx,
                upperArmAccelY = uaAy,
                upperArmAccelZ = uaAz,
                upperArmAngle = uaAngle,
                upperArmAngleValid = uaAngleValid,
                forearmGyroX = faGx,
                forearmGyroY = faGy,
                forearmGyroZ = faGz,
                forearmAccelX = faAx,
                forearmAccelY = faAy,
                forearmAccelZ = faAz,
                forearmAngle = faAngle,
                forearmAngleValid = faAngleValid,
                elbowAngle = elbowAngle,
                angleValid = angleValid,
                heartRate = hr,
                heartRateValid = hrValid,
                spo2 = spo2,
                spo2Valid = spo2Valid,
                sensorQuality = quality,
                reps = reps,
                form = form,
                exercise = exercise,
                calibrationStatus = calibStatus,
                ax = uaAx,
                ay = uaAy,
                az = uaAz,
                gx = uaGx,
                gy = uaGy,
                gz = uaGz
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compact CSV format:
     * seq,uaGx,uaGy,uaGz,faGx,faGy,faGz,uaAng,faAng,elbAng,hr,spo2,reps,form
     */
    private fun parseDelimitedPayload(line: String): SensorData? {
        val parts = line.split(",").map { it.trim() }
        if (parts.size < 13) return null

        return try {
            val seq = parts[0].toLongOrNull() ?: 0L
            val uaGx = parts[1].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val uaGy = parts[2].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val uaGz = parts[3].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val faGx = parts[4].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val faGy = parts[5].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val faGz = parts[6].toFloatOrNull()?.coerceIn(-2000f, 2000f) ?: 0f
            val uaAngle = parts[7].toFloatOrNull()?.coerceIn(-180f, 180f) ?: 0f
            val faAngle = parts[8].toFloatOrNull()?.coerceIn(-180f, 180f) ?: 0f
            val elbowAngle = parts[9].toFloatOrNull()?.coerceIn(0f, 180f) ?: 0f
            val hr = parts[10].toIntOrNull()?.coerceIn(35, 220) ?: 80
            val spo2 = parts[11].toIntOrNull()?.coerceIn(70, 100) ?: 98
            val reps = parts[12].toIntOrNull() ?: 0
            val formStr = if (parts.size > 13) parts[13] else "CORRECT_CURL"
            val form = try { FormStatus.valueOf(formStr) } catch (e: Exception) { FormStatus.NO_EXERCISE }

            SensorData(
                timestamp = System.currentTimeMillis(),
                sequence = seq,
                upperArmGyroX = uaGx,
                upperArmGyroY = uaGy,
                upperArmGyroZ = uaGz,
                forearmGyroX = faGx,
                forearmGyroY = faGy,
                forearmGyroZ = faGz,
                upperArmAngle = uaAngle,
                forearmAngle = faAngle,
                elbowAngle = elbowAngle,
                angleValid = true,
                heartRate = hr,
                heartRateValid = true,
                spo2 = spo2,
                spo2Valid = true,
                reps = reps,
                form = form,
                exercise = ExerciseType.BICEP_CURL,
                calibrationStatus = "calibrated"
            )
        } catch (e: Exception) {
            null
        }
    }

    fun createVibrationCommand(pattern: String): ByteArray {
        val payload = "CMD:VIBE:$pattern"
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun createCalibrationCommand(): ByteArray {
        val payload = "CMD:CALIBRATE:ZERO"
        return payload.toByteArray(Charsets.UTF_8)
    }
}
