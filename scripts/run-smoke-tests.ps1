# Test Execution Scripts

## Run Smoke Tests (5-10 minutes)
Write-Host "Running Smoke Tests..." -ForegroundColor Yellow
mvn test -Dgroups="smoke"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ SMOKE TESTS PASSED - System is stable for deployment" -ForegroundColor Green
} else {
    Write-Host "`n❌ SMOKE TESTS FAILED - Critical issues detected!" -ForegroundColor Red
    exit 1
}
