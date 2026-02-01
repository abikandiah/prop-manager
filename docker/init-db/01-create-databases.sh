#!/bin/bash
set -e
# Create propmanager and authentik databases and users (single Postgres instance).
# Single password for all: POSTGRES_PASSWORD (postgres superuser, propmanager, authentik).

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER propmanager WITH PASSWORD '${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required}';
	CREATE DATABASE propmanager OWNER propmanager;

	CREATE USER authentik WITH PASSWORD '${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required}';
	CREATE DATABASE authentik OWNER authentik;
EOSQL
