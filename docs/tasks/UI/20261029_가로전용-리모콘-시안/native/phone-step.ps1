param([ValidateSet('Snapshot','TapId','TapText','TapDescription')][string]$Action='Snapshot',[string]$Value='',[string]$Name='current')
$ErrorActionPreference='Stop'
$adb='C:\Users\EUNSU\AppData\Local\Android\Sdk\platform-tools\adb.exe'
if($Action -ne 'Snapshot') {
 & $adb -s R3CM7068Y6P shell uiautomator dump --compressed /sdcard/td2-ui.xml | Out-Null
 & $adb -s R3CM7068Y6P pull /sdcard/td2-ui.xml (Join-Path $PSScriptRoot 'current-ui.xml') 2>&1 | Out-Null
 [xml]$tree=Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot 'current-ui.xml')
 $nodes=$tree.SelectNodes('//node') | Where-Object {$node=$_;switch($Action){'TapId' {$node.'resource-id' -eq "kr.co.todoc.cochlear.implant.ota:id/$Value"} 'TapText' {$node.text -eq $Value} 'TapDescription' {$node.'content-desc' -eq $Value}}} | Where-Object {$_.bounds -ne '[0,0][0,0]'}
 $target=$nodes | Select-Object -First 1
 if(!$target){throw "Visible target not found: $Action $Value"}
 $numbers=[regex]::Matches($target.bounds,'\d+') | ForEach-Object {[int]$_.Value}
 & $adb -s R3CM7068Y6P shell input tap ([int](($numbers[0]+$numbers[2])/2)) ([int](($numbers[1]+$numbers[3])/2))
 Start-Sleep -Milliseconds 450
}
& $adb -s R3CM7068Y6P shell screencap -p /sdcard/td2-native.png
& $adb -s R3CM7068Y6P pull /sdcard/td2-native.png (Join-Path $PSScriptRoot "screens/$Name.png") 2>&1 | Out-Null
