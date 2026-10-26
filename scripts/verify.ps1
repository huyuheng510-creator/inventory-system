# Builds the Project 2 system, runs the JUnit 4 test suite and verifies the
# self-contained runnable jar produced by the Maven package phase.
#
# Usage (from anywhere):
#   powershell -ExecutionPolicy Bypass -File scripts\verify.ps1
#
# Requirements: JDK 17+ and Maven 3.8+. Maven downloads the Derby and JUnit
# dependencies from Maven Central on the first run (see pom.xml).
$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$expectedJar = Join-Path $project "target\inventory-management-system-2.0.0.jar"

Push-Location $project
try {
    Write-Host "== Project 2 verification: mvn clean verify =="
    mvn clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    if (-not (Test-Path -LiteralPath $expectedJar)) {
        throw "Expected runnable jar was not produced: $expectedJar"
    }
    $jar = Get-Item -LiteralPath $expectedJar
    Write-Host ("Runnable jar : {0} ({1:N0} bytes)" -f $jar.FullName, $jar.Length)
    Write-Host "Test reports : target\surefire-reports\"
    Write-Host ""
    Write-Host "SUCCESS: the database-backed Swing system builds, all JUnit 4 tests pass,"
    Write-Host "and the self-contained jar (Derby included) is ready to run:"
    Write-Host ("    java -jar `"{0}`"" -f $jar.FullName)
}
finally {
    Pop-Location
}
