# FC-6A-POST / Build-C2：BP-PDDR 大规模稳定性诊断报告

- 日期：2026-08-19（服务器 inspur-NP5570M5，32 核/125G，stage5 批次 17:00–17:35 完成全部 12 跑）
- 阶段：FC-6A-POST（诊断阶段）。**Build C/C2 未修改任何算法行为**（证据见 §3、§4）。
- 交付代码：`V35Fc6BpPddrDiagnosticAudit.java`（纯观察审计，默认关闭）、`ZhangBoMOHPSOQ.java` 4 处只读 hook、runner 启用/落盘 8 行、`V35Fc6BuildCObservationTest` 等价测试
- 两个构建：C2-BP（`…BUILD-C2-BP-diag.jar`，BP-PDDR selector + 审计）/ C2-BASE（`…BUILD-C2-BASE-diag.jar`，原始 ranked-fill selector + 同一审计）
- 数据归档：本目录 `08-STAGE5-C2-DIAG/`（raw gz / parsed txt / tables CSV / fronts）；指标工具 `06-METRICS-TOOL/fc6_metrics.py`（paired + arm-stats + 双管线）

## 0. 裁决（先说结论）

**C4（其它机制）为主，叠加 C1（多边界过度干预）成分。C2（rescue 反馈本身不稳）与 C3（无结构性塌陷）被证伪。**

一句话因果链（100-job seed 20260824，500k FE，62 cycles）：

> BP-PDDR 的首个 rescue 事件（c17，CMAX 角色）使两臂随机流彻底去相关（两臂最终 front **零共享点**，三 seed 皆然）；此后 seed24-BP 进入一条与 baseline 不同的搜索轨迹，在该轨迹上出现「**同一 Cmax/TEC/TWC 边界极值被逐轮重复救回**」（c37–46 Cmax 686.26 连续 8 轮、c48–56 TWC 88848.8 连续 6 轮、c53–60 Cmax 682.73 连续 6 轮）与「**角色互踩**」（c51–53 TWC rescue 挤掉的 displaced 恰是上一轮 Cmax 边界极值，见 §8 表）；这些被反复救回的极值长期占据 population 槽位并经 Qg 教师/CFVF gbest 通道垄断学习信号（G2 群对 rescued 解的 gbest 学习 7548 次，为 seed23-BP 同项 170 的 44 倍），使 population 中段支配解的生成/存活被持续挤压；archive 接受率从 c58 起崩落（c58 accept=37 → c60=33 → c62=52 且单轮 −57），最终 archive 418→213（对照 BASE 同期 422→513）。**极值全部保住**（minCmax 689.34→682.73、minTEC 48134→47709、minTWC 87333→86596，BP 全面更优）**但中段密度坍塌**（front 513→213，IGD +28.1%）。

四个预注册假说逐一裁决（判定标准见 §15）：

| 假说 | 裁决 | 关键证据 |
|---|---|---|
| C1 多边界过度干预 | **部分成立（次因）** | 角色互踩 c51–53；TWC/TEC 边界槽与 Cmax 边界槽在 population 拥挤时互相挤占（displaced 分析）；但 20-job rescue 数几乎翻倍（62/86/71 vs 34/24/34）三 seed 全部过门 → 干预频次本身不是失稳判据 |
| C2 rescue 反馈本身不稳 | **证伪** | 全部 3 seed × 2 实例的 rescued 解 **100% 进入最终 front**（15/15、10/10、14/14；20-job 同样 100%）；每个 rescue 的知识都兑现了，问题不在单个 rescue 的反馈回路 |
| C3 无结构性塌陷 | **证伪** | seed24-BP 出现明确的失稳结构：archive 单轮暴跌（−113/−111/−57）+ G2 学习垄断（44×）+ 重复救回 20 轮次；这是结构性动态，不是均匀噪声 |
| C4 其它机制 | **成立（主因）** | 三 seed BP/BASE front 零共享点 → 两臂在首个 rescue 后即不可比；失稳由「重复救回 × 教师垄断 × 中段挤压」的耦合动态驱动，单 rescue 机制工作正常 |

## 1. 任务与背景

FC-5.2 确认 PDDR Boundary-Loss 根因（best-ever Cmax 174.44/169.63/191.21 全部死于 score 排名挤出）；FC-6 Build B 实现 BP-PDDR（≤3 个 q==0 边界槽保留）。Stage 2（20-job）全门通过；Stage 3（100-job）触发 veto：seed24 paired ΔHV=−6.76%/ΔIGD=+28.10%（corrected 管线 −6.94%/+28.08%）。本阶段任务：解释「seed22/23 改善、seed24 退化」的分野，并对 C1/C2/C3/C4 四假说裁决，为 FC-6B 路线提供证据。本阶段禁止实现任何 FC-6B 要素（未违反，见 §10 声明）。

## 2. 指标口径（STEP 1/2 交付）

- **paired**：严格逐 seed 配对 `deltaHV_s=(HV_BP_s−HV_BASE_s)/HV_BASE_s`；IGD 负数=改善。与用户复算逐位一致。
- **corrected pipeline**：raw 去重 → raw nondominated → 6-front union 统一 min/max → normalize（**不 clamp**）→ HV（rx=ry=rz=1.1 扫描线，坐标先收敛到积分盒，这不是拓扑过滤）→ IGD。old pipeline（normalize 后 clamp 到 [0,1]）会拍平支配点制造伪支配：seed24-BASE 513 点被 clamp 后只剩 389。
- **方向稳定性**：两管线下逐 seed 方向与 arm-level 中位数方向完全一致（100-job HV/IGD 双退化、20-job 双改善）。**corrected 未改变任何开发结论**，仅修正幅度。
- runtime：BP 493–526s（100-job）、258–340s（20-job）；baseline 100-job 无本地 runtime（历史批次未留 runNanos，如实注明）。

### 2.1 100-job 双口径指标总表

| seed | 管线 | BASE HV | BP HV | ΔHV | BASE IGD | BP IGD | ΔIGD | 判定 |
|---|---|---:|---:|---:|---:|---:|---:|---|
| 20260822 | old | 0.4726 | 0.5130 | +8.56% | 0.1651 | 0.1425 | −13.69% | 改善 |
| 20260822 | corrected | 0.4719 | 0.5109 | +8.25% | 0.1651 | 0.1425 | −13.67% | 改善 |
| 20260823 | old | 0.6485 | 0.6748 | +4.06% | 0.0482 | 0.0381 | −21.04% | 改善 |
| 20260823 | corrected | 0.6485 | 0.6742 | +3.97% | 0.0482 | 0.0381 | −20.81% | 改善 |
| 20260824 | old | 0.5151 | 0.4803 | −6.76% | 0.1377 | 0.1764 | +28.10% | **VETO** |
| 20260824 | corrected | 0.5140 | 0.4784 | −6.94% | 0.1378 | 0.1765 | +28.08% | **VETO** |

### 2.2 20-job 双口径指标总表

| seed | 管线 | BASE HV | BP HV | ΔHV | BASE IGD | BP IGD | ΔIGD |
|---|---|---:|---:|---:|---:|---:|---:|
| 20260822 | corrected | 0.8969 | 0.9347 | +4.2% | 0.0406 | 0.0320 | −21.2% |
| 20260823 | corrected | 0.9375 | 0.9023 | −3.76% | 0.0265 | 0.0409 | +54.3%* |
| 20260824 | corrected | 0.8745 | 0.8997 | +2.9% | 0.0576 | 0.0435 | −24.5% |

*20-job seed23 逐 seed ΔHV −3.76% 超 2% 线、ΔIGD +54.3%（幅度大但绝对 IGD 0.0409 仍极低）——口径敏感点如实记录；arm-level 中位数方向仍为 HV 改善。20-job 预注册门（FC-6B 验收门尚未生效，本阶段仅记录）在 arm 中位数口径下通过。

## 3. Build C2 行为中性验证（STEP 4 交付）

**三层证据**：

1. **字节码级**：C2-BASE jar 内 `ZhangBoEvaluatedPddrSelector.class` sha256 == 服务器历史 BUILD-A（baseline）jar 同类 `14040a20…`；C2-BP jar 同类 == BUILD-C jar `8e70ad91…`。即 C2-BASE 与 C2-BP 分别精确复现 baseline/BP 算法本体，唯一差异为观察代码。
2. **本地等价测试**：`V35Fc6BuildCObservationTest`（OFF/ON/ON 三连 20k FE 回放）front sha256 三者全等、FE 全等、diag 文本非空。全量回归：11/11 绿（唯 P10.1 冻结测试已知红，与本次改动无关，FC-6B 后统一处理）。
3. **服务器 12/12 front sha256 全部命中历史**：

| 实例 | 臂 | seed 20260822 | 20260823 | 20260824 |
|---|---|---|---|---|
| 100-job | C2-BASE | fcb2dc88（=BASE 357 行） | 2920d9ea（362） | 5feb4c4b（513） |
| 100-job | C2-BP | c95525a1（=BP 440 行） | 55133b73（391） | c6a8a11e（213） |
| 20-job | C2-BASE | aee14130（622） | f8c23aa1（613） | 47f41524（601） |
| 20-job | C2-BP | 793c64e4（604） | 60a20486（796） | 99ecd83a（585） |

行数与 Stage-1/2/3 历史完全一致；算法确定性（controlled start + 确定性随机序）下逐字节复现 == 观察代码零行为影响。**「Build C 未修改任何算法行为」就此实证。**

## 4. 审计覆盖与自校验

- PDDR 轮数：全部 12 跑 = 62 轮（与 formalOuterCycles 一致）。
- **BASE 臂 12 跑 rescue 恒 0、displacement 恒 0**：counterfactual（S_original=(score,originalOrder) 前 targetSize）在原始 selector 下逐位复现实际选择——反事实基线自校验通过。
- boundary pool：186 = 62 轮 × 3 角色槽（cmax/tec/twc 各 62），multiRole 计数见 §7。
- 曝光表只登记被 rescue 的 fingerprint（BASE 臂无 rescue → 曝光恒 0），符合设计。

## 5. R_retain 与 rescue 频次总表

boundary 候选（q==0 三向极值）共 186/臂/seed；实际 rescue（不在 S_original 内被 BP 保留）：

| 实例 | seed | rescues | cmax | tec | twc | displaced | R_retain 隐含 |
|---|---|---:|---:|---:|---:|---:|---|
| 100-job | 22 | 34 | 21 | 12 | 1 | 21 | 34/186=18.3% |
| 100-job | 23 | 24 | 18 | 3 | 3 | 7 | 12.9% |
| 100-job | 24 | 34 | 17 | 5 | 12 | 20 | 18.3% |
| 20-job | 22 | 62 | 32 | 10 | 20 | 50 | 33.3% |
| 20-job | 23 | 86 | 44 | 5 | 37 | 57 | 46.2% |
| 20-job | 24 | 71 | 50 | 7 | 14 | 55 | 38.2% |

**关键反差**：seed22 与 seed24 的 rescue 总数相同（34）而结局相反；20-job rescue 数近两倍却全过门。→ **失稳与 rescue 频次无关，与 rescue 的时序/角色构成/后续动态有关。**

## 6. 时间线口径（预注册）

- rescue 事件记于其 PDDR 轮所在 cycle c（`formalBaselineOuterCycles` 当值，audit 从 applyEvaluatedPddr rebuild 后读取，与下一轮 population 对齐）。
- 几何快照 label c+1（L774 递增后、进入下一轮前的 swarm/archive 状态）。
- Qg/CFVF 曝光记于发生轮（被救解进入 archive 后最早曝光机会为 c+1，与几何 c+1 对齐）。
- 两臂对比同 cycle 编号直接对齐（确定性算法、同 FE 预算、同 cycle 数 62）。

## 7. 角色重叠与 multiRole

| run | multiRole 轮数/186 | 说明 |
|---|---|---|
| 100-BASE-22/23/24 | 2/0/6 | baseline 中边界极值大多由不同解分别占据 |
| 100-BP-22/23/24 | 20/4/6 | seed22-BP 出现 20 轮 multiRole（同一解同时是 minCmax+minTEC 等）——但该 seed 结局最好，**multiRole 本身无害** |
| 20-BP-* | 0 | 小池不重叠 |

## 8. seed24 失稳时间线（100-job，逐段叙事）

**起点 c17**：首个 rescue（CMAX，Cmax=697.16，origRank=102，挤掉 dRank=99 的 dScore=0.067 中段解）。此事件后两臂随机流分叉。c1–c16 两臂 archive 曲线本已可见差异（c11 BP 甚至领先 −45），说明轨迹分叉不等于失稳。

**平稳段 c17–c36**：两臂 archive 交替领先（gap −45…+175 波动），BP 并未单调恶化；rescue 稀疏（c32 TWC 1 次）。

**第一失稳段 c37–c46（重复救回形成）**：
- Cmax 686.26 在 c37/38/39/40/41/43/44/46 被救回 **8 次**（origRank 106–133，每轮都排不进前 100，靠边界槽存活）；
- TWC 687.30/90067.6 与 700.50/89978.4 在 c38/39/41/44 被救回；
- displaced 的 dScore 全部 ≈0.04–0.09（中段支配解），dRank 98–99。
- 同期两臂 archive gap 从 61 收敛到 −26（c43 BP 反超）——**此时失稳尚未显性化**。

**第二失稳段 c47–c54（教师垄断显性化）**：
- c47：BP archive 单轮 −113（418→305）。同周期 teacherSel/gbest 学习启动（c49: 50/812；c50: 48/828）。
- 曝光分组数据：rescued CMAX 解从 G2 群获得 **454 次 Qg 教师 + 7772 次 gbest 学习**；对照 seed23-BP 同项 16/412、seed22-BP 186/3352。**G2（TEC 群）在 seed24 中被 Cmax 边界解垄断教师位**——G2 的 gbest 学习 7548 次，是 seed23 的 44 倍。该解 TEC=57771（池内 TEC 第 62 名开外），作为 G2 教师在 TEC 维度是劣质信号。
- c51–53 **角色互踩**：TWC rescue 的 displaced dCmax=686.14/686.26——正是前段反复救回的 Cmax 边界极值；c53 Cmax rescue 的 displaced 又是 TEC 边界解（dTEC=48309）。三类边界槽在 population 拥挤期互相淘汰。

**第三失稳段 c55–c62（接受率崩落与终局）**：
- archive accept 逐轮下滑：c58=37 → c60=33 → c61=78 → c62=52；c54（−111）、c62（−57）两次单轮暴跌；终局 archive=213（BASE=513）。
- popND 持续=100（population 全非支配，中段解在 population 内已无生存空间）；popRngCmax 终局 416（三 BP 臂最大）——种群被边界拉宽的同时 archive 中段坍塌。
- 曝光侧 teacherSel 在 c49–62 几乎每轮 34–50 次、gbest 学习 300–900+ 次：垄断持续到终局。

**失稳开始点判定：c37（第一个被重复救回的 Cmax 极值出现的轮次），显性化点 c47（首次 archive 单轮 −113 + 学习通道起飞）。c1–c36 无失稳证据。**

## 9. seed22/23 正常对照

- **seed22-BP（结局最好，ΔHV +8.25%）**：c34 起同样有 Cmax 702.00 的连续重复救回（c38–c62 约 15 轮）与 TEC 重复救回；G2 gbest 学习 3338 次也不低。但 displaced 的 dScore 中位数更高（~0.045 vs seed24 ~0.05 差异微小），且**没有角色互踩**（TWC 仅 1 次 rescue，从未挤掉 Cmax/TEC 极值）。archive 终局 440 > BASE 357：重复救回的极值在 seed22 里撑起了更大的边界 HV，而中段未受损。
- **seed23-BP（ΔHV +3.97%）**：rescue 最少（24），无重复救回链条（同一指纹至多 3 轮），无角色互踩；G2 学习量最低（170）。**最干净的对照**——rescue 少而分散 = 稳。
- 分野总结：**seed22/23 的 rescue 是「一次性事件」；seed24 的 rescue 是「驻留态+互踩」**。同为 34 次 rescue，seed24 有 20 次发生在重复救回链条内、且边界槽互踩 3 次以上。

## 10. Build C 合规声明

- **Build C 未修改任何算法行为。** 全部新增代码为纯观察（只读 hook、静态 enable 门、不进决策路径、不消耗随机数）；OFF/ON 等价测试 + 12/12 服务器 front sha256 双重实证（§3）。
- 本阶段未实现 Conditional Cmax Rescue 或任何 FC-6B 要素；未修改 PDDR score 公式；未调整任何参数（MAX_BOUNDARY_SLOTS=3、阈值、cooldown、diversity 条件均未动）。
- STEP 1/2（指标脚本与管线）为离线数据处理，不触算法。
- stage4 的 xargs jobs 追加 bug 已在 stage5 修正（每批次独立 jobs 文件）。

## 11. 双口径指标表

见 §2.1/§2.2（逐 seed + 双管线）。FC-8 正式统计接口（Wilcoxon 配对）已预留于 fc6_metrics.py（--json 输出），3 seed 不做显著性结论。

## 12. 与 FC-5.2 / Stage-2/3 结论的一致性

- FC-5.2 的 Boundary-Loss 根因成立且 BP-PDDR 确实修复了它：三 seed minCmax/minTEC/minTWC **BP 全部更优**（100-job：minCmax 695.4/727.3/682.7 vs 720.8/737.6/689.3）。
- Stage-2（20-job）门通过维持；Stage-3 veto 维持（本诊断不推翻 veto，而是解释它）。
- 增量发现：边界知识的「保留」不是终点——保留后的**社会学习通道分配**（谁当教师、被谁学）决定它是增益还是毒药。seed24 的 G2 群 44 倍学习垄断是此前所有阶段都不可见的机制。

## 13. 局限

- 3 seed 无法做显著性分离；本报告全部为确定性单跑的机制性结论（算法确定性使重跑无意义，但 seed 间外推需谨慎）。
- teacherSel/gbest 学习的「因果性」是观察性关联：无法在不违反 Build C 约束的前提下做反事实干预（那是 FC-6B 的实验）。c47 暴跌与学习垄断的时序耦合（暴跌先于学习起飞一轮）支持但不证明因果方向。
- displaced 配对按 S_BP 槽位序 × S_original 排名序对齐，NaN 行 = 无 displaced 配对（rescued>displaced 轮次），报告口径已在 §6 预注册。
- BASE 100-job 无 runtime（历史批次未留）；不影响本阶段任何结论。

## 14. 对 FC-6B 路线的含义（仅建议，不实现）

裁决 C4 主导 + C1 成分指向的修正方向（按证据强度排序，全部待用户裁决后进入 FC-6B）：

1. **教师侧治理（针对主因 C4）**：rescued 边界解进入社会学习通道前做角色-维度匹配（Cmax 极值不应垄断 G2/TEC 群教师位 44 倍）。候选：教师维度适配门 / rescued 解教师资格冷却期。
2. **重复救回治理（针对 C1 成分）**：同一指纹连续被救 ≥K 轮（如 4）视为「驻留」信号，转为允许淘汰或降权（它已失去「会被淹没的新知识」属性，成为占位者）。20-job 高频 rescue 无害、100-job 重复链条有害 → 治理目标是重复性不是频次。
3. **Conditional Cmax Rescue（用户预注册选项 A）**：本证据下不推荐作为首选——Cmax rescue 本身在三个 seed 都工作（全部进终局 front），问题在它的**曝光放大**；条件化 Cmax 只会削弱已验证有效的部分。
4. **Cmax Knowledge Memory / Archive-Population Decoupling（选项 B）**：被动 archive 不解决教师垄断（rescued 解已在 population 内被学习），且可能进一步减少中段生成。与本证据方向相悖。

## 15. 判定标准预注册（已于分析前固定）

- C1 成立要件：角色互踩/边界槽互挤证据 + 干预频次与失稳相关。
- C2 成立要件：rescued 解未能进入 front 或 rescue 后代灭绝。
- C3 成立要件：无重复救回、无学习垄断、archive 波动与 baseline 同分布。
- C4 成立要件：两臂零共享点（去相关）+ 失稳由跨机制耦合动态（重复救回×教师分配×中段挤压）驱动且单机制假说不充分。
- 实测：C2/C3 证据反向（rescued 100% 进 front；重复救回+垄断明确存在）→ 证伪。C1 部分成立（互踩有、频次无关）。C4 成立（零共享点 + 耦合动态完整链条）。

## 16. 数据与复现

- 原始 12 组 mechanism-summary（含完整 fc6Diag 段）：`08-STAGE5-C2-DIAG/raw-100job/*.gz`、`raw-20job/*.gz`（服务器 `/home/inspur/aicomp/zhangbo-fc6-20260818/results/stage5-c2/`）
- 解析表（cycles/rescues/exposures CSV）：`08-STAGE5-C2-DIAG/parsed-*/tables/`；解析器 `parse_fc6diag.py`
- 12 组 front.csv + console.log：`08-STAGE5-C2-DIAG/fronts/`
- 指标 JSON：`06-METRICS-TOOL/fc6-100job.json`、`fc6-20job.json`
- jar 与脚本：服务器 `jars/jmetal-exec-5.8-BUILD-C2-{BP,BASE}-diag.jar`（sha256 29e2aa4b/67b91008）、`fc6-stage5-c2.sh`；本地 `张博改进/build-artifacts/c2/`
- 服务器运行日志：`logs/stage5-task.log`（12 行完成记录）、`stage5-{started,completed}.txt`

## 17. 预注册 FC-6B 验收门（仅记录，Build C 阶段未用于任何决策）

- 20-job/500k/3 seeds 与 100-job/500k/3 seeds；Cmax median 明显优于 baseline；HV median 退化 ≤2%；IGD median 退化 ≤10%；不得再现单 seed HV<−5% 且 IGD>+20% 失稳。**本阶段未据此改动任何东西。**

## 18. 结论

BP-PDDR 的单次 rescue 机制在全部 6 个 BP 运行中 100% 兑现知识（rescued 解全部进入最终 front、三目标极值全面优于 baseline）；seed24 的失稳不是 rescue 失败，而是 rescue 成功后的**社会学习再分配**问题：重复救回的驻留极值通过 G2 教师位垄断（44 倍学习量）扭曲了种群的学习信号，配合边界槽互踩，把中段支配结构的生成挤压到崩溃（archive 513→213）。裁决 **C4（主）+ C1（次）**。FC-6B 的第一刀建议指向**教师通道治理与重复救回治理**，而非削弱 Cmax rescue 本身。
