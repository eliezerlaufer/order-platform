## What does this PR do?

<!-- One or two sentences describing the change and WHY it's needed. -->

## Type of change

- [ ] `feat` — new feature
- [ ] `fix` — bug fix
- [ ] `refactor` — code improvement (no behavior change)
- [ ] `test` — adding or improving tests
- [ ] `chore` — CI, dependencies, build

## Related issue / ticket

<!-- Closes #123 -->

## Checklist

### Code quality
- [ ] Code compiles without warnings
- [ ] No hardcoded secrets or credentials
- [ ] No unused imports or dead code introduced
- [ ] Error handling covers failure paths

### Tests
- [ ] Unit tests added / updated for changed logic
- [ ] Integration tests pass locally (`./gradlew test`)
- [ ] New Kafka listeners have idempotency tests

### Architecture (microservices)
- [ ] If new Kafka topic: added to `order-platform.md` rules and `docker-compose.yml`
- [ ] If new endpoint: added OpenAPI `@Operation` annotation
- [ ] If new DB table: Flyway migration created (`V{n}__description.sql`)
- [ ] Outbox pattern used for all Kafka publishes (no direct producer in business logic)

### CI
- [ ] CI is green on this branch
- [ ] No test skipped without a documented reason

## Testing evidence

<!-- Paste relevant test output or screenshots -->
```
BUILD SUCCESSFUL
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
```

## Notes for reviewer

<!-- Anything the reviewer should know: tricky logic, design decisions, TODOs left intentionally -->
