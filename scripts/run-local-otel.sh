#!/bin/bash
# Lance jbp-patient avec l'agent OTEL configuré pour Jaeger local
#
# Usage :
#   ./scripts/run-local-otel.sh                          # valeurs par défaut
#   ./scripts/run-local-otel.sh -n mon-service           # nom du service dans Jaeger
#   ./scripts/run-local-otel.sh -l build/monRun.log      # fichier de log
#   ./scripts/run-local-otel.sh -e http://host:4318      # endpoint OTLP différent
#   ./scripts/run-local-otel.sh -n api-v2 -l build/api.log -e http://jaeger-prod:4318
#
# Pré-requis : ./gradlew installDist lancé au moins une fois

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

# --- valeurs par défaut ---
SERVICE_NAME="jbp-patient"
LOG_FILE="build/runG10.log"
OTLP_ENDPOINT="http://localhost:4318"
MAIN_CLASS="fr.testlab.jbp.patient.app.JbpApplication"

# --- parsing des options ---
while getopts "n:l:e:c:h" opt; do
  case $opt in
    n) SERVICE_NAME="$OPTARG" ;;         # -n nom-du-service
    l) LOG_FILE="$OPTARG" ;;             # -l chemin/vers/run.log
    e) OTLP_ENDPOINT="$OPTARG" ;;        # -e http://host:port  (endpoint OTLP)
    c) MAIN_CLASS="$OPTARG" ;;           # -c fr.autre.MainClass
    h)
      echo "Usage: $0 [-n service-name] [-l log-file] [-e otlp-endpoint] [-c main-class]"
      echo "  -n  Nom du service dans Jaeger    (défaut: jbp-patient)"
      echo "  -l  Fichier de log                (défaut: build/runG10.log)"
      echo "  -e  Endpoint OTLP HTTP            (défaut: http://localhost:4318)"
      echo "  -c  Classe principale             (défaut: fr.testlab...JbpApplication)"
      exit 0 ;;
    *) echo "Option inconnue. Utilisez -h pour l'aide." ; exit 1 ;;
  esac
done

echo "=== jbp-patient OTEL ==="
echo "  Service  : $SERVICE_NAME"
echo "  Log      : $LOG_FILE"
echo "  Endpoint : $OTLP_ENDPOINT"
echo ""

pkill -f "JbpApplication" 2>/dev/null || true; sleep 1

java \
  -javaagent:otel/opentelemetry-javaagent.jar \
  -Dnet.bytebuddy.experimental=true \
  -Dotel.service.name="$SERVICE_NAME" \
  -Dotel.exporter.otlp.endpoint="$OTLP_ENDPOINT" \
  -Dotel.traces.exporter=otlp \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -cp "build/install/jbp-patient/lib/*" \
  "$MAIN_CLASS" 2>&1 | tee "$LOG_FILE"
