package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-SOURCE-LEDGER (2026-08-31): pure observation hook for the
 * source-contribution diagnostics.  Collects (a) an evaluation-ordered
 * per-candidate ledger (fingerprint, source label, FE, objectives,
 * parentLineageId) and (b) a per-outer-cycle merge-pool ledger (pool rows with
 * selector-side source labels, and PDDR selection outcomes matched back to the
 * pool by fingerprint multiset).  Never enters the search archive, never
 * changes PDDR input or teacher selection, never consumes randomness, never
 * evaluates anything, never mutates candidates.
 */
public final class V35SourceLedgerHook {

  private static boolean armed = false;
  private static long errorCount = 0L;
  private static String lastError = "";
  private static long unsetSourceRows = 0L;
  private static final StringBuilder evaluationLedger = new StringBuilder(
      "observedFE,source,candidateFingerprint,Cmax,TEC,TWC,parentLineageId\n");
  private static final StringBuilder pddrLedger = new StringBuilder(
      "cycle,fe,poolIndex,selectorSource,candidateFingerprint,enteredMergePool,"
      + "selectedByPddr,selectedRank,pddrScore\n");
  private static long pddrRoundCount = 0L;

  private V35SourceLedgerHook() { }

  public static void arm() {
    disarm();
    armed = true;
  }

  public static void disarm() {
    armed = false;
    errorCount = 0L;
    lastError = "";
    unsetSourceRows = 0L;
    evaluationLedger.setLength(0);
    evaluationLedger.append(
        "observedFE,source,candidateFingerprint,Cmax,TEC,TWC,parentLineageId\n");
    pddrLedger.setLength(0);
    pddrLedger.append(
        "cycle,fe,poolIndex,selectorSource,candidateFingerprint,enteredMergePool,"
        + "selectedByPddr,selectedRank,pddrScore\n");
    pddrRoundCount = 0L;
  }

  public static boolean isArmed() { return armed; }

  /** Called by the shadowed passive archive after every admitted observation. */
  static void onEvaluated(PermutationSolution<Integer> evaluated,
      V35EvaluationSourceContext.Source source, long observedCount) {
    if (!armed) return;
    try {
      String sourceName = source == null ? "UNSET" : source.name();
      if (source == null) unsetSourceRows++;
      long parentLineage = -1L;
      Object tag = evaluated.getAttribute(ZhangBoLineageTag.class);
      if (tag instanceof ZhangBoLineageTag) {
        parentLineage = ((ZhangBoLineageTag) tag).getParentLineageId();
      }
      appendRow(evaluationLedger, observedCount, sourceName,
          fingerprint(evaluated), evaluated.getObjective(0),
          evaluated.getObjective(1), evaluated.getObjective(6), parentLineage);
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  /**
   * Called by the shadowed algorithm right after PDDR selection.  Pool rows
   * mirror the selector-input order (global offspring, then CA-TA locals, then
   * parents); selected candidates are matched back to pool rows by fingerprint
   * multiset (rank = selector return order).
   */
  public static void onPddrRound(List<PermutationSolution<Integer>> poolSolutions,
      List<String> poolSourceNames, List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int outerCycle) {
    if (!armed) return;
    try {
      pddrRoundCount++;
      Map<String, java.util.ArrayDeque<double[]>> selectedByFingerprint =
          new HashMap<String, java.util.ArrayDeque<double[]>>();
      for (int rank = 0; rank < selected.size(); rank++) {
        ZhangBoEvaluatedPddrSelector.Candidate candidate = selected.get(rank);
        String fp = fingerprint(candidate.getSolution());
        java.util.ArrayDeque<double[]> queue = selectedByFingerprint.get(fp);
        if (queue == null) {
          queue = new java.util.ArrayDeque<double[]>();
          selectedByFingerprint.put(fp, queue);
        }
        queue.add(new double[]{rank + 1, candidate.getPddrScore()});
      }
      for (int index = 0; index < poolSolutions.size(); index++) {
        String fp = fingerprint(poolSolutions.get(index));
        java.util.ArrayDeque<double[]> queue = selectedByFingerprint.get(fp);
        boolean sel = false;
        double rank = -1;
        double score = Double.NaN;
        if (queue != null && !queue.isEmpty()) {
          double[] hit = queue.poll();
          sel = true;
          rank = hit[0];
          score = hit[1];
        }
        appendPddrRow(pddrLedger, outerCycle, fe, index + 1,
            poolSourceNames.get(index), fp, sel, rank, score);
      }
      int leftover = 0;
      for (java.util.ArrayDeque<double[]> queue : selectedByFingerprint.values()) {
        leftover += queue.size();
      }
      if (leftover != 0) {
        fail("selectedRowsNotMatchedToPool=" + leftover);
      }
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  public static String getEvaluationLedgerCsv() {
    return evaluationLedger.toString();
  }

  public static String getPddrLedgerCsv() {
    return pddrLedger.toString();
  }

  public static long getPddrRoundCount() { return pddrRoundCount; }

  public static long getErrorCount() { return errorCount; }

  public static String getLastError() { return lastError; }

  public static long getUnsetSourceRows() { return unsetSourceRows; }

  private static void fail(String message) {
    errorCount++;
    lastError = message;
  }

  private static void appendRow(StringBuilder out, Object... fields) {
    for (int index = 0; index < fields.length; index++) {
      if (index > 0) out.append(',');
      out.append(fields[index]);
    }
    out.append('\n');
  }

  private static void appendPddrRow(StringBuilder out, long cycle, long fe, long poolIndex,
      String source, String fingerprint, boolean selected, double rank, double score) {
    out.append(cycle).append(',').append(fe).append(',').append(poolIndex).append(',')
        .append(source).append(',').append(fingerprint).append(",true,")
        .append(selected).append(',').append(rank).append(',')
        .append(Double.isNaN(score) ? "NOT_EXPORTED_AT_POOL_LEVEL" : score)
        .append('\n');
  }

  /** Canonical candidate fingerprint: SHA-256 over the frozen four-vector
   *  fingerprint text (hex output keeps the CSV comma-safe). */
  private static String fingerprint(PermutationSolution<Integer> solution) {
    String raw = ZhangBoQgController.fingerprint(solution);
    try {
      byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
          .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
