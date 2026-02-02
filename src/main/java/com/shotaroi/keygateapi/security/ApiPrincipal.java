package com.shotaroi.keygateapi.security;

public record ApiPrincipal(
        String name,
        String apiKeyHash,
        int requestsPerMinute
) {}
