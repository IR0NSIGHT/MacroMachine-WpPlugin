#Requires -Version 5.1
# Installs into a fake WorldPainter folder (per-user, no admin) and checks the result.
# Uses its own AppId and uninstaller folder, so a real MacroMachine install is never touched.
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string] $IsccPath,
    [Parameter(Mandatory = $true)][string] $PluginJar,
    [Parameter(Mandatory = $true)][string] $Version
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Join-Path ([IO.Path]::GetTempPath()) ('macromachine-setup-test-' + [guid]::NewGuid().ToString('N'))
$wp = Join-Path $root 'World Painter'          # space on purpose
$plugins = Join-Path $root 'plugins'
$uninstallDir = Join-Path $root 'uninstaller'
$guid = '3EA75D52-9B20-4051-A96F-71C8F76D9BF1'
$uninstallKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\{$guid}_is1"
if (Test-Path $uninstallKey) { throw "Test registration already exists: $uninstallKey. Remove it first." }

function Fail([string] $Message) { throw "$Message (diagnostics: $root)" }

function New-FakeWorldPainter([string] $JavaVersion) {
    Remove-Item -LiteralPath $wp, $plugins -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path "$wp\.install4j", "$wp\lib", "$root\jdk\bin", $plugins -Force | Out-Null
    [IO.File]::WriteAllText("$wp\worldpainter.exe", 'fake, never executed')
    [IO.File]::WriteAllText("$root\jdk\release", "IMPLEMENTOR=`"Test`"`nJAVA_VERSION=`"$JavaVersion`"`n")
    [IO.File]::WriteAllText("$wp\.install4j\inst_jre.cfg", "$root\jdk`n")
    [IO.File]::WriteAllText("$wp\worldpainter.vmoptions", $script:originalOptions, [Text.Encoding]::ASCII)
    [IO.File]::WriteAllText("$plugins\Macro-Machine-Plugin-0.4.14.jar", 'old')
    [IO.File]::WriteAllText("$plugins\macromachine-0.4.16.jar", 'old renamed')
    [IO.File]::WriteAllText("$plugins\SomeOtherPlugin.jar", 'unrelated')
}

function Invoke-Setup([string] $Exe, [string] $Log, [string[]] $Extra = @()) {
    $argList = @('/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', "/LOG=`"$root\$Log.log`"") + $Extra
    $p = Start-Process -FilePath $Exe -ArgumentList $argList -PassThru
    if (-not $p.WaitForExit(120000)) { $p.Kill(); Fail "$Log timed out" }
    return $p.ExitCode
}

function Install([string] $Log) {
    Invoke-Setup $setup $Log @('/CURRENTUSER', "/DIR=`"$wp`"", "/MMPLUGINS=`"$plugins`"")
}

$script:originalOptions = "# comment kept`r`n-Xmx2G`r`n-Dsome.option=1"   # no trailing newline on purpose
$jarDir = "$plugins\MacroMachine-Plugin"
$installed = "$jarDir\Macro-Machine-Plugin-$Version.jar"
$line = "-classpath/a $installed"
$jar = (Get-Item -LiteralPath $PluginJar).FullName

New-Item -ItemType Directory -Path $root | Out-Null
& $IsccPath /Q "/DAppVersion=$Version" "/DPluginJar=$jar" "/DOutputDirectory=$root" `
    "/DAppGuid=$guid" "/DUninstallDir=$uninstallDir" (Join-Path $PSScriptRoot '..\MacroMachine.iss')
if ($LASTEXITCODE -ne 0) { Fail 'Compilation failed' }
$setup = Join-Path $root "MacroMachine-Setup-$Version.exe"

try {
    # --- Java too old: nothing may change ---
    New-FakeWorldPainter '17.0.2'
    if ((Install 'java17') -eq 0) { Fail 'Setup succeeded with Java 17' }
    if ([IO.File]::ReadAllText("$wp\worldpainter.vmoptions") -cne $originalOptions) { Fail 'Java 17: vmoptions changed' }
    if (Test-Path $installed) { Fail 'Java 17: plugin copied' }
    if (-not (Test-Path "$plugins\Macro-Machine-Plugin-0.4.14.jar")) { Fail 'Java 17: old JAR disabled' }

    # --- Install over an older setup version (old JAR in our subfolder) ---
    New-FakeWorldPainter '21.0.6'
    New-Item -ItemType Directory -Path $jarDir | Out-Null
    [IO.File]::WriteAllText("$jarDir\Macro-Machine-Plugin-0.0.1.jar", 'previous setup version')
    if ((Install 'install') -ne 0) { Fail 'Install failed' }
    if ((Get-FileHash $installed).Hash -ne (Get-FileHash $jar).Hash) { Fail 'Installed JAR differs' }
    $expected = $originalOptions + "`r`n" + $line + "`r`n"
    if ([IO.File]::ReadAllText("$wp\worldpainter.vmoptions") -cne $expected) { Fail 'vmoptions not as expected after install' }
    foreach ($old in 'Macro-Machine-Plugin-0.4.14.jar', 'macromachine-0.4.16.jar') {
        if ((Test-Path "$plugins\$old") -or -not (Test-Path "$plugins\$old.ignored")) { Fail "Old JAR not disabled: $old" }
    }
    if (-not (Test-Path "$plugins\SomeOtherPlugin.jar")) { Fail 'Unrelated plugin touched' }
    if (Test-Path "$jarDir\Macro-Machine-Plugin-0.0.1.jar") { Fail 'Previous version JAR not removed' }

    # --- Repair / rerun is idempotent ---
    # A stale line from an earlier location must be replaced, not duplicated.
    Remove-Item -LiteralPath $installed
    [IO.File]::WriteAllText("$wp\worldpainter.vmoptions", $originalOptions + "`r`n-classpath/a C:\old\lib\macromachine\macromachine.jar`r`n")
    if ((Install 'repair') -ne 0) { Fail 'Repair failed' }
    if (-not (Test-Path $installed)) { Fail 'Repair did not restore the JAR' }
    if ([IO.File]::ReadAllText("$wp\worldpainter.vmoptions") -cne $expected) { Fail 'Repair changed vmoptions' }

    # --- Uninstall keeps later user edits ---
    [IO.File]::AppendAllText("$wp\worldpainter.vmoptions", "-Duser.later=true`r`n")
    $uninstaller = Get-ChildItem -LiteralPath $uninstallDir -Filter 'unins*.exe' | Select-Object -First 1
    if (-not $uninstaller) { Fail 'Uninstaller missing' }
    if ((Invoke-Setup $uninstaller.FullName 'uninstall') -ne 0) { Fail 'Uninstall failed' }
    $after = [IO.File]::ReadAllText("$wp\worldpainter.vmoptions")
    if ($after -cne ($originalOptions + "`r`n-Duser.later=true`r`n")) { Fail "Uninstall left wrong vmoptions: $after" }
    if (Test-Path $installed) { Fail 'Uninstall left the JAR' }
    if (Test-Path $uninstallKey) { Fail 'Uninstall left the registration' }

    # --- Uninstall still works after WorldPainter was deleted ---
    New-FakeWorldPainter '21.0.6'
    if ((Install 'install2') -ne 0) { Fail 'Second install failed' }
    Remove-Item -LiteralPath $wp -Recurse -Force
    $uninstaller = Get-ChildItem -LiteralPath $uninstallDir -Filter 'unins*.exe' | Select-Object -First 1
    if ((Invoke-Setup $uninstaller.FullName 'uninstall2') -ne 0) { Fail 'Uninstall without WorldPainter failed' }
    if (Test-Path $uninstallKey) { Fail 'Orphaned registration after WorldPainter removal' }

    Write-Host 'Installer tests passed.'
    Remove-Item -LiteralPath $root -Recurse -Force -ErrorAction SilentlyContinue
}
catch {
    if (Test-Path $uninstallKey) { Remove-Item $uninstallKey -Recurse -Force }
    throw
}
