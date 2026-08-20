# V35-P2 序列无关设置时间契约报告

状态：`completed`

已实现 `SetupMode` 和 `ProductFamilySetupModel`。正式模式固定为 `SEQUENCE_INDEPENDENT`，设置时间只读取现有 `SUT[job][stage]`；产品族转移和机器换型因子在退化配置中分别为0和1。

`ZhangBoCanonicalProductionProblem` 保存并校验该模型，`ZhangBoV35ProblemFactory` 无法注入移位或作者诊断路径，并始终绑定单族、序列无关设置模型。

测试：`ProductFamilySetupModelTest`、`ZhangBoV35ProblemFactoryTest` 通过。

PF-SDST真实序列相关设置时间：`false`，未采样、未进入正式比较。
