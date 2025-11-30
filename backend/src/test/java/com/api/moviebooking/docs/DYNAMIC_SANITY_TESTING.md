# Dynamic Sanity Testing System

This system automatically selects and runs relevant tests based on code changes, implementing a smart sanity testing strategy.

## 📋 Overview

The Dynamic Sanity Testing system:
- ✅ Automatically detects changed files from Git
- ✅ Maps changes to affected modules using configuration
- ✅ Includes dependent module tests
- ✅ Runs only relevant tests (faster feedback)
- ✅ Falls back to smoke tests if no changes detected

## 🏗️ Architecture

```
┌─────────────────┐
│  Git Changes    │
│  (diff files)   │
└────────┬────────┘
         │
         v
┌─────────────────────────────┐
│  DynamicSanityTestSelector  │
│  (Java utility)             │
└────────┬────────────────────┘
         │
         v
┌─────────────────────────────┐
│  Module Mapping (YAML)      │
│  - Source patterns          │
│  - Test classes             │
│  - Test tags                │
│  - Dependencies             │
└────────┬────────────────────┘
         │
         v
┌─────────────────────────────┐
│  Test Execution             │
│  - By class names           │
│  - By tags                  │
└─────────────────────────────┘
```

## 📁 File Structure

```
backend/
├── src/test/
│   ├── java/com/api/moviebooking/utils/
│   │   └── DynamicSanityTestSelector.java  # Main selector logic
│   └── resources/test-config/
│       └── module-test-mapping.yml         # Module-to-test mapping
scripts/
├── run-dynamic-sanity.sh                   # Linux/Mac script
└── run-dynamic-sanity.ps1                  # Windows script
.github/workflows/
└── ci.yml                                  # CI/CD integration
```

## 🎯 Module Mapping Configuration

Edit `backend/src/test/resources/test-config/module-test-mapping.yml`:

```yaml
modules:
  module-name:
    description: "Module description"
    source_patterns:
      - "**/controllers/SomeController.java"
      - "**/services/SomeService.java"
    test_classes:
      - "com.api.moviebooking.integrations.SomeIntegrationTest"
      - "com.api.moviebooking.services.SomeServiceTest"
    test_tags:
      - "SomeTests"
    dependent_modules:
      - "dependent-module-1"
      - "dependent-module-2"
```

### Pattern Matching Rules

- `**` = Match any number of directories
- `*` = Match anything except directory separator
- Patterns are case-sensitive
- Use forward slashes `/` in patterns

### Example

If you change `PaymentController.java`, the system will:
1. Detect `payment` module is affected
2. Run `PaymentIntegrationTest`
3. Also run tests from dependent modules: `booking`, `refund`

## 🚀 Usage

### Local Development

**Windows:**
```powershell
.\scripts\run-dynamic-sanity.ps1
```

**Linux/Mac:**
```bash
./scripts/run-dynamic-sanity.sh
```

### Manual Test Selection

**Test specific files:**
```bash
cd backend
mvn exec:java \
  -Dexec.mainClass="com.api.moviebooking.utils.DynamicSanityTestSelector" \
  -Dexec.args="src/main/java/com/api/moviebooking/controllers/CinemaController.java"
```

**Run detected tests:**
```bash
# By test classes
mvn test -Dtest="CinemaIntegrationTest,SeatIntegrationTest"

# By test tags
mvn test -Dgroups="CinemaTests,SeatTests"
```

### CI/CD Integration

The system automatically runs in GitHub Actions on:
- Pull requests to `main` or `develop`
- Pushes to `main`, `develop`, or `integrateSystemTests/**`

**Workflow:**
1. `detect-changes` job analyzes Git diff
2. `sanity-tests` job runs selected tests
3. Falls back to smoke tests if no changes

## 📊 Examples

### Example 1: Modify Payment Service

**Changed file:**
```
backend/src/main/java/com/api/moviebooking/services/PaymentService.java
```

**Detected modules:**
- `payment` (direct)
- `booking` (dependent)
- `refund` (dependent)

**Tests executed:**
- `PaymentIntegrationTest`
- `BookingIntegrationTest`
- `RefundIntegrationTest`

### Example 2: Modify Cinema Controller

**Changed file:**
```
backend/src/main/java/com/api/moviebooking/controllers/CinemaController.java
```

**Detected modules:**
- `cinema-management` (direct)
- `showtime-management` (dependent)
- `seat-management` (dependent)

**Tests executed:**
- `CinemaIntegrationTest`
- `CinemaServiceTest`
- `ShowtimeIntegrationTest`
- `SeatIntegrationTest`

### Example 3: No Java Changes

**Changed files:**
```
README.md
docs/API.md
```

**Result:**
- No modules detected
- Falls back to **Smoke Tests** only

## 🔧 Maintenance

### Adding a New Module

1. **Edit mapping file:**
```yaml
modules:
  new-module:
    description: "Description of new module"
    source_patterns:
      - "**/controllers/NewController.java"
      - "**/services/NewService.java"
    test_classes:
      - "com.api.moviebooking.integrations.NewIntegrationTest"
    test_tags:
      - "NewModuleTests"
    dependent_modules: []
```

2. **No code changes needed** - mapping is loaded dynamically

### Adding Dependencies

If module A depends on module B (changes to A should test B):

```yaml
modules:
  module-a:
    # ... other config
    dependent_modules:
      - "module-b"
```

### Debugging

**Enable verbose output:**
```bash
cd backend
mvn exec:java \
  -Dexec.mainClass="com.api.moviebooking.utils.DynamicSanityTestSelector" \
  -Dexec.classpathScope=test
```

**Test pattern matching:**
```bash
# Test a specific file pattern
mvn exec:java \
  -Dexec.mainClass="com.api.moviebooking.utils.DynamicSanityTestSelector" \
  -Dexec.args="backend/src/main/java/com/api/moviebooking/controllers/TestController.java"
```

## 📈 Benefits

✅ **Faster feedback** - Only run relevant tests  
✅ **Better coverage** - Include dependent modules  
✅ **Maintainable** - Configuration in YAML  
✅ **Flexible** - Works with tags or class names  
✅ **CI/CD ready** - Automatic integration  
✅ **Smart fallback** - Smoke tests when unsure

## 🔄 Test Strategy

| Scenario | Tests Run |
|----------|-----------|
| **Code changes detected** | Related tests + dependent modules |
| **No code changes** | Smoke tests only |
| **Multiple modules changed** | Union of all related tests |
| **Config/docs changes** | Smoke tests only |

## 🆘 Troubleshooting

### "Mapping file not found"
- Ensure `backend/src/test/resources/test-config/module-test-mapping.yml` exists
- Check file is included in test resources

### "No tests selected"
- Verify patterns in mapping match your file paths
- Check module names are correct
- Try running selector manually to see output

### "Git command failed"
- Ensure you're in a Git repository
- Check Git is installed and in PATH
- Verify you have commits to compare

## 📝 Notes

- Patterns are evaluated against normalized paths (forward slashes)
- Multiple modules can match the same file
- Dependent modules are followed recursively
- Falls back to smoke tests on any error

## 🔗 Related Documentation

- [Test Strategy](../backend/src/test/TEST_STRATEGY.md)
- [Tagging Guide](../backend/src/test/TAGGING_GUIDE.md)
- [Maven Test Config](../backend/MAVEN_TEST_CONFIG.md)
