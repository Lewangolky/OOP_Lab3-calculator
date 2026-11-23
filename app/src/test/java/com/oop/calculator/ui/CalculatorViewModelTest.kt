package com.oop.calculator.ui

import androidx.lifecycle.SavedStateHandle
import com.oop.calculator.engine.AngleUnit
import com.oop.calculator.engine.CalcError
import com.oop.calculator.engine.MathFunction
import com.oop.calculator.engine.Symbols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorViewModelTest {

    private val savedState = SavedStateHandle()
    private val viewModel = CalculatorViewModel(savedState)
    private val state get() = viewModel.uiState.value

    private fun type(keys: String) {
        for (key in keys) {
            when {
                key.isDigit() -> viewModel.onDigit(key)
                key == Symbols.DECIMAL_POINT -> viewModel.onDecimalPoint()
                key in Symbols.BINARY_OPERATORS -> viewModel.onOperator(key)
                key == Symbols.LEFT_PAREN -> viewModel.onLeftParen()
                key == Symbols.RIGHT_PAREN -> viewModel.onRightParen()
                key == '=' -> viewModel.onEquals()
                else -> error("No key for '$key'")
            }
        }
    }

    @Test
    fun `builds an expression and shows a live preview`() {
        type("12+3")
        assertEquals("12+3", state.expression)
        assertEquals(15.0, state.preview!!, 0.0)
    }

    @Test
    fun `evaluates and records history`() {
        type("2×3=")
        assertEquals(6.0, state.result!!, 0.0)
        assertEquals("2×3", state.evaluatedExpression)
        assertEquals(listOf(HistoryEntry("2×3", 6.0)), state.history)
    }

    @Test
    fun `operator after a result continues from it, a digit starts over`() {
        type("2×3=+1")
        assertEquals("6+1", state.expression)
        assertNull(state.result)

        type("=5")
        assertEquals("5", state.expression)
    }

    @Test
    fun `replaces operators but allows a negative sign`() {
        type("5+×")
        assertEquals("5×", state.expression)
        type("−")
        assertEquals("5×−", state.expression)
        type("+")
        assertEquals("5+", state.expression)
    }

    @Test
    fun `ignores invalid keys`() {
        type("×")
        assertEquals("", state.expression)
        type("(+")
        assertEquals("(", state.expression)
        type(")")
        assertEquals("(", state.expression)
        viewModel.onFactorial()
        assertEquals("(", state.expression)
    }

    @Test
    fun `allows a single decimal point per number`() {
        type(".5.")
        assertEquals("0.5", state.expression)
        type("+.")
        assertEquals("0.5+0.", state.expression)
    }

    @Test
    fun `backspace removes whole function names`() {
        viewModel.onFunction(MathFunction.ASIN)
        assertEquals("asin(", state.expression)
        viewModel.onBackspace()
        assertEquals("", state.expression)
    }

    @Test
    fun `shows a typed error and keeps the expression`() {
        type("1÷0=")
        assertEquals(CalcError.DIVISION_BY_ZERO, state.error)
        assertEquals("1÷0", state.expression)
        assertTrue(state.history.isEmpty())

        viewModel.onBackspace()
        assertNull(state.error)
    }

    @Test
    fun `angle unit changes trigonometry`() {
        viewModel.onFunction(MathFunction.COS)
        type("0)")
        viewModel.onConstant(Symbols.PI)
        assertEquals(Math.PI, state.preview!!, 1e-12)

        viewModel.onBackspace()
        viewModel.onBackspace()
        viewModel.onBackspace()
        viewModel.onConstant(Symbols.PI)
        viewModel.onToggleAngleUnit()
        assertEquals(AngleUnit.RADIANS, state.angleUnit)
        assertEquals(-1.0, state.preview!!, 1e-12)
    }

    @Test
    fun `answer key inserts the last result`() {
        type("7×6=")
        viewModel.onClear()
        type("1+")
        viewModel.onAnswer()
        assertEquals("1+42", state.expression)
        viewModel.onAnswer()
        assertEquals("1+42×42", state.expression)
    }

    @Test
    fun `state survives recreation`() {
        type("2+2=")
        viewModel.onToggleInverse()
        viewModel.onToggleAngleUnit()

        val restored = CalculatorViewModel(savedState).uiState.value
        assertEquals(4.0, restored.result!!, 0.0)
        assertEquals(AngleUnit.RADIANS, restored.angleUnit)
        assertTrue(restored.inverse)
        assertEquals(state.history, restored.history)
    }

    @Test
    fun `clear history empties the list`() {
        type("1+1=")
        viewModel.onClearHistory()
        assertTrue(state.history.isEmpty())
        assertFalse(state.showsResult && state.history.isNotEmpty())
    }
}
