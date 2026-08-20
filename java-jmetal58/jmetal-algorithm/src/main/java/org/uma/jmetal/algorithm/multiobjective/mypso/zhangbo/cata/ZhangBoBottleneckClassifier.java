package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;

/**
 * Deterministic six-way bottleneck classifier driven only by an evaluated
 * fatigue trace. Scores are relative diagnostic signals, not extra objectives.
 */
public final class ZhangBoBottleneckClassifier {
  public static final double DEFAULT_WARNING_THRESHOLD = 0.80;
  private static final double EPSILON = 1.0e-12;

  public static final class Classification {
    private final ZhangBoBottleneck bottleneck;
    private final Map<ZhangBoBottleneck, Double> rawScores;
    private final Map<ZhangBoBottleneck, Double> normalizedScores;

    private Classification(ZhangBoBottleneck bottleneck,
        Map<ZhangBoBottleneck, Double> rawScores,
        Map<ZhangBoBottleneck, Double> normalizedScores) {
      this.bottleneck = bottleneck;
      this.rawScores = Collections.unmodifiableMap(new EnumMap<>(rawScores));
      this.normalizedScores = Collections.unmodifiableMap(new EnumMap<>(normalizedScores));
    }

    public ZhangBoBottleneck getBottleneck() { return bottleneck; }
    public double getRawScore(ZhangBoBottleneck value) { return rawScores.get(value); }
    public double getNormalizedScore(ZhangBoBottleneck value) {
      return normalizedScores.get(value);
    }
    public Map<ZhangBoBottleneck, Double> getRawScores() { return rawScores; }
    public Map<ZhangBoBottleneck, Double> getNormalizedScores() { return normalizedScores; }

    public String toCanonicalText() {
      StringBuilder out = new StringBuilder("bottleneck=").append(bottleneck).append('\n');
      for (ZhangBoBottleneck value : ZhangBoBottleneck.values()) {
        out.append(value).append(".raw=").append(rawScores.get(value)).append('\n')
            .append(value).append(".normalized=").append(normalizedScores.get(value))
            .append('\n');
      }
      return out.toString();
    }
  }

  private final double warningThreshold;

  public ZhangBoBottleneckClassifier() { this(DEFAULT_WARNING_THRESHOLD); }

  public ZhangBoBottleneckClassifier(double warningThreshold) {
    if (!Double.isFinite(warningThreshold) || warningThreshold < 0.0 || warningThreshold >= 1.0) {
      throw new IllegalArgumentException("warningThreshold must be finite and in [0,1)");
    }
    this.warningThreshold = warningThreshold;
  }

  public Classification classify(ZhangBoFatigueEvaluationResult evaluation) {
    if (evaluation == null) throw new IllegalArgumentException("evaluation");
    return classify(evaluation.getOperations());
  }

  public Classification classify(List<ZhangBoFatigueOperationRecord> operations) {
    if (operations == null) throw new IllegalArgumentException("operations");
    Map<ZhangBoBottleneck, Double> raw = zeros();
    if (operations.isEmpty()) return classification(raw, ZhangBoBottleneck.BAL);

    double setup = 0.0;
    double fatigue = 0.0;
    Map<Integer, Double> factoryWork = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      raw.put(ZhangBoBottleneck.SEQ, raw.get(ZhangBoBottleneck.SEQ)
          + Math.max(0.0, operation.start - operation.predecessorCompletion));
      raw.put(ZhangBoBottleneck.MAC, raw.get(ZhangBoBottleneck.MAC)
          + Math.max(0.0, operation.start - operation.machineAvailableBefore));
      raw.put(ZhangBoBottleneck.WOR, raw.get(ZhangBoBottleneck.WOR)
          + Math.max(0.0, operation.start - operation.workerAvailableBefore));
      setup += Math.max(0.0, operation.baseSetupDuration);
      fatigue += Math.max(0.0, operation.fatigueAfter - warningThreshold)
          * Math.max(0.0, operation.actualDuration);
      Double accumulated = factoryWork.get(operation.factory);
      factoryWork.put(operation.factory, (accumulated == null ? 0.0 : accumulated)
          + Math.max(0.0, operation.actualDuration));
    }
    raw.put(ZhangBoBottleneck.SET, setup);
    raw.put(ZhangBoBottleneck.FAT, fatigue);
    raw.put(ZhangBoBottleneck.BAL, imbalance(factoryWork));
    return classification(raw, null);
  }

  private static Classification classification(
      Map<ZhangBoBottleneck, Double> raw, ZhangBoBottleneck forced) {
    double maximum = 0.0;
    for (ZhangBoBottleneck value : ZhangBoBottleneck.values()) {
      double score = Math.max(0.0, raw.get(value));
      raw.put(value, score);
      maximum = Math.max(maximum, score);
    }
    Map<ZhangBoBottleneck, Double> normalized = zeros();
    if (maximum <= EPSILON) {
      return new Classification(forced == null ? ZhangBoBottleneck.BAL : forced, raw, normalized);
    }
    ZhangBoBottleneck best = forced == null ? ZhangBoBottleneck.SEQ : forced;
    for (ZhangBoBottleneck value : ZhangBoBottleneck.values()) {
      double score = raw.get(value) / maximum;
      normalized.put(value, score);
      if (forced == null && score > normalized.get(best) + EPSILON) best = value;
    }
    return new Classification(best, raw, normalized);
  }

  private static Map<ZhangBoBottleneck, Double> zeros() {
    Map<ZhangBoBottleneck, Double> result = new EnumMap<>(ZhangBoBottleneck.class);
    for (ZhangBoBottleneck value : ZhangBoBottleneck.values()) result.put(value, 0.0);
    return result;
  }

  private static double imbalance(Map<Integer, Double> workloads) {
    if (workloads.size() < 2) return 0.0;
    double sum = 0.0;
    for (double value : workloads.values()) sum += value;
    double mean = sum / workloads.size();
    if (mean <= EPSILON) return 0.0;
    double squared = 0.0;
    for (double value : workloads.values()) {
      double delta = value - mean;
      squared += delta * delta;
    }
    return Math.sqrt(squared / workloads.size()) / mean;
  }
}
