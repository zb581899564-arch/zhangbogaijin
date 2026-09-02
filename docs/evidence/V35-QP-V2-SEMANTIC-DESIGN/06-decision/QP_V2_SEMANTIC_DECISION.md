# Qp-v2 语义裁决报告（QP_V2_SEMANTIC_DECISION）

裁决日期：`2026-09-02`（Phase B0.5）
裁决性质：**0-FE 纯设计科学裁决**（无代码改动、无编译、无运行、0 FE）。

---

## 1. 最终裁决结果

```ini
QP_V2_SEMANTIC_DECISION=SELECT_ONE
SELECTED_CANDIDATE_ID=CANDIDATE_A_TOPK_UNIFORM
SELECTED_CANDIDATE_NAME=动作一致Top-K候选池+均匀随机探索
K1_STRICT_REDUCTION_PROVEN=true
SINGLE_VARIABLE_BOUNDARY_CLEAN=true
HISTORICAL_OVERLAP_RISK=CLEAN_EXCLUDED
DECISION_STATUS=APPROVED_FOR_PREREGISTRATION_ONLY
IMPLEMENTATION_AUTHORIZED=false
EXPERIMENT_AUTHORIZED=false
```

---

## 2. 裁决理由（对照任务书十项标准逐一论证）

### 2.1 准则 1：K=1 严格还原当前 A4（权重最高）
- **候选 A**：数学证明（定理 1）与程序级状态归纳法证明（`K1_REDUCTION_PROOF.md`）已完备。当 $K=1$ 时，$Pool(a, 1)$ 大小恒为 1，直接分支返回规范首项，**消耗 0 次额外伪随机数**。Mask、动作抽取序列、所选条目指纹、CFVF 输入、子代染色体、解码目标值、奖励计算、Q 表更新、FE 预算与终态 `front.csv` 实现**端到端逐位等价**（SHA-256 相同）。**判定：PASS**。
- **候选 B**：同理，模运算 $0 \bmod 1 = 0$，零 RNG，逐位等价。**判定：PASS**。
- **候选 C**：结构性改变 CFVF 或 FE 步长，无法还原 A4。**判定：FAIL**。

### 2.2 准则 2：单一变量边界清晰
- **候选 A**：$K \in \{1,2,3,4\}$ 是进入算法的唯一新参数；池构造严格继承既有比较器；池内抽取采用最小假设的均匀分布（直接对照 Controller:477 既有的动作层均匀探索）。**无任何第二隐藏自由度**。**判定：PASS**。
- **候选 B**：轮转索引函数需要人为选择计数器来源（谱系序数？代数？粒子ID？），构成未经验证的第二设计自由度，不满足严格单一变量标准。**判定：FAIL**。
- **候选 C**：改变 CFVF 公式或 FE 预算，严重违反单一变量原则。**判定：FAIL**。

### 2.3 准则 3：FE 预算与相一致终止完全恒等
- **候选 A & B**：候选步纯属内存对象选择，零增加或减少解码器调用。保持 `MaxFEs=500000` 与相一致终止协议。**判定：PASS**。
- **候选 C (C2)**：每轮生成 $K$ 个子代并评价，FE 膨胀 $K$ 倍。**判定：FAIL**。

### 2.4 准则 4：Qp 动作、动作掩码与奖励公式完全不修改
- **候选 A & B**：Mask 显式由 $K=1$ 规范候选生成；奖励 $r = f(parent, child, archiveSurvived, fatigue)$ 不读取领导实体；Q 表更新与 TD 结构 100% 保持不变。**判定：PASS**。

### 2.5 准则 5：真实可触达的非空冗余多样性杠杆（非理论空谈）
- **经验量化事实**（来自既有已验收遥测，0 新 FE）：
  - A4 500k 真实运行（100-job）中，非 KEEP 动作被选中的频率达 **50.6%（HARD）至 56.4%（NORMAL）**；
  - 50k HARD 实例上，选择时个人档案规模达 1–5（$\ge 2$ 占 59.95%，$\ge 3$ 占 35.9%）；
  - 非 KEEP 动作被选中且档案 $\ge 2$ 的事件占全部 Qp 动作的 **33.60%**（50k）至 **50.6%**（500k）；
  - Qp 占全部教师事件的 **95.6%**，故 $K$ 影响覆盖全部教师事件的 **32%–54%**。
- **杠杆有效性**：绝非“理论上多样但现场不足2个”的无用参数；在目标 100-job 算例上具备极为充沛的物理触发空间。**判定：PASS**。

### 2.6 准则 6：不重复历史失败路线
- 详见 `HISTORICAL_OVERLAP_ANALYSIS.md`。与已关闭的 teacher-lambda（Qg侧/1.12%覆盖/记账惩罚）存在两个数量级的物理差异；与 Q1（动作级破平）正交；与 Q0（确认确定性四动作 pbest 策略存在伤害）在科学归因上方向一致（假设驱动检验）。**判定：PASS**。

### 2.7 准则 7–9：可观测性、Java 8 兼容性与计算复杂度
- 5 个只读遥测字段已完整设计；纯 Java 8 标准库实现；单次选择对最多 6 个条目做排序，复杂度 $O(1)$，对运行时间影响 $< 0.01\%$。**判定：PASS**。

### 2.8 准则 10：论文级可解释性
- 候选 A 可规范表述为：*“Action-consistent Top-K cognitive leader exploration under lineage non-dominated archives”*（谱系非支配档案下动作一致的 Top-K 认知领导均匀探索），直接将控制器动作层的 $\epsilon$-greedy 均匀探索自然延拓至候选层，理论形式对称优美，论文论证逻辑极度自洽。**判定：PASS**。

---

## 3. 候选综合排序与裁决推论

1. **候选 A（`CANDIDATE_A_TOPK_UNIFORM`）**：在全部 10 项准则上全票通过，为唯一兼具理论自洽、实现完备与强可触达性的合格方案。
2. **候选 B（`CANDIDATE_B_TOPK_DETERMINISTIC`）**：虽具零 RNG 优势，但在准则 2（第二自由度）与准则 10（论文可解释性）上显著弱于 A，被 A 完全支配。
3. **候选 C（`CANDIDATE_C_MULTI_LEADER`）**：严重越界，直接否决。

因此，按照任务书决策逻辑，本裁决**不存在需要提交用户二选一的真正科研取舍**（候选 B 存在结构性缺陷），依规直接判定为 `SELECT_ONE(CANDIDATE_A_TOPK_UNIFORM)`。

---

## 4. 后续工程任务草案（Phase B1 预注册模板，不执行）

若后续获得用户新任务书批准实施 Phase B1，建议采用如下工程协议：

1. **单变量配置**：
   - 算法主线保持冻结 Jar；在独立分支引入 `QpCandidateSelectionPolicy` 枚举（`A4_CANONICAL`, `TOPK_UNIFORM`）与参数 $K \in \{1,2,3,4\}$。
2. **三级测试门**：
   - **门 1（2k 本地）**：$K=1$ 单机运行与 A4 逐位完全一致（`front.csv` 与 `evaluation_trace` SHA-256 相同，`qpPoolExtraDraws == 0`）。
   - **门 2（20k 机器）**：$K=1$ 保持严格等价；$K=2$ 在 100_5_3_1 上真实触发池分流（`qpPoolIndex > 0` 占比 $> 0$）；$K=3/4$ 依靠合成档案单元测试验证逻辑。
   - **门 3（250k 科学矩阵）**：4 档位 $\times$ 2 实例（Hard / Normal）$\times$ 2 seed = 16 条运行。
3. **晋升判定标准**：
   - 困难实例上 $K \ge 2$ 闭合 $\ge 50\%$ 原外部差距，且正常实例上无明显退化。

**再次重申：本草案仅为预注册归档，当前严禁启动任何编码、编译、构建或实验运行。**
