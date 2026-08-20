package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.solution.PermutationSolution;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;

/** Immutable, lightweight four-vector entry stored in a lineage archive. */
public final class ZhangBoArchiveEntry {
  private final int[] jobs;
  private final int[] factories;
  private final int[] machines;
  private final int[] workers;
  private final double[] objectives;
  private final double maximumFatigue;
  private final double fatigueExcess;
  private final ZhangBoEvaluatedPddrSelector.Source source;
  private final int generation;
  private final long evaluationOrdinal;
  private final String fingerprint;

  public ZhangBoArchiveEntry(
      int[] jobs, int[] factories, int[] machines, int[] workers,
      double[] objectives, double maximumFatigue, double fatigueExcess,
      ZhangBoEvaluatedPddrSelector.Source source, int generation,
      long evaluationOrdinal) {
    if (jobs == null || factories == null || machines == null || workers == null
        || objectives == null || source == null) {
      throw new IllegalArgumentException("Archive entry fields cannot be null");
    }
    if (jobs.length == 0 || factories.length != jobs.length || machines.length != jobs.length
        || workers.length < jobs.length || objectives.length != 3) {
      throw new IllegalArgumentException("Archive entry vector dimensions are invalid");
    }
    for (double objective : objectives) requireFinite(objective, "objective");
    requireFinite(maximumFatigue, "maximumFatigue");
    requireFinite(fatigueExcess, "fatigueExcess");
    this.jobs = jobs.clone();
    this.factories = factories.clone();
    this.machines = machines.clone();
    this.workers = workers.clone();
    this.objectives = objectives.clone();
    this.maximumFatigue = maximumFatigue;
    this.fatigueExcess = fatigueExcess;
    this.source = source;
    this.generation = generation;
    this.evaluationOrdinal = evaluationOrdinal;
    this.fingerprint = sha256(vectorText());
  }

  @SuppressWarnings("unchecked")
  public static ZhangBoArchiveEntry fromSolution(
      PermutationSolution<Integer> solution,
      ZhangBoEvaluatedPddrSelector.Source source,
      int generation,
      long evaluationOrdinal) {
    return fromSolution(solution, source, generation, evaluationOrdinal, false);
  }

  @SuppressWarnings("unchecked")
  public static ZhangBoArchiveEntry fromSolution(
      PermutationSolution<Integer> solution,
      ZhangBoEvaluatedPddrSelector.Source source,
      int generation,
      long evaluationOrdinal,
      boolean allowMissingFatigueAsZero) {
    if (solution == null) throw new IllegalArgumentException("solution cannot be null");
    int jobsCount = solution.getNumberOfVariables();
    int[] jobs = new int[jobsCount];
    int[] factories = new int[jobsCount];
    int[] workers = new int[solution.getNumberOfVariablesworker()];
    for (int index = 0; index < jobsCount; index++) {
      jobs[index] = requireInteger(solution.getVariableValue(index), "JS", index);
      factories[index] = requireInteger(solution.getVariableValueid(index), "FA", index);
    }
    for (int index = 0; index < workers.length; index++) {
      workers[index] = requireInteger(solution.getVariableValueworker(index), "WA", index);
    }
    List<Integer> machineValues = ZhangBoMachineVectorSupport.copy(solution, jobsCount);
    int[] machines = new int[jobsCount];
    for (int index = 0; index < jobsCount; index++) {
      machines[index] = requireInteger(machineValues.get(index), "MA", index);
    }
    double[] objectives = new double[]{solution.getObjective(0), solution.getObjective(1),
        solution.getObjective(6)};
    ZhangBoFatigueEvaluationResult fatigue =
        (ZhangBoFatigueEvaluationResult) solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    if (fatigue == null) {
      if (!allowMissingFatigueAsZero) {
        throw new IllegalArgumentException("Lineage archive requires P5 fatigue evaluation result");
      }
      return new ZhangBoArchiveEntry(jobs, factories, machines, workers, objectives,
          0.0, 0.0, source, generation, evaluationOrdinal);
    }
    return new ZhangBoArchiveEntry(jobs, factories, machines, workers, objectives,
        fatigue.getMetrics().maximumFatigue,
        fatigue.getMetrics().fatigueExcessIntegral,
        source, generation, evaluationOrdinal);
  }

  private static int requireInteger(Integer value, String vector, int index) {
    if (value == null) throw new IllegalArgumentException(vector + '[' + index + "] is null");
    return value;
  }

  private static void requireFinite(double value, String label) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException(label + " must be finite");
  }

  private String vectorText() {
    StringBuilder out = new StringBuilder();
    append(out, "JS", jobs);
    append(out, "FA", factories);
    append(out, "MA", machines);
    append(out, "WA", workers);
    return out.toString();
  }

  private static void append(StringBuilder out, String name, int[] values) {
    out.append(name).append('=');
    for (int index = 0; index < values.length; index++) {
      if (index > 0) out.append(',');
      out.append(values[index]);
    }
    out.append('\n');
  }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte item : digest) out.append(String.format("%02X", item & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  public int[] getJobs() { return jobs.clone(); }
  public int[] getFactories() { return factories.clone(); }
  public int[] getMachines() { return machines.clone(); }
  public int[] getWorkers() { return workers.clone(); }
  public double getObjective(int index) { return objectives[index]; }
  public double[] getObjectives() { return objectives.clone(); }
  public double getMaximumFatigue() { return maximumFatigue; }
  public double getFatigueExcess() { return fatigueExcess; }
  public ZhangBoEvaluatedPddrSelector.Source getSource() { return source; }
  public int getGeneration() { return generation; }
  public long getEvaluationOrdinal() { return evaluationOrdinal; }
  public String getFingerprint() { return fingerprint; }

  /** Restores only the encoded leader vectors; no decoding or evaluation is performed. */
  @SuppressWarnings("unchecked")
  public PermutationSolution<Integer> toSolution(PermutationSolution<Integer> template) {
    if (template == null) throw new IllegalArgumentException("template cannot be null");
    if (template.getNumberOfVariables() != jobs.length
        || template.getNumberOfVariablesworker() != workers.length) {
      throw new IllegalArgumentException("Template dimensions do not match archive entry");
    }
    PermutationSolution<Integer> result = ZhangBoSolutionSupport.deepCopy(template);
    for (int index = 0; index < jobs.length; index++) {
      result.setVariableValue(index, jobs[index]);
      result.setVariableValueid(index, factories[index]);
    }
    for (int index = 0; index < workers.length; index++) {
      result.setVariableValueworker(index, workers[index]);
    }
    List<Integer> machineValues = new ArrayList<>(machines.length);
    for (int index = 0; index < machines.length; index++) {
      machineValues.add(machines[index]);
    }
    ZhangBoMachineVectorSupport.write(result, machineValues);
    if (result.getNumberOfObjectives() > 6) {
      result.setObjective(0, objectives[0]);
      result.setObjective(1, objectives[1]);
      result.setObjective(6, objectives[2]);
    }
    return result;
  }

  public String toCanonicalText() {
    return "fingerprint=" + fingerprint + ",objectives=" + objectives[0] + ','
        + objectives[1] + ',' + objectives[2] + ",Fmax=" + maximumFatigue
        + ",FE=" + fatigueExcess + ",source=" + source + ",generation="
        + generation + ",evaluation=" + evaluationOrdinal;
  }
}
