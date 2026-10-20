param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $buildArgs = @('--no-daemon', ':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
    if ($Offline) { $buildArgs = @('--offline') + $buildArgs }
    & .\gradlew.bat @buildArgs
    if ($LASTEXITCODE -ne 0) { throw "Build failed ($LASTEXITCODE)" }
} finally { Pop-Location }
