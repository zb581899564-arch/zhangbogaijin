package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.io.Serializable;

/** Immutable P7.2 Test-and-Apply configuration. */
public final class ZhangBoCaTaConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String ALGORITHM_SEMANTICS_VERSION = "cata-apply-v2";
  public static final int DEFAULT_N_TEST = 1;
  public static final int DEFAULT_APPLY_MULTIPLIER = 1;
  public static final double DEFAULT_APPLY_EXPLORE_PROBABILITY = 0.10;
  public static final int DEFAULT_STAGNATION_THRESHOLD = 3;
  public static final double DEFAULT_NEED_WEIGHTED_PROBABILITY = 0.80;
  public static final long DOMAIN_SEED = 0x43415441564E53L; // "CATAVNS"

  private final boolean enabled;
  private final int nTest;
  private final int applyMultiplier;
  private final double applyExploreProbability;
  private final int stagnationThreshold;
  private final double needWeightedProbability;

  private ZhangBoCaTaConfiguration(boolean enabled, int nTest, int applyMultiplier,
      double applyExploreProbability, int stagnationThreshold,
      double needWeightedProbability) {
    if (nTest < 1 || applyMultiplier < 1 || stagnationThreshold < 1) {
      throw new IllegalArgumentException("CA-TA counts must be positive");
    }
    requireProbability(applyExploreProbability, "applyExploreProbability");
    requireProbability(needWeightedProbability, "needWeightedProbability");
    this.enabled = enabled;
    this.nTest = nTest;
    this.applyMultiplier = applyMultiplier;
    this.applyExploreProbability = applyExploreProbability;
    this.stagnationThreshold = stagnationThreshold;
    this.needWeightedProbability = needWeightedProbability;
  }

  public static ZhangBoCaTaConfiguration disabled() {
    return new ZhangBoCaTaConfiguration(false, DEFAULT_N_TEST, DEFAULT_APPLY_MULTIPLIER,
        DEFAULT_APPLY_EXPLORE_PROBABILITY, DEFAULT_STAGNATION_THRESHOLD,
        DEFAULT_NEED_WEIGHTED_PROBABILITY);
  }

  public static ZhangBoCaTaConfiguration standard() {
    return new ZhangBoCaTaConfiguration(true, DEFAULT_N_TEST, DEFAULT_APPLY_MULTIPLIER,
        DEFAULT_APPLY_EXPLORE_PROBABILITY, DEFAULT_STAGNATION_THRESHOLD,
        DEFAULT_NEED_WEIGHTED_PROBABILITY);
  }

  public ZhangBoCaTaConfiguration withEnabled(boolean value) {
    return new ZhangBoCaTaConfiguration(value, nTest, applyMultiplier,
        applyExploreProbability, stagnationThreshold, needWeightedProbability);
  }

  public ZhangBoCaTaConfiguration withTestAndApply(int tests, int multiplier) {
    return new ZhangBoCaTaConfiguration(enabled, tests, multiplier,
        applyExploreProbability, stagnationThreshold, needWeightedProbability);
  }

  public ZhangBoCaTaConfiguration withApplyExploreProbability(double value) {
    return new ZhangBoCaTaConfiguration(enabled, nTest, applyMultiplier,
        value, stagnationThreshold, needWeightedProbability);
  }

  public ZhangBoCaTaConfiguration withStagnationThreshold(int value) {
    return new ZhangBoCaTaConfiguration(enabled, nTest, applyMultiplier,
        applyExploreProbability, value, needWeightedProbability);
  }

  public boolean isEnabled() { return enabled; }
  public int getNTest() { return nTest; }
  public int getApplyMultiplier() { return applyMultiplier; }
  public double getApplyExploreProbability() { return applyExploreProbability; }
  public int getStagnationThreshold() { return stagnationThreshold; }
  public double getNeedWeightedProbability() { return needWeightedProbability; }

  public String toCanonicalText() {
    return "caTa.algorithmSemanticsVersion=" + ALGORITHM_SEMANTICS_VERSION + '\n'
        + "caTa.enabled=" + enabled + '\n'
        + "caTa.nTest=" + nTest + '\n'
        + "caTa.applyMultiplier=" + applyMultiplier + '\n'
        + "caTa.applyExploreProbability=" + applyExploreProbability + '\n'
        + "caTa.stagnationThreshold=" + stagnationThreshold + '\n'
        + "caTa.needWeightedProbability=" + needWeightedProbability + '\n'
        + "caTa.phaseBoundaries=EARLY<0.33,MIDDLE<0.67,LATE<=1\n";
  }

  private static void requireProbability(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
  }
}
