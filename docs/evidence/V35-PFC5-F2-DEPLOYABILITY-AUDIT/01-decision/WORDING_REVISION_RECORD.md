# F2 审计措辞修正记录

执行时间：2026-08-30
触发：V35-PFC5-CAL-P0 工作包 §二
性质：**纯文档性措辞修正**，不改变任何裁决、不改变任何证据数值。

---

## 1. 被修正的表述

| 位置 | 旧表述 | 新表述 |
|---|---|---|
| `01-decision/F2_DEPLOYABILITY_AUDIT.md` §2.2 标题 | 「一个必须指出的反常事实」 | 「诊断 runtime 的 CFVF 能力边界」 |
| `01-decision/F2_DEPLOYABILITY_AUDIT.md` §2.2 首句 | 「**开启 telemetry 反而会丢失 CFVF 可见性。**」 | 「**诊断 runtime 缺少 CFVF 事件级遥测，仅保留有限聚合计数。**」 |
| `01-decision/F2_DEPLOYABILITY_DECISION.properties` | `telemetryOnLosesCfvfVisibility=true` | `cfvfEventTelemetryMissing=true` + `cfvfAggregateCountAvailable=true` |

同时在 properties 中补充两项限定，避免新键被过度解读：

```ini
cfvfAggregateCountSource=FORMAL_JAR_STATUS_MECHANISM_SUMMARY_ONLY
cfvfEventTelemetryMissingScope=DIAGNOSTIC_RUNTIME_121FBB49
```

## 2. 为什么修正

旧表述隐含了一个不成立的前提：仿佛存在同一个运行实体，开 telemetry 使原本可见的 CFVF 变得不可见。

事实是两者为**不同部署实体**，提供不同观测面，不构成同一实体的 ON/OFF 对照：

- 正式 `8DAD8F40…` 的 OFF 运行：通过 `status.properties` 的 `mechanismSummary` 提供 CFVF **聚合计数**（`cfvfOffspring=310000`、`cfvfRepairs=0`、`baselineUpdateEvents=0`），**没有事件级明细**。
- 诊断 `121FBB49…` 的 ON 运行：提供 Teacher / PDDR / CA-TA / checkpoint 四域**事件级遥测**，但**不提供 CFVF 事件级遥测，也不产出上述三个聚合计数**。

因此净事实是「CFVF 在事件级完全不可观测，聚合级仅正式 Jar 侧可得」，而不是「开遥测丢失了可见性」。修正后的表述更弱也更准确。

## 3. 明确未改变

```ini
F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT
blockingField=CFVF
FC5=MECHANISM_UNRESOLVED
```

三项裁决与全部证据数值（遥测行数、列数、字节数、grep 命中数、SHA-256）均未改动。§2.1 的 CFVF 缺失实证五条亦未改动。

同时明确：本次修正**不构成**对任何组件的排除或证成结论。CFVF 事件级不可观测是**诊断工具的能力边界**，不是对正式算法本身的否定。

## 4. 修改前后 SHA-256

| 文件 | 修改前 | 修改后 |
|---|---|---|
| `01-decision/F2_DEPLOYABILITY_AUDIT.md` | `df98a7961269865f5208bc93bf55d8aa5e6f37f4e2b906653e69fcdf5d1d2da4` | 见下方实测 |
| `01-decision/F2_DEPLOYABILITY_DECISION.properties` | `7849ccc5568444d3c360f73744d66bc0360e2897f3a50bcb15dac78fc35150d8` | 见下方实测 |
| `00-field-coverage/field-coverage-matrix.csv` | `2b346b61760800f2c82011e0bc0adda022cec52af7f29840106f60c2c6c455ef` | 未修改 |
| `00-field-coverage/telemetry-schema-inventory.csv` | `523b22e8b09c875de7c43f69bbfc3d8a2f62a526910c6f1719d8b88e5641e4f6` | 未修改 |
| `01-decision/PFC5-CAL_ROUTE_PROPOSAL.md` | `52cd24f12c2038f6d0217a8188dc9b2ae07ecc61a3e9eb601885cad4898d54fc` | 未修改 |

修改后实测值与重建后的证据清单见 `evidence-sha256.tsv`。
