param(
    [ValidateSet('Capture', 'Snapshot', 'TapId', 'TapText', 'TapDescription')][string]$Action = 'Snapshot',
    [string]$Value = '',
    [string]$Name = 'current'
)
$ErrorActionPreference = 'Stop'
$adb = 'C:\Users\EUNSU\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$serial = 'R3CM7068Y6P'
$xmlPath = Join-Path $PSScriptRoot 'current-ui.xml'
New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot 'screens') -Force | Out-Null
if ($Action -ne 'Capture') {
    & $adb -s $serial shell rm -f /sdcard/td2-multi-ui.xml
    $dumpResult = & $adb -s $serial shell uiautomator dump --compressed /sdcard/td2-multi-ui.xml 2>&1
    if ($LASTEXITCODE -ne 0 -or $dumpResult -notmatch 'UI hierchary dumped') {
        throw "Fresh UI dump failed: $dumpResult"
    }
    & $adb -s $serial pull /sdcard/td2-multi-ui.xml $xmlPath 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'UI dump pull failed' }
}
if ($Action -ne 'Snapshot' -and $Action -ne 'Capture') {
    [xml]$tree = Get-Content -Raw -Encoding UTF8 -LiteralPath $xmlPath
    $target = $tree.SelectNodes('//node') | Where-Object {
        $node = $_
        switch ($Action) {
            'TapId' { $node.'resource-id' -eq "kr.co.todoc.cochlear.implant.ota:id/$Value" }
            'TapText' { $node.text -eq $Value }
            'TapDescription' { $node.'content-desc' -eq $Value }
        }
    } | Where-Object { $_.bounds -ne '[0,0][0,0]' } | Select-Object -First 1
    if (!$target) { throw "Visible target not found: $Action $Value" }
    $bounds = [regex]::Matches($target.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    & $adb -s $serial shell input tap ([int](($bounds[0] + $bounds[2]) / 2)) ([int](($bounds[1] + $bounds[3]) / 2))
    Start-Sleep -Milliseconds 450
}
& $adb -s $serial shell screencap -p /sdcard/td2-multi-native.png
& $adb -s $serial pull /sdcard/td2-multi-native.png (Join-Path $PSScriptRoot "screens/$Name.png") 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Screenshot pull failed' }
