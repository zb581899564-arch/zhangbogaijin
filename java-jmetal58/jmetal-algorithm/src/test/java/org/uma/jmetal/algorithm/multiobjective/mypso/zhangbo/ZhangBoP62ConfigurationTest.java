package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoP62ConfigurationTest {
  @Test
  public void legacyCfvfFactoryKeepsNewModesDisabled() {
    ZhangBoGlobalSearchConfiguration configuration =
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 20260808L);
    assertFalse(configuration.isEvaluatedPddrEnabled());
    assertFalse(configuration.isLineageArchiveEnabled());
    assertFalse(configuration.isQpEnabled());
    assertFalse(configuration.toCanonicalText().contains("environmentalSelectionMode"));
    assertFalse(configuration.toCanonicalText().contains("personalArchive."));
  }

  @Test
  public void evaluatedPddrAndArchiveFactoriesAreSeparated() {
    ZhangBoGlobalSearchConfiguration pddr =
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvfEvaluatedPddr(0.4, 20260808L);
    ZhangBoGlobalSearchConfiguration archive =
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvfAndLineageArchive(0.4, 20260808L);
    assertTrue(pddr.isEvaluatedPddrEnabled());
    assertFalse(pddr.isLineageArchiveEnabled());
    assertTrue(archive.isEvaluatedPddrEnabled());
    assertTrue(archive.isLineageArchiveEnabled());
    assertFalse(archive.isQpEnabled());
    assertTrue(archive.toCanonicalText().contains("personalArchive.capacity=6"));
    assertTrue(archive.toCanonicalText().contains("personalArchive.indicatorKappa=0.05"));
  }

  @Test
  public void qpFactoryAddsOnlyTheExplicitP63Configuration() {
    ZhangBoGlobalSearchConfiguration configuration =
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveAndQp(
            0.4, 20260808L);
    assertTrue(configuration.isQpEnabled());
    assertTrue(configuration.isLineageArchiveEnabled());
    assertTrue(configuration.isEvaluatedPddrEnabled());
    assertTrue(configuration.toCanonicalText().contains("qp.qualityTolerance=0.15"));
    assertTrue(configuration.toCanonicalText().contains("qp.rewardWeights=2.0,1.0,0.5,0.25"));
    assertFalse(configuration.isBlockFrozenDualQEnabled());
    assertFalse(configuration.toCanonicalText().contains("dualQ."));
  }

  @Test
  public void p64FactoryAddsBlockFrozenCoordinationWithoutChangingP63Factory() {
    ZhangBoGlobalSearchConfiguration configuration =
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveQpBlockFrozen(
            0.4, 20260808L);
    assertTrue(configuration.isBlockFrozenDualQEnabled());
    assertEquals(0.10,
        configuration.getDualQCoordinationConfiguration().getWarmupRatio(), 0.0);
    assertEquals(5, configuration.getDualQCoordinationConfiguration().getBlockLength());
    assertTrue(configuration.toCanonicalText().contains("dualQ.mode=BLOCK_FROZEN"));
    assertTrue(configuration.toCanonicalText().contains("dualQ.blockLength=5"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void archiveCannotRunOnAuthorPddrTiming() {
    new ZhangBoGlobalSearchConfiguration(
        ZhangBoGlobalSearchConfiguration.GlobalLeaderMode.ORIGINAL_QG,
        ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.CFVF,
        20260808L, 0.8, 1.0, 0.8, 0.4, 0.4, 0.5, 0.05,
        ZhangBoGlobalSearchConfiguration.EnvironmentalSelectionMode.AUTHOR_PDDR_ACTIVE,
        ZhangBoPersonalArchiveConfiguration.standard());
  }

  @Test(expected = IllegalArgumentException.class)
  public void qpCannotRunWithoutTheLineageArchivePrerequisites() {
    new ZhangBoGlobalSearchConfiguration(
        ZhangBoGlobalSearchConfiguration.GlobalLeaderMode.ORIGINAL_QG,
        ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.CFVF,
        20260808L, 0.8, 1.0, 0.8, 0.4, 0.4, 0.5, 0.05,
        ZhangBoGlobalSearchConfiguration.EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.disabled(),
        ZhangBoQpConfiguration.standard());
  }

  @Test(expected = IllegalArgumentException.class)
  public void blockFrozenCoordinationCannotRunWithoutQp() {
    new ZhangBoGlobalSearchConfiguration(
        ZhangBoGlobalSearchConfiguration.GlobalLeaderMode.ORIGINAL_QG,
        ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.CFVF,
        20260808L, 0.8, 1.0, 0.8, 0.4, 0.4, 0.5, 0.05,
        ZhangBoGlobalSearchConfiguration.EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.standard(),
        ZhangBoQpConfiguration.disabled(),
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
  }
}
