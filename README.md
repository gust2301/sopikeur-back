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
docker compose up -d
```

### 2) Configurer l'application
Le fichier `application.yml` pointe vers :
- DB `sopikeur` / user `sopikeur` / password `sopikeur`
- JWT secret en dev

### 3) Lancer l'API
```bash
./mvnw spring-boot:run
```

### Swagger
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Admin (JWT)
Seed admin Flyway :
- username: `admin`
- password: `password`

Récupérer un token :
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

### Base URL (prod)
- `https://api.sopikeur.sn/api/v1`

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
- `PATCH /api/v1/admin/products/{id}/stock`
- `POST /api/v1/admin/stock/movements`
- `GET /api/v1/admin/quotes`
- `PATCH /api/v1/admin/quotes/{id}`
- `GET /api/v1/admin/contact`
- `PATCH /api/v1/admin/contact/{id}`
- `GET /api/v1/admin/preorders`
- `PATCH /api/v1/admin/preorders/{id}`
- `GET/POST/PUT/DELETE /api/v1/admin/inspirations`
- `POST /api/v1/admin/inspirations/{id}/products/{productId}`
- `DELETE /api/v1/admin/inspirations/{id}/products/{productId}`
