package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DhhfspSolutionFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Instance-bound, replayable four-vector factory for the canonical production path. */
public final class ZhangBoCanonicalSolutionFactory
    implements DhhfspSolutionFactory {
  private static final long serialVersionUID = 1L;

  private final ZhangBoFatigueInstanceData instance;
  private final ProductionDecodeMode mode;
  private final long seed;
  private long nextOrdinal;

  public ZhangBoCanonicalSolutionFactory(
      ZhangBoFatigueInstanceData instance, ProductionDecodeMode mode, long seed) {
    if (instance == null || mode == null) {
      throw new IllegalArgumentException("instance and mode must not be null");
    }
    if (mode.isAuthorDiagnostic()) {
      throw new IllegalArgumentException("AUTHOR_DIAGNOSTIC has no production solution factory");
    }
    if (!instance.hasStandardSetupTimes()) {
      throw new IllegalArgumentException("Canonical production requires an instance-bound SUT extension");
    }
    this.instance = instance;
    this.mode = mode;
    this.seed = seed;
  }

  @Override
  public DhhfspFourVectorSolution create() {
    return create(nextOrdinal++);
  }

  public DhhfspFourVectorSolution create(long ordinal) {
    if (ordinal < 0L) throw new IllegalArgumentException("ordinal must be nonnegative");
    int jobs = instance.getJobs();
    List<Integer> js = new ArrayList<>(jobs);
    for (int job = 0; job < jobs; job++) js.add(job);
    for (int index = jobs - 1; index > 0; index--) {
      Collections.swap(js, index, bounded(mix(seed, ordinal, index), index + 1));
    }
    List<Integer> fa = new ArrayList<>(jobs);
    List<Integer> ma = new ArrayList<>(jobs);
    List<Integer> wa = new ArrayList<>(jobs);
    for (int position = 0; position < jobs; position++) {
      long value = mix(seed, ordinal, position);
      int factory = bounded(value, instance.getFactories());
      int[] eligible = instance.getEligibleWorkers(factory, 0);
      fa.add(factory);
      ma.add(bounded(mix(value, 0x4D41L, position),
          instance.getMachineCount(factory, 0)));
      wa.add(eligible[bounded(mix(value, 0x5741L, position), eligible.length)]);
    }
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        js, fa, ma, wa, mode.getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
    validate(result);
    return result;
  }

  public ZhangBoFatigueInstanceData getInstance() { return instance; }
  public ProductionDecodeMode getMode() { return mode; }
  public long getSeed() { return seed; }

  private void validate(PermutationSolution<Integer> solution) {
    if (solution.getVariables().size() != instance.getJobs()
        || solution.getVariablesid().size() != instance.getJobs()
        || solution.getVariablesworker().size() != instance.getJobs()) {
      throw new IllegalStateException("Canonical factory produced a non-P2 four-vector");
    }
    boolean[] seen = new boolean[instance.getJobs()];
    for (int position = 0; position < instance.getJobs(); position++) {
      int job = solution.getVariableValue(position);
      int factory = solution.getVariableValueid(position);
      int machine = ((DhhfspFourVectorSolution) solution).getMachineAssignment(position);
      int worker = solution.getVariableValueworker(position);
      if (job < 0 || job >= instance.getJobs() || seen[job]) {
        throw new IllegalStateException("Canonical factory produced an invalid JS");
      }
      seen[job] = true;
      if (factory < 0 || factory >= instance.getFactories()
          || machine < 0 || machine >= instance.getMachineCount(factory, 0)
          || !instance.isWorkerEligible(factory, 0, worker)) {
        throw new IllegalStateException("Canonical factory produced an invalid first-stage resource");
      }
    }
  }

  private static int bounded(long value, int bound) {
    if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
    return (int) ((value & Long.MAX_VALUE) % bound);
  }

  private static long mix(long seed, long ordinal, long coordinate) {
    long value = seed + 0x9E3779B97F4A7C15L * (ordinal + 1L);
    value ^= 0xBF58476D1CE4E5B9L * (coordinate + 1L);
    value ^= value >>> 30;
    value *= 0xBF58476D1CE4E5B9L;
    value ^= value >>> 27;
    value *= 0x94D049BB133111EBL;
    return value ^ (value >>> 31);
  }
}
