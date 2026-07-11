package com.se.idempotency;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {


    private final PaymentService paymentService;

    @PostMapping()
    public ResponseEntity<Payment> pay(@RequestBody PaymentRequest paymentRequest){

        Payment payment = paymentService.pay(paymentRequest.toAccount(), paymentRequest.amountCents());

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }


    public record PaymentRequest(String toAccount, long amountCents) {
    }
}
