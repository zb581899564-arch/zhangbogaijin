param(
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$outputRoot = Join-Path $ArchiveRoot 'local-primary'
$manifestRoot = Join-Path $ArchiveRoot 'manifests'
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
New-Item -ItemType Directory -Path $manifestRoot -Force | Out-Null

$targets = @(
  [pscustomobject]@{
    id='final-freeze'
    path='E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823'
  },
  [pscustomobject]@{
    id='current-docs-evidence'
    path='E:\学习\李明哲-毕业材料\张博改进\docs\evidence'
  },
  [pscustomobject]@{
    id='current-paper-evidence'
    path='E:\学习\李明哲-毕业材料\张博改进\paper_evidence'
  }
)

$records = New-Object System.Collections.Generic.List[object]
foreach ($target in $targets) {
  if (-not (Test-Path -LiteralPath $target.path -PathType Container)) {
    throw "Primary evidence target missing: $($target.path)"
  }
  $resolved = (Resolve-Path -LiteralPath $target.path).Path
  $leaf = Split-Path -Leaf $resolved
  $parent = Split-Path -Parent $resolved
  $files = @(Get-ChildItem -LiteralPath $resolved -Recurse -File -Force)
  $sourceBytes = [long](($files | Measure-Object Length -Sum).Sum)
  $sourceManifest = Join-Path $manifestRoot ($target.id + '.source-sha256.tsv')
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("relativePath`tbytes`tsha256")
  foreach ($file in ($files | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($resolved.Length + 1).Replace('\','/')
    $lines.Add(($relative, $file.Length, (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -join "`t"))
  }
  [System.IO.File]::WriteAllLines($sourceManifest, $lines, $utf8)

  $archive = Join-Path $outputRoot ($target.id + '.tar.gz')
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
    id=$target.id
    sourcePath=$resolved
    rootName=$leaf
    status='ARCHIVE_CREATED_FILE_COUNT_VERIFIED'
    fileCount=$files.Count
    sourceBytes=$sourceBytes
    archiveBytes=$archiveItem.Length
    archiveSha256=(Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    archivePath=$archive
    sourceManifest=$sourceManifest
  })
}

$records | Export-Csv -LiteralPath (Join-Path $manifestRoot 'local-primary-archive-manifest.csv') -NoTypeInformation -Encoding UTF8
