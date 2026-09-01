package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.Iterator;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/** Stable incremental equivalent of the legacy [0,1,6] archive scan. */
public final class ZhangBoIncrementalParetoArchive {
  public enum Disposition { ADDED, REJECTED_DOMINATED, REJECTED_EQUAL }

  /** Observation result; it does not expose or mutate archive members. */
  public static final class Update {
    private final Disposition disposition;
    private final int removedDominated;

    private Update(Disposition disposition, int removedDominated) {
      this.disposition = disposition;
      this.removedDominated = removedDominated;
    }

    public Disposition getDisposition() { return disposition; }
    public int getRemovedDominated() { return removedDominated; }
  }

  private ZhangBoIncrementalParetoArchive() { }

  public static void add(List<PermutationSolution<Integer>> archive,
      PermutationSolution<Integer> candidate) {
    addWithReport(archive, candidate);
  }

  /** Same stable update as {@link #add}, with an observation-only outcome. */
  public static Update addWithReport(List<PermutationSolution<Integer>> archive,
      PermutationSolution<Integer> candidate) {
    for (PermutationSolution<Integer> existing : archive) {
      if (sameObjectives(existing, candidate)) {
        return new Update(Disposition.REJECTED_EQUAL, 0);
      }
      if (weaklyDominates(existing, candidate)) {
        return new Update(Disposition.REJECTED_DOMINATED, 0);
      }
    }
    int removed = 0;
    Iterator<PermutationSolution<Integer>> iterator = archive.iterator();
    while (iterator.hasNext()) {
      if (weaklyDominates(candidate, iterator.next())) {
        iterator.remove();
        removed++;
      }
    }
    archive.add(candidate);
    return new Update(Disposition.ADDED, removed);
  }

  private static boolean sameObjectives(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    return Double.doubleToLongBits(left.getObjective(0))
            == Double.doubleToLongBits(right.getObjective(0))
        && Double.doubleToLongBits(left.getObjective(1))
            == Double.doubleToLongBits(right.getObjective(1))
        && Double.doubleToLongBits(left.getObjective(6))
            == Double.doubleToLongBits(right.getObjective(6));
  }

  static boolean weaklyDominates(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    return left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
  }
}
