# Resets the runtime data directory so the next run starts from the demo seed.
# Deletes the embedded Derby database (data\inventorydb) and exported CSV reports
# (data\exports). The next application start recreates and seeds both.
# Usage (from anywhere): powershell -ExecutionPolicy Bypass -File scripts\reset-data.ps1
$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$data = Join-Path $project "data"
if (Test-Path $data) {
    Remove-Item -LiteralPath $data -Recurse -Force
    Write-Host "Deleted data directory: $data"
} else {
    Write-Host "No data directory to delete: $data"
}
Write-Host "The next application start will recreate data\inventorydb and seed the demo dataset."
