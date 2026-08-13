pipeline {
    agent any

    tools {
        jdk 'jdk25'
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
        // Utile en CI car le workspace peut contenir des resultats du build precedent
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
    }

    post {
        success {
            echo 'Pipeline G6 OK - compilation + 12 tests verts'
        }
        failure {
            echo 'Pipeline FAILED - voir les logs et le rapport de tests'
        }
    }
}
