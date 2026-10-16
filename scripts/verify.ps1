$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    if (-not $env:JAVA_HOME) {
        $jdkPath = 'C:\Program Files\Android\Android Studio\jbr'
        if (Test-Path -LiteralPath "$jdkPath\bin\java.exe") { $env:JAVA_HOME = $jdkPath }
    }
    python data_pipeline/scripts/validate_seed.py
    if ($LASTEXITCODE -ne 0) { throw 'Seed validation failed.' }
    python -m unittest discover -s data_pipeline/validation -v
    if ($LASTEXITCODE -ne 0) { throw 'Pipeline tests failed.' }
    & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon --max-workers=1
    if ($LASTEXITCODE -ne 0) { throw 'Android verification failed.' }
} finally { Pop-Location }
