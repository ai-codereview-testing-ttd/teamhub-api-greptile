# TeamHub API

## Project Overview
TeamHub API is a RESTful backend service providing async, non-blocking APIs for managing projects, tasks, team members, and organization settings. Built with Java 21 and Vert.x for high-performance reactive operations.

## Tech Stack
- **Language**: Java 21
- **Framework**: Vert.x 4.5 (async, non-blocking, reactive)
- **Database**: MongoDB 7.0
- **Authentication**: JWT (Nimbus JOSE library)
- **Build Tool**: Maven
- **Testing**: JUnit 5 + Mockito

## Project Structure
```
src/main/java/com/teamhub/
  MainVerticle.java          # Application entry point
  config/                    # AppConfig with application constants
  common/                    # Shared utilities (AppException, ErrorCode, MongoRepository)
  models/                    # Domain models (Project, Task, Member, Organization)
  repositories/              # Data access layer (MongoDB queries)
  managers/                  # Business logic layer (validation, orchestration)
  handlers/                  # HTTP layer (request/response handling)
  middleware/                # Auth, error handling, security headers
  routes/                    # API route definitions
  utils/                     # Helpers (pagination, JWT, crypto, validation)
src/test/java/com/teamhub/  # Test suites
```

## Architecture

### Three-Layer Pattern
```
Handler → Manager → Repository → MongoDB
```

- **Handlers**: Parse HTTP requests, call managers, serialize responses
- **Managers**: Implement business logic, validation, orchestration between repositories
- **Repositories**: Execute MongoDB queries via Vert.x MongoClient

### Reactive/Async Patterns
Vert.x uses an event loop model. All I/O operations return `Future<T>` and must be non-blocking:
- Use `Future.compose()` for sequential operations
- Use `Future.all()` / `Future.join()` for parallel operations
- Never block the event loop (no `Thread.sleep()`, synchronous I/O, etc.)

### Authentication Flow
1. Client sends JWT in `Authorization: Bearer <token>` header
2. `AuthHandler` middleware validates JWT signature and expiration
3. Extracts `userId` and `organizationId` from claims
4. Attaches to routing context for downstream handlers

### Error Handling
- Business logic throws `AppException` with specific `ErrorCode`
- `ErrorHandler` catches exceptions and returns appropriate HTTP status
- All errors logged with context (user, org, operation)

## Important Conventions

### Java Code Style
- Google Java Style Guide
- Builder pattern for complex objects
- Immutable models where possible
- Proper null checks (use `Optional` where appropriate)

### Vert.x Best Practices
- Never block the event loop (no synchronous operations)
- Always return `Future<T>` from async methods
- Use `vertx.executeBlocking()` only for truly blocking operations (rare)
- Proper error handling in Future chains (`.onFailure()` or `.compose()` error branches)

### MongoDB Patterns
- Use `JsonObject` for queries and documents
- Proper indexing on frequently queried fields
- Aggregation pipelines for complex queries
- Field-level projection to reduce payload size

### Security Patterns
- JWT signed with HS256 using secret from `AppConfig`
- All endpoints (except health check) require authentication
- Role-based access control via `Member.role` field
- Input validation on all public endpoints

### Testing Approach
- Unit tests for managers and utilities
- Repository tests use in-memory MongoDB (not mocked)
- Handler tests mock manager layer
- Focus on business logic and edge cases

## Code Examples

### Handler Pattern (parse, validate, delegate, respond)

```java
private void createProject(RoutingContext ctx) {
    String userId = ctx.get("userId");
    String organizationId = ctx.get("organizationId");
    JsonObject body = ctx.body().asJsonObject();

    if (body == null) {
        ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
        return;
    }

    ValidationHelper.requireNonBlank(body, "name");
    ValidationHelper.validateLength(body.getString("name"), "name", 1, 200);

    projectManager.createProject(body, userId, organizationId)
            .onSuccess(project -> sendJson(ctx, 201, project.toJson()))
            .onFailure(ctx::fail);
}
```

Reference: `src/main/java/com/teamhub/handlers/ProjectHandler.java`

### Manager Pattern (Future.compose chains, AppException for errors)

```java
public Future<Project> createProject(JsonObject body, String userId, String organizationId) {
    String name = body.getString("name");
    String description = body.getString("description", "");

    return billingManager.getCurrentPlan(organizationId).compose(plan -> {
        return projectRepository.countByOrganization(organizationId).compose(count -> {
            if (count >= plan.getMaxProjects()) {
                return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                        "Project limit reached for current billing plan. Max: " + plan.getMaxProjects()));
            }

            JsonObject projectDoc = new JsonObject()
                    .put("name", name)
                    .put("description", description)
                    .put("organizationId", organizationId)
                    .put("status", Project.Status.ACTIVE.name())
                    .put("memberIds", new JsonArray().add(userId))
                    .put("createdBy", userId);

            return projectRepository.insert(projectDoc).map(id -> {
                projectDoc.put("_id", id);
                return Project.fromJson(projectDoc);
            });
        });
    });
}
```

Reference: `src/main/java/com/teamhub/managers/ProjectManager.java`

### Repository Pattern (extend MongoRepository, JsonObject queries)

```java
public class ProjectRepository extends MongoRepository {

    public ProjectRepository(MongoClient mongoClient) {
        super(mongoClient, "projects");
    }

    public Future<List<JsonObject>> findByOrganization(String organizationId, int skip, int limit) {
        JsonObject query = new JsonObject().put("organizationId", organizationId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<Long> countByOrganization(String organizationId) {
        JsonObject query = new JsonObject().put("organizationId", organizationId);
        return count(query);
    }
}
```

Reference: `src/main/java/com/teamhub/repositories/ProjectRepository.java`

Note: `findAll()` and `count()` from `MongoRepository` automatically apply `withNotDeleted()` to filter soft-deleted records.

### Model Pattern (Lombok @Data @Builder, toJson/fromJson)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    public enum Status { ACTIVE, ARCHIVED, COMPLETED }

    private String id;
    private String name;
    private String organizationId;
    private Status status;
    @Builder.Default
    private List<String> memberIds = new ArrayList<>();

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("name", name)
                .put("organizationId", organizationId)
                .put("status", status != null ? status.name() : null)
                .put("memberIds", new JsonArray(memberIds != null ? memberIds : new ArrayList<>()));
    }

    public static Project fromJson(JsonObject json) {
        if (json == null) return null;
        return Project.builder()
                .id(json.getString("_id", json.getString("id")))
                .name(json.getString("name"))
                .organizationId(json.getString("organizationId"))
                .status(json.getString("status") != null ? Status.valueOf(json.getString("status")) : Status.ACTIVE)
                .build();
    }
}
```

Reference: `src/main/java/com/teamhub/models/Project.java`

### Error Handling Pattern (AppException + ErrorCode)

```java
// In managers — throw AppException with specific ErrorCode
return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Project not found"));
return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN, "Access denied to this project"));
return Future.failedFuture(new AppException(ErrorCode.BAD_REQUEST, "Project is already archived"));

// In handlers — validation throws synchronously, caught by ErrorHandler
ValidationHelper.requireNonBlank(body, "name"); // throws AppException(ErrorCode.VALIDATION_ERROR)

// ErrorHandler middleware catches AppException and returns appropriate HTTP status
```

Reference: `src/main/java/com/teamhub/common/AppException.java`, `src/main/java/com/teamhub/common/ErrorCode.java`

### Test Pattern (VertxExtension + Mockito, ScenarioSetup, ctx.succeeding/failing)

```java
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class ProjectManagerTest extends TestBase {

    @Mock private ProjectRepository projectRepository;
    @Mock private BillingManager billingManager;
    private ProjectManager projectManager;

    @BeforeEach
    void setUp() {
        projectManager = new ProjectManager(projectRepository, billingManager, memberManager);
    }

    @Test
    void createProject_success(Vertx vertx, VertxTestContext ctx) {
        ScenarioSetup setup = scenario().withFreePlan();
        BillingPlan plan = BillingPlan.fromJson(setup.getBillingPlan());

        when(billingManager.getCurrentPlan(TEST_ORG_ID)).thenReturn(Future.succeededFuture(plan));
        when(projectRepository.countByOrganization(TEST_ORG_ID)).thenReturn(Future.succeededFuture(0L));
        when(projectRepository.insert(any(JsonObject.class))).thenReturn(Future.succeededFuture("proj-123"));

        JsonObject body = new JsonObject().put("name", "New Project");

        projectManager.createProject(body, TEST_USER_ID, TEST_ORG_ID)
                .onComplete(ctx.succeeding(project -> {
                    ctx.verify(() -> {
                        assertNotNull(project);
                        assertEquals("New Project", project.getName());
                        verify(projectRepository).insert(any(JsonObject.class));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void createProject_limitExceeded(Vertx vertx, VertxTestContext ctx) {
        ScenarioSetup setup = scenario().withBillingPlan("FREE", 5, 3);
        BillingPlan plan = BillingPlan.fromJson(setup.getBillingPlan());

        when(billingManager.getCurrentPlan(TEST_ORG_ID)).thenReturn(Future.succeededFuture(plan));
        when(projectRepository.countByOrganization(TEST_ORG_ID)).thenReturn(Future.succeededFuture(3L));

        projectManager.createProject(body, TEST_USER_ID, TEST_ORG_ID)
                .onComplete(ctx.failing(err -> {
                    ctx.verify(() -> {
                        assertInstanceOf(AppException.class, err);
                        assertEquals(ErrorCode.FORBIDDEN, ((AppException) err).getErrorCode());
                    });
                    ctx.completeNow();
                }));
    }
}
```

Reference: `src/test/java/com/teamhub/managers/ProjectManagerTest.java`

## Common Anti-Patterns

These are patterns that should be flagged during code review:

- **Blocking the Vert.x event loop** — No `Thread.sleep()`, synchronous I/O, or blocking DB calls on the event loop; use `vertx.executeBlocking()` only when unavoidable
- **Raw string concatenation for MongoDB queries** — Use `JsonObject` query builders instead of string concatenation
- **Returning `null` from managers** — Return `Future.failedFuture(new AppException(...))` instead of `null` or `Future.succeededFuture(null)` for error conditions
- **Missing `withNotDeleted()` on custom repository queries** — All repository queries must go through `findAll()`/`count()`/`findById()` (which auto-apply soft-delete filter) or explicitly call `withNotDeleted()`
- **Catching exceptions silently** — Don't swallow exceptions; let them propagate to `ErrorHandler` via `.onFailure(ctx::fail)` or `Future.failedFuture()`
- **Hardcoded strings for error messages** — Use `ErrorCode` enum with `AppException` instead of raw strings or status codes
- **Missing input validation in handlers** — All public endpoints must validate input via `ValidationHelper` before delegating to managers
- **Mutable shared state in handlers** — Handlers must be stateless; use `RoutingContext` for per-request state
- **`Future.all()` when partial failures should be tolerated** — Use `Future.join()` instead when you need all results regardless of individual failures
- **Logging sensitive data** — Never log tokens, passwords, or PII in error/debug messages

## Security Checklist

- All endpoints (except `/health`) must require JWT authentication via `AuthHandler`
- Validate `organizationId` from JWT claims matches requested resource (prevent cross-org access)
- Input validation on all public endpoints using `ValidationHelper`
- Never log tokens, passwords, or PII
- Use `AppException(ErrorCode.FORBIDDEN)` for authorization failures, not generic 500s
- Sanitize user input before storing in MongoDB (prevent NoSQL injection via `$` operators in field values)

## Testing Conventions

- Tests live in `src/test/java/com/teamhub/`
- Naming convention: `{Class}Test.java`
- Structure: `@ExtendWith({VertxExtension.class, MockitoExtension.class})`, extend `TestBase`
- Manager tests mock repositories with Mockito
- Use `ScenarioSetup` builder from `TestBase` for test data (e.g., `scenario().withFreePlan().withProject()`)
- Test both success and error paths using `ctx.succeeding()` and `ctx.failing()`
- Pure utility classes and managers should have tests; handlers tested at integration level
- Reference files: `src/test/java/com/teamhub/managers/ProjectManagerTest.java`, `src/test/java/com/teamhub/TestBase.java`
