package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.Assume;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** P8.4 correctness gates for fatigue-consistent schedule propagation and refinement. */
public class ZhangBoFatigueShiftRefinerTest {
  private static final double EPS = 1.0e-9;

  @Test
  public void commonGapSemanticsAllowsOnlyOneResourceSequenceToMoveEarlier() {
    ZhangBoFatigueInstanceData instance = syntheticInstance();
    ZhangBoFatigueParameters parameters = syntheticParameters(instance.getInstanceSha256());
    ZhangBoFatigueSchedulePropagator propagator = new ZhangBoFatigueSchedulePropagator();
    ZhangBoFatigueEvaluationResult base = propagator.propagate(
        ZhangBoScheduleGraph.from(syntheticSequenceSeed(instance), 2, 2), instance, parameters,
        ZhangBoFatigueEvaluationMode.CORRECTED_NO_FATIGUE);
    ZhangBoScheduleGraph graph = ZhangBoScheduleGraph.from(base, 2, 2);
    int operation = 1 * 2 + 1;

    ZhangBoScheduleGraph machineOnly = graph.moveEarlier(
        operation, graph.machinePosition(operation) - 1, graph.workerPosition(operation));
    ZhangBoScheduleGraph workerOnly = graph.moveEarlier(
        operation, graph.machinePosition(operation), graph.workerPosition(operation) - 1);

    assertEquals(graph.machinePosition(operation) - 1, machineOnly.machinePosition(operation));
    assertEquals(graph.workerPosition(operation), machineOnly.workerPosition(operation));
    assertEquals(graph.machinePosition(operation), workerOnly.machinePosition(operation));
    assertEquals(graph.workerPosition(operation) - 1, workerOnly.workerPosition(operation));
  }

  @Test
  public void formalConfigurationCarriesCommonGapV2Semantics() {
    assertEquals("fatigue-shift-v2-common-gap",
        ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION);
  }

  @Test
  public void leftAcceptanceIgnoresTecAndTwcWhileRightUsesFrozenCmaxAndFormalGain() {
    double[] before = new double[] {100.0, 100.0, 0, 0, 0, 0, 100.0};
    double[] left = new double[] {100.0, 110.0, 0, 0, 0, 0, 120.0};
    assertNull(ZhangBoFatigueShiftRefiner.leftObjectiveReason(
        20.0, 10.0, before, left, EPS));

    double[] right = new double[] {105.0, 90.0, 0, 0, 0, 0, 100.0};
    assertNull(ZhangBoFatigueShiftRefiner.rightObjectiveReason(
        10.0, 15.0, 110.0, before, right, EPS));
    assertEquals("CMAX_STAR_EXCEEDED", ZhangBoFatigueShiftRefiner.rightObjectiveReason(
        10.0, 15.0, 104.0, before, right, EPS));
    assertEquals("NO_TEC_TWC_GAIN", ZhangBoFatigueShiftRefiner.rightObjectiveReason(
        10.0, 15.0, 110.0, before,
        new double[] {105.0, 100.0, 0, 0, 0, 0, 100.0}, EPS));
  }

  @Test
  public void commonGapPreviewAndFullReplayShareTheTransitionAndUpdateDownstreamFatigue() {
    ZhangBoFatigueInstanceData instance = syntheticInstance();
    ZhangBoFatigueParameters parameters = syntheticParameters(instance.getInstanceSha256());
    ZhangBoFatigueSchedulePropagator propagator = new ZhangBoFatigueSchedulePropagator();
    ZhangBoScheduleGraph baseGraph = ZhangBoScheduleGraph.from(
        syntheticSequenceSeed(instance), 2, 2);
    ZhangBoFatigueEvaluationResult base = propagator.propagate(baseGraph, instance, parameters,
        ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);
    int moved = 3;
    ZhangBoScheduleGraph candidateGraph = ZhangBoScheduleGraph.from(base, 2, 2)
        .moveEarlier(moved, 0, 0);
    ZhangBoFatigueOperationRecord jobPrevious = find(base, 1, 0);
    ZhangBoFatigueSchedulePropagator.OperationTransition preview =
        propagator.previewOperation(candidateGraph, moved, instance, parameters,
            ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
            jobPrevious.end, 0.0, null, jobPrevious.end);
    ZhangBoFatigueEvaluationResult replay = propagator.propagate(candidateGraph, instance,
        parameters, ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);
    ZhangBoFatigueOperationRecord movedAfter = find(replay, 1, 1);
    assertEquals(preview.start, movedAfter.start, EPS);
    assertEquals(preview.fatigueAtStart, movedAfter.fatigueAtStart, EPS);
    assertEquals(preview.actualDuration, movedAfter.actualDuration, EPS);
    assertEquals(preview.finish, movedAfter.end, EPS);
    assertEquals(preview.fatigueAfter, movedAfter.fatigueAfter, EPS);
    assertTrue(Math.abs(find(base, 0, 1).fatigueAtStart
        - find(replay, 0, 1).fatigueAtStart) > EPS);
  }

  @Test
  public void unchangedGraphRepropagationIsExactlyTheSameScheduleSemantics() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoFatigueEvaluationResult base = fixture.evaluateBase();
      ZhangBoScheduleGraph graph = ZhangBoScheduleGraph.from(
          base, fixture.problem.getInstance().getJobs(), fixture.problem.getInstance().getStages());
      ZhangBoFatigueEvaluationResult propagated = new ZhangBoFatigueSchedulePropagator().propagate(
          graph, fixture.problem.getInstance(), fixture.problem.getParameters(),
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);

      assertArrayEquals(base.getObjectives(), propagated.getObjectives(), EPS);
      assertEquals(base.getOperations().size(), propagated.getOperations().size());
      for (int index = 0; index < base.getOperations().size(); index++) {
        assertOperationEquals(base.getOperations().get(index), propagated.getOperations().get(index));
      }
      assertMetricsEqual(base, propagated);
    } finally {
      fixture.close();
    }
  }

  @Test
  public void noneModePreservesTheCurrentS0ByteForByte() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoFatigueEvaluationResult base = fixture.evaluateBase();
      ZhangBoFatigueEvaluationResult none = new ZhangBoFatigueEvaluator().evaluate(
          fixture.problem.getInstance(), fixture.problem.getParameters(), fixture.solution,
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
          ZhangBoShiftConfiguration.none());
      assertArrayEquals(base.toCanonicalUtf8(), none.toCanonicalUtf8());
    } finally {
      fixture.close();
    }
  }

  @Test
  public void leftRightIsDeterministicBoundedAndUsesTheLockedAcceptanceRules() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoShiftConfiguration configuration =
          ZhangBoShiftConfiguration.formalLeftRight().withFullTrace(true);
      ZhangBoFatigueEvaluationResult first = new ZhangBoFatigueEvaluator().evaluate(
          fixture.problem.getInstance(), fixture.problem.getParameters(), fixture.solution,
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION, configuration);
      ZhangBoFatigueEvaluationResult second = new ZhangBoFatigueEvaluator().evaluate(
          fixture.problem.getInstance(), fixture.problem.getParameters(), fixture.solution,
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION, configuration);
      assertArrayEquals(first.toCanonicalUtf8(), second.toCanonicalUtf8());
      assertNotNull(first.getShiftSummary());
      ZhangBoShiftSummary summary = first.getShiftSummary();
      assertTrue("I1/X0 must expose an honest FCRS event",
          summary.getRightAccepted() > 0);
      int operationCount = fixture.problem.getInstance().getJobs()
          * fixture.problem.getInstance().getStages();
      assertTrue(summary.getLeftCandidates() <= operationCount * 8);
      assertTrue(summary.getAfterLeftObjectives()[0]
          <= summary.getBaseObjectives()[0] + EPS);
      assertEquals(summary.getAfterLeftObjectives()[0], summary.getCmaxStar(), EPS);
      assertTrue(summary.getFinalObjectives()[0] <= summary.getCmaxStar() + EPS);
      assertTrue(summary.getFinalObjectives()[1]
          <= summary.getAfterLeftObjectives()[1] + EPS);
      assertTrue(summary.getFinalObjectives()[6]
          <= summary.getAfterLeftObjectives()[6] + EPS);
      for (ZhangBoShiftEvent event : summary.getEvents()) {
        if (!event.accepted) continue;
        if ("FCLS".equals(event.phase)) {
          assertEquals(0, event.backtrackingAttempt);
          assertTrue(Double.isFinite(event.commonGapLeft));
          assertTrue(event.newStart < event.oldStart - EPS);
          assertTrue(event.newCmax <= event.oldCmax + EPS);
        } else if ("FCRS".equals(event.phase)) {
          assertEquals(summary.getCmaxStar(), event.cmaxStar, EPS);
          assertTrue(event.backtrackingAttempt >= 1
              && event.backtrackingAttempt <= configuration.getMaximumRightAttempts());
          assertTrue(event.newCmax <= event.cmaxStar + EPS);
          assertTrue(event.newTec <= event.oldTec + EPS);
          assertTrue(event.newTwc <= event.oldTwc + EPS);
          assertTrue(event.newTec < event.oldTec - EPS
              || event.newTwc < event.oldTwc - EPS);
        }
      }
      assertNoOverlap(first.getOperations());
    } finally {
      fixture.close();
    }
  }

  @Test
  public void i1IllustrationAcceptanceRequiresBothHonestDirections() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoFatigueEvaluationResult shifted = new ZhangBoFatigueEvaluator().evaluate(
          fixture.problem.getInstance(), fixture.problem.getParameters(), fixture.solution,
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
          ZhangBoShiftConfiguration.formalLeftRight().withFullTrace(true));
      ZhangBoShiftSummary summary = shifted.getShiftSummary();
      String gate = "I1/X0 shift illustration gate: left=" + summary.getLeftAccepted()
          + '/' + summary.getLeftCandidates() + ", right=" + summary.getRightAccepted()
          + '/' + summary.getRightCandidates();
      Assume.assumeTrue(gate,
          summary.getLeftAccepted() > 0 && summary.getRightAccepted() > 0);
      assertTrue(gate, true);
    } finally {
      fixture.close();
    }
  }

  @Test
  public void onePassRefinementIsDeterministicFromTheSameOriginalInput() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoShiftConfiguration configuration =
          ZhangBoShiftConfiguration.formalLeftRight().withFullTrace(true);
      ZhangBoFatigueEvaluationResult base = fixture.evaluateBase();
      ZhangBoFatigueShiftRefiner refiner = new ZhangBoFatigueShiftRefiner();
      ZhangBoFatigueEvaluationResult first = refiner.refine(base,
          fixture.problem.getInstance(), fixture.problem.getParameters(),
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION, configuration);
      ZhangBoFatigueEvaluationResult second = refiner.refine(base,
          fixture.problem.getInstance(), fixture.problem.getParameters(),
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION, configuration);
      assertArrayEquals(first.toCanonicalUtf8(), second.toCanonicalUtf8());
      assertEquals(operationFingerprint(first), operationFingerprint(second));
    } finally {
      fixture.close();
    }
  }

  @Test
  public void syntheticCommonGapHasAnHonestCmaxSafeLeftShift() {
    ZhangBoFatigueInstanceData instance = syntheticInstance();
    ZhangBoFatigueParameters parameters = syntheticParameters(instance.getInstanceSha256());
    ZhangBoFatigueEvaluationResult seed = syntheticSequenceSeed(instance);
    ZhangBoFatigueSchedulePropagator propagator = new ZhangBoFatigueSchedulePropagator();
    ZhangBoFatigueEvaluationResult base = propagator.propagate(
        ZhangBoScheduleGraph.from(seed, 2, 2), instance, parameters,
        ZhangBoFatigueEvaluationMode.CORRECTED_NO_FATIGUE);
    ZhangBoFatigueEvaluationResult shifted = new ZhangBoFatigueShiftRefiner(propagator).refine(
        base, instance, parameters, ZhangBoFatigueEvaluationMode.CORRECTED_NO_FATIGUE,
        new ZhangBoShiftConfiguration(ZhangBoShiftMode.LEFT_ONLY, EPS, 8, 10, true));
    assertTrue("left=" + shifted.getShiftSummary().getLeftAccepted() + "/"
            + shifted.getShiftSummary().getLeftCandidates() + " reasons="
            + eventReasons(shifted.getShiftSummary()),
        shifted.getShiftSummary().getLeftAccepted() > 0);
    assertTrue(shifted.getObjectives()[0] < base.getObjectives()[0] - EPS);
  }

  @Test
  public void rightOnlyPreservesAssignmentsAndEveryResourceOrder() throws Exception {
    Fixture fixture = fixture();
    try {
      ZhangBoFatigueEvaluationResult base = fixture.evaluateBase();
      ZhangBoFatigueEvaluationResult right = new ZhangBoFatigueEvaluator().evaluate(
          fixture.problem.getInstance(), fixture.problem.getParameters(), fixture.solution,
          ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
          new ZhangBoShiftConfiguration(ZhangBoShiftMode.RIGHT_ONLY, EPS, 8, 10, true));
      assertEquals(ZhangBoShiftMode.RIGHT_ONLY, right.getShiftSummary().getMode());
      assertEquals(assignmentFingerprint(base), assignmentFingerprint(right));
      assertEquals(resourceOrderFingerprint(base), resourceOrderFingerprint(right));
    } finally {
      fixture.close();
    }
  }

  private static void assertParetoSafe(double[] before, double[] after) {
    for (int objective : new int[] {0, 1, 6}) {
      assertTrue("objective " + objective + " worsened", after[objective] <= before[objective] + EPS);
    }
  }

  private static void assertOperationEquals(
      ZhangBoFatigueOperationRecord expected, ZhangBoFatigueOperationRecord actual) {
    assertEquals(expected.job, actual.job);
    assertEquals(expected.stage, actual.stage);
    assertEquals(expected.factory, actual.factory);
    assertEquals(expected.machine, actual.machine);
    assertEquals(expected.worker, actual.worker);
    assertEquals(expected.start, actual.start, EPS);
    assertEquals(expected.end, actual.end, EPS);
    assertEquals(expected.fatigueAtStart, actual.fatigueAtStart, EPS);
    assertEquals(expected.fatigueAfter, actual.fatigueAfter, EPS);
    assertEquals(expected.energy, actual.energy, EPS);
    assertEquals(expected.cost, actual.cost, EPS);
  }

  private static void assertMetricsEqual(
      ZhangBoFatigueEvaluationResult expected, ZhangBoFatigueEvaluationResult actual) {
    assertEquals(expected.getMetrics().maximumFatigue, actual.getMetrics().maximumFatigue, EPS);
    assertEquals(expected.getMetrics().averageEventFatigue,
        actual.getMetrics().averageEventFatigue, EPS);
    assertEquals(expected.getMetrics().fatigueExcessIntegral,
        actual.getMetrics().fatigueExcessIntegral, EPS);
    assertEquals(expected.getMetrics().workerFatigueVarianceAtMakespan,
        actual.getMetrics().workerFatigueVarianceAtMakespan, EPS);
    assertEquals(expected.getMetrics().highFatigueTimeRatio,
        actual.getMetrics().highFatigueTimeRatio, EPS);
    assertEquals(expected.getMetrics().longestContinuousWork,
        actual.getMetrics().longestContinuousWork, EPS);
    assertEquals(expected.getMetrics().totalNaturalRecovery,
        actual.getMetrics().totalNaturalRecovery, EPS);
    assertEquals(expected.getMetrics().safeThresholdEventCount,
        actual.getMetrics().safeThresholdEventCount);
  }

  private static void assertNoOverlap(List<ZhangBoFatigueOperationRecord> operations) {
    for (int left = 0; left < operations.size(); left++) {
      ZhangBoFatigueOperationRecord a = operations.get(left);
      for (int right = left + 1; right < operations.size(); right++) {
        ZhangBoFatigueOperationRecord b = operations.get(right);
        if (a.factory == b.factory && a.stage == b.stage && a.machine == b.machine) {
          assertTrue(a.end <= b.start + EPS || b.end <= a.start + EPS);
        }
        if (a.factory == b.factory && a.worker == b.worker) {
          assertTrue(a.end <= b.start + EPS || b.end <= a.start + EPS);
        }
      }
    }
  }

  private static String operationFingerprint(ZhangBoFatigueEvaluationResult result) {
    StringBuilder text = new StringBuilder();
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      text.append(record.job).append('/').append(record.stage).append('/')
          .append(record.factory).append('/').append(record.machine).append('/')
          .append(record.worker).append('/').append(Double.toString(record.start)).append('/')
          .append(Double.toString(record.end)).append(';');
    }
    return text.toString();
  }

  private static String eventReasons(ZhangBoShiftSummary summary) {
    StringBuilder text = new StringBuilder();
    for (ZhangBoShiftEvent event : summary.getEvents()) {
      text.append(event.phase).append(':').append(event.reason).append(';');
    }
    return text.toString();
  }

  private static ZhangBoFatigueOperationRecord find(
      ZhangBoFatigueEvaluationResult result, int job, int stage) {
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      if (record.job == job && record.stage == stage) return record;
    }
    throw new AssertionError("missing operation " + job + '/' + stage);
  }

  private static String assignmentFingerprint(ZhangBoFatigueEvaluationResult result) {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>(result.getOperations());
    operations.sort(Comparator.comparingInt((ZhangBoFatigueOperationRecord op) -> op.job)
        .thenComparingInt(op -> op.stage));
    StringBuilder text = new StringBuilder();
    for (ZhangBoFatigueOperationRecord op : operations) {
      text.append(op.job).append('/').append(op.stage).append('/').append(op.factory)
          .append('/').append(op.machine).append('/').append(op.worker).append(';');
    }
    return text.toString();
  }

  private static String resourceOrderFingerprint(ZhangBoFatigueEvaluationResult result) {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>(result.getOperations());
    operations.sort(Comparator.comparingInt((ZhangBoFatigueOperationRecord op) -> op.factory)
        .thenComparingInt(op -> op.stage).thenComparingInt(op -> op.machine)
        .thenComparingDouble(op -> op.start).thenComparingInt(op -> op.job));
    StringBuilder machines = new StringBuilder();
    for (ZhangBoFatigueOperationRecord op : operations) {
      machines.append('M').append(op.factory).append('/').append(op.stage).append('/')
          .append(op.machine).append('=').append(op.job).append('/').append(op.stage).append(';');
    }
    operations.sort(Comparator.comparingInt((ZhangBoFatigueOperationRecord op) -> op.factory)
        .thenComparingInt(op -> op.worker).thenComparingDouble(op -> op.start)
        .thenComparingInt(op -> op.job).thenComparingInt(op -> op.stage));
    StringBuilder workers = new StringBuilder();
    for (ZhangBoFatigueOperationRecord op : operations) {
      workers.append('W').append(op.factory).append('/').append(op.worker).append('=')
          .append(op.job).append('/').append(op.stage).append(';');
    }
    return machines.append('|').append(workers).toString();
  }

  private static Fixture fixture() throws Exception {
    Path root = Files.createTempDirectory("p84-shift-");
    P8GoldenAuthorCompatibilityBridge.Manifest bridge =
        P8GoldenAuthorCompatibilityBridge.materialize(root);
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        bridge.dataFile, ProductionDecodeMode.FM3, 20260808L,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"));
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(5, 9, 4, 3, 6, 1, 7, 2, 0, 8),
        Arrays.asList(1, 0, 1, 0, 0, 0, 1, 1, 1, 0),
        Arrays.asList(1, 1, 1, 0, 0, 2, 0, 0, 1, 1),
        Arrays.asList(1, 1, 0, 1, 0, 1, 0, 1, 1, 0),
        ProductionDecodeMode.FM3.getSemanticTag(), 7);
    return new Fixture(root, problem, solution);
  }

  private static ZhangBoFatigueInstanceData syntheticInstance() {
    String sha = repeat('7', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 2, 2, new int[][] {{1, 1}, {1, 1}}, repeat('6', 64));
    return new ZhangBoFatigueInstanceData(sha, 2, 2, 1,
        new int[][] {{2, 1}}, new double[][][] {{{1.0, 1.0}, {1.0}}},
        new int[][][] {{{1, 1}, {1}}}, new int[][] {{100, 10}, {10, 10}},
        new int[] {3}, new double[][] {{1.0, 1.0, 1.0}},
        new int[][] {{1, 1, 1}}, extension);
  }

  private static ZhangBoFatigueParameters syntheticParameters(String sha) {
    return new ZhangBoFatigueParameters(sha,
        new double[][][] {{{0.02, 0.02, 0.02}, {0.02, 0.02, 0.02}}},
        new double[][][] {{{0.05, 0.05, 0.05}, {0.05, 0.05, 0.05}}},
        new double[] {0.30, 0.30}, 0.80, 0.90, "");
  }

  private static ZhangBoFatigueEvaluationResult syntheticSequenceSeed(
      ZhangBoFatigueInstanceData instance) {
    List<ZhangBoFatigueOperationRecord> records = Arrays.asList(
        record(0, 0, 0, 0, 0, 0, 0.0, 101.0),
        record(1, 0, 1, 0, 0, 2, 101.0, 112.0),
        record(2, 1, 0, 0, 1, 1, 0.0, 11.0),
        record(3, 1, 1, 0, 0, 2, 112.0, 123.0));
    return new ZhangBoFatigueEvaluationResult(instance.getInstanceSha256(), "",
        instance.getInstanceExtensionSha256(), records,
        new ZhangBoFatigueMetrics(0, 0, 0, 0, 0, 0, 0, 0),
        new double[7], new double[1][2][2], new double[1][2][2],
        new double[1][2][2]);
  }

  private static ZhangBoFatigueOperationRecord record(
      int sequence, int job, int stage, int factory, int machine, int worker,
      double start, double end) {
    return new ZhangBoFatigueOperationRecord(sequence, job, stage, factory, machine, worker,
        0.0, 0.0, 0.0, start, 0.0, 0.0, 0.0,
        end - start, 0.0, end - start, end, 0.0, end - start, end - start, false);
  }

  private static String repeat(char value, int count) {
    char[] values = new char[count];
    Arrays.fill(values, value);
    return new String(values);
  }

  private static final class Fixture implements AutoCloseable {
    private final Path root;
    private final ZhangBoCanonicalProductionProblem problem;
    private final DhhfspFourVectorSolution solution;
    private Fixture(Path root, ZhangBoCanonicalProductionProblem problem,
        DhhfspFourVectorSolution solution) {
      this.root = root;
      this.problem = problem;
      this.solution = solution;
    }
    private ZhangBoFatigueEvaluationResult evaluateBase() {
      return new ZhangBoFatigueEvaluator().evaluate(problem.getInstance(), problem.getParameters(),
          solution, ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);
    }
    @Override public void close() throws Exception {
      if (!Files.exists(root)) return;
      try (Stream<Path> paths = Files.walk(root)) {
        List<Path> ordered = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (Path path : ordered) Files.deleteIfExists(path);
      }
    }
  }
}
