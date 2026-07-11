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

    public Payment pay(String toAccount, long amountCents){
        Payment payment = Payment.builder()
                .toAccount(toAccount)
                .amountCents(amountCents)
                .build();
        paymentRepository.save(payment);

        return payment;
    }

}
