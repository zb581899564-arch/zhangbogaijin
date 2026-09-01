param(
  [string]$ArchiveRoot = 'G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823',
  [int]$ThrottleLimit = 4
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$campaign = 'zhangbo-v35-doe1-20260820'
$remoteRoot = '/home/inspur/aicomp/zhangbo-v35-paper-evidence-catalog-20260823/chunks/' + $campaign
$chunkRoot = Join-Path $ArchiveRoot ('remote-campaigns\.chunks-' + $campaign)
$manifestRoot = Join-Path $ArchiveRoot 'manifests'
New-Item -ItemType Directory -Path $chunkRoot -Force | Out-Null

$partsManifest = Join-Path $chunkRoot 'parts.sha256'
& scp.exe -O -q ('aic-inspur-home:' + $remoteRoot + '/parts.sha256') $partsManifest
if ($LASTEXITCODE -ne 0) { throw 'Failed to download DOE split manifest' }
$partRows = @(Get-Content -LiteralPath $partsManifest | ForEach-Object {
  $columns = $_ -split '\s+', 2
  [pscustomobject]@{sha256=$columns[0].ToUpperInvariant();name=$columns[1].Trim()}
})
if ($partRows.Count -ne 7) { throw "Expected 7 DOE parts, found $($partRows.Count)" }
$totalBytes = [long]887844164
$partSize = [long](128MB)

$results = $partRows | ForEach-Object -Parallel {
  $row = $_
  $chunkRoot = $using:chunkRoot
  $remoteRoot = $using:remoteRoot
  $partSize = $using:partSize
  $totalBytes = $using:totalBytes
  $expectedBytes = if ($row.name -eq 'part-06') { $totalBytes - 6 * $partSize } else { $partSize }
  $target = Join-Path $chunkRoot $row.name
  $valid = $false
  if (Test-Path -LiteralPath $target -PathType Leaf) {
    $item = Get-Item -LiteralPath $target
    if ($item.Length -eq $expectedBytes) {
      $valid = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -eq $row.sha256
    }
  }
  if (-not $valid) {
    foreach ($attempt in 1..5) {
      if (Test-Path -LiteralPath $target) {
        $current = (Get-Item -LiteralPath $target).Length
        if ($current -gt $expectedBytes) { Remove-Item -LiteralPath $target -Force }
      }
      $localSftpPath = $target.Replace('\','/')
      $batch = 'reget "' + $remoteRoot + '/' + $row.name + '" "' + $localSftpPath + '"'
      $batchFile = Join-Path ([System.IO.Path]::GetTempPath()) ('v35-doe-split-' + $row.name + '-' + $PID + '.txt')
      [System.IO.File]::WriteAllText($batchFile, $batch + [Environment]::NewLine, [System.Text.Encoding]::UTF8)
      try {
        $process = Start-Process -FilePath sftp.exe -ArgumentList @('-q','-B','262144','-R','128','-b',$batchFile,'-o','ConnectionAttempts=3','-o','ServerAliveInterval=15','-o','ServerAliveCountMax=4','aic-inspur-home') -WindowStyle Hidden -Wait -PassThru
      } finally {
        if (Test-Path -LiteralPath $batchFile) { Remove-Item -LiteralPath $batchFile -Force }
      }
      if ($process.ExitCode -eq 0 -and (Get-Item -LiteralPath $target).Length -eq $expectedBytes) {
        $hash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
        if ($hash -eq $row.sha256) { $valid = $true; break }
        Remove-Item -LiteralPath $target -Force
      }
      Start-Sleep -Seconds (2 * $attempt)
    }
  }
  if (-not $valid) { throw "DOE split part failed: $($row.name)" }
  [pscustomobject]@{part=$row.name;bytes=$expectedBytes;sha256=$row.sha256;status='VERIFIED'}
} -ThrottleLimit $ThrottleLimit

$targetArchive = Join-Path $ArchiveRoot ('remote-campaigns\' + $campaign + '.tar.gz')
$partialArchive = $targetArchive + '.rebuild.partial'
if (Test-Path -LiteralPath $partialArchive) { Remove-Item -LiteralPath $partialArchive -Force }
$output = [System.IO.File]::Open($partialArchive, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
try {
  foreach ($row in ($partRows | Sort-Object name)) {
    $part = [System.IO.File]::OpenRead((Join-Path $chunkRoot $row.name))
    try { $part.CopyTo($output, 8MB) } finally { $part.Dispose() }
  }
} finally {
  $output.Dispose()
}
if ((Get-Item -LiteralPath $partialArchive).Length -ne $totalBytes) { throw 'Rebuilt DOE archive length mismatch' }
$expectedArchiveHash = 'BF12E1ED5F8AE5B640FE886C18046BD6E9BAC7F52BD4F8A7F3217BCEE23EC1C8'
$actualArchiveHash = (Get-FileHash -LiteralPath $partialArchive -Algorithm SHA256).Hash
if ($actualArchiveHash -ne $expectedArchiveHash) { throw 'Rebuilt DOE archive hash mismatch' }
if (Test-Path -LiteralPath $targetArchive) { Remove-Item -LiteralPath $targetArchive -Force }
Move-Item -LiteralPath $partialArchive -Destination $targetArchive

$results | Sort-Object part | Export-Csv -LiteralPath (Join-Path $manifestRoot 'doe1-split-transfer-validation.csv') -NoTypeInformation -Encoding UTF8
Remove-Item -LiteralPath $chunkRoot -Recurse -Force
