# V35-P22 10 工件多实例 pilot 证据

诊断性证据：单 seed 20260808，三个 10 工件实例各 baseline/FULL 500000 FE。机制级验收 + 方向 sanity，无统计、无正式结论。

## 实例

- 10_2_2_1：10 工件、2 阶段、2 工厂（p8 桥接既有实例，来源 `p8-bridge/v1`）
- 10_2_3_1：10 工件、2 阶段、3 工厂（V35PilotInstanceGeneratorTest 生成，来源 `EADHFSP-pilot`）
- 10_3_2_1：10 工件、3 阶段、2 工厂（V35PilotInstanceGeneratorTest 生成，来源 `EADHFSP-pilot`）

## 逐臂状态

| 实例 | 臂 | 状态 | FE | 前沿大小 | minCmax | minTEC | minTWC |
|---|---|---|---|---|---|---|---|
| 10_2_2_1 | baseline | COMPLETED | 500000 | 185 | 44.5837755401935 | 1907.0917830838102 | 2624.3191059978453 |
| 10_2_2_1 | full | COMPLETED | 500000 | 202 | 44.15547165184895 | 1901.4125092850013 | 2625.310818648327 |
| 10_2_3_1 | baseline | COMPLETED | 500000 | 231 | 49.158705945917816 | 2744.431804060063 | 3249.5042841403347 |
| 10_2_3_1 | full | COMPLETED | 500000 | 190 | 48.96930937449028 | 2722.359961727 | 3268.9827786387027 |
| 10_3_2_1 | baseline | COMPLETED | 499642 | 115 | 93.6779531114316 | 4785.614523404305 | 6216.800498232936 |
| 10_3_2_1 | full | COMPLETED | 500000 | 129 | 93.04688437375047 | 4726.636737982096 | 6112.298757948743 |

机制级验收：三实例 FULL 均 CFVF>0、CA-TA-Lite>0、DSCR teacherUses>0 且 DTUR=0、档案插入>0；baseline 均零；双臂 FE 收口 ≥495000。

## 数据文件

- `PILOT_METRICS.csv`：逐臂机制计数与极值
- `PILOT_HV_METRICS.csv`：池化参考 HV/IGD/覆盖
- `runs/`：每臂 configuration.txt / front.csv / 审计与 DSCR 文件
