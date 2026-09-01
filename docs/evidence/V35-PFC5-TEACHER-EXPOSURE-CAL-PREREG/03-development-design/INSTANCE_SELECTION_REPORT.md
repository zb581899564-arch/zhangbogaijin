# Development 实例与 seed 选择报告

日期：2026-08-30　|　性质：预登记冻结，运行后不得更换
数据来源：`docs/evidence/V35-PFC5-PHASE0/02-instance-role-registry/instance-exposure-role-registry.csv`（49 条，Phase 0 已冻结）

---

## 1. 角色分布与准入

| currentRole | 数量 | 可否进入 Race |
|---|---|---|
| `CONTAMINATED_DEVELOPMENT` | 17 | **可** |
| `VALIDATION_RESERVED` | 27 | 否 |
| `LEGACY_EXCLUDED` | 4 | 否 |
| `CASE_SELECTED_DIAGNOSTIC_ONLY` | 1（`100_5_3_1`） | **否**（F1 诊断 case） |

只有 `CONTAMINATED_DEVELOPMENT` 可入 Race。`100_5_3_1` 与 seed `20260901` 按角色与任务要求**双重排除**。

---

## 2. 实例选择（非性能规则）

| 槽位 | 选中实例 | 规则 | normal/hard 标签来源 |
|---|---|---|---|
| 20-job DEVELOPMENT | **20_2_3_1** | 在 size-20 的 `CONTAMINATED_DEVELOPMENT` 候选中取**实例 ID 字典序最小** | 本槽位不要求标签 |
| 50-job DEVELOPMENT | **50_2_3_1** | 在 size-50 的候选中取字典序最小 | 本槽位不要求标签 |
| normal100 DEVELOPMENT | **100_2_4_1** | 标签驱动：取既有证据中已标注为**正例**者 | ROADMAP D-107：`A2↔A4` 配对的 `100_2_4_1` 正例 |
| hard100 DEVELOPMENT | **100_8_3_1** | 标签驱动：取既有证据中已标注为**退化例**者；任务亦点名要求审计该实例 | ROADMAP D-107：`A0↔A2` 配对的 `100_8_3_1` 退化例 |

**标签必须来自既有证据、不得来自本次结果** —— 两个 100-job 槽位均按此执行，未使用任何本次或待运行的指标。

### 需要如实标注的一处口径差异

`100_8_3_1` 的「hard」标签来自 **A0↔A2** 配对（D-107），而不是 F1 所处的 **A2↔A4** 配对。A2↔A4 配对的退化例是 `100_5_3_1`，但该实例已被 `CASE_SELECTED_DIAGNOSTIC_ONLY` 排除。因此 hard100 槽位采用 `100_8_3_1`，其困难标签是**跨配对借用**的，这一点在解释 Race 结果时必须计入。

### 被拒绝的 100-job 候选及理由

- `100_2_3_1`、`100_5_4_1`：无既有 normal/hard 标签，且 `100_2_3_1` 的 `usedForAlgorithmDecision=false`。
- `100_2_5_1`：其正例标签属于 A0↔A2 配对，与本次 A2↔A4 语境不一致，故未用于 normal100。
- `100_5_5_1`、`100_8_4_1`、`100_8_5_1`：`VALIDATION_RESERVED`，角色禁止。

---

## 3. 实例绑定（冻结哈希）

| 实例 | instance SHA-256（前 16） | setup 文件 SHA-256（前 16） | fatigue 文件 SHA-256（前 16） |
|---|---|---|---|
| 20_2_3_1 | `47d32d48e719219c` | `c39040dbc92f41ee` | `7116c996e84c229a` |
| 50_2_3_1 | `d08d6abc46788d46` | `f9bde51a5f873896` | `46192fe26bfc7920` |
| 100_2_4_1 | `10b57d8c0c8ec590` | `70f5b340bdb94058` | `afe3cc7d3d4d6d85` |
| 100_8_3_1 | `cf5a0bb2283612e9` | `dd34e561e76cdd9b` | `caccad36fe5381fd` |

四个实例的 instance SHA 与 Phase 0 角色登记表的 `sourceHashes` **逐项一致**（实测复核通过）。

`setupConfigurationSha256` / `fatigueConfigurationSha256` / `problemConfigurationSha256` 在表中标记为 `TO_BE_COMPUTED_BY_FROZEN_LAUNCHER`：**禁止手工推测或复制其它实例的值**，必须由冻结代码在构造 RunKey 时实测产出（与 F1 的做法一致，见 F1 `provenance.properties`）。

---

## 4. seed 选择（确定性最小规则）

候选池：`20260906 … 20260915`（预登记，先于任何运行冻结）。
已排除：`20260901`（历史失败 case + F1）、`20260902–20260905`（A2/A4 多实例确认 campaign；其中 `20260903` 亦用于 FC5-T 100k 筛查）。

**选中：`20260906`、`20260907`** —— 池中数值最小的两个合法 seed。

规则明文：**按数值最小选取，禁止以「表现稳定」或「有利」为由选 seed**。两个 seed 在任何既有 campaign 中均无使用记录。

---

## 5. 快照要求

每个 `(instance × seed)` 必须绑定一个**显式四向量快照**，由冻结的 `ZhangBoV35FormalInitialPopulationFreezeRunner` 生成（零 FE），并记录：
`snapshotSha256`、`initialPopulationHashV35`、`initialPopulationHashP8`。

同一 `(instance × seed)` 的 C0/C1/C2/C3 四个配置**必须共用同一快照**，从而保证四配置起点逐位一致。

---

## 6. 冻结声明

```ini
developmentInstances=20_2_3_1;50_2_3_1;100_2_4_1;100_8_3_1
developmentSeeds=20260906;20260907
excludedDiagnosticInstance=100_5_3_1
excludedCaseSeed=20260901
instanceChangeAfterFreeze=forbidden
seedChangeAfterFreeze=forbidden
snapshotReuseAcrossConfigs=required
```
