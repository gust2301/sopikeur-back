# 🚀 Guide de Déploiement - Sopiker Backend

Déploiement du backend Spring Boot sur le VPS OVH (VPS-3, Varsovie) avec Docker et GitHub Container Registry (GHCR).

> Les noms DNS restent en `sopikeur.sn` (domaine historique) ; tout le reste (dossiers, conteneurs, volumes, réseaux) s'écrit `sopiker`.

## 📋 Table des matières

- [Architecture](#️-architecture)
- [Organisation du VPS](#-organisation-du-vps)
- [Déployer une nouvelle version](#-déployer-une-nouvelle-version)
- [Rollback](#️-rollback)
- [Mode isolé / mode live](#-mode-isolé--mode-live)
- [Monitoring et dépannage](#-monitoring-et-dépannage)
- [Installation initiale d'un VPS](#-installation-initiale-dun-vps)
- [Sécurité](#-sécurité)

---

## 🏗️ Architecture

```
GitHub Actions (push develop/master)
  └─ mvn verify → build image → ghcr.io/gust2301/sopikeur-back:<sha court> (+ latest sur master)

Cloudflare (proxy, TLS) ──► VPS 57.131.201.16
                              │
                              └─ /srv/proxy : Caddy partagé (80/443, HTTPS)
                                   ├─ api.sopikeur.sn         ─► sopiker-backend         ─► sopiker-mysql
                                   └─ api-staging.sopikeur.sn ─► sopiker-backend-staging ─► sopiker-mysql-staging
```

- Le front (`sopikeur.sn`) et le back-office (`admin.sopikeur.sn`) sont sur Cloudflare Pages et appellent ces API.
- MySQL n'est jamais exposé : chaque environnement a son réseau `db` interne.
- Caddy rejoint chaque backend par un réseau `edge` interne dédié ; production et staging ne se voient pas.

---

## 📁 Organisation du VPS

Les fichiers de référence sont dans [`deploy/vps/`](deploy/vps/) et reproduisent `/srv` :

```
/srv/
├── proxy/                       ← Caddy partagé par tous les projets du VPS
│   ├── compose.yml
│   ├── Caddyfile                ← blocs communs + import sites/*.caddy
│   ├── sites/sopiker.caddy      ← api + api-staging
│   └── .env                     ← BIND_PREFIX (voir .env.example)
└── sopiker/
    ├── deploy.sh                ← ce script (copié depuis le repo)
    ├── production/
    │   ├── compose.yml
    │   ├── compose.live.yml     ← accès Internet sortant (mode live)
    │   ├── .env                 ← secrets, chmod 600, jamais commité
    │   └── deploy-history.txt
    └── staging/                 ← même structure
```

Docker nécessite `sudo` sur le VPS (l'utilisateur `ubuntu` n'est pas dans le groupe `docker`).

---

## 🎯 Déployer une nouvelle version

1. **Merger / pousser** sur `develop` (staging) ou `master` (production) → GitHub Actions publie l'image.
2. **Récupérer le SHA court** du commit (7 caractères), par ex. via `git log --oneline -n 5` ou la page *Actions*.
3. **Sur le VPS :**

```bash
ssh -i ~/.ssh/sopikeur_mac_ovh ubuntu@57.131.201.16

# Staging d'abord
sudo /srv/sopiker/deploy.sh staging abc1234

# Puis production, une fois le staging validé
sudo /srv/sopiker/deploy.sh production abc1234
```

Le script met à jour `APP_IMAGE_TAG` dans `.env` (backup en `.env.backup`), télécharge l'image, redémarre **uniquement le backend**, attend le healthcheck Docker et journalise dans `deploy-history.txt`.

**Vérifier depuis l'extérieur :**

```bash
curl -s https://api.sopikeur.sn/actuator/health
curl -s https://api-staging.sopikeur.sn/actuator/health
```

---

## ⏮️ Rollback

Le script affiche l'ancien tag à la fin de chaque déploiement :

```bash
sudo /srv/sopiker/deploy.sh production <ANCIEN_TAG>
```

Historique : `cat /srv/sopiker/production/deploy-history.txt`.

⚠️ Un rollback d'image ne défait pas une migration Flyway déjà appliquée : vérifier la compatibilité du schéma avant de revenir en arrière.

---

## 🔌 Mode isolé / mode live

Les réseaux `db` et `edge` n'ont **pas d'accès Internet**. Sans `compose.live.yml`, le backend ne peut appeler ni Stripe, ni Wave, ni Orange Money, ni envoyer d'e-mails ou de notifications : pratique pour tester une copie sans effet externe.

| Mode | Ligne dans `.env` | Usage |
|---|---|---|
| Isolé | *(absente)* | Tests, restauration, copie de vérification |
| Live | `COMPOSE_FILE=compose.yml:compose.live.yml` | Production réelle |

De même, Caddy n'écoute que sur `127.0.0.1` tant que `/srv/proxy/.env` ne contient pas `BIND_PREFIX=` (vide). Docker contourne UFW : c'est ce réglage qui contrôle l'exposition publique.

Après modification d'un `.env` :

```bash
cd /srv/sopiker/production && sudo docker compose up -d
cd /srv/proxy && sudo docker compose up -d
```

---

## 📊 Monitoring et dépannage

```bash
cd /srv/sopiker/production            # ou staging

sudo docker compose ps                # état et healthchecks
sudo docker compose logs -f backend   # logs (backend_staging en staging)
sudo docker stats                     # CPU / mémoire de tous les conteneurs
sudo docker logs -f proxy-caddy       # HTTPS, certificats, routage

# Tester une API sans passer par Cloudflare (depuis le VPS)
curl -s --resolve api.sopikeur.sn:443:127.0.0.1 https://api.sopikeur.sn/actuator/health
```

| Symptôme | Piste |
|---|---|
| `pull` refusé (`denied`, `unauthorized`) | Refaire `sudo docker login ghcr.io -u gust2301` avec un PAT `read:packages` valide |
| Backend `unhealthy` | `docker compose logs backend` ; variable manquante dans `.env` ; MySQL pas prêt |
| 502 via Caddy | Backend arrêté, ou nom de conteneur différent de `sites/sopiker.caddy` |
| Appels externes en échec (paiements, e-mails) | Mode isolé actif : ajouter `COMPOSE_FILE=…compose.live.yml` |

⚠️ `docker compose down -v` **supprime les volumes MySQL**. Ne jamais l'utiliser en production.

---

## 🧰 Installation initiale d'un VPS

1. Ubuntu LTS, accès SSH par clé uniquement (`PasswordAuthentication no`, `PermitRootLogin no`), UFW avec 22 ouvert.
2. Docker Engine + Compose depuis le [dépôt officiel](https://docs.docker.com/engine/install/ubuntu/), logs Docker en rotation (`/etc/docker/daemon.json`).
3. Copier `deploy/vps/proxy` → `/srv/proxy` et `deploy/vps/sopiker` → `/srv/sopiker`, plus `deploy.sh` → `/srv/sopiker/deploy.sh` (`chmod 750`).
4. Créer chaque `.env` depuis son `.env.example` (`chmod 600`).
5. `sudo docker login ghcr.io -u gust2301` avec un PAT GitHub limité à `read:packages`.
6. Démarrer les projets **avant** le proxy (le proxy rejoint leurs réseaux `edge`) :

```bash
cd /srv/sopiker/production && sudo docker compose up -d
cd /srv/sopiker/staging    && sudo docker compose up -d
cd /srv/proxy              && sudo docker compose up -d
```

---

## 🔒 Sécurité

- ✅ `.env` en `600`, jamais commités (voir `.gitignore`)
- ✅ MySQL sur réseau interne uniquement, aucun port publié
- ✅ Backends sans port publié, joignables seulement par Caddy
- ✅ Production et staging sur des réseaux séparés
- ✅ SSH par clé uniquement, UFW actif
- ⬜ Sauvegarde SQL automatique hors VPS (à mettre en place)
- ⬜ Restreindre 80/443 aux IP Cloudflare (à évaluer)

---

## 🗄️ Ancien serveur (Public Cloud b2-7)

`docker-compose.prod.yml` et `Caddyfile` à la racine du repo décrivent l'ancien serveur (`~/deploy/sopiker`, conteneurs `sopikeur-*`). Ils sont conservés pendant la période de retour arrière et seront supprimés avec l'instance.
