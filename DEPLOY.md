# 🚀 Guide de Déploiement Production - Sopikeur Backend

Ce guide explique comment déployer l'application Spring Boot sur votre VPS en utilisant Docker + GitHub Container Registry (GHCR).

## 📋 Table des matières

- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Configuration initiale](#configuration-initiale)
- [Déploiement par SHA](#déploiement-par-sha)
- [Rollback](#rollback)
- [Monitoring](#monitoring)
- [Dépannage](#dépannage)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions (CI/CD)                    │
│  ┌────────────┐      ┌────────────┐      ┌──────────────┐  │
│  │   Build    │─────>│    Test    │─────>│ Push to GHCR │  │
│  │   Maven    │      │   (skip)   │      │   (latest +  │  │
│  └────────────┘      └────────────┘      │     SHA)     │  │
│                                           └──────┬───────┘  │
└───────────────────────────────────────────────────┼──────────┘
                                                    │
                                                    ▼
┌─────────────────────────────────────────────────────────────┐
│                        VPS Production                        │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              docker-compose.prod.yml                  │   │
│  │                                                        │   │
│  │  ┌──────────────┐          ┌────────────────────┐   │   │
│  │  │    MySQL     │          │   Spring Boot      │   │   │
│  │  │   (8.4)      │<────────>│   Backend          │   │   │
│  │  │              │          │   (Java 21)        │   │   │
│  │  └──────────────┘          └────────────────────┘   │   │
│  │                                      │                │   │
│  └──────────────────────────────────────┼────────────────┘   │
│                                         │                    │
│                                    Port 8080                 │
│                                         │                    │
│                         ┌───────────────▼──────────────┐    │
│                         │   Reverse Proxy (optionnel)  │    │
│                         │   Nginx / Caddy / Traefik    │    │
│                         └───────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Prérequis

### Sur votre machine locale
- Git configuré
- Accès au repository GitHub

### Sur le VPS
- Docker Engine installé (v24+)
- Docker Compose installé (v2+)
- Accès SSH au VPS
- Nom de domaine configuré (optionnel)

### Installation Docker sur Ubuntu/Debian

```bash
# Installation Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Ajouter votre utilisateur au groupe docker
sudo usermod -aG docker $USER

# Vérifier l'installation
docker --version
docker compose version
```

---

## 🔧 Configuration initiale

### 1. Cloner le repository sur le VPS

```bash
# Créer un répertoire pour l'application
mkdir -p ~/sopikeur-back
cd ~/sopikeur-back

# Cloner uniquement les fichiers de config (ou créer manuellement)
# Option A: Clone complet puis cleanup
git clone https://github.com/gust2301/sopikeur-back.git .
rm -rf .git src pom.xml

# Option B: Télécharger uniquement docker-compose.prod.yml et .env.prod.example
wget https://raw.githubusercontent.com/gust2301/sopikeur-back/main/docker-compose.prod.yml
wget https://raw.githubusercontent.com/gust2301/sopikeur-back/main/.env.prod.example
```

### 2. Configurer les variables d'environnement

```bash
# Éditer avec vos vraies valeurs
nano .env.prod
```

**⚠️ IMPORTANT: Changez TOUS les mots de passe et secrets !**

```bash
# Exemple de génération de secrets sécurisés
# JWT Secret (256 bits en base64)
openssl rand -base64 32

# Mot de passe MySQL fort
openssl rand -base64 24
```

**Configuration minimale requise dans `.env.prod`:**

```env
# Image Docker (sera mis à jour à chaque déploiement)
APP_IMAGE_TAG=latest

# MySQL
MYSQL_DATABASE=sopikeur
MYSQL_USER=sopikeur
MYSQL_PASSWORD=VotreMotDePasseFort123!
MYSQL_ROOT_PASSWORD=VotreRootPasswordFort456!

# Spring Boot
SPRING_DATASOURCE_USERNAME=sopikeur
SPRING_DATASOURCE_PASSWORD=VotreMotDePasseFort123!

# JWT (généré avec: openssl rand -base64 32)
APP_JWT_SECRET=Votre_Secret_JWT_Long_Et_Aleatoire_Base64==

# JVM Options (ajuster selon RAM disponible)
JAVA_OPTS=-Xmx512m -Xms256m
```

### 3. Sécuriser le fichier .env.prod

```bash
# Restreindre les permissions (lecture/écriture uniquement pour l'utilisateur)
chmod 600 .env.prod

# Vérifier
ls -la .env.prod
# Devrait afficher: -rw------- 1 user user ...
```

### 4. Login sur GitHub Container Registry

```bash
# Créer un Personal Access Token (PAT) sur GitHub avec scope 'read:packages'
# Settings → Developer settings → Personal access tokens → Generate new token

# Login
echo "VOTRE_GITHUB_TOKEN" | docker login ghcr.io -u VOTRE_USERNAME_GITHUB --password-stdin

# Tester
docker pull ghcr.io/gust2301/sopikeur-back:latest
```

---

## 🎯 Déploiement par SHA

### Workflow typique

1. **Push du code sur GitHub** → GitHub Actions build automatiquement
2. **Récupérer le SHA du commit** depuis GitHub
3. **Mettre à jour `APP_IMAGE_TAG`** dans `.env.prod` sur le VPS
4. **Pull et redéployer** avec Docker Compose

### Étape par étape

#### 1. Identifier le SHA à déployer

Sur GitHub, allez sur la page **Actions** du repository:
- Cliquez sur le workflow "Docker Image CI/CD"
- Trouvez le build correspondant au commit que vous voulez déployer
- Notez le **SHA court** (7 premiers caractères du commit)

Exemple: `abc1234`

Ou via ligne de commande:

```bash
# Lister les derniers commits
git log --oneline -n 10
# Sortie: abc1234 feat: add new feature
```

#### 2. Sur le VPS: Mettre à jour .env.prod

```bash
cd ~/sopikeur-back

# Éditer .env.prod
nano .env.prod

# Modifier la ligne:
APP_IMAGE_TAG=abc1234  # Remplacer par le SHA voulu
```

#### 3. Pull de la nouvelle image

```bash
# Pull de l'image depuis GHCR
docker compose -f docker-compose.prod.yml pull backend

# Vérifier que l'image a bien été téléchargée
docker images | grep sopikeur-back
```

#### 4. Déployer

```bash
# Démarrer les services (MySQL + Backend)
docker compose -f docker-compose.prod.yml up -d

# Vérifier les logs
docker compose -f docker-compose.prod.yml logs -f backend
```

#### 5. Vérifier le déploiement

```bash
# Healthcheck Actuator
curl http://localhost:8080/actuator/health

# Devrait retourner:
# {"status":"UP"}

# Vérifier que l'application répond
curl http://localhost:8080/api/v1/health

# Vérifier les logs
docker compose -f docker-compose.prod.yml logs backend --tail=100
```

---

## ⏮️ Rollback

En cas de problème, revenez simplement à l'ancien SHA:

```bash
cd ~/sopikeur-back

# 1. Éditer .env.prod et remettre l'ancien SHA
nano .env.prod
# APP_IMAGE_TAG=def5678  (ancien SHA qui fonctionnait)

# 2. Pull de l'ancienne image
docker compose -f docker-compose.prod.yml pull backend

# 3. Redéployer
docker compose -f docker-compose.prod.yml up -d

# 4. Vérifier
curl http://localhost:8080/actuator/health
docker compose -f docker-compose.prod.yml logs backend
```

**💡 Astuce:** Gardez un historique des SHA déployés dans un fichier:

```bash
# Créer un fichier de log des déploiements
echo "$(date) - Deployed SHA: abc1234" >> ~/sopikeur-back/deploy-history.txt
```

---

## 📊 Monitoring

### Vérifier l'état des services

```bash
# Status des containers
docker compose -f docker-compose.prod.yml ps

# Santé des containers (healthchecks)
docker ps --format "table {{.Names}}\t{{.Status}}"

# Logs en temps réel
docker compose -f docker-compose.prod.yml logs -f

# Logs d'un service spécifique
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f mysql
```

### Métriques et ressources

```bash
# Utilisation CPU/Mémoire/Réseau
docker stats

# Espace disque des volumes
docker system df -v

# Inspecter un container
docker inspect sopikeur-backend
```

### Endpoints Spring Boot Actuator

Exposés sur `/actuator/*`:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Informations générales
curl http://localhost:8080/actuator/info

# Métriques
curl http://localhost:8080/actuator/metrics
```

---

## 🔧 Dépannage

### Backend ne démarre pas

```bash
# Vérifier les logs
docker compose -f docker-compose.prod.yml logs backend

# Problèmes courants:
# - MySQL pas prêt → Attendre que healthcheck passe
# - Variables d'env manquantes → Vérifier .env.prod
# - JWT_SECRET vide → Définir dans .env.prod
```

### MySQL connection refused

```bash
# Vérifier que MySQL est healthy
docker compose -f docker-compose.prod.yml ps mysql

# Tester la connexion depuis le backend
docker exec -it sopikeur-backend sh
wget --spider http://mysql:3306  # Devrait échouer mais montrer si réseau OK
```

### Port 8080 déjà utilisé

```bash
# Identifier le processus
sudo lsof -i :8080

# Changer le port dans .env.prod
echo "BACKEND_PORT=8081" >> .env.prod

# Redémarrer
docker compose -f docker-compose.prod.yml up -d
```

### Nettoyer les anciennes images

```bash
# Supprimer les images non utilisées
docker image prune -a

# Supprimer les containers arrêtés
docker container prune

# Nettoyer tout (containers, images, volumes non utilisés)
docker system prune -a --volumes
```

### Réinitialiser la base de données

```bash
# ⚠️ ATTENTION: Supprime toutes les données !
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d
```

---

## 🔒 Sécurité

### Checklist de sécurité

- ✅ `.env.prod` a les permissions `600`
- ✅ Tous les mots de passe ont été changés
- ✅ JWT_SECRET est long et aléatoire (min 256 bits)
- ✅ MySQL n'est PAS exposé publiquement (pas de `ports:` dans docker-compose)
- ✅ Firewall configuré (seulement ports 22, 80, 443 ouverts)
- ✅ Certificat SSL configuré (Let's Encrypt via Nginx/Caddy)
- ✅ Backup de la base de données configuré

### Backup MySQL

```bash
# Backup manuel
docker exec sopikeur-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} sopikeur > backup-$(date +%Y%m%d).sql

# Automatiser avec cron (exemple: tous les jours à 2h du matin)
crontab -e
# Ajouter:
# 0 2 * * * docker exec sopikeur-mysql mysqldump -u root -pVotrePassword sopikeur > ~/backups/sopikeur-$(date +\%Y\%m\%d).sql
```

---

## 📚 Ressources

- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [MySQL Docker Hub](https://hub.docker.com/_/mysql)

---

## 🆘 Support

En cas de problème:
1. Vérifier les logs: `docker compose logs backend`
2. Vérifier la configuration: `cat .env.prod` (sans exposer les secrets)
3. Tester le healthcheck: `curl http://localhost:8080/actuator/health`
4. Consulter ce guide de dépannage

---

**🎉 Bon déploiement !**

---

## 🌐 Runbook Caddy (HTTPS) pour `api.sopikeur.sn`

```bash
# 1) Vérifier le DNS (A record vers IP du VPS)
dig +short api.sopikeur.sn

# 2) Déployer / mettre à jour la stack (backend + mysql + caddy)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 3) Vérifier les logs Caddy (émission certificat Let's Encrypt + proxy)
docker logs -f sopikeur-caddy

# 4) Tester la racine de l'API
curl -I https://api.sopikeur.sn

# 5) Tester le health endpoint Spring Boot
curl -I https://api.sopikeur.sn/actuator/health

# 6) Firewall VPS: ouvrir uniquement 22, 80, 443 (pas 8080)
```

Rappel: si le reverse proxy est correctement en place, **ne pas ouvrir 8080 publiquement**.
