package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-P17 passive evaluation archive: a read-only, fully-evaluated,
 * nondominated history of every candidate observed in the formal run.
 *
 * Passive by construction: {@link #observe} only stores defensive copies and
 * performs Pareto maintenance on its own members; no method writes back to the
 * algorithm, consumes randomness, or participates in any search decision.  The
 * only consumers are evidence exporters and tests.
 */
public final class V35PassiveEvaluationArchive implements Serializable {
  private static final long serialVersionUID = 1L;

  private final List<PermutationSolution<Integer>> members = new ArrayList<>();
  private long observedCount;

  /** Observes one evaluated candidate; Pareto-maintained, copy-only insert. */
  @SuppressWarnings("unchecked")
  public void observe(PermutationSolution<Integer> evaluated) {
    if (evaluated == null) throw new IllegalArgumentException("evaluated");
    observedCount++;
    double[] objectives = {evaluated.getObjective(0), evaluated.getObjective(1),
        evaluated.getObjective(6)};
    List<PermutationSolution<Integer>> dominatedMembers = new ArrayList<>();
    for (PermutationSolution<Integer> member : members) {
      double[] memberObjectives = {member.getObjective(0), member.getObjective(1),
          member.getObjective(6)};
      if (strictlyDominates(memberObjectives, objectives)) {
        return; // the candidate is dominated: never admitted.
      }
      if (strictlyDominates(objectives, memberObjectives)) {
        dominatedMembers.add(member);
      }
    }
    members.removeAll(dominatedMembers);
    members.add((PermutationSolution<Integer>) evaluated.copy());
  }

  private static boolean strictlyDominates(double[] left, double[] right) {
    boolean strict = false;
    for (int index = 0; index < left.length; index++) {
      if (left[index] > right[index]) return false;
      if (left[index] < right[index]) strict = true;
    }
    return strict;
  }

  /** Defensive snapshot; mutating the returned list/solutions cannot affect the archive. */
  @SuppressWarnings("unchecked")
  public List<PermutationSolution<Integer>> snapshot() {
    List<PermutationSolution<Integer>> copy = new ArrayList<>();
    for (PermutationSolution<Integer> member : members) {
      copy.add((PermutationSolution<Integer>) member.copy());
    }
    return Collections.unmodifiableList(copy);
  }

  public int size() { return members.size(); }
  public long getObservedCount() { return observedCount; }

  public String toCsv() {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (PermutationSolution<Integer> member : members) {
      out.append(member.getObjective(0)).append(',')
          .append(member.getObjective(1)).append(',')
          .append(member.getObjective(6)).append('\n');
    }
    return out.toString();
  }

  public String statistics() {
    return "observedCount=" + observedCount + ",archiveSize=" + members.size()
        + ",retentionRate=" + (observedCount == 0L
            ? Double.NaN : (double) members.size() / observedCount);
  }
}
