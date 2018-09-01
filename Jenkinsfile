pipeline {
    agent any
    stages {
        stage('Run Tests') {
            parallel {
                stage('Do E2E Testing') {
                build job: 'E2E_Testing'
                }
                stage('Do Perf Testing') {
                build job: 'Perf_Testing'
                }
            }
        }
    }
}