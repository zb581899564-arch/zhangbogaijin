package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-CHECKPOINT-OBSERVER (2026-08-31): pure observation-only checkpoint hook
 * for the LOCAL-FE-PACING 250k package.
 *
 * <p>Attached from the instrumented {@link V35FairRunner} copy at run start and
 * fired from the instrumented {@link V35PassiveEvaluationArchive} copy after
 * every admitted observation.  When not armed every entry point is a no-op, so
 * OFF mode is behaviourally the frozen runner.  The observer never enters the
 * search archive, never changes PDDR input or teacher selection, never draws
 * randomness, never evaluates anything and never mutates algorithm state: it
 * only copies objective values and candidate fingerprints of the decision
 * front ({@code ZhangBoMOHPSOQ.getResult()}) and of the passive archive
 * snapshot at deterministic instants.</p>
 *
 * <p>Freeze rule (task §4): the first admission whose archive
 * {@code observedCount >= targetFE}.  Because the archive count increments by
 * exactly one per admitted evaluation, the freeze lands at
 * {@code observedFE == targetFE} (overshootFE = 0) and the frozen
 * observed-full-front is exactly the non-dominated filter of the first
 * {@code targetFE} successful evaluations.</p>
 */
public final class V35CheckpointObserverHook {

  /** One frozen checkpoint: immutable extracted values, no solution references. */
  public static final class CheckpointRecord {
    public final long targetFE;
    public final long observedFE;
    public final long overshootFE;
    public final long counterFE;
    public final String decisionFrontCsv;
    public final String observedFullFrontCsv;

    CheckpointRecord(long targetFE, long observedFE, long overshootFE, long counterFE,
        String decisionFrontCsv, String observedFullFrontCsv) {
      this.targetFE = targetFE;
      this.observedFE = observedFE;
      this.overshootFE = overshootFE;
      this.counterFE = counterFE;
      this.decisionFrontCsv = decisionFrontCsv;
      this.observedFullFrontCsv = observedFullFrontCsv;
    }
  }

  private static long[] targets = null;
  private static int nextIndex = 0;
  private static ZhangBoCanonicalProductionProblem problem = null;
  private static ZhangBoMOHPSOQ algorithm = null;
  private static final List<CheckpointRecord> records = new ArrayList<CheckpointRecord>();
  private static String terminalDecisionCsv = "";
  private static String terminalObservedCsv = "";
  private static long terminalObservedFE = -1L;
  private static long terminalCounterFE = -1L;
  private static long errorCount = 0L;
  private static String lastError = "";

  private V35CheckpointObserverHook() { }

  /** Arms the observer with ascending checkpoint targets (called by the runner). */
  public static void arm(long[] checkpointTargets) {
    disarm();
    if (checkpointTargets == null || checkpointTargets.length == 0) {
      throw new IllegalArgumentException("checkpointTargets");
    }
    targets = checkpointTargets.clone();
    Arrays.sort(targets);
    nextIndex = 0;
    errorCount = 0L;
    lastError = "";
  }

  /** Clears all state so a later run in the same JVM starts clean (OFF). */
  public static void disarm() {
    targets = null;
    nextIndex = 0;
    problem = null;
    algorithm = null;
    records.clear();
    terminalDecisionCsv = "";
    terminalObservedCsv = "";
    terminalObservedFE = -1L;
    terminalCounterFE = -1L;
    errorCount = 0L;
    lastError = "";
  }

  /** Called by the instrumented runner; no-op unless armed. */
  static void attach(ZhangBoCanonicalProductionProblem canonical, ZhangBoMOHPSOQ algo) {
    if (targets == null) return;
    problem = canonical;
    algorithm = algo;
  }

  /** Called by the instrumented passive archive after every admission; no-op unless armed. */
  static void afterObserve(V35PassiveEvaluationArchive archive) {
    if (targets == null || problem == null || nextIndex >= targets.length) return;
    try {
      long observed = archive.getObservedCount();
      if (observed < targets[nextIndex]) return;
      long target = targets[nextIndex];
      long counterFE = problem.getEvaluationCounter().getSuccessfulEvaluations();
      if (counterFE < observed) {
        fail("evaluationCounterBehindArchive counter=" + counterFE + " archive=" + observed);
        return;
      }
      String decision = decisionFrontCsv();
      String observedFull = membersCsv(archive.snapshot());
      records.add(new CheckpointRecord(target, observed, observed - target, counterFE,
          decision, observedFull));
      nextIndex++;
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  /** Called by the instrumented runner at normal completion; no-op unless armed. */
  static void recordTerminal(List<PermutationSolution<Integer>> result,
      V35PassiveEvaluationArchive archive) {
    if (targets == null) return;
    try {
      StringBuilder out = new StringBuilder("candidateFingerprint,Cmax,TEC,TWC\n");
      for (PermutationSolution<Integer> solution : result) {
        appendPoint(out, fingerprint(solution), solution.getObjective(0),
            solution.getObjective(1), solution.getObjective(6));
      }
      terminalDecisionCsv = out.toString();
      if (archive != null) {
        terminalObservedCsv = membersCsv(archive.snapshot());
        terminalObservedFE = archive.getObservedCount();
        terminalCounterFE = problem == null ? -1L
            : problem.getEvaluationCounter().getSuccessfulEvaluations();
      }
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  public static boolean isArmed() { return targets != null; }
  public static List<CheckpointRecord> getRecords() {
    return new ArrayList<CheckpointRecord>(records);
  }
  public static String getTerminalDecisionCsv() { return terminalDecisionCsv; }
  public static String getTerminalObservedCsv() { return terminalObservedCsv; }
  public static long getTerminalObservedFE() { return terminalObservedFE; }
  public static long getTerminalCounterFE() { return terminalCounterFE; }
  public static long getErrorCount() { return errorCount; }
  public static String getLastError() { return lastError; }

  private static void fail(String message) {
    errorCount++;
    lastError = message;
  }

  private static String decisionFrontCsv() {
    StringBuilder out = new StringBuilder("candidateFingerprint,Cmax,TEC,TWC\n");
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      appendPoint(out, fingerprint(solution), solution.getObjective(0),
          solution.getObjective(1), solution.getObjective(6));
    }
    return out.toString();
  }

  private static String membersCsv(List<PermutationSolution<Integer>> members) {
    StringBuilder out = new StringBuilder("candidateFingerprint,Cmax,TEC,TWC\n");
    for (PermutationSolution<Integer> solution : members) {
      appendPoint(out, fingerprint(solution), solution.getObjective(0),
          solution.getObjective(1), solution.getObjective(6));
    }
    return out.toString();
  }

  private static void appendPoint(StringBuilder out, String fingerprint,
      double cmax, double tec, double twc) {
    out.append(fingerprint).append(',').append(cmax).append(',').append(tec)
        .append(',').append(twc).append('\n');
  }

  /** Canonical candidate fingerprint: SHA-256 over the same vector text the
   *  formal run hashes for initial-population identity (pure, no RNG; hex
   *  output keeps the checkpoint CSV comma-safe). */
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
