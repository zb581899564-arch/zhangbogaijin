package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoIncrementalParetoArchive;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

public class V35ArchiveExperimentRuntimeTest {
  @Test
  public void teacherViewDoesNotMutateFullArchive() {
    List<PermutationSolution<Integer>> full = nondominated(90);
    String before = objectiveText(full);
    V35ArchiveExperimentRuntime runtime = new V35ArchiveExperimentRuntime(
        V35ArchiveExperimentProfile.ND1_TEACHER_VIEW_K50,
        new V35PassiveEvaluationArchive());
    List<PermutationSolution<Integer>> view = runtime.teacherCandidates(full);
    assertEquals(50, view.size());
    assertEquals(90, full.size());
    assertEquals(before, objectiveText(full));
  }

  @Test
  public void activeArchiveIsStrictlyBoundedWithoutChangingPddrConfiguration() {
    List<PermutationSolution<Integer>> archive = nondominated(220);
    V35PassiveEvaluationArchive observed = new V35PassiveEvaluationArchive();
    for (PermutationSolution<Integer> value : archive) observed.observe(value);
    V35ArchiveExperimentRuntime runtime = new V35ArchiveExperimentRuntime(
        V35ArchiveExperimentProfile.ND4_ACTIVE_ARCHIVE_K100,
        observed);
    PermutationSolution<Integer> candidate = solution(500, 500, 500, 999);
    int before = archive.size();
    ZhangBoIncrementalParetoArchive.Update update =
        ZhangBoIncrementalParetoArchive.addWithReport(archive, candidate);
    runtime.afterArchiveUpdate(archive, 1000L, 1L, before, update, System.nanoTime());
    assertEquals(100, archive.size());
    assertStrictlyNondominated(archive);
    assertTrue(containsObjectives(archive, 0, 220, 100));
    assertTrue(containsObjectives(archive, 219, 1, 100));
    assertEquals(220, observed.size());
    assertTrue(runtime.getProfile().canonicalText()
        .contains("pddrSelectionMode=UNCHANGED_GLOBAL_ORIGINAL"));
  }

  @Test
  public void k200ActiveArchiveRetainsCapacityAndPassiveFullFront() {
    List<PermutationSolution<Integer>> archive = nondominated(240);
    V35PassiveEvaluationArchive observed = new V35PassiveEvaluationArchive();
    for (PermutationSolution<Integer> value : archive) observed.observe(value);
    V35ArchiveExperimentRuntime runtime = new V35ArchiveExperimentRuntime(
        V35ArchiveExperimentProfile.ND3_ACTIVE_ARCHIVE_K200, observed);
    PermutationSolution<Integer> dominated = solution(500, 500, 500, 1001);
    int before = archive.size();
    ZhangBoIncrementalParetoArchive.Update update =
        ZhangBoIncrementalParetoArchive.addWithReport(archive, dominated);
    runtime.afterArchiveUpdate(archive, 1000L, 1L, before, update, System.nanoTime());
    assertEquals(200, archive.size());
    assertEquals(240, observed.size());
    assertStrictlyNondominated(archive);
  }

  @Test
  public void controlNeverPrunes() {
    List<PermutationSolution<Integer>> archive = nondominated(220);
    V35ArchiveExperimentRuntime runtime = new V35ArchiveExperimentRuntime(
        V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL,
        new V35PassiveEvaluationArchive());
    PermutationSolution<Integer> candidate = solution(500, 500, 500, 999);
    int before = archive.size();
    ZhangBoIncrementalParetoArchive.Update update =
        ZhangBoIncrementalParetoArchive.addWithReport(archive, candidate);
    runtime.afterArchiveUpdate(archive, 1000L, 1L, before, update, System.nanoTime());
    assertEquals(220, archive.size());
  }

  private static List<PermutationSolution<Integer>> nondominated(int count) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      result.add(solution(index, count - index, 100, index));
    }
    return result;
  }

  private static boolean containsObjectives(List<PermutationSolution<Integer>> values,
      double cmax, double tec, double twc) {
    for (PermutationSolution<Integer> value : values) {
      if (value.getObjective(0) == cmax && value.getObjective(1) == tec
          && value.getObjective(6) == twc) return true;
    }
    return false;
  }

  private static void assertStrictlyNondominated(List<PermutationSolution<Integer>> values) {
    for (int left = 0; left < values.size(); left++) {
      for (int right = 0; right < values.size(); right++) if (left != right) {
        boolean noWorse = true;
        boolean strict = false;
        for (int objective : new int[] {0, 1, 6}) {
          double a = values.get(left).getObjective(objective);
          double b = values.get(right).getObjective(objective);
          if (a > b) noWorse = false;
          if (a < b) strict = true;
        }
        assertFalse("archive contains a strict dominance pair", noWorse && strict);
      }
    }
  }

  private static DhhfspFourVectorSolution solution(double cmax, double tec, double twc, int id) {
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        Arrays.asList(id, id + 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 7);
    result.setObjective(0, cmax); result.setObjective(1, tec); result.setObjective(6, twc);
    return result;
  }

  private static String objectiveText(List<PermutationSolution<Integer>> values) {
    StringBuilder out = new StringBuilder();
    for (PermutationSolution<Integer> value : values) out.append(value.getObjective(0)).append('|')
        .append(value.getObjective(1)).append('|').append(value.getObjective(6)).append('\n');
    return out.toString();
  }
}
