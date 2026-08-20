package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.util.Arrays;

/** Immutable, auditable description of one P8 matrix entry. */
public final class P8ExperimentSpec {
  private final P8MatrixKind matrix;
  private final String label;
  private final String mechanism;
  private final String configurationKey;
  private final P8AblationProfile ablationProfile;
  private final P8RunStatus status;
  private final String reason;
  private final int populationSize;
  private final int maxFEs;
  private final int[] physicalSubswarmSizes;

  public P8ExperimentSpec(P8MatrixKind matrix, String label, String mechanism,
      String configurationKey, P8AblationProfile ablationProfile,
      P8RunStatus status, String reason,
      int populationSize, int maxFEs, int[] physicalSubswarmSizes) {
    if (matrix == null || label == null || mechanism == null || configurationKey == null
        || ablationProfile == null || status == null || reason == null || physicalSubswarmSizes == null
        || physicalSubswarmSizes.length != 4) {
      throw new IllegalArgumentException("P8 experiment spec contains null or invalid fields");
    }
    if (populationSize <= 0 || maxFEs < populationSize) {
      throw new IllegalArgumentException("P8 population/maxFEs are invalid");
    }
    int sum = 0;
    for (int value : physicalSubswarmSizes) {
      if (value <= 0) throw new IllegalArgumentException("P8 subgroup sizes must be positive");
      sum += value;
    }
    if (sum != populationSize) {
      throw new IllegalArgumentException("P8 subgroup sizes must sum to populationSize");
    }
    if (status == P8RunStatus.DIAGNOSTIC_ONLY && !ablationProfile.isAuthorDiagnostic()) {
      throw new IllegalArgumentException("Only AUTHOR_DIAGNOSTIC profiles may be diagnostic-only");
    }
    if (status != P8RunStatus.DIAGNOSTIC_ONLY && ablationProfile.isAuthorDiagnostic()) {
      throw new IllegalArgumentException("AUTHOR_DIAGNOSTIC cannot be a formal P8 entry");
    }
    if (status != P8RunStatus.DIAGNOSTIC_ONLY
        && ablationProfile.getResourceFlightMode()
        == P8AblationProfile.ResourceFlightMode.AUTHOR_GA) {
      throw new IllegalArgumentException("AUTHOR_GA is restricted to A0 diagnostics");
    }
    if (matrix == P8MatrixKind.DIAGNOSTIC && status != P8RunStatus.DIAGNOSTIC_ONLY) {
      throw new IllegalArgumentException("Diagnostic matrix entries must be DIAGNOSTIC_ONLY");
    }
    if (matrix != P8MatrixKind.DIAGNOSTIC && status == P8RunStatus.DIAGNOSTIC_ONLY) {
      throw new IllegalArgumentException("DIAGNOSTIC_ONLY entries require DIAGNOSTIC matrix");
    }
    this.matrix = matrix;
    this.label = label;
    this.mechanism = mechanism;
    this.configurationKey = configurationKey;
    this.ablationProfile = ablationProfile;
    this.status = status;
    this.reason = reason;
    this.populationSize = populationSize;
    this.maxFEs = maxFEs;
    this.physicalSubswarmSizes = Arrays.copyOf(physicalSubswarmSizes, 4);
  }

  public P8MatrixKind getMatrix() { return matrix; }
  public String getLabel() { return label; }
  public String getMechanism() { return mechanism; }
  public String getConfigurationKey() { return configurationKey; }
  public P8AblationProfile getAblationProfile() { return ablationProfile; }
  public P8RunStatus getStatus() { return status; }
  public String getReason() { return reason; }
  public int getPopulationSize() { return populationSize; }
  public int getMaxFEs() { return maxFEs; }
  public int[] getPhysicalSubswarmSizes() { return Arrays.copyOf(physicalSubswarmSizes, 4); }
  public boolean isDiagnosticOnly() {
    return status == P8RunStatus.DIAGNOSTIC_ONLY || matrix == P8MatrixKind.DIAGNOSTIC
        || ablationProfile.isAuthorDiagnostic();
  }
  public boolean isFrontEligible() {
    return !isDiagnosticOnly() && status == P8RunStatus.COMPLETED;
  }
  public String getSemanticTag() { return ablationProfile.getSemanticTag(); }
  public String getMechanismVectorHash() { return ablationProfile.mechanismVectorHash(); }

  public String canonicalText() {
    return "matrix=" + matrix + "\nlabel=" + label + "\nmechanism=" + mechanism
        + "\nconfigurationKey=" + configurationKey + "\nstatus=" + status
        + "\nreason=" + reason + "\npopulationSize=" + populationSize
        + "\nmaxFEs=" + maxFEs + "\nphysicalSubswarmSizes="
        + Arrays.toString(physicalSubswarmSizes) + "\nfrontEligible="
        + isFrontEligible() + "\n" + ablationProfile.canonicalText();
  }
}
