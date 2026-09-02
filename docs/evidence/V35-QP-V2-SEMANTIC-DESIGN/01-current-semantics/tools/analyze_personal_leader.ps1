$ErrorActionPreference = "Stop"
$runs = @("seed-20260822","seed-20260823","seed-20260824")
foreach ($arm in @("D3_A3_BLOCK_FROZEN","D2_QP_SYNCHRONOUS")) {
  $actionTot = @{}
  $warmup = 0
  $qp = 0
  $arcHist = @{}
  $nonKeepByArc = @{}
  $keepByArc = @{}
  $maskHist = @{}
  $fallbackTrue = 0
  foreach ($s in $runs) {
    $f = "docs/evidence/V35-A2-A3-DECOMPOSITION/04-runs/$s/$arm/a2a3-personal-leader-events.csv"
    $rows = Import-Csv $f
    foreach ($r in $rows) {
      if ($r.source -eq "WARMUP_DIRECTIONAL") { $warmup++ ; continue }
      if ($r.source -ne "QP_ACTION") { continue }
      $qp++
      $a = $r.action
      if (-not $actionTot.ContainsKey($a)) { $actionTot[$a] = 0 }
      $actionTot[$a]++
      $n = [int]$r.archiveSize
      if (-not $arcHist.ContainsKey($n)) { $arcHist[$n] = 0 }
      $arcHist[$n]++
      if ($a -ne "KEEP") {
        if (-not $nonKeepByArc.ContainsKey($n)) { $nonKeepByArc[$n] = 0 }
        $nonKeepByArc[$n]++
      } else {
        if (-not $keepByArc.ContainsKey($n)) { $keepByArc[$n] = 0 }
        $keepByArc[$n]++
      }
      $m = $r.mask
      if (-not $maskHist.ContainsKey($m)) { $maskHist[$m] = 0 }
      $maskHist[$m]++
      if ($r.fallback -eq "True") { $fallbackTrue++ }
    }
  }
  Write-Output "=== $arm  (3 seeds pooled; warmup=$warmup, QP_ACTION=$qp) ==="
  Write-Output "-- action distribution --"
  foreach ($k in ($actionTot.Keys | Sort-Object)) { $p = 100.0 * $actionTot[$k] / $qp; Write-Output ("{0,-14} {1,6}  {2,7:N2}%" -f $k, $actionTot[$k], $p) }
  Write-Output "-- archiveSize histogram at QP_ACTION --"
  foreach ($k in ($arcHist.Keys | Sort-Object {[int]$_})) { $p = 100.0 * $arcHist[$k] / $qp; Write-Output ("size={0,-3} count={1,6}  {2,7:N2}%" -f $k, $arcHist[$k], $p) }
  Write-Output "-- non-KEEP selections by archiveSize --"
  $nk = 0; foreach ($v in $nonKeepByArc.Values) { $nk += $v }
  foreach ($k in ($nonKeepByArc.Keys | Sort-Object {[int]$_})) { $p = 100.0 * $nonKeepByArc[$k] / $qp; Write-Output ("size={0,-3} nonKeep={1,6}  ({2,6:N2}% of all QP_ACTION)" -f $k, $nonKeepByArc[$k], $p) }
  Write-Output ("non-KEEP total = {0} ({1:N2}% of QP_ACTION)" -f $nk, (100.0*$nk/$qp))
  Write-Output "-- mask distribution (top 10) --"
  $i = 0
  foreach ($k in ($maskHist.Keys | Sort-Object @{Expression={$maskHist[$_]};Descending=$true})) { $p = 100.0 * $maskHist[$k] / $qp; Write-Output ("mask={0,-8} count={1,6}  {2,7:N2}%" -f $k, $maskHist[$k], $p); $i++; if ($i -ge 10) { break } }
  Write-Output ("fallback=True rows: {0}" -f $fallbackTrue)
  Write-Output ""
}
