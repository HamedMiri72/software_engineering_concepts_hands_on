package com.se.idempotency;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    PaymentRepository paymentRepository;


    @Test
    void sendingTheSamePaymentTwice_createsTwoPayments(){
        String toAccount = "acct_" + System.nanoTime();
        var request = new PaymentController.PaymentRequest(toAccount, 500);

        String url = "http://localhost:" + port + "/api/v1/payments";

        http.postForObject(url, request, Payment.class);
        http.postForObject(url, request, Payment.class);

        long count = paymentRepository.countByToAccount(toAccount);

        assertThat(count).isEqualTo(2);
    }


}
