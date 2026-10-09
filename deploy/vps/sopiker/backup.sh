#!/usr/bin/env bash
# ==============================================================================
# /srv/sopiker/backup.sh — sauvegarde quotidienne Sopiker (lancée par systemd)
# ==============================================================================
# - Export SQL cohérent (--single-transaction) de production et staging
# - Archive des configurations (/srv/sopiker, /srv/proxy, .env compris)
# - Fichiers en 600 dans /var/backups/sopiker, rétention KEEP_DAYS jours
# Manuel :  sudo /srv/sopiker/backup.sh
# Restaurer : zcat <fichier>.sql.gz | sudo docker exec -i sopiker-mysql \
#               sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'
# ==============================================================================

set -euo pipefail

DEST="${BACKUP_DIR:-/var/backups/sopiker}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
FAILED=0

umask 077

for spec in production:sopiker-mysql staging:sopiker-mysql-staging; do
    env_name="${spec%%:*}"
    container="${spec#*:}"
    mkdir -p "$DEST/$env_name"
    out="$DEST/$env_name/$env_name-$STAMP.sql.gz"

    if docker exec "$container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --routines --events --triggers --hex-blob --no-tablespaces --set-gtid-purged=OFF --databases "$MYSQL_DATABASE"' \
        | gzip -9 > "$out.tmp" \
        && zcat "$out.tmp" | tail -n 1 | grep -q '^-- Dump completed'; then
        mv "$out.tmp" "$out"
        echo "OK $env_name $(stat -c %s "$out") octets -> $out"
    else
        rm -f "$out.tmp"
        echo "ERREUR: sauvegarde SQL $env_name incomplète" >&2
        FAILED=1
    fi
done

mkdir -p "$DEST/config"
tar -C / -czf "$DEST/config/config-$STAMP.tar.gz" srv/sopiker srv/proxy
echo "OK config $(stat -c %s "$DEST/config/config-$STAMP.tar.gz") octets"

find "$DEST" -type f \( -name '*.sql.gz' -o -name '*.tar.gz' \) -mtime +"$KEEP_DAYS" -delete

exit "$FAILED"
