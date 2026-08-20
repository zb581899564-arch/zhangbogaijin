package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One auditable phase of decoding. */
public final class ScheduleSnapshot implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String phase;
  private final List<OperationRecord> operations;
  private final ObjectiveBreakdown objectives;
  private final ScheduleValidationReport validation;
  private final List<ResourceTimeline> machineTimelines;
  private final List<ResourceTimeline> workerTimelines;
  private final boolean accepted;
  private final String note;

  public ScheduleSnapshot(
      String phase, List<OperationRecord> operations, ObjectiveBreakdown objectives,
      ScheduleValidationReport validation, boolean accepted, String note) {
    this.phase = phase;
    List<OperationRecord> sorted = new ArrayList<>(operations);
    Collections.sort(sorted, canonicalComparator());
    this.operations = Collections.unmodifiableList(sorted);
    this.objectives = objectives;
    this.validation = validation;
    this.machineTimelines = timelines(sorted, true);
    this.workerTimelines = timelines(sorted, false);
    this.accepted = accepted;
    this.note = note;
  }

  public String getPhase() { return phase; }
  public List<OperationRecord> getOperations() { return operations; }
  public ObjectiveBreakdown getObjectives() { return objectives; }
  public ScheduleValidationReport getValidation() { return validation; }
  public List<ResourceTimeline> getMachineTimelines() { return machineTimelines; }
  public List<ResourceTimeline> getWorkerTimelines() { return workerTimelines; }
  public boolean isAccepted() { return accepted; }
  public String getNote() { return note; }

  public String operationsCsv() {
    StringBuilder builder = new StringBuilder();
    builder.append("job,stage,factory,machine,worker,start,setup,processing,end,dispatchOrdinal\n");
    for (OperationRecord operation : operations) {
      builder.append(operation.toCsv()).append('\n');
    }
    return builder.toString();
  }

  private static Comparator<OperationRecord> canonicalComparator() {
    return new Comparator<OperationRecord>() {
      @Override
      public int compare(OperationRecord left, OperationRecord right) {
        int value = Integer.compare(left.getFactory(), right.getFactory());
        if (value == 0) value = Integer.compare(left.getStage(), right.getStage());
        if (value == 0) value = Double.compare(left.getStartTime(), right.getStartTime());
        if (value == 0) value = Integer.compare(left.getDispatchOrdinal(), right.getDispatchOrdinal());
        if (value == 0) value = Integer.compare(left.getJob(), right.getJob());
        return value;
      }
    };
  }

  private static List<ResourceTimeline> timelines(
      List<OperationRecord> operations, boolean machine) {
    Map<String, List<OperationRecord>> grouped = new LinkedHashMap<>();
    for (OperationRecord operation : operations) {
      int resource = machine ? operation.getMachine() : operation.getWorker();
      String key = operation.getFactory() + ":" + operation.getStage() + ":" + resource;
      List<OperationRecord> assigned = grouped.get(key);
      if (assigned == null) {
        assigned = new ArrayList<>();
        grouped.put(key, assigned);
      }
      assigned.add(operation);
    }
    List<ResourceTimeline> result = new ArrayList<>();
    for (Map.Entry<String, List<OperationRecord>> entry : grouped.entrySet()) {
      OperationRecord sample = entry.getValue().get(0);
      result.add(new ResourceTimeline(
          machine ? "machine" : "worker", sample.getFactory(), sample.getStage(),
          machine ? sample.getMachine() : sample.getWorker(), entry.getValue()));
    }
    Collections.sort(result, new Comparator<ResourceTimeline>() {
      @Override public int compare(ResourceTimeline left, ResourceTimeline right) {
        return left.getKey().compareTo(right.getKey());
      }
    });
    return Collections.unmodifiableList(result);
  }
}
