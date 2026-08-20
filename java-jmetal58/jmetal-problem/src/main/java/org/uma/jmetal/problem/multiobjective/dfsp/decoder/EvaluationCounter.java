package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;

/** Counts successful full problem evaluations, not direct decoder calls. */
public final class EvaluationCounter implements Serializable {
  private static final long serialVersionUID = 1L;
  private long successfulEvaluations;

  public synchronized void recordSuccessfulEvaluation() {
    successfulEvaluations++;
  }

  public synchronized long getSuccessfulEvaluations() {
    return successfulEvaluations;
  }
}
