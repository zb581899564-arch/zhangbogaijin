package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Immutable standardized fatigue scenario. Values are computational abstractions, not physiology. */
public final class ZhangBoFatigueParameters {
  public static final int SCHEMA_VERSION = 1;
  public static final long SAMPLER_SEED = 20260808L;
  public static final String SEMANTIC_TAG = "standardized_fatigue_scenario";
  private static final double LOG_TWO = Math.log(2.0);

  private final String instanceSha256;
  private final double[][][] lambda;
  private final double[][][] mu;
  private final double[] maximumIncrease;
  private final double warningThreshold;
  private final double safeThreshold;
  private final String configurationSha256;

  public ZhangBoFatigueParameters(
      String instanceSha256,
      double[][][] lambda,
      double[][][] mu,
      double[] maximumIncrease,
      double warningThreshold,
      double safeThreshold,
      String configurationSha256) {
    if (instanceSha256 == null || !instanceSha256.matches("[0-9A-Fa-f]{64}")) {
      throw new IllegalArgumentException("Invalid instance SHA-256: " + instanceSha256);
    }
    if (lambda == null || mu == null || lambda.length == 0 || lambda.length != mu.length) {
      throw new IllegalArgumentException("Lambda/mu factory dimensions must match and be nonempty");
    }
    this.lambda = copyAndValidate(lambda, "lambda", 0.0, Double.POSITIVE_INFINITY);
    this.mu = copyAndValidate(mu, "mu", 0.0, Double.POSITIVE_INFINITY);
    ensureSameShape(this.lambda, this.mu);
    if (maximumIncrease == null || maximumIncrease.length == 0) {
      throw new IllegalArgumentException("maximumIncrease must be nonempty");
    }
    this.maximumIncrease = maximumIncrease.clone();
    for (double value : this.maximumIncrease) {
      if (value < 0.0 || !Double.isFinite(value)
          || Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
        throw new IllegalArgumentException(
            "maximumIncrease must be finite, nonnegative, and use +0.0 for zero: " + value);
      }
    }
    for (int f = 0; f < this.lambda.length; f++) {
      if (this.lambda[f].length != this.maximumIncrease.length) {
        throw new IllegalArgumentException("Stage dimension mismatch at factory " + f);
      }
    }
    if (!(warningThreshold > 0.0 && warningThreshold < 1.0)
        || !(safeThreshold > warningThreshold && safeThreshold < 1.0)) {
      throw new IllegalArgumentException("Thresholds must satisfy 0 < warning < safe < 1");
    }
    this.instanceSha256 = instanceSha256.toUpperCase();
    this.warningThreshold = warningThreshold;
    this.safeThreshold = safeThreshold;
    String normalizedConfiguration = configurationSha256 == null
        ? "" : configurationSha256.toUpperCase();
    if (!normalizedConfiguration.isEmpty()
        && !normalizedConfiguration.matches("[0-9A-F]{64}")) {
      throw new IllegalArgumentException("Invalid configuration SHA-256: " + configurationSha256);
    }
    this.configurationSha256 = normalizedConfiguration;
  }

  public String getInstanceSha256() { return instanceSha256; }
  public int getFactories() { return lambda.length; }
  public int getStages() { return maximumIncrease.length; }
  public int getWorkers(int factory, int stage) { return lambda[factory][stage].length; }
  public double getLambda(int factory, int worker, int stage) { return lambda[factory][stage][worker]; }
  public double getMu(int factory, int worker, int stage) { return mu[factory][stage][worker]; }
  public double getMaximumIncrease(int stage) { return maximumIncrease[stage]; }
  public double getDelta(int factory, int worker, int stage) {
    return maximumIncrease[stage] / (getLambda(factory, worker, stage) * LOG_TWO);
  }
  public double getWarningThreshold() { return warningThreshold; }
  public double getSafeThreshold() { return safeThreshold; }
  public String getConfigurationSha256() { return configurationSha256; }
  public boolean isZeroImpact() {
    for (double value : maximumIncrease) if (Double.compare(value, 0.0) != 0) return false;
    return true;
  }
  public ZhangBoFatigueParameters withZeroImpact() {
    return new ZhangBoFatigueParameters(instanceSha256, lambda, mu,
        new double[maximumIncrease.length], warningThreshold, safeThreshold, "");
  }

  double[][][] copyLambda() { return copy(lambda); }
  double[][][] copyMu() { return copy(mu); }
  double[] copyMaximumIncrease() { return maximumIncrease.clone(); }

  private static double[][][] copyAndValidate(double[][][] source, String name, double lowerExclusive, double upper) {
    double[][][] result = copy(source);
    for (int f = 0; f < result.length; f++) {
      if (result[f] == null || result[f].length == 0) throw new IllegalArgumentException(name + " stage dimension empty at factory " + f);
      for (int k = 0; k < result[f].length; k++) {
        if (result[f][k] == null || result[f][k].length == 0) throw new IllegalArgumentException(name + " worker dimension empty at factory=" + f + ", stage=" + k);
        for (double value : result[f][k]) {
          if (!(value > lowerExclusive) || value > upper || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " value invalid: " + value);
          }
        }
      }
    }
    return result;
  }

  private static void ensureSameShape(double[][][] a, double[][][] b) {
    for (int f = 0; f < a.length; f++) {
      if (a[f].length != b[f].length) throw new IllegalArgumentException("Lambda/mu stage shapes differ");
      for (int k = 0; k < a[f].length; k++) {
        if (a[f][k].length != b[f][k].length) throw new IllegalArgumentException("Lambda/mu worker shapes differ");
      }
    }
  }

  private static double[][][] copy(double[][][] source) {
    double[][][] result = new double[source.length][][];
    for (int f = 0; f < source.length; f++) {
      if (source[f] == null) throw new IllegalArgumentException("Null factory parameter row");
      result[f] = new double[source[f].length][];
      for (int k = 0; k < source[f].length; k++) {
        if (source[f][k] == null) throw new IllegalArgumentException("Null stage parameter row");
        result[f][k] = source[f][k].clone();
      }
    }
    return result;
  }
}
