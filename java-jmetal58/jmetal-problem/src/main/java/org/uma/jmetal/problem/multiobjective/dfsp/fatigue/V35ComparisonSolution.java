package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.util.List;
import java.util.Map;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * P25E search-facing solution. It owns a deep-copied four-vector delegate and
 * prevents a comparison algorithm from depending on the seven-slot production
 * result type. Objective count is fixed when the delegate is constructed.
 */
public final class V35ComparisonSolution implements PermutationSolution<Integer> {
  private static final long serialVersionUID = 1L;
  private final DhhfspFourVectorSolution delegate;

  public V35ComparisonSolution(DhhfspFourVectorSolution source) {
    if (source == null) throw new IllegalArgumentException("source");
    this.delegate = source.copy();
  }

  private V35ComparisonSolution(V35ComparisonSolution source) {
    this.delegate = source.delegate.copy();
  }

  /** Internal representation access for the representation-only operators. */
  public DhhfspFourVectorSolution asFourVector() { return delegate; }
  public List<Integer> getMachineAssignments() { return delegate.getMachineAssignments(); }
  public List<Integer> getJobSequence() { return delegate.getJobSequence(); }
  public List<Integer> getFactoryAssignments() { return delegate.getFactoryAssignments(); }
  public List<Integer> getWorkerAssignments() { return delegate.getWorkerAssignments(); }

  @Override public void setObjective(int index, double value) { delegate.setObjective(index, value); }
  @Override public double getObjective(int index) { return delegate.getObjective(index); }
  @Override public double[] getObjectives() { return delegate.getObjectives(); }
  @Override public Integer getVariableValue(int index) { return delegate.getVariableValue(index); }
  @Override public List<Integer> getVariables() { return delegate.getVariables(); }
  @Override public void setVariableValue(int index, Integer value) {
    delegate.setVariableValue(index, value);
  }
  @Override public String getVariableValueString(int index) {
    return delegate.getVariableValueString(index);
  }
  @Override public int getNumberOfVariables() { return delegate.getNumberOfVariables(); }
  @Override public int getNumberOfObjectives() { return delegate.getNumberOfObjectives(); }
  @Override public V35ComparisonSolution copy() { return new V35ComparisonSolution(this); }
  @Override public void setAttribute(Object id, Object value) { delegate.setAttribute(id, value); }
  @Override public Object getAttribute(Object id) { return delegate.getAttribute(id); }
  @Override public Map<Object, Object> getAttributes() { return delegate.getAttributes(); }
  @Override public Integer getVariableValueid(int index) { return delegate.getVariableValueid(index); }
  @Override public List<Integer> getVariablesid() { return delegate.getVariablesid(); }
  @Override public void setVariableValueid(int index, Integer value) {
    delegate.setVariableValueid(index, value);
  }
  @Override public int getNumberOfVariablesid() { return delegate.getNumberOfVariablesid(); }
  @Override public List<Integer> getVariablesworker() { return delegate.getVariablesworker(); }
  @Override public int getNumberOfVariablesworker() { return delegate.getNumberOfVariablesworker(); }
  @Override public void setVariableValueworker(int index, Integer value) {
    delegate.setVariableValueworker(index, value);
  }
  @Override public Integer getVariableValueworker(int index) {
    return delegate.getVariableValueworker(index);
  }
  @Override public String toString() { return "V35ComparisonSolution{" + delegate + '}'; }
}
