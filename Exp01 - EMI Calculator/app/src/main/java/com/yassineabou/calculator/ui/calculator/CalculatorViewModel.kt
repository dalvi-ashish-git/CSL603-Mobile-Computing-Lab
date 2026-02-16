package com.yassineabou.calculator.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yassineabou.calculator.data.model.PreviousOperation
import com.yassineabou.calculator.data.repository.PreviousOperationRepository
import com.yassineabou.calculator.util.isBalancedBrackets
import com.yassineabou.calculator.util.safeLet
import com.yassineabou.calculator.util.trimTrailingZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mariuszgromada.math.mxparser.Expression
import javax.inject.Inject

/**
 * Data class representing the UI state of the calculator.
 * 
 * @property isInvalidFormat Indicates if the user entered an invalid mathematical expression
 * @property isFirstGroupFunctions Indicates if the first group of scientific functions is visible
 * @property isSecondGroupFunctions Indicates if the second group of scientific functions is visible
 */
data class CalculatorState(
    val isInvalidFormat: Boolean = false,
    val isFirstGroupFunctions: Boolean = true,
    val isSecondGroupFunctions: Boolean = false,
)

/**
 * ViewModel for managing calculator operations and state.
 * 
 * This ViewModel handles all calculator logic including:
 * - Mathematical expression building and validation
 * - Expression evaluation using mXparser library
 * - Calculator history management through Room database
 * - UI state management for different calculator modes
 * 
 * The ViewModel uses StateFlow to expose reactive state to the UI layer,
 * following Android's recommended architecture patterns.
 * 
 * @property previousOperationRepository Repository for accessing calculator history from database
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val previousOperationRepository: PreviousOperationRepository,
) : ViewModel() {

    // Flow of previous calculator operations from the database
    val listPreviousOperationsFlow = previousOperationRepository.listPreviousOperationsFlow
    
    // Characters that can appear at the end of an expression without being a number
    private val listChars: List<Char> = listOf(')', 'e', 'i', '%')
    
    // Arithmetic operation symbols
    private val listArithmeticSymbols: List<Char> = listOf('+', '-', '×', '÷', '.')
    
    // Valid number characters including constants (π, e) and complex number unit (i)
    private val listNumbers: List<Char> =
        listOf(')', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '%', 'e', 'i', 'ℼ')
    
    // Flag to control decimal point input (prevents multiple decimal points in same number)
    private var isDecimalPointClicked = true
    
    // Tracks the last arithmetic symbol used for expression parsing
    private var arithmeticSymbol = '+'

    // Current mathematical expression input by the user
    private val _input: MutableStateFlow<String?> = MutableStateFlow(null)
    val input = _input.asStateFlow()

    // Calculated result of the current expression
    private val _result: MutableStateFlow<String?> = MutableStateFlow(null)
    val result = _result.asStateFlow()

    // Current state of the calculator UI (function groups, error states)
    private val _calculatorState = MutableStateFlow(CalculatorState())
    val calculatorState: StateFlow<CalculatorState> = _calculatorState.asStateFlow()

    init {
        // Initialize with empty input and result
        _input.value = ""
        _result.value = ""
    }

    /**
     * Updates the current input expression.
     * 
     * @param input The new input expression
     */
    fun updateInput(input: String) {
        _input.value = input
    }

    /**
     * Updates the current calculation result.
     * 
     * @param result The new result value
     */
    fun updateResult(result: String) {
        _result.value = result
    }

    /**
     * Updates the calculator UI state.
     * 
     * @param calculatorState The new calculator state
     */
    fun updateCalculatorState(calculatorState: CalculatorState) {
        viewModelScope.launch {
            _calculatorState.value = calculatorState
        }
    }

    /**
     * Appends a number from calculator history to the current input.
     * 
     * Handles special cases:
     * - Replaces leading zero with the new number
     * - Adds multiplication symbol when appending after special characters
     * - Validates that numbers aren't appended in invalid contexts
     * 
     * @param number The number string from history to append
     */
    fun appendNumberFromHistory(number: String) {
        val inputAfterArithmetic = _input.value?.substringAfterLast(arithmeticSymbol)
        inputAfterArithmetic?.let {
            when {
                inputAfterArithmetic == "0" -> {
                    _input.value = _input.value?.dropLast(1) + number
                }
                (inputAfterArithmetic.lastOrNull() in listChars) && !inputAfterArithmetic.contains('.') -> {
                    _input.value = "${_input.value}×$number"
                }
                (inputAfterArithmetic.lastOrNull() != '%') && !inputAfterArithmetic.contains('.') -> {
                    _input.value += number
                }
                else -> {
                    updateCalculatorState(CalculatorState(isInvalidFormat = true))
                }
            }
        }
    }

    /**
     * Appends a mathematical function to the input expression.
     * 
     * Automatically adds a multiplication symbol if the function is appended
     * after a number, maintaining proper mathematical syntax.
     * 
     * Examples: sin(, cos(, tan(, log(, etc.
     * 
     * @param function The function name to append
     */
    fun appendFunction(function: String) {
        _input.value?.let { input ->
            _input.value = when {
                input.lastOrNull() in listNumbers -> ("$input×$function")
                else -> ("$input$function")
            }
        }
    }

    /**
     * Appends a power operator (^) to the input expression.
     * 
     * Validates that the power operator is only appended after valid numbers.
     * 
     * @param power The power operator string
     */
    fun appendPower(power: String) {
        _input.value?.let { input ->
            val inputAfterArithmetic = input.substringAfterLast(arithmeticSymbol)
            if (inputAfterArithmetic.isNotEmpty() && (inputAfterArithmetic.lastOrNull() in listNumbers)) {
                _input.value = ("$input$power")
            } else {
                updateCalculatorState(CalculatorState(isInvalidFormat = true))
            }
        }
    }

    /**
     * Calculates and evaluates the current mathematical expression.
     * 
     * Uses the mXparser library to parse and evaluate complex mathematical expressions.
     * The calculation is performed on the IO dispatcher to avoid blocking the main thread.
     * 
     * Handles:
     * - Converting custom symbols (× to *, ÷ to /) for parsing
     * - Floating point precision and trailing zero removal
     * - Invalid expressions (returns empty result for NaN or incomplete expressions)
     */
    fun calculateInput() {
        _input.value?.let { input ->
            viewModelScope.launch(Dispatchers.IO) {
                isDecimalPointClicked = true
                input.substringAfterLast(arithmeticSymbol)

                // Replace calculator symbols with mXparser-compatible operators
                val expression = Expression(input.replace('×', '*').replace('÷', '/'))
                val output =
                    trimTrailingZero(java.lang.String.valueOf(expression.calculate().toFloat()))
                
                // Don't show result for incomplete expressions or invalid calculations
                _result.value =
                    if (input.lastOrNull() in listOf('+', '-') || output == "NaN") "" else output
            }
        }
    }

    /**
     * Removes the last character from the input expression (backspace functionality).
     */
    fun removeLastInputChar() {
        _input.value?.let {
            if (it != "") {
                _input.value = it.substring(0, it.length - 1)
            }
        }
    }

    /**
     * Appends a decimal point to the current number being entered.
     * 
     * Handles special cases:
     * - Automatically adds "0." if decimal is pressed without a number
     * - Prevents multiple decimal points in the same number
     * - Validates that decimal point is only added after arithmetic operators or at start
     */
    fun appendDecimalPoint() {
        _input.value?.let { input ->
            val inputAfterArithmetic = input.substringAfterLast(arithmeticSymbol)
            if (inputAfterArithmetic.isEmpty() || inputAfterArithmetic.lastOrNull() == '(' && isDecimalPointClicked) {
                _input.value = (input + "0.")
                isDecimalPointClicked = false
            }
            if (isDecimalPointClicked && (inputAfterArithmetic.lastOrNull()?.isDigit() == true) && inputAfterArithmetic.none { char -> char == '.' }
            ) {
                _input.value = ("$input.")
                isDecimalPointClicked = false
            }
        }
    }

    /**
     * Clears both input expression and result, resetting the calculator.
     */
    fun clearInput() {
        _input.value = ""
        _result.value = ""
    }

    /**
     * Appends a minus sign to the input expression.
     * 
     * Handles both subtraction operation and negative numbers:
     * - If after a number: appends "×(-" for multiplication with negative
     * - Otherwise: appends "(-" for a negative number
     */
    /**
     * Appends a minus sign to the input expression.
     * 
     * Handles both subtraction operation and negative numbers:
     * - If after a number: appends "×(-" for multiplication with negative
     * - Otherwise: appends "(-" for a negative number
     */
    fun appendMinusSign() {
        _input.value?.let { input ->
            _input.value = if (input.lastOrNull() in listNumbers) ("$input×(-") else ("$input(-")
        }
    }

    /**
     * Appends brackets to the input expression with intelligent behavior.
     * 
     * Logic:
     * - If brackets are balanced and after a number: adds "×(" (multiplication)
     * - If brackets are balanced: adds "(" (opening bracket)
     * - If brackets are not balanced: adds ")" (closing bracket)
     * 
     * This provides intuitive bracket input without requiring separate open/close buttons.
     */
    fun appendBrackets() {
        _input.value?.let { input ->
            _input.value = when {
                isBalancedBrackets(input) && input.lastOrNull() in listNumbers -> ("$input×(")
                isBalancedBrackets(input) -> ("$input(")
                else -> ("$input)")
            }
        }
    }

    /**
     * Inserts the current calculation into the history database.
     * 
     * Saves both the input expression and its result to the Room database
     * for future reference. After insertion, the result becomes the new input
     * for continued calculations.
     * 
     * Only saves if a valid result exists.
     */
    fun insert() {
        safeLet(_input.value, _result.value) { input, result ->
            if (result.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val previousOperation = PreviousOperation(input, result)
                    previousOperationRepository.insert(previousOperation)
                    _input.value = result
                    _result.value = ""
                }
            }
        }
    }

    /**
     * Appends a percentage symbol (%) to the current number.
     * 
     * Validates that:
     * - The percentage is appended only after valid numbers
     * - No duplicate percentage symbols are added to the same number
     * 
     * The percentage is evaluated during calculation (100% becomes 1.0).
     */
    fun appendPercentage() {
        _input.value?.let { input ->
            val inputAfterArithmetic = input.substringAfterLast(arithmeticSymbol)
            if (inputAfterArithmetic.isNotEmpty() && (inputAfterArithmetic.lastOrNull() in listNumbers) && (inputAfterArithmetic.lastOrNull() != '%')) {
                _input.value = ("$input%")
                isDecimalPointClicked = true
            } else {
                updateCalculatorState(CalculatorState(isInvalidFormat = true))
            }
        }
    }

    /**
     * Appends a numeric digit (0-9) to the input expression.
     * 
     * Handles special cases:
     * - Replaces leading zeros with the new digit
     * - Adds multiplication symbol when appending after special characters (e, π, %, etc.)
     * - Prevents invalid number formations
     * 
     * @param number The digit string to append ("0" through "9")
     */
    fun appendNumber(number: String) {
        _input.value?.let { input ->
            val inputAfterArithmetic = input.substringAfterLast(arithmeticSymbol)

            _input.value = when (inputAfterArithmetic) {
                "0", "(0", "(-0" -> input.dropLast(1) + number
                else -> when (inputAfterArithmetic.lastOrNull()) {
                    in listChars -> "$input×$number"
                    else -> input + number
                }
            }
        }
    }

    /**
     * Appends an arithmetic operator (+, -, ×, ÷) to the input expression.
     * 
     * Features:
     * - Prevents consecutive arithmetic symbols
     * - Replaces the last operator if a new one is entered
     * - Resets decimal point flag to allow decimals in the next number
     * - Updates the tracked arithmetic symbol for expression parsing
     * 
     * @param arithmeticSymbol The arithmetic operator string to append
     */
    fun appendArithmetic(arithmeticSymbol: String) {
        _input.value?.let { input ->
            this.arithmeticSymbol = arithmeticSymbol.single()

            if (input.lastOrNull() !in listArithmeticSymbols && input.isNotEmpty()) {
                _input.value = (input + arithmeticSymbol)
                isDecimalPointClicked = true
            } else if (input.lastOrNull() in listArithmeticSymbols) {
                _input.value = (input.dropLast(1) + arithmeticSymbol)
                isDecimalPointClicked = true
            }
        }
    }

    /**
     * Clears all calculator history from the database.
     * 
     * This operation is performed on the IO dispatcher to avoid blocking the main thread.
     * The UI will automatically update through the Flow-based observation.
     */
    fun clearListPreviousOperations() = viewModelScope.launch(Dispatchers.IO) {
        previousOperationRepository.clear()
    }
}
