pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "StandByClock"
include(":app")
// Parcours sur l'APK release passé par R8 (voir release-smoke/build.gradle.kts).
include(":release-smoke")
