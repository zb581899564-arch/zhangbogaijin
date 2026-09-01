# FC-6B Region-aware Environmental Selection：500k 配对裁决

## 试验边界

在 FC-6A.4 已裁决保留 `CA-TA-Lite → inherited LS` 后，本组只改变 PDDR
环境选择：

```text
GLOBAL_ORIGINAL  vs  REGION_AWARE
```

共同条件为 FM3、单一产品族、序列无关 SUT、`ShiftMode=NONE`、A4-Pacing、同 seed
共同初始种群、population `100`、每条精确 `500000 FE`。区域模式固定物理容量为
`G1/G4/G2/G3 = 15/55/15/15`，BP 预留模式为零。

实例与配对 seed：`20_2_3_1`、`100_2_3_1` × `20260822/23/24`。12 条记录均为
`COMPLETED`，均无超过预算的记录。

## 裁决

**STOP_REVIEW：不冻结 `REGION_AWARE`，维持 `GLOBAL_ORIGINAL`。**

| 实例 | 最小 Cmax 中位改善 | HV 中位变化 | IGD 中位变化 | 灾难 seed 门 | 结论 |
|---|---:|---:|---:|---|---|
| `20_2_3_1` | +1.6710% | -3.9689% | +67.8729% | 否 | 拒绝：未达 2% Cmax、HV/IGD 门失败 |
| `100_2_3_1` | -1.5743% | -22.7133% | +371.7009% | 是 | 拒绝：Cmax、HV、IGD 全部失败 |

这不是显著性检验，也不表示区域分层理论上永远无效；它只证明该预注册的
`15/55/15/15` 生存选择在当前冻结主线和两个代表规模上不满足转正条件。按照
FC-6 的单支纪律，不自动叠加教师门控、压力掩码或新的搜索动作来挽救该结果。

## 可审计数据

完整远端结果压缩包：`remote-results-r3.tar.gz`，SHA-256：

```text
60d70be77d64fd6d9aa091ad0106f6c9cbdac567f864f2b47e90d25db86b98ae
```

解压后的 `remote-results-r3/results/report-20` 与 `report-100` 的每份
`evidence-sha256.tsv` 已在本地逐文件复核通过。逐 seed 前沿、局部候选账本、
精确 merge-pool、跨区域教师旁路记录和统一参考前沿均在该目录下。

## 部署失败隔离

前两次远端提交分别被容量硬门和缺失 100-job 输入的上传清单问题中止；没有把
它们的部分前沿混入本组参考集。详见 `FAILURE_CAPACITY_GATE.md`。正式裁决只使用
本报告所列 r3 完整 12 条运行。
