param(
  [string]$MasterRoot = 'E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER',
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$sandboxManifest = Import-Csv -LiteralPath (Join-Path $ArchiveRoot 'manifests\local-sandbox-archive-manifest.csv')
$sandboxMap = @{}
foreach ($row in $sandboxManifest) { $sandboxMap[$row.target] = $row }

$targets = @(
  [pscustomobject]@{
    id='LOCAL_SANDBOX_1'
    path='E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-20260823'
    parent='E:\学习\李明哲-毕业材料'
    archive='G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\local-sandboxes\_isolated-v35-final-doe1-regression-sandbox-20260823.tar.gz'
    expected='sandbox'
  },
  [pscustomobject]@{
    id='LOCAL_SANDBOX_BYTE'
    path='E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-byte-20260823'
    parent='E:\学习\李明哲-毕业材料'
    archive='G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\local-sandboxes\_isolated-v35-final-doe1-regression-sandbox-byte-20260823.tar.gz'
    expected='sandbox'
  },
  [pscustomobject]@{
    id='LOCAL_SANDBOX_LF'
    path='E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-lf-20260823'
    parent='E:\学习\李明哲-毕业材料'
    archive='G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\local-sandboxes\_isolated-v35-final-doe1-regression-sandbox-lf-20260823.tar.gz'
    expected='sandbox'
  },
  [pscustomobject]@{
    id='DOE1_EXTRACTED'
    path='D:\CodexTemp\V35-DOE1-Acceptance\extracted'
    parent='D:\CodexTemp\V35-DOE1-Acceptance'
    archive='G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\local-primary\development-runs-20260822.tar.gz'
    expected='doe'
  }
)

$records = New-Object System.Collections.Generic.List[object]
foreach ($target in $targets) {
  if (-not (Test-Path -LiteralPath $target.path -PathType Container)) {
    $records.Add([pscustomobject]@{targetId=$target.id;host='local';path=$target.path;bytesBefore=0;archivePath=$target.archive;archiveSha256='';result='ALREADY_ABSENT';recoverable='true'})
    continue
  }
  $resolved = (Resolve-Path -LiteralPath $target.path).Path
  $parent = (Resolve-Path -LiteralPath $target.parent).Path
  if (-not $resolved.StartsWith($parent + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing cleanup outside intended parent: $resolved"
  }
  if (-not (Test-Path -LiteralPath $target.archive -PathType Leaf)) {
    throw "Required archive missing: $($target.archive)"
  }
  $archiveSha = (Get-FileHash -LiteralPath $target.archive -Algorithm SHA256).Hash
  $bytesBefore = [long](0)
  if ($target.expected -eq 'sandbox') {
    $meta = $sandboxMap[$resolved]
    if ($null -eq $meta -or $meta.status -ne 'VERIFIED_FULL_ARCHIVE') {
      throw "Sandbox archive was not verified: $resolved"
    }
    if ($archiveSha -ne $meta.archiveSha256) {
      throw "Sandbox archive hash drift: $resolved"
    }
    $bytesBefore = [long]$meta.sourceBytes
  } else {
    $validation = Get-Content -LiteralPath 'D:\CodexTemp\V35-DOE1-Acceptance\full-sha256-validation.tsv'
    if ($validation -notcontains 'verified=2035' -or $validation -notcontains 'failures=0') {
      throw 'DOE1 full archive verification marker is invalid'
    }
    $bytesBefore = [long](Get-ChildItem -LiteralPath $resolved -Recurse -File -Force | Measure-Object Length -Sum).Sum
  }
  Remove-Item -LiteralPath $resolved -Recurse -Force
  if (Test-Path -LiteralPath $resolved) { throw "Cleanup target still exists: $resolved" }
  $records.Add([pscustomobject]@{targetId=$target.id;host='local';path=$resolved;bytesBefore=$bytesBefore;archivePath=$target.archive;archiveSha256=$archiveSha;result='DELETED_AFTER_VERIFIED_ARCHIVE';recoverable='true'})
}

$csv = $records | ConvertTo-Csv -NoTypeInformation
[System.IO.File]::WriteAllLines((Join-Path $MasterRoot 'cleanup-execution.csv'), $csv, $utf8)
