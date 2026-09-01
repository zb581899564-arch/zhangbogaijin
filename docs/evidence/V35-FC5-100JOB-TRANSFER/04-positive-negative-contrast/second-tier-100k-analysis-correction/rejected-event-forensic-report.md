# Rejected Representative Forensic Report — 6条 PDDR拒绝逐项法证

> 总控独立抽查原始日志 2 seed 验证通过；源码行号见 FIELD_SEMANTICS_AUDIT.md

## 总览

- 扫描范围：144 DIRECTION_LABEL_EVENT (36轮×4)，其中 A2 36条、A4 108条。
- `pddrSelected=false` 总计 **6条**（4.2%），全部位于 **A4 W2**，A2 零拒绝。
- `UNIQUE_DIRECTIONAL_REPRESENTATIVE` 去重后亦为6条（本次无同轮同指纹跨标签去重带来的数量差异）。
- 拒绝率：overall pool→PDDR 95.8% (138/144)，A4 W2 90.0% (54/60)，seed分：20260901 80% (16/20), 20260902 100% (20/20), 20260903 90% (18/20)。

## 逐条法证

### 1) seed 20260901 A4 cycle 8 FE 56932 E_C (Cmax)

- fingerprintSha256: 21861d118f03512a2c7de4d98b8b7fb655c9c857906e47129507d6375bc5116c
- source: PARENT, objectives: Cmax 768.687 / TEC 128913.3 / TWC 357517.58
- pddrScore 1.0, pddrRank 107, cutoffRank 100, rankBeyondCutoff 7, rejectReason PDDR_SCORE_RANK_NOT_SELECTED, nextSlot -1 NONE
- 同指纹其他方向标签：无
- 等价解保留：UNAVAILABLE_SELECTED_FINGERPRINT_SET_NOT_PERSISTED（日志未持久化完整选中集合，无法判定是否有相同目标向量的等价解被保留）
- cutoffScore/替代者：UNAVAILABLE_FULL_POOL_SCORE_LEDGER_NOT_PERSISTED / UNAVAILABLE_SELECTED_IDENTITY_LEDGER_NOT_PERSISTED
- 方向极值：workingDirectionEqualAfter false (workingBestCmax≠768.68，档案极值更优), archiveDirectionEqualAfter true (archive仍保有该Cmax极值) — 说明被拒绝的是pool极值但非全局最优的解
- 下一轮：cycle 9 未重发现同指纹

### 2) seed 20260901 A4 cycle 9 FE 65313 E_C (Cmax)

- fingerprintSha256: 1c3d77e2ec8703b2e2706c7656ac0d4157c5e9bd0bc654e8ab1aca1facf901ab
- Cmax 769.361, pddrRank 108 (+8), rankBeyond 8
- 其他字段同上，working false / archive false（此时archive最优已更优于769.36，archive亦不保有）
- 下一轮cycle10未重发现

### 3) seed 20260901 A4 cycle 9 FE 65313 E_E (TEC)

- fingerprintSha256: e021942d0d1fa202de014b974b77bdf1cc467ece572ce6510a88258026b0cc0a
- TEC 118149.20, pddrRank 113 (+13), working false / archive false

### 4) seed 20260901 A4 cycle 12 FE 96025 E_E (TEC)

- fingerprintSha256: a033136e4922a1f76e45e6a30d5fe43d8dde8ab6dbf70a787327876f992390dc
- TEC 117803.13, pddrRank 106 (+6), 末轮right-censored
- working false / archive false

### 5) seed 20260903 A4 cycle 11 FE 84405 E_C (Cmax)

- fingerprintSha256: c2d1e4fe2d2a79cb7aa9e86502f76b84bf032243b6457f7590f1bffe92c0f44a
- Cmax 768.963, pddrRank 101 (+1) — 仅差1名被挤掉，临界拒绝
- working false / archive true (archive仍保有该方向极值)

### 6) seed 20260903 A4 cycle 12 FE 96025 E_C (Cmax)

- fingerprintSha256: c6644e47462f0aac144d8be5e66ecc44e1cf5cda559911879770297b1d33dedb
- Cmax 770.731, pddrRank 109 (+9), 末轮
- working false / archive false

## 关键限制（evidence-field-limitations.csv）

- 日志未持久化完整MergePool分数表 → cutoffScore、替代者身份、等价非代表解均 **UNAVAILABLE_NOT_PERSISTED**，不得猜测。
- “archive方向极值仍存在”≠“相同solution仍存在”。本报告用 `archiveWorkingGap.csv` 的 `archiveBestCmax/TEC/TWC` 与被拒代表的客观值比对1e-9，判定archive极值是否相等，未断言指纹相等。

## 结论

存在真实PDDR拒绝（局部失败事件），但：
- 非系统性：仅A4 W2 6/60 (10%)，A2 0/36，seed 20260902 0拒绝；
- 临界性：5条rankBeyond 1-13，未出现Roverflow>1伴随的批量拒绝；
- 无批量挤出证据，`pddrToNextRate` 仍 1.0（选中即进入next）。
