package com.se;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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









}
