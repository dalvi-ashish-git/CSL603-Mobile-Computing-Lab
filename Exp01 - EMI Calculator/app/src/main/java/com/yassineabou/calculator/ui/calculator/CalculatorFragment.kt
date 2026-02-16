package com.yassineabou.calculator.ui.calculator

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yassineabou.calculator.R
import com.yassineabou.calculator.databinding.FragmentCalculatorBinding
import com.yassineabou.calculator.ui.emi.EMIViewModel
import com.yassineabou.calculator.ui.emi.EmiCalculatorState
import com.yassineabou.calculator.util.isDarkMode
import com.yassineabou.calculator.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main calculator fragment handling both basic and scientific calculator functionality.
 * 
 * Features:
 * - Basic arithmetic operations (+, -, ×, ÷)
 * - Scientific functions (trigonometry, logarithms, etc.)
 * - Portrait mode (basic) and landscape mode (scientific)
 * - Calculation history with database persistence
 * - Support for functions, brackets, percentages, and constants
 * 
 * Architecture:
 * - Uses MVVM pattern with ViewModel for business logic
 * - Data binding for automatic UI updates
 * - Kotlin Coroutines and Flow for reactive state management
 * - Navigation component for screen transitions
 * - Hilt for dependency injection
 */
@AndroidEntryPoint
class CalculatorFragment : Fragment(R.layout.fragment_calculator) {

    // Fragment-scoped ViewModel for calculator operations
    private val calculatorViewModel: CalculatorViewModel by viewModels()
    
    // Activity-scoped ViewModel for EMI calculator (shared state)
    private val emiViewModel: EMIViewModel by activityViewModels()
    
    // View binding for type-safe view access
    private val fragmentCalculatorBinding by viewBinding(FragmentCalculatorBinding::bind)
    
    // Lazy-initialized adapter for calculator history RecyclerView
    private val adapter by lazy {
        ListPreviousOperationsAdapter(object : PreviousOperationAction {
            override fun appendNumberFromHistory(number: String) {
                calculatorViewModel.appendNumberFromHistory(number)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup data binding for automatic UI updates
        fragmentCalculatorBinding.viewModel = calculatorViewModel
        fragmentCalculatorBinding.lifecycleOwner = viewLifecycleOwner

        // Configure RecyclerView for calculator history display
        fragmentCalculatorBinding.listPreviousOperations.adapter = adapter
        val manager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        fragmentCalculatorBinding.listPreviousOperations.layoutManager = manager

        // Cleanup adapter on fragment destruction to prevent memory leaks
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                fragmentCalculatorBinding.listPreviousOperations.adapter = null
                super.onDestroy(owner)
            }
        })

        // Setup click listeners for calculator controls
        fragmentCalculatorBinding.apply {
            // Navigate to EMI Calculator screen
            emiCalculator.setOnClickListener {
                navigateToEmiCalculator()
            }

            // Switch to portrait (normal/basic) mode
            normalMode?.setOnClickListener {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            // Switch to landscape (scientific) mode
            scientificMode?.setOnClickListener {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            // Toggle between first and second group of scientific functions
            alternativeFunctions?.setOnClickListener {
                changeGroupFunctionsVisibility()
            }

            // Toggle calculator history visibility
            history.setOnClickListener {
                changeHistoryVisibility()
                fragmentCalculatorBinding.listPreviousOperations.scrollToPosition(0)
            }
        }

        // Observe input changes and update backspace button color accordingly
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calculatorViewModel.input.collectLatest {
                    it?.let {
                        calculatorViewModel.updateInput(it)
                        // Darken backspace button when there's input to delete
                        val backSpaceColor = if (it.isNotEmpty()) "#997592" else "#D8BFD8"
                        fragmentCalculatorBinding.backspace.setColorFilter(
                            Color.parseColor(backSpaceColor),
                            PorterDuff.Mode.SRC_ATOP,
                        )
                    }
                }
            }
        }

        // Observe result changes for real-time calculation display
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calculatorViewModel.result.collectLatest {
                    it?.let {
                        calculatorViewModel.updateResult(it)
                    }
                }
            }
        }

        // Observe calculator state for UI updates (error messages, function groups)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calculatorViewModel.calculatorState.collect { calculatorState ->
                    when {
                        calculatorState.isInvalidFormat -> showInvalidFormat()
                        calculatorState.isSecondGroupFunctions -> showSecondGroupFunctions()
                        calculatorState.isFirstGroupFunctions -> showFirstGroupFunctions()
                    }
                }
            }
        }

        // Observe calculator history changes from database
        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calculatorViewModel.listPreviousOperationsFlow.collect {
                    adapter.submitList(it)

                    // Enable/disable history button based on whether history exists
                    when (it.size) {
                        0 -> {
                            disableHistoryButton()
                            hideHistoryGroup()
                        }
                        else -> enableHistoryButton()
                    }
                }
            }
        }
    }

    /**
     * Toggles between first and second group of scientific functions.
     * 
     * First group: sin, cos, tan, log, ln, etc.
     * Second group: arcsin, arccos, arctan, etc.
     */
    private fun changeGroupFunctionsVisibility() {
        if (calculatorViewModel.calculatorState.value.isSecondGroupFunctions) {
            calculatorViewModel.updateCalculatorState(
                CalculatorState(isFirstGroupFunctions = true, isSecondGroupFunctions = false),
            )
        } else {
            calculatorViewModel.updateCalculatorState(
                CalculatorState(isFirstGroupFunctions = false, isSecondGroupFunctions = true),
            )
        }
    }

    /**
     * Enables the history button with proper color for the current theme.
     */
    private fun enableHistoryButton() {
        fragmentCalculatorBinding.history.isEnabled = true
        fragmentCalculatorBinding.history.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.black_200),
            PorterDuff.Mode.SRC_ATOP,
        )
    }

    /**
     * Disables the history button when there's no history to display.
     * Color adapts to dark/light mode.
     */
    private fun disableHistoryButton() {
        fragmentCalculatorBinding.history.isEnabled = false
        val disabledColor = if (requireContext().isDarkMode()) "#333333" else "#CACACA"
        fragmentCalculatorBinding.history.setColorFilter(
            Color.parseColor(disabledColor),
            PorterDuff.Mode.SRC_ATOP,
        )
    }

    /**
     * Displays a toast message when the user enters an invalid mathematical expression.
     */
    private fun showInvalidFormat() {
        Toast.makeText(requireContext(), "Invalid format used.", Toast.LENGTH_SHORT).show()
        calculatorViewModel.updateCalculatorState(CalculatorState(isInvalidFormat = false))
    }

    /**
     * Shows the first group of scientific functions (sin, cos, tan, etc.).
     */
    private fun showFirstGroupFunctions() {
        fragmentCalculatorBinding.secondGroupFunctions?.visibility = GONE
        fragmentCalculatorBinding.firstGroupFunctions?.visibility = VISIBLE
    }

    /**
     * Shows the second group of scientific functions (arcsin, arccos, etc.).
     */
    private fun showSecondGroupFunctions() {
        fragmentCalculatorBinding.firstGroupFunctions?.visibility = INVISIBLE
        fragmentCalculatorBinding.secondGroupFunctions?.visibility = VISIBLE
    }

    /**
     * Toggles the visibility of the calculator history panel.
     */
    private fun changeHistoryVisibility() {
        fragmentCalculatorBinding.apply {
            if (historyGroup.isVisible) {
                hideHistoryGroup()
            } else {
                showHistoryGroup()
            }
        }
    }

    /**
     * Hides the calculator history and shows the normal calculator interface.
     */
    private fun hideHistoryGroup() {
        fragmentCalculatorBinding.apply {
            history.setImageResource(R.drawable.history)
            historyGroup.visibility = GONE
            normalModeGroup.visibility = VISIBLE

            // Restore the previously visible function group
            if (calculatorViewModel.calculatorState.value.isSecondGroupFunctions) {
                secondGroupFunctions?.visibility = VISIBLE
            } else {
                firstGroupFunctions?.visibility = VISIBLE
            }
        }
    }

    /**
     * Shows the calculator history panel and hides the calculator buttons.
     */
    private fun showHistoryGroup() {
        fragmentCalculatorBinding.apply {
            history.setImageResource(R.drawable.ic_baseline_calculate_24)
            historyGroup.visibility = VISIBLE
            normalModeGroup.visibility = INVISIBLE

            // Hide function groups when history is shown
            if (calculatorViewModel.calculatorState.value.isSecondGroupFunctions) {
                secondGroupFunctions?.visibility = INVISIBLE
            } else {
                firstGroupFunctions?.visibility = INVISIBLE
            }
        }
    }

    /**
     * Navigates from the calculator screen to the EMI (Equated Monthly Installment) calculator.
     * 
     * Initializes the EMI calculator state to show the first calculator by default.
     */
    private fun navigateToEmiCalculator() {
        emiViewModel.updateEmiCalculatorState(
            EmiCalculatorState(isFirstEmiCalculator = true, isSecondEmiCalculator = false),
        )
        val action = CalculatorFragmentDirections.calculatorFragmentToEmiCalculator()
        findNavController(this).navigate(action)
    }
}

