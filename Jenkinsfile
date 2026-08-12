pipeline {
    agent any

    tools {
        // Nom du JDK configure dans Jenkins -> Manage Jenkins -> Tools
        jdk 'jdk25'
    }

    stages {

        // (1) Cloner le repo depuis le remote configure dans le pipeline
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // (2) Compiler le projet - valide que le code compile proprement
        stage('Build') {
            steps {
                sh './gradlew compileJava'
            }
        }

        // (3) Tests - ajoute en G6 (JUnit 5 + Testcontainers)
        // stage('Tests') {
        //     steps { sh './gradlew test' }
        //     post { always { junit 'build/test-results/**/*.xml' } }
        // }
    }

    post {
        success {
            echo 'Pipeline G1 OK - compilation reussie'
        }
        failure {
            echo 'Pipeline FAILED - voir les logs'
        }
    }
}
