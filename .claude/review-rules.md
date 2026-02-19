# TeamHub API — Code Review Rules

These rules augment CLAUDE.md with review-specific guidance. Apply these checks
to every pull request in addition to the general conventions.

---

## Handler Layer

- **Validation before delegation**: Every public endpoint must call
  `ValidationHelper` to validate required fields before delegating to the
  manager. Missing validation is a high-severity bug.
- **Correct HTTP status codes**: `201 Created` for POST, `200 OK` for GET/PATCH,
  `204 No Content` for DELETE. Returning `200` for creates is wrong.
- **Organization isolation**: Every handler must extract `organizationId` from
  the JWT context (via `AuthHandler`) and pass it to the manager. Never trust
  the request body for org scoping.
- **No business logic in handlers**: Handlers parse requests and serialize
  responses only. Any conditional logic, data transformation, or validation
  beyond input parsing belongs in the manager layer.
- **Error propagation**: Handlers must call `ctx.fail(exception)` or let
  `Future.onFailure(ctx::fail)` propagate errors to `ErrorHandler`. Do not
  catch exceptions silently in handlers.

## Manager Layer

- **Always return `Future<T>`**: Manager methods must be non-blocking. Never
  return `null` — use `Future.failedFuture(new AppException(...))` for error
  conditions.
- **Use `AppException` with `ErrorCode`**: Business rule violations must throw
  `AppException` with the appropriate `ErrorCode` enum value. Never throw raw
  exceptions or return generic 500 errors for known error cases.
- **Compose chains must handle failures**: Every `Future.compose()` chain must
  have an `.onFailure()` handler or propagate via `ctx::fail`. Silent failures
  in compose chains are high-severity bugs.
- **Billing and permission checks first**: When an operation is gated by billing
  plan or role, those checks must happen before any data is read or written.
- **`Future.join()` vs `Future.all()`**: Use `Future.all()` when the entire
  operation should fail if any sub-operation fails. Use `Future.join()` when
  partial success is acceptable and you need all results regardless.

## Repository Layer

- **Soft-delete filter**: All queries that retrieve user-facing data must go
  through the base repository methods (`findAll`, `findById`, `count`) which
  apply the soft-delete filter automatically. Custom aggregation queries must
  explicitly add the `deleted: false` (or equivalent `withNotDeleted()`) filter.
- **`JsonObject` for queries**: Use `JsonObject` query builders for all MongoDB
  queries. String concatenation for query construction is not acceptable.
- **Field projection**: Use field projection to limit returned fields on queries
  that return large documents or lists. Do not return full documents when only
  a subset of fields is needed.
- **Index awareness**: New queries on fields not covered by an existing index
  should be flagged — they may cause full collection scans.

## Security

- **JWT claims are authoritative**: Always derive `userId` and `organizationId`
  from JWT claims, never from request body or query parameters.
- **Cross-organization access**: Every data operation must filter by
  `organizationId` from JWT. Missing this filter allows any authenticated user
  to access other organizations' data.
- **Input sanitization**: User-supplied strings stored in MongoDB must not
  contain MongoDB operator keys (keys starting with `$`). Use `ValidationHelper`
  to strip or reject such input.
- **Sensitive data in logs**: Tokens, passwords, API keys, and PII must never
  appear in log statements. Flag any `logger.info/debug/error` that includes
  sensitive field values.

## Async / Vert.x

- **No blocking on the event loop**: `Thread.sleep()`, synchronous file I/O,
  synchronous DB calls, and heavy CPU work must not run on the event loop. Use
  `vertx.executeBlocking()` only when truly necessary and document why.
- **Unhandled promise rejections**: Every `Future` that is not returned to a
  caller must have `.onFailure()` attached. Unhandled failures are silent.

## Testing

- **Test both success and failure paths**: Every manager method should have at
  least one test for the happy path and one for each distinct error condition
  (e.g., billing limit exceeded, resource not found, unauthorized).
- **Mock at repository boundary**: Manager tests should mock repositories with
  Mockito. Tests must not make real network or database calls.
- **Use `ScenarioSetup`**: Use the `scenario()` builder from `TestBase` for
  test data setup. Inline hardcoded `JsonObject` construction in tests makes
  tests fragile.
- **`ctx.succeeding()` / `ctx.failing()`**: Async test assertions must use
  Vert.x test context callbacks. Plain `assertEquals` outside a `ctx.verify()`
  block will not fail the test correctly on assertion errors.

## False Positives to Avoid

- Pre-existing code not modified in the PR is out of scope.
- Vert.x's async style may look unusual — do not flag idiomatic `Future.compose`
  chains as complex or "hard to read."
- `@SuppressWarnings` annotations with a documented reason are intentional.
- Test-only constants and builders (`TestBase`, `ScenarioSetup`) are not
  production code and have different standards.
