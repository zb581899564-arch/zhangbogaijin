package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspEncodingValidator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Overall-v2 numbering for the original nine neighborhoods. */
public final class OriginalNeighborhoods implements Serializable {
  private static final long serialVersionUID = 1L;
  private final DhhfspInstance instance;
  private final PseudoRandomGenerator random;

  public OriginalNeighborhoods(DhhfspInstance instance, PseudoRandomGenerator random) {
    this.instance = instance;
    this.random = random;
  }

  public String name(int operation) {
    switch (operation) {
      case 1: return "O1_JS_INSERT";
      case 2: return "O2_JS_REVERSE";
      case 3: return "O3_JS_SWAP";
      case 4: return "O4_WA_LOAD_TRANSFER";
      case 5: return "O5_WA_WEAK_TO_STRONG";
      case 6: return "O6_WA_SWAP";
      case 7: return "O7_MA_LOAD_TRANSFER";
      case 8: return "O8_MA_WEAK_TO_STRONG";
      case 9: return "O9_MA_SWAP";
      default: throw new IllegalArgumentException("operation must be O1..O9");
    }
  }

  public DhhfspFourVectorSolution apply(
      int operation, DhhfspFourVectorSolution source, int factory) {
    name(operation);
    DhhfspFourVectorSolution candidate = source.copy();
    List<Integer> positions = positions(candidate, factory);
    if (positions.isEmpty()) return candidate;
    if (operation <= 3 && positions.size() >= 2) {
      int first = positions.get(random.nextInt(0, positions.size() - 1));
      int second = positions.get(random.nextInt(0, positions.size() - 1));
      if (operation == 1) insertAligned(candidate, first, second);
      if (operation == 2) reverseAligned(candidate, Math.min(first, second), Math.max(first, second));
      if (operation == 3) swapAligned(candidate, first, second);
    } else if (operation == 4) {
      transferLoad(candidate.getWorkerAssignments(), positions,
          instance.getWorkerCount(factory, 0));
    } else if (operation == 5) {
      weakToStrong(candidate.getWorkerAssignments(), positions,
          instance.getWorkerEfficiencies(factory, 0));
    } else if (operation == 6) {
      swapResources(candidate.getWorkerAssignments(), positions);
    } else if (operation == 7) {
      transferLoad(candidate.getMachineAssignments(), positions,
          instance.getMachineCount(factory, 0));
    } else if (operation == 8) {
      weakToStrong(candidate.getMachineAssignments(), positions,
          instance.getMachineSpeeds(factory, 0));
    } else if (operation == 9) {
      swapResources(candidate.getMachineAssignments(), positions);
    }
    DhhfspEncodingValidator.validateOrThrow(candidate, instance);
    return candidate;
  }

  public DhhfspFourVectorSolution criticalFactoryInsert(
      DhhfspFourVectorSolution source, int maximumFactory, int minimumFactory) {
    DhhfspFourVectorSolution candidate = source.copy();
    List<Integer> from = positions(candidate, maximumFactory);
    if (from.isEmpty() || maximumFactory == minimumFactory) return candidate;
    int position = from.get(random.nextInt(0, from.size() - 1));
    candidate.getFactoryAssignments().set(position, minimumFactory);
    assignLegalResources(candidate, position, minimumFactory);
    DhhfspEncodingValidator.validateOrThrow(candidate, instance);
    return candidate;
  }

  public DhhfspFourVectorSolution criticalFactorySwap(
      DhhfspFourVectorSolution source, int firstFactory, int secondFactory) {
    DhhfspFourVectorSolution candidate = source.copy();
    List<Integer> first = positions(candidate, firstFactory);
    List<Integer> second = positions(candidate, secondFactory);
    if (first.isEmpty() || second.isEmpty() || firstFactory == secondFactory) return candidate;
    int left = first.get(random.nextInt(0, first.size() - 1));
    int right = second.get(random.nextInt(0, second.size() - 1));
    candidate.getFactoryAssignments().set(left, secondFactory);
    candidate.getFactoryAssignments().set(right, firstFactory);
    assignLegalResources(candidate, left, secondFactory);
    assignLegalResources(candidate, right, firstFactory);
    DhhfspEncodingValidator.validateOrThrow(candidate, instance);
    return candidate;
  }

  private void assignLegalResources(
      DhhfspFourVectorSolution candidate, int position, int factory) {
    candidate.getMachineAssignments().set(
        position, random.nextInt(0, instance.getMachineCount(factory, 0) - 1));
    candidate.getWorkerAssignments().set(
        position, random.nextInt(0, instance.getWorkerCount(factory, 0) - 1));
  }

  private static List<Integer> positions(DhhfspFourVectorSolution solution, int factory) {
    List<Integer> result = new ArrayList<>();
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      if (solution.getFactoryAssignments().get(position) == factory) result.add(position);
    }
    return result;
  }

  private static void insertAligned(DhhfspFourVectorSolution solution, int from, int to) {
    if (from == to) return;
    move(solution.getJobSequence(), from, to);
    move(solution.getFactoryAssignments(), from, to);
    move(solution.getMachineAssignments(), from, to);
    move(solution.getWorkerAssignments(), from, to);
  }

  private static void move(List<Integer> values, int from, int to) {
    Integer value = values.remove(from);
    values.add(to, value);
  }

  private static void reverseAligned(DhhfspFourVectorSolution solution, int from, int to) {
    reverse(solution.getJobSequence(), from, to);
    reverse(solution.getFactoryAssignments(), from, to);
    reverse(solution.getMachineAssignments(), from, to);
    reverse(solution.getWorkerAssignments(), from, to);
  }

  private static void reverse(List<Integer> values, int from, int to) {
    while (from < to) Collections.swap(values, from++, to--);
  }

  private static void swapAligned(DhhfspFourVectorSolution solution, int first, int second) {
    Collections.swap(solution.getJobSequence(), first, second);
    Collections.swap(solution.getFactoryAssignments(), first, second);
    Collections.swap(solution.getMachineAssignments(), first, second);
    Collections.swap(solution.getWorkerAssignments(), first, second);
  }

  private void transferLoad(List<Integer> vector, List<Integer> positions, int resources) {
    int[] count = new int[resources];
    for (Integer position : positions) count[vector.get(position)]++;
    int maximum = 0;
    int minimum = 0;
    for (int resource = 1; resource < resources; resource++) {
      if (count[resource] > count[maximum]) maximum = resource;
      if (count[resource] < count[minimum]) minimum = resource;
    }
    List<Integer> loaded = new ArrayList<>();
    for (Integer position : positions) if (vector.get(position) == maximum) loaded.add(position);
    if (!loaded.isEmpty()) vector.set(loaded.get(random.nextInt(0, loaded.size() - 1)), minimum);
  }

  private static void weakToStrong(
      List<Integer> vector, List<Integer> positions, double[] capability) {
    int weak = 0;
    int strong = 0;
    for (int resource = 1; resource < capability.length; resource++) {
      if (capability[resource] < capability[weak]) weak = resource;
      if (capability[resource] > capability[strong]) strong = resource;
    }
    for (Integer position : positions) {
      if (vector.get(position) == weak) {
        vector.set(position, strong);
        return;
      }
    }
  }

  private void swapResources(List<Integer> vector, List<Integer> positions) {
    if (positions.size() < 2) return;
    int first = positions.get(random.nextInt(0, positions.size() - 1));
    int second = positions.get(random.nextInt(0, positions.size() - 1));
    Collections.swap(vector, first, second);
  }
}
