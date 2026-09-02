$ErrorActionPreference = "Stop"
$path = "docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/A4-50k-ON-s20260901-121FBB49/telemetry-teacher-use-events.csv"
$reader = New-Object System.IO.StreamReader($path)
$null = $reader.ReadLine()
$qpTotal = 0
$qgTotal = 0
$qpAction = @{}
$qpArcHist = @{}
$qpActionArc = @{}
$regretNonZero = 0
$regretSum = 0.0
$regretCount = 0
$nonKeepArcGE2 = 0
while ($null -ne ($line = $reader.ReadLine())) {
  $cut = $line.IndexOf(',"')
  if ($cut -lt 0) { continue }
  $prefix = $line.Substring(0, $cut)
  $p = $prefix.Split(',')
  if ($p.Count -lt 23) { continue }
  if ($p[11] -eq "QP") {
    $qpTotal++
    $act = $p[16]
    $cvs = [int]$p[22]
    if (-not $qpAction.ContainsKey($act)) { $qpAction[$act] = 0 }
    $qpAction[$act]++
    if (-not $qpArcHist.ContainsKey($cvs)) { $qpArcHist[$cvs] = 0 }
    $qpArcHist[$cvs]++
    $key = "$act|$cvs"
    if (-not $qpActionArc.ContainsKey($key)) { $qpActionArc[$key] = 0 }
    $qpActionArc[$key]++
    $r = 0.0
    if ([double]::TryParse($p[26], [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$r)) {
      $regretSum += $r; $regretCount++
      if ($r -gt 0.0 -and $act -ne "KEEP") { $regretNonZero++ }
    }
    if ($act -ne "KEEP" -and $cvs -ge 2) { $nonKeepArcGE2++ }
  } elseif ($p[11] -eq "QG") {
    $qgTotal++
  }
}
$reader.Close()
Write-Output "===== A4-50k ON, 100_5_3_1, seed 20260901 (telemetry-teacher-use-events) ====="
Write-Output ("QP rows = {0}   QG rows = {1}" -f $qpTotal, $qgTotal)
Write-Output "-- QP action distribution --"
foreach ($k in ($qpAction.Keys | Sort-Object)) { Write-Output ("{0,-14} {1,7}  {2,7:N2}%" -f $k, $qpAction[$k], (100.0*$qpAction[$k]/[Math]::Max(1,$qpTotal))) }
Write-Output "-- QP candidateViewSize (=archive size at selection) histogram --"
foreach ($k in ($qpArcHist.Keys | Sort-Object {[int]$_})) { Write-Output ("archiveSize={0,-3} count={1,7}  {2,7:N2}%" -f $k, $qpArcHist[$k], (100.0*$qpArcHist[$k]/[Math]::Max(1,$qpTotal))) }
Write-Output "-- joint action x archiveSize --"
foreach ($k in ($qpActionArc.Keys | Sort-Object)) { Write-Output ("{0,-22} {1,7}" -f $k, $qpActionArc[$k]) }
Write-Output ("non-KEEP with archive>=2: {0} ({1:N2}% of QP)" -f $nonKeepArcGE2, (100.0*$nonKeepArcGE2/[Math]::Max(1,$qpTotal)))
Write-Output ("mean directionalRegret (all QP rows): {0:N6}  (n={1})" -f ($regretSum/[Math]::Max(1,$regretCount)), $regretCount)
Write-Output ("non-KEEP rows with directionalRegret>0: {0}" -f $regretNonZero)
