# Règles R8 de l'APK release (en plus de proguard-android-optimize.txt et des règles fournies par les bibliothèques).
#
# Rien à garder pour l'app elle-même : MainActivity, SettingsActivity, StandByDreamService et MediaListenerService
# sont déclarés dans le manifeste, donc gardés automatiquement ; la musique en cours passe par les classes
# MediaSession du système, sans réflexion.
#
# Les parcours e2e (app/src/androidTest/.../e2e) tournent aussi sur cet APK release (-PtestBuildType=release) et
# préparent leurs scénarios en appelant directement ces classes : sans ces règles, R8 les renommerait ou les
# supprimerait et les tests ne les trouveraient plus. Coût : quelques centaines d'octets.
-keep class fr.clementvassant.standbyclock.ClockPrefs { public *; }
-keep enum fr.clementvassant.standbyclock.ClockStyle { *; }
-keep enum fr.clementvassant.standbyclock.ThemeMode { *; }
-keep enum fr.clementvassant.standbyclock.OrientationMode { *; }
-keep class fr.clementvassant.standbyclock.DeviceCalendar { public *; }
-keep class fr.clementvassant.standbyclock.ui.TestTagsKt { public *; }
