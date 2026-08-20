package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZhangBoDualQCoordinatorTest {
  @Test
  public void twoThousandFeScheduleIncludesInitialPopulationAndRoundsWarmupByGeneration() {
    ZhangBoDualQCoordinator coordinator = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
    assertDecision(coordinator, 100, ZhangBoDualQCoordinator.Phase.WARMUP, 200, -1, -1);
    assertDecision(coordinator, 200, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 0);
    assertDecision(coordinator, 600, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 4);
    assertDecision(coordinator, 700, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 1, 0);
    assertDecision(coordinator, 1100, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 1, 4);
    assertDecision(coordinator, 1200, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 2, 0);
    assertDecision(coordinator, 1700, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 3, 0);
    assertDecision(coordinator, 1900, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 3, 2);
  }

  @Test
  public void nonIntegralWarmupThresholdRoundsUpAndSmallThresholdNeedsNoExtraWarmupGeneration() {
    ZhangBoDualQCoordinator rounded = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 3));
    assertDecision(rounded, 100, ZhangBoDualQCoordinator.Phase.WARMUP, 300, -1, -1, 2001);
    assertDecision(rounded, 300, ZhangBoDualQCoordinator.Phase.P_BLOCK, 300, 0, 0, 2001);

    ZhangBoDualQCoordinator belowInitial = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen(0.01, 1));
    assertDecision(belowInitial, 100, ZhangBoDualQCoordinator.Phase.P_BLOCK, 100, 0, 0);
  }

  @Test
  public void synchronousAndSupportedBlockLengthsAreConstructible() {
    ZhangBoDualQCoordinator synchronous = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.synchronous());
    assertEquals(ZhangBoDualQCoordinator.Phase.SYNCHRONOUS,
        synchronous.decide(100, 2000, 100).getPhase());
    for (int block : new int[] {1, 3, 5, 10}) {
      assertEquals(block, ZhangBoDualQCoordinationConfiguration
          .blockFrozen(0.10, block).getBlockLength());
    }
  }

  @Test
  public void localSearchFeDoesNotAdvancePostWarmupBlockOffset() {
    ZhangBoDualQCoordinator coordinator = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
    // Warmup is still FE-aligned.  There is one completed outer generation,
    // but local candidates have already consumed an extra 200 FE.
    assertDecision(coordinator, 100, 0,
        ZhangBoDualQCoordinator.Phase.WARMUP, 200, -1, -1);
    assertDecision(coordinator, 400, 1,
        ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 0);
    assertDecision(coordinator, 600, 2,
        ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 1);
    assertDecision(coordinator, 1000, 4,
        ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 3);
    assertDecision(coordinator, 1200, 5,
        ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 4);
    assertDecision(coordinator, 1400, 6,
        ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 1, 0);
  }

  @Test
  public void observedWarmupBoundaryAnchorsBlocksWhenLocalFeEndsWarmupEarly() {
    ZhangBoDualQCoordinator coordinator = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
    // For 5000 FE and population 10 the theoretical no-local-search warmup
    // would last 49 outer generations.  Here local FE crosses 500 at the end
    // of outer generation 10, so blocks must be anchored at that observation.
    ZhangBoDualQCoordinator.Decision first = coordinator.decide(
        552, 5000, 10, 10, 10);
    assertEquals(ZhangBoDualQCoordinator.Phase.P_BLOCK, first.getPhase());
    assertEquals(0L, first.getPostWarmupGeneration());
    assertEquals(0, first.getBlockOffset());
    ZhangBoDualQCoordinator.Decision fifth = coordinator.decide(
        2800, 5000, 10, 15, 10);
    assertEquals(ZhangBoDualQCoordinator.Phase.G_BLOCK, fifth.getPhase());
    assertEquals(5L, fifth.getPostWarmupGeneration());
    assertEquals(0, fifth.getBlockOffset());
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroBlockLengthIsRejected() {
    ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroGBlockLengthIsRejected() {
    ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 5, 0);
  }

  @Test
  public void unequalPAndGBlockLengthsScheduleLongerQgLearningPhase() {
    // P blocks of 2 generations alternate with G blocks of 4 generations:
    //   postWarmup 0-1 -> P(0), 2-5 -> G(1), 6-7 -> P(2), 8-11 -> G(3).
    ZhangBoDualQCoordinator coordinator = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 2, 4));
    assertDecision(coordinator, 100, ZhangBoDualQCoordinator.Phase.WARMUP, 200, -1, -1);
    assertDecision(coordinator, 200, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 0);
    assertDecision(coordinator, 300, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 0, 1);
    assertDecision(coordinator, 400, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 1, 0);
    assertDecision(coordinator, 700, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 1, 3);
    assertDecision(coordinator, 800, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 2, 0);
    assertDecision(coordinator, 900, ZhangBoDualQCoordinator.Phase.P_BLOCK, 200, 2, 1);
    assertDecision(coordinator, 1200, ZhangBoDualQCoordinator.Phase.G_BLOCK, 200, 3, 2);
  }

  @Test
  public void equalBlockLengthsReproduceHistoricalScheduleExactly() {
    ZhangBoDualQCoordinator equal = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 5, 5));
    ZhangBoDualQCoordinator historical = new ZhangBoDualQCoordinator(
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
    for (long before : new long[] {200, 600, 700, 1100, 1200, 1700, 1900}) {
      ZhangBoDualQCoordinator.Decision decision = equal.decide(before, 2000, 100);
      ZhangBoDualQCoordinator.Decision expected = historical.decide(before, 2000, 100);
      assertEquals(expected.getPhase(), decision.getPhase());
      assertEquals(expected.getBlockIndex(), decision.getBlockIndex());
      assertEquals(expected.getBlockOffset(), decision.getBlockOffset());
    }
  }

  private static void assertDecision(
      ZhangBoDualQCoordinator coordinator, long before,
      ZhangBoDualQCoordinator.Phase phase, long warmupEnd,
      long block, int offset) {
    assertDecision(coordinator, before, phase, warmupEnd, block, offset, 2000);
  }

  private static void assertDecision(
      ZhangBoDualQCoordinator coordinator, long before,
      ZhangBoDualQCoordinator.Phase phase, long warmupEnd,
      long block, int offset, long maximum) {
    ZhangBoDualQCoordinator.Decision decision = coordinator.decide(before, maximum, 100);
    assertEquals(phase, decision.getPhase());
    assertEquals(warmupEnd, decision.getWarmupEndEvaluations());
    assertEquals(block, decision.getBlockIndex());
    assertEquals(offset, decision.getBlockOffset());
  }

  private static void assertDecision(
      ZhangBoDualQCoordinator coordinator, long before, long completedOuterGenerations,
      ZhangBoDualQCoordinator.Phase phase, long warmupEnd, long block, int offset) {
    ZhangBoDualQCoordinator.Decision decision = coordinator.decide(
        before, 2000, 100, completedOuterGenerations);
    assertEquals(phase, decision.getPhase());
    assertEquals(warmupEnd, decision.getWarmupEndEvaluations());
    assertEquals(block, decision.getBlockIndex());
    assertEquals(offset, decision.getBlockOffset());
  }
}
