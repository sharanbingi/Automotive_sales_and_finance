$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Java version:"
java -version

Set-Location "C:\Users\shara\AndroidStudioProjects\Automotivesalesandfinance2\rules-tests"

Write-Host "Running npm install..."
npm install

Write-Host "Running Firestore emulator rule tests..."
npx firebase emulators:exec --only firestore "npm test"
