# Concurrency

A hands-on lab of REST endpoints that each demonstrate one Java concurrency
concept: sequential vs. parallel work, lost updates on shared state, fixing
them with locks/atomics, thread-safe collections, and deadlocks.

## Endpoints

| Endpoint | Demonstrates |
|---|---|
| `GET /sequential` | Fetches 5 "URLs" (simulated 1s each) one at a time. Baseline: ~5s total. |
| `GET /concurrent` | Fetches the same 5 URLs via a 5-thread `ExecutorService`, submitting all tasks before collecting results so the waits overlap. ~1s total. |
| `GET /race` | 1000 concurrent threads incrementing a plain `int` with no synchronization. A `Thread.sleep(1)` between the read and the write widens the race window, so the final count comes in below 1000 — a classic **lost update**. |
| `GET /fixed` | Runs the same 1000-increment workload three ways side by side: unsynchronized (still broken), behind a `synchronized` lock (correct), and via `AtomicInteger` (correct, lock-free). |
| `GET /map` | Same broken-vs-fixed comparison, but for a `Map`: a plain `HashMap` loses updates under concurrent `put()`s on the same key, while `ConcurrentHashMap.merge()` doesn't. |
| `GET /deadlock` | Two threads acquire two locks (`lockA`, `lockB`) in opposite orders. Each ends up holding one lock while waiting on the other — classic deadlock, detected via a timed `join(3000)`. |
| `GET /deadlock-fixed` | Same two threads, but both acquire the locks in the same order. A consistent lock order removes the cycle, so no deadlock occurs. |

## Stack
- Java 21, Spring Boot 3.5
- Plain `java.util.concurrent` (`ExecutorService`, `Future`, `AtomicInteger`, `ConcurrentHashMap`) — no external dependencies needed to see these concepts.

## Run

```bash
mvn -pl concurrency spring-boot:run
```

App listens on `localhost:8081`.

## Example

```bash
curl localhost:8081/sequential   # ~5000 ms
curl localhost:8081/concurrent   # ~1000 ms
curl localhost:8081/race         # lost updates > 0
curl localhost:8081/fixed        # unsafe still broken, sync/atomic exact
curl localhost:8081/map          # HashMap short of expected, ConcurrentHashMap exact
curl localhost:8081/deadlock     # hangs ~3s, then reports DEADLOCK
curl localhost:8081/deadlock-fixed
```

## Status
✅ Done — sequential/concurrent baseline, race condition + two fixes
(`synchronized`, `AtomicInteger`), thread-safe collections (`ConcurrentHashMap`),
and a lock-ordering deadlock + fix are all implemented.