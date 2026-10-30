# StandBy Clock

[![CI](https://github.com/clement-vassant/StandByClock/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/clement-vassant/StandByClock/actions/workflows/ci.yml)

**Le mode StandBy de l'iPhone sur Android : une grande horloge pendant que le téléphone charge, en paysage comme en portrait.**

Fonctionne sur tout téléphone ou tablette Android 10 ou plus récent. Appareil de test : un Galaxy Z Fold (voir [Écrans pliables](#écrans-pliables)).

| Téléphone, mode nuit | Téléphone, analogique et calendrier |
|---|---|
| ![Horloge digitale en mode nuit sur un téléphone](screenshots/fold-01.png) | ![Horloge analogique et calendrier sur un téléphone](screenshots/fold-analog.png) |
| **Grand écran, Digital** | **Grand écran, Minimal en mode nuit** |
| ![Horloge digitale et calendrier sur le grand écran](screenshots/01-digital.png) | ![Horloge minimale en mode nuit sur le grand écran](screenshots/04-minimal-nuit.png) |
| **Portrait, Digital** | **Portrait, analogique** |
| ![Écran de veille en portrait : horloge digitale en haut, calendrier dessous](screenshots/portrait-digital.png) | ![Écran de veille en portrait : cadran analogique en haut, calendrier dessous](screenshots/portrait-analog.png) |
| **Grand écran, événements du jour** | **Portrait, événements du jour** |
| ![Semaine en cours et événements du jour à côté de l'horloge digitale](screenshots/evenements-digital.png) | ![Écran de veille en portrait : semaine et événements du jour en haut, horloge minimale dessous](screenshots/evenements-portrait.png) |

## Pourquoi

Sur Android, l'écran toujours allumé (Always On Display) est réservé au système et au fabricant : une app ne peut pas y dessiner. StandBy Clock passe donc par deux autres moyens :

- **l'écran de veille Android**, qui se lance tout seul quand le téléphone charge ;
- **une app plein écran**, à ouvrir soi-même : le téléphone passe en paysage et l'écran reste allumé.

## Fonctionnalités

- **Trois styles d'horloge** : Digital (grands chiffres), Analogique (cadran à aiguilles) et Minimal (chiffres fins). Le choix est mémorisé.
- **Calendrier du mois** à côté de l'horloge (dessous en portrait), avec le jour courant entouré. Un appui long l'affiche ou le masque.
- **Événements du jour** : tant qu'il reste un événement aujourd'hui dans les agendas cochés, le mois laisse la place à la semaine en cours et à la liste du jour (4 lignes au plus, puis « +N autres ») : « Journée », « → 15:00 » pour un événement en cours, sinon l'heure de début. Les événements terminés disparaissent, les invitations refusées sont masquées ; sans événement, et toujours la nuit, le mois revient. Les agendas se cochent un par un dans les réglages (aucun au départ), après avoir autorisé la lecture de l'agenda. La liste se met à jour dès que l'agenda change, sans interroger régulièrement.
- **Batterie et prochain réveil**, sur la ligne de la date (sous le cadran en analogique) : le niveau de batterie avec un éclair pendant la charge, ou téléphone débranché sous 20 % ; l'heure du prochain réveil s'il sonne dans les 24 heures. La nuit, seul le réveil reste (et la batterie faible). Aucune autorisation à donner.
- **Musique en cours** (Spotify ou tout lecteur) : quand un morceau joue, ou est en pause depuis moins de 10 minutes, une barre s'affiche sous l'heure, dans les trois styles (les chiffres ou le cadran se réduisent un peu pour lui faire place). Calendrier masqué par l'appui long, c'est une grande carte qui prend sa place. Barre et carte montrent la pochette atténuée (en gris la nuit), le titre, l'artiste et des boutons précédent, lecture/pause, suivant et « j'aime » (si le lecteur le propose, comme Spotify). Il faut accorder à l'app l'*accès aux notifications* une fois, depuis ses réglages.
- **12 h ou 24 h** : l'heure suit le format choisi dans les paramètres du téléphone (Date et heure), même s'il change pendant que l'horloge tourne. En 12 h, la grande horloge garde deux chiffres sans AM/PM (« 02:05 ») ; les horaires de l'agenda, de la plage de nuit et du prochain réveil, eux, portent AM ou PM.
- **Français ou anglais** : l'app suit la langue du téléphone, en anglais sur un téléphone en anglais et en français sinon ; depuis Android 13, on peut aussi choisir sa langue dans les paramètres de l'app. La date suit l'ordre de la région (« mardi 20 octobre », « Tuesday, October 20 » aux États-Unis, « Tuesday 20 October » au Royaume-Uni), et la semaine des calendriers commence le jour d'usage de la région, avec sa numérotation (lundi en France, dimanche aux États-Unis).
- **Jour ou nuit rouge** : en *Auto*, la nuit suit une plage horaire (22:00 → 07:00 par défaut) ; on peut aussi forcer *Jour* ou *Nuit*. Le mode nuit passe en rouge sombre à luminosité minimale pour préserver la vision nocturne.
- **Gestes** :
  - balayer à gauche ou à droite pour changer de style ;
  - toucher pour inverser jour et nuit jusqu'à la prochaine bascule programmée ;
  - appui long pour afficher ou masquer le calendrier ;
  - double toucher pour ouvrir les réglages.
- **Lancement pendant la charge** grâce à l'écran de veille Android.
- **Paysage ou portrait** : en paysage, l'horloge et le calendrier sont côte à côte ; en portrait (téléphone debout sur son chargeur, par exemple), l'horloge passe en haut et le calendrier dessous. Le réglage *Orientation* s'applique à l'app comme à l'écran de veille : *Automatique* suit la rotation du téléphone, *Toujours paysage* garde l'app en paysage et, dans l'écran de veille, tourne l'horloge d'un quart de tour si la rotation automatique est coupée.
- **Une seule icône** : *StandBy Clock* se lance depuis vos routines ou outils d'automatisation (par exemple *Modes et routines* sur Samsung, ou Tasker), à heure fixe ou au début d'un mode. Un appui long sur l'icône propose *Horloge jour* et *Horloge nuit* pour forcer le mode une fois.
- **Réglages** : style, calendrier, événements du jour et agendas affichés, musique en cours, jour/nuit et plage de nuit, orientation, délai d'extinction.
- **Protection de l'écran OLED** : le contenu suit une orbite lente, l'horloge et le calendrier échangent leur place chaque heure, la luminosité est limitée et l'horloge s'arrête toute seule après 2 h par défaut. Détails dans [Protection de l'écran](#protection-de-lécran).

Pas de publicité, pas de réseau. Deux autorisations facultatives : l'accès aux notifications pour la musique en cours, la lecture de l'agenda pour les événements du jour.

<details>
<summary>Plus de captures</summary>

| Minimal | Sans calendrier | Réglages |
|---|---|---|
| ![Minimal](screenshots/03-minimal.png) | ![Sans calendrier](screenshots/05-minimal-sans-calendrier.png) | ![Réglages](screenshots/08-reglages.png) |

| Analogique et événements du jour |
|---|
| ![Cadran analogique à côté de la semaine et des événements du jour](screenshots/evenements-analog.png) |

</details>

## Démarrage rapide

Le plus simple : télécharger l'APK `standbyclock-vX.Y.Z.apk` de la [dernière release](https://github.com/clement-vassant/StandByClock/releases/latest) et l'ouvrir sur le téléphone. La version en cours de développement est dans l'onglet [Actions](https://github.com/clement-vassant/StandByClock/actions/workflows/ci.yml) (dernière exécution réussie, artefact **standbyclock-debug**). Pour compiler soi-même, avec le SDK Android installé :

1. **Java 17 avec [mise](https://mise.jdx.dev)** :
   ```bash
   mise install
   ```
2. **Compiler l'APK** :
   ```bash
   ./gradlew assembleDebug
   ```
   (ou `mise exec -- ./gradlew assembleDebug` si mise n'est pas activé dans le shell)
3. **Installer sur le téléphone** (débogage USB ou sans fil activé dans les options pour les développeurs) :
   ```bash
   # par câble
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   # ou sans fil : Débogage sans fil > Associer l'appareil avec un code d'association
   adb pair <ip>:<port_association> <code>
   adb connect <ip>:<port_connexion>
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Feuille de route

- Un 4e style d'horloge inspiré de celui d'Apple.
- Musique : barre de progression et choix du lecteur quand plusieurs jouent.

## Programmer avec vos routines

*StandBy Clock* suit le mode Auto : le jour et la nuit dépendent de la plage horaire réglée. L'app se lance depuis n'importe quel outil d'automatisation capable d'ouvrir une app : Tasker, Automate, ou les routines intégrées de certains fabricants. Le principe :
1. créer une routine ;
2. *Si* : par exemple une **heure** (22:30, tous les jours), **en charge**, ou le début d'un mode (Sommeil...) ;
3. *Alors* : **ouvrir une app** > *StandBy Clock*.

Exemple sur Samsung : Paramètres > Modes et routines > Routines > **+**, puis *Applications > Ouvrir une app*.

Pour forcer le jour ou la nuit le temps d'une séance, faites un appui long sur l'icône : *Horloge jour* ou *Horloge nuit*. Les outils qui acceptent les raccourcis d'app (Tasker, par exemple) peuvent aussi les lancer.

## Écrans pliables

Sur un téléphone pliable, l'horloge s'affiche sur l'écran actif : l'écran extérieur quand il est plié (posé sur un dock, par exemple), le grand écran quand il est ouvert. Les captures ci-dessus viennent de l'écran extérieur d'un Galaxy Z Fold. Tenu ouvert en portrait, l'écran de veille affiche l'horloge et le calendrier l'un au-dessus de l'autre ; l'app plein écran, elle, reste dans une bande paysage (Android 16 n'y fait pas tourner l'écran).

## Protection de l'écran

Les fabricants d'écrans OLED (Samsung notamment) conseillent d'éviter les images fixes affichées longtemps, de baisser la luminosité et de préférer les fonds sombres. L'app applique :

- **Fond noir pur** : les pixels OLED du fond restent éteints.
- **Orbite anti-marquage** : tout le contenu se déplace chaque minute sur une spirale lente jusqu'à ±20 dp, sans jamais rester deux minutes au même endroit.
- **Inversion des côtés chaque heure** : heures paires, horloge à gauche et calendrier à droite (en portrait : horloge en haut, calendrier dessous) ; heures impaires, l'inverse.
- **Moins de pixels allumés** : pas de blanc pur (gris clair `#BDBDBD` le jour), jour du calendrier entouré d'un anneau fin plutôt que d'un disque plein, mode nuit rouge sombre.
- **Luminosité plafonnée** : 35 % le jour, minimale la nuit dans l'app ; l'écran de veille garde le jour le niveau atténué du système et passe lui aussi au minimum la nuit.
- **Arrêt automatique** : réglage *Éteindre après* (1 h, 2 h, 4 h, 8 h, Jamais ; 2 h par défaut). Le délai écoulé, l'horloge se ferme et l'écran peut s'éteindre normalement.
- **Économie d'énergie** : la nuit, ni trotteuse ni deux-points clignotants, et l'affichage n'est redessiné qu'une fois par minute (1 image par minute au lieu de 60, mesuré sur l'appareil de test). L'app demande aussi le taux de rafraîchissement le plus bas de l'écran (souvent 60 Hz au lieu de 120).

## Compiler l'APK

### Avec Android Studio (le plus simple)
1. Ouvrir le dossier du projet dans Android Studio (Ladybug ou plus récent).
2. Laisser la synchronisation Gradle se terminer (accepter les mises à jour proposées si besoin).
3. Menu **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. L'APK est dans `app/build/outputs/apk/debug/app-debug.apk`.

### En ligne de commande
Prérequis : le SDK Android (`ANDROID_HOME` défini, ou un fichier `local.properties` avec `sdk.dir=...`) et Java 17, géré par [mise](https://mise.jdx.dev) via `mise.toml` (Gradle refuse les JDK trop récents). Installer mise, puis :
```bash
mise install
mise exec -- ./gradlew assembleDebug
```
Dans un shell où mise est activé, `./gradlew assembleDebug` suffit.

### Sans rien installer (GitHub)
À chaque push sur `main` et sur chaque pull request, le workflow `.github/workflows/ci.yml` compile l'APK et le met en téléchargement dans l'onglet [Actions](https://github.com/clement-vassant/StandByClock/actions/workflows/ci.yml).

## Installer sur le téléphone

- **Par câble** : activer les options développeur (Paramètres > À propos du téléphone > Informations sur le logiciel > taper 7 fois sur "Numéro de version"), activer le débogage USB, puis `adb install app-debug.apk` (ou bouton ▶ Run d'Android Studio).
- **Sans câble** : copier l'APK sur le téléphone, l'ouvrir depuis Mes fichiers et autoriser l'installation d'apps inconnues pour Mes fichiers.

## Activer le mode automatique en charge

Paramètres > Affichage > **Écran de veille** (le chemin varie selon les fabricants ; chercher « Écran de veille » ou *Screen saver* dans les Paramètres) :
1. Choisir **StandBy Clock**.
2. Quand démarrer : **Pendant la charge**.

Astuce : le bouton ⚙ de l'écran de veille ouvre les réglages de l'app.

L'écran de veille démarre quand l'écran s'éteint tout seul pendant la charge (filaire ou par induction) : poser le téléphone écran allumé. Réglage **Orientation** (app et écran de veille) : *Automatique* suit l'écran (en portrait, horloge et calendrier l'un au-dessus de l'autre, qui échangent leur place chaque heure) ; *Toujours paysage* tourne l'horloge d'un quart de tour quand l'écran est en portrait (rotation automatique coupée), pour la lire téléphone couché, haut de l'écran vers la gauche.

## Tests

- **Tests unitaires** (JVM, rapides) : plage de nuit, raccourcis jour/nuit, grille du calendrier, côté de l'horloge, orbite anti-marquage, rythme de rafraîchissement, délai d'extinction automatique, écriture 12 h ou 24 h, détection du « j'aime », affichage et place de la musique (sous l'heure, ou carte si le calendrier est masqué), événements du jour (jours UTC des événements « toute la journée », minuit, tri, agendas cochés), semaine selon la région (premier jour, numéro), date dans la langue de l'app et l'ordre de la région, batterie et prochain réveil (seuil de batterie faible, charge masquée la nuit, réveil dans les 24 heures, dans le fuseau de l'horloge et en 12 h ou 24 h, niveau calculé sur l'échelle du système), textes visibles (tous dans `strings.xml`, tous traduits en anglais, aucun modèle de téléphone), version d'Android visée et maintien de *Toujours paysage* sur grand écran.
  ```bash
  mise run test
  ```
- **Tests d'interface** (Compose, sur un émulateur ou un téléphone branché, à heure fixe) : calendrier affiché par défaut, calendrier et musique visibles ensemble, barre musique sous l'heure sans chevauchement en paysage et en portrait (3 styles), carte musique si le calendrier est masqué, cadran pas plus haut que le calendrier, toucher pour la nuit, appui long pour le calendrier, balayage entre les styles, semaine et événements du jour à la place du mois (et le mois la nuit ou quand tout est passé), choix des agendas dans les réglages, heures en 12 h ou 24 h selon le téléphone, batterie et prochain réveil à côté de la date (réveil seul la nuit, batterie débranchée seulement si faible, sous le cadran en analogique, avec ou sans calendrier).
  ```bash
  mise exec -- ./gradlew connectedDebugAndroidTest   # ou mise run test:connected, qui joue aussi les parcours sur l'APK release
  ```
  Avec plusieurs appareils connectés, choisir l'émulateur avec `ANDROID_SERIAL=emulator-5554` : les tests effacent les réglages de l'app.

- **Captures de référence** (Roborazzi, sur la JVM) : les 3 styles en jour et nuit, paysage et portrait, avec musique, sans calendrier, avec les événements du jour, en 12 h, en *Toujours paysage*, en anglais, avec la batterie et le prochain réveil, et l'écran des réglages en français et en anglais ; chaque image est comparée à celle de `app/src/test/screenshots/`.
  ```bash
  mise run screenshots
  ```

Ces suites tournent dans GitHub Actions sur chaque pull request, avec ktlint, detekt, Android Lint et des parcours de bout en bout sur émulateur (détail dans [CONTRIBUTING.md](CONTRIBUTING.md)).

## Qualité

`main` est protégée : on n'y pousse jamais directement, chaque changement passe par une pull request qui ne peut être mergée que si la CI est verte. Avant de pousser :

```bash
mise run check      # ktlint, detekt, Android Lint, tests unitaires, captures de référence, APK debug et release
mise run check:all  # idem + tests d'interface, parcours e2e, et parcours courts sur l'APK release
```

Ces tâches sont définies dans `mise.toml` (`mise tasks` les liste) et la CI lance les mêmes. Le détail (jobs de la CI, conventions de code) est dans [CONTRIBUTING.md](CONTRIBUTING.md).

## Structure

```
app/src/main/java/fr/clementvassant/standbyclock/
  MainActivity.kt          activité plein écran, écran allumé
  StandByDreamService.kt   écran de veille Android (charge)
  SettingsActivity.kt      réglages (style, calendrier, agendas, musique, jour/nuit, orientation, extinction)
  ClockPrefs.kt            préférences partagées + calcul jour/nuit
  Orientation.kt           réglage Orientation appliqué à l'app plein écran
  AutoOff.kt               délai avant l'extinction automatique, commun à l'app et à l'écran de veille
  TimeFormat.kt            écriture des heures en 12 h ou 24 h, d'après le téléphone
  TimeUnits.kt             constantes de temps nommées (minutes par heure…)
  CalendarEvents.kt        agendas et événements du jour (tri, filtres, jours « toute la journée »)
  DeviceCalendar.kt        lecture de l'agenda d'Android, relue quand il change
  NowPlaying.kt            suivi de la session média active (titre, pochette, contrôles, j'aime)
  MediaListenerService.kt  écouteur de notifications qui donne accès aux sessions média
  RefreshRate.kt           demande le taux de rafraîchissement le plus bas de l'écran
  DeviceStatus.kt          batterie et prochain réveil lus auprès du système, et ce qui s'en affiche jour et nuit
  ui/StandByScreen.kt      pager des styles, gestes, mode nuit
  ui/ClockLayout.kt        horloge et panneau côte à côte ou empilés, orbite et inversion anti-marquage
  ui/Landscape.kt          quart de tour de l'horloge en Toujours paysage
  ui/Clocks.kt             les styles d'horloge et le calendrier du mois
  ui/Agenda.kt             semaine en cours et événements du jour
  ui/DateTexts.kt          dates dans la langue de l'app et l'ordre de la région, semaine de la région
  ui/MediaCard.kt          carte « musique en cours » et ses boutons
  ui/StatusRow.kt          batterie et prochain réveil sur la ligne de la date, avec leurs icônes
  ui/ClockColors.kt        palettes jour et nuit
  ui/TestTags.kt           repères des nœuds testés
app/src/main/res/values/strings.xml      tous les textes visibles, en français (langue par défaut)
app/src/main/res/values-en/strings.xml   leur traduction anglaise
gradle/libs.versions.toml                versions des plugins et bibliothèques (catalogue Gradle)
```

Pour ajouter un style : une valeur dans `ClockStyle`, un `@Composable` dans `Clocks.kt`, une ligne dans le `when` de `StandByScreen.kt`.
