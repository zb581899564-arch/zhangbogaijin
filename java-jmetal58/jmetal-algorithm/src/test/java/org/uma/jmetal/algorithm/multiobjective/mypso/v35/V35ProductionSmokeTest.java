package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.solution.PermutationSolution;

import static org.junit.Assert.assertTrue;

/** Small mechanism-chain smoke only; it is not a performance or paper experiment. */
public class V35ProductionSmokeTest {
  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void v35CanonicalProblemAndAlgorithmCompleteSmallRun() throws Exception {
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      Path project = Paths.get("").toAbsolutePath().normalize();
      if (project.getFileName() != null
          && "jmetal-algorithm".equals(project.getFileName().toString())) {
        project = project.getParent();
      }
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      ZhangBoCanonicalProductionProblem canonical = ZhangBoV35ProblemFactory.create(
          source.getFatigueInstanceData(), source.getFatigueParameters(),
          ProductionDecodeMode.FM3, 20260808L);
      Problem<PermutationSolution<Integer>> problem = (Problem) canonical;
       V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
           .seed(20260808L).populationSize(10).maxEvaluations(1000)
          .decoderMode(ProductionDecodeMode.FM3)
          .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true).build();
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, 10, 3,
          0.0, 0.8, 0.8, 2.0)
          .setMaxIterations(1000)
          .setV35Configuration(configuration)
          .build();
      algorithm.run();
      assertTrue(algorithm.getFullEvaluationCount() > 0L);
      assertTrue(algorithm.getFullEvaluationCount() <= 1000L);
      List<PermutationSolution<Integer>> result = algorithm.getResult();
      assertTrue(result != null && !result.isEmpty());
      assertTrue(algorithm.getZhangBoP6Events().stream()
          .anyMatch(value -> value.contains("DSCR evaluatedSnapshot=")));
      assertTrue(algorithm.getZhangBoP6Events().stream()
          .anyMatch(value -> value.contains("DSCR_TEACHER")));
      assertTrue(algorithm.getCaTaEvents().stream()
          .anyMatch(value -> value.contains("v35Lite:")));
      assertTrue(algorithm.getCaTaTestCalls() > 0L);
      assertTrue(algorithm.getCaTaFullEvaluations() > 0L);
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void v35BaselineUsesStructuredUpdaterAndQgWithoutLiteFeatures() throws Exception {
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      Path project = Paths.get("").toAbsolutePath().normalize();
      if (project.getFileName() != null
          && "jmetal-algorithm".equals(project.getFileName().toString())) project = project.getParent();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir", project.resolve("instance-extensions/v1").toString());
      ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      Problem<PermutationSolution<Integer>> problem = (Problem) ZhangBoV35ProblemFactory.create(
          source.getFatigueInstanceData(), source.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
       V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
           .seed(20260808L).populationSize(10).maxEvaluations(600)
          .decoderMode(ProductionDecodeMode.FM3).dscr(true).cfvf(false).qg(true).qp(false).caTaLite(false).build();
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, 10, 3, 0.0, 0.8, 0.8, 2.0)
          .setMaxIterations(600).setV35Configuration(configuration).build();
      algorithm.run();
      assertTrue(algorithm.getFullEvaluationCount() > 0L);
      assertTrue(algorithm.getBaselineUpdateEventCount() > 0L);
      assertTrue(algorithm.getFormalBaselineQgRounds() > 0L);
      assertTrue(algorithm.getResult() != null && !algorithm.getResult().isEmpty());
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
