package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Chapter4OperatorFixturesTest {
  @Test
  public void shouldLoadFig5ExchangeFixtureExactly() {
    Chapter4OperatorFixtures fixtures = Chapter4OperatorFixtures.load();
    Chapter4OperatorFixtures.Fig5Fixture fig5 = fixtures.getFig5();

    assertEquals(1, fixtures.getIndexBase());
    assertEquals(Arrays.asList(3,1,7,6,2,5,4,8), fig5.getX());
    assertEquals(Arrays.asList(1,3,7,2,6,8,4,5), fig5.getY());
    assertEquals(Arrays.asList(
        pair(1,2), pair(4,5), pair(6,8)), fig5.getExchangeSequence());
    assertEquals(0.4, fig5.getR1(), 0.0);
    assertEquals(1, fig5.getSelectedCount());
    assertEquals(pair(1,2), fig5.getSelectedPair());
    assertEquals(Arrays.asList(3,1,7,2,6,8,4,5), fig5.getExpected());
  }

  @Test
  public void shouldLoadAllSixFig6CasesExactly() {
    Chapter4OperatorFixtures fixtures = Chapter4OperatorFixtures.load();
    assertEquals(Arrays.asList(1,2,1,1,2), fixtures.getFig6BaseFa());
    assertEquals(Arrays.asList(1,2,2,3,1), fixtures.getFig6BaseMa());
    assertEquals(Arrays.asList(1,1,2,2,1), fixtures.getFig6BaseWa());
    assertEquals(1, fixtures.getFig6Factory2Stage1WorkerCount());

    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.FA_CROSSOVER,
        "crossover", "FA", Arrays.asList(2,4), Arrays.asList(2,1,1,2,1),
        Arrays.asList(1,1,1,2,2), Arrays.asList(1,2,2,1,1), Arrays.asList(1,1,2,1,1));
    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.FA_MUTATION,
        "mutation", "FA", Arrays.asList(4), Arrays.<Integer>asList(),
        Arrays.asList(1,2,1,2,2), Arrays.asList(1,2,2,2,1), Arrays.asList(1,1,2,1,1));
    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.MA_CROSSOVER,
        "crossover", "MA", Arrays.asList(4,5), Arrays.asList(2,1,3,1,3),
        Arrays.asList(1,2,1,1,2), Arrays.asList(1,2,2,1,2), Arrays.asList(1,1,2,2,1));
    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.MA_MUTATION,
        "mutation", "MA", Arrays.asList(4), Arrays.<Integer>asList(),
        Arrays.asList(1,2,1,1,2), Arrays.asList(1,2,2,1,1), Arrays.asList(1,1,2,2,1));
    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.WA_CROSSOVER,
        "crossover", "WA", Arrays.asList(4,5), Arrays.asList(1,2,1,1,2),
        Arrays.asList(1,2,1,1,2), Arrays.asList(1,2,2,3,1), Arrays.asList(1,1,2,1,1));
    assertCase(fixtures, Chapter4OperatorFixtures.Fig6CaseId.WA_MUTATION,
        "mutation", "WA", Arrays.asList(4), Arrays.<Integer>asList(),
        Arrays.asList(1,2,1,1,2), Arrays.asList(1,2,2,3,1), Arrays.asList(1,1,2,1,1));
  }

  private static void assertCase(
      Chapter4OperatorFixtures fixtures,
      Chapter4OperatorFixtures.Fig6CaseId id,
      String operation,
      String changedVector,
      java.util.List<Integer> selectedPositions,
      java.util.List<Integer> parentVector,
      java.util.List<Integer> expectedFa,
      java.util.List<Integer> expectedMa,
      java.util.List<Integer> expectedWa) {
    Chapter4OperatorFixtures.Fig6Case fixture = fixtures.getFig6Case(id);
    assertEquals(operation, fixture.getOperation());
    assertEquals(changedVector, fixture.getChangedVector());
    assertEquals(selectedPositions, fixture.getSelectedPositions());
    assertEquals(parentVector, fixture.getParentVector());
    assertEquals(expectedFa, fixture.getExpectedFa());
    assertEquals(expectedMa, fixture.getExpectedMa());
    assertEquals(expectedWa, fixture.getExpectedWa());
    org.junit.Assert.assertFalse(fixture.getEvents().isEmpty());
  }

  private static Chapter4OperatorFixtures.SwapPair pair(int first, int second) {
    return new Chapter4OperatorFixtures.SwapPair(first, second);
  }
}
