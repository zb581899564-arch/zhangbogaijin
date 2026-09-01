param(
  [string]$MasterRoot = 'E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER',
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$campaigns = Import-Csv -LiteralPath (Join-Path $MasterRoot 'campaign-ledger.csv')
$remoteSummary = Import-Csv -LiteralPath (Join-Path $MasterRoot 'inventory\remote-campaign-summary.csv')
$remoteClass = @{}
foreach ($campaign in $campaigns) {
  if ($campaign.remotePath) {
    $id = Split-Path -Leaf $campaign.remotePath
    $remoteClass[$id] = $campaign
  }
}

function Local-Class([string]$path) {
  if ($path -match 'V35-P25D|P8\.4|P8\.6|\\P9(?:-|\\)|legacy') { return 'LEGACY_EXCLUDED' }
  if ($path -match 'V35-STAGE2-PILOT|V35-P25E|V35-P21') { return 'PILOT_DIAGNOSTIC' }
  if ($path -match 'V35-DOE1|V35-P24\.2|FC6|heldout') { return 'PAPER_PARAMETER_SELECTION' }
  if ($path -match 'paper_evidence\\I0-v35|paper_evidence\\I1|docs\\evidence\\P[1256](?:\\|\.)') { return 'MAIN_METHOD_EVIDENCE' }
  if ($path -match 'regression-sandbox|\\target\\|\.codex-temp') { return 'TEMPORARY_OR_RETRY' }
  return 'REPRODUCIBILITY_ONLY'
}

$artifactLines = New-Object System.Collections.Generic.List[string]
$artifactLines.Add("host`tcampaignId`tpath`tbytes`tsha256`tpaperUseClass`treferenceEligible`tretentionClass`tarchivePath`tcleanupAction")

$localLedger = Get-Content -LiteralPath (Join-Path $MasterRoot 'inventory\local-artifact-ledger.tsv') | Select-Object -Skip 1
foreach ($line in $localLedger) {
  $parts = $line -split "`t", 5
  if ($parts.Count -ne 5) { continue }
  $class = Local-Class $parts[2]
  $retention = if ($class -eq 'TEMPORARY_OR_RETRY') { 'DELETE_AFTER_VERIFIED_ARCHIVE' } elseif ($class -eq 'LEGACY_EXCLUDED') { 'ARCHIVE_THIN' } else { 'KEEP_OR_ARCHIVE_FULL' }
  $artifactLines.Add(($parts[0], '', $parts[2], $parts[3], $parts[4], $class, 'false', $retention, $ArchiveRoot, 'PENDING_CLASSIFIED_CLEANUP' -join "`t"))
}

$remoteLedger = Get-Content -LiteralPath (Join-Path $MasterRoot 'inventory\remote-artifact-ledger.tsv') | Select-Object -Skip 1
foreach ($line in $remoteLedger) {
  $parts = $line -split "`t", 5
  if ($parts.Count -ne 5) { continue }
  $campaignId = $parts[1]
  $meta = $remoteClass[$campaignId]
  $class = if ($null -ne $meta) { $meta.paperUseClass } else { 'REPRODUCIBILITY_ONLY' }
  $retention = if ($null -ne $meta) { $meta.retentionClass } else { 'REVIEW' }
  $artifactLines.Add(($parts[0], $campaignId, $parts[2], $parts[3], $parts[4], $class, 'false', $retention, (Join-Path $ArchiveRoot 'remote-campaigns'), 'PENDING_CLASSIFIED_CLEANUP' -join "`t"))
}
[System.IO.File]::WriteAllLines((Join-Path $MasterRoot 'artifact-ledger.tsv'), $artifactLines, $utf8)

$locations = foreach ($summary in $remoteSummary) {
  $meta = $remoteClass[$summary.campaignId]
  [pscustomobject]@{
    campaignId = $summary.campaignId
    remotePath = $summary.remotePath
    status = $summary.status
    fileCount = $summary.fileCount
    totalBytes = $summary.totalBytes
    paperUseClass = if ($null -ne $meta) { $meta.paperUseClass } else { 'REVIEW_REQUIRED' }
    referenceEligible = if ($null -ne $meta) { $meta.referenceEligible } else { 'false' }
    localPath = if ($null -ne $meta) { $meta.localPath } else { '' }
    retentionClass = if ($null -ne $meta) { $meta.retentionClass } else { 'REVIEW' }
    archivePath = Join-Path $ArchiveRoot ('remote-campaigns\' + $summary.campaignId + '.tar.gz')
  }
}
$csv = $locations | Sort-Object campaignId | ConvertTo-Csv -NoTypeInformation
[System.IO.File]::WriteAllLines((Join-Path $MasterRoot 'remote-location-map.csv'), $csv, $utf8)

$summary = @(
  "artifactRows=$($artifactLines.Count - 1)",
  "remoteCampaigns=$($locations.Count)",
  "generatedAt=2026-08-23T00:00:00+08:00"
)
[System.IO.File]::WriteAllLines((Join-Path $MasterRoot 'LEDGER_BUILD_SUMMARY.properties'), $summary, $utf8)
