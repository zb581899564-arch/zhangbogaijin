# FC-5：Cmax 生命周期四层审计报告（2026-08-18）

> 实验：20_2_3_1 / 500k / seed 20260822,23,24 / PACING 正式配置 / 本地串行。纯观察旁路（V35Fc5CmaxLifecycleAuditTest 1/1 通过：重复重放 front hash+FE 一致）。
> 对照：QGS minCmax 中位 184.3；本批 pacing 三 seed 最终 minCmax = **188.39 / 175.70 / 195.70**（与 FC-2 存档逐位一致）。

## 0. 一句话结论

**好 Cmax 解能产生（Q 轮与 LS 都产过 175–182 级）、能进 archive（179.77–188.79 被长期记住）、能被接受——但种群无法围绕已知好 Cmax 持续开发（Exploitation 弱）。seed 差异的本质是"种群是否贴得住 archive 里的好解"：seed23 贴着（176.87 vs archive 175.70）所以最终 175.70；seed24 离着（swarm 257–274 vs archive 195.70）所以最终 195.70。**

## 1. 四层漏斗

### 1.1 Generation——能产生（✅ 通过）

每 cycle 的 genQrounds（Q 轮/CFVF 后 swarm bestCmax）与 genFinal 可低至：

| seed | 历史最低 genFinal | 出生算子（Top-5 统计） |
|---|---|---|
| 22 | 196.08 | VNS 为主 |
| 23 | **176.87（cycle59-62 稳定）**；cycle10 曾现 180.87 | VNS 9/12、GLOBAL_OFFSPRING 3/12 |
| 24 | 204.64（cycle5） | VNS 为主 |

- **180/182 级好 Cmax 确实被生成过**（seed23 cycle10 genFinal=180.87，seed23 最终 175.70 亦出自 Q 轮+LS 链）。
- Top-5 Cmax 解出生算子统计：**INTRA_FACTORY_VNS（LS）占绝大多数，GLOBAL_OFFSPRING（CFVF Q 轮）仅少数**——好 Cmax 主要诞生于 LS 深挖，而非 CFVF 广搜。
- 注：CA-TA/LS 候选延迟到 PDDR 环境选择才入群，故 genCaTa/genLs 列与 genQrounds 相同属设计使然，不构成 CA-TA/LS 无产出的证据。

### 1.2 Admission——能接受（✅ 通过）

每 cycle 的 nextPop（PDDR 环境选择后）**系统性优于** genQrounds：

- seed22：genQrounds 197–240 → nextPop 196.08 稳定；
- seed24：genQrounds 257–274 → nextPop 221–245（PDDR 每轮从候选池捞回 30–50 单位）。

候选好解被接受并改善 swarm——接受机制没有挡路。

### 1.3 Survival——archive 记得住（✅ 通过）

archive 列（archAfter）稳步下降并被长期钉住：seed22 → 188.39、seed23 → 175.70、seed24 → 195.70（各保持 30–50+ 轮不变）。**好 Cmax 解进了外部 archive 且不被遗忘**。

### 1.4 Exploitation——种群贴不住已知好解（❌ 短板）

最关键的 seed 间对比（cycle 59–62）：

| seed | genQrounds | nextPop | archive | 差距（swarm↔archive） |
|---|---:|---:|---:|---:|
| 23 | 176.87–186 | 176.87 | **175.70** | ≈1.2（贴着 ✅） |
| 22 | 197–240 | 196.08 | 188.39 | ≈8–50（漂移 ⚠️） |
| 24 | 257–274 | 221–245 | **195.70** | **26–78（失联 ❌）** |

- seed24 的种群每轮 Cmax 都离 archive 已知好解（195.70）60 个单位以上且从不收敛回去——**好解被记住但没有被种群围绕开发**。
- seed23 的种群恰好稳定在 176.87（≈archive 175.70）——最终 175.70。
- **种子差异 = 种群能否围绕已知好 Cmax 持续开发**，与"会不会找好 Cmax"无关（都会找）。

## 2. G1 条件 GIR（四向量协同，无单向量主导）

| seed | 全体 P(JS/FA/MA/WA) | Cmax 改善解 P(JS/FA/MA/WA) | 差异 |
|---|---|---|---|
| 22 | .884/.837/.841/.816 | .894/.887/.883/.853 | +1~+5pt |
| 23 | .921/.871/.890/.855 | .920/.901/.916/.880 | −0~+3pt |
| 24 | .978/.949/.959/.957 | .975/.949/.960/.958 | ≈0 |

**结论：Cmax 改善是四向量协同结果，无任何单向量特异**——不支持"CFVF 把搜索浪费在 MA/WA"之类的假设，也不支持 FC-6 走"强化单向量"路线。

## 3. Lineage 生命周期（Top-5 Cmax 解）

- 出生：VNS 为主（见 1.1）；
- **寿命短：lastSeenCycle 1–4**——Top-5 Cmax 解出生后 1–4 个 cycle 就掉出种群 Top-5（被变异/替换）；
- teacher 曝光存在：部分好 Cmax 解被选为 Qg teacher 并被学习 378–2877 次（cfvfUsedCount）——**曝光有，但学习后粒子未向 Cmax 收敛**（多目标折衷使然）；
- 诚实标注：enteredPool/survived 两列因 markPool/markSurvived 钩子未接而恒 false（不影响四层结论——漏斗列已覆盖 pool/nextPop/archive 事实）。

## 4. FC-6 四选一建议

| 分支 | 判据 | 结论 |
|---|---|---|
| Generation（G1 CFVF/N3 强化） | 好 Cmax 产生不足？ | ✗ 能产生（176–182 级） |
| Admission（Cmax 接受放宽） | 好解被挡？ | ✗ nextPop 每轮优于 genQrounds |
| Survival（四方向精英保留） | 好解被杀？ | ✗ archive 记住且长期钉住 |
| **Exploitation（G1 teacher exposure / Cmax lineage 复用）** | 好解被持续开发？ | **✓ 种群贴不住 archive 好解（seed24 差距 26–78）** |

**建议 FC-6 只修 Exploitation 一支**：让 G1 组围绕"已知最好 Cmax 解"持续开发——例如 G1 的 CFVF 学习中把 archive 内最优 Cmax 解作为（或混入）gbest 教师池，或对 top-Cmax lineage 增加再访机制（如将其后代重新注入 G1）。具体形式待 FC-6 设计（单支、不改三创新、过 20/100×3 seed 验收）。

## 5. 证据

- 运行：`fc5-cmax-audit/runs2/seed-{20260822,20260823,20260824}/`（front.csv + mechanism-summary.txt 含 cmaxLifecycleAudit 块）
- 测试：`V35Fc5CmaxLifecycleAuditTest`（1/1，行为中性）
- 代码：`V35CmaxLifecycleAudit` + ZhangBoMOHPSOQ 插桩（漏斗/GIR/lineage 钩子）