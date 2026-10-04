pipeline {
    agent any

    parameters {
        string(name: 'DEPLOY_PORT', defaultValue: '8081', description: 'Port to deploy the application on')
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
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    PID=$(lsof -ti tcp:${DEPLOY_PORT} || true)
                    if [ -n "$PID" ]; then
                        echo "Stopping existing process on port ${DEPLOY_PORT} (PID $PID)"
                        kill -9 $PID
                    fi
                    JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
                    echo "Deploying $JAR_FILE on port ${DEPLOY_PORT}"
                    nohup java -jar "$JAR_FILE" --server.port=${DEPLOY_PORT} > deploy.log 2>&1 &
                    sleep 5
                    curl -sf http://localhost:${DEPLOY_PORT}/ > /dev/null && echo "Deployment verified: app responding on port ${DEPLOY_PORT}" || echo "WARNING: app did not respond on port ${DEPLOY_PORT}"
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully. App deployed on port ${params.DEPLOY_PORT}."
        }
        failure {
            echo "Pipeline failed."
        }
    }
}
