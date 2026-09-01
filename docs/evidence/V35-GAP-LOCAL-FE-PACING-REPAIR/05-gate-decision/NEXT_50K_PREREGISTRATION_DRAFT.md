# NEXT 50k 预登记草案（V35-GAP-LOCAL-FE-PACING-REPAIR-V1 → 50k 开发门）

- 日期：2026-08-31
- 性质：**草案 only**。20k 机制门已 PASSED（见同目录 `20K_MECHANISM_GATE_REPORT.md`）；50k 未获用户批准前不得启动（`50kStarted=false`）。
- 计数更正：此前材料中"24 条 50k"数学上不正确（把 2 seed 误写成 3）。正确基线为 **4 配置 × 2 实例 × 2 seed = 16 条**。如需 24 条必须预注册第三个新 seed（4×2×3）。

## 1. 运行矩阵（16条）

```text
profiles: C0(betaMax=0.65), C1(0.55), C2(0.45), C3(0.35)
instances: 50_2_3_1, 100_5_3_1
seeds:     20260907, 20260914（升序未消耗池；20260827/20260906 已被 Gap Probe 消耗，
           20260901 为 Failure Replay seed；seed 规则见 single-repair-family-decision.md §4）
population=100, MaxFEs=50000
```

| # | runKey | profile | instance | seed | maxFEs |
|---|---|---|---|---|---|
| 1-4 | GAPL50K-C{0..3}-50_2_3_1-20260907 | C0–C3 | 50_2_3_1 | 20260907 | 50000 |
| 5-8 | GAPL50K-C{0..3}-50_2_3_1-20260914 | C0–C3 | 50_2_3_1 | 20260914 | 50000 |
| 9-12 | GAPL50K-C{0..3}-100_5_3_1-20260907 | C0–C3 | 100_5_3_1 | 20260907 | 50000 |
| 13-16 | GAPL50K-C{0..3}-100_5_3_1-20260914 | C0–C3 | 100_5_3_1 | 20260914 | 50000 |

（若用户批准扩展为 24 条：新增 seed 20260921，4×2×3，预注册后加入；基线方案为 16 条。）

## 2. 快照

seed 20260907 快照已物化并哈希登记（50_2_3_1 `79d1de2a…`、100_5_3_1 `57ecc786…`）。seed 20260914 快照在启动前用同一 `V35RepairSnapshotMaterializer` 物化（零FE、同 schema、reload 校验），哈希登记入 artifact-binding 与 run plan。

## 3. Reference 与指标

- 每实例 PFref 在该实例**全部 8 条运行结束后**一次性构造：`PFref = ND(union of C0..C3 × 2 seeds)`。单变量族内 reference；不混入 A0/NSGA-II-F/SPEA2-F（它们属于 Gap Probe V2 的 reference 体系，两套不得互混）。
- 同一实例的全部运行共用同一归一化边界（由 PFref 生成）。
- 输出指标：HV、IGD、SP、双向 C-metric、非支配解数、Cmax/TEC/TWC 极值（双管线口径沿用 `fc6_metrics.py` corrected 管线：raw 去重→raw 非支配→统一 min/max→不 clamp）。
- 配对基准：C0（当前 A4 语义）。逐 (instance, seed) 内 C1/C2/C3 对 C0 配对比较。

## 4. 门与晋级规则（预注册，不得事后修改）

### 4.1 质量门（困难实例须有改善方向）
- 100_5_3_1：两 seed 中位 `ΔHV > 0` 且中位 `ΔIGD < 0`（相对 C0，至少一个 C1–C3 配置）。
- 50_2_3_1（正常实例不回退门）：任一 C1–C3 配置的中位 HV 退化 ≤2% 且 IGD 恶化 ≤10%。

### 4.2 灾难门（任一触发即该配置出局）
- 单 seed HV < −5% 且 IGD > +20%（paired，任一实例）。
- actualFE/decoderCalls 违约、illegalSolutions>0、来源丢失>0、机制零触发。

### 4.3 晋级 250k
- 至少一个 C1–C3 配置同时通过 4.1 两门且未触发 4.3 之外的灾难门 → 推荐其 betaMax 进入 250k（C0 同批作对照臂，32 条方案另报批准）。
- 全部 C1–C3 失败 → repair family 判 `REJECTED`，停止（不换轴、不调参续命，按 single-repair-family-decision.md §3 证伪条款执行）。

### 4.4 20k 遗留问题的 50k 观察点
- totalLocalFE 四档分化（20k 的 C1=C2=C3 并列预计随外层循环数增加而解除）。
- localFeShare 实际曲线 vs 理论 β(u)（每 run budget-termination 已记账）。
- PDDR 池级归因缺口是否以不改冻结Jar的方式补观察（见 gate report §7）。

## 5. 执行协议（同 20k）

- 训练机独立目录 `/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-repair-20260831`（沿用，新增 `results50k/`），同一实验Jar `a0788580…` 与 binding（seed 20260914 新增 binding 行）。
- 每条独立 JVM `-Xmx4g` `nice -n 10`；同实例五臂并行（5 JVM），两组串行；预计 16 条总时长 < 1 小时。
- 原子输出、失败 attempt 保留、逐 run 机制门断言与 20k 相同（20k 专用门项 `maxFEs` 参数化）。
- 中性核验：C0–C3 seed 20260907 的 50k 与 20k 无共享断言（不同预算），但 seed 20260907 的 C0 50k 初群哈希必须与 20k 一致（同快照）。

## 6. 停止条件

- 任何 FE/来源/合法性违约 → 停止并报告 `EVIDENCE_INCOMPLETE`。
- 50k 结果不得回流修改 20k 报告或冻结Jar。
- 50k 全部完成前不启动 250k/DOE/validation/正式矩阵。
