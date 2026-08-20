package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;

/** FE-only compatibility adapter; it never changes search decisions. */
public final class V35P25EBudgetAwareEvaluator
    implements SolutionListEvaluator<PermutationSolution<Integer>> {
  private static final long serialVersionUID = 1L;

  @Override
  public List<PermutationSolution<Integer>> evaluate(
      List<PermutationSolution<Integer>> solutions,
      Problem<PermutationSolution<Integer>> genericProblem) {
    if (!(genericProblem instanceof V35ComparisonProblemAdapter)) {
      throw new IllegalArgumentException("P25E evaluator requires comparison problem");
    }
    V35ComparisonProblemAdapter problem = (V35ComparisonProblemAdapter) genericProblem;
    List<PermutationSolution<Integer>> pending = new ArrayList<>();
    for (PermutationSolution<Integer> solution : solutions) {
      if (!(solution instanceof DhhfspFourVectorSolution)
          && !(solution instanceof V35ComparisonSolution)) {
        throw new IllegalArgumentException("P25E evaluator requires four-vector solutions");
      }
      if (!problem.getBudget().isAlreadyEvaluatedUnchanged(solution)) pending.add(solution);
    }
    if (problem.getBudget().getRemaining() < pending.size()) {
      throw new ExactBudgetStop(problem.getBudget().getEvaluations(), pending.size());
    }
    for (PermutationSolution<Integer> solution : pending) problem.evaluate(solution);
    return solutions;
  }

  @Override public void shutdown() { }

  /** Raised before a whole evaluation batch; no half-batch can be produced. */
  public static final class ExactBudgetStop extends RuntimeException {
    private final int evaluations;
    private final int requested;
    private ExactBudgetStop(int evaluations, int requested) {
      super("exact budget stop at " + evaluations + ", next batch=" + requested);
      this.evaluations = evaluations; this.requested = requested;
    }
    public int getEvaluations() { return evaluations; }
    public int getRequested() { return requested; }
  }
}
