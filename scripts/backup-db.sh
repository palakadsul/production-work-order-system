#!/usr/bin/env bash
# Usage: backup-db.sh  - saves pwos_db to ~/pwos-backups and keeps the newest 20
set -o pipefail
. "$(dirname "$0")/load-env.sh" || exit 1
BACKUP_DIR="${PWOS_BACKUP_DIR:-$HOME/pwos-backups}"
PG_DUMP="${PG_DUMP:-/opt/homebrew/opt/postgresql@15/bin/pg_dump}"
mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/pwos_db_$(date +%Y%m%d_%H%M%S).sql.gz"

if PGPASSWORD="$DB_PASSWORD" "$PG_DUMP" -h localhost -U pwos_user -d pwos_db \
     --clean --if-exists --no-owner | gzip > "$FILE"; then
  echo "Backup saved: $FILE ($(du -h "$FILE" | cut -f1))"
else
  echo "ERROR: backup failed"
  rm -f "$FILE"
  exit 1
fi

ls -1t "$BACKUP_DIR"/pwos_db_*.sql.gz | tail -n +21 | while read -r old; do
  rm -f "$old"
done
