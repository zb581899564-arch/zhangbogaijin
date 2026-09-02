package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2;

import java.util.Locale;

/**
 * Immutable configuration for Qp-v2 Candidate A (Action-consistent Top-K Candidate Pool
 * + Uniform Random Exploration).
 *
 * <p>Valid K values: {@code 1, 2, 3, 4}.
 * <ul>
 *   <li>K=1: Strictly reduces to canonical A4 (zero additional RNG draws, bit-identical).
 *   <li>K=2..4: Action-consistent top-K candidate pool with uniform exploration.
 * </ul>
 */
public final class V35QpTopKConfiguration {

  private final int k;
  private final boolean enabled;

  public static final V35QpTopKConfiguration CANONICAL_A4 = new V35QpTopKConfiguration(1, false);

  public V35QpTopKConfiguration(int k, boolean enabled) {
    if (k < 1 || k > 4) {
      throw new IllegalArgumentException("Qp-v2 K must be in {1, 2, 3, 4}, got: " + k);
    }
    this.k = k;
    this.enabled = enabled;
  }

  public static V35QpTopKConfiguration ofK(int k) {
    if (k == 1) {
      return new V35QpTopKConfiguration(1, true);
    }
    return new V35QpTopKConfiguration(k, true);
  }

  public int getK() {
    return k;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String canonicalText() {
    return String.format(Locale.US, "Qp-v2[candidate=CANDIDATE_A_TOPK_UNIFORM,K=%d,enabled=%b]", k, enabled);
  }

  @Override
  public String toString() {
    return canonicalText();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    V35QpTopKConfiguration that = (V35QpTopKConfiguration) o;
    return k == that.k && enabled == that.enabled;
  }

  @Override
  public int hashCode() {
    return 31 * k + (enabled ? 1 : 0);
  }
}
