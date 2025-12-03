# Dynamic Sanity Test Runner for Local Development (Windows)

Write-Host "🚀 Dynamic Sanity Test Runner" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

# Navigate to backend directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location "$scriptDir\..\backend"

# Compile test classes
Write-Host "📦 Compiling tests..." -ForegroundColor Yellow
.\mvnw.cmd test-compile -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Compilation failed" -ForegroundColor Red
    exit 1
}

# Run the selector
Write-Host ""
Write-Host "🔍 Detecting changed modules..." -ForegroundColor Yellow
$output = .\mvnw.cmd exec:java `
    -D"exec.mainClass=com.api.moviebooking.utils.DynamicSanityTestSelector" `
    -D"exec.classpathScope=test" `
    -q 2>&1

Write-Host $output

# Extract test classes and tags
$testClasses = ($output | Select-String "^TEST_CLASSES=" | ForEach-Object { $_ -replace "TEST_CLASSES=", "" }).Trim()
$testTags = ($output | Select-String "^TEST_TAGS=" | ForEach-Object { $_ -replace "TEST_TAGS=", "" }).Trim()

# Run tests
Write-Host ""
if ($testClasses -and $testClasses -ne "") {
    Write-Host "🧪 Running tests for classes: $testClasses" -ForegroundColor Green
    .\mvnw.cmd test -D"test=$testClasses" -DfailIfNoTests=false
}
elseif ($testTags -and $testTags -ne "") {
    Write-Host "🏷️ Running tests with tags: $testTags" -ForegroundColor Green
    .\mvnw.cmd test -D"groups=$testTags" -DfailIfNoTests=false
}
else {
    Write-Host "⚠️ No changes detected. Running smoke tests." -ForegroundColor Yellow
    .\mvnw.cmd test -D"groups=SmokeTest" -DfailIfNoTests=false
}

exit $LASTEXITCODE
