package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import org.uma.jmetal.solution.PermutationSolution;

/** The only P7.1 path allowed to consume a complete evaluation. */
public interface ZhangBoNeighborhoodEvaluationGateway {
  void evaluate(PermutationSolution<Integer> candidate);
  int getEvaluationCount();
}
