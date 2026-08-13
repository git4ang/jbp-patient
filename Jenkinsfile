pipeline {
    agent any

    tools {
        jdk 'jdk25'
    }

    // IMAGE_TAG : tag de l'image Docker -- combinaison du nom du job + numéro de build
    // Ex. : jbp-patient:42 -- permet de tracer quelle image correspond à quel build
    environment {
        IMAGE_TAG = "jbp-patient:${env.BUILD_NUMBER}"
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

        // --rerun : force la reexecution des tests meme si Gradle les considere UP-TO-DATE
        // jacocoTestReport est déclenché automatiquement après les tests (finalizedBy)
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

        // SonarQube : analyse qualité + rapport JaCoCo (coverage)
        // withCredentials injecte SONAR_TOKEN depuis le coffre-fort Jenkins -- jamais en clair
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

        // Docker Build : construit l'image à partir du Dockerfile multi-stage
        // Le .dockerignore exclut build/, .gradle/, logs/, .git/ -- contexte réduit
        // IMAGE_TAG = "jbp-patient:<numéro-de-build>" -- image tracable par build Jenkins
        stage('Docker Build') {
            steps {
                sh 'docker build -t ${IMAGE_TAG} .'
                // Tag supplémentaire "latest" pour toujours avoir un alias stable
                sh 'docker tag ${IMAGE_TAG} jbp-patient:latest'
            }
        }
    }

    post {
        success {
            echo "Pipeline G7 OK - tests + couverture JaCoCo + SonarQube + image Docker ${IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline FAILED - voir les logs et le rapport de tests'
        }
    }
}
