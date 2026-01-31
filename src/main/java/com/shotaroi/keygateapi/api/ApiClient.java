package com.shotaroi.keygateapi.api;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "api_clients")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the client app ("my-mobile-app", "integration-test", etc.)
    @Column(nullable = false)
    private String name;

    // This is the "secret key" the client will send in requests
    @Column(nullable = false, unique = true, length = 64)
    private String apiKeyHash;

    // how many requests per minute they are allowed
    @Column(nullable = false)
    private int requestsPerMinute;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
