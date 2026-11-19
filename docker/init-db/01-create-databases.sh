#!/bin/bash
set -e
# Creates the propmange user and database. Used by the devcontainer Postgres only;
# prod uses the homelab's shared Postgres, where the role is created separately.

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER propmange WITH PASSWORD '${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required}';
	CREATE DATABASE propmange OWNER propmange;
EOSQL
