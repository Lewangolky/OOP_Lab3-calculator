plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.oop.calculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oop.calculator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    androidResources {
        // Builds res/xml/locales_config from the values-* folders, so a new
        // language only needs a new strings.xml (see res/resources.properties).
        generateLocaleConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// logback-android talks to Logcat and cannot run on a plain JVM, so local unit
// tests log through slf4j-simple instead (configured in simplelogger.properties).
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.github.tony19", module = "logback-android")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    implementation(libs.slf4j.api)
    implementation(libs.logback.android)

    testImplementation(libs.junit)
    testImplementation(libs.slf4j.simple)
}
