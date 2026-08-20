# V35-P8 CA-TA-Lite 契约与动作掩码报告

状态：`completed_engineering_smoke`

本轮已补齐独立的 `V35MacroCandidateGateway`：N1--N5 直接操作规范四向量，
不再把动作映射为历史 O10--O13，也不调用任何 Shift/ReleaseOverride 逻辑。
同时新增 `V35FairRunner`，以显式初始种群覆盖分别运行 `V35_BASELINE` 与
`V35_FULL`，并校验初始种群哈希一致。两模式仅作 I1/20_2_3_1 小规模工程烟测，
不代表正式统计实验。

已实现：

- `V35CaTaContext` 固定4个子群角色×6个瓶颈，共24个上下文；
- `V35MacroNeighborhood` 固定N1–N5；
- `V35CaTaLiteConfiguration.standard()` 固定 `nTest=1`、`applyMultiplier=1`、探索率`0.10`、连续失败阈值`3`；
- 动作掩码与 v3.5 的 SEQ/MAC/WOR/SET/FAT/BAL 路由一致。
- 新增 `V35CaTaLiteController`，固定 24 个 role-bottleneck 桶、`nTest=1`、`applyMultiplier=1`、10% 探索和连续失败3次重测；生产桥接在每个全局后代上执行并将局部候选送入评价后 PDDR。
- `V35ProductionSmokeTest` 已断言 `v35Lite` 事件、Test 调用和局部完整评价均大于0。

尚未完成：N1–N5 的专用候选生成器仍通过现有合法邻域网关适配，宏动作的独立语义统计、跨父粒子长期 Apply 预算和正式 20000 FE 烟测尚未完成；因此暂不宣称 CA-TA-Lite 方案完全对齐。
