package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/** P7.2 deterministic Test-and-Apply controller contract. */
public class ZhangBoCaTaControllerTest {
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

  private static ZhangBoCaTaContext context() {
    return new ZhangBoCaTaContext(ZhangBoSubSwarm.G2_TEC,
        ZhangBoCaTaPhase.MIDDLE, false, ZhangBoBottleneck.WOR);
  }

  @Test
  public void allValidNeighborhoodsAreTestedExactlyOnceBeforeApply() {
    ZhangBoCaTaController controller = new ZhangBoCaTaController(
        ZhangBoCaTaConfiguration.standard());
    List<ZhangBoNeighborhoodId> valid = Arrays.asList(
        ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW,
        ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER);
    ZhangBoCaTaController.Decision decision = controller.decide(
        context(), valid, new FixedRandom(0.99));
    assertTrue(decision.isTestPhase());
    assertEquals(2, decision.getNeighborhoods().size());
    assertEquals(1, decision.getRepetitions());
    controller.record(context(), ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER,
        true, 0.5, 10L, 1L);
    controller.record(context(), ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW,
        false, -0.5, 20L, 1L);
    ZhangBoCaTaController.Decision apply = controller.decide(
        context(), valid, new FixedRandom(0.99));
    assertFalse(apply.isTestPhase());
    assertEquals(ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER, apply.getNeighborhoods().get(0));
    assertEquals(1, apply.getRepetitions());

    controller.record(context(), ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER,
        true, 0.5, 10L, 1L);
    ZhangBoCaTaController.Decision secondApply = controller.decide(
        context(), valid, new FixedRandom(0.99));
    assertFalse(secondApply.isTestPhase());
    assertEquals(1, secondApply.getRepetitions());

    controller.record(context(), ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER,
        true, 0.5, 10L, 1L);
    ZhangBoCaTaController.Decision nextEpoch = controller.decide(
        context(), valid, new FixedRandom(0.99));
    assertTrue(nextEpoch.isTestPhase());
    assertEquals("APPLY_HORIZON_COMPLETE_TEST", nextEpoch.getReason());
    assertEquals("cata-apply-v2",
        ZhangBoCaTaConfiguration.ALGORITHM_SEMANTICS_VERSION);
    assertTrue(ZhangBoCaTaConfiguration.standard().toCanonicalText()
        .contains("caTa.algorithmSemanticsVersion=cata-apply-v2"));
  }

  @Test
  public void v2CostUsesEqualWeightedMedianNormalization() {
    ZhangBoCaTaStatistics statistics = new ZhangBoCaTaStatistics();
    ZhangBoCaTaContext c = context();
    statistics.record(c, ZhangBoNeighborhoodId.O1_JS_INSERT,
        false, 0.0, 100L, 1L);
    statistics.record(c, ZhangBoNeighborhoodId.O2_JS_REVERSE,
        false, 0.0, 10L, 2L);
    statistics.record(c, ZhangBoNeighborhoodId.O3_JS_SWAP,
        false, 0.0, 1L, 10L);

    assertEquals(ZhangBoNeighborhoodId.O2_JS_REVERSE,
        statistics.best(c, Arrays.asList(
            ZhangBoNeighborhoodId.O1_JS_INSERT,
            ZhangBoNeighborhoodId.O2_JS_REVERSE,
            ZhangBoNeighborhoodId.O3_JS_SWAP), true));
  }

  @Test
  public void threeFailuresTriggerRetestAndPhasesUseCanonicalBoundaries() {
    ZhangBoCaTaController controller = new ZhangBoCaTaController(
        ZhangBoCaTaConfiguration.standard());
    ZhangBoCaTaContext c = context();
    ZhangBoNeighborhoodId id = ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER;
    controller.decide(c, Collections.singletonList(id), new FixedRandom(0.99));
    controller.record(c, id, false, 0.0, 1L, 1L);
    controller.record(c, id, false, 0.0, 1L, 1L);
    controller.record(c, id, false, 0.0, 1L, 1L);
    assertTrue(controller.isStagnated(c.getSubSwarm(), c.getPhase(), c.getBottleneck()));
    ZhangBoCaTaController.Decision retest = controller.decide(
        c, Collections.singletonList(id), new FixedRandom(0.99));
    assertTrue(retest.isTestPhase());
    assertEquals("CONSECUTIVE_FAILURE_RETEST", retest.getReason());
    assertEquals(ZhangBoCaTaPhase.EARLY, ZhangBoCaTaPhase.fromProgress(0, 100));
    assertEquals(ZhangBoCaTaPhase.MIDDLE, ZhangBoCaTaPhase.fromProgress(33, 100));
    assertEquals(ZhangBoCaTaPhase.LATE, ZhangBoCaTaPhase.fromProgress(67, 100));
  }

  @Test
  public void allOneHundredFortyFourContextsHaveAStableInitialTestDecision() {
    ZhangBoCaTaController controller = new ZhangBoCaTaController(
        ZhangBoCaTaConfiguration.standard());
    int contexts = 0;
    for (ZhangBoSubSwarm role : ZhangBoSubSwarmSemantics.roles()) {
      for (ZhangBoCaTaPhase phase : ZhangBoCaTaPhase.values()) {
        for (boolean stagnated : new boolean[] {false, true}) {
          for (ZhangBoBottleneck bottleneck : ZhangBoBottleneck.values()) {
            ZhangBoCaTaContext value = new ZhangBoCaTaContext(
                role, phase, stagnated, bottleneck);
            ZhangBoCaTaController.Decision decision = controller.decide(value,
                Arrays.asList(ZhangBoNeighborhoodId.O2_JS_REVERSE,
                    ZhangBoNeighborhoodId.O1_JS_INSERT), new FixedRandom(0.99));
            assertTrue(decision.isTestPhase());
            assertEquals(ZhangBoNeighborhoodId.O1_JS_INSERT,
                decision.getNeighborhoods().get(0));
            contexts++;
          }
        }
      }
    }
    assertEquals(144, contexts);
  }

  @Test
  public void fixedEventsReplayTheCompleteStateMachineOneHundredTimes() {
    String expected = null;
    for (int repeat = 0; repeat < 100; repeat++) {
      ZhangBoCaTaController controller = new ZhangBoCaTaController(
          ZhangBoCaTaConfiguration.standard());
      ZhangBoCaTaContext c = context();
      List<ZhangBoNeighborhoodId> valid = Arrays.asList(
          ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER,
          ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW);
      StringBuilder trace = new StringBuilder();
      append(trace, controller.decide(c, valid, new FixedRandom(0.99)));
      controller.record(c, valid.get(0), true, 0.5, 1000L, 1L);
      controller.record(c, valid.get(1), false, -0.5, 2000L, 1L);
      append(trace, controller.decide(c, valid, new FixedRandom(0.99)));
      controller.record(c, valid.get(0), true, 0.25, 1500L, 1L);
      append(trace, controller.decide(c, valid, new FixedRandom(0.99)));
      controller.record(c, valid.get(0), false, -0.25, 1500L, 1L);
      append(trace, controller.decide(c, valid, new FixedRandom(0.99)));
      trace.append(controller.getStatistics().toCanonicalText());
      if (expected == null) expected = trace.toString();
      else assertEquals(expected, trace.toString());
    }
  }

  private static void append(StringBuilder trace, ZhangBoCaTaController.Decision value) {
    trace.append(value.isTestPhase()).append('|')
        .append(value.getNeighborhoods()).append('|')
        .append(value.getRepetitions()).append('|')
        .append(value.getContextEpoch()).append('|')
        .append(value.getCallOrdinal()).append('|')
        .append(value.getRemainingApplyCalls()).append('|')
        .append(value.getReason()).append('\n');
  }
}
