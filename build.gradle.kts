plugins {
    //alias(libs.plugins.android.application) apply false
    // alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.allopen) apply false
    alias(libs.plugins.kotlin.plugin.parcelize) apply false
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath(libs.kover.plugin)
        classpath(libs.dokka.gradle.plugin)
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
    }
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    additionalEditorconfig.set(
        mapOf(
            "max_line_length" to "200",
        ),
    )
}

