package com.yassineabou.calculator.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.yassineabou.calculator.R
import com.yassineabou.calculator.databinding.ActivityMainBinding
import com.yassineabou.calculator.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity serving as the host for the calculator application.
 * 
 * This activity:
 * - Hosts the navigation graph with NavHostFragment
 * - Manages the toolbar visibility based on the current destination
 * - Provides edge-to-edge display support for modern Android versions
 * - Uses Hilt for dependency injection
 * - Implements view binding for type-safe view access
 * 
 * The activity follows Android's single-activity architecture pattern,
 * where fragments handle different screens and this activity manages navigation.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // View binding instance for type-safe access to views
    private val activityMainBinding by viewBinding(ActivityMainBinding::inflate)
    
    // Navigation controller for managing fragment navigation
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for Android 15+ (immersive experience)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(activityMainBinding.root)
        setupNavigation()

        // Hide toolbar on calculator screen, show on other screens (EMI calculator, etc.)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.calculatorFragment) {
                activityMainBinding.toolbar.visibility = View.GONE
            } else {
                activityMainBinding.toolbar.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Configures the navigation component and toolbar.
     * 
     * Sets up:
     * - NavHostFragment for hosting navigation destinations
     * - Toolbar with up navigation support
     * - ActionBar integration with navigation controller
     */
    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        setSupportActionBar(activityMainBinding.toolbar)
        setupActionBarWithNavController(navController)
    }

    /**
     * Handles the up button press in the action bar.
     * 
     * Delegates to the navigation controller to navigate up in the navigation hierarchy.
     * Falls back to default behavior if navigation doesn't handle it.
     * 
     * @return true if navigation was handled, false otherwise
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
