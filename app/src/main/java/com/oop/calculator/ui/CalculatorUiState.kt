package com.oop.calculator.ui

import com.oop.calculator.engine.AngleUnit
import com.oop.calculator.engine.CalcError

/** One finished calculation. */
data class HistoryEntry(val expression: String, val result: Double)

/**
 * Everything the calculator screen shows. Values are kept as numbers and
 * engine-format strings; the activity formats them for the current locale.
 */
data class CalculatorUiState(
    /** Expression being typed, in engine symbols. */
    val expression: String = "",
    /** Live result of [expression] while typing, if it can already be computed. */
    val preview: Double? = null,
    /** Result of the last `=`, shown instead of [expression] until the user types again. */
    val result: Double? = null,
    /** Expression that produced [result], shown above it. */
    val evaluatedExpression: String? = null,
    /** Why the last `=` failed, if it did. */
    val error: CalcError? = null,
    val angleUnit: AngleUnit = AngleUnit.DEGREES,
    /** "2nd" mode: inverse functions on the scientific keys. */
    val inverse: Boolean = false,
    /** Finished calculations, newest first. */
    val history: List<HistoryEntry> = emptyList(),
) {
    val showsResult: Boolean get() = result != null
}
