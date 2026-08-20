# FC-TIME-1 模块耗时账——第一版报告（2026-08-18）

> 运行：20_2_3_1 / 50k FE / seed 20260822 / population=100 / 本地串行（zbdepc JDK17 单 JVM -Xmx4g）。插桩为纯旁路（V35Fc1ModuleTimerTest 已验证打开/关闭行为逐位一致）。模块口径见 `V35ModuleTimer`；CFVF 含 DSCR（updatePosition 的 CFVF 路径内嵌 DSCR 过滤）；Other=updateVelocity 组建/排序/select 混合段 + 循环基建。

## 1. 总览（50k，pacing vs legacy）

| 模块 | pacing 时间 | pacing 占比 | legacy 时间 | legacy 占比 |
|---|---:|---:|---:|---:|
| CFVF（含 DSCR） | 15.37s | **68.6%** | 2.90s | **47.2%** |
| Dominance（个人史剪枝） | 2.94s | 13.1% | 0.60s | 9.7% |
| Other（updateVelocity/select 混合） | 1.46s | 6.5% | 0.40s | 6.4% |
| FM3Decode | 0.75s | 3.3% | 0.33s | 5.4% |
| InterFactoryLS | 0.60s | 2.7% | 1.40s | **22.7%** |
| Qp | 0.61s | 2.7% | 0.17s | 2.8% |
| CA-TA Test/Apply 整段 | 0.27s | 1.2% | 0.18s | 2.9% |
| PDDR_FF | 0.27s | 1.2% | 0.13s | 2.0% |
| Archive | 0.07s | 0.3% | 0.03s | 0.4% |
| Qg | 0.04s | 0.2% | 0.02s | 0.3% |
| SolutionCopyRepair | 0.03s | 0.1% | 0.01s | 0.1% |
| AuditLogging | 0.00s | 0.0% | 0.00s | 0.0% |
| **合计/总墙钟** | 22.9s（模块和 22.4s） | 98% | 6.6s（模块和 6.1s） | 93% |

## 2. 关键发现

### 2.1 CFVF 是绝对大头，且随 archive 规模二次方增长

pacing 下 CFVF 占 68.6%。按 cycle 分解（每 cycle 50 次 Q round，每次 updatePosition 计入一次 CFVF）：

| cycle | archiveSize | CFVF 本轮总耗时 | 每 Q round 平均 |
|---|---:|---:|---:|
| 1 | 56 | 1.38s | 27.5ms |
| 2 | 95 | 1.61s | 32.2ms |
| 3 | 97 | 1.79s | 35.8ms |
| 4 | 139 | 2.03s | 40.7ms |
| 5 | 191 | 3.33s | 66.7ms |
| 6 | 224 | 5.22s | 104.4ms |

**每 Q round 的 CFVF 耗时随 archiveSize 从 27ms 涨到 104ms（4 倍）**——典型的"成本随 pool/archive/front 规模增长"曲线，与"18 轮 6.7s/cycle vs 62 轮 17.6s/cycle"的现象直接对应：循环数 3.4×，每循环又因为 archive 更大而变贵，二者相乘构成 8× 总差。

### 2.2 pacing 相对 legacy 的净效果

- legacy 50k：6.6s，CFVF 47.2% + LS 22.7%（LS 一次吃 81% FE 预算的解读：2 个 cycle × 每次 0.7s）。
- pacing 50k：22.9s，CFVF 68.6% + LS 2.7%（LS 被截断成 6 段，但 CFVF 被多调用 300 次且每次更贵）。
- **pacing 的代价本质不是 LS 变贵，而是 CFVF 更新路径在主循环里的次数×单价暴涨**。

### 2.3 修正初步判断

探查阶段曾推断"PDDR/Archive/Dominance 最可疑"——第一版账显示：Archive 仅 0.3%、PDDR 1.2%、个人史 Dominance 13.1%；**CFVF 更新路径（updatePosition→updateCfvfGroup 链）才是首号目标**。此外 select() 的支配比较混在 Other(6.5%) 里未单列，第二版账需拆分。

## 3. 第二版账计划（拆分 CFVF 内部）

1. `updateCfvfGroup`（G1/G4/G2/G3 四组）内部细拆：gbest 支配扫描 / 候选生成 / 支配过滤 / DSCR 过滤（applyV35Dscr 在 candidates 生成处 :2342）。
2. `updateVelocity` 内部拆出 `select()` 的全局锦标赛支配比较（计入 Dominance）。
3. 500k 正式账（三臂同机串行，入 FC-TIME-0 批次）验证 50k 结论是否延伸到长预算。

## 4. 决定

- FC-TIME-2 优先级重排：**A1 = CFVF 路径内部去重**（若发现重复计算；尤其 gbest 选择与支配过滤是否重复扫 archive）、A2 = 四热点支配去重（不变）、B = FM3 DAG 缓存（不变，FM3Decode 3.3% 收益小但零风险）。
- FC-TIME-2-C（Solution.copy）不投入：copy 仅 0.1%。

## 5. 证据

- 运行：`fc-time1-module-audit/{pacing-50k-profile,legacy-50k-profile}/`（front.csv + mechanism-summary.txt 含 moduleTiming 与 perCycleTiming 块）
- 测试：`V35Fc1ModuleTimerTest`（模块覆盖 ≥80% 门 + 行为不变门，2/2 通过）
- 代码：`V35ModuleTimer.java`、`ZhangBoMOHPSOQ.runFormalHmopsoQgsBaseline` 插桩点、runner `--profile-modules` 开关