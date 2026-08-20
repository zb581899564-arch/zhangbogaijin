# V35-P10 Cmax 教师生命周期审计报告（ShiftMode=NONE）

日期：2026-08-13

## 结论

在 v3.5 新语义（FM3、单族序列无关 SUT、`ShiftMode=NONE`、分块冻结 G-block 已修复）下对 `20_2_3_1`/seed `20260808`/population 100 的 FULL 运行完成 Cmax 纪录教师生命周期审计，并修复了审计器在正式继承局部搜索路径上的三处生成漏记（critical factory swap/insert 与 O1–O9 固定邻域此前未接入 `observeGenerated`，导致母表出现无法追溯的"幽灵解"208.53；修复后记录数 20k 从 5 增至 16、100k 达 56，BestGlobal 与末条纪录闭合）。

新语义下旧语义"201/205 类现象"**复现并确认**：

1. **CFVF 全局后代零新纪录**：100k 内 52 条搜索期新 Cmax 纪录中，50 条来自 O1–O9 固定邻域局部搜索、1 条 INTER_FACTORY_INSERTION、1 条 CA-TA-Lite/N1；**CFVF 的 20,000 次全局后代评价从未刷新 Cmax**。
2. **新纪录到首次教学的延迟巨大**：最终纪录（rec56，Cmax=195.244，FE 62657 产生）直到 FE 84304 才首次作为 G1 社会教师使用（延迟 21,647 FE，约等于剩余预算的 58%）；此前的 G1 社会教师是较早的 rec49（Cmax=198.173，200 粒子次，教学至 FE 85304）。
3. **G1 当前种群退化**：历史极值 195.244 由全局档案保存，但 100k 末尾 `currentG1` 在 233–257 间波动——搜索后期 G1 种群实际不再逼近历史最优，改进完全依赖局部搜索链。

审计为只观察旁路：修复前后 front.csv 逐位一致（20k：66 行、min=208.52800612603394/8890.454508546356/13575.604100182347），FE 与随机流不变。

## 实验设置

| 项 | 值 |
|---|---|
| 实例 | `20_2_3_1` |
| seed | `20260808` |
| population | 100 |
| 预算 | 20,000 / 100,000 FE |
| 解码 | FM3，单族，`SEQUENCE_INDEPENDENT`，`ShiftMode=NONE` |
| 模式 | `V35_FULL`（CFVF+Qg/Qp+DSCR+档案+CA-TA-Lite+分块冻结） |
| 审计器 | `cmax-audit-v3-v35-lifecycle`，checkpoint 每 1000 FE |
| 初始种群 SHA-256 | `07311d31f51e6a71efcbf70435bf8924c02cb8be302023ddeed7f86c2ebca01b` |

## 100k 纪录链概览（56 条）

| 来源 | 条数 | 说明 |
|---|---:|---|
| INITIAL | 4 | 328.50 → 261.26（初始种群内严格递减） |
| FIXED_VNS/O1_O9 | 50 | 全部搜索期新纪录的主体 |
| INTER_FACTORY/INSERTION | 1 | FE 5751，Cmax=243.84 |
| CA_TA_LITE/N1 | 1 | FE 5120，Cmax=250.06，**未进入候选集** |
| **CFVF** | **0** | 20,000 次全局后代评价零新纪录 |

## 生命周期阶段（Generation→Admission→Survival→Exploitation）

```text
生成 56 → 进入候选集 54 → PDDR 保留 9 → 个人档案 9 → 全局档案 10 → 下一轮存活 2
```

- 2 条未进入候选集：CA_TA_LITE/N1（FE 5120）与一条 O1_O9 未被接受的候选（FE 61146，Cmax=198.139）；后者仍刷新了审计窗内纪录但从未进入任何档案。
- 大量纪录 `nextRoundSurvival=PENDING`：审计器只索引严格新纪录，未对全部候选建立生命周期状态（V35-P19 待办），不得把 PENDING 解释为"被淘汰"。

## G1 教师使用对照（核心问题）

| 纪录 | Cmax | 产生 FE | 首教 FE | 延迟 FE | 社会使用（粒子次/代） | 个人使用（粒子次/代） |
|---|---:|---:|---:|---:|---:|---:|
| rec4 (INITIAL) | 261.263 | 47 | 100 | 53 | 40 / 1 | 2 / 1 |
| rec17 (O1_O9) | 220.456 | 7000 | 27964 | 20,964 | 20 / 1 | 5 / 3 |
| rec39 (O1_O9) | 205.653 | 8820 | 28064 | 19,244 | 20 / 1 | 5 / 3 |
| rec49 (O1_O9) | 198.173 | 33995 | 56011 | 22,016 | **200 / 1** | 296 / 2 |
| rec56 (O1_O9，最终) | 195.244 | 62657 | 84304 | **21,647** | 100 / 1 | 5 / 1 |

其余 50 条纪录的社会教师使用为 0。

**判定**："在全局档案中"远不等于"被 G1 社会引导实际使用"。新纪录从产生到成为 G1 社会教师平均滞后约 2 万 FE；教学链由旧纪录主导（rec49 教学 200 粒子次直至 FE 85304，此时更低的 rec56 已产生 2.1 万 FE）。该模式与旧语义 P9.1（201.279 从未成为 G1 社会教师、205.902 教学 34 代）一致——**新语义下现象复现**。

注意：DSCR 只替换"被严格支配"的缓存教师。rec49（198.173）与 rec56（195.244）若互不严格支配（如 rec56 的 TEC/TWC 更差），DSCR 合法地不替换——这是规范的保守设计，不是缺陷；但 G1_CMAX 子群的 Cmax 方向引导因此长期使用非最优 Cmax 教师。

## 曲线（checkpoint 1000 FE）

- BestGlobal/BestGenerated/BestSurvived：195.244（FE 62657 后冻结）；
- `currentG1`：末尾 3 个 checkpoint 为 250.375 → 233.208 → 256.635——G1 当前种群在历史极值附近失去稳定性，搜索后期不再逼近最优。

## 行为隔离证明

审计修复（新增三处 `observeGenerated` 调用）只增旁路观测：
- 修复前后 20k FULL 的 front.csv 逐位一致（66 行、min Cmax/TEC/TWC 完全相同）；
- `ZhangBoCmaxAuditTest`/`V35FairRunnerTest`/`V35FairComparisonSmokeTest` 共 8/8 通过；
- FE 计数与机制摘要不变。

## 边界

- 单实例单 seed，不构成统计结论；
- 审计只索引严格新 Cmax 纪录，全候选生命周期索引属 V35-P19；
- CFVF 零新纪录是"对 Cmax 的直接贡献为零"，不等于 CFVF 对 TEC/TWC/多样性无贡献；
- 教师使用口径为"实际进入 CFVF 更新的领导"，资格不等同于使用。

## 证据文件

- `runs/full-20k/`：20k 母表（cmax-audit-records.csv 16 条、curves、summary、front、status）
- `runs/full-100k/`：100k 母表（56 条、curves、summary、front、status、dscr-*、ca-ta-lite-events.log）
- `evidence-sha256.tsv`：本目录全部证据哈希
