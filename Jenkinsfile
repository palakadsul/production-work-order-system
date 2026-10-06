pipeline {
    agent any

    parameters {
        string(name: 'DEPLOY_PORT', defaultValue: '8081',
               description: 'Port for the live application')
    }

    environment {
        TEST_PORT = '8090'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar',
                                     fingerprint: true
                }
            }
        }

        stage('Deploy to Test') {
            steps {
                sh 'bash scripts/deploy.sh $TEST_PORT'
            }
        }

        stage('Selenium Tests') {
            steps {
                sh '''
                    mvn -B test -Dtest=PwosJourneysIT \
                      -Dbase.url=http://localhost:$TEST_PORT \
                      -Dheadless=true
                '''
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/screenshots/*.png',
                                     allowEmptyArchive: true
                    sh 'bash scripts/stop.sh $TEST_PORT'
                    sh 'bash scripts/cleanup-test-data.sh'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'bash scripts/deploy.sh ${DEPLOY_PORT:-8081}'
            }
        }
    }

    post {
        success {
            echo "All tests passed. Live app deployed."
        }
        failure {
            echo "Pipeline failed. Live app was NOT changed."
        }
    }
}
