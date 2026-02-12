# ==============================================================================
# Multi-stage Dockerfile pour Spring Boot 3 + Java 21
# - Stage 1: build Maven (cache deps)
# - Stage 2: runtime JRE Alpine (non-root) + curl pour healthcheck (optionnel)
# ==============================================================================

# ------------------------------------------------------------------------------
# STAGE 1: BUILD
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven: dépendances d'abord
COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw


RUN mvn -B -DskipTests dependency:go-offline

# Build de l'application
COPY src ./src
RUN mvn -B -DskipTests clean package

# ------------------------------------------------------------------------------
# STAGE 2: RUNTIME
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/gust2301/sopikeur-back"
LABEL org.opencontainers.image.description="Sopikeur Backend - Spring Boot 3 + Java 21"
LABEL org.opencontainers.image.licenses="MIT"

# Outils utiles (pour healthcheck HTTP si tu en ajoutes un)
RUN apk add --no-cache curl

# Utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder /build/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring
EXPOSE 8080

# Optionnel (recommandé) : healthcheck container (actuator doit être dispo)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#   CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
