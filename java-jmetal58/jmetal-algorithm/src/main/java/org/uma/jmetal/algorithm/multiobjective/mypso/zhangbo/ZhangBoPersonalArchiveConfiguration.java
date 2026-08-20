package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable P6.2 personal-archive parameters. */
public final class ZhangBoPersonalArchiveConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final int DEFAULT_CAPACITY = 6;
  public static final double DEFAULT_NORMALIZATION_EPSILON = 1.0e-12;
  public static final double DEFAULT_DUPLICATE_EPSILON = 1.0e-4;
  public static final double DEFAULT_INDICATOR_KAPPA = 0.05;
  public static final double DEFAULT_FATIGUE_WEIGHT_FMAX = 0.5;
  public static final double DEFAULT_FATIGUE_WEIGHT_FE = 0.5;
  public static final double DEFAULT_SIMILARITY_EPSILON = 1.0e-4;

  private final boolean enabled;
  private final int capacity;
  private final double normalizationEpsilon;
  private final double duplicateEpsilon;
  private final double indicatorKappa;
  private final double fatigueWeightFmax;
  private final double fatigueWeightFe;
  private final double similarityEpsilon;

  private ZhangBoPersonalArchiveConfiguration(
      boolean enabled, int capacity, double normalizationEpsilon,
      double duplicateEpsilon, double indicatorKappa,
      double fatigueWeightFmax, double fatigueWeightFe,
      double similarityEpsilon) {
    if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
    requirePositiveFinite(normalizationEpsilon, "normalizationEpsilon");
    requirePositiveFinite(duplicateEpsilon, "duplicateEpsilon");
    requirePositiveFinite(indicatorKappa, "indicatorKappa");
    requireNonnegativeFinite(fatigueWeightFmax, "fatigueWeightFmax");
    requireNonnegativeFinite(fatigueWeightFe, "fatigueWeightFe");
    if (fatigueWeightFmax + fatigueWeightFe <= 0.0) {
      throw new IllegalArgumentException("fatigue weights must have a positive sum");
    }
    requirePositiveFinite(similarityEpsilon, "similarityEpsilon");
    this.enabled = enabled;
    this.capacity = capacity;
    this.normalizationEpsilon = normalizationEpsilon;
    this.duplicateEpsilon = duplicateEpsilon;
    this.indicatorKappa = indicatorKappa;
    double total = fatigueWeightFmax + fatigueWeightFe;
    this.fatigueWeightFmax = fatigueWeightFmax / total;
    this.fatigueWeightFe = fatigueWeightFe / total;
    this.similarityEpsilon = similarityEpsilon;
  }

  public static ZhangBoPersonalArchiveConfiguration disabled() {
    return new ZhangBoPersonalArchiveConfiguration(false, DEFAULT_CAPACITY,
        DEFAULT_NORMALIZATION_EPSILON, DEFAULT_DUPLICATE_EPSILON,
        DEFAULT_INDICATOR_KAPPA, DEFAULT_FATIGUE_WEIGHT_FMAX,
        DEFAULT_FATIGUE_WEIGHT_FE, DEFAULT_SIMILARITY_EPSILON);
  }

  public static ZhangBoPersonalArchiveConfiguration standard() {
    return new ZhangBoPersonalArchiveConfiguration(true, DEFAULT_CAPACITY,
        DEFAULT_NORMALIZATION_EPSILON, DEFAULT_DUPLICATE_EPSILON,
        DEFAULT_INDICATOR_KAPPA, DEFAULT_FATIGUE_WEIGHT_FMAX,
        DEFAULT_FATIGUE_WEIGHT_FE, DEFAULT_SIMILARITY_EPSILON);
  }

  public static ZhangBoPersonalArchiveConfiguration of(
      int capacity, double normalizationEpsilon, double duplicateEpsilon,
      double indicatorKappa, double fatigueWeightFmax, double fatigueWeightFe,
      double similarityEpsilon) {
    return new ZhangBoPersonalArchiveConfiguration(true, capacity, normalizationEpsilon,
        duplicateEpsilon, indicatorKappa, fatigueWeightFmax, fatigueWeightFe,
        similarityEpsilon);
  }

  private static void requirePositiveFinite(double value, String name) {
    if (!(value > 0.0) || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }

  private static void requireNonnegativeFinite(double value, String name) {
    if (value < 0.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be nonnegative and finite");
    }
  }

  public boolean isEnabled() { return enabled; }
  public int getCapacity() { return capacity; }
  public double getNormalizationEpsilon() { return normalizationEpsilon; }
  public double getDuplicateEpsilon() { return duplicateEpsilon; }
  public double getIndicatorKappa() { return indicatorKappa; }
  public double getFatigueWeightFmax() { return fatigueWeightFmax; }
  public double getFatigueWeightFe() { return fatigueWeightFe; }
  public double getSimilarityEpsilon() { return similarityEpsilon; }

  public String toCanonicalText() {
    return "personalArchive.enabled=" + enabled + "\n"
        + "personalArchive.capacity=" + capacity + "\n"
        + "personalArchive.normalizationEpsilon=" + normalizationEpsilon + "\n"
        + "personalArchive.duplicateEpsilon=" + duplicateEpsilon + "\n"
        + "personalArchive.indicatorKappa=" + indicatorKappa + "\n"
        + "personalArchive.fatigueWeightFmax=" + fatigueWeightFmax + "\n"
        + "personalArchive.fatigueWeightFe=" + fatigueWeightFe + "\n"
        + "personalArchive.similarityEpsilon=" + similarityEpsilon + "\n";
  }
}
