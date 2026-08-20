package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoQpControllerTest {
  private static final ZhangBoArchiveBounds BOUNDS = ZhangBoArchiveBounds.of(
      new double[]{0.0, 0.0, 0.0}, new double[]{10.0, 10.0, 10.0},
      0.0, 1.0, 0.0, 10.0, 1.0e-12);

  @Test
  public void standardConfigurationUsesTheUserApprovedP63Values() {
    ZhangBoQpConfiguration configuration = ZhangBoQpConfiguration.standard();
    assertTrue(configuration.isEnabled());
    assertEquals(0.30, configuration.getAlpha(), 0.0);
    assertEquals(0.80, configuration.getGamma(), 0.0);
    assertEquals(0.30, configuration.getEpsilonStart(), 0.0);
    assertEquals(0.05, configuration.getEpsilonEnd(), 0.0);
    assertEquals(0.15, configuration.getQualityTolerance(), 0.0);
    assertEquals(2.0, configuration.getDominanceWeight(), 0.0);
    assertEquals(1.0, configuration.getDirectionWeight(), 0.0);
    assertEquals(0.5, configuration.getArchiveWeight(), 0.0);
    assertEquals(0.25, configuration.getFatigueWeight(), 0.0);
  }

  @Test
  public void allSixteenStatesAreUniqueAndFollowTheDeclaredEncoding() {
    boolean[] seen = new boolean[16];
    for (int evolution = 0; evolution < 4; evolution++) {
      for (int stagnation = 0; stagnation < 2; stagnation++) {
        for (int redundancy = 0; redundancy < 2; redundancy++) {
          int state = ZhangBoQpController.stateIndex(evolution, stagnation, redundancy);
          assertFalse(seen[state]);
          seen[state] = true;
          assertEquals(4 * (2 * stagnation + redundancy) + evolution, state);
        }
      }
    }
    for (boolean value : seen) assertTrue(value);
  }

  @Test
  public void oneEntryArchiveMasksEveryActionExceptKeep() {
    ZhangBoQpCandidateSelector selector = selector();
    ZhangBoArchiveEntry only = entry(1, 3.0, 4.0, 5.0, 0.4, 4.0);
    ZhangBoQpCandidateSelector.Candidates candidates = selector.build(
        Collections.singletonList(only), "missing", ZhangBoSubSwarm.G1_CMAX,
        only, entry(2, 2.0, 3.0, 4.0, 0.3, 3.0), BOUNDS);
    assertTrue(Arrays.equals(new boolean[]{true, false, false, false}, candidates.getMask()));
    assertEquals(only.getFingerprint(), candidates.get(ZhangBoQpAction.KEEP).getFingerprint());
  }

  @Test
  public void duplicateCandidatesKeepOnlyTheLowestNumberedAction() {
    ZhangBoQpCandidateSelector selector = selector();
    ZhangBoArchiveEntry best = entry(1, 1.0, 9.0, 9.0, 0.4, 4.0);
    ZhangBoArchiveEntry other = entry(2, 4.0, 4.0, 4.0, 0.3, 3.0);
    ZhangBoQpCandidateSelector.Candidates candidates = selector.build(
        Arrays.asList(best, other), best.getFingerprint(), ZhangBoSubSwarm.G1_CMAX,
        other, entry(3, 0.5, 8.0, 8.0, 0.2, 2.0), BOUNDS);
    assertTrue(candidates.isValid(ZhangBoQpAction.KEEP));
    assertFalse(candidates.isValid(ZhangBoQpAction.DIRECTIONAL));
  }

  @Test
  public void complementaryUsesThresholdQualitySetInsteadOfTopFraction() {
    ZhangBoQpCandidateSelector selector = selector();
    ZhangBoArchiveEntry current = entry(0, 5.0, 5.0, 5.0, 0.5, 5.0);
    ZhangBoArchiveEntry sameDirection = entry(1, 2.0, 5.0, 5.0, 0.4, 4.0);
    ZhangBoArchiveEntry complementary = entry(2, 3.0, 8.0, 5.0, 0.3, 3.0);
    ZhangBoArchiveEntry tooWeak = entry(3, 8.0, 1.0, 1.0, 0.2, 2.0);
    ZhangBoQpCandidateSelector.Candidates candidates = selector.build(
        Arrays.asList(sameDirection, complementary, tooWeak),
        sameDirection.getFingerprint(), ZhangBoSubSwarm.G1_CMAX, current,
        entry(4, 1.0, 5.0, 5.0, 0.2, 2.0), BOUNDS);
    assertTrue(candidates.get(ZhangBoQpAction.COMPLEMENTARY) != null);
    assertEquals(complementary.getFingerprint(),
        candidates.get(ZhangBoQpAction.COMPLEMENTARY).getFingerprint());
    assertNotEquals(tooWeak.getFingerprint(),
        candidates.get(ZhangBoQpAction.COMPLEMENTARY).getFingerprint());
  }

  @Test
  public void epsilonActionUsesTheMinimumAdditiveIndicatorFitness() {
    ZhangBoQpCandidateSelector selector = selector();
    List<ZhangBoArchiveEntry> entries = Arrays.asList(
        entry(1, 1.0, 9.0, 9.0, 0.4, 4.0),
        entry(2, 4.0, 4.0, 4.0, 0.3, 3.0),
        entry(3, 9.0, 1.0, 1.0, 0.2, 2.0));
    Map<String, Double> fitness = new ZhangBoPersonalArchive(
        ZhangBoPersonalArchiveConfiguration.standard()).epsilonFitnessValues(entries, BOUNDS);
    ZhangBoArchiveEntry expected = entries.get(0);
    for (ZhangBoArchiveEntry candidate : entries) {
      int comparison = Double.compare(fitness.get(candidate.getFingerprint()),
          fitness.get(expected.getFingerprint()));
      if (comparison < 0 || (comparison == 0 && candidate.getFingerprint()
          .compareTo(expected.getFingerprint()) < 0)) expected = candidate;
    }
    ZhangBoQpCandidateSelector.Candidates candidates = selector.build(entries,
        entries.get(0).getFingerprint(), ZhangBoSubSwarm.G4_BALANCED,
        entry(0, 5.0, 5.0, 5.0, 0.5, 5.0),
        entry(4, 2.0, 2.0, 2.0, 0.2, 2.0), BOUNDS);
    assertEquals(expected.getFingerprint(),
        candidates.get(ZhangBoQpAction.EPSILON).getFingerprint());
  }

  @Test
  public void rewardUsesDominanceDirectionArchiveAndAuxiliaryFatigueWeights() {
    ZhangBoQpController controller = controller();
    ZhangBoArchiveEntry parent = entry(1, 5.0, 5.0, 5.0, 0.8, 8.0);
    ZhangBoArchiveEntry child = entry(2, 4.0, 4.0, 4.0, 0.4, 4.0);
    ZhangBoQpController.Reward reward = controller.rewardForTest(parent, child,
        ZhangBoSubSwarm.G1_CMAX, true, BOUNDS);
    assertEquals(1.0, reward.getDominance(), 1.0e-12);
    assertEquals(0.2, reward.getDirection(), 1.0e-12);
    assertEquals(1.0, reward.getArchive(), 1.0e-12);
    assertEquals(0.4, reward.getFatigue(), 1.0e-12);
    assertEquals(2.8, reward.getTotal(), 1.0e-12);
  }

  @Test
  public void batchUpdateIsIndependentOfTransitionTraversalOrder() {
    boolean[] keep = new boolean[]{true, false, false, false};
    List<ZhangBoQpController.Transition> transitions = new ArrayList<>();
    transitions.add(new ZhangBoQpController.Transition(20L, 3, 0, 1.0, 3, keep));
    transitions.add(new ZhangBoQpController.Transition(10L, 3, 0, 3.0, 3, keep));
    ZhangBoQpController reference = controller();
    reference.batchUpdateForTest(ZhangBoSubSwarm.G1_CMAX, transitions);
    for (int repetition = 0; repetition < 100; repetition++) {
      List<ZhangBoQpController.Transition> shuffled = new ArrayList<>(transitions);
      Collections.shuffle(shuffled, new Random(20260808L + repetition));
      ZhangBoQpController candidate = controller();
      candidate.batchUpdateForTest(ZhangBoSubSwarm.G1_CMAX, shuffled);
      assertArrayEquals(reference.getTable(ZhangBoSubSwarm.G1_CMAX)[3],
          candidate.getTable(ZhangBoSubSwarm.G1_CMAX)[3], 0.0);
    }
    assertEquals(0.6, reference.getTable(ZhangBoSubSwarm.G1_CMAX)[3][0], 1.0e-12);
  }

  @Test
  public void eachSubgroupOwnsAnIndependentQpTable() {
    ZhangBoQpController controller = controller();
    controller.setTableValueForTest(ZhangBoSubSwarm.G1_CMAX, 3, 2, 7.0);
    assertEquals(7.0, controller.getTable(ZhangBoSubSwarm.G1_CMAX)[3][2], 0.0);
    assertEquals(0.0, controller.getTable(ZhangBoSubSwarm.G4_BALANCED)[3][2], 0.0);
    assertEquals(0.0, controller.getTable(ZhangBoSubSwarm.G2_TEC)[3][2], 0.0);
    assertEquals(0.0, controller.getTable(ZhangBoSubSwarm.G3_TWC)[3][2], 0.0);
  }

  @Test
  public void actionSelectionHonorsMasksGreedyTiesAndInjectedExploration() {
    ZhangBoQpController greedy = new ZhangBoQpController(
        ZhangBoQpConfiguration.standard(), ZhangBoPersonalArchiveConfiguration.standard(),
        new ZhangBoScriptedRandom(new double[]{0.9}, new int[0]), 123L);
    assertEquals(ZhangBoQpAction.DIRECTIONAL.ordinal(), greedy.selectActionForTest(
        new double[]{0.0, 2.0, 2.0, 99.0},
        new boolean[]{true, true, true, false}, 0.3, 1L, ZhangBoSubSwarm.G1_CMAX));

    ZhangBoQpController exploratory = new ZhangBoQpController(
        ZhangBoQpConfiguration.standard(), ZhangBoPersonalArchiveConfiguration.standard(),
        new ZhangBoScriptedRandom(new double[]{0.1}, new int[]{2}), 123L);
    assertEquals(ZhangBoQpAction.COMPLEMENTARY.ordinal(), exploratory.selectActionForTest(
        new double[]{9.0, 8.0, 7.0, 6.0},
        new boolean[]{true, false, true, true}, 0.3, 1L, ZhangBoSubSwarm.G1_CMAX));
  }

  @Test
  public void explorationRateIsLinearInConsumedEvaluationsAndClamped() {
    ZhangBoQpController controller = controller();
    assertEquals(0.30, controller.explorationProbabilityForTest(0, 2000), 0.0);
    assertEquals(0.175, controller.explorationProbabilityForTest(1000, 2000), 1.0e-12);
    assertEquals(0.05, controller.explorationProbabilityForTest(2000, 2000), 0.0);
    assertEquals(0.05, controller.explorationProbabilityForTest(3000, 2000), 0.0);
  }

  @Test
  public void frozenGreedySelectionHonorsMaskAndStableActionTieBreakWithoutRandomDraw() {
    ZhangBoQpController controller = new ZhangBoQpController(
        ZhangBoQpConfiguration.standard(), ZhangBoPersonalArchiveConfiguration.standard(),
        new ZhangBoScriptedRandom(new double[0], new int[0]), 20260808L);
    assertEquals(ZhangBoQpAction.DIRECTIONAL.ordinal(),
        controller.selectGreedyActionForTest(
            new double[] {9.0, 5.0, 5.0, 8.0},
            new boolean[] {false, true, true, false}));
  }

  @Test
  public void nextStateMaximumReadsOnlyActionsAllowedByItsMask() {
    ZhangBoQpController controller = controller();
    controller.setTableValueForTest(ZhangBoSubSwarm.G1_CMAX, 4, 0, 1.0);
    controller.setTableValueForTest(ZhangBoSubSwarm.G1_CMAX, 4, 1, 100.0);
    controller.batchUpdateForTest(ZhangBoSubSwarm.G1_CMAX,
        Collections.singletonList(new ZhangBoQpController.Transition(
            1L, 3, 0, 0.0, 4, new boolean[]{true, false, false, false})));
    assertEquals(0.24, controller.getTable(ZhangBoSubSwarm.G1_CMAX)[3][0], 1.0e-12);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void archiveLeaderRestorationDoesNotMutateTemplateOrLaterWorkerBlock() {
    ZhangBoTestPermutationSolution template = new ZhangBoTestPermutationSolution(
        new int[]{0, 1}, new int[]{0, 0}, new int[]{0, 0},
        new int[]{0, 0}, new int[]{7, 8}, 9.0, 9.0, 9.0);
    String before = template.vectors();
    ZhangBoArchiveEntry entry = new ZhangBoArchiveEntry(
        new int[]{1, 0}, new int[]{1, 0}, new int[]{2, 1},
        new int[]{1, 0, 7, 8}, new double[]{4.0, 5.0, 6.0}, 0.4, 4.0,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 1, 1L);
    ZhangBoTestPermutationSolution restored =
        (ZhangBoTestPermutationSolution) entry.toSolution(template);
    assertEquals(before, template.vectors());
    assertEquals(Integer.valueOf(1), restored.getVariableValue(0));
    assertEquals(Integer.valueOf(1), restored.getVariableValueid(0));
    assertEquals(Integer.valueOf(2), ((List<Integer>) restored.getAttribute("machine")).get(0));
    assertEquals(Integer.valueOf(1), restored.getVariableValueworker(0));
    assertEquals(Integer.valueOf(7), restored.getVariableValueworker(2));
    assertEquals(Integer.valueOf(8), restored.getVariableValueworker(3));
    assertEquals(4.0, restored.getObjective(0), 0.0);
    assertEquals(5.0, restored.getObjective(1), 0.0);
    assertEquals(6.0, restored.getObjective(6), 0.0);
  }

  @Test
  public void groupNeedCoversAllFourEvolutionCases() {
    ZhangBoQpController.GroupStats previous = new ZhangBoQpController.GroupStats(1.0, 1.0);
    assertEquals(0, ZhangBoQpController.evolutionNeed(previous,
        new ZhangBoQpController.GroupStats(0.8, 1.0), 1.0e-4, 1.0e-4));
    assertEquals(1, ZhangBoQpController.evolutionNeed(previous,
        new ZhangBoQpController.GroupStats(0.8, 0.8), 1.0e-4, 1.0e-4));
    assertEquals(2, ZhangBoQpController.evolutionNeed(previous,
        new ZhangBoQpController.GroupStats(1.0, 1.2), 1.0e-4, 1.0e-4));
    assertEquals(3, ZhangBoQpController.evolutionNeed(previous,
        new ZhangBoQpController.GroupStats(1.0, 1.0), 1.0e-4, 1.0e-4));
  }

  private static ZhangBoQpCandidateSelector selector() {
    return new ZhangBoQpCandidateSelector(ZhangBoQpConfiguration.standard(),
        ZhangBoPersonalArchiveConfiguration.standard());
  }

  private static ZhangBoQpController controller() {
    return new ZhangBoQpController(ZhangBoQpConfiguration.standard(),
        ZhangBoPersonalArchiveConfiguration.standard(),
        new ZhangBoScriptedRandom(new double[0], new int[0]), 123L);
  }

  private static ZhangBoArchiveEntry entry(
      int variant, double cmax, double tec, double twc, double fmax, double fe) {
    return new ZhangBoArchiveEntry(new int[]{variant, 100 + variant},
        new int[]{variant % 2, (variant + 1) % 2},
        new int[]{variant % 3, (variant + 1) % 3},
        new int[]{variant % 2, (variant + 1) % 2},
        new double[]{cmax, tec, twc}, fmax, fe,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 1, variant + 1L);
  }
}
