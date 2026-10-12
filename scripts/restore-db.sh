#!/usr/bin/env bash
# Usage: restore-db.sh <backup.sql.gz>  - replaces ALL data in pwos_db with a backup
FILE="${1:?backup file required, e.g. ~/pwos-backups/pwos_db_YYYYMMDD_HHMMSS.sql.gz}"
. "$(dirname "$0")/load-env.sh" || exit 1
PSQL="${PSQL:-/opt/homebrew/opt/postgresql@15/bin/psql}"
if [ ! -r "$FILE" ]; then
  echo "ERROR: cannot read $FILE"
  exit 1
fi
read -r -p "This replaces ALL data in pwos_db with $FILE. Type yes to continue: " ok
if [ "$ok" != "yes" ]; then
  echo "Cancelled"
  exit 1
fi
gunzip -c "$FILE" | PGPASSWORD="$DB_PASSWORD" "$PSQL" -h localhost -U pwos_user \
  -d pwos_db -q -v ON_ERROR_STOP=1 && echo "Restored pwos_db from $FILE"
