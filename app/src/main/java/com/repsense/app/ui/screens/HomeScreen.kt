package com.repsense.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repsense.app.data.ble.ConnectionState
import com.repsense.app.data.model.Workout
import com.repsense.app.ui.theme.*
import com.repsense.app.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkoutClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onViewAllHistoryClick: () -> Unit,
    onWorkoutDetailClick: (String) -> Unit
) {
    val workouts by viewModel.workouts.collectAsState()
    val user by viewModel.user.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()

    val todayReps = workouts.take(3).sumOf { it.totalReps }
    val todayCalories = workouts.take(3).sumOf { it.estimatedCalories }
    val todayTime = workouts.take(3).sumOf { it.duration }
    val recentWorkout = workouts.firstOrNull()

    val isConnected = connectionState == ConnectionState.CONNECTED && connectedDevice != null

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Greeting + Device Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good morning,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Slate400
                    )
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // Device Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate900,
                    modifier = Modifier
                        .clickable { onDeviceClick() }
                        .testTag("device_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) SuccessGreen else Slate400)
                        )
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = if (isConnected) PrimaryBlueLight else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isConnected) "Band Connected" else "Pair Band",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isConnected) Color.White else Slate400,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Stats Grid (Today's Reps, Calories, Workout Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Today's Reps",
                    value = todayReps.toString(),
                    icon = Icons.Default.Repeat,
                    tint = PrimaryBlueLight
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Calories",
                    value = "$todayCalories kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = AmberAccent
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Duration",
                    value = "${todayTime / 60}m ${todayTime % 60}s",
                    icon = Icons.Default.Timer,
                    tint = NeonCyan
                )
            }

            // Primary Big Action Button: START WORKOUT
            Button(
                onClick = onStartWorkoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("start_workout_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Workout",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "START WORKOUT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                )
            }

            // Weekly Goal Card
            Card(
                shape = RoundedCornerShape(18.dp),
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
                            text = "Weekly Rep Target",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$todayReps / $weeklyGoal reps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlueLight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = (todayReps.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 1f) SuccessGreen else PrimaryBlue,
                        trackColor = Slate800
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% completed this week",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            // Recent Workout Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onViewAllHistoryClick) {
                        Text(
                            text = "View All",
                            color = PrimaryBlueLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (recentWorkout != null) {
                    RecentWorkoutCard(
                        workout = recentWorkout,
                        onClick = { onWorkoutDetailClick(recentWorkout.id) }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No workouts recorded yet. Tap Start Workout to begin!",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

@Composable
fun RecentWorkoutCard(
    workout: Workout,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(workout.startTime))

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Workout",
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = workout.exerciseType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${workout.totalReps} Reps • ${workout.duration / 60}m ${workout.duration % 60}s • ${workout.formAccuracy.toInt()}% Form",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = Slate400
            )
        }
    }
}
