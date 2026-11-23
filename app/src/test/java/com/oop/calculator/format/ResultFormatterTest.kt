package com.oop.calculator.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResultFormatterTest {

    private val english = ResultFormatter(Locale.forLanguageTag("en-US"))
    private val ukrainian = ResultFormatter(Locale.forLanguageTag("uk-UA"))
    private val german = ResultFormatter(Locale.forLanguageTag("de-DE"))

    @Test
    fun `hides floating point noise`() {
        assertEquals("0.3", english.format(0.1 + 0.2))
        assertEquals("0.5", english.format(0.49999999999999994))
        assertEquals("0.333333333333", english.format(1.0 / 3.0))
    }

    @Test
    fun `formats integers and zero`() {
        assertEquals("42", english.format(42.0))
        assertEquals("0", english.format(-0.0))
        assertEquals("-7", english.format(-7.0))
    }

    @Test
    fun `groups thousands`() {
        assertEquals("1,234,567.5", english.format(1234567.5))
    }

    @Test
    fun `uses locale separators`() {
        val ukSymbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("uk-UA"))
        assertEquals(',', ukrainian.decimalSeparator)
        assertEquals("1${ukSymbols.groupingSeparator}234,5", ukrainian.format(1234.5))
        assertEquals("1.234,5", german.format(1234.5))
        assertEquals('.', english.decimalSeparator)
    }

    @Test
    fun `switches to scientific notation for extreme magnitudes`() {
        assertEquals("1.5E20", english.format(1.5e20))
        assertEquals("2.5E-9", english.format(2.5e-9))
        assertEquals("1,5E20", german.format(1.5e20))
        assertEquals("999,999,999,999", english.format(999_999_999_999.0))
    }

    @Test
    fun `localizes expressions`() {
        assertEquals("1,5+2", ukrainian.localizeExpression("1.5+2"))
        assertEquals("1.5+2", english.localizeExpression("1.5+2"))
    }
}
