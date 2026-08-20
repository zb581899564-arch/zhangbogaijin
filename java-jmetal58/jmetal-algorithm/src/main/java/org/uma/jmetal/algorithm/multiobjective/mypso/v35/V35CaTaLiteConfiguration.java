package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;

/** Immutable CA-TA-Lite defaults from v3.5. */
public final class V35CaTaLiteConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int nTest;
  private final int applyMultiplier;
  private final double applyExploreProbability;
  private final int stagnationThreshold;
  /**
   * V35-FC-3: when the top-2 macro neighborhoods are indistinguishable on the
   * primary credit key after a completed Test round, grant each one extra
   * probe evaluation instead of committing blindly.  Default {@code false}
   * keeps the archived A4 behaviour.
   */
  private final boolean top2ProbeEnabled;
  /**
   * V35-FC-3: hard cap on the Test share of CA-TA-Lite evaluations,
   * {@code FE_Test <= cap * FE_local}.  Once reached, expensive Re-tests are
   * suppressed in favour of the current winner.  Default {@code 1.0}
   * (never binding) keeps the archived A4 behaviour; the FC-3 candidate is
   * {@code 0.20}.
   */
  private final double testFeShareCap;

  public V35CaTaLiteConfiguration(int nTest, int applyMultiplier,
      double applyExploreProbability, int stagnationThreshold) {
    this(nTest, applyMultiplier, applyExploreProbability, stagnationThreshold, false, 1.0);
  }

  public V35CaTaLiteConfiguration(int nTest, int applyMultiplier,
      double applyExploreProbability, int stagnationThreshold,
      boolean top2ProbeEnabled, double testFeShareCap) {
    if (nTest <= 0 || applyMultiplier <= 0 || stagnationThreshold <= 0
        || !Double.isFinite(applyExploreProbability)
        || applyExploreProbability < 0.0 || applyExploreProbability > 1.0
        || !Double.isFinite(testFeShareCap)
        || testFeShareCap <= 0.0 || testFeShareCap > 1.0) {
      throw new IllegalArgumentException("invalid CA-TA-Lite configuration");
    }
    this.nTest = nTest;
    this.applyMultiplier = applyMultiplier;
    this.applyExploreProbability = applyExploreProbability;
    this.stagnationThreshold = stagnationThreshold;
    this.top2ProbeEnabled = top2ProbeEnabled;
    this.testFeShareCap = testFeShareCap;
  }

  public static V35CaTaLiteConfiguration standard() {
    return new V35CaTaLiteConfiguration(1, 1, 0.10, 3);
  }

  /** V35-FC-3 candidate: cheap Test with a top-2 tie probe and a 20% Test-FE cap. */
  public static V35CaTaLiteConfiguration cheapTest() {
    return new V35CaTaLiteConfiguration(1, 1, 0.10, 3, true, 0.20);
  }

  public int getNTest() { return nTest; }
  public int getApplyMultiplier() { return applyMultiplier; }
  public double getApplyExploreProbability() { return applyExploreProbability; }
  public int getStagnationThreshold() { return stagnationThreshold; }
  public boolean isTop2ProbeEnabled() { return top2ProbeEnabled; }
  public double getTestFeShareCap() { return testFeShareCap; }
  public int contextCount() { return V35CaTaContext.contextCount(); }
}
