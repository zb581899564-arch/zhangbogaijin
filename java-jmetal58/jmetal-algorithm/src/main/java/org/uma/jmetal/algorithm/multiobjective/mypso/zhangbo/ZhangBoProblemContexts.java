package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;

/** Resolves the algorithm-side problem contract while retaining a legacy bridge. */
public final class ZhangBoProblemContexts {
  private ZhangBoProblemContexts() { }

  public static ZhangBoProblemContext resolve(Problem<?> problem) {
    if (problem instanceof ZhangBoProblemContext) {
      return (ZhangBoProblemContext) problem;
    }
    if (problem instanceof ZhangBoCanonicalProductionProblem) {
      return new CanonicalContext((ZhangBoCanonicalProductionProblem) problem);
    }
    if (problem instanceof ZhangBoEDHHFSPW) {
      return new LegacyContext((ZhangBoEDHHFSPW) problem);
    }
    return null;
  }

  private static final class LegacyContext implements ZhangBoProblemContext {
    private static final long serialVersionUID = 1L;
    private final ZhangBoEDHHFSPW problem;

    private LegacyContext(ZhangBoEDHHFSPW problem) {
      this.problem = problem;
    }

    @Override
    public ZhangBoFatigueInstanceData getFatigueInstanceData() {
      return problem.getFatigueInstanceData();
    }

    @Override
    public ZhangBoFatigueParameters getFatigueParameters() {
      return problem.getFatigueParameters();
    }
  }

  private static final class CanonicalContext implements ZhangBoProblemContext {
    private static final long serialVersionUID = 1L;
    private final ZhangBoCanonicalProductionProblem problem;

    private CanonicalContext(ZhangBoCanonicalProductionProblem problem) {
      this.problem = problem;
    }

    @Override
    public ZhangBoFatigueInstanceData getFatigueInstanceData() {
      return problem.getInstance();
    }

    @Override
    public ZhangBoFatigueParameters getFatigueParameters() {
      return problem.getParameters();
    }
  }
}
