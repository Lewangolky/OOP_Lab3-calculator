package com.oop.calculator.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oop.calculator.BuildConfig
import com.oop.calculator.R
import com.oop.calculator.databinding.ActivityMainBinding
import com.oop.calculator.engine.AngleUnit
import com.oop.calculator.engine.CalcError
import com.oop.calculator.engine.MathFunction
import com.oop.calculator.engine.Symbols
import com.oop.calculator.format.ResultFormatter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val log = LoggerFactory.getLogger(MainActivity::class.java)

    private val viewModel: CalculatorViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var formatter: ResultFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val locale = currentLocale()
        log.debug("onCreate (restored: {}), locale {}", savedInstanceState != null, locale.toLanguageTag())

        formatter = ResultFormatter(locale)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyWindowInsets()
        bindKeys()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onDestroy() {
        log.debug("onDestroy (configuration change: {})", isChangingConfigurations)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_history -> showHistory().let { true }
        R.id.action_language -> showLanguagePicker().let { true }
        R.id.action_about -> showAbout().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun bindKeys() {
        val basic = binding.basicKeys
        listOf(
            basic.btnDigit0, basic.btnDigit1, basic.btnDigit2, basic.btnDigit3, basic.btnDigit4,
            basic.btnDigit5, basic.btnDigit6, basic.btnDigit7, basic.btnDigit8, basic.btnDigit9,
        ).forEachIndexed { digit, button ->
            button.setOnClickListener { viewModel.onDigit('0' + digit) }
        }
        // The decimal key shows the separator of the current locale ("." or ",").
        basic.btnDecimal.text = formatter.decimalSeparator.toString()
        basic.btnDecimal.setOnClickListener { viewModel.onDecimalPoint() }
        basic.btnPlus.setOnClickListener { viewModel.onOperator(Symbols.PLUS) }
        basic.btnMinus.setOnClickListener { viewModel.onOperator(Symbols.MINUS) }
        basic.btnMultiply.setOnClickListener { viewModel.onOperator(Symbols.TIMES) }
        basic.btnDivide.setOnClickListener { viewModel.onOperator(Symbols.DIVIDE) }
        basic.btnPercent.setOnClickListener { viewModel.onPercent() }
        basic.btnEquals.setOnClickListener { viewModel.onEquals() }
        basic.btnAnswer.setOnClickListener { viewModel.onAnswer() }
        basic.btnClear.setOnClickListener { viewModel.onClear() }
        basic.btnBackspace.setOnClickListener { viewModel.onBackspace() }
        basic.btnBackspace.setOnLongClickListener { viewModel.onClear().let { true } }

        val scientific = binding.scientificKeys
        scientific.btnInverse.setOnClickListener { viewModel.onToggleInverse() }
        scientific.btnAngle.setOnClickListener { viewModel.onToggleAngleUnit() }
        scientific.btnSin.setOnClickListener { onFunctionKey(MathFunction.SIN, MathFunction.ASIN) }
        scientific.btnCos.setOnClickListener { onFunctionKey(MathFunction.COS, MathFunction.ACOS) }
        scientific.btnTan.setOnClickListener { onFunctionKey(MathFunction.TAN, MathFunction.ATAN) }
        scientific.btnSqrt.setOnClickListener { onFunctionKey(MathFunction.SQRT, MathFunction.CBRT) }
        scientific.btnLn.setOnClickListener {
            if (isInverse()) viewModel.onExponential(Symbols.EULER.toString()) else viewModel.onFunction(MathFunction.LN)
        }
        scientific.btnLog.setOnClickListener {
            if (isInverse()) viewModel.onExponential("10") else viewModel.onFunction(MathFunction.LOG)
        }
        scientific.btnSquare.setOnClickListener { viewModel.onPower(if (isInverse()) 3 else 2) }
        scientific.btnPower.setOnClickListener { viewModel.onOperator(Symbols.POWER) }
        scientific.btnParenOpen.setOnClickListener { viewModel.onLeftParen() }
        scientific.btnParenClose.setOnClickListener { viewModel.onRightParen() }
        scientific.btnFactorial.setOnClickListener { viewModel.onFactorial() }
        scientific.btnPi.setOnClickListener { viewModel.onConstant(Symbols.PI) }
        scientific.btnE.setOnClickListener { viewModel.onConstant(Symbols.EULER) }
    }

    private fun isInverse() = viewModel.uiState.value.inverse

    private fun onFunctionKey(function: MathFunction, inverse: MathFunction) =
        viewModel.onFunction(if (isInverse()) inverse else function)

    private fun render(state: CalculatorUiState) {
        val display = binding.display
        display.expression.text = when {
            state.result != null -> formatter.format(state.result)
            state.expression.isEmpty() -> formatter.format(0.0)
            else -> formatter.localizeExpression(state.expression)
        }
        display.evaluatedExpression.text = state.evaluatedExpression
            ?.let { getString(R.string.evaluated_expression, formatter.localizeExpression(it)) }
            .orEmpty()

        val error = state.error
        display.resultPreview.text = when {
            error != null -> getString(error.messageRes())
            state.preview != null -> getString(R.string.preview_result, formatter.format(state.preview))
            else -> ""
        }
        display.resultPreview.setTextColor(
            MaterialColors.getColor(
                display.resultPreview,
                if (error != null) androidx.appcompat.R.attr.colorError else com.google.android.material.R.attr.colorOnSurfaceVariant,
            ),
        )

        val angleLabel = getString(if (state.angleUnit == AngleUnit.DEGREES) R.string.angle_degrees else R.string.angle_radians)
        display.angleIndicator.text = angleLabel
        display.angleIndicator.contentDescription = getString(R.string.cd_angle_indicator, angleLabel)
        display.inverseIndicator.visibility = if (state.inverse) View.VISIBLE else View.INVISIBLE

        val scientific = binding.scientificKeys
        scientific.btnAngle.text = angleLabel
        scientific.btnInverse.isChecked = state.inverse
        val inv = state.inverse
        scientific.btnSin.setKey(inv, R.string.key_sin, R.string.cd_sin, R.string.key_asin, R.string.cd_asin)
        scientific.btnCos.setKey(inv, R.string.key_cos, R.string.cd_cos, R.string.key_acos, R.string.cd_acos)
        scientific.btnTan.setKey(inv, R.string.key_tan, R.string.cd_tan, R.string.key_atan, R.string.cd_atan)
        scientific.btnLn.setKey(inv, R.string.key_ln, R.string.cd_ln, R.string.key_exp, R.string.cd_exp)
        scientific.btnLog.setKey(inv, R.string.key_log, R.string.cd_log, R.string.key_ten_power, R.string.cd_ten_power)
        scientific.btnSqrt.setKey(inv, R.string.key_sqrt, R.string.cd_sqrt, R.string.key_cbrt, R.string.cd_cbrt)
        scientific.btnSquare.setKey(inv, R.string.key_square, R.string.cd_square, R.string.key_cube, R.string.cd_cube)
    }

    private fun Button.setKey(
        inverse: Boolean,
        @StringRes label: Int,
        @StringRes description: Int,
        @StringRes inverseLabel: Int,
        @StringRes inverseDescription: Int,
    ) {
        setText(if (inverse) inverseLabel else label)
        contentDescription = getString(if (inverse) inverseDescription else description)
    }

    private fun showHistory() {
        val history = viewModel.uiState.value.history
        log.debug("Showing history with {} entries", history.size)
        val dialog = MaterialAlertDialogBuilder(this)
            .setNegativeButton(R.string.action_close, null)
        if (history.isEmpty()) {
            dialog.setTitle(R.string.history_title).setMessage(R.string.history_empty)
        } else {
            val items = history.map {
                getString(R.string.history_item, formatter.localizeExpression(it.expression), formatter.format(it.result))
            }
            dialog.setTitle(resources.getQuantityString(R.plurals.history_count, history.size, history.size))
                .setItems(items.toTypedArray()) { _, which -> viewModel.onHistoryEntrySelected(history[which]) }
                .setNeutralButton(R.string.history_clear) { _, _ -> viewModel.onClearHistory() }
        }
        dialog.show()
    }

    /**
     * Lets the user pick the app language independently of the system one.
     * AppCompat applies it to all activities and persists it (see the
     * AppLocalesMetadataHolderService entry in the manifest).
     */
    private fun showLanguagePicker() {
        val tags = listOf("") + resources.getStringArray(R.array.supported_language_tags)
        val names = tags.map { tag ->
            if (tag.isEmpty()) {
                getString(R.string.language_system)
            } else {
                // Every language is named in itself, e.g. "Українська", "Deutsch".
                val locale = Locale.forLanguageTag(tag)
                locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
            }
        }
        val current = AppCompatDelegate.getApplicationLocales()
        val checked = if (current.isEmpty) 0 else tags.indexOfFirst { it == current[0]?.language }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(names.toTypedArray(), checked) { dialog, which ->
                dialog.dismiss()
                val tag = tags[which]
                log.info("App language changed to '{}'", tag.ifEmpty { "system default" })
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                )
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME))
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun currentLocale(): Locale =
        ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault()

    @StringRes
    private fun CalcError.messageRes(): Int = when (this) {
        CalcError.SYNTAX -> R.string.error_syntax
        CalcError.DIVISION_BY_ZERO -> R.string.error_division_by_zero
        CalcError.DOMAIN -> R.string.error_domain
        CalcError.FACTORIAL -> R.string.error_factorial
        CalcError.OVERFLOW -> R.string.error_overflow
    }
}
