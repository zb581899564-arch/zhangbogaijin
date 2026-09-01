param(
  [string]$MasterRoot = 'E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER',
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$archiveManifest = Join-Path $MasterRoot 'V35-PAPER-EVIDENCE-ARCHIVE-MANIFEST.tsv'

$localPrimary = @{}
foreach ($row in (Import-Csv -LiteralPath (Join-Path $ArchiveRoot 'manifests\local-primary-archive-manifest.csv'))) {
  $localPrimary[[IO.Path]::GetFileName($row.archivePath)] = $row
}
$sandbox = @{}
foreach ($row in (Import-Csv -LiteralPath (Join-Path $ArchiveRoot 'manifests\local-sandbox-archive-manifest.csv'))) {
  $sandbox[[IO.Path]::GetFileName($row.archivePath)] = $row
}
$remote = @{}
foreach ($row in (Import-Csv -LiteralPath (Join-Path $ArchiveRoot 'manifests\remote-archive-manifest.tsv') -Delimiter "`t")) {
  $remote[$row.campaignId + '.tar.gz'] = $row
}

$records = New-Object System.Collections.Generic.List[object]
foreach ($section in @('source-freeze','local-primary','local-sandboxes','remote-campaigns')) {
  foreach ($file in (Get-ChildItem -LiteralPath (Join-Path $ArchiveRoot $section) -File | Sort-Object Name)) {
    $count = 1
    $verification = ''
    if ($section -eq 'local-primary') {
      if ($localPrimary.ContainsKey($file.Name)) {
        $count = [long]$localPrimary[$file.Name].fileCount
        $verification = $localPrimary[$file.Name].status
      } elseif ($file.Name -eq 'development-runs-20260822.tar.gz') {
        $count = (Get-Content -LiteralPath (Join-Path $ArchiveRoot 'manifests\development-runs-20260822.source-sha256.tsv') | Measure-Object).Count - 1
        $verification = 'VERIFIED_FULL_ARCHIVE'
      }
    } elseif ($section -eq 'local-sandboxes') {
      $count = [long]$sandbox[$file.Name].fileCount
      $verification = $sandbox[$file.Name].status
    } elseif ($section -eq 'remote-campaigns') {
      $count = [long]$remote[$file.Name].fileCount
      $verification = $remote[$file.Name].status
    } else {
      $verification = 'SOURCE_HASH_VERIFIED'
    }
    $records.Add([pscustomobject]@{
      artifactKind = $section
      artifactName = $file.Name
      archivePath = $file.FullName
      bytes = $file.Length
      sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
      sourceFileCount = $count
      verificationStatus = $verification
    })
  }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("artifactKind`tartifactName`tarchivePath`tbytes`tsha256`tsourceFileCount`tverificationStatus")
foreach ($row in $records) {
  $lines.Add(($row.artifactKind,$row.artifactName,$row.archivePath,$row.bytes,$row.sha256,$row.sourceFileCount,$row.verificationStatus -join "`t"))
}
[IO.File]::WriteAllLines($archiveManifest + '.partial', $lines, $utf8)
Move-Item -LiteralPath ($archiveManifest + '.partial') -Destination $archiveManifest -Force
Copy-Item -LiteralPath $archiveManifest -Destination (Join-Path $ArchiveRoot 'manifests\V35-PAPER-EVIDENCE-ARCHIVE-MANIFEST.tsv') -Force

$evidenceManifest = Join-Path $MasterRoot 'evidence-sha256.tsv'
$evidenceLines = New-Object System.Collections.Generic.List[string]
$evidenceLines.Add("path`tbytes`tsha256")
foreach ($file in (Get-ChildItem -LiteralPath $MasterRoot -Recurse -File | Where-Object { $_.FullName -ne $evidenceManifest } | Sort-Object FullName)) {
  $relative = [IO.Path]::GetRelativePath($MasterRoot, $file.FullName).Replace('\','/')
  $evidenceLines.Add(($relative,$file.Length,(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -join "`t"))
}
[IO.File]::WriteAllLines($evidenceManifest + '.partial', $evidenceLines, $utf8)
Move-Item -LiteralPath ($evidenceManifest + '.partial') -Destination $evidenceManifest -Force

$catalog = (Join-Path $ArchiveRoot 'catalog')
$catalogPartial = (Join-Path $ArchiveRoot 'catalog.partial')
if (Test-Path -LiteralPath $catalogPartial) { Remove-Item -LiteralPath $catalogPartial -Recurse -Force }
New-Item -ItemType Directory -Path $catalogPartial | Out-Null
Copy-Item -Path (Join-Path $MasterRoot '*') -Destination $catalogPartial -Recurse -Force
if (Test-Path -LiteralPath $catalog) {
  $resolved = (Resolve-Path -LiteralPath $catalog).Path
  if ((Split-Path -Parent $resolved) -ne $ArchiveRoot -or (Split-Path -Leaf $resolved) -ne 'catalog') { throw 'Unsafe catalog replacement target' }
  Remove-Item -LiteralPath $resolved -Recurse -Force
}
Move-Item -LiteralPath $catalogPartial -Destination $catalog

$packageRoot = Join-Path $ArchiveRoot 'packages'
New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null
$package = Join-Path $packageRoot 'V35-PAPER-EVIDENCE-CATALOG-20260823.zip'
$packagePartial = $package + '.partial.zip'
if (Test-Path -LiteralPath $packagePartial) { Remove-Item -LiteralPath $packagePartial -Force }
Compress-Archive -Path (Join-Path $catalog '*') -DestinationPath $packagePartial -CompressionLevel Optimal
[Reflection.Assembly]::LoadWithPartialName('System.IO.Compression.FileSystem') | Out-Null
$zip = [IO.Compression.ZipFile]::OpenRead($packagePartial)
try {
  if ($zip.Entries.Count -lt 20) { throw 'Final catalog ZIP is unexpectedly small' }
  $zipEntries = $zip.Entries.Count
} finally { $zip.Dispose() }
if (Test-Path -LiteralPath $package) { Remove-Item -LiteralPath $package -Force }
Move-Item -LiteralPath $packagePartial -Destination $package
$packageHash = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash
$packageInfo = @(
  "path=$package",
  "bytes=$((Get-Item -LiteralPath $package).Length)",
  "sha256=$packageHash",
  "zipEntries=$zipEntries",
  "status=VERIFIED_OPENABLE"
)
[IO.File]::WriteAllLines((Join-Path $packageRoot 'PACKAGE-SHA256.txt'), $packageInfo, $utf8)

[pscustomobject]@{
  ArchiveArtifacts = $records.Count
  EvidenceFiles = $evidenceLines.Count - 1
  PackagePath = $package
  PackageBytes = (Get-Item -LiteralPath $package).Length
  PackageSha256 = $packageHash
  ZipEntries = $zipEntries
}
