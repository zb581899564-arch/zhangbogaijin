package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.List;

/** Per-generation frozen normalization bounds for archive decisions. */
public final class ZhangBoArchiveBounds {
  private final double[] objectiveMin;
  private final double[] objectiveMax;
  private final double fatigueMaxMin;
  private final double fatigueMaxMax;
  private final double fatigueExcessMin;
  private final double fatigueExcessMax;
  private final double epsilon;

  private ZhangBoArchiveBounds(
      double[] objectiveMin, double[] objectiveMax,
      double fatigueMaxMin, double fatigueMaxMax,
      double fatigueExcessMin, double fatigueExcessMax,
      double epsilon) {
    this.objectiveMin = objectiveMin.clone();
    this.objectiveMax = objectiveMax.clone();
    this.fatigueMaxMin = fatigueMaxMin;
    this.fatigueMaxMax = fatigueMaxMax;
    this.fatigueExcessMin = fatigueExcessMin;
    this.fatigueExcessMax = fatigueExcessMax;
    this.epsilon = epsilon;
  }

  public static ZhangBoArchiveBounds fromSolutions(
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalNondominated,
      double epsilon) {
    return fromSolutions(population, globalNondominated, epsilon, false);
  }

  public static ZhangBoArchiveBounds fromSolutions(
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalNondominated,
      double epsilon, boolean allowMissingFatigueAsZero) {
    List<PermutationSolution<Integer>> values = new ArrayList<>();
    if (population != null) values.addAll(population);
    if (globalNondominated != null) values.addAll(globalNondominated);
    if (values.isEmpty()) throw new IllegalArgumentException("Archive bounds require solutions");
    double[] min = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY};
    double[] max = new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY};
    double fmaxMin = Double.POSITIVE_INFINITY;
    double fmaxMax = Double.NEGATIVE_INFINITY;
    double feMin = Double.POSITIVE_INFINITY;
    double feMax = Double.NEGATIVE_INFINITY;
    for (PermutationSolution<Integer> solution : values) {
      double[] objectives = new double[]{solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)};
      for (int objective = 0; objective < 3; objective++) {
        requireFinite(objectives[objective], "objective");
        min[objective] = Math.min(min[objective], objectives[objective]);
        max[objective] = Math.max(max[objective], objectives[objective]);
      }
      ZhangBoFatigueEvaluationResult fatigue =
          (ZhangBoFatigueEvaluationResult) solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
      if (fatigue == null) {
        if (!allowMissingFatigueAsZero) {
          throw new IllegalArgumentException("Archive bounds require P5 fatigue result");
        }
        fmaxMin = Math.min(fmaxMin, 0.0);
        fmaxMax = Math.max(fmaxMax, 0.0);
        feMin = Math.min(feMin, 0.0);
        feMax = Math.max(feMax, 0.0);
        continue;
      }
      double fmax = fatigue.getMetrics().maximumFatigue;
      double fe = fatigue.getMetrics().fatigueExcessIntegral;
      requireFinite(fmax, "Fmax");
      requireFinite(fe, "FE");
      fmaxMin = Math.min(fmaxMin, fmax);
      fmaxMax = Math.max(fmaxMax, fmax);
      feMin = Math.min(feMin, fe);
      feMax = Math.max(feMax, fe);
    }
    return new ZhangBoArchiveBounds(min, max, fmaxMin, fmaxMax, feMin, feMax,
        epsilon);
  }

  public static ZhangBoArchiveBounds of(
      double[] objectiveMin, double[] objectiveMax,
      double fatigueMaxMin, double fatigueMaxMax,
      double fatigueExcessMin, double fatigueExcessMax,
      double epsilon) {
    if (objectiveMin == null || objectiveMax == null
        || objectiveMin.length != 3 || objectiveMax.length != 3) {
      throw new IllegalArgumentException("Archive bounds require three objectives");
    }
    return new ZhangBoArchiveBounds(objectiveMin, objectiveMax, fatigueMaxMin,
        fatigueMaxMax, fatigueExcessMin, fatigueExcessMax, epsilon);
  }

  private static void requireFinite(double value, String label) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException(label + " must be finite");
  }

  public double objective(ZhangBoArchiveEntry entry, int objective) {
    return normalize(entry.getObjective(objective), objectiveMin[objective], objectiveMax[objective]);
  }

  public double[] getObjectiveMinimums() { return objectiveMin.clone(); }
  public double[] getObjectiveMaximums() { return objectiveMax.clone(); }

  public double fatigueRisk(
      ZhangBoArchiveEntry entry, ZhangBoPersonalArchiveConfiguration configuration) {
    return configuration.getFatigueWeightFmax()
        * normalize(entry.getMaximumFatigue(), fatigueMaxMin, fatigueMaxMax)
        + configuration.getFatigueWeightFe()
        * normalize(entry.getFatigueExcess(), fatigueExcessMin, fatigueExcessMax);
  }

  public double objectiveDistance(ZhangBoArchiveEntry left, ZhangBoArchiveEntry right) {
    double sum = 0.0;
    for (int objective = 0; objective < 3; objective++) {
      double difference = objective(left, objective) - objective(right, objective);
      sum += difference * difference;
    }
    return Math.sqrt(sum);
  }

  private double normalize(double value, double min, double max) {
    return (value - min) / (max - min + epsilon);
  }

  public String toCanonicalText() {
    return "objectiveMin=" + objectiveMin[0] + ',' + objectiveMin[1] + ','
        + objectiveMin[2] + "\nobjectiveMax=" + objectiveMax[0] + ','
        + objectiveMax[1] + ',' + objectiveMax[2] + "\nfatigueMin="
        + fatigueMaxMin + ',' + fatigueExcessMin + "\nfatigueMax="
        + fatigueMaxMax + ',' + fatigueExcessMax + '\n';
  }
}
