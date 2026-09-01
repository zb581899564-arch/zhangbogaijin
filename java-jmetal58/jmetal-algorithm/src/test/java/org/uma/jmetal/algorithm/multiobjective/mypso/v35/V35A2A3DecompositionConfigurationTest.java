package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;

/** Contract tests for the non-production A2-to-A3 causal decomposition. */
public class V35A2A3DecompositionConfigurationTest {
  @Test
  public void directionalArchiveControlCreatesNoQpButMapsAnEnabledLineageArchive() {
    V35ProductionConfiguration config = common().qp(false)
        .personalLeaderMode(V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL).build();
    assertFalse(config.isQpEnabled());
    assertTrue(config.isLineageArchiveEnabled());
    assertEquals(V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL, config.getPersonalLeaderMode());
    assertTrue(config.canonicalText().contains("personalLeaderMode=ARCHIVE_DIRECTIONAL"));
    ZhangBoGlobalSearchConfiguration runtime = ZhangBoGlobalSearchConfiguration.forV35(config);
    assertTrue(runtime.isLineageArchiveEnabled());
    assertFalse(runtime.isQpEnabled());
  }

  @Test
  public void synchronousQpHasNoWarmupOrPBlockFreeze() {
    V35ProductionConfiguration config = common().qp(true)
        .personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
        .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous()).build();
    assertTrue(config.isQpEnabled());
    assertEquals(V35PersonalLeaderMode.QP_FOUR_ACTIONS, config.getPersonalLeaderMode());
    assertFalse(config.getDualQCoordination().isBlockFrozen());
    assertFalse(ZhangBoGlobalSearchConfiguration.forV35(config).isBlockFrozenDualQEnabled());
  }

  @Test
  public void observeOnlyQpKeepsFourActionSelectionButCannotEnterProductionCanonicalText() {
    V35ProductionConfiguration config = common().qp(true)
        .personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
        .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous())
        .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
    assertEquals(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES,
        config.getQpSettlementPolicy());
    assertTrue(config.canonicalText().contains(
        "qpSettlementPolicy=OBSERVE_ONLY_ALL_CYCLES"));
    assertEquals(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES,
        ZhangBoGlobalSearchConfiguration.forV35(config).getV35QpSettlementPolicy());
  }

  @Test
  public void diagnosticDirectionalTieIsExplicitAndDoesNotAlterTheDefaultQpConfiguration() {
    V35ProductionConfiguration config = common().qp(true)
        .personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
        .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous())
        .qpConfiguration(ZhangBoQpConfiguration.diagnosticDirectionalGreedyTie())
        .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
    assertEquals(ZhangBoQpConfiguration.GreedyTiePolicy.DIRECTIONAL_IF_TIED,
        ZhangBoGlobalSearchConfiguration.forV35(config).getQpConfiguration().getGreedyTiePolicy());
    assertTrue(config.canonicalText().contains("qp.greedyTiePolicy=DIRECTIONAL_IF_TIED"));
    assertFalse(ZhangBoQpConfiguration.standard().toCanonicalText().contains("qp.greedyTiePolicy"));
  }

  @Test
  public void frozenA3KeepsItsImplicitLegacyPersonalLeaderCanonicalText() {
    V35ProductionConfiguration a3 = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE, 20260822L, 100, 2000);
    assertEquals(V35PersonalLeaderMode.QP_FOUR_ACTIONS, a3.getPersonalLeaderMode());
    assertTrue(a3.isLineageArchiveEnabled());
    assertTrue(a3.getDualQCoordination().isBlockFrozen());
    assertFalse(a3.canonicalText().contains("personalLeaderMode="));
    assertEquals(PddrSelectionMode.GLOBAL_ORIGINAL, a3.getPddrSelectionMode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void directionalArchiveCannotSilentlyEnableQp() {
    common().qp(true).personalLeaderMode(V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void qpModeCannotSilentlyDisableQp() {
    common().qp(false).personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void observeOnlySettlementRejectsFrozenDualQ() {
    common().qp(true).personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
        .dualQCoordination(ZhangBoDualQCoordinationConfiguration.blockFrozen())
        .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
  }

  private static V35ProductionConfiguration.Builder common() {
    return V35ProductionConfiguration.builder().seed(20260822L).populationSize(100)
        .maxEvaluations(2000).qg(true).dscr(true).cfvf(true).caTaLite(false)
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED);
  }
}
