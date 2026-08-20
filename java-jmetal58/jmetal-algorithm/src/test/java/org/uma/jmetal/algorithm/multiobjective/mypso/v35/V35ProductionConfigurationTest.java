package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.FamilyMode;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.SetupMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;

public class V35ProductionConfigurationTest {
  @Test
  public void formalConfigurationIsShiftFreeAndSingleFamily() {
    V35ProductionConfiguration configuration = V35ProductionConfiguration.formal(20260808L);
    assertEquals(ZhangBoShiftMode.NONE, configuration.getShiftMode());
    assertEquals(FamilyMode.DEGENERATE_SINGLE_FAMILY, configuration.getFamilyMode());
    assertEquals(SetupMode.SEQUENCE_INDEPENDENT, configuration.getSetupMode());
    assertEquals(ProductionDecodeMode.FM3, configuration.getDecoderMode());
    assertTrue(configuration.canonicalText().contains("shiftMode=NONE"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void futureFamilyModeIsRejectedByFormalConfiguration() {
    V35ProductionConfiguration.builder()
        .familyMode(FamilyMode.SEQUENCE_DEPENDENT_FUTURE).build();
  }

  @Test
  public void fullConfigurationMapsToStructuredDualQAndCaTa() {
    ZhangBoGlobalSearchConfiguration runtime = ZhangBoGlobalSearchConfiguration.forV35(
        V35ProductionConfiguration.smoke(20260808L));
    assertTrue(runtime.isCfvfEnabled());
    assertTrue(runtime.isQgEnabled());
    assertTrue(runtime.isQpEnabled());
    assertTrue(runtime.isBlockFrozenDualQEnabled());
    assertTrue(runtime.isCaTaEnabled());
    assertTrue(runtime.isEvaluatedPddrEnabled());
    assertTrue(runtime.isDscrEnabled());
    assertTrue(runtime.isV35CaTaLiteEnabled());
    assertEquals(20260808L, runtime.getSeed());
  }

  @Test
  public void directionalTeacherPoolDefaultsToOff() {
    V35ProductionConfiguration configuration = V35ProductionConfiguration.smoke(1L);
    assertTrue(!configuration.isDirectionalTeacherPoolEnabled());
    assertEquals(10, configuration.getTeacherPoolSize());
  }

  @Test
  public void directionalTeacherPoolMapsThroughForV35() {
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .directionalTeacherPool(true).teacherPoolSize(7).build();
    assertTrue(configuration.canonicalText().contains("directionalTeacherPool=true"));
    assertTrue(configuration.canonicalText().contains("teacherPoolSize=7"));
    ZhangBoGlobalSearchConfiguration runtime = ZhangBoGlobalSearchConfiguration.forV35(configuration);
    assertTrue(runtime.isDirectionalTeacherPoolEnabled());
    assertEquals(7, runtime.getTeacherPoolSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void directionalPoolWithoutQgIsRejected() {
    V35ProductionConfiguration.builder().directionalTeacherPool(true).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void directionalPoolWithSingleCandidateKIsRejected() {
    V35ProductionConfiguration.builder().qg(true)
        .directionalTeacherPool(true).teacherPoolSize(1).build();
  }
}
