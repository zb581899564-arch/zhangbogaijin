package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/** Non-evaluating candidate preview used by P7.2 Test-and-Apply. */
public final class ZhangBoNeighborhoodPreview {
  private final ZhangBoNeighborhoodId id;
  private final boolean applicable;
  private final String reason;
  private final List<PermutationSolution<Integer>> candidates;
  private final List<String> diagnostics;

  ZhangBoNeighborhoodPreview(ZhangBoNeighborhoodId id, boolean applicable, String reason,
      List<PermutationSolution<Integer>> candidates, List<String> diagnostics) {
    this.id = id;
    this.applicable = applicable;
    this.reason = reason;
    this.candidates = Collections.unmodifiableList(copy(candidates));
    this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
  }

  public ZhangBoNeighborhoodId getId() { return id; }
  public boolean isApplicable() { return applicable; }
  public String getReason() { return reason; }
  public int getCandidateCount() { return candidates.size(); }
  public List<String> getDiagnostics() { return diagnostics; }

  public List<PermutationSolution<Integer>> getCandidates() { return copy(candidates); }

  public PermutationSolution<Integer> getFirstCandidate() {
    return candidates.isEmpty() ? null : copy(candidates).get(0);
  }

  @SuppressWarnings("unchecked")
  private static List<PermutationSolution<Integer>> copy(List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    if (values != null) {
      for (PermutationSolution<Integer> value : values) {
        result.add((PermutationSolution<Integer>) value.copy());
      }
    }
    return result;
  }
}
