plugins {
    id("com.android.application") version "9.4.1" apply false
    // Non appliqué (Kotlin intégré à AGP 9) : fixe seulement la version du compilateur Kotlin utilisée par AGP.
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("io.github.takahirom.roborazzi") version "1.77.0" apply false
}
