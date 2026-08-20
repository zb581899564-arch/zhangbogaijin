package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DscrTeacherCache;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialKnowledgeSnapshot;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialTeacher;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmRole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoQgControllerTest {
  @Test
  public void statesRewardsAndQUpdateFollowTheFrozenRules() {
    List<PermutationSolution<Integer>> before = Arrays.<PermutationSolution<Integer>>asList(
        solution(10, 20, 30), solution(20, 40, 60));
    List<PermutationSolution<Integer>> after = Arrays.<PermutationSolution<Integer>>asList(
        solution(8, 18, 27), solution(18, 36, 54));
    assertEquals(2.0, ZhangBoQgController.reward(ZhangBoSubSwarm.G1_CMAX, before, after), 0.0);
    assertEquals(1.0 / 3.0,
        ZhangBoQgController.reward(ZhangBoSubSwarm.G4_BALANCED, before, after), 1.0e-12);
    assertEquals(0, ZhangBoQgController.stateFor(0.0));
    assertEquals(1, ZhangBoQgController.stateFor(-1.0e-12));

    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {0.0}, new int[] {0, 1}), 1.0, 1.0, 0.8);
    ZhangBoQgController.Selection selection = controller.select(ZhangBoSubSwarm.G1_CMAX, before);
    assertEquals(0, selection.getAction());
    assertEquals(2.0, controller.settle(selection, before, after), 0.0);
    assertEquals(2.0, controller.getTable(ZhangBoSubSwarm.G1_CMAX)[0][0], 0.0);
  }

  @Test
  public void allActionsCanBeExploredAndTieBreakIsStable() {
    List<PermutationSolution<Integer>> candidates = Arrays.<PermutationSolution<Integer>>asList(
        solution(10, 20, 30), solution(11, 19, 31));
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {1.0, 1.0, 1.0},
            new int[] {0, 1, 0, 1, 2, 0, 1}), 0.0, 1.0, 0.8);
    assertEquals(0, controller.select(ZhangBoSubSwarm.G1_CMAX, candidates).getAction());
    assertEquals(1, controller.select(ZhangBoSubSwarm.G1_CMAX, candidates).getAction());
    assertEquals(2, controller.select(ZhangBoSubSwarm.G1_CMAX, candidates).getAction());
  }

  @Test
  public void fixedSeedReplaysCanonicalEventLog() {
    assertEquals(run(20260808L), run(20260808L));
    assertTrue(!run(20260808L).equals(run(20260809L)));
  }

  @Test
  public void frozenGreedySelectionRefreshesStateWithoutChangingQTable() {
    List<PermutationSolution<Integer>> before = Arrays.<PermutationSolution<Integer>>asList(
        solution(10, 20, 30), solution(20, 40, 60));
    List<PermutationSolution<Integer>> after = Arrays.<PermutationSolution<Integer>>asList(
        solution(12, 20, 30), solution(22, 40, 60));
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[0], new int[] {0, 1}), 0.8, 1.0, 0.8);
    String beforeHash = controller.tableHash();
    ZhangBoQgController.Selection selection = controller.selectGreedy(
        ZhangBoSubSwarm.G1_CMAX, before);
    assertEquals(0, selection.getAction());
    assertEquals(-2.0, controller.observeWithoutUpdate(selection, before, after), 0.0);
    assertEquals(beforeHash, controller.tableHash());
    assertEquals(1L, controller.getSelectionCount());
    assertEquals(0L, controller.getTdUpdateCount());
    assertEquals(1L, controller.getFrozenObservationCount());
    assertTrue(controller.getEvents().toString().contains("greedyFrozen"));
  }

  @Test
  public void canonicalFingerprintIncludesTheExplicitMachineVector() {
    DhhfspFourVectorSolution left = new DhhfspFourVectorSolution(
        Arrays.asList(1, 0), Arrays.asList(0, 0), Arrays.asList(0, 1),
        Arrays.asList(0, 0), "deterministic_canonical", 7);
    DhhfspFourVectorSolution right = new DhhfspFourVectorSolution(
        Arrays.asList(1, 0), Arrays.asList(0, 0), Arrays.asList(1, 0),
        Arrays.asList(0, 0), "deterministic_canonical", 7);
    assertTrue(!ZhangBoQgController.fingerprint(left)
        .equals(ZhangBoQgController.fingerprint(right)));
    assertTrue(ZhangBoQgController.fingerprint(left).contains("|[0, 1]|"));
  }

  @Test
  public void dscrSanitizesTheActualPreviousAndHistoricalCaches() {
    ZhangBoTestPermutationSolution stale = solution(10, 10, 10);
    ZhangBoTestPermutationSolution dominator = solution(5, 5, 5);
    dominator.setVariableValue(0, 1);
    dominator.setVariableValue(1, 0);
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {0.0}, new int[] {0, 0}),
        1.0, 1.0, 0.8);
    controller.select(ZhangBoSubSwarm.G1_CMAX,
        Collections.<PermutationSolution<Integer>>singletonList(stale));
    V35SocialKnowledgeSnapshot snapshot = V35SocialKnowledgeSnapshot
        .fromEvaluatedSolutions(Collections.<PermutationSolution<Integer>>singletonList(dominator));
    V35DscrTeacherCache ledger = new V35DscrTeacherCache();

    controller.sanitizeTeacherCaches(ZhangBoSubSwarm.G1_CMAX, snapshot, ledger,
        1L, 1L, 100L);

    String expected = ZhangBoQgController.fingerprint(dominator);
    assertEquals(expected, ZhangBoQgController.fingerprint(controller.cachedTeacher(
        ZhangBoSubSwarm.G1_CMAX, V35DscrTeacherCache.CacheType.PREVIOUS)));
    assertEquals(expected, ZhangBoQgController.fingerprint(controller.cachedTeacher(
        ZhangBoSubSwarm.G1_CMAX, V35DscrTeacherCache.CacheType.HISTORICAL)));
    assertEquals(2L, ledger.getValidityChecks());
    assertEquals(2L, ledger.getReplacements());
  }

  @Test
  public void sanitizeThenSelectNeverInstallsADominatedTeacher() {
    // V35-P12 gate pin: after DSCR sanitization, a select over the filtered
    // (nondominated) candidate list must never install a dominated leader,
    // and the post-select caches must stay nondominated (no post-action override).
    ZhangBoTestPermutationSolution stale = solution(10, 10, 10);
    ZhangBoTestPermutationSolution dominator = solution(5, 5, 5);
    dominator.setVariableValue(0, 1);
    dominator.setVariableValue(1, 0);
    ZhangBoTestPermutationSolution incomparable = solution(6, 4, 8);
    incomparable.setVariableValue(0, 0);
    incomparable.setVariableValue(1, 1);
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {0.0, 1.0}, new int[] {0, 0, 2, 1, 0}),
        1.0, 1.0, 0.8);
    controller.select(ZhangBoSubSwarm.G1_CMAX,
        Collections.<PermutationSolution<Integer>>singletonList(stale));
    List<PermutationSolution<Integer>> filtered = Arrays.<PermutationSolution<Integer>>asList(
        dominator, incomparable);
    V35SocialKnowledgeSnapshot snapshot = V35SocialKnowledgeSnapshot
        .fromEvaluatedSolutions(filtered);
    V35DscrTeacherCache ledger = new V35DscrTeacherCache();

    controller.sanitizeTeacherCaches(ZhangBoSubSwarm.G1_CMAX, snapshot, ledger,
        1L, 1L, 100L);
    // Second select: draw 1.0 is not < epsilon 1.0 -> explore path; scripted
    // arm 2 -> tournament draw [1,0] inside the filtered list.
    ZhangBoQgController.Selection selection = controller.select(ZhangBoSubSwarm.G1_CMAX, filtered);

    assertEquals(0, snapshot.strictDominatorCount(objectives(selection.getLeader())));
    assertEquals(0, snapshot.strictDominatorCount(objectives(controller.cachedTeacher(
        ZhangBoSubSwarm.G1_CMAX, V35DscrTeacherCache.CacheType.PREVIOUS))));
    assertEquals(0, snapshot.strictDominatorCount(objectives(controller.cachedTeacher(
        ZhangBoSubSwarm.G1_CMAX, V35DscrTeacherCache.CacheType.HISTORICAL))));
    // Negative control: the production gate detector flags the dominated teacher,
    // so any hypothetical override would trip the hard throw in selectQgLeader.
    V35DscrTeacherCache.TeacherUse use = ledger.recordTeacherUse(1L, 1L, 200L,
        V35SubSwarmRole.G1_CMAX, new V35SocialTeacher(objectives(stale),
            ZhangBoQgController.fingerprint(stale)), snapshot);
    assertTrue(use.isDominated());
    assertEquals(1L, ledger.getDominatedTeacherUses());
  }

  @Test(expected = IllegalArgumentException.class)
  public void dscrRefreshInvariantRejectsNondominatedReplacement() {
    // V35-P12 gate pin: the ledger refuses a replacement of a teacher that the
    // snapshot does not strictly dominate (belt-and-braces behind the sanitizer).
    V35SocialTeacher before = new V35SocialTeacher(new double[] {10, 10, 10}, "before");
    V35SocialTeacher after = new V35SocialTeacher(new double[] {9, 11, 11}, "after");
    ZhangBoTestPermutationSolution incomparable = solution(9, 11, 11);
    V35SocialKnowledgeSnapshot snapshot = V35SocialKnowledgeSnapshot
        .fromEvaluatedSolutions(Collections.<PermutationSolution<Integer>>singletonList(incomparable));
    V35DscrTeacherCache ledger = new V35DscrTeacherCache();
    ledger.recordRefresh(1L, 1L, 100L, V35SubSwarmRole.G1_CMAX,
        V35DscrTeacherCache.CacheType.PREVIOUS, before, after, snapshot, 0L);
  }

  private static double[] objectives(PermutationSolution<Integer> solution) {
    return new double[] {solution.getObjective(0), solution.getObjective(1),
        solution.getObjective(6)};
  }

  @Test
  public void directionalTopKPoolDrawsActionTwoFromBoundaryBest() {
    List<PermutationSolution<Integer>> candidates = Arrays.<PermutationSolution<Integer>>asList(
        solution(50, 150, 250), solution(10, 110, 210), solution(40, 140, 240),
        solution(20, 120, 220), solution(30, 130, 230), solution(60, 160, 260));
    // epsilon=0.0 forces the explore path, so the scripted action int drives the arm.
    // ints: [0,0] initialize, [0] arm, [2] arm, [2,0] action-2 draw inside the top-3 pool.
    // Index 2 in a full 6-candidate list would be Cmax 40; the pool turns it into Cmax 10.
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {1.0, 1.0}, new int[] {0, 0, 0, 2, 2, 0}),
        0.0, 1.0, 0.8);
    controller.setDirectionalTeacherPool(true, 3);
    controller.select(ZhangBoSubSwarm.G1_CMAX, candidates);
    ZhangBoQgController.Selection selection = controller.select(ZhangBoSubSwarm.G1_CMAX, candidates);
    assertEquals(2, selection.getAction());
    assertEquals(10.0, selection.getLeader().getObjective(0), 0.0);
    assertTrue(controller.getEvents().toString().contains("tournament"));
    assertTrue(controller.getDirectionalPoolRequestCount() > 0L);
    assertTrue(controller.getDirectionalPoolFilteredCount() > 0L);
  }

  @Test
  public void directionalPoolHonoursCandidateShortageAndUsesWholeList() {
    List<PermutationSolution<Integer>> candidates = Arrays.<PermutationSolution<Integer>>asList(
        solution(11, 101, 211), solution(7, 107, 207), solution(9, 109, 209));
    // k=5 with 3 candidates: the whole list stays the pool, so draw index 2 is legal.
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {1.0, 1.0}, new int[] {0, 0, 0, 2, 2, 0}),
        0.0, 1.0, 0.8);
    controller.setDirectionalTeacherPool(true, 5);
    controller.select(ZhangBoSubSwarm.G2_TEC, candidates);
    ZhangBoQgController.Selection selection = controller.select(ZhangBoSubSwarm.G2_TEC, candidates);
    assertEquals(2, selection.getAction());
    assertEquals(101.0, selection.getLeader().getObjective(1), 0.0);
    assertTrue(controller.getDirectionalPoolRequestCount() > 0L);
    assertEquals(0L, controller.getDirectionalPoolFilteredCount());
  }

  @Test
  public void balancedGroupIgnoresDirectionalPool() {
    List<PermutationSolution<Integer>> candidates = Arrays.<PermutationSolution<Integer>>asList(
        solution(50, 150, 250), solution(10, 110, 210), solution(40, 140, 240),
        solution(20, 120, 220), solution(30, 130, 230), solution(60, 160, 260));
    // k=2 would shrink the draw range to [0,1]; scripted index 3 is only legal
    // on the untouched full list, so this proves the pool never applies to G4.
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {1.0, 1.0}, new int[] {0, 0, 0, 2, 0, 3}),
        0.0, 1.0, 0.8);
    controller.setDirectionalTeacherPool(true, 2);
    controller.select(ZhangBoSubSwarm.G4_BALANCED, candidates);
    ZhangBoQgController.Selection selection = controller.select(ZhangBoSubSwarm.G4_BALANCED, candidates);
    assertEquals(2, selection.getAction());
    assertTrue(controller.getEvents().toString().contains("right=3"));
    assertEquals(0L, controller.getDirectionalPoolRequestCount());
    assertEquals(0L, controller.getDirectionalPoolFilteredCount());
  }

  @Test
  public void disabledPoolReplaysBitIdenticalToOversizedPool() {
    List<PermutationSolution<Integer>> candidates = Arrays.<PermutationSolution<Integer>>asList(
        solution(9, 109, 209), solution(10, 110, 210), solution(11, 111, 211),
        solution(12, 112, 212), solution(13, 113, 213), solution(14, 114, 214));
    assertEquals(selectWithPool(candidates, false, 2), selectWithPool(candidates, true, 100));
  }

  private static String selectWithPool(List<PermutationSolution<Integer>> candidates,
      boolean enabled, int k) {
    ZhangBoQgController controller = new ZhangBoQgController(
        new ZhangBoScriptedRandom(new double[] {1.0, 1.0}, new int[] {1, 4, 0, 2, 0, 3}),
        0.0, 1.0, 0.8);
    if (enabled) controller.setDirectionalTeacherPool(true, k);
    controller.select(ZhangBoSubSwarm.G1_CMAX, candidates);
    controller.select(ZhangBoSubSwarm.G1_CMAX, candidates);
    return controller.toCanonicalText();
  }

  private static String run(long seed) {
    List<PermutationSolution<Integer>> values = Arrays.<PermutationSolution<Integer>>asList(
        solution(9, 19, 29), solution(10, 18, 31), solution(11, 21, 27));
    ZhangBoQgController controller = new ZhangBoQgController(
        new JavaRandomGenerator(seed), 0.8, 1.0, 0.8);
    for (ZhangBoSubSwarm group : ZhangBoSubSwarm.values()) {
      ZhangBoQgController.Selection selection = controller.select(group, values);
      controller.settle(selection, values, values);
    }
    return controller.toCanonicalText();
  }

  private static ZhangBoTestPermutationSolution solution(double cmax, double tec, double twc) {
    return new ZhangBoTestPermutationSolution(new int[] {0, 1}, new int[] {0, 0},
        new int[] {0, 0}, new int[] {0, 0}, new int[0], cmax, tec, twc);
  }
}
