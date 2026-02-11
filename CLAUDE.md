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
