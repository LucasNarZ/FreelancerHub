#!/usr/bin/env bash

set -euo pipefail

NAME="${1:-}"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-backend/src/main/resources/db/migration}"

if [ -z "$NAME" ]; then
  echo "Usage: ./scripts/new-migration.sh create_users_table"
  echo "Or:    make migration name=create_users_table"
  exit 1
fi

if [[ ! "$NAME" =~ ^[a-z0-9_]+$ ]]; then
  echo "Invalid name: use only lowercase letters, numbers, and underscores."
  echo "Example: create_users_table"
  exit 1
fi

VERSION="$(date +"%Y%m%d%H%M%S")"
FILE="$MIGRATIONS_DIR/V${VERSION}__${NAME}.sql"

mkdir -p "$MIGRATIONS_DIR"
touch "$FILE"

echo "Migration created: $FILE"
