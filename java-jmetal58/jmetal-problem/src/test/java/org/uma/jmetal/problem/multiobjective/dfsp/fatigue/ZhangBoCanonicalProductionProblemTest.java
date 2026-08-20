package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/** P8.1 production-boundary and identity/deep-copy gates. */
public class ZhangBoCanonicalProductionProblemTest {
  @Test
  public void explicitModesUseSevenLegacyObjectiveSlotsAndFatigueTrace() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = parameters(instance.getInstanceSha256());
    for (ProductionDecodeMode mode : new ProductionDecodeMode[] {
        ProductionDecodeMode.CANONICAL_NO_FATIGUE,
        ProductionDecodeMode.FM1, ProductionDecodeMode.FM2, ProductionDecodeMode.FM3}) {
      ZhangBoCanonicalProductionProblem problem =
          new ZhangBoCanonicalProductionProblem(instance, parameters, mode, 20260808L);
      DhhfspFourVectorSolution solution = problem.createSolution();
      assertEquals(7, solution.getNumberOfObjectives());
      problem.evaluate(solution);
      assertEquals(1L, problem.getEvaluationCounter().getSuccessfulEvaluations());
      assertEquals(mode, solution.getAttribute(
          org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.class));
      assertTrue(solution.getAttribute(ZhangBoFatigueEvaluationResult.class)
          instanceof ZhangBoFatigueEvaluationResult);
      assertEquals(solution.getObjective(0),
          ((ZhangBoFatigueEvaluationResult) solution.getAttribute(
              ZhangBoFatigueEvaluationResult.class)).getObjectives()[0], 0.0);
    }
  }

  @Test
  public void formalLoaderParsesInstanceAndBothManifestsWithoutLegacyProblem() throws Exception {
    Path project = Paths.get("..").toAbsolutePath().normalize();
    Path data = project.resolve("EADHFSP/20_2_3_1.txt");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        data, ProductionDecodeMode.FM2, 20260808L,
        project.resolve("instance-extensions/v1"),
        project.resolve("fatigue-parameters/v1"));
    DhhfspFourVectorSolution solution = problem.createSolution();
    problem.evaluate(solution);
    assertEquals(7, problem.getNumberOfObjectives());
    assertTrue(solution.getAttribute(ZhangBoFatigueEvaluationResult.class)
        instanceof ZhangBoFatigueEvaluationResult);
  }

  @Test
  public void formalLoaderAcceptsTheIsolatedGoldenCompatibilityBridge() throws Exception {
    Path root = Files.createTempDirectory("p8-canonical-golden-");
    try {
      P8GoldenAuthorCompatibilityBridge.Manifest bridge =
          P8GoldenAuthorCompatibilityBridge.materialize(root);
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          bridge.dataFile, ProductionDecodeMode.CANONICAL_NO_FATIGUE, 20260808L,
          root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"));
      DhhfspFourVectorSolution solution = problem.createSolution();
      problem.evaluate(solution);
      assertEquals(7, solution.getNumberOfObjectives());
    } finally {
      deleteTree(root);
    }
  }

  @Test
  public void factoryIsInstanceBoundAndNonCanonicalIdentitySurvivesEvaluation() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoCanonicalSolutionFactory factory = new ZhangBoCanonicalSolutionFactory(
        instance, ProductionDecodeMode.FM3, 19L);
    DhhfspFourVectorSolution first = factory.create(0L);
    DhhfspFourVectorSolution second = factory.create(0L);
    assertNotSame(first, second);
    assertEquals(first.getJobSequence(), second.getJobSequence());

    ZhangBoFatigueInstanceData identityInstance = nonCanonicalInstance();
    DhhfspFourVectorSolution nonCanonical = new DhhfspFourVectorSolution(
        Arrays.asList(2, 0, 1, 3), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 0, 0), Arrays.asList(0, 1, 0, 1),
        ProductionDecodeMode.FM3.getSemanticTag(), 7);
    ZhangBoFatigueEvaluationResult result = new ZhangBoFatigueEvaluator().evaluate(
        identityInstance, parametersFour(identityInstance.getInstanceSha256()), nonCanonical,
        ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);
    // The first record is job 2, and its WA is looked up by JS inverse (position 0).
    ZhangBoFatigueOperationRecord firstRecord = result.getOperations().get(0);
    assertEquals(2, firstRecord.job);
    assertEquals(0, firstRecord.worker);
  }

  @Test
  public void factoriesKeepIndependentInstanceDimensionsInOneJvm() {
    ZhangBoCanonicalSolutionFactory fourJob = new ZhangBoCanonicalSolutionFactory(
        instance(), ProductionDecodeMode.FM1, 1L);
    ZhangBoCanonicalSolutionFactory twoJob = new ZhangBoCanonicalSolutionFactory(
        twoJobInstance(), ProductionDecodeMode.FM1, 1L);
    assertEquals(4, fourJob.create(0L).getNumberOfVariables());
    assertEquals(2, twoJob.create(0L).getNumberOfVariables());
    assertEquals(4, fourJob.create(1L).getNumberOfVariables());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void nestedAttributeCopyDoesNotShareMutableArrayElements() {
    DhhfspFourVectorSolution source = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0), Arrays.asList(0, 0),
        "deterministic_canonical");
    Object[] nested = new Object[] {new ArrayList<String>(Arrays.asList("original"))};
    source.setAttribute("nested", nested);
    DhhfspFourVectorSolution copy = source.copy();
    ((List<String>) ((Object[]) copy.getAttribute("nested"))[0]).set(0, "changed");
    assertEquals("original", ((List<String>) ((Object[]) source.getAttribute("nested"))[0]).get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void productionProblemRejectsNonP2PermutationSolution() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoCanonicalProductionProblem problem = new ZhangBoCanonicalProductionProblem(
        instance, parameters(instance.getInstanceSha256()), ProductionDecodeMode.FM1, 7L);
    @SuppressWarnings("unchecked")
    PermutationSolution<Integer> foreign = (PermutationSolution<Integer>) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {PermutationSolution.class},
        (proxy, method, args) -> null);
    problem.evaluate(foreign);
  }

  @Test
  public void oneShiftedExternalEvaluationStillConsumesExactlyOneFe() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoCanonicalProductionProblem problem = new ZhangBoCanonicalProductionProblem(
        instance, parameters(instance.getInstanceSha256()), ProductionDecodeMode.FM3, 7L,
        ZhangBoShiftConfiguration.formalLeftRight());
    DhhfspFourVectorSolution solution = problem.createSolution();
    problem.evaluate(solution);
    assertEquals(1L, problem.getEvaluationCounter().getSuccessfulEvaluations());
    assertEquals(ZhangBoShiftMode.LEFT_RIGHT,
        problem.getShiftConfiguration().getMode());
    assertTrue(((ZhangBoFatigueEvaluationResult) solution.getAttribute(
        ZhangBoFatigueEvaluationResult.class)).getShiftSummary() != null);
  }

  private static ZhangBoFatigueInstanceData instance() {
    String sha = repeat('C', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}}, repeat('D', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 2, 1,
        new int[][] {{1, 2}}, new double[][][] {{{1.0}, {1.0, 1.0}}},
        new int[][][] {{{5}, {5, 5}}},
        new int[][] {{10, 20}, {10, 20}, {10, 20}, {10, 20}},
        new int[] {3}, new double[][] {{1.0, 1.0, 1.0}},
        new int[][] {{10, 10, 10}}, extension);
  }

  private static ZhangBoFatigueParameters parameters(String sha) {
    double[][][] lambda = new double[][][] {{{0.03, 0.03, 0.03}, {0.03, 0.03, 0.03}}};
    double[][][] mu = new double[][][] {{{0.05, 0.05, 0.05}, {0.05, 0.05, 0.05}}};
    return new ZhangBoFatigueParameters(sha, lambda, mu,
        new double[] {0.30, 0.30}, 0.80, 0.90, "");
  }

  private static ZhangBoFatigueInstanceData nonCanonicalInstance() {
    String sha = repeat('B', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}}, repeat('A', 64));
    return new ZhangBoFatigueInstanceData(sha, 4, 2, 1,
        new int[][] {{2, 2}}, new double[][][] {{{1.0, 1.0}, {1.0, 1.0}}},
        new int[][][] {{{5, 5}, {5, 5}}},
        new int[][] {{10, 20}, {10, 20}, {10, 20}, {10, 20}},
        new int[] {4}, new double[][] {{1.0, 1.0, 1.0, 1.0}},
        new int[][] {{10, 10, 10, 10}}, extension);
  }

  private static ZhangBoFatigueParameters parametersFour(String sha) {
    double[][][] lambda = new double[][][] {{{0.03, 0.03, 0.03, 0.03},
        {0.03, 0.03, 0.03, 0.03}}};
    double[][][] mu = new double[][][] {{{0.05, 0.05, 0.05, 0.05},
        {0.05, 0.05, 0.05, 0.05}}};
    return new ZhangBoFatigueParameters(sha, lambda, mu,
        new double[] {0.30, 0.30}, 0.80, 0.90, "");
  }

  private static ZhangBoFatigueInstanceData twoJobInstance() {
    String sha = repeat('9', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 2, 1, new int[][] {{1}, {2}}, repeat('8', 64));
    return new ZhangBoFatigueInstanceData(sha, 2, 1, 1,
        new int[][] {{1}}, new double[][][] {{{1.0}}}, new int[][][] {{{1}}},
        new int[][] {{1}, {1}}, new int[] {1}, new double[][] {{1.0}},
        new int[][] {{1}}, extension);
  }

  private static String repeat(char value, int count) {
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }

  private static void deleteTree(Path root) throws Exception {
    if (!Files.exists(root)) return;
    try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
      java.util.List<Path> ordered = paths.sorted(java.util.Comparator.reverseOrder())
          .collect(java.util.stream.Collectors.toList());
      for (Path path : ordered) Files.deleteIfExists(path);
    }
  }
}
