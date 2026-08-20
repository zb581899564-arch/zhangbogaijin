package org.uma.jmetal.runner.lc_psode;

import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.MOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.MOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.EDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** P4.1 zero-innovation smoke test for the author and direct-derivation paths. */
public class ZhangBoDirectDerivationSmokeTest {
  private static final int JOBS = 20;
  private static final int STAGES = 2;
  private static final int FACTORIES = 3;
  private static final int PROBLEM_ID = 1;
  private static final int SWARM_SIZE = 100;
  private static final int INITIAL_EVALUATION_BUDGET = 100;
  private static final long SEED = 20260808L;

  @Test
  public void authorAndDirectDerivationShouldRunWithTheSameConfiguration() throws Exception {
    String previous = System.getProperty("dhfsp.data.dir");
    try {
      System.setProperty("dhfsp.data.dir",
          Paths.get("..", "EADHFSP").toAbsolutePath().normalize().toString());

      EDHHFSPW authorProblem = new EDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      JMetalRandom.getInstance().setSeed(SEED);
      MOHPSOQ author = authorBuilder(authorProblem).build();
      author.run();

      ZhangBoEDHHFSPW derivedProblem =
          new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      JMetalRandom.getInstance().setSeed(SEED);
      ZhangBoMOHPSOQ derived = derivedBuilder(derivedProblem).build();
      derived.run();

      assertEquals(author.getClass().getSuperclass(), derived.getClass().getSuperclass());
      assertEquals(author.getName(), derived.getName());
      assertEquals(authorProblem.getNumberOfObjectives(), derivedProblem.getNumberOfObjectives());
      assertEquals(7, authorProblem.getNumberOfObjectives());
      assertEquals(author.getSwarm().size(), derived.getSwarm().size());
      assertEquals(SWARM_SIZE, author.getSwarm().size());

      assertResultShape(author.getResult());
      assertResultShape(derived.getResult());
    } finally {
      if (previous == null) {
        System.clearProperty("dhfsp.data.dir");
      } else {
        System.setProperty("dhfsp.data.dir", previous);
      }
    }
  }

  @Test
  public void runnerShouldKeepTheAuthorSignatureAndUseAnIsolatedClass() throws Exception {
    assertFalse(MOHPSOQRun.class.equals(ZhangBoMOHPSOQRun.class));
    assertNotNull(MOHPSOQRun.class.getMethod("mainexe", runnerParameterTypes()));
    assertNotNull(ZhangBoMOHPSOQRun.class.getMethod("mainexe", runnerParameterTypes()));
  }

  @Test
  public void authorAndDerivedRunnerEntriesShouldExecuteIndependently() throws Exception {
    String previous = System.getProperty("dhfsp.data.dir");
    try {
      System.setProperty("dhfsp.data.dir",
          Paths.get("..", "EADHFSP").toAbsolutePath().normalize().toString());
      Path root = Paths.get("target", "p4-1-runner-smoke",
          Long.toString(System.nanoTime())).toAbsolutePath();
      Path authorOutput = Files.createDirectories(root.resolve("author"));
      Path derivedOutput = Files.createDirectories(root.resolve("derived"));

      JMetalRandom.getInstance().setSeed(SEED);
      MOHPSOQRun.mainexe(new EDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID),
          JOBS, STAGES, FACTORIES, PROBLEM_ID, SWARM_SIZE, 0.4, 0.2, 0.06,
          40.0, 0.0, 0.0, 0.8, 0.9, INITIAL_EVALUATION_BUDGET,
          withSeparator(authorOutput), withSeparator(authorOutput), 1, true,
          0.4, 0.3, 0.1, 0.25, 40);

      JMetalRandom.getInstance().setSeed(SEED);
      ZhangBoMOHPSOQRun.mainexe(
          new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID),
          JOBS, STAGES, FACTORIES, PROBLEM_ID, SWARM_SIZE, 0.4, 0.2, 0.06,
          40.0, 0.0, 0.0, 0.8, 0.9, INITIAL_EVALUATION_BUDGET,
          withSeparator(derivedOutput), withSeparator(derivedOutput), 1, true,
          0.4, 0.3, 0.1, 0.25, 40);

      assertTrue(containsFileName(authorOutput, "MOHPSO-Q"));
      assertTrue(containsFileName(derivedOutput, "ZhangBo-MOHPSO-Q"));
    } finally {
      if (previous == null) {
        System.clearProperty("dhfsp.data.dir");
      } else {
        System.setProperty("dhfsp.data.dir", previous);
      }
    }
  }

  private static MOHPSOQBuilder authorBuilder(
      PermutationProblem<PermutationSolution<Integer>> problem) {
    return new MOHPSOQBuilder(problem, SWARM_SIZE, FACTORIES, 0.0, 0.8, 0.9, 40.0)
        .setMaxIterations(INITIAL_EVALUATION_BUDGET)
        .setSwarmSize(SWARM_SIZE)
        .setRand_k(0.4)
        .setCrossoverRate(0.2)
        .setMutationRate(0.06)
        .setCrossoverRates4worker(0.4)
        .setCrossoverRates4machine(0.3)
        .setMutationRate4worker(0.1)
        .setMutationRate4machine(0.25)
        .setLocalSearch(40);
  }

  private static ZhangBoMOHPSOQBuilder derivedBuilder(
      PermutationProblem<PermutationSolution<Integer>> problem) {
    return new ZhangBoMOHPSOQBuilder(problem, SWARM_SIZE, FACTORIES,
        0.0, 0.8, 0.9, 40.0)
        .setMaxIterations(INITIAL_EVALUATION_BUDGET)
        .setSwarmSize(SWARM_SIZE)
        .setRand_k(0.4)
        .setCrossoverRate(0.2)
        .setMutationRate(0.06)
        .setCrossoverRates4worker(0.4)
        .setCrossoverRates4machine(0.3)
        .setMutationRate4worker(0.1)
        .setMutationRate4machine(0.25)
        .setLocalSearch(40);
  }

  private static void assertResultShape(List<PermutationSolution<Integer>> result) {
    assertNotNull(result);
    assertFalse(result.isEmpty());
    for (PermutationSolution<Integer> solution : result) {
      assertEquals(JOBS, solution.getNumberOfVariables());
      assertEquals(JOBS, solution.getNumberOfVariablesid());
      assertEquals(7, solution.getNumberOfObjectives());
      assertNotNull(solution.getVariablesworker());
      assertTrue(solution.getAttribute("machine") instanceof List);
    }
  }

  private static Class<?>[] runnerParameterTypes() {
    return new Class<?>[] {
        PermutationProblem.class,
        int.class, int.class, int.class, int.class,
        int.class,
        double.class,
        double.class, double.class,
        double.class, double.class,
        double.class,
        double.class, double.class,
        int.class,
        String.class, String.class,
        int.class, boolean.class,
        double.class, double.class,
        double.class, double.class,
        int.class
    };
  }

  private static String withSeparator(Path directory) {
    return directory.toString() + java.io.File.separator;
  }

  private static boolean containsFileName(Path directory, String fragment) throws Exception {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths.anyMatch(path -> path.getFileName().toString().contains(fragment));
    }
  }
}
