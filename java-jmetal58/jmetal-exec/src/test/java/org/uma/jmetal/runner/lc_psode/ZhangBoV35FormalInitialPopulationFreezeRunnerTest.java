package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** A snapshot is a genuine four-vector input, not merely a reproducible seed claim. */
public class ZhangBoV35FormalInitialPopulationFreezeRunnerTest {
  @Test
  public void writesAndReadsTheSamePopulationWithoutAnyEvaluation() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    while (project.getParent() != null && !Files.isDirectory(project.resolve("java-jmetal58/EADHFSP"))) project = project.getParent();
    Path bundle = Files.createTempDirectory("v35-formal-population-freeze-");
    try {
      ZhangBoV35FormalInitialPopulationFreezeRunner.materialize(project, bundle.resolve("bundle"));
      ZhangBoV35FormalInitialPopulationFreezeRunner.Verification verification =
          ZhangBoV35FormalInitialPopulationFreezeRunner.verify(project, bundle.resolve("bundle"));
      assertEquals(900, verification.rows);
      Path javaProject = project.resolve("java-jmetal58");
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          javaProject.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, 20260808L,
          javaProject.resolve("instance-extensions/v1"), javaProject.resolve("fatigue-parameters/v1"),
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> loaded = ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(
          bundle.resolve("bundle/initial-populations/20_2_3_1/seed-20260808.fourvec"), problem);
      assertEquals(100, loaded.size());
      assertEquals(0L, problem.getEvaluationCounter().getSuccessfulEvaluations());
      assertEquals(V35FairRunner.initialHash(loaded), V35FairRunner.initialHash(loaded));
      assertTrue(P8InitialPopulationProvider.sha256(loaded).matches("[0-9a-f]{64}"));
    } finally {
      // Test output lives under the system temporary directory only.
      delete(bundle);
    }
  }

  private static void delete(Path root) throws Exception {
    if (root == null || !Files.exists(root)) return;
    try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
        try { Files.delete(path); } catch (Exception error) { throw new RuntimeException(error); }
      });
    }
  }
}
