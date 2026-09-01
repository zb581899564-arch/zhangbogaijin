# FC-6A.4 顺序裁决

运行：`20_2_3_1`、seed `20260822/23/24`、population `100`、`500000 FE`、FM3、单族、序列无关 SUT、`ShiftMode=NONE`、A4-Pacing、`GLOBAL_ORIGINAL` PDDR。

## 裁决

**保留当前顺序：`CA-TA-Lite → inherited inter-factory/O1–O9`。**

反序臂 `inherited LS → CA-TA-Lite` 的中位最小 Cmax 改善为 `6.870539%`，中位 HV 变化为 `-0.867938%`，但中位 IGD 变化为 `+11.678867%`，超过预注册的 `+10%` 上限，因此不满足转正条件。未出现“单 seed HV < -5% 且 IGD > +20%”的灾难组合，但这不能抵消 IGD 门失败。

详细逐 seed 指标、局部来源 FE、进入/通过 PDDR 数量和冻结 reference 见同目录 `results/report/`。这一裁决只决定 FC-6B 的调用顺序，不构成统计显著性或论文优越性结论。
