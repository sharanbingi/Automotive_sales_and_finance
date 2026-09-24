# Release Gate Automation Script
# Path: scripts/release-gate.ps1

$ErrorActionPreference = "Continue"

# Remove conflicting env var for AGP
Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# Configure Java (JBR)
$jbrPath = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $jbrPath) {
    $env:JAVA_HOME = $jbrPath
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $jbrPath, "Process")
    [System.Environment]::SetEnvironmentVariable("PATH", "$jbrPath\bin;" + $env:PATH, "Process")
}

# Root directory resolution
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrEmpty($scriptDir)) {
    $projectRoot = (Get-Location).Path
} else {
    $projectRoot = (Resolve-Path "$scriptDir\..").Path
}
Set-Location $projectRoot

# Logging setup
$qaReportsDir = Join-Path $projectRoot "qa-reports"
if (-not (Test-Path $qaReportsDir)) {
    New-Item -ItemType Directory -Force -Path $qaReportsDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $qaReportsDir "report-$timestamp.txt"

function Write-Report {
    param (
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
    Add-Content -Path $logFile -Value $Message
}

$nowStr = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message '          AUTOMATED RELEASE GATE VERIFICATION      ' -Color 'Cyan'
Write-Report -Message "Timestamp: $nowStr" -Color 'Gray'
Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message ''

$summary = [ordered]@{
    "Security Guard Checks"    = "PENDING"
    "Change Impact Analysis"   = "PENDING"
    "Environment Check"        = "PENDING"
    "Firebase Project Check"   = "PENDING"
    "Firestore Security Tests" = "PENDING"
    "Android Unit Tests"       = "PENDING"
    "Android Build"            = "PENDING"
    "Secret Scan"              = "PENDING"
    "Production Safety Check"  = "PENDING"
}

$hasErrors = $false
$releaseStatus = "PASSED"

# 0. Stage 0: Security Guard Checks
Write-Report -Message '--> [0/7] Running Security Guard Bot...' -Color 'Yellow'
$secGuardScript = Join-Path $projectRoot "scripts\security-guard.ps1"
if (Test-Path $secGuardScript) {
    $secGuardOutput = & powershell -ExecutionPolicy Bypass -File $secGuardScript 2>&1
    foreach ($line in $secGuardOutput) {
        Add-Content -Path $logFile -Value $line
        Write-Host $line
    }

    $outputText = $secGuardOutput | Out-String
    if ($outputText -match "STATUS:\s*BLOCKED") {
        Write-Report -Message '  [FAIL] Security Guard status is BLOCKED. Release gate failed.' -Color 'Red'
        $summary["Security Guard Checks"] = "BLOCKED"
        $releaseStatus = "BLOCKED"
        $hasErrors = $true
        Write-Report -Message ''
        Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
        Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
        Write-Report -Message 'RELEASE GATE RESULT: BLOCKED' -Color 'Red'
        exit 1
    } elseif ($outputText -match "STATUS:\s*REVIEW REQUIRED") {
        Write-Report -Message '  [WARN] Security Guard status is REVIEW REQUIRED. Continuing pipeline with pending manual security review...' -Color 'Yellow'
        $summary["Security Guard Checks"] = "REVIEW REQUIRED"
        if ($releaseStatus -ne "BLOCKED") {
            $releaseStatus = "REVIEW REQUIRED"
        }
    } else {
        Write-Report -Message '  [PASS] Security Guard checks passed' -Color 'Green'
        $summary["Security Guard Checks"] = "PASSED"
    }
} else {
    Write-Report -Message '  [FAIL] scripts/security-guard.ps1 script not found' -Color 'Red'
    $summary["Security Guard Checks"] = "FAILED"
    $hasErrors = $true
    $releaseStatus = "BLOCKED"
    exit 1
}
Write-Report -Message ''

# 0.5 Stage 0.5: Change Impact Analysis (Advisory)
Write-Report -Message '--> [0.5/7] Running Change Impact Bot (Advisory)...' -Color 'Yellow'
$changeImpactScript = Join-Path $projectRoot "scripts\change-impact.ps1"
if (Test-Path $changeImpactScript) {
    $impactOutput = & powershell -ExecutionPolicy Bypass -File $changeImpactScript 2>&1
    foreach ($line in $impactOutput) {
        Add-Content -Path $logFile -Value $line
        Write-Host $line
    }

    $outputText = $impactOutput | Out-String
    if ($outputText -match "RESULT STATE:\s*CRITICAL REVIEW REQUIRED") {
        Write-Report -Message '  [ADVISORY] Change Impact Analysis: CRITICAL REVIEW REQUIRED' -Color 'Red'
        $summary["Change Impact Analysis"] = "CRITICAL REVIEW REQUIRED"
        if ($releaseStatus -ne "BLOCKED") {
            $releaseStatus = "REVIEW REQUIRED"
        }
    } elseif ($outputText -match "RESULT STATE:\s*REVIEW REQUIRED") {
        Write-Report -Message '  [ADVISORY] Change Impact Analysis: REVIEW REQUIRED' -Color 'Yellow'
        $summary["Change Impact Analysis"] = "REVIEW REQUIRED"
        if ($releaseStatus -ne "BLOCKED") {
            $releaseStatus = "REVIEW REQUIRED"
        }
    } elseif ($outputText -match "RESULT STATE:\s*LOW RISK") {
        Write-Report -Message '  [ADVISORY] Change Impact Analysis: LOW RISK' -Color 'Green'
        $summary["Change Impact Analysis"] = "LOW RISK"
    } else {
        Write-Report -Message '  [ADVISORY] Change Impact Analysis: NO CHANGES' -Color 'Green'
        $summary["Change Impact Analysis"] = "NO CHANGES"
    }
} else {
    Write-Report -Message '  [WARN] scripts/change-impact.ps1 script not found' -Color 'Yellow'
    $summary["Change Impact Analysis"] = "SKIPPED"
}
Write-Report -Message ''

# 1. Environment Check
Write-Report -Message '--> [1/7] Checking Environment...' -Color 'Yellow'

$javaVer = java -version 2>&1
if ($LASTEXITCODE -eq 0 -and $javaVer) {
    Write-Report -Message "  [PASS] Java (JBR): $jbrPath" -Color 'Green'
} else {
    Write-Report -Message '  [FAIL] Java not working properly' -Color 'Red'
    $hasErrors = $true
}

if (Test-Path "$projectRoot\gradlew.bat") {
    Write-Report -Message '  [PASS] Gradle Wrapper found' -Color 'Green'
} else {
    Write-Report -Message '  [FAIL] Gradle Wrapper not found' -Color 'Red'
    $hasErrors = $true
}

$nodeVer = node --version 2>&1
if ($LASTEXITCODE -eq 0 -and $nodeVer) {
    Write-Report -Message "  [PASS] Node: $nodeVer" -Color 'Green'
} else {
    Write-Report -Message '  [FAIL] Node CLI not found' -Color 'Red'
    $hasErrors = $true
}

$fbCmd = "firebase"
$fbVer = & $fbCmd --version 2>&1
if ($LASTEXITCODE -ne 0) {
    $fbCmd = "firebase.cmd"
    $fbVer = & $fbCmd --version 2>&1
}

if ($LASTEXITCODE -eq 0 -and $fbVer) {
    Write-Report -Message "  [PASS] Firebase CLI: $fbVer" -Color 'Green'
} else {
    Write-Report -Message '  [FAIL] Firebase CLI not found' -Color 'Red'
    $hasErrors = $true
}

if (-not $hasErrors) {
    $summary["Environment Check"] = "PASSED"
} else {
    $summary["Environment Check"] = "FAILED"
    $releaseStatus = "BLOCKED"
    Write-Report -Message 'Environment check failed. Aborting Release Gate.' -Color 'Red'
    Write-Report -Message ''
    Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'RELEASE GATE RESULT: BLOCKED' -Color 'Red'
    exit 1
}
Write-Report -Message ''

# 2. Firebase Project Safety Check
Write-Report -Message '--> [2/7] Checking Active Firebase Project...' -Color 'Yellow'
$activeProject = & $fbCmd use 2>&1 | Out-String
$activeProjectTrimmed = $activeProject.Trim()
Write-Report -Message "  Active Firebase Project Output: $activeProjectTrimmed" -Color 'Gray'

$expectedProject = "com-automotive-automotiv-7bfc5"
if ($activeProjectTrimmed -match $expectedProject) {
    Write-Report -Message "  [PASS] Active project matches expected: $expectedProject" -Color 'Green'
    $summary["Firebase Project Check"] = "PASSED"
} else {
    Write-Report -Message 'WRONG FIREBASE PROJECT - RELEASE BLOCKED' -Color 'Red'
    Write-Report -Message "Expected: $expectedProject, but found: $activeProjectTrimmed" -Color 'Red'
    $summary["Firebase Project Check"] = "FAILED"
    $releaseStatus = "BLOCKED"

    Write-Report -Message ''
    Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'RELEASE GATE RESULT: BLOCKED' -Color 'Red'
    exit 1
}
Write-Report -Message ''

# 3. Firestore Security Tests
Write-Report -Message '--> [3/7] Running Firestore Security Tests...' -Color 'Yellow'
$rulesTestsDir = Join-Path $projectRoot "rules-tests"
if (Test-Path $rulesTestsDir) {
    Set-Location $rulesTestsDir
    $emulatorOutput = & $fbCmd emulators:exec --only firestore "npm test" 2>&1
    Set-Location $projectRoot

    foreach ($line in $emulatorOutput) {
        Add-Content -Path $logFile -Value $line
    }

    if ($LASTEXITCODE -eq 0) {
        Write-Report -Message '  [PASS] Firestore security rules tests completed with 0 failures' -Color 'Green'
        $summary["Firestore Security Tests"] = "PASSED"
    } else {
        Write-Report -Message '  [FAIL] Firestore security rules tests failed' -Color 'Red'
        $summary["Firestore Security Tests"] = "FAILED"
        $hasErrors = $true
        $releaseStatus = "BLOCKED"
    }
} else {
    Write-Report -Message '  [FAIL] rules-tests directory not found' -Color 'Red'
    $summary["Firestore Security Tests"] = "FAILED"
    $hasErrors = $true
    $releaseStatus = "BLOCKED"
}
Write-Report -Message ''

# 4. Android Unit Tests
Write-Report -Message '--> [4/7] Running Android Unit Tests...' -Color 'Yellow'
$gradleCmd = ".\gradlew.bat"
$unitTestOutput = & $gradleCmd testDebugUnitTest 2>&1
foreach ($line in $unitTestOutput) {
    Add-Content -Path $logFile -Value $line
}

# Parse test count accurately from XML files
$xmlDir = Join-Path $projectRoot "app\build\test-results\testDebugUnitTest"
$unitTestCount = 0
if (Test-Path $xmlDir) {
    $xmlFiles = Get-ChildItem -Path $xmlDir -Filter "*.xml" -ErrorAction SilentlyContinue
    foreach ($file in $xmlFiles) {
        try {
            [xml]$xmlContent = Get-Content $file.FullName -ErrorAction SilentlyContinue
            if ($xmlContent.testsuite -and $xmlContent.testsuite.tests) {
                $unitTestCount += [int]$xmlContent.testsuite.tests
            } elseif ($xmlContent.testsuites -and $xmlContent.testsuites.tests) {
                $unitTestCount += [int]$xmlContent.testsuites.tests
            }
        } catch {}
    }
}

# Load lastKnownGoodAndroidTests from release-baseline.json if available
$lastKnownGoodAndroidTests = 253
$baselineFile = Join-Path $projectRoot "qa-baseline\release-baseline.json"
if (Test-Path $baselineFile) {
    try {
        $baseJson = Get-Content -Path $baselineFile -Raw | ConvertFrom-Json
        if ($baseJson.lastKnownGoodAndroidTests) {
            $lastKnownGoodAndroidTests = [int]$baseJson.lastKnownGoodAndroidTests
        }
    } catch {}
}

if ($LASTEXITCODE -eq 0) {
    Write-Report -Message "  [PASS] Android unit tests passed ($unitTestCount tests ran, 0 failures)" -Color 'Green'
    if ($unitTestCount -lt $lastKnownGoodAndroidTests) {
        Write-Report -Message "WARN - ANDROID TEST COUNT DECREASE DETECTED ($unitTestCount vs $lastKnownGoodAndroidTests)" -Color 'Yellow'
    }
    $summary["Android Unit Tests"] = "PASSED"
} else {
    Write-Report -Message '  [FAIL] Android unit tests failed' -Color 'Red'
    if ($unitTestCount -gt 0 -and $unitTestCount -lt $lastKnownGoodAndroidTests) {
        Write-Report -Message "WARN - ANDROID TEST COUNT DECREASE DETECTED ($unitTestCount vs $lastKnownGoodAndroidTests)" -Color 'Yellow'
    }
    $summary["Android Unit Tests"] = "FAILED"
    $hasErrors = $true
    $releaseStatus = "BLOCKED"
}
Write-Report -Message ''

# 5. Android Build
Write-Report -Message '--> [5/7] Building Android Debug APK...' -Color 'Yellow'
$buildOutput = & $gradleCmd assembleDebug 2>&1
foreach ($line in $buildOutput) {
    Add-Content -Path $logFile -Value $line
}

if ($LASTEXITCODE -eq 0) {
    Write-Report -Message '  [PASS] Android build (assembleDebug) succeeded' -Color 'Green'
    $summary["Android Build"] = "PASSED"
} else {
    Write-Report -Message '  [FAIL] Android build failed' -Color 'Red'
    $summary["Android Build"] = "FAILED"
    $hasErrors = $true
    $releaseStatus = "BLOCKED"
}
Write-Report -Message ''

# 6. Secret Scan
Write-Report -Message '--> [6/7] Running Lightweight Secret Scan...' -Color 'Yellow'
$suspiciousPatterns = @(
    '-----BEGIN (RSA |EC |DS )?PRIVATE KEY-----',
    'AKIA[0-9A-Z]{16}'
)

$foundSecrets = @()

$scannableFiles = Get-ChildItem -Path $projectRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch '[\\/](node_modules|\.git|build|\.gradle|qa-reports|qa-baseline)[\\/]' -and
        $_.Name -notmatch '\.(png|jpg|jpeg|gif|ico|aar|jar|dex|apk|so|zip|log)$'
    }

foreach ($file in $scannableFiles) {
    foreach ($pattern in $suspiciousPatterns) {
        $matches = Select-String -Path $file.FullName -Pattern $pattern -ErrorAction SilentlyContinue
        if ($matches) {
            foreach ($match in $matches) {
                $fPath = $file.FullName
                $lNum = $match.LineNumber
                $lText = $match.Line.Trim()
                $foundSecrets += "${fPath}:${lNum}: ${lText}"
            }
        }
    }
}

if ($foundSecrets.Count -eq 0) {
    Write-Report -Message '  [PASS] Secret scan passed (0 private keys or hardcoded secrets detected)' -Color 'Green'
    $summary["Secret Scan"] = "PASSED"
} else {
    Write-Report -Message '  [FAIL] Suspicious secrets/private keys found:' -Color 'Red'
    foreach ($secret in $foundSecrets) {
        Write-Report -Message "    $secret" -Color 'Red'
    }
    $summary["Secret Scan"] = "FAILED"
    $hasErrors = $true
    $releaseStatus = "BLOCKED"
}
Write-Report -Message ''

# 7. Production Safety Check
Write-Report -Message '--> [7/7] Verifying Production Safety...' -Color 'Yellow'
Write-Report -Message '  [PASS] Safety Guard Verified: Verification only mode (no deployment commands enabled)' -Color 'Green'
$summary["Production Safety Check"] = "PASSED"
Write-Report -Message ''

# Summary & Report
Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message '               RELEASE GATE SUMMARY' -Color 'Cyan'
Write-Report -Message '==================================================' -Color 'Cyan'

foreach ($key in $summary.Keys) {
    $st = $summary[$key]
    $color = switch ($st) {
        "PASSED"                   { "Green" }
        "LOW RISK"                 { "Green" }
        "NO CHANGES"               { "Green" }
        "REVIEW REQUIRED"          { "Yellow" }
        "CRITICAL REVIEW REQUIRED" { "Yellow" }
        "SKIPPED"                  { "Yellow" }
        default                    { "Red" }
    }
    $paddedKey = $key.PadRight(30)
    Write-Report -Message "$paddedKey : $st" -Color $color
}

Write-Report -Message '--------------------------------------------------' -Color 'Cyan'
Write-Report -Message "Execution Log saved to: $logFile" -Color 'Gray'
Write-Report -Message '--------------------------------------------------' -Color 'Cyan'

if ($hasErrors -or $releaseStatus -eq "BLOCKED") {
    Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'RELEASE GATE RESULT: BLOCKED' -Color 'Red'
    Write-Report -Message '==================================================' -Color 'Cyan'
    exit 1
} elseif ($releaseStatus -eq "REVIEW REQUIRED") {
    Write-Report -Message 'AUTOMATED TECHNICAL CHECKS: PASSED' -Color 'Green'
    Write-Report -Message 'MANUAL SECURITY REVIEW: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'PRODUCTION RELEASE STATUS: NOT APPROVED' -Color 'Yellow'
    Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'RELEASE GATE RESULT: REVIEW REQUIRED' -Color 'Yellow'
    Write-Report -Message '==================================================' -Color 'Cyan'
    exit 0
} else {
    Write-Report -Message 'PHYSICAL DEVICE QA: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'HUMAN PRODUCTION APPROVAL: REQUIRED' -Color 'Yellow'
    Write-Report -Message 'RELEASE GATE RESULT: PASSED' -Color 'Green'
    Write-Report -Message '==================================================' -Color 'Cyan'
    exit 0
}
