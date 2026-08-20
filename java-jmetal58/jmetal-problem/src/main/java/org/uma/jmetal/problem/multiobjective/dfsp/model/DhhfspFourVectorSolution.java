package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.solution.Solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * jMetal 5.8 compatible four-vector solution for Chapter 4.
 *
 * <p>All runtime identifiers are zero-based. The four vectors are position-aligned; MA and WA
 * encode first-stage resources only.</p>
 */
public final class DhhfspFourVectorSolution implements PermutationSolution<Integer> {
  private static final long serialVersionUID = 1L;
  public static final int NUMBER_OF_OBJECTIVES = 3;

  private final List<Integer> jobSequence;
  private final List<Integer> factoryAssignments;
  private final List<Integer> machineAssignments;
  private final List<Integer> workerAssignments;
  private final double[] objectives;
  private final Map<Object, Object> attributes;
  private final String semanticTag;

  public DhhfspFourVectorSolution(
      List<Integer> jobSequence,
      List<Integer> factoryAssignments,
      List<Integer> machineAssignments,
      List<Integer> workerAssignments,
      String semanticTag) {
    this(jobSequence, factoryAssignments, machineAssignments, workerAssignments,
        semanticTag, NUMBER_OF_OBJECTIVES);
  }

  /**
   * Creates a four-vector with an explicit objective-slot count.  The default
   * constructor remains the three-objective P2/P3 contract; the seven-slot
   * Zhang-Bo production path uses this overload so legacy callers do not move
   * their TWC slot.
   */
  public DhhfspFourVectorSolution(
      List<Integer> jobSequence,
      List<Integer> factoryAssignments,
      List<Integer> machineAssignments,
      List<Integer> workerAssignments,
      String semanticTag,
      int objectiveCount) {
    this.jobSequence = copyRequired("JS", jobSequence);
    this.factoryAssignments = copyRequired("FA", factoryAssignments);
    this.machineAssignments = copyRequired("MA", machineAssignments);
    this.workerAssignments = copyRequired("WA", workerAssignments);
    if (objectiveCount <= 0) {
      throw new IllegalArgumentException("objectiveCount must be positive: " + objectiveCount);
    }
    this.objectives = new double[objectiveCount];
    this.attributes = new HashMap<>();
    this.semanticTag = requireSemanticTag(semanticTag);
  }

  private DhhfspFourVectorSolution(DhhfspFourVectorSolution source) {
    this.jobSequence = new ArrayList<>(source.jobSequence);
    this.factoryAssignments = new ArrayList<>(source.factoryAssignments);
    this.machineAssignments = new ArrayList<>(source.machineAssignments);
    this.workerAssignments = new ArrayList<>(source.workerAssignments);
    this.objectives = source.objectives.clone();
    this.attributes = copyAttributes(source.attributes);
    this.semanticTag = source.semanticTag;
  }

  public String getSemanticTag() {
    return semanticTag;
  }

  public List<Integer> getJobSequence() {
    return jobSequence;
  }

  public List<Integer> getFactoryAssignments() {
    return factoryAssignments;
  }

  public List<Integer> getMachineAssignments() {
    return machineAssignments;
  }

  public List<Integer> getWorkerAssignments() {
    return workerAssignments;
  }

  public Integer getMachineAssignment(int index) {
    return machineAssignments.get(index);
  }

  public void setMachineAssignment(int index, Integer value) {
    machineAssignments.set(index, value);
  }

  public int jobAtPosition(int position) {
    if (position < 0 || position >= jobSequence.size()) {
      throw new IllegalArgumentException(
          "JS position " + position + " outside [0," + (jobSequence.size() - 1) + "]");
    }
    Integer job = jobSequence.get(position);
    if (job == null) {
      throw new IllegalStateException("JS position " + position + " contains null");
    }
    return job;
  }

  public int positionOfJob(int job) {
    int position = jobSequence.indexOf(job);
    if (position < 0) {
      throw new IllegalArgumentException("JS does not contain job " + job);
    }
    return position;
  }

  public int getFactoryAssignmentForJob(int job) {
    return requiredValueForJob("FA", factoryAssignments, job);
  }

  public int getMachineAssignmentForJob(int job) {
    return requiredValueForJob("MA", machineAssignments, job);
  }

  public int getWorkerAssignmentForJob(int job) {
    return requiredValueForJob("WA", workerAssignments, job);
  }

  @Override
  public void setObjective(int index, double value) {
    objectives[index] = value;
  }

  @Override
  public double getObjective(int index) {
    return objectives[index];
  }

  @Override
  public double[] getObjectives() {
    return objectives;
  }

  @Override
  public Integer getVariableValue(int index) {
    return jobSequence.get(index);
  }

  @Override
  public List<Integer> getVariables() {
    return jobSequence;
  }

  @Override
  public void setVariableValue(int index, Integer value) {
    jobSequence.set(index, value);
  }

  @Override
  public String getVariableValueString(int index) {
    return String.valueOf(jobSequence.get(index));
  }

  @Override
  public int getNumberOfVariables() {
    return jobSequence.size();
  }

  @Override
  public int getNumberOfObjectives() {
    return objectives.length;
  }

  @Override
  public DhhfspFourVectorSolution copy() {
    return new DhhfspFourVectorSolution(this);
  }

  @Override
  public void setAttribute(Object id, Object value) {
    attributes.put(id, value);
  }

  @Override
  public Object getAttribute(Object id) {
    return attributes.get(id);
  }

  @Override
  public Map<Object, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Integer getVariableValueid(int index) {
    return factoryAssignments.get(index);
  }

  @Override
  public List<Integer> getVariablesid() {
    return factoryAssignments;
  }

  @Override
  public void setVariableValueid(int index, Integer value) {
    factoryAssignments.set(index, value);
  }

  @Override
  public int getNumberOfVariablesid() {
    return factoryAssignments.size();
  }

  @Override
  public List<Integer> getVariablesworker() {
    return workerAssignments;
  }

  @Override
  public int getNumberOfVariablesworker() {
    return workerAssignments.size();
  }

  @Override
  public void setVariableValueworker(int index, Integer value) {
    workerAssignments.set(index, value);
  }

  @Override
  public Integer getVariableValueworker(int index) {
    return workerAssignments.get(index);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DhhfspFourVectorSolution)) {
      return false;
    }
    DhhfspFourVectorSolution that = (DhhfspFourVectorSolution) other;
    return jobSequence.equals(that.jobSequence)
        && factoryAssignments.equals(that.factoryAssignments)
        && machineAssignments.equals(that.machineAssignments)
        && workerAssignments.equals(that.workerAssignments)
        && Arrays.equals(objectives, that.objectives)
        && attributes.equals(that.attributes)
        && semanticTag.equals(that.semanticTag);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(
        jobSequence, factoryAssignments, machineAssignments, workerAssignments, attributes,
        semanticTag);
    return 31 * result + Arrays.hashCode(objectives);
  }

  @Override
  public String toString() {
    return "DhhfspFourVectorSolution{"
        + "semanticTag='" + semanticTag + '\''
        + ", JS=" + jobSequence
        + ", FA=" + factoryAssignments
        + ", MA=" + machineAssignments
        + ", WA=" + workerAssignments
        + ", objectives=" + Arrays.toString(objectives)
        + '}';
  }

  private static List<Integer> copyRequired(String name, List<Integer> source) {
    if (source == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return new ArrayList<>(source);
  }

  private static String requireSemanticTag(String value) {
    if (!"published_baseline".equals(value)
        && !"author_actual".equals(value)
        && !"deterministic_canonical".equals(value)
        && !"fatigue_improved".equals(value)
        && !"fatigue_fm1".equals(value)
        && !"fatigue_fm2".equals(value)
        && !"fatigue_fm3".equals(value)) {
      throw new IllegalArgumentException("Unknown semanticTag: " + value);
    }
    return value;
  }

  private int requiredValueForJob(String vector, List<Integer> values, int job) {
    int position = positionOfJob(job);
    Integer value = values.get(position);
    if (value == null) {
      throw new IllegalStateException(vector + " position " + position + " for job " + job
          + " contains null");
    }
    return value;
  }

  private static Map<Object, Object> copyAttributes(Map<Object, Object> source) {
    Map<Object, Object> copy = new HashMap<>();
    for (Map.Entry<Object, Object> entry : source.entrySet()) {
      copy.put(entry.getKey(), copyMutableValue(entry.getValue()));
    }
    return copy;
  }

  @SuppressWarnings("unchecked")
  private static Object copyMutableValue(Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Character
        || value instanceof Enum<?>) {
      return value;
    }
    if (value instanceof Solution<?>) {
      return ((Solution<?>) value).copy();
    }
    if (value instanceof List<?>) {
      List<Object> copy = new ArrayList<>();
      for (Object element : (List<Object>) value) {
        copy.add(copyMutableValue(element));
      }
      return copy;
    }
    if (value instanceof Set<?>) {
      Set<Object> copy = new LinkedHashSet<>();
      for (Object element : (Set<Object>) value) {
        copy.add(copyMutableValue(element));
      }
      return copy;
    }
    if (value instanceof Map<?, ?>) {
      Map<Object, Object> copy = new LinkedHashMap<>();
      for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
        copy.put(copyMutableValue(entry.getKey()), copyMutableValue(entry.getValue()));
      }
      return copy;
    }
    if (value instanceof int[]) {
      return ((int[]) value).clone();
    }
    if (value instanceof double[]) {
      return ((double[]) value).clone();
    }
    if (value instanceof long[]) {
      return ((long[]) value).clone();
    }
    if (value instanceof boolean[]) {
      return ((boolean[]) value).clone();
    }
    if (value instanceof byte[]) {
      return ((byte[]) value).clone();
    }
    if (value instanceof short[]) {
      return ((short[]) value).clone();
    }
    if (value instanceof float[]) {
      return ((float[]) value).clone();
    }
    if (value instanceof char[]) {
      return ((char[]) value).clone();
    }
    if (value instanceof Object[]) {
      Object[] source = (Object[]) value;
      Object[] copy = source.clone();
      for (int index = 0; index < copy.length; index++) {
        copy[index] = copyMutableValue(copy[index]);
      }
      return copy;
    }
    return value;
  }
}
