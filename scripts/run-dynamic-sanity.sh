#!/bin/bash
# Dynamic Sanity Test Runner for Local Development

echo "🚀 Dynamic Sanity Test Runner"
echo "=============================="

# Navigate to backend directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../backend" || exit 1

# Compile test classes
echo "📦 Compiling tests..."
./mvnw test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

# Run the selector
echo ""
echo "🔍 Detecting changed modules..."
OUTPUT=$(./mvnw exec:java \
  -Dexec.mainClass="com.api.moviebooking.utils.DynamicSanityTestSelector" \
  -Dexec.classpathScope=test \
  -q 2>&1)

echo "$OUTPUT"

# Extract test classes and tags
TEST_CLASSES=$(echo "$OUTPUT" | grep "^TEST_CLASSES=" | cut -d'=' -f2)
TEST_TAGS=$(echo "$OUTPUT" | grep "^TEST_TAGS=" | cut -d'=' -f2)

# Run tests
echo ""
if [ -n "$TEST_CLASSES" ] && [ "$TEST_CLASSES" != "" ]; then
  echo "🧪 Running tests for classes: $TEST_CLASSES"
  ./mvnw test -Dtest="$TEST_CLASSES" -DfailIfNoTests=false
elif [ -n "$TEST_TAGS" ] && [ "$TEST_TAGS" != "" ]; then
  echo "🏷️ Running tests with tags: $TEST_TAGS"
  ./mvnw test -Dgroups="$TEST_TAGS" -DfailIfNoTests=false
else
  echo "⚠️ No changes detected. Running smoke tests."
  ./mvnw test -Dgroups="SmokeTest" -DfailIfNoTests=false
fi

exit $?
