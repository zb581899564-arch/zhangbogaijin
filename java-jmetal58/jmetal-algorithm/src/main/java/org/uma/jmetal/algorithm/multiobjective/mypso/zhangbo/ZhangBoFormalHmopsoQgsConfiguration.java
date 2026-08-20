package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Immutable runtime contract for the paper-explicit HMOPSO-QGS backbone.
 *
 * <p>This object is deliberately separate from the historical builder fields.  A formal run
 * must provide it explicitly, and the builder verifies that every legacy scalar passed to the
 * algorithm agrees with this contract.  Consequently the values written to the evidence text
 * are the values consumed by the update and local-search loops.</p>
 */
public final class ZhangBoFormalHmopsoQgsConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SEMANTICS_VERSION = "formal-hmopso-qgs-v1";

  private final boolean enabled;
  private final double randomCoefficientUpperBound;
  private final double faCrossover;
  private final double maCrossover;
  private final double waCrossover;
  private final double faMutation;
  private final double maMutation;
  private final double waMutation;
  private final int qTimes;
  private final int localSearchTimes;
  private final double gamma;
  private final double epsilon;

  private ZhangBoFormalHmopsoQgsConfiguration(boolean enabled,
      double randomCoefficientUpperBound,
      double faCrossover, double maCrossover, double waCrossover,
      double faMutation, double maMutation, double waMutation,
      int qTimes, int localSearchTimes, double gamma, double epsilon) {
    this.enabled = enabled;
    this.randomCoefficientUpperBound = probability(
        randomCoefficientUpperBound, "randomCoefficientUpperBound");
    this.faCrossover = probability(faCrossover, "faCrossover");
    this.maCrossover = probability(maCrossover, "maCrossover");
    this.waCrossover = probability(waCrossover, "waCrossover");
    this.faMutation = probability(faMutation, "faMutation");
    this.maMutation = probability(maMutation, "maMutation");
    this.waMutation = probability(waMutation, "waMutation");
    this.gamma = probability(gamma, "gamma");
    this.epsilon = probability(epsilon, "epsilon");
    if (enabled && qTimes <= 0) {
      throw new IllegalArgumentException("qTimes must be positive for a formal run");
    }
    if (localSearchTimes < 0) {
      throw new IllegalArgumentException("localSearchTimes cannot be negative");
    }
    this.qTimes = qTimes;
    this.localSearchTimes = localSearchTimes;
  }

  public static ZhangBoFormalHmopsoQgsConfiguration disabled() {
    return new ZhangBoFormalHmopsoQgsConfiguration(false,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0.0);
  }

  /** ESWA Table 9 parameters used by P9 and later formal experiments. */
  public static ZhangBoFormalHmopsoQgsConfiguration table9() {
    return of(0.6, 0.2, 0.5, 0.5, 0.08, 0.15, 0.25,
        50, 30, 0.8, 0.8);
  }

  /** Same operator semantics with bounded loops for P8.5 engineering audits only. */
  public static ZhangBoFormalHmopsoQgsConfiguration engineeringAudit() {
    return of(0.6, 0.2, 0.5, 0.5, 0.08, 0.15, 0.25,
        2, 1, 0.8, 0.8);
  }

  public static ZhangBoFormalHmopsoQgsConfiguration of(
      double randomCoefficientUpperBound,
      double faCrossover, double maCrossover, double waCrossover,
      double faMutation, double maMutation, double waMutation,
      int qTimes, int localSearchTimes, double gamma, double epsilon) {
    return new ZhangBoFormalHmopsoQgsConfiguration(true,
        randomCoefficientUpperBound, faCrossover, maCrossover, waCrossover,
        faMutation, maMutation, waMutation, qTimes, localSearchTimes, gamma, epsilon);
  }

  public boolean isEnabled() { return enabled; }
  public double getRandomCoefficientUpperBound() { return randomCoefficientUpperBound; }
  public double getFaCrossover() { return faCrossover; }
  public double getMaCrossover() { return maCrossover; }
  public double getWaCrossover() { return waCrossover; }
  public double getFaMutation() { return faMutation; }
  public double getMaMutation() { return maMutation; }
  public double getWaMutation() { return waMutation; }
  public int getQTimes() { return qTimes; }
  public int getLocalSearchTimes() { return localSearchTimes; }
  public double getGamma() { return gamma; }
  public double getEpsilon() { return epsilon; }

  public String canonicalText() {
    return "formalBaselineSemanticsVersion=" + SEMANTICS_VERSION + '\n'
        + "formalBaselineEnabled=" + enabled + '\n'
        + "formalBaseline.randomCoefficientUpperBound="
        + randomCoefficientUpperBound + '\n'
        + "formalBaseline.faCrossover=" + faCrossover + '\n'
        + "formalBaseline.maCrossover=" + maCrossover + '\n'
        + "formalBaseline.waCrossover=" + waCrossover + '\n'
        + "formalBaseline.faMutation=" + faMutation + '\n'
        + "formalBaseline.maMutation=" + maMutation + '\n'
        + "formalBaseline.waMutation=" + waMutation + '\n'
        + "formalBaseline.qTimes=" + qTimes + '\n'
        + "formalBaseline.localSearchTimes=" + localSearchTimes + '\n'
        + "formalBaseline.gamma=" + gamma + '\n'
        + "formalBaseline.epsilon=" + epsilon + '\n';
  }

  public String sha256() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02X", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static double probability(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
    return value;
  }
}
