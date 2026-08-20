package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/**
 * Canonical generic implementation of the two inherited inter-factory searches.
 * It mirrors the P4 paper oracle while supporting both canonical and legacy four-vector
 * carriers through {@link ZhangBoMachineVectorSupport}.
 */
public final class ZhangBoCriticalFactoryNeighborhoods {
  private final ZhangBoResourceDomain domain;
  private final PseudoRandomGenerator random;

  public ZhangBoCriticalFactoryNeighborhoods(
      ZhangBoResourceDomain domain, PseudoRandomGenerator random) {
    if (domain == null || random == null) throw new IllegalArgumentException("domain/random");
    this.domain = domain;
    this.random = random;
  }

  public PermutationSolution<Integer> swap(
      PermutationSolution<Integer> source, int maximumFactory, int minimumFactory) {
    PermutationSolution<Integer> candidate = ZhangBoSolutionSupport.deepCopy(source);
    List<Integer> maximum = positions(candidate, maximumFactory);
    List<Integer> minimum = positions(candidate, minimumFactory);
    if (maximumFactory == minimumFactory || maximum.isEmpty() || minimum.isEmpty()) {
      return candidate;
    }
    int left = maximum.get(random.nextInt(0, maximum.size() - 1));
    int right = minimum.get(random.nextInt(0, minimum.size() - 1));
    candidate.getVariablesid().set(left, minimumFactory);
    candidate.getVariablesid().set(right, maximumFactory);
    assignLegalResources(candidate, left, minimumFactory);
    assignLegalResources(candidate, right, maximumFactory);
    validate(candidate);
    return candidate;
  }

  public PermutationSolution<Integer> insert(
      PermutationSolution<Integer> source, int maximumFactory, int minimumFactory) {
    PermutationSolution<Integer> candidate = ZhangBoSolutionSupport.deepCopy(source);
    List<Integer> maximum = positions(candidate, maximumFactory);
    if (maximumFactory == minimumFactory || maximum.isEmpty()) return candidate;
    int position = maximum.get(random.nextInt(0, maximum.size() - 1));
    candidate.getVariablesid().set(position, minimumFactory);
    assignLegalResources(candidate, position, minimumFactory);
    validate(candidate);
    return candidate;
  }

  private void assignLegalResources(
      PermutationSolution<Integer> solution, int position, int factory) {
    List<Integer> machines = ZhangBoMachineVectorSupport.copy(
        solution, solution.getNumberOfVariables());
    machines.set(position, random.nextInt(0, domain.getMachineCount(factory) - 1));
    ZhangBoMachineVectorSupport.write(solution, machines);
    int[] workers = domain.getWorkers(factory);
    solution.getVariablesworker().set(position,
        workers[random.nextInt(0, workers.length - 1)]);
  }

  private static List<Integer> positions(
      PermutationSolution<Integer> solution, int factory) {
    List<Integer> result = new ArrayList<>();
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      if (solution.getVariableValueid(position) == factory) result.add(position);
    }
    return result;
  }

  private void validate(PermutationSolution<Integer> solution) {
    List<Integer> machines = ZhangBoMachineVectorSupport.copy(
        solution, solution.getNumberOfVariables());
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      int factory = solution.getVariableValueid(position);
      if (!domain.isFactoryValid(factory)
          || !domain.isMachineValid(factory, machines.get(position))
          || !domain.isWorkerValid(factory, solution.getVariableValueworker(position))) {
        throw new IllegalArgumentException("Illegal critical-factory resource at position="
            + position + ",factory=" + factory + ",machine=" + machines.get(position)
            + ",worker=" + solution.getVariableValueworker(position));
      }
    }
  }
}
