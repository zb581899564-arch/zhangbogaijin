package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;

/** Configuration-only guardrails for the A0--A4 Final ablation ladder. */
public class V35FinalAblationProfileTest {
  @Test
  public void admitsOnlyTheFiveLegalInnovationLevelArms() {
    assertEquals(5, V35FinalAblationProfile.ARMS.size());
    for (V35FinalAblationProfile.Arm arm : V35FinalAblationProfile.ARMS) {
      V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
          arm, 20260822L, 10, 2000);
      V35FinalAblationProfile.validate(arm, configuration);
      assertTrue(configuration.isQgEnabled());
      assertFalse(configuration.isDirectionalTeacherPoolEnabled());
      assertEquals(PddrSelectionMode.GLOBAL_ORIGINAL, configuration.getPddrSelectionMode());
      assertEquals(V35LocalSearchOrder.CATA_THEN_INHERITED, configuration.getLocalSearchOrder());
      if (arm.isQpEnabled()) {
        assertTrue(configuration.getDualQCoordination().isBlockFrozen());
        assertEquals(0.0, configuration.getDualQCoordination().getSoftFreezeRho(), 0.0);
      } else {
        assertTrue(configuration.getDualQCoordination() == null);
      }
      assertTrue(V35FinalAblationProfile.canonicalTextFor(arm, 20260822L, 10, 2000)
          .contains("shiftMode=NONE"));
    }
  }

  @Test
  public void recordsTheA4BundleInsteadOfPretendingItIsACataOnlyContrast() {
    assertEquals(Arrays.asList("dscr"), V35FinalAblationProfile.differingHighLevelFields(
        V35FinalAblationProfile.Arm.A0_BASELINE, V35FinalAblationProfile.Arm.A1_DSCR));
    assertEquals(Arrays.asList("cfvf"), V35FinalAblationProfile.differingHighLevelFields(
        V35FinalAblationProfile.Arm.A1_DSCR, V35FinalAblationProfile.Arm.A2_CFVF));
    assertEquals(Arrays.asList("PA_i+qp"), V35FinalAblationProfile.differingHighLevelFields(
        V35FinalAblationProfile.Arm.A2_CFVF,
        V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE));
    assertEquals(Arrays.asList("caTaLite", "localFeBudget"),
        V35FinalAblationProfile.differingHighLevelFields(
            V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE,
            V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA));
    assertTrue(V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, 20260822L, 10, 2000)
        .getLocalFeBudget() != null);
    assertTrue(V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE, 20260822L, 10, 2000)
        .getLocalFeBudget() == null);
  }
}
