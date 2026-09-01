package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1 checkpoint front observer.
 *
 * <p>Pure observation: captures workingPopulationND / decisionArchiveFront /
 * observedFullFront at atomic-phase boundaries when {@code actualFE} first
 * reaches or exceeds a nominal checkpoint FE.  It never writes back, never
 * consumes randomness and never changes FE.  Each capture deep-copies the
 * input vectors into an immutable, Pareto-filtered, deduplicated, stably
 * sorted snapshot.
 */
public final class V35CheckpointFrontObserver {
  public static final String VERSION = "V35_MIDHORIZON_V3_1";
  public static final long MAX_CHECKPOINT_OVERSHOOT_FE = 5000L;
  public static final long DEFAULT_Q_PHASE_FE = 5000L;
  /** The only callback boundary accepted by the formal diagnostic contract. */
  public static final String ATOMIC_BOUNDARY =
      "COMPLETE_QG_QP_GLOBAL_OFFSPRING_CATA_INHERITED_LS_PDDR_POST_SAMPLE";
  public static final String BOUNDARY_Q_ROUND =
      "COMPLETE_QG_QP_GLOBAL_OFFSPRING_BATCH";
  public static final String BOUNDARY_CATA_BATCH = "COMPLETE_CATA_BATCH";
  public static final String BOUNDARY_INHERITED_LS_BATCH = "COMPLETE_INHERITED_LS_BATCH";
  public static final String BOUNDARY_PDDR_POST_SAMPLE = "COMPLETE_PDDR_POST_SAMPLE";
  public static final String TERMINATION_KIND_PHASE_CONSISTENT_BUDGET =
      "PHASE_CONSISTENT_BUDGET_TERMINATION";
  public static final String CHECKPOINT_KIND_ATOMIC_BOUNDARY = "ATOMIC_BOUNDARY";
  public static final String CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT =
      "PHASE_CONSISTENT_TERMINAL";
  public static final String CHECKPOINT_KIND_UNOBSERVABLE = "UNOBSERVABLE";
  public static final String REAL_ATOMIC_RUN_END_SNAPSHOT =
      "REAL_ATOMIC_RUN_END_SNAPSHOT";
  private static final String WORKING_POPULATION_SOURCE = "ALGORITHM_WORKING_POPULATION";
  private static final String DECISION_ARCHIVE_SOURCE = "ALGORITHM_DECISION_ARCHIVE";
  private static final String OBSERVED_FULL_FRONT_SOURCE = "V35_PASSIVE_EVALUATION_ARCHIVE";
  private static final String RUN_END_BOUNDARY = "RUN_END_NO_ATOMIC_SNAPSHOT";
  private static final int[] OBJECTIVES = new int[]{0, 1, 6};

  /**
   * Explicit reasons for a field that is not observable in this architecture.
   * These values are evidence labels, never synthetic objective or sequence
   * values.
   */
  public enum UnobservableReason {
    NONE,
    CHECKPOINT_OVERSHOOT,
    CHECKPOINT_NOT_REACHED,
    PHASE_BOUNDARY_NOT_OBSERVED,
    FRONT_NOT_EXPOSED,
    FRONT_EMPTY,
    INVALID_ACTUAL_FE,
    FRONT_OBSERVATION_ERROR,
    TERMINAL_SNAPSHOT_NOT_OBSERVABLE,
    TERMINAL_ACTUAL_OVER_REQUESTED,
    TERMINAL_REMAINING_NOT_UNDER_Q_PHASE,
    TERMINAL_ATOMIC_BOUNDARY_MISMATCH,
    TERMINAL_PARTIAL_FORMAL_Q_PHASE,
    TERMINAL_KIND_MISMATCH
  }

  private boolean enabled;
  private final long[] nominalCheckpoints;
  private int nextCheckpoint;
  private final String runId;
  private final String sourceJarSha256;
  private final String configurationHash;
  private final String instanceHash;
  private final long seed;
  private final String arm;
  private final String telemetryMode;
  private long observerErrors;
  private long observerExecutionErrors;
  private long unobservableCheckpointCount;
  private long observedCheckpointCount;
  private long nominalCheckpointNotExactlyReachedCount;
  private long terminalSnapshotCount;
  private long lastCompletedAtomicBoundaryFE = -1L;
  private int lastCompletedGeneration = -1;
  private int lastCompletedOuterCycle = -1;
  private int lastCompletedQRound = -1;
  private String lastCompletedAtomicBoundary = "NOT_APPLICABLE";
  private String lastTerminationKind = "NOT_APPLICABLE";
  private String lastCheckpointKind = "NOT_APPLICABLE";
  private String lastCheckpointAtomicBoundary = "NOT_APPLICABLE";
  private long lastNominalCheckpointFE = -1L;
  private long lastActualCheckpointFE = -1L;
  private long lastActualSnapshotFE = -1L;
  private long lastCheckpointDeltaFE = Long.MIN_VALUE;
  private FrontSnapshot lastCompletedSnapshot;
  private V35TerminalCheckpointContract.Classification terminalClassification;
  private boolean finished;
  private final EnumMap<UnobservableReason, Long> unobservableReasons =
      new EnumMap<UnobservableReason, Long>(UnobservableReason.class);
  private final List<String> rows = new ArrayList<String>();

  private static final class FrontSnapshot {
    private final List<PermutationSolution<Integer>> workingPopulationND;
    private final List<PermutationSolution<Integer>> decisionArchiveFront;
    private final List<PermutationSolution<Integer>> observedFullFront;

    private FrontSnapshot(List<PermutationSolution<Integer>> workingPopulationND,
        List<PermutationSolution<Integer>> decisionArchiveFront,
        List<PermutationSolution<Integer>> observedFullFront) {
      this.workingPopulationND = Collections.unmodifiableList(
          new ArrayList<PermutationSolution<Integer>>(workingPopulationND));
      this.decisionArchiveFront = Collections.unmodifiableList(
          new ArrayList<PermutationSolution<Integer>>(decisionArchiveFront));
      this.observedFullFront = Collections.unmodifiableList(
          new ArrayList<PermutationSolution<Integer>>(observedFullFront));
    }

    private boolean hasDistinguishableFronts() {
      String working = frontSignature(workingPopulationND);
      String decision = frontSignature(decisionArchiveFront);
      String observed = frontSignature(observedFullFront);
      return !working.equals(decision) && !working.equals(observed)
          && !decision.equals(observed);
    }
  }

  public V35CheckpointFrontObserver(long[] nominalCheckpoints, String runId,
      String sourceJarSha256, String configurationHash, String instanceHash,
      long seed, String arm, boolean enabled) {
    this.nominalCheckpoints = nominalCheckpoints == null ? new long[0] : nominalCheckpoints.clone();
    validateCheckpoints(this.nominalCheckpoints);
    this.runId = runId;
    this.sourceJarSha256 = sourceJarSha256;
    this.configurationHash = configurationHash;
    this.instanceHash = instanceHash;
    this.seed = seed;
    this.arm = arm;
    this.enabled = enabled;
    this.telemetryMode = enabled ? "ON" : "OFF";
    this.finished = false;
  }

  public void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      rows.clear();
      nextCheckpoint = 0;
      observerErrors = 0L;
      observerExecutionErrors = 0L;
      unobservableCheckpointCount = 0L;
      observedCheckpointCount = 0L;
      nominalCheckpointNotExactlyReachedCount = 0L;
      terminalSnapshotCount = 0L;
      lastCompletedAtomicBoundaryFE = -1L;
      lastCompletedGeneration = -1;
      lastCompletedOuterCycle = -1;
      lastCompletedQRound = -1;
      lastCompletedAtomicBoundary = "NOT_APPLICABLE";
      lastTerminationKind = "NOT_APPLICABLE";
      lastCheckpointKind = "NOT_APPLICABLE";
      lastCheckpointAtomicBoundary = "NOT_APPLICABLE";
      lastNominalCheckpointFE = -1L;
      lastActualCheckpointFE = -1L;
      lastActualSnapshotFE = -1L;
      lastCheckpointDeltaFE = Long.MIN_VALUE;
      lastCompletedSnapshot = null;
      terminalClassification = null;
      finished = false;
      unobservableReasons.clear();
    }
  }

  public boolean isEnabled() { return enabled; }

  /**
   * Called only after a complete Qg/Qp round, global offspring batch, CA-TA
   * batch, inherited-LS batch, and PDDR post-sampling.  When enabled and
   * {@code actualFE} has first reached the next nominal checkpoint, captures
   * the three front types.  Inputs are defensive-copied; null/empty fronts
   * remain NOT_APPLICABLE with an explicit reason.
   */
  public void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront) {
    onAtomicPhaseEnd(actualFE, generation, outerCycle, qRound, workingPopulation,
        decisionArchiveFront, observedFullFront, ATOMIC_BOUNDARY, false);
  }

  public void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront,
      String atomicBoundary) {
    onAtomicPhaseEnd(actualFE, generation, outerCycle, qRound, workingPopulation,
        decisionArchiveFront, observedFullFront, atomicBoundary, true);
  }

  private void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront,
      String atomicBoundary, boolean explicitBoundary) {
    if (!enabled || finished) return;
    try {
      String boundary = atomicBoundary == null || atomicBoundary.length() == 0
          ? ATOMIC_BOUNDARY : atomicBoundary;
      if (!isAtomicBoundary(boundary)) return;

      // Record the real phase boundary before reading front data.  This is
      // intentionally independent of snapshot validity and therefore also
      // survives a throwing front accessor.
      boolean terminalAlreadyPublished = nextCheckpoint >= nominalCheckpoints.length;
      if (actualFE >= 0L) {
        rememberAtomicBoundary(actualFE, generation, outerCycle, qRound, boundary, null,
            !terminalAlreadyPublished);
      }
      FrontSnapshot snapshot = strictSnapshot(workingPopulation, decisionArchiveFront,
          observedFullFront);
      if (actualFE >= 0L) {
        rememberAtomicBoundary(actualFE, generation, outerCycle, qRound, boundary, snapshot,
            !terminalAlreadyPublished);
      }
      boolean terminalCaptureRequested = explicitBoundary && isTerminalBoundary(boundary);
      while (nextCheckpoint < nominalCheckpoints.length) {
        long nominal = nominalCheckpoints[nextCheckpoint];
        if (actualFE < nominal) break;
        boolean finalPending = nextCheckpoint == nominalCheckpoints.length - 1;
        long overshoot = actualFE - nominal;
        String feRow = nominal + "," + actualFE + "," + overshoot + "," + generation
            + "," + outerCycle + "," + qRound;
        // Frozen contract: 0 <= actualSnapshotFE - nominalCheckpointFE < 5000.
        if (overshoot >= MAX_CHECKPOINT_OVERSHOOT_FE) {
          recordUnobservable(UnobservableReason.CHECKPOINT_OVERSHOOT);
          appendUnobservable(feRow, "workingPopulationND",
              UnobservableReason.CHECKPOINT_OVERSHOOT, boundary,
              CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
              TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
          appendUnobservable(feRow, "decisionArchiveFront",
              UnobservableReason.CHECKPOINT_OVERSHOOT, boundary,
              CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
              TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
          appendUnobservable(feRow, "observedFullFront",
              UnobservableReason.CHECKPOINT_OVERSHOOT, boundary,
              CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
              TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
          nextCheckpoint++;
          continue;
        }
        if (snapshot != null && (!terminalCaptureRequested || snapshot.hasDistinguishableFronts())) {
          String checkpointKind = finalPending && actualFE != nominal
              ? CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT : CHECKPOINT_KIND_ATOMIC_BOUNDARY;
          String checkpointBoundary = finalPending && actualFE != nominal
              ? REAL_ATOMIC_RUN_END_SNAPSHOT : boundary;
          appendSnapshot(feRow, snapshot, UnobservableReason.NONE, checkpointBoundary,
              checkpointKind, actualFE, overshoot,
              TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
          // An exact terminal checkpoint can be consumed by this real atomic
          // boundary before onRunEnd is invoked.  Persist its accounting here
          // so the later lifecycle hook can classify the already-published
          // snapshot instead of leaving all terminal FE fields at -1.
          if (terminalCaptureRequested && finalPending) {
            rememberLastCheckpoint(nominal, actualFE, actualFE, overshoot,
                checkpointKind, checkpointBoundary,
                TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
            if (actualFE != nominal) {
              nominalCheckpointNotExactlyReachedCount++;
              terminalSnapshotCount++;
            }
          }
        } else if (terminalCaptureRequested) {
          recordUnobservable(UnobservableReason.TERMINAL_SNAPSHOT_NOT_OBSERVABLE);
          appendTerminalUnavailable(feRow, boundary, actualFE, overshoot,
              workingPopulation, decisionArchiveFront, observedFullFront);
        } else {
          // Keep the historical non-terminal rows observable per front.  A
          // terminal row is stricter: it is emitted only from one complete
          // three-front snapshot in onRunEnd(..., state...).
          appendRegularFront(feRow, "workingPopulationND", WORKING_POPULATION_SOURCE,
              workingPopulation, boundary);
          appendRegularFront(feRow, "decisionArchiveFront", DECISION_ARCHIVE_SOURCE,
              decisionArchiveFront, boundary);
          appendRegularFront(feRow, "observedFullFront", OBSERVED_FULL_FRONT_SOURCE,
              observedFullFront, boundary);
        }
        if (snapshot != null || !terminalCaptureRequested) observedCheckpointCount++;
        nextCheckpoint++;
      }
    } catch (RuntimeException error) {
      observerErrors++;
      observerExecutionErrors++;
    }
  }

  /**
   * Finalizes the observer at run termination.  Pending checkpoints are not
   * silently dropped: if no complete atomic boundary reached one, the row is
   * emitted as unavailable and counted as an unobservable checkpoint.
   */
  public void onRunEnd(long actualFE, int generation, int outerCycle, int qRound) {
    onRunEnd(actualFE, generation, outerCycle, qRound, null, null, null,
        TERMINATION_KIND_PHASE_CONSISTENT_BUDGET, false);
  }

  /**
   * Finalizes the run using the actual algorithm state at termination.  The
   * three lists are treated as one atomic snapshot: if any list is absent,
   * empty, or contains a non-finite objective, no terminal front is emitted.
   */
  public void onRunEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront,
      String terminationKind) {
    onRunEnd(actualFE, generation, outerCycle, qRound, workingPopulation,
        decisionArchiveFront, observedFullFront, terminationKind, false);
  }

  /**
   * Finalizes a run with an explicit phase-consistent terminal contract.  A
   * terminal row is legal only for the next nominal checkpoint and only when
   * the supplied state is a real snapshot at the last accepted atomic FE.
   */
  public void onRunEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront,
      String terminationKind, boolean allowTerminalPartialFormalQPhase) {
    if (!enabled || finished) return;
    try {
      String finalTerminationKind = terminationKind == null || terminationKind.length() == 0
          ? TERMINATION_KIND_PHASE_CONSISTENT_BUDGET : terminationKind;
      boolean stateProvided = workingPopulation != null || decisionArchiveFront != null
          || observedFullFront != null;

      // Only a snapshot captured by a real terminal boundary can be
      // published.  Explicit state is accepted only when it is asserted to
      // correspond to that already-recorded boundary; it never creates a
      // synthetic boundary or replaces an existing snapshot.
      FrontSnapshot terminalSnapshot = actualFE == lastCompletedAtomicBoundaryFE
          && isTerminalBoundary(lastCompletedAtomicBoundary) ? lastCompletedSnapshot : null;
      if (terminalSnapshot == null && stateProvided
          && actualFE == lastCompletedAtomicBoundaryFE
          && isTerminalBoundary(lastCompletedAtomicBoundary)) {
        terminalSnapshot = strictSnapshot(workingPopulation, decisionArchiveFront,
            observedFullFront);
      }

      // If the final nominal checkpoint was already observed exactly, retain
      // the existing exact-checkpoint semantics while exposing the pure
      // terminal classification to the driver.
      if (nominalCheckpoints.length > 0
          && nextCheckpoint >= nominalCheckpoints.length
          && lastNominalCheckpointFE == nominalCheckpoints[nominalCheckpoints.length - 1]) {
        terminalClassification = classifyTerminal(lastNominalCheckpointFE,
            lastActualCheckpointFE, lastCompletedSnapshot, finalTerminationKind,
            allowTerminalPartialFormalQPhase);
      }

      while (nextCheckpoint < nominalCheckpoints.length) {
        long nominal = nominalCheckpoints[nextCheckpoint];
        boolean finalPending = nextCheckpoint == nominalCheckpoints.length - 1;
        if (finalPending) {
          terminalClassification = classifyTerminal(nominal, actualFE, terminalSnapshot,
              finalTerminationKind, allowTerminalPartialFormalQPhase);
        }
        if (finalPending && terminalClassification
            == V35TerminalCheckpointContract.Classification.ACCEPTED) {
          long delta = actualFE - nominal;
          String feRow = nominal + "," + actualFE + "," + delta + ","
              + lastCompletedGeneration + "," + lastCompletedOuterCycle + ","
              + lastCompletedQRound;
          String kind = actualFE == nominal ? CHECKPOINT_KIND_ATOMIC_BOUNDARY
              : CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT;
          String boundary = actualFE == nominal ? lastCompletedAtomicBoundary
              : REAL_ATOMIC_RUN_END_SNAPSHOT;
          appendSnapshot(feRow, terminalSnapshot, UnobservableReason.NONE,
              boundary, kind, actualFE, delta, finalTerminationKind);
          if (actualFE != nominal) {
            nominalCheckpointNotExactlyReachedCount++;
            terminalSnapshotCount++;
          }
          observedCheckpointCount++;
          rememberLastCheckpoint(nominal, actualFE, actualFE, delta, kind,
              boundary, finalTerminationKind);
          nextCheckpoint++;
          continue;
        }

        UnobservableReason reason = finalPending
            ? failureReason(actualFE, nominal, finalTerminationKind,
                allowTerminalPartialFormalQPhase, terminalSnapshot)
            : actualFE < 0L
                ? UnobservableReason.INVALID_ACTUAL_FE
                : actualFE < nominal
                    ? UnobservableReason.CHECKPOINT_NOT_REACHED
                    : UnobservableReason.PHASE_BOUNDARY_NOT_OBSERVED;
        recordUnobservable(reason);
        String feRow = nominal + ",NOT_APPLICABLE,NOT_APPLICABLE," + generation + ","
            + outerCycle + "," + qRound;
        appendUnobservable(feRow, "workingPopulationND", reason, RUN_END_BOUNDARY,
            CHECKPOINT_KIND_UNOBSERVABLE, -1L, 0L, finalTerminationKind);
        appendUnobservable(feRow, "decisionArchiveFront", reason, RUN_END_BOUNDARY,
            CHECKPOINT_KIND_UNOBSERVABLE, -1L, 0L, finalTerminationKind);
        appendUnobservable(feRow, "observedFullFront", reason, RUN_END_BOUNDARY,
            CHECKPOINT_KIND_UNOBSERVABLE, -1L, Long.MIN_VALUE, finalTerminationKind);
        rememberLastCheckpoint(nominal, -1L, -1L, Long.MIN_VALUE,
            CHECKPOINT_KIND_UNOBSERVABLE, RUN_END_BOUNDARY, finalTerminationKind);
        nextCheckpoint++;
      }
      finished = true;
    } catch (RuntimeException error) {
      observerErrors++;
      observerExecutionErrors++;
    }
  }

  private UnobservableReason failureReason(long actualFE, long nominal,
      String terminationKind, boolean allowTerminalPartialFormalQPhase,
      FrontSnapshot terminalSnapshot) {
    if (observerErrors != 0L || observerExecutionErrors != 0L) {
      // Keep the historical, explicit reason used by the V3 observer test.
      return UnobservableReason.PHASE_BOUNDARY_NOT_OBSERVED;
    }
    if (actualFE <= 0L) return UnobservableReason.INVALID_ACTUAL_FE;
    if (actualFE < nominal) {
      long remaining = nominal - actualFE;
      return remaining >= DEFAULT_Q_PHASE_FE
          ? UnobservableReason.TERMINAL_REMAINING_NOT_UNDER_Q_PHASE
          : UnobservableReason.CHECKPOINT_NOT_REACHED;
    }
    if (actualFE > nominal) return UnobservableReason.TERMINAL_ACTUAL_OVER_REQUESTED;
    if (allowTerminalPartialFormalQPhase) {
      return UnobservableReason.TERMINAL_PARTIAL_FORMAL_Q_PHASE;
    }
    if (!TERMINATION_KIND_PHASE_CONSISTENT_BUDGET.equals(terminationKind)) {
      return UnobservableReason.TERMINAL_KIND_MISMATCH;
    }
    if (!isTerminalBoundary(lastCompletedAtomicBoundary)
        || actualFE != lastCompletedAtomicBoundaryFE) {
      return UnobservableReason.TERMINAL_ATOMIC_BOUNDARY_MISMATCH;
    }
    return terminalSnapshot == null || !terminalSnapshot.hasDistinguishableFronts()
        ? UnobservableReason.TERMINAL_SNAPSHOT_NOT_OBSERVABLE
        : UnobservableReason.TERMINAL_SNAPSHOT_NOT_OBSERVABLE;
  }

  /** Alias kept explicit for callers that name lifecycle hooks as finalize. */
  public void finish(long actualFE, int generation, int outerCycle, int qRound) {
    onRunEnd(actualFE, generation, outerCycle, qRound);
  }

  private static boolean isAtomicBoundary(String boundary) {
    return BOUNDARY_Q_ROUND.equals(boundary) || BOUNDARY_CATA_BATCH.equals(boundary)
        || BOUNDARY_INHERITED_LS_BATCH.equals(boundary)
        || BOUNDARY_PDDR_POST_SAMPLE.equals(boundary) || ATOMIC_BOUNDARY.equals(boundary);
  }

  private static boolean isTerminalBoundary(String boundary) {
    return BOUNDARY_PDDR_POST_SAMPLE.equals(boundary) || ATOMIC_BOUNDARY.equals(boundary);
  }

  private static String frontSignature(List<PermutationSolution<Integer>> values) {
    StringBuilder result = new StringBuilder();
    for (PermutationSolution<Integer> value : values) {
      if (result.length() > 0) result.append('|');
      for (int objective : OBJECTIVES) {
        if (result.length() > 0 && result.charAt(result.length() - 1) != '|') {
          result.append(':');
        }
        result.append(Long.toHexString(Double.doubleToLongBits(value.getObjective(objective))));
      }
    }
    return result.toString();
  }

  private static UnobservableReason terminalUnavailableReason(
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront) {
    List<List<PermutationSolution<Integer>>> fronts = new ArrayList<
        List<PermutationSolution<Integer>>>();
    fronts.add(workingPopulation);
    fronts.add(decisionArchiveFront);
    fronts.add(observedFullFront);
    boolean empty = false;
    for (List<PermutationSolution<Integer>> front : fronts) {
      if (front == null) return UnobservableReason.FRONT_NOT_EXPOSED;
      if (front.isEmpty()) empty = true;
      for (PermutationSolution<Integer> solution : front) {
        if (solution == null) return UnobservableReason.FRONT_NOT_EXPOSED;
        for (int objective : OBJECTIVES) {
          if (!Double.isFinite(solution.getObjective(objective))) {
            return UnobservableReason.FRONT_OBSERVATION_ERROR;
          }
        }
      }
    }
    return empty ? UnobservableReason.FRONT_EMPTY
        : UnobservableReason.TERMINAL_SNAPSHOT_NOT_OBSERVABLE;
  }

  private void appendTerminalUnavailable(String feRow, String boundary, long actualFE,
      long overshoot, List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront) {
    UnobservableReason reason = terminalUnavailableReason(workingPopulation,
        decisionArchiveFront, observedFullFront);
    appendUnobservable(feRow, "workingPopulationND", reason, boundary,
        CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
        TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
    appendUnobservable(feRow, "decisionArchiveFront", reason, boundary,
        CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
        TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
    appendUnobservable(feRow, "observedFullFront", reason, boundary,
        CHECKPOINT_KIND_UNOBSERVABLE, actualFE, overshoot,
        TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
  }

  private V35TerminalCheckpointContract.Classification classifyTerminal(long requestedMaxFE,
      long actualFE, FrontSnapshot snapshot, String terminationKind,
      boolean allowTerminalPartialFormalQPhase) {
    boolean complete = snapshot != null && snapshot.hasDistinguishableFronts();
    String contractBoundary = isTerminalBoundary(lastCompletedAtomicBoundary)
        ? ATOMIC_BOUNDARY : lastCompletedAtomicBoundary;
    return V35TerminalCheckpointContract.classify(requestedMaxFE, actualFE,
        lastCompletedAtomicBoundaryFE, DEFAULT_Q_PHASE_FE,
        allowTerminalPartialFormalQPhase, terminationKind, contractBoundary,
        complete, complete, complete, observerErrors + observerExecutionErrors);
  }

  private static FrontSnapshot strictSnapshot(
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront) {
    if (!validFront(workingPopulation) || !validFront(decisionArchiveFront)
        || !validFront(observedFullFront)) return null;
    try {
      FrontSnapshot snapshot = new FrontSnapshot(filterNonDominated(workingPopulation),
          filterNonDominated(decisionArchiveFront), filterNonDominated(observedFullFront));
      return snapshot.hasDistinguishableFronts() ? snapshot : null;
    } catch (RuntimeException error) {
      return null;
    }
  }

  private static boolean validFront(List<PermutationSolution<Integer>> values) {
    if (values == null || values.isEmpty()) return false;
    for (PermutationSolution<Integer> value : values) {
      if (value == null) return false;
      for (int objective : OBJECTIVES) {
        if (!Double.isFinite(value.getObjective(objective))) return false;
      }
    }
    return true;
  }

  private void rememberAtomicBoundary(long actualFE, int generation, int outerCycle,
      int qRound, String boundary, FrontSnapshot snapshot, boolean replaceTerminalSnapshot) {
    lastCompletedAtomicBoundaryFE = actualFE;
    lastCompletedGeneration = generation;
    lastCompletedOuterCycle = outerCycle;
    lastCompletedQRound = qRound;
    lastCompletedAtomicBoundary = boundary;
    if (isTerminalBoundary(boundary) && replaceTerminalSnapshot) {
      lastCompletedSnapshot = snapshot;
    }
  }

  private void rememberLastCheckpoint(long nominal, long actualCheckpoint,
      long actualSnapshot, long delta, String kind, String boundary, String terminationKind) {
    lastNominalCheckpointFE = nominal;
    lastActualCheckpointFE = actualCheckpoint;
    lastActualSnapshotFE = actualSnapshot;
    lastCheckpointDeltaFE = delta;
    lastCheckpointKind = kind;
    lastCheckpointAtomicBoundary = boundary;
    lastTerminationKind = terminationKind;
  }

  private void recordUnobservable(UnobservableReason reason) {
    unobservableCheckpointCount++;
    Long previous = unobservableReasons.get(reason);
    unobservableReasons.put(reason, previous == null ? 1L : previous + 1L);
  }

  private static void validateCheckpoints(long[] checkpoints) {
    long previous = Long.MIN_VALUE;
    for (long checkpoint : checkpoints) {
      if (checkpoint < 0L || checkpoint <= previous) {
        throw new IllegalArgumentException(
            "checkpoint FEs must be non-negative and strictly increasing");
      }
      previous = checkpoint;
    }
  }

  private void appendSnapshot(String feRow, FrontSnapshot snapshot,
      UnobservableReason reason, String boundary, String checkpointKind,
      long actualCheckpointFE, long checkpointDeltaFE, String terminationKind) {
    appendFront("workingPopulationND", WORKING_POPULATION_SOURCE, feRow,
        snapshot.workingPopulationND, reason, boundary, checkpointKind,
        actualCheckpointFE, checkpointDeltaFE, terminationKind);
    appendFront("decisionArchiveFront", DECISION_ARCHIVE_SOURCE, feRow,
        snapshot.decisionArchiveFront, reason, boundary, checkpointKind,
        actualCheckpointFE, checkpointDeltaFE, terminationKind);
    appendFront("observedFullFront", OBSERVED_FULL_FRONT_SOURCE, feRow,
        snapshot.observedFullFront, reason, boundary, checkpointKind,
        actualCheckpointFE, checkpointDeltaFE, terminationKind);
  }

  private void appendRegularFront(String feRow, String frontType, String frontSource,
      List<PermutationSolution<Integer>> input, String boundary) {
    if (input != null && !input.isEmpty()) {
      appendFront(frontType, frontSource, feRow, filterNonDominated(input),
          UnobservableReason.NONE, boundary, CHECKPOINT_KIND_ATOMIC_BOUNDARY,
          parseActualFE(feRow), parseDeltaFE(feRow), TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
    } else {
      appendNa(feRow, frontType, frontSource,
          input == null ? UnobservableReason.FRONT_NOT_EXPOSED : UnobservableReason.FRONT_EMPTY,
          boundary, CHECKPOINT_KIND_ATOMIC_BOUNDARY, parseActualFE(feRow),
          parseDeltaFE(feRow), TERMINATION_KIND_PHASE_CONSISTENT_BUDGET);
    }
  }

  private void appendFront(String frontType, String frontSource, String feRow,
      List<PermutationSolution<Integer>> front, UnobservableReason reason, String boundary,
      String checkpointKind, long actualCheckpointFE, long checkpointDeltaFE,
      String terminationKind) {
    for (PermutationSolution<Integer> solution : front) {
      rows.add(baseRow(feRow) + frontType + "," + reason.name() + ","
          + fingerprint(solution) + ","
          + solution.getObjective(0) + "," + solution.getObjective(1) + ","
          + solution.getObjective(6) + "," + boundary + "," + checkpointKind + ","
          + actualCheckpointText(actualCheckpointFE) + ","
          + deltaText(checkpointDeltaFE) + "," + frontSource + "," + terminationKind);
    }
  }

  private void appendNa(String feRow, String frontType, String frontSource,
      UnobservableReason reason, String boundary, String checkpointKind,
      long actualCheckpointFE, long checkpointDeltaFE, String terminationKind) {
    rows.add(baseRow(feRow) + frontType + "," + reason.name()
        + ",NOT_APPLICABLE,NOT_APPLICABLE,NOT_APPLICABLE,NOT_APPLICABLE," + boundary + ","
        + checkpointKind + "," + actualCheckpointText(actualCheckpointFE) + ","
        + deltaText(checkpointDeltaFE) + "," + frontSource + "," + terminationKind);
  }

  private void appendUnobservable(String feRow, String frontType,
      UnobservableReason reason, String boundary, String checkpointKind,
      long actualCheckpointFE, long checkpointDeltaFE, String terminationKind) {
    String frontSource = frontType.equals("workingPopulationND")
        ? WORKING_POPULATION_SOURCE
        : frontType.equals("decisionArchiveFront")
            ? DECISION_ARCHIVE_SOURCE : OBSERVED_FULL_FRONT_SOURCE;
    rows.add(baseRow(feRow) + frontType + "," + reason.name()
        + ",CHECKPOINT_UNOBSERVABLE,NOT_APPLICABLE,NOT_APPLICABLE,NOT_APPLICABLE,"
        + boundary + "," + checkpointKind + "," + actualCheckpointText(actualCheckpointFE)
        + "," + deltaText(checkpointDeltaFE) + "," + frontSource + "," + terminationKind);
  }

  private String baseRow(String feRow) {
    return runId + "," + sourceJarSha256 + "," + configurationHash + ","
        + instanceHash + "," + seed + "," + arm + "," + telemetryMode + "," + feRow + ",";
  }

  private static String actualCheckpointText(long value) {
    return value < 0L ? "NOT_APPLICABLE" : String.valueOf(value);
  }

  private static String deltaText(long value) {
    return value == Long.MIN_VALUE ? "NOT_APPLICABLE" : String.valueOf(value);
  }

  private static long parseActualFE(String feRow) {
    String[] values = feRow.split(",", -1);
    return values.length > 1 && isLong(values[1]) ? Long.parseLong(values[1]) : -1L;
  }

  private static long parseDeltaFE(String feRow) {
    String[] values = feRow.split(",", -1);
    return values.length > 2 && isLong(values[2]) ? Long.parseLong(values[2]) : Long.MIN_VALUE;
  }

  private static boolean isLong(String value) {
    if (value == null || value.length() == 0) return false;
    try {
      Long.parseLong(value);
      return true;
    } catch (NumberFormatException error) {
      return false;
    }
  }

  /** Strict Pareto filter after exact tri-objective dedup, with stable sort. */
  private static List<PermutationSolution<Integer>> filterNonDominated(
      List<PermutationSolution<Integer>> input) {
    List<PermutationSolution<Integer>> dedup = new ArrayList<PermutationSolution<Integer>>();
    java.util.Set<String> seen = new java.util.HashSet<String>();
    for (PermutationSolution<Integer> solution : input) {
      String key = objectiveKey(solution);
      if (seen.add(key)) dedup.add(copy(solution));
    }
    List<PermutationSolution<Integer>> nondominated = new ArrayList<PermutationSolution<Integer>>();
    for (int left = 0; left < dedup.size(); left++) {
      boolean dominated = false;
      for (int right = 0; right < dedup.size(); right++) {
        if (left == right) continue;
        if (strictlyDominates(dedup.get(right), dedup.get(left))) {
          dominated = true;
          break;
        }
      }
      if (!dominated) nondominated.add(dedup.get(left));
    }
    Collections.sort(nondominated, new Comparator<PermutationSolution<Integer>>() {
      @Override public int compare(PermutationSolution<Integer> a,
          PermutationSolution<Integer> b) {
        for (int objective : OBJECTIVES) {
          int comparison = Double.compare(a.getObjective(objective), b.getObjective(objective));
          if (comparison != 0) return comparison;
        }
        return fingerprint(a).compareTo(fingerprint(b));
      }
    });
    return nondominated;
  }

  private static boolean strictlyDominates(
      PermutationSolution<Integer> left, PermutationSolution<Integer> right) {
    boolean noWorse = left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
    boolean strictlyBetter = left.getObjective(0) < right.getObjective(0)
        || left.getObjective(1) < right.getObjective(1)
        || left.getObjective(6) < right.getObjective(6);
    return noWorse && strictlyBetter;
  }

  private static String objectiveKey(PermutationSolution<Integer> value) {
    return Long.toHexString(Double.doubleToLongBits(value.getObjective(0))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(1))) + ':'
        + Long.toHexString(Double.doubleToLongBits(value.getObjective(6)));
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copy(PermutationSolution<Integer> value) {
    return (PermutationSolution<Integer>) value.copy();
  }

  private static String fingerprint(PermutationSolution<Integer> value) {
    return stableId(
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController.fingerprint(value));
  }

  /** Deterministic comma-free hex id so CSV field splitting stays safe. */
  private static String stableId(String raw) {
    try {
      byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
          .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte b : digest) result.append(String.format("%02x", b & 0xff));
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public String toCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "nominalCheckpointFE,actualSnapshotFE,overshootFE,generation,formalOuterCycle,qRound,"
        + "frontType,unobservableReason,solutionFingerprint,Cmax,TEC,TWC,atomicBoundary,"
        + "checkpointKind,actualCheckpointFE,checkpointDeltaFE,frontSource,terminationKind\n");
    for (String row : rows) out.append(row).append('\n');
    return out.toString();
  }

  public long getObserverErrors() { return observerErrors; }
  public long getObserverExecutionErrors() { return observerExecutionErrors; }
  public long getUnobservableCheckpointCount() { return unobservableCheckpointCount; }
  public long getObservedCheckpointCount() { return observedCheckpointCount; }
  public long getNominalCheckpointNotExactlyReachedCount() {
    return nominalCheckpointNotExactlyReachedCount;
  }
  public long getTerminalSnapshotCount() { return terminalSnapshotCount; }
  public long getLastCompletedAtomicBoundaryFE() { return lastCompletedAtomicBoundaryFE; }
  public String getLastCompletedAtomicBoundary() { return lastCompletedAtomicBoundary; }
  public String getLastCheckpointKind() { return lastCheckpointKind; }
  public long getLastNominalCheckpointFE() { return lastNominalCheckpointFE; }
  public long getLastActualCheckpointFE() { return lastActualCheckpointFE; }
  public long getLastActualSnapshotFE() { return lastActualSnapshotFE; }
  public long getLastCheckpointDeltaFE() { return lastCheckpointDeltaFE; }
  public String getLastTerminationKind() { return lastTerminationKind; }
  public String getLastCheckpointAtomicBoundary() { return lastCheckpointAtomicBoundary; }
  public String getTerminalClassification() {
    return terminalClassification == null ? "NOT_APPLICABLE" : terminalClassification.name();
  }
  public boolean isTerminalCheckpointAccepted() {
    return terminalClassification == V35TerminalCheckpointContract.Classification.ACCEPTED;
  }
  public boolean hasTerminalSnapshot() { return terminalSnapshotCount > 0L; }
  public int getNominalCheckpointCount() { return nominalCheckpoints.length; }
  public int getPendingCheckpointCount() {
    return nominalCheckpoints.length - nextCheckpoint;
  }
  public boolean isComplete() { return nextCheckpoint >= nominalCheckpoints.length; }
  public String getUnobservableReasonSummary() {
    StringBuilder summary = new StringBuilder();
    for (Map.Entry<UnobservableReason, Long> entry : unobservableReasons.entrySet()) {
      if (entry.getValue() == null || entry.getValue() <= 0L) continue;
      if (summary.length() > 0) summary.append(',');
      summary.append(entry.getKey().name()).append(':').append(entry.getValue());
    }
    return summary.toString();
  }
  public int getRowCount() { return rows.size(); }
}
