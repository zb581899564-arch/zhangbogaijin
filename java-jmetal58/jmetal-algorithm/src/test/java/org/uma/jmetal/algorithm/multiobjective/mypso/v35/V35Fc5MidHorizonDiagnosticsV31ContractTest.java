package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1 protocol tests.
 *
 * <p>This is a pure in-memory contract test.  It never starts an algorithm,
 * reads an instance, writes evidence, or treats the fixtures below as run
 * evidence.  The budget cases are synthetic state-machine inputs whose only
 * purpose is to exercise the production terminal classifier.</p>
 *
 * <p>B must provide the small production seam documented in
 * {@code docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/21-terminal-checkpoint-contract/}:
 * {@code V35TerminalCheckpointContract.classify(...)}.  Reflection keeps this
 * test compilable while B is implementing that production class.  Missing or
 * incompatible production API is deliberately a test failure, never a local
 * fallback or a fabricated pass.</p>
 */
public class V35Fc5MidHorizonDiagnosticsV31ContractTest {
  private static final long REQUESTED_MAX_FE = 50000L;
  private static final long Q_PHASE_FE = 5000L;
  private static final long LEGAL_A4_TAIL_FE = 48269L;
  private static final long LEGAL_A4_REMAINING_FE = 1731L;
  private static final String TERMINATION_KIND =
      "PHASE_CONSISTENT_BUDGET_TERMINATION";
  private static final String PRODUCTION_CONTRACT_CLASS =
      "org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35TerminalCheckpointContract";
  private static final Class<?>[] CONTRACT_SIGNATURE = new Class<?>[] {
      long.class, long.class, long.class, long.class, boolean.class,
      String.class, String.class, boolean.class, boolean.class, boolean.class,
      long.class
  };

  private static final String WORKING_FRONT = "workingPopulationND";
  private static final String DECISION_FRONT = "decisionArchiveFront";
  private static final String OBSERVED_FRONT = "observedFullFront";

  /** Synthetic terminal state; it is not an algorithm result or evidence row. */
  private static final class TerminalState {
    private final long requestedMaxFE;
    private final long actualFE;
    private final long lastCompletedAtomicBoundaryFE;
    private final long qPhaseFE;
    private final boolean allowTerminalPartialFormalQPhase;
    private final String terminationKind;
    private final String checkpointBoundary;
    /* Each flag means: that named front was captured non-empty and finite. */
    private final boolean workingPopulationNDComplete;
    private final boolean decisionArchiveFrontComplete;
    private final boolean observedFullFrontComplete;
    private final long observerErrors;

    private TerminalState(long requestedMaxFE, long actualFE,
        long lastCompletedAtomicBoundaryFE, long qPhaseFE,
        boolean allowTerminalPartialFormalQPhase, String terminationKind,
        String checkpointBoundary, boolean workingPopulationNDComplete,
        boolean decisionArchiveFrontComplete, boolean observedFullFrontComplete,
        long observerErrors) {
      this.requestedMaxFE = requestedMaxFE;
      this.actualFE = actualFE;
      this.lastCompletedAtomicBoundaryFE = lastCompletedAtomicBoundaryFE;
      this.qPhaseFE = qPhaseFE;
      this.allowTerminalPartialFormalQPhase = allowTerminalPartialFormalQPhase;
      this.terminationKind = terminationKind;
      this.checkpointBoundary = checkpointBoundary;
      this.workingPopulationNDComplete = workingPopulationNDComplete;
      this.decisionArchiveFrontComplete = decisionArchiveFrontComplete;
      this.observedFullFrontComplete = observedFullFrontComplete;
      this.observerErrors = observerErrors;
    }

    private static TerminalState valid(long actualFE) {
      return new TerminalState(REQUESTED_MAX_FE, actualFE, actualFE, Q_PHASE_FE,
          false, TERMINATION_KIND, V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
          true, true, true, 0L);
    }
  }

  @Test
  public void terminalClassifierAcceptsExactBudgetAndKnownA4TailOnlyAsCompleteBoundaries() {
    TerminalState exact = TerminalState.valid(REQUESTED_MAX_FE);
    assertEquals(0L, exact.requestedMaxFE - exact.actualFE);
    assertEquals("ACCEPTED", productionClassification(exact));

    TerminalState knownTail = TerminalState.valid(LEGAL_A4_TAIL_FE);
    assertEquals(LEGAL_A4_REMAINING_FE,
        knownTail.requestedMaxFE - knownTail.actualFE);
    assertTrue(LEGAL_A4_REMAINING_FE < Q_PHASE_FE);
    assertEquals("ACCEPTED", productionClassification(knownTail));
  }

  @Test
  public void terminalClassifierRejectsEveryInvalidBudgetOrObservationState() {
    assertRejected("remaining exactly q phase",
        new TerminalState(REQUESTED_MAX_FE, 45000L, 45000L, Q_PHASE_FE,
            false, TERMINATION_KIND, V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
            true, true, true, 0L));
    assertRejected("remaining above q phase",
        new TerminalState(REQUESTED_MAX_FE, 44999L, 44999L, Q_PHASE_FE,
            false, TERMINATION_KIND, V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
            true, true, true, 0L));
    assertRejected("actual above requested max",
        new TerminalState(REQUESTED_MAX_FE, 50001L, 50001L, Q_PHASE_FE,
            false, TERMINATION_KIND, V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
            true, true, true, 0L));
    assertRejected("actual differs from last atomic boundary",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, 48268L, Q_PHASE_FE,
            false, TERMINATION_KIND, V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
            true, true, true, 0L));
    assertRejected("partial formal Q phase enabled",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, true, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, true, true, true, 0L));
    assertRejected("wrong termination kind",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, "MAX_FES_REACHED",
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, true, true, true, 0L));
    assertRejected("non-terminal callback boundary",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.BOUNDARY_Q_ROUND, true, true, true, 0L));
    assertRejected("working front missing",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, false, true, true, 0L));
    assertRejected("decision front empty",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, true, false, true, 0L));
    assertRejected("observed front non-finite or indistinguishable",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, true, true, false, 0L));
    assertRejected("observer exception",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, false, false, false, 1L));
  }

  @Test
  public void terminalClassifierIsPureAndDoesNotConsumeJMetalRandom() {
    TerminalState state = TerminalState.valid(LEGAL_A4_TAIL_FE);
    JMetalRandom.getInstance().setSeed(20260901L);
    double expectedNext = JMetalRandom.getInstance().nextDouble();
    JMetalRandom.getInstance().setSeed(20260901L);
    assertEquals("ACCEPTED", productionClassification(state));
    double observedNext = JMetalRandom.getInstance().nextDouble();
    assertEquals(expectedNext, observedNext, 0.0);
    assertEquals("ACCEPTED", productionClassification(state));
  }

  @Test
  public void currentDriverExposesOnlyTheSafe50000CheckpointSchedule() {
    assertEquals(50000, V35MidHorizonDiagnosticDriver.MAX_BOUNDED_DIAGNOSTIC_FE);
    assertArrayEquals(new long[]{10000L, 20000L, 30000L, 40000L, 50000L},
        V35MidHorizonDiagnosticDriver.defaultCheckpoints(50000));
    assertFalse(V35MidHorizonDiagnosticDriver.ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE);
    assertFalse(V35MidHorizonTelemetry.ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE);
  }

  @Test
  public void observerCapturesExact50000AndKnownA4TailAtTheSameAtomicBoundaryOnce() {
    assertValidTerminalCapture(REQUESTED_MAX_FE);
    assertValidTerminalCapture(LEGAL_A4_TAIL_FE);
  }

  @Test
  public void observerRequiresAllThreeFrontsAndDoesNotPublishPartialValidRows() {
    assertUnavailableFront("missing working front", 0, true, "FRONT_NOT_EXPOSED");
    assertUnavailableFront("empty working front", 0, false, "FRONT_EMPTY");
    assertUnavailableFront("missing decision front", 1, true, "FRONT_NOT_EXPOSED");
    assertUnavailableFront("empty decision front", 1, false, "FRONT_EMPTY");
    assertUnavailableFront("missing observed front", 2, true, "FRONT_NOT_EXPOSED");
    assertUnavailableFront("empty observed front", 2, false, "FRONT_EMPTY");
  }

  @Test
  public void observerRejectsNonFiniteFrontWithoutAdvertisingAValidSnapshot() {
    V35CheckpointFrontObserver observer = newObserver(LEGAL_A4_TAIL_FE);
    observer.onAtomicPhaseEnd(LEGAL_A4_TAIL_FE, 1, 1, 1,
        singletonFront(Double.NaN, 20.0, 30.0),
        singletonFront(11.0, 19.0, 31.0),
        singletonFront(12.0, 18.0, 32.0),
        V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
    String csv = observer.toCsv();
    assertEquals("non-finite input must have no valid rows", 0, validSnapshotRows(csv));
    assertTrue("invalid input must remain explicitly unavailable",
        csv.contains("NOT_APPLICABLE") || csv.contains("CHECKPOINT_UNOBSERVABLE"));
    assertRejected("non-finite front",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, false, true, true, 0L));
  }

  @Test
  public void observerExceptionIsCountedAndCannotBecomeTerminalEvidence() {
    V35CheckpointFrontObserver checkpoint = newObserver(LEGAL_A4_TAIL_FE);
    V35MidHorizonTelemetry telemetry = new V35MidHorizonTelemetry(
        checkpoint, null, null, null, "v31-exception", "jar", "cfg", "inst",
        20260901L, "A4", true);

    telemetry.onAtomicPhaseEnd(LEGAL_A4_TAIL_FE, 1, 1, 1,
        Collections.singletonList(throwingObjectiveSolution()),
        singletonFront(11.0, 19.0, 31.0),
        singletonFront(12.0, 18.0, 32.0),
        V35CheckpointFrontObserver.ATOMIC_BOUNDARY);

    assertTrue("observer exception must be counted", telemetry.getObserverErrors() > 0L);
    assertTrue("execution exception must be counted",
        telemetry.getObserverExecutionErrors() > 0L);
    assertEquals("exception must not complete a checkpoint", 1,
        checkpoint.getPendingCheckpointCount());
    assertEquals("exception must not publish a valid row", 0,
        validSnapshotRows(telemetry.getCheckpointFrontCsv()));

    telemetry.onRunEnd(LEGAL_A4_TAIL_FE, 1, 1, 1);
    assertTrue(telemetry.isRunFinalized());
    assertTrue(telemetry.getCheckpointFrontCsv().contains("PHASE_BOUNDARY_NOT_OBSERVED"));
    assertRejected("observer exception terminal state",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, false, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, false, false, false, 1L));
  }

  @Test
  public void disabledTelemetryShortCircuitsWithoutTouchingFrontInputsOrClaimingEvidence() {
    V35MidHorizonTelemetry telemetry = new V35MidHorizonTelemetry(
        null, null, null, null, "v31-off", "jar", "cfg", "inst",
        20260901L, "A4", false);
    List<PermutationSolution<Integer>> exploding = new ExplodingList<PermutationSolution<Integer>>();

    telemetry.onAtomicPhaseEnd(LEGAL_A4_TAIL_FE, 1, 1, 1,
        exploding, exploding, exploding, V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
    telemetry.onRunEnd(LEGAL_A4_TAIL_FE, 1, 1, 1);

    assertFalse(telemetry.isEnabled());
    assertEquals(0L, telemetry.getObserverErrors());
    assertEquals("OFF must not construct or claim telemetry", "", telemetry.csvBundle());
  }

  @Test
  public void telemetryContractPropertiesKeepPartialFormalPhaseDisabled() {
    V35MidHorizonTelemetry telemetry = new V35MidHorizonTelemetry(
        null, null, null, null, "v31-properties", "jar", "cfg", "inst",
        20260901L, "A4", true);
    assertFalse(V35MidHorizonTelemetry.ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE);
    assertTrue(telemetry.contractProperties().contains(
        "allowTerminalPartialFormalQPhase=false"));
    assertRejected("partial flag cannot be enabled",
        new TerminalState(REQUESTED_MAX_FE, LEGAL_A4_TAIL_FE, LEGAL_A4_TAIL_FE,
            Q_PHASE_FE, true, TERMINATION_KIND,
            V35CheckpointFrontObserver.ATOMIC_BOUNDARY, true, true, true, 0L));
  }

  private static void assertValidTerminalCapture(long actualFE) {
    V35CheckpointFrontObserver observer = newObserver(actualFE);
    List<PermutationSolution<Integer>> working = singletonFront(10.0, 20.0, 30.0);
    List<PermutationSolution<Integer>> decision = singletonFront(11.0, 19.0, 31.0);
    List<PermutationSolution<Integer>> observed = singletonFront(12.0, 18.0, 32.0);

    observer.onAtomicPhaseEnd(actualFE, 1, 1, 1, working, decision, observed,
        V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
    String first = observer.toCsv();
    assertEquals("exactly three front rows", 3, validSnapshotRows(first));
    assertEquals(1L, observer.getObservedCheckpointCount());
    assertEquals(0, observer.getPendingCheckpointCount());

    // A completed nominal checkpoint is terminally captured once.  A later
    // callback cannot replace the real snapshot with a different state.
    observer.onAtomicPhaseEnd(actualFE, 99, 99, 99,
        singletonFront(1.0, 2.0, 3.0), singletonFront(2.0, 1.0, 4.0),
        singletonFront(3.0, 1.0, 2.0), V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
    assertEquals(first, observer.toCsv());

    Set<String> frontTypes = new HashSet<String>();
    for (String[] row : dataRows(first)) {
      assertEquals(String.valueOf(actualFE), row[7]);
      assertEquals(String.valueOf(actualFE), row[8]);
      assertEquals("0", row[9]);
      assertEquals("NONE", row[14]);
      frontTypes.add(row[13]);
      assertFinite("Cmax", row[16]);
      assertFinite("TEC", row[17]);
      assertFinite("TWC", row[18]);
      assertEquals(V35CheckpointFrontObserver.ATOMIC_BOUNDARY, row[19]);
    }
    assertEquals(new HashSet<String>(Arrays.asList(
        WORKING_FRONT, DECISION_FRONT, OBSERVED_FRONT)), frontTypes);
    assertEquals(0L, observer.getUnobservableCheckpointCount());
    observer.onRunEnd(actualFE, 1, 1, 1);
    assertEquals(0L, observer.getUnobservableCheckpointCount());
    assertEquals(actualFE, observer.getLastNominalCheckpointFE());
    assertEquals(actualFE, observer.getLastActualCheckpointFE());
    assertEquals(actualFE, observer.getLastActualSnapshotFE());
    assertEquals(0L, observer.getLastCheckpointDeltaFE());
    assertEquals(V35CheckpointFrontObserver.CHECKPOINT_KIND_ATOMIC_BOUNDARY,
        observer.getLastCheckpointKind());
    assertEquals(V35CheckpointFrontObserver.ATOMIC_BOUNDARY,
        observer.getLastCheckpointAtomicBoundary());
    assertEquals(V35CheckpointFrontObserver.TERMINATION_KIND_PHASE_CONSISTENT_BUDGET,
        observer.getLastTerminationKind());
    assertTrue(observer.isTerminalCheckpointAccepted());
  }

  private static void assertUnavailableFront(String label, int missingIndex,
      boolean missing, String expectedReason) {
    V35CheckpointFrontObserver observer = newObserver(LEGAL_A4_TAIL_FE);
    List<PermutationSolution<Integer>> working = singletonFront(10.0, 20.0, 30.0);
    List<PermutationSolution<Integer>> decision = singletonFront(11.0, 19.0, 31.0);
    List<PermutationSolution<Integer>> observed = singletonFront(12.0, 18.0, 32.0);
    List<PermutationSolution<Integer>> unavailable = missing
        ? null : new ArrayList<PermutationSolution<Integer>>();
    if (missingIndex == 0) working = unavailable;
    if (missingIndex == 1) decision = unavailable;
    if (missingIndex == 2) observed = unavailable;

    observer.onAtomicPhaseEnd(LEGAL_A4_TAIL_FE, 1, 1, 1,
        working, decision, observed, V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
    String csv = observer.toCsv();
    assertEquals(label + " must not publish a partial valid snapshot", 0,
        validSnapshotRows(csv));
    assertTrue(label + " must expose its unavailable reason",
        csv.contains(expectedReason));
    assertTrue(label + " must retain explicit unavailable values",
        csv.contains("NOT_APPLICABLE") || csv.contains("CHECKPOINT_UNOBSERVABLE"));
  }

  private static V35CheckpointFrontObserver newObserver(long nominalCheckpoint) {
    return new V35CheckpointFrontObserver(
        new long[]{nominalCheckpoint}, "v31-test", "jar", "cfg", "inst",
        20260901L, "A4", true);
  }

  private static List<PermutationSolution<Integer>> singletonFront(
      double cmax, double tec, double twc) {
    return Collections.singletonList(solution(cmax, tec, twc));
  }

  private static PermutationSolution<Integer> solution(
      double cmax, double tec, double twc) {
    DhhfspFourVectorSolution value = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 7);
    value.setObjective(0, cmax);
    value.setObjective(1, tec);
    value.setObjective(6, twc);
    return value;
  }

  private static int validSnapshotRows(String csv) {
    int count = 0;
    for (String[] row : dataRows(csv)) {
      if (row.length > 14 && "NONE".equals(row[14])) count++;
    }
    return count;
  }

  private static void assertFinite(String label, String value) {
    double parsed = Double.parseDouble(value);
    assertFalse(label + " must be finite", Double.isNaN(parsed)
        || Double.isInfinite(parsed));
  }

  private static List<String[]> dataRows(String csv) {
    List<String[]> rows = new ArrayList<String[]>();
    if (csv == null || csv.length() == 0) return rows;
    for (String line : csv.split("\\r?\\n")) {
      if (line.length() == 0 || line.startsWith("generatedByRunId,")) continue;
      rows.add(line.split(",", -1));
    }
    return rows;
  }

  private static void assertRejected(String label, TerminalState state) {
    assertEquals(label, "REJECTED", productionClassification(state));
  }

  /**
   * Calls only the agreed production classifier.  There is intentionally no
   * test-local classification fallback: an absent B API is a fail-closed red
   * test, not an accepted protocol result.
   */
  private static String productionClassification(TerminalState state) {
    try {
      Class<?> contract = Class.forName(PRODUCTION_CONTRACT_CLASS);
      Method classify = contract.getDeclaredMethod("classify", CONTRACT_SIGNATURE);
      if (!Modifier.isPublic(contract.getModifiers())
          || !Modifier.isPublic(classify.getModifiers())) {
        classify.setAccessible(true);
      }
      Object result = classify.invoke(null,
          Long.valueOf(state.requestedMaxFE), Long.valueOf(state.actualFE),
          Long.valueOf(state.lastCompletedAtomicBoundaryFE), Long.valueOf(state.qPhaseFE),
          Boolean.valueOf(state.allowTerminalPartialFormalQPhase), state.terminationKind,
          state.checkpointBoundary, Boolean.valueOf(state.workingPopulationNDComplete),
          Boolean.valueOf(state.decisionArchiveFrontComplete),
          Boolean.valueOf(state.observedFullFrontComplete),
          Long.valueOf(state.observerErrors));
      return String.valueOf(result);
    } catch (ClassNotFoundException error) {
      throw new AssertionError("B production API missing: " + PRODUCTION_CONTRACT_CLASS
          + ".class");
    } catch (NoSuchMethodException error) {
      throw new AssertionError("B production API must expose public/package-private "
          + "static classify(long,long,long,long,boolean,String,String,boolean,boolean,boolean,long)");
    } catch (IllegalAccessException error) {
      throw new AssertionError("B production classifier is not callable", error);
    } catch (InvocationTargetException error) {
      Throwable cause = error.getCause();
      AssertionError failure = new AssertionError(
          "B production classifier must fail closed by returning ACCEPTED/REJECTED: "
              + (cause == null ? error.toString() : cause.toString()));
      if (cause != null) failure.initCause(cause);
      throw failure;
    }
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> throwingObjectiveSolution() {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
        if ("getObjective".equals(method.getName())) {
          throw new IllegalStateException("synthetic observer failure");
        }
        if ("copy".equals(method.getName())) return proxy;
        if (method.getReturnType() == int.class) return Integer.valueOf(0);
        if (method.getReturnType() == double.class) return Double.valueOf(0.0);
        if (method.getReturnType() == boolean.class) return Boolean.FALSE;
        if (List.class.isAssignableFrom(method.getReturnType())) {
          return new ArrayList<Object>();
        }
        if (Map.class.isAssignableFrom(method.getReturnType())) {
          return new HashMap<Object, Object>();
        }
        return null;
      }
    };
    return (PermutationSolution<Integer>) Proxy.newProxyInstance(
        PermutationSolution.class.getClassLoader(),
        new Class<?>[]{PermutationSolution.class}, handler);
  }

  /** A list that proves OFF mode returns before touching front arguments. */
  private static final class ExplodingList<E> extends AbstractList<E> {
    @Override
    public E get(int index) {
      throw new AssertionError("OFF telemetry touched a front input");
    }

    @Override
    public int size() {
      throw new AssertionError("OFF telemetry touched a front input");
    }
  }
}
