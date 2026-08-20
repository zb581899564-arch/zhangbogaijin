# V35-P13 N3/N4/N5 macro audit

日期：2026-08-13

N3、N4、N5 现在由 `V35MacroCandidateGateway` 独立构造，不调用历史 O10–O13、ReleaseOverride 或 Shift：

- N3 根据关键/设置压力移动完整 JS+FA+MA+WA 工件包；
- N4 按 WOR、MAC、SET、FAT、BAL 路由第一阶段合法资源；
- N5 先执行一个结构 JS 动作，再执行一个必要资源动作，候选四向量必须改变。

网关单元测试覆盖了工件包身份保持、WOR/MAC 路由和 N5 双动作；完整疲劳解码后的自然恢复增益仍由主循环验收，尚待 I1/20k 集成矩阵，故 P13–P16 保持 `in_progress`。
