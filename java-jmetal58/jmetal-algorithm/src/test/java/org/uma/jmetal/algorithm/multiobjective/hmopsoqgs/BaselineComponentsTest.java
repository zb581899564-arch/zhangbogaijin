package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaselineComponentsTest {
  @Test
  public void table9AndSmokeConfigurationsAreFrozen() {
    HmopsoQgsConfiguration table9 = HmopsoQgsConfiguration.publishedTable9(7L);
    assertEquals(100, table9.getPopulationSize());
    assertArrayEquals(new int[] {20, 20, 20, 40}, table9.getSubSwarmSizes());
    assertEquals(500000L, table9.getMaxEvaluations());
    assertEquals("author_actual_compatibility", table9.getAlphaProvenance());
    HmopsoQgsConfiguration smoke = HmopsoQgsConfiguration.engineeringSmoke(20260808L);
    assertArrayEquals(new int[] {2, 2, 2, 4}, smoke.getSubSwarmSizes());
    assertEquals(2000L, smoke.getMaxEvaluations());
  }

  @Test
  public void pddrUsesStrictParetoAndStableFingerprints() {
    DhhfspFourVectorSolution a = solution(0, 1, 1, 1);
    DhhfspFourVectorSolution b = solution(1, 2, 2, 2);
    DhhfspFourVectorSolution c = solution(2, 0.5, 3, 3);
    List<DhhfspFourVectorSolution> reference = Arrays.asList(a, b, c);
    PddrFf pddr = new PddrFf();
    assertTrue(PddrFf.dominates(a, b));
    assertEquals(0.5, pddr.score(a, reference), 0.0);
    assertEquals(2.0, pddr.score(b, reference), 0.0);
    assertEquals(2, pddr.nonDominated(reference).size());
  }

  @Test
  public void m3DirectionsAndCenterHaveRequestedSizes() {
    List<DhhfspFourVectorSolution> population = new ArrayList<>();
    population.add(solution(0, 1, 9, 9));
    population.add(solution(1, 9, 1, 9));
    population.add(solution(2, 9, 9, 1));
    population.add(solution(3, 3, 3, 3));
    Map<SubSwarm, List<DhhfspFourVectorSolution>> groups =
        new SubSwarmDecomposer().decompose(population, new int[] {1, 1, 1, 1});
    assertEquals(1.0, groups.get(SubSwarm.G1_CMAX).get(0).getObjective(0), 0.0);
    assertEquals(1.0, groups.get(SubSwarm.G2_TEC).get(0).getObjective(1), 0.0);
    assertEquals(1.0, groups.get(SubSwarm.G3_TWC).get(0).getObjective(2), 0.0);
    assertEquals(1, groups.get(SubSwarm.G4_CENTER).size());
  }

  @Test
  public void qStateExploitationAndAlphaOneUpdateAreExact() {
    ScriptedRandomGenerator random = new ScriptedRandomGenerator().doubles(0.0);
    QGbestController q = new QGbestController(random, 0.8, 1.0, 0.8);
    assertEquals(0, q.selectAction(SubSwarm.G1_CMAX, 0));
    assertEquals(0, QGbestController.stateFor(0.0));
    assertEquals(1, QGbestController.stateFor(-0.01));
    q.update(SubSwarm.G1_CMAX, 0, 1, 3.0, 0);
    assertEquals(3.0, q.getTable(SubSwarm.G1_CMAX)[0][1], 0.0);
  }

  @Test
  public void qExposesAllThreeActionsIncludingSeededExploration() {
    List<DhhfspFourVectorSolution> leaders = Arrays.asList(
        solution(0, 1, 3, 3), solution(1, 2, 2, 2), solution(2, 3, 1, 1));
    ScriptedRandomGenerator random = new ScriptedRandomGenerator()
        .ints(0, 1, 2, 0, 2).doubles(0.99);
    QGbestController q = new QGbestController(random, 0.8, 1.0, 0.8);
    q.initialize(SubSwarm.G1_CMAX, leaders);
    assertTrue(leaders.contains(q.selectLeader(SubSwarm.G1_CMAX, 0, leaders)));
    assertTrue(leaders.contains(q.selectLeader(SubSwarm.G1_CMAX, 1, leaders)));
    assertTrue(leaders.contains(q.selectLeader(SubSwarm.G1_CMAX, 2, leaders)));
    assertEquals(2, q.selectAction(SubSwarm.G1_CMAX, 0));
  }

  private static DhhfspFourVectorSolution solution(int rotation, double a, double b, double c) {
    List<Integer> jobs = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
    java.util.Collections.rotate(jobs, rotation);
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(jobs,
        Arrays.asList(0, 0, 0, 0), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 0, 0), "published_baseline");
    solution.setObjective(0, a);
    solution.setObjective(1, b);
    solution.setObjective(2, c);
    return solution;
  }
}
