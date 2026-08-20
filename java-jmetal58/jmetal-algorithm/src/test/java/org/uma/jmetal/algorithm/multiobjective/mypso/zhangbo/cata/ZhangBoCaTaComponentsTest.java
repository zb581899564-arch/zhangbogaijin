package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/** P7.2 bottleneck and 80/20 factory-Need component tests. */
public class ZhangBoCaTaComponentsTest {
  @Test
  public void classifierCoversAllSixBottlenecksWithStableSignals() {
    ZhangBoBottleneckClassifier classifier = new ZhangBoBottleneckClassifier(0.80);
    assertEquals(ZhangBoBottleneck.SEQ,
        classifier.classify(Collections.singletonList(op(0, 10, 0, 10, 10, 0, 1, .1))).getBottleneck());
    assertEquals(ZhangBoBottleneck.MAC,
        classifier.classify(Collections.singletonList(op(0, 10, 10, 0, 10, 0, 1, .1))).getBottleneck());
    assertEquals(ZhangBoBottleneck.WOR,
        classifier.classify(Collections.singletonList(op(0, 10, 10, 10, 0, 0, 1, .1))).getBottleneck());
    assertEquals(ZhangBoBottleneck.SET,
        classifier.classify(Collections.singletonList(op(0, 0, 0, 0, 0, 10, 1, .1))).getBottleneck());
    assertEquals(ZhangBoBottleneck.FAT,
        classifier.classify(Collections.singletonList(op(0, 0, 0, 0, 0, 0, 100, .99))).getBottleneck());
    List<ZhangBoFatigueOperationRecord> imbalance = Arrays.asList(
        op(0, 0, 0, 0, 0, 0, 100, .1), op(1, 0, 0, 0, 0, 0, 1, .1));
    assertEquals(ZhangBoBottleneck.BAL, classifier.classify(imbalance).getBottleneck());
    assertEquals(ZhangBoBottleneck.BAL,
        classifier.classify(Collections.<ZhangBoFatigueOperationRecord>emptyList()).getBottleneck());
  }

  @Test
  public void factorySelectorSeparatesNeedWeightedAndUniformExploration() {
    ZhangBoFatigueEvaluationResult evaluation = result(Arrays.asList(
        op(0, 0, 0, 0, 0, 2, 10, .85),
        op(1, 0, 0, 0, 0, 8, 30, .95)));
    ZhangBoFactoryNeedSelector selector = new ZhangBoFactoryNeedSelector();
    List<ZhangBoFactoryNeedSelector.Need> needs = selector.calculate(
        evaluation, 2, ZhangBoSubSwarm.G2_TEC);
    assertEquals(2, needs.size());
    ZhangBoFactoryNeedSelector.Selection weighted = selector.select(
        evaluation, 2, ZhangBoSubSwarm.G2_TEC, 0.80, new FixedRandom(.10));
    assertTrue(weighted.isApplicable());
    assertFalse(weighted.isExploratory());
    assertEquals("NEED_WEIGHTED", weighted.getReason());
    ZhangBoFactoryNeedSelector.Selection exploration = selector.select(
        evaluation, 2, ZhangBoSubSwarm.G2_TEC, 0.80, new FixedRandom(.95));
    assertTrue(exploration.isApplicable());
    assertTrue(exploration.isExploratory());
    assertEquals("UNIFORM_EXPLORATION", exploration.getReason());
  }

  @Test
  public void O13HardGateIsSharedByFixedAndCaTaAcceptance() {
    ZhangBoFatigueEvaluationResult parent = resultWithRecovery(1.0);
    ZhangBoFatigueEvaluationResult unchanged = resultWithRecovery(1.0);
    ZhangBoFatigueEvaluationResult improved = resultWithRecovery(1.1);
    assertFalse(ZhangBoNaturalRecoveryGate.allows(
        ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW, parent, unchanged));
    assertTrue(ZhangBoNaturalRecoveryGate.allows(
        ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW, parent, improved));
    // Non-O13 neighborhoods do not inherit the recovery-only restriction.
    assertTrue(ZhangBoNaturalRecoveryGate.allows(
        ZhangBoNeighborhoodId.O1_JS_INSERT, parent, unchanged));
  }

  private static ZhangBoFatigueOperationRecord op(int factory, double start,
      double predecessor, double machineAvailable, double workerAvailable,
      double setup, double duration, double fatigueAfter) {
    return new ZhangBoFatigueOperationRecord(0, factory, 0, factory, 0, 0,
        predecessor, machineAvailable, workerAvailable, start, 0.0, 0.0, 0.0,
        Math.max(0.0, duration - setup), setup, duration, 1.0,
        Math.max(0.0, duration - setup), setup, duration, start + duration,
        fatigueAfter, duration, duration, fatigueAfter >= .90);
  }

  private static ZhangBoFatigueEvaluationResult result(
      List<ZhangBoFatigueOperationRecord> operations) {
    ZhangBoFatigueMetrics metrics = new ZhangBoFatigueMetrics(
        .95, .5, .1, 0, 0, 0, 0, 0);
    return new ZhangBoFatigueEvaluationResult("instance", "configuration", operations,
        metrics, new double[7], new double[0][][], new double[0][][], new double[0][][]);
  }

  private static ZhangBoFatigueEvaluationResult resultWithRecovery(double recovery) {
    ZhangBoFatigueMetrics metrics = new ZhangBoFatigueMetrics(
        .95, .5, .1, 0, 0, 0, recovery, 0);
    return new ZhangBoFatigueEvaluationResult("instance", "configuration",
        Collections.<ZhangBoFatigueOperationRecord>emptyList(), metrics,
        new double[7], new double[0][][], new double[0][][], new double[0][][]);
  }

  private static final class FixedRandom implements PseudoRandomGenerator {
    private final double value;
    FixedRandom(double value) { this.value = value; }
    @Override public int nextInt(int lowerBound, int upperBound) { return lowerBound; }
    @Override public double nextDouble(double lowerBound, double upperBound) { return value; }
    @Override public double nextDouble() { return value; }
    @Override public void setSeed(long seed) { }
    @Override public long getSeed() { return 20260808L; }
    @Override public String getName() { return "fixed"; }
  }
}
