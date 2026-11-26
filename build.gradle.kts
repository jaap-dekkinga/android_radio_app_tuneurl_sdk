// AGP 9 compiles Kotlin itself (built-in Kotlin) and brings its own Kotlin
// Gradle plugin. Pin it to the catalog version so the Compose compiler plugin
// and KSP match it exactly.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}