# 候选A：动作一致Top-K候选池 + 均匀随机探索（CANDIDATE_A_TOPK_UNIFORM）

候选ID：`CANDIDATE_A_TOPK_UNIFORM`
设计性质：单变量 $K \in \{1,2,3,4\}$ 扩展；将当前Qp动作层已有的均匀探索语义（Controller:477）下延至候选层。

---

## 1. 16项完整定义

| # | 字段 | 完整规范 |
|---|---|---|
| 1 | **候选名称与核心机制** | `CANDIDATE_A_TOPK_UNIFORM`（动作一致Top-K候选池 + 均匀随机探索）。在当前动作既有比较器全序下截取前 $\min(K, n)$ 个条目构成候选池；池大小 $\ge 2$ 时均匀随机抽取1个作为本轮个人领导；池大小 $=1$ 时直接返回首项且零RNG抽取。 |
| 2 | **K 的具体计数对象** | 每次Qp选择时，**被选定动作**在当前粒子谱系个人档案中按该动作既有比较器排序后的**前 $K$ 个非支配条目**（`ZhangBoArchiveEntry`）。KEEP动作豁免（池天然单例）。 |
| 3 | **K 的参数空间与默认值** | $K \in \{1, 2, 3, 4\}$。$K=1$ 为基线等价值（严格还原当前A4）；推荐探索值 $K=2$；$K \in \{3,4\}$ 为深度探索臂。不可变配置字段，运行期恒定。 |
| 4 | **每个动作的候选集合来源与过滤条件** | • `KEEP`：单例池 $\{ find(E, fp_{req}) ?? argmin_\phi(E) \}$（无过滤，大小恒为1）。<br>• `DIRECTIONAL`：从档案全集 $E$ 排序后取前 $K$ 项（要求 $\|E\|>1$）。<br>• `EPSILON`：从档案全集 $E$ 排序后取前 $K$ 项（要求 $\|E\|>1$）。<br>• `COMPLEMENTARY`：从 quality集 $Q = \{e \in E : \phi(e) \le \min\phi + 0.15\}$ 中先过滤掉 $\|dir(cur,e)\| \le 10^{-12}$ 的退化条目得到 $Q'$；从 $Q'$ 排序后取前 $K$ 项。要求 $\|E\|>1 \land \|Q\| \ge 2 \land \|dir(cur,gb)\| > 10^{-12} \land \|Q'\| \ge 1$；否则池为空。 |
| 5 | **候选集合在动作执行前的排序规则** | 严格继承既有比较器的全序化：<br>• `DIRECTIONAL`：$(\phi(e) \text{ asc}, fp(e) \text{ asc})$<br>• `EPSILON`：$(\epsilon\text{-fitness}(e) \text{ asc}, fp(e) \text{ asc})$<br>• `COMPLEMENTARY`：$(\cos(e) \text{ asc}, -\text{spacing}(e) \text{ asc}, fp(e) \text{ asc})$（即spacing降序）<br>• `KEEP`：无序（单例） |
| 6 | **动作掩码（mask）生成逻辑** | **完全由 $K=1$ 规范候选生成**（调用既有 `ZhangBoQpCandidateSelector.build` 的4个规范候选，按 KEEP→DIRECTIONAL→EPSILON→COMPLEMENTARY 顺序做指纹去重）。**候选池不改变 mask**。这保证：(a) 动作合法性跨 $K$ 恒等；(b) 动作层 `nextInt(0,|valid|-1)` 的抽取界跨 $K$ 恒等；(c) 动作选择概率分布不受 $K$ 污染。 |
| 7 | **单动作被选中后如何从候选集选取最终领导** | 设所选动作为 $a$，其候选池为 $Pool(a)$：<br>• 若 $\|Pool(a)\| = 1$：直接返回 $Pool(a)[0]$（**无任何RNG调用**）。<br>• 若 $\|Pool(a)\| \ge 2$：调用 `random.nextInt(0, |Pool(a)| - 1)` 抽取索引 $idx$，返回 $Pool(a)[idx]$。<br>• 若 $\|Pool(a)\| = 0$：抛 `IllegalStateException`（合法的动作其池大小必 $\ge 1$）。 |
| 8 | **并列破平（tie-break）规则** | • 排序全序化：比较器最后一级为 `fingerprint.compareTo` 字典序升序破平，排序结果唯一确定，无并列。<br>• 随机抽取：`nextInt(0, |Pool|-1)` 内部由 Java 均匀伪随机产生，池内成员皆为档案中互异条目（指纹唯一），无并列可能。 |
| 9 | **回退（fallback）规则** | (a) 候选不足 $K$ 个：池截取为前 $\min(K, n)$ 项，有多少用多少，**不填充、不复制**。<br>(b) COMPLEMENTARY 候选集为空：池为空，该动作在 mask 中已被置为非法，无法被选中；若意外选中则抛异常。<br>(c) settle结转缺失：下一代档案中找不到所选指纹时，沿用既有 $argmin_\phi(\text{nextArchive})$ 方向回退。<br>(d) reconcilePopulation 缺失：沿用既有方向回退。全部回退均为既有代码逻辑，零新增恢复规则。 |
| 10 | **与当前Qp四动作/掩码/奖励/TD的接口关系** | • 四动作枚举与语义不变；<br>• 掩码完全不变（由规范候选决定）；<br>• TD转移三元组 $(s, a, s')$ 不变（$a$ 仍为所选动作，不记录池内索引）；<br>• 奖励 $r = f(parent, child, archiveSurvived, fatigue)$ 不读取所选领导对象（只读父子代与档案存活），公式零修改；<br>• Q表更新步完全不变。 |
| 11 | **与个人档案容量L=6、更新与去重的接口关系** | • 容量 $L=6$ 保持不变；<br>• 档案更新、三目标严格ND过滤、近重复去重（$10^{-4}$）、容量截断规则全部不变；<br>• 候选池仅在当前档案只读快照上构造，不向档案写回解，不影响档案状态。 |
| 12 | **与CFVF、Qg、PDDR、CA-TA、FE预算的接口关系** | • CFVF：全向量更新公式不变，仅其输入 `personalLeader` 的解实体由所选领导提供；<br>• Qg：完全独立，零交互；<br>• PDDR：全局原版 $GLOBAL\_ORIGINAL$ 不变；<br>• CA-TA-Lite：五宏邻域、时钟、代价信用不变；<br>• FE预算：零额外评价，FE闭合与相一致终止协议完全不变。 |
| 13 | **RNG消费合同** | • $K=1$：候选步 **0 次抽取**。全局RNG流与当前A4逐位完全相同。<br>• $K \ge 2$：在 EPSILON_GREEDY 与 GREEDY_FROZEN 模式下，当且仅当所选动作的 $\|Pool(a)\| \ge 2$ 时，在当前动作抽取之后立即执行恰好 1 次 `random.nextInt(0, |Pool(a)| - 1)`。<br>• KEEP 动作、warmup 阶段、以及 $\|Pool(a)\|=1$ 的事件（含档案大小=1）恒消耗 0 次。<br>• 无其他任何 RNG 消费变化。 |
| 14 | **可观测性与遥测字段** | 新增只读观察字段（零决策影响）：<br>`qpPoolK`（配置值）、`qpPoolSize`（所选动作实际池大小）、`qpPoolIndex`（所选索引，0=argmin）、`qpSelectedIsCanonical`（布尔，是否等于规范候选）、`qpPoolExtraDraws`（累计额外抽取计数）。既有字段（`qAction`, `candidateViewSize`, `directionalRegret`, `teacherFingerprint`）继续记录。 |
| 15 | **为什么可能改善困难实例中后段覆盖** | (a) 困难实例（100_5_3_1）上档案规模增长至 1–5（50k实测 60% $\ge 2$），存在真实多锚点底质；<br>(b) 当前确定性 argmin 每次在相同动作下重复暴露同一极值锚点，导致 Qp 教师 top5 集中度达 54.7%，驱动 CFVF 四向量反复向相同方向放大，CA-TA 定向强化，引发目标空间覆盖收缩；<br>(c) Top-K 均匀探索将暴露分散到池内各非支配锚点，打破极值锁定，使 CFVF 生成更广泛的目标权衡子代，促进中后段覆盖再生；<br>(d) 正常实例上档案规模小（20k 实测 91.4% 为单例），Top-K 自然退化为单例无扰动，形成结构性安全边际。 |
| 16 | **与历史失败路线（teacher-lambda/PDDR/pacing）的本质差异** | • **vs teacher-lambda（PFC5-CAL）**：teacher-lambda 作用于 Qg 锦标赛 comparator 惩罚项，带曝光记账，实测事件覆盖率仅 1.12%（结构性无杠杆被关闭）；候选A作用于 Qp 候选生成层（Qp占全部教师事件95.6%），无记账，实测覆盖率 33.6%–56.4%（两个数量级差异），是真实可触达的有效注入点。<br>• **vs PDDR（FC-6）**：FC-6 修改环境选择/生存配额；候选A完全不触碰环境选择。<br>• **vs pacing（FC-2）**：FC-2 调节局部搜索 FE 配额；候选A完全不改变 FE 分配。<br>• **vs Q1 cold-start**：Q1 修改零表动作级 tie-break 偏好；候选A保持动作策略完全不变，仅在动作内扩展候选集。 |

---

## 2. 精确算法伪代码（Java 8 语义）

```java
// =========================================================================
// 候选 A 伪代码：ZhangBoQpController.selectGroup 中的候选选择替换
// =========================================================================

public class CandidateA_TopKUniformSelector {

  // 1. 扩展候选池构造（替换或包装现有 ZhangBoQpCandidateSelector.build）
  public static class CandidatesWithPools {
    public final ZhangBoQpCandidateSelector.Candidates canonical; // 既有规范候选+mask
    public final Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools;

    public CandidatesWithPools(
        ZhangBoQpCandidateSelector.Candidates canonical,
        Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools) {
      this.canonical = canonical;
      this.pools = pools;
    }
  }

  public CandidatesWithPools buildPools(
      List<ZhangBoArchiveEntry> entries,
      String requestedFingerprint,
      ZhangBoSubSwarm group,
      ZhangBoArchiveEntry current,
      ZhangBoArchiveEntry gbest,
      ZhangBoArchiveBounds bounds,
      int K) {
    // 调用既有完全未修改的 build 方法获取规范候选与动作掩码
    ZhangBoQpCandidateSelector.Candidates canonical =
        selector.build(entries, requestedFingerprint, group, current, gbest, bounds);

    Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools = new EnumMap<>(ZhangBoQpAction.class);

    // KEEP 动作：恒定单例池
    ZhangBoArchiveEntry keepEntry = canonical.get(ZhangBoQpAction.KEEP);
    pools.put(ZhangBoQpAction.KEEP, Collections.singletonList(keepEntry));

    List<ZhangBoArchiveEntry> sorted = new ArrayList<>(entries);
    sorted.sort(Comparator.comparing(ZhangBoArchiveEntry::getFingerprint));

    if (sorted.size() > 1) {
      // DIRECTIONAL 池：按 (phi asc, fingerprint asc) 全序取前 min(K, sorted.size())
      List<ZhangBoArchiveEntry> dirList = new ArrayList<>(sorted);
      dirList.sort(Comparator
          .comparingDouble((ZhangBoArchiveEntry e) -> ZhangBoSubSwarmSemantics.archivePhi(e, group, bounds))
          .thenComparing(ZhangBoArchiveEntry::getFingerprint));
      pools.put(ZhangBoQpAction.DIRECTIONAL,
          Collections.unmodifiableList(dirList.subList(0, Math.min(K, dirList.size()))));

      // EPSILON 池：按 (epsFitness asc, fingerprint asc) 全序取前 min(K, sorted.size())
      Map<String, Double> epsFit = archive.epsilonFitnessValues(sorted, bounds);
      List<ZhangBoArchiveEntry> epsList = new ArrayList<>(sorted);
      epsList.sort(Comparator
          .comparingDouble((ZhangBoArchiveEntry e) -> epsFit.get(e.getFingerprint()))
          .thenComparing(ZhangBoArchiveEntry::getFingerprint));
      pools.put(ZhangBoQpAction.EPSILON,
          Collections.unmodifiableList(epsList.subList(0, Math.min(K, epsList.size()))));

      // COMPLEMENTARY 池：质量集内按 (cos asc, -spacing asc, fingerprint asc) 取前 min(K, |Q'|)
      double bestPhi = Double.POSITIVE_INFINITY;
      for (ZhangBoArchiveEntry e : sorted) {
        bestPhi = Math.min(bestPhi, ZhangBoSubSwarmSemantics.archivePhi(e, group, bounds));
      }
      double[] social = ZhangBoQpCandidateSelector.direction(current, gbest, bounds);
      double socialNorm = ZhangBoQpCandidateSelector.norm(social);

      List<ZhangBoArchiveEntry> quality = new ArrayList<>();
      for (ZhangBoArchiveEntry e : sorted) {
        if (ZhangBoSubSwarmSemantics.archivePhi(e, group, bounds) <= bestPhi + configuration.getQualityTolerance()) {
          quality.add(e);
        }
      }

      if (quality.size() >= 2 && socialNorm > ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
        List<ZhangBoArchiveEntry> compList = new ArrayList<>();
        Map<String, Double> cosMap = new HashMap<>();
        Map<String, Double> spacingMap = new HashMap<>();
        for (ZhangBoArchiveEntry candidate : quality) {
          double[] personal = ZhangBoQpCandidateSelector.direction(current, candidate, bounds);
          if (ZhangBoQpCandidateSelector.norm(personal) > ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
            compList.add(candidate);
            cosMap.put(candidate.getFingerprint(), ZhangBoQpCandidateSelector.cosine(personal, social));
            spacingMap.put(candidate.getFingerprint(), ZhangBoQpCandidateSelector.nearestDistance(candidate, sorted, bounds));
          }
        }
        if (!compList.isEmpty()) {
          compList.sort(Comparator
              .comparingDouble((ZhangBoArchiveEntry e) -> cosMap.get(e.getFingerprint()))
              .thenComparing((ZhangBoArchiveEntry e) -> -spacingMap.get(e.getFingerprint()))
              .thenComparing(ZhangBoArchiveEntry::getFingerprint));
          pools.put(ZhangBoQpAction.COMPLEMENTARY,
              Collections.unmodifiableList(compList.subList(0, Math.min(K, compList.size()))));
        } else {
          pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.emptyList());
        }
      } else {
        pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.emptyList());
      }
    } else {
      pools.put(ZhangBoQpAction.DIRECTIONAL, Collections.emptyList());
      pools.put(ZhangBoQpAction.EPSILON, Collections.emptyList());
      pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.emptyList());
    }

    return new CandidatesWithPools(canonical, pools);
  }

  // 2. 候选选择步（插入在 ZhangBoQpController.selectGroup 第160行）
  public ZhangBoArchiveEntry selectLeaderFromPool(
      ZhangBoQpAction action,
      Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools,
      PseudoRandomGenerator random,
      // 遥测输出容器
      TelemetrySink sink) {

    List<ZhangBoArchiveEntry> pool = pools.get(action);
    if (pool == null || pool.isEmpty()) {
      throw new IllegalStateException("Selected masked Qp action " + action + " has empty pool");
    }

    int poolSize = pool.size();
    ZhangBoArchiveEntry selected;
    int selectedIndex;

    if (poolSize == 1) {
      // ★ 关键合同：单例池恒为 0 次 RNG 抽取
      // 当 K=1 时，所有合法动作的 poolSize 恒为 1，完全走此分支
      selected = pool.get(0);
      selectedIndex = 0;
    } else {
      // ★ K >= 2 且 poolSize >= 2 时：均匀随机抽取
      selectedIndex = random.nextInt(0, poolSize - 1);
      selected = pool.get(selectedIndex);
      sink.recordExtraRngDraw(); // 计数器+1
    }

    sink.recordPoolTelemetry(poolSize, selectedIndex, selectedIndex == 0);
    return selected;
  }
}
```
