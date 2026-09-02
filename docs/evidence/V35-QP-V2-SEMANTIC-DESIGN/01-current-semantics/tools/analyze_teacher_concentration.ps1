$ErrorActionPreference = "Stop"
$targets = @(
  @{ name="A4-50k 100_5_3_1 s20260901 (hard)"; path="docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/A4-50k-ON-s20260901-121FBB49/telemetry-teacher-use-events.csv" },
  @{ name="A4-20k 100_2_4_1 s20260901 (normal)"; path="docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/18-final-2k-20k-50k-gates/A4-20k-effective-20258-100_2_4_1-ON-final/telemetry-teacher-use-events.csv" }
)
foreach ($t in $targets) {
  if (-not (Test-Path $t.path)) { Write-Output "MISSING $($t.path)"; continue }
  $reader = New-Object System.IO.StreamReader($t.path)
  $null = $reader.ReadLine()
  $qpTotal = 0
  $arcHist = @{}
  $teacherCounts = @{}
  $keepArcGE2 = 0
  while ($null -ne ($line = $reader.ReadLine())) {
    $cut = $line.IndexOf(',"')
    if ($cut -lt 0) { continue }
    $prefix = $line.Substring(0, $cut)
    $p = $prefix.Split(',')
    if ($p.Count -lt 23) { continue }
    if ($p[11] -ne "QP") { continue }
    $qpTotal++
    $cvs = [int]$p[22]
    if (-not $arcHist.ContainsKey($cvs)) { $arcHist[$cvs] = 0 }
    $arcHist[$cvs]++
    # teacher fingerprint: first quoted field after prefix
    $rest = $line.Substring($cut + 2)
    $end = $rest.IndexOf('"')
    if ($end -gt 0) {
      $tfp = $rest.Substring(0, $end)
      if (-not $teacherCounts.ContainsKey($tfp)) { $teacherCounts[$tfp] = 0 }
      $teacherCounts[$tfp]++
    }
  }
  $reader.Close()
  Write-Output "===== $($t.name) ====="
  Write-Output ("QP rows = {0}" -f $qpTotal)
  Write-Output "archiveSize histogram:"
  foreach ($k in ($arcHist.Keys | Sort-Object {[int]$_})) { Write-Output ("  size={0,-3} {1,7}  {2,7:N2}%" -f $k, $arcHist[$k], (100.0*$arcHist[$k]/[Math]::Max(1,$qpTotal))) }
  Write-Output ("distinct QP teacher identities = {0}" -f $teacherCounts.Count)
  $top = $teacherCounts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 5
  $i = 0
  foreach ($e in $top) { $i++; Write-Output ("  top{0}: {1,6} ({2,6:N2}%)" -f $i, $e.Value, (100.0*$e.Value/[Math]::Max(1,$qpTotal))) }
  $t5 = 0; $i = 0
  foreach ($e in ($teacherCounts.GetEnumerator() | Sort-Object Value -Descending)) { $i++; if ($i -le 5) { $t5 += $e.Value } }
  Write-Output ("top5 share = {0:N2}%" -f (100.0*$t5/[Math]::Max(1,$qpTotal)))
  Write-Output ""
}
