package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** Accesses MA for both the canonical four-vector solution and the legacy attribute layout. */
public final class ZhangBoMachineVectorSupport {
  private ZhangBoMachineVectorSupport() { }

  public static List<Integer> copy(PermutationSolution<Integer> solution, int jobs) {
    if (solution == null) throw new IllegalArgumentException("solution");
    if (solution instanceof DhhfspFourVectorSolution) {
      DhhfspFourVectorSolution canonical = (DhhfspFourVectorSolution) solution;
      List<Integer> result = new ArrayList<>(jobs);
      for (int position = 0; position < jobs; position++) {
        result.add(canonical.getMachineAssignment(position));
      }
      return result;
    }
    Object raw = solution.getAttribute("machine");
    if (!(raw instanceof List) || ((List<?>) raw).size() < jobs) {
      throw new IllegalArgumentException("Missing machine vector for "
          + solution.getClass().getName());
    }
    List<Integer> result = new ArrayList<>(jobs);
    for (int position = 0; position < jobs; position++) {
      Object value = ((List<?>) raw).get(position);
      if (!(value instanceof Number)) {
        throw new IllegalArgumentException("Machine vector contains non-numeric value at "
            + position);
      }
      result.add(((Number) value).intValue());
    }
    return result;
  }

  public static void write(
      PermutationSolution<Integer> solution, List<Integer> machines) {
    if (solution == null || machines == null) throw new IllegalArgumentException("solution/machines");
    if (solution instanceof DhhfspFourVectorSolution) {
      DhhfspFourVectorSolution canonical = (DhhfspFourVectorSolution) solution;
      if (machines.size() < canonical.getNumberOfVariables()) {
        throw new IllegalArgumentException("Machine vector does not cover every job");
      }
      for (int position = 0; position < canonical.getNumberOfVariables(); position++) {
        canonical.setMachineAssignment(position, machines.get(position));
      }
      return;
    }
    solution.setAttribute("machine", new ArrayList<>(machines));
  }

  public static int get(PermutationSolution<Integer> solution, int position) {
    return copy(solution, solution.getNumberOfVariables()).get(position);
  }
}
