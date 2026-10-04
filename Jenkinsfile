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
                    PORT="${DEPLOY_PORT:-8081}"
                    PID=$(lsof -ti tcp:$PORT || true)
                    if [ -n "$PID" ]; then
                        echo "Stopping existing process on port $PORT (PID $PID)"
                        kill -9 $PID
                        sleep 2
                    fi
                    JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
                    echo "Deploying $JAR_FILE on port $PORT"
                    JENKINS_NODE_COOKIE=dontKillMe BUILD_ID=dontKillMe \
                      nohup java -jar "$JAR_FILE" --server.port=$PORT > deploy.log 2>&1 &

                    for i in $(seq 1 30); do
                        if curl -sf http://localhost:$PORT/ > /dev/null; then
                            echo "Deployment verified: app responding on port $PORT"
                            exit 0
                        fi
                        sleep 2
                    done

                    echo "ERROR: app did not respond on port $PORT"
                    tail -30 deploy.log
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully. App deployed on port ${params.DEPLOY_PORT ?: '8081'}."
        }
        failure {
            echo "Pipeline failed."
        }
    }
}