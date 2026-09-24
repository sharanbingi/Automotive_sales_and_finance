$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
cd rules-tests
npx firebase emulators:exec --only firestore "npm test"
cd ..
powershell -ExecutionPolicy Bypass -File scripts/security-guard.ps1
powershell -ExecutionPolicy Bypass -File scripts/change-impact.ps1
powershell -ExecutionPolicy Bypass -File scripts/release-gate.ps1
