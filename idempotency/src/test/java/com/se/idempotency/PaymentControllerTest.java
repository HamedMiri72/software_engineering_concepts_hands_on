package com.se.idempotency;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    PaymentRepository paymentRepository;


//    @Test
//    void sendingTheSamePaymentTwice_createsTwoPayments(){
//        String toAccount = "acct_" + System.nanoTime();
//        var request = new PaymentController.PaymentRequest(toAccount, 500);
//
//        String url = "http://localhost:" + port + "/api/v1/payments";
//
//        http.postForObject(url, request, Payment.class);
//        http.postForObject(url, request, Payment.class);
//
//        long count = paymentRepository.countByToAccount(toAccount);
//
//        assertThat(count).isEqualTo(2);
//    }

    @Test
    void sameIdempotencyKey_createsOnlyOnePayment(){
        String toAccount = "acc_" + System.nanoTime();

        String url = "http://localhost:" + port + "/api/v1/payments";

        String key = UUID.randomUUID().toString();

        var request = new PaymentController.PaymentRequest(toAccount, 500);

        HttpEntity<PaymentController.PaymentRequest> entity = withKey(request, key);

        http.exchange(url, HttpMethod.POST, entity, Payment.class);
        http.exchange(url, HttpMethod.POST, entity, Payment.class);

        long count = paymentRepository.countByToAccount(toAccount);

        // Fixed: only one payment exists, even though we called twice.
        assertThat(count).isEqualTo(1);


    }

    private HttpEntity<PaymentController.PaymentRequest> withKey(PaymentController.PaymentRequest request, String key) {

        HttpHeaders headers = new HttpHeaders();

        headers.set("Idempotency_key", key);

        return new HttpEntity<>(request, headers);
    }

    @Test
    void sameKeyFiredSimultaneously_shouldCreateOnlyOnePayment() throws InterruptedException{

        String account = "acc_" + System.nanoTime();

        String url = "http://localhost:" + port + "/api/v1/payments";

        String key = UUID.randomUUID().toString();

        var request = new PaymentController.PaymentRequest(account, 500);

        HttpEntity<PaymentController.PaymentRequest> entity = withKey(request, key);

        // A "starting gate": both threads wait here until we open it, so they
        // fire as close to the exact same instant as we can arrange.

        CountDownLatch startGate = new CountDownLatch(1);

        CountDownLatch finishLine = new CountDownLatch(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        Runnable fireOneRequest = () -> {
            try {
                startGate.await();                 // wait at the gate
                http.exchange(url, HttpMethod.POST, entity, Payment.class);
            } catch (Exception ignored) {
                // a request may fail under the race — that's fine, we count DB rows below
            } finally {
                finishLine.countDown();            // I'm done
            }
        };

        pool.submit(fireOneRequest);
        pool.submit(fireOneRequest);

        startGate.countDown();                     // OPEN THE GATE — both fire together
        finishLine.await(10, TimeUnit.SECONDS);    // wait for both to finish
        pool.shutdown();

        long count = paymentRepository.countByToAccount(account);

        assertThat(count).isEqualTo(1);





    }


}
