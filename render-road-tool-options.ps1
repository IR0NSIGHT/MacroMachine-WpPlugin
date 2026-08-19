$ErrorActionPreference = "Stop"

mvn -q -DskipTests compile
mvn -q dependency:build-classpath "-Dmdep.outputFile=target\path-tool-classpath.txt"

$outputDirectory = Join-Path (Get-Location) "target\screenshots"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$classpath = "target\classes;$(Get-Content -Raw 'target\path-tool-classpath.txt')"
$mainClass = "org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathToolScreenshot"

& java -cp $classpath $mainClass (Join-Path $outputDirectory "road-tool-options-default.png") "default"
& java -cp $classpath $mainClass (Join-Path $outputDirectory "road-tool-options-dropdown.png") "dropdown"

Write-Host "Screenshots written to $outputDirectory"
