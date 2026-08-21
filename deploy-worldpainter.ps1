#Requires -Version 5.1

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    $administrator = [Security.Principal.WindowsBuiltInRole]::Administrator

    if (-not $principal.IsInRole($administrator)) {
        throw 'Run this script from an elevated PowerShell window.'
    }
}

function Get-UniquePath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $Path
    }

    $directory = Split-Path -Parent $Path
    $name = Split-Path -Leaf $Path
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'

    if ($name.EndsWith('.jar.ignored', [StringComparison]::OrdinalIgnoreCase)) {
        $name = $name.Substring(0, $name.Length - '.ignored'.Length)
        return Join-Path $directory "$name.$timestamp.ignored"
    }

    return Join-Path $directory "$name.$timestamp"
}

Assert-Administrator

$repositoryRoot = $PSScriptRoot
$worldPainterRoot = Join-Path $env:APPDATA 'WorldPainter'
$pluginDirectory = Join-Path $worldPainterRoot 'plugins'
$vmOptionsPath = 'C:\Program Files\WorldPainter\worldpainter.vmoptions'
$targetDirectory = Join-Path $repositoryRoot 'target'

if (Get-Process -Name 'worldpainter' -ErrorAction SilentlyContinue) {
    throw 'WorldPainter is running. Close it before deploying the plugin.'
}

if (-not (Test-Path -LiteralPath $vmOptionsPath -PathType Leaf)) {
    throw "WorldPainter VM options file was not found: $vmOptionsPath"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven (mvn) was not found on PATH.'
}

if (-not (Test-Path -LiteralPath $pluginDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $pluginDirectory -Force | Out-Null
}

Push-Location $repositoryRoot
try {
    & mvn --batch-mode --no-transfer-progress clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

$artifacts = @(
    Get-ChildItem -LiteralPath $targetDirectory -Filter 'Macro-Machine-Plugin-*.jar' -File |
        Where-Object { $_.Name -notlike 'original-*' }
)

if ($artifacts.Count -ne 1) {
    throw "Expected exactly one shaded Macro Machine JAR in $targetDirectory, found $($artifacts.Count)."
}

$artifact = $artifacts[0]
$destinationJar = Join-Path $pluginDirectory $artifact.Name

foreach ($jar in @(Get-ChildItem -LiteralPath $pluginDirectory -Filter '*.jar' -File)) {
    $ignoredPath = Get-UniquePath -Path "$($jar.FullName).ignored"
    Rename-Item -LiteralPath $jar.FullName -NewName (Split-Path -Leaf $ignoredPath)
    Write-Host "Disabled $($jar.Name) -> $(Split-Path -Leaf $ignoredPath)"
}

Copy-Item -LiteralPath $artifact.FullName -Destination $destinationJar -Force

$vmOptionsBackup = Get-UniquePath -Path "$vmOptionsPath.backup"
Copy-Item -LiteralPath $vmOptionsPath -Destination $vmOptionsBackup

$vmOptionsText = [IO.File]::ReadAllText($vmOptionsPath)
$lines = @()
if ($vmOptionsText.Trim().Length -gt 0) {
    $lines = @($vmOptionsText.TrimEnd("`r", "`n") -split "`r?`n")
}

# Keep unrelated classpath entries, including the existing WPLayerViewPlugin entry.
$lines = @($lines | Where-Object {
        $_ -notmatch '^\s*-classpath/a\s+.*Macro-Machine-Plugin-.*\.jar\s*$'
    })

$classpathLine = '-classpath/a "' + [IO.Path]::GetFullPath($destinationJar) + '"'
$lines += $classpathLine
$newVmOptionsText = [string]::Join("`r`n", $lines) + "`r`n"
$utf8NoBom = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText($vmOptionsPath, $newVmOptionsText, $utf8NoBom)

Write-Host "Built:      $($artifact.FullName)"
Write-Host "Installed:  $destinationJar"
Write-Host "VM backup:  $vmOptionsBackup"
Write-Host "VM options: $classpathLine"
