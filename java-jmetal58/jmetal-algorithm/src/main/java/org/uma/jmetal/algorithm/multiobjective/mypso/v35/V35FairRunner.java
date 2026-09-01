package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
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
  public enum Mode {
    V35_BASELINE, V35_FULL, V35_QG0, V35_QG1, V35_FULL_POOL_OFF, V35_A2, V35_A3,
    /** V35-A3-D only: lineage archive plus deterministic directional pbest. */
    V35_DIAG_PA_DIRECTIONAL,
    /** V35-A3-D only: four-action Qp with synchronous Qg/Qp learning. */
    V35_DIAG_QP_SYNCHRONOUS,
    /** V35-A3-D2 only: Qp action selection remains active but all TD settlement is observation-only. */
    V35_DIAG_QP_OBSERVE_ONLY,
    /** V35-A3-D3 only: Q0 plus a deterministic directional greedy-tie prior. */
    V35_DIAG_QP_DIRECTIONAL_TIE
  }

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
    private final String evaluationTraceHash;
    private final V35ArchiveExperimentArtifacts archiveExperimentArtifacts;
    private final String a2A3PersonalLeaderAuditCsv;
    private final long a2A3PersonalLeaderAuditEvents;
    private final String fc5TransferMergeRoundsCsv;
    private final String fc5TransferWindowedMergeCsv;
    private final String fc5TransferRepresentativesCsv;
    private final String fc5TransferArchiveWorkingGapCsv;
    private final String fc5TransferSummary;
    private final ObservationEvidence observationEvidence;
    private final V35MidHorizonTelemetry midHorizonTelemetry;
    private final String qgTableHash;
    private final String pddrEventStreamHash;
    private final long caTaTestCalls;
    private final long caTaEventCount;
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
        String formalBaselineCanonicalText, String evaluationTraceHash,
        V35ArchiveExperimentArtifacts archiveExperimentArtifacts,
        String a2A3PersonalLeaderAuditCsv, long a2A3PersonalLeaderAuditEvents,
        String fc5TransferMergeRoundsCsv, String fc5TransferWindowedMergeCsv,
        String fc5TransferRepresentativesCsv, String fc5TransferArchiveWorkingGapCsv,
        String fc5TransferSummary,
        ObservationEvidence observationEvidence,
        V35MidHorizonTelemetry midHorizonTelemetry,
        String qgTableHash, String pddrEventStreamHash,
        long caTaTestCalls, long caTaEventCount) {
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
      this.evaluationTraceHash = evaluationTraceHash;
      this.archiveExperimentArtifacts = archiveExperimentArtifacts;
      this.a2A3PersonalLeaderAuditCsv = a2A3PersonalLeaderAuditCsv;
      this.a2A3PersonalLeaderAuditEvents = a2A3PersonalLeaderAuditEvents;
      this.fc5TransferMergeRoundsCsv = fc5TransferMergeRoundsCsv;
      this.fc5TransferWindowedMergeCsv = fc5TransferWindowedMergeCsv;
      this.fc5TransferRepresentativesCsv = fc5TransferRepresentativesCsv;
      this.fc5TransferArchiveWorkingGapCsv = fc5TransferArchiveWorkingGapCsv;
      this.fc5TransferSummary = fc5TransferSummary;
      this.observationEvidence = observationEvidence;
      this.midHorizonTelemetry = midHorizonTelemetry;
      this.qgTableHash = qgTableHash;
      this.pddrEventStreamHash = pddrEventStreamHash;
      this.caTaTestCalls = caTaTestCalls;
      this.caTaEventCount = caTaEventCount;
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
    public String getEvaluationTraceHash() { return evaluationTraceHash; }
    public V35ArchiveExperimentArtifacts getArchiveExperimentArtifacts() {
      return archiveExperimentArtifacts;
    }
    public String getA2A3PersonalLeaderAuditCsv() { return a2A3PersonalLeaderAuditCsv; }
    public long getA2A3PersonalLeaderAuditEvents() { return a2A3PersonalLeaderAuditEvents; }
    public String getFc5TransferMergeRoundsCsv() { return fc5TransferMergeRoundsCsv; }
    public String getFc5TransferWindowedMergeCsv() { return fc5TransferWindowedMergeCsv; }
    public String getFc5TransferRepresentativesCsv() { return fc5TransferRepresentativesCsv; }
    public String getFc5TransferArchiveWorkingGapCsv() { return fc5TransferArchiveWorkingGapCsv; }
    public String getFc5TransferSummary() { return fc5TransferSummary; }
    public V35MidHorizonTelemetry getMidHorizonTelemetry() { return midHorizonTelemetry; }
    public String getQgTableHash() { return qgTableHash; }
    public String getPddrEventStreamHash() { return pddrEventStreamHash; }
    public long getCaTaTestCalls() { return caTaTestCalls; }
    public long getCaTaEventCount() { return caTaEventCount; }
    /** Run-end-only controller evidence; it is never consumed by the algorithm. */
    public ObservationEvidence getObservationEvidence() { return observationEvidence; }
  }

  /**
   * Immutable run-end snapshot of already-maintained controller observables.
   * The event lists are the controller's retained rolling windows; total
   * counts and rolling hashes therefore remain authoritative when a window is
   * smaller than the full stream.
   */
  public static final class ObservationEvidence implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> qpEvents;
    private final List<String> lineageEvents;
    private final List<String> dualQEvents;
    private final long qpEventCount;
    private final long lineageEventCount;
    private final long dualQEventCount;
    private final String qpEventStreamHash;
    private final String lineageEventStreamHash;
    private final String dualQEventStreamHash;
    private final String qpActionStatistics;
    private final long qpPbestSwitches;
    private final String qpTableHash;
    private final long qpTrainedTransitions;
    private final long qpFrozenObservations;
    private final String qpRewardSummary;
    private final String qpTableSummary;
    private final long lineageSplits;
    private final long lineageDeletions;
    private final long lineageMigrations;
    private final long lineageInsertions;
    private final long lineageDominatedRemovals;
    private final long lineageDuplicateRemovals;
    private final long lineageTruncations;
    private final long dualQWarmup;
    private final long dualQP;
    private final long dualQG;

    private ObservationEvidence(ZhangBoMOHPSOQ algorithm) {
      qpEvents = immutable(algorithm.getQpEvents());
      lineageEvents = immutable(algorithm.getZhangBoLineageEvents());
      dualQEvents = immutable(algorithm.getDualQCoordinationEvents());
      qpEventCount = algorithm.getQpEventCount();
      lineageEventCount = algorithm.getZhangBoLineageEventCount();
      dualQEventCount = algorithm.getDualQEventCount();
      qpEventStreamHash = algorithm.getQpEventStreamHash();
      lineageEventStreamHash = algorithm.getZhangBoLineageEventStreamHash();
      dualQEventStreamHash = algorithm.getDualQEventStreamHash();
      qpActionStatistics = qpActionStatistics(algorithm);
      qpPbestSwitches = algorithm.getQpPbestSwitches();
      qpTableHash = algorithm.getQpTableHash();
      qpTrainedTransitions = algorithm.getQpTrainedTransitionCount();
      qpFrozenObservations = algorithm.getQpFrozenObservationCount();
      qpRewardSummary = algorithm.getQpRewardSummary();
      qpTableSummary = algorithm.getQpTableSummary();
      lineageSplits = algorithm.getZhangBoLineageSplitCount();
      lineageDeletions = algorithm.getZhangBoLineageDeletionCount();
      lineageMigrations = algorithm.getZhangBoLineageMigrationCount();
      lineageInsertions = algorithm.getZhangBoArchiveInsertionCount();
      lineageDominatedRemovals = algorithm.getZhangBoArchiveDominatedRemovalCount();
      lineageDuplicateRemovals = algorithm.getZhangBoArchiveDuplicateRemovalCount();
      lineageTruncations = algorithm.getZhangBoArchiveTruncationCount();
      dualQWarmup = algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.WARMUP);
      dualQP = algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.P_BLOCK);
      dualQG = algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.G_BLOCK);
    }

    private static List<String> immutable(List<String> values) {
      return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String qpActionStatistics(ZhangBoMOHPSOQ algorithm) {
      StringBuilder out = new StringBuilder();
      for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
        out.append(action.name()).append(".count=")
            .append(algorithm.getQpActionCount(action)).append('\n');
        out.append(action.name()).append(".averageReward=")
            .append(algorithm.getQpAverageReward(action)).append('\n');
      }
      return out.toString();
    }

    public List<String> getQpEvents() { return qpEvents; }
    public List<String> getLineageEvents() { return lineageEvents; }
    public List<String> getDualQEvents() { return dualQEvents; }
    public long getQpEventCount() { return qpEventCount; }
    public long getLineageEventCount() { return lineageEventCount; }
    public long getDualQEventCount() { return dualQEventCount; }
    public String getQpEventStreamHash() { return qpEventStreamHash; }
    public String getLineageEventStreamHash() { return lineageEventStreamHash; }
    public String getDualQEventStreamHash() { return dualQEventStreamHash; }
    public String getQpActionStatistics() { return qpActionStatistics; }
    public long getQpPbestSwitches() { return qpPbestSwitches; }
    public String getQpTableHash() { return qpTableHash; }
    public long getQpTrainedTransitions() { return qpTrainedTransitions; }
    public long getQpFrozenObservations() { return qpFrozenObservations; }
    public String getQpRewardSummary() { return qpRewardSummary; }
    public String getQpTableSummary() { return qpTableSummary; }
    public long getLineageSplits() { return lineageSplits; }
    public long getLineageDeletions() { return lineageDeletions; }
    public long getLineageMigrations() { return lineageMigrations; }
    public long getLineageInsertions() { return lineageInsertions; }
    public long getLineageDominatedRemovals() { return lineageDominatedRemovals; }
    public long getLineageDuplicateRemovals() { return lineageDuplicateRemovals; }
    public long getLineageTruncations() { return lineageTruncations; }
    public long getDualQWarmup() { return dualQWarmup; }
    public long getDualQP() { return dualQP; }
    public long getDualQG() { return dualQG; }
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
    return runInternal(mode, problem, initialPopulation, maxEvaluations, seed,
        attachPassiveArchive, diagnosisConfiguration, allowTerminalPartialFormalQPhase,
        explicitConfiguration, null, false, false);
  }

  /**
   * Isolated A2-to-A3 decomposition execution with a pure-observation
   * personal-leader ledger.  Formal A0-A4 runners never call this overload.
   */
  public static RunRecord runA2A3Diagnostic(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      V35ProductionConfiguration explicitConfiguration) {
    return runInternal(mode, problem, initialPopulation, maxEvaluations, seed, true,
        diagnosisConfiguration, false, explicitConfiguration, null, true, false);
  }

  /**
   * FC5-T only: exposes a merge-pool and working-population ledger without
   * changing the explicit arm, random stream, evaluation budget or result.
   */
  public static RunRecord runFc5TransferDiagnostic(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      V35ProductionConfiguration explicitConfiguration) {
    return runInternal(mode, problem, initialPopulation, maxEvaluations, seed, false,
        diagnosisConfiguration, false, explicitConfiguration, null, false, true);
  }

  /**
   * V35-FC5-MIDHORIZON-V2: same frozen arm/search semantics as
   * {@link #runFc5TransferDiagnostic} plus the unified observation bundle.
   * {@code telemetryOn == false} is behaviour-identical to the OFF control
   * (no observer is constructed, no random/FE/selection change).  When ON, a
   * V35MidHorizonTelemetry is built and attached; its CSV bundle is exposed on
   * the returned RunRecord.  Note: the internal run already attaches FC5
   * transfer telemetry, so the resulting record carries both ledgers.
   */
  public static RunRecord runMidHorizonDiagnostic(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      V35ProductionConfiguration explicitConfiguration,
      long[] nominalCheckpoints, boolean telemetryOn,
      String runId, String sourceJarSha256) {
    V35MidHorizonTelemetry telemetry = null;
    if (telemetryOn) {
      String configHash = explicitConfiguration == null ? "UNKNOWN"
          : explicitConfiguration.configurationHash();
      String instanceHash = problem instanceof ZhangBoCanonicalProductionProblem
          ? ((ZhangBoCanonicalProductionProblem) problem).getInstance().getInstanceSha256()
          : "UNKNOWN";
      String armName = mode == Mode.V35_A2 ? "A2" : "A4";
      V35CheckpointFrontObserver checkpoint = new V35CheckpointFrontObserver(
          nominalCheckpoints, runId, sourceJarSha256, configHash, instanceHash,
          seed, armName, true);
      V35FullPddrLedgerObserver pddr = new V35FullPddrLedgerObserver(
          runId, sourceJarSha256, configHash, instanceHash, seed, armName, true);
      V35TeacherConcentrationObserver teacher = new V35TeacherConcentrationObserver(
          runId, sourceJarSha256, configHash, instanceHash, seed, armName, true);
      V35CaTaContributionObserver cata = new V35CaTaContributionObserver(
          runId, sourceJarSha256, configHash, instanceHash, seed, armName, true);
      telemetry = new V35MidHorizonTelemetry(checkpoint, pddr, teacher, cata,
          runId, sourceJarSha256, configHash, instanceHash, seed, armName, true);
    }
    RunRecord record = runInternal(mode, problem, initialPopulation, maxEvaluations, seed,
        true, diagnosisConfiguration, true, explicitConfiguration, null, false, true,
        telemetry);
    return record;
  }

  static RunRecord runArchiveExperiment(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      V35ProductionConfiguration explicitConfiguration,
      V35ArchiveExperimentProfile archiveProfile) {
    if (archiveProfile == null) throw new IllegalArgumentException("archiveProfile");
    return runInternal(mode, problem, initialPopulation, maxEvaluations, seed, true,
        diagnosisConfiguration, false, explicitConfiguration, archiveProfile, false, false);
  }

  private static RunRecord runInternal(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      boolean allowTerminalPartialFormalQPhase,
      V35ProductionConfiguration explicitConfiguration,
      V35ArchiveExperimentProfile archiveProfile,
      boolean a2A3DiagnosticTelemetry,
      boolean fc5TransferTelemetry) {
    return runInternal(mode, problem, initialPopulation, maxEvaluations, seed,
        attachPassiveArchive, diagnosisConfiguration, allowTerminalPartialFormalQPhase,
        explicitConfiguration, archiveProfile, a2A3DiagnosticTelemetry,
        fc5TransferTelemetry, null);
  }

  private static RunRecord runInternal(Mode mode,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      boolean attachPassiveArchive,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      boolean allowTerminalPartialFormalQPhase,
      V35ProductionConfiguration explicitConfiguration,
      V35ArchiveExperimentProfile archiveProfile,
      boolean a2A3DiagnosticTelemetry,
      boolean fc5TransferTelemetry,
      V35MidHorizonTelemetry midHorizonTelemetry) {
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
    if (explicitConfiguration == null && (mode == Mode.V35_DIAG_PA_DIRECTIONAL
        || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY
        || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE)) {
      throw new IllegalArgumentException("V35 A2-A3 diagnostic modes require an explicit configuration");
    }
    boolean full = mode == Mode.V35_FULL || mode == Mode.V35_FULL_POOL_OFF;
    // QG0/QG1 are the one-variable DSCR pairing: both retain original Qg;
    // only DSCR sanitation changes.
    // V35-P21 ablation ladder rungs: V35_A2 = QG1 + CFVF, V35_A3 = A2 + Q-pbest.
    // Both follow the partial order qp=>cfvf=>qg and keep caTaLite/pool off.
    boolean qg = true;
    boolean dscr = full || mode == Mode.V35_QG1 || mode == Mode.V35_A2 || mode == Mode.V35_A3
        || mode == Mode.V35_DIAG_PA_DIRECTIONAL || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
    boolean cfvf = full || mode == Mode.V35_A2 || mode == Mode.V35_A3
        || mode == Mode.V35_DIAG_PA_DIRECTIONAL || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
    boolean qp = full || mode == Mode.V35_A3 || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
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
    // The true audit must be installed before the algorithm constructor: the
    // constructor creates the P6/QP/CA-TA streams and those streams are part
    // of the actual request path.  Installing it after build misses them.
    if (midHorizonTelemetry != null) midHorizonTelemetry.startTrueAudit();
    ZhangBoMOHPSOQ algorithm;
    try {
      algorithm = new ZhangBoMOHPSOQBuilder(problem, initialPopulation.size(),
          problem instanceof org.uma.jmetal.problem.PermutationProblem
              ? ((org.uma.jmetal.problem.PermutationProblem<?>) problem).getNumberOfFactories() : 2,
          0.6, 0.5, 0.5, 50).setV35Configuration(config)
          .setMaxIterations(maxEvaluations).setInitialSwarmOverride(copy(initialPopulation)).build();
    } catch (RuntimeException error) {
      if (midHorizonTelemetry != null) midHorizonTelemetry.stopTrueAudit();
      throw error;
    }
    ZhangBoCmaxAudit cmaxAudit = new ZhangBoCmaxAudit(1000L);
    algorithm.setAllowTerminalPartialFormalQPhase(allowTerminalPartialFormalQPhase);
    algorithm.setCmaxAudit(cmaxAudit);
    algorithm.setV35A2A3PersonalLeaderAuditEnabled(a2A3DiagnosticTelemetry);
    algorithm.setV35Fc5TransferAuditEnabled(fc5TransferTelemetry);
    algorithm.setV35Fc5TransferAuditSeed(seed);
    if (midHorizonTelemetry != null) {
      algorithm.setMidHorizonTelemetry(midHorizonTelemetry);
    }
    V35PassiveEvaluationArchive passiveArchive =
        attachPassiveArchive ? new V35PassiveEvaluationArchive() : null;
    if (passiveArchive != null) algorithm.setPassiveEvaluationArchive(passiveArchive);
    V35ArchiveExperimentRuntime archiveRuntime = archiveProfile == null ? null
        : new V35ArchiveExperimentRuntime(archiveProfile, passiveArchive);
    if (archiveRuntime != null) algorithm.setArchiveExperimentRuntime(archiveRuntime);
    long algorithmStart = System.nanoTime();
    try {
      algorithm.run();
      long algorithmNanos = System.nanoTime() - algorithmStart;
      if (midHorizonTelemetry != null) midHorizonTelemetry.stopTrueAudit();
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
      V35ArchiveExperimentArtifacts archiveArtifacts = archiveRuntime == null ? null
          : archiveRuntime.finish(result,
              passiveArchive == null ? result : passiveArchive.snapshot());
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
          instanceSha, extensionSha, fatigueSha, table9Sha, table9Text,
          canonical.getEvaluationObservation().getEvaluationTraceHash(), archiveArtifacts,
          a2A3DiagnosticTelemetry ? algorithm.getV35A2A3PersonalLeaderAuditCsv() : "",
          a2A3DiagnosticTelemetry ? algorithm.getV35A2A3PersonalLeaderAuditEventCount() : 0L,
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferMergeRoundsCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferWindowedMergeCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferRepresentativesCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferArchiveWorkingGapCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferSummary() : "",
          new ObservationEvidence(algorithm),
          algorithm.getMidHorizonTelemetry(),
          algorithm.getQgTableHash(), algorithm.getZhangBoPddrEventStreamHash(),
          algorithm.getCaTaTestCalls(), algorithm.getCaTaEventCount());
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
          instanceSha, extensionSha, fatigueSha, table9Sha, table9Text,
          canonical.getEvaluationObservation().getEvaluationTraceHash(), null,
          a2A3DiagnosticTelemetry ? algorithm.getV35A2A3PersonalLeaderAuditCsv() : "",
          a2A3DiagnosticTelemetry ? algorithm.getV35A2A3PersonalLeaderAuditEventCount() : 0L,
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferMergeRoundsCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferWindowedMergeCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferRepresentativesCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferArchiveWorkingGapCsv() : "",
          fc5TransferTelemetry ? algorithm.getV35Fc5TransferSummary() : "",
          new ObservationEvidence(algorithm),
          algorithm.getMidHorizonTelemetry(),
          algorithm.getQgTableHash(), algorithm.getZhangBoPddrEventStreamHash(),
          algorithm.getCaTaTestCalls(), algorithm.getCaTaEventCount());
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
        + "\nevaluationTraceHash=" + record.getEvaluationTraceHash()
        + "\nstopReason=" + record.getStopReason() + "\n"
        + "mechanismSummary=" + record.getMechanismSummary() + "\n"
        + "algorithmRunNanos=" + record.getAlgorithmRunNanos() + "\n"
        + "decoderTiming=" + timingText(record.getDecoderTiming()) + "\n").getBytes(StandardCharsets.UTF_8));
    ObservationEvidence observation = record.getObservationEvidence();
    if (observation != null) {
      Files.write(directory.resolve("qp-events.log"),
          eventText(observation.getQpEvents()).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("lineage-events.log"),
          eventText(observation.getLineageEvents()).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("dual-q-events.log"),
          eventText(observation.getDualQEvents()).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("qp-summary.properties"), (
          "eventCountTotal=" + observation.getQpEventCount() + "\n"
          + "eventsRetained=" + observation.getQpEvents().size() + "\n"
          + "eventStreamHash=" + observation.getQpEventStreamHash() + "\n"
          + "eventCapture=" + eventCaptureMode(observation.getQpEvents(),
              observation.getQpEventCount()) + "\n"
          + "pbestSwitches=" + observation.getQpPbestSwitches() + "\n"
          + "trainedTransitions=" + observation.getQpTrainedTransitions() + "\n"
          + "frozenObservations=" + observation.getQpFrozenObservations() + "\n"
          + "tableHash=" + observation.getQpTableHash() + "\n"
          + observation.getQpRewardSummary()
          + observation.getQpTableSummary()
          + observation.getQpActionStatistics()).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("lineage-summary.properties"), (
          "eventCountTotal=" + observation.getLineageEventCount() + "\n"
          + "eventsRetained=" + observation.getLineageEvents().size() + "\n"
          + "eventStreamHash=" + observation.getLineageEventStreamHash() + "\n"
          + "eventCapture=" + eventCaptureMode(observation.getLineageEvents(),
              observation.getLineageEventCount()) + "\n"
          + "splits=" + observation.getLineageSplits() + "\n"
          + "deletions=" + observation.getLineageDeletions() + "\n"
          + "migrations=" + observation.getLineageMigrations() + "\n"
          + "insertions=" + observation.getLineageInsertions() + "\n"
          + "dominatedRemovals=" + observation.getLineageDominatedRemovals() + "\n"
          + "duplicateRemovals=" + observation.getLineageDuplicateRemovals() + "\n"
          + "truncations=" + observation.getLineageTruncations() + "\n"
          ).getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("dual-q-summary.properties"), (
          "eventCountTotal=" + observation.getDualQEventCount() + "\n"
          + "eventsRetained=" + observation.getDualQEvents().size() + "\n"
          + "eventStreamHash=" + observation.getDualQEventStreamHash() + "\n"
          + "eventCapture=" + eventCaptureMode(observation.getDualQEvents(),
              observation.getDualQEventCount()) + "\n"
          + "warmup=" + observation.getDualQWarmup() + "\n"
          + "pBlock=" + observation.getDualQP() + "\n"
          + "gBlock=" + observation.getDualQG() + "\n"
          ).getBytes(StandardCharsets.UTF_8));
    }
    if (!record.getA2A3PersonalLeaderAuditCsv().isEmpty()) {
      Files.write(directory.resolve("a2a3-personal-leader-events.csv"),
          record.getA2A3PersonalLeaderAuditCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("a2a3-personal-leader-summary.properties"),
          ("eventCount=" + record.getA2A3PersonalLeaderAuditEvents() + "\n"
              + "observationOnly=true\n").getBytes(StandardCharsets.UTF_8));
    }
    if (!record.getFc5TransferSummary().isEmpty()) {
      Files.write(directory.resolve("fc5-transfer-merge-rounds.csv"),
          record.getFc5TransferMergeRoundsCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("fc5-transfer-windowed-merge-overflow.csv"),
          record.getFc5TransferWindowedMergeCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("fc5-transfer-directional-representative-lifecycle.csv"),
          record.getFc5TransferRepresentativesCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("fc5-transfer-archive-working-gap.csv"),
          record.getFc5TransferArchiveWorkingGapCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("fc5-transfer-summary.properties"),
          record.getFc5TransferSummary().getBytes(StandardCharsets.UTF_8));
    }
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
        || record.getMode() == Mode.V35_A3
        || record.getMode() == Mode.V35_DIAG_PA_DIRECTIONAL
        || record.getMode() == Mode.V35_DIAG_QP_SYNCHRONOUS
        || record.getMode() == Mode.V35_DIAG_QP_OBSERVE_ONLY
        || record.getMode() == Mode.V35_DIAG_QP_DIRECTIONAL_TIE) {
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
    V35ArchiveExperimentArtifacts archiveArtifacts = record.getArchiveExperimentArtifacts();
    if (archiveArtifacts != null) {
      Files.write(directory.resolve("decision-front.csv"),
          front.toString().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("observed-full-front.csv"),
          record.getPassiveArchiveCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("archive-experiment-configuration.txt"),
          (archiveArtifacts.getProfileCanonicalText()
              + "archiveExperimentHash=" + archiveArtifacts.getProfileHash() + '\n')
              .getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("archive-audit-summary.properties"),
          archiveArtifacts.getAuditSummary().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("archive-audit-events.csv"),
          archiveArtifacts.getAuditEventsCsv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("representative-front-k30.csv"),
          archiveArtifacts.getRepresentativeK30Csv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("sensitivity-front-k25.csv"),
          archiveArtifacts.getSensitivityK25Csv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("sensitivity-front-k50.csv"),
          archiveArtifacts.getSensitivityK50Csv().getBytes(StandardCharsets.UTF_8));
      Files.write(directory.resolve("front-kind-registry.csv"),
          V35FrontKind.registryCsv().getBytes(StandardCharsets.UTF_8));
    }
  }

  private static String eventCaptureMode(List<String> events, long total) {
    if (total == 0L) {
      return "EMPTY";
    }
    return total == events.size() ? "FULL" : "ROLLING";
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
    boolean dscr = full || mode == Mode.V35_QG1 || mode == Mode.V35_A2 || mode == Mode.V35_A3
        || mode == Mode.V35_DIAG_PA_DIRECTIONAL || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
    boolean cfvf = full || mode == Mode.V35_A2 || mode == Mode.V35_A3
        || mode == Mode.V35_DIAG_PA_DIRECTIONAL || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
    boolean qp = full || mode == Mode.V35_A3 || mode == Mode.V35_DIAG_QP_SYNCHRONOUS
        || mode == Mode.V35_DIAG_QP_OBSERVE_ONLY || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE;
    if (!config.isQgEnabled() || config.isDscrEnabled() != dscr
        || config.isCfvfEnabled() != cfvf || config.isQpEnabled() != qp
        || config.isCaTaLiteEnabled() != full) {
      throw new IllegalArgumentException("V35 configuration mechanisms do not match mode=" + mode);
    }
    boolean directionalPool = mode == Mode.V35_FULL;
    if (config.isDirectionalTeacherPoolEnabled() != directionalPool) {
      throw new IllegalArgumentException("directional teacher pool does not match mode=" + mode);
    }
    if (mode == Mode.V35_DIAG_PA_DIRECTIONAL
        && config.getPersonalLeaderMode() != V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL) {
      throw new IllegalArgumentException("D1 requires deterministic archive-directional pbest");
    }
    if (mode == Mode.V35_DIAG_QP_SYNCHRONOUS) {
      if (config.getPersonalLeaderMode() != V35PersonalLeaderMode.QP_FOUR_ACTIONS
          || config.getDualQCoordination() == null
          || config.getDualQCoordination().isBlockFrozen()) {
        throw new IllegalArgumentException("D2 requires four-action Qp with explicit synchronous dual-Q");
      }
    }
    if (mode == Mode.V35_DIAG_QP_OBSERVE_ONLY
        || mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE) {
      if (config.getPersonalLeaderMode() != V35PersonalLeaderMode.QP_FOUR_ACTIONS
          || config.getDualQCoordination() == null
          || config.getDualQCoordination().isBlockFrozen()
          || config.getQpSettlementPolicy() != V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES) {
        throw new IllegalArgumentException(
            "Qp observe diagnostics require four-action Qp, synchronous dual-Q and observe-only settlement");
      }
      boolean directionalTie = config.getQpConfiguration() != null
          && config.getQpConfiguration().getGreedyTiePolicy()
              == org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration.GreedyTiePolicy.DIRECTIONAL_IF_TIED;
      if ((mode == Mode.V35_DIAG_QP_DIRECTIONAL_TIE) != directionalTie) {
        throw new IllegalArgumentException("Q0/Q1 greedy-tie configuration does not match the named mode");
      }
    } else if (config.getQpSettlementPolicy()
        != V35QpSettlementPolicy.STANDARD_BY_DUAL_Q) {
      throw new IllegalArgumentException("observe-only Qp settlement is diagnostic-only");
    } else if (config.getQpConfiguration() != null
        && config.getQpConfiguration().getGreedyTiePolicy()
            != org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration.GreedyTiePolicy.FIRST_VALID) {
      throw new IllegalArgumentException("nonstandard Qp greedy-tie policy is diagnostic-only");
    }
    if (mode == Mode.V35_A3 && (config.getDualQCoordination() == null
        || !config.getDualQCoordination().isBlockFrozen())) {
      throw new IllegalArgumentException("A3 requires the frozen P5/G5 dual-Q schedule");
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

  private static String eventText(List<String> events) {
    if (events == null || events.isEmpty()) return "";
    return String.join("\n", events) + "\n";
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
