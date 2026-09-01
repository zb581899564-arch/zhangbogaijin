package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58NSGAII;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58SPEA2;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/** Runs only the isolated official jMetal 5.8 NSGA-II and SPEA2 cores. */
public final class V35P25EOfficialJMetalEngine {
  public enum Algorithm { NSGA_II_F, SPEA2_F }
  public static final String UPSTREAM_TAG = "jmetal-5.8";
  public static final String UPSTREAM_COMMIT =
      "831d62d0bbf384e1770efc1bb6eef69ce0ce75b9";

  private V35P25EOfficialJMetalEngine() { }

  public static V35P25EAlgorithmResult run(Algorithm algorithm,
      V35ComparisonProblemAdapter problem, int populationSize, int maxEvaluations,
      long seed) {
    return run(algorithm, problem, populationSize, maxEvaluations, seed, null, null, null);
  }

  /**
   * Operator-injection overload for external fair-baseline instrumentation.
   * Search semantics are identical to {@link #run}: null overrides fall back to the
   * canonical P25E operators, and the decoder FE contract is unchanged.
   */
  public static V35P25EAlgorithmResult run(Algorithm algorithm,
      V35ComparisonProblemAdapter problem, int populationSize, int maxEvaluations,
      long seed,
      CrossoverOperator<PermutationSolution<Integer>> crossoverOverride,
      MutationOperator<PermutationSolution<Integer>> mutationOverride,
      SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>>
          selectionOverride) {
    if (algorithm == null || problem == null || populationSize <= 0
        || maxEvaluations < populationSize || maxEvaluations % populationSize != 0) {
      throw new IllegalArgumentException("invalid official jMetal P25E request");
    }
    if (problem.getObjectiveView()
        != V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE) {
      throw new IllegalArgumentException(
          "official jMetal cores require the THREE_OBJECTIVE comparison view");
    }
    JMetalRandom.getInstance().setSeed(seed);
    ZhangBoResourceDomain domain = new ZhangBoResourceDomain(
        problem.getCanonicalProblem().getInstance());
    JavaRandomGenerator crossoverRandom = new JavaRandomGenerator(domainSeed(seed, 1));
    JavaRandomGenerator mutationRandom = new JavaRandomGenerator(domainSeed(seed, 2));
    CrossoverOperator<PermutationSolution<Integer>> crossover = crossoverOverride;
    MutationOperator<PermutationSolution<Integer>> mutation = mutationOverride;
    SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>>
        selection = selectionOverride;
    long start = System.nanoTime();
    List<PermutationSolution<Integer>> result;
    String implementation;
    String identity;
    if (algorithm == Algorithm.NSGA_II_F) {
      if (crossover == null) {
        crossover = new V35FourVectorVariation.Crossover(
            0.40, 0.30, 0.30, 0.40, domain, crossoverRandom);
      }
      if (mutation == null) {
        mutation = new V35FourVectorVariation.Mutation(
            0.30, 0.04, 0.15, 0.15, domain, mutationRandom);
      }
      if (selection == null) {
        selection = new BinaryTournamentSelection<PermutationSolution<Integer>>(
            new RankingAndCrowdingDistanceComparator<PermutationSolution<Integer>>());
      }
      OfficialJMetal58NSGAII<PermutationSolution<Integer>> core =
          new OfficialJMetal58NSGAII<>(problem, maxEvaluations, populationSize,
              populationSize, populationSize, crossover, mutation, selection,
              new SequentialSolutionListEvaluator<PermutationSolution<Integer>>());
      core.run();
      result = core.getResult();
      implementation = core.getClass().getName();
      identity = "binaryTournament=true;ranking=true;crowdingDistance=true;"
          + "replacement=RankingAndCrowdingSelection;upstream=" + UPSTREAM_TAG;
    } else {
      if (crossover == null) {
        crossover = new V35FourVectorVariation.Crossover(
            0.50, 0.20, 0.30, 0.30, domain, crossoverRandom);
      }
      if (mutation == null) {
        mutation = new V35FourVectorVariation.Mutation(
            0.30, 0.04, 0.10, 0.15, domain, mutationRandom);
      }
      if (selection == null) {
        selection = new BinaryTournamentSelection<PermutationSolution<Integer>>();
      }
      int iterations = maxEvaluations / populationSize;
      OfficialJMetal58SPEA2<PermutationSolution<Integer>> core =
          new OfficialJMetal58SPEA2<>(problem, iterations, populationSize,
              crossover, mutation, selection,
              new SequentialSolutionListEvaluator<PermutationSolution<Integer>>(), 1);
      core.run();
      result = core.getResult();
      implementation = core.getClass().getName();
      identity = "strengthRawFitness=true;archive=true;"
          + "environmentalSelection=SPEA2;upstream=" + UPSTREAM_TAG;
    }
    int evaluations = (int) problem.getCanonicalProblem().getEvaluationCounter()
        .getSuccessfulEvaluations();
    if (evaluations != maxEvaluations) {
      throw new IllegalStateException(algorithm + " decoder FE " + evaluations
          + " != " + maxEvaluations);
    }
    return new V35P25EAlgorithmResult(algorithm.name(), "OFFICIAL_JMETAL_CORE",
        implementation, evaluations, System.nanoTime() - start, result, identity);
  }

  /** splitmix-style domain separation shared with the external fair-baseline runner. */
  public static long domainSeed(long seed, int domain) {
    long value = seed ^ (0x9E3779B97F4A7C15L * domain);
    value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
    value ^= value >>> 27; value *= 0x94D049BB133111EBL;
    return value ^ (value >>> 31);
  }

  public static String canonicalParameters(Algorithm algorithm) {
    return algorithm == Algorithm.NSGA_II_F
        ? "pop=100;elite=100;lv1=0.4;lv2=0.3;lv3=0.3;lv4=0.4;"
            + "mv1=0.3;mv2=0.04;mv3=0.15;mv4=0.15"
        : "pop=100;elite=100;lv1=0.5;lv2=0.2;lv3=0.3;lv4=0.3;"
            + "mv1=0.3;mv2=0.04;mv3=0.1;mv4=0.15";
  }
}
