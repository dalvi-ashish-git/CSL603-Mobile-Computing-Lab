package com.yassineabou.calculator.util

import android.content.Context
import android.content.res.Configuration
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.yassineabou.calculator.data.model.PreviousOperation
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Data binding adapter for setting calculator input text from a PreviousOperation object.
 * 
 * This binding adapter is used in XML layouts with data binding to automatically
 * display the input expression from calculator history.
 * 
 * Usage in XML:
 * ```xml
 * <TextView
 *     app:setInput="@{previousOperation}" />
 * ```
 * 
 * @param previousOperation The calculator operation containing the input expression
 */
@BindingAdapter("setInput")
fun TextView.setInput(previousOperation: PreviousOperation?) {
    previousOperation?.let {
        text = it.input
    }
}

/**
 * Data binding adapter for setting calculator result text from a PreviousOperation object.
 * 
 * This binding adapter is used in XML layouts with data binding to automatically
 * display the calculated result from calculator history.
 * 
 * Usage in XML:
 * ```xml
 * <TextView
 *     app:setResult="@{previousOperation}" />
 * ```
 * 
 * @param previousOperation The calculator operation containing the result
 */
@BindingAdapter("setResult")
fun TextView.setResult(previousOperation: PreviousOperation?) {
    previousOperation?.let {
        text = it.result
    }
}

/**
 * Extension function to check if the device is in dark mode.
 * 
 * Checks the current UI mode configuration to determine if the dark theme
 * is active. This is useful for adapting UI colors dynamically.
 * 
 * Usage:
 * ```kotlin
 * if (context.isDarkMode()) {
 *     // Apply dark mode colors
 * } else {
 *     // Apply light mode colors
 * }
 * ```
 * 
 * @return true if dark mode is active, false otherwise
 */
fun Context.isDarkMode(): Boolean {
    val darkModeFlag = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Extension function to safely parse a String to Double.
 * 
 * Handles empty strings by returning 0.0, preventing NumberFormatException.
 * Useful for parsing user input from EditText fields.
 * 
 * Usage:
 * ```kotlin
 * val amount = editText.text.toString().parseDouble()
 * // If empty, amount = 0.0
 * ```
 * 
 * @return The parsed Double value, or 0.0 if the string is empty
 */
fun String.parseDouble(): Double {
    return if (this.isEmpty()) 0.0 else this.toDouble()
}

/**
 * Safe let function for two nullable parameters.
 * 
 * This inline function only executes the block if both parameters are non-null,
 * providing a more concise way to handle multiple nullable values.
 * 
 * Usage:
 * ```kotlin
 * safeLet(input, result) { nonNullInput, nonNullResult ->
 *     // Both are guaranteed to be non-null here
 *     saveCalculation(nonNullInput, nonNullResult)
 * }
 * ```
 * 
 * @param p1 First nullable parameter
 * @param p2 Second nullable parameter
 * @param block Lambda to execute if both parameters are non-null
 * @return The result of the block, or null if any parameter is null
 */
inline fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

/**
 * Checks if a mathematical expression has balanced brackets.
 * 
 * Validates that every opening bracket '(' has a corresponding closing bracket ')',
 * and they are properly nested. This is essential for mathematical expressions.
 * 
 * Algorithm:
 * - Increment counter for '('
 * - Decrement counter for ')'
 * - If counter becomes negative, brackets are unbalanced
 * - Final counter must be 0 for balanced brackets
 * 
 * Examples:
 * ```kotlin
 * isBalancedBrackets("(1+2)")      // true
 * isBalancedBrackets("((1+2)*3)")  // true
 * isBalancedBrackets("(1+2")       // false
 * isBalancedBrackets("1+2)")       // false
 * ```
 * 
 * @param str The expression string to check
 * @return true if brackets are balanced, false otherwise
 */
fun isBalancedBrackets(str: String): Boolean {
    var count = 0
    var i = 0
    while (i < str.length && count >= 0) {
        if (str[i] == '(') count++ else if (str[i] == ')') count--
        i++
    }
    return count == 0
}

/**
 * Extension function to format a Number with decimal formatting.
 * 
 * Formats numbers to a maximum of 2 decimal places using CEILING rounding mode.
 * Returns "Not applicable" for NaN values.
 * 
 * Formatting rules:
 * - Maximum 2 decimal places
 * - CEILING rounding (rounds up)
 * - Removes unnecessary decimal points and zeros
 * 
 * Examples:
 * ```kotlin
 * 123.456.decimalFormat()  // "123.46"
 * 100.0.decimalFormat()    // "100"
 * Double.NaN.decimalFormat() // "Not applicable"
 * ```
 * 
 * @return Formatted string representation of the number
 */
fun Number.decimalFormat(): String {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING
    return df.format(this).takeIf { it != "NaN" } ?: "Not applicable"
}

/**
 * Removes trailing zeros and unnecessary decimal points from a numeric string.
 * 
 * This function cleans up decimal numbers by removing:
 * - Trailing zeros after the decimal point
 * - The decimal point itself if no fractional part remains
 * 
 * Examples:
 * ```kotlin
 * trimTrailingZero("123.000")  // "123"
 * trimTrailingZero("123.450")  // "123.45"
 * trimTrailingZero("123")      // "123"
 * trimTrailingZero(null)       // null
 * ```
 * 
 * @param value The numeric string to clean up
 * @return The cleaned string, or the original value if null/empty
 */
fun trimTrailingZero(value: String?): String? {
    return if (!value.isNullOrEmpty()) {
        if (value.indexOf(".") < 0) {
            // No decimal point, return as-is
            value
        } else {
            // Remove trailing zeros and then trailing decimal point if needed
            value.replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")
        }
    } else {
        value
    }
}

