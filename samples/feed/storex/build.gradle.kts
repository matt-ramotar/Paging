plugins {
    id("storex.android.application")
    alias(libs.plugins.compose)
}

android {
    namespace = "storex.feed"

    defaultConfig {
        applicationId = "storex.feed"
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/versions/9/previous-compilation-data.bin")
        }
    }
}

dependencies {
    implementation(projects.paging)
    implementation(projects.samples.feed.common)
}