package com.se;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.beans.IntrospectionException;
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

        // A pool of 5 worker threads. We hand it tasks; it runs them on its workers.
        ExecutorService pool = Executors.newFixedThreadPool(5);

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
        StringBuilder out = new StringBuilder();
        for (Future<String> future : futures) {
            out.append(future.get()).append("\n");   // waits for the result
        }

        pool.shutdown(); // stop accepting new tasks; let running ones finish

        long elapsed = System.currentTimeMillis() - start;
        return out + "\nTOTAL TIME: " + elapsed + " ms\n";
    }


}
