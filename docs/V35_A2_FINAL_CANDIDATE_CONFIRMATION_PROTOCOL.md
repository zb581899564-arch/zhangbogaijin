# V35 A2 主候选跨尺度确认协议

版本：`v35-a2-final-candidate-confirmation-v1`  
日期：`2026-08-25`  
状态：`PRE_REGISTERED_NOT_STARTED`

## 1. 目的与前置结论

`D-104` 已经以预注册的六实例、五 seed、60 条配对运行否决 A4 的 Final 晋升。该结论并不自动证明 A2
优于规范 HMOPSO-QGS 基线 A0：A2 是当前主候选，而不是已经验收的 Final 算法。

本协议只回答一个问题：在没有参与 DOE、FC-6、A2/A4 确认或主版本选择的新实例和新 seed 上，A2 是否能稳定优于
A0。它不重新研究 A3/A4，不允许为了 A2 改写任何搜索机制。

## 2. 冻结的比较对象与算法语义

| public label | frozen profile | 论文角色 |
|---|---|---|
| `A0_BASELINE` | `A0` | 规范、公平的 HMOPSO-QGS 基线 |
| `A2_CFVF` | `A2` | 当前 V35 主候选：A0 + DSCR + CFVF |

两臂都固定为：

```text
FM3
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
ShiftMode=NONE
objectives=[Cmax, TEC, TWC]=slots[0,1,6]
mixture=[G1,G4,G2,G3]=[20,40,20,20]
PDDR=GLOBAL_ORIGINAL
local-search order=CA-TA-Lite -> inherited LS
P=5/G=5, rho=0
directional teacher pool=OFF
pressure classifier=diagnostic-only/BAL
population=100
MaxFEs=500000
PHASE_CONSISTENT_BUDGET_TERMINATION
```

冻结算法 Jar 必须为：

```text
8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

`A3` 与 `A4` 保留为已经验证的负向/组合历史分支；不进入本协议、不会被重新调参或用作主版本。

## 3. 新的实例、seed 与配对规则

六个实例在现有 45 实例冻结清单中选择。排除集合包含所有已用于 DOE、FC-6、Stage2 和 D-103 的
20/50/100-job 实例。为避免事后选择，剩余候选按每个规模的两个极端结构固定：

| scale | instance | 结构 |
|---:|---|---|
| 20 | `20_2_5_1` | 2 stage / 5 factories |
| 20 | `20_8_3_1` | 8 stage / 3 factories |
| 50 | `50_2_5_1` | 2 stage / 5 factories |
| 50 | `50_8_3_1` | 8 stage / 3 factories |
| 100 | `100_2_5_1` | 2 stage / 5 factories |
| 100 | `100_8_3_1` | 8 stage / 3 factories |

种子固定为：`20260911, 20260912, 20260913, 20260914, 20260915`。

同一 `(instance, seed)` 的 A0/A2 必须读取同一个独立冻结四向量 snapshot；不同 arm 只能改变
`V35FinalAblationProfile` 所规定的 A0/A2 机制。总计：

```text
6 instances × 5 paired seeds × 2 arms = 60 physical 500k runs
```

本确认集不进入之前 D-103 确认集、DOE、Stage2 或未来正式论文 PFref，也不用于调参。

## 4. 运行接收门

每条运行必须同时满足：

```text
status=COMPLETED
0 < actualFE = decoderCalls <= 500000
0 <= remainingFE < 5000
utilizationRate > 0.99
front non-empty and finite
illegalSolutions=0
duplicateEvaluations=0
exceptionalRepair=0
sourceLoss=0
```

每个 A0/A2 配对另外核验：实例、seed、Jar、snapshot、V35/P8 初群 hash、问题 provenance 完全一致，且
`abs(actualFE_A0 - actualFE_A2) < 5000`。缺任一臂、文件 hash、front 或 provenance 的配对整体无效，不得进入
reference 或指标。

## 5. Reference、指标与预注册裁决

每个实例仅在其全部十条有效 raw fronts 完成后构造：

```text
PFref_final_candidate(instance) = ND(A0 ∪ A2, 5 seeds each)
```

同一实例的两臂共享归一化边界和归一化空间的 HV reference point `(1.1,1.1,1.1)`。主指标为 HV、IGD、
IGD+、Cmax、TEC、TWC；Spacing、front size、双向 C-metric、FE 和 runtime 仅作解释。

以 A0 为配对基准：

```text
DeltaCmax = (Cmax_A0 - Cmax_A2) / Cmax_A0
DeltaTEC  = (TEC_A0  - TEC_A2)  / TEC_A0
DeltaTWC  = (TWC_A0  - TWC_A2)  / TWC_A0
DeltaHV   = (HV_A2   - HV_A0)   / HV_A0
DeltaIGD  = (IGD_A0  - IGD_A2)  / IGD_A0
```

只有同时满足下列门，A2 才获得 `A2_FINAL_CANDIDATE_CONFIRMED`：

1. 30/30 配对有效；
2. 全部 30 个 block 的中位 `DeltaHV > 0`、`DeltaIGD > 0`、`DeltaCmax >= 0`；
3. 至少 4/6 个实例的中位 `DeltaHV >= 0` 且 `DeltaIGD >= 0`；
4. 20、50、100 三个规模的 pooled 中位 `DeltaHV >= 0` 且 `DeltaIGD >= 0`；
5. 任何 100-job 实例均不得同时出现 `median(DeltaHV) < -5%` 与 `median(DeltaIGD) < -20%`；
6. TEC 与 TWC 不得在三种规模上均出现中位退化小于 `-2%`。

任一门失败则为 `A2_NOT_PROMOTED`：停止主搜索算法的 Final 冻结与正式矩阵，不通过改参数、重抽实例或重用本确认集
来挽救结果。

## 6. 成功后的唯一允许路径

本协议通过后才允许：

```text
A2 Final roster freeze
-> A0/A2 production preflight
-> host throughput benchmark
-> user approval
-> formal A0/A1/A2 campaign and independent external-algorithm comparison
```

正式内部消融 roster 的建议为 `A0 -> A1 -> A2`。A3/A4仅以已有的预注册负向/组合诊断证据解释，不得被写成
独立正贡献，也不得在未预注册的反事实下被重新包装进主算法。

## 7. 证据输出与禁止事项

证据根目录：`docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/`。每次运行必须保存预登记、输入与 profile
hash、snapshot receipt、状态、完整 raw front、预算、机制摘要、分析脚本、reference、指标、裁决与文件级 SHA-256。

本协议不授权：修改 frozen Jar、PDDR、Qp/个人档案/双Q、CA-TA、Pacing、rho、子群配比、压力掩码、Shift、PF-SDST，
或恢复旧 A0--A4/4500 Master。
