pipeline {
    agent any 
    stages {
        stage('Do RC Testing') {
            parallel {
                stage('Run E2E Testing') {
                    steps {
                        build(job:"E2E_Testing")
                    }
                    }
                stage('Run Perf Testing') {
                    steps {
                        build(job:"Perf_Testing")
                    }
                }
            }
        }
    }
}