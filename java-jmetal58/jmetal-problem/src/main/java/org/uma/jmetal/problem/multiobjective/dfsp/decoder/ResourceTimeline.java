package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Immutable ordered machine or worker timeline. */
public final class ResourceTimeline implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String resourceType;
  private final int factory;
  private final int stage;
  private final int resource;
  private final List<OperationRecord> operations;

  ResourceTimeline(
      String resourceType, int factory, int stage, int resource,
      List<OperationRecord> operations) {
    this.resourceType = resourceType;
    this.factory = factory;
    this.stage = stage;
    this.resource = resource;
    List<OperationRecord> sorted = new ArrayList<>(operations);
    Collections.sort(sorted, new Comparator<OperationRecord>() {
      @Override public int compare(OperationRecord left, OperationRecord right) {
        int value = Double.compare(left.getStartTime(), right.getStartTime());
        if (value == 0) value = Integer.compare(
            left.getDispatchOrdinal(), right.getDispatchOrdinal());
        return value;
      }
    });
    this.operations = Collections.unmodifiableList(sorted);
  }

  public String getResourceType() { return resourceType; }
  public int getFactory() { return factory; }
  public int getStage() { return stage; }
  public int getResource() { return resource; }
  public List<OperationRecord> getOperations() { return operations; }
  public String getKey() {
    return "f" + factory + ":s" + stage + ":"
        + ("machine".equals(resourceType) ? "m" : "w") + resource;
  }
}
