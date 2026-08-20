param(
  [Parameter(Mandatory=$true)][string]$ProjectRoot,
  [Parameter(Mandatory=$true)][string]$EvidenceRoot
)
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path -LiteralPath $ProjectRoot).Path
$evidence = (Resolve-Path -LiteralPath $EvidenceRoot).Path
$tempParent = Join-Path $project '.codex-temp\p82-replay-100'
if (Test-Path -LiteralPath $tempParent) { throw "Replay temp already exists: $tempParent" }
New-Item -ItemType Directory -Path $tempParent | Out-Null
$jar = Join-Path $project 'java-jmetal58\jmetal-exec\target\jmetal-exec-5.8-jar-with-dependencies.jar'
$files = @(
  '05_one_particle_evolution\trace_summary.properties',
  '05_one_particle_evolution\qg_events.log',
  '05_one_particle_evolution\qp_tracked_lineage_events.log',
  '05_one_particle_evolution\cfvf_tracked_lineage_events.log',
  '05_one_particle_evolution\dual_q_events.log',
  '06_local_search\cata_tracked_lineage_events.log',
  '07_environment_selection\pddr_events.log',
  '07_environment_selection\lineage_events.log',
  '07_environment_selection\final_population.csv'
)
$baseline = @{}
foreach ($file in $files) {
  $baseline[$file] = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $evidence $file)).Hash
}
$rows = New-Object System.Collections.Generic.List[object]
for ($iteration = 3; $iteration -le 100; $iteration++) {
  $target = Join-Path $tempParent ("run-{0:D3}" -f $iteration)
  New-Item -ItemType Directory -Path $target | Out-Null
  & java -cp $jar org.uma.jmetal.runner.lc_psode.ZhangBoCanonicalEvolutionTraceRunner `
      --project-root $project --evidence-root $target *> (Join-Path $target 'console.log')
  if ($LASTEXITCODE -ne 0) { throw "Replay iteration $iteration failed" }
  foreach ($file in $files) {
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $target $file)).Hash
    $same = $actual -eq $baseline[$file]
    $rows.Add([pscustomobject]@{Iteration=$iteration; File=$file; Same=$same; Hash=$actual})
    if (-not $same) { throw "Replay drift at iteration $iteration file $file" }
  }
  $resolvedTarget = (Resolve-Path -LiteralPath $target).Path
  if (-not $resolvedTarget.StartsWith($tempParent, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe replay cleanup target: $resolvedTarget"
  }
  Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
  if ($iteration % 10 -eq 0) { Write-Output "REPLAY_PROGRESS iteration=$iteration" }
}
$output = Join-Path $evidence '05_one_particle_evolution\replay_100_hash_audit.csv'
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath $output
$summary = @(
  'schema=zhangbo-i1-evolution-replay-v1',
  'existingVerifiedRuns=2',
  'additionalVerifiedRuns=98',
  'totalVerifiedRuns=100',
  'coreFilesPerRun=9',
  'hashComparisons=882',
  'allMatch=true'
) -join "`n"
$summary += "`n"
[System.IO.File]::WriteAllText(
  (Join-Path $evidence '05_one_particle_evolution\replay_100_summary.properties'),
  $summary, (New-Object System.Text.UTF8Encoding($false)))
$resolvedParent = (Resolve-Path -LiteralPath $tempParent).Path
if (-not $resolvedParent.StartsWith((Join-Path $project '.codex-temp'), [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Unsafe replay parent cleanup target: $resolvedParent"
}
Remove-Item -LiteralPath $resolvedParent -Recurse -Force
Write-Output 'REPLAY_100_COMPLETED allMatch=true'
