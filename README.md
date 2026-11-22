# Max Studio v4

Open-source Kotlin Android IDE for building apps on mobile with code editing, XML layout preview, diagnostics, and APK packaging.

## Overview

Max Studio v4 is an Android Studio project structured to help developers write, inspect, and build Android applications.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open **Android Studio**.
2. Select **Open** and choose the project directory.
3. Allow Android Studio and Gradle to sync dependencies and fix any build incompatibilities.
4. Create a `.env` file in the project directory and add required environment values (see `.env.example` for reference).
5. Check `app/build.gradle.kts` and configure proper release signing before building a release version.
6. Run the app on an emulator or physical Android device.

## Project Structure

- `app/` — Application source code, resources, and module build configuration
- `gradle/` — Gradle wrapper and build setup files
- `build.gradle.kts` — Root Gradle build script

## Contributing & Support

Contributions, issues, and suggestions are welcome. Feel free to open an issue or submit a pull request.
