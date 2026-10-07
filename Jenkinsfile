pipeline {
    agent any

    parameters {
        string(name: 'DEPLOY_PORT', defaultValue: '8081',
               description: 'Port for the live application')
    }

    environment {
        TEST_PORT = '8090'
        REGISTRY  = 'localhost:5001'
        IMAGE     = "localhost:5001/pwos:${BUILD_NUMBER}"
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

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE -t $REGISTRY/pwos:latest .'
            }
        }

        stage('Deploy to Test') {
            steps {
                sh 'bash scripts/deploy-container.sh pwos-test $IMAGE $TEST_PORT'
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
                    sh 'docker rm -f pwos-test || true'
                    sh 'bash scripts/cleanup-test-data.sh'
                }
            }
        }

        stage('Push Image') {
            steps {
                sh 'docker push $IMAGE'
                sh 'docker push $REGISTRY/pwos:latest'
            }
        }

        stage('Deploy') {
            steps {
                sh 'bash scripts/deploy-container.sh pwos-live $IMAGE ${DEPLOY_PORT:-8081}'
            }
        }

        stage('Provision Node (Ansible)') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh '''
                        export PATH=/opt/homebrew/bin:$PATH
                        cd ansible
                        ansible-playbook playbook.yml -e pwos_version=${BUILD_NUMBER}
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Image ${env.IMAGE} tested, pushed and deployed."
        }
        failure {
            echo "Pipeline failed. Live container was NOT changed."
        }
    }
}
