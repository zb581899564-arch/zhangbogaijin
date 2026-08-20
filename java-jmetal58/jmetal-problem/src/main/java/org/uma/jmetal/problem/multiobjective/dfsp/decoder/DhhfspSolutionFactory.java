package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.io.Serializable;

/** P3 does not define population initialization; callers inject the solution factory. */
public interface DhhfspSolutionFactory extends Serializable {
  DhhfspFourVectorSolution create();
}
