# V35-P9 公平烟测报告

状态：`in_progress`

本阶段只完成工程烟测，不启动正式500000 FE实验、多seed、多实例或消融矩阵。

## 固定边界

- `familyMode=DEGENERATE_SINGLE_FAMILY`
- `setupMode=SEQUENCE_INDEPENDENT`
- `ShiftMode=NONE`
- `decoderMode=FM3`
- 主目标适配器：`[0,1,6]`
- seed：`20260808`

## 已验证

- `jmetal-problem`：67项测试通过。
- `jmetal-algorithm` V35及相关回归：46项测试通过。
- 本轮扩展后的 V35/历史定向集合：95项测试通过。
- 六模块 Maven Java 8 目标打包通过。
- 10粒子、500 FE V35生产烟测通过。
- DSCR事件、CA-TA-Lite事件、Test调用、局部完整评价均真实触发。
- FE未超过预算，最终非支配结果非空。

新增公平运行桥 `V35FairRunner`：固定 `FM3`、单族/序列无关设置和
`ShiftMode=NONE`，为 `V35_BASELINE` 与 `V35_FULL` 注入同一四向量初始种群，
并校验 `initialPopulationHash` 一致。CA-TA-Lite 使用独立
`V35MacroCandidateGateway` 的 N1--N5，不复用历史 O10--O13。

## 证据边界

当前结果只能证明规范生产配置的机制链路可运行。尚未证明正式500000 FE性能、统计稳定性、论文复现或三项创新的最终算法优越性。
