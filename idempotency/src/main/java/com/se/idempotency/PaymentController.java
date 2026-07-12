package com.se.idempotency;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {


    private final PaymentService paymentService;

    @PostMapping()
    public ResponseEntity<Payment> pay(@RequestBody PaymentRequest paymentRequest,
                                       @RequestHeader("idempotency_key") String idempotencyKey){

        Payment payment = paymentService.pay(paymentRequest.toAccount(), paymentRequest.amountCents(), idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }


    public record PaymentRequest(String toAccount, long amountCents) {
    }
}
