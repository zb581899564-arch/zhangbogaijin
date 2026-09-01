param(
  [string]$ProjectRoot = 'E:\学习\李明哲-毕业材料\张博改进',
  [string]$MasterRoot = 'E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$frozenJar = 'E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823\java-jmetal58\jmetal-exec\target\jmetal-exec-5.8-jar-with-dependencies.jar'
$expectedJarHash = '8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9'
if ((Get-FileHash -LiteralPath $frozenJar -Algorithm SHA256).Hash -ne $expectedJarHash) {
  throw 'Frozen formal jar hash changed; rebuildable cleanup refused'
}

$targets = @(
  'java-jmetal58\jmetal-core\target',
  'java-jmetal58\jmetal-exec\target',
  'java-jmetal58\jmetal-algorithm\target',
  'java-jmetal58\jmetal-problem\target',
  'java-jmetal58\tool\target',
  'java-jmetal58\target',
  '.codex-temp',
  '.codex-temp-doe1-class-patch',
  '.codex-temp-doe1-heldout-smoke',
  '.codex-temp-doe1-heldout-smoke2',
  '.codex-temp-doe1-heldout-smoke3',
  '.codex-temp-doe1-observer-v2b',
  '.codex-temp-doe1-observer-v2c'
)
$knownBytes = @{
  'java-jmetal58\jmetal-core\target' = 216746634
  'java-jmetal58\jmetal-exec\target' = 58520443
  'java-jmetal58\jmetal-algorithm\target' = 142198332
  'java-jmetal58\jmetal-problem\target' = 95341809
  'java-jmetal58\tool\target' = 314897
  'java-jmetal58\target' = 11261
  '.codex-temp' = 63203277
  '.codex-temp-doe1-class-patch' = 447817
  '.codex-temp-doe1-heldout-smoke' = 11614
  '.codex-temp-doe1-heldout-smoke2' = 11622
  '.codex-temp-doe1-heldout-smoke3' = 11624
  '.codex-temp-doe1-observer-v2b' = 3348
  '.codex-temp-doe1-observer-v2c' = 4857
}

$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$rows = New-Object System.Collections.Generic.List[object]
foreach ($relative in $targets) {
  $path = Join-Path $root $relative
  $targetId = 'REBUILDABLE_' + ($relative -replace '[^A-Za-z0-9]+','_').Trim('_')
  if (-not (Test-Path -LiteralPath $path -PathType Container)) {
    if ($knownBytes.ContainsKey($relative)) {
      $rows.Add([pscustomobject]@{
        targetId=$targetId
        host='local'
        path=$path
        bytesBefore=$knownBytes[$relative]
        archivePath=$frozenJar
        archiveSha256=$expectedJarHash
        result='DELETED_REBUILDABLE_OUTPUT_POSTHOC_ABSENCE_VERIFIED'
        recoverable='true'
      })
    }
    continue
  }
  $resolved = (Resolve-Path -LiteralPath $path).Path
  if (-not $resolved.StartsWith($root + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Rebuildable target escaped project root: $resolved"
  }
  $bytes = [long](Get-ChildItem -LiteralPath $resolved -Recurse -File -Force | Measure-Object Length -Sum).Sum
  Remove-Item -LiteralPath $resolved -Recurse -Force
  if (Test-Path -LiteralPath $resolved) { throw "Rebuildable target still exists: $resolved" }
  $rows.Add([pscustomobject]@{
    targetId=$targetId
    host='local'
    path=$resolved
    bytesBefore=$bytes
    archivePath=$frozenJar
    archiveSha256=$expectedJarHash
    result='DELETED_REBUILDABLE_OUTPUT'
    recoverable='true'
  })
}

$accidentalManifest = 'D:\CodexTemp\V35-DOE1-Acceptance\full-evidence-sha256.saved.tsv'
if (Test-Path -LiteralPath $accidentalManifest -PathType Leaf) {
  $resolvedAccidental = (Resolve-Path -LiteralPath $accidentalManifest).Path
  $expectedAccidentalParent = (Resolve-Path -LiteralPath 'D:\CodexTemp\V35-DOE1-Acceptance').Path
  if ((Split-Path -Parent $resolvedAccidental) -ne $expectedAccidentalParent) {
    throw "Accidental temporary manifest escaped expected parent: $resolvedAccidental"
  }
  $accidentalBytes = (Get-Item -LiteralPath $resolvedAccidental).Length
  Remove-Item -LiteralPath $resolvedAccidental -Force
  $rows.Add([pscustomobject]@{
    targetId='REBUILDABLE_ACCIDENTAL_FULL_EVIDENCE_MANIFEST'
    host='local'
    path=$resolvedAccidental
    bytesBefore=$accidentalBytes
    archivePath='G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\manifests\development-runs-20260822.source-sha256.tsv'
    archiveSha256=(Get-FileHash -LiteralPath 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\manifests\development-runs-20260822.source-sha256.tsv' -Algorithm SHA256).Hash
    result='DELETED_AGENT_CREATED_TEMPORARY_FILE'
    recoverable='true'
  })
}

$cleanupPath = Join-Path $MasterRoot 'cleanup-execution.csv'
$existing = @(Import-Csv -LiteralPath $cleanupPath)
$combined = @($existing) + @($rows | ForEach-Object { $_ }) |
  Group-Object targetId,path |
  ForEach-Object { $_.Group[-1] }
$csv = $combined | ConvertTo-Csv -NoTypeInformation
[System.IO.File]::WriteAllLines($cleanupPath, $csv, $utf8)
