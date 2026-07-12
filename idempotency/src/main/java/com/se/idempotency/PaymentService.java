package com.se.idempotency;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public Payment pay(String toAccount, long amountCents, String idempotencyKey){

        var existingKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey);

        if(existingKey.isPresent()){
            var originalPaymentId = existingKey.get().getPaymentId();
            return paymentRepository.findById(originalPaymentId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key points to a payment that no longer exists"));
        }

        Payment payment = Payment.builder()
                .amountCents(amountCents)
                .toAccount(toAccount)
                .build();

        paymentRepository.save(payment);
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                        .keyValue(idempotencyKey)
                        .paymentId(payment.getId())
                .build());

        return payment;
    }

}
