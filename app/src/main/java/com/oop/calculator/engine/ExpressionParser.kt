package com.oop.calculator.engine

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Recursive-descent parser that evaluates a token list while parsing.
 *
 * Grammar, from the lowest precedence to the highest:
 * ```
 * expression := term (('+' | '−') term)*
 * term       := unary (('×' | '÷') unary | <implicit ×> power)*
 * unary      := ('+' | '−') unary | power
 * power      := postfix ('^' unary)?              right-associative
 * postfix    := primary ('!' | '%')*
 * primary    := number | constant | '(' expression ')' | function primaryArgument
 * ```
 * Juxtaposition means multiplication (`2π`, `3(1+2)`, `2sin(30)`), and closing
 * parentheses missing at the end of the input are added implicitly, as on a
 * pocket calculator: `sin(30` is read as `sin(30)`.
 */
class ExpressionParser(
    private val tokens: List<Token>,
    private val angleUnit: AngleUnit,
) {
    private var position = 0

    fun parse(): Double {
        if (tokens.isEmpty()) syntaxError("Expression is empty")
        val value = parseExpression()
        if (position < tokens.size) syntaxError("Unexpected ${tokens[position]} at token $position")
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            value = when (peek()) {
                PLUS -> {
                    position++
                    value + parseTerm()
                }
                MINUS -> {
                    position++
                    value - parseTerm()
                }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseUnary()
        while (true) {
            val next = peek()
            value = when {
                next == TIMES -> {
                    position++
                    value * parseUnary()
                }
                next == DIVIDE -> {
                    position++
                    val divisor = parseUnary()
                    if (divisor == 0.0) throw CalculationException(CalcError.DIVISION_BY_ZERO, "Division of $value by zero")
                    value / divisor
                }
                next != null && startsOperand(next) -> value * parsePower()
                else -> return value
            }
        }
    }

    private fun parseUnary(): Double = when (peek()) {
        MINUS -> {
            position++
            -parseUnary()
        }
        PLUS -> {
            position++
            parseUnary()
        }
        else -> parsePower()
    }

    private fun parsePower(): Double {
        val base = parsePostfix()
        if (peek() != POWER) return base
        position++
        val exponent = parseUnary()
        if (base == 0.0 && exponent < 0.0) {
            throw CalculationException(CalcError.DIVISION_BY_ZERO, "0 raised to negative power $exponent")
        }
        return checked(base.pow(exponent)) { "$base ^ $exponent" }
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (true) {
            value = when (peek()) {
                Token.Factorial -> {
                    position++
                    factorial(value)
                }
                Token.Percent -> {
                    position++
                    value / 100.0
                }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        val token = tokens.getOrNull(position++) ?: syntaxError("Expression ends where a number is expected")
        return when (token) {
            is Token.Number -> token.value
            is Token.Constant -> token.value
            Token.LeftParen -> parseParenthesized()
            is Token.Function -> {
                val argument = if (peek() == Token.LeftParen) {
                    position++
                    parseParenthesized()
                } else {
                    parsePower()
                }
                checked(token.function.apply(argument, angleUnit)) { "${token.function.symbol}($argument)" }
            }
            else -> syntaxError("Unexpected $token at token ${position - 1}")
        }
    }

    /** Parses the inside of a group whose `(` was already consumed. */
    private fun parseParenthesized(): Double {
        val value = parseExpression()
        when {
            peek() == Token.RightParen -> position++
            position >= tokens.size -> Unit // auto-close at the end of input
            else -> syntaxError("Expected ')' at token $position")
        }
        return value
    }

    private fun factorial(value: Double): Double {
        if (value < 0.0 || abs(value - round(value)) > INTEGER_TOLERANCE) {
            throw CalculationException(CalcError.FACTORIAL, "Factorial of $value is undefined")
        }
        if (value > MAX_FACTORIAL) throw CalculationException(CalcError.OVERFLOW, "$value! is too large")
        var result = 1.0
        for (factor in 2..value.roundToInt()) result *= factor
        return result
    }

    private fun peek(): Token? = tokens.getOrNull(position)

    private fun startsOperand(token: Token): Boolean =
        token is Token.Number || token is Token.Constant || token is Token.Function || token == Token.LeftParen

    private inline fun checked(value: Double, describe: () -> String): Double = when {
        value.isNaN() -> throw CalculationException(CalcError.DOMAIN, "${describe()} is undefined")
        value.isInfinite() -> throw CalculationException(CalcError.OVERFLOW, "${describe()} overflows")
        else -> value
    }

    private fun syntaxError(message: String): Nothing = throw CalculationException(CalcError.SYNTAX, message)

    private companion object {
        val PLUS = Token.Operator(Symbols.PLUS)
        val MINUS = Token.Operator(Symbols.MINUS)
        val TIMES = Token.Operator(Symbols.TIMES)
        val DIVIDE = Token.Operator(Symbols.DIVIDE)
        val POWER = Token.Operator(Symbols.POWER)

        /** 171! exceeds [Double.MAX_VALUE]. */
        const val MAX_FACTORIAL = 170
        const val INTEGER_TOLERANCE = 1e-9
    }
}
