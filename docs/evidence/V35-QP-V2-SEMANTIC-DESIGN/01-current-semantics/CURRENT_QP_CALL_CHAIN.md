# 当前 Qp 个人领导选择调用链（源码事实审计）

审计日期：`2026-09-02`（Phase B0.5）
审计对象：工作树 `java-jmetal58/`（与冻结Jar `8DAD8F40…BAD8B9` 同源的 v3.5 语义；B1 实现时必须重新指向冻结源码树，见 `V35-PFC5-TEACHER-EXPOSURE-CAL-PREREG/01-source-semantics/TEACHER_SELECTION_CALL_CHAIN.md` §5 的既有差异警示）。
性质：**只读**。未修改源码、未编译、未运行、0 FE。

---

## 1. 端到端调用链（含行号）

```text
ZhangBoMOHPSOQ 主循环
│
├─ prepareDualQCoordination()                            ZhangBoMOHPSOQ.java:2791
│    └─ zhangBoDualQCoordinator.decide(FE,max,swarm,roundCounter,warmupEnd)  :2797
│         → pendingDualQDecision ∈ {WARMUP, P_BLOCK, G_BLOCK}
│
├─ [WARMUP] selectWarmupGroup()                          :3759-3782
│    └─ zhangBoQpController.selectDirectionalWarmupGroup(...)
│         └─ selector.directional(memory.entries, group, bounds)   ZhangBoQpController.java:341
│              = argmin φ（无RNG、无Q表、无动作、无mask）              ZhangBoQpCandidateSelector.java:63-74
│
├─ [P_BLOCK/G_BLOCK/无冻结] selectQpGroup(group,...)     ZhangBoMOHPSOQ.java:3685-3734
│    ├─ selectionMode = isGBlock ? GREEDY_FROZEN : EPSILON_GREEDY   :3692-3695
│    └─ zhangBoQpController.selectGroup(...)             ZhangBoQpController.java:96-195
│         ├─ 逐粒子（:116-137）：
│         │    ├─ selector.build(memory.entries, requested, group, current, gbest, bounds)
│         │    │    → Candidates{candidates, mask, resolvedKeep}     Selector.java:25-61
│         │    ├─ resolved = candidates.getResolvedKeepFingerprint()  Controller:126
│         │    └─ particle.setAttribute(ZhangBoQpLineageState, resolved) :127-128
│         ├─ 冗余阈值 redundancyThreshold = max(0.80, median(ρ))      :139-141
│         ├─ evolutionNeed 由 previous/current 组统计差分得到          :142-146
│         ├─ exploration = ε(FE/maxFE)（0.30→0.05线性）               :147, :508-514
│         └─ 逐粒子选动作+候选（:149-192）：
│              ├─ state = stateIndex(E,H,R)（16状态）                 :150-154
│              ├─ [GREEDY_FROZEN] selectGreedyAction(frozen[state], mask)  :155-156
│              │    零RNG；首个合法动作起，严格更大Q才替换（并列取首个=KEEP）:501-506
│              ├─ [EPSILON_GREEDY] selectAction(...)                   :157-158 → :470-486
│              │    ├─ draw = random.nextDouble()          恒定1次    :473
│              │    ├─ draw < exploration → random.nextInt(0,|valid|-1) 仅探索分支 :477
│              │    └─ 否则 selectConfiguredGreedyAction（默认FIRST_VALID）:481,:488-499
│              ├─ selected = value.candidates.get(action)  ←★K轴唯一插入点（当前无RNG）:160
│              ├─ selectedDirectionalScore = φ(selected)              :162
│              ├─ eligibleBestDirectionalScore = min φ over 合法动作候选 :163-171
│              ├─ pbestSwitches++（selected≠resolved时）              :175
│              ├─ particle.setAttribute(ZhangBoQpLineageState, selected.fp) :177-178
│              │    → 下一轮 KEEP 的 requested fingerprint 即本轮 selected
│              └─ Selection{branchId,lineageId,group,state,action,mask,current,gbest,
│                   selectedPbest,previousArchive,noUpdateCount,stats,exploration,
│                   rho,rhoThreshold,selectedDirectionalScore,eligibleBest...} :179-183
│
├─ 消费 selectedPbest（K 改变领导identity后影响的所有下游）：
│    ├─ updateCfvfGroup(): personalLeader = decision.pbestSolution(current)
│    │    → CFVF四向量更新（认知引导向量）                ZhangBoMOHPSOQ.java:3454-3464
│    ├─ selectQpGroup(): teacher = selection.pbestSolution(particle)
│    │    → pendingQpTeacherSolutions + 教师遥测（含previousArchiveSize、
│    │       selectedDirectionalScore、eligibleBestDirectionalScore、directionalRegret） :3710-3727
│    └─ v35A2A3PersonalLeaderAudit.record(..., action, mask, archiveSize, fp) :3728-3732
│
├─ settleQp()                                             :3791-3827
│    ├─ settlementMode: warmup→无; G_BLOCK→OBSERVE_ONLY; 其余→LEARN（rho=0无SOFT_LEARN）
│    └─ zhangBoQpController.settle(evaluated, pendingQpSelections, bounds, firstOrdinal, mode)
│         逐子代（Controller:206-322）：
│         ├─ archive.update(previous, childEntry, group, bounds)   ZhangBoPersonalArchive.java:45-69
│         │    严格ND过滤→近重复去重→容量6截断→fingerprint排序
│         ├─ nextPbest = find(nextArchive, selection.selectedPbest.fp)
│         │    └─ 缺失→ directional(nextArchive)（既有回退规则）    Controller:253-255
│         ├─ reward = f(父current, 子child, archiveSurvived, fatigue)  :371-396
│         │    = 2.0×dominance + 1.0×direction(φ) + 0.5×archive + 0.25×fatigue
│         │    （不读取 selectedPbest —— 奖励与领导identity无直接耦合，
│         │      只通过CFVF子代间接影响）
│         └─ TD transitions 按lineageId稳定排序批量提交（frozen表）  :323-445
│
└─ reconcilePopulation()（PDDR后）                        Controller:352-369
     重建谱系记忆中 requested fingerprint；缺失→directional回退（既有规则）
```

## 2. 候选构造器逐行语义（ZhangBoQpCandidateSelector.build, :25-61）

```text
输入: entries(谱系档案,容量L=6) / selectedFingerprint(上轮selected) / group / current / gbest / bounds
L33-35  sorted = entries 按 fingerprint 字典序稳定排序
L36     directional = argmin(φ)，并列取fingerprint小者            (:63-74)
L37-38  keep = find(sorted, selectedFingerprint)；缺失→keep=directional
L39-47  仅当 |sorted|>1:
          DIRECTIONAL   = directional
          EPSILON       = argmin(ε-fitness)，并列取fingerprint小者   (:76-88)
          COMPLEMENTARY = quality集{e: φ(e)≤minφ+qualityTolerance(0.15)}内，
                          先剔除 norm(direction(current,e))≤1e-12 的退化项，
                          再按 (cosine asc, spacing desc, fingerprint asc) 取最优；
                          quality<2 或 norm(social)≤1e-12 → null（动作非法） (:90-130)
L48-56  mask：按 KEEP→DIRECTIONAL→EPSILON→COMPLEMENTARY 顺序，候选fingerprint
        与更早动作重复者置非法（去重）
L57-59  KEEP必须恒合法，否则抛IllegalStateException
输出: Candidates{candidates(action→唯一entry), mask, resolvedKeepFingerprint}
```

**既有审计结论继承**（`TEACHER_SELECTION_CALL_CHAIN.md` §2.2，2026-08-30对冻结Jar源码树）：
"Qp 不是'一个候选集合+选择'，而是'动作→唯一候选'的固定映射；每个动作的合法候选集合均为
单元素（或空），不存在'动作已定、在集合内改选哪个teacher'的自由度。"

## 3. RNG 消费合同（当前A4，逐位置）

| 位置 | 代码 | 消费 | 触发条件 |
|---|---|---|---|
| Qp动作选择 | Controller:473 | `nextDouble()` ×1 | 每次EPSILON_GREEDY选动作（恒定） |
| Qp动作探索 | Controller:477 | `nextInt(0,\|valid\|-1)` ×1 | 仅 draw<ε 探索分支 |
| Qp候选步 | Controller:160 | **无** | `candidates.get(action)` 确定性映射 |
| GREEDY_FROZEN动作 | Controller:155-156 | **无** | G-block贪婪无抽取 |
| warmup | Controller:325-350 | **无** | directional argmin |
| Qg侧（对照） | QgController:70/73/76/87 | nextDouble/nextInt | 独立调用链，K轴不触碰 |

**关键事实：K轴插入点（Controller:160）当前零RNG消费**——这是K=1零额外抽取合同的锚点。

## 4. 动作枚举与并列语义

- `ZhangBoQpAction`：`KEEP(0), DIRECTIONAL(1), EPSILON(2), COMPLEMENTARY(3)`。
- 贪婪并列：默认 `FIRST_VALID`——零表/并列时选首个合法动作=KEEP（Q0实测97.15% KEEP）。
- `DIRECTIONAL_IF_TIED` 仅为Q1诊断策略（`COLD_START_TIE_BREAK_NOT_CONFIRMED`，禁启清单）。

## 5. P5/G5 冻结与Qp的交互

- WARMUP（10%预算，含初群评价，边界按完整FE向上取整）：无动作/无转移/无RNG，pbest=directional argmin。
- P_BLOCK（5轮）：Qp正常ε-greedy学习；Qg表冻结。
- G_BLOCK（5轮）：Qp动作用冻结表贪婪执行（`GREEDY_FROZEN`，零RNG）；结算`OBSERVE_ONLY`（rho=0无SOFT_LEARN）。
- 实测轮数（SA-HARD 500k）：warmup=382轮，P=1360轮，G=1358轮，合计3100 Q轮；
  Qp动作事件=271,800（=2718非warmup轮×100粒子），LEARN转移=136,000。

## 6. 个人档案（容量与更新）

- `ZhangBoPersonalArchiveConfiguration`：capacity=6、normalizationEpsilon=1e-12、
  duplicateEpsilon=1e-4、indicatorKappa=0.05、similarityEpsilon=1e-4（`standard()`）。
- 更新（`update`, :45-69）：union(旧档案+本谱系已评价子代) → 三目标严格Pareto过滤 →
  近重复连通分量去重（组件内按 风险→ε-fitness→evaluationOrdinal→fingerprint 留一）→
  超6截断（方向锚点+ε锚点保留，最远点填充；疲劳邻域选择）→ fingerprint排序。
- 档案只收本谱系已评价父代/全局子代/局部子代；全局非支配集只用于冻结归一化边界。
