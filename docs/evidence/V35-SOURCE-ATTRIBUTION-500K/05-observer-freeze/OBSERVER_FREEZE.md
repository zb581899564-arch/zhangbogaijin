# OBSERVER_FREEZE — Schema/Jar/合同冻结（流式修正版）

- 冻结日期：2026-09-01
- Jar：`jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar`
- **Jar SHA-256：78bf4d3016a612a9f3073ca00abb94181ef4883b2838540ac9776b1eed046565**
- Schema：`v35-source-attribution-observer-schema-v1`（含真流式ledger修正）
- 行为等价：12文件逐字节一致 + 2文件掩码等价 + 1测量（03/BEHAVIORAL_EQUIVALENCE_20K.md）
- 完整性：ledgerRows==actualFE==15258、UNSET=0、observerErrors=0、droppedEvents=0、
  checkpointRows=3、boundedCapacityViolations=0
- 内存门：ratio=0.2891 < 0.60 → PASS（04/MEMORY_PREFLIGHT_20K.md）
- 正式Jar `8dad8f40…bad8b9` 逐字节不动

```ini
observerImplemented=true
observerBehavioralEquivalent=true
observerMemoryPreflightExecuted=true
memoryGatePassed=true
observerSchemaFrozen=true
observerJarFrozen=true
observerErrors=0
droppedEvents=0
sourceAttribution500kEligible=true
sourceAttribution500kStarted=false
SA_HARD_500K_STARTED=false
```

版本纪律：任何字段/标签/上限/哈希口径修改 → schema版本递增 → 20k OFF/ON+内存门全部重跑。
500k执行顺序（须用户批准后启动）：SA-HARD → failure-class复现门 → SA-NORMAL → G1/G3分析
→ (仅G1)SA-A2-CONDITIONAL → G1-G4 → 强制停止。
