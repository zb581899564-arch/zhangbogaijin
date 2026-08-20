package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import static org.junit.Assert.*;

/**
 * V35-P13 pins: the 24-context mask system, the mask-change epoch reset and
 * the apply-horizon re-test trigger of the v3.5 Test-and-Apply controller.
 */
public class V35CaTaLiteControllerTest {

  @Test
  public void allTwentyFourContextsHaveLegalMasksAndDistinctStates() {
    V35CaTaLiteController controller =
        new V35CaTaLiteController(V35CaTaLiteConfiguration.standard());
    Set<String> keys = new HashSet<>();
    for (V35SubSwarmRole role : V35SubSwarmRole.values()) {
      for (V35Bottleneck bottleneck : V35Bottleneck.values()) {
        V35CaTaContext context = new V35CaTaContext(role, bottleneck);
        List<V35MacroNeighborhood> mask = mask(context);
        assertFalse("mask must not be empty: " + role + "|" + bottleneck, mask.isEmpty());
        // Fresh state: first decision is a TEST spanning the whole mask.
        V35CaTaLiteController.Decision test = controller.decide(
            context, mask, new ScriptedRandom(0.99));
        assertTrue(test.isTest());
        assertEquals(mask, test.getActions());
        // One observation per action completes the test quota (nTest=1).
        for (V35MacroNeighborhood action : mask) {
          controller.record(context, action, true, 1.0, 1);
        }
        V35CaTaLiteController.Decision apply = controller.decide(
            context, mask, new ScriptedRandom(0.99));
        assertFalse("apply after complete test: " + role + "|" + bottleneck, apply.isTest());
        assertTrue(mask.contains(apply.getActions().get(0)));
        keys.add(role + "|" + bottleneck);
      }
    }
    assertEquals(24, keys.size());
  }

  @Test
  public void maskChangeStartsAFreshTestEpoch() {
    V35CaTaLiteController controller =
        new V35CaTaLiteController(V35CaTaLiteConfiguration.standard());
    V35CaTaContext context = new V35CaTaContext(V35SubSwarmRole.G1_CMAX, V35Bottleneck.SEQ);
    List<V35MacroNeighborhood> full = mask(context);
    controller.decide(context, full, new ScriptedRandom(0.99));
    for (V35MacroNeighborhood action : full) {
      controller.record(context, action, true, 1.0, 1);
    }
    assertFalse(controller.decide(context, full, new ScriptedRandom(0.99)).isTest());

    // N3 becomes inapplicable: the mask signature changes for the same state,
    // so the controller must open a fresh test epoch instead of applying.
    List<V35MacroNeighborhood> shrunk = new ArrayList<>();
    shrunk.add(V35MacroNeighborhood.N1);
    V35CaTaLiteController.Decision decision = controller.decide(
        context, shrunk, new ScriptedRandom(0.99));
    assertTrue(decision.isTest());
    assertEquals(shrunk, decision.getActions());
  }

  @Test
  public void applyHorizonRetestTriggersFreshTestEpoch() {
    V35CaTaLiteController controller =
        new V35CaTaLiteController(V35CaTaLiteConfiguration.standard());
    V35CaTaContext context = new V35CaTaContext(V35SubSwarmRole.G2_TEC, V35Bottleneck.MAC);
    List<V35MacroNeighborhood> mask = mask(context);
    controller.decide(context, mask, new ScriptedRandom(0.99));
    for (V35MacroNeighborhood action : mask) {
      controller.record(context, action, true, 1.0, 1);
    }
    // remainingApply = mask.size() * nTest * applyMultiplier = 2.
    assertFalse(controller.decide(context, mask, new ScriptedRandom(0.99)).isTest());
    assertFalse(controller.decide(context, mask, new ScriptedRandom(0.99)).isTest());
    V35CaTaLiteController.Decision retest = controller.decide(
        context, mask, new ScriptedRandom(0.99));
    assertTrue(retest.isTest());
    assertEquals("APPLY_HORIZON_COMPLETE_TEST", retest.getReason());
  }

  private static List<V35MacroNeighborhood> mask(V35CaTaContext context) {
    List<V35MacroNeighborhood> result = new ArrayList<>();
    for (V35MacroNeighborhood action : V35MacroNeighborhood.values()) {
      if (context.allows(action)) result.add(action);
    }
    return result;
  }

  private static final class ScriptedRandom implements PseudoRandomGenerator {
    private final double value;
    ScriptedRandom(double value) { this.value = value; }
    @Override public int nextInt(int lowerBound, int upperBound) { return lowerBound; }
    @Override public double nextDouble(double lowerBound, double upperBound) {
      return lowerBound + value * (upperBound - lowerBound);
    }
    @Override public double nextDouble() { return value; }
    @Override public long getSeed() { return 0L; }
    @Override public void setSeed(long seed) { }
    @Override public String getName() { return "ScriptedRandom"; }
  }
}
