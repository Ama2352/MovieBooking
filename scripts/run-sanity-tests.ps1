# Test Execution Scripts

## Run Sanity Tests (10-15 minutes)
Write-Host "Running Sanity Tests..." -ForegroundColor Yellow
mvn test -Dgroups="sanity"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ SANITY TESTS PASSED - Feature changes validated" -ForegroundColor Green
} else {
    Write-Host "`n❌ SANITY TESTS FAILED - Issues in modified features!" -ForegroundColor Red
    exit 1
}
