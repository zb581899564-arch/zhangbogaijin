# FC-6A.2 审计规格：Region × PDDR Composition Audit（Go/No-Go 数据批）

日期：2026-08-20 ｜ 前置：FC-6A.1（情况 C 判定）、用户区域吸收提案（§1–13）
本批性质：**纯观察**。不改任何算法行为（selector 用原始版，同 C3-COMP 模式）。产出数据回答一个
问题：**Region-aware PDDR Environmental Selection 的 Go / No-Go**。

---

## 0. 对用户提案的三个事实修正（已核实代码，必须先对齐）

1. **分组容量不是 25/25/25/25，而是 15/55/15/15**（`ZhangBoMOHPSOQBuilder` L108-110：
   `upSize=15, upNewSize=15, centralSize=55, downSize=15`）。中心综合群占 55 席。这**强化**了
   区域吸收假设的可测性：方向解的"自然容量"只有 15 席/方向，比提案假设更稀缺。
2. **分组标签**（按代码真实顺序）：G1=Cmax 方向（`updateVelocity` 先取 Cmax-min 15）、
   G2=综合（PDDR score 排序取 55，**公式与 PDDR 逐字相同**）、G3=TEC、G4=TWC。
   提案中的 G2=TEC/G3=TWC/G4=Balanced 编号与代码不一致，审计表按代码标签。
3. **现有分群发生在环境选择之后**（`updateVelocity` 对 PDDR 选出的 100 解再分组），且
   sub2 的准入量就是 PDDR score 本身 --当前架构等价于"环境选择 = 全部 100 席按综合 score 竞争，
   方向结构事后补"。这就是提案 §8"架构一致性"论断的代码级根据。

## 1. 插桩点（唯一改动，纯观察）

`V35Fc6BpPddrDiagnosticAudit.recordPddrRound`（与 FC-6A.1 组成审计同一位置，scores[] 已在
手上）。新增 Region 分类，**镜像 `updateVelocity` 的贪心分配规则**（不发明新规则）：

```
greedyAssign(pool q=0 解, 容量 [15, 55, 15, 15]):
  G1: 按 (Cmax, TEC, TWC, 池序) 字典序取前 15 个 q=0 解
  G2: 从剩余按 (score, 池序) 取前 55 个
  G3: 从剩余按 (TEC, Cmax, TWC, 池序) 取前 15 个
  G4: 从剩余按 (TWC, Cmax, TEC, 池序) 取前 15 个
  剩余 q=0 解 = 溢出（有区域需求但四区皆满）
```

## 2. 每轮输出行（新增前缀 `fc6diagRegion`，不动现有行）

```
fc6diagRegion <cycle> TAB fe=<fe> TAB pool=<n> TAB nd=<q=0 数>
  TAB g1Lt1=<n> TAB g1Eq1=<n> TAB g2Lt1=<n> TAB g2Eq1=<n>
  TAB g3Lt1=<n> TAB g3Eq1=<n> TAB g4Lt1=<n> TAB g4Eq1=<n>
  TAB ovfLt1=<n> TAB ovfEq1=<n>          # 溢出解（q=0 但四区皆满）
  TAB rejG1=<n> TAB rejG2=<n> TAB rejG3=<n> TAB rejG4=<n> TAB rejOvf=<n>
      # 原版全局 PDDR 被拒解的区域归属（按 greedyAssign 标签）
  TAB absorbable=<n>                     # 被拒且非溢出的 q=0 解数
      #   = 区域分配下有位置（含本区或借用容量）的全局被拒解数
```

汇总行：`fc6diagRegionSummary rounds=<n> ovfRounds=<n> absorbRounds=<n>
  totalRejNd=<n> totalAbsorbable=<n> totalOvf=<n>`

容量借用模拟（离线由分析脚本做，Java 只给原始归属）：某区未满时，溢出解按字典序补位；
统计"借用后仍溢出"数（`postOvf`，分析脚本算）。

## 3. 174.44 反事实探针（随批观察，不专跑）

在 `recordPddrRound` 内对**预设目标三元组**（20-job seed22: Cmax=174.43665028596877,
TEC=11123.472680537456, TWC=15044.462631959621）做逐轮判定：若该三元组作为虚拟候选加入池，
（a）全局 PDDR 下是否入选（预期：绝大多数轮 No，与 FC-5.2 结论一致）；
（b）区域分配下是否获得席位（G1 字典序前 15 或借用位）。
输出：`fc6diagProbe174 <cycle> TAB global=<yes/no> TAB region=<G1|G2|G3|G4|ovf|no>`
汇总：`fc6diagProbe174Summary rounds=<n> globalYes=<n> regionYes=<n> regionG1=<n>`
（探针只读池内目标值做比较，不进任何决策路径；判它是否真被生成过由 fc52 记录另行对账。）

## 4. 运行矩阵（12 跑，与 stage6 完全同构）

- QGS 臂 ×6：`CorrectedComparisonRunner --algorithm HMOPSO_QGS_F` {20,100}_2_3_1 × 3 seed
- BASE 臂 ×6：`BudgetDiagnosticRunner`（ls=30 / g-block=5 / budget 0.25:0.65）同参数
- 单一 jar：**BUILD-C4-REGION（原始 selector + Region 审计）**；`-Dmaven.test.skip=true` 等
  同 C3-COMP 构建法；selector 临时换回原始版构建，构建后恢复 BP 版工作树（`.bk-bp` 备份在）

## 5. 中性验证门（沿用，7/7 预期）

- 6/6 BASE front sha256 == stage5 C2-BASE（同 stage6 数值）
- QGS 100-job seed22 front == 历史 `d193056a…`
- Region 审计与 FC-6A.1 组成行数量一致（BASE 62/跑、QGS 16-18/跑）

## 6. Go / No-Go 判据（预注册，跑前锁定）

**GO**（支持 Region-aware PDDR Environmental Selection）需同时满足：
1. 被全局 PDDR 拒绝的 q=0 解中，**absorbable 占比 ≥50%**（中位，BASE 100-job）--
   即被切的好解多数有区域归宿，不是纯溢出；
2. 174.44 探针：区域分配下 regionYes 轮占比明显高于 globalYes（如 ≥2 倍），
   且归属 G1（验证"它在 G1 内竞争而非与 100+ 综合强解竞争"）；
3. 溢出率 ovfLt1/nd 未随相位恶化到 >50%（否则只是把挤压换了位置）。

**NO-GO**（回到容量/teacher 路线）：absorbable 占比 <30%，或被拒解主要是 ovf
（区域自身也满），或 174.44 探针 region 与 global 无显著差。

中间地带（30–50%）：报告如实给出，FC-6B 方向由用户裁决（可组合：区域吸收 + 教师通道治理）。

## 7. 明确不做

- 不实现 Region-aware selection 本身（等 Go/No-Go）
- 不改 teacher / Qg / CFVF（提案 §8 的教师收益由探针+既有 stage5 数据间接推断）
- 不动 BP-PDDR 现有实现（工作树保持 BP 版 selector）
