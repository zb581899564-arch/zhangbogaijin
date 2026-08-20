package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ZhangBoTestPermutationSolution implements PermutationSolution<Integer> {
  private final List<Integer> jobs;
  private final List<Integer> factories;
  private final List<Integer> workers;
  private final double[] objectives;
  private final Map<Object, Object> attributes;

  ZhangBoTestPermutationSolution(
      int[] jobs, int[] factories, int[] machines, int[] workers, int[] workerTail,
      double cmax, double tec, double twc) {
    this.jobs = list(jobs);
    this.factories = list(factories);
    this.workers = list(workers);
    for (int value : workerTail) this.workers.add(value);
    this.objectives = new double[7];
    this.objectives[0] = cmax;
    this.objectives[1] = tec;
    this.objectives[6] = twc;
    this.attributes = new HashMap<>();
    this.attributes.put("machine", list(machines));
  }

  private ZhangBoTestPermutationSolution(ZhangBoTestPermutationSolution source) {
    jobs = new ArrayList<>(source.jobs);
    factories = new ArrayList<>(source.factories);
    workers = new ArrayList<>(source.workers);
    objectives = source.objectives.clone();
    attributes = new HashMap<>(source.attributes);
    Object machines = source.attributes.get("machine");
    if (machines instanceof List) attributes.put("machine", new ArrayList<>((List<?>) machines));
  }

  private static List<Integer> list(int[] values) {
    List<Integer> result = new ArrayList<>(values.length);
    for (int value : values) result.add(value);
    return result;
  }

  @Override public void setObjective(int index, double value) { objectives[index] = value; }
  @Override public double getObjective(int index) { return objectives[index]; }
  @Override public double[] getObjectives() { return objectives; }
  @Override public Integer getVariableValue(int index) { return jobs.get(index); }
  @Override public List<Integer> getVariables() { return jobs; }
  @Override public void setVariableValue(int index, Integer value) { jobs.set(index, value); }
  @Override public String getVariableValueString(int index) { return String.valueOf(jobs.get(index)); }
  @Override public int getNumberOfVariables() { return jobs.size(); }
  @Override public int getNumberOfObjectives() { return objectives.length; }
  @Override public PermutationSolution<Integer> copy() { return new ZhangBoTestPermutationSolution(this); }
  @Override public void setAttribute(Object id, Object value) { attributes.put(id, value); }
  @Override public Object getAttribute(Object id) { return attributes.get(id); }
  @Override public Map<Object, Object> getAttributes() { return attributes; }
  @Override public Integer getVariableValueid(int index) { return factories.get(index); }
  @Override public List<Integer> getVariablesid() { return factories; }
  @Override public void setVariableValueid(int index, Integer value) { factories.set(index, value); }
  @Override public int getNumberOfVariablesid() { return factories.size(); }
  @Override public List<Integer> getVariablesworker() { return workers; }
  @Override public int getNumberOfVariablesworker() { return workers.size(); }
  @Override public void setVariableValueworker(int index, Integer value) { workers.set(index, value); }
  @Override public Integer getVariableValueworker(int index) { return workers.get(index); }

  String vectors() {
    return jobs + "|" + factories + "|" + getAttribute("machine") + "|" + workers
        + "|" + Arrays.toString(objectives);
  }

  ZhangBoTestPermutationSolution withFatigue(double maximumFatigue, double fatigueExcess) {
    ZhangBoFatigueMetrics metrics = new ZhangBoFatigueMetrics(maximumFatigue, maximumFatigue,
        fatigueExcess, 0.0, 0.0, 0.0, 0.0, 0);
    setAttribute(ZhangBoFatigueEvaluationResult.class,
        new ZhangBoFatigueEvaluationResult("test-instance", "test-config",
            java.util.Collections.emptyList(), metrics, objectives,
            new double[0][][], new double[0][][], new double[0][][]));
    return this;
  }
}
