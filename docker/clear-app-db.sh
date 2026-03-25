#!/usr/bin/env bash
# Clears all app data from the propmanager database.
# Leaves the logto database and Liquibase changelog tables untouched.
#
# Usage (from repo root or prop-manager/):
#   ./prop-manager/docker/clear-app-db.sh               # dev-oidc container (default)
#   PROFILE=prod ./prop-manager/docker/clear-app-db.sh  # prod container (prop-manager-db)
#
# Override any variable individually:
#   CONTAINER=my-container ./prop-manager/docker/clear-app-db.sh
#
# Requires the postgres container to be running:
#   Prod: docker compose -f prop-manager/docker-compose.yml up -d postgres
#   Dev:  docker compose -f .devcontainer/docker-compose.yml --profile dev-oidc up -d postgres

set -euo pipefail

PROFILE="${PROFILE:-dev}"

if [[ "$PROFILE" == "prod" ]]; then
  CONTAINER="${CONTAINER:-prop-manager-db}"
else
  CONTAINER="${CONTAINER:-prop-mange-dev-postgres-1}"
fi

PGUSER="${PGUSER:-propmanager}"
PGDATABASE="${PGDATABASE:-propmanager}"

echo "Connecting via docker exec into ${CONTAINER} (db=${PGDATABASE}, user=${PGUSER})"
echo "This will DELETE all app data. Logto DB and Liquibase changelog are preserved."
echo ""
read -r -p "Continue? [y/N] " confirm
[[ "${confirm}" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

docker exec -i "$CONTAINER" psql -U "$PGUSER" -d "$PGDATABASE" <<'SQL'
-- Discover all tables except Liquibase changelog, then truncate with CASCADE.
-- CASCADE handles FK ordering automatically; no need to specify table order.
DO $$
DECLARE
    tables text;
BEGIN
    SELECT string_agg(quote_ident(tablename), ', ')
    INTO tables
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename NOT IN ('databasechangelog', 'databasechangeloglock');

    IF tables IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
    END IF;
END;
$$;
SQL

echo ""
echo "Done. All app tables cleared."
echo "Restart the Spring Boot app and Liquibase will re-validate the existing schema."
