plugins {
    // AGP 9 compile le Kotlin lui-même : le plugin org.jetbrains.kotlin.android ne s'applique plus ici.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("io.github.takahirom.roborazzi")
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
        targetSdk = 35
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
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

roborazzi {
    // Images de référence versionnées à côté des tests : une PR qui change l'affichage montre les images changées.
    outputDir.set(file("src/test/screenshots"))
}

ktlint {
    // Même version que celle visée par les règles Compose (io.nlopez.compose.rules:ktlint).
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

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.savedstate:savedstate-ktx:1.3.0")
    // L'APK s'installe hors du Play Store : c'est profileinstaller qui précompile au premier lancement le code listé
    // dans src/main/baseline-prof.txt et les profils fournis par Compose.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    testImplementation("junit:junit:4.13.2")
    // Captures de référence sur la JVM (Robolectric) : StandByScreenshotTest.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.75.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.75.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.75.0")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    // Parcours de bout en bout : lanceur, boîte de permission, écran de veille, rotation (dossier androidTest/.../e2e).
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    ktlintRuleset("io.nlopez.compose.rules:ktlint:0.6.8")
    detektPlugins("io.nlopez.compose.rules:detekt:0.6.8")
}
