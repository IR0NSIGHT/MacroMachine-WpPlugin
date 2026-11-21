#Requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $PluginJar,

    [Parameter(Mandatory = $true)]
    [string] $Version,

    [Parameter(Mandatory = $true)]
    [string] $IsccPath,

    [string] $OutputDirectory = 'target/installer'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($Version -cnotmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$' -or
    @($Version.Split('.') | Where-Object { $_.Length -gt 5 -or [int]$_ -gt 65535 }).Count -gt 0) {
    throw 'Version must contain three numeric components, for example 0.4.18.'
}

$jar = Get-Item -LiteralPath $PluginJar -ErrorAction Stop
if ($jar.PSIsContainer -or $jar.Name -cne "Macro-Machine-Plugin-$Version.jar") {
    throw "Expected the final shaded JAR named Macro-Machine-Plugin-$Version.jar."
}

$compiler = Get-Item -LiteralPath $IsccPath -ErrorAction Stop
if ($compiler.PSIsContainer -or $compiler.Name -ine 'ISCC.exe') {
    throw 'IsccPath must point to the Inno Setup 6.7.4 ISCC.exe compiler.'
}
@('#if PREPROCVER != 0x06070400', '#error Wrong compiler version', '#endif', '[Setup]', 'AppName=Version probe', 'AppVersion=1', 'DefaultDirName={tmp}\probe', 'Output=no') | & $compiler.FullName /Q -
if ($LASTEXITCODE -ne 0) { throw 'IsccPath must point to the Inno Setup 6.7.4 ISCC.exe compiler.' }

$script = Join-Path $PSScriptRoot 'MacroMachine.iss'

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
try {
    $descriptors = @($archive.Entries | Where-Object { $_.FullName -ceq 'org.pepsoft.worldpainter.plugins' })
    if ($descriptors.Count -ne 1) {
        throw 'The plugin JAR must contain exactly one WorldPainter plugin descriptor.'
    }

    $reader = [System.IO.StreamReader]::new($descriptors[0].Open())
    try {
        $descriptor = $reader.ReadToEnd() | ConvertFrom-Json
    }
    finally {
        $reader.Dispose()
    }

    if ($descriptor.name -cne 'Macro-Machine-Plugin' -or $descriptor.version -cne $Version -or
        @($descriptor.classes | Where-Object { $_ -ceq 'org.ironsight.wpplugin.macromachine.MacroMachinePlugin' }).Count -ne 1) {
        throw 'The WorldPainter plugin descriptor does not match the expected plugin and version.'
    }

    $classes = @($archive.Entries | Where-Object {
        $_.FullName -ceq 'org/ironsight/wpplugin/macromachine/MacroMachinePlugin.class'
    })
    if ($classes.Count -ne 1) {
        throw 'The plugin JAR must contain exactly one MacroMachinePlugin class.'
    }
}
finally {
    $archive.Dispose()
}

$inputHash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$expectedOutput = Join-Path $outputPath "MacroMachine-Setup-$Version.exe"
if (Test-Path -LiteralPath $expectedOutput) {
    throw "Output already exists: $expectedOutput. Choose an empty output directory."
}

$stage = Join-Path $outputPath ('.build-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage | Out-Null
try {
    & $compiler.FullName "/DPluginJar=$($jar.FullName)" "/DAppVersion=$Version" "/DOutputDirectory=$stage" $script
    if ($LASTEXITCODE -ne 0) { throw "Inno Setup compilation failed with exit code $LASTEXITCODE." }
    $stagedInstaller = Join-Path $stage "MacroMachine-Setup-$Version.exe"
    if (-not (Test-Path -LiteralPath $stagedInstaller -PathType Leaf)) {
        throw 'Inno Setup did not produce the expected installer.'
    }
    if ((Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash -cne $inputHash) {
        throw 'The plugin JAR changed while the installer was being built; do not publish these artifacts.'
    }
    $info = (Get-Item -LiteralPath $stagedInstaller).VersionInfo
    $numericVersion = "$Version.0"
    if ($info.FileVersion.Trim() -cne $numericVersion -or $info.ProductVersion.Trim() -cne $numericVersion) {
        throw "Installer version metadata does not match $numericVersion."
    }
    Move-Item -LiteralPath $stagedInstaller -Destination $expectedOutput
}
finally {
    Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Plugin SHA-256: $inputHash"
Write-Host "Installer: $expectedOutput"
