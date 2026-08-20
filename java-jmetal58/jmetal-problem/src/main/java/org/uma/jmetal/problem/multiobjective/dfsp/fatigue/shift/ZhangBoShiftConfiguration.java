package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

/** Immutable P8.4 shift-decoder configuration. */
public final class ZhangBoShiftConfiguration {
  public static final String ALGORITHM_SEMANTICS_VERSION = "fatigue-shift-v2-common-gap";
  public static final double DEFAULT_EPSILON = 1.0e-9;
  public static final int DEFAULT_LEFT_CANDIDATES = 8;
  public static final int DEFAULT_RIGHT_ATTEMPTS = 10;

  private final ZhangBoShiftMode mode;
  private final double epsilon;
  private final int maximumLeftCandidates;
  private final int maximumRightAttempts;
  private final boolean captureFullTrace;

  public ZhangBoShiftConfiguration(
      ZhangBoShiftMode mode, double epsilon, int maximumLeftCandidates,
      int maximumRightAttempts, boolean captureFullTrace) {
    if (mode == null) throw new IllegalArgumentException("shift mode must not be null");
    if (!(epsilon > 0.0) || !Double.isFinite(epsilon)) {
      throw new IllegalArgumentException("shift epsilon must be positive and finite");
    }
    if (maximumLeftCandidates <= 0 || maximumRightAttempts <= 0) {
      throw new IllegalArgumentException("shift candidate limits must be positive");
    }
    this.mode = mode;
    this.epsilon = epsilon;
    this.maximumLeftCandidates = maximumLeftCandidates;
    this.maximumRightAttempts = maximumRightAttempts;
    this.captureFullTrace = captureFullTrace;
  }

  public static ZhangBoShiftConfiguration none() {
    return new ZhangBoShiftConfiguration(ZhangBoShiftMode.NONE, DEFAULT_EPSILON,
        DEFAULT_LEFT_CANDIDATES, DEFAULT_RIGHT_ATTEMPTS, false);
  }

  public static ZhangBoShiftConfiguration formalLeftRight() {
    return new ZhangBoShiftConfiguration(ZhangBoShiftMode.LEFT_RIGHT, DEFAULT_EPSILON,
        DEFAULT_LEFT_CANDIDATES, DEFAULT_RIGHT_ATTEMPTS, false);
  }

  public ZhangBoShiftConfiguration withFullTrace(boolean enabled) {
    return new ZhangBoShiftConfiguration(mode, epsilon, maximumLeftCandidates,
        maximumRightAttempts, enabled);
  }

  public ZhangBoShiftMode getMode() { return mode; }
  public double getEpsilon() { return epsilon; }
  public int getMaximumLeftCandidates() { return maximumLeftCandidates; }
  public int getMaximumRightAttempts() { return maximumRightAttempts; }
  public boolean isCaptureFullTrace() { return captureFullTrace; }

  public String toCanonicalText() {
    return "algorithmSemanticsVersion=" + ALGORITHM_SEMANTICS_VERSION + "\n"
        + "shiftMode=" + mode.name() + "\n"
        + "epsilon=" + Double.toString(epsilon) + "\n"
        + "maximumLeftCandidates=" + maximumLeftCandidates + "\n"
        + "maximumRightAttempts=" + maximumRightAttempts + "\n";
  }
}
