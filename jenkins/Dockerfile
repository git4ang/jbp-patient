# Image Jenkins personnalisée avec Docker CLI + Trivy préinstallés
# Base LTS avec JDK 21 (Jenkins lui-même tourne en JDK 21)
# jenkins:lts-jdk21 = Debian 12 Bookworm -- "bookworm" hardcodé (pas de substitution shell)
FROM jenkins/jenkins:lts-jdk21

# Passage en root pour les installations système
USER root

# Docker CLI -- permet à Jenkins d'exécuter "docker build/push" via le socket monté
# bookworm = Debian 12, base de jenkins:lts-jdk21
RUN apt-get update -qq && \
    apt-get install -y ca-certificates curl gnupg && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg \
      | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
      > /etc/apt/sources.list.d/docker.list && \
    apt-get update -qq && \
    apt-get install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

# Trivy -- scan CVE des images Docker (stage Trivy dans le Jenkinsfile)
RUN apt-get update -qq && \
    apt-get install -y wget gpg && \
    mkdir -p /etc/apt/keyrings && \
    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key \
      | gpg --dearmor > /etc/apt/keyrings/trivy.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb jammy main" \
      > /etc/apt/sources.list.d/trivy.list && \
    apt-get update -qq && \
    apt-get install -y trivy && \
    rm -rf /var/lib/apt/lists/*

# Retour à l'utilisateur jenkins (bonne pratique sécurité)
USER jenkins
