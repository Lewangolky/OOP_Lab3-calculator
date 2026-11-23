package com.oop.calculator.engine

/**
 * Reason a calculation failed. The UI maps every value to a translated string,
 * so the engine itself never produces user-facing text.
 */
enum class CalcError {
    /** The expression is malformed, e.g. `2×` or `)(`. */
    SYNTAX,

    /** Division by zero. */
    DIVISION_BY_ZERO,

    /** Argument outside the function's domain, e.g. `√(−1)`, `ln(0)` or `tan(90°)`. */
    DOMAIN,

    /** Factorial of a negative or non-integer number. */
    FACTORIAL,

    /** The result is too large to be represented. */
    OVERFLOW,
}

class CalculationException(
    val error: CalcError,
    message: String,
) : ArithmeticException(message)
