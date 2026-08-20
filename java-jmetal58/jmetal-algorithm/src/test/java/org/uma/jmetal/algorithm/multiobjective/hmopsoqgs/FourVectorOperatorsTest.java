package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4OperatorFixtures;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FourVectorOperatorsTest {
  @Test
  public void fig5ExchangeSequenceAndPrefixMatchPaper() {
    Chapter4OperatorFixtures.Fig5Fixture fixture = Chapter4OperatorFixtures.load().getFig5();
    List<Integer> current = zero(fixture.getY());
    List<Integer> target = zero(fixture.getX());
    List<Chapter4OperatorFixtures.SwapPair> sequence =
        FourVectorOperators.exchangeSequence(current, target);
    assertEquals(Arrays.asList(
        new Chapter4OperatorFixtures.SwapPair(0, 1),
        new Chapter4OperatorFixtures.SwapPair(3, 4),
        new Chapter4OperatorFixtures.SwapPair(5, 7)), sequence);
    assertEquals(zero(fixture.getExpected()),
        FourVectorOperators.applyExchangePrefix(current, sequence, fixture.getR1()));
  }

  @Test
  public void allSixFig6CasesMatchPaperAndOnlyRepairInvalidPositions() {
    Chapter4OperatorFixtures fixture = Chapter4OperatorFixtures.load();
    DhhfspInstance instance = fig6Instance();
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.FA_CROSSOVER,
        new ScriptedRandomGenerator().ints(0, 0));
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.FA_MUTATION,
        new ScriptedRandomGenerator().ints(1, 1, 0));
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.MA_CROSSOVER,
        new ScriptedRandomGenerator().ints(1));
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.MA_MUTATION,
        new ScriptedRandomGenerator().ints(0));
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.WA_CROSSOVER,
        new ScriptedRandomGenerator().ints(0));
    assertCase(fixture, instance, Chapter4OperatorFixtures.Fig6CaseId.WA_MUTATION,
        new ScriptedRandomGenerator().ints(0));
  }

  private static void assertCase(
      Chapter4OperatorFixtures fixture, DhhfspInstance instance,
      Chapter4OperatorFixtures.Fig6CaseId id, ScriptedRandomGenerator random) {
    Chapter4OperatorFixtures.Fig6Case paper = fixture.getFig6Case(id);
    DhhfspFourVectorSolution child = base(fixture);
    DhhfspFourVectorSolution parent = base(fixture);
    List<Integer> parentValues = zero(paper.getParentVector());
    if (id == Chapter4OperatorFixtures.Fig6CaseId.FA_CROSSOVER) {
      replace(parent.getFactoryAssignments(), parentValues);
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.MA_CROSSOVER) {
      replace(parent.getMachineAssignments(), parentValues);
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.WA_CROSSOVER) {
      replace(parent.getWorkerAssignments(), parentValues);
    }
    FourVectorOperators operators = new FourVectorOperators(instance, random);
    List<Integer> selected = zero(paper.getSelectedPositions());
    if (id == Chapter4OperatorFixtures.Fig6CaseId.FA_CROSSOVER) {
      operators.crossoverFa(child, parent, range(selected), true);
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.FA_MUTATION) {
      operators.mutateFa(child, selected.get(0));
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.MA_CROSSOVER) {
      operators.crossoverMa(child, parent, range(selected), true);
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.MA_MUTATION) {
      operators.mutateMa(child, selected.get(0));
    } else if (id == Chapter4OperatorFixtures.Fig6CaseId.WA_CROSSOVER) {
      operators.crossoverWa(child, parent, range(selected), true);
    } else {
      operators.mutateWa(child, selected.get(0));
    }
    assertEquals(zero(paper.getExpectedFa()), child.getFactoryAssignments());
    assertEquals(zero(paper.getExpectedMa()), child.getMachineAssignments());
    assertEquals(zero(paper.getExpectedWa()), child.getWorkerAssignments());
  }

  private static DhhfspFourVectorSolution base(Chapter4OperatorFixtures fixture) {
    return new DhhfspFourVectorSolution(Arrays.asList(0, 1, 2, 3, 4),
        zero(fixture.getFig6BaseFa()), zero(fixture.getFig6BaseMa()),
        zero(fixture.getFig6BaseWa()), "published_baseline");
  }
  private static DhhfspInstance fig6Instance() {
    return new DhhfspInstance(5, 1, 2,
        new double[][] {{1, 1, 1, 1, 1}}, new double[][] {{1, 1, 1, 1, 1}},
        new double[][][] {{{1, 1, 1}}, {{1, 1}}},
        new double[][][] {{{1, 1, 1}}, {{1, 1}}},
        new double[][][] {{{1, 1}}, {{1}}},
        new double[][][] {{{1, 1}}, {{1}}});
  }
  private static int[] range(List<Integer> selected) {
    return new int[] {selected.get(0), selected.get(selected.size() - 1)};
  }
  private static List<Integer> zero(List<Integer> source) {
    List<Integer> result = new ArrayList<>();
    for (Integer value : source) result.add(value - 1);
    return result;
  }
  private static void replace(List<Integer> target, List<Integer> values) {
    target.clear(); target.addAll(values);
  }
}
