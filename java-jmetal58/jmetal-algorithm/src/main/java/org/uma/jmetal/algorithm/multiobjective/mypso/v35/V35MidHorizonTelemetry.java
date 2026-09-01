package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1 unified observation coordinator.
 *
 * <p>Pure observation only: when {@code enabled == false} no log object is
 * built, no random number is consumed, no FE is changed and no selection,
 * reward, PDDR, archive or stop rule is touched.  When enabled it forwards
 * the four real event families to the four observers and accumulates
 * {@code observerErrors} on any observer exception without propagating.
 */
public final class V35MidHorizonTelemetry {
  public static final String VERSION = "V35_MIDHORIZON_V3_1";
  public static final String FORMAL_BUDGET_SEMANTICS =
      "PHASE_CONSISTENT_BUDGET_TERMINATION";
  public static final boolean ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE = false;
  public static final String GENERATED_CANDIDATE_SOURCE_UNAVAILABLE =
      "UNAVAILABLE_GENERATED_CANDIDATE_SEQUENCE_NOT_EXPOSED";

  private final V35CheckpointFrontObserver checkpointFront;
  private final V35FullPddrLedgerObserver pddrLedger;
  private final V35TeacherConcentrationObserver teacherConcentration;
  private final V35CaTaContributionObserver caTaContribution;
  private final String runId;
  private final String sourceJarSha256;
  private final String configurationHash;
  private final String instanceHash;
  private final long seed;
  private final String arm;
  private final String telemetryMode;
  private boolean enabled;
  private long observerErrors;
  private boolean runFinalized;
  private final V35TrueSequenceAudit trueAudit = new V35TrueSequenceAudit();

  public V35MidHorizonTelemetry(
      V35CheckpointFrontObserver checkpointFront,
      V35FullPddrLedgerObserver pddrLedger,
      V35TeacherConcentrationObserver teacherConcentration,
      V35CaTaContributionObserver caTaContribution,
      String runId, String sourceJarSha256, String configurationHash,
      String instanceHash, long seed, String arm, boolean enabled) {
    this.checkpointFront = checkpointFront;
    this.pddrLedger = pddrLedger;
    this.teacherConcentration = teacherConcentration;
    this.caTaContribution = caTaContribution;
    this.runId = runId;
    this.sourceJarSha256 = sourceJarSha256;
    this.configurationHash = configurationHash;
    this.instanceHash = instanceHash;
    this.seed = seed;
    this.arm = arm;
    this.enabled = enabled;
    this.telemetryMode = enabled ? "ON" : "OFF";
    this.observerErrors = 0L;
    this.runFinalized = false;
  }

  public void setEnabled(boolean value) {
    this.enabled = value;
    if (checkpointFront != null) checkpointFront.setEnabled(value);
    if (pddrLedger != null) pddrLedger.setEnabled(value);
    if (teacherConcentration != null) teacherConcentration.setEnabled(value);
    if (caTaContribution != null) caTaContribution.setEnabled(value);
    if (!value) this.observerErrors = 0L;
    if (!value) this.runFinalized = false;
  }

  public boolean isEnabled() { return enabled; }
  public long getObserverErrors() {
    long sum = observerErrors;
    if (checkpointFront != null) sum += checkpointFront.getObserverErrors();
    if (pddrLedger != null) sum += pddrLedger.getObserverErrors();
    if (teacherConcentration != null) sum += teacherConcentration.getObserverErrors();
    if (caTaContribution != null) sum += caTaContribution.getObserverErrors();
    return sum;
  }

  public long getObserverExecutionErrors() {
    long sum = observerErrors;
    if (checkpointFront != null) sum += checkpointFront.getObserverExecutionErrors();
    // The other observers expose only an aggregate observerErrors counter;
    // their failures cannot honestly be reclassified as execution errors.
    return sum;
  }

  public long getUnobservableCheckpointCount() {
    if (checkpointFront != null) return checkpointFront.getUnobservableCheckpointCount();
    return 0L;
  }

  public String getUnobservableReasonSummary() {
    return checkpointFront == null ? "" : checkpointFront.getUnobservableReasonSummary();
  }

  public long getNominalCheckpointNotExactlyReachedCount() {
    return checkpointFront == null ? 0L
        : checkpointFront.getNominalCheckpointNotExactlyReachedCount();
  }

  public long getTerminalSnapshotCount() {
    return checkpointFront == null ? 0L : checkpointFront.getTerminalSnapshotCount();
  }

  public long getLastCompletedAtomicBoundaryFE() {
    return checkpointFront == null ? -1L : checkpointFront.getLastCompletedAtomicBoundaryFE();
  }

  public String getLastCompletedAtomicBoundary() {
    return checkpointFront == null ? "NOT_APPLICABLE"
        : checkpointFront.getLastCompletedAtomicBoundary();
  }

  public String getLastCheckpointKind() {
    return checkpointFront == null ? "NOT_APPLICABLE" : checkpointFront.getLastCheckpointKind();
  }

  public String getLastCheckpointAtomicBoundary() {
    return checkpointFront == null ? "NOT_APPLICABLE"
        : checkpointFront.getLastCheckpointAtomicBoundary();
  }

  public String getLastTerminationKind() {
    return checkpointFront == null ? "NOT_APPLICABLE"
        : checkpointFront.getLastTerminationKind();
  }

  public long getLastNominalCheckpointFE() {
    return checkpointFront == null ? -1L : checkpointFront.getLastNominalCheckpointFE();
  }

  public long getLastActualCheckpointFE() {
    return checkpointFront == null ? -1L : checkpointFront.getLastActualCheckpointFE();
  }

  public long getLastActualSnapshotFE() {
    return checkpointFront == null ? -1L : checkpointFront.getLastActualSnapshotFE();
  }

  public long getLastCheckpointDeltaFE() {
    return checkpointFront == null ? Long.MIN_VALUE : checkpointFront.getLastCheckpointDeltaFE();
  }

  public String getTerminalClassification() {
    return checkpointFront == null ? "NOT_APPLICABLE"
        : checkpointFront.getTerminalClassification();
  }

  public boolean isTerminalCheckpointAccepted() {
    return checkpointFront != null && checkpointFront.isTerminalCheckpointAccepted();
  }

  public void startTrueAudit() {
    if (!enabled) return;
    trueAudit.clear();
    runFinalized = false;
    trueAudit.attachToJMetalRandom();
  }

  public void stopTrueAudit() {
    if (!enabled) return;
    trueAudit.detachFromJMetalRandom();
  }

  public String getTrueRngHash() { return trueAudit.rngSequenceHash(); }
  public String getTrueCandidateHash() { return trueAudit.candidateSequenceHash(); }
  public int getTrueRngCount() { return trueAudit.rngCount(); }
  public int getTrueCandidateCount() { return trueAudit.candidateCount(); }
  public String getTrueRngHashSource() { return trueAudit.rngHashSource(); }
  public String getTrueCandidateHashSource() { return trueAudit.candidateHashSource(); }

  public long getTrueCandidateSourceCount(String source) {
    return trueAudit.candidateSourceCount(source);
  }

  public String getTrueCandidateSourceCounts() {
    return trueAudit.candidateSourceCountsText();
  }

  /**
   * Returns an ACTUAL_* source only when the audit recorded actual RNG calls.
   * The audit's candidate-derived fallback is deliberately rejected here.
   */
  public String getTrueRngEvidenceSource() {
    if (!enabled) return "NOT_APPLICABLE";
    return trueAudit.rngCount() > 0 ? "ACTUAL_JMETAL_RANDOM"
        : "UNAVAILABLE_FORMAL_RNG_STREAM_NOT_EXPOSED";
  }

  /**
   * The current architecture records PDDR pools, not every generated
   * candidate.  Therefore no PDDR hash may be advertised as a generated
   * candidate sequence.
   */
  public String getGeneratedCandidateEvidenceSource() {
    if (!enabled) return "NOT_APPLICABLE";
    return trueAudit.candidateCount() > 0 ? "ACTUAL_GENERATED_CANDIDATES"
        : GENERATED_CANDIDATE_SOURCE_UNAVAILABLE;
  }

  public String getTrueRngHashOrUnavailable() {
    return getTrueRngEvidenceSource().startsWith("ACTUAL_")
        ? trueAudit.rngSequenceHash() : "UNAVAILABLE";
  }

  public String getGeneratedCandidateHashOrUnavailable() {
    return getGeneratedCandidateEvidenceSource().startsWith("ACTUAL_")
        ? trueAudit.candidateSequenceHash() : "UNAVAILABLE";
  }

  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int cycle) {
    onPddrRound(pool, sources, selected, fe, cycle, -1);
  }

  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int cycle, int generation) {
    if (!enabled) return;
    if (pddrLedger != null) {
      try {
        pddrLedger.onPddrRound(pool, sources, selected, fe, cycle, generation);
      } catch (RuntimeException error) {
        observerErrors++;
      }
    }
    if (caTaContribution != null && caTaContribution.isEnabled()) {
      try {
        caTaContribution.onPddrRound(pool, sources, selected, fe, cycle, generation);
      } catch (RuntimeException error) {
        observerErrors++;
      }
    }
    try {
      trueAudit.recordPddrPool(pool, sources, fe);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  private static String stableId(String raw) {
    try {
      byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
          .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte b : digest) result.append(String.format("%02x", b & 0xff));
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront) {
    onAtomicPhaseEnd(actualFE, generation, outerCycle, qRound, workingPopulation,
        decisionArchiveFront, observedFullFront, V35CheckpointFrontObserver.ATOMIC_BOUNDARY);
  }

  public void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront, String atomicBoundary) {
    if (!enabled || checkpointFront == null) return;
    try {
      checkpointFront.onAtomicPhaseEnd(actualFE, generation, outerCycle, qRound,
          workingPopulation, decisionArchiveFront, observedFullFront, atomicBoundary);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  /** Completes checkpoint accounting without inventing a terminal snapshot. */
  public void onRunEnd(long actualFE, int generation, int outerCycle, int qRound) {
    onRunEnd(actualFE, generation, outerCycle, qRound, true);
  }

  public void onRunEnd(long actualFE, int generation, int outerCycle, int qRound,
      boolean cataApplicable) {
    if (!enabled || runFinalized) return;
    try {
      if (checkpointFront != null) {
        checkpointFront.onRunEnd(actualFE, generation, outerCycle, qRound);
      }
      if (caTaContribution != null) {
        caTaContribution.onRunEnd(cataApplicable);
      }
      runFinalized = true;
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  /**
   * Completes the run with the state observed at the real terminal phase
   * boundary.  The three fronts are passed together so the checkpoint
   * observer can accept or reject them transactionally.
   */
  public void onTerminalRunEnd(long actualFE, int generation, int outerCycle, int qRound,
      List<PermutationSolution<Integer>> workingPopulation,
      List<PermutationSolution<Integer>> decisionArchiveFront,
      List<PermutationSolution<Integer>> observedFullFront,
      String terminationKind, boolean cataApplicable) {
    if (!enabled || runFinalized) return;
    try {
      if (checkpointFront != null) {
        checkpointFront.onRunEnd(actualFE, generation, outerCycle, qRound,
            workingPopulation, decisionArchiveFront, observedFullFront,
            terminationKind, ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE);
      }
      if (caTaContribution != null) {
        caTaContribution.onRunEnd(cataApplicable);
      }
      runFinalized = true;
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public boolean isRunFinalized() { return runFinalized; }

  /** Small, source-labelled contract block for independent acceptance. */
  public String contractProperties() {
    StringBuilder result = new StringBuilder();
    result.append("telemetryContractVersion=").append(VERSION).append('\n');
    result.append("formalBudgetSemantics=").append(FORMAL_BUDGET_SEMANTICS).append('\n');
    result.append("allowTerminalPartialFormalQPhase=")
        .append(ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE).append('\n');
    result.append("checkpointAtomicBoundary=")
        .append(V35CheckpointFrontObserver.ATOMIC_BOUNDARY).append('\n');
    result.append("trueRngEvidenceSource=").append(getTrueRngEvidenceSource()).append('\n');
    result.append("generatedCandidateEvidenceSource=")
        .append(getGeneratedCandidateEvidenceSource()).append('\n');
    result.append("observerErrors=").append(getObserverErrors()).append('\n');
    result.append("observerExecutionErrors=").append(getObserverExecutionErrors()).append('\n');
    result.append("unobservableCheckpointCount=")
        .append(getUnobservableCheckpointCount()).append('\n');
    result.append("unobservableCheckpointReasons=")
        .append(checkpointFront == null ? "" : checkpointFront.getUnobservableReasonSummary())
        .append('\n');
    result.append("nominalCheckpointNotExactlyReachedCount=")
        .append(getNominalCheckpointNotExactlyReachedCount()).append('\n');
    result.append("terminalSnapshotCount=").append(getTerminalSnapshotCount()).append('\n');
    result.append("lastCompletedAtomicBoundaryFE=")
        .append(getLastCompletedAtomicBoundaryFE()).append('\n');
    result.append("lastCompletedAtomicBoundary=")
        .append(getLastCompletedAtomicBoundary()).append('\n');
    result.append("checkpointKind=").append(getLastCheckpointKind()).append('\n');
    result.append("actualCheckpointFE=").append(getLastActualCheckpointFE()).append('\n');
    result.append("checkpointDeltaFE=").append(getLastCheckpointDeltaFE()).append('\n');
    result.append("terminalCheckpointClassification=")
        .append(getTerminalClassification()).append('\n');
    result.append("lastCheckpointAtomicBoundary=")
        .append(getLastCheckpointAtomicBoundary()).append('\n');
    result.append("lastActualSnapshotFE=").append(getLastActualSnapshotFE()).append('\n');
    return result.toString();
  }

  public void onTeacherUse(String qSystem, ZhangBoSubSwarm requesterRole,
      PermutationSolution<Integer> teacher, long fe, int cycle) {
    if (!enabled || teacherConcentration == null) return;
    try {
      teacherConcentration.onTeacherUse(qSystem, requesterRole, teacher, fe, cycle);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public String onTeacherSelection(V35TeacherConcentrationObserver.SelectionContext context,
      PermutationSolution<Integer> teacher, long fe, int generation, int cycle) {
    if (!enabled || teacherConcentration == null) return "";
    try {
      return teacherConcentration.onTeacherSelection(context, teacher, fe, generation, cycle);
    } catch (RuntimeException error) {
      observerErrors++;
      return "";
    }
  }

  public void onTeacherOffspringEvaluated(String eventId,
      PermutationSolution<Integer> offspring, boolean improved) {
    if (!enabled || teacherConcentration == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      teacherConcentration.onOffspringEvaluated(eventId, offspring, improved);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaCandidate(String testApply, V35MacroNeighborhood macro,
      ZhangBoSubSwarm group, String bottleneck,
      PermutationSolution<Integer> parent, PermutationSolution<Integer> candidate,
      boolean accepted, long fe, int cycle) {
    if (!enabled || caTaContribution == null) return;
    try {
      caTaContribution.onCaTaCandidate(testApply, macro, group, bottleneck,
          parent, candidate, accepted, fe, cycle);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public String onCaTaCandidateGenerated(String testApply, V35MacroNeighborhood macro,
      ZhangBoSubSwarm group, String bottleneck,
      PermutationSolution<Integer> parent, PermutationSolution<Integer> candidate,
      long fe, int generation, int cycle) {
    if (!enabled || caTaContribution == null) return "";
    try {
      return caTaContribution.onCandidateGenerated(testApply, macro, group, bottleneck,
          parent, candidate, fe, generation, cycle);
    } catch (RuntimeException error) {
      observerErrors++;
      return "";
    }
  }

  public void onCaTaCandidateEvaluated(String eventId,
      PermutationSolution<Integer> candidate, long fe) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onCandidateEvaluated(eventId, candidate, fe);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaAcceptedLocally(String eventId, boolean accepted, String result) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onAcceptedLocally(eventId, accepted, result);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaEnteredMergePool(String eventId, boolean entered) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onEnteredMergePool(eventId, entered);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaPddrDecision(String eventId, boolean selected) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onPddrDecision(eventId, selected);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaArchiveDecision(String eventId,
      V35CaTaContributionObserver.ArchiveKind kind, boolean entered) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onArchiveDecision(eventId, kind, entered);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public void onCaTaSurvived(String eventId, boolean survived) {
    if (!enabled || caTaContribution == null || eventId == null || eventId.length() == 0) {
      return;
    }
    try {
      caTaContribution.onSurvivedNextGeneration(eventId, survived);
    } catch (RuntimeException error) {
      observerErrors++;
    }
  }

  public int checkpointRows() {
    return checkpointFront == null ? 0 : checkpointFront.getRowCount();
  }
  public int pddrLedgerRows() {
    return pddrLedger == null ? 0 : pddrLedger.getRowCount();
  }
  public int teacherRows() {
    return teacherConcentration == null ? 0 : teacherConcentration.getRowCount();
  }
  public int cataRows() {
    return caTaContribution == null ? 0 : caTaContribution.getRowCount();
  }

  public V35PddrCandidateMetadataAdapter.ContractReport getPddrContractReport() {
    return pddrLedger == null ? null : pddrLedger.getMetadataContractReport();
  }

  public V35TeacherConcentrationObserver.ContractReport getTeacherContractReport(
      boolean qgRequired, boolean qpRequired) {
    return teacherConcentration == null ? null
        : teacherConcentration.getContractReport(qgRequired, qpRequired);
  }

  public boolean cataLifecycleHasUnobservableFields() {
    return caTaContribution != null && caTaContribution.hasUnobservableFields();
  }

  public String getCheckpointFrontCsv() {
    return checkpointFront == null ? "" : checkpointFront.toCsv();
  }
  public String getPddrLedgerCsv() {
    return pddrLedger == null ? "" : pddrLedger.ledgerCsv();
  }
  public String getPddrCycleSummaryCsv() {
    return pddrLedger == null ? "" : pddrLedger.cycleSummaryCsv();
  }
  public String getTeacherEventsCsv() {
    return teacherConcentration == null ? "" : teacherConcentration.eventsCsv();
  }
  public String getTeacherConcentrationCsv() {
    return teacherConcentration == null ? "" : teacherConcentration.concentrationCsv();
  }
  public String getCataEventsCsv() {
    return caTaContribution == null ? "" : caTaContribution.eventsCsv();
  }
  public String getCataSummaryCsv() {
    return caTaContribution == null ? "" : caTaContribution.summaryCsv();
  }

  /** All CSV texts joined with a separator, ready to be written to files. */
  public String csvBundle() {
    if (!enabled) return "";
    StringBuilder out = new StringBuilder();
    appendCsv(out, "checkpoint-fronts.csv", checkpointFront == null ? null : checkpointFront.toCsv());
    appendCsv(out, "checkpoint-metrics.csv", null);
    appendCsv(out, "pddr-full-ledger.csv", pddrLedger == null ? null : pddrLedger.ledgerCsv());
    appendCsv(out, "pddr-cycle-summary.csv", pddrLedger == null ? null : pddrLedger.cycleSummaryCsv());
    appendCsv(out, "teacher-use-events.csv", teacherConcentration == null ? null : teacherConcentration.eventsCsv());
    appendCsv(out, "teacher-concentration.csv", teacherConcentration == null ? null : teacherConcentration.concentrationCsv());
    appendCsv(out, "cata-contribution-events.csv", caTaContribution == null ? null : caTaContribution.eventsCsv());
    appendCsv(out, "cata-contribution-summary.csv", caTaContribution == null ? null : caTaContribution.summaryCsv());
    return out.toString();
  }

  private static void appendCsv(StringBuilder out, String name, String csv) {
    if (csv == null || csv.isEmpty()) return;
    out.append("==== ").append(name).append(" ====\n").append(csv).append('\n');
  }

  public String getRunId() { return runId; }
  public String getSourceJarSha256() { return sourceJarSha256; }
  public String getConfigurationHash() { return configurationHash; }
  public String getInstanceHash() { return instanceHash; }
  public long getSeed() { return seed; }
  public String getArm() { return arm; }
  public String getTelemetryMode() { return telemetryMode; }

  public static List<PermutationSolution<Integer>> nullSafe(List<PermutationSolution<Integer>> value) {
    return value == null ? new ArrayList<PermutationSolution<Integer>>() : value;
  }
}
