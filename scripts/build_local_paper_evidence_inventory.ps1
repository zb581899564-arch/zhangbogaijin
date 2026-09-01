param(
  [string]$ProjectRoot = 'E:\学习\李明哲-毕业材料\张博改进',
  [string]$OutputRoot = 'E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER\inventory'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$roots = @(
  (Join-Path $ProjectRoot 'docs\evidence'),
  (Join-Path $ProjectRoot 'paper_evidence'),
  'E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823',
  'D:\CodexTemp\V35-DOE1-Acceptance',
  'D:\CodexTemp\V35-DOE1-heldout-audit-20260822'
)
$sourceFiles = @(
  'E:\学习\eswa2026-最新李明哲第四.pdf',
  'E:\学习\李明哲-毕业材料\3.毕业论文\104_2022930913_李明哲.pdf',
  'E:\学习\李明哲-毕业材料\6.第四章小论文\elsarticle\elsarticle-template-num-20250610.pdf',
  'E:\学习\ziliao\v3.5.md',
  'E:\学习\ziliao\HMOPSO_QGS_疲劳_全向量双Q_CA-TA-VNS_综合改进方案_v2.md',
  'E:\学习\ziliao\HMOPSO_Qpbest_认知社会双引导完整设计方案.md',
  'E:\学习\ziliao\基于李明哲HMOPSO-QGS的多技能疲劳恢复与双Q引导改进方案.md',
  'E:\学习\ziliao\李明哲第四章VNS改进方案_代价感知上下文自适应VNS.md'
)

New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null
$rows = New-Object System.Collections.Generic.List[object]

foreach ($root in $roots) {
  if (-not (Test-Path -LiteralPath $root -PathType Container)) { continue }
  Get-ChildItem -LiteralPath $root -Recurse -File -Force -ErrorAction Stop | ForEach-Object {
    $rows.Add([pscustomobject]@{
      host = 'local-windows'
      root = $root
      path = $_.FullName
      bytes = $_.Length
      sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
  }
}
foreach ($path in $sourceFiles) {
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
  $item = Get-Item -LiteralPath $path
  $rows.Add([pscustomobject]@{
    host = 'local-windows'
    root = 'source-material'
    path = $item.FullName
    bytes = $item.Length
    sha256 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash
  })
}

$artifactPath = Join-Path $OutputRoot 'local-artifact-ledger.tsv'
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("host`troot`tpath`tbytes`tsha256")
foreach ($row in ($rows | Sort-Object path -Unique)) {
  $lines.Add(($row.host, $row.root, $row.path, $row.bytes, $row.sha256 -join "`t"))
}
[System.IO.File]::WriteAllLines($artifactPath, $lines, $utf8)

$summary = $rows | Group-Object root | ForEach-Object {
  [pscustomobject]@{
    root = $_.Name
    fileCount = $_.Count
    totalBytes = ($_.Group | Measure-Object bytes -Sum).Sum
  }
}
$summaryCsv = $summary | Sort-Object root | ConvertTo-Csv -NoTypeInformation
[System.IO.File]::WriteAllLines((Join-Path $OutputRoot 'local-root-summary.csv'), $summaryCsv, $utf8)
[System.IO.File]::WriteAllText((Join-Path $OutputRoot 'INVENTORY_COMPLETE'), "complete=true`n", $utf8)
