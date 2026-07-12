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
    private final IdempotencyCache idempotencyCache;

    @Transactional
    public Payment pay(String toAccount, long amountCents, String idempotencyKey){

       if(!idempotencyCache.claim(idempotencyKey)){
           var seen = idempotencyKeyRepository.findByKeyValue(idempotencyKey);
           if(seen.isPresent()){
               return paymentRepository.findById(seen.get().getPaymentId())
                       .orElseThrow(() -> new IllegalStateException(
                               "Idempotency key points to a missing payment"));
           }
       }

        var existing = idempotencyKeyRepository.findByKeyValue(idempotencyKey);
        if (existing.isPresent()) {
            return paymentRepository.findById(existing.get().getPaymentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency key points to a missing payment"));
        }

        Payment payment = Payment.builder()
                .toAccount(toAccount)
                .amountCents(amountCents)
                .build();

        paymentRepository.save(payment);

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                        .paymentId(payment.getId())
                        .keyValue(idempotencyKey)
                .build());

       return payment;
    }

}
