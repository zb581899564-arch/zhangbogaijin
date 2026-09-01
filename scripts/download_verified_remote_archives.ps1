param(
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823',
  [int]$ThrottleLimit = 4,
  [string[]]$CampaignIds = @()
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$manifest = Join-Path $ArchiveRoot 'manifests\remote-archive-manifest.tsv'
$destinationRoot = Join-Path $ArchiveRoot 'remote-campaigns'
New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null

& scp.exe -O -q 'aic-inspur-home:/home/inspur/aicomp/zhangbo-v35-paper-evidence-catalog-20260823/remote-archive-manifest.tsv' $manifest
if ($LASTEXITCODE -ne 0) { throw 'Failed to download remote archive manifest' }
$rows = @(Import-Csv -LiteralPath $manifest -Delimiter "`t" | Where-Object { $_.status -eq 'VERIFIED' })
if ($rows.Count -ne 25) { throw "Expected 25 verified remote archives, found $($rows.Count)" }
if ($CampaignIds.Count -gt 0) {
  $rows = @($rows | Where-Object { $_.campaignId -in $CampaignIds })
  if ($rows.Count -ne $CampaignIds.Count) { throw 'One or more requested campaign IDs were not found in the verified manifest' }
}

$results = $rows | ForEach-Object -Parallel {
  $ErrorActionPreference = 'Stop'
  $PSNativeCommandUseErrorActionPreference = $false
  $row = $_
  $destinationRoot = $using:destinationRoot
  $target = Join-Path $destinationRoot ($row.campaignId + '.tar.gz')
  $valid = $false
  if (Test-Path -LiteralPath $target -PathType Leaf) {
    $item = Get-Item -LiteralPath $target
    if ($item.Length -eq [long]$row.archiveBytes) {
      $valid = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -eq $row.archiveSha256
    }
  }
  if (-not $valid) {
    $copied = $false
    foreach ($attempt in 1..5) {
      if (Test-Path -LiteralPath $target) {
        $currentLength = (Get-Item -LiteralPath $target).Length
        if ($currentLength -gt [long]$row.archiveBytes) { Remove-Item -LiteralPath $target -Force }
      }
      $localSftpPath = $target.Replace('\','/')
      $batch = 'reget "' + $row.archivePath + '" "' + $localSftpPath + '"'
      $batchFile = Join-Path ([System.IO.Path]::GetTempPath()) ('v35-sftp-' + $row.campaignId + '-' + $PID + '.txt')
      [System.IO.File]::WriteAllText($batchFile, $batch + [Environment]::NewLine, [System.Text.Encoding]::UTF8)
      try {
        $process = Start-Process -FilePath sftp.exe -ArgumentList @('-q','-B','262144','-R','128','-b',$batchFile,'-o','ConnectionAttempts=3','-o','ServerAliveInterval=15','-o','ServerAliveCountMax=4','aic-inspur-home') -WindowStyle Hidden -Wait -PassThru
        $exitCode = $process.ExitCode
      } finally {
        if (Test-Path -LiteralPath $batchFile) { Remove-Item -LiteralPath $batchFile -Force }
      }
      if ($exitCode -eq 0 -and (Get-Item -LiteralPath $target).Length -eq [long]$row.archiveBytes) {
        $candidateHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
        if ($candidateHash -eq $row.archiveSha256) {
          $copied = $true
          break
        }
        Remove-Item -LiteralPath $target -Force
      }
      Start-Sleep -Seconds (2 * $attempt)
    }
    if (-not $copied) { throw "scp failed after 5 attempts: $($row.campaignId)" }
  }
  $item = Get-Item -LiteralPath $target
  $hash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
  if ($item.Length -ne [long]$row.archiveBytes -or $hash -ne $row.archiveSha256) {
    throw "Downloaded archive verification failed: $($row.campaignId)"
  }
  [pscustomobject]@{
    campaignId = $row.campaignId
    bytes = $item.Length
    sha256 = $hash
    localPath = $target
    status = 'VERIFIED_LOCAL_COPY'
  }
} -ThrottleLimit $ThrottleLimit

$validationName = if ($CampaignIds.Count -gt 0) { 'remote-archive-download-validation-priority.csv' } else { 'remote-archive-download-validation.csv' }
$results | Sort-Object campaignId | Export-Csv -LiteralPath (Join-Path $ArchiveRoot ('manifests\' + $validationName)) -NoTypeInformation -Encoding UTF8
