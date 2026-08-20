package org.uma.jmetal.problem.multiobjective.dfsp;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterGenerator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.solution.PermutationSolution;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ZhangBoFatigueIntegrationTest {
  private static final int JOBS = 20;
  private static final int STAGES = 2;
  private static final int FACTORIES = 3;
  private static final int PROBLEM_ID = 1;
  private static final double EPS = 1.0e-12;

  @Test
  public void zeroImpactConstructorShouldUseTheFrozenAuthorBodyExactly() throws Exception {
    String previous = configureDataDirectory();
    String previousExtension = configureExtensionDirectory();
    try {
      ZhangBoEDHHFSPW authorPath = new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoFatigueParameters zero = ZhangBoFatigueParameterGenerator
          .generate(authorPath.getFatigueInstanceData()).withZeroImpact();
      ZhangBoEDHHFSPW zeroPath = new ZhangBoEDHHFSPW(
          JOBS, STAGES, FACTORIES, PROBLEM_ID, zero);
      PermutationSolution<Integer> original = explicitSolution(authorPath);
      PermutationSolution<Integer> authorSolution = copy(original);
      PermutationSolution<Integer> zeroSolution = copy(original);
      String fingerprintBefore = fingerprint(original);

      authorPath.evaluate(authorSolution);
      zeroPath.evaluate(zeroSolution);

      assertArrayEquals(authorSolution.getObjectives(), zeroSolution.getObjectives(), 0.0);
      assertCubeEquals(authorPath.time, zeroPath.time);
      assertCubeEquals(authorPath.jobEndPower, zeroPath.jobEndPower);
      assertCubeEquals(authorPath.jobCost, zeroPath.jobCost);
      assertNull(zeroSolution.getAttribute(ZhangBoFatigueEvaluationResult.class));
      assertEquals(fingerprintBefore, fingerprint(original));
      assertEquals(fingerprint(authorSolution), fingerprint(zeroSolution));
    } finally {
      restoreDataDirectory(previous);
      restore("dhfsp.instance.extension.dir", previousExtension);
    }
  }

  @Test
  public void activeFatiguePathShouldBeDeterministicFeasibleAndUseFirstStageWa() throws Exception {
    String previous = configureDataDirectory();
    String previousExtension = configureExtensionDirectory();
    try {
      ZhangBoEDHHFSPW source = new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoFatigueInstanceData instance = source.getFatigueInstanceData();
      Path parameterFile = ZhangBoFatigueParameterCodec.fileFor(
          Paths.get("..").toAbsolutePath().normalize().resolve("fatigue-parameters/v1"),
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoFatigueParameters parameters = ZhangBoFatigueParameterCodec.read(parameterFile, instance);
      byte[] parameterFileBefore = Files.readAllBytes(parameterFile);
      ZhangBoEDHHFSPW active = new ZhangBoEDHHFSPW(
          JOBS, STAGES, FACTORIES, PROBLEM_ID, parameters);
      PermutationSolution<Integer> base = explicitSolution(active);
      String fingerprint = fingerprint(base);
      byte[] expected = null;
      for (int repeat = 0; repeat < 100; repeat++) {
        PermutationSolution<Integer> evaluated = copy(base);
        active.evaluate(evaluated);
        ZhangBoFatigueEvaluationResult result = (ZhangBoFatigueEvaluationResult)
            evaluated.getAttribute(ZhangBoFatigueEvaluationResult.class);
        assertEquals(JOBS * STAGES, result.getOperations().size());
        assertArrayEquals(result.getObjectives(), evaluated.getObjectives(), 0.0);
        assertFeasible(result.getOperations());
        for (ZhangBoFatigueOperationRecord operation : result.getOperations()) {
          assertTrue(operation.fatigueAfter >= 0.0 && operation.fatigueAfter < 1.0);
          assertTrue(operation.durationMultiplier >= 1.0 - EPS);
          assertTrue(operation.durationMultiplier <= 1.30 + EPS);
          if (operation.stage == 0) {
            int position = base.getVariables().indexOf(operation.job);
            assertEquals(base.getVariableValueworker(position).intValue(), operation.worker);
            @SuppressWarnings("unchecked")
            List<Integer> machines = (List<Integer>) base.getAttribute("machine");
            assertEquals(machines.get(position).intValue(), operation.machine);
            assertEquals(operation.baseDuration,
                operation.baseProcessingDuration + operation.baseSetupDuration, EPS);
            assertEquals(operation.actualDuration,
                operation.actualProcessingDuration + operation.actualSetupDuration, EPS);
          }
        }
        byte[] actual = result.toCanonicalUtf8();
        if (expected == null) expected = actual;
        else assertArrayEquals(expected, actual);
        assertEquals(fingerprint, fingerprint(base));
      }
      assertArrayEquals(parameterFileBefore, Files.readAllBytes(parameterFile));
    } finally {
      restoreDataDirectory(previous);
      restore("dhfsp.instance.extension.dir", previousExtension);
    }
  }

  private static PermutationSolution<Integer> explicitSolution(ZhangBoEDHHFSPW problem) {
    PermutationSolution<Integer> solution = problem.createSolution();
    ZhangBoFatigueInstanceData instance = problem.getFatigueInstanceData();
    for (int position = 0; position < JOBS; position++) {
      int factory = position % FACTORIES;
      solution.setVariableValue(position, position);
      solution.setVariableValueid(position, factory);
      solution.setVariableValueworker(position, instance.getEligibleWorkers(factory, 0)[0]);
      @SuppressWarnings("unchecked")
      List<Integer> machines = (List<Integer>) solution.getAttribute("machine");
      machines.set(position, position % instance.getMachineCount(factory, 0));
    }
    return solution;
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copy(PermutationSolution<Integer> source) {
    return (PermutationSolution<Integer>) source.copy();
  }

  private static String fingerprint(PermutationSolution<Integer> solution) {
    return solution.getVariables().toString() + "|" + solution.getVariablesid().toString()
        + "|" + solution.getVariablesworker().toString() + "|" + solution.getAttribute("machine");
  }

  private static void assertFeasible(List<ZhangBoFatigueOperationRecord> operations) {
    Map<String, Double> machineEnd = new HashMap<>();
    Map<String, Double> workerEnd = new HashMap<>();
    Map<String, Double> jobEnd = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      String machine = operation.factory + ":" + operation.stage + ":" + operation.machine;
      String worker = operation.factory + ":" + operation.worker;
      String predecessor = operation.job + ":" + (operation.stage - 1);
      assertTrue(operation.start + EPS >= value(machineEnd, machine));
      assertTrue(operation.start + EPS >= value(workerEnd, worker));
      if (operation.stage > 0) assertTrue(operation.start + EPS >= value(jobEnd, predecessor));
      machineEnd.put(machine, operation.end);
      workerEnd.put(worker, operation.end);
      jobEnd.put(operation.job + ":" + operation.stage, operation.end);
    }
  }

  private static double value(Map<String, Double> values, String key) {
    Double value = values.get(key);
    return value == null ? 0.0 : value;
  }

  private static void assertCubeEquals(double[][][] expected, double[][][] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].length, actual[i].length);
      for (int j = 0; j < expected[i].length; j++) {
        assertArrayEquals(expected[i][j], actual[i][j], 0.0);
      }
    }
  }

  private static String configureDataDirectory() {
    String previous = System.getProperty("dhfsp.data.dir");
    System.setProperty("dhfsp.data.dir",
        Paths.get("..").toAbsolutePath().normalize().resolve("EADHFSP").toString());
    return previous;
  }

  private static void restoreDataDirectory(String previous) {
    if (previous == null) System.clearProperty("dhfsp.data.dir");
    else System.setProperty("dhfsp.data.dir", previous);
  }

  private static String configureExtensionDirectory() {
    String previous = System.getProperty("dhfsp.instance.extension.dir");
    System.setProperty("dhfsp.instance.extension.dir",
        Paths.get("..").toAbsolutePath().normalize().resolve("instance-extensions/v1").toString());
    return previous;
  }

  private static void restore(String key, String previous) {
    if (previous == null) System.clearProperty(key);
    else System.setProperty(key, previous);
  }
}
