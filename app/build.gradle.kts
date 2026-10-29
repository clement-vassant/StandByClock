plugins {
    // AGP 9 compile le Kotlin lui-même : le plugin org.jetbrains.kotlin.android ne s'applique plus ici.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.roborazzi)
}

// Version SemVer de l'app : la release la passe en -PappVersion=X.Y.Z (voir release.yml) ;
// les builds locaux et de PR portent une version de développement.
val appVersionName = providers.gradleProperty("appVersion").getOrElse("0.0.1-dev")

// Android n'accepte une mise à jour que si versionCode augmente : X.Y.Z devient X*10000 + Y*100 + Z.
val versionCodeStep = 100
val appVersionCode =
    appVersionName.substringBefore("-").split(".").map(String::toInt).let { (major, minor, patch) ->
        require(minor < versionCodeStep && patch < versionCodeStep) { "versionCode ne peut pas encoder $appVersionName" }
        (major * versionCodeStep + minor) * versionCodeStep + patch
    }

// Clé de signature des releases, fournie par la CI depuis les secrets GitHub (voir CONTRIBUTING.md).
// Sans elle (build local), l'APK release est signé avec la clé debug pour rester installable.
val releaseKeystorePath: String? = System.getenv("RELEASE_KEYSTORE_PATH")

android {
    namespace = "fr.clementvassant.standbyclock"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.clementvassant.standbyclock"
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 retire le code et les ressources inutilisés et optimise le reste : APK bien plus léger.
            // Les composants déclarés dans le manifeste (activités, écran de veille, écouteur de notifications)
            // sont gardés d'office. Le module release-smoke vérifie sur cet APK que l'app démarre toujours.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (releaseKeystorePath != null) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        // Déclare le français et l'anglais à Android 13+, qui propose alors de choisir la langue de l'app
        // dans ses réglages, indépendamment de celle du téléphone (langue de values/ : res/resources.properties).
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        // Expose la version à l'app et aux tests (AppVersionTest).
        buildConfig = true
    }
    sourceSets {
        // Pilote de l'écran de veille, partagé avec le module release-smoke.
        getByName("androidTest").kotlin.directories.add(rootProject.file("e2e-shared/java").path)
    }
    testOptions {
        // Robolectric (captures Roborazzi) a besoin des ressources de l'app dans les tests JVM.
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        // Les avertissements importants font échouer la CI ; les avertissements de mise à jour de dépendances
        // sont suivis par Dependabot et ne bloquent pas.
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

roborazzi {
    // Images de référence versionnées à côté des tests : une PR qui change l'affichage montre les images changées.
    outputDir.set(file("src/test/screenshots"))
}

ktlint {
    // Même version que celle visée par les règles Compose (voir gradle/libs.versions.toml).
    version.set(libs.versions.ktlint)
    android.set(true)
}

detekt {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

// detekt 1.23 est compilé avec une version de Kotlin plus ancienne (detektKotlin) : il doit tourner avec elle.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.detektKotlin.get())
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.savedstate.ktx)
    // L'APK s'installe hors du Play Store : c'est profileinstaller qui précompile au premier lancement le code listé
    // dans src/main/baseline-prof.txt et les profils fournis par Compose.
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    // Captures de référence sur la JVM (Robolectric) : StandByScreenshotTest.
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Parcours de bout en bout : lanceur, boîte de permission, écran de veille, rotation (dossier androidTest/.../e2e).
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.compose.ui.test.manifest)

    ktlintRuleset(libs.compose.rules.ktlint)
    detektPlugins(libs.compose.rules.detekt)
}
