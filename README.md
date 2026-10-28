# RepSense (Android)

RepSense is an intelligent workout tracker and biomechanics analysis application built for Android using Kotlin and Jetpack Compose (Material 3).

## Key Features

- **Live Workout Tracking**: Real-time repetition counting, kinematic analysis, and form correction (Correct, Partial, Fast, and Incorrect classifications).
- **Biometric Telemetry**: Live Heart Rate (BPM), Blood Oxygen (SpO₂), active workout duration, and calorie burn estimation.
- **IMU Motion Waveform**: Real-time 20Hz accelerometer and gyroscope motion oscilloscope.
- **Hardware & Sensor Integration**: BLE pairing for the RepSense Band with battery telemetry, signal strength indicators, and realistic background telemetry simulation.
- **Analytics & Trends**: Weekly rep volume targets, interactive goal customization, cardio load and form accuracy trend charts.
- **Data Export**: Export workout metrics to CSV using standard Android sharing intents.
- **Workout History & Logs**: Detailed session breakdowns, set-by-set telemetry, and local persistence.
- **Athlete Profile**: Configurable height, weight, gender, age, and fitness goals.

## Tech Stack

- **Framework**: Kotlin & Jetpack Compose
- **Design System**: Material Design 3 (M3) Dark Athletic Theme
- **Architecture**: MVVM with Coroutines & StateFlow
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin 9.1.1
