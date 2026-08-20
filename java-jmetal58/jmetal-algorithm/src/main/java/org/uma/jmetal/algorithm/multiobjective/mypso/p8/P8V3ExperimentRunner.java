package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Scope-locked current shift-semantic matrix runner. It owns registry traversal and exact-run reuse;
 * callers inject the canonical problem/initial-population factories so this module never
 * falls back to the author's DefaultIntegerPermutationSolution or author decoder.
 */
public final class P8V3ExperimentRunner {
  public static final long[] DEFAULT_SEEDS = {20260808L, 20260809L, 20260810L};
  public static final int REQUIRED_INSTANCES = 2;
  public static final int REQUIRED_SEEDS = 3;

  private P8V3ExperimentRunner() { }

  public interface InstanceBinding {
    String getName();
    String getInstanceSha256();
    PermutationProblem<PermutationSolution<Integer>> createProblem(
        P8ExperimentSpec spec, long seed);
    /**
     * Builds the common four-vector start for this seed.  The spec is supplied so a
     * canonical factory can attach the matching semantic decoder tag; the hash contract
     * intentionally covers JS/FA/MA/WA only and must therefore remain identical across labels.
     */
    List<PermutationSolution<Integer>> createInitialPopulation(P8ExperimentSpec spec, long seed);
  }

  /** Runs 34 formal labels for each injected instance and seed (204 by default). */
  public static List<P8RunRecord> run(List<InstanceBinding> instances, long[] seeds) {
    if (instances == null || instances.size() != REQUIRED_INSTANCES) {
      throw new IllegalArgumentException("Current shift matrix requires exactly two instance bindings");
    }
    if (seeds == null || seeds.length != REQUIRED_SEEDS) {
      throw new IllegalArgumentException("Current shift matrix requires exactly three seeds");
    }
    List<P8ExperimentSpec> specs = P8ExperimentRegistry.currentMatrix();
    P8ExperimentRegistry.assertCurrentMatrix(specs);
    List<P8RunRecord> records = new ArrayList<>();
    for (InstanceBinding binding : instances) {
      if (binding == null || binding.getName() == null || binding.getInstanceSha256() == null) {
        throw new IllegalArgumentException("P8 instance binding is incomplete");
      }
      for (long seed : seeds) {
        String expectedInitialHash = null;
        Map<String, P8RunRecord> physical = new LinkedHashMap<>();
        for (P8ExperimentSpec spec : specs) {
          List<PermutationSolution<Integer>> initial = binding.createInitialPopulation(spec, seed);
          String initialHash = P8InitialPopulationProvider.sha256(initial);
          if (expectedInitialHash == null) expectedInitialHash = initialHash;
          if (!expectedInitialHash.equals(initialHash)) {
            throw new IllegalStateException("Current shift matrix initial four-vector drift for "
                + binding.getName() + ", seed=" + seed + ", label=" + spec.getLabel());
          }
          String key = binding.getName() + "|" + binding.getInstanceSha256() + "|" + seed
              + "|" + initialHash + "|" + spec.getMechanismVectorHash();
          P8RunRecord source = physical.get(key);
          if (source != null) {
            records.add(P8ExperimentExecutor.aliasIfExact(source, spec, binding.getName(),
                binding.getInstanceSha256(), seed, initialHash));
            continue;
          }
          PermutationProblem<PermutationSolution<Integer>> problem =
              binding.createProblem(spec, seed);
          P8RunRecord record = P8ExperimentExecutor.execute(spec, binding.getName(),
              binding.getInstanceSha256(), seed, problem, P8InitialPopulationProvider.copy(initial));
          records.add(record);
          if (record.getStatus() == P8RunStatus.COMPLETED) physical.put(key, record);
        }
      }
    }
    int expected = expectedRecordCount(instances.size(), seeds.length);
    if (records.size() != expected) {
      throw new IllegalStateException("Current shift matrix record count mismatch: " + records.size()
          + " != " + expected);
    }
    return records;
  }

  public static int expectedRecordCount(int instanceCount, int seedCount) {
    if (instanceCount < 0 || seedCount < 0) throw new IllegalArgumentException("negative count");
    return P8ExperimentRegistry.currentMatrix().size() * instanceCount * seedCount;
  }
}
