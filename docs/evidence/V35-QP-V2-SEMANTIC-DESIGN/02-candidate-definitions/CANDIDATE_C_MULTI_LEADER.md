# 候选C：多领导联合参与 / 多子代生成（超范围/否决对照）

候选ID：`CANDIDATE_C_MULTI_LEADER`
设计性质：**超单变量边界候选（定义完备后明确论证为何不可作为合格单变量候选）**。

---

## 1. 16项完整定义与超范围论证

| # | 字段 | 完整规范与超范围分析 |
|---|---|---|
| 1 | **候选名称与核心机制** | `CANDIDATE_C_MULTI_LEADER`（多领导联合参与 / 多子代生成）。定义为：Qp 单动作选出 Top-K 个个人领导后，(C1) 在 CFVF 中聚合为多重心引导向量，或 (C2) 由每个领导分别生成一个 CFVF 子代并全部评价，或 (C3) 生成 $K$ 个子代但用代理准则无评价选取 1 个。 |
| 2 | **K 的具体计数对象** | 同时进入 CFVF 速度更新计算的个人领导数量（C1），或单粒子在单轮 Qp 中生成的待选/待评价子代数量（C2/C3）。 |
| 3 | **K 的参数空间与默认值** | $K \in \{1, 2, 3, 4\}$。$K=1$ 时单领导。 |
| 4 | **每个动作的候选集合来源与过滤条件** | 与候选A相同，取前 $K$ 项。 |
| 5 | **候选集合在动作执行前的排序规则** | 与候选A相同。 |
| 6 | **动作掩码（mask）生成逻辑** | 同候选A。 |
| 7 | **单动作被选中后如何从候选集选取最终领导** | 不再是“从中选 1 个领导”，而是：<br>• C1：全部 $K$ 个领导进入 CFVF 速度公式；<br>• C2：调用 $K$ 次 CFVF 生成 $K$ 个子代；<br>• C3：调用 $K$ 次 CFVF 生成 $K$ 个子代并用无FE代理准则挑选1个。 |
| 8 | **并列破平（tie-break）规则** | N/A（C1全部使用；C2全部生成；C3需发明额外代理破平）。 |
| 9 | **回退（fallback）规则** | 类似A。 |
| 10 | **与当前Qp四动作/掩码/奖励/TD的接口关系** | C1改变CFVF与奖励结算时序；C2改变每轮转移数量（每粒子 $K$ 个子代存活/奖励如何记入单个TD？）；严重破坏单变量关系。 |
| 11 | **与个人档案容量L=6、更新与去重的接口关系** | C2 每轮向档案插入 $K$ 个子代，导致档案写入速率翻倍，改变更新收敛动态。 |
| 12 | **与CFVF、Qg、PDDR、CA-TA、FE预算的接口关系** | **★ 致命越界**：<br>• C1 改变 CFVF 核心公式（从四向量单差分变为多点中心加权），直接违反总路线图和任务书“不改变 CFVF 公式”硬性冻结；<br>• C2 每轮消耗 $100 \times K$ 次评价，直接改变每轮 FE 步长与相一致终止协议，破坏 $MaxFEs=500000$ 的公平比较基座；<br>• C3 在 CFVF 内部产生 $K$ 倍的变异/交叉 RNG 消费，且发明了无先例的“子代筛选代理”，其实质是“CFVF 子代选择轴”，而非“Qp 领导选择轴”。 |
| 13 | **RNG消费合同** | C1 需定义权重采样（若随机）；C2/C3 导致 CFVF RNG 消费按 $K$ 成倍膨胀，无法实现跨 $K$ 隔离。 |
| 14 | **可观测性与遥测字段** | 难以在既有 observer 框架下单槽位表达（单粒子多子代/多领导）。 |
| 15 | **为什么可能改善困难实例中后段覆盖** | 理论上多点联合拉动可提供更丰富的组合方向；但代价是彻底破坏现有算法体系的因果隔离。 |
| 16 | **与历史失败路线与冻结边界的冲突判定** | **直接违反全部三项硬性冻结**（CFVF公式冻结、FE预算恒等冻结、单一变量边界）。不是合格的单轴候选，**必须在预注册阶段以超范围为由直接排除**。 |

---

## 2. 伪代码轮廓（标明越界位置）

```java
// =========================================================================
// 候选 C 伪代码轮廓（展示越界违规点）
// =========================================================================

public class CandidateC_MultiLeader_OUT_OF_SCOPE {

  // 变体 C1：多领导加权聚合（★ 违规：改变 CFVF 公式）
  public double[] computeAggregatedVelocity(
      List<ZhangBoArchiveEntry> topKLeaders,
      double[] currentPosition,
      double[] gbestPosition) {
    // 违规点：现有 CFVF 公式为 velocity = w*v + c1*r1*(pbest - x) + c2*r2*(gbest - x)
    // 聚合型将 pbest 替换为 sum(w_i * pbest_i)，引入了新的权重超参数与非线性组合
    // 属于结构性算法重写，超出了单参数 K 的范畴
    throw new UnsupportedOperationException("VIOLATES_CFVF_FREEZE");
  }

  // 变体 C2：多子代全评价（★ 违规：改变 FE 预算协议）
  public List<PermutationSolution<Integer>> generateAndEvaluateKOffspring(
      PermutationSolution<Integer> particle,
      List<ZhangBoArchiveEntry> topKLeaders,
      Problem problem) {
    List<PermutationSolution<Integer>> offspring = new ArrayList<>();
    for (ZhangBoArchiveEntry leader : topKLeaders) {
      // 每次循环产生一次完整的 CFVF 变异并消耗 1 次 FE
      // 导致单代 FE 从 100 膨胀到 100*K
      // 违规：改变了相一致预算终止协议与外循环时序
      PermutationSolution<Integer> child = cfvfUpdate(particle, leader);
      problem.evaluate(child); // ★ 改变 FE 消耗！
      offspring.add(child);
    }
    return offspring;
  }
}
```
