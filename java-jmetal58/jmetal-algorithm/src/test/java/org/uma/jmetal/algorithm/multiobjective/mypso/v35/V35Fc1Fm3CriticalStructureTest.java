package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/**
 * V35-FC-1: N3/N4 candidate selection is FM3-consistent.  The critical
 * structure comes from the parent's actual fatigue-adjusted trace (the real
 * zero-slack DAG / actual bottleneck values), never from a PT0 proxy, when a
 * trace is available; a null or empty trace keeps the legacy proxy behaviour
 * byte-for-byte.  The gateway only reads the trace.
 */
public class V35Fc1Fm3CriticalStructureTest {

  private static ZhangBoFatigueInstanceData instance() {
    String sha = repeat('A', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{9, 4}, {8, 5}, {7, 6}, {6, 7}}, repeat('B', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 2, 1,
        new int[][] {{2, 2}}, new double[][][] {{{1.0, 1.5}, {1.0, 1.3}}},
        new int[][][] {{{8, 11}, {7, 10}}},
        new int[][] {{20, 12}, {18, 14}, {16, 16}, {14, 18}},
        new int[] {4}, new double[][] {{1.0, 1.3, 1.0, 1.2}},
        new int[][] {{10, 13, 10, 12}}, extension);
  }

  private static DhhfspFourVectorSolution source() {
    return new DhhfspFourVectorSolution(
        Arrays.asList(1, 0, 2, 3), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 1, 1), Arrays.asList(0, 0, 1, 1),
        "fatigue_improved", 7);
  }

  /** Serial single-machine chain per stage: every operation is zero-slack
   *  critical.  Job 2 has by far the largest first-stage actual duration
   *  (fatigue-inflated); job 1 carries the highest post-operation fatigue. */
  private static ZhangBoFatigueEvaluationResult fm3Trace() {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>();
    double time = 0.0;
    time = addStageZero(operations, 0, 1, time, 5.0, 0.9);
    time = addStageZero(operations, 1, 0, time, 5.0, 0.2);
    time = addStageZero(operations, 2, 2, time, 100.0, 0.3);
    addStageZero(operations, 3, 3, time, 5.0, 0.1);
    return new ZhangBoFatigueEvaluationResult(
        repeat('A', 64), repeat('C', 64), repeat('B', 64),
        operations, null, new double[] {time, 0.0, 0.0}, new double[0][0][0], new double[0][0][0], new double[0][0][0]);
  }

  private static double addStageZero(List<ZhangBoFatigueOperationRecord> operations,
      int sequence, int job, double time, double actual, double fatigueAfter) {
    ZhangBoFatigueOperationRecord record = operation(
        sequence, job, 0, time, actual, actual * 0.1, fatigueAfter * 0.5, fatigueAfter);
    operations.add(record);
    return record.end;
  }

  private static ZhangBoFatigueOperationRecord operation(
      int sequence, int job, int stage, double start,
      double processing, double setup, double fatigueAtStart, double fatigueAfter) {
    return new ZhangBoFatigueOperationRecord(
        sequence, job, stage, 0, 0, 0,
        0.0, 0.0, 0.0, start, 0.0, 0.0, fatigueAtStart,
        processing, setup, processing + setup, 1.0,
        processing, setup, processing + setup, start + processing + setup,
        fatigueAfter, 0.0, 0.0, false);
  }

  private static Map<Integer, String> packageByJob(DhhfspFourVectorSolution s) {
    Map<Integer, String> out = new HashMap<>();
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      out.put(s.getJobSequence().get(p),
          s.getFactoryAssignments().get(p) + "/" + s.getMachineAssignments().get(p)
              + "/" + s.getWorkerAssignments().get(p));
    }
    return out;
  }

  @Test public void nullTraceKeepsLegacyProxyBehaviourExactly() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway gateway = new V35MacroCandidateGateway();
    for (V35MacroNeighborhood action : V35MacroNeighborhood.values()) {
      V35MacroCandidateGateway.Prepared legacy = gateway.prepare(
          action, parent, instance(), 0, V35Bottleneck.SEQ);
      V35MacroCandidateGateway.Prepared viaNull = gateway.prepareWithEvaluation(
          action, parent, instance(), 0, V35Bottleneck.SEQ, null);
      assertEquals("applicable flag must match for " + action,
          legacy.isApplicable(), viaNull.isApplicable());
      assertEquals("route must match for " + action,
          legacy.getRoute(), viaNull.getRoute());
      assertEquals("null trace must report the PT0 proxy for " + action,
          V35MacroCandidateGateway.Prepared.SOURCE_PT0_PROXY, viaNull.getStructureSource());
      if (legacy.isApplicable()) {
        DhhfspFourVectorSolution a = (DhhfspFourVectorSolution) legacy.getCandidate();
        DhhfspFourVectorSolution b = (DhhfspFourVectorSolution) viaNull.getCandidate();
        assertEquals(a.getJobSequence(), b.getJobSequence());
        assertEquals(a.getFactoryAssignments(), b.getFactoryAssignments());
        assertEquals(a.getMachineAssignments(), b.getMachineAssignments());
        assertEquals(a.getWorkerAssignments(), b.getWorkerAssignments());
      }
    }
  }

  @Test public void n3PicksTheFm3CriticalJobNotThePt0ProxyJob() {
    DhhfspFourVectorSolution parent = source();
    ZhangBoFatigueEvaluationResult trace = fm3Trace();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepareWithEvaluation(V35MacroNeighborhood.N3, parent, instance(), 0,
            V35Bottleneck.SEQ, trace);
    assertTrue(prepared.isApplicable());
    assertEquals(V35MacroCandidateGateway.Prepared.SOURCE_FM3_ACTUAL,
        prepared.getStructureSource());
    assertEquals("CRITICAL_SOURCE", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    // The fatigue-inflated job 2 (actual duration 100 vs 5) is the critical
    // pick; it must move strictly earlier in the job sequence.
    assertTrue("critical job must move earlier",
        candidate.getJobSequence().indexOf(2) < parent.getJobSequence().indexOf(2));
    // JS-only action by job identity: packages travel with their jobs.
    assertEquals(packageByJob(parent), packageByJob(candidate));
    // Read-only contract: the candidate is a fresh four-vector copy without
    // any evaluation attribute attached - the trace is never edited.
    assertNull(prepared.getCandidate().getAttribute(ZhangBoFatigueEvaluationResult.class));
  }

  @Test public void n4Fm3FatigueRoutePicksTheHighestFatiguePosition() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepareWithEvaluation(V35MacroNeighborhood.N4, parent, instance(), 0,
            V35Bottleneck.FAT, fm3Trace());
    assertTrue(prepared.isApplicable());
    assertEquals(V35MacroCandidateGateway.Prepared.SOURCE_FM3_ACTUAL,
        prepared.getStructureSource());
    assertEquals("FAT_RESOURCE_ROUTE", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    // Job 1 (position 1) carries the highest fatigueAfter in the trace, so
    // exactly that package gets its worker reassigned.
    for (int p = 0; p < parent.getJobSequence().size(); p++) {
      boolean changed = !parent.getWorkerAssignments().get(p)
          .equals(candidate.getWorkerAssignments().get(p));
      assertEquals("worker change must hit the FM3 fatigue bottleneck only (job "
          + parent.getJobSequence().get(p) + ")", parent.getJobSequence().get(p)
              .intValue() == 1, changed);
    }
    assertEquals(parent.getJobSequence(), candidate.getJobSequence());
  }

  @Test public void n3EmptyFactoryTraceFallsBackToProxy() {
    // Trace restricted to a different factory: the FM3 route is unusable and
    // the legacy proxy must take over with its exact behaviour.
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway gateway = new V35MacroCandidateGateway();
    V35MacroCandidateGateway.Prepared legacy = gateway.prepare(
        V35MacroNeighborhood.N3, parent, instance(), 0, V35Bottleneck.SEQ);
    V35MacroCandidateGateway.Prepared fallback = gateway.prepareWithEvaluation(
        V35MacroNeighborhood.N3, parent, instance(), 0, V35Bottleneck.SEQ,
        emptyOtherFactoryTrace());
    assertEquals(legacy.isApplicable(), fallback.isApplicable());
    assertEquals(V35MacroCandidateGateway.Prepared.SOURCE_PT0_PROXY,
        fallback.getStructureSource());
    if (legacy.isApplicable()) {
      assertEquals(
          ((DhhfspFourVectorSolution) legacy.getCandidate()).getJobSequence(),
          ((DhhfspFourVectorSolution) fallback.getCandidate()).getJobSequence());
    }
  }

  @Test public void nonStructuralMacrosIgnoreTheTrace() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared n1 = new V35MacroCandidateGateway()
        .prepareWithEvaluation(V35MacroNeighborhood.N1, parent, instance(), 0,
            V35Bottleneck.SEQ, fm3Trace());
    assertTrue(n1.isApplicable());
    assertEquals(V35MacroCandidateGateway.Prepared.SOURCE_PT0_PROXY, n1.getStructureSource());
    V35MacroCandidateGateway.Prepared n5 = new V35MacroCandidateGateway()
        .prepareWithEvaluation(V35MacroNeighborhood.N5, parent, instance(), 0,
            V35Bottleneck.WOR, fm3Trace());
    assertTrue(n5.isApplicable());
    assertEquals(V35MacroCandidateGateway.Prepared.SOURCE_PT0_PROXY, n5.getStructureSource());
    assertNotNull(n5.getCandidate());
    assertNotEquals(parent.getJobSequence(),
        ((DhhfspFourVectorSolution) n5.getCandidate()).getJobSequence());
  }

  private static ZhangBoFatigueEvaluationResult emptyOtherFactoryTrace() {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>();
    ZhangBoFatigueOperationRecord other = operation(0, 1, 0, 0.0, 5.0, 0.5, 0.0, 0.1);
    operations.add(new ZhangBoFatigueOperationRecord(
        other.sequence, other.job, other.stage, 5, other.machine, other.worker,
        other.predecessorCompletion, other.machineAvailableBefore,
        other.workerAvailableBefore, other.start, other.recoveryDuration,
        other.fatigueBeforeRecovery, other.fatigueAtStart, other.baseProcessingDuration,
        other.baseSetupDuration, other.baseDuration, other.durationMultiplier,
        other.actualProcessingDuration, other.actualSetupDuration, other.actualDuration,
        other.end, other.fatigueAfter, other.energy, other.cost,
        other.safeThresholdExceeded));
    return new ZhangBoFatigueEvaluationResult(
        repeat('A', 64), repeat('C', 64), repeat('B', 64),
        operations, null, new double[] {0.0, 0.0, 0.0}, new double[0][0][0], new double[0][0][0], new double[0][0][0]);
  }

  private static String repeat(char value, int count) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < count; i++) out.append(value);
    return out.toString();
  }
}
