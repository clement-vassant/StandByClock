# Règles R8 de l'APK release (en plus de proguard-android-optimize.txt et des règles fournies par les bibliothèques).
#
# Rien à garder pour l'app elle-même : MainActivity, SettingsActivity, StandByDreamService et MediaListenerService
# sont déclarés dans le manifeste, donc gardés automatiquement ; la musique en cours passe par les classes
# MediaSession du système, sans réflexion. Le module release-smoke joue des parcours sur l'APK release pour le
# vérifier : une règle ne s'ajoute ici que si l'un d'eux montre qu'elle manque.
