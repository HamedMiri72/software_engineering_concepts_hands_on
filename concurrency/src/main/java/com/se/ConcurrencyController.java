package com.se;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ConcurrencyController {


    // Pretend these are real URLs. Each "fetch" takes ~1 second.
    private static final List<String> URLS = List.of(
            "https://a.example.com",
            "https://b.example.com",
            "https://c.example.com",
            "https://d.example.com",
            "https://e.example.com"
    );

    // Simulates a network call: 1 second of "waiting for the server".
    private String fetch(String url) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "200 OK  <- " + url + "  (fetched on thread: "
                + Thread.currentThread().getName() + ")";
    }

    @GetMapping("/sequential")
    public String sequential() {
        long start = System.currentTimeMillis();

        StringBuilder out = new StringBuilder();
        for (String url : URLS) {          // one at a time
            out.append(fetch(url)).append("\n");
        }

        long elapsed = System.currentTimeMillis() - start;
        return out + "\nTOTAL TIME: " + elapsed + " ms\n";
    }

    @GetMapping("/concurrent")
    public String concurrent() throws InterruptedException, ExecutionException {
        long start = System.currentTimeMillis();

        StringBuilder out = new StringBuilder();

        // A pool of 5 worker threads. We hand it tasks; it runs them on its workers.
        // try-with-resources ensures the pool is shut down even if get() throws.
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {

            // STEP 1: submit ALL tasks first. Each submit() returns immediately with a
            // Future (an IOU). The task starts running on a worker thread right away.
            // Because we submit all five before collecting any result, all five workers
            // hit their Thread.sleep at the same time — the waits overlap.
            List<Future<String>> futures = new ArrayList<>();
            for (String url : URLS) {
                Future<String> future = pool.submit(() -> fetch(url)); // returns instantly
                futures.add(future);
            }

            // STEP 2: now collect the results. future.get() BLOCKS until that task is
            // done. But since all five are already running concurrently, by the time
            // the first one finishes the others are nearly done too.
            for (Future<String> future : futures) {
                out.append(future.get()).append("\n");   // waits for the result
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        return out + "\nTOTAL TIME: " + elapsed + " ms\n";
    }

    // Shared, unguarded state. Multiple threads read-modify-write this
    // without any synchronization, so updates can be lost.
    private int unsafeCount = 0;

    // "current + 1" is really three separate steps: read unsafeCount, compute
    // current + 1, write it back. The Thread.sleep(1) here just widens the
    // window between the read and the write, making the race easy to see —
    // two threads can both read the same "current" value before either
    // writes back, so one of their increments gets silently overwritten.
    private void incrementUnsafe(){
        int current = unsafeCount;

        try{
            Thread.sleep(1);
        }catch (InterruptedException ex){
            Thread.currentThread().interrupt();
        }
        unsafeCount = current + 1;
    }

    @GetMapping("/race")
    public String race() throws InterruptedException{

        unsafeCount = 0;

        int tasks = 1000;

        // 50 threads hammering the same unsafeCount field concurrently.
        ExecutorService pool = Executors.newFixedThreadPool(50);
        List<Future<?>> futures = new ArrayList<>();

        // Fire off all 1000 increments so their read-sleep-write windows overlap.
        for(int i = 0; i < tasks; i++){
            Future<?> future = pool.submit(this::incrementUnsafe);
            futures.add(future);
        }

        // Wait for every task to finish before reading the final count.
        for(Future<?> future: futures){
            try{
                future.get();
            }catch (ExecutionException ex){
                //just ignore for demo
            }
        }

        pool.shutdown();

        // Expected == Actual only if increments never interleave. In practice,
        // concurrent read-modify-write on a plain int loses updates, so
        // actualCount ends up lower than tasks — those are the "lost updates".
        return "Expected count: " + tasks + "\n"
                + "Actual count:   " + unsafeCount + "\n"
                + "Lost updates:   " + (tasks - unsafeCount) + "\n";
    }

    // Second counter for the "fixed" comparison: guarded by a lock instead of left bare.
    private int syncCount = 0;
    // Intrinsic lock (monitor). Any thread must acquire it before entering a
    // "synchronized (lock)" block, so only one thread runs that block at a time.
    private final Object lock = new Object();

    // Third counter for the comparison: no locking needed, updates are atomic by construction.
    private final AtomicInteger atomicCount = new AtomicInteger(0);

    // Same read-sleep-write shape as incrementUnsafe(), but wrapped in a synchronized
    // block so the whole read-modify-write happens as one uninterruptible unit per thread.
    private void incrementSafe(){

        // Block here until the lock is free, then hold it until this block exits.
        synchronized (lock){
            int current = syncCount;
            try{
                Thread.sleep(1);
            }catch (InterruptedException ex){
                Thread.currentThread().interrupt();
            }
            // No other thread could have changed syncCount between the read above and
            // this write, because they were all blocked waiting for the lock.
            syncCount = current  + 1;
        }
        // Lock auto-released as the synchronized block exits (even if an exception is thrown).
    }

    // incrementAndGet() is a single atomic hardware-level operation (CAS loop internally),
    // so there's no read-modify-write window for another thread to land in.
    private void incrementAtomic(){
        atomicCount.incrementAndGet();
    }


    // Runs all three increment strategies back to back so their end results can be
    // compared directly: unsafe should stay broken, the other two should be exact.
    @GetMapping("/fixed")
    public String fixed(){

        // Reset every counter before each run of the comparison.
        unsafeCount = 0;
        syncCount = 0;
        atomicCount.set(0);
        int tasks = 1000;

        // Control group: identical to /race, included here to show it's still broken.
        runOnPool(tasks, this::incrementUnsafe);
        // Lock-protected: every increment is serialized, so no updates get lost.
        runOnPool(tasks, this::incrementSafe);
        // Lock-free but still correct: hardware-atomic increment.
        runOnPool(tasks, this::incrementAtomic);


        return "Expected:            " + tasks + "\n"
                + "unsafe (plain int):  " + unsafeCount + "   <- still broken\n"
                + "synchronized:        " + syncCount + "   <- correct\n"
                + "AtomicInteger:       " + atomicCount.get() + "   <- correct\n";

    }

    // Shared test harness: fires `tasks` copies of `job` at a fresh 50-thread pool and
    // blocks until every one has completed before returning control to the caller.
    private void runOnPool(int tasks, Runnable job){
        ExecutorService pool = Executors.newFixedThreadPool(50);

        List<Future<?>> futures = new ArrayList<>();

        // Submit everything up front so all `tasks` runs overlap in time.
        for(int i = 0; i < tasks; i++){
            Future<?> future = pool.submit(job);
            futures.add(future);
        }

        // Block on each Future in turn; by the time this loop finishes, every task has run.
        for(Future<?> future: futures){
            try{
                future.get();
            }catch (ExecutionException | InterruptedException ex){
                //handle later
            }
        }

        pool.shutdown();
    }

    // Same broken-vs-fixed comparison as /fixed, but for a Map instead of an int:
    // plain HashMap under concurrent writes vs. ConcurrentHashMap.
    @GetMapping("/map")
    public String map() throws ExecutionException {

        // Each of the 5 URLs gets its counter bumped this many times.
        int tasksPerUrl = 200;

        // Not thread-safe: concurrent put()s on the same key can race, same as unsafeCount.
        Map<String, Integer> unsafeMap = new HashMap<>();
        ExecutorService pool1 = Executors.newFixedThreadPool(50);

        List<Future<?>> f1 = new ArrayList<>();

        // One task per (repetition, URL) pair — tasksPerUrl * URLS.size() tasks total.
        for(int i = 0; i < tasksPerUrl; i++){
            for(String url: URLS){
                Future<?> future = pool1.submit(() -> {
                    // Read-then-put: two threads can read the same current value for a
                    // key before either writes back, so one increment is silently lost.
                    unsafeMap.put(url, unsafeMap.getOrDefault(url, 0) + 1);
                });
                f1.add(future);

            }
        }

        // Wait for every unsafeMap writer to finish before moving on.
        for(Future<?> future: f1){
            try{
                future.get();
            }catch (InterruptedException e){
                //handle later
            }
        }

        pool1.shutdown();


        // Thread-safe: merge() performs its read-modify-write atomically per key.
        Map<String, Integer> safeMap = new ConcurrentHashMap<>();

        ExecutorService pool2 = Executors.newFixedThreadPool(50);

        List<Future<?>> f2 = new ArrayList<>();

        // Identical workload to the unsafeMap run above, writing into safeMap instead.
        for(int i = 0; i < tasksPerUrl; i++){
            for(String url: URLS){
                Future<?> future = pool2.submit(() -> {
                    // merge() is atomic per key, so no other thread can interleave
                    // between reading the current sum and writing the updated one.
                    safeMap.merge(url, 1, Integer::sum);
                });
                f2.add(future);
            }
        }

        // Wait for every safeMap writer to finish before reading final totals.
        for(Future<?> future: f2){
            try{
                future.get();
            }catch (InterruptedException ex){
                //later
            }
        }

        pool2.shutdown();

        // unsafeMap counts typically land short of tasksPerUrl per URL (lost updates);
        // safeMap counts should land exactly on tasksPerUrl for every URL.
        return "Each URL was fetched " + tasksPerUrl + " times. Expected: " + tasksPerUrl + " each.\n\n"
                + "HashMap (broken):\n" + unsafeMap + "\n\n"
                + "ConcurrentHashMap (correct):\n" + safeMap + "\n";


    }

    // Two independent locks. Deadlock happens not because locks are held, but
    // because different threads acquire them in different orders.
    private final Object lockA = new Object();
    private final Object lockB = new Object();


    // Classic deadlock: t1 grabs A then waits for B, while t2 grabs B then waits
    // for A. Neither can proceed because each is holding what the other needs.
    @GetMapping("/deadlock")
    public String deadlock() throws ExecutionException, InterruptedException {

        // Acquires A first, sleeps (giving t2 time to grab B), then tries for B.
        Thread t1 = new Thread(() -> {
            synchronized (lockA){
                sleepQuality(100);
                synchronized (lockB){
                    // never reach
                }
            }
        }, "thread-1");

        // Acquires B first, sleeps, then tries for A — the opposite order to t1.
        Thread t2 = new Thread(() -> {
            synchronized (lockB){
                sleepQuality(100);
                synchronized (lockA){
                    //never reach
                }
            }
        }, "thread-2");

        t1.start();
        t2.start();

        // join(3000) waits up to 3s for each thread to finish rather than blocking
        // forever, so this endpoint can still return a response instead of hanging.
        t1.join(3000);
        t2.join(3000);

        // If either thread is still running after the timeout, it's stuck waiting
        // on a lock the other thread holds — that's the deadlock.
        boolean stuck = t1.isAlive() || t2.isAlive();

        return stuck
                ? "DEADLOCK: both threads still frozen after 3s. In real life this hangs forever.\n"
                : "Completed (no deadlock this run).\n";
    }

    // Same scenario as /deadlock, but both threads acquire the locks in the same
    // order (A then B). With a consistent lock order, one thread always gets both
    // locks and finishes before the other even starts waiting — no cycle, no deadlock.
    @GetMapping("/deadlock-fixed")
    public String deadlockFixed() throws InterruptedException{

        Thread t1 = new Thread(() -> {
            synchronized (lockA){
                sleepQuality(100);
                synchronized (lockB){

                }
            }
        }, "thread-1");

        // Note: t2 also takes A before B, unlike the broken version above.
        Thread t2 = new Thread(() -> {
            synchronized (lockA){
                sleepQuality(100);
                synchronized (lockB){

                }
            }
        }, "thread-2");


        t1.start();
        t2.start();

        t1.join(3000);
        t2.join(3000);

        boolean stuck = t1.isAlive() || t2.isAlive();

        return stuck
                ? "Still stuck (unexpected).\n"
                : "Completed cleanly — same lock order means no deadlock.\n";
    }

    // Thread.sleep() wrapped so callers don't need their own try/catch boilerplate.
    private void sleepQuality(long ms){
        try{
            Thread.sleep(ms);
        }catch (InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }







}
