🤖 AI Collaboration Protocol

You (Claude) are the Primary Implementer. You are paired with the Gemini CLI, which acts as your High-Context Research Assistant.
When to use Gemini via CLI:

    Massive Context: If you need to analyze patterns across the entire codebase (which exceeds your current context window).

    Needle-in-a-Haystack: When searching for specific logic or variable flows across many directories.

    Architectural Review: When a plan .md needs a "second opinion" based on global project knowledge.

    Usage: Run gemini -p "@src/ [your query]" to get a summary of global patterns.

# prop-manager — Spring Boot API

Property management REST API. Spring Boot 4.0.2, Java 21, stateless JWT auth (Spring Security OAuth2 Resource Server). Feature-based package structure.

> Cross-cutting architecture decisions (IDs, optimistic locking, error contract, auth) are in the root [`../CLAUDE.md`](../CLAUDE.md).

---

## Commands

```bash
# Run (dev profile: H2 in-memory DB, local JWT, H2 console at /h2-console)
mvn spring-boot:run

# Test
mvn test
mvn test -Dtest=PropServiceTest           # Specific class
mvn test -Dtest=PropServiceTest#shouldCreateProp  # Specific method

# Build
mvn clean package
mvn clean package -DskipTests

# Format (Google Java Format via Spotless)
mvn spotless:check
mvn spotless:apply
```

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **H2 Console**: `http://localhost:8080/h2-console` (dev only, JDBC URL: `jdbc:h2:mem:testdb`)

---

## Package Layout

Root package: `com.akandiah.propmanager`

```
config/         # @Configuration classes, @ConfigurationProperties records
security/       # Filters: RateLimitFilter, AuditLoggingFilter
common/
  exception/    # GlobalExceptionHandler, ResourceNotFoundException, HasChildrenException
  util/         # OptimisticLockingUtil, DeleteGuardUtil
  dto/          # PageResponse<T> generic pagination wrapper
features/
  prop/         # Property management (main aggregate)
  unit/         # Rental units within properties
  tenant/       # Tenant management
  lease/        # Lease contracts + state machine + template rendering
  asset/        # Asset tracking
  legal/        # Legal documents and clauses
  user/         # User management
  auth/         # Dev JWT controller (@Profile("dev") only)
  invite/       # Invite/onboarding system
```

### Feature Layout

Every feature follows this exact structure:

```
features/{name}/
  domain/
    {Entity}.java             # JPA entity
    {Entity}Repository.java   # extends JpaRepository<Entity, UUID>
    {EnumName}.java           # Domain enums (if needed)
  service/
    {Entity}Service.java      # Business logic, transactions, returns DTOs
  api/
    {Entity}Controller.java   # REST controller at /api/{plural}
    dto/
      Create{Entity}Request.java
      Update{Entity}Request.java
      {Entity}Response.java
```

---

## Conventions

### Entities

```java
@Entity
@Table(name = "props")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prop extends BaseEntity {

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    // id, version, createdAt, updatedAt — inherited from BaseEntity
}
```

**Rules:**

- All user-facing entities **extend `BaseEntity`** — never declare `@Id`, `@Version`, `createdAt`, or `updatedAt` directly
- Use `@SuperBuilder` — never `@Builder` (which can't see inherited fields)
- Remove `@AllArgsConstructor` — `@SuperBuilder` generates its own constructor chain; coexistence causes a compile error
- Keep `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA requires it
- Exceptions: `Address` (owned value object, never client-addressed by ID) and system-managed entities (`User`, `Invite`) keep their own `@UuidGenerator(style = TIME)` and do not extend `BaseEntity`
- Monetary fields: `BigDecimal` with `@Column(precision=19, scale=4)`
- JSON columns: `@JdbcTypeCode(SqlTypes.JSON)` for maps or complex embedded types
- Large text: `@Lob` for markdown/template content

### Services

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default for all reads
public class PropService {

    private final PropRepository repository;

    public List<PropResponse> findAll() { ... }

    public PropResponse findById(UUID id) {
        return repository.findById(id)
            .map(PropResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Prop", id));
    }

    @Transactional  // Override for writes
    public PropResponse create(CreatePropRequest req) { ... }

    @Transactional
    public PropResponse update(UUID id, UpdatePropRequest req) {
        var existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prop", id));
        // Always verify version before mutating
        OptimisticLockingUtil.requireVersionMatch("Prop", id,
            existing.getVersion(), req.version());
        // ... apply changes, save, return DTO
    }

    @Transactional
    public void deleteById(UUID id) {
        // Guard child records first
        long unitCount = unitRepository.countByPropId(id);
        DeleteGuardUtil.requireNoChildren("Prop", id, unitCount, "units", "delete");
        repository.deleteById(id);
    }
}
```

**Rules:**

- `@Transactional(readOnly=true)` at the class level; `@Transactional` on every write method
- Services return DTOs, **never entities**
- `ResourceNotFoundException("ResourceName", id)` for missing entities → 404
- `OptimisticLockingUtil.requireVersionMatch()` before every update → 409 on mismatch
- `DeleteGuardUtil.requireNoChildren()` before deleting parents → 422 on violation

### DTOs (Records)

```java
// Create request — required fields annotated, no version
public record CreatePropRequest(
    UUID id, // Client-supplied identity (null -> generator fallback)
    @NotBlank(message = "Legal name is required") String legalName,
    @NotNull @Valid AddressInput address,
    @NotNull PropertyType propertyType
) {}

// Update request — all fields optional EXCEPT version
public record UpdatePropRequest(
    @Size(max = 255) String legalName,
    @Valid AddressInput address,
    PropertyType propertyType,
    @NotNull(message = "version is required for optimistic-lock verification") Integer version
) {}

// Response — static factory from entity
public record PropResponse(
    UUID id,
    String legalName,
    AddressView address,
    PropertyType propertyType,
    Integer version,
    Instant createdAt,
    Instant updatedAt
) {
    public static PropResponse from(Prop prop) { ... }
}
```

**Rules:**

- All DTOs are **Java records**
- Create requests: `@NotBlank`/`@NotNull` on required fields
- Update requests: all fields optional; `version` always `@NotNull`
- Response records: static `from(Entity)` factory method
- Nested embedded types use their own records (e.g., `AddressInput`, `AddressView`)

### Controllers

```java
@RestController
@RequestMapping("/api/props")
@RequiredArgsConstructor
@Tag(name = "Props", description = "Property management")
public class PropController {

    private final PropService service;

    @GetMapping
    @Operation(summary = "List all properties")
    public ResponseEntity<List<PropResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a property by ID")
    public ResponseEntity<PropResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a property")
    public ResponseEntity<PropResponse> create(@Valid @RequestBody CreatePropRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a property")
    public ResponseEntity<PropResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a property")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Rules:**

- HTTP methods: `GET` list/get, `POST` create, `@PatchMapping` update (partial), `DELETE` delete
- `@Valid` on every `@RequestBody`
- `@Operation(summary = "...")` on every endpoint (required for Swagger)
- `ResponseEntity.status(CREATED).body(...)` for POST → 201
- `ResponseEntity.ok(...)` for GET/PATCH → 200
- `ResponseEntity.noContent().build()` for DELETE → 204
- Optional filters as `@RequestParam(required = false)`
- **No `@ExceptionHandler` in controllers** — all exceptions handled by `GlobalExceptionHandler`

### Error Handling

`GlobalExceptionHandler` in `common.exception` returns RFC 7807 `ProblemDetail` for all errors:

| Exception                         | Status | When                              |
| --------------------------------- | ------ | --------------------------------- |
| `ResourceNotFoundException`       | 404    | Entity not found by ID            |
| `MethodArgumentNotValidException` | 400    | `@Valid` constraint failure       |
| `OptimisticLockException`         | 409    | `@Version` conflict               |
| `DataIntegrityViolationException` | 409    | DB constraint violation           |
| `HasChildrenException`            | 422    | Can't delete parent with children |
| `AccessDeniedException`           | 403    | Missing role/permission           |
| `AuthenticationException`         | 401    | Invalid/missing JWT               |

Do not add controller-level `@ExceptionHandler`. Extend `GlobalExceptionHandler` for new exception types.

### Security & Authorization

- **Public endpoints**: `/actuator/health`, `/api/public/**`, `/api/dev/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Rate limiting**: Per-IP via `RateLimitFilter` (Resilience4j + Caffeine)
- **Audit logging**: `AuditLoggingFilter` logs all requests

Role-based access:

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable UUID id) { ... }

// Available roles (from JWT groups claim):
// ROLE_ADMIN, ROLE_USER — defined in JwtConfig
```

### Pagination (when needed)

Use `PageResponse<T>` from `common.dto` for paginated endpoints:

```java
// Service
@Transactional(readOnly = true)
public PageResponse<PropResponse> findAll(Pageable pageable) {
    return PageResponse.from(repository.findAll(pageable).map(PropResponse::from));
}

// Controller
@GetMapping
public ResponseEntity<PageResponse<PropResponse>> list(
        @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
    return ResponseEntity.ok(service.findAll(pageable));
}
```

---

## Common Utilities

```java
// Missing entity → 404
throw new ResourceNotFoundException("Prop", id);

// Version check before update → 409 on mismatch
OptimisticLockingUtil.requireVersionMatch("Prop", id, existing.getVersion(), req.version());

// Child guard before delete → 422 if count > 0
DeleteGuardUtil.requireNoChildren("Prop", id, unitCount, "units", "delete");
```

---

## Database

### Dev Profile (default)

- H2 in-memory: `jdbc:h2:mem:testdb`
- `ddl-auto: update` (schema auto-created from entities)
- Liquibase **disabled**

### Prod Profile

- PostgreSQL (configured via env vars)
- Liquibase **enabled** — migrations in `src/main/resources/db/changelog/`
- `ddl-auto: validate`

### Liquibase Migrations

Add a new YAML file in `src/main/resources/db/changelog/changes/` and reference it in `db.changelog-master.yaml`:

```yaml
# changes/002-add-lease-table.yaml
databaseChangeLog:
  - changeSet:
      id: 002-add-lease-table
      author: dev
      changes:
        - createTable:
            tableName: leases
            columns:
              - column:
                  name: id
                  type: uuid
                  constraints:
                    primaryKey: true
              - column:
                  name: version
                  type: int
                  defaultValueNumeric: 0
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: timestamp with time zone
              - column:
                  name: updated_at
                  type: timestamp with time zone
```

**Naming**: `{NNN}-{description}.yaml` (sequential number, kebab-case description).

---

## Configuration Profiles

| Profile         | DB           | JWT                               | CORS               | Liquibase |
| --------------- | ------------ | --------------------------------- | ------------------ | --------- |
| `dev` (default) | H2 in-memory | Local HS256 (DevAuthController)   | localhost:3000     | Disabled  |
| `prod`          | PostgreSQL   | External OIDC (`AUTH_ISSUER_URI`) | Configured domains | Enabled   |

Set `SPRING_PROFILES_ACTIVE=prod` in production.

---

## Complex Feature: Lease State Machine

`LeaseService` is the reference for workflow-based features:

- **State machine**: `LeaseStateMachine` — transitions: `DRAFT → REVIEW → ACTIVE → TERMINATED`
- **Template rendering**: `LeaseTemplateRenderer` stamps Markdown with parameters
- **Workflow endpoints**: `POST /api/leases/{id}/submit`, `/activate`, `/revert`, `/terminate`
- Status guards: only `DRAFT` leases can be edited or deleted

---

## Testing

```
src/test/java mirrors src/main/java package structure.

Available test infrastructure:
- TestSecurityConfig   — mock JwtDecoder (any non-blank token → sub=test-user, groups=[USER])
- TestDataFactory      — builder-based factory for entity defaults
```

### Known pre-existing test failures

These failures exist in the repo independently of any feature work. Do not investigate them unless directly relevant to the task at hand.

| Test                                              | Failure reason                                                                         |
| ------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `PropManagerApplicationTests.contextLoads`        | `JavaMailSender` bean not available in the test context — mail sender not mocked       |
| `LeaseServiceTest.shouldActivateAndStampTemplate` | Mockito interaction on `LeaseTemplateRenderer` — `stampMarkdownFromLease` never called |

### Unit Test (Service)

```java
@ExtendWith(MockitoExtension.class)
class PropServiceTest {

    @Mock PropRepository repository;
    @InjectMocks PropService service;

    @Test
    void shouldCreateProp() {
        var req = new CreatePropRequest("Acme House", address, PropertyType.RESIDENTIAL);
        var saved = TestDataFactory.prop().legalName("Acme House").build();
        when(repository.save(any())).thenReturn(saved);

        var result = service.create(req);

        var captor = ArgumentCaptor.forClass(Prop.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLegalName()).isEqualTo("Acme House");
        assertThat(result.legalName()).isEqualTo("Acme House");
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowOnVersionMismatch() {
        var prop = TestDataFactory.prop().version(1).build();
        when(repository.findById(prop.getId())).thenReturn(Optional.of(prop));
        var req = new UpdatePropRequest(null, null, null, 0); // stale version

        assertThatThrownBy(() -> service.update(prop.getId(), req))
            .isInstanceOf(OptimisticLockException.class);
    }
}
```

### Controller Tests

**Do NOT write controller tests with `@WebMvcTest` — it is not used in this project and will fail to compile.**

Controller `@PreAuthorize` rules are covered at the service layer via `@ExtendWith(MockitoExtension.class)` unit tests. There are no controller slice tests in this codebase. Do not create them.

---

## Adding a New Feature — Checklist

1. **Package**: Create `features.{name}` package
2. **Entity**: `domain/{Entity}.java` — extend `BaseEntity`; `@SuperBuilder`; remove redundant fields and constructors
3. **Repository**: `domain/{Entity}Repository.java` — `extends JpaRepository<Entity, UUID>`
4. **DTOs**: `api/dto/` — `Create{Name}Request`, `Update{Name}Request` (with `version`), `{Name}Response` (with `from(Entity)`)
5. **Service**: `service/{Entity}Service.java` — `findAll`, `findById`, `create`, `update`, `deleteById`; use `ResourceNotFoundException`, `OptimisticLockingUtil`, `DeleteGuardUtil`
6. **Controller**: `api/{Entity}Controller.java` — `@Tag`, `@Operation` on each method, `@Valid` on all request bodies, `@PatchMapping` for updates
7. **Liquibase**: Add `db/changelog/changes/{NNN}-add-{name}-table.yaml` for prod schema; reference in `db.changelog-master.yaml`
8. **Tests**: Mirror package structure in `src/test/java`; unit test service with `@ExtendWith(MockitoExtension.class)`. Do **not** write controller tests — `@WebMvcTest` is not used in this project.
