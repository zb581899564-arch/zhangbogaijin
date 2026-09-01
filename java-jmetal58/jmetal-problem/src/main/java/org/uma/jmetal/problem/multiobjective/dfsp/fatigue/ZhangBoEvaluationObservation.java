package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/**
 * Purely observational integrity counters for a canonical production problem.
 * They never reject, repair, or otherwise influence an evaluation.
 */
public final class ZhangBoEvaluationObservation {
  /**
   * Observation must never retain every candidate in a long formal run.  The
   * bounded identity window catches the only meaningful failure here: an
   * unchanged candidate accidentally sent back to the decoder immediately or
   * in the same recent evaluation window.  It is never consulted by search.
   */
  private static final int MAX_RECENT_IDENTITIES = 16384;
  private final Map<IdentityReference, Long> lastFingerprints = new HashMap<>();
  private final MessageDigest evaluationTrace;
  private int duplicateEvaluations;
  private int illegalSolutions;

  public ZhangBoEvaluationObservation() {
    try {
      evaluationTrace = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  public synchronized void beforeEvaluation(DhhfspFourVectorSolution solution) {
    if (lastFingerprints.size() >= MAX_RECENT_IDENTITIES) {
      lastFingerprints.clear();
    }
    IdentityReference lookup = new IdentityReference(solution);
    long fingerprint = fingerprint(solution);
    Long prior = lastFingerprints.get(lookup);
    if (prior != null && prior.longValue() == fingerprint) {
      duplicateEvaluations++;
    }
    lastFingerprints.put(lookup, fingerprint);
  }

  public synchronized void recordIllegalSolution() {
    illegalSolutions++;
  }

  /** Records one successful, fully decoded evaluation without affecting search state. */
  public synchronized void afterEvaluation(DhhfspFourVectorSolution solution) {
    StringBuilder row = new StringBuilder();
    append(row, solution.getJobSequence());
    append(row, solution.getFactoryAssignments());
    append(row, solution.getMachineAssignments());
    append(row, solution.getWorkerAssignments());
    row.append('|').append(Double.toHexString(solution.getObjective(0)))
        .append('|').append(Double.toHexString(solution.getObjective(1)))
        .append('|').append(Double.toHexString(solution.getObjective(6))).append('\n');
    evaluationTrace.update(row.toString().getBytes(StandardCharsets.UTF_8));
  }

  public synchronized String getEvaluationTraceHash() {
    try {
      MessageDigest copy = (MessageDigest) evaluationTrace.clone();
      return hex(copy.digest());
    } catch (CloneNotSupportedException error) {
      throw new IllegalStateException("SHA-256 provider is not cloneable", error);
    }
  }

  public synchronized int getDuplicateEvaluations() {
    return duplicateEvaluations;
  }

  public synchronized int getIllegalSolutions() {
    return illegalSolutions;
  }

  public int getIdentityWindowSize() {
    return MAX_RECENT_IDENTITIES;
  }

  private static long fingerprint(DhhfspFourVectorSolution solution) {
    long value = 0xcbf29ce484222325L;
    value = append(value, solution.getJobSequence());
    value = append(value, solution.getFactoryAssignments());
    value = append(value, solution.getMachineAssignments());
    return append(value, solution.getWorkerAssignments());
  }

  private static long append(long value, java.util.List<Integer> values) {
    value ^= values.size(); value *= 0x100000001b3L;
    for (Integer entry : values) {
      value ^= entry == null ? 0xffffffffL : entry.intValue() & 0xffffffffL;
      value *= 0x100000001b3L;
    }
    return value;
  }

  private static void append(StringBuilder out, java.util.List<Integer> values) {
    out.append('[');
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) out.append(',');
      out.append(values.get(index));
    }
    out.append(']');
  }

  private static String hex(byte[] values) {
    StringBuilder out = new StringBuilder();
    for (byte value : values) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  private static final class IdentityReference extends WeakReference<DhhfspFourVectorSolution> {
    private final int identityHash;
    IdentityReference(DhhfspFourVectorSolution value) {
      super(value);
      this.identityHash = System.identityHashCode(value);
    }
    @Override public int hashCode() { return identityHash; }
    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof IdentityReference)) return false;
      DhhfspFourVectorSolution left = get();
      DhhfspFourVectorSolution right = ((IdentityReference) other).get();
      return left != null && left == right;
    }
  }
}
