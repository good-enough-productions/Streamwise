$adb = "C:\Users\dschm\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"

$deviceLine = (& $adb devices | Where-Object { $_ -match '\s+device$' } | Select-Object -First 1)
$serial = $deviceLine -replace '\s+device$', ''
Write-Host "Device: $serial"

& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { Write-Error "Install failed"; exit 1 }

& $adb -s $serial shell am start -n "com.aistudio.streammanager.qpwoei/com.example.MainActivity"
Write-Host "Done. App launched."
