# GAP-PROBE-V2 分析报告

- 日期：2026-08-30
- 数据：16/16 条 500k 运行（训练机 `zhangbo-v2` 目录，已本地只读同步于
  `04-v2-remote-500k-runs/sync/`）；全部 ACCEPTED（16/16 预算闭合、零非法解/
  零重复评价/零禁止机制事件、4 个公平组全有效）
- 工具：`tools/analyze_gap_v2.py`（冻结算法副本；PFref 顺序无关自检通过）

## 1. 统一 reference 与指标（每实例 8 条 raw front 严格 ND 并集）

| 实例 | PFref 点数 | canonical SHA-256（前16） | 顺序无关 |
|---|---:|---|---|
| 50_2_3_1 | 426 | 426点逐位记录于 reference-fronts/ | true |
| 100_5_3_1 | 146 | 见 reference-fronts/ | true |

HV 参考点 (1.1,1.1,1.1)；HV/IGD/minCmax/minTEC/minTWC 全部统一重算
（`metrics.csv` 全 16 行）。

## 2. 逐实例 median HV/IGD（两 seed 均值口径的原始值见 metrics.csv）

| 实例 | 臂 | median HV | median IGD | front 规模 |
|---|---|---:|---:|---|
| 50_2_3_1 | A4-Pacing | 0.8303 | 0.0561 | 645/475 |
| 50_2_3_1 | A0(HMOPSO-QGS-F) | 0.5552 | 0.3175 | 523/574 |
| 50_2_3_1 | SPEA2-F | 0.8657 | 0.0920 | 100/100 |
| 50_2_3_1 | NSGA-II-F | 0.7172 | 0.1809 | 100/100 |
| 100_5_3_1 | A4-Pacing | 0.2542 | 0.4432 | 373/347 |
| 100_5_3_1 | A0(HMOPSO-QGS-F) | 0.2500 | 0.4338 | 219/242 |
| 100_5_3_1 | SPEA2-F | 0.6971 | 0.1227 | 100/100 |
| 100_5_3_1 | NSGA-II-F | 0.7818 | 0.1134 | 100/100 |

## 3. A4 落后差距（gap = (B−A4)/HV_B 或 (IGD_A4−IGD_B)/IGD_B，正=A4差）

| 实例 | 竞争者 | median gapHV | median gapIGD | 带宽 |
|---|---|---:|---:|---|
| 50_2_3_1 | A0 | **−50.5%** | **−82.2%** | GAP_WITHIN_5 |
| 50_2_3_1 | SPEA2-F | +4.0% | **−38.5%** | GAP_WITHIN_5 |
| 50_2_3_1 | NSGA-II-F | −16.0% | −67.6% | GAP_WITHIN_5 |
| 100_5_3_1 | A0 | −43.4% | +13.5% | GAP_5_TO_15 |
| 100_5_3_1 | SPEA2-F | **+63.5%** | **+260.7%** | **GAP_GT_15** |
| 100_5_3_1 | NSGA-II-F | **+67.8%** | **+311.3%** | **GAP_GT_15** |

读法：负值 = A4 领先。50-job 开发实例上 A4 对 A0/NSGA-II-F 全面领先、
对 SPEA2-F 的 HV 差距仅 4%（IGD 大幅领先）→ WITHIN_5。
**困难实例 100_5_3_1 上 A4 相对两个官方经典核大幅落后**
（HV 落后 63-68%，IGD 落后 260-311%），A0 同样落后但幅度较小且 seed 间
方差极大（A0 在 100_5_3_1 上 gapHV 从 −130.9% 到 +44.1% 剧烈波动）。

## 4. RED / NOT_RED

```ini
gapProbeRed=false
```
RED 需要同一竞争者、同一主指标在两个实例与两个 seed 全部 >15% 落后；
50-job 上 A4 对任何竞争者的任何主指标落后均 ≤4.0%，RED 条件不成立。
总带宽由 100_5_3_1 驱动：**GAP_GT_15**（对 SPEA2-F 与 NSGA-II-F）。
禁止称 GREEN；本裁决不是 Final 通过，也不是对 A4 的否定——它把"冻结 A4
在困难 100-job 实例上相对官方经典核的 HV/IGD 差距"正式量化为后续
leverage audit 与单一 repair family 的输入。

## 5. 后续最强 external（预注册规则：两实例×两 seed 的 HV/IGD 平均秩联合）

| 臂 | meanRankHV | meanRankIGD | score |
|---|---:|---:|---:|
| **SPEA2-F** | **3.00** | **1.50** | **2.25** |
| A0(HMOPSO-QGS-F) | 1.50 | 3.50 | 2.50 |
| NSGA-II-F | 4.00 | 1.50 | 2.75 |
| （A4-Pacing | 1.50 | 3.50 | 参与排名但不参选） |

**最强 external = SPEA2-F**（score 2.25，规则冻结于
GAP_PROBE_V2_REFERENCE_CONTRACT.md §3；并列时依次按 HV 秩、IGD 秩、
标签序破平——本轮无并列）。该选择仅用于后续开发对照相，不是论文结论。

## 6. 三目标极值要点（完整见 metrics.csv）

- 50_2_3_1：A4 的 minCmax 319.8–323.0（全场最优区间）；minTEC 与 NSGA-II-F
  （20186/20193）相比有 ~3% 差距；minTWC 32162–32284 全场最优。
- 100_5_3_1：A4 的 minCmax 767.5/768.1 与全场最优（750.8–761.8）接近；
  minTEC 112512/112952 落后于 NSGA-II-F（110081/112424）；minTWC
  295061/296101 与 SPEA2-F（287640–289876）存在 ~3% 差距——极值维度
  A4 并未全面崩塌，崩塌集中在 Pareto 覆盖质量（HV/IGD），
  与 250k 诊断"覆盖收缩"的机制候选方向一致。
