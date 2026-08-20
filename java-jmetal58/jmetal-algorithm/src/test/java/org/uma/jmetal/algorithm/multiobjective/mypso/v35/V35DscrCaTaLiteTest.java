package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class V35DscrCaTaLiteTest {
  @Test
  public void dscrKeepsCachedTeacherWithoutKnownStrictDominator() {
    V35SocialTeacher cached = new V35SocialTeacher(new double[] {5, 5, 5}, "cached");
    V35SocialTeacher incomparable = new V35SocialTeacher(new double[] {4, 6, 5}, "other");
    V35SocialTeacher actual = V35DscrSanitizer.sanitize(V35SubSwarmRole.G1_CMAX, cached,
        new V35SocialKnowledgeSnapshot(Arrays.asList(incomparable)));
    assertSame(cached, actual);
  }

  @Test
  public void dscrReplacesOnlyWithStrictDominatorAndStableTie() {
    V35SocialTeacher cached = new V35SocialTeacher(new double[] {5, 5, 5}, "cached");
    V35SocialTeacher b = new V35SocialTeacher(new double[] {4, 4, 5}, "b");
    V35SocialTeacher a = new V35SocialTeacher(new double[] {4, 4, 5}, "a");
    V35SocialTeacher actual = V35DscrSanitizer.sanitize(V35SubSwarmRole.G1_CMAX, cached,
        new V35SocialKnowledgeSnapshot(Arrays.asList(b, a)));
    assertEquals("a", actual.getFingerprint());
  }

  @Test
  public void dscrSelectsStrictDominatorBySubswarmDirection() {
    V35SocialTeacher cached = new V35SocialTeacher(new double[] {10, 10, 10}, "cached");
    V35SocialTeacher cmax = new V35SocialTeacher(new double[] {4, 9, 9}, "cmax");
    V35SocialTeacher tec = new V35SocialTeacher(new double[] {9, 4, 9}, "tec");
    V35SocialTeacher twc = new V35SocialTeacher(new double[] {9, 9, 4}, "twc");
    V35SocialTeacher balanced = new V35SocialTeacher(new double[] {6, 6, 6}, "balanced");
    V35SocialKnowledgeSnapshot snapshot = new V35SocialKnowledgeSnapshot(
        Arrays.asList(cmax, tec, twc, balanced));
    assertEquals("cmax", V35DscrSanitizer.sanitize(
        V35SubSwarmRole.G1_CMAX, cached, snapshot).getFingerprint());
    assertEquals("tec", V35DscrSanitizer.sanitize(
        V35SubSwarmRole.G2_TEC, cached, snapshot).getFingerprint());
    assertEquals("twc", V35DscrSanitizer.sanitize(
        V35SubSwarmRole.G3_TWC, cached, snapshot).getFingerprint());
    assertEquals("balanced", V35DscrSanitizer.sanitize(
        V35SubSwarmRole.G4_BALANCED, cached, snapshot).getFingerprint());
  }

  @Test
  public void cataLiteHasTwentyFourContextsAndV35Masks() {
    V35CaTaLiteConfiguration configuration = V35CaTaLiteConfiguration.standard();
    assertEquals(24, configuration.contextCount());
    assertEquals(1, configuration.getNTest());
    assertEquals(3, configuration.getStagnationThreshold());
    assertTrue(new V35CaTaContext(V35SubSwarmRole.G1_CMAX, V35Bottleneck.SEQ)
        .allows(V35MacroNeighborhood.N1));
    assertFalse(new V35CaTaContext(V35SubSwarmRole.G4_BALANCED, V35Bottleneck.SEQ)
        .allows(V35MacroNeighborhood.N5));
    assertTrue(new V35CaTaContext(V35SubSwarmRole.G2_TEC, V35Bottleneck.FAT)
        .allows(V35MacroNeighborhood.N5));
  }

  @Test
  public void cataLiteTestCoversAllValidActionsBeforeApply() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.standard());
    V35CaTaContext context = new V35CaTaContext(V35SubSwarmRole.G1_CMAX, V35Bottleneck.SEQ);
    org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator random =
        new org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator(20260808L);
    V35CaTaLiteController.Decision decision = controller.decide(
        context, Arrays.asList(V35MacroNeighborhood.values()), random);
    assertTrue(decision.isTest());
    assertEquals(2, decision.getActions().size());
    controller.record(context, decision.getActions().get(0), true, 0.2, 1, true);
    controller.record(context, decision.getActions().get(1), false, -0.1, 1, true);
    V35CaTaLiteController.Decision apply = controller.decide(
        context, Arrays.asList(V35MacroNeighborhood.values()), random);
    assertFalse(apply.isTest());
    assertEquals(1, apply.getActions().size());
  }

  @Test
  public void cataLiteRetestUsesConsecutiveApplyFailuresAndStartsAFreshEpoch() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.standard());
    V35CaTaContext context = new V35CaTaContext(
        V35SubSwarmRole.G4_BALANCED, V35Bottleneck.BAL);
    org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator random =
        new org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator(20260808L);
    List<V35MacroNeighborhood> actions = Arrays.asList(V35MacroNeighborhood.values());

    V35CaTaLiteController.Decision test = controller.decide(context, actions, random);
    for (V35MacroNeighborhood action : test.getActions()) {
      controller.record(context, action, action == V35MacroNeighborhood.N1,
          action == V35MacroNeighborhood.N1 ? 1.0 : 0.0, 1, true);
    }
    for (int i = 0; i < 3; i++) {
      V35CaTaLiteController.Decision apply = controller.decide(context, actions, random);
      assertFalse(apply.isTest());
      controller.record(context, apply.getActions().get(0), false, -0.1, 1, false);
    }

    V35CaTaLiteController.Decision retest = controller.decide(context, actions, random);
    assertTrue(retest.isTest());
    assertEquals("CONSECUTIVE_APPLY_FAILURE_RETEST", retest.getReason());
    for (V35MacroNeighborhood action : retest.getActions()) {
      controller.record(context, action, false, -0.1, 1, true);
    }
    V35CaTaLiteController.Decision resumedApply = controller.decide(context, actions, random);
    assertFalse("a completed retest must not become a permanent Test loop",
        resumedApply.isTest());
  }

  @Test
  public void cataLiteUsesLowerNormalizedAverageCostAfterSuccessAndGainTie() {
    V35CaTaLiteController controller = new V35CaTaLiteController(
        V35CaTaLiteConfiguration.standard());
    V35CaTaContext context = new V35CaTaContext(
        V35SubSwarmRole.G4_BALANCED, V35Bottleneck.BAL);
    org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator random =
        new org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator(20260808L);
    List<V35MacroNeighborhood> actions = Arrays.asList(V35MacroNeighborhood.values());
    V35CaTaLiteController.Decision test = controller.decide(context, actions, random);
    for (V35MacroNeighborhood action : test.getActions()) {
      long elapsed = action == V35MacroNeighborhood.N3 ? 10L : 100L;
      controller.record(context, action, false, 0.0, 1,
          action == V35MacroNeighborhood.N3 ? 10L : 100L, elapsed, true);
    }
    V35CaTaLiteController.Decision apply = controller.decide(context, actions, random);
    assertFalse(apply.isTest());
    assertEquals(V35MacroNeighborhood.N3, apply.getActions().get(0));
  }

  @Test
  public void wallClockDoesNotChangeDeterministicActionChoice() {
    V35CaTaContext context = new V35CaTaContext(
        V35SubSwarmRole.G4_BALANCED, V35Bottleneck.BAL);
    List<V35MacroNeighborhood> actions = Arrays.asList(V35MacroNeighborhood.values());
    V35CaTaLiteController fast = new V35CaTaLiteController(V35CaTaLiteConfiguration.standard());
    V35CaTaLiteController slow = new V35CaTaLiteController(V35CaTaLiteConfiguration.standard());
    org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator randomFast =
        new org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator(20260808L);
    org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator randomSlow =
        new org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator(20260808L);
    V35CaTaLiteController.Decision fastTest = fast.decide(context, actions, randomFast);
    V35CaTaLiteController.Decision slowTest = slow.decide(context, actions, randomSlow);
    for (V35MacroNeighborhood action : fastTest.getActions()) {
      fast.record(context, action, true, 0.0, 1, 10L, 1L, true);
      slow.record(context, action, true, 0.0, 1, 10L, 1000000000L, true);
    }
    assertEquals(fast.decide(context, actions, randomFast).getActions(),
        slow.decide(context, actions, randomSlow).getActions());
  }
}
