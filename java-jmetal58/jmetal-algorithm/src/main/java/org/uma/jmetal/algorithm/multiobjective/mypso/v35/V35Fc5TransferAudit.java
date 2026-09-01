package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * FC5-T's opt-in, observer-only transfer ledger.
 *
 * <p>It deliberately reads the same merge pool that {@code GLOBAL_ORIGINAL}
 * ranks, but it never supplies a score, a candidate, a random draw, or a
 * control decision back to the algorithm.  Records are bounded by formal PDDR
 * rounds and their four deterministic representatives.
 */
public final class V35Fc5TransferAudit {
  public static final String VERSION = "FC5_100JOB_TRANSFER_V1";
  private static final int TARGET_WORKING_POPULATION = 100;
  private static final long WINDOW_FE = 50_000L;
  private static final int[] OBJECTIVES = new int[]{0, 1, 6};

  private boolean enabled;
  private long seed = -1L;
  private long observerErrors;
  private final List<Round> rounds = new ArrayList<Round>();
  private final List<Representative> representatives = new ArrayList<Representative>();
  private final Map<String, List<Representative>> liveByFingerprint =
      new HashMap<String, List<Representative>>();
  /*
   * A child fingerprint can occur more than once in a cycle.  Preserve each
   * generation call as a FIFO group so that the first evaluation cannot
   * accidentally settle several physically distinct, equal-genotype children.
   */
  private final Map<String, List<List<PendingOffspring>>> pendingOffspring =
      new HashMap<String, List<List<PendingOffspring>>>();
  private final List<ArchiveWorkingGap> gaps = new ArrayList<ArchiveWorkingGap>();

  public void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      rounds.clear();
      representatives.clear();
      liveByFingerprint.clear();
      pendingOffspring.clear();
      gaps.clear();
      observerErrors = 0L;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setSeed(long value) {
    if (enabled) seed = value;
  }

  /**
   * Reads the exact candidate ordering passed to the PDDR selector.  The
   * caller supplies the parallel source list because CandidateInput is
   * intentionally write-only in the selection API.
   */
  public void recordPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int cycle, int g1Size, int g4Size, int g2Size, int g3Size) {
    if (!enabled) return;
    try {
      if (pool == null || sources == null || selected == null || pool.size() != sources.size()) {
        throw new IllegalArgumentException("unaligned FC5-T PDDR ledger inputs");
      }
      final double[] scores = scores(pool);
      final List<Integer> ranked = scoreOrder(scores);
      final Map<Integer, Integer> selectedSlotByOrder = selectedSlots(selected);
      final boolean[] unique = firstObjectiveTriples(pool);
      final int uniqueCount = count(unique);
      final int nondominated = strictNondominatedCount(pool, unique);
      Round round = new Round(cycle, fe, pool.size(), uniqueCount, nondominated,
          TARGET_WORKING_POPULATION == 0 ? Double.NaN
              : ((double) nondominated) / TARGET_WORKING_POPULATION);
      rounds.add(round);

      // A selected representative remains live only while it is actually
      // retained in the reconstructed working population.  The comparison
      // is fingerprint-based because the selector returns reconstructed
      // candidate objects, not stable pool indices.
      Set<String> selectedFingerprints = selectedFingerprints(selected);
      retireMissing(selectedFingerprints, cycle);

      double[] min = bounds(pool, true);
      double[] max = bounds(pool, false);
      int[] winners = new int[]{
          objectiveWinner(pool, 0), objectiveWinner(pool, 1), objectiveWinner(pool, 6),
          balancedWinner(pool, min, max)
      };
      String[] labels = new String[]{"E_C", "E_E", "E_W", "E_B"};
      for (int index = 0; index < winners.length; index++) {
        int poolIndex = winners[index];
        boolean selectedNow = selectedSlotByOrder.containsKey(poolIndex);
        int nextSlot = selectedNow ? selectedSlotByOrder.get(poolIndex) : -1;
        String fingerprint = fingerprint(pool.get(poolIndex));
        int rank = ranked.indexOf(poolIndex) + 1;
        Representative record = new Representative(cycle, fe, labels[index], poolIndex,
            sources.get(poolIndex), fingerprint, objectives(pool.get(poolIndex)), scores[poolIndex],
            rank, selectedNow, selectedNow ? "SELECTED" : "PDDR_SCORE_RANK_NOT_SELECTED",
            nextSlot, selectedNow ? roleForSlot(nextSlot, g1Size, g4Size, g2Size, g3Size) : "NONE");
        representatives.add(record);
        if (selectedNow) addLive(record);
      }
    } catch (RuntimeException ignored) {
      // The audit is deliberately fail-open for search behaviour.  The
      // resulting error count makes an incomplete observer visible in evidence.
      observerErrors++;
    }
  }

  public void observeTeacherUse(String teacherKind, ZhangBoSubSwarm requestingRole,
      PermutationSolution<Integer> teacher, long fe, int cycle) {
    if (!enabled || teacher == null) return;
    try {
      List<Representative> records = liveByFingerprint.get(fingerprint(teacher));
      if (records == null) return;
      for (Representative record : records) {
        if ("QG".equals(teacherKind)) record.qgTeacherUses++;
        if ("QP".equals(teacherKind)) record.qpTeacherUses++;
        record.teacherUseCycles.add(Integer.valueOf(cycle));
        record.lastTeacherRole = requestingRole == null ? "UNASSIGNED" : requestingRole.name();
        record.lastTeacherFe = fe;
      }
    } catch (RuntimeException ignored) {
      observerErrors++;
    }
  }

  /** Associates a generated CFVF child with an actually inherited teacher. */
  public void observeGeneratedOffspring(PermutationSolution<Integer> parent,
      PermutationSolution<Integer> teacher, PermutationSolution<Integer> offspring,
      String teacherKind, ZhangBoSubSwarm requestingRole) {
    if (!enabled || parent == null || teacher == null || offspring == null) return;
    try {
      List<Representative> records = liveByFingerprint.get(fingerprint(teacher));
      if (records == null || records.isEmpty()) return;
      String child = fingerprint(offspring);
      List<List<PendingOffspring>> pendingGroups = pendingOffspring.get(child);
      if (pendingGroups == null) {
        pendingGroups = new ArrayList<List<PendingOffspring>>();
        pendingOffspring.put(child, pendingGroups);
      }
      List<PendingOffspring> pending = new ArrayList<PendingOffspring>();
      for (Representative record : records) {
        pending.add(new PendingOffspring(record, teacherKind, requestingRole,
            objectives(parent)));
      }
      pendingGroups.add(pending);
    } catch (RuntimeException ignored) {
      observerErrors++;
    }
  }

  /** Resolves pending teacher-derived children only after their real decoder evaluation. */
  public void observeEvaluatedCandidate(PermutationSolution<Integer> candidate, long fe) {
    if (!enabled || candidate == null) return;
    try {
      String childFingerprint = fingerprint(candidate);
      List<List<PendingOffspring>> pendingGroups = pendingOffspring.get(childFingerprint);
      if (pendingGroups == null || pendingGroups.isEmpty()) return;
      List<PendingOffspring> pending = pendingGroups.remove(0);
      if (pendingGroups.isEmpty()) pendingOffspring.remove(childFingerprint);
      double[] child = objectives(candidate);
      for (PendingOffspring item : pending) {
        if (improvesRepresentativeDirection(item.representative.label, child,
            item.parentObjectives)) {
          item.representative.improvedOffspringCount++;
          item.representative.lastImprovementFe = fe;
          item.representative.lastImprovementTeacherKind = item.teacherKind;
          item.representative.lastImprovementRequestingRole = item.requestingRole == null
              ? "UNASSIGNED" : item.requestingRole.name();
        }
      }
    } catch (RuntimeException ignored) {
      observerErrors++;
    }
  }

  /** Captures the archive/working-population utilization gap after archive refresh. */
  public void observeArchiveWorkingGap(int cycle, long fe,
      List<PermutationSolution<Integer>> working,
      List<PermutationSolution<Integer>> archive) {
    if (!enabled) return;
    try {
      gaps.add(new ArchiveWorkingGap(cycle, fe, best(working, 0), best(archive, 0),
          best(working, 1), best(archive, 1), best(working, 6), best(archive, 6),
          working == null ? 0 : working.size(), archive == null ? 0 : archive.size()));
    } catch (RuntimeException ignored) {
      observerErrors++;
    }
  }

  public String mergeRoundsCsv() {
    StringBuilder out = new StringBuilder("seed,cycle,fe,Nmerge,Nunique,Nnd,Roverflow\n");
    for (Round round : rounds) {
      out.append(seed).append(',').append(round.cycle).append(',').append(round.fe).append(',')
          .append(round.merge).append(',').append(round.unique).append(',').append(round.nd)
          .append(',').append(round.overflow).append('\n');
    }
    return out.toString();
  }

  public String windowedMergeCsv() {
    TreeMap<Long, Window> windows = new TreeMap<Long, Window>();
    for (Round round : rounds) {
      long end = ((Math.max(1L, round.fe) - 1L) / WINDOW_FE + 1L) * WINDOW_FE;
      Window window = windows.get(Long.valueOf(end));
      if (window == null) {
        window = new Window(end);
        windows.put(Long.valueOf(end), window);
      }
      window.add(round);
    }
    StringBuilder out = new StringBuilder(
        "seed,windowEndFE,rounds,meanNmerge,meanNnd,maxNnd,meanRoverflow,maxRoverflow\n");
    for (Window window : windows.values()) out.append(window.csv(seed));
    return out.toString();
  }

  public String representativesCsv() {
    StringBuilder out = new StringBuilder(
        "seed,cycle,fe,representative,poolIndex,source,fingerprint,Cmax,TEC,TWC,pddrScore,pddrRank,"
        + "poolPresent,pddrSelected,rejectReason,nextPopulationSlot,nextSemanticRole,qgTeacherUses,"
        + "qpTeacherUses,teacherUseCycles,improvedOffspringCount,lastImprovementFE,"
        + "lastImprovementTeacherKind,lastImprovementRequestingRole,lastTeacherFE,lastTeacherRole,"
        + "retiredAtCycle\n");
    for (Representative value : representatives) out.append(value.csv(seed));
    return out.toString();
  }

  public String archiveWorkingGapCsv() {
    StringBuilder out = new StringBuilder(
        "seed,cycle,fe,workingBestCmax,archiveBestCmax,cmaxGap,workingBestTEC,archiveBestTEC,"
        + "tecGap,workingBestTWC,archiveBestTWC,twcGap,workingSize,archiveSize\n");
    for (ArchiveWorkingGap gap : gaps) out.append(gap.csv(seed));
    return out.toString();
  }

  public String summaryProperties() {
    int selected = 0;
    int improved = 0;
    for (Representative record : representatives) {
      if (record.pddrSelected) selected++;
      if (record.improvedOffspringCount > 0L) improved++;
    }
    return "schema=" + VERSION + "\nseed=" + seed + "\nenabled=" + enabled
        + "\npddrRounds=" + rounds.size() + "\nrepresentativeRecords=" + representatives.size()
        + "\nrepresentativesSelected=" + selected + "\nrepresentativesWithImprovedOffspring=" + improved
        + "\narchiveWorkingSnapshots=" + gaps.size() + "\nobserverErrors=" + observerErrors + "\n";
  }

  private void addLive(Representative record) {
    List<Representative> values = liveByFingerprint.get(record.fingerprint);
    if (values == null) {
      values = new ArrayList<Representative>();
      liveByFingerprint.put(record.fingerprint, values);
    }
    values.add(record);
  }

  private static Set<String> selectedFingerprints(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected) {
    Set<String> result = new HashSet<String>();
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      result.add(fingerprint(candidate.getSolution()));
    }
    return result;
  }

  private void retireMissing(Set<String> selectedFingerprints, int cycle) {
    java.util.Iterator<Map.Entry<String, List<Representative>>> iterator =
        liveByFingerprint.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, List<Representative>> entry = iterator.next();
      if (selectedFingerprints.contains(entry.getKey())) continue;
      for (Representative record : entry.getValue()) {
        if (record.retiredAtCycle < 0) record.retiredAtCycle = cycle;
      }
      iterator.remove();
    }
  }

  private static Map<Integer, Integer> selectedSlots(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected) {
    Map<Integer, Integer> result = new HashMap<Integer, Integer>();
    for (int index = 0; index < selected.size(); index++) {
      result.put(Integer.valueOf(selected.get(index).getOriginalOrder()), Integer.valueOf(index + 1));
    }
    return result;
  }

  private static String roleForSlot(int slot, int g1, int g4, int g2, int g3) {
    if (slot <= 0 || slot > g1 + g4 + g2 + g3) return "NONE";
    if (slot <= g1) return ZhangBoSubSwarm.G1_CMAX.name();
    if (slot <= g1 + g4) return ZhangBoSubSwarm.G4_BALANCED.name();
    if (slot <= g1 + g4 + g2) return ZhangBoSubSwarm.G2_TEC.name();
    return ZhangBoSubSwarm.G3_TWC.name();
  }

  private static double[] scores(List<PermutationSolution<Integer>> pool) {
    double[] score = new double[pool.size()];
    for (int left = 0; left < pool.size(); left++) {
      int dominates = 0;
      int dominatedBy = 0;
      for (int right = 0; right < pool.size(); right++) {
        if (left == right) continue;
        if (dominates(pool.get(left), pool.get(right))) dominates++;
        if (dominates(pool.get(right), pool.get(left))) dominatedBy++;
      }
      score[left] = dominatedBy + 1.0 / (dominates + 1.0);
    }
    return score;
  }

  private static List<Integer> scoreOrder(final double[] scores) {
    List<Integer> order = new ArrayList<Integer>();
    for (int i = 0; i < scores.length; i++) order.add(Integer.valueOf(i));
    Collections.sort(order, new Comparator<Integer>() {
      @Override public int compare(Integer left, Integer right) {
        int score = Double.compare(scores[left.intValue()], scores[right.intValue()]);
        return score != 0 ? score : Integer.compare(left.intValue(), right.intValue());
      }
    });
    return order;
  }

  private static boolean[] firstObjectiveTriples(List<PermutationSolution<Integer>> pool) {
    boolean[] unique = new boolean[pool.size()];
    Set<String> seen = new HashSet<String>();
    for (int i = 0; i < pool.size(); i++) {
      String key = objectiveKey(pool.get(i));
      unique[i] = seen.add(key);
    }
    return unique;
  }

  private static int strictNondominatedCount(List<PermutationSolution<Integer>> pool, boolean[] unique) {
    int result = 0;
    for (int left = 0; left < pool.size(); left++) {
      if (!unique[left]) continue;
      boolean dominated = false;
      for (int right = 0; right < pool.size(); right++) {
        if (!unique[right] || left == right) continue;
        if (dominates(pool.get(right), pool.get(left))) {
          dominated = true;
          break;
        }
      }
      if (!dominated) result++;
    }
    return result;
  }

  private static int count(boolean[] values) {
    int result = 0;
    for (boolean value : values) if (value) result++;
    return result;
  }

  private static boolean dominates(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    boolean noWorse = true;
    boolean better = false;
    for (int objective : OBJECTIVES) {
      double a = left.getObjective(objective);
      double b = right.getObjective(objective);
      noWorse &= a <= b;
      better |= a < b;
    }
    return noWorse && better;
  }

  private static int objectiveWinner(List<PermutationSolution<Integer>> pool, final int primary) {
    int best = 0;
    int[] order = primary == 0 ? new int[]{0, 1, 6}
        : primary == 1 ? new int[]{1, 0, 6} : new int[]{6, 0, 1};
    for (int index = 1; index < pool.size(); index++) {
      if (lexicographicallyBetter(pool.get(index), pool.get(best), order)) best = index;
    }
    return best;
  }

  private static int balancedWinner(List<PermutationSolution<Integer>> pool,
      double[] min, double[] max) {
    int best = 0;
    double bestPhi = ZhangBoSubSwarmSemantics.balancedPhi(pool.get(best), min, max);
    for (int index = 1; index < pool.size(); index++) {
      double value = ZhangBoSubSwarmSemantics.balancedPhi(pool.get(index), min, max);
      if (value < bestPhi || (Double.compare(value, bestPhi) == 0
          && lexicographicallyBetter(pool.get(index), pool.get(best), new int[]{0, 1, 6}))) {
        best = index;
        bestPhi = value;
      }
    }
    return best;
  }

  private static boolean lexicographicallyBetter(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right, int[] order) {
    for (int objective : order) {
      int compare = Double.compare(left.getObjective(objective), right.getObjective(objective));
      if (compare != 0) return compare < 0;
    }
    return false;
  }

  private static double[] bounds(List<PermutationSolution<Integer>> pool, boolean minimum) {
    double[] result = new double[]{minimum ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY,
        minimum ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY,
        minimum ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY};
    for (PermutationSolution<Integer> value : pool) {
      double[] objectives = objectives(value);
      for (int i = 0; i < objectives.length; i++) {
        result[i] = minimum ? Math.min(result[i], objectives[i]) : Math.max(result[i], objectives[i]);
      }
    }
    return result;
  }

  private static boolean improvesRepresentativeDirection(String label, double[] child,
      double[] parent) {
    if ("E_E".equals(label)) return child[1] < parent[1];
    if ("E_W".equals(label)) return child[2] < parent[2];
    if ("E_B".equals(label)) {
      boolean noWorse = child[0] <= parent[0] && child[1] <= parent[1]
          && child[2] <= parent[2];
      return noWorse && (child[0] < parent[0] || child[1] < parent[1]
          || child[2] < parent[2]);
    }
    return child[0] < parent[0];
  }

  private static double best(List<PermutationSolution<Integer>> solutions, int objective) {
    if (solutions == null || solutions.isEmpty()) return Double.NaN;
    double best = Double.POSITIVE_INFINITY;
    for (PermutationSolution<Integer> solution : solutions) {
      best = Math.min(best, solution.getObjective(objective));
    }
    return best;
  }

  private static double[] objectives(PermutationSolution<Integer> value) {
    return new double[]{value.getObjective(0), value.getObjective(1), value.getObjective(6)};
  }

  private static String objectiveKey(PermutationSolution<Integer> value) {
    return Long.toHexString(Double.doubleToLongBits(value.getObjective(0))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(1))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(6)));
  }

  private static String fingerprint(PermutationSolution<Integer> value) {
    return ZhangBoQgController.fingerprint(value);
  }

  private static final class Round {
    private final int cycle;
    private final long fe;
    private final int merge;
    private final int unique;
    private final int nd;
    private final double overflow;
    private Round(int cycle, long fe, int merge, int unique, int nd, double overflow) {
      this.cycle = cycle; this.fe = fe; this.merge = merge; this.unique = unique;
      this.nd = nd; this.overflow = overflow;
    }
  }

  private static final class Window {
    private final long end;
    private long rounds;
    private double merge;
    private double nd;
    private double overflow;
    private int maxNd;
    private double maxOverflow;
    private Window(long end) { this.end = end; }
    private void add(Round value) {
      rounds++; merge += value.merge; nd += value.nd; overflow += value.overflow;
      maxNd = Math.max(maxNd, value.nd); maxOverflow = Math.max(maxOverflow, value.overflow);
    }
    private String csv(long seed) {
      return seed + "," + end + "," + rounds + "," + merge / rounds + "," + nd / rounds
          + "," + maxNd + "," + overflow / rounds + "," + maxOverflow + "\n";
    }
  }

  private static final class Representative {
    private final int cycle;
    private final long fe;
    private final String label;
    private final int poolIndex;
    private final ZhangBoEvaluatedPddrSelector.Source source;
    private final String fingerprint;
    private final double[] objectives;
    private final double score;
    private final int rank;
    private final boolean pddrSelected;
    private final String rejectReason;
    private final int nextSlot;
    private final String nextRole;
    private long qgTeacherUses;
    private long qpTeacherUses;
    private final List<Integer> teacherUseCycles = new ArrayList<Integer>();
    private long improvedOffspringCount;
    private long lastImprovementFe = -1L;
    private String lastImprovementTeacherKind = "NONE";
    private String lastImprovementRequestingRole = "NONE";
    private long lastTeacherFe = -1L;
    private String lastTeacherRole = "NONE";
    private int retiredAtCycle = -1;
    private Representative(int cycle, long fe, String label, int poolIndex,
        ZhangBoEvaluatedPddrSelector.Source source, String fingerprint, double[] objectives,
        double score, int rank, boolean pddrSelected, String rejectReason, int nextSlot,
        String nextRole) {
      this.cycle = cycle; this.fe = fe; this.label = label; this.poolIndex = poolIndex;
      this.source = source; this.fingerprint = fingerprint; this.objectives = objectives;
      this.score = score; this.rank = rank; this.pddrSelected = pddrSelected;
      this.rejectReason = rejectReason; this.nextSlot = nextSlot; this.nextRole = nextRole;
    }
    private String csv(long seed) {
      StringBuilder cycles = new StringBuilder();
      for (int i = 0; i < teacherUseCycles.size(); i++) {
        if (i > 0) cycles.append(';');
        cycles.append(teacherUseCycles.get(i));
      }
      return seed + "," + cycle + "," + fe + "," + label + "," + poolIndex + "," + source
          + "," + fingerprint + "," + objectives[0] + "," + objectives[1] + "," + objectives[2]
          + "," + score + "," + rank + ",true," + pddrSelected + "," + rejectReason + ","
          + nextSlot + "," + nextRole + "," + qgTeacherUses + "," + qpTeacherUses + ","
          + cycles + "," + improvedOffspringCount + "," + lastImprovementFe + ","
          + lastImprovementTeacherKind + "," + lastImprovementRequestingRole + ","
          + lastTeacherFe + "," + lastTeacherRole + "," + retiredAtCycle + "\n";
    }
  }

  private static final class PendingOffspring {
    private final Representative representative;
    private final String teacherKind;
    private final ZhangBoSubSwarm requestingRole;
    private final double[] parentObjectives;
    private PendingOffspring(Representative representative, String teacherKind,
        ZhangBoSubSwarm requestingRole, double[] parentObjectives) {
      this.representative = representative; this.teacherKind = teacherKind;
      this.requestingRole = requestingRole; this.parentObjectives = parentObjectives;
    }
  }

  private static final class ArchiveWorkingGap {
    private final int cycle;
    private final long fe;
    private final double workingCmax;
    private final double archiveCmax;
    private final double workingTec;
    private final double archiveTec;
    private final double workingTwc;
    private final double archiveTwc;
    private final int workingSize;
    private final int archiveSize;
    private ArchiveWorkingGap(int cycle, long fe, double workingCmax, double archiveCmax,
        double workingTec, double archiveTec, double workingTwc, double archiveTwc,
        int workingSize, int archiveSize) {
      this.cycle = cycle; this.fe = fe; this.workingCmax = workingCmax;
      this.archiveCmax = archiveCmax; this.workingTec = workingTec; this.archiveTec = archiveTec;
      this.workingTwc = workingTwc; this.archiveTwc = archiveTwc;
      this.workingSize = workingSize; this.archiveSize = archiveSize;
    }
    private String csv(long seed) {
      return seed + "," + cycle + "," + fe + "," + workingCmax + "," + archiveCmax + ","
          + (workingCmax - archiveCmax) + "," + workingTec + "," + archiveTec + ","
          + (workingTec - archiveTec) + "," + workingTwc + "," + archiveTwc + ","
          + (workingTwc - archiveTwc) + "," + workingSize + "," + archiveSize + "\n";
    }
  }
}
