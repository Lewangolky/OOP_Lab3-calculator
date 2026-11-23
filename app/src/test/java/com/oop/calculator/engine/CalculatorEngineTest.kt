package com.oop.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI

class CalculatorEngineTest {

    private val engine = CalculatorEngine()

    private fun eval(expression: String, unit: AngleUnit = AngleUnit.DEGREES) = engine.evaluate(expression, unit)

    private fun assertValue(expected: Double, expression: String, unit: AngleUnit = AngleUnit.DEGREES) =
        assertEquals(expression, expected, eval(expression, unit), 1e-9)

    private fun assertError(expected: CalcError, expression: String, unit: AngleUnit = AngleUnit.DEGREES) {
        val exception = assertThrows(CalculationException::class.java) { eval(expression, unit) }
        assertEquals(expression, expected, exception.error)
    }

    @Test
    fun `respects operator precedence`() {
        assertValue(14.0, "2+3×4")
        assertValue(20.0, "(2+3)×4")
        assertValue(1.0, "10−3×3")
        assertValue(2.5, "10÷4")
        assertValue(7.0, "1+2×3")
    }

    @Test
    fun `accepts ascii operators`() {
        assertValue(5.0, "10 - 2 * 3 + 6 / 6")
    }

    @Test
    fun `power is right associative and binds tighter than unary minus`() {
        assertValue(512.0, "2^3^2")
        assertValue(-4.0, "−2^2")
        assertValue(4.0, "(−2)^2")
        assertValue(0.5, "2^−1")
    }

    @Test
    fun `handles unary signs`() {
        assertValue(-5.0, "−5")
        assertValue(-6.0, "2×−3")
        assertValue(5.0, "2−−3")
        assertValue(3.0, "+3")
    }

    @Test
    fun `parses decimals and exponent literals`() {
        assertValue(0.5, ".5")
        assertValue(1.5e20, "1.5E20")
        assertValue(2.5e-7, "2.5E−7")
        assertValue(0.3, "0.1+0.2")
    }

    @Test
    fun `supports implicit multiplication`() {
        assertValue(2 * PI, "2π")
        assertValue(9.0, "3(1+2)")
        assertValue(6.0, "(1+1)(1+2)")
        assertValue(1.0, "2sin(30)")
    }

    @Test
    fun `closes missing parentheses at the end`() {
        assertValue(20.0, "(2+3×(4+2")
        assertValue(0.5, "sin(30")
    }

    @Test
    fun `evaluates trigonometry in degrees`() {
        assertValue(0.5, "sin(30)")
        assertValue(0.0, "sin(180)")
        assertValue(0.5, "cos(60)")
        assertValue(0.0, "cos(90)")
        assertValue(1.0, "tan(45)")
        assertValue(30.0, "asin(0.5)")
        assertValue(60.0, "acos(0.5)")
        assertValue(45.0, "atan(1)")
    }

    @Test
    fun `evaluates trigonometry in radians`() {
        assertValue(1.0, "sin(π÷2)", AngleUnit.RADIANS)
        assertValue(-1.0, "cos(π)", AngleUnit.RADIANS)
        assertValue(PI / 4, "atan(1)", AngleUnit.RADIANS)
    }

    @Test
    fun `evaluates logarithms roots and constants`() {
        assertValue(1.0, "ln(e)")
        assertValue(3.0, "log(1000)")
        assertValue(3.0, "√(9)")
        assertValue(3.0, "√9")
        assertValue(-3.0, "∛(−27)")
        assertValue(E * E, "e^2")
        assertValue(100.0, "10^(2)")
    }

    @Test
    fun `evaluates factorial and percent`() {
        assertValue(120.0, "5!")
        assertValue(1.0, "0!")
        assertValue(720.0, "3!!")
        assertValue(0.25, "25%")
        assertValue(50.0, "200×25%")
    }

    @Test
    fun `reports division by zero`() {
        assertError(CalcError.DIVISION_BY_ZERO, "1÷0")
        assertError(CalcError.DIVISION_BY_ZERO, "5÷(2−2)")
        assertError(CalcError.DIVISION_BY_ZERO, "0^−1")
    }

    @Test
    fun `reports domain errors`() {
        assertError(CalcError.DOMAIN, "√(−4)")
        assertError(CalcError.DOMAIN, "ln(0)")
        assertError(CalcError.DOMAIN, "log(−1)")
        assertError(CalcError.DOMAIN, "asin(2)")
        assertError(CalcError.DOMAIN, "tan(90)")
        assertError(CalcError.DOMAIN, "(−8)^(1÷3)")
    }

    @Test
    fun `reports factorial errors and overflow`() {
        assertError(CalcError.FACTORIAL, "(−1)!")
        assertError(CalcError.FACTORIAL, "2.5!")
        assertError(CalcError.OVERFLOW, "171!")
        assertError(CalcError.OVERFLOW, "10^400")
    }

    @Test
    fun `reports syntax errors`() {
        assertError(CalcError.SYNTAX, "")
        assertError(CalcError.SYNTAX, "2×")
        assertError(CalcError.SYNTAX, "×2")
        assertError(CalcError.SYNTAX, "2)")
        assertError(CalcError.SYNTAX, "()")
        assertError(CalcError.SYNTAX, ".")
        assertError(CalcError.SYNTAX, "2#3")
    }

    @Test
    fun `writes results back as literals that evaluate to the same value`() {
        val samples = listOf(0.0, 5.0, -5.0, 0.1 + 0.2, 1.0 / 3.0, 1.5e20, -2.5e-9, 123456.789)
        for (value in samples) {
            val literal = CalculatorEngine.toExpressionLiteral(value)
            assertEquals(literal, value, eval(literal), Math.abs(value) * 1e-14)
        }
        assertEquals("0.3", CalculatorEngine.toExpressionLiteral(0.1 + 0.2))
        assertEquals("−5", CalculatorEngine.toExpressionLiteral(-5.0))
        assertEquals("1.5E20", CalculatorEngine.toExpressionLiteral(1.5e20))
    }
}
