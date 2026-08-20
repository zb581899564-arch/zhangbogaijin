package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable selected-pbest sidecar copied with a lineage candidate. */
public final class ZhangBoQpLineageState implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String selectedPbestFingerprint;

  public ZhangBoQpLineageState(String selectedPbestFingerprint) {
    if (selectedPbestFingerprint == null || selectedPbestFingerprint.isEmpty()) {
      throw new IllegalArgumentException("selectedPbestFingerprint");
    }
    this.selectedPbestFingerprint = selectedPbestFingerprint;
  }

  public String getSelectedPbestFingerprint() { return selectedPbestFingerprint; }
}
