package com.repsense.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repsense.app.ui.theme.*
import com.repsense.app.ui.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
) {
    val context = LocalContext.current
    val workouts by viewModel.workouts.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Month") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf(weeklyGoal.toString()) }

    val summary = viewModel.getSummary()
    val weeklyData = viewModel.getWeeklyAnalytics()
    val dailyMetrics = viewModel.getDailyMetrics()

    val currentWeeklyReps = workouts.take(4).sumOf { it.totalReps }
    val goalProgress = if (weeklyGoal > 0) (currentWeeklyReps.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f) else 0f

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Title & CSV Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Performance Analytics",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Historical trends & biometric intelligence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }

                IconButton(
                    onClick = {
                        val csv = viewModel.generateCsvExport()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            putExtra(Intent.EXTRA_TITLE, "RepSense_Analytics_Export.csv")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Export RepSense Analytics")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Slate900)
                        .testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = PrimaryBlueLight
                    )
                }
            }

            // Period Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Week", "Month", "Year", "All").forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Slate400
                        )
                    }
                }
            }

            // Weekly Goal Card with Edit Button
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
                            text = "Weekly Rep Target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = {
                                tempGoalInput = weeklyGoal.toString()
                                showGoalDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Goal",
                                tint = PrimaryBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$currentWeeklyReps of $weeklyGoal reps",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { goalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (goalProgress >= 1f) SuccessGreen else PrimaryBlue,
                        trackColor = Slate800
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${(goalProgress * 100).toInt()}% completed this period",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            // High-Level Summary Grid
            Text(
                text = "Lifetime Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsStatTile(
                    modifier = Modifier.weight(1f),
                    title = "Workouts",
                    value = "${summary.totalWorkouts}",
                    icon = Icons.Default.FitnessCenter,
                    tint = PrimaryBlueLight
                )
                AnalyticsStatTile(
                    modifier = Modifier.weight(1f),
                    title = "Total Reps",
                    value = "${summary.totalReps}",
                    icon = Icons.Default.Repeat,
                    tint = NeonCyan
                )
                AnalyticsStatTile(
                    modifier = Modifier.weight(1f),
                    title = "Time",
                    value = "${summary.totalDuration / 60}m",
                    icon = Icons.Default.Timer,
                    tint = AmberAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsStatTile(
                    modifier = Modifier.weight(1f),
                    title = "Calories",
                    value = "${summary.totalCalories} kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = AmberAccent
                )
                AnalyticsStatTile(
                    modifier = Modifier.weight(1f),
                    title = "Form Accuracy",
                    value = "${summary.averageFormAccuracy.toInt()}%",
                    icon = Icons.Default.Star,
                    tint = SuccessGreen
                )
            }

            // Weekly Reps Bar Chart Canvas
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Weekly Rep Volume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val count = weeklyData.size
                        val barWidth = (w / count) * 0.45f
                        val maxReps = (weeklyData.maxOfOrNull { it.totalReps } ?: 100).coerceAtLeast(60).toFloat()

                        weeklyData.forEachIndexed { i, item ->
                            val centerX = (i + 0.5f) * (w / count)
                            val barHeight = (item.totalReps / maxReps) * (h - 25f)
                            val topY = h - 25f - barHeight

                            // Background subtle pillar
                            drawRoundRect(
                                color = Slate800,
                                topLeft = Offset(centerX - barWidth / 2f, 0f),
                                size = Size(barWidth, h - 25f),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Filled bar
                            drawRoundRect(
                                color = PrimaryBlue,
                                topLeft = Offset(centerX - barWidth / 2f, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weeklyData.forEach {
                            Text(text = it.week, style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                }
            }

            // Heart Rate Trend Canvas
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Heart Rate & Cardio Load (BPM)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val maxHr = 180f
                        val minHr = 60f

                        // Grid
                        drawLine(color = Slate800, start = Offset(0f, h * 0.5f), end = Offset(w, h * 0.5f), strokeWidth = 1f)

                        val stepX = w / (dailyMetrics.size - 1)
                        val avgPath = Path()
                        val peakPath = Path()

                        dailyMetrics.forEachIndexed { i, metric ->
                            val x = i * stepX
                            val avgY = h - ((metric.avgHeartRate - minHr) / (maxHr - minHr)) * h
                            val peakY = h - ((metric.maxHeartRate - minHr) / (maxHr - minHr)) * h

                            if (i == 0) avgPath.moveTo(x, avgY) else avgPath.lineTo(x, avgY)
                            if (i == 0) peakPath.moveTo(x, peakY) else peakPath.lineTo(x, peakY)
                        }

                        drawPath(path = peakPath, color = AmberAccent.copy(alpha = 0.6f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path = avgPath, color = ErrorRed, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dailyMetrics.forEach {
                            Text(text = it.dateLabel, style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text("Set Weekly Rep Target", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Enter target repetitions for the week:", color = Slate400)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempGoalInput,
                        onValueChange = { tempGoalInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = tempGoalInput.toIntOrNull()
                        if (num != null && num > 0) {
                            viewModel.setGoal(num)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun AnalyticsStatTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
