package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.Arrays;

/** Immutable three-objective social teacher snapshot. */
public final class V35SocialTeacher implements Serializable {
  private static final long serialVersionUID = 1L;
  private final double[] objectives;
  private final String fingerprint;

  public V35SocialTeacher(double[] objectives, String fingerprint) {
    if (objectives == null || objectives.length != 3 || fingerprint == null || fingerprint.isEmpty()) {
      throw new IllegalArgumentException("teacher requires three objectives and a fingerprint");
    }
    this.objectives = objectives.clone();
    for (double value : this.objectives) if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("teacher objectives must be finite");
    }
    this.fingerprint = fingerprint;
  }
  public double[] getObjectives() { return objectives.clone(); }
  public String getFingerprint() { return fingerprint; }
  @Override public String toString() { return Arrays.toString(objectives) + "|" + fingerprint; }
}
