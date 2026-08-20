# V35-P18 deterministic CA-TA cost

日期：2026-08-13

CA-TA-Lite 的动作选择不再读取真实墙钟。选择代价固定为：

```text
0.5 * normalized(averageWorkUnits)
+ 0.5 * normalized(averageFullEvaluations)
```

`workUnits` 由 preview、合法性检查、候选构造和支配比较的确定性代理累计；`elapsedNanos` 只保留为诊断字段。`recent` 无效队列已移除。单元测试验证脚本时钟变化不会改变动作选择。

本工作包的工程实现已完成；跨 JVM 的完整 20k 审计仍由 V35-P10/P12/P13 集成门统一确认。
