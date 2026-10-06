#!/usr/bin/env bash
# Usage: deploy-container.sh <name> <image> <port>
# Replaces container <name> with a fresh one from
# <image> on host <port>, then waits until it answers.
NAME="$1"
IMAGE="$2"
PORT="$3"
DB_URL="${DB_URL:-jdbc:postgresql://host.docker.internal:5432/pwos_db}"

docker rm -f "$NAME" > /dev/null 2>&1 || true

# Free the port if an old jar deployment still holds it
PID=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN || true)
if [ -n "$PID" ] && ps -p "$PID" -o comm= | grep -q java; then
  echo "Stopping old jar process on port $PORT (PID $PID)"
  kill "$PID"
  sleep 3
fi

echo "Starting $NAME from $IMAGE on port $PORT"
docker run -d --name "$NAME" --restart unless-stopped \
  -p "$PORT:8080" \
  -e SPRING_DATASOURCE_URL="$DB_URL" \
  "$IMAGE" > /dev/null

for i in $(seq 1 30); do
  if curl -sf "http://localhost:$PORT/" > /dev/null; then
    echo "Container $NAME is healthy on port $PORT"
    exit 0
  fi
  sleep 2
done
echo "ERROR: $NAME did not respond on port $PORT"
docker logs --tail 30 "$NAME"
exit 1
