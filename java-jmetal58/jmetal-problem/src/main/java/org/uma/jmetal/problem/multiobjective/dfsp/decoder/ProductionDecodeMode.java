package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

/**
 * Explicit decoder modes used by the production P8.1 path.
 *
 * <p>The legacy {@link DecodeMode} values remain available for the P2/P3
 * compatibility decoder.  Production callers must select one of these values
 * directly; no fatigue parameter (and in particular no {@code r == 0}) is
 * inspected to infer a mode.</p>
 */
public enum ProductionDecodeMode {
  /** Corrected deterministic decoder with no fatigue state. */
  CANONICAL_NO_FATIGUE("deterministic_canonical", false, false, false),
  /** Fatigue accumulation and duration feedback. */
  FM1("fatigue_fm1", true, false, false),
  /** FM1 plus natural recovery between events. */
  FM2("fatigue_fm2", true, true, false),
  /** FM2 plus fatigue-aware worker selection. */
  FM3("fatigue_fm3", true, true, true),
  /** Read-only author path; never accepted by a production problem. */
  AUTHOR_DIAGNOSTIC("author_actual", false, false, false);

  private final String semanticTag;
  private final boolean accumulatesFatigue;
  private final boolean recoversNaturally;
  private final boolean fatigueAwareWorkerSelection;

  ProductionDecodeMode(
      String semanticTag, boolean accumulatesFatigue, boolean recoversNaturally,
      boolean fatigueAwareWorkerSelection) {
    this.semanticTag = semanticTag;
    this.accumulatesFatigue = accumulatesFatigue;
    this.recoversNaturally = recoversNaturally;
    this.fatigueAwareWorkerSelection = fatigueAwareWorkerSelection;
  }

  public String getSemanticTag() {
    return semanticTag;
  }

  public boolean accumulatesFatigue() {
    return accumulatesFatigue;
  }

  public boolean recoversNaturally() {
    return recoversNaturally;
  }

  public boolean usesFatigueAwareWorkerSelection() {
    return fatigueAwareWorkerSelection;
  }

  public boolean isAuthorDiagnostic() {
    return this == AUTHOR_DIAGNOSTIC;
  }

  public boolean isFormalProductionMode() {
    return !isAuthorDiagnostic();
  }

  /** Maps explicit production semantics to the legacy evaluator's mode API. */
  public org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
      toFatigueEvaluationMode() {
    switch (this) {
      case CANONICAL_NO_FATIGUE:
        return org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
            .CORRECTED_NO_FATIGUE;
      case FM1:
        return org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
            .ACCUMULATION_ONLY;
      case FM2:
        return org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
            .ACCUMULATION_RECOVERY;
      case FM3:
        return org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
            .FATIGUE_AWARE_SELECTION;
      default:
        return org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode
            .AUTHOR_ACTUAL;
    }
  }

  /**
   * Converts the mode to the legacy decoder's deterministic mode.  FM1--FM3
   * are intentionally kept as explicit tags at the production boundary; the
   * P2 decoder itself has no fatigue state and therefore receives the same
   * deterministic scheduling policy until a fatigue-aware decoder is injected.
   */
  DecodeMode toLegacyDeterministicMode() {
    return DecodeMode.DETERMINISTIC_CANONICAL;
  }
}
