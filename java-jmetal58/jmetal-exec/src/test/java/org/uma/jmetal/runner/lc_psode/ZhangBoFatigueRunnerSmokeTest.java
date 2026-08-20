package org.uma.jmetal.runner.lc_psode;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.solution.PermutationSolution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** P5 integration smoke: active fatigue problem through ZhangBo runner/builder/algorithm. */
public class ZhangBoFatigueRunnerSmokeTest {
  @Test
  public void activeFatigueProblemShouldRunThroughTheDirectDerivationChain() throws Exception {
    int jobs = 20;
    int stages = 2;
    int factories = 3;
    int problemId = 1;
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW fatigueProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          jobs, stages, factories, problemId);

      PermutationSolution<Integer> probe = fatigueProblem.createSolution();
      fatigueProblem.evaluate(probe);
      assertNotNull(probe.getAttribute(ZhangBoFatigueEvaluationResult.class));

      Path output = Files.createDirectories(Paths.get("target", "p5-fatigue-runner-smoke",
          Long.toString(System.nanoTime())).toAbsolutePath());
      ZhangBoMOHPSOQRun.mainexe(fatigueProblem,
          jobs, stages, factories, problemId, 100, 0.4, 0.2, 0.06,
          40.0, 0.0, 0.0, 0.8, 0.9, 100,
          output.toString() + java.io.File.separator,
          output.toString() + java.io.File.separator, 1, true,
          0.4, 0.3, 0.1, 0.25, 40);
      try (Stream<Path> files = Files.walk(output)) {
        assertTrue(files.anyMatch(path -> path.getFileName().toString().contains("ZhangBo-MOHPSO-Q")));
      }
    } finally {
      if (previousData == null) System.clearProperty("dhfsp.data.dir");
      else System.setProperty("dhfsp.data.dir", previousData);
      if (previousFatigue == null) System.clearProperty("dhfsp.fatigue.dir");
      else System.setProperty("dhfsp.fatigue.dir", previousFatigue);
    }
  }
}
