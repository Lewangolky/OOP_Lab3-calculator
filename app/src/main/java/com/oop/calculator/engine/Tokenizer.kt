package com.oop.calculator.engine

import kotlin.math.E
import kotlin.math.PI

/**
 * Splits an expression string into [Token]s.
 *
 * Besides the calculator symbols from [Symbols] it also accepts the ASCII
 * operators `-`, `*` and `/`, so expressions can be typed or pasted freely.
 */
object Tokenizer {

    private val SINGLE_CHAR_TOKENS: Map<Char, Token> = mapOf(
        Symbols.PLUS to Token.Operator(Symbols.PLUS),
        Symbols.MINUS to Token.Operator(Symbols.MINUS),
        '-' to Token.Operator(Symbols.MINUS),
        Symbols.TIMES to Token.Operator(Symbols.TIMES),
        '*' to Token.Operator(Symbols.TIMES),
        Symbols.DIVIDE to Token.Operator(Symbols.DIVIDE),
        '/' to Token.Operator(Symbols.DIVIDE),
        Symbols.POWER to Token.Operator(Symbols.POWER),
        Symbols.LEFT_PAREN to Token.LeftParen,
        Symbols.RIGHT_PAREN to Token.RightParen,
        Symbols.FACTORIAL to Token.Factorial,
        Symbols.PERCENT to Token.Percent,
        Symbols.PI to Token.Constant(Symbols.PI, PI),
    )

    private val EXPONENT_SIGNS = setOf('+', '-', Symbols.MINUS)

    fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < expression.length) {
            val char = expression[index]
            if (char.isWhitespace()) {
                index++
                continue
            }
            if (char.isDigit() || char == Symbols.DECIMAL_POINT) {
                val end = scanNumber(expression, index)
                tokens += Token.Number(parseNumber(expression.substring(index, end)))
                index = end
                continue
            }
            SINGLE_CHAR_TOKENS[char]?.let {
                tokens += it
                index++
                continue
            }
            // Function names are matched before the constant e, so that no name is split up.
            val function = MathFunction.BY_SYMBOL_LENGTH.firstOrNull { expression.startsWith(it.symbol, index) }
            when {
                function != null -> {
                    tokens += Token.Function(function)
                    index += function.symbol.length
                }
                char == Symbols.EULER -> {
                    tokens += Token.Constant(Symbols.EULER, E)
                    index++
                }
                else -> throw CalculationException(
                    CalcError.SYNTAX,
                    "Unexpected character '$char' at position $index",
                )
            }
        }
        return tokens
    }

    /** Returns the index right after the number literal at [start]: digits, one point, optional exponent. */
    private fun scanNumber(expression: String, start: Int): Int {
        var index = start
        var seenPoint = false
        while (index < expression.length) {
            val char = expression[index]
            when {
                char.isDigit() -> index++
                char == Symbols.DECIMAL_POINT && !seenPoint -> {
                    seenPoint = true
                    index++
                }
                else -> break
            }
        }
        if (index < expression.length && expression[index] == Symbols.EXPONENT) {
            var exponentEnd = index + 1
            if (exponentEnd < expression.length && expression[exponentEnd] in EXPONENT_SIGNS) exponentEnd++
            val digitsStart = exponentEnd
            while (exponentEnd < expression.length && expression[exponentEnd].isDigit()) exponentEnd++
            if (exponentEnd > digitsStart) index = exponentEnd
        }
        return index
    }

    private fun parseNumber(literal: String): Double {
        if (literal == Symbols.DECIMAL_POINT.toString()) {
            throw CalculationException(CalcError.SYNTAX, "A lone decimal point is not a number")
        }
        return literal.replace(Symbols.MINUS, '-').toDouble()
    }
}
