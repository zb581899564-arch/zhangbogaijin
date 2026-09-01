# V35-PFC5-F1 冻结契约分析报告

分析工具：`tools/analyze_f1_frozen_reference.py`（零 FE，不改算法）
结论：**F1 = FAILURE_CLASS_REPRODUCED**

---

## 1. 参考合同身份（未更新、未重建）

| 项 | 值 |
|---|---|
| `reference-contract.properties` SHA-256 | `ecdc5589ab4d36a028a0d53e9fcdbfc40ee1e04864df929c2e5c035b4481235f` |
| PFref `pfref-100_5_3_1.csv` canonical SHA-256 | `4dc85dd4fa3c7824ed2bf302b648355df796be7f15375db84047d23c4de683da` |
| PFref 点数 | **757** |
| ideal | `[755.144349612787, 110254.658096021, 286005.04205418704]` |
| nadir | `[1483.1764464620087, 129014.9215496972, 361084.4776851587]` |
| 归一化 | 逐目标 `(x − ideal)/(nadir − ideal)`，span 下限 EPS=1e-12，ideal/nadir 只取自冻结 PFref |
| HV | 归一化空间参考点 `(1.1,1.1,1.1)`，取值 clamp 到 `[0,1.1]`；实现为 `analyze_confirmation.py` 的精确副本（x-sweep + yz_union，EPS=1e-12） |
| IGD | 对归一化 PFref 的 mean-min 欧氏距离；同上为精确副本 |
| 目标顺序 | `[Cmax, TEC, TWC]`（目标槽位 `[0,1,6]`） |

PFref 由 PFref 自身归一化后反算出的 ideal/nadir 与合同中记录的冻结值**逐项相等**（脚本内已断言），证明 PFref 未被改动。

未执行任何被禁止的操作：F1 新前沿**未**加入 PFref；ideal/nadir **未**更新；reference **未**重新构造；**未**使用其它 campaign 的 reference；失败阈值**未**因结果而修改。

---

## 2. Gold 自检（计算任何 F1 数值之前必须先通过）

用与冻结锚点**完全同一条管线**（原始有限 front → 按冻结 PFref 归一化 → HV/IGD）重算历史 A2：

| 项 | 冻结锚点 | 本次重算 | 绝对偏差 |
|---|---|---|---|
| HV_A2 | `0.810244195451609` | `0.810244195451609` | **0.0** |
| IGD_A2 | `0.057804242003353316` | `0.057804242003353316` | **0.0** |

历史 A2 front SHA-256 = `75d8a44a71428274a591a1c6413ddac0cb7e7deb421da419a11b2d3196a204aa`（412 点），与契约记录的 `comparisonTargetFrontSha256` 一致。

**Gold 自检 PASS**，分析管线与冻结锚点同口径，可以继续。

---

## 3. F1 终态前沿

先规范化并排序后统计：

| 项 | 值 |
|---|---|
| rawFrontSize | **387** |
| finiteFrontSize | **387** |
| exactDedupSize | **387** |
| strictNdSize | **387** |
| canonicalFrontSha256 | `256dbce5e44dcd51fbfa10797773ec7e50299501cac6618028e0305f3b2ecb57` |
| front.csv 原始 SHA-256 | `f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd` |

`raw = finite = exactDedup = strictNd = 387`，说明终态前沿本身已严格非支配、无重复、无非法点。因此主判据值（原始口径）与严格非支配口径**完全相同**，两种口径不存在分歧。

三目标极值：

| 项 | 值 |
|---|---|
| minCmax | `755.144349612787` |
| minTEC | `113858.2152135067` |
| minTWC | `307754.57119086105` |

（minCmax 恰等于 PFref 的 Cmax ideal，属记录性观察；**Cmax 不是失败门**，按合同 `cmaxRole=NOT a failure gate; reported for mechanism interpretation only`。）

---

## 4. 指标与配对比较

配对比较对象固定为**历史 A2**（`instance=100_5_3_1`、`seed=20260901`、`arm=A2`、历史 accepted 500k 终态前沿）。
**没有**拿历史 A4 作为比较基线——历史 A4 仅用于确定性对照，不参与 Δ 计算。

| 角色 | HV | IGD | ΔHV | ΔIGD |
|---|---|---|---|---|
| 历史 A2（基线） | `0.810244195451609` | `0.057804242003353316` | 0（定义） | 0（定义） |
| **F1 新鲜 A4（500k, OFF）** | **`0.5545772540415207`** | **`0.15898065502479636`** | **`-0.31554307065117104`** | **`-1.7503285142217353`** |

### 判据与符号约定

用户指令给出：

```text
deltaHV  = (HV_A4_F1  - HV_A2_HISTORICAL) / HV_A2_HISTORICAL
deltaIGD = (IGD_A2_HISTORICAL - IGD_A4_F1) / IGD_A2_HISTORICAL
```

冻结合同 `reference-contract.properties` 的机器可读定义：

```text
deltaHVDefinition  = (HV_fresh - HV_histA2) / HV_histA2
deltaIGDDefinition = (IGD_histA2 - IGD_fresh) / IGD_histA2   (positive = fresh better)
```

**两者完全一致，无符号分歧，无需以合同为准做修正。** 两个 Δ 均为负表示新鲜 A4 比历史 A2 更差（HV 更小、IGD 更大）。

### 失败门

```text
deltaHV  < -0.05    →   -0.31554307065117104 < -0.05   成立
deltaIGD < -0.20    →   -1.7503285142217353  < -0.20   成立
```

**联合条件成立 → `failureGateTriggered = true`。**

---

## 5. 输出文件

- `normalized-front.csv` — 按冻结 PFref 归一化并排序后的 F1 前沿（387 点，`Cmax,TEC,TWC` 表头，`%.17g`，排序/去重规则与 `canonical_text` 一致）
- `f1-metrics.csv` — 全部指标与裁决
- `historical-a2-comparison.csv` — 历史 A2 基线 / 本次重算 / F1 / 历史 A4（仅确定性参考，明确标注 NOT A BASELINE）
- `frozen-reference-analysis.properties` — 机器可读结论

---

## 6. 判定

```ini
GOLD_SELFCHECK=PASS
FROZEN_REFERENCE_ANALYSIS=PASS
failureGateTriggered=true
deltaHV=-0.31554307065117104
deltaIGD=-1.7503285142217353
F1=FAILURE_CLASS_REPRODUCED
consumedFE=0
changedAlgorithm=false
```
