package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;

/** Verifies P8's explicit common-start boundary without changing the author initializer. */
public class ZhangBoP8InitialPopulationProviderTest {
  @Test
  public void shouldCreateASeedStableTwoStagePopulationForTheExplicitInstance() throws Exception {
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      ZhangBoEDHHFSPW firstProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      List<PermutationSolution<Integer>> first = P8InitialPopulationProvider.create(
          firstProblem, 5, 20260808L, 2);
      ZhangBoEDHHFSPW secondProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      List<PermutationSolution<Integer>> second = P8InitialPopulationProvider.create(
          secondProblem, 5, 20260808L, 2);
      assertEquals(P8InitialPopulationProvider.sha256(first), P8InitialPopulationProvider.sha256(second));
      for (PermutationSolution<Integer> solution : first) {
        assertEquals(40, solution.getNumberOfVariablesworker());
      }
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key); else System.setProperty(key, value);
  }
}
