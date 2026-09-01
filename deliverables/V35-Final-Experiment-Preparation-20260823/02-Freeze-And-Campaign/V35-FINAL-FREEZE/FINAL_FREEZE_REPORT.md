# V35 Final Freeze Report

日期：2026-08-23  
状态：`CANDIDATE_INFRASTRUCTURE_READY`，**不是**正式运行授权。

## 已冻结的科学边界

本候选边界继承已验收的 DOE-1 参数冻结：

```text
FINAL_SEARCH_MIXTURE=[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE
GLOBAL_ORIGINAL; CA-TA-Lite -> inherited LS; A4-Pacing
dual-Q P=5/G=5; rho=0; directionalTeacherPool=false
population=100; nominal MaxFEs=500000
```

`ORDER_SWAP`、`REGION_AWARE`、`BP_RESERVED_LEGACY`、`rho>0`、active Shift、PF-SDST 与产品族/序列相关设置时间均未被重新启用。

## 溯源指纹

| 项目 | 值 | 状态 |
|---|---|---|
| 历史候选边界哈希 | `d85a07381da31dffb403a5fa08caaa3093f3d4d4ff2d10fb81c4a85b3081c34f` | 仅支持既有短预检 |
| 当前 Java/POM 树 SHA-256 | `6479e3bddd89be05e2a7423c0fc30a49c4f6756d7cc7ad035bee6d5c163ba4ba`（1146 文件） | 当前可重建树记录 |
| 当前 fat jar SHA-256 | `9631C821AD37522059F1BA3CEA278ACD974FD96166E939883FF7CE13373CFC08` | JDK 17 本地重新打包产物 |
| Git HEAD | `e034faafce5f9458a324a03a4aea7ef7098e698d` | **不可单独代表源代码**；工作树不干净 |
| DOE-1 held-out | 60/60 完成；没有替代 mixture 达到预注册 `median ΔCmax >= 2%` 门 | 已冻结的参数选择事实 |

`FREEZE_MANIFEST.json` 的 `freezeStatus=CANDIDATE_WITH_DIRTY_WORKTREE` 被保留为历史候选证据。它早于本轮 formal-gate 代码修订，故不能被用于正式源代码/部署一致性断言；FC-8 后必须用最终 source tree、jar、实例、扩展、疲劳参数和双初群哈希重新物化一个新的 formal manifest。

## 构建与隔离核验

- 使用 `E:\javavava` JDK 17 完成 Maven 打包；仅有 Javadoc 本地链接缺失的已忽略告警。
- `scripts/v35_campaign_runner.py`：4/4 定向测试通过。
- `tools/v35-analysis/`：7/7 数学与输入门测试通过。
- `ZhangBoV35FormalComparisonRunnerTest`：2/2 通过，证明 shipped formal plan 在 FC-8/seed/roster 未冻结时 fail-closed，且 A0 不携带 A4 的双Q协调对象。
- 本轨没有修改核心算法、PDDR、解码语义、DOE-1 冻结参数或历史实验结果。

## 尚未完成、不得跨越的门

`EXP-1=blocked_by_FC-8`；正式 20 个 seed、正式算法 roster、45 实例+SUT+疲劳参数 manifest、每 seed 双初群 hash、最终 main 机制哈希以及 FC-9/用户资源批准均未冻结。因此：

```text
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

只有这些前置关闭后，候选基础设施才可以接收一份不可变正式 campaign manifest。
