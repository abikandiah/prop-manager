#!/bin/bash
set -e
# Creates the propmanager user and database. Used by the devcontainer Postgres only;
# prod uses the homelab's shared Postgres, where the role is created separately.

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER propmanager WITH PASSWORD '${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required}';
	CREATE DATABASE propmanager OWNER propmanager;
EOSQL
