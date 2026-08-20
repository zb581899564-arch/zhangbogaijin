package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;

/** Immutable summary attached only to shifted production results. */
public final class ZhangBoShiftSummary {
  private final ZhangBoShiftMode mode;
  private final double[] baseObjectives;
  private final double[] afterLeftObjectives;
  private final double[] finalObjectives;
  private final ZhangBoFatigueMetrics afterLeftMetrics;
  private final double cmaxStar;
  private final int leftCandidates;
  private final int leftAccepted;
  private final int rightCandidates;
  private final int rightAccepted;
  private final int leftFullRecomputations;
  private final int rightFullRecomputations;
  private final long leftShiftNanos;
  private final long rightShiftNanos;
  private final long propagationNanos;
  private final String eventSha256;
  private final String baseScheduleSha256;
  private final String afterLeftScheduleSha256;
  private final String finalScheduleSha256;
  private final String evaluationTraceSha256;
  private final List<ZhangBoShiftEvent> events;
  private final List<ZhangBoFatigueOperationRecord> baseOperations;
  private final List<ZhangBoFatigueOperationRecord> afterLeftOperations;

  public ZhangBoShiftSummary(
      ZhangBoShiftMode mode, double[] baseObjectives, double[] afterLeftObjectives,
      double[] finalObjectives, ZhangBoFatigueMetrics afterLeftMetrics, double cmaxStar,
      int leftCandidates, int leftAccepted,
      int rightCandidates, int rightAccepted,
      int leftFullRecomputations, int rightFullRecomputations,
      long leftShiftNanos, long rightShiftNanos, long propagationNanos,
      List<ZhangBoShiftEvent> allEvents, boolean captureFullTrace,
      List<ZhangBoFatigueOperationRecord> baseOperations,
      List<ZhangBoFatigueOperationRecord> afterLeftOperations,
      List<ZhangBoFatigueOperationRecord> finalOperations) {
    this.mode = mode;
    this.baseObjectives = baseObjectives.clone();
    this.afterLeftObjectives = afterLeftObjectives.clone();
    this.finalObjectives = finalObjectives.clone();
    this.afterLeftMetrics = afterLeftMetrics;
    this.cmaxStar = cmaxStar;
    this.leftCandidates = leftCandidates;
    this.leftAccepted = leftAccepted;
    this.rightCandidates = rightCandidates;
    this.rightAccepted = rightAccepted;
    this.leftFullRecomputations = leftFullRecomputations;
    this.rightFullRecomputations = rightFullRecomputations;
    this.leftShiftNanos = leftShiftNanos;
    this.rightShiftNanos = rightShiftNanos;
    this.propagationNanos = propagationNanos;
    this.eventSha256 = hash(allEvents);
    this.baseScheduleSha256 = hashOperations(baseOperations);
    this.afterLeftScheduleSha256 = hashOperations(afterLeftOperations);
    this.finalScheduleSha256 = hashOperations(finalOperations);
    this.evaluationTraceSha256 = hashText(eventSha256 + '\n'
        + objectives(baseObjectives) + '\n' + objectives(afterLeftObjectives) + '\n'
        + objectives(finalObjectives) + '\n' + metrics(afterLeftMetrics) + '\n'
        + Double.toString(cmaxStar) + '\n'
        + getInternalPropagations() + '\n');
    this.events = captureFullTrace
        ? Collections.unmodifiableList(new ArrayList<>(allEvents))
        : Collections.<ZhangBoShiftEvent>emptyList();
    this.baseOperations = captureFullTrace
        ? Collections.unmodifiableList(new ArrayList<>(baseOperations))
        : Collections.<ZhangBoFatigueOperationRecord>emptyList();
    this.afterLeftOperations = captureFullTrace
        ? Collections.unmodifiableList(new ArrayList<>(afterLeftOperations))
        : Collections.<ZhangBoFatigueOperationRecord>emptyList();
  }

  public ZhangBoShiftMode getMode() { return mode; }
  public double[] getBaseObjectives() { return baseObjectives.clone(); }
  public double[] getAfterLeftObjectives() { return afterLeftObjectives.clone(); }
  public double[] getFinalObjectives() { return finalObjectives.clone(); }
  public ZhangBoFatigueMetrics getAfterLeftMetrics() { return afterLeftMetrics; }
  public double getCmaxStar() { return cmaxStar; }
  public int getLeftCandidates() { return leftCandidates; }
  public int getLeftAccepted() { return leftAccepted; }
  public int getRightCandidates() { return rightCandidates; }
  public int getRightAccepted() { return rightAccepted; }
  public int getLeftFullRecomputations() { return leftFullRecomputations; }
  public int getRightFullRecomputations() { return rightFullRecomputations; }
  public int getInternalPropagations() {
    return leftFullRecomputations + rightFullRecomputations;
  }
  public long getLeftShiftNanos() { return leftShiftNanos; }
  public long getRightShiftNanos() { return rightShiftNanos; }
  public long getPropagationNanos() { return propagationNanos; }
  public String getEventSha256() { return eventSha256; }
  public String getBaseScheduleSha256() { return baseScheduleSha256; }
  public String getAfterLeftScheduleSha256() { return afterLeftScheduleSha256; }
  public String getFinalScheduleSha256() { return finalScheduleSha256; }
  public String getEvaluationTraceSha256() { return evaluationTraceSha256; }
  public List<ZhangBoShiftEvent> getEvents() { return events; }
  public List<ZhangBoFatigueOperationRecord> getBaseOperations() { return baseOperations; }
  public List<ZhangBoFatigueOperationRecord> getAfterLeftOperations() { return afterLeftOperations; }

  private static String hash(List<ZhangBoShiftEvent> events) {
    StringBuilder text = new StringBuilder();
    for (ZhangBoShiftEvent event : events) text.append(event.canonicalLine()).append('\n');
    return hashText(text.toString());
  }

  private static String hashOperations(List<ZhangBoFatigueOperationRecord> operations) {
    StringBuilder text = new StringBuilder();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      text.append(operation.job).append(',').append(operation.stage).append(',')
          .append(operation.factory).append(',').append(operation.machine).append(',')
          .append(operation.worker).append(',').append(Double.toString(operation.start))
          .append(',').append(Double.toString(operation.end)).append(',')
          .append(Double.toString(operation.fatigueAtStart)).append(',')
          .append(Double.toString(operation.fatigueAfter)).append('\n');
    }
    return hashText(text.toString());
  }

  private static String objectives(double[] values) {
    StringBuilder text = new StringBuilder();
    for (double value : values) text.append(Double.toString(value)).append(',');
    return text.toString();
  }

  private static String metrics(ZhangBoFatigueMetrics value) {
    return Double.toString(value.maximumFatigue) + ','
        + Double.toString(value.averageEventFatigue) + ','
        + Double.toString(value.fatigueExcessIntegral) + ','
        + Double.toString(value.workerFatigueVarianceAtMakespan) + ','
        + Double.toString(value.highFatigueTimeRatio) + ','
        + Double.toString(value.longestContinuousWork) + ','
        + Double.toString(value.totalNaturalRecovery) + ','
        + value.safeThresholdEventCount;
  }

  private static String hashText(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
