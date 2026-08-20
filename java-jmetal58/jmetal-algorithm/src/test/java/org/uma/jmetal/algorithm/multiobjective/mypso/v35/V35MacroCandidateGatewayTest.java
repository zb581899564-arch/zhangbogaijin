package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/** Regression tests that keep v3.5 N3/N4/N5 independent from historical O10-O13/Shift code. */
public class V35MacroCandidateGatewayTest {
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

  @Test public void n3MovesAWholeJobPackageAndExposesItsRoute() {
    DhhfspFourVectorSolution parent = source();
    Map<Integer, String> packageByJob = packageByJob(parent);
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N3, parent, instance(), 0, V35Bottleneck.SEQ);
    assertTrue(prepared.isApplicable());
    assertEquals("CRITICAL_SOURCE", prepared.getRoute());
    assertNotEquals(parent.getJobSequence(),
        ((DhhfspFourVectorSolution) prepared.getCandidate()).getJobSequence());
    assertEquals(packageByJob, packageByJob((DhhfspFourVectorSolution) prepared.getCandidate()));
  }

  @Test public void n4RoutesWorkerAndMachineActionsByBottleneck() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared worker = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N4, parent, instance(), 0, V35Bottleneck.WOR);
    assertTrue(worker.isApplicable());
    assertEquals("WOR_RESOURCE_ROUTE", worker.getRoute());
    assertNotEquals(parent.getWorkerAssignments(),
        ((DhhfspFourVectorSolution) worker.getCandidate()).getWorkerAssignments());

    V35MacroCandidateGateway.Prepared machine = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N4, parent, instance(), 0, V35Bottleneck.MAC);
    assertTrue(machine.isApplicable());
    assertEquals("MAC_RESOURCE_ROUTE", machine.getRoute());
    assertNotEquals(parent.getMachineAssignments(),
        ((DhhfspFourVectorSolution) machine.getCandidate()).getMachineAssignments());
  }

  @Test public void n5IsARealTwoPartActionAndNeverTouchesReleaseOrShiftState() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N5, parent, instance(), 0, V35Bottleneck.WOR);
    assertTrue(prepared.isApplicable());
    assertEquals("STRUCTURAL_RECOVERY_MIX", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    assertNotEquals(parent.getJobSequence(), candidate.getJobSequence());
    assertNotEquals(parent.getWorkerAssignments(), candidate.getWorkerAssignments());
    assertEquals(0L, prepared.getCandidate().getAttribute("releaseOverride") == null ? 0L : 1L);
  }

  @Test public void n1SwapsFirstSameFactoryAdjacentPackage() {
    DhhfspFourVectorSolution parent = source();
    Map<Integer, String> packageByJob = packageByJob(parent);
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N1, parent, instance(), 0, V35Bottleneck.SEQ);
    assertTrue(prepared.isApplicable());
    assertEquals("N1", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    assertNotEquals(parent.getJobSequence(), candidate.getJobSequence());
    assertEquals(packageByJob, packageByJob(candidate));
  }

  @Test public void n2RelocatesFirstCrossFactoryJobAndRebaselinesResources() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N2, parent, twoFactoryInstance(), 1, V35Bottleneck.MAC);
    assertTrue(prepared.isApplicable());
    assertEquals("N2", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    assertNotEquals(parent.getFactoryAssignments(), candidate.getFactoryAssignments());
    // Necessary re-baseline: machine reset to 0 and worker to the destination's
    // first eligible worker on the relocated position.
    int relocated = -1;
    for (int p = 0; p < candidate.getNumberOfVariablesid(); p++) {
      if (!candidate.getFactoryAssignments().get(p).equals(parent.getFactoryAssignments().get(p))) {
        relocated = p;
        break;
      }
    }
    assertTrue(relocated >= 0);
    assertEquals(Integer.valueOf(0), candidate.getMachineAssignments().get(relocated));
    assertEquals(Integer.valueOf(twoFactoryInstance().getEligibleWorkers(1, 0)[0]),
        candidate.getWorkerAssignments().get(relocated));
  }

  @Test public void n3SetBottleneckRoutesSetupEdgeSource() {
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N3, parent, instance(), 0, V35Bottleneck.SET);
    assertTrue(prepared.isApplicable());
    assertEquals("SETUP_EDGE_SOURCE", prepared.getRoute());
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) prepared.getCandidate();
    assertNotEquals(parent.getJobSequence(), candidate.getJobSequence());
    assertEquals(packageByJob(parent), packageByJob(candidate));
  }

  @Test public void n4SetFatBalRoutesRemainLegallyAssigned() {
    DhhfspFourVectorSolution parent = source();
    for (V35Bottleneck bottleneck : new V35Bottleneck[]{
        V35Bottleneck.SET, V35Bottleneck.FAT, V35Bottleneck.BAL}) {
      V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
          .prepare(V35MacroNeighborhood.N4, parent, instance(), 0, bottleneck);
      assertTrue(prepared.isApplicable());
      assertEquals(bottleneck.name() + "_RESOURCE_ROUTE", prepared.getRoute());
      assertLegallyAssigned((DhhfspFourVectorSolution) prepared.getCandidate(), instance());
    }
  }

  @Test public void n5FallsBackWithoutResourceLegAndNeverReturnsJsOnly() {
    // Single machine + single worker: the resource leg is structurally infeasible,
    // so the two-part contract discards the whole candidate instead of leaking
    // the JS-only structural move.
    DhhfspFourVectorSolution parent = source();
    V35MacroCandidateGateway.Prepared prepared = new V35MacroCandidateGateway()
        .prepare(V35MacroNeighborhood.N5, parent, degenerateResourceInstance(), 0,
            V35Bottleneck.WOR);
    assertEquals(false, prepared.isApplicable());
    assertEquals("NO_APPLICABLE_ACTION", prepared.getReason());
    assertEquals(null, prepared.getCandidate());
  }

  private static void assertLegallyAssigned(DhhfspFourVectorSolution candidate,
      ZhangBoFatigueInstanceData data) {
    for (int p = 0; p < candidate.getNumberOfVariables(); p++) {
      int factory = candidate.getFactoryAssignments().get(p);
      int machine = candidate.getMachineAssignments().get(p);
      int worker = candidate.getWorkerAssignments().get(p);
      assertTrue(machine >= 0 && machine < data.getMachineCount(factory, 0));
      boolean eligible = false;
      for (int candidateWorker : data.getEligibleWorkers(factory, 0)) {
        if (candidateWorker == worker) { eligible = true; break; }
      }
      assertTrue(eligible);
    }
  }

  private static ZhangBoFatigueInstanceData twoFactoryInstance() {
    String sha = repeat('A', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{9, 4}, {8, 5}, {7, 6}, {6, 7}}, repeat('B', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 2, 2,
        new int[][] {{2, 2}, {2, 2}},
        new double[][][] {{{1.0, 1.5}, {1.0, 1.3}}, {{1.0, 1.5}, {1.0, 1.3}}},
        new int[][][] {{{8, 11}, {7, 10}}, {{8, 11}, {7, 10}}},
        new int[][] {{20, 12}, {18, 14}, {16, 16}, {14, 18}},
        new int[] {4, 4},
        new double[][] {{1.0, 1.3, 1.0, 1.2}, {1.0, 1.3, 1.0, 1.2}},
        new int[][] {{10, 13, 10, 12}, {10, 13, 10, 12}},
        extension);
  }

  private static ZhangBoFatigueInstanceData degenerateResourceInstance() {
    String sha = repeat('C', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 1, new int[][] {{9}, {8}, {7}, {6}}, repeat('D', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 1, 1,
        new int[][] {{1}},
        new double[][][] {{{1.0}}},
        new int[][][] {{{8}}},
        new int[][] {{20}, {18}, {16}, {14}},
        new int[] {1},
        new double[][] {{1.0}},
        new int[][] {{10}},
        extension);
  }

  private static Map<Integer, String> packageByJob(DhhfspFourVectorSolution value) {
    Map<Integer, String> result = new HashMap<>();
    for (int p = 0; p < value.getNumberOfVariables(); p++) {
      result.put(value.getJobSequence().get(p), value.getFactoryAssignments().get(p) + ":"
          + value.getMachineAssignments().get(p) + ":" + value.getWorkerAssignments().get(p));
    }
    return result;
  }

  private static String repeat(char value, int count) {
    char[] result = new char[count];
    Arrays.fill(result, value);
    return new String(result);
  }
}
