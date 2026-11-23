package com.oop.calculator.format

import com.oop.calculator.engine.Symbols
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats numbers for display according to the conventions of [locale]
 * (decimal and grouping separators, minus sign, exponent symbol).
 *
 * Values are rounded to [SIGNIFICANT_DIGITS] digits to hide binary floating-point
 * noise such as `0.1 + 0.2 = 0.30000000000000004`. Very large and very small
 * magnitudes are shown in scientific notation.
 */
class ResultFormatter(locale: Locale) {

    private val symbols = DecimalFormatSymbols.getInstance(locale)

    private val plainFormat = DecimalFormat("#,##0.###############", symbols)
    private val scientificFormat = DecimalFormat("0.###########E0", symbols)

    /** Decimal separator of the locale, e.g. `.` for English and `,` for Ukrainian. */
    val decimalSeparator: Char get() = symbols.decimalSeparator

    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        if (value == 0.0) return plainFormat.format(0L) // also turns −0 into 0
        val rounded = BigDecimal(value).round(MathContext(SIGNIFICANT_DIGITS))
        val magnitude = rounded.abs()
        return if (magnitude >= SCIENTIFIC_UPPER_BOUND || magnitude < SCIENTIFIC_LOWER_BOUND) {
            scientificFormat.format(rounded)
        } else {
            plainFormat.format(rounded)
        }
    }

    /** Converts an engine expression into its on-screen form for this locale. */
    fun localizeExpression(expression: String): String =
        expression.replace(Symbols.DECIMAL_POINT, decimalSeparator)

    companion object {
        const val SIGNIFICANT_DIGITS = 12
        private val SCIENTIFIC_UPPER_BOUND = BigDecimal("1E12")
        private val SCIENTIFIC_LOWER_BOUND = BigDecimal("1E-6")
    }
}
