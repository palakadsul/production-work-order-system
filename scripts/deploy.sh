#!/usr/bin/env bash
# Usage: deploy.sh <port>  - runs the packaged jar on <port> (local test copy)
PORT="${1:?port required}"
. "$(dirname "$0")/load-env.sh" || exit 1
PID=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN || true)
if [ -n "$PID" ]; then
  echo "Stopping existing app on port $PORT (PID $PID)"
  kill $PID
  sleep 3
fi
JAR=$(ls target/*.jar | grep -v original | head -1)
echo "Deploying $JAR on port $PORT"
JENKINS_NODE_COOKIE=dontKillMe BUILD_ID=dontKillMe \
  nohup java -jar "$JAR" --server.port="$PORT" > "deploy-$PORT.log" 2>&1 &
for i in $(seq 1 30); do
  if curl -s -o /dev/null "http://localhost:$PORT/login"; then
    echo "Deployment verified: app responding on port $PORT"
    exit 0
  fi
  sleep 2
done
echo "ERROR: app did not respond on port $PORT"
tail -30 "deploy-$PORT.log"
exit 1
