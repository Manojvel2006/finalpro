package com.repsense.app

import com.repsense.app.data.ble.BleProtocol
import com.repsense.app.data.model.FormStatus
import org.junit.Assert.*
import org.junit.Test

class BleProtocolTest {

    @Test
    fun testParseJsonSensorPacket() {
        val sampleJson = """
            {
              "version": 1,
              "type": "sensor_update",
              "sequence": 120,
              "timestamp": 1730000000,
              "exercise": "bicep_curl",
              "workoutState": "active",
              "repCount": 6,
              "upperArmGyroX": 2.4,
              "upperArmGyroY": -1.2,
              "upperArmGyroZ": 8.7,
              "forearmGyroX": 4.1,
              "forearmGyroY": 3.5,
              "forearmGyroZ": 21.6,
              "upperArmAngle": 24.0,
              "forearmAngle": 68.0,
              "elbowAngle": 44.0,
              "angleValid": true,
              "calibrationStatus": "complete",
              "heartRate": 92,
              "spo2": 97,
              "heartRateValid": true,
              "spo2Valid": true,
              "form": "CORRECT_CURL"
            }
        """.trimIndent()

        val parsed = BleProtocol.parseSensorPacket(sampleJson.toByteArray(Charsets.UTF_8))
        assertNotNull(parsed)
        parsed?.let {
            assertEquals(120L, it.sequence)
            assertEquals(6, it.reps)
            assertEquals(2.4f, it.upperArmGyroX, 0.01f)
            assertEquals(8.7f, it.upperArmGyroZ, 0.01f)
            assertEquals(21.6f, it.forearmGyroZ, 0.01f)
            assertEquals(24.0f, it.upperArmAngle, 0.01f)
            assertEquals(68.0f, it.forearmAngle, 0.01f)
            assertEquals(44.0f, it.elbowAngle, 0.01f)
            assertTrue(it.angleValid)
            assertEquals(92, it.heartRate)
            assertEquals(97, it.spo2)
            assertTrue(it.heartRateValid)
            assertTrue(it.spo2Valid)
            assertEquals(FormStatus.CORRECT_CURL, it.form)
        }
    }

    @Test
    fun testParseDelimitedPacket() {
        val line = "142,1.2,-0.5,4.0,15.2,8.0,92.0,5.0,75.0,70.0,118,98,8,CORRECT_CURL"
        val parsed = BleProtocol.parseSensorPacket(line.toByteArray(Charsets.UTF_8))
        assertNotNull(parsed)
        parsed?.let {
            assertEquals(142L, it.sequence)
            assertEquals(1.2f, it.upperArmGyroX, 0.01f)
            assertEquals(92.0f, it.forearmGyroZ, 0.01f)
            assertEquals(70.0f, it.elbowAngle, 0.01f)
            assertEquals(118, it.heartRate)
            assertEquals(98, it.spo2)
            assertEquals(8, it.reps)
            assertEquals(FormStatus.CORRECT_CURL, it.form)
        }
    }

    @Test
    fun testBoundaryClamping() {
        val outOfBoundsJson = """
            {
              "sequence": 1,
              "upperArmGyroX": 99999.0,
              "elbowAngle": 350.0,
              "heartRate": 400,
              "spo2": 150
            }
        """.trimIndent()

        val parsed = BleProtocol.parseSensorPacket(outOfBoundsJson.toByteArray(Charsets.UTF_8))
        assertNotNull(parsed)
        parsed?.let {
            assertEquals(2000f, it.upperArmGyroX, 0.01f)
            assertEquals(180f, it.elbowAngle, 0.01f)
            assertEquals(220, it.heartRate)
            assertEquals(100, it.spo2)
        }
    }

    @Test
    fun testVibrationAndCalibrationCommands() {
        val vibe = BleProtocol.createVibrationCommand("WARNING")
        val vibeStr = String(vibe, Charsets.UTF_8)
        assertEquals("CMD:VIBE:WARNING", vibeStr)

        val cal = BleProtocol.createCalibrationCommand()
        val calStr = String(cal, Charsets.UTF_8)
        assertEquals("CMD:CALIBRATE:ZERO", calStr)
    }

    @Test
    fun testMalformedPayload() {
        val malformed = "NOT_A_VALID_PACKET"
        val parsed = BleProtocol.parseSensorPacket(malformed.toByteArray(Charsets.UTF_8))
        assertNull(parsed)
    }
}
