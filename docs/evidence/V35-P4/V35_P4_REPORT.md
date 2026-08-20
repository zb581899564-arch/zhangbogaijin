# V35-P4 规范疲劳解码报告

状态：`completed`

规范生产问题继续使用显式 `CANONICAL_NO_FATIGUE/FM1/FM2/FM3` 模式。`ZhangBoFatigueEvaluator` 统一接收 v3.5 的退化设置模型，按工件身份读取四向量资源，使用既有 `PT0/SET0/AT0`、自然恢复、疲劳倍率和后续阶段疲劳感知选工语义。

测试：jmetal-problem 全部 67 项通过，包含公式、MA/WA 身份、FM 模式、SUT/参数清单和 v3.5 工厂回归。

移位在 v3.5 工厂中固定为 `NONE`，不进入本报告的调度结果。
