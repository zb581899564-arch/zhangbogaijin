package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/** Immutable neighborhood outcome, including applicability and exact evaluation cost. */
public final class ZhangBoNeighborhoodResult {
  private final ZhangBoNeighborhoodId id;
  private final boolean applicable;
  private final String reason;
  private final int generatedCandidates;
  private final int completeEvaluations;
  private final long elapsedNanos;
  private final List<String> diagnostics;
  private final PermutationSolution<Integer> selected;

  public ZhangBoNeighborhoodResult(
      ZhangBoNeighborhoodId id, boolean applicable, String reason,
      int generatedCandidates, int completeEvaluations, long elapsedNanos,
      List<String> diagnostics, PermutationSolution<Integer> selected) {
    this.id = id;
    this.applicable = applicable;
    this.reason = reason;
    this.generatedCandidates = generatedCandidates;
    this.completeEvaluations = completeEvaluations;
    this.elapsedNanos = elapsedNanos;
    this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    this.selected = selected;
  }

  public ZhangBoNeighborhoodId getId() { return id; }
  public boolean isApplicable() { return applicable; }
  public String getReason() { return reason; }
  public int getGeneratedCandidates() { return generatedCandidates; }
  public int getCompleteEvaluations() { return completeEvaluations; }
  public long getElapsedNanos() { return elapsedNanos; }
  public List<String> getDiagnostics() { return diagnostics; }
  public PermutationSolution<Integer> getSelected() { return selected; }
}
