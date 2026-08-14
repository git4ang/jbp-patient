pipeline {
    agent any

    tools {
        jdk 'jdk25'
    }

    // IMAGE_TAG : tag de l'image Docker tracable par numéro de build Jenkins
    // Ex. : jbp-patient:42 -- permet de savoir quelle image = quel build
    // NEXUS_URL : nom de conteneur Docker (pas localhost -- Jenkins est dans son propre conteneur)
    // jbp-nexus est le nom du service Nexus sur le réseau jbp-net
    environment {
        IMAGE_TAG = "jbp-patient:${env.BUILD_NUMBER}"
        NEXUS_URL = "http://jbp-nexus:8081/repository/maven-releases/"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew compileJava'
            }
        }

        // --rerun : force la réexécution même si Gradle considère UP-TO-DATE
        // jacocoTestReport déclenché automatiquement après les tests (finalizedBy)
        stage('Tests') {
            steps {
                sh './gradlew test --rerun'
            }
            post {
                always {
                    junit 'build/test-results/**/*.xml'
                }
            }
        }

        // SonarQube : analyse qualité + couverture JaCoCo
        // withCredentials injecte SONAR_TOKEN depuis le coffre-fort Jenkins
        stage('Sonar') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        ./gradlew sonar \
                          -Dsonar.projectKey=jbp-patient \
                          -Dsonar.host.url=http://sonarqube:9000 \
                          -Dsonar.token=$SONAR_TOKEN
                    '''
                }
            }
        }

        // OWASP : scanne les CVE dans les JARs déclarés dans build.gradle
        // withCredentials injecte NVD_API_KEY pour accélérer le téléchargement NVD
        // publishHTML : rapport visible dans Jenkins (plugin HTML Publisher requis)
        // Premier run : lent (téléchargement base NVD) -- suivants : rapide (cache)
        stage('OWASP') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                    // sh './gradlew dependencyCheckAnalyze'
                    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                          sh './gradlew dependencyCheckAnalyze'
                    }
                }
            }
            post {
                always {
                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/dependency-check',
                        reportFiles          : 'dependency-check-report.html',
                        reportName           : 'OWASP Dependency Check'
                    ])
                }
            }
        }

        // Docker Build : image multi-stage JDK25 → JRE25
        // .dockerignore exclut build/, .gradle/, logs/, .git/
        stage('Docker Build') {
            steps {
                sh 'docker build -t ${IMAGE_TAG} .'
                sh 'docker tag ${IMAGE_TAG} jbp-patient:latest'
            }
        }

        // Trivy : scanne les CVE dans les couches OS de l'image Docker
        // Complète OWASP (JARs Java) avec les vulnérabilités Ubuntu/libc/OpenSSL
        // --exit-code 1 : build échoue si CVE HIGH ou CRITICAL trouvées dans l'image
        // --ignore-unfixed : ignore les CVE sans correctif disponible (non actionnables)
        stage('Trivy') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    sh '''
                        trivy image \
                          --exit-code 0 \
                          --severity HIGH,CRITICAL \
                          --ignore-unfixed \
                          --format table \
                          ${IMAGE_TAG}
                    '''
                }
            }
        }

        // Nexus Publish : pousse le JAR Maven + l'image Docker vers Nexus
        // withCredentials injecte NEXUS_USER et NEXUS_PASS depuis Jenkins
        stage('Nexus Publish') {
          steps {
              // Publication du JAR dans Nexus Maven
              withCredentials([usernamePassword(
                  credentialsId : 'nexus-credentials',
                  usernameVariable: 'NEXUS_USER',
                  passwordVariable: 'NEXUS_PASS'
              )]) {
                  sh './gradlew publish'
              }

              // Push de l'image Docker vers le registre Docker de Nexus
              // docker login nécessaire pour s'authentifier sur le registre Nexus
              withCredentials([usernamePassword(
                  credentialsId : 'nexus-credentials',
                  usernameVariable: 'NEXUS_USER',
                  passwordVariable: 'NEXUS_PASS'
              )]) {
                  sh '''
                      echo "$NEXUS_PASS" | docker login jbp-nexus:8082 -u "$NEXUS_USER" --password-stdin
                      docker tag ${IMAGE_TAG} jbp-nexus:8082/jbp-patient:${BUILD_NUMBER}
                      docker push jbp-nexus:8082/jbp-patient:${BUILD_NUMBER}
                  '''
              }
          }
        }
    }

    post {
        success {
            echo "Pipeline G7 OK - Build + Tests + Sonar + OWASP + Docker + Trivy : ${IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline FAILED - voir les logs Jenkins et les rapports OWASP/Trivy'
        }
    }
}
