# V35 Final 交付包：构建与核验摘要

## 已验证的构建/测试

- Java 定向测试：6 项通过。
- campaign 调度器测试：4 项通过。
- 指标与统计管线测试：7 项通过。
- Java 目标字节码：`major version 52`（Java 8）。
- `V35-FORMAL-EXPERIMENTS/00_protocol` 证据清单：12 项匹配。
- `V35-FINAL-FREEZE` 证据清单：121 项匹配。
- `V35-ABLATION/2000FE-smoke-20260822` 证据清单：77 项匹配。

## 当前源与构建绑定

```text
source tree SHA-256 = 6479e3bddd89be05e2a7423c0fc30a49c4f6756d7cc7ad035bee6d5c163ba4ba
formal runner fat jar SHA-256 = 9631C821AD37522059F1BA3CEA278ACD974FD96166E939883FF7CE13373CFC08
```

源树哈希覆盖当前相关 Java/POM 源（排除 `target`）。该值代表本包生成时的工作副本，不意味着整个历史工作树处于 Git clean 状态。

## 正式实验状态

```text
正式 500k 运行数 = 0
正式论文结论 = 未产生
可运行性 = 已准备，但受到 FC-8 / EXP-1 冻结门约束
```
