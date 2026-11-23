package com.oop.calculator.engine

/**
 * Characters used in calculator expressions. The engine always works with these
 * locale-neutral symbols; the UI only swaps [DECIMAL_POINT] for the separator
 * of the current locale when it displays an expression.
 */
object Symbols {
    const val PLUS = '+'
    const val MINUS = '−' // −
    const val TIMES = '×' // ×
    const val DIVIDE = '÷' // ÷
    const val POWER = '^'
    const val FACTORIAL = '!'
    const val PERCENT = '%'
    const val LEFT_PAREN = '('
    const val RIGHT_PAREN = ')'
    const val DECIMAL_POINT = '.'
    const val EXPONENT = 'E'
    const val PI = 'π' // π
    const val EULER = 'e'

    val BINARY_OPERATORS: Set<Char> = setOf(PLUS, MINUS, TIMES, DIVIDE, POWER)
}
