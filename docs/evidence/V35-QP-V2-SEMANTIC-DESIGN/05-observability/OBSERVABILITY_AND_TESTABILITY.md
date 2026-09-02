# 可观测性与可测试性设计（OBSERVABILITY_AND_TESTABILITY）

设计日期：`2026-09-02`（Phase B0.5）
目标：为后续 Phase B1（若获批准）制定无需修改主算法即可完整验证 K 语义的观察字段与测试协议。

---

## 1. 遥测输出字段规范（只读观察者协议）

所有新字段必须遵循 **V5 Observer 纪律**：仅通过 observer/listener 旁路记录，不进入搜索决策，不消耗 FE，不修改算法状态。

| 字段名称 | 类型 | 语义说明 | 预期取值 / 约束 |
|---|---|---|---|
| `qpPoolK` | `int` | 当前运行配置的 $K$ 值 | $\{1, 2, 3, 4\}$，单次运行恒定 |
| `qpPoolSize` | `int` | 当前被选定动作的候选池实际大小 | $[1, \min(K, \text{candidateViewSize})]$ |
| `qpPoolIndex` | `int` | 本次实际选中的池内索引 | $[0, \text{qpPoolSize}-1]$；0 表示规范最优候选 |
| `qpSelectedIsCanonical` | `boolean` | 是否选中了与 A4 相同的规范候选 | `qpPoolIndex == 0` |
| `qpPoolExtraDraws` | `long` | 候选步累计发生的伪随机数抽取次数 | $K=1$ 恒为 0；$K \ge 2$ 时仅当池大小 $\ge 2$ 时单调累加 |

既有字段继续完整保留并复用：
- `qAction`：KEEP / DIRECTIONAL / EPSILON / COMPLEMENTARY
- `candidateViewSize`：选择时个人档案实际大小
- `selectedDirectionalScore`：所选条目的方向标量分 $\phi$
- `eligibleBestDirectionalScore`：当前合法动作中最佳条目的方向标量分
- `directionalRegret`：$\phi(\text{selected}) - \phi(\text{best})$
- `teacherFingerprint`：所选领导的四向量解指纹

---

## 2. Phase B1 验收测试门设计（预注册草案，不执行）

### 2.1 门 1：2k 本地行为等价门（Byte-Level Equivalence Gate）
- **目标**：验证 $K=1$ 在任意算例上与当前冻结 A4 逐位完全一致。
- **运行配置**：单机、固定 seed（如 20260822）、2000 FE、100_5_3_1。
- **断言集**：
  1. `front.csv` SHA-256 逐位一致；
  2. `evaluation_trace` SHA-256 逐位一致；
  3. `initialPopulationHash` 逐位一致；
  4. `qpPoolExtraDraws == 0`（全生命周期 0 额外抽取）；
  5. `qpSelectedIsCanonical == true`（100% 吻合）。

### 2.2 门 2：20k 机制触发与等价门（Machine-Level Gate）
- **目标**：验证 (a) $K=1$ 在 20k 阶段仍保持严格等价；(b) $K=2$ 真实产生池分流事件。
- **运行配置**：20258 FE、100_5_3_1、seed 20260901。
- **断言集**：
  1. $K=1$ 臂与 A4 50k/20k 基线前沿及事件流哈希严格一致；
  2. $K=2$ 臂中 `qpPoolSize >= 2` 的事件数占比 $\ge 5\%$（由既有经验数据，20k hard 预期约 7.15%）；
  3. $K=2$ 臂中 `qpPoolIndex > 0`（即选出非规范条目）的事件数真实大于 0（预期约 3.5%）；
  4. **工程限制特别说明**：由于在 20k 时 100-job 实例档案规模 $\le 2$（实测 89% size=1, 11% size=2, 无 size $\ge 3$），**$K=3$ 与 $K=4$ 的池大小分化在 20k 现场无法观测**。因此 $K=3/4$ 在 20k 的池大小正确性必须辅以**合成档案单元测试**（见 §3），不可在 20k 现场强行要求 $K=3/4$ 池大小大于 2。

### 2.3 门 3：250k 科学探索与正交检验门
- **目标**：在目标算例规模（100_5_3_1 与 100_2_3_1）上检验 $K \in \{1,2,3,4\}$ 的 Pareto 中后段覆盖与安全边际。
- **矩阵设计**：4 个 $K$ 档位 $\times$ 2 实例（Hard / Normal）$\times$ 2 新 seed = 16 条运行。
- **关键断言**：
  - Hard 实例上 $K=2/3/4$ 的 `qpPoolSize >= 3` 事件真实出现（预期 50k+ 时占比达 35%）；
  - Normal 实例上 $K$ 变异保持算法安全，不触发 100-job 否决线。

---

## 3. 单元测试套件规范（合成档案，零 FE）

必须建立以下独立单元测试类，覆盖各边界情况：

1. **`ZhangBoQpPoolConstructionTest`**：
   - 构造容量为 6 的合成非支配档案；
   - 验证 $K=1,2,3,4$ 时各动作池大小严格等于 $\min(K, |S_a|)$；
   - 验证各池元素按既有比较器严格有序，且第 0 个元素与 A4 规范 candidate 对象完全相同；
   - 验证 COMPLEMENTARY 在 quality 集合不足 2 时池为空且动作非法。

2. **`ZhangBoQpK1ReductionUnitTest`**：
   - 注入 Mock 随机数生成器（任何调用即抛异常）；
   - 执行 $K=1$ 下的 `selectLeaderFromPool`；
   - 断言返回对象与规范 candidate 一致，且 Mock 随机数未被触发（0 抽取调用）。

3. **`ZhangBoQpPoolDistributionTest`**：
   - 构造大小为 4 的池，运行 10,000 次抽取；
   - 断言索引 0, 1, 2, 3 的频次符合均匀分布（卡方检验 $p > 0.01$）。
