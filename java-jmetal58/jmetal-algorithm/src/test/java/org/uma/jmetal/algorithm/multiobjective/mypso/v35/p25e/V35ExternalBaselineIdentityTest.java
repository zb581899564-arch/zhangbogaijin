package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.algorithm.multiobjective.spea2.util.EnvironmentalSelection;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.operator.impl.selection.RankingAndCrowdingSelection;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;
import org.uma.jmetal.util.solutionattribute.impl.StrengthRawFitness;

/**
 * Algorithm-identity gates: the external baselines must really execute the official
 * jMetal 5.8 search machinery (binary tournament, four-vector crossover/mutation,
 * fast nondominated sorting + crowding distance for NSGA-II; strength, raw fitness,
 * density, archive and truncation for SPEA2) with counters greater than zero, on the
 * three-objective view only.
 */
public class V35ExternalBaselineIdentityTest {
  private static final int POPULATION = 20;
  private static final int BUDGET = 400;
  private static final long SEED = 20260822L;

  @Test
  public void nsga2RunsOfficialMachineryWithPositiveIdentityEvents() throws Exception {
    Fixture fixture = fixture();
    long[] counters = new long[3];
    V35P25EAlgorithmResult result = V35P25EOfficialJMetalEngine.run(
        V35P25EOfficialJMetalEngine.Algorithm.NSGA_II_F, fixture.adapter, POPULATION, BUDGET,
        SEED, countingCrossover(fixture, counters), countingMutation(fixture, counters),
        countingTournament(counters, true));
    assertTrue("crossover calls must be positive", counters[0] > 0);
    assertTrue("mutation calls must be positive", counters[1] > 0);
    assertTrue("tournament calls must be positive", counters[2] > 0);
    assertEquals(BUDGET, result.getEvaluations());
    assertTrue(result.getFront().isEmpty() == false);
    assertTrue(result.getIdentityEvidence().contains("binaryTournament=true"));
    assertTrue(result.getIdentityEvidence().contains("ranking=true"));
    assertTrue(result.getIdentityEvidence().contains("crowdingDistance=true"));
    for (double[] point : result.getFront()) {
      for (double value : point) {
        assertTrue("non-finite objective", Double.isFinite(value));
      }
    }
  }

  @Test
  public void spea2RunsOfficialMachineryWithPositiveIdentityEvents() throws Exception {
    Fixture fixture = fixture();
    long[] counters = new long[3];
    V35P25EAlgorithmResult result = V35P25EOfficialJMetalEngine.run(
        V35P25EOfficialJMetalEngine.Algorithm.SPEA2_F, fixture.adapter, POPULATION, BUDGET,
        SEED, countingCrossover(fixture, counters), countingMutation(fixture, counters),
        countingTournament(counters, false));
    assertTrue("crossover calls must be positive", counters[0] > 0);
    assertTrue("mutation calls must be positive", counters[1] > 0);
    assertTrue("tournament calls must be positive", counters[2] > 0);
    assertEquals(BUDGET, result.getEvaluations());
    assertTrue(result.getFront().isEmpty() == false);
    assertTrue(result.getIdentityEvidence().contains("strengthRawFitness=true"));
    assertTrue(result.getIdentityEvidence().contains("archive=true"));
    assertTrue(result.getIdentityEvidence().contains("environmentalSelection=SPEA2"));
  }

  @Test
  public void nsga2EnvironmentalSelectionAppliesRankingAndCrowding() throws Exception {
    Fixture fixture = fixture(2 * POPULATION);
    List<PermutationSolution<Integer>> union = evaluatedUnion(fixture, 2 * POPULATION);
    int attributesBefore = totalAttributes(union);
    RankingAndCrowdingSelection<PermutationSolution<Integer>> environmentalSelection =
        new RankingAndCrowdingSelection<PermutationSolution<Integer>>(POPULATION,
            new org.uma.jmetal.util.comparator.DominanceComparator<PermutationSolution<Integer>>());
    List<PermutationSolution<Integer>> survivors = environmentalSelection.execute(union);
    assertEquals(POPULATION, survivors.size());
    assertTrue("environmental selection must annotate solutions (ranking + crowding)",
        totalAttributes(union) > attributesBefore);
  }

  @Test
  public void spea2EnvironmentalSelectionAppliesStrengthDensityAndTruncation() throws Exception {
    Fixture fixture = fixture(2 * POPULATION);
    List<PermutationSolution<Integer>> union = evaluatedUnion(fixture, 2 * POPULATION);
    StrengthRawFitness<PermutationSolution<Integer>> estimator =
        new StrengthRawFitness<PermutationSolution<Integer>>();
    estimator.computeDensityEstimator(union);
    for (PermutationSolution<Integer> solution : union) {
      assertTrue("strength/raw fitness/density must be recorded on every solution",
          estimator.getAttribute(solution) != null);
    }
    EnvironmentalSelection<PermutationSolution<Integer>> environmentalSelection =
        new EnvironmentalSelection<PermutationSolution<Integer>>(POPULATION);
    List<PermutationSolution<Integer>> archive = environmentalSelection.execute(union);
    assertEquals("SPEA2 truncation must cut the union to the archive size",
        POPULATION, archive.size());
  }

  @Test
  public void engineRejectsSevenSlotView() throws Exception {
    Fixture fixture = sevenSlotFixture();
    try {
      V35P25EOfficialJMetalEngine.run(V35P25EOfficialJMetalEngine.Algorithm.NSGA_II_F,
          fixture.adapter, POPULATION, BUDGET, SEED);
      throw new AssertionError("official cores must reject the seven-slot view");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("THREE_OBJECTIVE"));
    }
  }

  private static List<PermutationSolution<Integer>> evaluatedUnion(
      Fixture fixture, int count) {
    List<PermutationSolution<Integer>> union = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      PermutationSolution<Integer> solution = fixture.adapter.createSolution();
      fixture.adapter.evaluate(solution);
      union.add(solution);
    }
    return union;
  }

  private static int totalAttributes(List<PermutationSolution<Integer>> union) {
    int total = 0;
    for (PermutationSolution<Integer> solution : union) {
      total += solution.getAttributes().size();
    }
    return total;
  }

  private static CrossoverOperator<PermutationSolution<Integer>> countingCrossover(
      Fixture fixture, long[] counters) {
    final V35FourVectorVariation.Crossover delegate =
        new V35FourVectorVariation.Crossover(0.40, 0.30, 0.30, 0.40,
            new ZhangBoResourceDomain(fixture.problem.getInstance()),
            new JavaRandomGenerator(V35P25EOfficialJMetalEngine.domainSeed(SEED, 1)));
    return new CrossoverOperator<PermutationSolution<Integer>>() {
      @Override public List<PermutationSolution<Integer>> execute(
          List<PermutationSolution<Integer>> source) {
        counters[0]++;
        return delegate.execute(source);
      }
      @Override public int getNumberOfRequiredParents() {
        return delegate.getNumberOfRequiredParents();
      }
      @Override public int getNumberOfGeneratedChildren() {
        return delegate.getNumberOfGeneratedChildren();
      }
      @Override public int getCrossoverProbabilityflag() {
        return delegate.getCrossoverProbabilityflag();
      }
    };
  }

  private static MutationOperator<PermutationSolution<Integer>> countingMutation(
      Fixture fixture, long[] counters) {
    final V35FourVectorVariation.Mutation delegate =
        new V35FourVectorVariation.Mutation(0.30, 0.04, 0.15, 0.15,
            new ZhangBoResourceDomain(fixture.problem.getInstance()),
            new JavaRandomGenerator(V35P25EOfficialJMetalEngine.domainSeed(SEED, 2)));
    return new MutationOperator<PermutationSolution<Integer>>() {
      @Override public PermutationSolution<Integer> execute(PermutationSolution<Integer> s) {
        counters[1]++;
        return delegate.execute(s);
      }
      @Override public int getMutationProbabilityflag() {
        return delegate.getMutationProbabilityflag();
      }
    };
  }

  private static SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>>
      countingTournament(long[] counters, final boolean rankingComparator) {
    final BinaryTournamentSelection<PermutationSolution<Integer>> delegate =
        rankingComparator
            ? new BinaryTournamentSelection<PermutationSolution<Integer>>(
                new RankingAndCrowdingDistanceComparator<PermutationSolution<Integer>>())
            : new BinaryTournamentSelection<PermutationSolution<Integer>>();
    return new SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>>() {
      @Override public PermutationSolution<Integer> execute(
          List<PermutationSolution<Integer>> source) {
        counters[2]++;
        return delegate.execute(source);
      }
    };
  }

  private static Fixture fixture() throws Exception {
    return fixture(POPULATION);
  }

  private static Fixture fixture(int frozenSize) throws Exception {
    Path root = V35ExternalBaselineRepresentationTest.javaProject();
    org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem problem =
        org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader.load(
            root.resolve("EADHFSP/20_2_3_1.txt"),
            org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.FM3, SEED,
            root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
            org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < frozenSize; i++) initial.add(problem.createSolution());
    V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(problem,
        org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider.copy(initial),
        V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE, BUDGET);
    return new Fixture(problem, adapter);
  }

  private static Fixture sevenSlotFixture() throws Exception {
    Path root = V35ExternalBaselineRepresentationTest.javaProject();
    org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem problem =
        org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader.load(
            root.resolve("EADHFSP/20_2_3_1.txt"),
            org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.FM3, SEED,
            root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
            org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) initial.add(problem.createSolution());
    V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(problem,
        org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider.copy(initial),
        V35ComparisonProblemAdapter.ObjectiveView.AUTHOR_SEVEN_SLOT, BUDGET);
    return new Fixture(problem, adapter);
  }

  private static final class Fixture {
    private final org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem
        problem;
    private final V35ComparisonProblemAdapter adapter;

    private Fixture(
        org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem problem,
        V35ComparisonProblemAdapter adapter) {
      this.problem = problem;
      this.adapter = adapter;
    }
  }
}
