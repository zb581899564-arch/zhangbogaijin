[CmdletBinding()]
param(
  [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path,
  [string]$OutputRoot = (Join-Path $PSScriptRoot '..\local-50k')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path $ProjectRoot).Path
$AuditRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$OutputRoot = [IO.Path]::GetFullPath($OutputRoot)
$Source = Join-Path $AuditRoot 'diagnostic-runner\src\v35audit\A2A3QpDiagnosticRunner.java'
$Classes = Join-Path $AuditRoot 'diagnostic-runner\classes'
$Jar = Join-Path $ProjectRoot 'java-jmetal58\jmetal-exec\target\jmetal-exec-5.8-jar-with-dependencies.jar'
$SnapshotRoot = Join-Path $ProjectRoot 'docs\evidence\V35-FORMAL-MANIFEST\initial-populations\100_2_3_1'

foreach ($path in @($Source, $Jar, $SnapshotRoot)) {
  if (-not (Test-Path -LiteralPath $path)) { throw "Missing diagnostic input: $path" }
}
New-Item -ItemType Directory -Force -Path $Classes, $OutputRoot | Out-Null

# Compile only the sidecar source under this audit directory.  No repository
# Java source, Jar, PDDR, ROADMAP, or AGENTS file is changed.
& javac -encoding UTF-8 -cp $Jar -d $Classes $Source
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$ClassPath = "$Classes;$Jar"
$seeds = @(20260822, 20260823, 20260824)
$arms = @('A2', 'A3')
foreach ($arm in $arms) {
  foreach ($seed in $seeds) {
    $snapshot = Join-Path $SnapshotRoot "seed-$seed.fourvec"
    $output = Join-Path (Join-Path $OutputRoot 'raw') "$arm\seed-$seed"
    & java -cp $ClassPath v35audit.A2A3QpDiagnosticRunner `
      --project-root $ProjectRoot --arm $arm --seed $seed `
      --snapshot $snapshot --output $output
    if ($LASTEXITCODE -ne 0) {
      throw "Diagnostic run failed: arm=$arm seed=$seed exit=$LASTEXITCODE"
    }
  }
}

Write-Output "LOCAL_50K_DIAGNOSTICS_COMPLETED output=$OutputRoot"
