package com.repsense.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.repsense.app.data.model.ExerciseType
import com.repsense.app.ui.screens.*
import com.repsense.app.ui.theme.*
import com.repsense.app.ui.viewmodel.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Workouts : Screen("workout_select", "Workouts", Icons.Default.FitnessCenter)
    object History : Screen("history", "History", Icons.Default.History)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    object LiveWorkout : Screen("live_workout/{exerciseType}", "Live Workout")
    object WorkoutSummary : Screen("workout_summary/{workoutId}", "Workout Summary")
    object WorkoutDetail : Screen("workout_detail/{workoutId}", "Workout Detail")
    object ConnectDevice : Screen("connect_device", "Connect Band")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.repsense.app.data.ble.BleManager.instance.initContext(this)
        com.repsense.app.utils.VoiceGuidanceManager.instance.initialize(this)

        setContent {
            RepSenseTheme(darkTheme = true) {
                RepSenseApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.repsense.app.utils.VoiceGuidanceManager.instance.shutdown()
    }
}

@Composable
fun RepSenseApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel: HomeViewModel = viewModel()
    val liveWorkoutViewModel: LiveWorkoutViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val deviceViewModel: DeviceViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Workouts,
        Screen.History,
        Screen.Analytics,
        Screen.Profile
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Workouts.route,
        Screen.History.route,
        Screen.Analytics.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Slate900,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryBlueLight,
                                indicatorColor = PrimaryBlue,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onStartWorkoutClick = {
                        navController.navigate(Screen.Workouts.route)
                    },
                    onDeviceClick = {
                        navController.navigate(Screen.ConnectDevice.route)
                    },
                    onViewAllHistoryClick = {
                        navController.navigate(Screen.History.route)
                    },
                    onWorkoutDetailClick = { workoutId ->
                        navController.navigate("workout_detail/$workoutId")
                    }
                )
            }

            composable(Screen.Workouts.route) {
                WorkoutSelectScreen(
                    onExerciseSelected = { exercise ->
                        navController.navigate("live_workout/${exercise.name}")
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onWorkoutClick = { workoutId ->
                        navController.navigate("workout_detail/$workoutId")
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    viewModel = analyticsViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onDeviceSettingsClick = {
                        navController.navigate(Screen.ConnectDevice.route)
                    }
                )
            }

            composable(
                route = Screen.LiveWorkout.route,
                arguments = listOf(navArgument("exerciseType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeStr = backStackEntry.arguments?.getString("exerciseType") ?: ExerciseType.BICEP_CURL.name
                val exerciseType = try {
                    ExerciseType.valueOf(typeStr)
                } catch (e: Exception) {
                    ExerciseType.BICEP_CURL
                }

                LiveWorkoutScreen(
                    exerciseType = exerciseType,
                    viewModel = liveWorkoutViewModel,
                    onWorkoutFinished = { workout ->
                        navController.navigate("workout_summary/${workout.id}") {
                            popUpTo(Screen.Workouts.route)
                        }
                    },
                    onExitWithoutSave = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.WorkoutSummary.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId")
                val workout = workoutId?.let { historyViewModel.getWorkout(it) }

                if (workout != null) {
                    WorkoutSummaryScreen(
                        workout = workout,
                        onDoneClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                } else {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onStartWorkoutClick = { navController.navigate(Screen.Workouts.route) },
                        onDeviceClick = { navController.navigate(Screen.ConnectDevice.route) },
                        onViewAllHistoryClick = { navController.navigate(Screen.History.route) },
                        onWorkoutDetailClick = { id -> navController.navigate("workout_detail/$id") }
                    )
                }
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId")
                val workout = workoutId?.let { historyViewModel.getWorkout(it) }

                WorkoutDetailScreen(
                    workout = workout,
                    onBackClick = { navController.popBackStack() },
                    onDeleteClick = { id ->
                        historyViewModel.deleteWorkout(id)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.ConnectDevice.route) {
                ConnectDeviceScreen(
                    viewModel = deviceViewModel,
                    onContinueClick = { navController.popBackStack() }
                )
            }
        }
    }
}
