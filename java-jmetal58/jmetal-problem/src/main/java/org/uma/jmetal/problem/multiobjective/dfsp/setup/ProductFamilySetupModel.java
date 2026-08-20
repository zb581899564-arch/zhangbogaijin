package org.uma.jmetal.problem.multiobjective.dfsp.setup;

import java.io.Serializable;

/**
 * Setup contract shared by the canonical decoder and future PF-SDST work.
 * The v3.5 formal line accepts only the degenerate, sequence-independent mode.
 */
public final class ProductFamilySetupModel implements Serializable {
  private static final long serialVersionUID = 1L;
  private final SetupMode setupMode;
  private final ProductFamilyData familyData;
  private final double machineChangeoverFactor;

  public ProductFamilySetupModel(SetupMode setupMode, ProductFamilyData familyData,
      double machineChangeoverFactor) {
    if (setupMode == null || familyData == null || !Double.isFinite(machineChangeoverFactor)
        || machineChangeoverFactor <= 0.0) {
      throw new IllegalArgumentException("invalid setup model");
    }
    this.setupMode = setupMode;
    this.familyData = familyData;
    this.machineChangeoverFactor = machineChangeoverFactor;
    if (isFormalDegenerate() && machineChangeoverFactor != 1.0) {
      throw new IllegalArgumentException("formal degenerate setup requires factor 1.0");
    }
  }

  public static ProductFamilySetupModel degenerate(int jobs, int stages) {
    return new ProductFamilySetupModel(SetupMode.SEQUENCE_INDEPENDENT,
        ProductFamilyData.degenerate(jobs, stages), 1.0);
  }
  public SetupMode getSetupMode() { return setupMode; }
  public ProductFamilyData getFamilyData() { return familyData; }
  public double getMachineChangeoverFactor() { return machineChangeoverFactor; }
  public boolean isFormalDegenerate() {
    return setupMode == SetupMode.SEQUENCE_INDEPENDENT
        && familyData.getFamilyMode() == FamilyMode.DEGENERATE_SINGLE_FAMILY
        && familyData.getAssignment().getFamilyCount() == 1
        && machineChangeoverFactor == 1.0;
  }
  public double setupTime(double sut, int stage, int previousJob, int currentJob) {
    if (!Double.isFinite(sut) || sut < 0.0) throw new IllegalArgumentException("invalid SUT");
    if (isFormalDegenerate() || previousJob < 0) return sut;
    int from = familyData.getAssignment().getFamilyOfJob(previousJob);
    int to = familyData.getAssignment().getFamilyOfJob(currentJob);
    return sut + familyData.getTransitionMatrix().get(stage, from, to) * machineChangeoverFactor;
  }
}
