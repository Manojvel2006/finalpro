package com.repsense.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repsense.app.data.model.Workout
import com.repsense.app.ui.components.*
import com.repsense.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workout: Workout?,
    onBackClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (workout == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Slate950),
            contentAlignment = Alignment.Center
        ) {
            Text("Workout session not found.", color = Slate400)
        }
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(workout.startTime))
    val mins = workout.duration / 60
    val secs = workout.duration % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = workout.exerciseType.displayName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_workout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timestamp header card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailHeaderItem("Reps", "${workout.totalReps}", PrimaryBlueLight)
                        DetailHeaderItem("Duration", "${mins}m ${secs}s", NeonCyan)
                        DetailHeaderItem("Form", "${workout.formAccuracy.toInt()}%", SuccessGreen)
                        DetailHeaderItem("Calories", "${workout.estimatedCalories} kcal", AmberAccent)
                    }
                }
            }

            // Arm & Elbow Biomechanics Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Elbow Angle & Biomechanics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailHeaderItem("Peak Flexion", "${workout.peakElbowAngle.toInt()}°", PrimaryBlueLight)
                        DetailHeaderItem("Min Extension", "${workout.minElbowAngle.toInt()}°", NeonCyan)
                        DetailHeaderItem("Total ROM", "${workout.rangeOfMotion.toInt()}°", AmberAccent)
                        DetailHeaderItem("Arm Stability", "${workout.upperArmStability.toInt()}%", SuccessGreen)
                    }
                }
            }

            // Simulated Heart Rate Trend
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
                            text = "Heart Rate Progression",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Avg: ${workout.averageHeartRate} • Peak: ${workout.maxHeartRate} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Slate950, RoundedCornerShape(12.dp))
                    ) {
                        val w = size.width
                        val h = size.height

                        drawLine(color = Slate800, start = Offset(0f, h * 0.25f), end = Offset(w, h * 0.25f), strokeWidth = 1f)
                        drawLine(color = Slate800, start = Offset(0f, h * 0.75f), end = Offset(w, h * 0.75f), strokeWidth = 1f)

                        val points = listOf(
                            0.75f, 0.70f, 0.65f, 0.55f, 0.50f, 0.40f, 0.35f, 0.30f, 0.38f, 0.42f, 0.32f, 0.28f, 0.35f, 0.45f
                        )
                        val path = Path()
                        val stepX = w / (points.size - 1)
                        points.forEachIndexed { i, factor ->
                            val x = i * stepX
                            val y = h * factor
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(path = path, color = ErrorRed, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }

            // Quality Breakdown
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Technique & Kinematics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    BreakdownRow(
                        label = "Correct Reps",
                        count = workout.correctReps,
                        total = workout.totalReps,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BreakdownRow(
                        label = "Incomplete ROM",
                        count = workout.partialReps,
                        total = workout.totalReps,
                        color = AmberAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BreakdownRow(
                        label = "Fast Tempo",
                        count = workout.fastReps,
                        total = workout.totalReps,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BreakdownRow(
                        label = "Excessive Arm Swing",
                        count = workout.incorrectReps,
                        total = workout.totalReps,
                        color = ErrorRed
                    )
                }
            }

            // Hardware & Protocol metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Synced",
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ESP32 Dual-MPU6050 Logged • ROM: ${workout.rangeOfMotion.toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("Delete Workout?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete this workout session record?",
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick(workout.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}
