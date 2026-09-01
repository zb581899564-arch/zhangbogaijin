package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/**
 * Pure V3.1 terminal-checkpoint protocol classifier.
 *
 * <p>This class deliberately receives evidence that has already been
 * collected by the real observer.  It does not inspect solutions, consume
 * randomness, or mutate algorithm state.  A rejected classification is
 * fail-closed: callers must not publish a complete terminal front.</p>
 */
public final class V35TerminalCheckpointContract {
  public enum Classification { ACCEPTED, REJECTED }

  public static final String TERMINATION_KIND =
      "PHASE_CONSISTENT_BUDGET_TERMINATION";
  public static final String CHECKPOINT_BOUNDARY =
      "COMPLETE_QG_QP_GLOBAL_OFFSPRING_CATA_INHERITED_LS_PDDR_POST_SAMPLE";

  private V35TerminalCheckpointContract() { }

  /**
   * Classifies one terminal checkpoint without any observable side effect.
   *
   * <p>The three completion flags must be computed from the same atomic
   * snapshot and must not be optimistic caller defaults.</p>
   */
  public static Classification classify(
      long requestedMaxFE,
      long actualFE,
      long lastCompletedAtomicBoundaryFE,
      long qPhaseFE,
      boolean allowTerminalPartialFormalQPhase,
      String terminationKind,
      String checkpointBoundary,
      boolean workingPopulationNDComplete,
      boolean decisionArchiveFrontComplete,
      boolean observedFullFrontComplete,
      long observerErrors) {
    if (requestedMaxFE <= 0L || actualFE <= 0L || qPhaseFE <= 0L) {
      return Classification.REJECTED;
    }
    if (actualFE > requestedMaxFE) {
      return Classification.REJECTED;
    }

    long remaining = requestedMaxFE - actualFE;
    if (remaining < 0L || remaining >= qPhaseFE) {
      return Classification.REJECTED;
    }
    if (actualFE != lastCompletedAtomicBoundaryFE) {
      return Classification.REJECTED;
    }
    if (allowTerminalPartialFormalQPhase) {
      return Classification.REJECTED;
    }
    if (!TERMINATION_KIND.equals(terminationKind)) {
      return Classification.REJECTED;
    }
    if (!CHECKPOINT_BOUNDARY.equals(checkpointBoundary)) {
      return Classification.REJECTED;
    }
    if (!workingPopulationNDComplete || !decisionArchiveFrontComplete
        || !observedFullFrontComplete) {
      return Classification.REJECTED;
    }
    if (observerErrors != 0L) {
      return Classification.REJECTED;
    }
    return Classification.ACCEPTED;
  }
}
