package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** Four-vector operations that keep resources attached to job identity during JS moves. */
final class ZhangBoNeighborhoodVectors {
  private ZhangBoNeighborhoodVectors() { }

  @SuppressWarnings("unchecked")
  static PermutationSolution<Integer> copy(PermutationSolution<Integer> source) {
    PermutationSolution<Integer> result = (PermutationSolution<Integer>) source.copy();
    Object machine = result.getAttribute("machine");
    if (machine instanceof List && !(result instanceof DhhfspFourVectorSolution)) {
      result.setAttribute("machine", new ArrayList<>((List<Integer>) machine));
    }
    return result;
  }

  static int positionOfJob(PermutationSolution<Integer> solution, int job) {
    int position = solution.getVariables().indexOf(job);
    if (position < 0) throw new IllegalArgumentException("job is absent from JS: " + job);
    return position;
  }

  static int machine(PermutationSolution<Integer> solution, int position) {
    if (solution instanceof DhhfspFourVectorSolution) {
      return ((DhhfspFourVectorSolution) solution).getMachineAssignment(position);
    }
    Object value = solution.getAttribute("machine");
    if (!(value instanceof List)) throw new IllegalArgumentException("missing machine vector attribute");
    return (Integer) ((List<?>) value).get(position);
  }

  @SuppressWarnings("unchecked")
  static void machine(PermutationSolution<Integer> solution, int position, int value) {
    if (solution instanceof DhhfspFourVectorSolution) {
      ((DhhfspFourVectorSolution) solution).setMachineAssignment(position, value);
      return;
    }
    Object raw = solution.getAttribute("machine");
    if (!(raw instanceof List)) throw new IllegalArgumentException("missing machine vector attribute");
    ((List<Integer>) raw).set(position, value);
  }

  static List<Integer> machines(PermutationSolution<Integer> solution) {
    List<Integer> result = new ArrayList<>();
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      result.add(machine(solution, position));
    }
    return result;
  }

  static void swapBundles(PermutationSolution<Integer> solution, int left, int right) {
    swap(solution.getVariables(), left, right);
    swap(solution.getVariablesid(), left, right);
    swap(solution.getVariablesworker(), left, right);
    int machine = machine(solution, left);
    machine(solution, left, machine(solution, right));
    machine(solution, right, machine);
  }

  static void reverseBundles(PermutationSolution<Integer> solution, int from, int to) {
    while (from < to) swapBundles(solution, from++, to--);
  }

  static void insertBundle(PermutationSolution<Integer> solution, int from, int to) {
    if (from == to) return;
    List<Integer> js = solution.getVariables();
    List<Integer> fa = solution.getVariablesid();
    List<Integer> wa = solution.getVariablesworker();
    List<Integer> ma = machines(solution);
    move(js, from, to);
    move(fa, from, to);
    move(wa, from, to);
    move(ma, from, to);
    for (int index = 0; index < ma.size(); index++) machine(solution, index, ma.get(index));
  }

  static String fingerprint(PermutationSolution<Integer> solution) {
    return solution.getVariables().toString() + '|' + solution.getVariablesid().toString()
        + '|' + machines(solution).toString() + '|' + solution.getVariablesworker().toString();
  }

  static void validateFirstStage(
      PermutationSolution<Integer> solution,
      org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData instance) {
    int length = solution.getNumberOfVariables();
    if (solution.getVariablesid().size() < length || solution.getVariablesworker().size() < length) {
      throw new IllegalArgumentException("four vectors do not cover every JS position");
    }
    List<Integer> sorted = new ArrayList<>(solution.getVariables());
    Collections.sort(sorted);
    for (int i = 0; i < length; i++) {
      if (sorted.get(i) != i) throw new IllegalArgumentException("JS is not a 0-based permutation");
      int factory = solution.getVariableValueid(i);
      int machine = machine(solution, i);
      int worker = solution.getVariableValueworker(i);
      if (factory < 0 || factory >= instance.getFactories()) {
        throw new IllegalArgumentException("illegal FA at position=" + i + ": " + factory);
      }
      if (machine < 0 || machine >= instance.getMachineCount(factory, 0)) {
        throw new IllegalArgumentException("illegal MA at position=" + i + ": " + machine);
      }
      if (!instance.isWorkerEligible(factory, 0, worker)) {
        throw new IllegalArgumentException("illegal WA at position=" + i + ": " + worker);
      }
    }
  }

  private static void move(List<Integer> values, int from, int to) {
    Integer value = values.remove(from);
    values.add(to, value);
  }

  private static void swap(List<Integer> values, int left, int right) {
    Integer value = values.get(left);
    values.set(left, values.get(right));
    values.set(right, value);
  }
}
