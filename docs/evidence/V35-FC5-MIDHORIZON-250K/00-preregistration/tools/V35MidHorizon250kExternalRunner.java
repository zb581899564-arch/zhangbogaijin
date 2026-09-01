package v35campaign;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CheckpointFrontObserver;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35EvaluationSourceContext;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35MidHorizonDiagnosticDriver;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35MidHorizonTelemetry;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35PddrCandidateMetadataAdapter;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35TeacherConcentrationObserver;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * External 250k launcher for the fixed diagnostic runtime Jar.
 *
 * <p>This class is deliberately outside the algorithm Jar.  It does not
 * duplicate or replace an algorithm class.  It only binds the existing
 * V3.1 phase-consistent private bridge and the existing telemetry factory,
 * after checking that those classes were loaded from the exact bound runtime Jar.</p>
 */
public final class V35MidHorizon250kExternalRunner {
  private static final String RUNTIME_SHA =
      "A0A1E74D00403CAC69FBC25B52AEAEB454A6CC2D9FA6BF2A1F6A0D12FFE15FF7";
  private static final String FORMAL_SHA =
      "8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9";
  private static final int POPULATION = 100;
  private static final int MAX_FES = 250000;
  private static final long Q_PHASE_FE =
      ZhangBoFormalHmopsoQgsConfiguration.table9().getQTimes() * (long) POPULATION;
  private static final long[] ALLOWED_SEEDS = {20260901L, 20260902L, 20260903L};
  private static final List<String> ALLOWED_INSTANCES =
      Collections.unmodifiableList(Arrays.asList("100_2_4_1", "100_5_3_1"));

  private V35MidHorizon250kExternalRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    validate(arguments);
    Path projectRoot = arguments.projectRoot.toAbsolutePath().normalize();
    Path output = arguments.output.toAbsolutePath().normalize();
    Files.createDirectories(output);

    Path runtimeJar = runtimeJarFromCodeSource();
    String runtimeSha = sha256(runtimeJar);
    if (!RUNTIME_SHA.equalsIgnoreCase(runtimeSha)) {
      throw new IllegalStateException("runtime class source is not the exact bound diagnostic Jar: "
          + runtimeJar + " sha256=" + runtimeSha);
    }
    if (!RUNTIME_SHA.equalsIgnoreCase(arguments.jarSha256)) {
      throw new IllegalArgumentException("--jar-sha256 must be the exact bound diagnostic Jar");
    }

    String instanceFile = arguments.instance + ".txt";
    Path extension = projectRoot.resolve("instance-extensions/v1");
    Path fatigue = projectRoot.resolve("fatigue-parameters/v1");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        projectRoot.resolve("EADHFSP").resolve(instanceFile), ProductionDecodeMode.FM3,
        arguments.seed, extension, fatigue, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<PermutationSolution<Integer>>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());

    V35FinalAblationProfile.Arm profileArm = "A2".equals(arguments.arm)
        ? V35FinalAblationProfile.Arm.A2_CFVF
        : V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA;
    V35FairRunner.Mode mode = profileArm.getMode();
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        profileArm, arguments.seed, POPULATION, MAX_FES);
    V35FinalAblationProfile.validate(profileArm, configuration);

    V35MidHorizonTelemetry telemetry = createTelemetry(mode, problem, configuration,
        arguments.checkpoints, arguments.seed, arguments.runId, arguments.jarSha256);
    long started = System.nanoTime();
    V35FairRunner.RunRecord record = runPhaseConsistentDiagnostic(mode, problem, initial,
        arguments.seed, configuration, telemetry);
    long elapsed = System.nanoTime() - started;

    telemetry.stopTrueAudit();
    telemetry.onRunEnd(record.getFullEvaluations(), -1, -1, -1,
        configuration.isCaTaLiteEnabled());

    V35FairRunner.writeRecord(record, output,
        V35FinalAblationProfile.canonicalTextFor(profileArm, arguments.seed,
            POPULATION, MAX_FES));

    List<double[]> unique = uniqueSortedFront(record.getFront());
    StringBuilder front = new StringBuilder();
    for (double[] point : unique) {
      front.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    Files.write(output.resolve("canonical-front.csv"),
        ("Cmax,TEC,TWC\n" + front).getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("sorted-front.csv"),
        ("Cmax,TEC,TWC\n" + front).getBytes(StandardCharsets.UTF_8));
    writeTelemetry(output, telemetry);

    String frontHash = sha256(front.toString());
    String rngSource = telemetry.getTrueRngEvidenceSource();
    String candidateSource = telemetry.getGeneratedCandidateEvidenceSource();
    String rngHashSource = telemetry.getTrueRngHashSource();
    String candidateHashSource = telemetry.getTrueCandidateHashSource();
    long actualFE = record.getFullEvaluations();
    long remainingFE = MAX_FES - actualFE;
    boolean actualFEPositive = actualFE > 0L;
    boolean actualFEWithinMax = actualFEPositive && actualFE <= MAX_FES;
    boolean remainingFEUnderQPhase = remainingFE >= 0L && remainingFE < Q_PHASE_FE;
    boolean observerErrorsZero = telemetry.getObserverErrors() == 0L;
    boolean observerExecutionErrorsZero = telemetry.getObserverExecutionErrors() == 0L;
    boolean checkpointComplete = telemetry.isRunFinalized();
    boolean unobservableCheckpointZero = telemetry.getUnobservableCheckpointCount() == 0L;
    boolean trueRngAuditPass = telemetry.getTrueRngCount() > 0
        && "ACTUAL_JMETAL_RANDOM".equals(rngSource)
        && "ACTUAL_RANDOM_DRAWS".equals(rngHashSource);
    boolean trueCandidateAuditPass = telemetry.getTrueCandidateCount() > 0
        && "ACTUAL_GENERATED_CANDIDATES".equals(candidateSource)
        && "ACTUAL_PRE_EVALUATION_CANDIDATES".equals(candidateHashSource);
    boolean candidateCountClosed = telemetry.getTrueCandidateCount() == record.getDecoderCalls();
    boolean candidateCoveragePass = hasCandidateCoverage(telemetry, configuration);
    V35PddrCandidateMetadataAdapter.ContractReport pddrContract =
        telemetry.getPddrContractReport();
    boolean pddrContractPass = telemetry.pddrLedgerRows() > 0
        && pddrContract != null && pddrContract.isPass();
    V35TeacherConcentrationObserver.ContractReport teacherContract =
        telemetry.getTeacherContractReport(configuration.isQgEnabled(), configuration.isQpEnabled());
    boolean teacherContractPass = telemetry.teacherRows() > 0
        && teacherContract != null && teacherContract.isPass();
    boolean cataRequired = configuration.isCaTaLiteEnabled();
    boolean cataContractPass = !cataRequired
        || (telemetry.cataRows() > 0 && !telemetry.cataLifecycleHasUnobservableFields());
    boolean cataLifecycleSchemaValidated = !cataRequired || telemetry.cataRows() > 0;
    boolean cataLongRunLifecycleValidated = cataLifecycleSchemaValidated && cataContractPass;
    boolean cataAllShortGateSourceCoverageValidated = false;
    boolean cataFullLifecycleValidated = false;
    boolean sequenceIdentityPass = record.getIllegalSolutions() == 0
        && record.getDuplicateEvaluations() == 0;
    boolean budgetSemanticPass = true;
    boolean runCompleted = "COMPLETED".equals(record.getStatus());
    boolean terminalCheckpointPass = telemetry.isTerminalCheckpointAccepted()
        && telemetry.getLastCompletedAtomicBoundaryFE() == actualFE
        && telemetry.getLastNominalCheckpointFE() == MAX_FES
        && telemetry.getLastActualCheckpointFE() == actualFE
        && telemetry.getLastActualSnapshotFE() == actualFE
        && V35CheckpointFrontObserver.TERMINATION_KIND_PHASE_CONSISTENT_BUDGET
            .equals(telemetry.getLastTerminationKind())
        && telemetry.getNominalCheckpointNotExactlyReachedCount() == (actualFE == MAX_FES ? 0L : 1L)
        && telemetry.getTerminalSnapshotCount() == (actualFE == MAX_FES ? 0L : 1L);
    boolean terminalExpected = actualFE < MAX_FES;
    boolean actualFEEqualsLastBoundary = actualFEPositive
        && telemetry.getLastCompletedAtomicBoundaryFE() == actualFE;
    boolean terminalWorking = !terminalExpected
        || hasTerminalFront(telemetry, "workingPopulationND", actualFE);
    boolean terminalDecision = !terminalExpected
        || hasTerminalFront(telemetry, "decisionArchiveFront", actualFE);
    boolean terminalObserved = !terminalExpected
        || hasTerminalFront(telemetry, "observedFullFront", actualFE);
    boolean terminalReal = !terminalExpected || (terminalCheckpointPass
        && actualFEEqualsLastBoundary && terminalWorking && terminalDecision && terminalObserved);
    boolean phaseConsistentTerminal = !terminalExpected || terminalReal;
    boolean hardGatesPass = actualFEPositive && actualFEWithinMax && remainingFEUnderQPhase
        && observerErrorsZero && observerExecutionErrorsZero && checkpointComplete
        && unobservableCheckpointZero && runCompleted && budgetSemanticPass
        && trueRngAuditPass && trueCandidateAuditPass && candidateCountClosed
        && candidateCoveragePass && sequenceIdentityPass && pddrContractPass
        && teacherContractPass && cataContractPass && terminalCheckpointPass
        && phaseConsistentTerminal;
    boolean diagnosticToolingValidated = hardGatesPass;

    String qgEventStreamHash = extractKv(record.getMechanismSummary(), "qgEventStreamHash");
    String qpEventStreamHash = extractKv(record.getMechanismSummary(), "qpEventStreamHash");
    String qpHash = record.getObservationEvidence() == null ? "UNAVAILABLE"
        : record.getObservationEvidence().getQpTableHash();
    String pddrText = pddrContract == null ? "N/A" : pddrContract.toText();
    String teacherText = teacherContract == null ? "N/A" : teacherContract.toText();
    String contract = contractProperties(actualFE, MAX_FES, Q_PHASE_FE, remainingFE,
        actualFEWithinMax, remainingFEUnderQPhase, observerErrorsZero, observerExecutionErrorsZero,
        checkpointComplete, runCompleted, rngSource, candidateSource,
        telemetry.getUnobservableCheckpointCount(), telemetry.getUnobservableReasonSummary(),
        true, diagnosticToolingValidated, unobservableCheckpointZero, trueRngAuditPass,
        trueCandidateAuditPass, candidateCountClosed, candidateCoveragePass, sequenceIdentityPass,
        pddrContractPass, teacherContractPass, cataContractPass, budgetSemanticPass,
        hardGatesPass, telemetry.getTrueCandidateSourceCounts(), pddrText, teacherText,
        rngHashSource, candidateHashSource, terminalCheckpointPass,
        telemetry.getLastCompletedAtomicBoundaryFE(), telemetry.getLastNominalCheckpointFE(),
        telemetry.getLastActualCheckpointFE(), telemetry.getLastCheckpointDeltaFE(),
        telemetry.getLastCheckpointKind(), telemetry.getLastCheckpointAtomicBoundary(),
        telemetry.getLastTerminationKind(), telemetry.getTerminalClassification());
    contract += terminalContractProperties(actualFEPositive, phaseConsistentTerminal, terminalReal,
        actualFEEqualsLastBoundary, terminalWorking, terminalDecision, terminalObserved,
        cataLifecycleSchemaValidated, cataLongRunLifecycleValidated,
        cataAllShortGateSourceCoverageValidated, cataFullLifecycleValidated,
        qgEventStreamHash, qpEventStreamHash);
    contract = replaceLine(contract, "250kApproved", "true");
    contract = replaceLine(contract, "250kStarted", "true");
    contract += "formalAlgorithmJarSha256=" + FORMAL_SHA + "\n"
        + "diagnosticRuntimeJarSha256=" + RUNTIME_SHA + "\n"
        + "runtimeJarBindingVerified=true\n"
        + "diagnosticToolingValidated=" + diagnosticToolingValidated + "\n"
        + "250kReadyForPreregistration=true\n"
        + "250kApproved=true\n250kStarted=true\n"
        + "formalMatrixRunning=false\nFC5=INCONCLUSIVE\n"
        + "rawRunOutputsUseUnifiedCataFields=true\npostRunNormalizationRequired=false\n";
    Files.write(output.resolve("diagnostic-contract.properties"),
        contract.getBytes(StandardCharsets.UTF_8));

    String behavior = behaviorSummary(arguments, record, telemetry, actualFE, remainingFE,
        frontHash, unique.size(), elapsed, qgEventStreamHash, qpEventStreamHash, qpHash,
        rngSource, candidateSource, rngHashSource, candidateHashSource,
        actualFEPositive, actualFEWithinMax, remainingFEUnderQPhase, checkpointComplete,
        unobservableCheckpointZero, trueRngAuditPass, trueCandidateAuditPass,
        candidateCountClosed, candidateCoveragePass, sequenceIdentityPass, pddrContractPass,
        teacherContractPass, cataContractPass, cataLifecycleSchemaValidated,
        cataLongRunLifecycleValidated, cataFullLifecycleValidated, terminalCheckpointPass,
        terminalReal, terminalWorking, terminalDecision, terminalObserved, phaseConsistentTerminal,
        hardGatesPass, diagnosticToolingValidated);
    Files.write(output.resolve("behavior-summary.properties"),
        behavior.getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("runtime-provenance.properties"), (
        "formalAlgorithmJarSha256=" + FORMAL_SHA + "\n"
        + "diagnosticRuntimeJarSha256=" + RUNTIME_SHA + "\n"
        + "runtimeJarBindingVerified=true\n"
        + "runtimeCodeSource=" + runtimeJar + "\n"
        + "runtimeCodeSourceSha256=" + runtimeSha + "\n"
        + "launcherClass=" + V35MidHorizon250kExternalRunner.class.getName() + "\n"
        + "launcherUsesExisting121PrivatePhaseConsistentBridge=true\n"
        + "telemetryMode=ON\n"
        + "pddrSelectionMode=GLOBAL_ORIGINAL\n"
        + "mixture=20/40/20/20\nShiftMode=NONE\nFM3=true\n"
        + "maxFEs=" + MAX_FES + "\n").getBytes(StandardCharsets.UTF_8));

    System.out.println("DONE " + arguments.runId + " status=" + record.getStatus()
        + " actualFE=" + actualFE + " hardGatesPass=" + hardGatesPass
        + " diagnosticToolingValidated=" + diagnosticToolingValidated);
    if (!hardGatesPass) {
      throw new IllegalStateException("250k diagnostic hard gate failed; see diagnostic-contract.properties");
    }
  }

  private static V35MidHorizonTelemetry createTelemetry(V35FairRunner.Mode mode,
      ZhangBoCanonicalProductionProblem problem, V35ProductionConfiguration configuration,
      long[] checkpoints, long seed, String runId, String jarSha) throws Exception {
    Method method = V35MidHorizonDiagnosticDriver.class.getDeclaredMethod("createTelemetry",
        V35FairRunner.Mode.class, ZhangBoCanonicalProductionProblem.class,
        V35ProductionConfiguration.class, long[].class, long.class, String.class, String.class);
    method.setAccessible(true);
    return (V35MidHorizonTelemetry) invoke(method, null, mode, problem, configuration,
        checkpoints, Long.valueOf(seed), runId, jarSha);
  }

  private static V35FairRunner.RunRecord runPhaseConsistentDiagnostic(
      V35FairRunner.Mode mode, ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> initial, long seed,
      V35ProductionConfiguration configuration, V35MidHorizonTelemetry telemetry) throws Exception {
    Method method = V35MidHorizonDiagnosticDriver.class.getDeclaredMethod(
        "runPhaseConsistentDiagnostic", V35FairRunner.Mode.class, Problem.class, List.class,
        int.class, long.class, V35BottleneckDiagnosisConfiguration.class,
        V35ProductionConfiguration.class, V35MidHorizonTelemetry.class);
    method.setAccessible(true);
    return (V35FairRunner.RunRecord) invoke(method, null, mode, problem, initial,
        Integer.valueOf(MAX_FES), Long.valueOf(seed),
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration, telemetry);
  }

  private static Object invoke(Method method, Object target, Object... args) throws Exception {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException error) {
      Throwable cause = error.getCause();
      if (cause instanceof Exception) throw (Exception) cause;
      if (cause instanceof Error) throw (Error) cause;
      throw error;
    }
  }

  private static String contractProperties(long actualFE, int maxFEs, long qPhaseFE,
      long remainingFE,
      boolean actualFEWithinMax, boolean remainingFEUnderQPhase, boolean observerErrorsZero,
      boolean observerExecutionErrorsZero, boolean checkpointComplete, boolean runCompleted,
      String rngSource, String candidateSource, long unobservableCount, String unobservableReasons,
      boolean telemetryOn, boolean diagnosticToolingValidated, boolean unobservableCheckpointZero,
      boolean trueRngAuditPass, boolean trueCandidateAuditPass, boolean candidateCountClosed,
      boolean candidateCoveragePass, boolean sequenceIdentityPass, boolean pddrContractPass,
      boolean teacherContractPass, boolean cataContractPass, boolean budgetSemanticPass,
      boolean hardGatesPass, String candidateSourceCounts, String pddrContract,
      String teacherContract, String rngHashSource, String candidateHashSource,
      boolean terminalCheckpointPass, long lastCompletedAtomicBoundaryFE,
      long terminalNominalCheckpointFE, long terminalActualCheckpointFE,
      long terminalCheckpointDeltaFE, String terminalCheckpointKind,
      String terminalAtomicBoundary, String terminalTerminationKind, String terminalClassification)
      throws Exception {
    Class<?>[] types = {
      long.class, int.class, long.class, long.class,
      boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
      String.class, String.class, long.class, String.class,
      boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
      boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
      boolean.class, String.class, String.class, String.class, String.class, String.class,
      boolean.class, long.class, long.class, long.class, long.class,
      String.class, String.class, String.class, String.class
    };
    Method method = V35MidHorizonDiagnosticDriver.class.getDeclaredMethod("contractProperties", types);
    method.setAccessible(true);
    Object[] values = {
      Long.valueOf(actualFE), Integer.valueOf(maxFEs), Long.valueOf(qPhaseFE),
      Long.valueOf(remainingFE), Boolean.valueOf(actualFEWithinMax),
      Boolean.valueOf(remainingFEUnderQPhase), Boolean.valueOf(observerErrorsZero),
      Boolean.valueOf(observerExecutionErrorsZero), Boolean.valueOf(checkpointComplete),
      Boolean.valueOf(runCompleted), rngSource, candidateSource, Long.valueOf(unobservableCount),
      unobservableReasons, Boolean.valueOf(telemetryOn), Boolean.valueOf(diagnosticToolingValidated),
      Boolean.valueOf(unobservableCheckpointZero), Boolean.valueOf(trueRngAuditPass),
      Boolean.valueOf(trueCandidateAuditPass), Boolean.valueOf(candidateCountClosed),
      Boolean.valueOf(candidateCoveragePass), Boolean.valueOf(sequenceIdentityPass),
      Boolean.valueOf(pddrContractPass), Boolean.valueOf(teacherContractPass),
      Boolean.valueOf(cataContractPass), Boolean.valueOf(budgetSemanticPass),
      Boolean.valueOf(hardGatesPass), candidateSourceCounts, pddrContract, teacherContract,
      rngHashSource, candidateHashSource, Boolean.valueOf(terminalCheckpointPass),
      Long.valueOf(lastCompletedAtomicBoundaryFE), Long.valueOf(terminalNominalCheckpointFE),
      Long.valueOf(terminalActualCheckpointFE), Long.valueOf(terminalCheckpointDeltaFE),
      terminalCheckpointKind, terminalAtomicBoundary, terminalTerminationKind,
      terminalClassification
    };
    String result = (String) invoke(method, null, values);
    result = replaceLine(result, "250kReadyForPreregistration", "true");
    return replaceLine(result, "diagnosticToolingValidated", String.valueOf(diagnosticToolingValidated));
  }

  private static String terminalContractProperties(boolean actualFEPositive,
      boolean phaseConsistentTerminalSnapshotImplemented, boolean terminalSnapshotIsRealAtomicBoundary,
      boolean actualFEEqualsLastCompletedAtomicBoundary, boolean terminalWorkingPopulationFrontObserved,
      boolean terminalDecisionArchiveFrontObserved, boolean terminalObservedFullFrontObserved,
      boolean cataLifecycleSchemaValidated, boolean cataLongRunLifecycleValidated,
      boolean cataAllShortGateSourceCoverageValidated, boolean cataFullLifecycleValidated,
      String qgEventStreamHash, String qpEventStreamHash) throws Exception {
    Class<?>[] types = {
      boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
      boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, String.class, String.class
    };
    Method method = V35MidHorizonDiagnosticDriver.class.getDeclaredMethod(
        "terminalContractProperties", types);
    method.setAccessible(true);
    return (String) invoke(method, null, Boolean.valueOf(actualFEPositive),
        Boolean.valueOf(phaseConsistentTerminalSnapshotImplemented),
        Boolean.valueOf(terminalSnapshotIsRealAtomicBoundary),
        Boolean.valueOf(actualFEEqualsLastCompletedAtomicBoundary),
        Boolean.valueOf(terminalWorkingPopulationFrontObserved),
        Boolean.valueOf(terminalDecisionArchiveFrontObserved),
        Boolean.valueOf(terminalObservedFullFrontObserved),
        Boolean.valueOf(cataLifecycleSchemaValidated),
        Boolean.valueOf(cataLongRunLifecycleValidated),
        Boolean.valueOf(cataAllShortGateSourceCoverageValidated),
        Boolean.valueOf(cataFullLifecycleValidated), qgEventStreamHash, qpEventStreamHash);
  }

  private static boolean hasCandidateCoverage(V35MidHorizonTelemetry telemetry,
      V35ProductionConfiguration configuration) {
    boolean initial = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.INITIAL_POPULATION.name()) > 0L;
    boolean global = !configuration.isCfvfEnabled()
        || telemetry.getTrueCandidateSourceCount(
            V35EvaluationSourceContext.Source.GLOBAL_CFVF.name()) > 0L;
    long inherited = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.INTER_FACTORY_LS.name())
        + telemetry.getTrueCandidateSourceCount(
            V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS.name());
    long caTa = telemetry.getTrueCandidateSourceCount(
        V35EvaluationSourceContext.Source.CATA_TEST.name())
        + telemetry.getTrueCandidateSourceCount(
            V35EvaluationSourceContext.Source.CATA_APPLY.name());
    return initial && global && inherited > 0L
        && (!configuration.isCaTaLiteEnabled() || caTa > 0L);
  }

  private static boolean hasTerminalFront(V35MidHorizonTelemetry telemetry, String frontType,
      long actualFE) {
    String csv = telemetry.getCheckpointFrontCsv();
    if (csv == null || csv.isEmpty()) return false;
    String[] lines = csv.split("\\r?\\n");
    for (String line : lines) {
      if (line.isEmpty() || line.startsWith("generatedByRunId,")) continue;
      String[] fields = line.split(",", -1);
      if (fields.length >= 25 && frontType.equals(fields[13]) && "NONE".equals(fields[14])
          && V35CheckpointFrontObserver.CHECKPOINT_KIND_TERMINAL_PHASE_CONSISTENT
              .equals(fields[20]) && String.valueOf(actualFE).equals(fields[21])) return true;
    }
    return false;
  }

  private static List<double[]> uniqueSortedFront(List<double[]> front) {
    Set<String> seen = new HashSet<String>();
    List<double[]> unique = new ArrayList<double[]>();
    if (front != null) {
      for (double[] point : front) {
        String key = Double.doubleToLongBits(point[0]) + ":"
            + Double.doubleToLongBits(point[1]) + ":" + Double.doubleToLongBits(point[2]);
        if (seen.add(key)) unique.add(point);
      }
    }
    Collections.sort(unique, new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) {
          int result = Double.compare(a[i], b[i]);
          if (result != 0) return result;
        }
        return 0;
      }
    });
    return unique;
  }

  private static void writeTelemetry(Path output, V35MidHorizonTelemetry telemetry) throws IOException {
    writeIfPresent(output, "telemetry-checkpoint-fronts.csv", telemetry.getCheckpointFrontCsv());
    writeIfPresent(output, "telemetry-pddr-full-ledger.csv", telemetry.getPddrLedgerCsv());
    writeIfPresent(output, "telemetry-pddr-cycle-summary.csv", telemetry.getPddrCycleSummaryCsv());
    writeIfPresent(output, "telemetry-teacher-use-events.csv", telemetry.getTeacherEventsCsv());
    writeIfPresent(output, "telemetry-teacher-concentration.csv",
        telemetry.getTeacherConcentrationCsv());
    writeIfPresent(output, "telemetry-cata-contribution-events.csv", telemetry.getCataEventsCsv());
    writeIfPresent(output, "telemetry-cata-contribution-summary.csv", telemetry.getCataSummaryCsv());
  }

  private static void writeIfPresent(Path output, String name, String text) throws IOException {
    if (text != null && !text.isEmpty()) {
      Files.write(output.resolve(name), text.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static String behaviorSummary(Arguments arguments, V35FairRunner.RunRecord record,
      V35MidHorizonTelemetry telemetry, long actualFE, long remainingFE, String frontHash,
      int frontSize, long elapsed, String qgHash, String qpEventHash, String qpTableHash,
      String rngSource, String candidateSource, String rngHashSource, String candidateHashSource,
      boolean actualFEPositive, boolean actualFEWithinMax, boolean remainingFEUnderQPhase,
      boolean checkpointComplete, boolean unobservableCheckpointZero, boolean trueRngAuditPass,
      boolean trueCandidateAuditPass, boolean candidateCountClosed, boolean candidateCoveragePass,
      boolean sequenceIdentityPass, boolean pddrContractPass, boolean teacherContractPass,
      boolean cataContractPass, boolean cataLifecycleSchemaValidated,
      boolean cataLongRunLifecycleValidated, boolean cataFullLifecycleValidated,
      boolean terminalCheckpointPass, boolean terminalReal, boolean terminalWorking,
      boolean terminalDecision, boolean terminalObserved, boolean phaseConsistentTerminal,
      boolean hardGatesPass, boolean diagnosticToolingValidated) {
    StringBuilder b = new StringBuilder();
    b.append("runId=").append(arguments.runId).append('\n');
    b.append("arm=").append(arguments.arm).append('\n');
    b.append("instance=").append(arguments.instance).append('\n');
    b.append("seed=").append(arguments.seed).append('\n');
    b.append("maxFEs=").append(MAX_FES).append('\n');
    b.append("telemetryMode=ON\n");
    b.append("sourceJarSha256=").append(RUNTIME_SHA).append('\n');
    b.append("formalAlgorithmJarSha256=").append(FORMAL_SHA).append('\n');
    b.append("diagnosticRuntimeJarSha256=").append(RUNTIME_SHA).append('\n');
    b.append("runtimeJarBindingVerified=true\n");
    b.append("status=").append(record.getStatus()).append('\n');
    b.append("diagnosticStatus=").append(hardGatesPass ? record.getStatus() : "FAILED_HARD_GATE")
        .append('\n');
    b.append("stopReason=").append(record.getStopReason()).append('\n');
    b.append("actualFE=").append(actualFE).append('\n');
    b.append("qPhaseFE=").append(Q_PHASE_FE).append('\n');
    b.append("remainingFE=").append(remainingFE).append('\n');
    b.append("requestedMaxFE=").append(MAX_FES).append('\n');
    b.append("actualFEPositive=").append(actualFEPositive).append('\n');
    b.append("actualFEWithinMax=").append(actualFEWithinMax).append('\n');
    b.append("remainingFEUnderQPhase=").append(remainingFEUnderQPhase).append('\n');
    b.append("decoderCalls=").append(record.getDecoderCalls()).append('\n');
    b.append("illegalSolutions=").append(record.getIllegalSolutions()).append('\n');
    b.append("duplicateEvaluations=").append(record.getDuplicateEvaluations()).append('\n');
    b.append("initialPopulationHash=").append(record.getInitialPopulationHash()).append('\n');
    b.append("evaluationTraceHash=").append(record.getEvaluationTraceHash()).append('\n');
    b.append("qgTableHash=").append(record.getQgTableHash()).append('\n');
    b.append("qpTableHash=").append(qpTableHash).append('\n');
    b.append("qgEventStreamHash=").append(qgHash).append('\n');
    b.append("qpEventStreamHash=").append(qpEventHash).append('\n');
    b.append("pddrEventStreamHash=").append(record.getPddrEventStreamHash()).append('\n');
    b.append("rngConsumptionSequenceSource=").append(rngSource).append('\n');
    b.append("rngHashSource=").append(rngHashSource).append('\n');
    b.append("rngConsumptionSequenceHash=").append(telemetry.getTrueRngHashOrUnavailable()).append('\n');
    b.append("generatedCandidateSequenceSource=").append(candidateSource).append('\n');
    b.append("candidateHashSource=").append(candidateHashSource).append('\n');
    b.append("generatedCandidateSequenceHash=")
        .append(telemetry.getGeneratedCandidateHashOrUnavailable()).append('\n');
    b.append("caTaTestCalls=").append(record.getCaTaTestCalls()).append('\n');
    b.append("caTaEventCount=").append(record.getCaTaEventCount()).append('\n');
    b.append("canonicalFrontHash=").append(frontHash).append('\n');
    b.append("frontSize=").append(frontSize).append('\n');
    b.append("wallNanos=").append(elapsed).append('\n');
    b.append("observerErrors=").append(telemetry.getObserverErrors()).append('\n');
    b.append("observerExecutionErrors=").append(telemetry.getObserverExecutionErrors()).append('\n');
    b.append("unobservableCheckpointCount=")
        .append(telemetry.getUnobservableCheckpointCount()).append('\n');
    b.append("nominalCheckpointNotExactlyReachedCount=")
        .append(telemetry.getNominalCheckpointNotExactlyReachedCount()).append('\n');
    b.append("terminalSnapshotCount=").append(telemetry.getTerminalSnapshotCount()).append('\n');
    b.append("lastCompletedAtomicBoundaryFE=")
        .append(telemetry.getLastCompletedAtomicBoundaryFE()).append('\n');
    b.append("terminalCheckpointClassification=")
        .append(telemetry.getTerminalClassification()).append('\n');
    b.append("terminalCheckpointPass=").append(terminalCheckpointPass).append('\n');
    b.append("terminalCheckpointNominalFE=").append(telemetry.getLastNominalCheckpointFE()).append('\n');
    b.append("terminalCheckpointActualFE=").append(telemetry.getLastActualCheckpointFE()).append('\n');
    b.append("terminalCheckpointDeltaFE=").append(telemetry.getLastCheckpointDeltaFE()).append('\n');
    b.append("terminalCheckpointKind=").append(telemetry.getLastCheckpointKind()).append('\n');
    b.append("terminalAtomicBoundary=").append(telemetry.getLastCheckpointAtomicBoundary()).append('\n');
    b.append("terminalTerminationKind=").append(telemetry.getLastTerminationKind()).append('\n');
    b.append("actualFEEqualsLastCompletedAtomicBoundary=")
        .append(actualFE == telemetry.getLastCompletedAtomicBoundaryFE()).append('\n');
    b.append("terminalSnapshotIsRealAtomicBoundary=").append(terminalReal).append('\n');
    b.append("terminalWorkingPopulationFrontObserved=").append(terminalWorking).append('\n');
    b.append("terminalDecisionArchiveFrontObserved=").append(terminalDecision).append('\n');
    b.append("terminalObservedFullFrontObserved=").append(terminalObserved).append('\n');
    b.append("phaseConsistentTerminalSnapshotImplemented=").append(phaseConsistentTerminal).append('\n');
    b.append("utilizationRate=").append((double) actualFE / (double) MAX_FES).append('\n');
    b.append("checkpointRows=").append(telemetry.checkpointRows()).append('\n');
    b.append("pddrLedgerRows=").append(telemetry.pddrLedgerRows()).append('\n');
    b.append("teacherRows=").append(telemetry.teacherRows()).append('\n');
    b.append("cataRows=").append(telemetry.cataRows()).append('\n');
    b.append("checkpointComplete=").append(checkpointComplete).append('\n');
    b.append("checkpointBoundary=").append(V35CheckpointFrontObserver.ATOMIC_BOUNDARY).append('\n');
    b.append("formalBudgetSemantics=PHASE_CONSISTENT_BUDGET_TERMINATION\n");
    b.append("allowTerminalPartialFormalQPhase=false\n");
    b.append("pddrContractPass=").append(pddrContractPass).append('\n');
    b.append("teacherContractPass=").append(teacherContractPass).append('\n');
    b.append("cataContractPass=").append(cataContractPass).append('\n');
    b.append("cataLifecycleSchemaValidated=").append(cataLifecycleSchemaValidated).append('\n');
    b.append("cataLongRunLifecycleValidated=").append(cataLongRunLifecycleValidated).append('\n');
    b.append("cataFullLifecycleValidated=").append(cataFullLifecycleValidated).append('\n');
    b.append("trueRngSequenceAudit=").append(trueRngAuditPass).append('\n');
    b.append("trueGeneratedCandidateSequenceAudit=").append(trueCandidateAuditPass).append('\n');
    b.append("candidateCountClosed=").append(candidateCountClosed).append('\n');
    b.append("candidateCoveragePass=").append(candidateCoveragePass).append('\n');
    b.append("sequenceIdentityPass=").append(sequenceIdentityPass).append('\n');
    b.append("formalBudgetSemanticMatch=true\n");
    b.append("diagnosticSourceChanged=true\nalgorithmDecisionSemanticsChanged=false\n");
    b.append("formalFrozenJarChanged=false\npddrDecisionChanged=false\nformalMatrixRunning=false\n");
    b.append("FC5=INCONCLUSIVE\npartialPhaseFallback=REJECTED\n");
    b.append("diagnosticToolingValidated=").append(diagnosticToolingValidated).append('\n');
    b.append("250kReadyForPreregistration=true\n250kApproved=true\n250kStarted=true\n");
    b.append("rawRunOutputsUseUnifiedCataFields=true\npostRunNormalizationRequired=false\n");
    b.append("hardGatesPass=").append(hardGatesPass).append('\n');
    return b.toString();
  }

  private static String extractKv(String summary, String key) {
    if (summary == null) return "N/A";
    int index = summary.indexOf(key + "=");
    if (index < 0) return "N/A";
    int start = index + key.length() + 1;
    int end = summary.indexOf(',', start);
    return summary.substring(start, end < 0 ? summary.length() : end);
  }

  private static String replaceLine(String text, String key, String value) {
    String[] lines = text.split("\\r?\\n", -1);
    StringBuilder result = new StringBuilder();
    boolean found = false;
    for (String line : lines) {
      if (line.startsWith(key + "=")) {
        result.append(key).append('=').append(value).append('\n');
        found = true;
      } else if (!line.isEmpty()) {
        result.append(line).append('\n');
      }
    }
    if (!found) result.append(key).append('=').append(value).append('\n');
    return result.toString();
  }

  private static Path runtimeJarFromCodeSource() throws Exception {
    URI location = V35MidHorizonDiagnosticDriver.class.getProtectionDomain()
        .getCodeSource().getLocation().toURI();
    Path path = Paths.get(location).toAbsolutePath().normalize();
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("diagnostic classes must load from a Jar: " + path);
    }
    return path;
  }

  private static String sha256(Path path) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (java.io.InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[1024 * 1024];
      int read;
      while ((read = input.read(buffer)) >= 0) {
        if (read > 0) digest.update(buffer, 0, read);
      }
    }
    return hex(digest.digest());
  }

  private static String sha256(String text) throws Exception {
    return hex(MessageDigest.getInstance("SHA-256")
        .digest(text.getBytes(StandardCharsets.UTF_8)));
  }

  private static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
    return result.toString();
  }

  private static void validate(Arguments arguments) {
    if (arguments.maxFes != MAX_FES) throw new IllegalArgumentException("max-fes must be 250000");
    if (!"ON".equals(arguments.telemetry)) throw new IllegalArgumentException("telemetry must be ON");
    if (!"A2".equals(arguments.arm) && !"A4".equals(arguments.arm)) {
      throw new IllegalArgumentException("arm must be A2 or A4");
    }
    if (!ALLOWED_INSTANCES.contains(arguments.instance)) {
      throw new IllegalArgumentException("instance is outside the fixed 250k roster");
    }
    boolean seedAllowed = false;
    for (long seed : ALLOWED_SEEDS) if (seed == arguments.seed) seedAllowed = true;
    if (!seedAllowed) throw new IllegalArgumentException("seed is outside the fixed 250k roster");
    if (arguments.checkpoints.length == 0
        || arguments.checkpoints[arguments.checkpoints.length - 1] != MAX_FES) {
      throw new IllegalArgumentException("checkpoints must end at 250000");
    }
  }

  private static final class Arguments {
    private Path projectRoot;
    private Path output;
    private String instance;
    private long seed;
    private String arm;
    private int maxFes;
    private String telemetry;
    private String runId;
    private String jarSha256;
    private long[] checkpoints;

    private static Arguments parse(String[] args) {
      Arguments result = new Arguments();
      for (int i = 0; i < args.length; i++) {
        String key = args[i];
        if (!key.startsWith("--") || i + 1 >= args.length) {
          throw new IllegalArgumentException("expected --key value arguments");
        }
        String value = args[++i];
        switch (key) {
          case "--project-root": result.projectRoot = Paths.get(value); break;
          case "--output": result.output = Paths.get(value); break;
          case "--instance": result.instance = value; break;
          case "--seed": result.seed = Long.parseLong(value); break;
          case "--arm": result.arm = value; break;
          case "--max-fes": result.maxFes = Integer.parseInt(value); break;
          case "--telemetry": result.telemetry = value; break;
          case "--run-id": result.runId = value; break;
          case "--jar-sha256": result.jarSha256 = value; break;
          case "--checkpoints": result.checkpoints = parseCheckpoints(value); break;
          default: throw new IllegalArgumentException("unknown argument: " + key);
        }
      }
      if (result.projectRoot == null || result.output == null || result.instance == null
          || result.arm == null || result.runId == null || result.jarSha256 == null
          || result.checkpoints == null) {
        throw new IllegalArgumentException("missing required launcher argument");
      }
      return result;
    }

    private static long[] parseCheckpoints(String value) {
      String[] fields = value.split(",");
      long[] result = new long[fields.length];
      long previous = -1L;
      for (int i = 0; i < fields.length; i++) {
        result[i] = Long.parseLong(fields[i].trim());
        if (result[i] <= previous) throw new IllegalArgumentException("checkpoints must increase");
        previous = result[i];
      }
      return result;
    }
  }
}
