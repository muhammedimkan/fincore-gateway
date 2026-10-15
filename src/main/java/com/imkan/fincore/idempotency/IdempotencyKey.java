package com.imkan.fincore.idempotency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = {"key_string", "user_id", "request_path"}))
@Getter @Setter
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_string", nullable = false)
    private String keyString;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
