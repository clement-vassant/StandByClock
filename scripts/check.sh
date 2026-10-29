#!/usr/bin/env bash
# Mêmes vérifications que la CI, à lancer avant de pousser.
#   scripts/check.sh          lint + tests unitaires + captures de référence + APK debug et release (R8)
#   scripts/check.sh --all    idem + tests d'interface et parcours e2e, aussi sur l'APK release (émulateur ou téléphone)
set -euo pipefail
cd "$(dirname "$0")/.."

gradle() {
    if command -v mise >/dev/null; then mise exec -- ./gradlew "$@"; else ./gradlew "$@"; fi
}

# Les images de référence sont rendues sous Linux, comme sur la CI : ailleurs (macOS), le rendu des polices diffère
# un peu, donc on produit seulement le rapport de comparaison (app/build/outputs/roborazzi) sans échouer.
if [[ "$(uname -s)" == "Linux" ]]; then screenshots=verifyRoborazziDebug; else screenshots=compareRoborazziDebug; fi

gradle ktlintCheck detekt lintDebug testDebugUnitTest "$screenshots" assembleDebug assembleRelease
if [[ "${1:-}" == "--all" ]]; then
    scripts/connected-tests.sh
fi
