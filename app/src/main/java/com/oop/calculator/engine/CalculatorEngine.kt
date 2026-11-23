package com.oop.calculator.engine

import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.MathContext

/** Entry point of the calculation engine: tokenizes, parses and evaluates an expression. */
class CalculatorEngine {

    private val log = LoggerFactory.getLogger(CalculatorEngine::class.java)

    /**
     * Evaluates [expression] written with [Symbols].
     *
     * @throws CalculationException if the expression is malformed or cannot be computed.
     */
    fun evaluate(expression: String, angleUnit: AngleUnit): Double {
        val tokens = Tokenizer.tokenize(expression)
        log.trace("Tokens of '{}': {}", expression, tokens)
        val result = ExpressionParser(tokens, angleUnit).parse()
        log.debug("'{}' [{}] = {}", expression, angleUnit, result)
        return result
    }

    companion object {
        private val PLAIN_UPPER_BOUND = BigDecimal("1E15")
        private val PLAIN_LOWER_BOUND = BigDecimal("1E-6")

        /**
         * Writes [value] back as an expression literal (rounded to 15 significant
         * digits), so a result can be reused as the start of a new expression.
         */
        fun toExpressionLiteral(value: Double): String {
            require(value.isFinite()) { "Only finite values can be written as a literal: $value" }
            if (value == 0.0) return "0"
            val decimal = BigDecimal(value).round(MathContext(15)).stripTrailingZeros()
            val magnitude = decimal.abs()
            val text = if (magnitude >= PLAIN_UPPER_BOUND || magnitude < PLAIN_LOWER_BOUND) {
                decimal.toString().replace("E+", "E")
            } else {
                decimal.toPlainString()
            }
            return text.replace('-', Symbols.MINUS)
        }
    }
}
