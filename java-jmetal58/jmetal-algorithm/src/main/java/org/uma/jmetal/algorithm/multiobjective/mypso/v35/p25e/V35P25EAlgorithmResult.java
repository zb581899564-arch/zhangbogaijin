package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.uma.jmetal.solution.Solution;

/** Minimal immutable result shared by the faithful P25E adapters. */
public final class V35P25EAlgorithmResult {
  private final String algorithm;
  private final String sourceKind;
  private final String implementationClass;
  private final int evaluations;
  private final long runNanos;
  private final List<double[]> front;
  private final String identityEvidence;

  public V35P25EAlgorithmResult(String algorithm, String sourceKind,
      String implementationClass, int evaluations, long runNanos,
      List<? extends Solution<?>> solutions, String identityEvidence) {
    this.algorithm = algorithm;
    this.sourceKind = sourceKind;
    this.implementationClass = implementationClass;
    this.evaluations = evaluations;
    this.runNanos = runNanos;
    this.identityEvidence = identityEvidence;
    List<double[]> values = new ArrayList<>();
    for (Solution<?> solution : solutions) {
      if (solution.getNumberOfObjectives() == 3) {
        values.add(new double[] {solution.getObjective(0), solution.getObjective(1),
            solution.getObjective(2)});
      } else if (solution.getNumberOfObjectives() >= 7) {
        values.add(new double[] {solution.getObjective(0), solution.getObjective(1),
            solution.getObjective(6)});
      } else {
        throw new IllegalArgumentException("unsupported objective layout: "
            + solution.getNumberOfObjectives());
      }
    }
    values.sort(new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) {
        int value = Double.compare(a[0], b[0]);
        if (value == 0) value = Double.compare(a[1], b[1]);
        if (value == 0) value = Double.compare(a[2], b[2]);
        return value;
      }
    });
    List<double[]> copy = new ArrayList<>(values.size());
    for (double[] point : values) copy.add(point.clone());
    this.front = Collections.unmodifiableList(copy);
  }

  public String getAlgorithm() { return algorithm; }
  public String getSourceKind() { return sourceKind; }
  public String getImplementationClass() { return implementationClass; }
  public int getEvaluations() { return evaluations; }
  public long getRunNanos() { return runNanos; }
  public List<double[]> getFront() {
    List<double[]> copy = new ArrayList<>(front.size());
    for (double[] point : front) copy.add(point.clone());
    return copy;
  }
  public String getIdentityEvidence() { return identityEvidence; }
}
