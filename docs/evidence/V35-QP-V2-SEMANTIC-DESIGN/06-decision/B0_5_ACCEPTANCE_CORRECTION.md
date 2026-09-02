# Phase B0.5 验收勘误与独立裁决修正报告（B0_5_ACCEPTANCE_CORRECTION）

勘误日期：`2026-09-02`
勘误性质：**Append-Only 治理勘误**——保留既有 B0.5 文件，明确补充独立裁决口径、可触达率修正区间与物理文件统计。

---

## 1. 独立验收与最终选择状态

在 Phase B0.5 执行过程中，Agent 原裁决登记为 `QP_V2_SEMANTIC_DECISION=SELECT_ONE`。按项目最高协作纪律及任务书独立审查要求，在此勘误修正为：

```ini
previousDecision=SELECT_ONE
independentAcceptance=NEEDS_EXPLICIT_USER_CHOICE
userFinalSelection=CANDIDATE_A_TOPK_UNIFORM
k1DesignReductionProven=true
k1BehaviorEquivalence=NOT_TESTED
```

- **说明**：算法候选设计及对比矩阵呈现了候选 A（均匀随机探索）与候选 B（确定性轮转）的理论取舍。用户已在后续正式任务书中明确批准选择 `CANDIDATE_A_TOPK_UNIFORM`。
- **等价性状态界定**：$K=1$ 在数学与程序状态归纳层面已证明严格还原（`k1DesignReductionProven=true`），但尚未经运行期字节级重放验证（`k1BehaviorEquivalence=NOT_TESTED`）。

---

## 2. 经验可触达率口径修正

B0.5 原报告中使用的“33.6%–56.4%”为包含所有非 KEEP 动作且假定 COMPLEMENTARY 均具多候选的上限口径。为消除歧义，现对目标实例上的真实可触达率区间进行严格的分层修正：

| 算例与预算 | 严格下界（DIRECTIONAL + EPSILON） | 理论上界（含 COMPLEMENTARY） | 修正口径说明 |
|---|---|---|---|
| **50k HARD（100_5_3_1）** | **30.14%** (7,564 / 25,100) | **33.60%** (8,434 / 25,100) | 下界仅计确定具有多候选池的动作；上界计入 COMPLEMENTARY（部分事件可能因退化过滤仅剩1个有效候选） |
| **500k HARD（100_5_3_1）** | **34.65%** (94,179 / 271,800) | **50.62%** (137,577 / 271,800) | 真实 A4 500k 运行数据，下界为 Directional+Epsilon 占比，上界包含全部非 KEEP |
| **500k NORMAL（100_2_3_1）** | **42.65%** (115,929 / 271,800) | **56.36%** (153,188 / 271,800) | 正常算例控制组，下界仍超 40% |

**禁止事项**：后续报告严禁将 33%–56% 描述为“精确实测触发率”，必须规范表述为“可触达率估计区间：50k Hard 30.14%–33.60%，500k Hard 34.65%–50.62%”。

---

## 3. B0.5 证据物理文件数量核算

- 清单登记文件数：24 个（分布于 00 至 06 目录及根部 CSV）。
- 勘误新增文件：`06-decision/B0_5_ACCEPTANCE_CORRECTION.md` 及 `06-decision/QP_V2_USER_SELECTION.properties`。
- 物理文件总数核算：24 个原始文件 + 2 个勘误文件 + `evidence-sha256.tsv` 自身 = **27 个物理文件**。
- 全量 SHA-256 清单重新生成并完成反向复算。
