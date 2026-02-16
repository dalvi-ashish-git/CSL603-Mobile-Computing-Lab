package com.yassineabou.calculator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class for the Calculator app.
 * 
 * This class serves as the entry point for the application and is responsible for:
 * - Initializing Hilt dependency injection
 * - Setting up app-wide configurations
 * - Providing global application context
 * 
 * The @HiltAndroidApp annotation:
 * - Triggers Hilt's code generation
 * - Creates the application-level dependency injection component
 * - Enables dependency injection throughout the app (Activities, Fragments, ViewModels, etc.)
 * 
 * Lifecycle:
 * - Created when the app process starts
 * - Lives for the entire duration of the app process
 * - Destroyed only when the app process is killed
 * 
 * Best practices:
 * - Keep this class lightweight
 * - Avoid heavy initialization here (use WorkManager for background tasks)
 * - Use dependency injection instead of storing global state
 * 
 * Configuration:
 * - Registered in AndroidManifest.xml with android:name attribute
 * - Must be public and have a public no-argument constructor
 */
@HiltAndroidApp
class CalculatorApplication : Application()
