# SA-HARD 500k 独立验收纠正（2026-09-01）

本文件为 append-only 独立验收结论，不覆盖原始运行、前沿、账本、Jar 或既有报告。

## 可接受结论

```ini
SA_HARD_EXECUTION_ACCEPTED=true
SA_HARD_FAILURE_CLASS_REPRODUCED=true
```

SA-HARD 的正式 Jar、快照、初始种群、预算、前沿与冻结 Reference Contract 均闭合；
新前沿与历史 A4 前沿逐字节相同，HV/IGD 同时越过预注册失败门。

## 被撤销或降级的结论

```ini
SOURCE_ATTRIBUTION_SCHEMA_COMPLIANT=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_ESTABLISHED=false
diagnosticToolingValidated=false
SA_NORMAL_AUTHORIZED=false
```

原因：V4 `source-ledger.csv` 未输出冻结 schema 强制要求的 `nominalFE`；B0 未作为
严格 ND 前沿导出；个人档案、教师、后代与改善后代等生命周期没有形成可离线闭合的真实
事件账本。`GLOBAL_CFVF=62%` 只说明 FE 构成，不能在缺少 HARD/NORMAL 窗口对照时解释失败根因。

## 纠正边界

- 原 SA-HARD raw front 保留为可靠的 failure replay 证据。
- 原 V4 source ledger 仅作为描述性账本，不进入 G1/G3 门控。
- 新 Observer 必须递增 schema/Jar 版本并重跑 2k/20k OFF/ON 与内存门。
- 新 Observer 合格前不得启动 SA-NORMAL。

