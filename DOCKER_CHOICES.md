# 🏗️ Choix Techniques - Infrastructure Docker

Ce document explique les décisions d'architecture et les best practices implémentées dans la configuration Docker/CI-CD.

---

## 📦 Dockerfile Multi-Stage

### Choix: Maven Alpine + JRE Alpine

**Stage 1: Builder**
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
```

**Avantages:**
- ✅ **Image Alpine** (~50 MB) vs Ubuntu (~200 MB) → build plus rapide
- ✅ **Cache Maven optimisé**: Copie d'abord `pom.xml` seul → layer cache si pas de changement de dépendances
- ✅ **Offline mode**: `mvn dependency:go-offline` télécharge toutes les deps avant le build
- ✅ **Skip tests**: `-DskipTests` car tests déjà exécutés en CI (ou à ajouter)

**Stage 2: Runtime**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
```

**Avantages:**
- ✅ **JRE seulement** (pas JDK complet) → image finale ~180 MB vs ~350 MB
- ✅ **Temurin**: Distribution OpenJDK officielle, support LTS
- ✅ **Alpine**: Sécurité renforcée, surface d'attaque réduite

---

## 👤 Utilisateur Non-Root

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
```

### Pourquoi c'est critique:

**Problème:** Par défaut, les containers Docker tournent en `root` (UID 0).

**Risques:**
- 🔴 Si un attaquant exploite une faille dans Spring Boot, il obtient root dans le container
- 🔴 Avec une mauvaise config Docker, root container = root host (escalade de privilèges)
- 🔴 Non-conforme aux standards de sécurité (CIS Docker Benchmark, NIST)

**Bénéfices:**
- ✅ **Principe du moindre privilège**: L'app n'a que les permissions nécessaires
- ✅ **Isolation renforcée**: Même en cas de compromission, l'attaquant n'a pas root
- ✅ **Audit et compliance**: Requis par de nombreuses certifications (ISO 27001, SOC 2)

**Alternative non recommandée:**
```dockerfile
# ❌ Mauvais: app tourne en root
USER root
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## 🏥 Healthcheck

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### Paramètres expliqués:

| Paramètre | Valeur | Justification |
|-----------|--------|---------------|
| `interval` | 30s | Vérifie la santé toutes les 30 secondes (compromis perfs/réactivité) |
| `timeout` | 3s | Si l'app ne répond pas en 3s, considérée comme down |
| `start-period` | 60s | Grace period de 60s au démarrage (Spring Boot met ~30-45s à démarrer) |
| `retries` | 3 | 3 échecs consécutifs avant de marquer le container comme unhealthy |

### Pourquoi c'est important:

**Sans healthcheck:**
```bash
docker ps
# STATUS: Up 5 minutes  ← Container tourne, mais app peut être crashée !
```

**Avec healthcheck:**
```bash
docker ps
# STATUS: Up 5 minutes (healthy)  ← On sait que l'app répond vraiment
# STATUS: Up 5 minutes (unhealthy)  ← App en erreur même si container up
```

**Bénéfices:**
- ✅ **Orchestration**: Docker Compose attend que MySQL soit `healthy` avant de démarrer le backend
- ✅ **Auto-restart**: Si unhealthy, Docker peut redémarrer automatiquement (avec `restart: unless-stopped`)
- ✅ **Load balancers**: En production avec Kubernetes/Swarm, les pods unhealthy sont retirés du load balancing
- ✅ **Monitoring**: Les outils (Prometheus, Datadog) peuvent alerter sur unhealthy

### Endpoint utilisé: Spring Boot Actuator

**Configuration requise dans `application.yml`:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: when-authorized
```

L'endpoint `/actuator/health` retourne:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 🚀 JAVA_OPTS Paramétrable

```dockerfile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### Pourquoi pas un ENTRYPOINT direct?

**❌ Mauvais: Pas flexible**
```dockerfile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```
→ Impossible d'ajouter des options JVM sans rebuild de l'image

**✅ Bon: Variable d'environnement**
```dockerfile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```
→ Configurable via `.env.prod` ou `docker run -e JAVA_OPTS="..."`

### Exemples d'utilisation:

**1. Limiter la mémoire (essentiel en prod):**
```bash
JAVA_OPTS="-Xmx512m -Xms256m"
```
- `Xmx`: Mémoire max (évite OOM killer si VPS a 1 GB RAM)
- `Xms`: Mémoire initiale (réduit les GC au démarrage)

**2. Activer le garbage collector G1 (recommandé Java 21):**
```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**3. Profiling en cas de problème:**
```bash
JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
```

**4. Remote debugging (dev uniquement):**
```bash
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

---

## 🐳 Cache Maven dans Dockerfile

### Technique du layer caching:

```dockerfile
# Étape 1: Copier SEULEMENT pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Étape 2: Copier le code source
COPY src ./src
RUN mvn clean package -DskipTests -B
```

### Pourquoi dans cet ordre?

**Scénario A: Changement dans `src/` mais pas `pom.xml`**
```
Build #1: 10 minutes (télécharge deps + compile)
Build #2: 30 secondes (cache deps, seulement recompile)
```

**Scénario B: Sans cache (tout copié d'un coup)**
```
Build #1: 10 minutes
Build #2: 10 minutes (re-télécharge tout)
```

**Gain de temps:**
- ✅ 95% des builds ne changent que le code, pas les dépendances
- ✅ Rebuild local: **30s** au lieu de **10 min**
- ✅ CI/CD plus rapide: **moins de coûts GitHub Actions**

---

## 🏷️ Tags Docker: latest + SHA

### Configuration GitHub Actions:

```yaml
tags: |
  type=raw,value=latest,enable={{is_default_branch}}
  type=sha,format=short,prefix=
```

**Résultat:**
```
ghcr.io/gust2301/sopikeur-back:latest
ghcr.io/gust2301/sopikeur-back:abc1234
```

### Pourquoi les deux?

| Tag | Usage | Avantages | Inconvénients |
|-----|-------|-----------|---------------|
| `latest` | Dev/test rapide | Toujours la dernière version | ❌ Pas traçable, rollback impossible |
| `SHA` | Production | ✅ Immuable, reproductible, rollback facile | Nécessite de noter le SHA |

### Best practice en production:

**❌ Déploiement avec `latest`:**
```bash
# Dangereux: Impossible de savoir quelle version tourne
docker pull ghcr.io/gust2301/sopikeur-back:latest
```

**✅ Déploiement avec SHA:**
```bash
# Traçable: On sait exactement quelle version est déployée
docker pull ghcr.io/gust2301/sopikeur-back:abc1234
```

**Avantages du SHA:**
- ✅ **Traçabilité**: `git show abc1234` pour voir le code exact déployé
- ✅ **Rollback instantané**: Changer `APP_IMAGE_TAG=def5678` dans `.env.prod`
- ✅ **Audit**: Garder un log des déploiements avec SHA + date
- ✅ **Blue/Green deploy**: Déployer `abc1234` sur un nouveau container avant de switcher

---

## 🔒 Sécurité MySQL en Production

### Configuration docker-compose:

```yaml
mysql:
  image: mysql:8.4
  networks:
    - sopikeur-internal
  # ❌ PAS DE ports: mapping → MySQL non exposé publiquement
```

### Pourquoi ne pas exposer MySQL?

**❌ Configuration dangereuse:**
```yaml
ports:
  - "3306:3306"  # MySQL accessible depuis Internet
```

**Risques:**
- 🔴 Brute force sur le port 3306
- 🔴 Exploitation de vulnérabilités MySQL
- 🔴 Scans automatisés (Shodan, etc.)

**✅ Configuration sécurisée:**
```yaml
# Pas de ports: mapping
networks:
  - sopikeur-internal  # Réseau interne Docker uniquement
```

**Accès:**
- ✅ Backend peut accéder via `mysql:3306` (nom du service Docker)
- ❌ Internet ne peut PAS accéder au port 3306
- ✅ Admin peut accéder via: `docker exec -it sopikeur-mysql mysql -u root -p`

---

## 🎯 Résumé des Best Practices

| Pratique | Implémenté | Impact |
|----------|-----------|--------|
| Multi-stage build | ✅ | Image 2x plus petite |
| Alpine Linux | ✅ | Surface d'attaque réduite |
| Non-root user | ✅ | Sécurité renforcée |
| Healthcheck | ✅ | Détection de pannes |
| Cache Maven | ✅ | Build 20x plus rapide |
| JAVA_OPTS flexible | ✅ | Tuning sans rebuild |
| Tags SHA | ✅ | Déploiements traçables |
| MySQL non exposé | ✅ | Sécurité DB |
| .dockerignore | ✅ | Contexte build léger |
| Secrets via .env | ✅ | Pas de secrets en dur |

---

## 📚 Références

- [Docker Multi-Stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker)
- [JVM Container Settings](https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness)

---

**💡 Questions ou suggestions d'amélioration? Ouvrir une issue sur GitHub!**
