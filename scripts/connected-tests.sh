#!/usr/bin/env bash
# Tests sur un émulateur ou un téléphone branché (adb) :
#   1. tests Compose et parcours e2e sur l'APK debug ;
#   2. parcours e2e seuls sur l'APK release, passé par R8 comme celui qu'on publie : une classe retirée à tort
#      par R8 casse l'app publiée sans casser l'APK debug.
# Les captures des parcours sont copiées dans app/build/e2e-captures/{debug,release}, même si un test échoue.
set -uo pipefail
cd "$(dirname "$0")/.."

gradle() {
    if command -v mise >/dev/null; then mise exec -- ./gradlew "$@"; else ./gradlew "$@"; fi
}

captures=/sdcard/Download/standbyclock-e2e

pull_captures() {
    mkdir -p app/build/e2e-captures
    adb pull "$captures" "app/build/e2e-captures/$1" || true
    adb shell rm -rf "$captures" || true
}

gradle connectedDebugAndroidTest
debug=$?
pull_captures debug

gradle connectedReleaseAndroidTest -PtestBuildType=release \
    -Pandroid.testInstrumentationRunnerArguments.package=fr.clementvassant.standbyclock.e2e
release=$?
pull_captures release

[[ $debug -eq 0 && $release -eq 0 ]]
