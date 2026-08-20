package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Closed-form fatigue/recovery equations used by the Zhang Bo production path. */
public final class ZhangBoFatigueModel {
  private static final double LOG_TWO = Math.log(2.0);
  private static final double ONE_MINUS_EPSILON = Math.nextAfter(1.0, 0.0);

  private ZhangBoFatigueModel() { }

  public static double recover(double fatigue, double mu, double idleTime) {
    requireFatigue(fatigue);
    requirePositiveFinite(mu, "mu");
    requireNonnegativeFinite(idleTime, "idleTime");
    return clampFatigue(fatigue * Math.exp(-mu * idleTime));
  }

  public static double durationMultiplier(double fatigueAtStart, double maximumIncrease) {
    requireFatigue(fatigueAtStart);
    requireNonnegativeFinite(maximumIncrease, "maximumIncrease");
    return 1.0 + maximumIncrease / LOG_TWO * Math.log1p(fatigueAtStart);
  }

  public static double actualDuration(
      double baseDuration, double fatigueAtStart, double maximumIncrease) {
    requirePositiveFinite(baseDuration, "baseDuration");
    return baseDuration * durationMultiplier(fatigueAtStart, maximumIncrease);
  }

  public static double accumulate(double fatigueAtStart, double lambda, double actualDuration) {
    requireFatigue(fatigueAtStart);
    requirePositiveFinite(lambda, "lambda");
    requireNonnegativeFinite(actualDuration, "actualDuration");
    double value = fatigueAtStart
        + (1.0 - fatigueAtStart) * (-Math.expm1(-lambda * actualDuration));
    return clampFatigue(value);
  }

  /** Integral of max(F(t)-threshold, 0) while working. */
  public static double excessIntegralDuringWork(
      double initialFatigue, double lambda, double duration, double threshold) {
    requireFatigue(initialFatigue);
    requirePositiveFinite(lambda, "lambda");
    requireNonnegativeFinite(duration, "duration");
    requireThreshold(threshold);
    if (duration == 0.0) return 0.0;
    double crossing = 0.0;
    if (initialFatigue < threshold) {
      crossing = -Math.log((1.0 - threshold) / (1.0 - initialFatigue)) / lambda;
      if (crossing >= duration) return 0.0;
    }
    double expAtStart = Math.exp(-lambda * crossing);
    double expAtEnd = Math.exp(-lambda * duration);
    return (1.0 - threshold) * (duration - crossing)
        + (1.0 - initialFatigue) / lambda * (expAtEnd - expAtStart);
  }

  /** Integral of max(F(t)-threshold, 0) while naturally recovering. */
  public static double excessIntegralDuringRecovery(
      double initialFatigue, double mu, double duration, double threshold) {
    requireFatigue(initialFatigue);
    requirePositiveFinite(mu, "mu");
    requireNonnegativeFinite(duration, "duration");
    requireThreshold(threshold);
    if (duration == 0.0 || initialFatigue <= threshold) return 0.0;
    double aboveDuration = Math.min(duration, Math.log(initialFatigue / threshold) / mu);
    return initialFatigue * (-Math.expm1(-mu * aboveDuration)) / mu
        - threshold * aboveDuration;
  }

  public static double timeAboveDuringWork(
      double initialFatigue, double lambda, double duration, double threshold) {
    requireFatigue(initialFatigue);
    requirePositiveFinite(lambda, "lambda");
    requireNonnegativeFinite(duration, "duration");
    requireThreshold(threshold);
    if (initialFatigue >= threshold) return duration;
    double crossing = -Math.log((1.0 - threshold) / (1.0 - initialFatigue)) / lambda;
    return Math.max(0.0, duration - crossing);
  }

  public static double timeAboveDuringRecovery(
      double initialFatigue, double mu, double duration, double threshold) {
    requireFatigue(initialFatigue);
    requirePositiveFinite(mu, "mu");
    requireNonnegativeFinite(duration, "duration");
    requireThreshold(threshold);
    if (initialFatigue <= threshold) return 0.0;
    return Math.min(duration, Math.log(initialFatigue / threshold) / mu);
  }

  private static double clampFatigue(double value) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite fatigue: " + value);
    if (value <= 0.0) return 0.0;
    return Math.min(value, ONE_MINUS_EPSILON);
  }

  private static void requireFatigue(double value) {
    if (!(value >= 0.0 && value < 1.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException("fatigue must satisfy 0 <= F < 1: " + value);
    }
  }

  private static void requireThreshold(double value) {
    if (!(value > 0.0 && value < 1.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException("threshold must satisfy 0 < value < 1: " + value);
    }
  }

  private static void requirePositiveFinite(double value, String name) {
    if (!(value > 0.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite: " + value);
    }
  }

  private static void requireNonnegativeFinite(double value, String name) {
    if (value < 0.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be nonnegative and finite: " + value);
    }
  }
}
