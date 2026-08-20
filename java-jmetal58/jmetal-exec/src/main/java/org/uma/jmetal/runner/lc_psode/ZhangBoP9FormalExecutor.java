package org.uma.jmetal.runner.lc_psode;

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
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunRecord;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunStatus;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNeighborhoodCandidateGateway;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/** Exact Table-9 executor used only by the P9 two-algorithm comparison. */
final class ZhangBoP9FormalExecutor {
  private ZhangBoP9FormalExecutor() { }

  static ZhangBoP9FormalRunResult execute(String alias, String sourceLabel,
      P8ExperimentSpec spec, ZhangBoP9FormalParameters parameters,
      String instanceName, String instanceSha256,
      ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> initialPopulation) {
    return execute(alias, sourceLabel, spec, parameters, instanceName, instanceSha256,
        problem, initialPopulation, null);
  }

  /** Audit overload: a deterministic clock makes cost-credit decisions byte-replayable. */
  static ZhangBoP9FormalRunResult execute(String alias, String sourceLabel,
      P8ExperimentSpec spec, ZhangBoP9FormalParameters parameters,
      String instanceName, String instanceSha256,
      ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> initialPopulation,
      ZhangBoNeighborhoodCandidateGateway.NanoClock auditClock) {
    return execute(alias, sourceLabel, spec, parameters, instanceName, instanceSha256,
        problem, initialPopulation, auditClock, null);
  }

  /** Observation-only overload used by the Cmax diagnostic runner. */
  static ZhangBoP9FormalRunResult execute(String alias, String sourceLabel,
      P8ExperimentSpec spec, ZhangBoP9FormalParameters parameters,
      String instanceName, String instanceSha256,
      ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> initialPopulation,
      ZhangBoNeighborhoodCandidateGateway.NanoClock auditClock,
      ZhangBoCmaxAudit cmaxAudit) {
    if (spec == null || parameters == null || problem == null || initialPopulation == null) {
      throw new IllegalArgumentException("P9 executor arguments must not be null");
    }
    if (problem.getMode().name().equals("AUTHOR_DIAGNOSTIC")) {
      throw new IllegalArgumentException("P9 formal executor forbids author diagnostic mode");
    }
    String initialHash = P8InitialPopulationProvider.sha256(initialPopulation);
    ZhangBoGlobalSearchConfiguration global =
        ZhangBoGlobalSearchConfiguration.forP8(spec.getAblationProfile(),
            parameters.getSeed(), parameters.getRandUpperBound());
    ZhangBoFormalHmopsoQgsConfiguration runtimeFormalBaseline =
        "B0".equals(sourceLabel) || "B1".equals(sourceLabel)
            ? parameters.formalBaselineConfiguration()
            : ZhangBoFormalHmopsoQgsConfiguration.disabled();
    String configurationText = "runnerSchema=zhangbo-p9-single-v1\n"
        + "algorithmAlias=" + alias + "\nsourceP8Label=" + sourceLabel + "\n"
        + "instance=" + instanceName + "\ninstanceSha256=" + instanceSha256 + "\n"
        + "decoderMode=" + problem.getMode() + "\n"
        + parameters.canonicalText()
        + "formalParameterSha256=" + parameters.sha256() + "\n"
        + "requestedBackboneParameterSha256="
        + parameters.formalBaselineConfiguration().sha256() + "\n"
        + "caTaCostClock=" + (auditClock == null ? "REAL_MONOTONIC" : "DETERMINISTIC_AUDIT")
        + "\n"
        + runtimeFormalBaseline.canonicalText()
        + "mechanismVectorHash=" + spec.getMechanismVectorHash() + "\n"
        + spec.getAblationProfile().canonicalText()
        + global.toCanonicalText()
        + "subSwarmSemanticsVersion=" + ZhangBoSubSwarmSemantics.VERSION + "\n"
        + "subSwarmMappingSha256=" + ZhangBoSubSwarmSemantics.mappingHash() + "\n";
    String configurationHash = sha256(configurationText);
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    long startCpu = bean.isCurrentThreadCpuTimeSupported()
        ? bean.getCurrentThreadCpuTime() : -1L;
    long startWall = System.nanoTime();
    long algorithmRunStarted = -1L;
    long algorithmRunNanos = 0L;
    ZhangBoMOHPSOQ algorithm = null;
    try {
      int[] sizes = parameters.getPhysicalSubswarmSizes();
      algorithm = new ZhangBoMOHPSOQBuilder(problem, parameters.getPopulation(),
          problem.getNumberOfFactories(), 0.0, parameters.getGamma(),
          parameters.getEpsilon(), parameters.getQTimes())
          .setMaxIterations(parameters.getMaxFEs())
          .setSwarmSize(parameters.getPopulation())
          .setRand_k(parameters.getRandUpperBound())
          .setCrossoverRate(parameters.getFaCrossover())
          .setCrossoverRates4machine(parameters.getMaCrossover())
          .setCrossoverRates4worker(parameters.getWaCrossover())
          .setMutationRate(parameters.getFaMutation())
          .setMutationRate4machine(parameters.getMaMutation())
          .setMutationRate4worker(parameters.getWaMutation())
          .setLocalSearch(parameters.getLocalSearchTimes())
          .setPhysicalSubswarmSizes(sizes[0], sizes[1], sizes[2], sizes[3])
          .setGlobalSearchConfiguration(global)
          .setFormalBaselineConfiguration(runtimeFormalBaseline)
          .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initialPopulation))
          .build();
      if (auditClock != null) algorithm.setCaTaNanoClock(auditClock);
      if (cmaxAudit != null) algorithm.setCmaxAudit(cmaxAudit);
      JMetalRandom.getInstance().setSeed(parameters.getSeed());
      algorithmRunStarted = System.nanoTime();
      algorithm.run();
      algorithmRunNanos = System.nanoTime() - algorithmRunStarted;
      List<PermutationSolution<Integer>> solutions = algorithm.getResult();
      List<double[]> front = new ArrayList<>();
      int illegal = 0;
      for (PermutationSolution<Integer> solution : solutions) {
        double[] point = {solution.getObjective(0), solution.getObjective(1),
            solution.getObjective(6)};
        if (finite(point)) front.add(point); else illegal++;
      }
      front = P8MetricCalculator.nondominated(front);
      double[] fatigue = fatigueSummary(solutions);
      long endCpu = startCpu < 0L ? -1L : bean.getCurrentThreadCpuTime();
      long experimentWallNanos = System.nanoTime() - startWall;
      String stop = algorithm.getFullEvaluationCount() >= parameters.getMaxFEs()
          ? "MAX_FES_REACHED" : "BUDGET_BEFORE_PARTIAL_GENERATION";
      P8RunRecord record = new P8RunRecord(instanceName, instanceSha256, "P9_SINGLE",
          alias, parameters.getSeed(), P8RunStatus.COMPLETED, stop,
          configurationHash, configurationText, initialHash,
          algorithm.getFullEvaluationCount(),
          experimentWallNanos / 1000000L,
          startCpu < 0L ? -1L : endCpu - startCpu,
          algorithm.getCfvfRepairCount(), algorithm.getCaTaFullEvaluations(), illegal,
          fatigue[0], fatigue[1], fatigue[2], fatigue[3], fatigue[4], fatigue[5],
          fatigue[6], fatigue[7], front);
      return result(record, algorithm, alias, sourceLabel, parameters, spec,
          problem.getDecoderTimingSnapshot(), algorithmRunNanos, experimentWallNanos);
    } catch (RuntimeException exception) {
      if (algorithmRunStarted >= 0L && algorithmRunNanos == 0L) {
        algorithmRunNanos = System.nanoTime() - algorithmRunStarted;
      }
      long endCpu = startCpu < 0L ? -1L : bean.getCurrentThreadCpuTime();
      long experimentWallNanos = System.nanoTime() - startWall;
      long fe = algorithm == null ? 0L : algorithm.getFullEvaluationCount();
      long repairs = algorithm == null ? 0L : algorithm.getCfvfRepairCount();
      long localFe = algorithm == null ? 0L : algorithm.getCaTaFullEvaluations();
      String reason = diagnostic(exception);
      P8RunRecord record = new P8RunRecord(instanceName, instanceSha256, "P9_SINGLE",
          alias, parameters.getSeed(), P8RunStatus.FAILED, reason,
          configurationHash, configurationText, initialHash, fe,
          experimentWallNanos / 1000000L,
          startCpu < 0L ? -1L : endCpu - startCpu, repairs, localFe, 0,
          Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
          Double.NaN, Double.NaN, Double.NaN, new ArrayList<double[]>());
      return algorithm == null
          ? new ZhangBoP9FormalRunResult(record, 0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
              "unavailable", "unavailable",
              spec.getMechanismVectorHash(), reason, algorithmRunNanos,
              experimentWallNanos, problem.getDecoderTimingSnapshot())
          : result(record, algorithm, alias, sourceLabel, parameters, spec,
              problem.getDecoderTimingSnapshot(), algorithmRunNanos, experimentWallNanos);
    }
  }

  private static ZhangBoP9FormalRunResult result(P8RunRecord record,
      ZhangBoMOHPSOQ algorithm, String alias, String sourceLabel,
      ZhangBoP9FormalParameters parameters, P8ExperimentSpec spec,
      ZhangBoDecoderTimingSnapshot decoderTiming, long algorithmRunNanos,
      long experimentWallNanos) {
    long baselineEvents = algorithm.getBaselineUpdateEventCount();
    long fixedEvents = algorithm.getFixedNeighborhoodEventCount();
    long pddrEvents = algorithm.getZhangBoPddrEventCount();
    long archiveInsertions = algorithm.getZhangBoArchiveInsertionCount();
    long lineageEvents = algorithm.getZhangBoLineageEventCount();
    long caTaEvents = algorithm.getCaTaEventCount();
    String summary = "schema=zhangbo-p9-mechanism-summary-v2\n"
        + "algorithmAlias=" + alias + "\nsourceP8Label=" + sourceLabel + "\n"
        + "ablationSemanticsVersion=" + P8AblationProfile.VERSION + "\n"
        + "formalBaselineSemanticsVersion="
        + ZhangBoFormalHmopsoQgsConfiguration.SEMANTICS_VERSION + "\n"
        + "formalBaselineRuntimeEnabled="
        + algorithm.getFormalBaselineConfiguration().isEnabled() + "\n"
        + "semanticTag=" + algorithm.getGlobalSearchConfiguration().getSemanticTag() + "\n"
        + "mechanismVectorHash=" + spec.getMechanismVectorHash() + "\n"
        + "formalParameterSha256=" + parameters.sha256() + "\n"
        + "runtimeResourceCognitiveScale="
        + algorithm.getGlobalSearchConfiguration().getResourceCognitiveScale() + "\n"
        + "runtimeResourceSocialScale="
        + algorithm.getGlobalSearchConfiguration().getResourceSocialScale() + "\n"
        + "formalOuterCycles=" + algorithm.getFormalBaselineOuterCycles() + "\n"
        + "formalQgRounds=" + algorithm.getFormalBaselineQgRounds() + "\n"
        + "formalCriticalFactorySwapEvaluations="
        + algorithm.getFormalCriticalFactorySwapEvaluations() + "\n"
        + "formalCriticalFactoryInsertEvaluations="
        + algorithm.getFormalCriticalFactoryInsertEvaluations() + "\n"
        + "formalO1O9Evaluations="
        + algorithm.getFormalOriginalNeighborhoodEvaluations() + "\n"
        + "fullEvaluations=" + algorithm.getFullEvaluationCount() + "\n"
        + "cfvfOffspring=" + algorithm.getCfvfOffspringCount() + "\n"
        + "cfvfRepairs=" + algorithm.getCfvfRepairCount() + "\n"
        + "pddrEvents=" + pddrEvents + "\nbaselineUpdateEvents=" + baselineEvents + "\n"
        + "fixedNeighborhoodEvents=" + fixedEvents + "\n"
        + "archiveInsertions=" + archiveInsertions + "\nlineageEvents=" + lineageEvents + "\n"
        + "qgSelections=" + algorithm.getQgSelectionCount() + "\n"
        + "qgTdUpdates=" + algorithm.getQgTdUpdateCount() + "\n"
        + "qgTableHash=" + algorithm.getQgTableHash() + "\n"
        + "qpActions=" + algorithm.getQpExecutedActionCount() + "\n"
        + "qpTransitions=" + algorithm.getQpTrainedTransitionCount() + "\n"
        + "qpSwitches=" + algorithm.getQpPbestSwitches() + "\n"
        + "qpTableHash=" + algorithm.getQpTableHash() + "\n"
        + "dualQEvents=" + algorithm.getDualQEventCount() + "\n"
        + "caTaEvents=" + caTaEvents + "\ncaTaTestCalls=" + algorithm.getCaTaTestCalls() + "\n"
        + "caTaApplyCalls=" + algorithm.getCaTaApplyCalls() + "\n"
        + "localFullEvaluations=" + algorithm.getCaTaFullEvaluations() + "\n"
        + "algorithmRunNanos=" + algorithmRunNanos + "\n"
        + "experimentWallNanos=" + experimentWallNanos + "\n"
        + "successfulDecoderCalls=" + decoderTiming.getSuccessfulDecoderCalls() + "\n"
        + "baseDecodeNanos=" + decoderTiming.getBaseDecodeNanos() + "\n"
        + "leftShiftNanos=" + decoderTiming.getLeftShiftNanos() + "\n"
        + "rightShiftNanos=" + decoderTiming.getRightShiftNanos() + "\n"
        + "decoderTotalNanos=" + decoderTiming.getDecoderTotalNanos() + "\n"
        + "decoderFrameworkOverheadNanos="
        + decoderTiming.getDecoderFrameworkOverheadNanos() + "\n"
        + "leftFullRecomputations=" + decoderTiming.getLeftFullRecomputations() + "\n"
        + "rightFullRecomputations=" + decoderTiming.getRightFullRecomputations() + "\n"
        + "leftAccepted=" + decoderTiming.getLeftAccepted() + "\n"
        + "rightAccepted=" + decoderTiming.getRightAccepted() + "\n"
        + "p6EventStreamHash=" + algorithm.getZhangBoP6EventStreamHash() + "\n"
        + "pddrEventStreamHash=" + algorithm.getZhangBoPddrEventStreamHash() + "\n"
        + "lineageEventStreamHash=" + algorithm.getZhangBoLineageEventStreamHash() + "\n"
        + "qgEventStreamHash=" + algorithm.getQgEventStreamHash() + "\n"
        + "qpEventStreamHash=" + algorithm.getQpEventStreamHash() + "\n"
        + "dualQEventStreamHash=" + algorithm.getDualQEventStreamHash() + "\n"
        + "caTaEventStreamHash=" + algorithm.getCaTaEventStreamHash() + "\n";
    return new ZhangBoP9FormalRunResult(record, algorithm.getCfvfOffspringCount(),
        pddrEvents, baselineEvents, fixedEvents, archiveInsertions, lineageEvents,
        algorithm.getQgSelectionCount(), algorithm.getQgTdUpdateCount(),
        algorithm.getQpExecutedActionCount(), algorithm.getQpTrainedTransitionCount(),
        algorithm.getQpPbestSwitches(), algorithm.getDualQEventCount(),
        caTaEvents, algorithm.getCaTaTestCalls(), algorithm.getCaTaApplyCalls(),
        algorithm.getFormalBaselineOuterCycles(), algorithm.getFormalBaselineQgRounds(),
        algorithm.getFormalCriticalFactorySwapEvaluations(),
        algorithm.getFormalCriticalFactoryInsertEvaluations(),
        algorithm.getFormalOriginalNeighborhoodEvaluations(),
        algorithm.getQgTableHash(), algorithm.getQpTableHash(),
        spec.getMechanismVectorHash(), summary, algorithmRunNanos,
        experimentWallNanos, decoderTiming);
  }

  private static long countContains(List<String> events, String token) {
    long count = 0L;
    for (String event : events) if (event.contains(token)) count++;
    return count;
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
      ZhangBoFatigueEvaluationResult evaluation = (ZhangBoFatigueEvaluationResult) value;
      ZhangBoFatigueMetrics metrics = evaluation.getMetrics();
      max = Math.max(max, metrics.maximumFatigue);
      sum[1] += metrics.averageEventFatigue;
      sum[2] += metrics.fatigueExcessIntegral;
      sum[3] += metrics.workerFatigueVarianceAtMakespan;
      sum[4] += metrics.highFatigueTimeRatio;
      sum[5] += metrics.longestContinuousWork;
      sum[6] += metrics.totalNaturalRecovery;
      sum[0] += loadImbalance(evaluation);
      count++;
    }
    if (count == 0) return result;
    result[0] = max;
    for (int index = 1; index < 7; index++) result[index] = sum[index] / count;
    result[7] = sum[0] / count;
    return result;
  }

  private static double loadImbalance(ZhangBoFatigueEvaluationResult result) {
    Map<String, Double> workload = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : result.getOperations()) {
      String key = operation.factory + ":" + operation.worker;
      Double previous = workload.get(key);
      workload.put(key, (previous == null ? 0.0 : previous) + operation.actualDuration);
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
    for (double value : values) if (!Double.isFinite(value)) return false;
    return true;
  }

  private static String diagnostic(RuntimeException exception) {
    StringBuilder result = new StringBuilder(exception.getClass().getName())
        .append(": ").append(exception.getMessage());
    StackTraceElement[] trace = exception.getStackTrace();
    for (int index = 0; index < Math.min(8, trace.length); index++) {
      result.append(" | at ").append(trace[index]);
    }
    return result.toString();
  }

  private static String sha256(String text) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
