# GAP_PROBE_REFERENCE_CONTRACT（冻结 v1，独立于一切历史 reference）

- 冻结日期：2026-08-30
- 适用：Gap Probe 的 2 实例 × 2 seed × 3 算法 = 每实例 6 条 500k 运行
- 独立性声明：禁止复用 Failure Replay reference、P25E/P25D 任何 reference、
  A2/A4 确认 reference 或未来 Validation reference。本合同按实例独立构造。
- 实现基础：与 Failure Replay Reference Contract 相同的冻结算法副本
  （`analyze_confirmation.py` 的精确拷贝；EPS=1e-12；原工具 SHA 见
  V35-PFC5-PHASE0/04-reference-contract/reference-contract.properties）

## 1. Reference 构造（每实例一次，6 条运行全部 ACCEPTED 之后）

```text
输入    = 该实例 3 算法 × 2 seed 的 6 条 raw 终态前沿（raw objectives，禁止
          使用遥测/观察前沿）
步骤    = 6 条前沿逐点合并 → 精确去重（sorted set）→ 严格 Pareto 非支配过滤
          （3 维最小化，相等不构成支配，EPS=1e-12）→ 实例级 empirical PFref
ideal   = PFref 各目标最小值    nadir = PFref 各目标最大值
归一化  = (x − ideal)/(nadir − ideal)，span 下限 EPS=1e-12
HV      = 归一化空间参考点 (1.1, 1.1, 1.1)，值截断到 [0, 1.1]
IGD     = 对归一化 PFref 的平均最近欧氏距离
重算    = 6 条 run 的 HV、IGD 与三目标极值（minCmax/minTEC/minTWC）统一重算；
          禁止采信各 runner 自报指标作为 Gap 裁决输入
禁止    = 逐 seed 构造 reference；跨实例合并；加入任何其他 campaign 的前沿；
          6 条运行未全部 ACCEPTED 前构造 reference
顺序无关性 = 构造时必须执行一次输入行随机打乱重建并要求 canonical 哈希一致
```

## 2. Gap 定义（预注册，运行后禁止修改）

比较对：`A4 vs A0(HMOPSO-QGS-F)`、`A4 vs SPEA2-F`（Gap Probe 衡量当前 A4
落后基线的程度；正值 = A4 更差）。

```text
gapHV(A4,B)(i,s) = (HV_B(i,s) − HV_A4(i,s)) / HV_B(i,s)
gapIGD(A4,B)(i,s) = (IGD_A4(i,s) − IGD_B(i,s)) / IGD_B(i,s)
i ∈ {50_2_3_1, 100_5_3_1}   s ∈ {20260827, 20260906}
分母 = 竞争对手（B）在该 (i,s) 的同口径指标值；方向固定为"A4 落后为正"。
聚合 = 每 (i, B) 对两个 seed 取中位数（2 个值的中位数）得 medGapHV、medGapIGD。
带宽（每 (i, B)，g(i,B) = max(medGapHV, medGapIGD)）：
  g ≤ 0.05            → GAP_WITHIN_5
  0.05 < g ≤ 0.15     → GAP_5_TO_15
  g > 0.15            → GAP_GT_15
总带宽 = 两实例 × 两比较对中最差带宽。
```

## 3. RED / NOT_RED 裁决（预注册）

```text
RED  ⇔ ∃ 指标 m ∈ {HV, IGD} 与竞争者 B，使得 gap_m(i, s, B) > 0.15
       对两个实例 i 与两个 seed s 全部成立（seed 级稳定性，严于中位数）。
NOT_RED = 未触发 RED；其余一切情况（含 GAP_GT_15）都只标 NOT_RED。
禁止称 GREEN；Gap Probe 通过与否不等于 Final 或 Validation 通过。
缺数据规则：任何一条 500k 运行缺失/INVALID/reference 不可构造 ⇒
  verdict = BLOCKED_REFERENCE_OR_RUNS（不得 RED/NOT_RED、不得解释算法）。
三目标极值（minCmax/minTEC/minTWC）仅随附报告，不参与带宽与 RED 判定。
```

## 4. 与主计划 §31–32 的对应

- 算法集、实例集、2×2 seed、500k、独立 reference、三档输出、RED/NOT_RED-only
  逐条落实；结果只输出三档与 RED/NOT_RED，禁止任何"绿色/通过"表述。
- Gap Probe 可与 Failure Replay 并行准备，但不得占用 Failure Replay 的 CPU
  计时域；全部 12 条运行前必须重申资源隔离登记。
