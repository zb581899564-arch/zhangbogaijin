package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

public class P8InitialPopulationProviderTest {
  @Test
  public void canonicalMachineVectorParticipatesInCommonInitialHash() {
    PermutationSolution<Integer> canonical = new DhhfspFourVectorSolution(
        Arrays.asList(1, 0), Arrays.asList(0, 1), Arrays.asList(2, 3),
        Arrays.asList(4, 5), "deterministic_canonical");
    PermutationSolution<Integer> otherTag = new DhhfspFourVectorSolution(
        Arrays.asList(1, 0), Arrays.asList(0, 1), Arrays.asList(2, 3),
        Arrays.asList(4, 5), "fatigue_improved");
    assertEquals(P8InitialPopulationProvider.sha256(Collections.singletonList(canonical)),
        P8InitialPopulationProvider.sha256(Collections.singletonList(otherTag)));
    otherTag = new DhhfspFourVectorSolution(
        Arrays.asList(1, 0), Arrays.asList(0, 1), Arrays.asList(9, 3),
        Arrays.asList(4, 5), "fatigue_improved");
    assertFalse(P8InitialPopulationProvider.sha256(Collections.singletonList(canonical))
        .equals(P8InitialPopulationProvider.sha256(Collections.singletonList(otherTag))));
  }
}
