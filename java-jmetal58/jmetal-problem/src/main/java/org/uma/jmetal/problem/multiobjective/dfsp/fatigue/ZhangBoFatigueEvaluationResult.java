package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftSummary;

/** Immutable fatigue evaluation result stored as a jMetal solution attribute. */
public final class ZhangBoFatigueEvaluationResult {
  private final String instanceSha256;
  private final String configurationSha256;
  private final String instanceExtensionSha256;
  private final List<ZhangBoFatigueOperationRecord> operations;
  private final ZhangBoFatigueMetrics metrics;
  private final double[] objectives;
  private final double[][][] completionMatrix;
  private final double[][][] energyMatrix;
  private final double[][][] costMatrix;
  private final String semanticTag;
  private final ZhangBoShiftSummary shiftSummary;
  private final ZhangBoDecoderTimingSample decoderTiming;

  public ZhangBoFatigueEvaluationResult(
      String instanceSha256, String configurationSha256,
      List<ZhangBoFatigueOperationRecord> operations,
      ZhangBoFatigueMetrics metrics, double[] objectives,
      double[][][] completionMatrix, double[][][] energyMatrix, double[][][] costMatrix) {
    this(instanceSha256, configurationSha256, "", operations, metrics, objectives,
        completionMatrix, energyMatrix, costMatrix, "fatigue_improved");
  }

  public ZhangBoFatigueEvaluationResult(
      String instanceSha256, String configurationSha256, String instanceExtensionSha256,
      List<ZhangBoFatigueOperationRecord> operations,
      ZhangBoFatigueMetrics metrics, double[] objectives,
      double[][][] completionMatrix, double[][][] energyMatrix, double[][][] costMatrix) {
    this(instanceSha256, configurationSha256, instanceExtensionSha256, operations, metrics,
        objectives, completionMatrix, energyMatrix, costMatrix, "fatigue_improved");
  }

  public ZhangBoFatigueEvaluationResult(
      String instanceSha256, String configurationSha256, String instanceExtensionSha256,
      List<ZhangBoFatigueOperationRecord> operations,
      ZhangBoFatigueMetrics metrics, double[] objectives,
      double[][][] completionMatrix, double[][][] energyMatrix, double[][][] costMatrix,
      String semanticTag) {
    this(instanceSha256, configurationSha256, instanceExtensionSha256, operations,
        metrics, objectives, completionMatrix, energyMatrix, costMatrix,
        semanticTag, null, null);
  }

  private ZhangBoFatigueEvaluationResult(
      String instanceSha256, String configurationSha256, String instanceExtensionSha256,
      List<ZhangBoFatigueOperationRecord> operations,
      ZhangBoFatigueMetrics metrics, double[] objectives,
      double[][][] completionMatrix, double[][][] energyMatrix, double[][][] costMatrix,
      String semanticTag, ZhangBoShiftSummary shiftSummary,
      ZhangBoDecoderTimingSample decoderTiming) {
    this.instanceSha256 = instanceSha256;
    this.configurationSha256 = configurationSha256;
    this.instanceExtensionSha256 = instanceExtensionSha256 == null ? "" : instanceExtensionSha256;
    this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
    this.metrics = metrics;
    this.objectives = objectives.clone();
    this.completionMatrix = copy(completionMatrix);
    this.energyMatrix = copy(energyMatrix);
    this.costMatrix = copy(costMatrix);
    if (semanticTag == null || semanticTag.isEmpty()) {
      throw new IllegalArgumentException("semanticTag must not be empty");
    }
    this.semanticTag = semanticTag;
    this.shiftSummary = shiftSummary;
    this.decoderTiming = decoderTiming;
  }

  public String getInstanceSha256() { return instanceSha256; }
  public String getConfigurationSha256() { return configurationSha256; }
  public String getInstanceExtensionSha256() { return instanceExtensionSha256; }
  public String getSemanticTag() { return semanticTag; }
  public List<ZhangBoFatigueOperationRecord> getOperations() { return operations; }
  public ZhangBoShiftSummary getShiftSummary() { return shiftSummary; }
  public ZhangBoDecoderTimingSample getDecoderTiming() { return decoderTiming; }
  public ZhangBoFatigueMetrics getMetrics() { return metrics; }
  public double[] getObjectives() { return objectives.clone(); }
  public double[][][] getCompletionMatrix() { return copy(completionMatrix); }
  public double[][][] getEnergyMatrix() { return copy(energyMatrix); }
  public double[][][] getCostMatrix() { return copy(costMatrix); }

  /** Returns a deep-copied result carrying an explicit production mode tag. */
  public ZhangBoFatigueEvaluationResult withSemanticTag(String tag) {
    return new ZhangBoFatigueEvaluationResult(instanceSha256, configurationSha256,
        instanceExtensionSha256, operations, metrics, objectives,
        completionMatrix, energyMatrix, costMatrix, tag, shiftSummary, decoderTiming);
  }

  public ZhangBoFatigueEvaluationResult withShiftSummary(ZhangBoShiftSummary summary) {
    if (summary == null) throw new IllegalArgumentException("shift summary must not be null");
    return new ZhangBoFatigueEvaluationResult(instanceSha256, configurationSha256,
        instanceExtensionSha256, operations, metrics, objectives,
        completionMatrix, energyMatrix, costMatrix, semanticTag, summary, decoderTiming);
  }

  public ZhangBoFatigueEvaluationResult withDecoderTiming(ZhangBoDecoderTimingSample timing) {
    if (timing == null) throw new IllegalArgumentException("decoder timing must not be null");
    return new ZhangBoFatigueEvaluationResult(instanceSha256, configurationSha256,
        instanceExtensionSha256, operations, metrics, objectives,
        completionMatrix, energyMatrix, costMatrix, semanticTag, shiftSummary, timing);
  }

  public byte[] toCanonicalUtf8() {
    StringBuilder out = new StringBuilder();
    out.append("schemaVersion=").append(shiftSummary == null ? "2" : "4")
        .append("\nsemanticTag=").append(semanticTag).append('\n')
        .append("instanceSha256=").append(instanceSha256).append('\n')
        .append("configurationSha256=").append(configurationSha256).append('\n')
        .append("instanceExtensionSha256=").append(instanceExtensionSha256).append('\n');
    if (shiftSummary != null) {
      out.append("shift.algorithmSemanticsVersion=")
          .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift
              .ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION).append('\n')
          .append("shift.mode=").append(shiftSummary.getMode().name()).append('\n')
          .append("shift.leftCandidates=").append(shiftSummary.getLeftCandidates()).append('\n')
          .append("shift.leftAccepted=").append(shiftSummary.getLeftAccepted()).append('\n')
          .append("shift.rightCandidates=").append(shiftSummary.getRightCandidates()).append('\n')
          .append("shift.rightAccepted=").append(shiftSummary.getRightAccepted()).append('\n')
          .append("shift.cmaxStar=").append(number(shiftSummary.getCmaxStar())).append('\n')
          .append("shift.internalPropagations=")
          .append(shiftSummary.getInternalPropagations()).append('\n')
          .append("shift.actionTraceSha256=").append(shiftSummary.getEventSha256()).append('\n')
          .append("shift.baseScheduleSha256=")
          .append(shiftSummary.getBaseScheduleSha256()).append('\n')
          .append("shift.afterLeftScheduleSha256=")
          .append(shiftSummary.getAfterLeftScheduleSha256()).append('\n')
          .append("shift.finalScheduleSha256=")
          .append(shiftSummary.getFinalScheduleSha256()).append('\n')
          .append("shift.evaluationTraceSha256=")
          .append(shiftSummary.getEvaluationTraceSha256()).append('\n');
    }
    for (int i = 0; i < objectives.length; i++) {
      out.append("objective.").append(i).append('=').append(number(objectives[i])).append('\n');
    }
    ZhangBoFatigueMetrics m = metrics;
    out.append("metric.Fmax=").append(number(m.maximumFatigue)).append('\n')
        .append("metric.Favg=").append(number(m.averageEventFatigue)).append('\n')
        .append("metric.FE=").append(number(m.fatigueExcessIntegral)).append('\n')
        .append("metric.VarFw=").append(number(m.workerFatigueVarianceAtMakespan)).append('\n')
        .append("metric.highFatigueRatio=").append(number(m.highFatigueTimeRatio)).append('\n')
        .append("metric.longestContinuousWork=").append(number(m.longestContinuousWork)).append('\n')
        .append("metric.totalNaturalRecovery=").append(number(m.totalNaturalRecovery)).append('\n')
        .append("metric.safeEvents=").append(m.safeThresholdEventCount).append('\n');
    out.append("operations=").append(operations.size()).append('\n');
    for (ZhangBoFatigueOperationRecord r : operations) {
      out.append("op=").append(r.sequence).append(',').append(r.job).append(',')
          .append(r.stage).append(',').append(r.factory).append(',').append(r.machine)
          .append(',').append(r.worker).append(',').append(number(r.predecessorCompletion))
          .append(',').append(number(r.machineAvailableBefore)).append(',')
          .append(number(r.workerAvailableBefore)).append(',').append(number(r.start))
          .append(',').append(number(r.recoveryDuration)).append(',')
          .append(number(r.fatigueBeforeRecovery)).append(',')
          .append(number(r.fatigueAtStart)).append(',')
          .append(number(r.baseProcessingDuration)).append(',')
          .append(number(r.baseSetupDuration)).append(',').append(number(r.baseDuration))
          .append(',').append(number(r.durationMultiplier)).append(',')
          .append(number(r.actualProcessingDuration)).append(',')
          .append(number(r.actualSetupDuration)).append(',')
          .append(number(r.actualDuration)).append(',').append(number(r.end))
          .append(',').append(number(r.fatigueAfter)).append(',').append(number(r.energy))
          .append(',').append(number(r.cost)).append(',').append(r.safeThresholdExceeded)
          .append('\n');
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }

  private static double[][][] copy(double[][][] source) {
    double[][][] result = new double[source.length][][];
    for (int i = 0; i < source.length; i++) {
      result[i] = new double[source[i].length][];
      for (int j = 0; j < source[i].length; j++) result[i][j] = source[i][j].clone();
    }
    return result;
  }
}
