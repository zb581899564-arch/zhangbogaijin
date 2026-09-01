# V35 离线等基数审计结果

- 审计时间：2026-08-24（只读分析；脚本不启动训练、不上传、不修改原始 front）。
- 输入归档：`G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\remote-campaigns\zhangbo-v35-stage2-master-v2-20260823.tar.gz`。归档 SHA-256：`0202356F28C7013894FB14B7347EB77A66243AD9312139CD0FE2A62F24CAD5FB`，与登记值一致。
- 临时解包目录（repo 外）：`C:\Users\33056\AppData\Local\Temp\v35_nd_cardinality_audit__w03d1pr\extracted`。仅提取 `100_2_3_1` 的 seed `20260808..20260819`、A0..A4，共 `60` 条通过 500k-budget phase-consistent 完成门的运行；实际 FE 分布为 `495301x1, 495590x1, 499452x1, 499607x1, 500000x56`。
- 逐文件证据校验：已验证 `816` 个 evidence-sha256.tsv 条目；HASH_MISMATCH=0，MISSING_FILE=0。

## 口径

三目标固定为 `[0,1,6] = [Cmax, TEC, TWC]`，全部按最小化处理；精确去重使用解析后 binary64 位模式，严格支配不使用 epsilon。主指标只读取完整 `decision-front`。K25/K50 使用各自独立 PFref 做敏感性；K30 只输出展示集，不计算 HV/IGD。源 front 未被改写。

## 等基数和近重复

- full pooled PFref：`1979` 点，SHA `3711e6dc7109f04fb2178cc0d49096d46ea3bf760fb58b47ea13a2368f2023fd`。K25 PFref：`119` 点；K50 PFref：`205` 点。
- full decision-front 的全体运行中位 normalized nearest-neighbor <=0.1% 率：`0.002584`（Gate A 阈值 0.20）；K25/K50/K30 输出和置换不变性结果见 CSV。
- 置换不变性失败数：`0`。

## Full / K25 / K50 指标

| variant | arm | HV median | IGD median | PFref size |
|---|---:|---:|---:|---:|
| full | A0 | 0.49517776 | 0.26940616 | 1979 |
| full | A1 | 0.48513359 | 0.25801112 | 1979 |
| full | A2 | 0.62229022 | 0.19905164 | 1979 |
| full | A3 | 0.50351901 | 0.28303409 | 1979 |
| full | A4 | 0.62502559 | 0.18790607 | 1979 |
| k25 | A0 | 0.47195706 | 0.28756811 | 119 |
| k25 | A1 | 0.46892964 | 0.2826484 | 119 |
| k25 | A2 | 0.60741628 | 0.19949898 | 119 |
| k25 | A3 | 0.48339691 | 0.24719161 | 119 |
| k25 | A4 | 0.61222342 | 0.19174482 | 119 |
| k50 | A0 | 0.48847044 | 0.28207912 | 205 |
| k50 | A1 | 0.47908191 | 0.27625812 | 205 |
| k50 | A2 | 0.61562216 | 0.18841264 | 205 |
| k50 | A3 | 0.49207315 | 0.25533491 | 205 |
| k50 | A4 | 0.61834223 | 0.18226313 | 205 |

## A4/A0 排序反转

Full 联合方向：`A4`；K25：`A4`（reverses=false）；K50：`A4`（reverses=false）。完整逐种子/中位数数值见 `a4-a0-ranking.csv`。
Leave-one-run/arm-out 中可比较的 A4/A0 反转数：`0`；PFref 结构稳定性见 `leaveout-reference.csv`，排名明细见 `leaveout-ranking.csv`。

## Gate A 结论边界

当前离线决策前沿只对“基数/参考前沿敏感性”给出证据：0.1% 近重复阈值未触发，K25/K50 与 leave-out 的 A4/A0 反转按 CSV 判定。归档没有 observed-full-front、archive scan ledger、teacher directional regret 或 PDDR lifecycle 输入，因此物理 ND0 observer 等价门尚未关闭，不能据此启动 ND1-ND4。Gate A 六项逐项状态见 `gate-a-assessment.csv`。

ND0 的硬门仍需单独的同 seed/同初始种群 observer run：initial population hash、FE/decoder calls、行为事件/Q 表 hash、最终 decision-front 必须等价，且 exact-dedup(decision-front) 必须等于 exact-dedup(observed-full-front)。本离线审计不冒充该等价实验。

## 与 PDDR 的隔离

本审计不改变、重算或归因 `GLOBAL_ORIGINAL` PDDR；不读取 PDDR Cmax lifecycle 作为 Gate A 证据，不改变 Qg/Qp/CFVF/CA-TA/local-search/mixture/FE，不把 observed-full、K25/K30/K50 送入搜索、teacher cache、PFref 或论文主表。`PDDR` 和 archive cardinality 是两个独立问题。

## 输出文件

`run-manifest.csv`、`file-verification.csv`、`cardinality.csv`、`permutation-invariance.csv`、`representative-front-k30.csv`、`sensitivity-front-k25.csv`、`sensitivity-front-k50.csv`、`reference-front-{full,k25,k50}.csv`、`metrics-{full,k25,k50}.csv`、`metric-summary.csv`、`a4-a0-ranking.csv`、`leaveout-reference.csv`、`leaveout-ranking.csv`、`gate-a-assessment.csv`、`audit-output-sha256.tsv`。
