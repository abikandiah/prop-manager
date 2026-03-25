#!/bin/sh
# Writes the crontab from $DB_CLEAR_SCHEDULE and starts crond.
# PGHOST, PGDATABASE, PGUSER, PGPASSWORD are injected by docker-compose.
set -e

SCHEDULE="${DB_CLEAR_SCHEDULE:-0 3 * * 0}"

# Export PGPASSWORD into crond's environment (crond inherits /etc/environment on Alpine).
echo "PGHOST=${PGHOST}" > /etc/environment
echo "PGDATABASE=${PGDATABASE}" >> /etc/environment
echo "PGUSER=${PGUSER}" >> /etc/environment
echo "PGPASSWORD=${PGPASSWORD}" >> /etc/environment

# Write root's crontab.
echo "${SCHEDULE} /db-scheduler/clear-db.sh >> /var/log/clear-db.log 2>&1" > /etc/crontabs/root

echo "DB scheduler ready."
echo "  Schedule : ${SCHEDULE} (UTC)"
echo "  Target   : ${PGUSER}@${PGHOST}/${PGDATABASE}"
echo ""
echo "Logs: docker compose logs db-scheduler  (or: docker exec prop-manager-db-scheduler tail -f /var/log/clear-db.log)"

# -f: foreground  -l 6: log level (info)
exec crond -f -l 6
