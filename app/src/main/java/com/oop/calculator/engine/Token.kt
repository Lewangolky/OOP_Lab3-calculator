package com.oop.calculator.engine

/** Lexical unit of an expression, produced by [Tokenizer] and consumed by [ExpressionParser]. */
sealed interface Token {
    data class Number(val value: Double) : Token
    data class Constant(val symbol: Char, val value: Double) : Token
    data class Function(val function: MathFunction) : Token
    data class Operator(val symbol: Char) : Token
    data object LeftParen : Token
    data object RightParen : Token
    data object Factorial : Token
    data object Percent : Token
}
