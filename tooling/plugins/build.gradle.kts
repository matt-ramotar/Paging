plugins {
    `kotlin-dsl`
}

group = "org.mobilenativefoundation.storex.paging"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.allopen)
    compileOnly(libs.mokkery)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatformConventionPlugin") {
            id = "storex.multiplatform"
            implementationClass =
                "org.mobilenativefoundation.storex.paging.tooling.plugins.KotlinMultiplatformConventionPlugin"
        }

        register("kotlinAndroidLibraryConventionPlugin") {
            id = "storex.android.library"
            implementationClass =
                "org.mobilenativefoundation.storex.paging.tooling.plugins.KotlinAndroidLibraryConventionPlugin"
        }

        register("androidApplicationConventionPlugin") {
            id = "storex.android.application"
            implementationClass =
                "org.mobilenativefoundation.storex.paging.tooling.plugins.AndroidApplicationConventionPlugin"
        }

        register("composeConventionPlugin") {
            id = "storex.compose"
            implementationClass = "org.mobilenativefoundation.storex.paging.tooling.plugins.ComposeConventionPlugin"
        }
    }
}