package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.util.List;

/** Deterministic legality checks for the position-aligned Chapter 4 encoding. */
public final class DhhfspEncodingValidator {
  private DhhfspEncodingValidator() {
  }

  public static void validateOrThrow(
      DhhfspFourVectorSolution solution, DhhfspInstance instance) {
    if (solution == null) {
      throw new IllegalArgumentException("solution must not be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("instance must not be null");
    }

    int jobs = instance.getNumberOfJobs();
    requireLength("JS", solution.getJobSequence(), jobs);
    requireLength("FA", solution.getFactoryAssignments(), jobs);
    requireLength("MA", solution.getMachineAssignments(), jobs);
    requireLength("WA", solution.getWorkerAssignments(), jobs);

    boolean[] seen = new boolean[jobs];
    for (int position = 0; position < jobs; position++) {
      Integer value = requireValue("JS", solution.getJobSequence(), position);
      requireRange("JS", position, value, 0, jobs - 1);
      if (seen[value]) {
        throw new IllegalArgumentException(
            "JS position " + position + " has duplicate job " + value
                + "; expected each value in [0," + (jobs - 1) + "] exactly once");
      }
      seen[value] = true;
    }

    for (int position = 0; position < jobs; position++) {
      int factory = requireValue("FA", solution.getFactoryAssignments(), position);
      requireRange("FA", position, factory, 0, instance.getNumberOfFactories() - 1);

      int machine = requireValue("MA", solution.getMachineAssignments(), position);
      requireRange("MA", position, machine, 0, instance.getMachineCount(factory, 0) - 1);

      int worker = requireValue("WA", solution.getWorkerAssignments(), position);
      requireRange("WA", position, worker, 0, instance.getWorkerCount(factory, 0) - 1);
    }
  }

  private static void requireLength(String vector, List<Integer> values, int expected) {
    if (values.size() != expected) {
      throw new IllegalArgumentException(
          vector + " length " + values.size() + "; expected " + expected);
    }
  }

  private static int requireValue(String vector, List<Integer> values, int position) {
    Integer value = values.get(position);
    if (value == null) {
      throw new IllegalArgumentException(vector + " position " + position + " contains null");
    }
    return value;
  }

  private static void requireRange(
      String vector, int position, int value, int minimum, int maximum) {
    if (value < minimum || value > maximum) {
      throw new IllegalArgumentException(
          vector + " position " + position + " has value " + value
              + "; allowed range [" + minimum + ',' + maximum + ']');
    }
  }
}
