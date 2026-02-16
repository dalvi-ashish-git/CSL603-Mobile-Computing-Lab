package com.yassineabou.calculator.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yassineabou.calculator.util.decimalFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * State class representing which EMI calculator is currently active.
 * 
 * The app supports comparing two different EMI calculations side-by-side.
 * 
 * @property isFirstEmiCalculator True if the first calculator is active
 * @property isSecondEmiCalculator True if the second calculator is active
 */
data class EmiCalculatorState(
    val isFirstEmiCalculator: Boolean = true,
    val isSecondEmiCalculator: Boolean = false,
)

/**
 * Data class containing all calculated EMI (Equated Monthly Installment) values.
 * 
 * EMI is a fixed payment amount made by a borrower to a lender at a specified
 * date each calendar month. It consists of both principal and interest components.
 * 
 * @property emiAmount The monthly installment amount to be paid
 * @property interest The total interest amount paid over the loan tenure
 * @property interestRate The annual interest rate as a percentage
 * @property totalAmount The total amount to be repaid (principal + interest)
 * @property principal The original loan amount borrowed
 * @property numberInstallments The total number of monthly payments (tenure in months)
 */
data class Emi(
    val emiAmount: String,
    val interest: String,
    val interestRate: String,
    val totalAmount: String,
    val principal: String,
    val numberInstallments: String,
)

/**
 * ViewModel for managing EMI (Equated Monthly Installment) calculations.
 * 
 * This ViewModel handles:
 * - EMI calculation using the standard EMI formula
 * - Managing state for two separate EMI calculations (for comparison)
 * - Converting calculation results to formatted strings
 * 
 * EMI Formula:
 * ```
 * EMI = [P × R × (1+R)^N] / [(1+R)^N - 1]
 * 
 * Where:
 * P = Principal loan amount
 * R = Monthly interest rate (annual rate / 12 / 100)
 * N = Number of monthly installments (tenure in months)
 * ```
 * 
 * Example calculation:
 * - Loan amount: ₹1,000,000
 * - Annual interest rate: 10%
 * - Tenure: 12 months
 * - EMI = ₹87,915.89 per month
 * - Total interest: ₹54,990.68
 * - Total payment: ₹1,054,990.68
 */
class EMIViewModel : ViewModel() {

    // Current state indicating which EMI calculator is active
    private val _emiCalculatorState = MutableStateFlow(EmiCalculatorState())
    val emiCalculatorState: StateFlow<EmiCalculatorState> = _emiCalculatorState.asStateFlow()

    // EMI calculation result for the first calculator
    private val _firstEmiCalculation: MutableStateFlow<Emi?> = MutableStateFlow(null)
    val firstEmiCalculation = _firstEmiCalculation.asStateFlow()

    // EMI calculation result for the second calculator (for comparison)
    private val _secondEmiCalculation: MutableStateFlow<Emi?> = MutableStateFlow(null)
    val secondEmiCalculation = _secondEmiCalculation.asStateFlow()

    /**
     * Updates which EMI calculator is currently active.
     * 
     * @param emiCalculatorState The new calculator state
     */
    fun updateEmiCalculatorState(emiCalculatorState: EmiCalculatorState) {
        _emiCalculatorState.value = emiCalculatorState
    }

    /**
     * Calculates EMI (Equated Monthly Installment) based on loan parameters.
     * 
     * Mathematical breakdown:
     * 1. Convert annual interest rate to monthly rate: R = (rate / 12 / 100)
     * 2. Calculate compound factor: (1 + R)^N
     * 3. Apply EMI formula: EMI = [P × R × (1+R)^N] / [(1+R)^N - 1]
     * 4. Calculate total interest: (EMI × N) - P
     * 5. Calculate total payment: Principal + Interest
     * 
     * The calculation is performed on the IO dispatcher to avoid blocking the main thread,
     * though EMI calculations are typically fast enough for the main thread.
     * 
     * @param loanAmount The principal loan amount (P) in currency units
     * @param interestRate The annual interest rate as a percentage (e.g., 10 for 10%)
     * @param numberInstallments The tenure in months (N) - total number of payments
     * 
     * Example usage:
     * ```kotlin
     * calculateEmi(
     *     loanAmount = 1000000.0,      // ₹10 lakhs
     *     interestRate = 10.0,          // 10% annual rate
     *     numberInstallments = 12.0     // 1 year tenure
     * )
     * ```
     */
    fun calculateEmi(loanAmount: Double, interestRate: Double, numberInstallments: Double) {
        viewModelScope.launch {
            // Step 1: Convert annual interest rate to monthly interest rate
            // Monthly rate = Annual rate / 12 months / 100 (to convert percentage to decimal)
            val interestValue = interestRate / 12 / 100
            
            // Step 2: Calculate the compound interest factor (1 + R)^N
            // This represents how much the money grows over the loan tenure
            val commonPart = (1 + interestValue).pow(numberInstallments)
            
            // Step 3: Apply EMI formula components
            // Numerator: P × R × (1+R)^N
            val divUp = (loanAmount * interestValue * commonPart)
            // Denominator: (1+R)^N - 1
            val divDown = commonPart - 1
            
            // Step 4: Calculate EMI per month
            val emiCalculationPerMonth: Float = ((divUp / divDown)).toFloat()
            
            // Step 5: Calculate total interest paid over the loan tenure
            // Total payment - Principal = Interest
            val totalInterest = (emiCalculationPerMonth * numberInstallments) - loanAmount
            
            // Step 6: Calculate total amount to be repaid
            val totalPayment = totalInterest + loanAmount

            // Create EMI result object with formatted values
            val emi = Emi(
                emiAmount = emiCalculationPerMonth.decimalFormat(),
                interest = totalInterest.decimalFormat(),
                interestRate = interestRate.decimalFormat(),
                totalAmount = totalPayment.decimalFormat(),
                principal = loanAmount.decimalFormat(),
                numberInstallments = numberInstallments.decimalFormat(),
            )

            // Store result in the appropriate calculator slot (first or second)
            if (_emiCalculatorState.value.isSecondEmiCalculator) {
                _secondEmiCalculation.value = emi
            } else {
                _firstEmiCalculation.value = emi
            }
        }
    }
}
