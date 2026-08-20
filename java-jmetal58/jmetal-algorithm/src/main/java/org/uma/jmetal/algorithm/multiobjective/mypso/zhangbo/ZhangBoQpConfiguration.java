package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable P6.3 Q-pbest engineering configuration. */
public final class ZhangBoQpConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum PersonalLeaderMode { AUTHOR_PERSONAL_HISTORY, QP_LINEAGE_ARCHIVE }

  public static final double DEFAULT_ALPHA = 0.30;
  public static final double DEFAULT_GAMMA = 0.80;
  public static final double DEFAULT_EPSILON_START = 0.30;
  public static final double DEFAULT_EPSILON_END = 0.05;
  public static final double DEFAULT_QUALITY_TOLERANCE = 0.15;
  public static final double DEFAULT_CONVERGENCE_TOLERANCE = 1.0e-4;
  public static final double DEFAULT_DIVERSITY_TOLERANCE = 1.0e-4;
  public static final int DEFAULT_STAGNATION_GENERATIONS = 3;
  public static final double DEFAULT_REDUNDANCY_FLOOR = 0.80;
  public static final double DEFAULT_DOMINANCE_WEIGHT = 2.0;
  public static final double DEFAULT_DIRECTION_WEIGHT = 1.0;
  public static final double DEFAULT_ARCHIVE_WEIGHT = 0.5;
  public static final double DEFAULT_FATIGUE_WEIGHT = 0.25;

  private final PersonalLeaderMode personalLeaderMode;
  private final double alpha;
  private final double gamma;
  private final double epsilonStart;
  private final double epsilonEnd;
  private final double qualityTolerance;
  private final double convergenceTolerance;
  private final double diversityTolerance;
  private final int stagnationGenerations;
  private final double redundancyFloor;
  private final double dominanceWeight;
  private final double directionWeight;
  private final double archiveWeight;
  private final double fatigueWeight;

  private ZhangBoQpConfiguration(
      PersonalLeaderMode personalLeaderMode, double alpha, double gamma,
      double epsilonStart, double epsilonEnd, double qualityTolerance,
      double convergenceTolerance, double diversityTolerance,
      int stagnationGenerations, double redundancyFloor,
      double dominanceWeight, double directionWeight,
      double archiveWeight, double fatigueWeight) {
    if (personalLeaderMode == null) throw new IllegalArgumentException("personalLeaderMode");
    requireProbability(alpha, "alpha");
    requireProbability(gamma, "gamma");
    requireProbability(epsilonStart, "epsilonStart");
    requireProbability(epsilonEnd, "epsilonEnd");
    if (epsilonStart < epsilonEnd) {
      throw new IllegalArgumentException("epsilonStart must be >= epsilonEnd");
    }
    requireNonnegativeFinite(qualityTolerance, "qualityTolerance");
    requireNonnegativeFinite(convergenceTolerance, "convergenceTolerance");
    requireNonnegativeFinite(diversityTolerance, "diversityTolerance");
    if (stagnationGenerations < 1) {
      throw new IllegalArgumentException("stagnationGenerations must be >= 1");
    }
    requireProbability(redundancyFloor, "redundancyFloor");
    requireNonnegativeFinite(dominanceWeight, "dominanceWeight");
    requireNonnegativeFinite(directionWeight, "directionWeight");
    requireNonnegativeFinite(archiveWeight, "archiveWeight");
    requireNonnegativeFinite(fatigueWeight, "fatigueWeight");
    this.personalLeaderMode = personalLeaderMode;
    this.alpha = alpha;
    this.gamma = gamma;
    this.epsilonStart = epsilonStart;
    this.epsilonEnd = epsilonEnd;
    this.qualityTolerance = qualityTolerance;
    this.convergenceTolerance = convergenceTolerance;
    this.diversityTolerance = diversityTolerance;
    this.stagnationGenerations = stagnationGenerations;
    this.redundancyFloor = redundancyFloor;
    this.dominanceWeight = dominanceWeight;
    this.directionWeight = directionWeight;
    this.archiveWeight = archiveWeight;
    this.fatigueWeight = fatigueWeight;
  }

  public static ZhangBoQpConfiguration disabled() {
    return standard(PersonalLeaderMode.AUTHOR_PERSONAL_HISTORY);
  }

  public static ZhangBoQpConfiguration standard() {
    return standard(PersonalLeaderMode.QP_LINEAGE_ARCHIVE);
  }

  private static ZhangBoQpConfiguration standard(PersonalLeaderMode mode) {
    return new ZhangBoQpConfiguration(mode, DEFAULT_ALPHA, DEFAULT_GAMMA,
        DEFAULT_EPSILON_START, DEFAULT_EPSILON_END, DEFAULT_QUALITY_TOLERANCE,
        DEFAULT_CONVERGENCE_TOLERANCE, DEFAULT_DIVERSITY_TOLERANCE,
        DEFAULT_STAGNATION_GENERATIONS, DEFAULT_REDUNDANCY_FLOOR,
        DEFAULT_DOMINANCE_WEIGHT, DEFAULT_DIRECTION_WEIGHT,
        DEFAULT_ARCHIVE_WEIGHT, DEFAULT_FATIGUE_WEIGHT);
  }

  public static ZhangBoQpConfiguration of(
      double alpha, double gamma, double epsilonStart, double epsilonEnd,
      double qualityTolerance, double convergenceTolerance,
      double diversityTolerance, int stagnationGenerations,
      double redundancyFloor, double dominanceWeight, double directionWeight,
      double archiveWeight, double fatigueWeight) {
    return new ZhangBoQpConfiguration(PersonalLeaderMode.QP_LINEAGE_ARCHIVE,
        alpha, gamma, epsilonStart, epsilonEnd, qualityTolerance,
        convergenceTolerance, diversityTolerance, stagnationGenerations,
        redundancyFloor, dominanceWeight, directionWeight, archiveWeight,
        fatigueWeight);
  }

  private static void requireProbability(double value, String name) {
    if (value < 0.0 || value > 1.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
  }

  private static void requireNonnegativeFinite(double value, String name) {
    if (value < 0.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be nonnegative and finite");
    }
  }

  public PersonalLeaderMode getPersonalLeaderMode() { return personalLeaderMode; }
  public boolean isEnabled() {
    return personalLeaderMode == PersonalLeaderMode.QP_LINEAGE_ARCHIVE;
  }
  public double getAlpha() { return alpha; }
  public double getGamma() { return gamma; }
  public double getEpsilonStart() { return epsilonStart; }
  public double getEpsilonEnd() { return epsilonEnd; }
  public double getQualityTolerance() { return qualityTolerance; }
  public double getConvergenceTolerance() { return convergenceTolerance; }
  public double getDiversityTolerance() { return diversityTolerance; }
  public int getStagnationGenerations() { return stagnationGenerations; }
  public double getRedundancyFloor() { return redundancyFloor; }
  public double getDominanceWeight() { return dominanceWeight; }
  public double getDirectionWeight() { return directionWeight; }
  public double getArchiveWeight() { return archiveWeight; }
  public double getFatigueWeight() { return fatigueWeight; }

  public String toCanonicalText() {
    return "qp.personalLeaderMode=" + personalLeaderMode + "\n"
        + "qp.alpha=" + alpha + "\n"
        + "qp.gamma=" + gamma + "\n"
        + "qp.epsilonStart=" + epsilonStart + "\n"
        + "qp.epsilonEnd=" + epsilonEnd + "\n"
        + "qp.qualityTolerance=" + qualityTolerance + "\n"
        + "qp.convergenceTolerance=" + convergenceTolerance + "\n"
        + "qp.diversityTolerance=" + diversityTolerance + "\n"
        + "qp.stagnationGenerations=" + stagnationGenerations + "\n"
        + "qp.redundancyFloor=" + redundancyFloor + "\n"
        + "qp.rewardWeights=" + dominanceWeight + ',' + directionWeight + ','
        + archiveWeight + ',' + fatigueWeight + "\n";
  }
}
