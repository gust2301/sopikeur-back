#!/usr/bin/env bash
# ==============================================================================
# /usr/local/sbin/sopiker-cf-firewall — n'accepte le web (80/443) que de Cloudflare
# ==============================================================================
# Docker contourne UFW. Selon la famille d'adresses, le trafic web passe par :
#   IPv4 : DNAT Docker -> chaîne FORWARD -> DOCKER-USER   (filtré ici)
#   IPv6 : docker-proxy -> chaîne INPUT                   (filtré ici, avant UFW)
# Les plages viennent de cloudflare.com/ips-v4 et ips-v6 (copie locale si
# le téléchargement échoue). SSH (22) n'est pas concerné.
# Lancé par sopiker-cf-firewall.service (démarrage, redémarrage de Docker)
# et sopiker-cf-firewall.timer (rafraîchissement hebdomadaire).
# Désactiver en urgence : sudo /usr/local/sbin/sopiker-cf-firewall --off
# ==============================================================================

set -euo pipefail

CACHE=/etc/sopiker-firewall
CHAIN=SOPIKER-CF-WEB
IFACE=$(ip -o route show default | awk '{print $5; exit}')

hook_rules() { # $1 = commande (iptables|ip6tables), $2 = chaîne d'accroche
    local cmd=$1 parent=$2
    if [ "$parent" = DOCKER-USER ]; then
        for spec in "tcp 80" "tcp 443" "udp 443"; do
            set -- $spec
            echo "-i $IFACE -p $1 -m conntrack --ctdir ORIGINAL --ctorigdstport $2 -j $CHAIN"
        done
    else
        for spec in "tcp 80" "tcp 443" "udp 443"; do
            set -- $spec
            echo "-i $IFACE -p $1 --dport $2 -j $CHAIN"
        done
    fi
}

unhook() { # $1 = commande, $2 = chaîne d'accroche
    local cmd=$1 parent=$2 rule
    while IFS= read -r rule; do
        # shellcheck disable=SC2086
        while $cmd -C "$parent" $rule 2>/dev/null; do $cmd -D "$parent" $rule; done
    done < <(hook_rules "$cmd" "$parent")
}

if [ "${1:-}" = "--off" ]; then
    for pair in "iptables DOCKER-USER" "ip6tables DOCKER-USER" "ip6tables INPUT"; do
        set -- $pair; unhook "$1" "$2"
    done
    echo "Filtrage Cloudflare désactivé : 80/443 ouverts à tous"
    exit 0
fi

install -d -m 755 "$CACHE"

fetch() { # $1 = v4|v6, $2 = nombre minimal de plages
    local tmp n bad
    tmp=$(mktemp)
    if curl -fsS -m 20 "https://www.cloudflare.com/ips-$1" -o "$tmp"; then
        n=$(grep -c . "$tmp" || true)
        bad=$(grep . "$tmp" | grep -Evc '^[0-9a-fA-F:.]+/[0-9]+$' || true)
        if [ "$n" -ge "$2" ] && [ "$bad" -eq 0 ]; then
            mv "$tmp" "$CACHE/ips-$1"; chmod 644 "$CACHE/ips-$1"
        else
            rm -f "$tmp"; echo "liste $1 invalide ($n plages, $bad lignes incorrectes), copie locale conservée" >&2
        fi
    else
        rm -f "$tmp"; echo "téléchargement $1 impossible, copie locale conservée" >&2
    fi
    [ -s "$CACHE/ips-$1" ] || { echo "ERREUR: aucune liste Cloudflare $1, rien n'est modifié" >&2; exit 1; }
}
fetch v4 10
fetch v6 5

build_chain() { # $1 = commande, $2 = fichier de plages
    local cmd=$1 list=$2 cidr
    $cmd -N "$CHAIN" 2>/dev/null || true
    $cmd -F "$CHAIN"
    while IFS= read -r cidr; do
        [ -n "$cidr" ] && $cmd -A "$CHAIN" -s "$cidr" -j RETURN
    done < "$list"
    $cmd -A "$CHAIN" -j DROP
}

hook() { # $1 = commande, $2 = chaîne d'accroche (règles en tête, une seule fois)
    local cmd=$1 parent=$2 rule
    while IFS= read -r rule; do
        # shellcheck disable=SC2086
        $cmd -C "$parent" $rule 2>/dev/null || $cmd -I "$parent" 1 $rule
    done < <(hook_rules "$cmd" "$parent")
}

build_chain iptables "$CACHE/ips-v4"
build_chain ip6tables "$CACHE/ips-v6"
hook iptables DOCKER-USER
hook ip6tables DOCKER-USER
hook ip6tables INPUT

echo "Filtrage Cloudflare actif sur $IFACE : $(grep -c . "$CACHE/ips-v4") plages IPv4, $(grep -c . "$CACHE/ips-v6") plages IPv6"
