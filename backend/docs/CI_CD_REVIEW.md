# 📋 CI/CD Pipeline Review - Ready for Commit

## ✅ Pipeline Overview

```
┌─────────────────────────────────────────────────────────────┐
│                 OPTIMIZED CI/CD PIPELINE                     │
└─────────────────────────────────────────────────────────────┘

Step 1: detect-changes (ALWAYS)
├─ Analyze Git changes
└─ Output: test-classes, test-tags, has-changes

Step 2: smoke-tests (ALWAYS) 
├─ Depends on: detect-changes
├─ Run: mvn test -Psmoke
└─ Goal: Verify environment (DB, Redis, basic functionality)

      ┌──────────────────────┬─────────────────────┐
      │                      │                     │
      ▼                      ▼                     ▼
Step 3a: sanity-tests    Step 3b: build-docker   Step 3c: regression-tests
├─ Depends: smoke        ├─ Depends: smoke       ├─ Depends: smoke
├─ Condition: has-changes├─ Condition: Push      ├─ Condition: Push OR
├─ Run: Dynamic tests    │   to main/develop     │   labeled PR
└─ SKIP if no changes    └─ Build Docker image   └─ mvn test -Pregression

     (PARALLEL EXECUTION)

## 🎯 Test Strategy Integration

| Test Level | When | Command | Purpose |
|------------|------|---------|---------|
| **Sanity (Dynamic)** | Every PR/Push | Selected tests based on changes | Fast feedback on affected code |
| **Smoke** | PR only | `mvn test -Psmoke` | Critical path validation |
| **Regression** | Push OR labeled PR | `mvn test -Pregression` | Full test suite |

## 📊 Workflow Decision Matrix

| Event | Branch | Label | Sanity | Smoke | Regression | Docker |
|-------|--------|-------|--------|-------|------------|--------|
| **Push** | main | - | ✅ Dynamic | ❌ | ✅ Full | ✅ |
| **Push** | develop | - | ✅ Dynamic | ❌ | ✅ Full | ✅ |
| **Push** | feature/** | - | ✅ Dynamic | ❌ | ✅ Full | ❌ |
| **PR** | → main/develop | - | ✅ Dynamic | ✅ | ❌ | ❌ |
| **PR** | → main/develop | run-full-tests | ✅ Dynamic | ✅ | ✅ Full | ❌ |

## ✨ Key Features

### 1. Dynamic Sanity Testing
```yaml
- Detects changed files from Git
- Maps to modules using YAML config
- Includes dependent module tests
- Falls back to smoke tests if no changes
```

### 2. Service Containers
```yaml
PostgreSQL 17:
  - Port: 5432
  - Health checks every 10s
  
Redis 7-alpine:
  - Port: 6379
  - Health checks every 10s
```

### 3. Caching
```yaml
Maven dependencies: ~/.m2
Cache key: OS + pom.xml hash
```

### 4. Artifacts Uploaded
```yaml
- sanity-test-results (Surefire reports)
- smoke-test-results (Surefire reports)
- regression-test-results (Surefire reports)
- docker-image (Only main/develop)
```

## 🚀 Usage Examples

### Example 1: Normal PR Workflow
```bash
# Developer creates PR
git checkout -b feature/update-payment
# ... make changes ...
git push origin feature/update-payment

# GitHub Actions runs:
1. ✅ detect-changes → Finds PaymentController changed
2. ✅ sanity-tests → Runs PaymentIntegrationTest + dependencies
3. ✅ smoke-tests → Quick critical path check
4. ⏭️ regression-tests → SKIPPED (no label)
5. ⏭️ build-docker → SKIPPED (not main/develop)
```

### Example 2: Push to Develop
```bash
git checkout develop
git merge feature/update-payment
git push origin develop

# GitHub Actions runs:
1. ✅ detect-changes → Analyzes merge changes
2. ✅ sanity-tests → Dynamic test selection
3. ⏭️ smoke-tests → SKIPPED (not PR)
4. ✅ regression-tests → FULL SUITE
5. ✅ build-docker → Build and save image
```

### Example 3: PR with Full Tests
```bash
# Add label "run-full-tests" to PR in GitHub UI

# GitHub Actions runs:
1. ✅ detect-changes → Analyzes changes
2. ✅ sanity-tests → Dynamic selection
3. ✅ smoke-tests → Quick check
4. ✅ regression-tests → FULL SUITE (due to label)
5. ⏭️ build-docker → SKIPPED (not main/develop)
```

### Example 4: No Java Changes
```bash
# Only update README.md
git add README.md
git commit -m "docs: update readme"
git push

# GitHub Actions runs:
1. ✅ detect-changes → No Java files changed
2. ✅ sanity-tests → Falls back to SMOKE TESTS
3. ⏭️ smoke-tests → SKIPPED (not PR)
4. ✅ regression-tests → FULL SUITE
5. ⏭️ build-docker → SKIPPED (not main/develop)
```

## 🔧 Configuration Files

### Required Files
```
✅ .github/workflows/ci.yml (This file)
✅ backend/pom.xml (with smoke/sanity/regression profiles)
✅ backend/src/test/resources/test-config/module-test-mapping.yml
✅ backend/src/test/java/.../utils/DynamicSanityTestSelector.java
```

### Test Profiles in pom.xml
```xml
<profiles>
  <profile><id>smoke</id></profile>
  <profile><id>sanity</id></profile>
  <profile><id>regression</id></profile>
</profiles>
```

## 📝 Environment Variables

```yaml
SPRING_PROFILES_ACTIVE: test
POSTGRES_DB: moviebooking_test
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres
```

## 🎨 Status Badge

Add to README.md:
```markdown
![CI Pipeline](https://github.com/Ama2352/MovieBooking/workflows/CI%20Pipeline/badge.svg)
```

## ⚠️ Important Notes

### 1. Parallel Execution
- **DISABLED** in maven-surefire-plugin
- Tests run sequentially to avoid race conditions
- See `pom.xml` line 232-233

### 2. Test Isolation
- Each integration test uses `@Transactional`
- Database state rolls back after each test
- Redis cache is isolated per test

### 3. Service Dependencies
- PostgreSQL and Redis are required
- Services have health checks
- Tests wait for services to be ready

### 4. Timeouts
- Job timeout: 30 minutes (default)
- Service health check: 5s timeout, 5 retries
- Test timeout: Configured per test

## 🐛 Troubleshooting

### Issue: "No tests selected"
**Fix:** Check module-test-mapping.yml patterns

### Issue: "Build failed"
**Fix:** Check compilation errors before running tests

### Issue: "Service unhealthy"
**Fix:** Increase health check timeout/retries

### Issue: "Tests passed locally but fail in CI"
**Fix:** 
- Check environment variables
- Verify service versions match
- Review parallel execution settings

## ✅ Pre-Commit Checklist

Before pushing:
- [ ] All tests pass locally: `mvn test`
- [ ] Smoke tests pass: `mvn test -Psmoke`
- [ ] Dynamic sanity works: `.\scripts\run-dynamic-sanity.ps1`
- [ ] Module mapping is up-to-date
- [ ] No compilation errors
- [ ] Docker builds (if applicable)

## 🚦 Ready to Push!

Your CI/CD pipeline is configured with:
✅ Dynamic sanity testing
✅ Smoke tests for PRs
✅ Full regression on push/labeled PR
✅ Docker build for main/develop
✅ Proper service containers
✅ Artifact uploads
✅ Test reporting

Run these commands:
```bash
git add .
git commit -m "ci: implement dynamic sanity testing with smoke/regression strategy"
git push origin integrateSystemTests/TQuang
```

Then create a PR to see the pipeline in action!
