#!/usr/bin/env bash
# Usage: stop.sh <port>
PORT="${1:?port required}"
PID=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN || true)
if [ -n "$PID" ]; then
  echo "Stopping app on port $PORT (PID $PID)"
  kill $PID
else
  echo "Nothing running on port $PORT"
fi
