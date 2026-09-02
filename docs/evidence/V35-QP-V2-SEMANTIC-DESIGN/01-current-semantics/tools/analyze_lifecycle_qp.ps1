$ErrorActionPreference = "Stop"
$files = @(
  @{ name="SA-HARD-100_5_3_1"; path="docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k/02-remote-run/results/SA-HARD-V5-500k/source-lifecycle-events.csv" },
  @{ name="SA-NORMAL-100_2_3_1"; path="docs/evidence/V35-SOURCE-ATTRIBUTION-500K/10-v5-sa-normal-500k/02-remote-run/results/SA-NORMAL-V5-500k/source-lifecycle-events.csv" }
)
foreach ($f in $files) {
  if (-not (Test-Path $f.path)) { Write-Output "MISSING: $($f.path)"; continue }
  $reader = New-Object System.IO.StreamReader($f.path)
  $null = $reader.ReadLine()  # header
  $actionCounts = @(0,0,0,0)
  $qpActionTotal = 0
  $teacherSet = @{}
  $teacherCounts = @{}
  $paDetail = @{}
  $qtSet = @{}
  while ($null -ne ($line = $reader.ReadLine())) {
    # actualFE,nominalFE,outerCycle,qRound,eventType,subjectFingerprint,relatedFingerprint,source,action,detail
    $idx = $line.IndexOf(',')
    $idx = $line.IndexOf(',', $idx+1)
    $idx = $line.IndexOf(',', $idx+1)
    $idx = $line.IndexOf(',', $idx+1)
    $idx5 = $line.IndexOf(',', $idx+1)          # end of eventType
    $eventType = $line.Substring($idx+1, $idx5-$idx-1)
    if ($eventType -eq "QP_ACTION") {
      $rest = $line.Substring($idx5+1)
      $p1 = $rest.IndexOf(',')
      $p2 = $rest.IndexOf(',', $p1+1)
      $p3 = $rest.IndexOf(',', $p2+1)
      $p4 = $rest.IndexOf(',', $p3+1)
      $actionStr = $rest.Substring($p3+1, $p4-$p3-1)
      $ai = [int]$actionStr
      $actionCounts[$ai]++
      $qpActionTotal++
      $relEnd = $rest.IndexOf(',', $p2+1)
      $teacher = $rest.Substring($p2+1, $relEnd-$p2-1)
      if (-not $teacherSet.ContainsKey($teacher)) { $teacherSet[$teacher] = 0 }
      $teacherSet[$teacher]++
    } elseif ($eventType -eq "QP_TEACHER") {
      $rest = $line.Substring($idx5+1)
      $p1 = $rest.IndexOf(',')
      $p2 = $rest.IndexOf(',', $p1+1)
      $relEnd = $rest.IndexOf(',', $p2+1)
      $t = $rest.Substring($p2+1, $relEnd-$p2-1)
      if (-not $qtSet.ContainsKey($t)) { $qtSet[$t] = 0 }
      $qtSet[$t]++
    } elseif ($eventType -eq "PERSONAL_ARCHIVE") {
      $lastComma = $line.LastIndexOf(',')
      $detail = $line.Substring($lastComma+1)
      if (-not $paDetail.ContainsKey($detail)) { $paDetail[$detail] = 0 }
      $paDetail[$detail]++
    }
  }
  $reader.Close()
  Write-Output "===== $($f.name) ====="
  Write-Output ("QP_ACTION total = {0}" -f $qpActionTotal)
  $names = @("KEEP","DIRECTIONAL","EPSILON","COMPLEMENTARY")
  for ($i=0; $i -lt 4; $i++) {
    Write-Output ("{0,-14} {1,8}  {2,8:N3}%" -f $names[$i], $actionCounts[$i], (100.0*$actionCounts[$i]/[Math]::Max(1,$qpActionTotal)))
  }
  $nonKeep = $actionCounts[1]+$actionCounts[2]+$actionCounts[3]
  Write-Output ("non-KEEP total  = {0}  ({1:N3}%)" -f $nonKeep, (100.0*$nonKeep/[Math]::Max(1,$qpActionTotal)))
  Write-Output ("distinct QP_ACTION selected-pbest fingerprints = {0}" -f $teacherSet.Count)
  $top = $teacherSet.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 5
  $i=0
  foreach ($t in $top) { $i++; Write-Output ("  top{0}: {1,7} ({2,6:N2}%)" -f $i, $t.Value, (100.0*$t.Value/[Math]::Max(1,$qpActionTotal))) }
  Write-Output ("distinct QP_TEACHER fingerprints = {0}" -f $qtSet.Count)
  Write-Output "PERSONAL_ARCHIVE detail histogram:"
  foreach ($k in ($paDetail.Keys | Sort-Object)) { Write-Output ("  {0,-30} {1,8}" -f $k, $paDetail[$k]) }
  Write-Output ""
}
