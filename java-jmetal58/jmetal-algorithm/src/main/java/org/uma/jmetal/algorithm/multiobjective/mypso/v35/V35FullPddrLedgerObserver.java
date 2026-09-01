package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC5-MIDHORIZON-DIAGNOSTICS-V2 PDDR full ledger observer.
 *
 * <p>Pure observation: records every MergePool candidate of every real PDDR
 * round.  It reads the same candidate ordering passed to GLOBAL_ORIGINAL but
 * never supplies a score, candidate, random draw or control decision back to
 * the algorithm.  Compaction is explained by cutoff / margin / source mix,
 * never by a fake one-to-one "displacer".
 */
public final class V35FullPddrLedgerObserver {
  public static final String VERSION = "V35_MIDHORIZON_V2";
  private static final int[] OBJECTIVES = new int[]{0, 1, 6};
  private static final int TARGET_WORKING_POPULATION = 100;

  private boolean enabled;
  private final String runId;
  private final String sourceJarSha256;
  private final String configurationHash;
  private final String instanceHash;
  private final long seed;
  private final String arm;
  private final String telemetryMode;
  private final int[] physicalCapacities;
  private final boolean lineageApplicable;
  private long observerErrors;
  private final List<String> ledgerRows = new ArrayList<String>();
  private final List<String> cycleRows = new ArrayList<String>();
  private final List<V35PddrCandidateMetadata> metadataRows =
      new ArrayList<V35PddrCandidateMetadata>();

  private static final String LEDGER_HEADER =
      "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
      + "cycle,generation,actualFE,candidateOrdinal,candidateId,candidateFingerprint,"
      + "stableFingerprint,objectives[Cmax|TEC|TWC],source,parentId,parentSlot,lineageId,"
      + "parentLineageId,physicalSlotBefore,semanticRoleBefore,isNewCandidate,preEvaluated,"
      + "pddrScore,originalOrder,selectedByPddr,selectedRank,selectedSlot,semanticRoleAfter,"
      + "isDirectionalCmaxRepresentative,isDirectionalTecRepresentative,"
      + "isDirectionalTwcRepresentative,isBalancedRepresentative,cutoffScore,cutoffMargin,"
      + "rejectionReason,metadataUnavailableReasons\n";

  public V35FullPddrLedgerObserver(String runId, String sourceJarSha256,
      String configurationHash, String instanceHash, long seed, String arm, boolean enabled) {
    this(runId, sourceJarSha256, configurationHash, instanceHash, seed, arm, enabled, null, true);
  }

  /**
   * Optional explicit physical capacities used only by the B-side contract
   * checker.  A null value is intentional when the real call point does not
   * expose the capacity vector.
   */
  public V35FullPddrLedgerObserver(String runId, String sourceJarSha256,
      String configurationHash, String instanceHash, long seed, String arm, boolean enabled,
      int[] physicalCapacities) {
    this(runId, sourceJarSha256, configurationHash, instanceHash, seed, arm, enabled,
        physicalCapacities, true);
  }

  public V35FullPddrLedgerObserver(String runId, String sourceJarSha256,
      String configurationHash, String instanceHash, long seed, String arm, boolean enabled,
      int[] physicalCapacities, boolean lineageApplicable) {
    this.runId = runId;
    this.sourceJarSha256 = sourceJarSha256;
    this.configurationHash = configurationHash;
    this.instanceHash = instanceHash;
    this.seed = seed;
    this.arm = arm;
    this.enabled = enabled;
    this.telemetryMode = enabled ? "ON" : "OFF";
    this.physicalCapacities = physicalCapacities == null ? null : physicalCapacities.clone();
    this.lineageApplicable = lineageApplicable;
  }

  public void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      ledgerRows.clear();
      cycleRows.clear();
      metadataRows.clear();
      observerErrors = 0L;
    }
  }

  public boolean isEnabled() { return enabled; }

  /**
   * Records one full PDDR round.  {@code pool} and {@code sources} must be the
   * exact merged candidate list and parallel source list; {@code selected} is
   * the reconstructed selection list from the selector.
   */
  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected, long fe, int cycle) {
    onPddrRound(pool, sources, selected, fe, cycle, -1);
  }

  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected, long fe, int cycle, int generation) {
    if (!enabled) return;
    try {
      if (pool == null || sources == null || selected == null
          || pool.size() != sources.size()) {
        throw new IllegalArgumentException("unaligned PDDR ledger inputs");
      }
      // This is the first observer-visible point after the existing selector
      // call.  It is a read-only pre-recording snapshot.  The current
      // forbidden call point does not pass a pre-selector metadata list; the
      // adapter therefore reports every unavailable field explicitly.
      final List<V35PddrCandidateMetadata> roundMetadata =
          V35PddrCandidateMetadataAdapter.capture(pool, sources, selected, lineageApplicable);
      final double[] scores = scores(pool);
      final List<Integer> ranked = scoreOrder(scores);
      final Map<Integer, Integer> selectedSlotByOrder = selectedSlots(selected);
      final Map<Integer, ZhangBoEvaluatedPddrSelector.Candidate> selectedCandidateByOrder =
          selectedCandidates(selected);
      final boolean[] unique = firstObjectiveTriples(pool);
      final int uniqueCount = count(unique);
      final int strictNd = strictNondominatedCount(pool, unique);
      double[] cutoffScore = cutoffAtRank(scores, ranked, TARGET_WORKING_POPULATION);
      int[] winners = new int[]{
          objectiveWinner(pool, 0), objectiveWinner(pool, 1), objectiveWinner(pool, 6),
          balancedWinner(pool, bounds(pool, true), bounds(pool, false))
      };
      boolean[] isDirectional = new boolean[pool.size()];
      for (int winner : winners) if (winner >= 0 && winner < pool.size()) isDirectional[winner] = true;
      int selectedCount = 0;
      List<String> roundRows = new ArrayList<String>(pool.size());
      for (int index = 0; index < pool.size(); index++) {
        boolean selectedNow = selectedSlotByOrder.containsKey(index);
        if (selectedNow) selectedCount++;
        int rank = ranked.indexOf(index) + 1;
        int slot = selectedNow ? selectedSlotByOrder.get(index) : -1;
        boolean keepFullVector = isDirectional[index]
            || (rank >= 90 && rank <= 115)
            || (!selectedNow && isDirectional[index]);
        String objectivesText = keepFullVector
            ? pool.get(index).getObjective(0) + "|" + pool.get(index).getObjective(1)
                + "|" + pool.get(index).getObjective(6)
            : "FINGERPRINT_ONLY";
        String rejectionReason = selectedNow ? "SELECTED_BY_PDDR"
            : "PDDR_SCORE_RANK_NOT_SELECTED";
        String slotText = selectedNow ? String.valueOf(slot) : "NOT_SELECTED";
        String roleAfter = semanticRoleAfter(selectedNow ? selectedCandidateByOrder.get(index) : null);
        V35PddrCandidateMetadata metadata = roundMetadata.get(index);
        String genText = generation >= 0 ? String.valueOf(generation)
            : V35PddrCandidateMetadata.UnobservableReason.GENERATION.token();
        List<String> fields = new ArrayList<String>();
        fields.add(runId);
        fields.add(sourceJarSha256);
        fields.add(configurationHash);
        fields.add(instanceHash);
        fields.add(String.valueOf(seed));
        fields.add(arm);
        fields.add(telemetryMode);
        fields.add(String.valueOf(cycle));
        fields.add(genText);
        fields.add(String.valueOf(fe));
        fields.add(String.valueOf(index));
        fields.add(metadata.getCandidateId());
        fields.add(metadata.getCandidateFingerprint());
        fields.add(stableFingerprint(metadata.getCandidateFingerprint()));
        fields.add(objectivesText);
        fields.add(metadata.getSourceText());
        fields.add(metadata.getParentId());
        fields.add(metadata.getParentSlot());
        fields.add(metadata.getLineageId());
        fields.add(metadata.getParentLineageId());
        fields.add(metadata.getPhysicalSlotBefore());
        fields.add(metadata.getSemanticRoleBefore());
        fields.add(String.valueOf(metadata.isNewCandidate()));
        fields.add(String.valueOf(metadata.isPreEvaluated()));
        fields.add(String.valueOf(scores[index]));
        fields.add(String.valueOf(index));
        fields.add(String.valueOf(selectedNow));
        fields.add(String.valueOf(rank));
        fields.add(slotText);
        fields.add(roleAfter);
        fields.add(String.valueOf(isDirectional[index] && winners[0] == index));
        fields.add(String.valueOf(isDirectional[index] && winners[1] == index));
        fields.add(String.valueOf(isDirectional[index] && winners[2] == index));
        fields.add(String.valueOf(isDirectional[index] && winners[3] == index));
        fields.add(String.valueOf(cutoffScore[0]));
        fields.add(String.valueOf(marginToCutoff(scores[index], cutoffScore[1])));
        fields.add(rejectionReason);
        fields.add(metadata.unavailableReasonsText());
        roundRows.add(joinCsv(fields));
      }
      ledgerRows.addAll(roundRows);
      metadataRows.addAll(roundMetadata);
      cycleRows.add(runId + "," + sourceJarSha256 + "," + configurationHash + ","
          + instanceHash + "," + seed + "," + arm + "," + telemetryMode + ","
          + cycle + "," + fe + "," + pool.size() + "," + uniqueCount + "," + strictNd + ","
          + TARGET_WORKING_POPULATION + "," + cutoffScore[0] + "," + cutoffScore[1] + ","
          + scoreAt(pool, scores, ranked, 99) + "," + scoreAt(pool, scores, ranked, 100) + ","
          + scoreAt(pool, scores, ranked, 101) + "," + selectedCount);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  private static double marginToCutoff(double score, double cutoff) {
    return score - cutoff;
  }


  private static String semanticRoleAfter(
      ZhangBoEvaluatedPddrSelector.Candidate selectedCandidate) {
    if (selectedCandidate == null || selectedCandidate.getAssignedRegionRole() == null) {
      return V35PddrCandidateMetadata.NOT_APPLICABLE;
    }
    ZhangBoSubSwarm role = selectedCandidate.getAssignedRegionRole();
    // Keep the post-PDDR field tied to the selector's explicit assignment;
    // never infer it from the selected output slot.  This is not a survival
    // claim and is intentionally not named survivedNextGeneration.
    return ZhangBoSubSwarmSemantics.roleForPhysicalSlot(
        ZhangBoSubSwarmSemantics.physicalSlotForRole(role)).name();
  }

  private static double scoreAt(List<PermutationSolution<Integer>> pool,
      double[] scores, List<Integer> ranked, int rank1Based) {
    if (ranked.size() < rank1Based) return Double.NaN;
    int index = ranked.get(rank1Based - 1);
    return scores[index];
  }

  private static double[] cutoffAtRank(double[] scores, List<Integer> ranked, int rank1Based) {
    double[] result = new double[]{Double.NaN, Double.NaN};
    if (ranked.size() < rank1Based) return result;
    int index = ranked.get(rank1Based - 1);
    result[0] = scores[index];
    result[1] = scores[index];
    return result;
  }

  /** Reproduces the frozen GLOBAL_ORIGINAL author score: dominatedBy + 1/(dominates+1). */
  private static double[] scores(List<PermutationSolution<Integer>> values) {
    double[] result = new double[values.size()];
    for (int left = 0; left < values.size(); left++) {
      double dominates = 0.0;
      double dominatedBy = 0.0;
      for (int right = 0; right < values.size(); right++) {
        if (left == right) continue;
        PermutationSolution<Integer> x = values.get(left);
        PermutationSolution<Integer> y = values.get(right);
        if (strictlyDominates(x, y)) dominates++;
        if (strictlyDominates(y, x)) dominatedBy++;
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

  private static List<Integer> scoreOrder(double[] scores) {
    List<Integer> order = new ArrayList<Integer>();
    for (int index = 0; index < scores.length; index++) order.add(index);
    Collections.sort(order, new Comparator<Integer>() {
      @Override public int compare(Integer left, Integer right) {
        int comparison = Double.compare(scores[left], scores[right]);
        if (comparison != 0) return comparison;
        return Integer.compare(left, right);
      }
    });
    return order;
  }

  private static Map<Integer, Integer> selectedSlots(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected) {
    Map<Integer, Integer> byOrder = new HashMap<Integer, Integer>();
    for (int slot = 0; slot < selected.size(); slot++) {
      byOrder.put(selected.get(slot).getOriginalOrder(), slot);
    }
    return byOrder;
  }

  private static Map<Integer, ZhangBoEvaluatedPddrSelector.Candidate> selectedCandidates(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected) {
    Map<Integer, ZhangBoEvaluatedPddrSelector.Candidate> byOrder =
        new HashMap<Integer, ZhangBoEvaluatedPddrSelector.Candidate>();
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      if (candidate == null) throw new IllegalArgumentException("null selected candidate");
      if (byOrder.put(candidate.getOriginalOrder(), candidate) != null) {
        throw new IllegalArgumentException("duplicate selected original order");
      }
    }
    return byOrder;
  }

  private static boolean[] firstObjectiveTriples(List<PermutationSolution<Integer>> values) {
    Set<String> seen = new HashSet<String>();
    boolean[] unique = new boolean[values.size()];
    for (int index = 0; index < values.size(); index++) {
      unique[index] = seen.add(objectiveKey(values.get(index)));
    }
    return unique;
  }

  private static int strictNondominatedCount(List<PermutationSolution<Integer>> values,
      boolean[] unique) {
    int count = 0;
    for (int left = 0; left < values.size(); left++) {
      if (!unique[left]) continue;
      boolean dominated = false;
      for (int right = 0; right < values.size(); right++) {
        if (left == right) continue;
        if (strictlyDominates(values.get(right), values.get(left))) {
          dominated = true;
          break;
        }
      }
      if (!dominated) count++;
    }
    return count;
  }

  private static int count(boolean[] values) {
    int count = 0;
    for (boolean value : values) if (value) count++;
    return count;
  }

  private static double[] bounds(List<PermutationSolution<Integer>> values, boolean min) {
    double[] bounds = new double[3];
    for (int oi = 0; oi < OBJECTIVES.length; oi++) {
      int objective = OBJECTIVES[oi];
      bounds[oi] = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
      for (PermutationSolution<Integer> value : values) {
        double current = value.getObjective(objective);
        if (min) bounds[oi] = Math.min(bounds[oi], current);
        else bounds[oi] = Math.max(bounds[oi], current);
      }
    }
    return bounds;
  }

  private static int objectiveWinner(List<PermutationSolution<Integer>> values, int objective) {
    int winner = -1;
    for (int index = 0; index < values.size(); index++) {
      if (winner < 0 || values.get(index).getObjective(objective)
          < values.get(winner).getObjective(objective)) {
        winner = index;
      }
    }
    return winner;
  }

  private static int balancedWinner(List<PermutationSolution<Integer>> values,
      double[] min, double[] max) {
    int winner = -1;
    double winnerScore = Double.POSITIVE_INFINITY;
    for (int index = 0; index < values.size(); index++) {
      double[] normalized = new double[3];
      for (int objective = 0; objective < 3; objective++) {
        double range = max[objective] - min[objective];
        normalized[objective] = range <= 1.0e-12 ? 0.0
            : (values.get(index).getObjective(objective) - min[objective]) / range;
      }
      double score = Math.abs(normalized[0] - normalized[1])
          + Math.abs(normalized[1] - normalized[2])
          + Math.abs(normalized[2] - normalized[0]);
      if (score < winnerScore) {
        winnerScore = score;
        winner = index;
      }
    }
    return winner;
  }

  private static String objectiveKey(PermutationSolution<Integer> value) {
    return Long.toHexString(Double.doubleToLongBits(value.getObjective(0))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(1))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(6)));
  }

  private static String stableFingerprint(String candidateFingerprint) {
    if (candidateFingerprint == null || candidateFingerprint.startsWith("UNOBSERVABLE_")) {
      return candidateFingerprint == null ? "UNOBSERVABLE_CANDIDATE_FINGERPRINT"
          : candidateFingerprint;
    }
    return stableId(candidateFingerprint);
  }

  /** Deterministic comma-free hex id so CSV field splitting stays safe. */
  private static String stableId(String raw) {
    try {
      byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
          .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte b : digest) result.append(String.format("%02x", b & 0xff));
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public String ledgerCsv() {
    StringBuilder out = new StringBuilder(LEDGER_HEADER);
    for (String row : ledgerRows) out.append(row).append('\n');
    return out.toString();
  }

  public String cycleSummaryCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "cycle,FE,poolSize,uniqueObjectiveCount,strictNdCount,cutoffRank,cutoffScore,"
        + "cutoffScoreDup,scoreAtRank99,scoreAtRank100,scoreAtRank101,selectedCount\n");
    for (String row : cycleRows) out.append(row).append('\n');
    return out.toString();
  }

  public long getObserverErrors() { return observerErrors; }
  public int getRowCount() { return ledgerRows.size(); }

  public List<V35PddrCandidateMetadata> getMetadataRows() {
    return Collections.unmodifiableList(new ArrayList<V35PddrCandidateMetadata>(metadataRows));
  }

  public V35PddrCandidateMetadataAdapter.ContractReport getMetadataContractReport() {
    return V35PddrCandidateMetadataAdapter.validateContract(metadataRows, physicalCapacities,
        lineageApplicable);
  }

  public static int ledgerHeaderFieldCount() {
    return csvFieldCount(LEDGER_HEADER.substring(0, LEDGER_HEADER.length() - 1));
  }

  public static int csvFieldCount(String line) {
    if (line == null || line.length() == 0) return 0;
    int fields = 1;
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char value = line.charAt(index);
      if (value == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (value == ',' && !quoted) {
        fields++;
      }
    }
    return fields;
  }

  private static String joinCsv(List<String> fields) {
    StringBuilder result = new StringBuilder();
    for (String field : fields) {
      if (result.length() > 0) result.append(',');
      result.append(V35PddrCandidateMetadataAdapter.csv(field));
    }
    return result.toString();
  }
}
