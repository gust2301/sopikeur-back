#!/usr/bin/env bash
# ==============================================================================
# /srv/sopiker/backup.sh — sauvegarde quotidienne Sopiker (lancée par systemd)
# ==============================================================================
# - Export SQL cohérent (--single-transaction) de production et staging
# - Archive des configurations (/srv/sopiker, /srv/proxy, .env compris)
# - Fichiers en 600 dans /var/backups/sopiker, rétention KEEP_DAYS jours
# - Copie hors VPS : chiffrée avec age (clé publique seule sur le VPS) puis
#   envoyée dans Cloudflare R2, rétention R2_KEEP_DAYS jours
# Manuel :  sudo /srv/sopiker/backup.sh
# Config R2 (saisie des clés, une fois) : sudo /srv/sopiker/backup.sh --setup-r2
# Restaurer : zcat <fichier>.sql.gz | sudo docker exec -i sopiker-mysql \
#               sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'
# Depuis R2 : age -d -i sopiker-backups-age.key <fichier>.age > <fichier>
# ==============================================================================

set -euo pipefail

DEST="${BACKUP_DIR:-/var/backups/sopiker}"
KEEP_DAYS="${KEEP_DAYS:-14}"
CONF_DIR=/etc/sopiker-backup
R2_ENV="$CONF_DIR/r2.env"
RECIPIENT="$CONF_DIR/age-recipient.txt"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
FAILED=0

umask 077

r2_load() {
    # shellcheck disable=SC1090
    set -a; . "$R2_ENV"; set +a
    export RCLONE_CONFIG_R2_TYPE=s3 \
        RCLONE_CONFIG_R2_PROVIDER=Cloudflare \
        RCLONE_CONFIG_R2_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID" \
        RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY" \
        RCLONE_CONFIG_R2_ENDPOINT="https://$R2_ACCOUNT_ID.r2.cloudflarestorage.com" \
        RCLONE_CONFIG_R2_NO_CHECK_BUCKET=true
}

if [ "${1:-}" = "--setup-r2" ]; then
    install -d -m 700 "$CONF_DIR"
    read -rp "Account ID Cloudflare [e19d509e124c4c1905d2998415294a0a] : " account
    read -rp "Bucket [sopiker-backups] : " bucket
    read -rp "Access Key ID : " key_id
    read -rsp "Secret Access Key (invisible) : " secret; echo
    printf 'R2_ACCOUNT_ID=%s\nR2_BUCKET=%s\nR2_ACCESS_KEY_ID=%s\nR2_SECRET_ACCESS_KEY=%s\n' \
        "${account:-e19d509e124c4c1905d2998415294a0a}" "${bucket:-sopiker-backups}" "$key_id" "$secret" > "$R2_ENV.tmp"
    chmod 600 "$R2_ENV.tmp" && mv "$R2_ENV.tmp" "$R2_ENV"
    r2_load
    probe="setup-test-$STAMP.txt"
    if echo ok | rclone rcat "r2:$R2_BUCKET/$probe" && rclone deletefile "r2:$R2_BUCKET/$probe"; then
        echo "R2 OK : écriture et suppression réussies dans $R2_BUCKET"
    else
        echo "ERREUR : accès R2 refusé (vérifier clés, bucket, permission Object Read & Write)" >&2
        exit 1
    fi
    exit 0
fi

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

# ------------------------------------------------------------------------------
# Copie chiffrée hors VPS (Cloudflare R2)
# ------------------------------------------------------------------------------
if [ -f "$R2_ENV" ] && [ -f "$RECIPIENT" ]; then
    r2_load
    while IFS= read -r file; do
        rel="${file#"$DEST"/}"
        if age -R "$RECIPIENT" "$file" | rclone rcat "r2:$R2_BUCKET/$rel.age"; then
            echo "OK R2 $rel.age"
        else
            echo "ERREUR: envoi R2 $rel" >&2
            FAILED=1
        fi
    done < <(find "$DEST" -type f -name "*-$STAMP.*")
    rclone delete "r2:$R2_BUCKET" --min-age "${R2_KEEP_DAYS:-30}d" --include "*.age" \
        || { echo "ERREUR: nettoyage R2" >&2; FAILED=1; }
else
    echo "ATTENTION: R2 non configuré, aucune copie hors VPS (sudo $0 --setup-r2)" >&2
    FAILED=1
fi

exit "$FAILED"
