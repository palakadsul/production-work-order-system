# Loads the PWOS secrets into the environment. Use with: . scripts/load-env.sh
PWOS_ENV_FILE="${PWOS_ENV_FILE:-$HOME/.pwos/pwos.env}"
if [ ! -r "$PWOS_ENV_FILE" ]; then
  echo "ERROR: secrets file not found: $PWOS_ENV_FILE" >&2
  return 1 2>/dev/null || exit 1
fi
set -a
. "$PWOS_ENV_FILE"
set +a
