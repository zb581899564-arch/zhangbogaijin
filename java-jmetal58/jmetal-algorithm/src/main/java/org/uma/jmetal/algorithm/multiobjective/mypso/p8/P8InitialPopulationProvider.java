package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSolutionSupport;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoMachineVectorSupport;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.solution.PermutationSolution;

/** Creates one seed-keyed initial population that can be deep-copied across ablations. */
public final class P8InitialPopulationProvider {
  private P8InitialPopulationProvider() { }

  public static List<PermutationSolution<Integer>> create(
      PermutationProblem<PermutationSolution<Integer>> problem, int size, long seed,
      int instanceStages) {
    if (problem == null || size <= 0 || instanceStages <= 0) {
      throw new IllegalArgumentException("Invalid P8 population request");
    }
    if (!(problem instanceof ZhangBoEDHHFSPW)) {
      throw new IllegalArgumentException("P8 requires the ZhangBo direct-derivation problem");
    }
    ZhangBoFatigueInstanceData data = ((ZhangBoEDHHFSPW) problem).getFatigueInstanceData();
    if (data == null) throw new IllegalStateException("P8 requires configured fatigue instance data");
    List<PermutationSolution<Integer>> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      PermutationSolution<Integer> solution = problem.createSolution();
      // The author's solution constructor reads a legacy default instance.  For an
      // explicit P8 instance the chromosome must nevertheless have exactly that
      // instance's stage blocks; otherwise the author mutation indexes a stale
      // stage (e.g. 8 legacy blocks for a 2-stage instance).  This is a
      // deterministic input normalization shared by every ablation, not a decoder
      // or algorithm change.
      int jobs = solution.getNumberOfVariables();
      int required = instanceStages * jobs;
      List<Integer> workers = solution.getVariablesworker();
      if (workers.size() < required) {
        throw new IllegalStateException("Initial worker chromosome has " + workers.size()
            + " values, but the instance requires " + required);
      }
      while (workers.size() > required) workers.remove(workers.size() - 1);
      overwriteDeterministically(solution, data, instanceStages, seed, i);
      result.add(solution);
    }
    return result;
  }

  /**
   * The direct author constructor contains independent {@code new Random()} calls.
   * P8 deliberately replaces only its resulting *initial chromosome* with a
   * seed-keyed legal one.  This makes every ablation start from the same input;
   * it does not change the author-compatible production initializer or update path.
   */
  @SuppressWarnings("unchecked")
  private static void overwriteDeterministically(PermutationSolution<Integer> solution,
      ZhangBoFatigueInstanceData data, int stages, long seed, int particle) {
    int jobs = solution.getNumberOfVariables();
    Random random = new Random(mix(seed, particle));
    List<Integer> jobsVector = new ArrayList<>(jobs);
    for (int job = 0; job < jobs; job++) jobsVector.add(job);
    Collections.shuffle(jobsVector, random);
    for (int position = 0; position < jobs; position++) {
      solution.setVariableValue(position, jobsVector.get(position));
      int factory = position < data.getFactories()
          ? position : random.nextInt(data.getFactories());
      solution.setVariableValueid(position, factory);
    }
    Object machineAttribute = solution.getAttribute("machine");
    if (!(machineAttribute instanceof List)) {
      throw new IllegalStateException("P8 initial solution has no machine vector");
    }
    List<Integer> machines = (List<Integer>) machineAttribute;
    if (machines.size() < jobs) throw new IllegalStateException("P8 machine vector is too short");
    for (int position = 0; position < jobs; position++) {
      int factory = solution.getVariableValueid(position);
      machines.set(position, random.nextInt(data.getMachineCount(factory, 0)));
      for (int stage = 0; stage < stages; stage++) {
        int[] domain = data.getEligibleWorkers(factory, stage);
        solution.setVariableValueworker(stage * jobs + position,
            domain[random.nextInt(domain.length)]);
      }
    }
    ZhangBoMachineVectorSupport.write(solution, machines);
  }

  private static long mix(long seed, int particle) {
    long value = seed ^ (0x9E3779B97F4A7C15L * (particle + 1L));
    value ^= value >>> 33;
    value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33;
    value *= 0xc4ceb9fe1a85ec53L;
    return value ^ (value >>> 33);
  }

  public static List<PermutationSolution<Integer>> copy(
      List<PermutationSolution<Integer>> source) {
    return ZhangBoSolutionSupport.deepCopySolutions(source);
  }

  public static String sha256(List<PermutationSolution<Integer>> population) {
    StringBuilder text = new StringBuilder();
    for (int p = 0; p < population.size(); p++) {
      PermutationSolution<Integer> solution = population.get(p);
      text.append("particle=").append(p).append('\n');
      append(text, "JS", solution.getVariables());
      append(text, "FA", solution.getVariablesid());
      append(text, "MA", ZhangBoMachineVectorSupport.copy(solution, solution.getNumberOfVariables()));
      append(text, "WA", solution.getVariablesworker());
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(text.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static void append(StringBuilder text, String name, List<?> values) {
    text.append(name).append('=');
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) text.append(',');
      text.append(values.get(i));
    }
    text.append('\n');
  }
}
