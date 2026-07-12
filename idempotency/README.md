# Idempotency

A payments API that demonstrates idempotency-key handling: sending the same
logical request twice (client retry, double-click, network timeout) creates
the payment at most once.

## How it works

`POST /api/v1/payments` requires an `idempotency_key` header supplied by the
client (a UUID is typical). On each request:

1. **Postgres check** — if a payment is already recorded for this key,
   return it instead of creating a new one.
2. **Redis claim** — otherwise, try to atomically claim the key in Redis
   (`SETNX`). Only one concurrent request can win this claim.
3. **Winner** creates the payment and records the key → payment mapping in
   Postgres.
4. **Loser** (lost the Redis claim) polls Postgres briefly for the winner's
   result and returns that instead of creating its own payment.

Redis makes the common case (two truly concurrent requests) fast and
race-free; the Postgres unique constraint on `key_value` is the backstop if
Redis and the database ever disagree.

## Stack
- Java 21, Spring Boot 3.5
- Spring Data JPA + Postgres (durable key → payment record)
- Spring Data Redis (fast atomic claim)

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

## Example

```bash
curl -X POST localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "idempotency_key: $(uuidgen)" \
  -d '{"toAccount":"acct_123","amountCents":500}'
```

Repeating the same request with the same `idempotency_key` returns the same
payment instead of creating a second one.

## Status
✅ Done — naive endpoint (no protection) and idempotency-key protection are
both implemented and covered by tests, including a concurrent double-fire
test.