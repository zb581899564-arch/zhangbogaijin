package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.util.IdentityHashMap;
import java.util.Map;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** Exact decoder-call budget and same-object duplicate-evaluation guard. */
public final class V35ExactEvaluationBudget {
  private final int maximum;
  private int evaluations;
  private int duplicateEvaluations;
  private final Map<PermutationSolution<Integer>, String> lastEvaluated = new IdentityHashMap<>();

  public V35ExactEvaluationBudget(int maximum) {
    if (maximum <= 0) throw new IllegalArgumentException("maximum must be positive");
    this.maximum = maximum;
  }

  public synchronized void beforeEvaluation(PermutationSolution<Integer> solution) {
    if (evaluations >= maximum) {
      throw new IllegalStateException("exact decoder budget exhausted at " + evaluations);
    }
    String fingerprint = genotypeFingerprint(solution);
    if (fingerprint.equals(lastEvaluated.get(solution))) {
      duplicateEvaluations++;
      throw new IllegalStateException("same unchanged candidate evaluated twice");
    }
    lastEvaluated.put(solution, fingerprint);
  }

  public synchronized boolean isAlreadyEvaluatedUnchanged(
      PermutationSolution<Integer> solution) {
    return genotypeFingerprint(solution).equals(lastEvaluated.get(solution));
  }

  public synchronized void afterSuccessfulEvaluation() { evaluations++; }
  public int getMaximum() { return maximum; }
  public synchronized int getEvaluations() { return evaluations; }
  public synchronized int getDuplicateEvaluations() { return duplicateEvaluations; }
  public synchronized int getRemaining() { return maximum - evaluations; }

  public static String genotypeFingerprint(DhhfspFourVectorSolution solution) {
    return solution.getJobSequence().toString() + '|'
        + solution.getFactoryAssignments() + '|'
        + solution.getMachineAssignments() + '|'
        + solution.getWorkerAssignments();
  }
  public static String genotypeFingerprint(PermutationSolution<Integer> solution) {
    if (solution instanceof V35ComparisonSolution) {
      return genotypeFingerprint(((V35ComparisonSolution) solution).asFourVector());
    }
    if (solution instanceof DhhfspFourVectorSolution) {
      return genotypeFingerprint((DhhfspFourVectorSolution) solution);
    }
    throw new IllegalArgumentException("P25E budget requires a four-vector solution");
  }
}
