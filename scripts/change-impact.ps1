# Change Impact Analysis Script (Bot #3)
# Path: scripts/change-impact.ps1
# Performs read-only Git change impact inspection, risk evaluation, special review flagging,
# and targeted test & QA recommendations, plus baseline management.

param (
    [switch]$CreateBaseline
)

$ErrorActionPreference = "Continue"
$env:GIT_PAGER = "cat"

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
$qaBaselineDir = Join-Path $projectRoot "qa-baseline"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportFile = Join-Path $qaReportsDir "change-impact-$timestamp.txt"

function Write-Report {
    param (
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
    Add-Content -Path $reportFile -Value $Message
}

# Helper function to get Android Unit Test Count from XML files
function Get-AndroidUnitTestCount {
    param ([string]$ProjectRoot)
    $xmlDir = Join-Path $ProjectRoot "app\build\test-results\testDebugUnitTest"
    $count = 0
    if (Test-Path $xmlDir) {
        $xmlFiles = Get-ChildItem -Path $xmlDir -Filter "*.xml" -ErrorAction SilentlyContinue
        foreach ($file in $xmlFiles) {
            try {
                [xml]$xmlContent = Get-Content $file.FullName -ErrorAction SilentlyContinue
                if ($xmlContent.testsuite -and $xmlContent.testsuite.tests) {
                    $count += [int]$xmlContent.testsuite.tests
                } elseif ($xmlContent.testsuites -and $xmlContent.testsuites.tests) {
                    $count += [int]$xmlContent.testsuites.tests
                }
            } catch {}
        }
    }
    return $count
}

# Helper function to get Firestore Test Count from spec file
function Get-FirestoreTestCount {
    param ([string]$ProjectRoot)
    $specFile = Join-Path $ProjectRoot "rules-tests\test\firestore.rules.spec.js"
    $count = 0
    if (Test-Path $specFile) {
        $specContent = Get-Content -Path $specFile -Raw -ErrorAction SilentlyContinue
        if ($specContent) {
            $matches = [regex]::Matches($specContent, "it\('Test \d+")
            $count = $matches.Count
        }
    }
    return $count
}

# Helper function to compute file manifest (SHA-256 hashes of source files)
function Get-ProjectFileManifest {
    param ([string]$ProjectRoot)
    $excludePatterns = @(
        "*\build\*", "*\.gradle\*", "*\qa-reports\*", "*\qa-baseline\*",
        "*\node_modules\*", "*\.git\*", "*\.idea\*"
    )
    $allFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
        $fullPath = $_.FullName
        $skip = $false
        foreach ($p in $excludePatterns) {
            if ($fullPath -like $p) {
                $skip = $true
                break
            }
        }
        -not $skip
    }

    $manifest = [ordered]@{}
    foreach ($file in $allFiles) {
        $relPath = $file.FullName.Replace($ProjectRoot, "").TrimStart('\', '/').Replace('\', '/')
        try {
            $hash = (Get-FileHash -Path $file.FullName -Algorithm SHA256).Hash
            $manifest[$relPath] = $hash
        } catch {}
    }
    return $manifest
}

# Helper function to categorize and analyze files
function Analyze-FileImpact {
    param (
        [string[]]$FilesToAnalyze,
        [string]$ProjectRoot
    )

    $fileCategoriesMap = @{}
    $allCategories = [System.Collections.Generic.HashSet[string]]::new()
    $specialReviewTriggers = @{
        "Security" = [System.Collections.Generic.HashSet[string]]::new()
        "Tenant"   = [System.Collections.Generic.HashSet[string]]::new()
        "Finance"  = [System.Collections.Generic.HashSet[string]]::new()
    }

    foreach ($fileRelPath in $FilesToAnalyze) {
        $fullPath = Join-Path $ProjectRoot $fileRelPath
        $fileContent = ""
        if (Test-Path $fullPath -PathType Leaf) {
            $fileContent = Get-Content -Path $fullPath -Raw -ErrorAction SilentlyContinue
        }
        if (-not $fileContent) { $fileContent = "" }

        $cats = [System.Collections.Generic.HashSet[string]]::new()

        if ($fileRelPath -like "*firestore.rules*" -or $fileRelPath -like "*firestore*.rules*") { [void]$cats.Add("FIRESTORE_SECURITY") }
        if ($fileRelPath -like "*AuthRepository*" -or $fileRelPath -like "*AuthViewModel*" -or $fileRelPath -like "*LoginScreen*" -or $fileRelPath -like "*SessionManager*" -or $fileRelPath -like "*User.kt*" -or $fileRelPath -like "*Role*" -or $fileRelPath -like "*CredentialManager*" -or $fileRelPath -like "*ui\auth\*" -or $fileContent -match "AuthRepository" -or $fileContent -match "FirebaseAuth") { [void]$cats.Add("AUTHENTICATION") }
        if ($fileRelPath -like "*TenantContext*" -or $fileRelPath -like "*Dealership*" -or $fileRelPath -like "*MultiTenant*" -or $fileContent -match "TenantContext" -or $fileContent -match "dealershipId") { [void]$cats.Add("TENANT_ISOLATION") }
        if ($fileRelPath -like "*CashPayment*" -or $fileRelPath -like "*CashReceipt*" -or $fileRelPath -like "*RecordCashPaymentDialog*" -or $fileContent -match "CashPayment" -or $fileContent -match "CashAuthorization") { [void]$cats.Add("CASH_PAYMENT") }
        if ($fileRelPath -like "*DigitalPayment*" -or $fileRelPath -like "*PaymentService*" -or $fileRelPath -like "*Razorpay*" -or $fileContent -match "DigitalPayment" -or $fileContent -match "UPI") { [void]$cats.Add("DIGITAL_PAYMENT") }
        if ($fileRelPath -like "*MoneyUtils*" -or $fileRelPath -like "*CurrencyUtils*" -or $fileRelPath -like "*Paise*" -or $fileContent -match "amountPaise" -or $fileContent -match "remainingBalancePaise") { [void]$cats.Add("MONEY_PRECISION") }
        if ($fileRelPath -like "*FinanceViewModel*" -or $fileRelPath -like "*FinanceRepository*" -or $fileRelPath -like "*Transaction*" -or $fileRelPath -like "*Payment*" -or $fileRelPath -like "*EmiCalculator*" -or $fileContent -match "FinanceViewModel") { [void]$cats.Add("FINANCE") }
        if ($fileRelPath -like "*LoanRepository*" -or $fileRelPath -like "*LoanDetails*" -or $fileRelPath -like "*LoanApplication*" -or $fileRelPath -like "*LoanViewModel*" -or $fileContent -match "LoanRepository") { [void]$cats.Add("LOAN") }
        if ($fileRelPath -like "*CustomerRepository*" -or $fileRelPath -like "*CustomerViewModel*" -or $fileRelPath -like "*Customer*") { [void]$cats.Add("CUSTOMER") }
        if ($fileRelPath -like "*InventoryRepository*" -or $fileRelPath -like "*InventoryViewModel*" -or $fileRelPath -like "*Vehicle*" -or $fileRelPath -like "*Bike*" -or $fileRelPath -like "*DeadStock*") { [void]$cats.Add("INVENTORY") }
        if ($fileRelPath -like "*ReportsViewModel*" -or $fileRelPath -like "*ReportsScreen*" -or $fileRelPath -like "*DashboardReporting*" -or $fileRelPath -like "*Analytics*") { [void]$cats.Add("REPORTING") }
        if ($fileRelPath -like "*AppNavigation*" -or $fileRelPath -like "*NavRoutes*" -or $fileRelPath -like "*NavHost*" -or $fileRelPath -like "*ui\navigation\*") { [void]$cats.Add("NAVIGATION") }
        if ($fileRelPath -like "scripts\*" -or $fileRelPath -like ".github\*" -or $fileRelPath -like "*.ps1" -or $fileRelPath -like "*.sh" -or $fileRelPath -like "*.yml") { [void]$cats.Add("AUTOMATION") }
        if ($fileRelPath -like "*src\test\*" -or $fileRelPath -like "*src\androidTest\*" -or $fileRelPath -like "rules-tests\*" -or $fileRelPath -like "*Test.kt" -or $fileRelPath -like "*Spec.kt") { [void]$cats.Add("TEST_ONLY") }
        if ($fileRelPath -like "*build.gradle*" -or $fileRelPath -like "*libs.versions.toml*" -or $fileRelPath -like "*settings.gradle*" -or $fileRelPath -like "gradle\*") { [void]$cats.Add("DEPENDENCY") }
        if ($fileRelPath -like "*google-services.json*" -or $fileRelPath -like "*firebase.json*" -or $fileRelPath -like "*AndroidManifest.xml*" -or $fileRelPath -like "*.properties") { [void]$cats.Add("CONFIGURATION") }

        if ($cats.Count -eq 0) {
            if ($fileRelPath -like "*app\src\main\res\*" -or $fileRelPath -like "*ui\components\*" -or $fileRelPath -like "*ui\theme\*") { [void]$cats.Add("UI_ONLY") }
        }
        if ($cats.Count -eq 0) { [void]$cats.Add("UNKNOWN") }

        $fileCategoriesMap[$fileRelPath] = @($cats)
        foreach ($c in $cats) { [void]$allCategories.Add($c) }

        # Triggers
        if ($fileRelPath -like "*firestore.rules*" -or $fileRelPath -like "*AuthRepository*" -or $fileRelPath -like "*Role*" -or $fileRelPath -like "*TenantContext*" -or $fileContent -match "firestore\.rules" -or $fileContent -match "AuthRepository" -or $fileContent -match "\bRole\b" -or $fileContent -match "TenantContext") {
            $trig = @()
            if ($fileRelPath -like "*firestore.rules*" -or $fileContent -match "firestore\.rules") { $trig += "firestore.rules" }
            if ($fileRelPath -like "*AuthRepository*" -or $fileContent -match "AuthRepository") { $trig += "AuthRepository" }
            if ($fileRelPath -like "*Role*" -or $fileContent -match "\bRole\b") { $trig += "Role" }
            if ($fileRelPath -like "*TenantContext*" -or $fileContent -match "TenantContext") { $trig += "TenantContext" }
            $trigStr = $trig -join ', '
            [void]$specialReviewTriggers["Security"].Add("$fileRelPath ($trigStr)")
        }
        if ($fileRelPath -like "*TenantContext*" -or $fileRelPath -like "*AuthRepository*" -or $fileContent -match "TenantContext" -or $fileContent -match "dealershipId" -or $fileContent -match "AuthRepository") {
            $trig = @()
            if ($fileRelPath -like "*TenantContext*" -or $fileContent -match "TenantContext") { $trig += "TenantContext" }
            if ($fileContent -match "dealershipId") { $trig += "dealershipId" }
            if ($fileRelPath -like "*AuthRepository*" -or $fileContent -match "AuthRepository") { $trig += "AuthRepository" }
            $trigStr = $trig -join ', '
            [void]$specialReviewTriggers["Tenant"].Add("$fileRelPath ($trigStr)")
        }
        if ($fileRelPath -like "*LoanRepository*" -or $fileRelPath -like "*FinanceViewModel*" -or $fileContent -match "LoanRepository" -or $fileContent -match "FinanceViewModel" -or $fileContent -match "amountPaise" -or $fileContent -match "remainingBalancePaise") {
            $trig = @()
            if ($fileRelPath -like "*LoanRepository*" -or $fileContent -match "LoanRepository") { $trig += "LoanRepository" }
            if ($fileRelPath -like "*FinanceViewModel*" -or $fileContent -match "FinanceViewModel") { $trig += "FinanceViewModel" }
            if ($fileContent -match "amountPaise") { $trig += "amountPaise" }
            if ($fileContent -match "remainingBalancePaise") { $trig += "remainingBalancePaise" }
            $trigStr = $trig -join ', '
            [void]$specialReviewTriggers["Finance"].Add("$fileRelPath ($trigStr)")
        }
    }

    return @{
        "CategoriesMap"          = $fileCategoriesMap
        "AllCategories"          = $allCategories
        "SpecialReviewTriggers"  = $specialReviewTriggers
    }
}

# ==============================================================================
# MODE: -CreateBaseline
# ==============================================================================
if ($CreateBaseline) {
    Write-Report -Message '==================================================' -Color 'Cyan'
    Write-Report -Message '    CREATE QA CHANGE IMPACT BASELINE MODE         ' -Color 'Cyan'
    Write-Report -Message '==================================================' -Color 'Cyan'
    Write-Report -Message ''

    Write-Report -Message '--> Verifying baseline prerequisites...' -Color 'Yellow'

    # 1. Security Guard check
    $secGuardScript = Join-Path $projectRoot "scripts\security-guard.ps1"
    $secGuardStatus = "FAILED"
    if (Test-Path $secGuardScript) {
        $secOutput = & powershell -ExecutionPolicy Bypass -File $secGuardScript 2>&1 | Out-String
        if ($secOutput -match "STATUS:\s*PASSED") {
            $secGuardStatus = "PASSED"
            Write-Report -Message '  [PASS] Security Guard: PASSED' -Color 'Green'
        } else {
            Write-Report -Message "  [FAIL] Security Guard status: $secGuardStatus" -Color 'Red'
        }
    } else {
        Write-Report -Message '  [FAIL] scripts/security-guard.ps1 not found' -Color 'Red'
    }

    # 2. Firestore tests count check
    $firestoreCount = Get-FirestoreTestCount -ProjectRoot $projectRoot
    if ($firestoreCount -ge 65) {
        Write-Report -Message "  [PASS] Firestore tests count: $firestoreCount (>= 65 required)" -Color 'Green'
    } else {
        Write-Report -Message "  [FAIL] Firestore tests count insufficient: $firestoreCount (< 65)" -Color 'Red'
    }

    # 3. Android unit tests count check
    $androidCount = Get-AndroidUnitTestCount -ProjectRoot $projectRoot
    if ($androidCount -lt 253) {
        Write-Report -Message "  Running Android unit tests to generate XML reports..." -Color 'Gray'
        $null = & ".\gradlew.bat" testDebugUnitTest 2>&1
        $androidCount = Get-AndroidUnitTestCount -ProjectRoot $projectRoot
    }

    if ($androidCount -ge 253) {
        Write-Report -Message "  [PASS] Android unit tests count: $androidCount (>= 253 required)" -Color 'Green'
    } else {
        Write-Report -Message "  [FAIL] Android unit tests count insufficient: $androidCount (< 253)" -Color 'Red'
    }

    # 4. Build status check
    Write-Report -Message '  Verifying Android build status...' -Color 'Gray'
    $buildOutput = & ".\gradlew.bat" assembleDebug 2>&1
    if ($LASTEXITCODE -eq 0) {
        $buildStatus = "SUCCESS"
        Write-Report -Message '  [PASS] Android Build: SUCCESS' -Color 'Green'
    } else {
        $buildStatus = "FAILED"
        Write-Report -Message '  [FAIL] Android Build: FAILED' -Color 'Red'
    }

    # Validation
    $isValid = ($secGuardStatus -eq "PASSED") -and ($firestoreCount -ge 65) -and ($androidCount -ge 253) -and ($buildStatus -eq "SUCCESS")

    if ($isValid) {
        if (-not (Test-Path $qaBaselineDir)) {
            New-Item -ItemType Directory -Force -Path $qaBaselineDir | Out-Null
        }

        # Save release-baseline.json
        $baselineData = [ordered]@{
            "lastKnownGoodFirestoreTests"      = $firestoreCount
            "lastKnownGoodAndroidTests"        = $androidCount
            "lastKnownGoodSecurityGuardStatus" = "PASSED"
            "lastKnownGoodBuildStatus"         = "SUCCESS"
        }
        $baselineJsonPath = Join-Path $qaBaselineDir "release-baseline.json"
        $baselineData | ConvertTo-Json -Depth 5 | Set-Content -Path $baselineJsonPath

        # Save file-manifest.json
        Write-Report -Message '  Computing SHA-256 hashes for file-manifest.json...' -Color 'Gray'
        $manifest = Get-ProjectFileManifest -ProjectRoot $projectRoot
        $manifestJsonPath = Join-Path $qaBaselineDir "file-manifest.json"
        $manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $manifestJsonPath

        Write-Report -Message ''
        Write-Report -Message '==================================================' -Color 'Green'
        Write-Report -Message 'CHANGE IMPACT BASELINE: ESTABLISHED' -Color 'Green'
        Write-Report -Message '==================================================' -Color 'Green'
        Write-Report -Message "Saved: $baselineJsonPath" -Color 'Gray'
        Write-Report -Message "Saved: $manifestJsonPath ($($manifest.Count) files hashed)" -Color 'Gray'
        exit 0
    } else {
        Write-Report -Message ''
        Write-Report -Message '==================================================' -Color 'Red'
        Write-Report -Message 'BASELINE CREATION BLOCKED' -Color 'Red'
        Write-Report -Message '==================================================' -Color 'Red'
        exit 1
    }
}

# ==============================================================================
# MODE: Normal Read-Only Execution
# ==============================================================================
$nowStr = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message '         CHANGE IMPACT ANALYSIS (BOT #3)          ' -Color 'Cyan'
Write-Report -Message "Timestamp: $nowStr" -Color 'Gray'
Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message ''

# Baseline presence check
$releaseBaselinePath = Join-Path $qaBaselineDir "release-baseline.json"
$fileManifestPath = Join-Path $qaBaselineDir "file-manifest.json"
$hasBaseline = (Test-Path $releaseBaselinePath) -and (Test-Path $fileManifestPath)

$lastKnownGoodAndroidTests = 253
$lastKnownGoodFirestoreTests = 65

if ($hasBaseline) {
    try {
        $baseJson = Get-Content -Path $releaseBaselinePath -Raw | ConvertFrom-Json
        if ($baseJson.lastKnownGoodAndroidTests) {
            $lastKnownGoodAndroidTests = [int]$baseJson.lastKnownGoodAndroidTests
        }
        if ($baseJson.lastKnownGoodFirestoreTests) {
            $lastKnownGoodFirestoreTests = [int]$baseJson.lastKnownGoodFirestoreTests
        }
    } catch {}
} else {
    Write-Report -Message 'CHANGE IMPACT BASELINE: NOT ESTABLISHED' -Color 'Yellow'
    Write-Report -Message ''
}

# Check Android test count decrease
$currentAndroidTestCount = Get-AndroidUnitTestCount -ProjectRoot $projectRoot
if ($currentAndroidTestCount -gt 0 -and $currentAndroidTestCount -lt $lastKnownGoodAndroidTests) {
    Write-Report -Message "WARN - ANDROID TEST COUNT DECREASE DETECTED ($currentAndroidTestCount vs $lastKnownGoodAndroidTests)" -Color 'Yellow'
    Write-Report -Message ''
}

# 1. Collect Git Workspace Changes
Write-Report -Message '--> [1/5] Inspecting Git Change Status (GIT WORKSPACE CHANGES)...' -Color 'Yellow'

$rawChangedPaths = @()
$statusOutput = & git --no-pager status --porcelain 2>&1
if ($LASTEXITCODE -eq 0 -and $statusOutput) {
    foreach ($line in $statusOutput) {
        $lStr = [string]$line
        if ([string]::IsNullOrWhiteSpace($lStr)) { continue }
        if ($lStr.Length -gt 2) {
            $trimmed = $lStr.Substring(2).Trim()
            if ($trimmed -match "->") {
                $parts = $trimmed -split "->"
                foreach ($p in $parts) {
                    $f = $p.Trim().Replace('"', '').Replace('/', '\')
                    if ($f) { $rawChangedPaths += $f }
                }
            } else {
                $f = $trimmed.Replace('"', '').Replace('/', '\')
                if ($f) { $rawChangedPaths += $f }
            }
        }
    }
}

$diffOutput = & git --no-pager diff --name-only 2>&1
if ($LASTEXITCODE -eq 0 -and $diffOutput) {
    foreach ($line in $diffOutput) {
        $lStr = [string]$line
        if ([string]::IsNullOrWhiteSpace($lStr)) { continue }
        $f = $lStr.Trim().Replace('"', '').Replace('/', '\')
        if ($f) { $rawChangedPaths += $f }
    }
}

$cachedDiffOutput = & git --no-pager diff --cached --name-only 2>&1
if ($LASTEXITCODE -eq 0 -and $cachedDiffOutput) {
    foreach ($line in $cachedDiffOutput) {
        $lStr = [string]$line
        if ([string]::IsNullOrWhiteSpace($lStr)) { continue }
        $f = $lStr.Trim().Replace('"', '').Replace('/', '\')
        if ($f) { $rawChangedPaths += $f }
    }
}

$expandedFiles = @()
foreach ($p in $rawChangedPaths) {
    if ($p -like "*node_modules*" -or $p -like ".idea*" -or $p -like ".gradle*" -or $p -like "build\*" -or $p -like ".agent\*" -or $p -like "qa-reports\*" -or $p -like "qa-baseline\*") { continue }
    $fullP = Join-Path $projectRoot $p
    if (Test-Path $fullP -PathType Container) {
        $children = Get-ChildItem -Path $fullP -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
            $_.FullName -notlike "*node_modules*" -and $_.FullName -notlike "*\.idea\*" -and $_.FullName -notlike "*\.gradle\*" -and $_.FullName -notlike "*\build\*" -and $_.FullName -notlike "*\.agent\*" -and $_.FullName -notlike "*\qa-reports\*" -and $_.FullName -notlike "*\qa-baseline\*"
        }
        foreach ($c in $children) {
            $rel = $c.FullName.Replace($projectRoot, "").TrimStart('\', '/')
            if ($rel) { $expandedFiles += $rel }
        }
    } else {
        $expandedFiles += $p
    }
}

$gitChangedFiles = $expandedFiles | Select-Object -Unique | Where-Object { $_ -ne "" }
Write-Report -Message "GIT WORKSPACE CHANGES: $($gitChangedFiles.Count) file(s) changed" -Color 'Gray'
Write-Report -Message ''

# 1b. Collect Changes Since Known-Good QA Baseline
$baselineChangedFiles = @()
if ($hasBaseline) {
    Write-Report -Message '--> [1.5/5] Inspecting Baseline Manifest (CHANGES SINCE KNOWN-GOOD QA BASELINE)...' -Color 'Yellow'
    try {
        $baselineManifest = Get-Content -Path $fileManifestPath -Raw | ConvertFrom-Json
        $currentManifest = Get-ProjectFileManifest -ProjectRoot $projectRoot

        $baseKeys = $baselineManifest.PSObject.Properties.Name
        $currKeys = $currentManifest.Keys

        foreach ($k in $currKeys) {
            if (-not ($baseKeys -contains $k)) {
                $baselineChangedFiles += $k
            } else {
                if ($currentManifest[$k] -ne $baselineManifest.$k) {
                    $baselineChangedFiles += $k
                }
            }
        }
        foreach ($k in $baseKeys) {
            if (-not ($currKeys -contains $k)) {
                if (-not ($baselineChangedFiles -contains $k)) {
                    $baselineChangedFiles += $k
                }
            }
        }
    } catch {}
    $baselineChangedFiles = $baselineChangedFiles | Select-Object -Unique
    Write-Report -Message "CHANGES SINCE KNOWN-GOOD QA BASELINE: $($baselineChangedFiles.Count) file(s) changed" -Color 'Gray'
    Write-Report -Message ''
}

# Categorize and analyze changes
$allChangedFiles = ($gitChangedFiles + $baselineChangedFiles) | Select-Object -Unique

Write-Report -Message '--> [2/5] Categorizing Changed Files...' -Color 'Yellow'
$analysisResult = Analyze-FileImpact -FilesToAnalyze $allChangedFiles -ProjectRoot $projectRoot
$fileCategoriesMap = $analysisResult["CategoriesMap"]
$allCategories = $analysisResult["AllCategories"]
$specialReviewTriggers = $analysisResult["SpecialReviewTriggers"]

if ($allChangedFiles.Count -eq 0) {
    Write-Report -Message "  No changed files detected." -Color 'Green'
} else {
    foreach ($f in $allChangedFiles) {
        $catList = ($fileCategoriesMap[$f]) -join ", "
        Write-Report -Message "  - $f" -Color 'White'
        Write-Report -Message "    Categories: $catList" -Color 'Gray'
    }
}
Write-Report -Message ''

# 3. Flag Special Reviews & Evaluate Risk Level
Write-Report -Message '--> [3/5] Evaluating Special Review Flags & Risk Level...' -Color 'Yellow'

$securityReview  = ($specialReviewTriggers["Security"].Count -gt 0)
$tenantReview    = ($specialReviewTriggers["Tenant"].Count -gt 0)
$financialReview = ($specialReviewTriggers["Finance"].Count -gt 0)

if ($securityReview) {
    Write-Report -Message '  [FLAG] SECURITY REVIEW REQUIRED' -Color 'Red'
    foreach ($item in $specialReviewTriggers["Security"]) {
        Write-Report -Message "         Triggered by: $item" -Color 'Red'
    }
}
if ($tenantReview) {
    Write-Report -Message '  [FLAG] TENANT REGRESSION REQUIRED' -Color 'Red'
    foreach ($item in $specialReviewTriggers["Tenant"]) {
        Write-Report -Message "         Triggered by: $item" -Color 'Red'
    }
}
if ($financialReview) {
    Write-Report -Message '  [FLAG] FINANCIAL REGRESSION REQUIRED' -Color 'Red'
    foreach ($item in $specialReviewTriggers["Finance"]) {
        Write-Report -Message "         Triggered by: $item" -Color 'Red'
    }
}

if (-not ($securityReview -or $tenantReview -or $financialReview)) {
    Write-Report -Message '  No special reviews required based on changed files.' -Color 'Green'
}
Write-Report -Message ''

# Risk Level Determination
$riskLevel = "LOW"
$resultState = "LOW RISK"

if ($allChangedFiles.Count -eq 0) {
    $riskLevel = "LOW"
    $resultState = "NO CHANGES"
} elseif ($securityReview -or $tenantReview -or $financialReview) {
    $riskLevel = "CRITICAL"
    $resultState = "CRITICAL REVIEW REQUIRED"
} elseif ($allCategories.Contains("FIRESTORE_SECURITY") -or $allCategories.Contains("AUTHENTICATION") -or $allCategories.Contains("TENANT_ISOLATION") -or $allCategories.Contains("MONEY_PRECISION")) {
    $riskLevel = "CRITICAL"
    $resultState = "CRITICAL REVIEW REQUIRED"
} elseif ($allCategories.Contains("FINANCE") -or $allCategories.Contains("LOAN") -or $allCategories.Contains("CASH_PAYMENT") -or $allCategories.Contains("DIGITAL_PAYMENT") -or $allCategories.Contains("INVENTORY") -or $allCategories.Contains("CUSTOMER") -or $allCategories.Contains("NAVIGATION") -or $allCategories.Contains("DEPENDENCY") -or $allCategories.Contains("CONFIGURATION") -or $allChangedFiles.Count -gt 10) {
    $riskLevel = "HIGH"
    $resultState = "REVIEW REQUIRED"
} elseif ($allCategories.Contains("REPORTING")) {
    $riskLevel = "MEDIUM"
    $resultState = "REVIEW REQUIRED"
} else {
    $riskLevel = "LOW"
    $resultState = "LOW RISK"
}

Write-Report -Message "  Evaluated Risk Level: $riskLevel" -Color 'Cyan'
Write-Report -Message ''

# 4. Recommend Targeted Automated Tests & Physical Device QA Steps
Write-Report -Message '--> [4/5] Generating Targeted QA Recommendations...' -Color 'Yellow'

$testCmds = [System.Collections.Generic.HashSet[string]]::new()
$qaSteps = [System.Collections.Generic.HashSet[string]]::new()

if ($allChangedFiles.Count -eq 0) {
    [void]$testCmds.Add("./gradlew testDebugUnitTest")
    [void]$qaSteps.Add("Perform standard regression smoke test across primary application screens on physical device.")
} else {
    if ($allCategories.Contains("FIRESTORE_SECURITY")) {
        [void]$testCmds.Add("cd rules-tests; firebase emulators:exec --only firestore ""npm test""")
        [void]$qaSteps.Add("Test Firestore security rules access control with different user roles and verify cross-tenant data isolation.")
    }
    if ($allCategories.Contains("AUTHENTICATION")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Auth*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Role*""")
        [void]$qaSteps.Add("Test login, logout, role switching, session persistence, and unauthorized route redirection on physical device.")
    }
    if ($allCategories.Contains("TENANT_ISOLATION")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Tenant*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Isolation*""")
        [void]$qaSteps.Add("Log in as User A (Tenant 1) and User B (Tenant 2) on physical device to verify complete isolation of inventory, customer, and financial data.")
    }
    if ($allCategories.Contains("FINANCE") -or $allCategories.Contains("LOAN") -or $allCategories.Contains("MONEY_PRECISION")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Finance*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Money*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Paise*""")
        [void]$qaSteps.Add("Open the existing active loan and inspect the payment amount presented by the current application before confirming the controlled payment.")
    }
    if ($allCategories.Contains("CASH_PAYMENT")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Cash*""")
        [void]$qaSteps.Add("Open the existing active loan and inspect the payment amount presented by the current application before confirming the controlled payment.")
    }
    if ($allCategories.Contains("DIGITAL_PAYMENT")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Payment*""")
        [void]$qaSteps.Add("Test digital payment flow, status updates, and error handling with mock/test payment gateway on physical device.")
    }
    if ($allCategories.Contains("CUSTOMER")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Customer*""")
        [void]$qaSteps.Add("Add new customer, edit customer details, and verify persistence and search reactivity on physical device.")
    }
    if ($allCategories.Contains("INVENTORY")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Inventory*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Vehicle*""")
        [void]$qaSteps.Add("Add new vehicle to stock, update stock status, and test inventory search/filtering on physical device.")
    }
    if ($allCategories.Contains("REPORTING")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Report*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Dashboard*""")
        [void]$qaSteps.Add("Generate dashboard KPI reports and export financial reports on physical device, checking visual layout and math correctness.")
    }
    if ($allCategories.Contains("NAVIGATION")) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Nav*""")
        [void]$testCmds.Add("./gradlew testDebugUnitTest --tests ""*Route*""")
        [void]$qaSteps.Add("Test bottom navigation, back button behavior, deep linking, and screen rotation handling on physical device.")
    }
    if ($testCmds.Count -eq 0) {
        [void]$testCmds.Add("./gradlew testDebugUnitTest")
    }
    if ($qaSteps.Count -eq 0) {
        [void]$qaSteps.Add("Inspect screen rendering, light/dark theme contrast, touch target sizes, and edge-to-edge layout on physical device.")
    }
}

Write-Report -Message '  Recommended Automated Test Commands:' -Color 'Cyan'
foreach ($cmd in $testCmds) {
    Write-Report -Message "    - $cmd" -Color 'White'
}

Write-Report -Message '  Recommended Physical Device QA Steps:' -Color 'Cyan'
foreach ($step in $qaSteps) {
    Write-Report -Message "    - $step" -Color 'White'
}
Write-Report -Message ''

# 5. Output Final Summary Header & Result State
$resultColor = switch ($resultState) {
    "NO CHANGES"                { "Green" }
    "LOW RISK"                  { "Green" }
    "REVIEW REQUIRED"           { "Yellow" }
    "CRITICAL REVIEW REQUIRED"  { "Red" }
    default                     { "White" }
}

Write-Report -Message '==================================================' -Color 'Cyan'
Write-Report -Message "RESULT STATE: $resultState" -Color $resultColor
Write-Report -Message '==================================================' -Color 'Cyan'
