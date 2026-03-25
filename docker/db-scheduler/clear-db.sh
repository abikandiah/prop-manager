#!/bin/sh
# Truncates all app tables in the propmanager database.
# Preserves Liquibase changelog tables and the logto database.
# Runs as a cron job inside the db-scheduler container.
set -e

echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] Starting scheduled DB clear (host=${PGHOST}, db=${PGDATABASE}, user=${PGUSER})..."

psql << 'SQL'
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

echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] DB clear complete."
