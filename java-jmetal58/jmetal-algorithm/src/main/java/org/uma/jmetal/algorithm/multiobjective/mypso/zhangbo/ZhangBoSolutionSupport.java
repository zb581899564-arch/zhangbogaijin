package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.List;

/** Small deep-copy helpers for the author four-vector representation. */
public final class ZhangBoSolutionSupport {
  private ZhangBoSolutionSupport() { }

  @SuppressWarnings("unchecked")
  public static PermutationSolution<Integer> deepCopy(PermutationSolution<Integer> source) {
    if (source == null) throw new IllegalArgumentException("source cannot be null");
    PermutationSolution<Integer> copy = (PermutationSolution<Integer>) source.copy();
    Object machine = source.getAttribute("machine");
    if (machine instanceof List) {
      copy.setAttribute("machine", new ArrayList<Integer>((List<Integer>) machine));
    }
    return copy;
  }

  public static List<PermutationSolution<Integer>> deepCopySolutions(
      List<PermutationSolution<Integer>> source) {
    List<PermutationSolution<Integer>> result = new ArrayList<>(source.size());
    for (PermutationSolution<Integer> solution : source) result.add(deepCopy(solution));
    return result;
  }

  public static List<List<PermutationSolution<Integer>>> deepCopyHistories(
      List<? extends List<PermutationSolution<Integer>>> source) {
    List<List<PermutationSolution<Integer>>> result = new ArrayList<>(source.size());
    for (List<PermutationSolution<Integer>> history : source) {
      result.add(deepCopySolutions(history));
    }
    return result;
  }
}
