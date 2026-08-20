package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ZhangBoLineageCoordinatorTest {
  @Test
  public void singleSurvivorKeepsIdAndSubgroupMigrationKeepsArchive() {
    ZhangBoLineageCoordinator coordinator = coordinator();
    ZhangBoTestPermutationSolution initial = solution(0, 5.0, 5.0, 5.0,
        ZhangBoSubSwarm.G1_CMAX);
    coordinator.initialize(Collections.<PermutationSolution<Integer>>singletonList(initial),
        Collections.<PermutationSolution<Integer>>emptyList(), 0);
    coordinator.freezeBounds(Collections.<PermutationSolution<Integer>>singletonList(initial),
        Collections.<PermutationSolution<Integer>>emptyList());
    ZhangBoTestPermutationSolution child = (ZhangBoTestPermutationSolution) initial.copy();
    child.setAttribute(ZhangBoSubSwarm.class, ZhangBoSubSwarm.G2_TEC);
    child.setObjective(0, 4.0);
    List<ZhangBoLineageCoordinator.Branch> branches = coordinator.rebuild(
        Collections.singletonList(candidate(child,
            ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 0, 2L)), 1);

    ZhangBoLineageTag tag = (ZhangBoLineageTag) branches.get(0).getSolution()
        .getAttribute(ZhangBoLineageTag.class);
    assertEquals(0L, tag.getLineageId());
    assertEquals(1L, coordinator.getMigrations());
    assertTrue(coordinator.getMemories().get(0L).getEntries().size() >= 1);
  }

  @Test
  public void multipleSurvivorsSplitAndMissingLineageIsDeleted() {
    ZhangBoLineageCoordinator coordinator = coordinator();
    ZhangBoTestPermutationSolution first = solution(0, 5.0, 5.0, 5.0,
        ZhangBoSubSwarm.G1_CMAX);
    ZhangBoTestPermutationSolution second = solution(1, 6.0, 4.0, 5.0,
        ZhangBoSubSwarm.G2_TEC);
    List<PermutationSolution<Integer>> initial = Arrays.<PermutationSolution<Integer>>asList(first, second);
    coordinator.initialize(initial, Collections.<PermutationSolution<Integer>>emptyList(), 0);
    coordinator.freezeBounds(initial, Collections.<PermutationSolution<Integer>>emptyList());

    ZhangBoTestPermutationSolution global = (ZhangBoTestPermutationSolution) first.copy();
    global.setObjective(0, 4.5);
    ZhangBoTestPermutationSolution local = (ZhangBoTestPermutationSolution) first.copy();
    local.getVariables().set(0, 1);
    local.getVariables().set(1, 0);
    local.setObjective(1, 4.5);
    List<ZhangBoLineageCoordinator.Branch> branches = coordinator.rebuild(Arrays.asList(
        candidate(global, ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 0, 3L),
        candidate(local, ZhangBoEvaluatedPddrSelector.Source.INTRA_FACTORY_VNS, 1, 4L)), 1);

    assertEquals(2, branches.size());
    Map<Long, ZhangBoLineageMemory> memories = coordinator.getMemories();
    assertFalse(memories.containsKey(0L));
    assertFalse(memories.containsKey(1L));
    assertTrue(memories.containsKey(2L));
    assertTrue(memories.containsKey(3L));
    assertEquals(1L, coordinator.getSplits());
    assertEquals(1L, coordinator.getDeletions());
    assertNotSame(memories.get(2L).getEntries(), memories.get(3L).getEntries());
  }

  @Test
  public void globalNormalizationReferenceNeverEntersPersonalArchive() {
    ZhangBoLineageCoordinator coordinator = coordinator();
    ZhangBoTestPermutationSolution initial = solution(0, 5.0, 5.0, 5.0,
        ZhangBoSubSwarm.G4_BALANCED);
    ZhangBoTestPermutationSolution external = solution(9, 1.0, 1.0, 1.0,
        ZhangBoSubSwarm.G4_BALANCED);
    coordinator.initialize(Collections.<PermutationSolution<Integer>>singletonList(initial),
        Collections.<PermutationSolution<Integer>>singletonList(external), 0);
    List<ZhangBoArchiveEntry> entries = coordinator.getMemories().get(0L).getEntries();
    assertEquals(1, entries.size());
    ZhangBoArchiveEntry externalEntry = ZhangBoArchiveEntry.fromSolution(external,
        ZhangBoEvaluatedPddrSelector.Source.PARENT, 0, 1L);
    assertFalse(entries.get(0).getFingerprint().equals(externalEntry.getFingerprint()));
  }

  private static ZhangBoLineageCoordinator coordinator() {
    return new ZhangBoLineageCoordinator(ZhangBoPersonalArchiveConfiguration.standard());
  }

  private static ZhangBoEvaluatedPddrSelector.Candidate candidate(
      ZhangBoTestPermutationSolution solution,
      ZhangBoEvaluatedPddrSelector.Source source, int order, long evaluation) {
    List<PermutationSolution<Integer>> history = new ArrayList<>();
    history.add((PermutationSolution<Integer>) solution.copy());
    return ZhangBoEvaluatedPddrSelector.Candidate.ofEvaluated(solution, history, source,
        order, evaluation, order, 0.0);
  }

  private static ZhangBoTestPermutationSolution solution(
      int variant, double cmax, double tec, double twc, ZhangBoSubSwarm group) {
    ZhangBoTestPermutationSolution solution = new ZhangBoTestPermutationSolution(
        new int[]{0, 1}, new int[]{variant % 2, (variant + 1) % 2},
        new int[]{variant % 2, (variant + 1) % 2},
        new int[]{variant % 2, (variant + 1) % 2}, new int[0], cmax, tec, twc)
        .withFatigue(0.2 + variant * 0.01, 0.1 + variant * 0.01);
    solution.setAttribute(ZhangBoSubSwarm.class, group);
    return solution;
  }
}
