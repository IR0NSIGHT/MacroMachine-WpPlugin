#Requires -Version 5.1
[CmdletBinding()]
param(
    [string] $Destination = (Join-Path $env:LOCALAPPDATA 'MacroMachine\tools\inno-6.7.4')
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$url = 'https://github.com/jrsoftware/issrc/releases/download/is-6_7_4/innosetup-6.7.4.exe'
$sha256 = 'A549929319F9120716677C685535CA4671E4B937B5FDC87E2585F32D8EB30E8C'
$destinationPath = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $destinationPath) { throw "Destination already exists: $destinationPath" }
$temp = Join-Path ([IO.Path]::GetTempPath()) ('inno-bootstrap-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$process = $null
try {
    $download = Join-Path $temp 'inno.exe'
    Invoke-WebRequest -Uri $url -OutFile $download -UseBasicParsing
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash -cne $sha256) { throw 'Inno Setup download checksum mismatch.' }
    $log = Join-Path $temp 'setup.log'
    $process = Start-Process -FilePath $download -ArgumentList @('/PORTABLE=1', '/CURRENTUSER', '/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', '/NOICONS', "/DIR=`"$destinationPath`"", "/LOG=`"$log`"") -Wait -PassThru
    if ($process.ExitCode -ne 0) { throw "Inno Setup bootstrap failed (exit $($process.ExitCode)); log: $log" }
    $iscc = Join-Path $destinationPath 'ISCC.exe'
    if (-not (Test-Path -LiteralPath $iscc -PathType Leaf)) { throw 'Installed ISCC.exe was not found.' }
    @('#if PREPROCVER != 0x06070400', '#error Wrong compiler version', '#endif', '[Setup]', 'AppName=Version probe', 'AppVersion=1', 'DefaultDirName={tmp}\probe', 'Output=no') | & $iscc /Q -
    if ($LASTEXITCODE -ne 0) { throw 'Installed ISCC version is not 6.7.4.' }
    Write-Output $iscc
}
finally {
    if ($process -and $process.ExitCode -ne 0) { Write-Warning "Bootstrap log retained at $log" } else { Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue }
}
