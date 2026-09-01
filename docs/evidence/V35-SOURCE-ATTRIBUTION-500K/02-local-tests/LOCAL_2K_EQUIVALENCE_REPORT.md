# LOCAL_2K_EQUIVALENCE_REPORT — Source Attribution Observer 2k OFF/ON（任务书§12.3）

- 日期：2026-09-01
- 配置：100_5_3_1 / seed 20260901 / A4(C0) / MaxFEs=2000 / -Xms1g -Xmx4g
- 终止：PHASE_CONSISTENT_TAIL_STOP，actualFE=100（初始种群后budget耗尽）
- checkpoint目标：50（可达），验证B_0与checkpoint捕获接线
- 本地V4 Jar SHA-256：见 01-observer-implementation/evidence-sha256.tsv

## 行为等价（掩码=版本/runId/墙钟/观察器溯源/内存实测字段）

15/15 行为产物逐字节一致：front.csv、passive-archive.csv、cmax-audit-curves/records、
ca-ta-lite-events.log、dscr-events/teacher-uses、bottleneck-pressure-events、
initial-population.sha256、profile.sha256、budget-termination.properties、
configuration.txt、formal-gate.properties、status.properties、pddr-observation.properties。

memory-summary.properties 的 heap/wallClock 差异属测量字段，按任务书§十四允许不一致
（wallClock、nanoTime、heap measurements、GC measurements、observer output files）。

## Observer完整性（ON侧）

- source-ledger.csv 行数 = actualFE = 100 ✓
- rawSource 无 UNSET ✓（100×INITIAL_POPULATION）
- observerErrors=0、droppedEvents=0、unknownSourceEvents=0 ✓
- checkpointRows>=1（B_0+checkpoint 50已捕获）✓
- heapUsedPeak：OFF=39,425,536 B / ON=46,804,992 B（delta≈7.4 MB，符合分解模型的流式设计预期）

## 结论

本地2k OFF/ON等价门 PASS；Observer接线真实、fail-closed无违约。可进入训练机20k工程门。
