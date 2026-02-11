# TeamHub API

A RESTful backend service for the TeamHub project management platform, built with Java 21 and Vert.x. Provides async, non-blocking APIs for managing projects, tasks, team members, and organization settings.

## Tech Stack

- **Language**: Java 21
- **Framework**: Vert.x 4.5 (async, non-blocking)
- **Database**: MongoDB 7.0
- **Auth**: JWT (Nimbus JOSE)
- **Build**: Maven
- **Testing**: JUnit 5 + Mockito

## Architecture

```
Handler → Manager → Repository → MongoDB
```

- **Handlers**: HTTP layer — parse requests, send responses
- **Managers**: Business logic — validation, orchestration
- **Repositories**: Data access — MongoDB queries via Vert.x MongoClient

## Project Structure

```
src/main/java/com/teamhub/
  MainVerticle.java          # Entry point, wires everything together
  config/                    # AppConfig (application constants)
  common/                    # AppException, ErrorCode, MongoRepository base
  models/                    # Domain models (Project, Task, Member, Organization, BillingPlan)
  repositories/              # Data access layer
  managers/                  # Business logic layer
  handlers/                  # HTTP handlers
  middleware/                # Auth, error handling, security headers
  routes/                    # API router
  utils/                     # Pagination, JWT, crypto, validation helpers
src/test/java/com/teamhub/  # Test suites
```

## Getting Started

```bash
# Start MongoDB
docker-compose up -d

# Build and test
mvn clean compile
mvn test

# Run the server
mvn exec:java       # Starts on http://localhost:8080
```

## API Endpoints

All routes are mounted at `/api/v1/`:

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/projects` | List/create projects |
| GET/PUT/DELETE | `/projects/:id` | Project CRUD |
| POST | `/projects/:id/archive` | Archive project |
| GET/POST | `/tasks` | List/create tasks |
| GET/PUT/DELETE | `/tasks/:id` | Task CRUD |
| PATCH | `/tasks/:id/status` | Update task status |
| GET | `/members` | List members |
| POST | `/members/invite` | Invite member |
| PUT | `/members/:id/role` | Update member role |
| GET/PUT | `/organizations/:id` | Organization CRUD |
| GET | `/analytics/dashboard` | Dashboard stats |
| GET | `/billing/plan` | Current billing plan |
| GET | `/health` | Health check |

## Configuration

The application uses hardcoded development defaults in `AppConfig`. For production, these should be externalized to environment variables or a config file.

## Code Review with Claude Code

This repository uses Claude Code for AI-powered code reviews.

### Prerequisites
Install Claude Code: https://code.claude.com

### Running a Review

```bash
# Navigate to repo root
cd teamhub-api-claude-code

# Start Claude Code
claude

# Run review on current changes or PR
/review-small        # Quick 2-agent review for small changes
/review-changes      # Comprehensive 7-agent review for complex changes
/re-review          # Verify fixes after initial review
```

### Review Skills

- **`/review-small`**: Streamlined 2-agent review (bug detection + CLAUDE.md compliance)
  - Best for: Simple bug fixes, small features, documentation updates
  - Agents: Bug detector + compliance checker

- **`/review-changes`**: Comprehensive 7-agent review
  - Best for: Complex features, architectural changes, security-sensitive code
  - Agents: CLAUDE.md compliance (2x), bug detection (2x), plan compliance, comment quality, test coverage

- **`/re-review`**: Follow-up verification
  - Best for: After fixing issues from initial review
  - Validates fixes and scans for regressions

See `.claude/skills/README.md` for detailed skill documentation.

### Understanding Review Output

Claude categorizes issues by severity:
- **Critical**: Security vulnerabilities, data loss risks
- **High**: Logic bugs that break functionality, performance bottlenecks
- **Medium**: Maintainability concerns, code quality issues
- **Low**: Style suggestions, minor improvements

See `CLAUDE.md` for project architecture and patterns Claude uses for context.

## Contributing

1. Create a feature branch from `main`
2. Make your changes and ensure tests pass (`mvn test`)
3. Ensure the build compiles cleanly (`mvn clean compile`)
4. Open a pull request
