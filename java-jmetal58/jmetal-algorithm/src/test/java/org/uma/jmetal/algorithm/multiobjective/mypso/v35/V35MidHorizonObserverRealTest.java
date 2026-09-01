package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** V3.1: verifies real terminal-front observation, protocol closure, and OFF short-circuiting. */
public class V35MidHorizonObserverRealTest {

  private static PermutationSolution<Integer> solution(double[] objectives) {
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 7);
    solution.setObjective(0, objectives[0]);
    solution.setObjective(1, objectives[1]);
    solution.setObjective(6, objectives[2]);
    return solution;
  }

  @Test
  public void checkpointObserverCapturesRealFront() {
    V35CheckpointFrontObserver observer = new V35CheckpointFrontObserver(
        new long[]{1000L}, "run-1", "jar", "cfg", "inst", 20260901L, "A4", true);
    List<PermutationSolution<Integer>> population = new ArrayList<>();
    population.add(solution(new double[]{10, 20, 30}));
    population.add(solution(new double[]{5, 25, 30}));   // dominated
    population.add(solution(new double[]{5, 20, 29}));   // nondominated best
    population.add(solution(new double[]{5, 20, 29}));   // exact duplicate
    observer.onAtomicPhaseEnd(1000, 1, 1, 2, population,
        new ArrayList<PermutationSolution<Integer>>(), new ArrayList<PermutationSolution<Integer>>());
    String csv = observer.toCsv();
    assertTrue("workingPopulationND must be captured", csv.contains("workingPopulationND"));
    assertTrue("must contain nominal 1000", csv.contains(",1000,1000,0,1,1,2,"));
    assertTrue("decisionArchiveFront NOT_APPLICABLE", csv.contains("decisionArchiveFront"));
    assertTrue("observedFullFront NOT_APPLICABLE", csv.contains("observedFullFront"));
    // 1 workingPopulationND row (after dedup + strict Pareto) + 2 NOT_APPLICABLE
    assertEquals(3, observer.getRowCount());
    assertEquals(0L, observer.getObserverErrors());
  }

  @Test
  public void checkpointOvershootIsUnobservableButNotAnObserverError() {
    V35CheckpointFrontObserver observer = new V35CheckpointFrontObserver(
        new long[]{1000L}, "run-overshoot", "jar", "cfg", "inst", 7L, "A4", true);
    observer.onAtomicPhaseEnd(6000L, 1, 1, 50,
        new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>());
    String csv = observer.toCsv();
    assertTrue(csv.contains("CHECKPOINT_UNOBSERVABLE"));
    assertTrue(csv.contains("CHECKPOINT_OVERSHOOT"));
    assertEquals(3, observer.getRowCount());
    assertEquals(1L, observer.getUnobservableCheckpointCount());
    assertEquals(0L, observer.getObserverErrors());
    assertEquals(0L, observer.getObserverExecutionErrors());
  }

  @Test
  public void checkpointAllowsOvershootBelowFiveThousandAndFinalizesUnreachedPoint() {
    V35CheckpointFrontObserver observed = new V35CheckpointFrontObserver(
        new long[]{1000L}, "run-valid", "jar", "cfg", "inst", 7L, "A4", true);
    observed.onAtomicPhaseEnd(5999L, 1, 1, 50,
        Arrays.<PermutationSolution<Integer>>asList(solution(new double[]{5, 20, 29})),
        new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>());
    assertTrue(observed.toCsv().contains(",1000,5999,4999,1,1,50,workingPopulationND,NONE,"));
    assertEquals(0L, observed.getUnobservableCheckpointCount());
    assertEquals(0L, observed.getObserverErrors());

    V35CheckpointFrontObserver unreached = new V35CheckpointFrontObserver(
        new long[]{2000L}, "run-unreached", "jar", "cfg", "inst", 7L, "A4", true);
    unreached.onAtomicPhaseEnd(1000L, 1, 1, 50,
        new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>());
    unreached.onRunEnd(1000L, 1, 1, 50);
    assertTrue(unreached.toCsv().contains("CHECKPOINT_NOT_REACHED"));
    assertEquals(1L, unreached.getUnobservableCheckpointCount());
    assertEquals(0, unreached.getPendingCheckpointCount());
    assertTrue(unreached.isComplete());
    assertEquals(0L, unreached.getObserverErrors());
    assertEquals(0L, unreached.getObserverExecutionErrors());
  }

  @Test
  public void phaseConsistentTailPublishesOneRealTerminalSnapshot() {
    V35CheckpointFrontObserver observer = new V35CheckpointFrontObserver(
        new long[]{10000L, 50000L}, "run-tail", "jar", "cfg", "inst",
        20260901L, "A4", true);
    List<PermutationSolution<Integer>> working = Arrays.asList(
        solution(new double[]{10, 20, 30}));
    List<PermutationSolution<Integer>> decision = Arrays.asList(
        solution(new double[]{11, 19, 31}));
    List<PermutationSolution<Integer>> observed = Arrays.asList(
        solution(new double[]{12, 18, 32}));

    observer.onAtomicPhaseEnd(10000L, 1, 1, 1, working, decision, observed,
        V35CheckpointFrontObserver.BOUNDARY_PDDR_POST_SAMPLE);
    observer.onAtomicPhaseEnd(48269L, 2, 2, 2, working, decision, observed,
        V35CheckpointFrontObserver.BOUNDARY_PDDR_POST_SAMPLE);
    observer.onRunEnd(48269L, 2, 2, 2);

    String csv = observer.toCsv();
    assertTrue(csv.contains("50000,48269,-1731"));
    assertTrue(csv.contains("PHASE_CONSISTENT_TERMINAL,48269,-1731"));
    assertTrue(csv.contains("REAL_ATOMIC_RUN_END_SNAPSHOT"));
    assertEquals(0L, observer.getUnobservableCheckpointCount());
    assertEquals(0L, observer.getObserverErrors());
    assertEquals(0L, observer.getObserverExecutionErrors());
    assertEquals(1L, observer.getNominalCheckpointNotExactlyReachedCount());
    assertEquals(1L, observer.getTerminalSnapshotCount());
    assertEquals(48269L, observer.getLastCompletedAtomicBoundaryFE());
    assertEquals("ACCEPTED", observer.getTerminalClassification());
    assertEquals("PHASE_CONSISTENT_TERMINAL", observer.getLastCheckpointKind());
    assertEquals(50000L, observer.getLastNominalCheckpointFE());
    assertEquals(48269L, observer.getLastActualCheckpointFE());
    assertEquals(-1731L, observer.getLastCheckpointDeltaFE());
    assertEquals(V35CheckpointFrontObserver.REAL_ATOMIC_RUN_END_SNAPSHOT,
        observer.getLastCheckpointAtomicBoundary());
  }

  @Test
  public void driverUsesOnlyTheV31FinalCheckpointSchedule() {
    assertArrayEquals(new long[]{10000L, 20000L, 30000L, 40000L, 50000L},
        V35MidHorizonDiagnosticDriver.defaultCheckpoints(50000));
    assertRejectedBudget(2000);
    assertRejectedBudget(20000);
    assertRejectedBudget(250000);
  }

  private static void assertRejectedBudget(int maxFEs) {
    try {
      V35MidHorizonDiagnosticDriver.defaultCheckpoints(maxFEs);
      throw new AssertionError("non-final budget must be rejected by the V3.1 driver");
    } catch (IllegalArgumentException expected) {
      // expected V3.1 scope rejection
    }
  }

  @Test
  public void telemetryDoesNotAdvertiseSyntheticSequenceEvidence() {
    V35MidHorizonTelemetry telemetry = new V35MidHorizonTelemetry(
        null, null, null, null, "run-source", "jar", "cfg", "inst", 7L, "A4", true);
    assertTrue(telemetry.getTrueRngEvidenceSource().startsWith("UNAVAILABLE_"));
    assertTrue(telemetry.getGeneratedCandidateEvidenceSource().startsWith("UNAVAILABLE_"));
    assertEquals("UNAVAILABLE", telemetry.getTrueRngHashOrUnavailable());
    assertEquals("UNAVAILABLE", telemetry.getGeneratedCandidateHashOrUnavailable());
    assertTrue(telemetry.contractProperties().contains(
        "allowTerminalPartialFormalQPhase=false"));
  }

  @Test
  public void pddrLedgerObserverRecordsEveryCandidate() {
    V35FullPddrLedgerObserver observer = new V35FullPddrLedgerObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", true);
    List<PermutationSolution<Integer>> pool = new ArrayList<>();
    pool.add(solution(new double[]{5, 20, 29}));
    pool.add(solution(new double[]{4, 21, 30}));
    pool.add(solution(new double[]{6, 19, 31}));
    List<Source> sources = Arrays.asList(Source.PARENT, Source.GLOBAL_OFFSPRING, Source.CATA_TEST);
    // selected = originalOrder 0 and 1
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected = new ArrayList<>();
    selected.add(ZhangBoEvaluatedPddrSelector.Candidate.ofEvaluated(
        solution(new double[]{5, 20, 29}), new ArrayList<PermutationSolution<Integer>>(),
        Source.PARENT, 0, 1, 0, 1.0));
    selected.add(ZhangBoEvaluatedPddrSelector.Candidate.ofEvaluated(
        solution(new double[]{4, 21, 30}), new ArrayList<PermutationSolution<Integer>>(),
        Source.GLOBAL_OFFSPRING, 1, 2, 1, 1.0));
    observer.onPddrRound(pool, sources, selected, 1000L, 1);
    String ledger = observer.ledgerCsv();
    String summary = observer.cycleSummaryCsv();
    assertTrue("must have 3 candidate rows", ledger.contains(",0,") && ledger.contains(",1,") && ledger.contains(",2,"));
    assertTrue("cycle summary must include poolSize=3", summary.contains(",1,1000,3,"));
    assertTrue("selected rows must be marked", ledger.contains(",true,"));
    assertEquals(0L, observer.getObserverErrors());
  }

  @Test
  public void teacherObserverDeduplicatesSameFingerprintPerCycle() {
    V35TeacherConcentrationObserver observer = new V35TeacherConcentrationObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", true);
    PermutationSolution<Integer> teacher = solution(new double[]{5, 20, 29});
    observer.onTeacherUse("QG", ZhangBoSubSwarm.G1_CMAX, teacher, 100L, 1);
    observer.onTeacherUse("QG", ZhangBoSubSwarm.G2_TEC, teacher, 150L, 1);
    observer.onTeacherUse("QP", ZhangBoSubSwarm.G1_CMAX, teacher, 200L, 1);
    String events = observer.eventsCsv();
    assertTrue("must have 3 event rows", events.split("\n").length >= 4);
    assertTrue("must include ALL_QG scope", events.contains("ALL_QG"));
    assertTrue("must include ALL_QP scope", events.contains("ALL_QP"));
    String concentration = observer.concentrationCsv();
    assertTrue("concentration must be produced", concentration.contains("ALL_QG"));
    assertEquals(0L, observer.getObserverErrors());
  }

  @Test
  public void caTaObserverRecordsRealCandidate() {
    V35CaTaContributionObserver observer = new V35CaTaContributionObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", true);
    observer.onCaTaCandidate("TEST", V35MacroNeighborhood.N1, ZhangBoSubSwarm.G1_CMAX,
        "SEQ", solution(new double[]{6, 21, 31}), solution(new double[]{5, 20, 29}),
        true, 1000L, 1);
    String events = observer.eventsCsv();
    assertTrue("must contain N1 TEST", events.contains("N1,TEST"));
    assertTrue("must mark accepted", events.contains(",true,"));
    String summary = observer.summaryCsv();
    assertTrue("summary must count generated", summary.contains("N1"));
    assertEquals(0L, observer.getObserverErrors());
  }

  @Test
  public void disabledObserversProduceZeroRows() {
    V35CheckpointFrontObserver checkpoint = new V35CheckpointFrontObserver(
        new long[]{1000L}, "run-1", "jar", "cfg", "inst", 20260901L, "A4", false);
    V35FullPddrLedgerObserver pddr = new V35FullPddrLedgerObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", false);
    V35TeacherConcentrationObserver teacher = new V35TeacherConcentrationObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", false);
    V35CaTaContributionObserver cata = new V35CaTaContributionObserver(
        "run-1", "jar", "cfg", "inst", 20260901L, "A4", false);
    checkpoint.onAtomicPhaseEnd(1000, 1, 1, 2, new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<PermutationSolution<Integer>>(), new ArrayList<PermutationSolution<Integer>>());
    pddr.onPddrRound(new ArrayList<PermutationSolution<Integer>>(),
        new ArrayList<Source>(), new ArrayList<ZhangBoEvaluatedPddrSelector.Candidate>(), 1000L, 1);
    teacher.onTeacherUse("QG", ZhangBoSubSwarm.G1_CMAX, solution(new double[]{5, 20, 29}), 1L, 1);
    cata.onCaTaCandidate("TEST", V35MacroNeighborhood.N1, ZhangBoSubSwarm.G1_CMAX,
        "SEQ", solution(new double[]{6, 21, 31}), solution(new double[]{5, 20, 29}), true, 1L, 1);
    assertEquals(0, checkpoint.getRowCount());
    assertEquals(0, pddr.getRowCount());
    assertEquals(0, teacher.getRowCount());
    assertEquals(0, cata.getRowCount());
  }
}
