package com.se.idempotency;


import jakarta.persistence.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
@Table(
        name = "idempotency_key",
        uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_key", columnNames = "key_value")
)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, name = "key_value")
    private String keyValue;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
}
