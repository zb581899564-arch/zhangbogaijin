package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ExactEvaluationBudget;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Fairness gates: under the same instance/seed/population the two external baselines
 * must consume byte-identical frozen initial populations, see exactly three objectives
 * ([0,1,6] = Cmax/TEC/TWC) through the FM3/NONE/single-family/sequence-independent-SUT
 * canonical problem, close the exact decoder budget with zero duplicate evaluations,
 * and expose no V35 mechanism events.
 */
public class V35ExternalBaselineFairnessTest {
  private static final int POPULATION = 100;
  private static final int BUDGET = 2000;
  private static final long SEED = 20260822L;

  @Test
  public void bothArmsStartFromByteIdenticalInitialPopulations() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    List<PermutationSolution<Integer>> frozen = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) frozen.add(problem.createSolution());
    String frozenHash = fourVectorHash(frozen);

    V35ComparisonProblemAdapter nsgaAdapter = adapter(problem, frozen);
    V35ComparisonProblemAdapter spea2Adapter = adapter(problem, frozen);
    List<PermutationSolution<Integer>> nsgaInitial = drain(nsgaAdapter);
    List<PermutationSolution<Integer>> spea2Initial = drain(spea2Adapter);
    assertEquals(frozenHash, fourVectorHash(nsgaInitial));
    assertEquals(frozenHash, fourVectorHash(spea2Initial));
  }

  /** Pure four-vector genotype hash; independent of the author-view machine attribute. */
  private static String fourVectorHash(List<PermutationSolution<Integer>> population) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      for (PermutationSolution<Integer> solution : population) {
        digest.update((V35ExactEvaluationBudget.genotypeFingerprint(solution) + "\n")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      StringBuilder builder = new StringBuilder();
      for (byte value : digest.digest()) {
        builder.append(String.format("%02x", value));
      }
      return builder.toString();
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  @Test
  public void sharedLayerIsFixedForBothArms() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    List<PermutationSolution<Integer>> frozen = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) frozen.add(problem.createSolution());
    V35ComparisonProblemAdapter adapter = adapter(problem, frozen);
    assertEquals(ProductionDecodeMode.FM3, problem.getMode());
    assertEquals(ZhangBoShiftConfiguration.none().getMode(),
        problem.getShiftConfiguration().getMode());
    assertEquals(V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE,
        adapter.getObjectiveView());
    assertEquals(3, adapter.getNumberOfObjectives());
    assertEquals(0, adapter.getNumberOfConstraints());
    PermutationSolution<Integer> solution = adapter.createSolution();
    adapter.evaluate(solution);
    assertEquals(3, solution.getNumberOfObjectives());
    for (int i = 0; i < 3; i++) {
      assertTrue("objective " + i + " must be finite",
          Double.isFinite(solution.getObjective(i)));
    }
  }

  @Test
  public void bothArmsCloseExactBudgetWithoutForbiddenMechanisms() throws Exception {
    for (V35P25EOfficialJMetalEngine.Algorithm algorithm
        : V35P25EOfficialJMetalEngine.Algorithm.values()) {
      ZhangBoCanonicalProductionProblem problem = problem();
      List<PermutationSolution<Integer>> frozen = new ArrayList<>();
      for (int i = 0; i < POPULATION; i++) frozen.add(problem.createSolution());
      String frozenHash = V35FairRunner.initialHash(frozen);
      V35ComparisonProblemAdapter adapter = adapter(problem, frozen);
      V35P25EAlgorithmResult result = V35P25EOfficialJMetalEngine.run(
          algorithm, adapter, POPULATION, BUDGET, SEED);
      assertEquals(BUDGET, result.getEvaluations());
      assertEquals(BUDGET, problem.getEvaluationCounter().getSuccessfulEvaluations());
      assertEquals(0, adapter.getBudget().getDuplicateEvaluations());
      assertEquals(0, adapter.getRepresentationRepairs());
      assertTrue("identity evidence must not mention any V35 mechanism",
              !result.getIdentityEvidence().contains("CFVF")
                  && !result.getIdentityEvidence().contains("PDDR")
                  && !result.getIdentityEvidence().contains("CA-TA")
                  && !result.getIdentityEvidence().contains("DSCR")
                  && !result.getIdentityEvidence().contains("Qg")
                  && !result.getIdentityEvidence().contains("Qp"));
      assertEquals("OFFICIAL_JMETAL_CORE", result.getSourceKind());
    }
  }

  private static List<PermutationSolution<Integer>> adapterDrain(
      V35ComparisonProblemAdapter adapter) {
    return drain(adapter);
  }

  private static List<PermutationSolution<Integer>> drain(
      V35ComparisonProblemAdapter adapter) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) result.add(adapter.createSolution());
    return result;
  }

  private static V35ComparisonProblemAdapter adapter(
      ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> frozen) {
    return new V35ComparisonProblemAdapter(problem,
        P8InitialPopulationProvider.copy(frozen),
        V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE, BUDGET);
  }

  private static ZhangBoCanonicalProductionProblem problem() throws Exception {
    java.nio.file.Path root = javaProject();
    return ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
  }

  static java.nio.file.Path javaProject() {
    java.nio.file.Path cwd =
        java.nio.file.Paths.get("").toAbsolutePath().normalize();
    if (java.nio.file.Files.isDirectory(cwd.resolve("EADHFSP"))) return cwd;
    if (cwd.getParent() != null
        && java.nio.file.Files.isDirectory(cwd.getParent().resolve("EADHFSP"))) {
      return cwd.getParent();
    }
    throw new IllegalStateException("cannot locate java project from " + cwd);
  }
}
