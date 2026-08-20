package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;

import java.io.Serializable;

/** One immutable CFVF update result. */
public final class ZhangBoCfvfResult implements Serializable {
  private static final long serialVersionUID = 1L;
  private final PermutationSolution<Integer> solution;
  private final ZhangBoResourceVelocity velocity;
  private final ZhangBoCfvfDiagnostics diagnostics;

  public ZhangBoCfvfResult(
      PermutationSolution<Integer> solution,
      ZhangBoResourceVelocity velocity,
      ZhangBoCfvfDiagnostics diagnostics) {
    this.solution = solution;
    this.velocity = velocity;
    this.diagnostics = diagnostics;
  }

  public PermutationSolution<Integer> getSolution() { return solution; }
  public ZhangBoResourceVelocity getVelocity() { return velocity; }
  public ZhangBoCfvfDiagnostics getDiagnostics() { return diagnostics; }
}
