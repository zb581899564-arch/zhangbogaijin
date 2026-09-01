# 250K_PREREGISTRATION — V35-LOCAL-FE-PACING C2/C3 双候选确认与完整前沿检查点实验（冻结版）

- 日期：2026-08-31
- 状态：**FROZEN**（训练机执行前冻结；门定义、指标口径、裁决规则不得再改。执行事实写 `16-remote-250k-runs/`，分析写 `17-250k-reference-and-metrics/`，裁决写 `18-250k-decision/`。）
- 上游：`12-50k-decision-correction/`（C2/C3 双候选晋级）、`13-checkpoint-observer/`（观察器实现）、`14-checkpoint-equivalence/`（OFF/ON 等价门 PASSED）。

## 1. 冻结科学语义（与 20k/50k 完全一致）

FM3；ShiftMode=NONE；single family；sequence-independent SUT；mixture=[20,40,20,20]；PDDR=GLOBAL_ORIGINAL；LS order=CA-TA-Lite → inherited LS；Dual-Q=P5/G5；rho=0；direction teacher pool=OFF；population=100；budget protocol=PHASE_CONSISTENT_BUDGET_TERMINATION。CFVF、Dual-Q、CA-TA-Lite 全程在环。唯一实验变量 betaMax：C0=0.65（对照）、C2=0.45、C3=0.35（**C1 不参与**）。

## 2. 工件绑定（逐文件 SHA 复核后上传）

- 冻结正式Jar `formal-algorithm-8DAD8F40.jar` = `8dad8f40…bad8b9`（磁盘字节不动；本实验通过 classpath 优先级加载 V2 影子类，不修改正式Jar文件）。
- **V2 实验Jar** `jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-CHECKPOINT-V2.jar` = `c2cf4294…35758`，18 类全部 major=52，内容：V35FairRunner 影子副本（冻结源码 + 2 行观察钩子接线）、V35PassiveEvaluationArchive 影子副本（冻结源码 + 1 行钩子调用）、V35CheckpointObserverHook（纯观察钩子）、V35CheckpointRepairRunner（薄Runner，CLI 在 V1 六flag 外新增 `--observer`/`--checkpoints`）、profile 类（V1 同源）。classpath 顺序 **V2:FORMAL**。
- 实例/SUT/疲劳链与 20k/50k 逐哈希一致（见 `artifact-binding.csv`）。
- 6 个新快照（3 seed × 2 实例）零FE物化+回读校验，SHA 见 `artifact-binding.csv`。

## 3. 检查点观察器契约（已验证）

- 检查点目标固定 `{50000, 100000, 150000, 200000, terminal}`；每次成功评价被被动档案接纳后，首次 `observedCount >= target` 时冻结——由于逐次接纳，`checkpointObservedFE == target`、`overshootFE = 0`（远小于 5000 上限）。
- 四种 frontType 严格分列：`checkpoint-decision-front`（决策前沿=algorithm.getResult() 目标+指纹）、`checkpoint-observed-full-front`（被动档案=前 target 次成功评估的非支配过滤，**含 candidateFingerprint**）、`terminal-decision-front`、`terminal-observed-full-front`。**不同 frontType 不得混入同一 reference 或指标。**
- 观察器禁止事项已由实现与等价门共同证明：不入搜索档案、不改 PDDR 输入/教师选择、不消耗随机数、不加FE、不改候选身份或排序（`14-checkpoint-equivalence/behavior-equivalence.csv`：OFF/ON 126 行 0 DIFFER，含全部事件流哈希/Q表哈希/机制计数/终态front逐字节一致；OFF 对存储冻结运行 114 行 0 DIFFER）。
- 公平组跨度：同组各臂 `checkpointObservedFE == target`（跨度 0 < 5000）。
- `evaluationCounterFE` 列记录批次内评估计数器领先量（<100，信息字段）。

## 4. 运行矩阵（18 条 = 3 臂 × 2 实例 × 3 seed）

arms {C0, C2, C3} × instances {50_2_3_1, 100_5_3_1} × seeds {20260916, 20260917, 20260918}，population=100，MaxFEs=250000，observer=ON。RunKey 与逐条参数见 `run-registry.csv`（18 行，唯一）。每个 instance×seed 为三臂公平组：同快照、同 V35/P8 初群双哈希、同实例/SUT/疲劳来源、独立 JVM、独立 Problem、独立算法对象。

**Seed 审计**（`seed-usage-audit.csv`）：扫描本地全部证据树（run 产物目录 + registry CSV + 文档）与训练机全部 zhangbo/v35 目录（`grep seed= 模式`），区分"实际运行"与"提及/manifest登记"：20260915 已实际运行（消耗）；20260916/17/18 零实际运行、零真实 seed 提及（唯一 grep 命中为某 seed-20260902 遥测行指纹哈希内的巧合子串）；20260921 仅被 50k 草案提及（提及不消耗，但数值更大）。按数值升序取最小三个未运行 seed = **20260916, 20260917, 20260918**。

## 5. 执行协议

- 远端新目录 `/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-250k-20260831/`（不覆盖任何历史目录），按 seed 分工作目录 `seed-2026091{6,7,8}/`（runner 以固定路径 `bindings/<instance>.binding.properties` 读取，每 seed 一目录）。
- 上传后逐文件 SHA 复算（对照 `upload-sha256.tsv`）；启动前只读预检（CPU/内存/磁盘/Java/遗留进程）。
- 每公平组 3 JVM（`-Xmx4g`、`nice -n 10`、无GPU），两个公平组并行（≤6 JVM）；scheduler 在训练机本地，SSH 只上传/启动/检查。
- `.partial-*` → manifest → atomic move；失败 attempt 保留不覆盖，自动重试至多 2 次；不删除远端原始结果；18 条全部完成后停止。

## 6. 逐条验收门

```
0 < actualFE = decoderCalls <= 250000
remainingFE < 5000
utilizationRate > 0.98
front finite and nonempty
illegalSolutions = 0
duplicateEvaluations = 0
unexplainedRepairs = 0（cfvfRepairs/directionalPool*/shadow* 全零）
sourceLoss = 0（passiveObservedCount == fullEvaluations）
shiftActivity = 0
checkpointRows = 4（四目标全部冻结）且 overshootFE 全 0
observerExecutionErrors = 0
failures = NONE
```

每个三臂公平组：same instance/seed/snapshot/initial V35+P8 hashes/problem provenance；`max(actualFE) − min(actualFE) < 5000`（闭合调度预测：C0 尾停、C2 exact 停或尾停、C3 尾停；50k 同族极差 4641）。

## 7. Reference 与指标契约（详见 checkpoint-reference-contract.md）

- **终态**：`PFref_terminal(instance) = ND(C0∪C2∪C3 × 3 seed 的 terminal-decision-front)`；objective mapping=[0,1,6]；每实例统一 ideal/nadir（PFref 的 min/max）；HV 参考 (1.1,1.1,1.1)。
- **检查点**：每个实例 × 每个有效检查点独立构造 `PFref_checkpoint(instance,targetFE) = ND(C0∪C2∪C3 × 3 seed 的 checkpoint-observed-full-front)`。禁止用终态 reference 算检查点指标，禁止 observed-front 与 decision-front 混用。
- 指标：HV、IGD、Spacing、双向 C-metric、frontSize、minCmax、minTEC、minTWC、runtime、actualFE；管线=原始去重→严格Pareto→统一归一→HV（fc6_metrics corrected 口径）。

## 8. 裁决规则（任务书 §九/§十，冻结）

- 配对响应以同 instance×seed 的 C0 为基准，正值=候选改善；median=3 seed 中位。
- 正常实例安全门（50_2_3_1）：median ΔHV ≥ −2% 且 median ΔIGD ≥ −10%。
- 困难实例改善门（100_5_3_1）：median ΔHV ≥ +2% 或 median ΔIGD ≥ +10%（至少一项），另一项不越安全门。
- 单seed灾难门：任一实例出现 ≥2/3 seed 同时 ΔHV < −5% 且 ΔIGD < −20% → 出局。
- 三目标门：仅当同一实例 ≥2/3 seed 退化 >2% 才构成系统性退化；**禁止以不足 1% 的单标量符号翻转淘汰候选**。
- 检查点一致性门：仅当 ≥2 个有效检查点、方向与终态相反、幅度达 HV>2% 或 IGD>10%、且 ≥2/3 seed 一致 → `BUDGET_SENSITIVITY_CONFLICT`；否则记 `MINOR_FLUCTUATION` 不淘汰。
- 候选选择 ∈ {NO_REPAIR_CANDIDATE, C2_ONLY, C3_ONLY, C2_AND_C3_PASS}；双过时破平序：困难实例终态 median ΔHV → median ΔIGD → 正常实例安全性 → 检查点稳定性 → 主要差异均 <1% 时选 C2（betaMax=0.45 更接近冻结基线）。
- 胜者只标记 `PROVISIONAL_250K_REPAIR_CANDIDATE=true`；不标记 FinalCandidateApproved/FINAL_FROZEN。

## 9. 停止条件

- 任何验收门失败且 2 次重试后仍失败 → `EXECUTION_OR_EVIDENCE_FAILURE`，停机保留现场。
- 18 条完成并分析后**立即停止**：`500kStarted=false`、`DOEStarted=false`、`validationStarted=false`、`FinalCandidateApproved=false`、`FINAL_FROZEN=false`、`formalMatrixRunning=false`。下一阶段（获胜候选 vs C0 的 500k 多实例确认）须另行批准。
