package com.repsense.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repsense.app.data.model.ExerciseType
import com.repsense.app.data.model.FormStatus
import com.repsense.app.data.model.Workout
import com.repsense.app.ui.components.*
import com.repsense.app.ui.theme.*
import com.repsense.app.ui.viewmodel.LiveWorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveWorkoutScreen(
    exerciseType: ExerciseType,
    viewModel: LiveWorkoutViewModel,
    onWorkoutFinished: (Workout) -> Unit,
    onExitWithoutSave: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseType) {
        if (!uiState.isWorkoutActive) {
            viewModel.startWorkout(exerciseType)
        }
    }

    val formColor by animateColorAsState(
        targetValue = when (uiState.formStatus) {
            FormStatus.CORRECT_CURL -> SuccessGreen
            FormStatus.PARTIAL_CURL -> AmberAccent
            FormStatus.FAST_CURL -> NeonCyan
            FormStatus.INCORRECT_FORM -> ErrorRed
            FormStatus.NO_EXERCISE -> Slate400
        },
        animationSpec = tween(300),
        label = "formColor"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Exercise Title, Status & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = uiState.exercise.displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isPaused) AmberAccent else SuccessGreen)
                        )
                        Text(
                            text = if (uiState.isPaused) "PAUSED" else "ESP32 TELEMETRY (20Hz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isPaused) AmberAccent else NeonCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Voice Guidance Toggle
                    IconButton(
                        onClick = { viewModel.toggleVoiceGuidance() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isVoiceGuidanceEnabled) PrimaryBlue.copy(alpha = 0.2f) else Slate900)
                            .border(1.dp, if (uiState.isVoiceGuidanceEnabled) PrimaryBlueLight else Slate800, CircleShape)
                            .testTag("voice_guidance_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceGuidanceEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                            contentDescription = "Voice Guidance",
                            tint = if (uiState.isVoiceGuidanceEnabled) PrimaryBlueLight else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Calibrate Zero Button
                    IconButton(
                        onClick = { viewModel.calibrateSensors() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Slate900)
                            .border(1.dp, Slate800, CircleShape)
                            .testTag("calibrate_sensors_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Calibrate Sensor",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pause / Resume
                    IconButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Slate900)
                            .border(1.dp, Slate800, CircleShape)
                            .testTag("pause_workout_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Stop / End Workout
                    IconButton(
                        onClick = { showEndDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.2f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.5f), CircleShape)
                            .testTag("end_workout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "End Workout",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Giant Rep Counter Hero Card with Live Coaching Form Cue
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REPETITIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${uiState.reps}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 84.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Form Status Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = formColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, formColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(formColor)
                            )
                            Text(
                                text = uiState.formStatus.label.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = formColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = uiState.feedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Real-Time Arm & Elbow Angle Gauges
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Real-Time Angle & ROM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sagittal Plane (${uiState.calibrationStatus})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Arc Angle Gauge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Visual Gauge for Relative Elbow Angle
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val animatedAngle by animateFloatAsState(
                                targetValue = uiState.elbowAngle.coerceIn(0f, 180f),
                                animationSpec = tween(120),
                                label = "gaugeAngle"
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                val arcSize = size.minDimension - strokeWidth
                                val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)

                                // Background track (180 deg)
                                drawArc(
                                    color = Slate800,
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Current Elbow angle arc
                                val sweep = (animatedAngle / 180f) * 180f
                                drawArc(
                                    color = if (animatedAngle >= 120f) SuccessGreen else if (animatedAngle >= 75f) PrimaryBlueLight else AmberAccent,
                                    startAngle = 180f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.elbowAngle.toInt()}°",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Elbow Angle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }

                        // Angle Breakdown metrics
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AngleValueRow(label = "Upper-Arm Angle", value = "${uiState.upperArmAngle.toInt()}°", tint = if (kotlin.math.abs(uiState.upperArmAngle) > 15f) ErrorRed else SuccessGreen)
                            AngleValueRow(label = "Forearm Angle", value = "${uiState.forearmAngle.toInt()}°", tint = NeonCyan)
                            AngleValueRow(label = "Peak Flexion", value = "${uiState.peakElbowAngle.toInt()}°", tint = PrimaryBlueLight)
                            AngleValueRow(label = "Range of Motion", value = "${uiState.rangeOfMotion.toInt()}°", tint = AmberAccent)
                            AngleValueRow(label = "Arm Stability", value = "${uiState.upperArmStability.toInt()}%", tint = SuccessGreen)
                        }
                    }
                }
            }

            // Real-Time Dual MPU6050 Gyroscope Monitor (°/s)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dual MPU6050 Gyroscopes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Angular Velocity (°/s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonLime
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val sensor = uiState.latestSensorData

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Upper-Arm Sensor Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate950)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Upper-Arm Sensor",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlueLight
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                GyroAxisRow("Gx", "${sensor?.upperArmGyroX?.toInt() ?: 0} °/s")
                                GyroAxisRow("Gy", "${sensor?.upperArmGyroY?.toInt() ?: 0} °/s")
                                GyroAxisRow("Gz", "${sensor?.upperArmGyroZ?.toInt() ?: 0} °/s")
                            }
                        }

                        // Forearm Sensor Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate950)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Forearm Sensor",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                GyroAxisRow("Gx", "${sensor?.forearmGyroX?.toInt() ?: 0} °/s")
                                GyroAxisRow("Gy", "${sensor?.forearmGyroY?.toInt() ?: 0} °/s")
                                GyroAxisRow("Gz", "${sensor?.forearmGyroZ?.toInt() ?: 0} °/s")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gyro Waveform Oscilloscope Canvas
                    val wave = uiState.recentAccelWave
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Slate950, RoundedCornerShape(10.dp))
                    ) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f

                        drawLine(
                            color = Slate800,
                            start = Offset(0f, midY),
                            end = Offset(width, midY),
                            strokeWidth = 1.dp.toPx()
                        )

                        if (wave.size > 1) {
                            val path = Path()
                            val stepX = width / (wave.size - 1)
                            wave.forEachIndexed { index, gz ->
                                val x = index * stepX
                                val y = (midY - (gz * 0.25f)).coerceIn(4f, height - 4f)
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = NeonCyan,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            // MAX30102 Physiological Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VitalTileWithValidity(
                    modifier = Modifier.weight(1f),
                    title = "Heart Rate",
                    value = "${uiState.heartRate}",
                    unit = "BPM",
                    isValid = uiState.heartRateValid,
                    icon = Icons.Default.Favorite,
                    tint = ErrorRed
                )
                VitalTileWithValidity(
                    modifier = Modifier.weight(1f),
                    title = "SpO₂",
                    value = "${uiState.spo2}",
                    unit = "%",
                    isValid = uiState.spo2Valid,
                    icon = Icons.Default.WaterDrop,
                    tint = NeonCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val mins = uiState.durationSeconds / 60
                val secs = uiState.durationSeconds % 60
                val formattedTime = "%02d:%02d".format(mins, secs)
                VitalTile(
                    modifier = Modifier.weight(1f),
                    title = "Duration",
                    value = formattedTime,
                    unit = "TIME",
                    icon = Icons.Default.Timer,
                    tint = PrimaryBlueLight
                )
                VitalTile(
                    modifier = Modifier.weight(1f),
                    title = "Burned",
                    value = "${uiState.calories.toInt()}",
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = AmberAccent
                )
            }

            // 4-Part Rep Breakdown
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rep Quality & Form Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RepBadgeColumn(title = "Correct", count = uiState.correctReps, color = SuccessGreen)
                        RepBadgeColumn(title = "Partial", count = uiState.partialReps, color = AmberAccent)
                        RepBadgeColumn(title = "Fast", count = uiState.fastReps, color = NeonCyan)
                        RepBadgeColumn(title = "Incorrect", count = uiState.incorrectReps, color = ErrorRed)
                    }
                }
            }

            // Manual Rep Injection button (for testing & simulation)
            OutlinedButton(
                onClick = { viewModel.manualRep(FormStatus.CORRECT_CURL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manual_rep_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Inject Rep",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Count Repetition (ESP32 Sensor Test)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = {
                Text(text = "End Workout?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Your kinematic data, peak elbow angle (${uiState.peakElbowAngle.toInt()}°), and rep breakdown will be saved to your workout log.",
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndDialog = false
                        val saved = viewModel.endWorkout()
                        if (saved != null) {
                            onWorkoutFinished(saved)
                        } else {
                            onExitWithoutSave()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Finish & View Summary", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Continue Workout", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun AngleValueRow(label: String, value: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall, color = Slate400)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
fun GyroAxisRow(axis: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = axis, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun VitalTileWithValidity(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    isValid: Boolean,
    icon: ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isValid) SuccessGreen.copy(alpha = 0.15f) else AmberAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isValid) "VALID" else "STALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isValid) SuccessGreen else AmberAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isValid) Color.White else Slate400
            )
            Text(
                text = "$title ($unit)",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}
