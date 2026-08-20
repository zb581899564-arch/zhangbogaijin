package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import java.io.Serializable;
import java.util.Arrays;

/** Immutable published-baseline parameters for Chapter 4 HMOPSO-QGS. */
public final class HmopsoQgsConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String SEMANTIC_TAG = "published_baseline";

  private final int populationSize;
  private final int[] subSwarmSizes;
  private final double randomCoefficientUpperBound;
  private final double[] crossoverProbabilities;
  private final double[] mutationProbabilities;
  private final int qTimes;
  private final int localSearchTimes;
  private final double gamma;
  private final double epsilon;
  private final double alpha;
  private final long maxEvaluations;
  private final long seed;

  public HmopsoQgsConfiguration(
      int populationSize, int[] subSwarmSizes, double randomCoefficientUpperBound,
      double[] crossoverProbabilities, double[] mutationProbabilities,
      int qTimes, int localSearchTimes, double gamma, double epsilon, double alpha,
      long maxEvaluations, long seed) {
    if (populationSize <= 0 || subSwarmSizes == null || subSwarmSizes.length != 4) {
      throw new IllegalArgumentException("population and four sub-swarm sizes are required");
    }
    int total = 0;
    for (int value : subSwarmSizes) {
      if (value <= 0) throw new IllegalArgumentException("sub-swarm sizes must be positive");
      total += value;
    }
    if (total != populationSize) {
      throw new IllegalArgumentException("sub-swarm sizes sum " + total
          + "; expected population " + populationSize);
    }
    requireProbability("randomCoefficientUpperBound", randomCoefficientUpperBound);
    requireThreeProbabilities("crossoverProbabilities", crossoverProbabilities);
    requireThreeProbabilities("mutationProbabilities", mutationProbabilities);
    requireProbability("gamma", gamma);
    requireProbability("epsilon", epsilon);
    requireProbability("alpha", alpha);
    if (qTimes <= 0 || localSearchTimes < 0 || maxEvaluations < populationSize) {
      throw new IllegalArgumentException("invalid iteration or evaluation budget");
    }
    this.populationSize = populationSize;
    this.subSwarmSizes = subSwarmSizes.clone();
    this.randomCoefficientUpperBound = randomCoefficientUpperBound;
    this.crossoverProbabilities = crossoverProbabilities.clone();
    this.mutationProbabilities = mutationProbabilities.clone();
    this.qTimes = qTimes;
    this.localSearchTimes = localSearchTimes;
    this.gamma = gamma;
    this.epsilon = epsilon;
    this.alpha = alpha;
    this.maxEvaluations = maxEvaluations;
    this.seed = seed;
  }

  public static HmopsoQgsConfiguration publishedTable9(long seed) {
    return new HmopsoQgsConfiguration(100, new int[] {20, 20, 20, 40}, 0.6,
        new double[] {0.2, 0.5, 0.5}, new double[] {0.08, 0.15, 0.25},
        50, 30, 0.8, 0.8, 1.0, 500000L, seed);
  }

  public static HmopsoQgsConfiguration engineeringSmoke(long seed) {
    return new HmopsoQgsConfiguration(10, new int[] {2, 2, 2, 4}, 0.6,
        new double[] {0.2, 0.5, 0.5}, new double[] {0.08, 0.15, 0.25},
        2, 1, 0.8, 0.8, 1.0, 2000L, seed);
  }

  public int getPopulationSize() { return populationSize; }
  public int[] getSubSwarmSizes() { return subSwarmSizes.clone(); }
  public double getRandomCoefficientUpperBound() { return randomCoefficientUpperBound; }
  public double[] getCrossoverProbabilities() { return crossoverProbabilities.clone(); }
  public double[] getMutationProbabilities() { return mutationProbabilities.clone(); }
  public int getQTimes() { return qTimes; }
  public int getLocalSearchTimes() { return localSearchTimes; }
  public double getGamma() { return gamma; }
  public double getEpsilon() { return epsilon; }
  public double getAlpha() { return alpha; }
  public long getMaxEvaluations() { return maxEvaluations; }
  public long getSeed() { return seed; }
  public String getSemanticTag() { return SEMANTIC_TAG; }
  public String getAlphaProvenance() { return "author_actual_compatibility"; }

  public String toCanonicalText() {
    return "semanticTag=" + SEMANTIC_TAG + '\n'
        + "population=" + populationSize + '\n'
        + "subSwarms=" + Arrays.toString(subSwarmSizes) + '\n'
        + "rUpper=" + randomCoefficientUpperBound + '\n'
        + "crossover=" + Arrays.toString(crossoverProbabilities) + '\n'
        + "mutation=" + Arrays.toString(mutationProbabilities) + '\n'
        + "qTimes=" + qTimes + '\n'
        + "localSearchTimes=" + localSearchTimes + '\n'
        + "gamma=" + gamma + '\n'
        + "epsilon=" + epsilon + '\n'
        + "alpha=" + alpha + '\n'
        + "alphaProvenance=" + getAlphaProvenance() + '\n'
        + "maxEvaluations=" + maxEvaluations + '\n'
        + "seed=" + seed + '\n';
  }

  private static void requireThreeProbabilities(String name, double[] values) {
    if (values == null || values.length != 3) {
      throw new IllegalArgumentException(name + " must contain FA/MA/WA values");
    }
    for (double value : values) requireProbability(name, value);
  }

  private static void requireProbability(String name, double value) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " outside [0,1]: " + value);
    }
  }
}
