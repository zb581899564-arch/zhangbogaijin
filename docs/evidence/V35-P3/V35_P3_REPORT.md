# V35-P3 正式 Shift 冻结报告

状态：`completed`

v3.5 正式入口已收敛为无移位路径：

- `V35ProductionConfiguration.getShiftMode()` 恒为 `NONE`；
- `V35ProductionConfiguration.getShiftConfiguration()` 恒返回 `ZhangBoShiftConfiguration.none()`；
- `ZhangBoV35ProblemFactory` 没有 shift 参数，始终创建单族、序列无关、无移位的规范问题；
- `AUTHOR_DIAGNOSTIC` 不能通过 v3.5 工厂进入生产链。

P8/P8.6/P9 的 `LEFT_RIGHT` 入口没有删除，继续作为历史诊断路径；它们不属于 v3.5 当前生产配置、参考前沿或正式实验。

测试：`V35ProductionConfigurationTest` 与 `ZhangBoV35ProblemFactoryTest` 通过。
