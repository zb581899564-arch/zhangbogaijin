package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/** Executes one exact P8 configuration with a caller-provided fixed initial population. */
public final class P8ExperimentExecutor {
  private P8ExperimentExecutor() { }

  public static P8RunRecord execute(P8ExperimentSpec spec, String instance,
      String instanceSha256, long seed,
      PermutationProblem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation) {
    if (spec == null) throw new IllegalArgumentException("spec cannot be null");
    if (initialPopulation == null) throw new IllegalArgumentException("initialPopulation cannot be null");
    if (spec.isDiagnosticOnly()) {
      return diagnosticOnly(spec, instance, instanceSha256, seed,
          P8InitialPopulationProvider.sha256(initialPopulation));
    }
    if (spec.getStatus() != P8RunStatus.COMPLETED) {
      return unavailable(spec, instance, instanceSha256, seed,
          P8InitialPopulationProvider.sha256(initialPopulation));
    }
    String initialHash = P8InitialPopulationProvider.sha256(initialPopulation);
    ZhangBoGlobalSearchConfiguration configuration =
        P8ExperimentRegistry.configurationFor(spec, seed);
    ZhangBoFormalHmopsoQgsConfiguration table9 =
        ZhangBoFormalHmopsoQgsConfiguration.table9();
    ZhangBoFormalHmopsoQgsConfiguration runtimeFormalBaseline =
        isFormalBaselineControl(spec)
            ? ZhangBoFormalHmopsoQgsConfiguration.engineeringAudit()
            : ZhangBoFormalHmopsoQgsConfiguration.disabled();
    String configurationText = spec.getAblationProfile().canonicalText()
        + "mechanismVectorHash=" + spec.getMechanismVectorHash() + "\n"
        + "populationSize=" + spec.getPopulationSize() + "\n"
        + "maxFEs=" + spec.getMaxFEs() + "\n"
        + "physicalSubswarmSizes=" + java.util.Arrays.toString(
        spec.getPhysicalSubswarmSizes()) + "\n" + configuration.toCanonicalText()
        + "p8SwitchAuditRuntime=true\n"
        + "requestedTable9BackboneSha256=" + table9.sha256() + "\n"
        + runtimeFormalBaseline.canonicalText()
        + "subSwarmSemanticsVersion=" + ZhangBoSubSwarmSemantics.VERSION + "\n"
        + "subSwarmMappingSha256=" + ZhangBoSubSwarmSemantics.mappingHash() + "\n";
    String configurationHash = sha256(configurationText);
    long startWall = System.nanoTime();
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    long startCpu = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1L;
    ZhangBoMOHPSOQ algorithm = null;
    try {
      int[] sizes = spec.getPhysicalSubswarmSizes();
      algorithm = new ZhangBoMOHPSOQBuilder(
          problem, spec.getPopulationSize(), 3, 0.0,
          table9.getGamma(), table9.getEpsilon(), table9.getQTimes())
          .setMaxIterations(spec.getMaxFEs())
          .setSwarmSize(spec.getPopulationSize())
          .setRand_k(table9.getRandomCoefficientUpperBound())
          .setCrossoverRate(table9.getFaCrossover())
          .setMutationRate(table9.getFaMutation())
          .setCrossoverRates4worker(table9.getWaCrossover())
          .setCrossoverRates4machine(table9.getMaCrossover())
          .setMutationRate4worker(table9.getWaMutation())
          .setMutationRate4machine(table9.getMaMutation())
          .setLocalSearch(table9.getLocalSearchTimes())
          .setPhysicalSubswarmSizes(sizes[0], sizes[1], sizes[2], sizes[3])
          .setGlobalSearchConfiguration(configuration)
          .setFormalBaselineConfiguration(runtimeFormalBaseline)
          .setInitialSwarmOverride(initialPopulation)
          .build();
      JMetalRandom.getInstance().setSeed(seed);
      algorithm.run();
      List<PermutationSolution<Integer>> solutions = algorithm.getResult();
      List<double[]> front = new ArrayList<>();
      int illegal = 0;
      for (PermutationSolution<Integer> solution : solutions) {
        double[] point = {solution.getObjective(0), solution.getObjective(1), solution.getObjective(6)};
        if (!finite(point)) illegal++;
        else front.add(point);
      }
      front = P8MetricCalculator.nondominated(front);
      double[] fatigue = fatigueSummary(solutions);
      long endCpu = startCpu < 0 ? -1L : bean.getCurrentThreadCpuTime();
      long wall = (System.nanoTime() - startWall) / 1000000L;
      String stop = algorithm.getFullEvaluationCount() >= spec.getMaxFEs()
          ? "MAX_FES_REACHED" : "BUDGET_BEFORE_PARTIAL_GENERATION";
      return new P8RunRecord(instance, instanceSha256, spec.getMatrix().name(),
          spec.getLabel(), seed, P8RunStatus.COMPLETED, stop, configurationHash,
          configurationText, initialHash, algorithm.getFullEvaluationCount(), wall,
          startCpu < 0 ? -1L : endCpu - startCpu, algorithm.getCfvfRepairCount(),
          algorithm.getCaTaFullEvaluations(), illegal,
          fatigue[0], fatigue[1], fatigue[2], fatigue[3], fatigue[4], fatigue[5],
          fatigue[6], fatigue[7], front);
    } catch (RuntimeException exception) {
      long endCpu = startCpu < 0 ? -1L : bean.getCurrentThreadCpuTime();
      StringBuilder diagnostic = new StringBuilder(exception.getClass().getName())
          .append(": ").append(exception.getMessage());
      StackTraceElement[] trace = exception.getStackTrace();
      for (int index = 0; index < Math.min(6, trace.length); index++) {
        diagnostic.append(" | at ").append(trace[index]);
      }
      long partialEvaluations = algorithm == null ? 0L : algorithm.getFullEvaluationCount();
      long partialRepairs = algorithm == null ? 0L : algorithm.getCfvfRepairCount();
      long partialCaTa = algorithm == null ? 0L : algorithm.getCaTaFullEvaluations();
      return new P8RunRecord(instance, instanceSha256, spec.getMatrix().name(),
          spec.getLabel(), seed, P8RunStatus.FAILED,
          diagnostic.toString(), configurationHash,
          configurationText, initialHash, partialEvaluations,
          (System.nanoTime() - startWall) / 1000000L,
          startCpu < 0 ? -1L : endCpu - startCpu, partialRepairs, partialCaTa, 0,
          Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
          Double.NaN, Double.NaN, Double.NaN, new ArrayList<double[]>());
    }
  }

  /** Alias helper used by v3 runners; it refuses profile-hash drift before reusing sourceRunId. */
  public static P8RunRecord aliasIfExact(P8RunRecord source, P8ExperimentSpec target) {
    if (source == null || target == null) throw new IllegalArgumentException("source/target null");
    if (!source.getMechanismVectorHash().equals(target.getMechanismVectorHash())) {
      throw new IllegalArgumentException("P8 sourceRunId reuse requires identical mechanism hash");
    }
    return source.alias(target);
  }

  public static P8RunRecord aliasIfExact(P8RunRecord source, P8ExperimentSpec target,
      String instance, String instanceSha256, long seed, String initialPopulationSha256) {
    if (source == null || target == null) throw new IllegalArgumentException("source/target null");
    if (!source.getMechanismVectorHash().equals(target.getMechanismVectorHash())) {
      throw new IllegalArgumentException("P8 sourceRunId reuse requires identical mechanism hash");
    }
    return source.alias(target, instance, instanceSha256, seed, initialPopulationSha256);
  }

  public static P8RunRecord unavailableForInstance(P8ExperimentSpec spec, String instance,
      String instanceSha256, long seed, String reason) {
    return new P8RunRecord(instance, instanceSha256, spec.getMatrix().name(),
        spec.getLabel(), seed, P8RunStatus.NOT_EXPOSED, reason, "",
        spec.canonicalText(), "NOT_AVAILABLE", 0L, 0L, 0L, 0L, 0L, 0,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN, Double.NaN, new ArrayList<double[]>());
  }

  private static P8RunRecord unavailable(P8ExperimentSpec spec, String instance,
      String instanceSha256, long seed, String initialHash) {
    return new P8RunRecord(instance, instanceSha256, spec.getMatrix().name(),
        spec.getLabel(), seed, P8RunStatus.NOT_EXPOSED, spec.getReason(), "",
        spec.canonicalText(), initialHash, 0L, 0L, 0L, 0L, 0L, 0, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        new ArrayList<double[]>());
  }

  private static P8RunRecord diagnosticOnly(P8ExperimentSpec spec, String instance,
      String instanceSha256, long seed, String initialHash) {
    return new P8RunRecord(instance, instanceSha256, spec.getMatrix().name(), spec.getLabel(),
        seed, P8RunStatus.DIAGNOSTIC_ONLY, spec.getReason(), "",
        spec.canonicalText(), initialHash, 0L, 0L, 0L, 0L, 0L, 0,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN, Double.NaN, new ArrayList<double[]>());
  }

  private static double[] fatigueSummary(List<PermutationSolution<Integer>> solutions) {
    double[] result = {Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    int count = 0;
    double max = 0.0;
    double[] sum = new double[7];
    for (PermutationSolution<Integer> solution : solutions) {
      Object value = solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
      if (!(value instanceof ZhangBoFatigueEvaluationResult)) continue;
      ZhangBoFatigueMetrics m = ((ZhangBoFatigueEvaluationResult) value).getMetrics();
      max = Math.max(max, m.maximumFatigue);
      sum[1] += m.averageEventFatigue;
      sum[2] += m.fatigueExcessIntegral;
      sum[3] += m.workerFatigueVarianceAtMakespan;
      sum[4] += m.highFatigueTimeRatio;
      sum[5] += m.longestContinuousWork;
      sum[6] += m.totalNaturalRecovery;
      sum[0] += loadImbalance((ZhangBoFatigueEvaluationResult) value);
      count++;
    }
    if (count == 0) return result;
    result[0] = max;
    for (int i = 1; i < 7; i++) result[i] = sum[i] / count;
    result[7] = sum[0] / count;
    return result;
  }

  private static double loadImbalance(ZhangBoFatigueEvaluationResult result) {
    Map<String, Double> workload = new HashMap<>();
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      String key = record.factory + ":" + record.worker;
      Double previous = workload.get(key);
      workload.put(key, (previous == null ? 0.0 : previous) + record.actualDuration);
    }
    if (workload.isEmpty()) return 0.0;
    double mean = 0.0;
    for (double value : workload.values()) mean += value;
    mean /= workload.size();
    if (mean <= 1e-12) return 0.0;
    double variance = 0.0;
    for (double value : workload.values()) variance += (value - mean) * (value - mean);
    return Math.sqrt(variance / workload.size()) / mean;
  }

  private static boolean finite(double[] values) {
    for (double value : values) if (Double.isNaN(value) || Double.isInfinite(value)) return false;
    return true;
  }

  private static String sha256(String value) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static boolean isFormalBaselineControl(P8ExperimentSpec spec) {
    return spec.getMatrix() == P8MatrixKind.FULL
        && ("B0".equals(spec.getLabel()) || "B1".equals(spec.getLabel()));
  }
}
