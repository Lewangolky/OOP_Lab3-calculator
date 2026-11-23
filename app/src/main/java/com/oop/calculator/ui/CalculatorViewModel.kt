package com.oop.calculator.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.oop.calculator.engine.AngleUnit
import com.oop.calculator.engine.CalculationException
import com.oop.calculator.engine.CalculatorEngine
import com.oop.calculator.engine.MathFunction
import com.oop.calculator.engine.Symbols
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.slf4j.LoggerFactory

/**
 * Turns key presses into an expression, evaluates it and keeps the history.
 * The state survives configuration changes (rotation, language switch) and,
 * through [SavedStateHandle], process death.
 */
class CalculatorViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val engine: CalculatorEngine = CalculatorEngine(),
) : ViewModel() {

    private val log = LoggerFactory.getLogger(CalculatorViewModel::class.java)

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val state: CalculatorUiState get() = _uiState.value

    fun onDigit(digit: Char) {
        require(digit.isDigit()) { "Not a digit: $digit" }
        append(digit.toString(), continuesResult = false)
    }

    fun onDecimalPoint() {
        val base = if (state.showsResult) "" else state.expression
        val currentNumber = base.takeLastWhile { it.isDigit() || it == Symbols.DECIMAL_POINT }
        when {
            Symbols.DECIMAL_POINT in currentNumber -> log.debug("Ignored second decimal point")
            currentNumber.isEmpty() -> append("0${Symbols.DECIMAL_POINT}", continuesResult = false)
            else -> append(Symbols.DECIMAL_POINT.toString(), continuesResult = false)
        }
    }

    /** Binary operator: one of [Symbols.BINARY_OPERATORS]. */
    fun onOperator(operator: Char) {
        require(operator in Symbols.BINARY_OPERATORS) { "Not an operator: $operator" }
        // After `=` the expression holds the result, so an operator continues from it.
        val base = state.expression
        val last = base.lastOrNull()
        val newExpression = when {
            last == null || last == Symbols.LEFT_PAREN ->
                // Only a sign may start an expression or a group.
                if (operator == Symbols.MINUS) base + operator else return ignored(operator)
            last in Symbols.BINARY_OPERATORS -> {
                // 2×−3 and 2^−1 are allowed; otherwise the new operator replaces the previous one.
                if (operator == Symbols.MINUS && last != Symbols.MINUS && last != Symbols.PLUS) {
                    base + operator
                } else {
                    val trimmed = base.trimEnd { it in Symbols.BINARY_OPERATORS }
                    if (!endsWithOperand(trimmed)) return ignored(operator)
                    trimmed + operator
                }
            }
            else -> base + operator
        }
        setExpression(newExpression)
    }

    fun onFunction(function: MathFunction) =
        append(function.symbol + Symbols.LEFT_PAREN, continuesResult = false)

    fun onConstant(symbol: Char) {
        require(symbol == Symbols.PI || symbol == Symbols.EULER) { "Unknown constant: $symbol" }
        append(symbol.toString(), continuesResult = false)
    }

    /** `x²` and `x³`. */
    fun onPower(exponent: Int) = append("${Symbols.POWER}$exponent", continuesResult = true)

    /** `eˣ` and `10ˣ`. */
    fun onExponential(base: String) =
        append("$base${Symbols.POWER}${Symbols.LEFT_PAREN}", continuesResult = false)

    fun onFactorial() = append(Symbols.FACTORIAL.toString(), continuesResult = true)

    fun onPercent() = append(Symbols.PERCENT.toString(), continuesResult = true)

    fun onLeftParen() = append(Symbols.LEFT_PAREN.toString(), continuesResult = false)

    fun onRightParen() {
        val expression = state.expression
        val open = expression.count { it == Symbols.LEFT_PAREN }
        val closed = expression.count { it == Symbols.RIGHT_PAREN }
        val last = expression.lastOrNull()
        if (state.showsResult || open <= closed || last == Symbols.LEFT_PAREN || last in Symbols.BINARY_OPERATORS) {
            ignored(Symbols.RIGHT_PAREN)
            return
        }
        append(Symbols.RIGHT_PAREN.toString(), continuesResult = false)
    }

    /** Inserts the result of the latest calculation ("Ans"). */
    fun onAnswer() {
        val last = state.history.firstOrNull() ?: return ignored('A')
        val literal = CalculatorEngine.toExpressionLiteral(last.result)
        // Typed right after a number, "Ans" multiplies it instead of gluing digits together.
        val needsTimes = !state.showsResult && endsWithOperand(state.expression)
        append(if (needsTimes) "${Symbols.TIMES}$literal" else literal, continuesResult = false)
    }

    fun onBackspace() {
        if (state.showsResult) {
            onClear()
            return
        }
        val expression = state.expression
        if (expression.isEmpty()) return
        val functionCall = MathFunction.BY_SYMBOL_LENGTH
            .map { it.symbol + Symbols.LEFT_PAREN }
            .firstOrNull { expression.endsWith(it) }
        setExpression(expression.dropLast(functionCall?.length ?: 1))
    }

    fun onClear() {
        log.debug("Clear")
        update { it.copy(expression = "", preview = null, result = null, evaluatedExpression = null, error = null) }
    }

    fun onEquals() {
        val expression = state.expression
        if (state.showsResult || expression.isBlank()) return
        try {
            val result = engine.evaluate(expression, state.angleUnit)
            log.info("Calculated {} = {} ({})", expression, result, state.angleUnit)
            update {
                it.copy(
                    expression = CalculatorEngine.toExpressionLiteral(result),
                    preview = null,
                    result = result,
                    evaluatedExpression = expression,
                    error = null,
                    history = (listOf(HistoryEntry(expression, result)) + it.history).take(MAX_HISTORY),
                )
            }
        } catch (e: CalculationException) {
            log.warn("Cannot calculate '{}': {} ({})", expression, e.error, e.message)
            update { it.copy(error = e.error, preview = null) }
        }
    }

    fun onToggleAngleUnit() {
        val unit = state.angleUnit.toggled()
        log.info("Angle unit switched to {}", unit)
        update { it.copy(angleUnit = unit, preview = preview(it.expression, unit)) }
    }

    fun onToggleInverse() {
        update { it.copy(inverse = !it.inverse) }
        log.debug("Inverse mode: {}", state.inverse)
    }

    fun onHistoryEntrySelected(entry: HistoryEntry) {
        log.debug("Restoring '{}' from history", entry.expression)
        setExpression(entry.expression)
    }

    fun onClearHistory() {
        log.info("History cleared ({} entries)", state.history.size)
        update { it.copy(history = emptyList()) }
    }

    /**
     * Appends [text]. A postfix key ([continuesResult]: `!`, `%`, `x²`) needs a
     * number before it and, right after `=`, applies to the result; any other
     * key typed after `=` starts a new expression.
     */
    private fun append(text: String, continuesResult: Boolean) {
        val base = if (continuesResult || !state.showsResult) state.expression else ""
        if (continuesResult && !endsWithOperand(base)) return ignored(text.first())
        setExpression(base + text)
    }

    /** True if [expression] ends with something a postfix or binary operator can follow. */
    private fun endsWithOperand(expression: String): Boolean {
        val last = expression.lastOrNull() ?: return false
        return last.isDigit() || last in OPERAND_ENDINGS
    }

    private fun setExpression(expression: String) {
        log.debug("Expression: '{}'", expression)
        update {
            it.copy(
                expression = expression,
                preview = preview(expression, it.angleUnit),
                result = null,
                evaluatedExpression = null,
                error = null,
            )
        }
    }

    /** Live result while typing; only shown once there is something to compute. */
    private fun preview(expression: String, angleUnit: AngleUnit): Double? {
        if (expression.isEmpty() || expression.all { it.isDigit() || it == Symbols.DECIMAL_POINT }) return null
        return try {
            engine.evaluate(expression, angleUnit)
        } catch (e: CalculationException) {
            log.trace("No preview for '{}': {}", expression, e.error)
            null
        }
    }

    private fun ignored(key: Char) {
        log.debug("Key '{}' ignored for expression '{}'", key, state.expression)
    }

    private inline fun update(transform: (CalculatorUiState) -> CalculatorUiState) {
        _uiState.update(transform)
        saveState(state)
    }

    private fun saveState(state: CalculatorUiState) {
        savedState[KEY_EXPRESSION] = state.expression
        savedState[KEY_RESULT] = state.result
        savedState[KEY_EVALUATED] = state.evaluatedExpression
        savedState[KEY_ANGLE_UNIT] = state.angleUnit.name
        savedState[KEY_INVERSE] = state.inverse
        savedState[KEY_HISTORY_EXPRESSIONS] = ArrayList(state.history.map { it.expression })
        savedState[KEY_HISTORY_RESULTS] = state.history.map { it.result }.toDoubleArray()
    }

    private fun restoreState(): CalculatorUiState {
        val expressions = savedState.get<ArrayList<String>>(KEY_HISTORY_EXPRESSIONS).orEmpty()
        val results = savedState.get<DoubleArray>(KEY_HISTORY_RESULTS) ?: DoubleArray(0)
        val restored = CalculatorUiState(
            expression = savedState[KEY_EXPRESSION] ?: "",
            result = savedState[KEY_RESULT],
            evaluatedExpression = savedState[KEY_EVALUATED],
            angleUnit = savedState.get<String>(KEY_ANGLE_UNIT)?.let(AngleUnit::valueOf) ?: AngleUnit.DEGREES,
            inverse = savedState[KEY_INVERSE] ?: false,
            history = expressions.zip(results.asList(), ::HistoryEntry),
        )
        if (restored != CalculatorUiState()) log.info("Restored state: {} history entries", restored.history.size)
        return restored.copy(preview = if (restored.showsResult) null else preview(restored.expression, restored.angleUnit))
    }

    private companion object {
        const val MAX_HISTORY = 50
        val OPERAND_ENDINGS = setOf(
            Symbols.DECIMAL_POINT, Symbols.RIGHT_PAREN, Symbols.FACTORIAL,
            Symbols.PERCENT, Symbols.PI, Symbols.EULER,
        )
        const val KEY_EXPRESSION = "expression"
        const val KEY_RESULT = "result"
        const val KEY_EVALUATED = "evaluated_expression"
        const val KEY_ANGLE_UNIT = "angle_unit"
        const val KEY_INVERSE = "inverse"
        const val KEY_HISTORY_EXPRESSIONS = "history_expressions"
        const val KEY_HISTORY_RESULTS = "history_results"
    }
}
