#!/usr/bin/env groovy

pipeline {
    agent any
    tools {
        jdk "jdk-25"
    }
    stages {
        stage('Clean') {
            steps {
                echo 'Cleaning Project'
                sh 'chmod +x gradlew'
                sh './gradlew clean'
            }
        }
        stage('Build') {
            steps {
                echo 'Building'
                sh './gradlew build'
            }
        }
        stage('Publish') {
            steps {
                echo 'Deploying to Maven'
                sh './gradlew publish'
            }
        }
    }
}
