# Contribuer

## Règles

- **Jamais de push direct sur `main`.** Chaque changement passe par une branche et une pull request dédiée, un sujet par PR.
- **Une PR n'est mergeable que si la CI est verte** (job `ci-ok`) et que la branche est à jour avec `main`.
- **Chaque changement arrive avec ses tests** : test unitaire JVM pour la logique, test Compose et capture de référence pour ce qui se voit à l'écran, parcours e2e pour ce qui passe par le système.

## Avant de pousser

```bash
mise install           # Java 17, une seule fois
scripts/check.sh       # ktlint, detekt, Android Lint, tests unitaires, captures de référence, APK
scripts/check.sh --all # idem + tests d'interface, parcours e2e, et parcours courts sur l'APK release (émulateur ou téléphone branché)
```

`mise exec -- ./gradlew ktlintFormat` corrige automatiquement la mise en forme.

## Ce que vérifie la CI

| Job | Commande | Rôle |
|---|---|---|
| Lint | `ktlintCheck detekt lintDebug` | style Kotlin (ktlint + règles Compose), code smells (detekt + règles Compose), erreurs Android (Lint, avertissements bloquants) |
| Tests unitaires | `testDebugUnitTest` | logique pure : plage de nuit, calendrier, anti-marquage, musique… |
| Captures de référence | `verifyRoborazziDebug` | l'écran de l'horloge (3 styles, jour/nuit, paysage/portrait) rendu sur la JVM et comparé aux images de `app/src/test/screenshots/` ; en cas d'écart, l'artefact `screenshot-diffs` montre la différence |
| Tests d'interface et parcours e2e | `scripts/connected-tests.sh` | tests Compose à heure fixe, puis parcours de bout en bout UI Automator (lanceur, réglages, autorisation de l'agenda, orientation Automatique et Toujours paysage, écran de veille, batterie et prochain réveil) sur un émulateur API 34 ; puis le module `release-smoke` ouvre l'horloge, les réglages et l'écran de veille sur l'APK release passé par R8 (tests dans leur propre processus, qui pilotent l'app par l'écran seulement) ; captures dans l'artefact `e2e-captures` |
| APK debug et release | `assembleDebug assembleRelease` | l'app compile dans les deux variantes ; la release passe par R8 (code et ressources inutilisés retirés, règles dans `app/proguard-rules.pro`) avec le profil de référence `app/src/main/baseline-prof.txt` ; l'APK debug est téléchargeable dans les artefacts du run |
| `ci-ok` | | vert seulement si tous les autres le sont ; c'est lui que la protection de `main` exige |

### Captures de référence

Un changement visible modifie les images de `app/src/test/screenshots/` : on les régénère avec `./gradlew recordRoborazziDebug` et elles apparaissent dans le diff de la PR, à relire comme le code. Elles sont rendues sous Linux comme sur la CI ; sur macOS, `scripts/check.sh` produit le rapport de comparaison sans échouer, car le rendu des polices diffère un peu. Pour régénérer sans Linux, prendre les images `*_actual.png` de l'artefact `screenshot-diffs` de la CI.

### Parcours e2e

Les parcours de `app/src/androidTest/java/fr/clementvassant/standbyclock/e2e/` pilotent l'app comme un utilisateur, en [UI Automator](https://developer.android.com/training/testing/ui-automator) : ils voient le lanceur, la vraie boîte de permission et l'écran de veille système. Ils créent leurs propres données (agenda local supprimé à la fin, alarmes programmées par `AlarmManager` puis annulées, batterie simulée par `dumpsys battery` puis rétablie), forcent le mode Jour et enregistrent une capture par étape dans `/sdcard/Download/standbyclock-e2e/`. L'agent `qa` les rejoue sur l'émulateur pliable, plié et déplié, avant tout scénario manuel.

Dependabot propose chaque semaine les mises à jour Gradle et chaque mois celles des actions GitHub ; elles passent par la même CI.

## Titres de PR et releases

Les PR sont mergées en squash et leur titre suit [Conventional Commits](https://www.conventionalcommits.org/fr/), vérifié par le workflow « Titre de PR ». Ce titre devient le message du commit sur `main`.

**Publier une release** : onglet Actions > **Release** > **Run workflow** (sur `main`). Le workflow s'appuie sur [github-tag-action](https://github.com/mathieudutour/github-tag-action) :

1. il part du dernier tag `vX.Y.Z` et choisit la version suivante d'après les commits depuis ce tag (on peut aussi la saisir dans le champ « Version ») :

   | Commits depuis la dernière release | Version (SemVer) |
   |---|---|
   | au moins un `feat!:` ou un pied `BREAKING CHANGE:` | majeure |
   | au moins un `feat:` | mineure (0.1.0 → 0.2.0) |
   | sinon (`fix:`, `perf:`, `deps:`, `docs:`…) | correctif (0.1.0 → 0.1.1) |

2. il compile l'APK release avec `versionName` = `X.Y.Z` et `versionCode` = `X*10000 + Y*100 + Z` ;
3. il crée le tag et la release GitHub, y attache `standbyclock-vX.Y.Z.apk` et son empreinte SHA-256, avec des notes groupées en « Nouveautés » (`feat`), « Corrections » (`fix`), « Performances », « Dépendances » (`deps`) et « Retours en arrière » (`revert`), chaque ligne liée à son commit. Un titre de PR clair donne donc une note de version claire.

La case « Essai à blanc » fait tout (version, notes, APK release signé) sans créer de tag ni de release : les notes s'affichent dans le résumé du run et l'APK est dans ses artefacts. Elle fonctionne depuis n'importe quelle branche, pour tester une modification du workflow avant de la merger.

La version n'est écrite dans aucun fichier : les builds locaux et de PR portent `0.0.1-dev`.

### Clé de signature

Android n'accepte une mise à jour que si elle est signée avec la même clé que la version installée. Sans clé, la release est signée avec une clé debug jetable (avertissement dans le run) : l'APK s'installe, mais la release suivante ne pourra pas le mettre à jour sans désinstaller. Pour signer les releases, une seule fois :

```bash
keytool -genkeypair -v -keystore alwaysclock-release.jks -alias alwaysclock \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i alwaysclock-release.jks | pbcopy   # macOS ; sous Linux : base64 -w0 alwaysclock-release.jks
```

Puis dans **Settings > Secrets and variables > Actions** du dépôt, créer `RELEASE_KEYSTORE_BASE64` (le contenu copié), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`alwaysclock`, nom d’origine du projet : la clé existante garde cet alias, une clé ne se renomme pas sans casser les mises à jour) et `RELEASE_KEY_PASSWORD`. Garder le fichier `.jks` et ses mots de passe en lieu sûr hors du dépôt : perdus, plus aucune mise à jour n'est possible.

## Écrire du code lisible

- Des noms qui disent ce qu'ils sont : pas d'abréviations obscures, des constantes nommées plutôt que des nombres magiques.
- Une fonction `@Composable` fait une chose ; au-delà d'une soixantaine de lignes, la découper.
- La logique (calculs de dates, de tailles, de jour/nuit) vit dans des fonctions pures testées en JVM, pas dans l'UI.
- Les commentaires expliquent le *pourquoi* (contrainte Android, choix anti-marquage), pas le *quoi*.
- Le seuil d'un outil ne se contourne pas avec `@Suppress` sans un commentaire qui dit pourquoi.

## Avec Claude Code

`CLAUDE.md` reprend ces règles pour Claude, et `.claude/agents/` définit des agents spécialisés : `code-reviewer` (relecture du diff), `test-writer` (tests), `ci-doctor` (CI rouge), `compose-ui` (vues Compose) et `qa` (suite e2e et scénarios sur l'émulateur du Mac).
