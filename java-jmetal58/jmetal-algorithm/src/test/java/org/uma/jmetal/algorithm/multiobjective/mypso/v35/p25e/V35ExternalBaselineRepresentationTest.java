package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ExactEvaluationBudget;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * Representation-level gates for the external fair baselines: the four-vector
 * operators must be pure representation repair (objective-blind), deep-copying,
 * and legality preserving; the exact budget must reject duplicate evaluations.
 */
public class V35ExternalBaselineRepresentationTest {
  private static final long SEED = 20260822L;

  @Test
  public void crossoverDeepCopiesAndKeepsParentsUnchanged() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    V35FourVectorVariation.Crossover crossover = crossover(problem, 12345L);
    for (int trial = 0; trial < 10; trial++) {
      DhhfspFourVectorSolution a = (DhhfspFourVectorSolution) problem.createSolution();
      DhhfspFourVectorSolution b = (DhhfspFourVectorSolution) problem.createSolution();
      problem.evaluate(a);
      problem.evaluate(b);
      String beforeA = V35ExactEvaluationBudget.genotypeFingerprint(a);
      String beforeB = V35ExactEvaluationBudget.genotypeFingerprint(b);
      List<PermutationSolution<Integer>> children = crossover.execute(parents(a, b));
      assertEquals(2, children.size());
      assertEquals(beforeA, V35ExactEvaluationBudget.genotypeFingerprint(a));
      assertEquals(beforeB, V35ExactEvaluationBudget.genotypeFingerprint(b));
      for (PermutationSolution<Integer> child : children) {
        assertValidPermutation(child);
        assertRepresentationLegal(problem, (DhhfspFourVectorSolution) child);
      }
    }
  }

  @Test
  public void mutationKeepsRepresentationLegal() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    V35FourVectorVariation.Mutation mutation = mutation(problem, 67890L);
    for (int trial = 0; trial < 50; trial++) {
      DhhfspFourVectorSolution solution = (DhhfspFourVectorSolution) problem.createSolution();
      PermutationSolution<Integer> child = mutation.execute(solution);
      assertValidPermutation(child);
      assertRepresentationLegal(problem, (DhhfspFourVectorSolution) child);
    }
  }

  @Test
  public void operatorsAreBlindToObjectives() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    DhhfspFourVectorSolution a = (DhhfspFourVectorSolution) problem.createSolution();
    DhhfspFourVectorSolution b = (DhhfspFourVectorSolution) problem.createSolution();

    V35FourVectorVariation.Crossover firstCrossover = crossover(problem, 99L);
    V35FourVectorVariation.Crossover secondCrossover = crossover(problem, 99L);
    a.setObjective(0, 0.0);
    b.setObjective(0, 1.0e9);
    List<PermutationSolution<Integer>> first = firstCrossover.execute(parents(a, b));
    a.setObjective(0, 1.0e9);
    b.setObjective(0, 0.0);
    List<PermutationSolution<Integer>> second = secondCrossover.execute(parents(a, b));
    assertEquals(V35ExactEvaluationBudget.genotypeFingerprint(
            (DhhfspFourVectorSolution) first.get(0)),
        V35ExactEvaluationBudget.genotypeFingerprint((DhhfspFourVectorSolution) second.get(0)));
    assertEquals(V35ExactEvaluationBudget.genotypeFingerprint(
            (DhhfspFourVectorSolution) first.get(1)),
        V35ExactEvaluationBudget.genotypeFingerprint((DhhfspFourVectorSolution) second.get(1)));

    V35FourVectorVariation.Mutation firstMutation = mutation(problem, 77L);
    V35FourVectorVariation.Mutation secondMutation = mutation(problem, 77L);
    DhhfspFourVectorSolution low = (DhhfspFourVectorSolution) problem.createSolution();
    DhhfspFourVectorSolution high = (DhhfspFourVectorSolution) low.copy();
    low.setObjective(0, 0.0);
    high.setObjective(0, 1.0e9);
    PermutationSolution<Integer> x = firstMutation.execute(low);
    PermutationSolution<Integer> y = secondMutation.execute(high);
    assertEquals(V35ExactEvaluationBudget.genotypeFingerprint((DhhfspFourVectorSolution) x),
        V35ExactEvaluationBudget.genotypeFingerprint((DhhfspFourVectorSolution) y));
    assertNotEquals(V35ExactEvaluationBudget.genotypeFingerprint(a),
        V35ExactEvaluationBudget.genotypeFingerprint((DhhfspFourVectorSolution) x));
  }

  @Test
  public void budgetRejectsDuplicateEvaluationOfUnchangedCandidate() throws Exception {
    ZhangBoCanonicalProductionProblem problem = problem();
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < 10; i++) initial.add(problem.createSolution());
    V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(problem,
        P8InitialPopulationProvider.copy(initial),
        V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE, 10);
    PermutationSolution<Integer> solution = adapter.createSolution();
    adapter.evaluate(solution);
    int before = adapter.getBudget().getDuplicateEvaluations();
    try {
      adapter.evaluate(solution);
      fail("duplicate evaluation of an unchanged candidate must be rejected");
    } catch (IllegalStateException expected) {
      assertEquals(before + 1, adapter.getBudget().getDuplicateEvaluations());
    }
  }

  private static void assertValidPermutation(PermutationSolution<Integer> solution) {
    int length = solution.getNumberOfVariables();
    Set<Integer> seen = new HashSet<>();
    for (int i = 0; i < length; i++) {
      int job = solution.getVariableValue(i);
      assertTrue("job out of range: " + job, job >= 0 && job < length);
      assertTrue("duplicate job: " + job, seen.add(job));
    }
    assertEquals(length, seen.size());
  }

  private static void assertRepresentationLegal(
      ZhangBoCanonicalProductionProblem problem, DhhfspFourVectorSolution solution) {
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      int factory = solution.getFactoryAssignments().get(position);
      assertTrue("factory out of range: " + factory,
          factory >= 0 && factory < problem.getInstance().getFactories());
      int machine = solution.getMachineAssignments().get(position);
      assertTrue("machine out of range for factory " + factory,
          machine >= 0 && machine < problem.getInstance().getMachineCount(factory, 0));
      int worker = solution.getWorkerAssignments().get(position);
      assertTrue("worker not eligible in factory " + factory,
          problem.getInstance().isWorkerEligible(factory, 0, worker));
    }
  }

  private static List<PermutationSolution<Integer>> parents(
      PermutationSolution<Integer> a, PermutationSolution<Integer> b) {
    List<PermutationSolution<Integer>> parents = new ArrayList<>();
    parents.add(a);
    parents.add(b);
    return parents;
  }

  private static ZhangBoCanonicalProductionProblem problem() throws Exception {
    Path root = javaProject();
    return ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
  }

  private static V35FourVectorVariation.Crossover crossover(
      ZhangBoCanonicalProductionProblem problem, long seed) {
    return new V35FourVectorVariation.Crossover(0.40, 0.30, 0.30, 0.40,
        new ZhangBoResourceDomain(problem.getInstance()), new JavaRandomGenerator(seed));
  }

  private static V35FourVectorVariation.Mutation mutation(
      ZhangBoCanonicalProductionProblem problem, long seed) {
    return new V35FourVectorVariation.Mutation(0.30, 0.04, 0.15, 0.15,
        new ZhangBoResourceDomain(problem.getInstance()), new JavaRandomGenerator(seed));
  }

  static Path javaProject() {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("EADHFSP"))) return cwd;
    if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("EADHFSP"))) {
      return cwd.getParent();
    }
    throw new IllegalStateException("cannot locate java project from " + cwd);
  }
}
