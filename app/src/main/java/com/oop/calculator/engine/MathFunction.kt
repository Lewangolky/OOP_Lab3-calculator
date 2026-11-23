package com.oop.calculator.engine

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Unary functions supported by the calculator. [symbol] is how the function is
 * written in an expression; it is always followed by an opening parenthesis.
 */
enum class MathFunction(val symbol: String) {
    SIN("sin"),
    COS("cos"),
    TAN("tan"),
    ASIN("asin"),
    ACOS("acos"),
    ATAN("atan"),
    LN("ln"),
    LOG("log"),
    SQRT("√"), // √
    CBRT("∛"); // ∛

    fun apply(argument: Double, angleUnit: AngleUnit): Double = when (this) {
        SIN -> snapToZero(sin(angleUnit.toRadians(argument)))
        COS -> snapToZero(cos(angleUnit.toRadians(argument)))
        TAN -> {
            val radians = angleUnit.toRadians(argument)
            val cosine = cos(radians)
            if (abs(cosine) < TRIG_EPSILON) domainError("tan is undefined for $argument")
            snapToZero(sin(radians) / cosine)
        }
        ASIN -> {
            if (argument !in -1.0..1.0) domainError("asin argument $argument is outside [-1, 1]")
            angleUnit.fromRadians(asin(argument))
        }
        ACOS -> {
            if (argument !in -1.0..1.0) domainError("acos argument $argument is outside [-1, 1]")
            angleUnit.fromRadians(acos(argument))
        }
        ATAN -> angleUnit.fromRadians(atan(argument))
        LN -> {
            if (argument <= 0.0) domainError("ln argument $argument is not positive")
            ln(argument)
        }
        LOG -> {
            if (argument <= 0.0) domainError("log argument $argument is not positive")
            log10(argument)
        }
        SQRT -> {
            if (argument < 0.0) domainError("square root of negative $argument")
            sqrt(argument)
        }
        CBRT -> cbrt(argument)
    }

    companion object {
        /** Values this close to zero are floating-point noise of sin/cos, e.g. sin(180°). */
        private const val TRIG_EPSILON = 1e-15

        /** Longest symbols first, so that `asin` is not read as `a` + `sin`. */
        val BY_SYMBOL_LENGTH: List<MathFunction> = entries.sortedByDescending { it.symbol.length }

        private fun snapToZero(value: Double): Double = if (abs(value) < TRIG_EPSILON) 0.0 else value

        private fun domainError(message: String): Nothing =
            throw CalculationException(CalcError.DOMAIN, message)
    }
}
