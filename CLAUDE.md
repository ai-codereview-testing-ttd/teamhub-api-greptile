# TeamHub API

## Project Overview
TeamHub API is the backend service for a project management platform. It manages projects, tasks, team members, and analytics for organizations. Built with Java 21, Vert.x 4.5, and MongoDB.

## Tech Stack
- **Runtime**: Java 21
- **Framework**: Vert.x 4.5 (async, non-blocking, event-loop)
- **Database**: MongoDB via Vert.x MongoClient
- **Auth**: JWT (HS256), validated in `AuthHandler` middleware
- **Testing**: JUnit 5 + Mockito + vertx-junit5
- **Build**: Maven (pom.xml), fat JAR via maven-shade-plugin

## Project Structure
```
src/main/java/com/teamhub/
  config/         # AppConfig (static constants)
  common/         # AppException, ErrorCode, mongo/MongoRepository
  middleware/     # AuthHandler, ErrorHandler, SecurityHeaderHandler
  handlers/       # HTTP layer — one handler class per resource
  managers/       # Business logic layer — one manager per resource
  repositories/   # Data access layer — one repo per collection
  models/         # Domain model classes with fromJson()/toJson()
  utils/          # ValidationHelper, PaginationHelper
  routing/        # ApiRouter — mounts all handlers
  MainVerticle.java
src/test/java/com/teamhub/
  # Manager tests with Mockito-mocked repositories
```

## Architecture: Three-Layer Pattern

Every resource follows the same strict layering:

```
HTTP Request
    ↓
Handler  (parse + validate + delegate)
    ↓
Manager  (business logic, Future<T> chains)
    ↓
Repository  (query building, extends MongoRepository)
    ↓
MongoDB
```

### Handler Pattern

Handlers are thin. They:
1. Extract auth context — `userId` and `organizationId` from `ctx.get(...)` (set by `AuthHandler`)
2. Parse the request body and path/query params
3. Call `ValidationHelper` static methods **synchronously** before any async work
4. Delegate to a single manager method returning `Future<T>`
5. Use `.onSuccess()` to send the response and `.onFailure(ctx::fail)` to route errors to `ErrorHandler`

```java
private void createProject(RoutingContext ctx) {
    String userId = ctx.get("userId");
    String organizationId = ctx.get("organizationId");
    JsonObject body = ctx.body().asJsonObject();

    if (body == null) {
        ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
        return;
    }

    // Validation throws AppException synchronously — Vert.x routes it to ctx.fail() automatically
    ValidationHelper.requireNonBlank(body, "name");
    ValidationHelper.validateLength(body.getString("name"), "name", 1, 200);

    projectManager.createProject(body, userId, organizationId)
            .onSuccess(project -> sendJson(ctx, 201, project.toJson()))
            .onFailure(ctx::fail);
}
```

Routes are registered via a `mount(Router)` method:
```java
public void mount(Router router) {
    router.get("/projects").handler(this::listProjects);
    router.post("/projects").handler(this::createProject);
    router.get("/projects/:id").handler(this::getProject);
    router.put("/projects/:id").handler(this::updateProject);
    router.delete("/projects/:id").handler(this::deleteProject);
}
```

**HTTP status codes**: `201 Created` for POST, `200 OK` for GET/PUT, `204 No Content` for DELETE.

### Manager Pattern

Managers contain all business logic and return `Future<T>` from every public method. Key patterns:

**Billing check before any data operation:**
```java
public Future<Project> createProject(JsonObject body, String userId, String organizationId) {
    return billingManager.getCurrentPlan(organizationId).compose(plan ->
        projectRepository.countByOrganization(organizationId).compose(count -> {
            if (count >= plan.getMaxProjects()) {
                return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                    "Project limit reached. Max: " + plan.getMaxProjects()));
            }
            JsonObject doc = new JsonObject()
                    .put("name", body.getString("name"))
                    .put("organizationId", organizationId)
                    .put("status", Project.Status.ACTIVE.name())
                    .put("createdBy", userId);
            return projectRepository.insert(doc).map(id -> {
                doc.put("_id", id);
                return Project.fromJson(doc);
            });
        })
    );
}
```

**Gatekeeper method — fetch + org authorization in one step:**
```java
public Future<Project> getProject(String projectId, String organizationId) {
    return projectRepository.findById(projectId).compose(doc -> {
        if (doc == null) {
            return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Project not found"));
        }
        Project project = Project.fromJson(doc);
        if (!organizationId.equals(project.getOrganizationId())) {
            return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN, "Access denied"));
        }
        return Future.succeededFuture(project);
    });
}
```

Update methods reuse the gatekeeper then re-fetch after writing:
```java
public Future<Project> updateProject(String id, JsonObject body, String organizationId) {
    return getProject(id, organizationId).compose(existing -> {
        JsonObject update = new JsonObject();
        if (body.containsKey("name")) update.put("name", body.getString("name"));
        return projectRepository.update(id, update)
                .compose(v -> getProject(id, organizationId)); // re-fetch fresh state
    });
}
```

**Rules:**
- Never return `null` from a manager — use `Future.failedFuture(new AppException(...))` for errors
- Use `Future.all()` when all sub-operations must succeed; `Future.join()` when partial success is acceptable
- Soft-delete via `repository.softDelete(id)` — never hard-delete

### Repository Pattern

Repositories extend `MongoRepository`, pass the collection name to `super()`, and build `JsonObject` queries. They return raw `JsonObject` (model conversion happens in the Manager).

```java
public class ProjectRepository extends MongoRepository {

    public ProjectRepository(MongoClient mongoClient) {
        super(mongoClient, "projects");
    }

    public Future<List<JsonObject>> findByOrganization(String orgId, int skip, int limit) {
        JsonObject query = new JsonObject().put("organizationId", orgId);
        JsonObject sort = new JsonObject().put("createdAt", -1); // newest first
        return findAll(query, sort, skip, limit);
    }

    public Future<Long> countByOrganization(String organizationId) {
        return count(new JsonObject().put("organizationId", organizationId));
    }
}
```

**`MongoRepository` base class applies soft-delete automatically** to all `findAll`, `findById`, `count`, and `update` calls by adding `"deletedAt": null` to every query. Custom aggregation queries must add this filter explicitly.

Timestamps are stored as ISO-8601 strings (`Instant.now().toString()`), not BSON dates.

## Error Handling

### AppException + ErrorCode

All known error conditions throw `AppException` with an `ErrorCode`:

```java
// ErrorCode enum → HTTP status
NOT_FOUND        → 404
UNAUTHORIZED     → 401
FORBIDDEN        → 403   (also used for billing limit violations)
BAD_REQUEST      → 400   (missing/malformed body)
CONFLICT         → 409
VALIDATION_ERROR → 422   (field-level validation failures)
INTERNAL_ERROR   → 500
```

Throw via: `new AppException(ErrorCode.NOT_FOUND, "Project not found")`

### Error Response Shape

All errors produce this JSON body:
```json
{
  "error": "NOT_FOUND",
  "message": "Project not found",
  "statusCode": 404
}
```

The `"error"` field is always the `ErrorCode` enum name (e.g., `"VALIDATION_ERROR"`, `"NOT_FOUND"`).

### ValidationHelper

Static utility — call **before** the async chain in handlers. Throws `AppException(VALIDATION_ERROR, ...)` synchronously:

```java
ValidationHelper.requireNonBlank(body, "name");           // throws if null or blank
ValidationHelper.validateLength(value, "name", 1, 200);   // throws if out of range
ValidationHelper.requireFields(body, "name", "status");   // throws if any field missing
ValidationHelper.validateEmail(body, "email");            // throws if invalid format
ValidationHelper.validateObjectId(id, "projectId");       // throws if not 24-char hex
ValidationHelper.validateEnum(value, "status", Project.Status.class);
```

Note: `validateEnum()` passes silently when `value == null` (null = no input, no validation needed). `VALIDATION_ERROR` maps to HTTP 422 (not 400). `BAD_REQUEST` (400) is for structural issues like a missing body.

## Pagination

Handlers use `PaginationHelper` to extract page/pageSize from query params. Managers compose a data fetch + count fetch.

**Paginated response shape:**
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 47,
    "totalPages": 3
  }
}
```

Defaults: `pageSize=20`, max `pageSize=100` (from `AppConfig.DEFAULT_PAGE_SIZE` / `MAX_PAGE_SIZE`).

## Configuration

All config is in `AppConfig` as `public static final` constants — no env var overrides:

```java
public static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
public static final String MONGO_DATABASE = "teamhub";
public static final int SERVER_PORT = 8080;
public static final int MAX_PAGE_SIZE = 100;
public static final int DEFAULT_PAGE_SIZE = 20;
public static final int JWT_EXPIRY_SECONDS = 86400;
```

All routes are mounted under `/api/v1/*`.

## Dependency Injection

No DI framework. All wiring is manual in `MainVerticle.start()`:
1. Create `MongoClient`
2. Create Repositories (pass `mongoClient`)
3. Create Managers (pass repositories + cross-manager dependencies)
4. Create Handlers (pass managers)
5. Set up Router: `SecurityHeaderHandler → CorsHandler → BodyHandler → AuthHandler → failureHandler(ErrorHandler) → ApiRouter`

## Security

- **JWT claims are authoritative**: `userId` and `organizationId` always come from `ctx.get(...)` (set by `AuthHandler`). Never trust the request body for these values.
- **Organization isolation**: Every repository query must include `organizationId`. The gatekeeper pattern (`getProject(id, orgId)`) enforces this for all update/delete/read-single operations.
- **Input sanitization**: User-supplied strings going to MongoDB must not contain `$`-prefixed keys. Use `ValidationHelper` to strip or reject such input.
- **No sensitive data in logs**: Tokens, passwords, API keys must not appear in `logger.info/debug/error`.

## Async Rules (Vert.x)

- **No blocking on the event loop**: No `Thread.sleep()`, synchronous file I/O, synchronous DB calls, or heavy CPU work.
- **Every `Future` that is not returned must have `.onFailure()`**. Unhandled failures are silently swallowed.
- **Thrown exceptions in handlers are caught by Vert.x** and routed to the global `ErrorHandler` automatically — no try/catch needed around `ValidationHelper` calls.

## Testing Conventions

- Manager tests use Mockito-mocked repositories — no real DB or network calls
- Use `ctx.succeeding()` / `ctx.failing()` for async Vert.x test assertions
- Use the `ScenarioSetup` / `scenario()` builder from `TestBase` for test data — no inline `JsonObject` construction
- Every manager method needs at minimum: one happy-path test and one test per distinct error condition (NOT_FOUND, FORBIDDEN, billing limit, etc.)

## Build Commands

```bash
mvn compile -B              # compile
mvn test -B                 # run all tests
mvn verify -B               # compile + test + integration checks
mvn package -DskipTests -B  # build fat JAR (skips tests)
mvn exec:java               # run the app locally (main = com.teamhub.MainVerticle)
mvn clean compile -B        # clean + compile
mvn clean verify -B         # clean + full build + tests
```

## Naming Conventions

- **Handler classes**: `<Resource>Handler` (e.g., `ProjectHandler`, `TaskHandler`)
- **Manager classes**: `<Resource>Manager` (e.g., `ProjectManager`, `MemberManager`)
- **Repository classes**: `<Resource>Repository` (e.g., `ProjectRepository`)
- **Model classes**: Singular noun (e.g., `Project`, `Task`, `Member`)
- **Test classes**: `<ClassName>Test` (e.g., `ProjectManagerTest`)
- **Error messages**: Sentence case, no trailing period (e.g., `"Project not found"`)
- **JSON field names**: camelCase in Java models, camelCase in MongoDB documents
