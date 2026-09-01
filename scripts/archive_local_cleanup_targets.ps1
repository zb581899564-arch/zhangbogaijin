param(
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$sandboxArchiveRoot = Join-Path $ArchiveRoot 'local-sandboxes'
$manifestRoot = Join-Path $ArchiveRoot 'manifests'
New-Item -ItemType Directory -Path $sandboxArchiveRoot -Force | Out-Null
New-Item -ItemType Directory -Path $manifestRoot -Force | Out-Null

$targets = @(
  'E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-20260823',
  'E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-byte-20260823',
  'E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-regression-sandbox-lf-20260823'
)

$records = New-Object System.Collections.Generic.List[object]
foreach ($target in $targets) {
  if (-not (Test-Path -LiteralPath $target -PathType Container)) {
    $records.Add([pscustomobject]@{target=$target;status='MISSING';fileCount=0;sourceBytes=0;archiveBytes=0;archiveSha256='';archivePath=''})
    continue
  }
  $resolved = (Resolve-Path -LiteralPath $target).Path
  $expectedParent = (Resolve-Path -LiteralPath 'E:\学习\李明哲-毕业材料').Path
  if (-not $resolved.StartsWith($expectedParent + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Target escaped expected parent: $resolved"
  }
  $leaf = Split-Path -Leaf $resolved
  $parent = Split-Path -Parent $resolved
  $files = @(Get-ChildItem -LiteralPath $resolved -Recurse -File -Force)
  $sourceBytes = [long](($files | Measure-Object Length -Sum).Sum)
  $sourceManifest = Join-Path $manifestRoot ($leaf + '.source-sha256.tsv')
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("relativePath`tbytes`tsha256")
  foreach ($file in ($files | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($resolved.Length + 1).Replace('\','/')
    $lines.Add(($relative, $file.Length, (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -join "`t"))
  }
  [System.IO.File]::WriteAllLines($sourceManifest, $lines, $utf8)

  $archive = Join-Path $sandboxArchiveRoot ($leaf + '.tar.gz')
  $partial = $archive + '.partial'
  if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) {
    if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial -Force }
    & tar.exe -C $parent -czf $partial $leaf
    if ($LASTEXITCODE -ne 0) { throw "tar failed for $resolved" }
    Move-Item -LiteralPath $partial -Destination $archive
  }
  $listed = @(& tar.exe -tzf $archive | Where-Object { $_ -and -not $_.EndsWith('/') })
  if ($LASTEXITCODE -ne 0) { throw "tar verification failed for $archive" }
  if ($listed.Count -ne $files.Count) {
    throw "archive file-count mismatch for ${resolved}: source=$($files.Count) archive=$($listed.Count)"
  }
  $archiveItem = Get-Item -LiteralPath $archive
  $records.Add([pscustomobject]@{
    target=$resolved
    status='VERIFIED_FULL_ARCHIVE'
    fileCount=$files.Count
    sourceBytes=$sourceBytes
    archiveBytes=$archiveItem.Length
    archiveSha256=(Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    archivePath=$archive
  })
}

$csv = $records | ConvertTo-Csv -NoTypeInformation
[System.IO.File]::WriteAllLines((Join-Path $manifestRoot 'local-sandbox-archive-manifest.csv'), $csv, $utf8)
