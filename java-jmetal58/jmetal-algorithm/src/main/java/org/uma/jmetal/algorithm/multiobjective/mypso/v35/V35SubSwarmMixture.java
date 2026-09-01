package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Immutable search-period physical subgroup capacity.  The order is the
 * physical slot order used by the production builder: G1, G4, G2, G3.
 * This type deliberately represents capacity only; it is not the rejected
 * FC-6B region-survival quota.
 */
public final class V35SubSwarmMixture implements Serializable, Comparable<V35SubSwarmMixture> {
  private static final long serialVersionUID = 1L;
  public static final V35SubSwarmMixture BASELINE = of(20, 40, 20, 20);
  public static final V35SubSwarmMixture HISTORICAL_REGION_CONTROL = of(15, 55, 15, 15);
  public static final V35SubSwarmMixture BALANCED_CONTROL = of(25, 25, 25, 25);

  private final int groupU1;
  private final int groupC2;
  private final int groupD3;
  private final int groupUNew;

  private V35SubSwarmMixture(int groupU1, int groupC2, int groupD3, int groupUNew) {
    validate(groupU1, groupC2, groupD3, groupUNew);
    this.groupU1 = groupU1;
    this.groupC2 = groupC2;
    this.groupD3 = groupD3;
    this.groupUNew = groupUNew;
  }

  public static V35SubSwarmMixture of(int g1Cmax, int g4Balanced, int g2Tec, int g3Twc) {
    return new V35SubSwarmMixture(g1Cmax, g4Balanced, g2Tec, g3Twc);
  }

  public int getGroupU1() { return groupU1; }
  public int getGroupC2() { return groupC2; }
  public int getGroupD3() { return groupD3; }
  public int getGroupUNew() { return groupUNew; }
  public int getTotal() { return groupU1 + groupC2 + groupD3 + groupUNew; }
  public int getG1Cmax() { return groupU1; }
  public int getG4Balanced() { return groupC2; }
  public int getG2Tec() { return groupD3; }
  public int getG3Twc() { return groupUNew; }

  public String canonicalText() {
    return "G1_CMAX=" + groupU1 + "\n"
        + "G4_BALANCED=" + groupC2 + "\n"
        + "G2_TEC=" + groupD3 + "\n"
        + "G3_TWC=" + groupUNew + "\n";
  }

  public String hash() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte b : digest) out.append(String.format("%02x", b & 0xff));
      return out.toString();
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private static void validate(int g1, int g4, int g2, int g3) {
    if (g1 < 10 || g1 > 30 || g2 < 10 || g2 > 30 || g3 < 10 || g3 > 30
        || g4 < 25 || g4 > 60 || g1 % 5 != 0 || g2 % 5 != 0 || g3 % 5 != 0
        || g4 % 5 != 0 || g1 + g2 + g3 + g4 != 100) {
      throw new IllegalArgumentException(
          "invalid V35 mixture; require G1/G2/G3 in [10,30], G4 in [25,60], "
              + "multiples of 5 and total 100");
    }
  }

  @Override public int compareTo(V35SubSwarmMixture other) {
    int c = Integer.compare(groupU1, other.groupU1); if (c != 0) return c;
    c = Integer.compare(groupC2, other.groupC2); if (c != 0) return c;
    c = Integer.compare(groupD3, other.groupD3); if (c != 0) return c;
    return Integer.compare(groupUNew, other.groupUNew);
  }
  @Override public boolean equals(Object value) {
    if (!(value instanceof V35SubSwarmMixture)) return false;
    V35SubSwarmMixture other = (V35SubSwarmMixture) value;
    return groupU1 == other.groupU1 && groupC2 == other.groupC2
        && groupD3 == other.groupD3 && groupUNew == other.groupUNew;
  }
  @Override public int hashCode() { return Objects.hash(groupU1, groupC2, groupD3, groupUNew); }
  @Override public String toString() {
    return groupU1 + "/" + groupC2 + "/" + groupD3 + "/" + groupUNew;
  }
}
