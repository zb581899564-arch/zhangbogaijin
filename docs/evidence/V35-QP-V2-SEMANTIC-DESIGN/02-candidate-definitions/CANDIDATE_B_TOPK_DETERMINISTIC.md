# 候选B：动作一致Top-K候选池 + 确定性轮转（CANDIDATE_B_TOPK_DETERMINISTIC）

候选ID：`CANDIDATE_B_TOPK_DETERMINISTIC`
设计性质：单变量 $K \in \{1,2,3,4\}$ 候选（零 RNG 改动对照）；通过确定性索引函数在池内轮转。

---

## 1. 16项完整定义

| # | 字段 | 完整规范 |
|---|---|---|
| 1 | **候选名称与核心机制** | `CANDIDATE_B_TOPK_DETERMINISTIC`（动作一致Top-K候选池 + 确定性轮转）。池构造与候选A完全相同；池内选取使用基于确定性状态计数器的模运算索引函数 $idx = rot(\text{state}) \bmod \|Pool\|$，实现完全零额外 RNG 消费的确定性轮转。 |
| 2 | **K 的具体计数对象** | 与候选A完全相同：被选定动作在当前档案中按该动作既有比较器排序后的前 $K$ 个条目。KEEP 豁免。 |
| 3 | **K 的参数空间与默认值** | $K \in \{1, 2, 3, 4\}$。$K=1$ 为基线等价值；不可变配置。 |
| 4 | **每个动作的候选集合来源与过滤条件** | 与候选A完全相同（KEEP单例；DIRECTIONAL/EPSILON取全档案；COMPLEMENTARY取质量集过滤后条目）。 |
| 5 | **候选集合在动作执行前的排序规则** | 与候选A完全相同：$(\phi, fp)$、$(\epsilon\text{-fit}, fp)$、$(\cos, -\text{spacing}, fp)$ 字典序全序。 |
| 6 | **动作掩码（mask）生成逻辑** | 与候选A完全相同：完全由 $K=1$ 规范候选生成，池不改变 mask。 |
| 7 | **单动作被选中后如何从候选集选取最终领导** | 设所选动作为 $a$，池为 $Pool(a)$：<br>• 若 $\|Pool(a)\| = 1$：直接返回 $Pool(a)[0]$。<br>• 若 $\|Pool(a)\| \ge 2$：计算确定性索引 $idx = (\text{lineageQpSelectionCount}) \bmod \|Pool(a)\|$，返回 $Pool(a)[idx]$。<br>• **全程零 RNG 调用**。 |
| 8 | **并列破平（tie-break）规则** | 比较器全序化由指纹升序保证；模运算输出唯一确定索引；无并列。 |
| 9 | **回退（fallback）规则** | 与候选A完全相同：不足 $K$ 项截取前 $\min(K, n)$；COMPLEMENTARY 空保持非法；settle/reconcile 沿用既有方向回退。 |
| 10 | **与当前Qp四动作/掩码/奖励/TD的接口关系** | 与候选A完全相同：四动作枚举、掩码、TD转移、奖励公式、Q表更新完全不变。 |
| 11 | **与个人档案容量L=6、更新与去重的接口关系** | 与候选A完全相同：容量6、三目标ND过滤、近重复去重、截断完全不变。 |
| 12 | **与CFVF、Qg、PDDR、CA-TA、FE预算的接口关系** | 与候选A完全相同：CFVF仅输入领导identity变化；Qg/PDDR/CA-TA/FE预算完全不变。 |
| 13 | **RNG消费合同** | **全 $K \in \{1,2,3,4\}$ 恒定 0 次额外抽取**。跨 $K$ 的全局 RNG 消费流与当前 A4 逐位完全相同。唯一的差异发生在被选定领导的 identity，CFVF 内部的随机数消费完全不漂移。 |
| 14 | **可观测性与遥测字段** | 与候选A相同：`qpPoolK`, `qpPoolSize`, `qpPoolIndex`, `qpSelectedIsCanonical`；`qpPoolExtraDraws` 恒为 0。 |
| 15 | **为什么可能改善困难实例中后段覆盖** | 通过谱系内的周期性轮转交替使用池内不同位置的非支配锚点，避免单点长期锁定；在保持完全确定性重放的同时引入多锚点分散效果。 |
| 16 | **与历史失败路线的本质差异与风险暴露** | • **vs teacher-lambda**：无曝光记账、无损失惩罚；但需特别证明轮转计数器**不是隐式曝光治理**（轮转依赖全局或谱系选择序数，不统计各教师被选次数，无自适应调节，性质是开环确定性交替）。<br>• **关键风险（第二自由度缺陷）**：轮转函数存在任意性（按谱系序数？按代数？按粒子槽位？按全局序数？），每种选择都是未经先例支持的新增微语义；周期性轮转可能与 P5/G5 的 5 轮分块产生共振伪影；论文可解释性显著弱于均匀随机探索。 |

---

## 2. 精确算法伪代码（Java 8 语义）

```java
// =========================================================================
// 候选 B 伪代码：确定性轮转选择器
// =========================================================================

public class CandidateB_TopKDeterministicSelector {

  // 谱系级别的选择计数器（维护在 ZhangBoLineageMemory 中）
  // 每次该谱系成功选择一次非 KEEP 且 poolSize >= 2 的动作时自增

  public ZhangBoArchiveEntry selectLeaderFromPoolDeterministic(
      ZhangBoQpAction action,
      Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools,
      ZhangBoLineageMemory memory,
      TelemetrySink sink) {

    List<ZhangBoArchiveEntry> pool = pools.get(action);
    if (pool == null || pool.isEmpty()) {
      throw new IllegalStateException("Selected masked Qp action " + action + " has empty pool");
    }

    int poolSize = pool.size();
    ZhangBoArchiveEntry selected;
    int selectedIndex;

    if (poolSize == 1) {
      // K=1 或池大小为1：确定性首项，计数器不自增
      selected = pool.get(0);
      selectedIndex = 0;
    } else {
      // K >= 2 且 poolSize >= 2：按谱系历史选择序数做模运算
      long count = memory.getQpNonKeepSelectionCount();
      selectedIndex = (int) (count % poolSize);
      selected = pool.get(selectedIndex);
      memory.incrementQpNonKeepSelectionCount(); // 状态自增
    }

    // ★ 零 RNG 调用：全程不传入也不调用 PseudoRandomGenerator
    sink.recordPoolTelemetry(poolSize, selectedIndex, selectedIndex == 0);
    return selected;
  }
}
```
