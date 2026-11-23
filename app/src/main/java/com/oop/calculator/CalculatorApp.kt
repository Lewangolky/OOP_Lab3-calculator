package com.oop.calculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import org.slf4j.LoggerFactory

class CalculatorApp : Application() {

    private val log = LoggerFactory.getLogger(CalculatorApp::class.java)

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        DynamicColors.applyToActivitiesIfAvailable(this)
        log.info(
            "Started {} {} ({}), app locales: '{}', system locales: {}",
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            BuildConfig.BUILD_TYPE,
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            resources.configuration.locales.toLanguageTags(),
        )
    }

    /** Writes uncaught exceptions to the log file before the default handler kills the app. */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log.error("Uncaught exception in thread {}", thread.name, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
