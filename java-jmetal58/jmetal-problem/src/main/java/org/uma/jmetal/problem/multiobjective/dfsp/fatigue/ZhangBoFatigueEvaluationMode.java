package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Explicit production decoder modes used by P8 fatigue ablation. */
public enum ZhangBoFatigueEvaluationMode {
  AUTHOR_ACTUAL,
  CORRECTED_NO_FATIGUE,
  ACCUMULATION_ONLY,
  ACCUMULATION_RECOVERY,
  FATIGUE_AWARE_SELECTION;

  public boolean usesCorrectedSutAndResources() { return this != AUTHOR_ACTUAL; }
  public boolean accumulatesFatigue() {
    return this == ACCUMULATION_ONLY || this == ACCUMULATION_RECOVERY
        || this == FATIGUE_AWARE_SELECTION;
  }
  public boolean recoversNaturally() {
    return this == ACCUMULATION_RECOVERY || this == FATIGUE_AWARE_SELECTION;
  }
  public boolean usesFatigueAwareWorkerSelection() {
    return this == FATIGUE_AWARE_SELECTION;
  }
}
