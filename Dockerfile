# ─────────────────────────────────────────────────────
# Stage 1 : BUILD
# Image complète JDK 25 -- compile et assemble l'application
# eclipse-temurin = distribution OpenJDK officielle (AdoptOpenJDK successor)
# ─────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copie du wrapper Gradle et des fichiers de config AVANT les sources
# Raison : Docker met en cache chaque couche -- si build.gradle ne change pas,
# la couche "téléchargement des dépendances" est réutilisée sans re-télécharger
COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle/ gradle/

# org.gradle.java.home=/opt/jdk-25 dans gradle.properties = chemin local Ubuntu
# Ce chemin n'existe pas dans cette image eclipse-temurin
# On l'écrase avec JAVA_HOME défini automatiquement par l'image (= /opt/java/openjdk)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon --quiet \
    -Dorg.gradle.java.home="${JAVA_HOME}"

# Copie des sources (couche invalidée seulement si les sources changent)
COPY src/ src/

# installDist : crée build/install/jbp-patient/ avec le script de lancement
# et tous les JARs nécessaires -- pas besoin de fat JAR
# -x test : les tests ne tournent pas dans le build Docker (déjà faits en CI)
RUN ./gradlew installDist -x test --no-daemon \
    -Dorg.gradle.java.home="${JAVA_HOME}"

# ─────────────────────────────────────────────────────
# Stage 2 : RUN
# Image légère JRE seulement (pas de compilateur) -- taille réduite
# Le JRE suffit pour exécuter le JAR compilé au stage 1
# ─────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copie uniquement le résultat du build (pas les sources, pas Gradle)
# --from=builder : pointe vers le stage 1
COPY --from=builder /app/build/install/jbp-patient/ .

# Port exposé par CXF/Jetty dans JbpApplication (port 8080)
EXPOSE 8080

# Point d'entrée : le script généré par le plugin application de Gradle
ENTRYPOINT ["bin/jbp-patient"]
