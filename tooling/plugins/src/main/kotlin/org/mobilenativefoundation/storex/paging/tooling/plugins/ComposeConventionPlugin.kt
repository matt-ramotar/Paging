package org.mobilenativefoundation.storex.paging.tooling.plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

//            extensions.configure<LibraryExtension> {
//                configureAndroidCompose(this)
//            }
        }
    }
}