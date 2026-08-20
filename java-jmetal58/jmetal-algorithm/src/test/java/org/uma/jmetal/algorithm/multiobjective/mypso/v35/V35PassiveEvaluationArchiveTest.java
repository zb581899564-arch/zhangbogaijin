package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/** V35-P17 pins: Pareto maintenance, defensive isolation and read-only surface. */
public class V35PassiveEvaluationArchiveTest {

  @Test
  public void paretoMaintenanceKeepsOnlyNondominatedMembers() {
    V35PassiveEvaluationArchive archive = new V35PassiveEvaluationArchive();
    archive.observe(solution(10, 10, 10));   // admitted
    archive.observe(solution(12, 5, 12));    // incomparable: admitted
    assertEquals(2, archive.size());
    archive.observe(solution(8, 8, 8));      // dominates (10,10,10): prunes it, admitted
    assertEquals(2, archive.size());
    archive.observe(solution(9, 9, 9));      // dominated by (8,8,8): never admitted
    assertEquals(2, archive.size());
    assertEquals(4L, archive.getObservedCount());
  }

  @Test
  public void defensiveSnapshotIsolation() {
    V35PassiveEvaluationArchive archive = new V35PassiveEvaluationArchive();
    archive.observe(solution(10, 10, 10));
    List<PermutationSolution<Integer>> snapshot = archive.snapshot();
    snapshot.get(0).setObjective(0, 1.0);
    assertEquals(10.0, archive.snapshot().get(0).getObjective(0), 0.0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void snapshotListIsUnmodifiable() {
    V35PassiveEvaluationArchive archive = new V35PassiveEvaluationArchive();
    archive.observe(solution(10, 10, 10));
    archive.snapshot().add(solution(1, 1, 1));
  }

  @Test
  public void countersAndCsvReflectObservedAndMaintainedArchive() {
    V35PassiveEvaluationArchive archive = new V35PassiveEvaluationArchive();
    archive.observe(solution(10, 10, 10));
    archive.observe(solution(8, 8, 8));
    archive.observe(solution(12, 5, 12));
    String csv = archive.toCsv();
    assertTrue(csv.startsWith("Cmax,TEC,TWC\n"));
    assertEquals(2, csv.split("\n").length - 1);
    assertTrue(archive.statistics().contains("observedCount=3"));
    assertTrue(archive.statistics().contains("archiveSize=2"));
  }

  private static DhhfspFourVectorSolution solution(double cmax, double tec, double twc) {
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 7);
    result.setObjective(0, cmax);
    result.setObjective(1, tec);
    result.setObjective(6, twc);
    return result;
  }
}
