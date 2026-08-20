package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;

/** Immutable Table-9 parameter contract for the P9 single-run comparison. */
public final class ZhangBoP9FormalParameters {
  public static final long SEED = 20260808L;
  public static final int POPULATION = 100;
  public static final int MAX_FES = 500000;
  /** Physical author-slot order: groupU1, groupC2, groupD3, groupUNew. */
  private static final int[] PHYSICAL_SUBSWARMS = {20, 40, 20, 20};

  private final long seed;
  private final int maxFEs;
  private final ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration;

  private ZhangBoP9FormalParameters(long seed, int maxFEs,
      ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration) {
    if (maxFEs < POPULATION) throw new IllegalArgumentException("maxFEs < population");
    if (seed < 20260808L || seed > 20260813L) {
      throw new IllegalArgumentException("seed is outside the approved P9 diagnostic set");
    }
    if (formalBaselineConfiguration == null || !formalBaselineConfiguration.isEnabled()) {
      throw new IllegalArgumentException("An enabled formal baseline configuration is required");
    }
    this.seed = seed;
    this.maxFEs = maxFEs;
    this.formalBaselineConfiguration = formalBaselineConfiguration;
  }

  public static ZhangBoP9FormalParameters formal() {
    return new ZhangBoP9FormalParameters(SEED, MAX_FES,
        ZhangBoFormalHmopsoQgsConfiguration.table9());
  }

  /** Five additional user-approved diagnostic seeds: 20260809..20260813. */
  static ZhangBoP9FormalParameters formalForApprovedSeed(long seed) {
    return new ZhangBoP9FormalParameters(seed, MAX_FES,
        ZhangBoFormalHmopsoQgsConfiguration.table9());
  }

  /**
   * Test-only bounded-loop contract.  Operator probabilities remain Table-9 values, while
   * qTimes/localSearchTimes are explicitly recorded as 2/1 so a 2000-FE engineering test can
   * reach both the Q loop and inherited local search without pretending to be a formal run.
   */
  static ZhangBoP9FormalParameters engineering(int maxFEs) {
    return new ZhangBoP9FormalParameters(SEED, maxFEs,
        ZhangBoFormalHmopsoQgsConfiguration.engineeringAudit());
  }

  static ZhangBoP9FormalParameters engineering(long seed, int maxFEs) {
    return new ZhangBoP9FormalParameters(seed, maxFEs,
        ZhangBoFormalHmopsoQgsConfiguration.engineeringAudit());
  }

  /** P8.5 bounded smoke with the exact Table-9 50/30 runtime contract. */
  static ZhangBoP9FormalParameters formalAudit(long seed, int maxFEs) {
    return new ZhangBoP9FormalParameters(seed, maxFEs,
        ZhangBoFormalHmopsoQgsConfiguration.table9());
  }

  public int getPopulation() { return POPULATION; }
  public int getMaxFEs() { return maxFEs; }
  public long getSeed() { return seed; }
  public int[] getPhysicalSubswarmSizes() {
    return Arrays.copyOf(PHYSICAL_SUBSWARMS, PHYSICAL_SUBSWARMS.length);
  }
  public double getRandUpperBound() {
    return formalBaselineConfiguration.getRandomCoefficientUpperBound();
  }
  public double getFaCrossover() { return formalBaselineConfiguration.getFaCrossover(); }
  public double getMaCrossover() { return formalBaselineConfiguration.getMaCrossover(); }
  public double getWaCrossover() { return formalBaselineConfiguration.getWaCrossover(); }
  public double getFaMutation() { return formalBaselineConfiguration.getFaMutation(); }
  public double getMaMutation() { return formalBaselineConfiguration.getMaMutation(); }
  public double getWaMutation() { return formalBaselineConfiguration.getWaMutation(); }
  public int getQTimes() { return formalBaselineConfiguration.getQTimes(); }
  public int getLocalSearchTimes() { return formalBaselineConfiguration.getLocalSearchTimes(); }
  public double getGamma() { return formalBaselineConfiguration.getGamma(); }
  public double getEpsilon() { return formalBaselineConfiguration.getEpsilon(); }

  public ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration() {
    return formalBaselineConfiguration;
  }

  public String canonicalText() {
    return "schema=zhangbo-p9-formal-v1\n"
        + "seed=" + seed + "\n"
        + "population=" + POPULATION + "\n"
        + "maxFEs=" + maxFEs + "\n"
        + "physicalSubswarmOrder=groupU1,groupC2,groupD3,groupUNew\n"
        + "physicalSubswarmSizes=" + Arrays.toString(PHYSICAL_SUBSWARMS) + "\n"
        + "randUpperBound=" + getRandUpperBound() + "\n"
        + "faCrossover=" + getFaCrossover() + "\n"
        + "maCrossover=" + getMaCrossover() + "\n"
        + "waCrossover=" + getWaCrossover() + "\n"
        + "faMutation=" + getFaMutation() + "\n"
        + "maMutation=" + getMaMutation() + "\n"
        + "waMutation=" + getWaMutation() + "\n"
        + "qTimes=" + getQTimes() + "\n"
        + "localSearchTimes=" + getLocalSearchTimes() + "\n"
        + "gamma=" + getGamma() + "\n"
        + "epsilon=" + getEpsilon() + "\n"
        + formalBaselineConfiguration.canonicalText();
  }

  public String sha256() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder text = new StringBuilder();
      for (byte value : digest) text.append(String.format("%02x", value & 0xff));
      return text.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
