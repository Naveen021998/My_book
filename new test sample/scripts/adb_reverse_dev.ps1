param(
  [string]$Adb = "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"

& $Adb devices -l
& $Adb reverse tcp:8080 tcp:8080 | Out-Null
& $Adb reverse tcp:8083 tcp:8083 | Out-Null
& $Adb reverse --list

