package com.repsense.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repsense.app.data.model.Workout
import com.repsense.app.ui.components.*
import com.repsense.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutSummaryScreen(
    workout: Workout,
    onDoneClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(workout.startTime))
    val mins = workout.duration / 60
    val secs = workout.duration % 60
    val durationText = "${mins}m ${secs}s"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Title with Checkmark
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Workout Complete!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${workout.exerciseType.displayName} • $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            }

            // Primary Stat Cards (Total Reps, Form Accuracy, Duration)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryPrimaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Reps",
                    value = "${workout.totalReps}",
                    subtitle = "completed",
                    tint = PrimaryBlueLight
                )
                SummaryPrimaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Form Accuracy",
                    value = "${workout.formAccuracy.toInt()}%",
                    subtitle = "technique",
                    tint = SuccessGreen
                )
                SummaryPrimaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Duration",
                    value = durationText,
                    subtitle = "elapsed",
                    tint = NeonCyan
                )
            }

            // Biomechanical Angles Card (NEW)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Elbow Biomechanics & Range of Motion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryAngleItem(
                            title = "Peak Flexion",
                            value = "${workout.peakElbowAngle.toInt()}°",
                            subtitle = "target >125°",
                            tint = PrimaryBlueLight
                        )
                        SummaryAngleItem(
                            title = "Range of Motion",
                            value = "${workout.rangeOfMotion.toInt()}°",
                            subtitle = "full travel",
                            tint = NeonCyan
                        )
                        SummaryAngleItem(
                            title = "Upper-Arm Steady",
                            value = "${workout.upperArmStability.toInt()}%",
                            subtitle = "no drift",
                            tint = SuccessGreen
                        )
                    }
                }
            }

            // Rep Breakdown Detail Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Kinematic Form Analysis",
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

            // Biometric Vitals Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MAX30102 Physiological Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VitalsSummaryColumn(
                            title = "Avg Heart Rate",
                            value = "${workout.averageHeartRate} BPM",
                            icon = Icons.Default.Favorite,
                            tint = ErrorRed
                        )
                        VitalsSummaryColumn(
                            title = "Peak Heart Rate",
                            value = "${workout.maxHeartRate} BPM",
                            icon = Icons.Default.Favorite,
                            tint = AmberAccent
                        )
                        VitalsSummaryColumn(
                            title = "Avg SpO₂",
                            value = "${workout.averageSpO2}%",
                            icon = Icons.Default.WaterDrop,
                            tint = NeonCyan
                        )
                    }
                }
            }

            // Calories Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories",
                            tint = AmberAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${workout.estimatedCalories} kcal burned",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Estimated based on duration and heart rate zones",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }

            // Return Button
            Button(
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("summary_done_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = "DONE & RETURN TO DASHBOARD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun SummaryAngleItem(
    title: String,
    value: String,
    subtitle: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = tint)
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Slate400)
    }
}
