package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** P6.1.1 author-compatible PDDR scoring applied only to evaluated candidates. */
public final class ZhangBoEvaluatedPddrSelector {
  /** FC-6 BP-PDDR：最多保留的 q==0 边界极值槽位数（minCmax / minTEC / minTWC 去重）。 */
  public static final int MAX_BOUNDARY_SLOTS = 3;
  public enum Source {
    GLOBAL_OFFSPRING,
    PARENT,
    INTER_FACTORY_LOCAL,
    INTRA_FACTORY_VNS,
    CATA_TEST,
    CATA_APPLY,
    CRITICAL_SWAP,
    CRITICAL_INSERT,
    O1_O9
  }

  public static final class Candidate {
    private final PermutationSolution<Integer> solution;
    private final List<PermutationSolution<Integer>> authorHistory;
    private final Source source;
    private final int sourceSlot;
    private final long evaluationOrdinal;
    private final int originalOrder;
    private final double pddrScore;
    private final ZhangBoSubSwarm assignedRegionRole;

    private Candidate(
        PermutationSolution<Integer> solution,
        List<PermutationSolution<Integer>> authorHistory,
        Source source, int sourceSlot, long evaluationOrdinal,
        int originalOrder, double pddrScore) {
      this(solution, authorHistory, source, sourceSlot, evaluationOrdinal,
          originalOrder, pddrScore, null);
    }

    private Candidate(
        PermutationSolution<Integer> solution,
        List<PermutationSolution<Integer>> authorHistory,
        Source source, int sourceSlot, long evaluationOrdinal,
        int originalOrder, double pddrScore, ZhangBoSubSwarm assignedRegionRole) {
      this.solution = ZhangBoSolutionSupport.deepCopy(solution);
      this.authorHistory = Collections.unmodifiableList(
          ZhangBoSolutionSupport.deepCopySolutions(authorHistory));
      this.source = source;
      this.sourceSlot = sourceSlot;
      this.evaluationOrdinal = evaluationOrdinal;
      this.originalOrder = originalOrder;
      this.pddrScore = pddrScore;
      this.assignedRegionRole = assignedRegionRole;
    }

    public static Candidate ofEvaluated(
        PermutationSolution<Integer> solution,
        List<PermutationSolution<Integer>> authorHistory,
        Source source, int sourceSlot, long evaluationOrdinal,
        int originalOrder, double pddrScore) {
      requireEvaluated(solution, "candidate", sourceSlot);
      return new Candidate(solution, authorHistory, source, sourceSlot,
          evaluationOrdinal, originalOrder, pddrScore);
    }

    public PermutationSolution<Integer> getSolution() {
      return ZhangBoSolutionSupport.deepCopy(solution);
    }
    public List<PermutationSolution<Integer>> getAuthorHistory() {
      return ZhangBoSolutionSupport.deepCopySolutions(authorHistory);
    }
    public Source getSource() { return source; }
    public int getSourceSlot() { return sourceSlot; }
    public long getEvaluationOrdinal() { return evaluationOrdinal; }
    public int getOriginalOrder() { return originalOrder; }
    public double getPddrScore() { return pddrScore; }
    /** Assigned only by REGION_AWARE; null means global/legacy regrouping. */
    public ZhangBoSubSwarm getAssignedRegionRole() { return assignedRegionRole; }

    private Candidate withAssignedRegionRole(ZhangBoSubSwarm role) {
      if (role == null) throw new IllegalArgumentException("role");
      return new Candidate(solution, authorHistory, source, sourceSlot,
          evaluationOrdinal, originalOrder, pddrScore, role);
    }
  }

  /** Explicit provenance used when global and local evaluated offspring are merged. */
  public static final class CandidateInput {
    private final PermutationSolution<Integer> solution;
    private final List<PermutationSolution<Integer>> authorHistory;
    private final Source source;
    private final int sourceSlot;
    private final long evaluationOrdinal;
    private final int originalOrder;

    private CandidateInput(PermutationSolution<Integer> solution,
        List<PermutationSolution<Integer>> authorHistory, Source source,
        int sourceSlot, long evaluationOrdinal, int originalOrder) {
      this.solution = ZhangBoSolutionSupport.deepCopy(solution);
      this.authorHistory = Collections.unmodifiableList(
          ZhangBoSolutionSupport.deepCopySolutions(authorHistory));
      this.source = source;
      this.sourceSlot = sourceSlot;
      this.evaluationOrdinal = evaluationOrdinal;
      this.originalOrder = originalOrder;
    }

    public static CandidateInput ofEvaluated(PermutationSolution<Integer> solution,
        List<PermutationSolution<Integer>> authorHistory, Source source,
        int sourceSlot, long evaluationOrdinal, int originalOrder) {
      if (source == null) throw new IllegalArgumentException("source");
      requireEvaluated(solution, "candidate", sourceSlot);
      return new CandidateInput(solution, authorHistory, source, sourceSlot,
          evaluationOrdinal, originalOrder);
    }
  }

  public List<Candidate> select(
      List<PermutationSolution<Integer>> evaluatedOffspring,
      List<? extends List<PermutationSolution<Integer>>> offspringHistories,
      List<PermutationSolution<Integer>> evaluatedParents,
      List<? extends List<PermutationSolution<Integer>>> parentHistories,
      int targetSize,
      long firstOffspringEvaluationOrdinal) {
    requireAligned(evaluatedOffspring, offspringHistories, "offspring");
    requireAligned(evaluatedParents, parentHistories, "parent");
    if (targetSize < 1 || targetSize > evaluatedOffspring.size() + evaluatedParents.size()) {
      throw new IllegalArgumentException("Invalid PDDR targetSize=" + targetSize);
    }
    List<CandidateInput> offspring = new ArrayList<>(evaluatedOffspring.size());
    int order = 0;
    for (int index = 0; index < evaluatedOffspring.size(); index++) {
      requireEvaluated(evaluatedOffspring.get(index), "offspring", index);
      offspring.add(CandidateInput.ofEvaluated(evaluatedOffspring.get(index),
          offspringHistories.get(index), Source.GLOBAL_OFFSPRING, index,
          firstOffspringEvaluationOrdinal + index, order++));
    }
    return select(offspring, evaluatedParents, parentHistories, targetSize);
  }

  /**
   * Selects evaluated global/local offspring plus parents. The caller fixes the
   * global-before-local input order and each candidate carries its own source.
   */
  public List<Candidate> select(
      List<CandidateInput> evaluatedOffspring,
      List<PermutationSolution<Integer>> evaluatedParents,
      List<? extends List<PermutationSolution<Integer>>> parentHistories,
      int targetSize) {
    return select(evaluatedOffspring, evaluatedParents, parentHistories, targetSize,
        PddrSelectionMode.BP_RESERVED_LEGACY);
  }

  /** Explicit FC-6 selector; legacy callers retain the BP branch above. */
  public List<Candidate> select(
      List<CandidateInput> evaluatedOffspring,
      List<PermutationSolution<Integer>> evaluatedParents,
      List<? extends List<PermutationSolution<Integer>>> parentHistories,
      int targetSize, PddrSelectionMode selectionMode) {
    if (evaluatedOffspring == null) throw new IllegalArgumentException("evaluatedOffspring");
    if (selectionMode == null) throw new IllegalArgumentException("selectionMode");
    requireAligned(evaluatedParents, parentHistories, "parent");
    if (targetSize < 1 || targetSize > evaluatedOffspring.size() + evaluatedParents.size()) {
      throw new IllegalArgumentException("Invalid PDDR targetSize=" + targetSize);
    }
    List<RawCandidate> values = new ArrayList<>(evaluatedOffspring.size() + evaluatedParents.size());
    for (CandidateInput input : evaluatedOffspring) {
      if (input == null) throw new IllegalArgumentException("null evaluated offspring input");
      values.add(new RawCandidate(input.solution, input.authorHistory, input.source,
          input.sourceSlot, input.evaluationOrdinal, input.originalOrder));
    }
    int order = evaluatedOffspring.size();
    for (int index = 0; index < evaluatedParents.size(); index++) {
      requireEvaluated(evaluatedParents.get(index), "parent", index);
      values.add(new RawCandidate(evaluatedParents.get(index), parentHistories.get(index),
          Source.PARENT, index, -1L, order++));
    }

    double[] scores = authorScores(values);
    List<Candidate> ranked = new ArrayList<>(values.size());
    for (int index = 0; index < values.size(); index++) {
      RawCandidate value = values.get(index);
      ranked.add(new Candidate(value.solution, value.authorHistory, value.source,
          value.sourceSlot, value.evaluationOrdinal, value.originalOrder, scores[index]));
    }
    Collections.sort(ranked, Comparator
        .comparingDouble(Candidate::getPddrScore)
        .thenComparingInt(Candidate::getOriginalOrder));

    if (selectionMode == PddrSelectionMode.GLOBAL_ORIGINAL) {
      return new ArrayList<>(ranked.subList(0, targetSize));
    }
    if (selectionMode == PddrSelectionMode.REGION_AWARE) {
      return selectRegionAware(ranked, targetSize);
    }

    // FC-6 BP-PDDR：先把 q==0 三向极值（minCmax/minTEC/minTWC，去重，<=3）放入结果，
    // 其余位置仍按原始 (score, originalOrder) 序填充。不修改 authorScores 公式。
    List<PermutationSolution<Integer>> pool = new ArrayList<>(values.size());
    for (RawCandidate value : values) {
      pool.add(value.solution);
    }
    List<Candidate> reserved = new ArrayList<>(MAX_BOUNDARY_SLOTS);
    Set<Integer> reservedOrders = new HashSet<>(MAX_BOUNDARY_SLOTS);
    for (int index : boundaryReservedIndices(pool, scores)) {
      RawCandidate value = values.get(index);
      reserved.add(new Candidate(value.solution, value.authorHistory, value.source,
          value.sourceSlot, value.evaluationOrdinal, value.originalOrder, scores[index]));
      reservedOrders.add(value.originalOrder);
    }
    List<Candidate> outcome = new ArrayList<>(targetSize);
    int reservationSlots = Math.min(reserved.size(), targetSize);
    for (int index = 0; index < reservationSlots; index++) {
      outcome.add(reserved.get(index));
    }
    for (Candidate candidate : ranked) {
      if (outcome.size() == targetSize) {
        break;
      }
      if (reservedOrders.contains(candidate.getOriginalOrder())) {
        continue;
      }
      outcome.add(candidate);
    }
    return outcome;
  }

  /**
   * FC-6B pre-registered region allocation. Only q==0 candidates compete for
   * a role first; an unfilled role is then backfilled from the unchanged global
   * PDDR ranking. The output remains in physical slot order 1,2,3,4.
   */
  private static List<Candidate> selectRegionAware(List<Candidate> ranked, int targetSize) {
    final int required = 15 + 55 + 15 + 15;
    if (targetSize != required) {
      throw new IllegalArgumentException("REGION_AWARE requires physical capacity 100, got "
          + targetSize);
    }
    List<Candidate> remaining = new ArrayList<>(ranked);
    List<Candidate> outcome = new ArrayList<>(targetSize);
    fillRegion(outcome, remaining, ZhangBoSubSwarm.G1_CMAX, 15);
    fillRegion(outcome, remaining, ZhangBoSubSwarm.G4_BALANCED, 55);
    fillRegion(outcome, remaining, ZhangBoSubSwarm.G2_TEC, 15);
    fillRegion(outcome, remaining, ZhangBoSubSwarm.G3_TWC, 15);
    if (outcome.size() != targetSize) {
      throw new IllegalStateException("REGION_AWARE selection did not close capacity");
    }
    return outcome;
  }

  private static void fillRegion(List<Candidate> outcome, List<Candidate> remaining,
      ZhangBoSubSwarm role, int capacity) {
    List<Candidate> qZero = new ArrayList<>();
    for (Candidate candidate : remaining) {
      if (candidate.getPddrScore() <= 1.0) qZero.add(candidate);
    }
    Collections.sort(qZero, regionComparator(role));
    int count = 0;
    for (Candidate candidate : qZero) {
      if (count == capacity) break;
      if (remaining.remove(candidate)) {
        outcome.add(candidate.withAssignedRegionRole(role));
        count++;
      }
    }
    // Only unfilled capacity flows back to the original PDDR global ordering.
    while (count < capacity && !remaining.isEmpty()) {
      outcome.add(remaining.remove(0).withAssignedRegionRole(role));
      count++;
    }
    if (count != capacity) throw new IllegalStateException("Insufficient candidates for " + role);
  }

  private static Comparator<Candidate> regionComparator(final ZhangBoSubSwarm role) {
    return new Comparator<Candidate>() {
      @Override public int compare(Candidate left, Candidate right) {
        int compare;
        switch (role) {
          case G1_CMAX:
            compare = compareObjectives(left.solution, right.solution, new int[]{0, 1, 6});
            break;
          case G2_TEC:
            compare = compareObjectives(left.solution, right.solution, new int[]{1, 0, 6});
            break;
          case G3_TWC:
            compare = compareObjectives(left.solution, right.solution, new int[]{6, 0, 1});
            break;
          case G4_BALANCED:
            compare = Double.compare(left.getPddrScore(), right.getPddrScore());
            break;
          default:
            throw new IllegalStateException("Unhandled role=" + role);
        }
        return compare != 0 ? compare
            : Integer.compare(left.getOriginalOrder(), right.getOriginalOrder());
      }
    };
  }

  private static int compareObjectives(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right, int[] objectives) {
    for (int objective : objectives) {
      int compare = Double.compare(left.getObjective(objective), right.getObjective(objective));
      if (compare != 0) return compare;
    }
    return 0;
  }

  static double[] authorScores(List<RawCandidate> values) {
    double[] result = new double[values.size()];
    for (int left = 0; left < values.size(); left++) {
      double dominates = 0.0;
      double dominatedBy = 0.0;
      for (int right = 0; right < values.size(); right++) {
        if (left == right) continue;
        PermutationSolution<Integer> x = values.get(left).solution;
        PermutationSolution<Integer> y = values.get(right).solution;
        if (strictlyDominates(x, y)) dominates++;
        if (strictlyDominates(y, x)) dominatedBy++;
      }
      result[left] = dominatedBy + 1.0 / (dominates + 1.0);
    }
    return result;
  }

  static boolean strictlyDominates(
      PermutationSolution<Integer> left, PermutationSolution<Integer> right) {
    boolean noWorse = left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
    boolean strictlyBetter = left.getObjective(0) < right.getObjective(0)
        || left.getObjective(1) < right.getObjective(1)
        || left.getObjective(6) < right.getObjective(6);
    return noWorse && strictlyBetter;
  }

  /**
   * FC-6 BP-PDDR 保留规则：在合并池的 q==0（不被任何候选支配，等价于
   * {@code pddrScores[i] <= 1.0}，因 dominatedBy>=1 时 score>1 而 dominatedBy==0 时
   * score 为 1/(dominates+1)<=1，数值上严格分离）候选里，取 minCmax、minTEC、minTWC
   * 三个极值对应的池下标，按此顺序返回，按 fingerprint 去重后最多
   * {@value #MAX_BOUNDARY_SLOTS} 个。每个极值扫描按 (主目标, 其余目标, 池序) 字典序
   * 破平。池序 = {@code pool} 列表顺序（select 内即 originalOrder 升序）。
   *
   * <p>依据（FC-5.2 结论 B）：author PDDR score {@code dominatedBy + 1/(dominates+1)}
   * 把中心支配点（score≈0.02）排在边界极值（不支配任何人，score≈1.0）之前，池内最优
   * Cmax/TEC/TWC 极值可因此被挤出前 targetSize，在 archive 见到之前即死亡（seed 22/23/24
   * 的 best evaluated Cmax 174.44/169.63/191.21 全部走此路径）。本方法只保留这些
   * 知识极值、不修改公式本身。</p>
   */
  public static List<Integer> boundaryReservedIndices(
      List<PermutationSolution<Integer>> pool, double[] pddrScores) {
    if (pool == null || pddrScores == null || pool.isEmpty()
        || pddrScores.length != pool.size()) {
      throw new IllegalArgumentException("pool and pddrScores must be aligned and non-empty");
    }
    List<Integer> nonDominated = new ArrayList<>();
    for (int index = 0; index < pool.size(); index++) {
      if (pddrScores[index] <= 1.0) {
        nonDominated.add(index);
      }
    }
    List<Integer> reserved = new ArrayList<>(MAX_BOUNDARY_SLOTS);
    Set<String> reservedFingerprints = new HashSet<>(MAX_BOUNDARY_SLOTS);
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
      if (reservedFingerprints.contains(fingerprint)) {
        continue;
      }
      reservedFingerprints.add(fingerprint);
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

  private static void requireAligned(
      List<PermutationSolution<Integer>> solutions,
      List<? extends List<PermutationSolution<Integer>>> histories,
      String label) {
    if (solutions == null || histories == null || solutions.size() != histories.size()) {
      throw new IllegalArgumentException(label + " solutions/history size mismatch");
    }
  }

  private static void requireEvaluated(
      PermutationSolution<Integer> solution, String label, int index) {
    for (int objective : new int[]{0, 1, 6}) {
      double value = solution.getObjective(objective);
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException(label + '[' + index + "] objective["
            + objective + "] is not evaluated: " + value);
      }
    }
  }

  static final class RawCandidate {
    private final PermutationSolution<Integer> solution;
    private final List<PermutationSolution<Integer>> authorHistory;
    private final Source source;
    private final int sourceSlot;
    private final long evaluationOrdinal;
    private final int originalOrder;

    RawCandidate(PermutationSolution<Integer> solution,
                 List<PermutationSolution<Integer>> authorHistory,
                 Source source, int sourceSlot, long evaluationOrdinal,
                 int originalOrder) {
      this.solution = solution;
      this.authorHistory = authorHistory;
      this.source = source;
      this.sourceSlot = sourceSlot;
      this.evaluationOrdinal = evaluationOrdinal;
      this.originalOrder = originalOrder;
    }
  }
}
