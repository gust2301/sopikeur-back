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
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

## Endpoints principaux
### Public
- `GET /api/products?type=SPC|PANEL&featured=true`
- `GET /api/products/{slug}`
- `GET /api/inspirations?tag=SPC|PANEL|BOTH`
- `GET /api/inspirations/{slug}`
- `POST /api/quotes`
- `POST /api/contact`
- `POST /api/preorders`

### Admin (JWT)
- `PATCH /api/admin/products/{id}/stock`
- `POST /api/admin/stock/movements`
- `GET /api/admin/quotes`
- `PATCH /api/admin/quotes/{id}`
- `GET /api/admin/contact`
- `PATCH /api/admin/contact/{id}`
- `GET /api/admin/preorders`
- `PATCH /api/admin/preorders/{id}`
- `GET/POST/PUT/DELETE /api/admin/inspirations`
- `POST /api/admin/inspirations/{id}/products/{productId}`
- `DELETE /api/admin/inspirations/{id}/products/{productId}`
