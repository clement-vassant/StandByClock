plugins {
    // Versions dans gradle/libs.versions.toml.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    // Non appliqué (Kotlin intégré à AGP 9) : fixe seulement la version du compilateur Kotlin utilisée par AGP.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false
}
