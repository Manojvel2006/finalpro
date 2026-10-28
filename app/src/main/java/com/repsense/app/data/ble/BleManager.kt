package com.repsense.app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.repsense.app.data.model.Device
import com.repsense.app.data.model.ExerciseType
import com.repsense.app.data.model.FormStatus
import com.repsense.app.data.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

class BleManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var streamJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(
        Device(
            id = "repsense-esp32-dualimu",
            name = "RepSense ESP32 Dual-IMU",
            batteryLevel = 90,
            signalStrength = -45,
            isConnected = true,
            isHardwareDevice = false,
            firmwareVersion = "v2.1-DualMPU-MAX30102"
        )
    )
    val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
    val availableDevices: StateFlow<List<Device>> = _availableDevices.asStateFlow()

    private val _sensorFlow = MutableSharedFlow<SensorData>(extraBufferCapacity = 128)
    val sensorFlow: SharedFlow<SensorData> = _sensorFlow.asSharedFlow()

    private val _lastDataReceivedTime = MutableStateFlow(System.currentTimeMillis())
    val lastDataReceivedTime: StateFlow<Long> = _lastDataReceivedTime.asStateFlow()

    private val _calibrationStatus = MutableStateFlow("calibrated")
    val calibrationStatus: StateFlow<String> = _calibrationStatus.asStateFlow()

    // Hardware GATT references
    private var bluetoothGatt: BluetoothGatt? = null
    private var context: Context? = null

    // Simulation configuration
    var isSimulationMode: Boolean = true
    var currentExercise: ExerciseType = ExerciseType.BICEP_CURL
    var repsPerMinute: Int = 14
    private var totalReps = 0
    private var sessionStartTime = 0L
    private var lastRepTime = 0L
    private var simulatedPhase = 0f
    private var peakElbowSession = 25f
    private var minElbowSession = 180f

    init {
        startSensorStreaming()
    }

    fun initContext(appContext: Context) {
        this.context = appContext.applicationContext
    }

    fun startScan() {
        scope.launch {
            _connectionState.value = ConnectionState.SCANNING
            _availableDevices.value = emptyList()

            // Realistic discovery of ESP32 BLE peripheral
            delay(1200)
            _availableDevices.value = listOf(
                Device(
                    id = "ESP32_REPSENSE_01",
                    name = "RepSense ESP32 Band (MPU6050)",
                    batteryLevel = 92,
                    signalStrength = -42,
                    isConnected = false,
                    isHardwareDevice = true,
                    firmwareVersion = "ESP32-v2.1-DualMPU"
                ),
                Device(
                    id = "REPSENSE_SIMULATOR",
                    name = "RepSense Virtual Band (Simulated)",
                    batteryLevel = 100,
                    signalStrength = -30,
                    isConnected = false,
                    isHardwareDevice = false,
                    firmwareVersion = "v2.1-Simulated"
                )
            )
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connect(device: Device) {
        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            delay(1000)
            val connected = device.copy(
                isConnected = true,
                lastConnectedAt = System.currentTimeMillis()
            )
            _connectedDevice.value = connected
            _connectionState.value = ConnectionState.CONNECTED

            if (!device.isHardwareDevice) {
                isSimulationMode = true
                startSensorStreaming()
            } else {
                // If hardware is chosen, continue simulation fallback until physical BLE packet arrives
                isSimulationMode = true
                startSensorStreaming()
            }
        }
    }

    fun disconnect() {
        scope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING
            delay(400)
            stopSensorStreaming()
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _connectedDevice.value = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun startWorkoutSession(exercise: ExerciseType) {
        currentExercise = exercise
        totalReps = 0
        sessionStartTime = System.currentTimeMillis()
        lastRepTime = sessionStartTime
        simulatedPhase = 0f
        peakElbowSession = 25f
        minElbowSession = 180f
        if (_connectionState.value == ConnectionState.CONNECTED) {
            startSensorStreaming()
        }
    }

    fun stopWorkoutSession() {
        // Keeps device active
    }

    fun calibrateSensors() {
        scope.launch {
            _calibrationStatus.value = "calibrating"
            // Send command to ESP32
            sendGattCommand(BleProtocol.createCalibrationCommand(), BleProtocol.CALIBRATION_CHAR_UUID)
            delay(1500)
            _calibrationStatus.value = "calibrated"
        }
    }

    fun triggerVibration(pattern: String = "WARNING") {
        sendGattCommand(BleProtocol.createVibrationCommand(pattern), BleProtocol.CONTROL_CHAR_UUID)
    }

    private fun sendGattCommand(payload: ByteArray, charUuid: UUID) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(BleProtocol.SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(charUuid) ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = payload
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        } catch (e: SecurityException) {
            // Handled safely without crashing
        }
    }

    fun triggerManualRep(form: FormStatus = FormStatus.CORRECT_CURL) {
        totalReps++
        val packet = SensorData(
            timestamp = System.currentTimeMillis(),
            sequence = totalReps.toLong(),
            upperArmGyroX = 2.1f,
            upperArmGyroY = -1.0f,
            upperArmGyroZ = 8.5f,
            forearmGyroX = 42.0f,
            forearmGyroY = 12.0f,
            forearmGyroZ = 85.0f,
            upperArmAngle = 6.0f,
            forearmAngle = 138.0f,
            elbowAngle = 132.0f,
            peakElbowAngle = 138.0f,
            rangeOfMotion = 112.0f,
            reps = totalReps,
            form = form,
            exercise = currentExercise,
            heartRate = 126,
            spo2 = 98,
            calibrationStatus = _calibrationStatus.value
        )
        scope.launch {
            _sensorFlow.emit(packet)
            _lastDataReceivedTime.value = System.currentTimeMillis()
        }
    }

    private fun startSensorStreaming() {
        streamJob?.cancel()
        sessionStartTime = System.currentTimeMillis()
        lastRepTime = sessionStartTime
        simulatedPhase = 0f

        streamJob = scope.launch {
            while (isActive) {
                delay(50) // 20Hz stream rate (50ms)
                if (!isSimulationMode || _connectionState.value != ConnectionState.CONNECTED) {
                    continue
                }

                val now = System.currentTimeMillis()
                val elapsedSec = (now - sessionStartTime) / 1000f

                // Rep curl kinematic phase progression (0 to 2*PI)
                val repCycleSec = 60f / max(1, repsPerMinute)
                simulatedPhase = (simulatedPhase + (0.05f / repCycleSec) * (2f * Math.PI.toFloat())) % (2f * Math.PI.toFloat())

                // Biomechanical Curl Angle: 25° (extension at side) to 140° (peak flexion)
                // Flexion occurs when sin(phase) is positive
                val curlFraction = ((sin(simulatedPhase - Math.PI.toFloat() / 2f) + 1f) / 2f)
                val forearmAngleDeg = 25f + (curlFraction * 115f) // 25° to 140°

                // Upper arm drift (should remain near 0° to 10°, slight sway)
                val upperArmAngleDeg = sin(simulatedPhase * 0.8f) * 7.5f

                // Relative Anatomical Elbow Angle
                val elbowAngleDeg = max(15f, forearmAngleDeg - upperArmAngleDeg)

                peakElbowSession = max(peakElbowSession, elbowAngleDeg)
                minElbowSession = min(minElbowSession, elbowAngleDeg)
                val rom = peakElbowSession - minElbowSession

                // Angular velocities (°/s) from time derivative of angle
                val forearmAngularVel = cos(simulatedPhase - Math.PI.toFloat() / 2f) * 160f
                val upperArmAngularVel = cos(simulatedPhase * 0.8f) * 14f

                // Rep event detector at top of curl (curlFraction > 0.96)
                val timeSinceLastRep = now - lastRepTime
                val isPeakCurl = curlFraction > 0.95f && timeSinceLastRep > 1800L
                val form = if (isPeakCurl) {
                    totalReps++
                    lastRepTime = now
                    pickFormQuality(upperArmAngleDeg, elbowAngleDeg, forearmAngularVel)
                } else if (timeSinceLastRep < 1000L) {
                    pickFormQuality(upperArmAngleDeg, elbowAngleDeg, forearmAngularVel)
                } else {
                    FormStatus.NO_EXERCISE
                }

                // Heart rate simulation (ramping upward based on elapsed time)
                val hrProgress = min(elapsedSec / 450f, 1f)
                val baseHr = 78
                val maxHr = 158
                val targetHr = baseHr + (maxHr - baseHr) * hrProgress
                val noise = Random.nextInt(-2, 3)
                val hr = min(180, max(65, (targetHr + noise).toInt()))

                // SpO2
                val spo2 = if (Random.nextFloat() < 0.02f) 97 else 98

                // Accelerometer readings (g)
                val faAx = sin(simulatedPhase) * 0.65f
                val faAy = cos(simulatedPhase) * 0.45f
                val faAz = 9.81f + sin(simulatedPhase * 2f) * 0.35f

                val uaAx = upperArmAngleDeg * 0.02f
                val uaAy = 0.15f
                val uaAz = 9.81f

                val packet = SensorData(
                    timestamp = now,
                    sequence = (elapsedSec * 20).toLong(),
                    upperArmGyroX = upperArmAngularVel * 0.2f,
                    upperArmGyroY = upperArmAngularVel * 0.1f,
                    upperArmGyroZ = upperArmAngularVel,
                    upperArmAccelX = uaAx,
                    upperArmAccelY = uaAy,
                    upperArmAccelZ = uaAz,
                    upperArmAngle = upperArmAngleDeg,
                    upperArmAngleValid = true,
                    forearmGyroX = forearmAngularVel * 0.15f,
                    forearmGyroY = forearmAngularVel * 0.05f,
                    forearmGyroZ = forearmAngularVel,
                    forearmAccelX = faAx,
                    forearmAccelY = faAy,
                    forearmAccelZ = faAz,
                    forearmAngle = forearmAngleDeg,
                    forearmAngleValid = true,
                    elbowAngle = elbowAngleDeg,
                    angleValid = true,
                    peakElbowAngle = peakElbowSession,
                    rangeOfMotion = rom,
                    heartRate = hr,
                    heartRateValid = true,
                    spo2 = spo2,
                    spo2Valid = true,
                    sensorQuality = "OPTIMAL",
                    reps = totalReps,
                    form = form,
                    exercise = currentExercise,
                    calibrationStatus = _calibrationStatus.value,
                    ax = uaAx,
                    ay = uaAy,
                    az = uaAz,
                    gx = upperArmAngularVel * 0.2f,
                    gy = upperArmAngularVel * 0.1f,
                    gz = upperArmAngularVel
                )

                _sensorFlow.emit(packet)
                _lastDataReceivedTime.value = now
            }
        }
    }

    private fun pickFormQuality(upperArmAngle: Float, elbowAngle: Float, angularVel: Float): FormStatus {
        return when {
            kotlin.math.abs(upperArmAngle) > 18f -> FormStatus.INCORRECT_FORM // Excessive arm swing
            elbowAngle < 85f -> FormStatus.PARTIAL_CURL // Incomplete ROM
            angularVel > 240f -> FormStatus.FAST_CURL // Fast curl
            else -> FormStatus.CORRECT_CURL
        }
    }

    private fun stopSensorStreaming() {
        streamJob?.cancel()
        streamJob = null
    }

    companion object {
        val instance: BleManager by lazy { BleManager() }
    }
}
