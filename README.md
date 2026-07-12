# Software Engineering — Concepts Monorepo

A monorepo of **self-contained** projects. Each concept lives in its own folder
with its own pom.xml, docker-compose.yml, config, and README, and runs in
complete isolation.

## Concepts
| # | Concept | Folder | Port | Status |
|---|---------|--------|------|--------|
| 1 | Idempotency | [`idempotency/`](./idempotency) | 8080 | ✅ done |

## Run a single concept
```bash
cd idempotency
docker compose up -d
mvn spring-boot:run
mvn test
```
