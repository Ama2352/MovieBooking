# 🚀 Quick Start: Dynamic Sanity Testing

## ✅ What You Just Got

A smart test selection system that:
- Automatically detects changed files
- Runs only relevant tests
- Includes dependent module tests
- Works in CI/CD and locally

## 🎯 Try It Now!

### 1. Test the Selector (See What Would Run)

**Windows:**
```powershell
cd backend
.\mvnw.cmd exec:java -D"exec.mainClass=com.api.moviebooking.utils.DynamicSanityTestSelector" -D"exec.classpathScope=test" -q
```

**Linux/Mac:**
```bash
cd backend
./mvnw exec:java \
  -Dexec.mainClass="com.api.moviebooking.utils.DynamicSanityTestSelector" \
  -Dexec.classpathScope=test \
  -q
```

### 2. Run Dynamic Sanity Tests

**Windows:**
```powershell
.\scripts\run-dynamic-sanity.ps1
```

**Linux/Mac:**
```bash
chmod +x scripts/run-dynamic-sanity.sh
./scripts/run-dynamic-sanity.sh
```

## 📝 Example Workflow

1. **Make changes to code:**
```bash
# Edit some controller
vim backend/src/main/java/com/api/moviebooking/controllers/PaymentController.java
```

2. **Commit your changes:**
```bash
git add .
git commit -m "fix: update payment validation"
```

3. **Run sanity tests:**
```bash
./scripts/run-dynamic-sanity.ps1  # Windows
# or
./scripts/run-dynamic-sanity.sh   # Linux/Mac
```

4. **See output:**
```
🚀 Dynamic Sanity Test Selector
================================

✅ Loaded 11 module mappings

📝 Changed files:
  - backend/src/main/java/com/api/moviebooking/controllers/PaymentController.java

🔍 Affected modules: [payment]
  ↳ Including dependent module: booking
  ↳ Including dependent module: refund

🧪 Selected test classes (3):
  - com.api.moviebooking.integrations.PaymentIntegrationTest
  - com.api.moviebooking.integrations.BookingIntegrationTest
  - com.api.moviebooking.integrations.RefundIntegrationTest

🏷️ Selected test tags (3):
  - PaymentTests
  - BookingTests
  - RefundTests

🧪 Running tests for classes: PaymentIntegrationTest,BookingIntegrationTest,RefundIntegrationTest
```

## 🎨 Common Scenarios

### Scenario 1: Changed a Controller

```bash
# Changed: CinemaController.java
# Runs: CinemaIntegrationTest + ShowtimeIntegrationTest + SeatIntegrationTest
```

### Scenario 2: Changed a Service

```bash
# Changed: BookingService.java
# Runs: BookingIntegrationTest + PaymentIntegrationTest + ShowtimeSeatIntegrationTest
```

### Scenario 3: Changed Documentation Only

```bash
# Changed: README.md
# Runs: SmokeTest only (fallback)
```

### Scenario 4: Multiple Files

```bash
# Changed: PaymentController.java, RefundService.java
# Runs: Union of all related tests
```

## 🔧 Customize Module Mapping

Edit `backend/src/test/resources/test-config/module-test-mapping.yml`:

```yaml
modules:
  your-new-module:
    description: "Your module description"
    source_patterns:
      - "**/controllers/YourController.java"
      - "**/services/YourService.java"
    test_classes:
      - "com.api.moviebooking.integrations.YourIntegrationTest"
    test_tags:
      - "YourModuleTests"
    dependent_modules:
      - "related-module-1"
```

No code changes needed - just edit the YAML!

## 📊 Verify It Works

**Test with a specific file:**
```powershell
# Windows
cd backend
.\mvnw.cmd exec:java `
  -D"exec.mainClass=com.api.moviebooking.utils.DynamicSanityTestSelector" `
  -D"exec.args=backend/src/main/java/com/api/moviebooking/controllers/PromotionController.java" `
  -D"exec.classpathScope=test" `
  -q

# Should output:
# TEST_CLASSES=PromotionIntegrationTest,BookingIntegrationTest,PaymentIntegrationTest
# TEST_TAGS=PromotionTests,BookingTests,PaymentTests
```

## 🚦 CI/CD Integration

**Already configured!** On every PR or push:
1. GitHub Actions detects changed files
2. Runs relevant sanity tests automatically
3. Falls back to smoke tests if no changes

## 🆘 Troubleshooting

**"No tests selected"**
- Check your file path matches patterns in mapping
- Verify patterns use `**/` for directory matching

**"Compilation failed"**
```bash
cd backend
.\mvnw.cmd clean compile test-compile
```

**"Git command failed"**
- Ensure you're in a Git repository
- Make sure you have commits to compare

## 📚 Learn More

- [Full Documentation](../docs/DYNAMIC_SANITY_TESTING.md)
- [Test Strategy](../backend/src/test/TEST_STRATEGY.md)
- [Maven Config](../backend/MAVEN_TEST_CONFIG.md)

## ✨ Next Steps

1. ✅ Try running `./scripts/run-dynamic-sanity.ps1`
2. ✅ Make a small code change
3. ✅ Run sanity tests again
4. ✅ See how it selects relevant tests
5. ✅ Customize module mapping for your needs

Happy Testing! 🎉
