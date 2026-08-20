package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.Iterator;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/** Stable incremental equivalent of the legacy [0,1,6] archive scan. */
public final class ZhangBoIncrementalParetoArchive {
  private ZhangBoIncrementalParetoArchive() { }

  public static void add(List<PermutationSolution<Integer>> archive,
      PermutationSolution<Integer> candidate) {
    for (PermutationSolution<Integer> existing : archive) {
      if (weaklyDominates(existing, candidate)) return;
    }
    Iterator<PermutationSolution<Integer>> iterator = archive.iterator();
    while (iterator.hasNext()) {
      if (weaklyDominates(candidate, iterator.next())) iterator.remove();
    }
    archive.add(candidate);
  }

  static boolean weaklyDominates(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right) {
    return left.getObjective(0) <= right.getObjective(0)
        && left.getObjective(1) <= right.getObjective(1)
        && left.getObjective(6) <= right.getObjective(6);
  }
}
