# Sopi Keur Backend (V1)

Backend Spring Boot V1 pour le front Angular Sopi Keur.

## Stack
- Java 21 / Spring Boot 3.x
- Spring Web, Validation, Data JPA, Security (JWT)
- Flyway
- MapStruct + Lombok
- Actuator
- Swagger/OpenAPI (springdoc)
- MySQL

## Lancer en local

### 1) Démarrer MySQL
```bash
cp .env.dev.example .env.dev
# Remplir .env.dev avec vos valeurs locales
docker compose --env-file .env.dev up -d
```

### 2) Configurer l'application
Charger `.env.dev` ou exporter les variables requises avant de lancer Spring Boot :

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `ADMIN_SUPER_PASSWORD_HASH`

### 3) Lancer l'API
```bash
./mvnw spring-boot:run
```

### Swagger
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Admin (JWT + RBAC)
Seed Flyway crée `superadmin@sopikeur.sn` (mot de passe hashé via placeholder Flyway).

Configurer le hash BCrypt du super-admin via variable d'environnement:
```bash
export ADMIN_SUPER_PASSWORD_HASH='$2a$10$...'
```

Récupérer un token :
```bash
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"superadmin@sopikeur.sn","password":"<votre-mot-de-passe>"}'
```

### Base URL (prod)
- `https://api.sopikeur.sn/api/v1`

---

## 🐳 Déploiement Production (Docker + CI/CD)

### 📚 Documentation Complète

- **[DEPLOY.md](DEPLOY.md)** - Guide complet de déploiement sur VPS
- **[DOCKER_CHOICES.md](DOCKER_CHOICES.md)** - Explications des choix techniques
- **[INFRASTRUCTURE_SUMMARY.md](INFRASTRUCTURE_SUMMARY.md)** - Vue d'ensemble de l'infrastructure

### 🚀 Quick Start (Production)

```bash
nano .env.prod  # Changer TOUS les secrets

# 2. Login GHCR
echo "GITHUB_TOKEN" | docker login ghcr.io -u USERNAME --password-stdin

# 3. Déployer
./deploy.sh abc1234  # Remplacer par le SHA voulu
```

### 🔄 Workflow CI/CD

```
Git Push → GitHub Actions → GHCR → VPS Deploy
```

- **Image**: `ghcr.io/gust2301/sopikeur-back`
- **Tags**: `latest` + SHA court (ex: `abc1234`)
- **Build**: Multi-stage (Maven + JRE Alpine)
- **Healthcheck**: `/actuator/health`

### 📦 Fichiers Infrastructure

```
├── Dockerfile                    # Multi-stage optimisé
├── docker-compose.prod.yml       # MySQL + Backend
├── .github/workflows/            # CI/CD GitHub Actions
└── deploy.sh                     # Script helper déploiement
```

---

## Endpoints principaux
### Public
- `GET /api/v1/products?type=SPC|PANEL&featured=true`
- `GET /api/v1/products/{slug}`
- `GET /api/v1/inspirations?tag=SPC|PANEL|BOTH`
- `GET /api/v1/inspirations/{slug}`
- `POST /api/v1/quotes`
- `POST /api/v1/contact`
- `POST /api/v1/preorders`

### Admin (JWT)
- `GET/POST/PUT/DELETE /api/v1/admin/products`
- `GET/PUT /api/v1/admin/stocks`
- `POST /api/v1/admin/stock/movements`
- `GET /api/v1/admin/quotes`
- `PATCH /api/v1/admin/quotes/{id}`
- `GET /api/v1/admin/contact`
- `PATCH /api/v1/admin/contact/{id}`
- `GET/POST/PUT/DELETE /api/v1/admin/users` (SUPER_ADMIN)
- `GET /api/v1/admin/roles` (SUPER_ADMIN)
- `PUT /api/v1/admin/users/{id}/roles` (SUPER_ADMIN)
- `GET /api/v1/admin/preorders`
- `PATCH /api/v1/admin/preorders/{id}`
- `GET/POST/PUT/DELETE /api/v1/admin/inspirations`
- `POST /api/v1/admin/inspirations/{id}/products/{productId}`
- `DELETE /api/v1/admin/inspirations/{id}/products/{productId}`
