#!/bin/bash
# Lance la simulation Gatling en mode standalone (bundle Maven autonome)
# Le plugin Gatling Gradle est incompatible avec Java 25 (ASM ne lit pas les class files v69)
#
# Usage :
#   ./scripts/run-gatling.sh              # télécharge Gatling si absent, lance PatientSimulation
#   ./scripts/run-gatling.sh -j /opt/jdk-21  # forcer un JDK alternatif
#
# Pré-requis : l'app doit tourner sur http://localhost:8080 (./scripts/run-local-otel.sh &)

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

GATLING_VERSION="3.11.3"
GATLING_DIR="build/gatling-standalone"
GATLING_ZIP="build/gatling-${GATLING_VERSION}.zip"
GATLING_URL="https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/${GATLING_VERSION}/gatling-charts-highcharts-bundle-${GATLING_VERSION}.zip"
SIM_SRC="src/gatling/java/fr/testlab/jbp/patient/PatientSimulation.java"

# --- option -j : chemin vers un JDK alternatif (ex: /opt/jdk-21) ---
JAVA_HOME_OVERRIDE=""
while getopts "j:h" opt; do
  case $opt in
    j) JAVA_HOME_OVERRIDE="$OPTARG" ;;
    h) echo "Usage: $0 [-j /path/to/jdk]" ; exit 0 ;;
  esac
done

if [ -n "$JAVA_HOME_OVERRIDE" ]; then
  export JAVA_HOME="$JAVA_HOME_OVERRIDE"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "=== Gatling standalone ${GATLING_VERSION} ==="

# Télécharger le bundle Gatling Maven si absent
if [ ! -d "$GATLING_DIR" ]; then
  echo "Téléchargement Gatling ${GATLING_VERSION}..."
  mkdir -p build
  curl -L "$GATLING_URL" -o "$GATLING_ZIP"
  # Le zip extrait un dossier du type gatling-charts-highcharts-bundle-3.11.3/
  unzip -q "$GATLING_ZIP" -d build/
  EXTRACTED=$(ls build/ | grep "gatling-charts-highcharts-bundle-${GATLING_VERSION}" | head -1)
  mv "build/${EXTRACTED}" "$GATLING_DIR"
  rm "$GATLING_ZIP"
  chmod +x "$GATLING_DIR/mvnw"
  echo "Gatling extrait dans $GATLING_DIR"
fi

# Copier la simulation Java dans src/test/java du bundle
SIM_DEST="${GATLING_DIR}/src/test/java/fr/testlab/jbp/patient"
mkdir -p "$SIM_DEST"
cp "$SIM_SRC" "$SIM_DEST/"

echo "Simulation : fr.testlab.jbp.patient.PatientSimulation"
echo "Rapport    : ${GATLING_DIR}/target/gatling/"
echo ""

# Lancer Gatling via le wrapper Maven fourni dans le bundle
cd "$GATLING_DIR"
./mvnw gatling:test \
  -Dgatling.simulationClass=fr.testlab.jbp.patient.PatientSimulation \
  -Dgatling.reportsOnly= \
  -q
