package org.uma.jmetal.problem.multiobjective.dfsp.setup;

import java.io.Serializable;

/** Immutable stage-specific family transition setup matrix. */
public final class ProductFamilyTransitionMatrix implements Serializable {
  private static final long serialVersionUID = 1L;
  private final double[][][] transition;
  private final int stages;
  private final int familyCount;

  public ProductFamilyTransitionMatrix(double[][][] transition, int stages, int familyCount) {
    if (transition == null || transition.length != stages || stages <= 0 || familyCount <= 0) {
      throw new IllegalArgumentException("transition dimensions do not match stages/families");
    }
    this.stages = stages;
    this.familyCount = familyCount;
    this.transition = new double[stages][familyCount][familyCount];
    for (int stage = 0; stage < stages; stage++) {
      if (transition[stage] == null || transition[stage].length != familyCount) {
        throw new IllegalArgumentException("transition stage " + stage + " has wrong source dimension");
      }
      for (int from = 0; from < familyCount; from++) {
        if (transition[stage][from] == null || transition[stage][from].length != familyCount) {
          throw new IllegalArgumentException("transition stage " + stage + " has wrong target dimension");
        }
        for (int to = 0; to < familyCount; to++) {
          double value = transition[stage][from][to];
          if (!Double.isFinite(value) || value < 0.0 || Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
            throw new IllegalArgumentException("transition value must be finite and non-negative");
          }
          this.transition[stage][from][to] = value;
        }
      }
    }
  }

  public static ProductFamilyTransitionMatrix zero(int stages) {
    return new ProductFamilyTransitionMatrix(new double[stages][1][1], stages, 1);
  }
  public int getStages() { return stages; }
  public int getFamilyCount() { return familyCount; }
  public double get(int stage, int fromFamily, int toFamily) {
    return transition[stage][fromFamily][toFamily];
  }
  public double[][][] toArray() {
    double[][][] copy = new double[stages][familyCount][familyCount];
    for (int stage = 0; stage < stages; stage++) {
      for (int from = 0; from < familyCount; from++) copy[stage][from] = transition[stage][from].clone();
    }
    return copy;
  }
}
