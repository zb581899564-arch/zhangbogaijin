package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2;

import java.util.Locale;

/**
 * Enum of all experimental profiles for V35 Qp-v2 Candidate A Phase B1 investigation.
 */
public enum V35QpV2Profile {

  REF_A4("REF_A4", 1, false, "Canonical A4 frozen baseline reference"),
  QP_V2_K1("QP_V2_K1", 1, true, "Qp-v2 Candidate A with K=1 (strict equivalence arm)"),
  QP_V2_K2("QP_V2_K2", 2, true, "Qp-v2 Candidate A with K=2 (recommended exploration)"),
  QP_V2_K3("QP_V2_K3", 3, true, "Qp-v2 Candidate A with K=3 (depth exploration)"),
  QP_V2_K4("QP_V2_K4", 4, true, "Qp-v2 Candidate A with K=4 (full archive exploration)");

  private final String profileName;
  private final int k;
  private final boolean topKEnabled;
  private final String description;

  V35QpV2Profile(String profileName, int k, boolean topKEnabled, String description) {
    this.profileName = profileName;
    this.k = k;
    this.topKEnabled = topKEnabled;
    this.description = description;
  }

  public String getProfileName() {
    return profileName;
  }

  public int getK() {
    return k;
  }

  public boolean isTopKEnabled() {
    return topKEnabled;
  }

  public String getDescription() {
    return description;
  }

  public V35QpTopKConfiguration toConfiguration() {
    return new V35QpTopKConfiguration(k, topKEnabled);
  }

  public static V35QpV2Profile fromString(String text) {
    if (text == null) {
      throw new IllegalArgumentException("Profile name cannot be null");
    }
    String normalized = text.trim().toUpperCase(Locale.US);
    for (V35QpV2Profile p : values()) {
      if (p.profileName.equals(normalized)) {
        return p;
      }
    }
    throw new IllegalArgumentException("Unknown Qp-v2 profile: " + text
        + ". Allowed: REF_A4, QP_V2_K1, QP_V2_K2, QP_V2_K3, QP_V2_K4");
  }
}
