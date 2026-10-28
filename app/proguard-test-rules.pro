# Règles R8 de l'APK de test, quand les parcours e2e visent l'APK release (-PtestBuildType=release) : AGP passe
# alors aussi l'APK de test par R8.
#
# Le lanceur de tests retrouve les classes et méthodes @Test par leur nom et par le package demandé (…e2e) :
# l'APK de test n'est ni réduit, ni optimisé, ni renommé.
-dontshrink
-dontoptimize
-dontobfuscate

# Annotations d'analyse statique référencées par androidx.test, absentes à l'exécution et sans effet.
-dontwarn com.google.errorprone.annotations.**
