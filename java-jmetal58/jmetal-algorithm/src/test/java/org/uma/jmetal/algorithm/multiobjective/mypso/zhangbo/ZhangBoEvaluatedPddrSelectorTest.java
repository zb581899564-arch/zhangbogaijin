package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoEvaluatedPddrSelectorTest {
  @Test
  public void selectsEvaluatedCandidatesAndKeepsTheirHistoryMapping() {
    ZhangBoTestPermutationSolution offspring = solution(1, 2.0, 8.0, 8.0);
    ZhangBoTestPermutationSolution parent = solution(2, 8.0, 2.0, 2.0);
    ZhangBoTestPermutationSolution dominated = solution(3, 9.0, 9.0, 9.0);
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected =
        new ZhangBoEvaluatedPddrSelector().select(
            Arrays.<PermutationSolution<Integer>>asList(offspring, dominated),
            Arrays.asList(history(11.0), history(33.0)),
            Collections.<PermutationSolution<Integer>>singletonList(parent),
            Collections.singletonList(history(22.0)), 2, 101L);

    assertEquals(2, selected.size());
    assertEquals(ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        selected.get(0).getSource());
    assertEquals(11.0, selected.get(0).getAuthorHistory().get(0).getObjective(0), 0.0);
    assertTrue(selected.get(0).getPddrScore() <= selected.get(1).getPddrScore());
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsAnUnevaluatedCandidate() {
    ZhangBoTestPermutationSolution invalid = solution(1, Double.NaN, 1.0, 1.0);
    new ZhangBoEvaluatedPddrSelector().select(
        Collections.<PermutationSolution<Integer>>singletonList(invalid),
        Collections.singletonList(history(1.0)), Collections.emptyList(),
        Collections.emptyList(), 1, 1L);
  }

  @Test
  public void stableTieKeepsGlobalOffspringBeforeParent() {
    ZhangBoTestPermutationSolution offspring = solution(1, 1.0, 1.0, 1.0);
    ZhangBoTestPermutationSolution parent = solution(2, 1.0, 1.0, 1.0);
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected =
        new ZhangBoEvaluatedPddrSelector().select(
            Collections.<PermutationSolution<Integer>>singletonList(offspring),
            Collections.singletonList(history(1.0)),
            Collections.<PermutationSolution<Integer>>singletonList(parent),
            Collections.singletonList(history(2.0)), 2, 5L);
    assertEquals(2, selected.size());
    assertEquals(ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        selected.get(0).getSource());
    // Equal objective vectors are mutually non-dominating under strict Pareto dominance.
    assertEquals(1.0, selected.get(0).getPddrScore(), 0.0);
    assertEquals(1.0, selected.get(1).getPddrScore(), 0.0);
    assertTrue(!ZhangBoEvaluatedPddrSelector.strictlyDominates(offspring, parent));
    assertTrue(!ZhangBoEvaluatedPddrSelector.strictlyDominates(parent, offspring));
  }

  @Test
  public void preservesIntraFactoryLocalSourceAndParentSlot() {
    ZhangBoTestPermutationSolution local = solution(1, 1.0, 1.0, 1.0);
    List<ZhangBoEvaluatedPddrSelector.CandidateInput> inputs =
        Collections.singletonList(ZhangBoEvaluatedPddrSelector.CandidateInput.ofEvaluated(
            local, history(7.0), ZhangBoEvaluatedPddrSelector.Source.INTRA_FACTORY_VNS,
            37, 201L, 0));
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected =
        new ZhangBoEvaluatedPddrSelector().select(inputs,
            Collections.<PermutationSolution<Integer>>emptyList(),
            Collections.<List<PermutationSolution<Integer>>>emptyList(), 1);
    assertEquals(ZhangBoEvaluatedPddrSelector.Source.INTRA_FACTORY_VNS,
        selected.get(0).getSource());
    assertEquals(37, selected.get(0).getSourceSlot());
    assertEquals(201L, selected.get(0).getEvaluationOrdinal());
    assertEquals(7.0, selected.get(0).getAuthorHistory().get(0).getObjective(0), 0.0);
  }

  private static List<PermutationSolution<Integer>> history(double cmax) {
    return Collections.<PermutationSolution<Integer>>singletonList(solution(9, cmax, cmax, cmax));
  }

  private static ZhangBoTestPermutationSolution solution(
      int variant, double cmax, double tec, double twc) {
    return new ZhangBoTestPermutationSolution(new int[]{0, 1},
        new int[]{variant % 2, (variant / 2) % 2},
        new int[]{variant % 3, (variant + 1) % 3},
        new int[]{variant % 2, (variant + 1) % 2}, new int[0], cmax, tec, twc)
        .withFatigue(0.2, 0.1);
  }
}
