# Dockerfile Jenkins avec JDK 25 integre
# Alternative au montage de volume /opt/jdk-25:/opt/jdk-25:ro
#
# Avantages vs volume mount :
#   - Image portable : fonctionne sur tout hote sans JDK 25 preinstalle
#   - CI/CD cloud-friendly (agents ephemeres, pas de dependance hote)
#   - JAVA_HOME stable et predefini dans l'image
#
# Utilisation :
#   docker build -t jbp-jenkins-jdk25 -f tools/jenkins/Dockerfile .
#   Puis dans docker-compose.yml remplacer :
#     image: jenkins/jenkins:lts-jdk21
#   par :
#     build:
#       context: .
#       dockerfile: tools/jenkins/Dockerfile

FROM jenkins/jenkins:lts-jdk21

# Passer en root pour installer le JDK 25
USER root

# Variables de version et de chemin
ARG JDK_VERSION=25
ARG JDK_URL=https://download.java.net/java/GA/jdk25/739c7e81e6974e95a39c02ad1d38d8e9/4/GPL/openjdk-25_linux-x64_bin.tar.gz
ARG JDK_INSTALL_DIR=/opt/jdk-25

# Telecharger et installer OpenJDK 25
RUN apt-get update -qq \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && mkdir -p ${JDK_INSTALL_DIR} \
    && curl -fsSL "${JDK_URL}" | tar -xz --strip-components=1 -C "${JDK_INSTALL_DIR}" \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Configurer JAVA_HOME vers JDK 25
ENV JAVA_HOME=${JDK_INSTALL_DIR}
ENV PATH="${JDK_INSTALL_DIR}/bin:${PATH}"

# Revenir a l'utilisateur jenkins
USER jenkins

# Verification au build
RUN java -version && javac -version
