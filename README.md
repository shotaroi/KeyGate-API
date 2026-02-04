# KeyGate API

KeyGate API is a Spring Boot backend that demonstrates **API key authentication** with **Redis-backed rate limiting**, similar to how real public APIs protect and throttle access.

This project is intentionally built as an **MVP**, focusing on correctness, clarity, and production-style backend patterns.

---

## Features

- API key–based authentication (`X-API-KEY`)
- Secure API key storage (hashed keys only)
- Redis-backed rate limiting (requests per minute)
- `Retry-After` header on rate limit violations
- Rate limit metadata headers on successful responses
- Usage inspection endpoint
- Consistent JSON error responses

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Docker

---

## Prerequisites

- Java 17 or 21
- Maven
- Docker

---

## Local Infrastructure Setup

### Start Redis

## bash
docker run --name keygate-redis -p 6379:6379 -d redis:7

## Start PostgreSQL
docker run --name keygate-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=keygate \
  -p 5432:5432 \
  -d postgres:16

## Running the Application
./mvnw spring-boot:run


## The API will be available at:

http://localhost:8080

## API Usage
1) Create an API Client (generate API key)
curl -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"my-client","requestsPerMinute":5}'


Example response:

{
  "id": 1,
  "name": "my-client",
  "requestsPerMinute": 5,
  "apiKey": "GENERATED_API_KEY"
}


The API key is returned only once. Store it securely.

2) Call a protected endpoint
curl -i http://localhost:8080/hello \
  -H "X-API-KEY: GENERATED_API_KEY"


Expected headers on success:

X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
X-RateLimit-Reset: 52

3) Inspect usage
curl -i http://localhost:8080/usage \
  -H "X-API-KEY: GENERATED_API_KEY"


Example response:

{
  "clientName": "my-client",
  "limitPerMinute": 5,
  "usedThisMinute": 2,
  "remainingThisMinute": 3,
  "resetsInSeconds": 41
}

4) Rate limit exceeded (429)

After exceeding the allowed number of requests per minute:

curl -i http://localhost:8080/hello \
  -H "X-API-KEY: GENERATED_API_KEY"


Expected headers:

HTTP/1.1 429
Retry-After: 17
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 17


Example response body:

{
  "error": "rate_limited",
  "message": "Too many requests",
  "details": {
    "limitPerMinute": 5,
    "usedThisMinute": 6,
    "resetsInSeconds": 17
  }
}

## Error Handling
Missing API key (401)
curl -i http://localhost:8080/hello


Example response:

{
  "error": "unauthorized",
  "message": "Missing API key",
  "path": "/hello"
}

Validation error (400)
curl -i -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"","requestsPerMinute":0}'


Example response:

{
  "error": "validation_error",
  "message": "Validation failed",
  "details": {
    "fieldErrors": {
      "name": "must not be blank",
      "requestsPerMinute": "must be greater than or equal to 1"
    }
  }
}

## Summary

Built a Spring Boot REST API secured with API key authentication and Redis-backed rate limiting, including usage introspection and standardized error handling.
