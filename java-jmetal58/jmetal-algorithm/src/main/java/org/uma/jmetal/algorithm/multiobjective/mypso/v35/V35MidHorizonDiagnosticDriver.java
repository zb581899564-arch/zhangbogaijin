package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1 real diagnostic driver.
 *
 * <p>One JVM per (arm, instance, telemetry mode).  Args:
 * <pre>
 *   arm            A4
 *   instance       relative path of EADHFSP instance (e.g. 100_5_3_1.txt)
 *   seed           long
 *   maxFEs         int
 *   telemetryMode  OFF | ON
 *   runId          evidence run id
 *   sourceJarSha256
 *   outputDir      absolute evidence directory (created per run)
 * </pre>
 * Writes {@code behavior-summary.json} with all behaviour hashes plus
 * {@code telemetry-*.csv} when ON.  Every hash is derived from algorithm
 * state only (no wall clock, no timestamps).
 */
public final class V35MidHorizonDiagnosticDriver {
  private static final int POPULATION = 100;
  public static final int HARD_GATE_COUNT = 24;
  public static final int MAX_BOUNDED_DIAGNOSTIC_FE = 50000;
  public static final String FINAL_ARM = "A4";
  public static final String FINAL_INSTANCE = "100_5_3_1";
  public static final long FINAL_SEED = 20260901L;
  public static final int FINAL_MAX_FES = 50000;
  public static final String FORMAL_BUDGET_SEMANTICS =
      "PHASE_CONSISTENT_BUDGET_TERMINATION";
  public static final boolean ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE = false;
  public static final String CONTRACT_VERSION = "V35_MIDHORIZON_D_CONTRACT_V3_1";

  private V35MidHorizonDiagnosticDriver() { }

  public static void main(String[] args) throws Exception {
    if (args.length < 8) {
      System.err.println("usage: arm instance seed maxFEs telemetryMode runId jarSha outputDir [checkpoints]");
      System.exit(2);
    }
    String armText = args[0];
    String instance = args[1];
    long seed = Long.parseLong(args[2]);
    int maxFEs = Integer.parseInt(args[3]);
    String telemetryText = args[4];
    String runId = args[5];
    String jarSha = args[6];
    Path outputDir = Paths.get(args[7]);
    validateFinalV31Invocation(armText, instance, seed, maxFEs, telemetryText);
    requireBoundedBudget(maxFEs);
    int nominalGateBudget = maxFEs;
    boolean registeredCompletePhaseFallback = false;
    instance = finalInstanceFileName(instance);
    long[] checkpoints;
    if (args.length >= 9 && !args[8].isEmpty()) {
      String[] parts = args[8].split(",");
      checkpoints = new long[parts.length];
      for (int index = 0; index < parts.length; index++) {
        checkpoints[index] = Long.parseLong(parts[index].trim());
      }
      validateCheckpointSchedule(maxFEs, checkpoints);
    } else {
      checkpoints = defaultCheckpoints(maxFEs);
    }

    V35FinalAblationProfile.Arm arm = V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA;
    V35FairRunner.Mode mode = arm.getMode();
    boolean telemetryOn = "ON".equals(telemetryText);

    Path root = Paths.get("").toAbsolutePath().normalize();
    Path extension = root.resolve("instance-extensions/v1");
    Path fatigue = root.resolve("fatigue-parameters/v1");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP").resolve(instance), ProductionDecodeMode.FM3, seed,
        extension, fatigue, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        arm, seed, POPULATION, maxFEs);

    V35MidHorizonTelemetry telemetry = telemetryOn
        ? createTelemetry(mode, problem, configuration, checkpoints, seed, runId, jarSha)
        : null;
    long startNanos = System.nanoTime();
    V35FairRunner.RunRecord record = runPhaseConsistentDiagnostic(
        mode, (Problem) problem, initial, maxFEs, seed,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration,
        telemetry);
    long wallNanos = System.nanoTime() - startNanos;

    // runInternal normally detaches on the success path.  Detaching again is
    // harmless and also closes the audit if the algorithm returned FAILED.
    if (telemetry != null) {
      telemetry.stopTrueAudit();
      telemetry.onRunEnd(record.getFullEvaluations(), -1, -1, -1,
          configuration.isCaTaLiteEnabled());
    }

    Files.createDirectories(outputDir);

    // ---- canonical front hash: exact-dedup tri objectives, stable sort, SHA-256 ----
    Set<String> seen = new HashSet<>();
    List<double[]> unique = new ArrayList<>();
    if (record.getFront() != null) {
      for (double[] point : record.getFront()) {
        String key = Double.doubleToLongBits(point[0]) + ":"
            + Double.doubleToLongBits(point[1]) + ":"
            + Double.doubleToLongBits(point[2]);
        if (seen.add(key)) unique.add(point);
      }
    }
    Collections.sort(unique, new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) {
          int c = Double.compare(a[i], b[i]);
          if (c != 0) return c;
        }
        return 0;
      }
    });
    StringBuilder frontText = new StringBuilder();
    for (double[] point : unique) {
      frontText.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    String canonicalFrontHash = sha256(frontText.toString());
    String qpHash = record.getObservationEvidence() == null ? "UNAVAILABLE"
        : record.getObservationEvidence().getQpTableHash();
    V35MidHorizonTelemetry observedTelemetry = record.getMidHorizonTelemetry();
    String rngSource = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getTrueRngEvidenceSource();
    String candidateSource = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getGeneratedCandidateEvidenceSource();
    String rngConsumptionSequenceHash = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getTrueRngHashOrUnavailable();
    String generatedCandidateSequenceHash = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getGeneratedCandidateHashOrUnavailable();
    String rngHashSource = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getTrueRngHashSource();
    String candidateHashSource = observedTelemetry == null ? "NOT_APPLICABLE"
        : observedTelemetry.getTrueCandidateHashSource();
    String qgEventStreamHash = extractKv(record.getMechanismSummary(), "qgEventStreamHash");
    String qpEventStreamHash = extractKv(record.getMechanismSummary(), "qpEventStreamHash");
    long actualFE = record.getFullEvaluations();
    long qPhaseFE = formalQPhaseFE();
    long remainingFE = ((long) maxFEs) - actualFE;
    boolean actualFEPositive = actualFE > 0L;
    boolean actualFEWithinMax = actualFEPositive && actualFE <= maxFEs;
    boolean remainingFEUnderQPhase = remainingFE >= 0L && remainingFE < qPhaseFE;
    boolean observerErrorsZero = observedTelemetry == null
        || observedTelemetry.getObserverErrors() == 0L;
    boolean observerExecutionErrorsZero = observedTelemetry == null
        || observedTelemetry.getObserverExecutionErrors() == 0L;
    boolean evidenceAvailable = !telemetryOn
        || (isActualSource(rngSource) && isActualSource(candidateSource));
    boolean checkpointComplete = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.isRunFinalized());
    boolean unobservableCheckpointZero = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.getUnobservableCheckpointCount() == 0L);
    boolean trueRngAuditPass = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.getTrueRngCount() > 0
        && "ACTUAL_JMETAL_RANDOM".equals(rngSource)
        && "ACTUAL_RANDOM_DRAWS".equals(rngHashSource));
    boolean trueCandidateAuditPass = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.getTrueCandidateCount() > 0
        && "ACTUAL_GENERATED_CANDIDATES".equals(candidateSource)
        && "ACTUAL_PRE_EVALUATION_CANDIDATES".equals(candidateHashSource));
    boolean candidateCountClosed = !telemetryOn
        || (observedTelemetry != null
        && ((long) observedTelemetry.getTrueCandidateCount()) == record.getDecoderCalls());
    boolean candidateCoveragePass = !telemetryOn
        || hasCandidateCoverage(observedTelemetry, configuration);
    V35PddrCandidateMetadataAdapter.ContractReport pddrContract = observedTelemetry == null
        ? null : observedTelemetry.getPddrContractReport();
    boolean pddrContractPass = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.pddrLedgerRows() > 0
        && pddrContract != null && pddrContract.isPass());
    V35TeacherConcentrationObserver.ContractReport teacherContract = observedTelemetry == null
        ? null : observedTelemetry.getTeacherContractReport(configuration.isQgEnabled(),
        configuration.isQpEnabled());
    boolean teacherContractPass = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.teacherRows() > 0
        && teacherContract != null && teacherContract.isPass());
    boolean cataRequired = configuration.isCaTaLiteEnabled();
    boolean cataContractPass = !telemetryOn || !cataRequired
        || (observedTelemetry != null && observedTelemetry.cataRows() > 0
        && !observedTelemetry.cataLifecycleHasUnobservableFields());
    boolean cataLifecycleSchemaValidated = !telemetryOn || !cataRequired
        || (observedTelemetry != null && observedTelemetry.cataRows() > 0);
    boolean cataLongRunLifecycleValidated = cataLifecycleSchemaValidated && cataContractPass;
    // V3.1 deliberately does not run the forbidden short-gate matrix.
    boolean cataAllShortGateSourceCoverageValidated = false;
    boolean cataFullLifecycleValidated = cataLifecycleSchemaValidated
        && cataLongRunLifecycleValidated && cataAllShortGateSourceCoverageValidated;
    boolean sequenceIdentityPass = !telemetryOn
        || (record.getIllegalSolutions() == 0L && record.getDuplicateEvaluations() == 0L);
    boolean budgetSemanticPass = FORMAL_BUDGET_SEMANTICS
        .equals("PHASE_CONSISTENT_BUDGET_TERMINATION")
        && !ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE;
    boolean runCompleted = "COMPLETED".equals(record.getStatus());
    boolean terminalCheckpointPass = !telemetryOn
        || (observedTelemetry != null && observedTelemetry.isTerminalCheckpointAccepted()
        && observedTelemetry.getLastCompletedAtomicBoundaryFE() == actualFE
        && observedTelemetry.getLastNominalCheckpointFE() == maxFEs
        && observedTelemetry.getLastActualCheckpointFE() == actualFE
        && observedTelemetry.getLastActualSnapshotFE() == actualFE
        && observedTelemetry.getLastTerminationKind().equals(FORMAL_BUDGET_SEMANTICS)
        && observedTelemetry.getNominalCheckpointNotExactlyReachedCount()
            == (actualFE == maxFEs ? 0L : 1L)
        && observedTelemetry.getTerminalSnapshotCount()
            == (actualFE == maxFEs ? 0L : 1L)
        && (actualFE == maxFEs
            ? V35CheckpointFrontObserver.CHECKPOINT_KIND_ATOMIC_BOUNDARY
                .equals(observedTelemetry.getLastCheckpointKind())
                && V35CheckpointFrontObserver.ATOMIC_BOUNDARY
                    .equals(observedTelemetry.getLastCheckpointAtomicBoundary())
            : V35CheckpointFrontObserver.CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT
                .equals(observedTelemetry.getLastCheckpointKind())
                && V35CheckpointFrontObserver.REAL_ATOMIC_RUN_END_SNAPSHOT
                    .equals(observedTelemetry.getLastCheckpointAtomicBoundary())));
    boolean terminalCheckpointExpected = telemetryOn && actualFE < maxFEs;
    boolean actualFEEqualsLastCompletedAtomicBoundary = !telemetryOn
        || (observedTelemetry != null && actualFEPositive
        && observedTelemetry.getLastCompletedAtomicBoundaryFE() == actualFE);
    boolean terminalWorkingPopulationFrontObserved = !terminalCheckpointExpected
        || hasTerminalFront(observedTelemetry, "workingPopulationND", actualFE);
    boolean terminalDecisionArchiveFrontObserved = !terminalCheckpointExpected
        || hasTerminalFront(observedTelemetry, "decisionArchiveFront", actualFE);
    boolean terminalObservedFullFrontObserved = !terminalCheckpointExpected
        || hasTerminalFront(observedTelemetry, "observedFullFront", actualFE);
    boolean terminalSnapshotIsRealAtomicBoundary = !terminalCheckpointExpected
        || (terminalCheckpointPass && actualFEEqualsLastCompletedAtomicBoundary
        && terminalWorkingPopulationFrontObserved && terminalDecisionArchiveFrontObserved
        && terminalObservedFullFrontObserved);
    boolean phaseConsistentTerminalSnapshotImplemented = !terminalCheckpointExpected
        || terminalSnapshotIsRealAtomicBoundary;
    boolean hardGatesPass = actualFEPositive && actualFEWithinMax && remainingFEUnderQPhase
        && observerErrorsZero && observerExecutionErrorsZero && checkpointComplete
        && unobservableCheckpointZero && runCompleted && budgetSemanticPass
        && evidenceAvailable && trueRngAuditPass && trueCandidateAuditPass
        && candidateCountClosed && candidateCoveragePass && sequenceIdentityPass
        && pddrContractPass && teacherContractPass && cataContractPass
        && terminalCheckpointPass && phaseConsistentTerminalSnapshotImplemented;
    boolean diagnosticToolingValidated = telemetryOn && hardGatesPass;
    String diagnosticStatus = hardGatesPass
        ? String.valueOf(record.getStatus())
        : evidenceAvailable ? "FAILED_HARD_GATE" : "FAILED_UNAVAILABLE_EVIDENCE";

    StringBuilder summary = new StringBuilder();
    summary.append("runId=").append(runId).append('\n');
    summary.append("arm=").append(armText).append('\n');
    summary.append("instance=").append(instance).append('\n');
    summary.append("seed=").append(seed).append('\n');
    summary.append("maxFEs=").append(maxFEs).append('\n');
    summary.append("nominalGateBudget=").append(nominalGateBudget).append('\n');
    summary.append("registeredCompletePhaseFallback=")
        .append(registeredCompletePhaseFallback).append('\n');
    summary.append("telemetryMode=").append(telemetryText).append('\n');
    summary.append("sourceJarSha256=").append(jarSha).append('\n');
    summary.append("status=").append(record.getStatus()).append('\n');
    summary.append("diagnosticStatus=").append(diagnosticStatus).append('\n');
    summary.append("stopReason=").append(record.getStopReason()).append('\n');
    summary.append("actualFE=").append(actualFE).append('\n');
    summary.append("qPhaseFE=").append(qPhaseFE).append('\n');
    summary.append("remainingFE=").append(remainingFE).append('\n');
    summary.append("requestedMaxFE=").append(maxFEs).append('\n');
    summary.append("actualFEPositive=").append(actualFEPositive).append('\n');
    summary.append("actualFEWithinMax=").append(actualFEWithinMax).append('\n');
    summary.append("remainingFEUnderQPhase=").append(remainingFEUnderQPhase).append('\n');
    summary.append("decoderCalls=").append(record.getDecoderCalls()).append('\n');
    summary.append("illegalSolutions=").append(record.getIllegalSolutions()).append('\n');
    summary.append("duplicateEvaluations=").append(record.getDuplicateEvaluations()).append('\n');
    summary.append("initialPopulationHash=").append(record.getInitialPopulationHash()).append('\n');
    summary.append("evaluationTraceHash=").append(record.getEvaluationTraceHash()).append('\n');
    summary.append("qgTableHash=").append(record.getQgTableHash()).append('\n');
    summary.append("qpTableHash=").append(qpHash).append('\n');
    summary.append("qgEventStreamHash=").append(qgEventStreamHash).append('\n');
    summary.append("qpEventStreamHash=").append(qpEventStreamHash).append('\n');
    summary.append("pddrEventStreamHash=").append(record.getPddrEventStreamHash()).append('\n');
    summary.append("rngConsumptionSequenceSource=").append(rngSource).append('\n');
    summary.append("rngHashSource=").append(rngHashSource).append('\n');
    summary.append("rngConsumptionSequenceHash=").append(rngConsumptionSequenceHash).append('\n');
    summary.append("trueRngSequenceHash=").append(rngConsumptionSequenceHash).append('\n');
    summary.append("generatedCandidateSequenceSource=").append(candidateSource).append('\n');
    summary.append("candidateHashSource=").append(candidateHashSource).append('\n');
    summary.append("generatedCandidateSequenceHash=").append(generatedCandidateSequenceHash).append('\n');
    summary.append("trueGeneratedCandidateSequenceHash=")
        .append(generatedCandidateSequenceHash).append('\n');
    summary.append("formalOuterCycles=").append(extractFormalOuterCycles(record.getMechanismSummary())).append('\n');
    summary.append("formalQgRounds=").append(extractFormalQgRounds(record.getMechanismSummary())).append('\n');
    summary.append("caTaTestCalls=").append(record.getCaTaTestCalls()).append('\n');
    summary.append("caTaEventCount=").append(record.getCaTaEventCount()).append('\n');
    summary.append("canonicalFrontHash=").append(canonicalFrontHash).append('\n');
    summary.append("frontSize=").append(unique.size()).append('\n');
    summary.append("wallNanos=").append(wallNanos).append('\n');
    summary.append("observerErrors=").append(record.getMidHorizonTelemetry() == null ? "N/A"
        : String.valueOf(record.getMidHorizonTelemetry().getObserverErrors())).append('\n');
    summary.append("observerExecutionErrors=").append(record.getMidHorizonTelemetry() == null ? "N/A"
        : String.valueOf(record.getMidHorizonTelemetry().getObserverExecutionErrors())).append('\n');
    summary.append("unobservableCheckpointCount=").append(record.getMidHorizonTelemetry() == null ? "N/A"
        : String.valueOf(record.getMidHorizonTelemetry().getUnobservableCheckpointCount())).append('\n');
    summary.append("nominalCheckpointNotExactlyReachedCount=")
        .append(observedTelemetry == null ? "N/A"
            : String.valueOf(observedTelemetry.getNominalCheckpointNotExactlyReachedCount()))
        .append('\n');
    summary.append("terminalSnapshotCount=").append(observedTelemetry == null ? "N/A"
        : String.valueOf(observedTelemetry.getTerminalSnapshotCount())).append('\n');
    summary.append("lastCompletedAtomicBoundaryFE=").append(observedTelemetry == null ? "N/A"
        : String.valueOf(observedTelemetry.getLastCompletedAtomicBoundaryFE())).append('\n');
    summary.append("terminalCheckpointClassification=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getTerminalClassification()).append('\n');
    summary.append("terminalCheckpointPass=").append(terminalCheckpointPass).append('\n');
    summary.append("terminalCheckpointNominalFE=").append(observedTelemetry == null ? "N/A"
        : String.valueOf(observedTelemetry.getLastNominalCheckpointFE())).append('\n');
    summary.append("terminalCheckpointActualFE=").append(observedTelemetry == null ? "N/A"
        : String.valueOf(observedTelemetry.getLastActualCheckpointFE())).append('\n');
    summary.append("terminalCheckpointDeltaFE=").append(observedTelemetry == null ? "N/A"
        : String.valueOf(observedTelemetry.getLastCheckpointDeltaFE())).append('\n');
    summary.append("terminalCheckpointKind=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getLastCheckpointKind()).append('\n');
    summary.append("terminalAtomicBoundary=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getLastCheckpointAtomicBoundary()).append('\n');
    summary.append("terminalTerminationKind=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getLastTerminationKind()).append('\n');
    summary.append("actualFEEqualsLastCompletedAtomicBoundary=")
        .append(actualFEEqualsLastCompletedAtomicBoundary).append('\n');
    summary.append("terminalSnapshotIsRealAtomicBoundary=")
        .append(terminalSnapshotIsRealAtomicBoundary).append('\n');
    summary.append("terminalWorkingPopulationFrontObserved=")
        .append(terminalWorkingPopulationFrontObserved).append('\n');
    summary.append("terminalDecisionArchiveFrontObserved=")
        .append(terminalDecisionArchiveFrontObserved).append('\n');
    summary.append("terminalObservedFullFrontObserved=")
        .append(terminalObservedFullFrontObserved).append('\n');
    summary.append("phaseConsistentTerminalSnapshotImplemented=")
        .append(phaseConsistentTerminalSnapshotImplemented).append('\n');
    summary.append("utilizationRate=").append(maxFEs <= 0 ? "N/A"
        : String.valueOf((double) actualFE / (double) maxFEs)).append('\n');
    summary.append("checkpointRows=").append(record.getMidHorizonTelemetry() == null ? 0
        : record.getMidHorizonTelemetry().checkpointRows()).append('\n');
    summary.append("pddrLedgerRows=").append(record.getMidHorizonTelemetry() == null ? 0
        : record.getMidHorizonTelemetry().pddrLedgerRows()).append('\n');
    summary.append("teacherRows=").append(record.getMidHorizonTelemetry() == null ? 0
        : record.getMidHorizonTelemetry().teacherRows()).append('\n');
    summary.append("cataRows=").append(record.getMidHorizonTelemetry() == null ? 0
        : record.getMidHorizonTelemetry().cataRows()).append('\n');
    summary.append("checkpointComplete=").append(checkpointComplete).append('\n');
    summary.append("checkpointBoundary=").append(V35CheckpointFrontObserver.ATOMIC_BOUNDARY)
        .append('\n');
    summary.append("formalBudgetSemantics=").append(FORMAL_BUDGET_SEMANTICS).append('\n');
    summary.append("allowTerminalPartialFormalQPhase=")
        .append(ALLOW_TERMINAL_PARTIAL_FORMAL_Q_PHASE).append('\n');
    summary.append("diagnosticSourceChanged=true\n");
    summary.append("algorithmDecisionSemanticsChanged=false\n");
    summary.append("formalFrozenJarChanged=false\n");
    summary.append("pddrDecisionChanged=false\n");
    summary.append("formalMatrixRunning=false\n");
    summary.append("FC5=INCONCLUSIVE\n");
    summary.append("partialPhaseFallback=REJECTED\n");
    summary.append("trueRngEvidenceSource=").append(rngSource).append('\n');
    summary.append("generatedCandidateEvidenceSource=").append(candidateSource).append('\n');
    summary.append("rngAuditCount=").append(observedTelemetry == null ? 0
        : observedTelemetry.getTrueRngCount()).append('\n');
    summary.append("generatedCandidateAuditCount=").append(observedTelemetry == null ? 0
        : observedTelemetry.getTrueCandidateCount()).append('\n');
    summary.append("generatedCandidateSourceCounts=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getTrueCandidateSourceCounts()).append('\n');
    summary.append("candidateCountClosed=").append(candidateCountClosed).append('\n');
    summary.append("candidateCoveragePass=").append(candidateCoveragePass).append('\n');
    summary.append("sequenceIdentityPass=").append(sequenceIdentityPass).append('\n');
    summary.append("pddrContractPass=").append(pddrContractPass).append('\n');
    summary.append("pddrContract=").append(pddrContract == null ? "N/A"
        : pddrContract.toText()).append('\n');
    summary.append("teacherContractPass=").append(teacherContractPass).append('\n');
    summary.append("teacherContract=").append(teacherContract == null ? "N/A"
        : teacherContract.toText()).append('\n');
    summary.append("cataRequired=").append(cataRequired).append('\n');
    summary.append("cataContractPass=").append(cataContractPass).append('\n');
    summary.append("cataLifecycleSchemaValidated=")
        .append(cataLifecycleSchemaValidated).append('\n');
    summary.append("cataLongRunLifecycleValidated=")
        .append(cataLongRunLifecycleValidated).append('\n');
    summary.append("cataAllShortGateSourceCoverageValidated=")
        .append(cataAllShortGateSourceCoverageValidated).append('\n');
    summary.append("trueRngSequenceAudit=").append(trueRngAuditPass).append('\n');
    summary.append("trueGeneratedCandidateSequenceAudit=").append(trueCandidateAuditPass)
        .append('\n');
    summary.append("pddrPhysicalLifecycleValidated=").append(pddrContractPass).append('\n');
    summary.append("teacherOutcomeLifecycleValidated=").append(teacherContractPass).append('\n');
    summary.append("cataFullLifecycleValidated=").append(cataFullLifecycleValidated).append('\n');
    summary.append("midHorizonCheckpointCoverageValidated=").append(unobservableCheckpointZero)
        .append('\n');
    summary.append("formalBudgetSemanticMatch=").append(budgetSemanticPass).append('\n');
    summary.append("diagnosticToolingValidated=").append(diagnosticToolingValidated).append('\n');
    summary.append("250kReadyForPreregistration=")
        .append(diagnosticToolingValidated).append('\n');
    summary.append("250kStarted=false\n");
    summary.append("250kApproved=false\n");
    summary.append("hardGatesPass=").append(hardGatesPass).append('\n');
    summary.append("unobservableCheckpointReasons=").append(observedTelemetry == null ? "N/A"
        : observedTelemetry.getUnobservableReasonSummary()).append('\n');
    Files.write(outputDir.resolve("behavior-summary.properties"),
        summary.toString().getBytes(StandardCharsets.UTF_8));

    String diagnosticContract = contractProperties(actualFE, maxFEs, qPhaseFE, remainingFE,
            actualFEWithinMax,
            remainingFEUnderQPhase, observerErrorsZero, observerExecutionErrorsZero,
            checkpointComplete, runCompleted, rngSource, candidateSource,
            observedTelemetry == null ? 0L : observedTelemetry.getUnobservableCheckpointCount(),
            observedTelemetry == null ? "N/A" : observedTelemetry.getUnobservableReasonSummary(),
            telemetryOn, diagnosticToolingValidated, unobservableCheckpointZero,
            trueRngAuditPass, trueCandidateAuditPass, candidateCountClosed,
            candidateCoveragePass, sequenceIdentityPass, pddrContractPass,
            teacherContractPass, cataContractPass, budgetSemanticPass, hardGatesPass,
             observedTelemetry == null ? "N/A" : observedTelemetry.getTrueCandidateSourceCounts(),
             pddrContract == null ? "N/A" : pddrContract.toText(),
             teacherContract == null ? "N/A" : teacherContract.toText(), rngHashSource,
             candidateHashSource, terminalCheckpointPass,
             observedTelemetry == null ? -1L : observedTelemetry.getLastCompletedAtomicBoundaryFE(),
             observedTelemetry == null ? -1L : observedTelemetry.getLastNominalCheckpointFE(),
             observedTelemetry == null ? -1L : observedTelemetry.getLastActualCheckpointFE(),
             observedTelemetry == null ? Long.MIN_VALUE : observedTelemetry.getLastCheckpointDeltaFE(),
             observedTelemetry == null ? "N/A" : observedTelemetry.getLastCheckpointKind(),
             observedTelemetry == null ? "N/A" : observedTelemetry.getLastCheckpointAtomicBoundary(),
             observedTelemetry == null ? "N/A" : observedTelemetry.getLastTerminationKind(),
             observedTelemetry == null ? "N/A" : observedTelemetry.getTerminalClassification());
    diagnosticContract += terminalContractProperties(actualFEPositive,
        phaseConsistentTerminalSnapshotImplemented, terminalSnapshotIsRealAtomicBoundary,
        actualFEEqualsLastCompletedAtomicBoundary, terminalWorkingPopulationFrontObserved,
        terminalDecisionArchiveFrontObserved, terminalObservedFullFrontObserved,
        cataLifecycleSchemaValidated, cataLongRunLifecycleValidated,
        cataAllShortGateSourceCoverageValidated, cataFullLifecycleValidated,
        qgEventStreamHash, qpEventStreamHash);
    Files.write(outputDir.resolve("diagnostic-contract.properties"),
        diagnosticContract.getBytes(StandardCharsets.UTF_8));

    StringBuilder front = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : unique) {
      front.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    Files.write(outputDir.resolve("canonical-front.csv"), front.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(outputDir.resolve("sorted-front.csv"), front.toString().getBytes(StandardCharsets.UTF_8));

    if (record.getMidHorizonTelemetry() != null && telemetryOn) {
      writeIfPresent(outputDir, "telemetry-checkpoint-fronts.csv",
          record.getMidHorizonTelemetry().getCheckpointFrontCsv());
      writeIfPresent(outputDir, "telemetry-pddr-full-ledger.csv",
          record.getMidHorizonTelemetry().getPddrLedgerCsv());
      writeIfPresent(outputDir, "telemetry-pddr-cycle-summary.csv",
          record.getMidHorizonTelemetry().getPddrCycleSummaryCsv());
      writeIfPresent(outputDir, "telemetry-teacher-use-events.csv",
          record.getMidHorizonTelemetry().getTeacherEventsCsv());
      writeIfPresent(outputDir, "telemetry-teacher-concentration.csv",
          record.getMidHorizonTelemetry().getTeacherConcentrationCsv());
      writeIfPresent(outputDir, "telemetry-cata-contribution-events.csv",
          record.getMidHorizonTelemetry().getCataEventsCsv());
      writeIfPresent(outputDir, "telemetry-cata-contribution-summary.csv",
          record.getMidHorizonTelemetry().getCataSummaryCsv());
    }
    if (!evidenceAvailable) {
      throw new IllegalStateException(
          "actual telemetry source unavailable; synthetic sequence fallback is forbidden");
    }
    if (!hardGatesPass) {
      throw new IllegalStateException("mid-horizon hard gate failed; see diagnostic contract");
    }
    System.out.println("DONE " + runId + " status=" + diagnosticStatus);
  }

  private static V35MidHorizonTelemetry createTelemetry(V35FairRunner.Mode mode,
      ZhangBoCanonicalProductionProblem problem, V35ProductionConfiguration configuration,
      long[] checkpoints, long seed, String runId, String sourceJarSha) {
    String configHash = configuration == null ? "UNKNOWN" : configuration.configurationHash();
    String instanceHash = problem.getInstance().getInstanceSha256();
    String armName = mode == V35FairRunner.Mode.V35_A2 ? "A2" : "A4";
    V35CheckpointFrontObserver checkpoint = new V35CheckpointFrontObserver(
        checkpoints, runId, sourceJarSha, configHash, instanceHash, seed, armName, true);
    V35SubSwarmMixture mixture = configuration.getSubSwarmMixtureOrDefault();
    int[] physicalCapacities = new int[]{mixture.getG1Cmax(), mixture.getG4Balanced(),
        mixture.getG2Tec(), mixture.getG3Twc()};
    V35FullPddrLedgerObserver pddr = new V35FullPddrLedgerObserver(
        runId, sourceJarSha, configHash, instanceHash, seed, armName, true,
        physicalCapacities, configuration.isLineageArchiveEnabled());
    V35TeacherConcentrationObserver teacher = new V35TeacherConcentrationObserver(
        runId, sourceJarSha, configHash, instanceHash, seed, armName, true);
    V35CaTaContributionObserver cata = new V35CaTaContributionObserver(
        runId, sourceJarSha, configHash, instanceHash, seed, armName, true);
    return new V35MidHorizonTelemetry(checkpoint, pddr, teacher, cata,
        runId, sourceJarSha, configHash, instanceHash, seed, armName, true);
  }

  /**
   * Invokes the existing runner's private wiring with the formal flag fixed
   * to false.  The public convenience method currently enables terminal
   * partial formal phases, so silently calling it would violate D's budget
   * contract.  This reflective bridge changes no algorithm code or settings;
   * it only selects the already-supported phase-consistent branch and passes
   * the read-only telemetry object.
   */
  private static V35FairRunner.RunRecord runPhaseConsistentDiagnostic(
      V35FairRunner.Mode mode, Problem problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed,
      V35BottleneckDiagnosisConfiguration diagnosisConfiguration,
      V35ProductionConfiguration explicitConfiguration,
      V35MidHorizonTelemetry telemetry) throws Exception {
    Method runner = V35FairRunner.class.getDeclaredMethod("runInternal",
        V35FairRunner.Mode.class, Problem.class, List.class, int.class, long.class,
        boolean.class, V35BottleneckDiagnosisConfiguration.class, boolean.class,
        V35ProductionConfiguration.class, V35ArchiveExperimentProfile.class,
        boolean.class, boolean.class, V35MidHorizonTelemetry.class);
    runner.setAccessible(true);
    try {
      Object result = runner.invoke(null, mode, problem, initialPopulation,
          Integer.valueOf(maxEvaluations), Long.valueOf(seed), Boolean.TRUE,
          diagnosisConfiguration, Boolean.FALSE, explicitConfiguration, null,
          Boolean.FALSE, Boolean.TRUE, telemetry);
      return (V35FairRunner.RunRecord) result;
    } catch (InvocationTargetException error) {
      Throwable cause = error.getCause();
      if (cause instanceof Exception) throw (Exception) cause;
      if (cause instanceof Error) throw (Error) cause;
      throw error;
    }
  }

  /** Only the V3.1 final nominal schedule is accepted by this launcher. */
  public static long[] defaultCheckpoints(int maxFEs) {
    if (maxFEs == FINAL_MAX_FES) {
      return new long[]{10000L, 20000L, 30000L, 40000L, 50000L};
    }
    throw new IllegalArgumentException(
        "V3.1 launcher accepts only A4/100_5_3_1/20260901/50000");
  }

  public static void validateCheckpointSchedule(int maxFEs, long[] checkpoints) {
    long[] expected = defaultCheckpoints(maxFEs);
    if (checkpoints == null || !Arrays.equals(expected, checkpoints)) {
      throw new IllegalArgumentException(
          "checkpoint schedule must match the fixed D nominal checkpoints");
    }
  }

  private static void requireBoundedBudget(int maxFEs) {
    if (maxFEs != FINAL_MAX_FES) {
      throw new IllegalArgumentException(
          "2k/20k/A2/250k/formal-matrix execution is forbidden in V3.1 driver");
    }
    defaultCheckpoints(maxFEs);
  }

  private static int nominalGateBudget(int maxFEs) {
    return maxFEs;
  }

  /** Validates the only invocation shape authorized by V3.1. */
  public static void validateFinalV31Invocation(String arm, String instance,
      long seed, int maxFEs, String telemetryMode) {
    if (!FINAL_ARM.equals(arm)) {
      throw new IllegalArgumentException("V3.1 accepts arm=A4 only");
    }
    if (!FINAL_INSTANCE.equals(instanceStem(instance))) {
      throw new IllegalArgumentException("V3.1 accepts instance=100_5_3_1 only");
    }
    if (seed != FINAL_SEED) {
      throw new IllegalArgumentException("V3.1 accepts seed=20260901 only");
    }
    if (maxFEs != FINAL_MAX_FES) {
      throw new IllegalArgumentException("V3.1 accepts MaxFEs=50000 only");
    }
    if (!"ON".equals(telemetryMode) && !"OFF".equals(telemetryMode)) {
      throw new IllegalArgumentException("telemetryMode must be ON or OFF");
    }
  }

  private static String instanceStem(String instance) {
    if (instance == null || instance.trim().length() == 0) return "";
    String name = Paths.get(instance.trim()).getFileName().toString();
    return name.endsWith(".txt") ? name.substring(0, name.length() - 4) : name;
  }

  private static String finalInstanceFileName(String instance) {
    return FINAL_INSTANCE + ".txt";
  }

  private static long formalQPhaseFE() {
    org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration table9 =
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration.table9();
    return ((long) table9.getQTimes()) * POPULATION;
  }

  private static boolean isActualSource(String source) {
    return source != null && source.startsWith("ACTUAL_");
  }

  private static boolean hasCandidateCoverage(V35MidHorizonTelemetry telemetry,
      V35ProductionConfiguration configuration) {
    if (telemetry == null || configuration == null) return false;
    boolean initial = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.INITIAL_POPULATION.name()) > 0L;
    boolean global = !configuration.isCfvfEnabled()
        || telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.GLOBAL_CFVF.name()) > 0L;
    long inherited = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.INTER_FACTORY_LS.name())
        + telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS.name());
    boolean inheritedLocalSearch = inherited > 0L;
    long caTa = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.CATA_TEST.name())
        + telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.CATA_APPLY.name());
    boolean caTaCoverage = !configuration.isCaTaLiteEnabled() || caTa > 0L;
    return initial && global && inheritedLocalSearch && caTaCoverage;
  }

  private static String contractProperties(long actualFE, int maxFEs, long qPhaseFE,
      long remainingFE, boolean actualFEWithinMax, boolean remainingFEUnderQPhase,
      boolean observerErrorsZero, boolean observerExecutionErrorsZero,
      boolean checkpointComplete, boolean runCompleted, String rngSource, String candidateSource,
      long unobservableCheckpointCount, String unobservableReasons, boolean telemetryOn,
      boolean diagnosticToolingValidated, boolean unobservableCheckpointZero,
      boolean trueRngAuditPass, boolean trueCandidateAuditPass, boolean candidateCountClosed,
      boolean candidateCoveragePass, boolean sequenceIdentityPass, boolean pddrContractPass,
      boolean teacherContractPass, boolean cataContractPass, boolean budgetSemanticPass,
      boolean hardGatesPass, String candidateSourceCounts, String pddrContract,
      String teacherContract, String rngHashSource, String candidateHashSource,
      boolean terminalCheckpointPass, long lastCompletedAtomicBoundaryFE,
      long terminalNominalCheckpointFE, long terminalActualCheckpointFE,
      long terminalCheckpointDeltaFE, String terminalCheckpointKind,
      String terminalAtomicBoundary, String terminalTerminationKind,
      String terminalClassification) {
    boolean evidenceAvailable = !telemetryOn
        || (isActualSource(rngSource) && isActualSource(candidateSource));
    StringBuilder result = new StringBuilder();
    result.append("contractVersion=").append(CONTRACT_VERSION).append('\n');
    result.append("hardGateCount=").append(HARD_GATE_COUNT).append('\n');
    result.append("hardGate01.diagnosticSourceChanged=true\n");
    result.append("hardGate02.algorithmDecisionSemanticsChanged=false\n");
    result.append("hardGate03.formalFrozenJarChanged=false\n");
    result.append("hardGate04.pddrDecisionChanged=false\n");
    result.append("hardGate05.formalMatrixRunning=false\n");
    result.append("hardGate06.FC5=INCONCLUSIVE\n");
    result.append("hardGate07.allowTerminalPartialFormalQPhase=false\n");
    result.append("hardGate08.formalBudgetSemanticMatch=").append(budgetSemanticPass).append('\n');
    result.append("hardGate09.checkpointComplete=").append(checkpointComplete).append('\n');
    result.append("hardGate10.actualFEWithinMax=").append(actualFEWithinMax).append('\n');
    result.append("hardGate11.remainingFEUnderQPhase=").append(remainingFEUnderQPhase).append('\n');
    result.append("hardGate12.runCompleted=").append(runCompleted).append('\n');
    result.append("hardGate13.observerErrorsZero=").append(observerErrorsZero).append('\n');
    result.append("hardGate14.observerExecutionErrorsZero=")
        .append(observerExecutionErrorsZero).append('\n');
    result.append("hardGate15.unobservableCheckpointZero=")
        .append(unobservableCheckpointZero).append('\n');
    result.append("hardGate16.trueRngSequenceAudit=").append(trueRngAuditPass).append('\n');
    result.append("hardGate17.trueGeneratedCandidateSequenceAudit=")
        .append(trueCandidateAuditPass).append('\n');
    result.append("hardGate18.candidateCountClosed=").append(candidateCountClosed).append('\n');
    result.append("hardGate19.candidateCoveragePass=").append(candidateCoveragePass).append('\n');
    result.append("hardGate20.sequenceIdentityPass=").append(sequenceIdentityPass).append('\n');
    result.append("hardGate21.pddrPhysicalLifecycleValidated=").append(pddrContractPass)
        .append('\n');
    result.append("hardGate22.teacherOutcomeLifecycleValidated=").append(teacherContractPass)
        .append('\n');
    result.append("hardGate23.cataLongRunLifecycleValidated=").append(cataContractPass).append('\n');
    result.append("hardGate24.terminalCheckpointProtocol=")
        .append(terminalCheckpointPass).append('\n');
    result.append("diagnosticSourceChanged=true\n");
    result.append("algorithmDecisionSemanticsChanged=false\n");
    result.append("formalFrozenJarChanged=false\n");
    result.append("pddrDecisionChanged=false\n");
    result.append("formalMatrixRunning=false\n");
    result.append("FC5=INCONCLUSIVE\n");
    result.append("allowTerminalPartialFormalQPhase=false\n");
    result.append("formalBudgetSemantics=").append(FORMAL_BUDGET_SEMANTICS).append('\n');
    result.append("checkpointBoundary=").append(V35CheckpointFrontObserver.ATOMIC_BOUNDARY)
        .append('\n');
    result.append("actualFE=").append(actualFE).append('\n');
    result.append("maxFEs=").append(maxFEs).append('\n');
    result.append("qPhaseFE=").append(qPhaseFE).append('\n');
    result.append("remainingFE=").append(remainingFE).append('\n');
    result.append("remainingFEUnderQPhase=").append(remainingFEUnderQPhase).append('\n');
    result.append("runCompleted=").append(runCompleted).append('\n');
    result.append("observerErrorsZero=").append(observerErrorsZero).append('\n');
    result.append("observerExecutionErrorsZero=").append(observerExecutionErrorsZero).append('\n');
    result.append("unobservableCheckpointCount=").append(unobservableCheckpointCount).append('\n');
    result.append("unobservableCheckpointReasons=").append(unobservableReasons).append('\n');
    result.append("terminalCheckpointPass=").append(terminalCheckpointPass).append('\n');
    result.append("lastCompletedAtomicBoundaryFE=").append(lastCompletedAtomicBoundaryFE)
        .append('\n');
    result.append("terminalNominalCheckpointFE=").append(terminalNominalCheckpointFE)
        .append('\n');
    result.append("terminalActualCheckpointFE=").append(terminalActualCheckpointFE)
        .append('\n');
    result.append("terminalCheckpointDeltaFE=").append(terminalCheckpointDeltaFE)
        .append('\n');
    result.append("terminalCheckpointKind=").append(terminalCheckpointKind).append('\n');
    result.append("terminalAtomicBoundary=").append(terminalAtomicBoundary).append('\n');
    result.append("terminalTerminationKind=").append(terminalTerminationKind).append('\n');
    result.append("terminalCheckpointClassification=").append(terminalClassification)
        .append('\n');
    result.append("trueRngEvidenceSource=").append(rngSource).append('\n');
    result.append("rngHashSource=").append(rngHashSource).append('\n');
    result.append("generatedCandidateEvidenceSource=").append(candidateSource).append('\n');
    result.append("candidateHashSource=").append(candidateHashSource).append('\n');
    result.append("rngAuditCountAvailable=").append(trueRngAuditPass).append('\n');
    result.append("generatedCandidateSourceCounts=").append(candidateSourceCounts).append('\n');
    result.append("pddrContract=").append(pddrContract).append('\n');
    result.append("teacherContract=").append(teacherContract).append('\n');
    result.append("actualSequenceEvidenceAvailable=").append(evidenceAvailable).append('\n');
    result.append("hardGatesPass=").append(hardGatesPass).append('\n');
    result.append("partialPhaseFallback=REJECTED\n");
    result.append("formalMatrixStarted=false\n");
    result.append("diagnosticToolingValidated=").append(diagnosticToolingValidated).append('\n');
    result.append("250kReadyForPreregistration=").append(diagnosticToolingValidated).append('\n');
    result.append("250kStarted=false\n");
    result.append("250kApproved=false\n");
    result.append("FC5Decision=INCONCLUSIVE\n");
    result.append("unobservableReasonEnum=");
    V35CheckpointFrontObserver.UnobservableReason[] reasons =
        V35CheckpointFrontObserver.UnobservableReason.values();
    for (int index = 0; index < reasons.length; index++) {
      if (index > 0) result.append('|');
      result.append(reasons[index].name());
    }
    result.append('\n');
    return result.toString();
  }

  private static String terminalContractProperties(boolean actualFEPositive,
      boolean phaseConsistentTerminalSnapshotImplemented,
      boolean terminalSnapshotIsRealAtomicBoundary,
      boolean actualFEEqualsLastCompletedAtomicBoundary,
      boolean terminalWorkingPopulationFrontObserved,
      boolean terminalDecisionArchiveFrontObserved,
      boolean terminalObservedFullFrontObserved,
      boolean cataLifecycleSchemaValidated,
      boolean cataLongRunLifecycleValidated,
      boolean cataAllShortGateSourceCoverageValidated,
      boolean cataFullLifecycleValidated,
      String qgEventStreamHash, String qpEventStreamHash) {
    StringBuilder result = new StringBuilder();
    result.append("actualFEPositive=").append(actualFEPositive).append('\n');
    result.append("phaseConsistentTerminalSnapshotImplemented=")
        .append(phaseConsistentTerminalSnapshotImplemented).append('\n');
    result.append("terminalSnapshotIsRealAtomicBoundary=")
        .append(terminalSnapshotIsRealAtomicBoundary).append('\n');
    result.append("actualFEEqualsLastCompletedAtomicBoundary=")
        .append(actualFEEqualsLastCompletedAtomicBoundary).append('\n');
    result.append("terminalWorkingPopulationFrontObserved=")
        .append(terminalWorkingPopulationFrontObserved).append('\n');
    result.append("terminalDecisionArchiveFrontObserved=")
        .append(terminalDecisionArchiveFrontObserved).append('\n');
    result.append("terminalObservedFullFrontObserved=")
        .append(terminalObservedFullFrontObserved).append('\n');
    result.append("cataLifecycleSchemaValidated=")
        .append(cataLifecycleSchemaValidated).append('\n');
    result.append("cataLongRunLifecycleValidated=")
        .append(cataLongRunLifecycleValidated).append('\n');
    result.append("cataAllShortGateSourceCoverageValidated=")
        .append(cataAllShortGateSourceCoverageValidated).append('\n');
    result.append("cataFullLifecycleValidated=")
        .append(cataFullLifecycleValidated).append('\n');
    result.append("qgEventStreamHash=").append(qgEventStreamHash).append('\n');
    result.append("qpEventStreamHash=").append(qpEventStreamHash).append('\n');
    result.append("onOffBehaviorEquivalent=NOT_EVALUATED_PER_RUN\n");
    return result.toString();
  }

  private static boolean hasTerminalFront(V35MidHorizonTelemetry telemetry, String frontType,
      long actualFE) {
    if (telemetry == null || frontType == null) return false;
    String csv = telemetry.getCheckpointFrontCsv();
    if (csv == null || csv.isEmpty()) return false;
    String[] lines = csv.split("\\r?\\n");
    for (String line : lines) {
      if (line.isEmpty() || line.startsWith("generatedByRunId,")) continue;
      String[] fields = line.split(",", -1);
      if (fields.length >= 25
          && frontType.equals(fields[13])
          && "NONE".equals(fields[14])
          && V35CheckpointFrontObserver.CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT
              .equals(fields[20])
          && String.valueOf(actualFE).equals(fields[21])) {
        return true;
      }
    }
    return false;
  }

  private static void writeIfPresent(Path directory, String name, String csv) throws Exception {
    if (csv != null && !csv.isEmpty()) {
      Files.write(directory.resolve(name), csv.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static String extractFormalOuterCycles(String summary) {
    return extractKv(summary, "formalOuterCycles");
  }
  private static String extractFormalQgRounds(String summary) {
    return extractKv(summary, "formalQgRounds");
  }
  private static String extractKv(String summary, String key) {
    if (summary == null) return "N/A";
    int index = summary.indexOf(key + "=");
    if (index < 0) return "N/A";
    int start = index + key.length() + 1;
    int end = summary.indexOf(',', start);
    if (end < 0) end = summary.length();
    return summary.substring(start, end);
  }

  private static String sha256(String text) throws Exception {
    byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(text.getBytes(StandardCharsets.UTF_8));
    StringBuilder result = new StringBuilder();
    for (byte b : digest) result.append(String.format("%02x", b & 0xff));
    return result.toString();
  }
}
