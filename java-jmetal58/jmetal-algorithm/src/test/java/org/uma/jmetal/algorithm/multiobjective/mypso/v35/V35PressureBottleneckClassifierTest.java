package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;

public class V35PressureBottleneckClassifierTest {
  private static final String HASH =
      "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  @Test
  public void emptyFactoryFailsClosedToBalanced() {
    V35PressureBottleneckClassifier.Classification result = classifier().classify(
        result(Collections.<ZhangBoFatigueOperationRecord>emptyList()), instance(), parameters(),
        0, V35BottleneckDiagnosisConfiguration.confidence(0.1, 0.1, false));
    assertEquals(V35Bottleneck.BAL, result.getBottleneck());
    assertEquals("NO_ACTIVE_OPERATIONS", result.getReason());
    assertFalse(result.isConfident());
  }

  @Test
  public void fullMaskAuditAlwaysFallsBackButPreservesPressures() {
    V35PressureBottleneckClassifier.Classification value = classifier().classify(
        result(Collections.singletonList(op(0, 0, 0, 0, 0, 0, 10, 0, 10, 0.2))),
        instance(), parameters(), 0,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow());
    assertEquals(V35Bottleneck.BAL, value.getBottleneck());
    assertEquals("FULL_MASK_AUDIT", value.getReason());
    assertTrue(value.getMaximumPressure() > 0.0);
    for (V35Bottleneck pressure : new V35Bottleneck[]{
        V35Bottleneck.SEQ, V35Bottleneck.MAC, V35Bottleneck.WOR,
        V35Bottleneck.SET, V35Bottleneck.FAT}) {
      assertTrue(value.getPressure(pressure) >= 0.0);
      assertTrue(value.getPressure(pressure) <= 1.0);
    }
  }

  @Test
  public void setupAndFatigueUseTheirOwnPressureRatherThanFmaxAlone() {
    List<ZhangBoFatigueOperationRecord> setup = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      setup.add(op(index, 0, index, index, 0, 0, 100, 90, 100, 0.2));
    }
    V35PressureBottleneckClassifier.Classification setupResult = classifier().classify(
        result(setup), instance(), parameters(), 0,
        V35BottleneckDiagnosisConfiguration.confidence(0.5, 0.05, false));
    assertEquals(V35Bottleneck.SET, setupResult.getBottleneck());

    List<ZhangBoFatigueOperationRecord> fatigue = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      fatigue.add(op(index, 0, index, index, 0, 0, 100, 0, 10, 0.85));
    }
    V35PressureBottleneckClassifier.Classification fatigueResult = classifier().classify(
        result(fatigue), instance(), parameters(), 0,
        V35BottleneckDiagnosisConfiguration.confidence(0.5, 0.05, false));
    assertEquals(V35Bottleneck.FAT, fatigueResult.getBottleneck());
    assertTrue(fatigueResult.getPressure(V35Bottleneck.FAT)
        > fatigueResult.getPressure(V35Bottleneck.SET));
  }

  @Test
  public void confidenceRequiresBothAbsoluteStrengthAndGap() {
    List<ZhangBoFatigueOperationRecord> parallel = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      parallel.add(op(index, 0, index, index, 0, 0, 10, 0, 10, 0.2));
    }
    ZhangBoFatigueEvaluationResult evaluation = result(parallel);
    V35PressureBottleneckClassifier.Classification absolute = classifier().classify(
        evaluation, instance(), parameters(), 0,
        V35BottleneckDiagnosisConfiguration.confidence(1.0, 0.0, false));
    assertEquals(V35Bottleneck.BAL, absolute.getBottleneck());
    assertEquals("BELOW_ABSOLUTE_THRESHOLD", absolute.getReason());

    V35PressureBottleneckClassifier.Classification gap = classifier().classify(
        evaluation, instance(), parameters(), 0,
        V35BottleneckDiagnosisConfiguration.confidence(0.0, 1.0, false));
    assertEquals(V35Bottleneck.BAL, gap.getBottleneck());
    assertEquals("INSUFFICIENT_PRESSURE_GAP", gap.getReason());
  }

  @Test
  public void sequenceMachineAndWorkerPressureCanEachDominate() {
    V35BottleneckDiagnosisConfiguration open =
        V35BottleneckDiagnosisConfiguration.confidence(0.0, 0.0, false);
    List<ZhangBoFatigueOperationRecord> sequence = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      sequence.add(op(index, 0, 0, 0, index * 10, index * 10, 10, 0, 10, 0.1));
    }
    assertEquals(V35Bottleneck.SEQ, classifier().classify(result(sequence), instance(),
        parameters(), 0, open).getBottleneck());

    List<ZhangBoFatigueOperationRecord> machine = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      machine.add(op(index, 0, 0, index, index * 10, index * 10, 10, 0, 10, 0.1));
    }
    for (int index = 0; index < 10; index++) {
      machine.add(op(10 + index, 0, 1 + index, 10 + index,
          0, 0, 50, 0, 50, 0.1));
    }
    assertEquals(V35Bottleneck.MAC, classifier().classify(result(machine),
        instance(20, 21, 20), parameters(20), 0, open).getBottleneck());

    List<ZhangBoFatigueOperationRecord> worker = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      worker.add(op(index, 0, index, 0, index * 10, index * 10, 10, 0, 10, 0.1));
      worker.add(op(10 + index, 0, 10 + index, 1 + index, 0, 0, 50, 0, 50, 0.1));
    }
    assertEquals(V35Bottleneck.WOR, classifier().classify(result(worker),
        instance(20, 21, 20), parameters(20), 0, open).getBottleneck());
  }

  @Test
  public void diagnosisUsesOnlyTheFactorySelectedByNeed() {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>();
    operations.add(opFactory(0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 10, 0.9));
    for (int index = 0; index < 4; index++) {
      operations.add(opFactory(1 + index, 1 + index, 0, 1, index, index,
          0, 0, 100, 90, 100, 0.1));
    }
    V35PressureBottleneckClassifier.Classification selected = classifier().classify(
        result(operations), twoFactoryInstance(), twoFactoryParameters(), 1,
        V35BottleneckDiagnosisConfiguration.confidence(0.0, 0.0, false));
    assertEquals(1, selected.getFactory());
    assertEquals(V35Bottleneck.SET, selected.getBottleneck());
  }

  private static V35PressureBottleneckClassifier classifier() {
    return new V35PressureBottleneckClassifier();
  }

  private static ZhangBoFatigueInstanceData instance() {
    return instance(4, 5, 4);
  }

  private static ZhangBoFatigueInstanceData instance(int jobs, int machines, int workers) {
    double[][][] speed = new double[1][1][machines];
    int[][][] idle = new int[1][1][machines];
    java.util.Arrays.fill(speed[0][0], 1.0);
    java.util.Arrays.fill(idle[0][0], 1);
    int[][] standard = new int[jobs][1];
    for (int job = 0; job < jobs; job++) standard[job][0] = 1;
    double[][] efficiency = new double[1][workers];
    int[][] cost = new int[1][workers];
    java.util.Arrays.fill(efficiency[0], 1.0);
    java.util.Arrays.fill(cost[0], 1);
    return new ZhangBoFatigueInstanceData(HASH, jobs, 1, 1,
        new int[][]{{machines}}, speed, idle, standard,
        new int[]{workers}, efficiency, cost);
  }

  private static ZhangBoFatigueParameters parameters() {
    return parameters(4);
  }

  private static ZhangBoFatigueParameters parameters(int workers) {
    double[][][] lambda = new double[1][1][workers];
    double[][][] mu = new double[1][1][workers];
    java.util.Arrays.fill(lambda[0][0], 0.02);
    java.util.Arrays.fill(mu[0][0], 0.05);
    return new ZhangBoFatigueParameters(HASH,
        lambda, mu,
        new double[]{0.30}, 0.80, 0.90, "");
  }

  private static ZhangBoFatigueInstanceData twoFactoryInstance() {
    return new ZhangBoFatigueInstanceData(HASH, 5, 1, 2,
        new int[][]{{5}, {5}}, new double[][][]{{{1, 1, 1, 1, 1}}, {{1, 1, 1, 1, 1}}},
        new int[][][]{{{1, 1, 1, 1, 1}}, {{1, 1, 1, 1, 1}}},
        new int[][]{{1}, {1}, {1}, {1}, {1}},
        new int[]{4, 4}, new double[][]{{1, 1, 1, 1}, {1, 1, 1, 1}},
        new int[][]{{1, 1, 1, 1}, {1, 1, 1, 1}});
  }

  private static ZhangBoFatigueParameters twoFactoryParameters() {
    return new ZhangBoFatigueParameters(HASH,
        new double[][][]{{{0.02, 0.02, 0.02, 0.02}}, {{0.02, 0.02, 0.02, 0.02}}},
        new double[][][]{{{0.05, 0.05, 0.05, 0.05}}, {{0.05, 0.05, 0.05, 0.05}}},
        new double[]{0.30}, 0.80, 0.90, "");
  }

  private static ZhangBoFatigueOperationRecord op(int job, int stage, int machine, int worker,
      double predecessor, double start, double actual, double setup,
      double base, double fatigueAtStart) {
    return opFactory(job, job, stage, 0, machine, worker, predecessor, start,
        actual, setup, base, fatigueAtStart);
  }

  private static ZhangBoFatigueOperationRecord opFactory(int sequence, int job, int stage,
      int factory, int machine, int worker, double predecessor, double start, double actual,
      double setup, double base, double fatigueAtStart) {
    return new ZhangBoFatigueOperationRecord(sequence, job, stage, factory, machine, worker,
        predecessor, start, start, start, 0.0, fatigueAtStart, fatigueAtStart,
        Math.max(0.0, base - setup), setup, base,
        base <= 0.0 ? 1.0 : actual / base,
        Math.max(0.0, actual - setup), setup, actual, start + actual,
        Math.min(0.99, fatigueAtStart + 0.05), actual, actual, false);
  }

  private static ZhangBoFatigueEvaluationResult result(
      List<ZhangBoFatigueOperationRecord> operations) {
    return new ZhangBoFatigueEvaluationResult(HASH, "", operations,
        new ZhangBoFatigueMetrics(0, 0, 0, 0, 0, 0, 0, 0),
        new double[7], new double[0][][], new double[0][][], new double[0][][]);
  }
}
