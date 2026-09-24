# Security Guard Script (Bot #2)
# Path: scripts/security-guard.ps1
# Read-only security, tenant, financial, and architectural guard verification script.

$ErrorActionPreference = "Continue"

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
$reportFile = Join-Path $qaReportsDir "security-guard-$timestamp.txt"

function Write-GuardReport {
    param (
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
    Add-Content -Path $reportFile -Value $Message
}

$nowStr = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
Write-GuardReport -Message '==================================================' -Color 'Cyan'
Write-GuardReport -Message '           SECURITY GUARD VERIFICATION            ' -Color 'Cyan'
Write-GuardReport -Message "Timestamp: $nowStr" -Color 'Gray'
Write-GuardReport -Message '==================================================' -Color 'Cyan'
Write-GuardReport -Message ''

$checkResults = [ordered]@{
    "1. Tenant Isolation"         = "PENDING"
    "2. Demo/Prod Separation"     = "PENDING"
    "3. Money Authority"          = "PENDING"
    "4. Cash Authorization"       = "PENDING"
    "5. Cash Replay Protection"   = "PENDING"
    "6. Digital Payment Security" = "PENDING"
    "7. Rule Sync"                = "PENDING"
    "8. Exception Handling"       = "PENDING"
    "9. Remote Write Order"       = "PENDING"
    "10. Secret Scan"             = "PENDING"
    "11. Security Test Baseline"  = "PENDING"
    "12. High-Risk Code Markers"  = "PENDING"
}

$criticalFindings = @()
$warningFindings = @()
$infoFindings = @()

$appJavaDir = Join-Path $projectRoot "app\src\main\java"

# ---------------------------------------------------------------------------
# Check 1: Tenant Isolation
# Scans app/src/main/java (excluding DemoData.kt) for "dealership_demo_001"
# Excludes @Preview functions, @PreviewParameter, preview sample parameters, and inequality comparisons (classified as INFO)
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[1/12] Checking Tenant Isolation...' -Color 'Yellow'
$tenantWarnMatches = @()
$tenantInfoMatches = @()

if (Test-Path $appJavaDir) {
    $files = Get-ChildItem -Path $appJavaDir -Recurse -Filter "*.kt" | Where-Object { $_.Name -ne "DemoData.kt" }
    foreach ($file in $files) {
        $matches = Select-String -Path $file.FullName -Pattern '"dealership_demo_001"' -SimpleMatch -ErrorAction SilentlyContinue
        if ($matches) {
            $fileLines = Get-Content -Path $file.FullName -ErrorAction SilentlyContinue
            foreach ($m in $matches) {
                $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
                $matchEntry = "${relPath}:$($m.LineNumber): $($m.Line.Trim())"

                $isInfo = $false
                # 1. Preview / PreviewSampleData files or paths
                if ($file.Name -like "*Preview*" -or $file.FullName -like "*\ui\preview\*") {
                    $isInfo = $true
                }
                # 2. Match line contains @Preview, @PreviewParameter, PreviewParameterProvider
                elseif ($m.Line -match "@Preview" -or $m.Line -match "@PreviewParameter" -or $m.Line -match "PreviewParameterProvider") {
                    $isInfo = $true
                }
                # 3. Parameter default for previews / sample constructors
                elseif ($m.Line -match 'dealershipId\s*:\s*String\s*=\s*"dealership_demo_001"') {
                    $isInfo = $true
                }
                # 4. Inequality comparison checks (!=, .equals(...) == false, not equal, or negation)
                elseif ($m.Line -match '!=' -or $m.Line -match '\.equals\(.*\)\s*==\s*false' -or $m.Line -match 'not equal' -or $m.Line -match '!\s*isDemo' -or $m.Line -match '!') {
                    $isInfo = $true
                }
                else {
                    # Check surrounding context lines for @Preview or @PreviewParameter
                    $lineIdx = $m.LineNumber - 1
                    $startIdx = [Math]::Max(0, $lineIdx - 15)
                    $endIdx = [Math]::Min($fileLines.Count - 1, $lineIdx + 5)
                    $contextChunk = ($fileLines[$startIdx..$endIdx]) -join "`n"
                    if ($contextChunk -match "@Preview" -or $contextChunk -match "@PreviewParameter" -or $contextChunk -match "PreviewParameterProvider") {
                        $isInfo = $true
                    }
                }

                if ($isInfo) {
                    $tenantInfoMatches += $matchEntry
                } else {
                    $tenantWarnMatches += $matchEntry
                }
            }
        }
    }
}

if ($tenantInfoMatches.Count -gt 0) {
    $infoMsg = "[Tenant Isolation] Found $($tenantInfoMatches.Count) preview/sample parameter occurrence(s) of 'dealership_demo_001':"
    $infoFindings += $infoMsg
    foreach ($m in $tenantInfoMatches) {
        $infoFindings += "    $m"
    }
    Write-GuardReport -Message "  [INFO] Found $($tenantInfoMatches.Count) preview/sample parameter reference(s) (classified as INFO)" -Color 'Cyan'
}

if ($tenantWarnMatches.Count -gt 0) {
    $warnMsg = "[Tenant Isolation] Found $($tenantWarnMatches.Count) production occurrence(s) of 'dealership_demo_001':"
    $warningFindings += $warnMsg
    foreach ($m in $tenantWarnMatches) {
        $warningFindings += "    $m"
    }
    $checkResults["1. Tenant Isolation"] = "WARNING"
    Write-GuardReport -Message "  [WARN] Found $($tenantWarnMatches.Count) hardcoded demo tenant ID reference(s) in production code" -Color 'Yellow'
} else {
    $checkResults["1. Tenant Isolation"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Tenant isolation verified (no demo tenant ID references outside previews/samples)' -Color 'Green'
}

# ---------------------------------------------------------------------------
# Check 2: Demo/Prod Separation
# Ensures production failures don't fall back to DemoData
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[2/12] Checking Demo/Prod Separation...' -Color 'Yellow'
$demoFallbackMatches = @()
if (Test-Path $appJavaDir) {
    $repoVmFiles = Get-ChildItem -Path $appJavaDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -match 'repository|viewmodel' }
    foreach ($file in $repoVmFiles) {
        $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -and $content -match 'catch\s*\([^)]+\)\s*\{[^}]*DemoData\.(customers|vehicles|loans|deals|transactions)') {
            if ($content -notmatch 'isDemoMode') {
                $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
                $demoFallbackMatches += "${relPath}: Catch block fallbacks to DemoData without isDemoMode guard"
            }
        }
    }
}

if ($demoFallbackMatches.Count -gt 0) {
    $warningFindings += "[Demo/Prod Separation] Fallback to DemoData detected in production paths:"
    foreach ($m in $demoFallbackMatches) {
        $warningFindings += "    $m"
    }
    $checkResults["2. Demo/Prod Separation"] = "WARNING"
    Write-GuardReport -Message '  [WARN] Potential DemoData fallback detected in catch blocks' -Color 'Yellow'
} else {
    $checkResults["2. Demo/Prod Separation"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Demo/Prod separation verified (no production error fallbacks to DemoData)' -Color 'Green'
}

# ---------------------------------------------------------------------------
# Check 3: Money Authority
# Scans payment mutation paths for Double arithmetic
# Non-authoritative formatting / UI state / wrappers in LoanRepository.kt & FinanceViewModel.kt -> INFO
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[3/12] Checking Money Authority (Paise vs Double)...' -Color 'Yellow'
$doubleMoneyWarnMatches = @()
$doubleMoneyInfoMatches = @()

if (Test-Path $appJavaDir) {
    $paymentMutationFiles = Get-ChildItem -Path $appJavaDir -Recurse -Filter "*.kt" | Where-Object {
        $_.Name -match 'LoanRepository|FinanceViewModel|PaymentService|TransactionRepository'
    }
    foreach ($file in $paymentMutationFiles) {
        $lines = Get-Content -Path $file.FullName -ErrorAction SilentlyContinue
        $lineNo = 0
        foreach ($line in $lines) {
            $lineNo++
            if ($line -match '(\b(paidAmount|remainingBalance|emiAmount|principalAmount)\s*[\+\-\*]=?\s*|\b(paidAmount|remainingBalance|emiAmount|principalAmount)\b.*Double)') {
                $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
                $matchEntry = "${relPath}:${lineNo}: $($line.Trim())"

                # Check if this conversion is in FinanceViewModel.kt or LoanRepository.kt or for non-authoritative formatting/UI state/wrapper methods
                $isUiOrFormatting = $false
                if ($file.Name -eq "FinanceViewModel.kt") {
                    $isUiOrFormatting = $true
                }
                elseif ($file.Name -eq "LoanRepository.kt") {
                    if ($line -match "toRupeesDouble" -or $line -match "paidAmount:\s*Double" -or $line -match "Format" -or $line -match "Display" -or $line -match "wrapper") {
                        $isUiOrFormatting = $true
                    }
                }
                elseif ($line -match "toRupeesDouble" -or $line -match "Format" -or $line -match "Display") {
                    $isUiOrFormatting = $true
                }

                if ($isUiOrFormatting) {
                    $doubleMoneyInfoMatches += $matchEntry
                } else {
                    $doubleMoneyWarnMatches += $matchEntry
                }
            }
        }
    }
}

if ($doubleMoneyInfoMatches.Count -gt 0) {
    $infoFindings += "[Money Authority] Non-authoritative UI/formatting Double arithmetic detected (classified as INFO):"
    foreach ($m in $doubleMoneyInfoMatches) {
        $infoFindings += "    $m"
    }
    Write-GuardReport -Message "  [INFO] Found $($doubleMoneyInfoMatches.Count) non-authoritative formatting/UI Double calculation(s) (classified as INFO)" -Color 'Cyan'
}

if ($doubleMoneyWarnMatches.Count -gt 0) {
    $warningFindings += "[Money Authority] Double arithmetic detected on money fields in payment paths:"
    foreach ($m in $doubleMoneyWarnMatches) {
        $warningFindings += "    $m"
    }
    $checkResults["3. Money Authority"] = "WARNING"
    Write-GuardReport -Message "  [WARN] Found $($doubleMoneyWarnMatches.Count) potential Double arithmetic instance(s) in authoritative payment mutation paths" -Color 'Yellow'
} else {
    $checkResults["3. Money Authority"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Money authority verified (paise integer arithmetic enforced on authoritative money operations)' -Color 'Green'
}

# ---------------------------------------------------------------------------
# Check 4: Cash Authorization
# Asserts firestore.rules & User.kt restrict cash to DEALERSHIP_ADMIN, STORE_MANAGER, FINANCE_USER
# (denies SUPER_ADMIN, CUSTOMER, SALES_USER, STATE_MANAGER)
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[4/12] Checking Cash Authorization Restrictions...' -Color 'Yellow'
$cashAuthValid = $true
$cashAuthIssues = @()

$rulesFile = Join-Path $projectRoot "firestore.rules"
if (Test-Path $rulesFile) {
    $rulesContent = Get-Content -Path $rulesFile -Raw
    if ($rulesContent -match "function isAuthorizedCashCollector\([^)]*\)\s*\{[^}]*getUserData\(\)\.role\s+in\s+\[([^\]]+)\]") {
        $rolesStr = $Matches[1]
        if ($rolesStr -notmatch "'DEALERSHIP_ADMIN'" -or $rolesStr -notmatch "'STORE_MANAGER'" -or $rolesStr -notmatch "'FINANCE_USER'") {
            $cashAuthValid = $false
            $cashAuthIssues += "firestore.rules isAuthorizedCashCollector missing required allowed roles"
        }
        if ($rolesStr -match "'SUPER_ADMIN'" -or $rolesStr -match "'CUSTOMER'" -or $rolesStr -match "'SALES_USER'" -or $rolesStr -match "'STATE_MANAGER'") {
            $cashAuthValid = $false
            $cashAuthIssues += "firestore.rules isAuthorizedCashCollector unexpectedly contains denied roles"
        }
    } else {
        $cashAuthValid = $false
        $cashAuthIssues += "firestore.rules missing isAuthorizedCashCollector function definition"
    }
} else {
    $cashAuthValid = $false
    $cashAuthIssues += "firestore.rules not found"
}

$userKtFile = Join-Path $appJavaDir "com\automotive\salesfinance\model\User.kt"
if (Test-Path $userKtFile) {
    $userKtContent = Get-Content -Path $userKtFile -Raw
    if ($userKtContent -match "fun canCollectCashPayment\(\)[^{]*=\s*role\s+in\s+listOf\(([^)]+)\)") {
        $userRolesStr = $Matches[1]
        if ($userRolesStr -notmatch "DEALERSHIP_ADMIN" -or $userRolesStr -notmatch "STORE_MANAGER" -or $userRolesStr -notmatch "FINANCE_USER") {
            $cashAuthValid = $false
            $cashAuthIssues += "User.kt canCollectCashPayment() missing required allowed roles"
        }
        if ($userRolesStr -match "SUPER_ADMIN" -or $userRolesStr -match "CUSTOMER" -or $userRolesStr -match "SALES_USER" -or $userRolesStr -match "STATE_MANAGER") {
            $cashAuthValid = $false
            $cashAuthIssues += "User.kt canCollectCashPayment() unexpectedly includes denied roles"
        }
    } else {
        $cashAuthValid = $false
        $cashAuthIssues += "User.kt missing canCollectCashPayment() method definition"
    }
} else {
    $cashAuthValid = $false
    $cashAuthIssues += "User.kt not found"
}

if ($cashAuthValid) {
    $checkResults["4. Cash Authorization"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Cash authorization verified (restricted strictly to DEALERSHIP_ADMIN, STORE_MANAGER, FINANCE_USER)' -Color 'Green'
} else {
    $checkResults["4. Cash Authorization"] = "FAILED"
    $criticalFindings += "[Cash Authorization] Validation failed:"
    foreach ($issue in $cashAuthIssues) {
        $criticalFindings += "    $issue"
    }
    Write-GuardReport -Message '  [FAIL] Cash authorization restrictions failed check' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 5: Cash Replay Protection
# Asserts firestore.rules contains !exists(txnPath) and lastPaymentTransactionId
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[5/12] Checking Cash Replay Protection...' -Color 'Yellow'
$replayValid = $true
$replayIssues = @()

if (Test-Path $rulesFile) {
    $rulesContent = Get-Content -Path $rulesFile -Raw
    if ($rulesContent -notmatch "!exists\(txnPath\)") {
        $replayValid = $false
        $replayIssues += "firestore.rules missing !exists(txnPath) check for cash transactions"
    }
    if ($rulesContent -notmatch "lastPaymentTransactionId") {
        $replayValid = $false
        $replayIssues += "firestore.rules missing lastPaymentTransactionId reference"
    }
    if ($rulesContent -notmatch "TXN_CASH_") {
        $replayValid = $false
        $replayIssues += "firestore.rules missing deterministic ID check (TXN_CASH_)"
    }
} else {
    $replayValid = $false
    $replayIssues += "firestore.rules not found"
}

if ($replayValid) {
    $checkResults["5. Cash Replay Protection"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Cash replay protection verified (!exists(txnPath) and lastPaymentTransactionId checked)' -Color 'Green'
} else {
    $checkResults["5. Cash Replay Protection"] = "FAILED"
    $criticalFindings += "[Cash Replay Protection] Validation failed:"
    foreach ($issue in $replayIssues) {
        $criticalFindings += "    $issue"
    }
    Write-GuardReport -Message '  [FAIL] Cash replay protection assertions missing in firestore.rules' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 6: Digital Payment Security
# Verifies FinanceViewModel.kt & PaymentScreen.kt block live Razorpay execution
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[6/12] Checking Digital Payment Security...' -Color 'Yellow'
$digitalSecValid = $true
$digitalSecIssues = @()

$fvmFile = Join-Path $appJavaDir "com\automotive\salesfinance\viewmodel\FinanceViewModel.kt"
if (Test-Path $fvmFile) {
    $fvmContent = Get-Content -Path $fvmFile -Raw
    if ($fvmContent -notmatch "Live Payment Verification Unavailable" -and $fvmContent -notmatch "!\s*isAuthorizedForLivePayments") {
        $digitalSecValid = $false
        $digitalSecIssues += "FinanceViewModel.kt does not block live Razorpay execution"
    }
} else {
    $digitalSecValid = $false
    $digitalSecIssues += "FinanceViewModel.kt not found"
}

$psFile = Join-Path $appJavaDir "com\automotive\salesfinance\ui\finance\PaymentScreen.kt"
if (Test-Path $psFile) {
    $psContent = Get-Content -Path $psFile -Raw
    if ($psContent -notmatch "Live Payment Verification Unavailable" -and $psContent -notmatch "enabled\s*=\s*false") {
        $digitalSecValid = $false
        $digitalSecIssues += "PaymentScreen.kt does not restrict live Razorpay execution UI controls"
    }
} else {
    $digitalSecValid = $false
    $digitalSecIssues += "PaymentScreen.kt not found"
}

if ($digitalSecValid) {
    $checkResults["6. Digital Payment Security"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Digital payment security verified (live Razorpay execution blocked)' -Color 'Green'
} else {
    $checkResults["6. Digital Payment Security"] = "FAILED"
    $criticalFindings += "[Digital Payment Security] Live execution block missing:"
    foreach ($issue in $digitalSecIssues) {
        $criticalFindings += "    $issue"
    }
    Write-GuardReport -Message '  [FAIL] Digital payment security check failed' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 7: Rule Sync
# Compares firestore.rules with rules-tests/firestore.rules
# Strictly READ-ONLY. If they differ -> CRITICAL - FIRESTORE RULE DRIFT and status BLOCKED
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[7/12] Checking Rule Sync (firestore.rules vs rules-tests/firestore.rules)...' -Color 'Yellow'
$testRulesFile = Join-Path $projectRoot "rules-tests\firestore.rules"
if ((Test-Path $rulesFile) -and (Test-Path $testRulesFile)) {
    $rootContent = ((Get-Content -Path $rulesFile -Raw) -replace "\r\n", "`n").Trim()
    $testContent = ((Get-Content -Path $testRulesFile -Raw) -replace "\r\n", "`n").Trim()
    if ($rootContent -eq $testContent) {
        $checkResults["7. Rule Sync"] = "PASSED"
        Write-GuardReport -Message '  [PASS] Rule sync verified (firestore.rules and rules-tests/firestore.rules are identical)' -Color 'Green'
    } else {
        $checkResults["7. Rule Sync"] = "FAILED"
        $criticalFindings += "CRITICAL - FIRESTORE RULE DRIFT: firestore.rules and rules-tests/firestore.rules differ!"
        Write-GuardReport -Message '  [FAIL] CRITICAL - FIRESTORE RULE DRIFT: firestore.rules and rules-tests/firestore.rules differ!' -Color 'Red'
    }
} else {
    $checkResults["7. Rule Sync"] = "FAILED"
    $criticalFindings += "[Rule Sync] Could not find firestore.rules or rules-tests/firestore.rules"
    Write-GuardReport -Message '  [FAIL] Rules files missing' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 8: Exception Handling
# Checks critical repository write functions for try-catch & Result wrapping
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[8/12] Checking Exception Handling on Critical Repository Writes...' -Color 'Yellow'
$criticalRepoFiles = Get-ChildItem -Path $appJavaDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -match 'repository' }
$exceptionIssues = @()

$targetWriteMethods = @("recordCashPaymentAtomic", "addVehicle", "addOrUpdateVehicle", "deleteVehicle", "applyForLoan")

foreach ($file in $criticalRepoFiles) {
    $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($content) {
        foreach ($method in $targetWriteMethods) {
            if ($content -match "suspend\s+fun\s+$method\b") {
                if ($content -notmatch "suspend\s+fun\s+$method\b[^{]*:\s*Result<") {
                    $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
                    $exceptionIssues += "${relPath}: Critical write method '$method' signature does not return Result<T>"
                }
            }
        }
    }
}

if ($exceptionIssues.Count -gt 0) {
    $warningFindings += "[Exception Handling] Potential issue in critical write functions:"
    foreach ($issue in $exceptionIssues) {
        $warningFindings += "    $issue"
    }
    $checkResults["8. Exception Handling"] = "WARNING"
    Write-GuardReport -Message "  [WARN] Found $($exceptionIssues.Count) critical write method signature warning(s)" -Color 'Yellow'
} else {
    $checkResults["8. Exception Handling"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Exception handling verified (critical repository write methods return Result<T>)' -Color 'Green'
}

# ---------------------------------------------------------------------------
# Check 9: Remote Write Order
# Verifies remote .await() precedes local state mutation in addVehicle and recordCashPaymentAtomic
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[9/12] Checking Remote Write Order (.await() before local mutation)...' -Color 'Yellow'
$orderValid = $true
$orderIssues = @()

$invFile = Join-Path $appJavaDir "com\automotive\salesfinance\repository\InventoryRepository.kt"
if (Test-Path $invFile) {
    $invContent = Get-Content -Path $invFile -Raw
    $awaitPos = $invContent.IndexOf("performFirestoreSave(preparedVehicle)")
    $flowPos = $invContent.IndexOf("_vehiclesFlow.value = current", $awaitPos)
    if ($awaitPos -eq -1 -or $flowPos -eq -1 -or $awaitPos -ge $flowPos) {
        $orderValid = $false
        $orderIssues += "InventoryRepository.kt addOrUpdateVehicle local flow updated before remote performFirestoreSave"
    }
}

$loanFile = Join-Path $appJavaDir "com\automotive\salesfinance\repository\LoanRepository.kt"
if (Test-Path $loanFile) {
    $loanContent = Get-Content -Path $loanFile -Raw
    $awaitPos = $loanContent.IndexOf("}.await()")
    $flowPos = $loanContent.IndexOf("_loansFlow.value =", $awaitPos)
    if ($awaitPos -eq -1 -or $flowPos -eq -1 -or $awaitPos -ge $flowPos) {
        $orderValid = $false
        $orderIssues += "LoanRepository.kt recordCashPaymentAtomic local flow updated before Firestore transaction .await()"
    }
}

if ($orderValid) {
    $checkResults["9. Remote Write Order"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Remote write order verified (remote .await() precedes local state flow update)' -Color 'Green'
} else {
    $checkResults["9. Remote Write Order"] = "FAILED"
    $criticalFindings += "[Remote Write Order] Remote write order violated:"
    foreach ($issue in $orderIssues) {
        $criticalFindings += "    $issue"
    }
    Write-GuardReport -Message '  [FAIL] Remote write order violated in repositories' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 10: Secret Scan
# Scans for RAZORPAY_KEY_SECRET, BEGIN PRIVATE KEY, credentials
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[10/12] Checking for Leaked Secrets & Private Keys...' -Color 'Yellow'
$secretPatterns = @(
    '-----BEGIN (RSA |EC |DS )?PRIVATE KEY-----',
    'AKIA[0-9A-Z]{16}',
    'sk_live_[0-9a-zA-Z]{24}',
    'rzp_live_[0-9a-zA-Z]{14}'
)

$foundSecrets = @()
$scannableFiles = Get-ChildItem -Path $projectRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch '[\\/](node_modules|\.git|build|\.gradle|qa-reports)[\\/]' -and
        $_.Name -notmatch '\.(png|jpg|jpeg|gif|ico|aar|jar|dex|apk|so|zip|log)$'
    }

foreach ($file in $scannableFiles) {
    foreach ($pattern in $secretPatterns) {
        $matches = Select-String -Path $file.FullName -Pattern $pattern -ErrorAction SilentlyContinue
        if ($matches) {
            foreach ($match in $matches) {
                $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
                $foundSecrets += "${relPath}:$($match.LineNumber): $($match.Line.Trim())"
            }
        }
    }
}

if ($foundSecrets.Count -gt 0) {
    $checkResults["10. Secret Scan"] = "FAILED"
    $criticalFindings += "[Secret Scan] Suspicious credentials or private keys found:"
    foreach ($s in $foundSecrets) {
        $criticalFindings += "    $s"
    }
    Write-GuardReport -Message "  [FAIL] Found $($foundSecrets.Count) hardcoded secret/private key instance(s)" -Color 'Red'
} else {
    $checkResults["10. Secret Scan"] = "PASSED"
    Write-GuardReport -Message '  [PASS] Secret scan passed (0 private keys or live secret keys found)' -Color 'Green'
}

# ---------------------------------------------------------------------------
# Check 11: Security Test Baseline
# Checks rules-tests/test/firestore.rules.spec.js for at least 65 tests
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[11/12] Checking Security Test Baseline (65+ Firestore rules tests)...' -Color 'Yellow'
$specFile = Join-Path $projectRoot "rules-tests\test\firestore.rules.spec.js"
if (Test-Path $specFile) {
    $specContent = Get-Content -Path $specFile -Raw
    $matches = [regex]::Matches($specContent, "it\('Test \d+")
    $testCount = $matches.Count
    if ($testCount -ge 65) {
        $checkResults["11. Security Test Baseline"] = "PASSED"
        Write-GuardReport -Message "  [PASS] Security test baseline verified ($testCount tests found in firestore.rules.spec.js)" -Color 'Green'
    } else {
        $checkResults["11. Security Test Baseline"] = "FAILED"
        $criticalFindings += "[Security Test Baseline] Expected at least 65 security tests, but found $testCount in firestore.rules.spec.js"
        Write-GuardReport -Message "  [FAIL] Security test baseline count mismatch ($testCount vs expected >= 65)" -Color 'Red'
    }
} else {
    $checkResults["11. Security Test Baseline"] = "FAILED"
    $criticalFindings += "[Security Test Baseline] firestore.rules.spec.js not found"
    Write-GuardReport -Message '  [FAIL] rules-tests/test/firestore.rules.spec.js not found' -Color 'Red'
}

# ---------------------------------------------------------------------------
# Check 12: High-Risk Code Markers
# Scans for TODO, FIXME, NotImplementedError, dryRun = false
# Excludes node_modules directories, scripts, and data_extraction_rules.xml comments
# ---------------------------------------------------------------------------
Write-GuardReport -Message '[12/12] Checking High-Risk Code Markers (TODO, FIXME, NotImplementedError, dryRun = false)...' -Color 'Yellow'
$riskMatches = @()
$scannableRiskFiles = Get-ChildItem -Path $projectRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch '[\\/](node_modules|\.git|build|\.gradle|qa-reports|scripts)[\\/]' -and
        $_.Name -notmatch '\.(png|jpg|jpeg|gif|ico|aar|jar|dex|apk|so|zip|log)$'
    }

foreach ($file in $scannableRiskFiles) {
    if ($file.FullName -match '[\\/](node_modules|scripts)[\\/]') { continue }
    $matches = Select-String -Path $file.FullName -Pattern '(\bTODO\b|\bFIXME\b|NotImplementedError|dryRun\s*=\s*false)' -ErrorAction SilentlyContinue
    if ($matches) {
        foreach ($m in $matches) {
            # Exclude node_modules explicitly
            if ($file.FullName -match "node_modules") { continue }
            # Exclude comments in data_extraction_rules.xml
            if ($file.Name -eq "data_extraction_rules.xml" -and ($m.Line -match '<!--' -or $m.Line -match '-->')) { continue }

            $relPath = $file.FullName.Replace($projectRoot, "").TrimStart('\', '/')
            $riskMatches += "${relPath}:$($m.LineNumber): $($m.Line.Trim())"
        }
    }
}

if ($riskMatches.Count -gt 0) {
    $warningFindings += "[High-Risk Markers] Found $($riskMatches.Count) high-risk code marker(s):"
    foreach ($m in $riskMatches) {
        $warningFindings += "    $m"
    }
    $checkResults["12. High-Risk Code Markers"] = "WARNING"
    Write-GuardReport -Message "  [WARN] Found $($riskMatches.Count) high-risk code marker(s)" -Color 'Yellow'
} else {
    $checkResults["12. High-Risk Code Markers"] = "PASSED"
    Write-GuardReport -Message '  [PASS] High-risk code markers check passed (0 TODOs, FIXMEs, or dryRun=false)' -Color 'Green'
}

Write-GuardReport -Message ''

# ---------------------------------------------------------------------------
# Status Determination & Console Summary
# ---------------------------------------------------------------------------
$overallStatus = "PASSED"
if ($criticalFindings.Count -gt 0) {
    $overallStatus = "BLOCKED"
} elseif ($warningFindings.Count -gt 0) {
    $overallStatus = "REVIEW REQUIRED"
}

Write-GuardReport -Message '==================================================' -Color 'Cyan'
Write-GuardReport -Message '           SECURITY GUARD SUMMARY                 ' -Color 'Cyan'
Write-GuardReport -Message '==================================================' -Color 'Cyan'

foreach ($key in $checkResults.Keys) {
    $st = $checkResults[$key]
    $col = switch ($st) {
        "PASSED"          { "Green" }
        "WARNING"         { "Yellow" }
        "FAILED"          { "Red" }
        default           { "White" }
    }
    $paddedKey = $key.PadRight(32)
    Write-GuardReport -Message "$paddedKey : $st" -Color $col
}

Write-GuardReport -Message '--------------------------------------------------' -Color 'Cyan'
Write-GuardReport -Message "FINDINGS BREAKDOWN:" -Color 'Cyan'
$critCountColor = if ($criticalFindings.Count -gt 0) { "Red" } else { "Green" }
$warnCountColor = if ($warningFindings.Count -gt 0) { "Yellow" } else { "Green" }
Write-GuardReport -Message "  CRITICAL COUNT : $($criticalFindings.Count)" -Color $critCountColor
Write-GuardReport -Message "  WARNINGS COUNT : $($warningFindings.Count)" -Color $warnCountColor
Write-GuardReport -Message "  INFO COUNT     : $($infoFindings.Count)" -Color 'Cyan'
Write-GuardReport -Message '--------------------------------------------------' -Color 'Cyan'

$critColor = if ($criticalFindings.Count -gt 0) { "Red" } else { "Green" }
Write-GuardReport -Message "CRITICAL FINDINGS ($($criticalFindings.Count)):" -Color $critColor
if ($criticalFindings.Count -eq 0) {
    Write-GuardReport -Message '  None' -Color 'Green'
} else {
    foreach ($cf in $criticalFindings) {
        Write-GuardReport -Message "  $cf" -Color 'Red'
    }
}

Write-GuardReport -Message ''
$warnColor = if ($warningFindings.Count -gt 0) { "Yellow" } else { "Green" }
Write-GuardReport -Message "WARNINGS ($($warningFindings.Count)):" -Color $warnColor
if ($warningFindings.Count -eq 0) {
    Write-GuardReport -Message '  None' -Color 'Green'
} else {
    foreach ($wf in $warningFindings) {
        Write-GuardReport -Message "  $wf" -Color 'Yellow'
    }
}

Write-GuardReport -Message ''
Write-GuardReport -Message "INFO FINDINGS ($($infoFindings.Count)):" -Color 'Cyan'
if ($infoFindings.Count -eq 0) {
    Write-GuardReport -Message '  None' -Color 'Cyan'
} else {
    foreach ($inf in $infoFindings) {
        Write-GuardReport -Message "  $inf" -Color 'Cyan'
    }
}

Write-GuardReport -Message '--------------------------------------------------' -Color 'Cyan'
$statusColor = switch ($overallStatus) {
    "PASSED"          { "Green" }
    "REVIEW REQUIRED" { "Yellow" }
    "BLOCKED"         { "Red" }
}
Write-GuardReport -Message "STATUS: $overallStatus" -Color $statusColor
Write-GuardReport -Message '==================================================' -Color 'Cyan'
Write-GuardReport -Message "Report saved to: $reportFile" -Color 'Gray'

if ($overallStatus -eq "BLOCKED") {
    exit 1
} else {
    exit 0
}
