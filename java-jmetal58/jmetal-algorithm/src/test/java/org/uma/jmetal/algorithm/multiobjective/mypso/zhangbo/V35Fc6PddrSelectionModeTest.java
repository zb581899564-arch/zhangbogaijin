package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.uma.jmetal.solution.PermutationSolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** FC-6A.3/FC-6B selector boundaries are explicit and independently testable. */
public class V35Fc6PddrSelectionModeTest {
  @Test
  public void globalOriginalHasNoBoundaryReservationOrRegionRole() {
    List<ZhangBoEvaluatedPddrSelector.CandidateInput> values = inputs(120);
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected = new ZhangBoEvaluatedPddrSelector()
        .select(values, Collections.<PermutationSolution<Integer>>emptyList(),
            Collections.<List<PermutationSolution<Integer>>>emptyList(), 100,
            PddrSelectionMode.GLOBAL_ORIGINAL);
    assertEquals(100, selected.size());
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      assertNull(candidate.getAssignedRegionRole());
    }
  }

  @Test
  public void regionAwareUsesFixedPhysicalCapacitiesAndCarriesRoles() {
    List<ZhangBoEvaluatedPddrSelector.CandidateInput> values = inputs(200);
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected = new ZhangBoEvaluatedPddrSelector()
        .select(values, Collections.<PermutationSolution<Integer>>emptyList(),
            Collections.<List<PermutationSolution<Integer>>>emptyList(), 100,
            PddrSelectionMode.REGION_AWARE);
    assertEquals(100, selected.size());
    Map<ZhangBoSubSwarm, Integer> counts = new EnumMap<ZhangBoSubSwarm, Integer>(
        ZhangBoSubSwarm.class);
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      ZhangBoSubSwarm role = candidate.getAssignedRegionRole();
      counts.put(role, counts.containsKey(role) ? counts.get(role) + 1 : 1);
    }
    assertEquals(15, counts.get(ZhangBoSubSwarm.G1_CMAX).intValue());
    assertEquals(55, counts.get(ZhangBoSubSwarm.G4_BALANCED).intValue());
    assertEquals(15, counts.get(ZhangBoSubSwarm.G2_TEC).intValue());
    assertEquals(15, counts.get(ZhangBoSubSwarm.G3_TWC).intValue());
    assertEquals(ZhangBoSubSwarm.G1_CMAX, selected.get(0).getAssignedRegionRole());
    assertEquals(ZhangBoSubSwarm.G4_BALANCED, selected.get(15).getAssignedRegionRole());
    assertEquals(ZhangBoSubSwarm.G2_TEC, selected.get(70).getAssignedRegionRole());
    assertEquals(ZhangBoSubSwarm.G3_TWC, selected.get(85).getAssignedRegionRole());
  }

  private static List<ZhangBoEvaluatedPddrSelector.CandidateInput> inputs(int size) {
    List<ZhangBoEvaluatedPddrSelector.CandidateInput> result = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      // Cmax increases while TEC decreases: all candidates remain mutually non-dominated.
      PermutationSolution<Integer> solution = new ZhangBoTestPermutationSolution(
          new int[]{0, 1}, new int[]{index % 2, (index + 1) % 2},
          new int[]{index % 3, (index + 1) % 3}, new int[]{index % 2, (index + 1) % 2},
          new int[0], 100.0 + index, 1000.0 - index, 50.0 + (index % 7)).withFatigue(0.2, 0.1);
      result.add(ZhangBoEvaluatedPddrSelector.CandidateInput.ofEvaluated(solution,
          new ArrayList<PermutationSolution<Integer>>(),
          ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, index, index, index));
    }
    return result;
  }
}
