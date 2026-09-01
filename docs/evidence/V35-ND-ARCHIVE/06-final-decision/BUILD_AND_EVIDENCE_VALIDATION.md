# 构建、测试与证据验收记录

日期：2026-08-24

## 通过项

- 档案候选/ND0行为定向测试：14项，0失败、0错误。
- Gate A Runner测试：1项，0失败、0错误。
- A2/A3事件sidecar解析测试：4项，0失败、0错误。
- A2/A3指定六条50k运行：6/6完成，150个运行清单条目反向复算失败0。
- Stage2恢复：60/60目标run完整；816个运行证据条目失败0。
- Java六模块 `package -DskipTests` 通过。
- 新增Runner与档案实验类字节码major version均为52（Java 8）。
- 冻结正式Jar仍为48269638字节，SHA-256仍为
  `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`。

## 历史回归边界

完整reactor测试在`jmetal-core`共执行651项，0 Failure、3 Error；三项均为P1已登记的作者/jMetal
遗留签名：`DefaultIntegerPermutationSolution`硬读缺失的`EADHFSP/150_8_5_1.txt`，涉及PMX、
PermutationSwapMutation和默认Solution构造。本轮没有新增core错误签名。

单独运行`jmetal-algorithm`全历史证据测试时，发现3个旧冻结证据断言与当前工作副本语义/前沿
顺序不一致：FC0/FC2的旧pressure语义hash与当前FC6语义hash不一致，P10.1的front文本比较仅
顺序不同。随后到达会执行500k历史重放的P21测试，主Agent为避免长时间重算及改写历史证据，
主动终止该全量历史重放。本轮相关定向测试和构建均已通过；这3项被登记为历史证据锚漂移，
未通过修改算法或更新旧期望值来掩盖。

因此本轮可以验收“新增观察工具、离线分析和证据裁决”，但不能把当前脏工作副本描述为“所有
历史证据测试全绿”。冻结正式Jar未重建、未被覆盖。

