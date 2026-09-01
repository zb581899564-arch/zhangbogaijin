package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;

/**
 * Small fair-run bridge for the v3.5 line.  It accepts an explicitly created
 * initial population so baseline and FULL cannot silently start from different
 * genotypes.  This class is intentionally an engineering runner, not a P9
 * formal-matrix launcher.
 */
public final class V35FairRunner {
  public enum Mode { V35_BASELINE, V35_FULL, V35_QG0, V35_QG1, V35_FULL_POOL_OFF, V35_A2, V35_A3 }

  public static final class RunRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Mode mode;
    private final String initialPopulationHash;
    private final int fullEvaluations;
    private final int decoderCalls;
    private final int illegalSolutions;
    private final int duplicateEvaluations;
    private final List<double[]> front;
    private final String status;
    private final String stopReason;
    private final String mechanismSummary;
    private final String runtimeSubSwarmSizes;
    private final long algorithmRunNanos;
    private final ZhangBoDecoderTimingSnapshot decoderTiming;
    private final ZhangBoCmaxAudit cmaxAudit;
    private final String dscrEvents;
    private final String dscrTeacherUses;
    private final String caTaEvents;
    private final String pressureEvents;
    private final String shadowProbes;
    private final long shadowSamples;
    private final int shadowEvaluations;
    private final String passiveArchiveCsv;
    private final long passiveObservedCount;
    private final int passiveArchiveSize;
    private final String instanceSha256;
    private final String instanceExtensionSha256;
    private final String fatigueConfigurationSha256;
    private final String formalBaselineSha256;
    private final String formalBaselineCanonicalText;
    private RunRecord(Mode mode, String initialPopulationHash, int fullEvaluations,
        int decoderCalls, int illegalSolutions, int duplicateEvaluations,
        List<double[]> front, String status, String stopReason, String mechanismSummary,
        String runtimeSubSwarmSizes,
        long algorithmRunNanos, ZhangBoDecoderTimingSnapshot decoderTiming,
        ZhangBoCmaxAudit cmaxAudit, String dscrEvents, String dscrTeacherUses,
        String caTaEvents, String pressureEvents, String shadowProbes,
        long shadowSamples, int shadowEvaluations,
        String passiveArchiveCsv, long passiveObservedCount,
        int passiveArchiveSize, String instanceSha256, String instanceExtensionSha256,
        String fatigueConfigurationSha256, String formalBaselineSha256,
        String formalBaselineCanonicalText) {
      this.mode = mode; this.initialPopulationHash = initialPopulationHash;
      this.fullEvaluations = fullEvaluations; this.decoderCalls = decoderCalls;
      this.illegalSolutions = illegalSolutions;
      this.duplicateEvaluations = duplicateEvaluations;
      this.front = front;
      this.status = status; this.stopReason = stopReason; this.mechanismSummary = mechanismSummary;
      this.runtimeSubSwarmSizes = runtimeSubSwarmSizes;
      this.algorithmRunNanos = algorithmRunNanos;
      this.decoderTiming = decoderTiming;
      this.cmaxAudit = cmaxAudit;
      this.dscrEvents = dscrEvents;
      this.dscrTeacherUses = dscrTeacherUses;
      this.caTaEvents = caTaEvents;
      this.pressureEvents = pressureEvents;
      this.shadowProbes = shadowProbes;
      this.shadowSamples = shadowSamples;
      this.shadowEvaluations = shadowEvaluations;
      this.passiveArchiveCsv = passiveArchiveCsv;
      this.passiveObservedCount = passiveObservedCount;
      this.passiveArchiveSize = passiveArchiveSize;
      this.instanceSha256 = instanceSha256;
      this.instanceExtensionSha256 = instanceExtensionSha256;
      this.fatigueConfigurationSha256 = fatigueConfigurationSha256;
      this.formalBaselineSha256 = formalBaselineSha256;
      this.formalBaselineCanonicalText = formalBaselineCanonicalText;
    }
    public Mode getMode() { return mode; }
    public String getInitialPopulationHash() { return initialPopulationHash; }
    public int getFullEvaluations() { return fullEvaluations; }
    public int getDecoderCalls() { return decoderCalls; }
    public int getIllegalSolutions() { return illegalSolutions; }
    public int getDuplicateEvaluations() { return duplicateEvaluations; }
    public List<double[]> getFront() { return front; }
    public String getStatus() { return status; }
    public String getStopReason() { return stopReason; }
    public String getMechanismSummary() { return mechanismSummary; }
    public String getRuntimeSubSwarmSizes() { return runtimeSubSwarmSizes; }
    public long getAlgorithmRunNanos() { return algorithmRunNanos; }
    public ZhangBoDecoderTimingSnapshot getDecoderTiming() { return decoderTiming; }
    public ZhangBoCmaxAudit getCmaxAudit() { return cmaxAudit; }
    public String getDscrEvents() { return dscrEvents; }
    public String getDscrTeacherUses() { return dscrTeacherUses; }
    public String getCaTaEvents() { return caTaEvents; }
    public String getPressureEvents() { return pressureEvents; }
    public String getShadowProbes() { return shadowProbes; }
    public long getShadowSamples() { return shadowSamples; }
    public int getShadowEvaluations() { return shadowEvaluations; }
    public String getPassiveArchiveCsv() { return passiveArchiveCsv; }
    public long getPassiveObservedCount() { return passiveObservedCount; }
    public int getPassiveArchiveSize() { return passiveArchiveSize; }
    public String getInstanceSha256() { return instanceSha256; }
    public String getInstanceExtensionSha256() { return instanceExtensionSha256; }
    public String getFatigueConfigurationSha256() { return fatigueConfigurationSha256; }
    public String getFormalBaselineSha256() { return formalBaselineSha256; }
    public String getFormalBaselineCanonicalText() { return formalBaselineCanonicalText; }
  }

  private V35FairRunner() { }

  public static RunRecord run(Mode mode, Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed) {
    return run(mode, problem, initialPopulation, maxEvaluations, seed, true);
  }

  public static RunRecord run(Mode mode, Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive) {
    return run(mode, problem, initialPopulation, maxEvaluations, seed, attachPassiveArchive,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow());
  }

  public static RunRecord run(Mode mode, Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration) {
    return run(mode, problem, initialPopulation, maxEvaluations, seed, attachPassiveArchive,
        diagnosisConfiguration, false);
  }

  public static RunRecord run(Mode mode, Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      boolean allowTerminalPartialFormalQPhase) {
    return run(mode, problem, initialPopulation, maxEvaluations, seed, attachPassiveArchive,
        diagnosisConfiguration, allowTerminalPartialFormalQPhase, null);
  }

  /**
   * Executes a V35 arm with an explicitly frozen configuration.  This is used
   * by FC-6 so PDDR mode and local-search order cannot be inherited from the
   * historical BP compatibility default.  The public legacy overload above
   * deliberately continues to construct its archival configuration unchanged.
   */
  public static RunRecord run(Mode mode, Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      boolean allowTerminalPartialFormalQPhase,
      V35ProductionConfiguration explicitConfiguration) {
    if (mode == null || problem == null || initialPopulation == null || initialPopulation.isEmpty()
        || diagnosisConfiguration == null) {
      throw new IllegalArgumentException("mode/problem/initialPopulation must be non-empty");
    }
    if (!(problem instanceof ZhangBoCanonicalProductionProblem)) {
      throw new IllegalArgumentException("V35 fairness requires ZhangBoCanonicalProductionProblem");
    }
    ZhangBoCanonicalProductionProblem canonical = (ZhangBoCanonicalProductionProblem) problem;
    if (canonical.getMode() != ProductionDecodeMode.FM3
        || canonical.getShiftConfiguration().getMode() != ZhangBoShiftMode.NONE
        || !canonical.getSetupModel().isFormalDegenerate()) {
      throw new IllegalArgumentException("V35 fairness requires FM3, single-family setup and ShiftMode.NONE");
    }
    if (maxEvaluations < initialPopulation.size()) throw new IllegalArgumentException("budget < initial population");
    String hash = initialHash(initialPopulation);
    // Run provenance: bind the evidence to the exact instance, SUT extension,
    // fatigue parameters and Table 9 formal baseline used by this run.
    String instanceSha = canonical.getInstance().getInstanceSha256();
    String extensionSha = canonical.getInstance().getInstanceExtensionSha256();
    String fatigueSha = canonical.getParameters().getConfigurationSha256();
    org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration table9 =
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration.table9();
    String table9Sha = table9.sha256();
    String table9Text = table9.canonicalText();
    // The legacy jMetal singleton is still used by the shared compatibility
    // path. Reset it at the run boundary so one V35 run cannot inherit the
    // previous run's random state.
    JMetalRandom.getInstance().setSeed(seed);
    boolean full = mode == Mode.V35_FULL || mode == Mode.V35_FULL_POOL_OFF;
    // QG0/QG1 are the one-variable DSCR pairing: both retain original Qg;
    // only DSCR sanitation changes.
    // V35-P21 ablation ladder rungs: V35_A2 = QG1 + CFVF, V35_A3 = A2 + Q-pbest.
    // Both follow the partial order qp=>cfvf=>qg and keep caTaLite/pool off.
    boolean qg = true;
    boolean dscr = full || mode == Mode.V35_QG1 || mode == Mode.V35_A2 || mode == Mode.V35_A3;
    boolean cfvf = full || mode == Mode.V35_A2 || mode == Mode.V35_A3;
    boolean qp = full || mode == Mode.V35_A3;
    boolean caTaLite = full;
    // V35_FULL_POOL_OFF is the FULL ablation arm with the directional pool off;
    // it must replay the pre-P10.1 FULL behaviour bit for bit.
    boolean directionalPool = mode == Mode.V35_FULL;
    V35ProductionConfiguration config = explicitConfiguration == null
        ? V35ProductionConfiguration.builder()
            .seed(seed).populationSize(initialPopulation.size()).maxEvaluations(maxEvaluations)
            .decoderMode(org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.FM3)
            .dscr(dscr).cfvf(cfvf).qg(qg)
            .qp(qp).caTaLite(caTaLite)
            .bottleneckDiagnosis(diagnosisConfiguration)
            // V35-P10.1: the directional top-k teacher pool is a FULL-only improvement.
            // BASELINE and the QG0/QG1 DSCR pairing keep it off so their semantics stay untouched.
            .directionalTeacherPool(directionalPool).teacherPoolSize(10).build()
        : explicitConfiguration;
    requireExplicitConfiguration(mode, config, seed, initialPopulation.size(), maxEvaluations,
        diagnosisConfiguration);
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, initialPopulation.size(),
        problem instanceof org.uma.jmetal.problem.PermutationProblem
            ? ((org.uma.jmetal.problem.PermutationProblem<?>) problem).getNumberOfFactories() : 2,
        0.6, 0.5, 0.5, 50).setV35Configuration(config)
        .setMaxIterations(maxEvaluations).setInitialSwarmOverride(copy(initialPopulation)).build();
    ZhangBoCmaxAudit cmaxAudit = new ZhangBoCmaxAudit(1000L);
    algorithm.setAllowTerminalPartialFormalQPhase(allowTerminalPartialFormalQPhase);
    algorithm.setCmaxAudit(cmaxAudit);
    V35PassiveEvaluationArchive passiveArchive =
        attachPassiveArchive ? new V35PassiveEvaluationArchive() : null;
    if (passiveArchive != null) algorithm.setPassiveEvaluationArchive(passiveArchive);
    // V35-CHECKPOINT-OBSERVER-PATCH (2026-08-31): wire observation-only hook
    // (no-op unless armed from the checkpoint runner).
    V35CheckpointObserverHook.attach(canonical, algorithm);
    long algorithmStart = System.nanoTime();
    try {
      algorithm.run();
      long algorithmNanos = System.nanoTime() - algorithmStart;
      List<double[]> front = new ArrayList<>();
      List<PermutationSolution<Integer>> result = algorithm.getResult();
      for (PermutationSolution<Integer> solution : result) {
        front.add(new double[]{solution.getObjective(0), solution.getObjective(1), solution.getObjective(6)});
      }
      int evaluations = maxEvaluations;
      if (problem instanceof org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem) {
        evaluations = (int) ((org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem) problem)
            .getEvaluationCounter().getSuccessfulEvaluations();
      }
      String summary = "p6EventsTotal=" + algorithm.getZhangBoP6EventCount()
          + ",p6EventsRetained=" + algorithm.getZhangBoP6Events().size()
          + ",p6EventStreamHash=" + algorithm.getZhangBoP6EventStreamHash()
          + ",formalOuterCycles=" + algorithm.getFormalBaselineOuterCycles()
          + ",formalQgRounds=" + algorithm.getFormalBaselineQgRounds()
          + ",baselineUpdateEvents=" + algorithm.getBaselineUpdateEventCount()
          + ",fixedNeighborhoodEvents=" + algorithm.getFixedNeighborhoodEventCount()
          + ",pddrEvents=" + algorithm.getZhangBoPddrEventCount()
          + ",pddrEventStreamHash=" + algorithm.getZhangBoPddrEventStreamHash()
          + ",qgSelections=" + algorithm.getQgSelectionCount()
          + ",qgTdUpdates=" + algorithm.getQgTdUpdateCount()
          + ",qgEventStreamHash=" + algorithm.getQgEventStreamHash()
          + ",qgTableHash=" + algorithm.getQgTableHash()
          + ",qpActions=" + algorithm.getQpExecutedActionCount()
          + ",qpTransitions=" + algorithm.getQpTrainedTransitionCount()
          + ",qpEventStreamHash=" + algorithm.getQpEventStreamHash()
          + ",qpTableHash=" + algorithm.getQpTableHash()
          + ",cfvfOffspring=" + algorithm.getCfvfOffspringCount()
          + ",cfvfRepairs=" + algorithm.getCfvfRepairCount()
          + ",archiveInsertions=" + algorithm.getZhangBoArchiveInsertionCount()
          + ",directionalPoolRequests=" + algorithm.getDirectionalTeacherPoolRequestCount()
          + ",directionalPoolFiltered=" + algorithm.getDirectionalTeacherPoolFilteredCount()
          + ",caTaLiteTest=" + algorithm.getCaTaTestCalls()
          + ",caTaLiteApply=" + algorithm.getCaTaApplyCalls()
          + ",caTaLiteFE=" + (algorithm.getCaTaTestCalls() + algorithm.getCaTaApplyCalls())
          + ",caTaEventStreamHash=" + algorithm.getCaTaEventStreamHash()
          + ",pressureDiagnosisEvents=" + algorithm.getV35PressureDiagnosisEventCount()
          + ",shadowSamples=" + algorithm.getV35ShadowDiagnosisSamples()
          + ",shadowEvaluations=" + algorithm.getV35ShadowDiagnosisEvaluations()
          + ",formalLocalFE=" + (algorithm.getFormalCriticalFactorySwapEvaluations()
              + algorithm.getFormalCriticalFactoryInsertEvaluations()
              + algorithm.getFormalOriginalNeighborhoodEvaluations())
          + ",dualQWarmup=" + algorithm.getDualQPhaseCount(
              org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.WARMUP)
          + ",dualQP=" + algorithm.getDualQPhaseCount(
              org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.P_BLOCK)
          + ",dualQG=" + algorithm.getDualQPhaseCount(
              org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.G_BLOCK)
          + ",dscr=" + algorithm.getV35DscrTeacherStatistics()
          + ",algorithmRunNanos=" + algorithmNanos
          + ",decoder=" + timingText(canonical.getDecoderTimingSnapshot());
      cmaxAudit.finish(evaluations, result, result);
      // V35-CHECKPOINT-OBSERVER-PATCH (2026-08-31): terminal snapshots (no-op unless armed).
      V35CheckpointObserverHook.recordTerminal(result, passiveArchive);
      int decoderCalls = (int) canonical.getDecoderTimingSnapshot().getSuccessfulDecoderCalls();
      return new RunRecord(mode, hash, evaluations, decoderCalls,
          canonical.getEvaluationObservation().getIllegalSolutions(),
          canonical.getEvaluationObservation().getDuplicateEvaluations(),
          front, "COMPLETED", "BUDGET_OR_NORMAL_STOP", summary,
          algorithm.getRuntimeSubSwarmSizes(),
          algorithmNanos, canonical.getDecoderTimingSnapshot(), cmaxAudit,
          algorithm.getV35DscrEventsCsv(), algorithm.getV35DscrTeacherUsesCsv(),
          String.join("\n", algorithm.getCaTaEvents()),
          algorithm.getV35PressureDiagnosisEventsCsv(), algorithm.getV35ShadowDiagnosisCsv(),
          algorithm.getV35ShadowDiagnosisSamples(), algorithm.getV35ShadowDiagnosisEvaluations(),
          passiveArchive == null ? "" : passiveArchive.toCsv(),
          passiveArchive == null ? 0L : passiveArchive.getObservedCount(),
          passiveArchive == null ? 0 : passiveArchive.size(),
          instanceSha, extensionSha, fatigueSha, table9Sha, table9Text);
    } catch (RuntimeException error) {
      int evaluations = 0;
      if (problem instanceof org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem) {
        evaluations = (int) ((org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem) problem)
            .getEvaluationCounter().getSuccessfulEvaluations();
      }
      int decoderCalls = (int) canonical.getDecoderTimingSnapshot().getSuccessfulDecoderCalls();
      return new RunRecord(mode, hash, evaluations, decoderCalls,
          canonical.getEvaluationObservation().getIllegalSolutions(),
          canonical.getEvaluationObservation().getDuplicateEvaluations(),
          new ArrayList<double[]>(), "FAILED", error.toString(), "",
          algorithm.getRuntimeSubSwarmSizes(),
          System.nanoTime() - algorithmStart, canonical.getDecoderTimingSnapshot(), cmaxAudit,
          algorithm.getV35DscrEventsCsv(), algorithm.getV35DscrTeacherUsesCsv(),
          String.join("\n", algorithm.getCaTaEvents()),
          algorithm.getV35PressureDiagnosisEventsCsv(), algorithm.getV35ShadowDiagnosisCsv(),
          algorithm.getV35ShadowDiagnosisSamples(), algorithm.getV35ShadowDiagnosisEvaluations(),
          passiveArchive == null ? "" : passiveArchive.toCsv(),
          passiveArchive == null ? 0L : passiveArchive.getObservedCount(),
          passiveArchive == null ? 0 : passiveArchive.size(),
          instanceSha, extensionSha, fatigueSha, table9Sha, table9Text);
    }
  }

  public static void writeRecord(RunRecord record, Path directory, String configurationText)
      throws java.io.IOException {
    if (record == null || directory == null || configurationText == null) {
      throw new IllegalArgumentException("record/directory/configurationText");
    }
    Files.createDirectories(directory);
    Files.write(directory.resolve("configuration.txt"),
        (configurationText + provenanceText(record)).getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("status.properties"), (
        "status=" + record.getStatus() + "\nmode=" + record.getMode()
        + "\nfullEvaluations=" + record.getFullEvaluations()
        + "\ndecoderCalls=" + record.getDecoderCalls()
        + "\nillegalSolutions=" + record.getIllegalSolutions()
        + "\nduplicateEvaluations=" + record.getDuplicateEvaluations()
        + "\nruntimeSubSwarmSizes=" + record.getRuntimeSubSwarmSizes()
        + "\ninitialPopulationHash=" + record.getInitialPopulationHash()
        + "\nstopReason=" + record.getStopReason() + "\n"
        + "mechanismSummary=" + record.getMechanismSummary() + "\n"
        + "algorithmRunNanos=" + record.getAlgorithmRunNanos() + "\n"
        + "decoderTiming=" + timingText(record.getDecoderTiming()) + "\n").getBytes(StandardCharsets.UTF_8));
    StringBuilder front = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] values : record.getFront()) front.append(values[0]).append(',').append(values[1]).append(',').append(values[2]).append('\n');
    Files.write(directory.resolve("front.csv"), front.toString().getBytes(StandardCharsets.UTF_8));
    if (record.getCmaxAudit() != null) {
      Files.write(directory.resolve("cmax-audit-curves.csv"),
          record.getCmaxAudit().curvesCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("cmax-audit-records.csv"),
          record.getCmaxAudit().recordsCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("cmax-audit-summary.txt"),
          record.getCmaxAudit().summaryText().getBytes(StandardCharsets.UTF_8));
    }
    if (!record.getPassiveArchiveCsv().isEmpty()) {
      Files.write(directory.resolve("passive-archive.csv"),
          record.getPassiveArchiveCsv().getBytes(StandardCharsets.UTF_8));
      double retention = record.getPassiveObservedCount() == 0L ? Double.NaN
          : (double) record.getPassiveArchiveSize() / record.getPassiveObservedCount();
      Files.write(directory.resolve("passive-summary.properties"), (
          "observedCount=" + record.getPassiveObservedCount()
          + "\narchiveSize=" + record.getPassiveArchiveSize()
          + "\nretentionRate=" + retention + "\n").getBytes(StandardCharsets.UTF_8));
    }
    if (record.getMode() == Mode.V35_FULL || record.getMode() == Mode.V35_FULL_POOL_OFF
        || record.getMode() == Mode.V35_QG1 || record.getMode() == Mode.V35_A2
        || record.getMode() == Mode.V35_A3) {
      Files.write(directory.resolve("dscr-summary.properties"),
          dscrProperties(record.getMechanismSummary()).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("dscr-events.csv"),
          record.getDscrEvents().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("dscr-teacher-uses.csv"),
          record.getDscrTeacherUses().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("ca-ta-lite-events.log"),
          record.getCaTaEvents().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("bottleneck-pressure-events.csv"),
          record.getPressureEvents().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("shadow-probes.csv"),
          record.getShadowProbes().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("diagnosis-summary.properties"), (
          "diagnosisVersion=" + V35BottleneckDiagnosisConfiguration.VERSION
          + "\nshadowSamples=" + record.getShadowSamples()
          + "\nshadowEvaluations=" + record.getShadowEvaluations() + "\n")
          .getBytes(StandardCharsets.UTF_8));
    }
  }

  /** Rejects an accidental mismatch between a named V35 arm and its frozen runtime contract. */
  private static void requireExplicitConfiguration(Mode mode, V35ProductionConfiguration config,
      long seed, int populationSize, int maxEvaluations,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration) {
    if (config.getSeed() != seed || config.getPopulationSize() != populationSize
        || config.getMaxEvaluations() != maxEvaluations
        || config.getDecoderMode() != ProductionDecodeMode.FM3) {
      throw new IllegalArgumentException("V35 configuration does not match run seed/population/budget/FM3");
    }
    boolean full = mode == Mode.V35_FULL || mode == Mode.V35_FULL_POOL_OFF;
    boolean dscr = full || mode == Mode.V35_QG1 || mode == Mode.V35_A2 || mode == Mode.V35_A3;
    boolean cfvf = full || mode == Mode.V35_A2 || mode == Mode.V35_A3;
    boolean qp = full || mode == Mode.V35_A3;
    if (!config.isQgEnabled() || config.isDscrEnabled() != dscr
        || config.isCfvfEnabled() != cfvf || config.isQpEnabled() != qp
        || config.isCaTaLiteEnabled() != full) {
      throw new IllegalArgumentException("V35 configuration mechanisms do not match mode=" + mode);
    }
    boolean directionalPool = mode == Mode.V35_FULL;
    if (config.isDirectionalTeacherPoolEnabled() != directionalPool) {
      throw new IllegalArgumentException("directional teacher pool does not match mode=" + mode);
    }
    if (config.getBottleneckDiagnosis() == null || diagnosisConfiguration == null) {
      throw new IllegalArgumentException("V35 bottleneck diagnosis is required");
    }
  }

  /**
   * Provenance section appended to every configuration.txt so the evidence
   * independently proves which instance, SUT extension, fatigue parameters and
   * Table 9 formal baseline a run consumed (acceptance review P1-2).
   */
  private static String provenanceText(RunRecord record) {
    StringBuilder out = new StringBuilder();
    out.append("instanceSha256=").append(record.getInstanceSha256()).append('\n');
    out.append("instanceExtensionSha256=").append(record.getInstanceExtensionSha256()).append('\n');
    out.append("fatigueConfigurationSha256=").append(record.getFatigueConfigurationSha256()).append('\n');
    out.append("formalBaselineSha256=").append(record.getFormalBaselineSha256()).append('\n');
    out.append("formalBaselineCanonicalBegin\n");
    out.append(record.getFormalBaselineCanonicalText());
    out.append("formalBaselineCanonicalEnd\n");
    return out.toString();
  }

  private static String dscrProperties(String mechanismSummary) {
    String marker = "dscr=";
    int start = mechanismSummary == null ? -1 : mechanismSummary.indexOf(marker);
    if (start < 0) return "schema=NOT_APPLICABLE\n";
    start += marker.length();
    int end = mechanismSummary.indexOf(",algorithmRunNanos=", start);
    if (end < 0) end = mechanismSummary.length();
    String nested = mechanismSummary.substring(start, end);
    StringBuilder out = new StringBuilder();
    for (String field : nested.split("\\|")) {
      int separator = field.indexOf('=');
      if (separator > 0) {
        out.append(field, 0, separator).append('=').append(field.substring(separator + 1)).append('\n');
      }
    }
    return out.toString();
  }

  public static String initialHash(List<PermutationSolution<Integer>> population) {
    StringBuilder text = new StringBuilder();
    for (PermutationSolution<Integer> solution : population) text.append(ZhangBoQgController.fingerprint(solution)).append('\n');
    try {
      byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
          .digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder hash = new StringBuilder();
      for (byte value : bytes) hash.append(String.format("%02x", value & 0xff));
      return hash.toString();
    } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }

  private static List<PermutationSolution<Integer>> copy(List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) result.add((PermutationSolution<Integer>) value.copy());
    return result;
  }

  private static String timingText(ZhangBoDecoderTimingSnapshot timing) {
    return "calls=" + timing.getSuccessfulDecoderCalls()
        + ",baseDecodeNanos=" + timing.getBaseDecodeNanos()
        + ",leftShiftNanos=" + timing.getLeftShiftNanos()
        + ",rightShiftNanos=" + timing.getRightShiftNanos()
        + ",decoderTotalNanos=" + timing.getDecoderTotalNanos()
        + ",frameworkOverheadNanos=" + timing.getDecoderFrameworkOverheadNanos()
        + ",leftRecomputations=" + timing.getLeftFullRecomputations()
        + ",rightRecomputations=" + timing.getRightFullRecomputations()
        + ",leftAccepted=" + timing.getLeftAccepted()
        + ",rightAccepted=" + timing.getRightAccepted();
  }
}
