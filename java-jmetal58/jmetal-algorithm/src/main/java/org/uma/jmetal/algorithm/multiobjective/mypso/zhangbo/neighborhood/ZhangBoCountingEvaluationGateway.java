package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.Objects;
import java.util.function.Consumer;
import org.uma.jmetal.solution.PermutationSolution;

/** Counting wrapper used to prove the exact FE cost of a neighborhood call. */
public final class ZhangBoCountingEvaluationGateway
    implements ZhangBoNeighborhoodEvaluationGateway {
  private final Consumer<PermutationSolution<Integer>> evaluator;
  private int evaluationCount;

  public ZhangBoCountingEvaluationGateway(Consumer<PermutationSolution<Integer>> evaluator) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
  }

  @Override
  public void evaluate(PermutationSolution<Integer> candidate) {
    evaluator.accept(candidate);
    evaluationCount++;
  }

  @Override
  public int getEvaluationCount() {
    return evaluationCount;
  }
}
