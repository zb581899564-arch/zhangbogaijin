param(
  [string]$ProjectRoot = 'E:\学习\李明哲-毕业材料\张博改进'
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$evidence = Join-Path $root 'docs\evidence\V35-FC5-100JOB-TRANSFER'
$manifest = Join-Path $evidence 'evidence-sha256.tsv'
$tracked = @(
  Get-ChildItem -LiteralPath $evidence -Recurse -File |
    Where-Object { $_.FullName -ne $manifest }
)
$relativeSources = @(
  'java-jmetal58\jmetal-algorithm\src\main\java\org\uma\jmetal\algorithm\multiobjective\mypso\v35\V35Fc5TransferAudit.java',
  'java-jmetal58\jmetal-algorithm\src\main\java\org\uma\jmetal\algorithm\multiobjective\mypso\ZhangBoMOHPSOQ.java',
  'java-jmetal58\jmetal-algorithm\src\main\java\org\uma\jmetal\algorithm\multiobjective\mypso\v35\V35FairRunner.java',
  'java-jmetal58\jmetal-exec\src\main\java\org\uma\jmetal\runner\lc_psode\ZhangBoV35Fc5TransferRunner.java',
  'java-jmetal58\jmetal-algorithm\src\test\java\org\uma\jmetal\algorithm\multiobjective\mypso\zhangbo\V35Fc5TransferAuditTest.java',
  'java-jmetal58\jmetal-algorithm\src\test\java\org\uma\jmetal\algorithm\multiobjective\mypso\v35\V35Fc5TransferTelemetryEquivalenceTest.java'
)
foreach ($relative in $relativeSources) {
  $path = Join-Path $root $relative
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
    throw "Missing tracked artifact: $path"
  }
  $tracked += Get-Item -LiteralPath $path
}
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("sha256`tbytes`tpath")
$tracked | Sort-Object FullName -Unique | ForEach-Object {
  $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
  $label = [IO.Path]::GetRelativePath($root, $_.FullName).Replace('\', '/')
  $lines.Add("$hash`t$($_.Length)`t$label")
}
[IO.File]::WriteAllText($manifest, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
