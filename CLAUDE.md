# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Property management system built with Spring Boot 4.0.2 (Java 21). Stateless JWT-based API with OAuth2 Resource Server for authentication. Uses feature-based architecture with clear separation between domain, service, and API layers.

## Build & Development Commands

```bash
# Build (compile + run tests)
mvn clean package

# Run application (dev profile active by default)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=PropServiceTest

# Run specific test method
mvn test -Dtest=PropServiceTest#shouldCreateProp

# Skip tests during build
mvn clean package -DskipTests

# Format check (if configured)
mvn spotless:check

# Apply formatting (if configured)
mvn spotless:apply
```

## Docker Setup

Full stack includes PostgreSQL, Authentik (OIDC), Caddy (reverse proxy), and the application. See `docker/README.md` for detailed setup.

```bash
# Start full stack (requires .env from .env.example)
docker compose up -d

# View logs
docker compose logs -f app

# Rebuild and restart app container
docker compose up -d --build app

# Stop stack
docker compose down

# Remove volumes (fresh start)
docker compose down -v
```

## Architecture

### Package Structure

- **`config/`** — Spring configuration classes (`SecurityConfig`, `JwtConfig`, `CorsConfig`, etc.) and `@ConfigurationProperties` records
- **`common/`** — Shared utilities and exceptions used across features (`GlobalExceptionHandler`, `ResourceNotFoundException`, pagination utilities)
- **`security/`** — Security filters (`RateLimitFilter`, `AuditLoggingFilter`)
- **`features/`** — Feature modules, each following domain/service/api/dto structure

### Feature Layout

Each feature package (e.g., `features.prop`, `features.lease`, `features.tenant`) contains:

- **`domain/`** — JPA entity and Spring Data `JpaRepository`
- **`service/`** — Application logic, transactions, orchestration. Returns DTOs, not entities
- **`api/`** — REST controller for the feature
- **`api/dto/`** — Request/response records: `CreateXxxRequest`, `UpdateXxxRequest`, `XxxResponse`

### Current Features

- **prop** — Property entities and management
- **unit** — Rental units within properties
- **tenant** — Tenant management
- **lease** — Lease contracts and terms
- **asset** — Asset tracking
- **legal** — Legal documents and clauses
- **user** — User management
- **auth** — Authentication utilities (dev JWT generation via `/api/dev/login` when `@Profile("dev")`)

### Security & Authentication

- **Stateless JWT** using Spring Security OAuth2 Resource Server
- **Dev mode**: Uses `DevAuthController` (`@Profile("dev")`) at `/api/dev/login` to generate JWTs locally with HS256
- **Production**: Expects external OIDC provider (Authentik). Set `AUTH_ISSUER_URI` in environment
- **Rate limiting**: Configurable per-IP limiting via `RateLimitFilter` (using Resilience4j + Caffeine cache)
- **CORS**: Configured via `CorsConfig` and `CorsProperties`
- **Method security**: `@PreAuthorize` available on service/controller methods

### Database

- **Dev**: H2 in-memory (`jdbc:h2:mem:testdb`), Liquibase disabled, Hibernate `ddl-auto: update`
- **Production**: PostgreSQL, Liquibase enabled for migrations
- **Migrations**: Liquibase changelogs in `src/main/resources/db/changelog/`

### Error Handling

All API errors return RFC 7807 ProblemDetail via `GlobalExceptionHandler`:

- `ResourceNotFoundException` → 404 with `{resourceName} with id [{id}] not found`
- Validation errors → 400 with `errors` array of field/message pairs
- Access/auth errors → 401/403

Services throw `ResourceNotFoundException(resourceName, id)` for missing entities.

## Coding Standards (from Cursor rules)

### Naming Conventions

- **Entities**: Singular noun (`Prop`), table name lowercase
- **Repositories**: `{Entity}Repository` extending `JpaRepository`
- **Services**: `{Entity}Service` with methods `findAll`, `findById`, `create`, `update`, `deleteById`
- **Controllers**: `{Entity}Controller` at `/api/{plural-resource}` (e.g., `/api/props`)
- **DTOs**: `Create{Entity}Request`, `Update{Entity}Request`, `{Entity}Response` (with static `from(Entity)`)
- **Config**: `{Concern}Config` and `{Concern}Properties` record

### Spring & Lombok Practices

- **Use `@Slf4j`** for logging (not manual `Logger` fields). Use parameterized messages: `log.info("User {} created", id)`
- **Use `@RequiredArgsConstructor`** for constructor injection (classes with only `final` fields)
- **Prefer records** for DTOs, requests, responses, and `@ConfigurationProperties`
- **Use `@Valid`** on controller request bodies; validation annotations on DTO records
- **ResponseEntity**: Use static factory methods (`ResponseEntity.ok()`, `ResponseEntity.created()`, `ResponseEntity.noContent().build()`)
- **Use `@Profile("dev")`/`@Profile("prod")`** for environment-specific beans, not runtime conditionals

### Error Handling

```java
// In services: throw ResourceNotFoundException for missing entities
Prop prop = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Prop", id));
```

Do not add controller-level `@ExceptionHandler`; keep all exception handling in `GlobalExceptionHandler`.

### New Features

To add a new feature:

1. Create `features.{name}` package
2. Add `domain/` with entity and repository
3. Add `service/` with business logic
4. Add `api/` with controller
5. Add `api/dto/` with request/response records

## Configuration Profiles

- **`dev`** (default): H2 database, CORS enabled for localhost, dev JWT signing, H2 console at `/h2-console`
- **`prod`**: PostgreSQL, external OIDC issuer required, Liquibase migrations enabled

Set `SPRING_PROFILES_ACTIVE=prod` in production environments.

## API Documentation

OpenAPI/Swagger UI available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

Public endpoints (no auth required):
- `/actuator/health`
- `/api/public/**`
- `/api/dev/**` (only in dev profile)
- `/h2-console/**` (only when H2 console enabled)

## Testing

- Tests in `src/test/java` mirror main package structure
- `TestSecurityConfig` provides test security setup
- Use `@SpringBootTest` for integration tests
- Use `@WebMvcTest` for controller tests
- H2 is used for test database
