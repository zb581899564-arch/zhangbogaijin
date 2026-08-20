package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * FC-5.2：Evaluated-Candidate → Archive → Final-Output Lifecycle Audit（纯观察旁路）。
 *
 * <p>只追踪"已真实 FM3 full-evaluated 且 Cmax 优秀"的候选解的生命周期，回答用户五个问题：
 * 出生来源算子 → 是否通过 local acceptance → 是否进入 merge pool / next population →
 * archive.add 是否被调用、accept 还是 reject（reject 的实时 dominator）→ 进入后是否被删除
 * （remover）→ 是否出现在最终输出。全部事件由算法侧在既有调用点旁路上报；archive 判定
 * 逻辑（弱支配、被支配成员删除）在此复制模拟，与 {@code ZhangBoIncrementalParetoArchive}
 * 完全同义，不修改任何算法决策、不进入任何选择路径。</p>
 *
 * <p>身份：fingerprint（沿用 {@link ZhangBoQgController#fingerprint}）+ evaluationOrdinal；
 * 事件按 fingerprint 匹配"出生最晚且不晚于事件时刻"的记录。出生条件（任一命中即建记录）：
 * A. 评估时刻 Cmax &lt; 当前 archive 最小 Cmax；B. 刷新本次运行正式评估 best-ever Cmax；
 * C. 进入 evaluated Cmax Top-20。普通解只做廉价判定（Top-20 维护），不计算 fingerprint。</p>
 *
 * <p>静态门与实例模式仿 {@code V35CmaxBestEver}：runner 在运行前
 * {@code setEnabled(true) + reset()}，结束后 {@code finish(...)} + {@code fc52SummaryText()}。</p>
 */
public final class V35Fc52LifecycleAudit {

  private static final int TOP_K = 20;

  private static volatile boolean enabled = false;
  private static V35Fc52LifecycleAudit current;

  private final List<LifecycleRecord> records = new ArrayList<>();
  private final TreeSet<TopEntry> top20 = new TreeSet<>(new Comparator<TopEntry>() {
    @Override public int compare(TopEntry left, TopEntry right) {
      int byCmax = Double.compare(left.cmax, right.cmax);
      if (byCmax != 0) return byCmax;
      return Long.compare(left.fe, right.fe);
    }
  });
  private final List<LifecycleRecord> acceptedTracked = new ArrayList<>();

  private double bestEverCmax = Double.POSITIVE_INFINITY;
  private double bestEverTec = Double.NaN;
  private double bestEverTwc = Double.NaN;
  private long bestEverAt = -1L;
  private double archiveMinCmax = Double.POSITIVE_INFINITY;
  private boolean archiveInitialized = false;
  private long finalArchiveSize = -1L;
  private long finalFrontSize = -1L;
  private int nextAuditId = 1;

  private long trackedCount;
  private long localAcceptedCount;
  private long localRejectedCount;
  private long mergePoolCount;
  private long pddrSurviveCount;
  private long pddrRejectCount;
  private long archiveAddCalledCount;
  private long archiveAcceptCount;
  private long archiveRejectCount;
  private long archiveDuplicateCount;
  private long archiveRemovedCount;
  private long finalPresentCount;
  private long boundaryPoolCount;
  private long boundarySurviveCount;
  private final Map<Integer, CycleArchiveStat> cycleArchiveStats = new TreeMap<>();

  private V35Fc52LifecycleAudit() {
  }

  public static void setEnabled(boolean value) {
    enabled = value;
    if (value) {
      current = new V35Fc52LifecycleAudit();
    } else {
      current = null;
    }
  }

  public static boolean isEnabled() {
    return enabled;
  }

  /** 当前审计实例（未启用时为 null）；算法侧经由此访问实例方法。 */
  public static V35Fc52LifecycleAudit current() {
    return current;
  }

  public static void reset() {
    if (enabled) {
      current = new V35Fc52LifecycleAudit();
    }
  }

  /** 出生记录：一次正式 FM3 full evaluation 完成后调用（评估入口已由算法侧标记来源）。 */
  public void recordEvaluated(PermutationSolution<Integer> solution,
      V35EvaluationSourceContext.Source source, long fe, int cycle, int qRound,
      long lineageId) {
    if (this != current) {
      return;
    }
    double cmax = solution.getObjective(0);
    double tec = solution.getObjective(1);
    double twc = solution.getObjective(6);
    boolean newBest = cmax < bestEverCmax;
    if (newBest) {
      bestEverCmax = cmax;
      bestEverTec = tec;
      bestEverTwc = twc;
      bestEverAt = fe;
    }
    boolean belowArchive = cmax < archiveMinCmax;
    TopEntry entry = new TopEntry(cmax, fe);
    top20.add(entry);
    if (top20.size() > TOP_K) {
      top20.pollLast();
    }
    boolean inTop20 = top20.contains(entry);
    if (!(newBest || belowArchive || inTop20)) {
      return;
    }
    String fingerprint = ZhangBoQgController.fingerprint(solution);
    LifecycleRecord record = new LifecycleRecord(nextAuditId++, fingerprint, source, fe,
        cycle, qRound, lineageId, cmax, tec, twc);
    records.add(record);
    trackedCount++;
  }

  /** 局部候选被 local acceptance 接受（reason 如 ACCEPTED / QUALITY_GAIN）。 */
  public void recordLocalAccepted(PermutationSolution<Integer> candidate, long fe,
      String reason) {
    if (this != current) {
      return;
    }
    LifecycleRecord record = match(candidate, fe);
    if (record == null || record.localSettled) {
      return;
    }
    record.localSettled = true;
    record.localAccepted = true;
    record.localReason = reason;
    localAcceptedCount++;
  }

  /** 局部候选被 local acceptance 拒绝（reason：NOT_BETTER / NO_RECOVERY_GAIN）。 */
  public void recordLocalRejected(PermutationSolution<Integer> candidate, long fe,
      String reason) {
    if (this != current) {
      return;
    }
    LifecycleRecord record = match(candidate, fe);
    if (record == null || record.localSettled) {
      return;
    }
    record.localSettled = true;
    record.localAccepted = false;
    record.localReason = reason;
    localRejectedCount++;
  }

  /** 候选进入 PDDR 候选池（applyEvaluatedPddr 物化局部候选处）。 */
  public void recordMergePool(List<PermutationSolution<Integer>> candidates, long fe) {
    if (this != current) {
      return;
    }
    for (PermutationSolution<Integer> candidate : candidates) {
      LifecycleRecord record = match(candidate, fe);
      if (record == null) {
        continue;
      }
      record.enteredMergePool = true;
      mergePoolCount++;
    }
  }

  /**
   * PDDR round 结算：allCandidates 的顺序必须与
   * {@link ZhangBoEvaluatedPddrSelector#select(List, List, List, int)} 内部的
   * values 顺序完全一致（inputs = 全局 offspring + 局部候选，随后 parents）。
   * score 复制 authorScores 逻辑（纯观察）。selected 为 select 返回的前
   * targetSize 个（已按 score 排序）。
   */
  public void recordPddrRound(List<PermutationSolution<Integer>> allCandidates,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected, long fe) {
    if (this != current) {
      return;
    }
    double[] scores = pddrScores(allCandidates);
    Set<String> selectedFingerprints = new HashSet<>();
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      selectedFingerprints.add(ZhangBoQgController.fingerprint(candidate.getSolution()));
    }
    // FC-6 R_retain：用与 select 完全相同的保留规则镜像计算每轮 q==0 边界极值
    // 进入池数量与存活数量（纯观察；Build A 基线运行中规则未生效时的存活率即基线值）。
    for (int index : boundaryReservedIndexes(allCandidates, scores)) {
      boundaryPoolCount++;
      if (selectedFingerprints.contains(
          ZhangBoQgController.fingerprint(allCandidates.get(index)))) {
        boundarySurviveCount++;
      }
    }
    for (int index = 0; index < allCandidates.size(); index++) {
      String fingerprint = ZhangBoQgController.fingerprint(allCandidates.get(index));
      LifecycleRecord record = matchByFingerprint(fingerprint, fe);
      if (record == null || record.pddrSettled) {
        continue;
      }
      record.pddrSettled = true;
      record.pddrScore = scores[index];
      if (selectedFingerprints.contains(fingerprint)) {
        record.pddrSurvived = true;
        record.pddrRank = rankIn(selected, fingerprint);
        pddrSurviveCount++;
      } else {
        record.pddrSurvived = false;
        pddrRejectCount++;
      }
    }
  }

  /** archive.add 调用点旁路：记录 ADD_CALLED / ACCEPT / REJECT(实时 dominator) / 删除模拟。 */
  public void observeArchiveAdd(PermutationSolution<Integer> candidate,
      List<PermutationSolution<Integer>> archive, long fe, int cycle) {
    if (this != current) {
      return;
    }
    String fingerprint = ZhangBoQgController.fingerprint(candidate);
    LifecycleRecord record = matchByFingerprint(fingerprint, fe);
    if (record != null) {
      record.archiveAddCalled = true;
      archiveAddCalledCount++;
    }
    if (!archiveInitialized) {
      archiveMinCmax = minCmax(archive);
      archiveInitialized = true;
    }
    PermutationSolution<Integer> dominator = null;
    for (PermutationSolution<Integer> existing : archive) {
      if (weaklyDominates(existing, candidate)) {
        dominator = existing;
        break;
      }
    }
    CycleArchiveStat stat = cycleArchiveStats.computeIfAbsent(cycle, c -> new CycleArchiveStat());
    stat.submits++;
    stat.lastSizeBefore = archive.size();
    if (dominator != null) {
      stat.rejects++;
      stat.lastAccepted = false;
      if (record != null) {
        boolean duplicate = Double.compare(dominator.getObjective(0), candidate.getObjective(0)) == 0
            && Double.compare(dominator.getObjective(1), candidate.getObjective(1)) == 0
            && Double.compare(dominator.getObjective(6), candidate.getObjective(6)) == 0;
        record.archiveAccepted = false;
        record.archiveRejectReason = duplicate ? "DUPLICATE" : "DOMINATED";
        record.archiveDominatorText = objectiveText(dominator);
        if (duplicate) {
          archiveDuplicateCount++;
        } else {
          archiveRejectCount++;
        }
      }
      return;
    }
    stat.accepts++;
    stat.lastAccepted = true;
    if (record != null) {
      record.archiveAccepted = true;
      record.archiveAcceptedAt = fe;
      record.archiveAcceptedCycle = cycle;
      acceptedTracked.add(record);
      archiveAcceptCount++;
    }
    // 与 ZhangBoIncrementalParetoArchive.add 同义的删除模拟：新成员弱支配的旧成员被移除。
    int removedThisCall = 0;
    for (LifecycleRecord accepted : acceptedTracked) {
      if (accepted != record && !accepted.archiveRemoved
          && weaklyDominates(candidate, accepted.objectives)) {
        accepted.archiveRemoved = true;
        accepted.removerText = objectiveText(candidate);
        accepted.removedAtFe = fe;
        archiveRemovedCount++;
        removedThisCall++;
      }
    }
    stat.removed += removedThisCall;
    archiveMinCmax = Math.min(archiveMinCmax, minCmax(archive));
    archiveMinCmax = Math.min(archiveMinCmax, candidate.getObjective(0));
  }

  /** 运行结束：最终 archive / front 尺寸与 final presence 判定。 */
  public void finish(List<PermutationSolution<Integer>> archive,
      List<double[]> finalFront, long fe) {
    if (this != current) {
      return;
    }
    finalArchiveSize = archive.size();
    finalFrontSize = finalFront == null ? -1L : finalFront.size();
    if (finalFront == null) {
      return;
    }
    for (LifecycleRecord record : records) {
      for (double[] point : finalFront) {
        if (Double.compare(point[0], record.cmax) == 0
            && Double.compare(point[1], record.tec) == 0
            && Double.compare(point[2], record.twc) == 0) {
          record.finalPresent = true;
          finalPresentCount++;
          break;
        }
      }
    }
  }

  public List<LifecycleRecord> records() {
    return records;
  }

  public double bestEverCmax() {
    return bestEverCmax;
  }

  public double bestEverTec() {
    return bestEverTec;
  }

  public double bestEverTwc() {
    return bestEverTwc;
  }

  public long bestEverAt() {
    return bestEverAt;
  }

  /** FC-6 R_retain = 边界极值候选存活于 PDDR / 进入合并池（无量纲 0..1）。 */
  private String rRetainText() {
    if (boundaryPoolCount == 0) {
      return "NA";
    }
    return String.format("%.4f", (double) boundarySurviveCount / boundaryPoolCount);
  }

  /** 每周期 archive 提交统计（FC-6 根因诊断用；纯观察，按 cycle 分桶）。 */
  private static final class CycleArchiveStat {
    int submits;
    int accepts;
    int rejects;
    int removed;
    int lastSizeBefore;
    boolean lastAccepted;
  }

  /** 每周期 archive 尺寸/吞吐诊断段（fc52 汇总尾部输出）。 */
  private void appendCycleArchiveStats(StringBuilder out) {
    out.append("fc52CycleArchiveBegin\n");
    for (Map.Entry<Integer, CycleArchiveStat> entry : cycleArchiveStats.entrySet()) {
      CycleArchiveStat stat = entry.getValue();
      int sizeAfter = stat.lastSizeBefore + (stat.lastAccepted ? 1 : 0);
      out.append("fc52CycleArchive ").append(entry.getKey())
          .append(":submit=").append(stat.submits)
          .append(",accept=").append(stat.accepts)
          .append(",reject=").append(stat.rejects)
          .append(",removed=").append(stat.removed)
          .append(",archSizeEst=").append(sizeAfter).append('\n');
    }
    out.append("fc52CycleArchiveEnd\n");
  }

  /**
   * FC-6 R_retain 镜像：与 {@link ZhangBoEvaluatedPddrSelector#boundaryReservedIndices}
   * 同一保留规则（q==0 ⇔ score&lt;=1；minCmax/minTEC/minTWC 字典序扫描；fingerprint 去重）。
   * 审计内部独立实现，保证 Build A（规则未生效）与 Build B（规则生效）用同一口径度量；
   * 二者一致性由 Build B 运行端到端校验（R_retain 应≈1）。
   */
  private static List<Integer> boundaryReservedIndexes(
      List<PermutationSolution<Integer>> pool, double[] pddrScores) {
    List<Integer> nonDominated = new ArrayList<>();
    for (int index = 0; index < pool.size(); index++) {
      if (pddrScores[index] <= 1.0) {
        nonDominated.add(index);
      }
    }
    List<Integer> reserved = new ArrayList<>(3);
    Set<String> fingerprints = new HashSet<>();
    for (int objective : new int[]{0, 1, 6}) {
      Integer best = null;
      for (int index : nonDominated) {
        if (best == null || lexicographicallyBetter(pool.get(index), pool.get(best),
            objective, index, best)) {
          best = index;
        }
      }
      if (best == null) {
        continue;
      }
      String fingerprint = ZhangBoQgController.fingerprint(pool.get(best));
      if (fingerprints.contains(fingerprint)) {
        continue;
      }
      fingerprints.add(fingerprint);
      reserved.add(best);
    }
    return reserved;
  }

  /** 按 (主目标, 其余目标按固定序, 池序) 字典序比较哪个更"极值"。 */
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

  /** 漏斗表 + 逐记录生命周期 + Top-20 命运表（运行结束后一次输出）。 */
  public String fc52SummaryText() {
    StringBuilder out = new StringBuilder();
    out.append("fc52LifecycleAudit\n")
        .append("fc52Tracked=").append(trackedCount).append('\n')
        .append("fc52LocalAccepted=").append(localAcceptedCount).append('\n')
        .append("fc52LocalRejected=").append(localRejectedCount).append('\n')
        .append("fc52EnteredMergePool=").append(mergePoolCount).append('\n')
        .append("fc52PddrSurvived=").append(pddrSurviveCount).append('\n')
        .append("fc52PddrRejected=").append(pddrRejectCount).append('\n')
        .append("fc52BoundaryPool=").append(boundaryPoolCount).append('\n')
        .append("fc52BoundarySurvived=").append(boundarySurviveCount).append('\n')
        .append("fc52RRetain=").append(rRetainText()).append('\n')
        .append("fc52ArchiveAddCalled=").append(archiveAddCalledCount).append('\n')
        .append("fc52ArchiveAccepted=").append(archiveAcceptCount).append('\n')
        .append("fc52ArchiveRejectedDominated=").append(archiveRejectCount).append('\n')
        .append("fc52ArchiveRejectedDuplicate=").append(archiveDuplicateCount).append('\n')
        .append("fc52ArchiveNeverObserved=")
        .append(countArchiveNeverObserved()).append('\n')
        .append("fc52ArchiveRemoved=").append(archiveRemovedCount).append('\n')
        .append("fc52FinalPresent=").append(finalPresentCount).append('\n')
        .append("fc52BestEverEvaluated=").append(bestEverCmax).append('\n')
        .append("fc52BestEverTec=").append(bestEverTec).append('\n')
        .append("fc52BestEverTwc=").append(bestEverTwc).append('\n')
        .append("fc52BestEverAt=").append(bestEverAt).append('\n')
        .append("fc52FinalArchiveSize=").append(finalArchiveSize).append('\n')
        .append("fc52FinalFrontSize=").append(finalFrontSize).append('\n')
        .append("fc52RecordsBegin\n");
    List<LifecycleRecord> ordered = new ArrayList<>(records);
    ordered.sort(Comparator.comparingLong(r -> r.evaluationOrdinal));
    for (LifecycleRecord record : ordered) {
      out.append(recordLine(record)).append('\n');
    }
    out.append("fc52RecordsEnd\n");
    out.append("fc52Top20Begin\n");
    List<LifecycleRecord> top = new ArrayList<>(records);
    top.sort(Comparator.comparingDouble(r -> r.cmax));
    int shown = Math.min(TOP_K, top.size());
    for (int index = 0; index < shown; index++) {
      LifecycleRecord record = top.get(index);
      out.append("fc52Top20 ").append(index + 1).append('|')
          .append(format(record.cmax)).append('|').append(format(record.tec)).append('|')
          .append(format(record.twc)).append('|').append(record.source).append('|')
          .append(record.evaluationOrdinal).append('|').append(record.fateText())
          .append('\n');
    }
    out.append("fc52Top20End\n");
    appendCycleArchiveStats(out);
    out.append("fc52LifecycleAuditEnd\n");
    return out.toString();
  }

  private static String recordLine(LifecycleRecord record) {
    StringBuilder out = new StringBuilder();
    out.append("fc52Record ").append(record.auditId)
        .append("|").append(record.fingerprint)
        .append("|").append(record.source)
        .append("|fe=").append(record.evaluationOrdinal)
        .append("|cycle=").append(record.birthCycle)
        .append("|qRound=").append(record.qRound)
        .append("|lineage=").append(record.lineageId)
        .append("|Cmax=").append(format(record.cmax))
        .append("|TEC=").append(format(record.tec))
        .append("|TWC=").append(format(record.twc))
        .append("|local=").append(record.localSettled
            ? (record.localAccepted ? "ACCEPT:" + record.localReason
                : "REJECT:" + record.localReason) : "NONE")
        .append("|mergePool=").append(record.enteredMergePool ? "yes" : "no")
        .append("|pddr=").append(record.pddrSettled
            ? (record.pddrSurvived ? "SURVIVE:rank=" + record.pddrRank
                : "REJECT:score=" + format(record.pddrScore)) : "NONE")
        .append("|archive=").append(record.archiveAddCalled ? "addCalled" : "never")
        .append(record.archiveAddCalled && record.archiveAccepted ? ":accepted@"
            + record.archiveAcceptedAt : "")
        .append(record.archiveAddCalled && !record.archiveAccepted
            ? ":rejected=" + record.archiveRejectReason + " by "
                + record.archiveDominatorText : "")
        .append(record.archiveRemoved ? ":removed@" + record.removedAtFe + " by "
            + record.removerText : "")
        .append("|final=").append(record.finalPresent ? "yes" : "no");
    return out.toString();
  }

  private long countArchiveNeverObserved() {
    long count = 0;
    for (LifecycleRecord record : records) {
      if (!record.archiveAddCalled) {
        count++;
      }
    }
    return count;
  }

  private static int rankIn(List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      String fingerprint) {
    for (int index = 0; index < selected.size(); index++) {
      String candidateFingerprint = ZhangBoQgController
          .fingerprint(selected.get(index).getSolution());
      if (candidateFingerprint.equals(fingerprint)) {
        return index;
      }
    }
    return -1;
  }

  /** 匹配"出生最晚且不晚于事件时刻"的同 fingerprint 记录。 */
  private static LifecycleRecord match(PermutationSolution<Integer> candidate, long fe) {
    String fingerprint = ZhangBoQgController.fingerprint(candidate);
    return matchByFingerprint(fingerprint, fe);
  }

  private static LifecycleRecord matchByFingerprint(String fingerprint, long fe) {
    if (current == null) {
      return null;
    }
    LifecycleRecord best = null;
    for (LifecycleRecord record : current.records) {
      if (record.fingerprint.equals(fingerprint) && record.evaluationOrdinal <= fe) {
        if (best == null || record.evaluationOrdinal > best.evaluationOrdinal) {
          best = record;
        }
      }
    }
    return best;
  }

  /** 复制 ZhangBoEvaluatedPddrSelector.authorScores（纯观察，不进决策）。 */
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

  private static boolean strictlyDominates(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    boolean noWorse = left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
    boolean strictlyBetter = left.getObjective(0) < right.getObjective(0)
        || left.getObjective(1) < right.getObjective(1)
        || left.getObjective(6) < right.getObjective(6);
    return noWorse && strictlyBetter;
  }

  /** 复制 ZhangBoIncrementalParetoArchive.weaklyDominates（3 目标 (0,1,6)）。 */
  private static boolean weaklyDominates(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    return left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
  }

  private static boolean weaklyDominates(PermutationSolution<Integer> left,
      double[] right) {
    return left.getObjective(0) <= right[0]
        && left.getObjective(1) <= right[1]
        && left.getObjective(6) <= right[2];
  }

  private static double minCmax(List<PermutationSolution<Integer>> archive) {
    double min = Double.POSITIVE_INFINITY;
    for (PermutationSolution<Integer> solution : archive) {
      min = Math.min(min, solution.getObjective(0));
    }
    return min;
  }

  private static String objectiveText(PermutationSolution<Integer> solution) {
    return "[" + format(solution.getObjective(0)) + ','
        + format(solution.getObjective(1)) + ','
        + format(solution.getObjective(6)) + ']';
  }

  private static String format(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return String.valueOf(value);
    }
    if (value == Math.rint(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  /** 一条被追踪候选的完整生命周期状态。 */
  public static final class LifecycleRecord {
    public final int auditId;
    public final String fingerprint;
    public final V35EvaluationSourceContext.Source source;
    public final long evaluationOrdinal;
    public final int birthCycle;
    public final int qRound;
    public final long lineageId;
    public final double cmax;
    public final double tec;
    public final double twc;
    public final double[] objectives;

    private boolean localSettled;
    private boolean localAccepted;
    private String localReason;
    private boolean enteredMergePool;
    private boolean pddrSettled;
    private boolean pddrSurvived;
    private double pddrScore = Double.NaN;
    private int pddrRank = -1;
    private boolean archiveAddCalled;
    private boolean archiveAccepted;
    private String archiveRejectReason;
    private String archiveDominatorText;
    private long archiveAcceptedAt = -1L;
    private int archiveAcceptedCycle = -1;
    private boolean archiveRemoved;
    private String removerText;
    private long removedAtFe = -1L;
    private boolean finalPresent;

    private LifecycleRecord(int auditId, String fingerprint,
        V35EvaluationSourceContext.Source source, long evaluationOrdinal, int birthCycle,
        int qRound, long lineageId, double cmax, double tec, double twc) {
      this.auditId = auditId;
      this.fingerprint = fingerprint;
      this.source = source;
      this.evaluationOrdinal = evaluationOrdinal;
      this.birthCycle = birthCycle;
      this.qRound = qRound;
      this.lineageId = lineageId;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
      this.objectives = new double[] {cmax, tec, twc};
    }

    public boolean isLocalAccepted() {
      return localAccepted;
    }

    public String localReason() {
      return localReason;
    }

    public boolean enteredMergePool() {
      return enteredMergePool;
    }

    public boolean pddrSurvived() {
      return pddrSurvived;
    }

    public double pddrScore() {
      return pddrScore;
    }

    public boolean archiveAddCalled() {
      return archiveAddCalled;
    }

    public boolean archiveAccepted() {
      return archiveAccepted;
    }

    public String archiveRejectReason() {
      return archiveRejectReason;
    }

    public boolean archiveRemoved() {
      return archiveRemoved;
    }

    public boolean finalPresent() {
      return finalPresent;
    }

    public String fateText() {
      StringBuilder out = new StringBuilder();
      if (!localSettled) {
        out.append("local=NONE");
      } else {
        out.append(localAccepted ? "local=ACCEPT" : "local=REJECT");
      }
      if (enteredMergePool) {
        out.append(",mergePool=yes");
      }
      if (pddrSettled) {
        out.append(pddrSurvived ? ",pddr=SURVIVE:" + pddrRank
            : ",pddr=REJECT:" + format(pddrScore));
      }
      if (archiveAddCalled) {
        out.append(archiveAccepted ? ",archive=ACCEPT"
            : ",archive=REJECT:" + archiveRejectReason);
      } else {
        out.append(",archive=NEVER");
      }
      if (archiveRemoved) {
        out.append(",REMOVED");
      }
      out.append(finalPresent ? ",final=yes" : ",final=no");
      return out.toString();
    }
  }

  private static final class TopEntry {
    final double cmax;
    final long fe;

    TopEntry(double cmax, long fe) {
      this.cmax = cmax;
      this.fe = fe;
    }
  }
}
