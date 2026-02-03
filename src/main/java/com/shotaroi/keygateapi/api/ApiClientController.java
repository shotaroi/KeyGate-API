package com.shotaroi.keygateapi.api;

import com.shotaroi.keygateapi.security.ApiKeyHasher;
import com.shotaroi.keygateapi.security.ApiKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ApiClientController {

    private final ApiClientRepository repo;
    private final ApiKeyService keyService;
    private final ApiKeyHasher hasher;

    public ApiClientController(ApiClientRepository repo, ApiKeyService keyService, ApiKeyHasher hasher) {
        this.repo = repo;
        this.keyService = keyService;
        this.hasher = hasher;
    }

    public record CreateClientRequest(
            @NotBlank String name,
            @Min(1) @Max(300) int requestsPerMinute
    ) {}

    public record CreateClientResponse(
            Long id,
            String name,
            int requestsPerMinute,
            String apiKey // show only once
    ) {}

    @PostMapping
    public CreateClientResponse create(@Valid @RequestBody CreateClientRequest req) {
        String rawKey = keyService.generateRawKey();
        String hash = hasher.sha256(rawKey);

//        ApiClient saved = repo.save(ApiClient.builder()
//                .name(req.name())
//                .requestsPerMinute(req.requestsPerMinute())
//                .apiKeyHash(hash)
//                .build());
        ApiClient client = new ApiClient();
        client.setName(req.name());
        client.setRequestsPerMinute(req.requestsPerMinute());
        client.setApiKeyHash(hash);

        ApiClient saved = repo.save(client);


        return new CreateClientResponse(saved.getId(), saved.getName(), saved.getRequestsPerMinute(), rawKey);
    }
}
