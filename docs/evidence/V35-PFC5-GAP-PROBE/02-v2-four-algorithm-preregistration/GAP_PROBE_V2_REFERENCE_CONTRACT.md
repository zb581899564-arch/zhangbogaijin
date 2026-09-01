# GAP_PROBE_V2_REFERENCE_CONTRACT（冻结，四算法）

- 冻结日期：2026-08-30
- 适用：每实例 4 算法 × 2 seed = 8 条 500k 运行；两实例各自独立构造
- 独立性：禁止复用 P25E/P25D、Failure Replay、A2/A4 确认或任何其他 campaign 的
  reference；v1 三算法合同（每实例 6 条）废止，不得使用
- 实现基础：与 Failure Replay Reference Contract 相同的冻结算法副本
  （`analyze_confirmation.py` 精确拷贝；EPS=1e-12）

## 1. 构造（每实例，等该实例 8 条运行全部 ACCEPTED 之后）

```text
PFref_gap(instance) = strictND(
    A4 × 2seed ∪ A0 × 2seed ∪ SPEA2-F × 2seed ∪ NSGA-II-F × 2seed 的 raw 终态前沿
)
步骤 = 合并 → 精确目标去重（sorted set）→ 严格三目标 Pareto 过滤（相等不支配）
ideal/nadir = PFref 各目标 min/max
归一化 = (x−ideal)/(nadir−ideal)，span 下限 1e-12
HV 参考点 = (1.1,1.1,1.1)，值截断 [0,1.1]
统一重算 = 8 条 run 的 HV、IGD、minCmax、minTEC、minTWC 全部重算，
           禁止采信 runner 自报指标
顺序无关 = 输入行随机打乱重建，canonical 哈希必须一致
任何缺臂/INVALID ⇒ 该实例 reference 不得构造（BLOCKED_REFERENCE_OR_RUNS）
```

## 2. Gap 定义（与 v1 相同，比较对扩为三个）

```text
比较对: A4 vs A0、A4 vs SPEA2-F、A4 vs NSGA-II-F（A4 落后为正）
gapHV(A4,B)(i,s) = (HV_B − HV_A4)/HV_B
gapIGD(A4,B)(i,s) = (IGD_A4 − IGD_B)/IGD_B
聚合 = 每 (实例, 竞争者) 两个 seed 取中位数 → medGapHV、medGapIGD
带宽 = max(medGapHV, medGapIGD)：≤5% WITHIN_5；≤15% 5_TO_15；>15% GT_15
总带宽 = 2 实例 × 3 比较对最差带宽
RED  = 同一竞争者、同一主指标（HV 或 IGD）在两个实例和两个 seed 全部
       A4 落后 >15%；否则 NOT_RED；禁止 GREEN
```

## 3. 后续"最强 external"选择规则（预注册）

Gap 完成后，在 {A0, SPEA2-F, NSGA-II-F} 中按两个实例 × 两个 seed 的 HV 与 IGD
平均秩联合选择：每 (instance, seed) 内对每指标在四臂（含 A4）中排名（1 最优），
对每臂计算 HV 平均秩与 IGD 平均秩，再取 (rankHV+rankIGD)/2 最小者为最强
external；并列依次按 HV 平均秩、IGD 平均秩、算法标签字母序破平。
该选择只决定后续开发（DOE migration / Validation）的对照相，不是论文结论。
