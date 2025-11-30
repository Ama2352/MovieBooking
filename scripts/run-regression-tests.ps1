# Test Execution Scripts

## Run Regression Tests (30-60 minutes)
Write-Host "Running Regression Tests..." -ForegroundColor Yellow
mvn test -Dgroups="regression"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ REGRESSION TESTS PASSED - All functionality validated" -ForegroundColor Green
} else {
    Write-Host "`n❌ REGRESSION TESTS FAILED - Existing functionality broken!" -ForegroundColor Red
    exit 1
}
