package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structural validation independent from objective computation. */
public final class ScheduleValidator {
  private static final double EPSILON = 1.0e-9;

  private ScheduleValidator() { }

  public static ScheduleValidationReport validate(
      DhhfspInstance instance, DhhfspFourVectorSolution solution,
      List<OperationRecord> operations) {
    List<String> violations = new ArrayList<>();
    Map<String, OperationRecord> byJobStage = new HashMap<>();
    Map<String, List<OperationRecord>> machines = new HashMap<>();
    Map<String, List<OperationRecord>> workers = new HashMap<>();

    for (OperationRecord operation : operations) {
      String key = operation.operationKey();
      if (byJobStage.put(key, operation) != null) {
        violations.add("duplicate operation job=" + operation.getJob()
            + ", stage=" + operation.getStage());
      }
      if (operation.getFactory() < 0
          || operation.getFactory() >= instance.getNumberOfFactories()) {
        violations.add("illegal factory for " + key + ": " + operation.getFactory());
        continue;
      }
      int expectedFactory = solution.getFactoryAssignmentForJob(operation.getJob());
      if (operation.getFactory() != expectedFactory) {
        violations.add("factory mismatch for " + key + ": actual="
            + operation.getFactory() + ", expected=" + expectedFactory);
      }
      if (operation.getStage() < 0 || operation.getStage() >= instance.getNumberOfStages()) {
        violations.add("illegal stage for " + key + ": " + operation.getStage());
        continue;
      }
      if (operation.getMachine() < 0 || operation.getMachine()
          >= instance.getMachineCount(operation.getFactory(), operation.getStage())) {
        violations.add("illegal machine for " + key + ": " + operation.getMachine());
      }
      if (operation.getWorker() < 0 || operation.getWorker()
          >= instance.getWorkerCount(operation.getFactory(), operation.getStage())) {
        violations.add("illegal worker for " + key + ": " + operation.getWorker());
      }
      if (operation.getStartTime() < -EPSILON
          || Math.abs(operation.getStartTime() + operation.getDuration()
              - operation.getEndTime()) > EPSILON) {
        violations.add("invalid interval for " + key + ": start="
            + operation.getStartTime() + ", end=" + operation.getEndTime()
            + ", duration=" + operation.getDuration());
      }
      add(machines, resourceKey(operation, true), operation);
      add(workers, resourceKey(operation, false), operation);
    }

    for (int job = 0; job < instance.getNumberOfJobs(); job++) {
      for (int stage = 0; stage < instance.getNumberOfStages(); stage++) {
        String key = job + ":" + stage;
        if (!byJobStage.containsKey(key)) {
          violations.add("missing operation job=" + job + ", stage=" + stage);
        }
      }
      for (int stage = 1; stage < instance.getNumberOfStages(); stage++) {
        OperationRecord previous = byJobStage.get(job + ":" + (stage - 1));
        OperationRecord current = byJobStage.get(job + ":" + stage);
        if (previous != null && current != null
            && current.getStartTime() + EPSILON < previous.getEndTime()) {
          violations.add("precedence violation job=" + job + ", stage=" + stage
              + ": start=" + current.getStartTime() + " < previousEnd="
              + previous.getEndTime());
        }
      }
    }
    validateNonOverlap("machine", machines, violations);
    validateNonOverlap("worker", workers, violations);
    return new ScheduleValidationReport(violations);
  }

  private static void validateNonOverlap(
      String type, Map<String, List<OperationRecord>> resources, List<String> violations) {
    for (Map.Entry<String, List<OperationRecord>> entry : resources.entrySet()) {
      List<OperationRecord> operations = entry.getValue();
      Collections.sort(operations, byStart());
      for (int index = 1; index < operations.size(); index++) {
        OperationRecord previous = operations.get(index - 1);
        OperationRecord current = operations.get(index);
        if (current.getStartTime() + EPSILON < previous.getEndTime()) {
          violations.add(type + " overlap " + entry.getKey() + ": job="
              + previous.getJob() + "/stage=" + previous.getStage() + " ["
              + previous.getStartTime() + "," + previous.getEndTime() + ") and job="
              + current.getJob() + "/stage=" + current.getStage() + " ["
              + current.getStartTime() + "," + current.getEndTime() + ")");
        }
      }
    }
  }

  private static Comparator<OperationRecord> byStart() {
    return new Comparator<OperationRecord>() {
      @Override
      public int compare(OperationRecord left, OperationRecord right) {
        int value = Double.compare(left.getStartTime(), right.getStartTime());
        if (value == 0) value = Integer.compare(left.getDispatchOrdinal(), right.getDispatchOrdinal());
        return value;
      }
    };
  }

  private static void add(
      Map<String, List<OperationRecord>> values, String key, OperationRecord operation) {
    List<OperationRecord> list = values.get(key);
    if (list == null) {
      list = new ArrayList<>();
      values.put(key, list);
    }
    list.add(operation);
  }

  static String resourceKey(OperationRecord operation, boolean machine) {
    return "f" + operation.getFactory() + ":s" + operation.getStage() + ":"
        + (machine ? "m" + operation.getMachine() : "w" + operation.getWorker());
  }
}
