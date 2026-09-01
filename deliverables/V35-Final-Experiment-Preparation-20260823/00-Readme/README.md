# V35 Final 实验阶段：收口交付包

生成日期：2026-08-23

## 这是什么

这是 V35 Final 论文实验启动前的完整交付包。它汇总本轮的：

- 实验 DAG、状态与冻结结论；
- 正式比较的 fail-closed Runner、协议、来源与公平性模板；
- A0–A4 语义消融梯子和 2k FE 工程烟测证据；
- 指标、统一参考前沿与统计分析管线；
- 论文方法与实验章节骨架；
- 对应的实现代码、单元测试和构建/核验记录。

它不包含整个工作区，也不重复打包历史大规模实验、Maven `target`、原始作者资料或历史移位实验。它只包含本轮 V35 Final 收口所需的结论、可执行入口与原始证据；每一类材料均可从包内清单定位。

## 当前正式结论

当前正式搜索语义冻结候选为：

```text
[G1_CMAX, G4_BALANCED, G2_TEC, G3_TWC] = [20,40,20,20]
FM3; 单一产品族; 序列无关 SUT; ShiftMode=NONE
GLOBAL_ORIGINAL PDDR; CA-TA-Lite -> inherited LS
A4-Pacing; 双Q P=5/G=5; rho=0; 方向教师池关闭
正式三目标 = [Cmax, TEC, TWC] = 七槽 [0,1,6]
```

正式 500k 论文实验 **尚未启动**。这是预期的 fail-closed 状态：FC-8 / EXP-1、20-seed 名单、算法 roster、45 实例哈希清单、初始种群来源与矩阵授权尚未正式冻结。

## 目录导览

- `01-Final-Planning`：DAG、状态表。
- `02-Freeze-And-Campaign`：候选冻结、短程并发预检、可恢复调度器。
- `03-Formal-Comparison`：A4 与规范公平适配 HMOPSO-QGS 基线的正式比较协议与 Runner。
- `04-Ablation`：A0–A4 依赖图、语义审计、2k FE 烟测与审批说明。
- `05-Analysis-And-Statistics`：PFref、指标和统计工具。
- `06-Paper-Skeleton`：方法/实验/结果占位文稿。
- `07-Implementation`：本轮新增或修正的 Java 与 Python 源码及测试。
- `08-Verification`：包级文件清单和 SHA-256 校验结果。

## 重要边界

1. A0 的名称为“规范、确定性、公平适配 HMOPSO-QGS 基线”，不是李明哲原始缺陷代码的直接可执行复现。
2. 2k FE 烟测只证明机制接入、预算与可重放闭合；不构成算法优越性或论文统计结论。
3. 任何正式 PFref 必须在同一实例的所有正式 raw fronts 完成后一次性冻结，不能与先导/历史数据混用。
4. `REGION_AWARE`、`BP_RESERVED_LEGACY`、`ORDER_SWAP`、Shift、PF-SDST、序列相关设置时间和第四目标均不属于当前正式主线。
5. `08-Verification/VERIFICATION_SUMMARY.md` 记录了本包生成前的构建、测试与证据核验；压缩包本身另有独立 SHA-256，可用于传输完整性验证。

## 启动正式实验前仍需人工决定

- 关闭 FC-8 / EXP-1；
- 冻结正式主版本、20 个 seed、算法 roster、45 个实例及哈希；
- 冻结初始种群来源与 45×20×2 的执行授权；
- 明确 A0–A4 是否进入单独正式消融矩阵，以及安全尾段的 FE 接受口径。
