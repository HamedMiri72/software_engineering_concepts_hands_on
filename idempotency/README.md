# Idempotency

Demonstrates handling idempotency keys on a Spring Boot API, backed by
Redis (fast key lookup) and Postgres (durable storage via JPA).

## Stack
- Java 21, Spring Boot 3.5
- Spring Data JPA + Postgres
- Spring Data Redis

## Run

Start dependencies:
```bash
docker compose up -d
```

Run the app:
```bash
mvn spring-boot:run
```

Run tests:
```bash
mvn test
```

App listens on `localhost:8080`.

## Status
🚧 In progress — skeleton only, no idempotency logic yet.