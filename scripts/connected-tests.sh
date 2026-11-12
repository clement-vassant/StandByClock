#!/usr/bin/env bash
# Tests sur un émulateur ou un téléphone branché (adb), par `mise run test:connected` (qui fournit Java 17) :
#   1. tests Compose et parcours e2e sur l'APK debug (:app) ;
#   2. parcours courts sur l'APK release, passé par R8 comme celui qu'on publie (:release-smoke) : une classe
#      retirée à tort par R8 casse l'app publiée sans casser l'APK debug.
# Les captures des parcours sont copiées dans app/build/e2e-captures/{debug,release}, même si un test échoue.
set -uo pipefail
cd "$(dirname "$0")/.."

captures=/sdcard/Download/standbyclock-e2e

pull_captures() {
    mkdir -p app/build/e2e-captures
    adb pull "$captures" "app/build/e2e-captures/$1" || true
    adb shell rm -rf "$captures" || true
}

./gradlew :app:connectedDebugAndroidTest
debug=$?
pull_captures debug

./gradlew :release-smoke:connectedReleaseAndroidTest
release=$?
pull_captures release

[[ $debug -eq 0 && $release -eq 0 ]]
