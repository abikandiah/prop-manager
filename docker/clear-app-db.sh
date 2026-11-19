#!/usr/bin/env bash
# Clears all app data from the dev-oidc propmange database (Liquibase changelog kept).
# Run from a host terminal (the devcontainer has no Docker access):
#   ./prop-manager/docker/clear-app-db.sh
#   CONTAINER=my-container ./prop-manager/docker/clear-app-db.sh
# Requires: docker compose -f .devcontainer/docker-compose.yml --profile dev-oidc up -d postgres

set -euo pipefail

CONTAINER="${CONTAINER:-prop-mange-dev-postgres-1}"
PGUSER="${PGUSER:-propmange}"
PGDATABASE="${PGDATABASE:-propmange}"

echo "Connecting via docker exec into ${CONTAINER} (db=${PGDATABASE}, user=${PGUSER})"
echo "This will DELETE all app data. The Liquibase changelog is preserved."
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
