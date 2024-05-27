plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.ktlint)
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
    }
}

allprojects {
    repositories {
        mavenCentral()
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
