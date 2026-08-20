package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4DecodeIllustrationFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OriginalDhhfspDecoderTest {
  private static final long SEED = 20260808L;
  private static final double EPSILON = 1.0e-9;

  @Test
  public void shouldMatchFrozenThreePhaseGoldenTracesAndObjectives() throws IOException {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), fixture.createSolution(), DecodeOptions.deterministic(SEED));
    assertEquals(resource("/dfsp/chapter4/p3-fig3-initial-deterministic.csv"),
        result.getInitial().operationsCsv().trim());
    assertEquals(resource("/dfsp/chapter4/p3-fig3-fine-deterministic.csv"),
        result.getFineTuned().operationsCsv().trim());
    assertEquals(resource("/dfsp/chapter4/p3-fig3-right-deterministic.csv"),
        result.getRightShifted().operationsCsv().trim());
    String frozenObjectives = resource("/dfsp/chapter4/p3-fig3-objectives.properties");
    assertTrue(frozenObjectives.contains("seed=20260808"));
    assertTrue(frozenObjectives.contains("right.tec=2011.4325892962256"));
    ObjectiveBreakdown objective = result.getFinalSnapshot().getObjectives();
    assertEquals(60.68870523415978, objective.getMakespan(), EPSILON);
    assertEquals(1982.7962256598619, objective.getProcessingEnergy(), EPSILON);
    assertEquals(28.636363636363647, objective.getStandbyEnergy(), EPSILON);
    assertEquals(2011.4325892962256, objective.getTotalEnergy(), EPSILON);
    assertEquals(2602.9254079254083, objective.getTotalWorkerCost(), EPSILON);
    assertTrue(result.getRightShifted().isAccepted());
  }

  @Test
  public void shouldDecodePublishedFig3WithExactStageOneAssignmentsAndDuration() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspFourVectorSolution solution = fixture.createSolution();
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), solution, DecodeOptions.deterministic(SEED));

    assertEquals(20, result.getInitial().getOperations().size());
    assertEquals(20, result.getFineTuned().getOperations().size());
    assertEquals(20, result.getFinalSnapshot().getOperations().size());
    assertTrue(result.getInitial().getValidation().isValid());
    assertTrue(result.getFineTuned().getValidation().isValid());
    assertTrue(result.getFinalSnapshot().getValidation().isValid());
    assertEquals("author_actual_compatibility:unit_standby_energy_rate=1.0",
        result.getStandbyEnergyProvenance());
    assertFalse(result.getInitial().getMachineTimelines().isEmpty());
    assertFalse(result.getInitial().getWorkerTimelines().isEmpty());

    Map<String, OperationRecord> initial = byKey(result.getInitial().getOperations());
    OperationRecord j6s1 = initial.get("5:0");
    assertEquals(1, j6s1.getFactory());
    assertEquals(1, j6s1.getMachine());
    assertEquals(1, j6s1.getWorker());
    assertEquals(2.0 / 1.2, j6s1.getSetupDuration(), EPSILON);
    assertEquals(8.0 / (1.1 * 1.2), j6s1.getProcessingDuration(), EPSILON);
    assertEquals(2.0 / 1.2 + 8.0 / (1.1 * 1.2), j6s1.getDuration(), EPSILON);

    for (int job = 0; job < 10; job++) {
      OperationRecord operation = initial.get(job + ":0");
      assertEquals(solution.getFactoryAssignmentForJob(job), operation.getFactory());
      assertEquals(solution.getMachineAssignmentForJob(job), operation.getMachine());
      assertEquals(solution.getWorkerAssignmentForJob(job), operation.getWorker());
    }
  }

  @Test
  public void deterministicAndPublishedModesShouldReplayOneHundredTimes() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    OriginalDhhfspDecoder decoder = new OriginalDhhfspDecoder();
    String deterministic = decoder.decode(
        fixture.getInstance(), fixture.createSolution(),
        DecodeOptions.deterministic(SEED)).toCanonicalText();
    String published = decoder.decode(
        fixture.getInstance(), fixture.createSolution(),
        DecodeOptions.published(SEED)).toCanonicalText();
    for (int repetition = 0; repetition < 100; repetition++) {
      assertEquals(deterministic, decoder.decode(
          fixture.getInstance(), fixture.createSolution(),
          DecodeOptions.deterministic(SEED)).toCanonicalText());
      assertEquals(published, decoder.decode(
          fixture.getInstance(), fixture.createSolution(),
          DecodeOptions.published(SEED)).toCanonicalText());
    }
    assertFalse("Published keyed worker choices must remain a distinct semantic mode",
        deterministic.equals(published));
  }

  @Test
  public void syntheticTieShouldExerciseEtcFifoFamAndWorkerBranches() {
    DhhfspInstance instance = new DhhfspInstance(
        3, 2, 1,
        new double[][] {{1, 1, 1}, {1, 1, 1}},
        new double[][] {{1, 1, 1}, {1, 1, 1}},
        new double[][][] {{{1, 1, 1}, {1, 1}}},
        new double[][][] {{{1, 1, 1}, {1, 1}}},
        new double[][][] {{{1, 1, 1}, {1, 1}}},
        new double[][][] {{{1, 1, 1}, {1, 1}}});
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(2, 0, 1), Arrays.asList(0, 0, 0),
        Arrays.asList(0, 1, 2), Arrays.asList(0, 1, 2),
        "deterministic_canonical");
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        instance, solution, DecodeOptions.deterministic(SEED));
    List<OperationRecord> secondStage = new ArrayList<>();
    for (OperationRecord operation : result.getInitial().getOperations()) {
      if (operation.getStage() == 1) secondStage.add(operation);
    }
    java.util.Collections.sort(secondStage,
        (a, b) -> Integer.compare(a.getDispatchOrdinal(), b.getDispatchOrdinal()));

    assertEquals(Arrays.asList(2, 0, 1), Arrays.asList(
        secondStage.get(0).getJob(), secondStage.get(1).getJob(), secondStage.get(2).getJob()));
    assertEquals(Arrays.asList(0, 1, 0), Arrays.asList(
        secondStage.get(0).getMachine(), secondStage.get(1).getMachine(),
        secondStage.get(2).getMachine()));
    assertEquals(Arrays.asList(0, 1, 0), Arrays.asList(
        secondStage.get(0).getWorker(), secondStage.get(1).getWorker(),
        secondStage.get(2).getWorker()));
    assertEquals(2.0, secondStage.get(0).getStartTime(), EPSILON);
    assertEquals(2.0, secondStage.get(1).getStartTime(), EPSILON);
    assertEquals(4.0, secondStage.get(2).getStartTime(), EPSILON);
    assertTrue(result.toCanonicalText().contains("ETC_FIFO_ORDER"));
    assertTrue(result.toCanonicalText().contains("ETC_TIE_BREAK"));
    assertTrue(result.toCanonicalText().contains("FAM_TIE_BREAK"));
    assertTrue(result.toCanonicalText().contains("WORKER_TIE_BREAK"));
    assertTrue(result.toCanonicalText().contains("WORKER_KEYED_CHOICE"));
    assertTrue(result.toCanonicalText().contains("FIRST_WAVE_WITHOUT_REPLACEMENT"));
    assertTrue(result.toCanonicalText().contains("EARLIEST_AVAILABLE"));
  }

  @Test
  public void fineTuningShouldInsertIntoACommonResourceGap() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), fixture.createSolution(), DecodeOptions.deterministic(SEED));
    OperationRecord initial = byKey(result.getInitial().getOperations()).get("0:0");
    OperationRecord fine = byKey(result.getFineTuned().getOperations()).get("0:0");
    assertEquals(44.022038567493, initial.getStartTime(), EPSILON);
    assertEquals(17.809917355372, fine.getStartTime(), EPSILON);
    assertTrue(fine.getEndTime() <= initial.getStartTime() + EPSILON);
  }

  @Test
  public void shouldPreserveInputAndAcceptOnlySafeRightShift() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspFourVectorSolution solution = fixture.createSolution();
    DhhfspFourVectorSolution before = solution.copy();
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), solution, DecodeOptions.deterministic(SEED));

    assertEquals(before.getJobSequence(), solution.getJobSequence());
    assertEquals(before.getFactoryAssignments(), solution.getFactoryAssignments());
    assertEquals(before.getMachineAssignments(), solution.getMachineAssignments());
    assertEquals(before.getWorkerAssignments(), solution.getWorkerAssignments());
    assertArrayEquals(before.getObjectives(), solution.getObjectives(), 0.0);
    assertTrue(solution.getAttributes().isEmpty());

    ObjectiveBreakdown fine = result.getFineTuned().getObjectives();
    ObjectiveBreakdown shifted = result.getRightShifted().getObjectives();
    assertEquals(fine.getMakespan(), shifted.getMakespan(), EPSILON);
    assertEquals(fine.getTotalWorkerCost(), shifted.getTotalWorkerCost(), EPSILON);
    assertTrue(shifted.getTotalEnergy() <= fine.getTotalEnergy() + EPSILON);
    assertEquals(factoryMakespans(result.getFineTuned()),
        factoryMakespans(result.getRightShifted()));
    assertResourceOrdersEqual(result.getFineTuned(), result.getRightShifted(), true);
    assertResourceOrdersEqual(result.getFineTuned(), result.getRightShifted(), false);
  }

  @Test
  public void jMetalAdapterShouldWriteExactlyThreeObjectivesAndCountOnce() {
    final Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    final DhhfspFourVectorSolution template = fixture.createSolution();
    EvaluationCounter counter = new EvaluationCounter();
    DhhfspProblem problem = new DhhfspProblem(
        fixture.getInstance(), new OriginalDhhfspDecoder(),
        DecodeOptions.deterministic(SEED),
        new DhhfspSolutionFactory() {
          @Override public DhhfspFourVectorSolution create() { return template.copy(); }
        }, counter);
    DhhfspFourVectorSolution solution = problem.createSolution();

    DecodeResult direct = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), solution, DecodeOptions.deterministic(SEED));
    assertEquals(0L, counter.getSuccessfulEvaluations());
    problem.evaluate(solution);
    assertEquals(1L, counter.getSuccessfulEvaluations());
    assertEquals(3, solution.getNumberOfObjectives());
    ObjectiveBreakdown expected = direct.getFinalSnapshot().getObjectives();
    assertArrayEquals(new double[] {
        expected.getMakespan(), expected.getTotalEnergy(), expected.getTotalWorkerCost()},
        solution.getObjectives(), EPSILON);
    assertNotNull(solution.getAttribute(DecodeResult.class));
  }

  @Test
  public void shouldKeepFig4IllustrationSeparateFromFig3ExecutableEncoding() {
    Chapter4DecodeIllustrationFixture illustration =
        Chapter4DecodeIllustrationFixture.load();
    assertEquals(Arrays.asList(6, 5, 8, 3, 1), illustration.getFig3Factory2Jobs());
    assertEquals(Arrays.asList(1, 3, 7, 8, 9), illustration.getFig4LegendJobs());
    assertFalse(illustration.getFig3Factory2Jobs().equals(illustration.getFig4LegendJobs()));
    assertEquals(54.9, illustration.getInitialCmax(), 0.0);
    assertEquals(45.9, illustration.getFineTunedCmax(), 0.0);
    assertEquals(45.9, illustration.getRightShiftedCmax(), 0.0);
  }

  @Test
  public void validatorShouldNameMachineWorkerPrecedenceFactoryAndResourceFailures() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspFourVectorSolution solution = fixture.createSolution();
    DecodeResult result = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), solution, DecodeOptions.deterministic(SEED));
    List<OperationRecord> valid = result.getFineTuned().getOperations();

    OperationRecord first = valid.get(0);
    assertViolation(fixture.getInstance(), solution,
        replace(valid, first, changed(first, first.getFactory(),
            fixture.getInstance().getMachineCount(first.getFactory(), first.getStage()),
            first.getWorker(), first.getStartTime())), "illegal machine");
    assertViolation(fixture.getInstance(), solution,
        replace(valid, first, changed(first, first.getFactory(), first.getMachine(),
            fixture.getInstance().getWorkerCount(first.getFactory(), first.getStage()),
            first.getStartTime())), "illegal worker");
    assertViolation(fixture.getInstance(), solution,
        replace(valid, first, changed(first, 1 - first.getFactory(),
            0, 0, first.getStartTime())), "factory mismatch");

    OperationRecord stageTwo = byKey(valid).get(first.getJob() + ":1");
    assertViolation(fixture.getInstance(), solution,
        replace(valid, stageTwo, changed(stageTwo, stageTwo.getFactory(),
            stageTwo.getMachine(), stageTwo.getWorker(), 0.0)), "precedence violation");

    OperationRecord sameStage = findSameStage(valid, first);
    assertViolation(fixture.getInstance(), solution,
        replace(valid, sameStage, changed(sameStage, sameStage.getFactory(),
            first.getMachine(), first.getWorker(), first.getStartTime())), "overlap");
  }

  private static Map<String, OperationRecord> byKey(List<OperationRecord> operations) {
    Map<String, OperationRecord> result = new HashMap<>();
    for (OperationRecord operation : operations) result.put(operation.operationKey(), operation);
    return result;
  }

  private static String resource(String path) throws IOException {
    InputStream stream = OriginalDhhfspDecoderTest.class.getResourceAsStream(path);
    assertNotNull(path, stream);
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = stream.read(buffer)) >= 0) bytes.write(buffer, 0, count);
      return new String(bytes.toByteArray(), StandardCharsets.UTF_8)
          .replace("\r\n", "\n").trim();
    } finally {
      stream.close();
    }
  }

  private static void assertResourceOrdersEqual(
      ScheduleSnapshot left, ScheduleSnapshot right, boolean machine) {
    assertEquals(resourceOrders(left.getOperations(), machine),
        resourceOrders(right.getOperations(), machine));
  }

  private static Map<String, List<String>> resourceOrders(
      List<OperationRecord> operations, boolean machine) {
    Map<String, List<OperationRecord>> grouped = new HashMap<>();
    for (OperationRecord operation : operations) {
      String key = operation.getFactory() + ":" + operation.getStage() + ":"
          + (machine ? operation.getMachine() : operation.getWorker());
      if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<OperationRecord>());
      grouped.get(key).add(operation);
    }
    Map<String, List<String>> result = new HashMap<>();
    for (Map.Entry<String, List<OperationRecord>> entry : grouped.entrySet()) {
      java.util.Collections.sort(entry.getValue(), (a, b) -> {
        int value = Double.compare(a.getStartTime(), b.getStartTime());
        return value == 0 ? Integer.compare(a.getDispatchOrdinal(), b.getDispatchOrdinal()) : value;
      });
      List<String> keys = new ArrayList<>();
      for (OperationRecord operation : entry.getValue()) keys.add(operation.operationKey());
      result.put(entry.getKey(), keys);
    }
    return result;
  }

  private static Map<Integer, Double> factoryMakespans(ScheduleSnapshot snapshot) {
    Map<Integer, Double> result = new HashMap<>();
    for (OperationRecord operation : snapshot.getOperations()) {
      if (operation.getStage() != 1) continue;
      Double previous = result.get(operation.getFactory());
      result.put(operation.getFactory(), previous == null
          ? operation.getEndTime() : Math.max(previous, operation.getEndTime()));
    }
    return result;
  }

  private static List<OperationRecord> replace(
      List<OperationRecord> source, OperationRecord oldValue, OperationRecord newValue) {
    List<OperationRecord> result = new ArrayList<>(source);
    result.set(result.indexOf(oldValue), newValue);
    return result;
  }

  private static OperationRecord changed(
      OperationRecord source, int factory, int machine, int worker, double start) {
    return new OperationRecord(
        source.getJob(), source.getStage(), factory, machine, worker, start,
        source.getSetupDuration(), source.getProcessingDuration(),
        start + source.getDuration(), source.getDispatchOrdinal());
  }

  private static OperationRecord findSameStage(
      List<OperationRecord> operations, OperationRecord source) {
    for (OperationRecord operation : operations) {
      if (operation.getStage() == source.getStage()
          && operation.getFactory() == source.getFactory()
          && operation.getJob() != source.getJob()) return operation;
    }
    throw new AssertionError("No operation in the same factory/stage");
  }

  private static void assertViolation(
      DhhfspInstance instance, DhhfspFourVectorSolution solution,
      List<OperationRecord> operations, String text) {
    ScheduleValidationReport report = ScheduleValidator.validate(instance, solution, operations);
    assertFalse(report.isValid());
    assertTrue(report.getViolations().toString(),
        report.getViolations().toString().contains(text));
  }
}
