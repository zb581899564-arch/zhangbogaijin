package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

public class V35P25EFaithfulEnginesTest {
  private static final int POPULATION = 100;
  private static final int BUDGET = 2000;
  private static final long SEED = 20260822L;

  @Test
  public void officialJMetalCoresCloseTheExactBudget() throws Exception {
    for (V35P25EOfficialJMetalEngine.Algorithm algorithm
        : V35P25EOfficialJMetalEngine.Algorithm.values()) {
      Fixture fixture = fixture(V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE);
      V35P25EAlgorithmResult result = V35P25EOfficialJMetalEngine.run(
          algorithm, fixture.adapter, POPULATION, BUDGET, SEED);
      assertEquals(BUDGET, result.getEvaluations());
      assertEquals(BUDGET, fixture.problem.getEvaluationCounter().getSuccessfulEvaluations());
      assertFalse(result.getFront().isEmpty());
      assertEquals("OFFICIAL_JMETAL_CORE", result.getSourceKind());
    }
  }

  @Test
  public void paperAuthorCoresCloseTheExactBudgetWithoutImprovementModules()
      throws Exception {
    for (V35P25EPaperAuthorEngine.AlgorithmKind algorithm
        : V35P25EPaperAuthorEngine.AlgorithmKind.values()) {
      Fixture fixture = fixture(V35ComparisonProblemAdapter.ObjectiveView.AUTHOR_SEVEN_SLOT);
      V35P25EAlgorithmResult result = V35P25EPaperAuthorEngine.run(
          algorithm, fixture.adapter, POPULATION, BUDGET, SEED);
      assertTrue(result.getEvaluations() <= BUDGET);
      assertEquals(result.getEvaluations(),
          fixture.problem.getEvaluationCounter().getSuccessfulEvaluations());
      assertEquals(0, fixture.adapter.getBudget().getDuplicateEvaluations());
      assertFalse(result.getFront().isEmpty());
      assertEquals("PAPER_AUTHOR_SOURCE", result.getSourceKind());
      assertTrue(result.getIdentityEvidence().contains("paperClass=")
          || algorithm == V35P25EPaperAuthorEngine.AlgorithmKind.MOPSO_F);
      String expectedEvent = algorithm == V35P25EPaperAuthorEngine.AlgorithmKind.MOPSO_F
          ? "MOPSO_VELOCITY_UPDATE="
          : algorithm == V35P25EPaperAuthorEngine.AlgorithmKind.MOPSODS_DE_F
              ? "MOPSODS_DE_GROUPED_UPDATE="
              : algorithm == V35P25EPaperAuthorEngine.AlgorithmKind.HMOPSO_QLS_F
                  ? "HMOPSO_QLS_Q_LEARNING=" : "MOHEADE_REPRODUCTION=";
      assertTrue(result.getIdentityEvidence(),
          result.getIdentityEvidence().contains(expectedEvent));
      if (algorithm == V35P25EPaperAuthorEngine.AlgorithmKind.HMOPSO_QLS_F) {
        assertEquals(1950, result.getEvaluations());
      } else {
        assertEquals(BUDGET, result.getEvaluations());
      }
    }
  }

  private static Fixture fixture(V35ComparisonProblemAdapter.ObjectiveView view)
      throws Exception {
    Path root = javaProject();
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) initial.add(problem.createSolution());
    String before = P8InitialPopulationProvider.sha256(initial);
    V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(
        problem, P8InitialPopulationProvider.copy(initial), view, BUDGET);
    assertEquals(before, P8InitialPopulationProvider.sha256(initial));
    return new Fixture(problem, adapter);
  }

  private static Path javaProject() {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("EADHFSP"))) return cwd;
    if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("EADHFSP"))) {
      return cwd.getParent();
    }
    throw new IllegalStateException("cannot locate java project from " + cwd);
  }

  private static final class Fixture {
    private final ZhangBoCanonicalProductionProblem problem;
    private final V35ComparisonProblemAdapter adapter;
    private Fixture(ZhangBoCanonicalProductionProblem problem,
        V35ComparisonProblemAdapter adapter) {
      this.problem = problem; this.adapter = adapter;
    }
  }
}
