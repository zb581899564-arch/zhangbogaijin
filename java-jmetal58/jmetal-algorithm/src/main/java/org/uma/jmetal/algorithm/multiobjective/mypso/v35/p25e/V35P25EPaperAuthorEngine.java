package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.List;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.mymohea.P25EAuthorMOHEADE;
import org.uma.jmetal.algorithm.multiobjective.mypso.P25EAuthorMOPSO;
import org.uma.jmetal.algorithm.multiobjective.mypso.P25EAuthorMOPSODivSub;
import org.uma.jmetal.algorithm.multiobjective.mypso.P25EAuthorMOPSODivSubDE;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.PMXCrossover;
import org.uma.jmetal.operator.impl.mutation.PermutationSwapMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs isolated copies of the paper-author search cores. The copies differ
 * from their sources only at the approved Problem, initial-population,
 * random-source, FE-stop and logging boundaries.
 */
public final class V35P25EPaperAuthorEngine {
  public enum AlgorithmKind {
    HMOPSO_QLS_F, MOPSO_F, MOPSODS_DE_F, MOHEADE_F
  }

  private V35P25EPaperAuthorEngine() { }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static V35P25EAlgorithmResult run(AlgorithmKind kind,
      V35ComparisonProblemAdapter problem, int populationSize,
      int maxEvaluations, long seed) {
    if (kind == null || problem == null
        || problem.getObjectiveView() != V35ComparisonProblemAdapter.ObjectiveView.AUTHOR_SEVEN_SLOT
        || populationSize != 100 || maxEvaluations < populationSize) {
      throw new IllegalArgumentException("invalid paper-author P25E request");
    }
    JMetalRandom.getInstance().setSeed(seed);
    V35P25EAuthorRuntime.install(seed, problem.getCanonicalProblem().getInstance());
    SolutionListEvaluator<PermutationSolution<Integer>> evaluator =
        new V35P25EBudgetAwareEvaluator();
    Algorithm<List<PermutationSolution<Integer>>> core;
    String identity;
    try {
      if (kind == AlgorithmKind.MOPSO_F) {
        core = new P25EAuthorMOPSO(
            0.30, 0.06, 0.50, problem, evaluator,
            populationSize, maxEvaluations, populationSize,
            0.40, 0.30, 0.20, 0.20);
        identity = "velocity=true;pbest=true;gbest=true;externalArchive=Pgd;"
            + "paperParams=r0.5,lv2-4=0.3/0.3/0.4,mv2-4=0.06/0.2/0.2";
      } else if (kind == AlgorithmKind.MOPSODS_DE_F) {
        // Table 9 grouping M3 = 20/20/20/40, expressed in the author's
        // physical order up/central/down/upNew.
        core = new P25EAuthorMOPSODivSub(
            problem.getCanonicalProblem().getInstance().getFactories(),
            0.20, 0.06, 0.40, problem, evaluator,
            populationSize, maxEvaluations, 20, 40, 20, 20,
            0.50, 0.30, 0.40, 0.20, 0.15);
        identity = "paperClass=MOPSODivSub;grouping=M3;subgroups=20/40/20/20;"
            + "paperLabel=MOPSODS-DE";
      } else if (kind == AlgorithmKind.HMOPSO_QLS_F) {
        P25EAuthorMOPSODivSubDE.GbestsetG1.clear();
        P25EAuthorMOPSODivSubDE.GbestsetG2.clear();
        P25EAuthorMOPSODivSubDE.GbestsetG3.clear();
        P25EAuthorMOPSODivSubDE.GbestsetG4.clear();
        // Table 9 grouping M2 = 15/15/15/55; Q=50, LS=40,
        // gamma=0.9 and epsilon=0.8 follow the author's constructor wiring.
        core = new P25EAuthorMOPSODivSubDE(
            problem.getCanonicalProblem().getInstance().getFactories(),
            0.20, 0.08, 0.50, problem, evaluator,
            populationSize, maxEvaluations, 15, 55, 15, 15,
            0.0, 0.90, 0.80, 50.0,
            0.40, 0.50, 0.15, 0.15, 40);
        identity = "paperClass=MOPSODivSubDE;grouping=M2;QTimes=50;LSTimes=40;"
            + "gamma=0.9;epsilon=0.8";
      } else {
        CrossoverOperator<PermutationSolution<Integer>> crossover =
            new PMXCrossover(0.50);
        MutationOperator<PermutationSolution<Integer>> mutation =
            new PermutationSwapMutation<Integer>(0.40);
        SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>>
            selection = new BinaryTournamentSelection<PermutationSolution<Integer>>(
                new RankingAndCrowdingDistanceComparator<PermutationSolution<Integer>>());
        core = (Algorithm) new P25EAuthorMOHEADE<PermutationSolution<Integer>>(
            problem, maxEvaluations, populationSize, crossover, mutation,
            selection, evaluator, 100, 100, 0.80,
            0.30, 0.08, 0.40, 0.50, 0.20, 0.20);
        identity = "paperClass=mymohea.MOHEADE;elite=100;VEGA=100;DE=0.8;"
            + "PMX=0.5;swapMutation=0.4";
      }

      long start = System.nanoTime();
      try {
        core.run();
      } catch (V35P25EBudgetAwareEvaluator.ExactBudgetStop stop) {
        // The author cores evaluate structural groups as indivisible batches.
        // Stop before a partial group rather than exceed the common FE budget.
      }
      int evaluations = problem.getBudget().getEvaluations();
      if (evaluations > maxEvaluations || evaluations < populationSize) {
        throw new IllegalStateException(kind + " invalid FE closure=" + evaluations
            + "/" + maxEvaluations);
      }
      List<PermutationSolution<Integer>> result =
          SolutionListUtils.getNondominatedSolutions(core.getResult());
      identity += ";randomObjects=" + V35P25EAuthorRuntime.randomObjectsCreated()
          + ";suppressedConsoleLines=" + V35P25EAuthorRuntime.suppressedConsoleLines()
          + ";runtimeEvents=" + V35P25EAuthorRuntime.eventSummary()
          + ";budgetStop=" + (evaluations == maxEvaluations ? "EXACT" : "WHOLE_BATCH_SHORT");
      return new V35P25EAlgorithmResult(kind.name(), "PAPER_AUTHOR_SOURCE",
          core.getClass().getName(), evaluations, System.nanoTime() - start,
          result, identity);
    } finally {
      V35P25EAuthorRuntime.clear();
    }
  }

  public static String canonicalParameters(AlgorithmKind kind) {
    if (kind == AlgorithmKind.MOPSO_F) {
      return "pop=100;r=0.5;lv1=-;lv2=0.3;lv3=0.3;lv4=0.4;"
          + "mv1=-;mv2=0.06;mv3=0.2;mv4=0.2";
    }
    if (kind == AlgorithmKind.MOPSODS_DE_F) {
      return "pop=100;group=M3;r=0.4;lv1=0.5;lv2=0.2;lv3=0.4;lv4=0.3;"
          + "mv1=0.4;mv2=0.06;mv3=0.15;mv4=0.2;DE=0.8";
    }
    if (kind == AlgorithmKind.HMOPSO_QLS_F) {
      return "pop=100;group=M2;r=0.5;lv1=-;lv2=0.2;lv3=0.5;lv4=0.4;"
          + "mv1=-;mv2=0.08;mv3=0.15;mv4=0.15;Q=50;LS=40;gamma=0.9;epsilon=0.8";
    }
    return "pop=100;elite=100;lv1=0.5;lv2=0.3;lv3=0.5;lv4=0.4;"
        + "mv1=0.4;mv2=0.08;mv3=0.2;mv4=0.2;DE=0.8";
  }
}
