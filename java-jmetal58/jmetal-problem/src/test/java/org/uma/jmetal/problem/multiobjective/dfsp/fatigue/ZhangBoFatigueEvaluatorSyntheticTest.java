package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoFatigueEvaluatorSyntheticTest {
  @Test
  public void laterStageShouldUseFatigueEctStableTieBreakAndNeverInsertRest() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = parameters(instance.getInstanceSha256(), 0.05, 0.10);
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1, 2, 3), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 0, 0), Arrays.asList(0, 0, 0, 0), "fatigue_improved");
    ZhangBoFatigueEvaluationResult result = new ZhangBoFatigueEvaluator()
        .evaluate(instance, parameters, solution);

    assertEquals(8, result.getOperations().size());
    ZhangBoFatigueOperationRecord firstLater = laterOperations(result).get(0);
    assertEquals(1, firstLater.worker); // workers 1 and 2 are equal: lower id wins.
    boolean workerTwoUsed = false;
    for (ZhangBoFatigueOperationRecord record : laterOperations(result)) {
      if (record.worker == 2) workerTwoUsed = true;
      assertTrue(record.end > record.start);
    }
    assertTrue(workerTwoUsed); // busy/fatigued worker loses an ECT comparison.
    assertTrue(result.getMetrics().safeThresholdEventCount > 0);
    assertEquals(8, result.getOperations().size()); // diagnostics did not insert rest operations.
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalFirstStageWorkerShouldBeRejectedWithNoRepair() {
    ZhangBoFatigueInstanceData instance = instance();
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1, 2, 3), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 0, 0), Arrays.asList(1, 0, 0, 0), "fatigue_improved");
    new ZhangBoFatigueEvaluator().evaluate(instance,
        parameters(instance.getInstanceSha256(), 0.80, 0.90), solution);
  }

  private static List<ZhangBoFatigueOperationRecord> laterOperations(
      ZhangBoFatigueEvaluationResult result) {
    java.util.ArrayList<ZhangBoFatigueOperationRecord> later = new java.util.ArrayList<>();
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      if (record.stage == 1) later.add(record);
    }
    return later;
  }

  private static ZhangBoFatigueInstanceData instance() {
    String sha = repeat('C', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}}, repeat('D', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 2, 1,
        new int[][] {{1, 2}},
        new double[][][] {{{1.0}, {1.0, 1.0}}},
        new int[][][] {{{5}, {5, 5}}},
        new int[][] {{10, 20}, {10, 20}, {10, 20}, {10, 20}},
        new int[] {3}, new double[][] {{1.0, 1.0, 1.0}},
        new int[][] {{10, 10, 10}}, extension);
  }

  private static ZhangBoFatigueParameters parameters(
      String instanceSha, double warning, double safe) {
    double[][][] lambda = new double[][][] {{{0.03, 0.03, 0.03}, {0.03, 0.03, 0.03}}};
    double[][][] mu = new double[][][] {{{0.05, 0.05, 0.05}, {0.05, 0.05, 0.05}}};
    return new ZhangBoFatigueParameters(instanceSha, lambda, mu,
        new double[] {0.30, 0.30}, warning, safe, "");
  }

  private static String repeat(char value, int count) {
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }
}
