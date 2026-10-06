#!/usr/bin/env bash
# Usage: deploy.sh <port>
# Stops whatever listens on <port>, starts the
# built jar there, waits until it responds.
PORT="${1:-8081}"
PID=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN || true)
if [ -n "$PID" ]; then
  echo "Stopping existing app on port $PORT (PID $PID)"
  kill -9 $PID
  sleep 2
fi
JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
echo "Deploying $JAR_FILE on port $PORT"
export JENKINS_NODE_COOKIE=dontKillMe
export BUILD_ID=dontKillMe
nohup java -jar "$JAR_FILE" \
  --server.port="$PORT" > "deploy-$PORT.log" 2>&1 &
for i in $(seq 1 30); do
  if curl -sf "http://localhost:$PORT/" > /dev/null; then
    echo "Deployment verified: app responding on port $PORT"
    exit 0
  fi
  sleep 2
done
echo "ERROR: app did not respond on port $PORT"
tail -30 "deploy-$PORT.log"
exit 1
