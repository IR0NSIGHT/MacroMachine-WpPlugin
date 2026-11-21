#Requires -Version 5.1
# Regenerates the installer images from imgs/MacroMachine_Icon.png. Run after changing the icon:
#   .\installer\windows\make-icons.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$source = [Drawing.Image]::FromFile((Resolve-Path (Join-Path $PSScriptRoot '..\..\imgs\MacroMachine_Icon.png')))

# Draws the icon centered on a white canvas. Radius > 0 gives a rounded tile with a transparent corner.
function New-Image([int] $Width, [int] $Height, [double] $Fill, [int] $Radius) {
    $bitmap = New-Object Drawing.Bitmap $Width, $Height, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = 'AntiAlias'; $g.InterpolationMode = 'HighQualityBicubic'; $g.PixelOffsetMode = 'HighQuality'
    if ($Radius -gt 0) {
        $path = New-Object Drawing.Drawing2D.GraphicsPath
        $d = 2 * $Radius
        $path.AddArc(0, 0, $d, $d, 180, 90); $path.AddArc($Width - $d - 1, 0, $d, $d, 270, 90)
        $path.AddArc($Width - $d - 1, $Height - $d - 1, $d, $d, 0, 90); $path.AddArc(0, $Height - $d - 1, $d, $d, 90, 90)
        $path.CloseFigure(); $g.FillPath([Drawing.Brushes]::White, $path)
    } else { $g.Clear([Drawing.Color]::White) }
    $scale = [Math]::Min($Width * $Fill / $source.Width, $Height * $Fill / $source.Height)
    $w = $source.Width * $scale; $h = $source.Height * $scale
    $g.DrawImage($source, [single](($Width - $w) / 2), [single](($Height - $h) / 2), [single]$w, [single]$h)
    $g.Dispose()
    return $bitmap
}

function Get-PngBytes($Bitmap) {
    $stream = New-Object IO.MemoryStream
    $Bitmap.Save($stream, [Drawing.Imaging.ImageFormat]::Png)
    return , $stream.ToArray()
}

# Setup / uninstaller icon: rounded white tile, PNG-compressed ICO entries.
$sizes = 16, 24, 32, 48, 64, 128, 256
$entries = foreach ($size in $sizes) {
    $bitmap = New-Image $size $size 0.82 ([Math]::Max(2, [int]($size / 6)))
    , (Get-PngBytes $bitmap); $bitmap.Dispose()
}
$ico = New-Object IO.MemoryStream
$writer = New-Object IO.BinaryWriter $ico
$writer.Write([uint16]0); $writer.Write([uint16]1); $writer.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $dim = if ($sizes[$i] -ge 256) { 0 } else { $sizes[$i] }
    $writer.Write([byte]$dim); $writer.Write([byte]$dim); $writer.Write([byte]0); $writer.Write([byte]0)
    $writer.Write([uint16]1); $writer.Write([uint16]32)
    $writer.Write([uint32]$entries[$i].Length); $writer.Write([uint32]$offset)
    $offset += $entries[$i].Length
}
foreach ($bytes in $entries) { $writer.Write([byte[]]$bytes) }
[IO.File]::WriteAllBytes((Join-Path $PSScriptRoot 'MacroMachine.ico'), $ico.ToArray())

# Wizard images at 200% size; Inno scales them down.
foreach ($image in @(@{ Name = 'wizard-small.png'; W = 110; H = 110; Fill = 0.9 },
                     @{ Name = 'wizard-large.png'; W = 328; H = 628; Fill = 0.6 })) {
    $bitmap = New-Image $image.W $image.H $image.Fill 0
    $bitmap.Save((Join-Path $PSScriptRoot $image.Name), [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
$source.Dispose()
Write-Host 'Wrote MacroMachine.ico, wizard-small.png, wizard-large.png'
