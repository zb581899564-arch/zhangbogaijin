package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * FC-6A-POST / Build-C2 纯观察诊断审计（BEHAVIOUR-NEUTRAL）。
 *
 * <p>为回答"为什么同样的边界保护，seed22/23 改善整体 Pareto 而 seed24 HV/IGD 明显退化"
 * 提供逐轮 counterfactual、按角色 rescue 统计、rescue 事件详情、per-cycle population/
 * archive 几何、rescued 解的 Qg 教师/CFVF 曝光与 lineage 后代追踪。
 *
 * <p>本类与 {@link V35Fc52LifecycleAudit} 相同模式：默认关闭、静态 enable 门，
 * 所有公开方法第一行 {@code if (this != current) return;}。不得修改任何算法状态、
 * 不得消费随机流、不得抛出会中断算法的异常。算法侧插入的调用点全部只读。
 *
 * <p>口径（在 FC6A_BUILD_C_STABILITY_DIAGNOSTIC_REPORT.md 中预注册）：
 * <ul>
 *   <li>pool 序 == originalOrder 升序（与 select 内部 values 序一致）。
 *   <li>q == dominatedBy（被池内其它候选严格支配的个数），p == dominates（严格支配
 *       其它候选的个数），score = q + 1/(p+1)（公式与 selector authorScores 逐位一致）。
 *   <li>S_original = 按 (score 升序, originalOrder 升序) 取前 targetSize 个；
 *       S_BP = 实际 selected（=下一轮 population），按 selected 顺序。
 *   <li>RESCUED = S_BP − S_original（按 fingerprint 集合差），DISPLACED = S_original − S_BP；
 *       配对按两者各自的升序（rescue 按 S_BP 槽位、displaced 按 S_original 排名）；
 *       池内重复 fingerprint 的极端情形下两者规模可能不等，此时按 min 配对，余下记录
 *       空 displaced（报告注明该约定）。
 *   <li>role：解是 q==0 池内 minCmax / minTEC / minTWC 字典序胜者之一；多角色记为
 *       MULTI_ROLE（只占一个 population slot，但计入其命中的每个角色计数）。
 *   <li>时间线：救回事件记于其 PDDR 轮所在 cycle c；几何快照 label c+1（L774 递增后）；
 *       Qg/CFVF 曝光事件记于其发生轮 cycle（救回解最早的曝光机会在其救回后的下一轮，
 *       即 cycle c+1 与几何 c+1 对齐）。一切按算法侧传入的 {@code formalBaselineOuterCycles}
 *       对齐，输出原样保留。
 *   <li>lineage 后代：rescue 的 lineage = 重建后下一轮 population 成员携带的新标签；
 *       成员快照逐 cycle 记录 (lineageId, Cmax, TEC, TWC)，lineage 父子表按 seen 的
 *       标签累积；后代 = 某成员沿 parent 链回溯命中任一 rescue lineage（含直系）。
 *       descendantPresence = 跨全部快照的出现次数；uniqueDescendantLineages = 命中的
 *       不同 lineageId 数；directLineages = 直系 lineageId 数；success = 命中且
 *       Cmax ≤ rescue Cmax 的快照数；nondomFinal = 命中且目标点出现在最终 front 的成员数。
 * </ul>
 */
public final class V35Fc6BpPddrDiagnosticAudit {
  private static final double EPS = 1e-12;
  private static final String TAB = "\t";
  private static final String LIST_SEP = ";";
  private static final int ROLE_CMAX = 1;
  private static final int ROLE_TEC = 2;
  private static final int ROLE_TWC = 4;

  private static volatile boolean enabled = false;
  private static V35Fc6BpPddrDiagnosticAudit current;

  private long seed = -1L;
  private long rounds;
  private long boundaryPoolTotal;
  private long multiRoleBoundaryCount;
  private final long[] boundaryCandidateByRole = new long[3];
  private long rescuedTotal;
  private final long[] rescuedByRole = new long[3];
  private long displacedTotal;
  private final long[] cumulativeRescueByRole = new long[3];
  private final TreeMap<Integer, int[]> cycleRescueCounts = new TreeMap<>();

  private final List<RoundRecord> roundRecords = new ArrayList<>();
  private final List<RescueEvent> rescueEvents = new ArrayList<>();
  private final LinkedHashMap<String, RescueExposure> exposureByFingerprint = new LinkedHashMap<>();
  private final Set<String> rescueFingerprints = new HashSet<>();
  private final LinkedHashMap<String, Long> rescueLineageByFingerprint = new LinkedHashMap<>();
  private final HashSet<Long> rescueLineages = new HashSet<>();
  private final HashMap<Long, Long> lineageParent = new HashMap<>();
  private final TreeMap<Integer, CycleGeometry> cycleGeometry = new TreeMap<>();
  private final TreeMap<Integer, int[]> cycleTeacherSel = new TreeMap<>();
  private final TreeMap<Integer, int[]> cycleCfvfGbest = new TreeMap<>();
  private final TreeMap<Integer, int[]> cycleCfvfPbest = new TreeMap<>();
  private final TreeMap<Integer, List<MemberSnapshot>> cycleMembers = new TreeMap<>();
  private final List<double[]> finalFront = new ArrayList<>();
  private final List<CompRecord> compRecords = new ArrayList<>();
  private long compRoundsLt1GtTarget;
  private long compRoundsNdGtTarget;
  // FC-6A.2 Region × PDDR 审计（纯观察）：镜像 updateVelocity 的贪心区域分配
  // （G1=Cmax×15、G2=综合 score×55、G3=TEC×15、G4=TWC×15，容量来自 builder 默认）。
  private final List<RegionRecord> regionRecords = new ArrayList<>();
  private final List<ProbeRecord> probeRecords = new ArrayList<>();
  private long probeGlobalYes;
  private long probeRegionYes;
  private long probeRegionG1;
  // 174.44 反事实探针目标三元组（20-job seed22 best-ever，FC-5.2 record 655）。
  // 仅当 setRegionProbe 配置后生效；纯读比较，不进任何决策路径。
  private double[] probeObjectives;

  /** FC-6A.2：配置反事实探针三元组 {Cmax, TEC, TWC}（runner 按实例+seed 决定是否启用）。 */
  public static void setRegionProbe(double cmax, double tec, double twc) {
    if (current != null) {
      current.probeObjectives = new double[] {cmax, tec, twc};
    }
  }

  private V35Fc6BpPddrDiagnosticAudit() { }

  public static void setEnabled(boolean value) {
    enabled = value;
    current = value ? new V35Fc6BpPddrDiagnosticAudit() : null;
  }

  public static boolean isEnabled() {
    return enabled;
  }

  public static V35Fc6BpPddrDiagnosticAudit current() {
    return current;
  }

  public static void reset() {
    if (enabled) {
      current = new V35Fc6BpPddrDiagnosticAudit();
    }
  }

  /** runner 调用：把 seed 写入输出（纯元数据，不参与计算）。 */
  public static void setSeed(long value) {
    if (current != null) {
      current.seed = value;
    }
  }

  // ---------------------------------------------------------------------
  // PDDR counterfactual + rescue events（算法侧：applyEvaluatedPddr，rebuild 之后）
  // ---------------------------------------------------------------------

  /**
   * @param pool           合并池（序 == originalOrder 升序，与 select 内部 values 一致）
   * @param selected       BP-PDDR 实际选中集（长度 == targetSize）
   * @param nextPopulation 重建打标后的下一轮 population（与 selected 同序，成员带新 lineage 标签）
   * @param fe             当前全量评估计数
   * @param cycle          formalBaselineOuterCycles（本轮所在 cycle）
   */
  public void recordPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      List<PermutationSolution<Integer>> nextPopulation, long fe, int cycle) {
    if (this != current) {
      return;
    }
    rounds++;
    int targetSize = selected.size();
    double[] scores = pddrScores(pool);

    // FC-6A.1 组成审计：按 score 对全池分类（<1 中心强势 / =1 边界非支配 / >1 被支配），
    // 并统计 S_original（=原始 PDDR 会选的集合，(score,池序) 前 targetSize）与
    // S_BP（实际存活集）的构成。纯观察，不进决策。
    int poolLt1 = 0;
    int poolEq1 = 0;
    int poolGt1 = 0;
    for (double score : scores) {
      if (score < 1.0) {
        poolLt1++;
      } else if (score == 1.0) {
        poolEq1++;
      } else {
        poolGt1++;
      }
    }
    if (poolLt1 > targetSize) {
      compRoundsLt1GtTarget++;
    }
    if (poolLt1 + poolEq1 > targetSize) {
      compRoundsNdGtTarget++;
    }
    int origLt1 = 0;
    int origEq1 = 0;
    int origGt1 = 0;
    List<Integer> compOrder = new ArrayList<>(pool.size());
    for (int i = 0; i < pool.size(); i++) {
      compOrder.add(i);
    }
    Collections.sort(compOrder, (a, b) -> {
      int byScore = Double.compare(scores[a], scores[b]);
      return byScore != 0 ? byScore : Integer.compare(a, b);
    });
    for (int i = 0; i < targetSize && i < compOrder.size(); i++) {
      double score = scores[compOrder.get(i)];
      if (score < 1.0) {
        origLt1++;
      } else if (score == 1.0) {
        origEq1++;
      } else {
        origGt1++;
      }
    }
    int selLt1 = origLt1;
    int selEq1 = origEq1;
    int selGt1 = origGt1;
    // S_BP 真实构成：从 nextPopulation 成员回查池内 score。
    int bpLt1 = 0;
    int bpEq1 = 0;
    int bpGt1 = 0;
    Map<String, Double> scoreByFingerprint = new HashMap<>();
    for (int i = 0; i < pool.size(); i++) {
      scoreByFingerprint.putIfAbsent(
          ZhangBoQgController.fingerprint(pool.get(i)), scores[i]);
    }
    for (PermutationSolution<Integer> member : nextPopulation) {
      Double score = scoreByFingerprint.get(ZhangBoQgController.fingerprint(member));
      if (score == null) {
        continue;
      }
      if (score < 1.0) {
        bpLt1++;
      } else if (score == 1.0) {
        bpEq1++;
      } else {
        bpGt1++;
      }
    }
    compRecords.add(new CompRecord(cycle, fe, pool.size(), targetSize,
        poolLt1, poolEq1, poolGt1, origLt1, origEq1, origGt1,
        bpLt1, bpEq1, bpGt1));

    // FC-6A.2 Region×PDDR 审计：镜像 updateVelocity 贪心分配（纯观察）。
    recordRegionRound(pool, scores, targetSize, cycle, fe);

    // FC-6A.2 174.44 反事实探针（仅当 setRegionProbe 配置时活跃）。
    recordProbeRound(pool, scores, targetSize, cycle, fe);

    // 与 selector.boundaryReservedIndices 同源的保留角色判定：每个目标在 q==0 内的字典序胜者。
    int[] roleWinners = {-1, -1, -1};
    int[] objectives = {0, 1, 6};
    for (int slot = 0; slot < 3; slot++) {
      roleWinners[slot] = winnerIndex(pool, scores, objectives[slot]);
    }
    for (int index : roleWinners) {
      if (index < 0) {
        continue;
      }
      int bits = rolesOf(roleWinners, index);
      boundaryPoolTotal++;
      boundaryCandidateByRole[objectiveOfRoleBits(bits)]++;
      if (Integer.bitCount(bits) > 1) {
        multiRoleBoundaryCount++;
      }
    }

    // S_original：按 (score, 池序) 排序取前 targetSize；池序==originalOrder 升序。
    List<Integer> order = new ArrayList<>(pool.size());
    for (int i = 0; i < pool.size(); i++) {
      order.add(i);
    }
    Collections.sort(order, (a, b) -> {
      int byScore = Double.compare(scores[a], scores[b]);
      return byScore != 0 ? byScore : Integer.compare(a, b);
    });
    Set<String> originalSelected = new HashSet<>(targetSize * 2);
    List<String> sOriginalFingerprints = new ArrayList<>(targetSize);
    for (int i = 0; i < targetSize; i++) {
      String fingerprint = ZhangBoQgController.fingerprint(pool.get(order.get(i)));
      originalSelected.add(fingerprint);
      sOriginalFingerprints.add(fingerprint);
    }

    // S_BP：nextPopulation 与 selected 同序。
    List<String> sBpFingerprints = new ArrayList<>(nextPopulation.size());
    Set<String> bpSelected = new HashSet<>(nextPopulation.size() * 2);
    Map<String, Long> newLineageByFingerprint = new HashMap<>();
    for (PermutationSolution<Integer> member : nextPopulation) {
      String fingerprint = ZhangBoQgController.fingerprint(member);
      bpSelected.add(fingerprint);
      sBpFingerprints.add(fingerprint);
      Object tag = member.getAttribute(ZhangBoLineageTag.class);
      if (tag instanceof ZhangBoLineageTag) {
        newLineageByFingerprint.put(fingerprint, ((ZhangBoLineageTag) tag).getLineageId());
      }
    }

    List<String> rescuedList = new ArrayList<>();
    for (String fingerprint : sBpFingerprints) {
      if (!originalSelected.contains(fingerprint)) {
        rescuedList.add(fingerprint);
      }
    }
    List<String> displacedList = new ArrayList<>();
    for (String fingerprint : sOriginalFingerprints) {
      if (!bpSelected.contains(fingerprint)) {
        displacedList.add(fingerprint);
      }
    }
    rescuedTotal += rescuedList.size();
    displacedTotal += displacedList.size();

    // 池内每个 fingerprint 的首现 index（pool 可能有重复 fingerprint，取首次）。
    Map<String, Integer> firstIndexByFingerprint = new HashMap<>();
    for (int i = 0; i < pool.size(); i++) {
      String fingerprint = ZhangBoQgController.fingerprint(pool.get(i));
      firstIndexByFingerprint.putIfAbsent(fingerprint, i);
    }
    // 每个 S_BP 成员的槽位。
    Map<String, Integer> slotByFingerprint = new HashMap<>();
    for (int i = 0; i < sBpFingerprints.size(); i++) {
      slotByFingerprint.putIfAbsent(sBpFingerprints.get(i), i);
    }
    // 每个 rescue 的 original rank（在完整 (score, 池序) 排序中的位置）。
    Map<String, Integer> rankByFingerprint = new HashMap<>();
    for (int i = 0; i < order.size(); i++) {
      String fingerprint = ZhangBoQgController.fingerprint(pool.get(order.get(i)));
      rankByFingerprint.putIfAbsent(fingerprint, i);
    }

    int pairCount = Math.min(rescuedList.size(), displacedList.size());
    int[] cycleBucket = cycleRescueCounts.computeIfAbsent(cycle, c -> new int[5]);
    cycleBucket[3] += rescuedList.size();
    cycleBucket[4] += displacedList.size();
    for (int i = 0; i < rescuedList.size(); i++) {
      String fingerprint = rescuedList.get(i);
      Integer poolIndex = firstIndexByFingerprint.get(fingerprint);
      int bits = poolIndex == null ? 0 : rolesOf(roleWinners, poolIndex);
      if (poolIndex != null) {
        for (int role = 0; role < 3; role++) {
          if ((bits & (1 << role)) != 0) {
            rescuedByRole[role]++;
            cumulativeRescueByRole[role]++;
            cycleBucket[role]++;
          }
        }
      }
      String displacedFp = i < pairCount ? displacedList.get(i) : "";
      Integer displacedIndex = displacedFp.isEmpty() ? null
          : firstIndexByFingerprint.get(displacedFp);
      RescueEvent event = poolIndex == null
          ? new RescueEvent(cycle, fe, roleName(bits), fingerprint, -1,
              slotByFingerprint.getOrDefault(fingerprint, -1), -1, Double.NaN, 0, 0,
              displacedFp, -1, -1, Double.NaN, 0, 0, Double.NaN, Double.NaN, Double.NaN,
              Double.NaN, Double.NaN, Double.NaN, -1L)
          : new RescueEvent(cycle, fe, roleName(bits), fingerprint, poolIndex,
              slotByFingerprint.getOrDefault(fingerprint, -1),
              rankByFingerprint.getOrDefault(fingerprint, -1), scores[poolIndex],
              countQ(pool, poolIndex), countP(pool, poolIndex),
              displacedFp,
              displacedIndex == null ? -1 : displacedIndex,
              displacedFp.isEmpty() ? -1
                  : rankByFingerprint.getOrDefault(displacedFp, -1),
              displacedFp.isEmpty() ? Double.NaN : scores[displacedIndex],
              displacedFp.isEmpty() ? 0 : countQ(pool, displacedIndex),
              displacedFp.isEmpty() ? 0 : countP(pool, displacedIndex),
              pool.get(poolIndex).getObjective(0), pool.get(poolIndex).getObjective(1),
              pool.get(poolIndex).getObjective(6),
              displacedFp.isEmpty() ? Double.NaN : pool.get(displacedIndex).getObjective(0),
              displacedFp.isEmpty() ? Double.NaN : pool.get(displacedIndex).getObjective(1),
              displacedFp.isEmpty() ? Double.NaN : pool.get(displacedIndex).getObjective(6),
              newLineageByFingerprint.getOrDefault(fingerprint, -1L));
      rescueEvents.add(event);

      rescueFingerprints.add(fingerprint);
      exposureByFingerprint.putIfAbsent(fingerprint, new RescueExposure(fingerprint));
      Long lineage = newLineageByFingerprint.get(fingerprint);
      if (lineage != null) {
        rescueLineageByFingerprint.put(fingerprint, lineage);
        rescueLineages.add(lineage);
      }
    }

    roundRecords.add(new RoundRecord(cycle, fe, pool.size(), targetSize,
        sOriginalFingerprints, sBpFingerprints, rescuedList.size()));
  }

  // ---------------------------------------------------------------------
  // Qg / CFVF 曝光（算法侧：selectQgLeader / updateCfvfGroup）
  // ---------------------------------------------------------------------

  public void observeQgTeacher(ZhangBoSubSwarm group,
      PermutationSolution<Integer> leader, long fe, int cycle) {
    if (this != current) {
      return;
    }
    String fingerprint = ZhangBoQgController.fingerprint(leader);
    RescueExposure exposure = exposureByFingerprint.get(fingerprint);
    if (exposure == null) {
      return;
    }
    int groupIndex = groupIndex(group);
    exposure.qgTeacherCount++;
    exposure.qgByGroup[groupIndex]++;
    cycleTeacherSel.computeIfAbsent(cycle, c -> new int[4])[groupIndex]++;
  }

  public void observeCfvfLearning(ZhangBoSubSwarm group,
      PermutationSolution<Integer> leader, PermutationSolution<Integer> personalLeader,
      long gbestInherited, long pbestInherited, long fe, int cycle) {
    if (this != current) {
      return;
    }
    int groupIndex = groupIndex(group);
    String leaderFp = ZhangBoQgController.fingerprint(leader);
    if (gbestInherited > 0) {
      RescueExposure exposure = exposureByFingerprint.get(leaderFp);
      if (exposure != null) {
        exposure.cfvfGbestCount++;
        exposure.cfvfGbestByGroup[groupIndex]++;
        cycleCfvfGbest.computeIfAbsent(cycle, c -> new int[4])[groupIndex]++;
      }
    }
    String pbestFp = ZhangBoQgController.fingerprint(personalLeader);
    if (pbestInherited > 0) {
      RescueExposure exposure = exposureByFingerprint.get(pbestFp);
      if (exposure != null) {
        exposure.cfvfPbestCount++;
        exposure.cfvfPbestByGroup[groupIndex]++;
        cycleCfvfPbest.computeIfAbsent(cycle, c -> new int[4])[groupIndex]++;
      }
    }
  }

  // ---------------------------------------------------------------------
  // Per-cycle 几何 + lineage 快照（算法侧：外层循环尾，formalBaselineOuterCycles++ 之后）
  // ---------------------------------------------------------------------

  public void observeCycle(int cycle, List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> archive, long fe) {
    if (this != current) {
      return;
    }
    CycleGeometry geometry = new CycleGeometry(cycle, fe,
        population.size(), nondominatedSize(population), archive.size());
    fillRange(geometry.popMin, geometry.popMax, population);
    fillRange(geometry.archMin, geometry.archMax, archive);
    for (int i = 0; i < 3; i++) {
      geometry.popRange[i] = geometry.popMax[i] - geometry.popMin[i];
      geometry.archRange[i] = geometry.archMax[i] - geometry.archMin[i];
    }
    // lineage 父子表 + 成员快照
    List<MemberSnapshot> members = new ArrayList<>(population.size());
    for (PermutationSolution<Integer> member : population) {
      long lineageId = -1L;
      Object tag = member.getAttribute(ZhangBoLineageTag.class);
      if (tag instanceof ZhangBoLineageTag) {
        lineageId = ((ZhangBoLineageTag) tag).getLineageId();
        lineageParent.put(lineageId, ((ZhangBoLineageTag) tag).getParentLineageId());
      }
      members.add(new MemberSnapshot(lineageId, member.getObjective(0),
          member.getObjective(1), member.getObjective(6)));
    }
    cycleMembers.put(cycle, members);
    cycleGeometry.put(cycle, geometry);
  }

  /** 运行结束：注入最终 front（raw 目标空间点），用于 per-rescue 终局判定。 */
  public void finish(List<double[]> finalFront) {
    if (this != current) {
      return;
    }
    this.finalFront.clear();
    if (finalFront != null) {
      for (double[] point : finalFront) {
        this.finalFront.add(new double[]{point[0], point[1], point[2]});
      }
    }
    // 逐 rescue 统计后代（沿累积 lineage 父子表回溯）。
    for (String fingerprint : exposureByFingerprint.keySet()) {
      RescueExposure exposure = exposureByFingerprint.get(fingerprint);
      Long rootLineage = rescueLineageByFingerprint.get(fingerprint);
      if (rootLineage == null) {
        continue;
      }
      double rescueCmax = rescueCmaxOf(fingerprint);
      Set<Long> descendantLineages = new HashSet<>();
      Set<Long> directLineages = new HashSet<>();
      for (Map.Entry<Integer, List<MemberSnapshot>> entry : cycleMembers.entrySet()) {
        for (MemberSnapshot member : entry.getValue()) {
          boolean hit = false;
          boolean direct = false;
          long cursor = member.lineageId;
          Set<Long> visited = new HashSet<>();
          while (cursor >= 0L && !visited.contains(cursor)) {
            visited.add(cursor);
            if (cursor == rootLineage) {
              hit = true;
              direct = true;
              break;
            }
            if (rescueLineages.contains(cursor)) {
              hit = true;
              break;
            }
            Long parent = lineageParent.get(cursor);
            cursor = parent == null ? -1L : parent;
          }
          if (!hit) {
            continue;
          }
          exposure.descendantPresence++;
          exposure.directPresence += direct ? 1 : 0;
          if (direct) {
            directLineages.add(member.lineageId);
          } else {
            descendantLineages.add(member.lineageId);
          }
          if (!Double.isNaN(rescueCmax) && member.cmax <= rescueCmax + EPS) {
            exposure.successPresence++;
          }
          if (onFinalFront(member)) {
            exposure.nondomFinal++;
          }
        }
      }
      exposure.uniqueDescendantLineages = directLineages.size() + descendantLineages.size();
      exposure.directLineages = directLineages.size();
    }
  }

  private double rescueCmaxOf(String fingerprint) {
    for (RescueEvent event : rescueEvents) {
      if (event.fingerprint.equals(fingerprint)) {
        return event.cmax;
      }
    }
    return Double.NaN;
  }

  private boolean onFinalFront(MemberSnapshot member) {
    for (double[] point : finalFront) {
      if (Math.abs(point[0] - member.cmax) <= 1e-9
          && Math.abs(point[1] - member.tec) <= 1e-9
          && Math.abs(point[2] - member.twc) <= 1e-9) {
        return true;
      }
    }
    return false;
  }

  // ---------------------------------------------------------------------
  // 输出
  // ---------------------------------------------------------------------

  public String fc6DiagText() {
    StringBuilder out = new StringBuilder();
    out.append("fc6DiagBegin\n");
    out.append("fc6diagSeed=").append(seed).append('\n');
    out.append("fc6diagRounds=").append(rounds).append('\n');
    out.append("fc6diagBoundaryPool=").append(boundaryPoolTotal)
        .append(" (cmax=").append(boundaryCandidateByRole[0])
        .append(",tec=").append(boundaryCandidateByRole[1])
        .append(",twc=").append(boundaryCandidateByRole[2])
        .append(",multiRole=").append(multiRoleBoundaryCount).append(")\n");
    out.append("fc6diagActualRescues=").append(rescuedTotal)
        .append(" (cmax=").append(rescuedByRole[0])
        .append(",tec=").append(rescuedByRole[1])
        .append(",twc=").append(rescuedByRole[2]).append(")\n");
    out.append("fc6diagDisplacements=").append(displacedTotal).append('\n');
    out.append("fc6diagCumulativeRescues=cmax=").append(cumulativeRescueByRole[0])
        .append(",tec=").append(cumulativeRescueByRole[1])
        .append(",twc=").append(cumulativeRescueByRole[2]).append('\n');

    out.append("fc6diagCycleBegin\n");
    for (Map.Entry<Integer, CycleGeometry> entry : cycleGeometry.entrySet()) {
      CycleGeometry g = entry.getValue();
      int[] rescue = cycleRescueCounts.getOrDefault(entry.getKey(), new int[5]);
      int[] teacher = cycleTeacherSel.getOrDefault(entry.getKey(), new int[4]);
      int[] cfvf = cycleCfvfGbest.getOrDefault(entry.getKey(), new int[4]);
      out.append("fc6diagCycle ").append(entry.getKey())
          .append(TAB).append("fe=").append(g.fe)
          .append(TAB).append("popSize=").append(g.populationSize)
          .append(TAB).append("popND=").append(g.populationNDSize)
          .append(TAB).append("archSize=").append(g.archiveSize)
          .append(TAB).append("popRanges=cmax[").append(fmt(g.popMin[0])).append(',')
          .append(fmt(g.popMax[0])).append(',').append(fmt(g.popRange[0])).append("],tec[")
          .append(fmt(g.popMin[1])).append(',').append(fmt(g.popMax[1])).append(',')
          .append(fmt(g.popRange[1])).append("],twc[").append(fmt(g.popMin[2])).append(',')
          .append(fmt(g.popMax[2])).append(',').append(fmt(g.popRange[2])).append(']')
          .append(TAB).append("archRanges=cmax[").append(fmt(g.archMin[0])).append(',')
          .append(fmt(g.archMax[0])).append(',').append(fmt(g.archRange[0])).append("],tec[")
          .append(fmt(g.archMin[1])).append(',').append(fmt(g.archMax[1])).append(',')
          .append(fmt(g.archRange[1])).append("],twc[").append(fmt(g.archMin[2])).append(',')
          .append(fmt(g.archMax[2])).append(',').append(fmt(g.archRange[2])).append(']')
          .append(TAB).append("rescues=").append(rescue[3])
          .append("(cmax=").append(rescue[0]).append(",tec=").append(rescue[1])
          .append(",twc=").append(rescue[2]).append(")")
          .append(TAB).append("displaced=").append(rescue[4])
          .append(TAB).append("teacherSel=").append(sum(teacher))
          .append(TAB).append("cfvfGbestLearn=").append(sum(cfvf))
          .append('\n');
    }
    out.append("fc6diagCycleEnd\n");

    out.append("fc6diagRoundBegin\n");
    for (RoundRecord round : roundRecords) {
      out.append("fc6diagRound ").append(round.cycle)
          .append(TAB).append("fe=").append(round.fe)
          .append(TAB).append("pool=").append(round.poolSize)
          .append(TAB).append("target=").append(round.targetSize)
          .append(TAB).append("rescued=").append(round.rescuedCount)
          .append(TAB).append("sOriginal=").append(join(round.sOriginalFingerprints))
          .append(TAB).append("sBp=").append(join(round.sBpFingerprints))
          .append('\n');
    }
    out.append("fc6diagRoundEnd\n");

    out.append("fc6diagRescueBegin\n");
    int id = 0;
    for (RescueEvent event : rescueEvents) {
      out.append("fc6diagRescue ").append(id++)
          .append(TAB).append("cycle=").append(event.cycle)
          .append(TAB).append("fe=").append(event.fe)
          .append(TAB).append("role=").append(event.role)
          .append(TAB).append("fp=").append(event.fingerprint)
          .append(TAB).append("Cmax=").append(fmt(event.cmax))
          .append(TAB).append("TEC=").append(fmt(event.tec))
          .append(TAB).append("TWC=").append(fmt(event.twc))
          .append(TAB).append("q=").append(event.q)
          .append(TAB).append("p=").append(event.p)
          .append(TAB).append("score=").append(fmt(event.score))
          .append(TAB).append("origRank=").append(event.originalPddrRank)
          .append(TAB).append("slot=").append(event.slot)
          .append(TAB).append("lineage=").append(event.lineage)
          .append(TAB).append("dFp=").append(event.displacedFingerprint)
          .append(TAB).append("dCmax=").append(fmt(event.displacedCmax))
          .append(TAB).append("dTEC=").append(fmt(event.displacedTec))
          .append(TAB).append("dTWC=").append(fmt(event.displacedTwc))
          .append(TAB).append("dq=").append(event.displacedQ)
          .append(TAB).append("dp=").append(event.displacedP)
          .append(TAB).append("dScore=").append(fmt(event.displacedScore))
          .append(TAB).append("dOrigRank=").append(event.displacedOriginalRank)
          .append('\n');
    }
    out.append("fc6diagRescueEnd\n");

    out.append("fc6diagExposureBegin\n");
    for (Map.Entry<String, RescueExposure> entry : exposureByFingerprint.entrySet()) {
      RescueExposure exposure = entry.getValue();
      out.append("fc6diagExposure ").append(entry.getKey())
          .append(TAB).append("qg=").append(exposure.qgTeacherCount)
          .append("(G1=").append(exposure.qgByGroup[0])
          .append(",G2=").append(exposure.qgByGroup[1])
          .append(",G3=").append(exposure.qgByGroup[2])
          .append(",G4=").append(exposure.qgByGroup[3]).append(")")
          .append(TAB).append("cfvfGbest=").append(exposure.cfvfGbestCount)
          .append("(G1=").append(exposure.cfvfGbestByGroup[0])
          .append(",G2=").append(exposure.cfvfGbestByGroup[1])
          .append(",G3=").append(exposure.cfvfGbestByGroup[2])
          .append(",G4=").append(exposure.cfvfGbestByGroup[3]).append(")")
          .append(TAB).append("cfvfPbest=").append(exposure.cfvfPbestCount)
          .append(TAB).append("descPresence=").append(exposure.descendantPresence)
          .append(TAB).append("directPresence=").append(exposure.directPresence)
          .append(TAB).append("uniqueDescLineages=").append(exposure.uniqueDescendantLineages)
          .append(TAB).append("directLineages=").append(exposure.directLineages)
          .append(TAB).append("successPresence=").append(exposure.successPresence)
          .append(TAB).append("nondomFinal=").append(exposure.nondomFinal)
          .append('\n');
    }
    out.append("fc6diagExposureEnd\n");
    out.append("fc6diagCompBegin\n");
    for (CompRecord comp : compRecords) {
      out.append("fc6diagComp ").append(comp.cycle)
          .append(TAB).append("fe=").append(comp.fe)
          .append(TAB).append("pool=").append(comp.poolSize)
          .append(TAB).append("target=").append(comp.targetSize)
          .append(TAB).append("nLT1=").append(comp.poolLt1)
          .append(TAB).append("nEq1=").append(comp.poolEq1)
          .append(TAB).append("nGt1=").append(comp.poolGt1)
          .append(TAB).append("nND=").append(comp.poolLt1 + comp.poolEq1)
          .append(TAB).append("selLT1=").append(comp.selLt1)
          .append(TAB).append("selEq1=").append(comp.selEq1)
          .append(TAB).append("selGt1=").append(comp.selGt1)
          .append(TAB).append("rejLT1=").append(Math.max(0, comp.poolLt1 - comp.selLt1))
          .append(TAB).append("rejEq1=").append(Math.max(0, comp.poolEq1 - comp.selEq1))
          .append(TAB).append("rejGt1=").append(Math.max(0, comp.poolGt1 - comp.selGt1))
          .append(TAB).append("bpLT1=").append(comp.bpLt1)
          .append(TAB).append("bpEq1=").append(comp.bpEq1)
          .append(TAB).append("bpGt1=").append(comp.bpGt1)
          .append('\n');
    }
    out.append("fc6diagCompEnd\n");
    out.append("fc6diagCompSummary rounds=").append(compRecords.size())
        .append(" targetLt1OnlyRounds=").append(compRoundsLt1GtTarget)
        .append(" targetNdGtRounds=").append(compRoundsNdGtTarget).append('\n');
    // FC-6A.2 Region×PDDR 审计输出（纯观察，向后兼容：解析器按行前缀取用）。
    out.append("fc6diagRegionBegin\n");
    for (RegionRecord region : regionRecords) {
      out.append("fc6diagRegion ").append(region.cycle)
          .append(TAB).append("fe=").append(region.fe)
          .append(TAB).append("pool=").append(region.poolSize)
          .append(TAB).append("target=").append(region.targetSize)
          .append(TAB).append("g1Lt1=").append(region.g1Lt1)
          .append(TAB).append("g1Eq1=").append(region.g1Eq1)
          .append(TAB).append("g2Lt1=").append(region.g2Lt1)
          .append(TAB).append("g2Eq1=").append(region.g2Eq1)
          .append(TAB).append("g3Lt1=").append(region.g3Lt1)
          .append(TAB).append("g3Eq1=").append(region.g3Eq1)
          .append(TAB).append("g4Lt1=").append(region.g4Lt1)
          .append(TAB).append("g4Eq1=").append(region.g4Eq1)
          .append(TAB).append("ovfLt1=").append(region.ovfLt1)
          .append(TAB).append("ovfEq1=").append(region.ovfEq1)
          .append(TAB).append("rejG1=").append(region.rejG1)
          .append(TAB).append("rejG2=").append(region.rejG2)
          .append(TAB).append("rejG3=").append(region.rejG3)
          .append(TAB).append("rejG4=").append(region.rejG4)
          .append(TAB).append("rejOvf=").append(region.rejOvf)
          .append(TAB).append("absorbable=").append(region.absorbable)
          .append('\n');
    }
    out.append("fc6diagRegionEnd\n");
    long totalRejNd = 0L;
    long totalAbsorbable = 0L;
    long totalOvf = 0L;
    long ovfRounds = 0L;
    for (RegionRecord region : regionRecords) {
      long rejNd = region.rejG1 + region.rejG2 + region.rejG3 + region.rejG4
          + region.rejOvf;
      totalRejNd += rejNd;
      totalAbsorbable += region.absorbable;
      totalOvf += region.rejOvf;
      if (region.ovfLt1 + region.ovfEq1 > 0) {
        ovfRounds++;
      }
    }
    out.append("fc6diagRegionSummary rounds=").append(regionRecords.size())
        .append(" ovfRounds=").append(ovfRounds)
        .append(" totalRejNd=").append(totalRejNd)
        .append(" totalAbsorbable=").append(totalAbsorbable)
        .append(" totalOvf=").append(totalOvf).append('\n');
    if (probeObjectives != null && !probeRecords.isEmpty()) {
      out.append("fc6diagProbeBegin\n");
      for (ProbeRecord probe : probeRecords) {
        out.append("fc6diagProbe ").append(probe.cycle)
            .append(TAB).append("fe=").append(probe.fe)
            .append(TAB).append("global=").append(probe.global)
            .append(TAB).append("region=").append(probe.region)
            .append('\n');
      }
      out.append("fc6diagProbeEnd\n");
      out.append("fc6diagProbeSummary rounds=").append(probeRecords.size())
          .append(" globalYes=").append(probeGlobalYes)
          .append(" regionYes=").append(probeRegionYes)
          .append(" regionG1=").append(probeRegionG1).append('\n');
    }
    out.append("fc6DiagEnd\n");
    return out.toString();
  }

  // ---------------------------------------------------------------------
  // 私有工具
  // ---------------------------------------------------------------------

  // ---------------------------------------------------------------------
  // FC-6A.2 Region×PDDR 审计（纯观察）
  // ---------------------------------------------------------------------

  /** 区域容量：G1=Cmax×15、G2=综合×55、G3=TEC×15、G4=TWC×15（builder 默认 15/55/15/15）。 */
  private static final int[] REGION_CAPACITY = {15, 55, 15, 15};

  /**
   * 镜像 updateVelocity 的贪心区域分配（对全池 q=0 候选）：
   * G1 按 (Cmax,TEC,TWC,池序) 字典序取前 15；G2 从剩余按 (score,池序) 取前 55；
   * G3 按 (TEC,Cmax,TWC,池序) 取前 15；G4 按 (TWC,Cmax,TEC,池序) 取前 15；
   * 剩余 q=0 解为溢出（ovf）。
   *
   * @return regionOf[i] ∈ {0,1,2,3,4}：0..3 = G1..G4 归属，4 = 溢出，-1 = q>0（被支配）。
   */
  private static int[] greedyRegionAssign(List<PermutationSolution<Integer>> pool,
      double[] scores) {
    int n = pool.size();
    List<Integer> candidates = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      if (scores[i] <= 1.0) {
        candidates.add(i);
      }
    }
    int[] regionOf = new int[n];
    java.util.Arrays.fill(regionOf, -1);
    // G1: Cmax 方向
    candidates.sort((a, b) -> {
      PermutationSolution<Integer> x = pool.get(a);
      PermutationSolution<Integer> y = pool.get(b);
      int c = Double.compare(x.getObjective(0), y.getObjective(0));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(1), y.getObjective(1));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(6), y.getObjective(6));
      return c != 0 ? c : Integer.compare(a, b);
    });
    int taken = 0;
    for (int index : candidates) {
      if (taken >= REGION_CAPACITY[0]) break;
      regionOf[index] = 0;
      taken++;
    }
    List<Integer> remaining = new ArrayList<>(candidates.size());
    for (int index : candidates) {
      if (regionOf[index] < 0) remaining.add(index);
    }
    // G2: 综合 score（updateVelocity sub2 的 aa 公式 == PDDR score）
    remaining.sort((a, b) -> {
      int c = Double.compare(scores[a], scores[b]);
      return c != 0 ? c : Integer.compare(a, b);
    });
    taken = 0;
    for (int index : remaining) {
      if (taken >= REGION_CAPACITY[1]) break;
      regionOf[index] = 1;
      taken++;
    }
    List<Integer> remaining2 = new ArrayList<>(remaining.size());
    for (int index : remaining) {
      if (regionOf[index] < 0) remaining2.add(index);
    }
    // G3: TEC 方向
    remaining2.sort((a, b) -> {
      PermutationSolution<Integer> x = pool.get(a);
      PermutationSolution<Integer> y = pool.get(b);
      int c = Double.compare(x.getObjective(1), y.getObjective(1));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(0), y.getObjective(0));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(6), y.getObjective(6));
      return c != 0 ? c : Integer.compare(a, b);
    });
    taken = 0;
    for (int index : remaining2) {
      if (taken >= REGION_CAPACITY[2]) break;
      regionOf[index] = 2;
      taken++;
    }
    List<Integer> remaining3 = new ArrayList<>(remaining2.size());
    for (int index : remaining2) {
      if (regionOf[index] < 0) remaining3.add(index);
    }
    // G4: TWC 方向
    remaining3.sort((a, b) -> {
      PermutationSolution<Integer> x = pool.get(a);
      PermutationSolution<Integer> y = pool.get(b);
      int c = Double.compare(x.getObjective(6), y.getObjective(6));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(0), y.getObjective(0));
      if (c != 0) return c;
      c = Double.compare(x.getObjective(1), y.getObjective(1));
      return c != 0 ? c : Integer.compare(a, b);
    });
    taken = 0;
    for (int index : remaining3) {
      if (taken >= REGION_CAPACITY[3]) break;
      regionOf[index] = 3;
      taken++;
    }
    for (int index : remaining3) {
      if (regionOf[index] < 0) regionOf[index] = 4;
    }
    return regionOf;
  }

  private void recordRegionRound(List<PermutationSolution<Integer>> pool,
      double[] scores, int targetSize, int cycle, long fe) {
    int[] regionOf = greedyRegionAssign(pool, scores);
    // 各区按 score 分类（<1 强非支配 / =1 孤立非支配）+ 溢出。
    int[][] regionByClass = new int[5][2];
    for (int i = 0; i < pool.size(); i++) {
      int region = regionOf[i];
      if (region < 0) {
        continue;
      }
      boolean lt1 = scores[i] < 1.0;
      regionByClass[region][lt1 ? 0 : 1]++;
    }
    // 全局 S_original 被拒的 q=0 解：其区域归属 + 吸收可行性
    // （非溢出 = 区域分配下有席位，即 absorbable）。
    List<Integer> order = new ArrayList<>(pool.size());
    for (int i = 0; i < pool.size(); i++) {
      order.add(i);
    }
    Collections.sort(order, (a, b) -> {
      int byScore = Double.compare(scores[a], scores[b]);
      return byScore != 0 ? byScore : Integer.compare(a, b);
    });
    int[] rejByRegion = new int[5];
    int absorbable = 0;
    for (int rank = targetSize; rank < order.size(); rank++) {
      int index = order.get(rank);
      if (scores[index] > 1.0) {
        continue;
      }
      int region = regionOf[index];
      if (region < 0) {
        continue;
      }
      rejByRegion[region]++;
      if (region != 4) {
        absorbable++;
      }
    }
    regionRecords.add(new RegionRecord(cycle, fe, pool.size(), targetSize,
        regionByClass[0][0], regionByClass[0][1],
        regionByClass[1][0], regionByClass[1][1],
        regionByClass[2][0], regionByClass[2][1],
        regionByClass[3][0], regionByClass[3][1],
        regionByClass[4][0], regionByClass[4][1],
        rejByRegion[0], rejByRegion[1], rejByRegion[2], rejByRegion[3], rejByRegion[4],
        absorbable));
  }

  private void recordProbeRound(List<PermutationSolution<Integer>> pool,
      double[] scores, int targetSize, int cycle, long fe) {
    if (probeObjectives == null) {
      return;
    }
    double probeCmax = probeObjectives[0];
    double probeTec = probeObjectives[1];
    double probeTwc = probeObjectives[2];
    // (a) 全局 PDDR 反事实：把探针按 (score=1, 池序最末) 插入后能否进前 targetSize。
    // score=1 解只在池内 q=0 数 < targetSize 时才有席位（FC-6A.1 已证挤压规律）。
    int ndCount = 0;
    for (double score : scores) {
      if (score <= 1.0) {
        ndCount++;
      }
    }
    boolean globalYes = ndCount < targetSize;
    // (b) 区域分配反事实：探针进入 G1 字典序排名（池内 Cmax 严格小于探针的 q=0 数）。
    int cmaxAhead = 0;
    for (int i = 0; i < pool.size(); i++) {
      if (scores[i] > 1.0) {
        continue;
      }
      double c = pool.get(i).getObjective(0);
      if (c < probeCmax - EPS
          || (Math.abs(c - probeCmax) <= EPS && pool.get(i).getObjective(1) < probeTec - EPS)
          || (Math.abs(c - probeCmax) <= EPS && Math.abs(pool.get(i).getObjective(1) - probeTec) <= EPS
              && pool.get(i).getObjective(6) < probeTwc - EPS)) {
        cmaxAhead++;
      }
    }
    String regionVerdict;
    boolean regionYes;
    if (cmaxAhead < REGION_CAPACITY[0]) {
      regionVerdict = "G1";
      regionYes = true;
    } else if (ndCount - 1 < REGION_CAPACITY[0] + REGION_CAPACITY[1]
        + REGION_CAPACITY[2] + REGION_CAPACITY[3]) {
      // 探针作为 q=0 解总计不溢出四区总容量（借用位视角的保守近似）。
      regionVerdict = "borrow";
      regionYes = true;
    } else {
      regionVerdict = "ovf";
      regionYes = false;
    }
    if (globalYes) {
      probeGlobalYes++;
    }
    if (regionYes) {
      probeRegionYes++;
    }
    if ("G1".equals(regionVerdict)) {
      probeRegionG1++;
    }
    probeRecords.add(new ProbeRecord(cycle, fe, globalYes ? "yes" : "no", regionVerdict));
  }

  private static double[] pddrScores(List<PermutationSolution<Integer>> solutions) {
    double[] result = new double[solutions.size()];
    for (int left = 0; left < solutions.size(); left++) {
      double dominates = 0.0;
      double dominatedBy = 0.0;
      for (int right = 0; right < solutions.size(); right++) {
        if (left == right) {
          continue;
        }
        PermutationSolution<Integer> x = solutions.get(left);
        PermutationSolution<Integer> y = solutions.get(right);
        if (strictlyDominates(x, y)) {
          dominates++;
        }
        if (strictlyDominates(y, x)) {
          dominatedBy++;
        }
      }
      result[left] = dominatedBy + 1.0 / (dominates + 1.0);
    }
    return result;
  }

  private static boolean strictlyDominates(
      PermutationSolution<Integer> left, PermutationSolution<Integer> right) {
    boolean noWorse = left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
    boolean strictlyBetter = left.getObjective(0) < right.getObjective(0)
        || left.getObjective(1) < right.getObjective(1)
        || left.getObjective(6) < right.getObjective(6);
    return noWorse && strictlyBetter;
  }

  private int winnerIndex(List<PermutationSolution<Integer>> pool, double[] scores, int objective) {
    Integer best = null;
    for (int index = 0; index < pool.size(); index++) {
      if (scores[index] > 1.0) {
        continue;
      }
      if (best == null || lexicographicallyBetter(pool.get(index), pool.get(best),
          objective, index, best)) {
        best = index;
      }
    }
    return best == null ? -1 : best;
  }

  private static boolean lexicographicallyBetter(
      PermutationSolution<Integer> candidate, PermutationSolution<Integer> current,
      int primary, int candidateIndex, int currentIndex) {
    int[] order;
    switch (primary) {
      case 0: order = new int[]{0, 1, 6}; break;
      case 1: order = new int[]{1, 0, 6}; break;
      case 6: order = new int[]{6, 0, 1}; break;
      default: throw new IllegalArgumentException("Unsupported objective " + primary);
    }
    for (int objective : order) {
      double value = candidate.getObjective(objective);
      double other = current.getObjective(objective);
      if (value != other) {
        return value < other;
      }
    }
    return candidateIndex < currentIndex;
  }

  private int rolesOf(int[] roleWinners, int index) {
    int bits = 0;
    for (int objective = 0; objective < 3; objective++) {
      if (roleWinners[objective] == index) {
        bits |= (1 << objective);
      }
    }
    return bits;
  }

  /** 位 -> 角色下标：CMAX=1, TEC=2, TWC=4；多角色时取最低位（用于按角色入桶与输出名称）。 */
  private static int objectiveOfRoleBits(int bits) {
    if ((bits & ROLE_CMAX) != 0) {
      return 0;
    }
    if ((bits & ROLE_TEC) != 0) {
      return 1;
    }
    return 2;
  }

  private static String roleName(int bits) {
    if (Integer.bitCount(bits) > 1) {
      return "MULTI_ROLE";
    }
    switch (bits) {
      case ROLE_CMAX: return "CMAX";
      case ROLE_TEC: return "TEC";
      case ROLE_TWC: return "TWC";
      default: return "NONE";
    }
  }

  private static int countP(List<PermutationSolution<Integer>> pool, int index) {
    int count = 0;
    for (int i = 0; i < pool.size(); i++) {
      if (i != index && strictlyDominates(pool.get(index), pool.get(i))) {
        count++;
      }
    }
    return count;
  }

  private static int countQ(List<PermutationSolution<Integer>> pool, int index) {
    int count = 0;
    for (int i = 0; i < pool.size(); i++) {
      if (i != index && strictlyDominates(pool.get(i), pool.get(index))) {
        count++;
      }
    }
    return count;
  }

  private static int groupIndex(ZhangBoSubSwarm group) {
    if (group == null) {
      return 3;
    }
    switch (group.getSemanticId()) {
      case 0: return 0;  // G1_CMAX
      case 1: return 1;  // G2_TEC
      case 2: return 2;  // G3_TWC
      default: return 3; // G4_BALANCED
    }
  }

  private static int nondominatedSize(List<PermutationSolution<Integer>> population) {
    int count = 0;
    for (int i = 0; i < population.size(); i++) {
      boolean dominated = false;
      for (int j = 0; j < population.size(); j++) {
        if (i != j && strictlyDominates(population.get(j), population.get(i))) {
          dominated = true;
          break;
        }
      }
      if (!dominated) {
        count++;
      }
    }
    return count;
  }

  private static void fillRange(double[] min, double[] max,
      List<PermutationSolution<Integer>> solutions) {
    int[] objectives = {0, 1, 6};
    for (int i = 0; i < 3; i++) {
      min[i] = Double.POSITIVE_INFINITY;
      max[i] = Double.NEGATIVE_INFINITY;
    }
    for (PermutationSolution<Integer> solution : solutions) {
      for (int i = 0; i < 3; i++) {
        double value = solution.getObjective(objectives[i]);
        min[i] = Math.min(min[i], value);
        max[i] = Math.max(max[i], value);
      }
    }
    for (int i = 0; i < 3; i++) {
      if (Double.isInfinite(min[i])) {
        min[i] = 0.0;
        max[i] = 0.0;
      }
    }
  }

  private static int sum(int[] values) {
    int total = 0;
    for (int value : values) {
      total += value;
    }
    return total;
  }

  private static String join(List<String> values) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(LIST_SEP);
      }
      builder.append(values.get(i));
    }
    return builder.toString();
  }

  private static String fmt(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return String.valueOf(value);
    }
    if (value == Math.rint(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  // ---------------------------------------------------------------------
  // 数据结构
  // ---------------------------------------------------------------------

  private static final class RoundRecord {
    final int cycle;
    final long fe;
    final int poolSize;
    final int targetSize;
    final List<String> sOriginalFingerprints;
    final List<String> sBpFingerprints;
    final int rescuedCount;

    RoundRecord(int cycle, long fe, int poolSize, int targetSize,
        List<String> sOriginalFingerprints, List<String> sBpFingerprints, int rescuedCount) {
      this.cycle = cycle;
      this.fe = fe;
      this.poolSize = poolSize;
      this.targetSize = targetSize;
      this.sOriginalFingerprints = sOriginalFingerprints;
      this.sBpFingerprints = sBpFingerprints;
      this.rescuedCount = rescuedCount;
    }
  }

  private static final class RescueEvent {
    final int cycle;
    final long fe;
    final String role;
    final String fingerprint;
    final int poolIndex;
    final int slot;
    final int originalPddrRank;
    final double score;
    final double cmax;
    final double tec;
    final double twc;
    final int q;
    final int p;
    final String displacedFingerprint;
    final int displacedPoolIndex;
    final int displacedOriginalRank;
    final double displacedScore;
    final double displacedCmax;
    final double displacedTec;
    final double displacedTwc;
    final int displacedQ;
    final int displacedP;
    final long lineage;

    RescueEvent(int cycle, long fe, String role, String fingerprint, int poolIndex,
        int slot, int originalPddrRank, double score, int q, int p, String displacedFingerprint,
        int displacedPoolIndex, int displacedOriginalRank, double displacedScore,
        int displacedQ, int displacedP, double cmax, double tec, double twc,
        double displacedCmax, double displacedTec, double displacedTwc, long lineage) {
      this.cycle = cycle;
      this.fe = fe;
      this.role = role;
      this.fingerprint = fingerprint;
      this.poolIndex = poolIndex;
      this.slot = slot;
      this.originalPddrRank = originalPddrRank;
      this.score = score;
      this.q = q;
      this.p = p;
      this.displacedFingerprint = displacedFingerprint;
      this.displacedPoolIndex = displacedPoolIndex;
      this.displacedOriginalRank = displacedOriginalRank;
      this.displacedScore = displacedScore;
      this.displacedQ = displacedQ;
      this.displacedP = displacedP;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
      this.displacedCmax = displacedCmax;
      this.displacedTec = displacedTec;
      this.displacedTwc = displacedTwc;
      this.lineage = lineage;
    }
  }

  private static final class CycleGeometry {
    final int cycle;
    final long fe;
    final int populationSize;
    final int populationNDSize;
    final int archiveSize;
    final double[] popMin = new double[3];
    final double[] popMax = new double[3];
    final double[] popRange = new double[3];
    final double[] archMin = new double[3];
    final double[] archMax = new double[3];
    final double[] archRange = new double[3];

    CycleGeometry(int cycle, long fe, int populationSize, int populationNDSize, int archiveSize) {
      this.cycle = cycle;
      this.fe = fe;
      this.populationSize = populationSize;
      this.populationNDSize = populationNDSize;
      this.archiveSize = archiveSize;
    }
  }

  private static final class MemberSnapshot {
    final long lineageId;
    final double cmax;
    final double tec;
    final double twc;

    MemberSnapshot(long lineageId, double cmax, double tec, double twc) {
      this.lineageId = lineageId;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
    }
  }

  /** FC-6A.1：每轮池组成（score 分类）计数，纯观察。 */
  private static final class CompRecord {
    final int cycle;
    final long fe;
    final int poolSize;
    final int targetSize;
    final int poolLt1;
    final int poolEq1;
    final int poolGt1;
    final int selLt1;
    final int selEq1;
    final int selGt1;
    final int bpLt1;
    final int bpEq1;
    final int bpGt1;

    CompRecord(int cycle, long fe, int poolSize, int targetSize,
        int poolLt1, int poolEq1, int poolGt1,
        int selLt1, int selEq1, int selGt1,
        int bpLt1, int bpEq1, int bpGt1) {
      this.cycle = cycle;
      this.fe = fe;
      this.poolSize = poolSize;
      this.targetSize = targetSize;
      this.poolLt1 = poolLt1;
      this.poolEq1 = poolEq1;
      this.poolGt1 = poolGt1;
      this.selLt1 = selLt1;
      this.selEq1 = selEq1;
      this.selGt1 = selGt1;
      this.bpLt1 = bpLt1;
      this.bpEq1 = bpEq1;
      this.bpGt1 = bpGt1;
    }
  }

  /** FC-6A.2：每轮 Region×PDDR 组成 + 被拒解区域归属（纯观察镜像）。 */
  private static final class RegionRecord {
    final int cycle;
    final long fe;
    final int poolSize;
    final int targetSize;
    final int g1Lt1;
    final int g1Eq1;
    final int g2Lt1;
    final int g2Eq1;
    final int g3Lt1;
    final int g3Eq1;
    final int g4Lt1;
    final int g4Eq1;
    final int ovfLt1;
    final int ovfEq1;
    final int rejG1;
    final int rejG2;
    final int rejG3;
    final int rejG4;
    final int rejOvf;
    final int absorbable;

    RegionRecord(int cycle, long fe, int poolSize, int targetSize,
        int g1Lt1, int g1Eq1, int g2Lt1, int g2Eq1,
        int g3Lt1, int g3Eq1, int g4Lt1, int g4Eq1,
        int ovfLt1, int ovfEq1,
        int rejG1, int rejG2, int rejG3, int rejG4, int rejOvf,
        int absorbable) {
      this.cycle = cycle;
      this.fe = fe;
      this.poolSize = poolSize;
      this.targetSize = targetSize;
      this.g1Lt1 = g1Lt1;
      this.g1Eq1 = g1Eq1;
      this.g2Lt1 = g2Lt1;
      this.g2Eq1 = g2Eq1;
      this.g3Lt1 = g3Lt1;
      this.g3Eq1 = g3Eq1;
      this.g4Lt1 = g4Lt1;
      this.g4Eq1 = g4Eq1;
      this.ovfLt1 = ovfLt1;
      this.ovfEq1 = ovfEq1;
      this.rejG1 = rejG1;
      this.rejG2 = rejG2;
      this.rejG3 = rejG3;
      this.rejG4 = rejG4;
      this.rejOvf = rejOvf;
      this.absorbable = absorbable;
    }
  }

  /** FC-6A.2：174.44 反事实探针逐轮判定。 */
  private static final class ProbeRecord {
    final int cycle;
    final long fe;
    final String global;
    final String region;

    ProbeRecord(int cycle, long fe, String global, String region) {
      this.cycle = cycle;
      this.fe = fe;
      this.global = global;
      this.region = region;
    }
  }

  private static final class RescueExposure {
    final String fingerprint;
    long qgTeacherCount;
    final long[] qgByGroup = new long[4];
    long cfvfGbestCount;
    final long[] cfvfGbestByGroup = new long[4];
    long cfvfPbestCount;
    final long[] cfvfPbestByGroup = new long[4];
    long descendantPresence;
    long directPresence;
    long uniqueDescendantLineages;
    long directLineages;
    long successPresence;
    long nondomFinal;

    RescueExposure(String fingerprint) {
      this.fingerprint = fingerprint;
    }
  }
}