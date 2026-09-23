# Release build for Project Coach (Windows — Sprint 32).
# Usage: powershell -File release\run.ps1
# Optional signing: set COACH_KEYSTORE / COACH_KEY_ALIAS / COACH_KEY_PASS then re-run.
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location (Join-Path $Root 'plugin')

if (-not $env:JAVA_HOME) {
    $jdk = Join-Path $env:USERPROFILE 'tools\jdk\jdk-11.0.32+9'
    if (Test-Path $jdk) { $env:JAVA_HOME = $jdk }
}

Write-Host "== Coach release build (JAVA_HOME=$env:JAVA_HOME) =="
& .\gradlew.bat --no-daemon clean buildRelease @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$rel = Join-Path $Root 'plugin\build\release'
Write-Host ""
Write-Host "Release package: $rel"
Get-ChildItem $rel | Format-Table Name, Length
