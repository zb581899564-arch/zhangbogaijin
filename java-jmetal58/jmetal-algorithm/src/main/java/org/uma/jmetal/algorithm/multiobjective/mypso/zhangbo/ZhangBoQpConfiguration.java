package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable P6.3 Q-pbest engineering configuration. */
public final class ZhangBoQpConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum PersonalLeaderMode { AUTHOR_PERSONAL_HISTORY, QP_LINEAGE_ARCHIVE }
  public enum DirectionRewardMode { LEGACY_UNCLIPPED, V35_CLIPPED }
  /** Deterministic cold-start rule for a greedy row whose legal Q values tie. */
  public enum GreedyTiePolicy { FIRST_VALID, DIRECTIONAL_IF_TIED }

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
  private final DirectionRewardMode directionRewardMode;
  private final GreedyTiePolicy greedyTiePolicy;
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
      PersonalLeaderMode personalLeaderMode, DirectionRewardMode directionRewardMode,
      GreedyTiePolicy greedyTiePolicy,
      double alpha, double gamma,
      double epsilonStart, double epsilonEnd, double qualityTolerance,
      double convergenceTolerance, double diversityTolerance,
      int stagnationGenerations, double redundancyFloor,
      double dominanceWeight, double directionWeight,
      double archiveWeight, double fatigueWeight) {
    if (personalLeaderMode == null) throw new IllegalArgumentException("personalLeaderMode");
    if (directionRewardMode == null) throw new IllegalArgumentException("directionRewardMode");
    if (greedyTiePolicy == null) throw new IllegalArgumentException("greedyTiePolicy");
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
    this.directionRewardMode = directionRewardMode;
    this.greedyTiePolicy = greedyTiePolicy;
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

  /** Current v3.5 equation (Section 31): clip the normalized direction reward to [-1,1]. */
  public static ZhangBoQpConfiguration v35ClippedDirection() {
    return standard(PersonalLeaderMode.QP_LINEAGE_ARCHIVE, DirectionRewardMode.V35_CLIPPED);
  }

  /**
   * Diagnostic-only cold-start policy.  It changes no reward, epsilon or TD
   * parameter: it only resolves a greedy tie in favour of DIRECTIONAL when
   * that action is legal.
   */
  public static ZhangBoQpConfiguration diagnosticDirectionalGreedyTie() {
    return standard(PersonalLeaderMode.QP_LINEAGE_ARCHIVE,
        DirectionRewardMode.LEGACY_UNCLIPPED, GreedyTiePolicy.DIRECTIONAL_IF_TIED);
  }

  private static ZhangBoQpConfiguration standard(PersonalLeaderMode mode) {
    return standard(mode, DirectionRewardMode.LEGACY_UNCLIPPED);
  }

  private static ZhangBoQpConfiguration standard(
      PersonalLeaderMode mode, DirectionRewardMode directionRewardMode) {
    return standard(mode, directionRewardMode, GreedyTiePolicy.FIRST_VALID);
  }

  private static ZhangBoQpConfiguration standard(
      PersonalLeaderMode mode, DirectionRewardMode directionRewardMode,
      GreedyTiePolicy greedyTiePolicy) {
    return new ZhangBoQpConfiguration(mode, directionRewardMode, greedyTiePolicy,
        DEFAULT_ALPHA, DEFAULT_GAMMA,
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
        DirectionRewardMode.LEGACY_UNCLIPPED, GreedyTiePolicy.FIRST_VALID,
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
  public DirectionRewardMode getDirectionRewardMode() { return directionRewardMode; }
  public GreedyTiePolicy getGreedyTiePolicy() { return greedyTiePolicy; }
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
    String text = "qp.personalLeaderMode=" + personalLeaderMode + "\n"
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
    if (directionRewardMode != DirectionRewardMode.LEGACY_UNCLIPPED) {
      text += "qp.directionRewardMode=" + directionRewardMode + "\n";
    }
    if (greedyTiePolicy != GreedyTiePolicy.FIRST_VALID) {
      text += "qp.greedyTiePolicy=" + greedyTiePolicy + "\n";
    }
    return text;
  }
}
