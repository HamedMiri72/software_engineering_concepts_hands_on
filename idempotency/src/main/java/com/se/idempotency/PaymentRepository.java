package com.se.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByToAccount(String toAccount);
}
