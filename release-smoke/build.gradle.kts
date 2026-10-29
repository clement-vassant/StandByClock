plugins {
    // Module de test à part, qui vise l'APK release de :app, passé par R8 comme celui qu'on publie.
    id("com.android.test")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "fr.clementvassant.standbyclock.smoke"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    // Les tests tournent dans leur propre processus et pilotent l'app par l'écran et le shell seulement : dans le
    // processus de l'app, ils trouveraient les bibliothèques partagées (Kotlin, androidx.test…) renommées par R8.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Même nom que la variante visée de :app ; l'APK de test, lui, est signé avec la clé debug.
        create("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        // Pilote de l'écran de veille, partagé avec les parcours e2e de :app.
        getByName("main").kotlin.directories.add(rootProject.file("e2e-shared/java").path)
    }
}

// Seule la variante release a un sens ici : l'APK debug est déjà couvert par les tests de :app.
androidComponents {
    beforeVariants(selector().withBuildType("debug")) { it.enable = false }
}

ktlint {
    version.set("1.8.0")
    android.set(true)
}

detekt {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

// detekt 1.23 est compilé avec Kotlin 2.0.21 : il doit tourner avec cette version, pas celle du projet.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android.compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("junit:junit:4.13.2")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")

    // Mêmes règles que :app : la configuration detekt commune a une section Compose.
    ktlintRuleset("io.nlopez.compose.rules:ktlint:0.6.8")
    detektPlugins("io.nlopez.compose.rules:detekt:0.6.8")
}
