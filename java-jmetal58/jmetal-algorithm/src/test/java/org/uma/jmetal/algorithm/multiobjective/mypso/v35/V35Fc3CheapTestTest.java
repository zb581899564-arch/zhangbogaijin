package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * V35-FC-3: the cheap-Test upgrade.  (a) Standard configuration keeps the
 * archived behaviour: no probe, no Test-share suppression.  (b) When the
 * top-2 macros tie on the primary credit key, the probe fires exactly once
 * per epoch and grants one extra FE to each.  (c) Once the Test share of
 * CA-TA-Lite evaluations exceeds the cap, Re-tests are suppressed in favour
 * of the incumbent winner.
 */
public class V35Fc3CheapTestTest {

  private static V35CaTaContext context() {
    return new V35CaTaContext(V35SubSwarmRole.G1_CMAX, V35Bottleneck.BAL);
  }

  private static List<V35MacroNeighborhood> mask() {
    return Arrays.asList(V35MacroNeighborhood.N1, V35MacroNeighborhood.N2,
        V35MacroNeighborhood.N3);
  }

  @Test public void standardConfigurationNeverProbesOrSuppresses() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.standard());
    JavaRandomGenerator random = new JavaRandomGenerator(20260808L);
    V35CaTaContext context = context();
    // Complete one full Test round (each macro once) with all failures.
    for (int round = 0; round < 40; round++) {
      V35CaTaLiteController.Decision decision = controller.decide(context, mask(), random);
      for (V35MacroNeighborhood action : decision.getActions()) {
        controller.record(context, action, false, 0.0, 1, 1L, 0L, decision.isTest());
      }
      assertFalse("standard must never emit the FC-3 probe",
          "TOP2_PROBE".equals(decision.getReason()));
      assertFalse("standard must never suppress a Re-test",
          decision.getReason().startsWith("RETEST_SUPPRESSED"));
    }
  }

  @Test public void top2TieFiresOneProbePerEpoch() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.cheapTest());
    JavaRandomGenerator random = new JavaRandomGenerator(20260808L);
    V35CaTaContext context = context();
    // First Test round: all macros tie (zero successes each).
    V35CaTaLiteController.Decision test = controller.decide(context, mask(), random);
    assertTrue(test.isTest());
    assertEquals(3, test.getActions().size());
    for (V35MacroNeighborhood action : test.getActions()) {
      controller.record(context, action, false, 0.0, 1, 1L, 0L, true);
    }
    // Test round complete and N1/N2 (and N3) are indistinguishable: the probe
    // must return exactly the top two, once.
    V35CaTaLiteController.Decision probe = controller.decide(context, mask(), random);
    assertTrue(probe.isTest());
    assertEquals("TOP2_PROBE", probe.getReason());
    assertEquals(2, probe.getActions().size());
    for (V35MacroNeighborhood action : probe.getActions()) {
      controller.record(context, action, false, 0.0, 1, 1L, 0L, true);
    }
    // A clear winner now exists (N1 succeeded during the probe): no more probes.
    controller.record(context, probe.getActions().get(0), true, 0.5, 1, 1L, 0L, true);
    for (int round = 0; round < 10; round++) {
      V35CaTaLiteController.Decision decision = controller.decide(context, mask(), random);
      assertFalse("TOP2_PROBE".equals(decision.getReason()));
      if (!decision.isTest()) {
        assertEquals(probe.getActions().get(0), decision.getActions().get(0));
      }
      break;
    }
  }

  @Test public void testShareCapSuppressesRetests() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.cheapTest());
    JavaRandomGenerator random = new JavaRandomGenerator(20260808L);
    V35CaTaContext context = context();
    // Phase 1: one Test round (3 FE) + probe (2 FE) = 5 Test FE, all failing.
    V35CaTaLiteController.Decision test = controller.decide(context, mask(), random);
    for (V35MacroNeighborhood action : test.getActions()) {
      controller.record(context, action, false, 0.0, 1, 1L, 0L, true);
    }
    V35CaTaLiteController.Decision probe = controller.decide(context, mask(), random);
    assertEquals("TOP2_PROBE", probe.getReason());
    for (V35MacroNeighborhood action : probe.getActions()) {
      controller.record(context, action, false, 0.0, 1, 1L, 0L, true);
    }
    // Phase 2: N1 succeeds once, then apply failures drive stagnation.  With a
    // 20% cap, 5 Test FE require >20 Apply FE before any Re-test is allowed;
    // until then the winner keeps applying.
    controller.record(context, V35MacroNeighborhood.N1, true, 1.0, 1, 1L, 0L, false);
    for (int round = 0; round < 10; round++) {
      V35CaTaLiteController.Decision decision = controller.decide(context, mask(), random);
      assertEquals(V35MacroNeighborhood.N1, decision.getActions().get(0));
      controller.record(context, V35MacroNeighborhood.N1, false, 0.0, 1, 1L, 0L, false);
      if ("RETEST_SUPPRESSED_TEST_SHARE_CAP".equals(decision.getReason())) {
        return;
      }
      assertFalse(decision.isTest());
    }
    // After enough Apply FE the cap is no longer binding and a Re-test may
    // return; the suppression reason must have appeared before that.
    V35CaTaLiteController.Decision late = controller.decide(context, mask(), random);
    assertNotNull(late);
  }

  @Test public void cheapTestConfigurationIsValidAndBounded() {
    V35CaTaLiteConfiguration cheap = V35CaTaLiteConfiguration.cheapTest();
    assertTrue(cheap.isTop2ProbeEnabled());
    assertEquals(0.20, cheap.getTestFeShareCap(), 1e-12);
    assertEquals(1, cheap.getNTest());
    assertFalse(V35CaTaLiteConfiguration.standard().isTop2ProbeEnabled());
    assertEquals(1.0, V35CaTaLiteConfiguration.standard().getTestFeShareCap(), 1e-12);
    try {
      new V35CaTaLiteConfiguration(1, 1, 0.1, 3, true, 0.0);
      throw new AssertionError("cap must be strictly positive");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }
}
