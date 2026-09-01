package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CheckpointObserverHook;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CheckpointObserverHook.CheckpointRecord;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFePacingRepairProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35EvaluationSourceContext;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-SOURCE-DIAGNOSTICS-V3 snapshot-bound experimental runner
 * (source-contribution diagnostics package).  Derived from the validated V2
 * checkpoint runner with one extension: {@code --telemetry ON} additionally
 * arms {@link V35SourceLedgerHook}, producing an evaluation-ordered
 * per-candidate source ledger and a per-outer-cycle merge-pool/PDDR ledger.
 * OFF mode replays the V2/V1 runner semantics byte-for-byte.
 *
 * <p>CLI: {@code --instance <name> --seed <long> --profile REF_A4|C0|C1|C2|C3
 * --max-fes <int> --snapshot <path> --output <path>
 * [--telemetry OFF|ON] [--checkpoints <csv of longs>]}.  Scientific parameters
 * are frozen inside {@link V35LocalFePacingRepairProfile}; no CLI override
 * exists for them.</p>
 *
 * <p>The runner never creates an initial solution and never modifies the
 * frozen formal jar or any algorithm behavior; C0--C3 differ only in the
 * local-FE budget betaMax.  All observers copy objective values, fingerprints
 * and source labels only; they cannot enter the search archive, change PDDR
 * inputs or teacher selection, consume randomness, add evaluations, or mutate
 * candidates.</p>
 */
public final class V35ObserverGateRunner {
  public static final String VERSION = "v35-source-attribution-observer-runner-v5";
  public static final String BUDGET_VERSION = "v35-phase-consistent-budget-v1";
  private static final int POPULATION = 100;
  private static final int Q_TIMES = 50;
  private static final long[] DEFAULT_CHECKPOINTS = {50000L, 100000L, 150000L, 200000L};
  private static final Pattern NUMBER = Pattern.compile("(?:^|[,|])%s=(-?\\d+)(?:$|[,|])");

  private V35ObserverGateRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    execute(value);
  }

  static void execute(Arguments value) throws Exception {
    V35LocalFePacingRepairProfile.Label label =
        V35LocalFePacingRepairProfile.fromCli(value.profile);
    Path instance = requireRegular(Paths.get("inputs/java-jmetal58/EADHFSP",
        value.instance + ".txt"));
    Path setup = requireRegular(Paths.get("inputs/java-jmetal58/instance-extensions/v1",
        value.instance + ".setup.txt"));
    Path fatigue = requireRegular(Paths.get("inputs/java-jmetal58/fatigue-parameters/v1",
        value.instance + ".fatigue.txt"));
    Path binding = requireRegular(Paths.get("bindings", value.instance
        + ".binding.properties"));
    Path formalJar = requireRegular(Paths.get("jars",
        "formal-algorithm-8DAD8F40.jar"));
    Path experimentalJar = requireRegular(Paths.get("jars",
        "jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V5.jar"));
    Path snapshot = requireRegular(value.snapshot.toAbsolutePath().normalize());

    Properties bound = load(binding);
    String formalSha = sha256(formalJar);
    String experimentalSha = sha256(experimentalJar);
    requireEquals("binding.formalJarSha256", required(bound, "formalJarSha256"), formalSha);
    requireEquals("binding.experimentalJarSha256", required(bound, "experimentalJarSha256"),
        experimentalSha);
    requireEquals("binding.instanceSha256", required(bound, "instanceSha256"), sha256(instance));
    requireEquals("binding.setupFileSha256", required(bound, "setupFileSha256"), sha256(setup));
    requireEquals("binding.fatigueFileSha256", required(bound, "fatigueFileSha256"),
        sha256(fatigue));
    requireEquals("binding.snapshotSha256", required(bound, "snapshotSha256"), sha256(snapshot));
    String runId = "SAOBS-" + value.maxFes + "-" + label.cliAlias() + "-" + value.instance
        + "-" + value.seed;

    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, value.seed, setup.getParent(), fatigue.getParent(),
        ZhangBoShiftConfiguration.none());
    requireEquals("binding.runtimeInstanceSha256", required(bound, "instanceSha256"),
        problem.getInstance().getInstanceSha256());
    requireEquals("binding.runtimeSetupConfigurationSha256",
        required(bound, "setupConfigurationSha256"),
        problem.getInstance().getInstanceExtensionSha256());
    requireEquals("binding.runtimeFatigueConfigurationSha256",
        required(bound, "fatigueConfigurationSha256"),
        problem.getParameters().getConfigurationSha256());

    List<PermutationSolution<Integer>> initial =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, problem);
    if (initial.size() != POPULATION) {
      throw new IllegalStateException("snapshot population mismatch=" + initial.size());
    }
    String v35Hash = V35FairRunner.initialHash(initial);
    String p8Hash = P8InitialPopulationProvider.sha256(initial);
    requireEquals("binding.initialPopulationHashV35", required(bound, "initialPopulationHashV35"),
        v35Hash);
    requireEquals("binding.initialPopulationHashP8", required(bound, "initialPopulationHashP8"),
        p8Hash);
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("snapshot load consumed FE");
    }

    V35ProductionConfiguration configuration =
        V35LocalFePacingRepairProfile.configurationFor(
            label, value.seed, POPULATION, value.maxFes);
    V35LocalFePacingRepairProfile.validate(label, configuration);
    if (Double.compare(configuration.getLocalFeBudget().getBetaMax(), label.betaMax()) != 0
        || Double.compare(configuration.getLocalFeBudget().getBetaMin(),
            V35LocalFePacingRepairProfile.BETA_MIN) != 0) {
      throw new IllegalStateException("runtime beta read-back mismatch for " + label.name());
    }
    String profileText = V35LocalFePacingRepairProfile.canonicalText(label, value.seed,
        POPULATION, value.maxFes, formalSha, experimentalSha);
    String profileHash = sha256(profileText);

    Path target = value.output.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    Path partial = target.resolveSibling(".partial-" + target.getFileName() + "-"
        + System.nanoTime());
    Files.createDirectory(partial);
    boolean moved = false;
    boolean observerArmed = value.telemetryOn;
    List<CheckpointRecord> checkpoints = new ArrayList<CheckpointRecord>();
    String terminalDecision = "";
    String terminalObserved = "";
    long terminalObservedFE = -1L;
    long terminalCounterFE = -1L;
    long observerErrors = 0L;
    String observerLastError = "";
    String evaluationLedger = "";
    String pddrLedger = "";
    long pddrRounds = 0L;
    long ledgerErrors = 0L;
    String ledgerLastError = "";
    long unsetSourceRows = 0L;
    long ledgerRows = 0L;
    long lifecycleRows = 0L;
    Map<Long, Map<String, String>> observerFronts =
        new TreeMap<Long, Map<String, String>>();
    RunnerMemorySampler sampler = new RunnerMemorySampler();
    try {
      sampler.start();
      long wallStart = System.nanoTime();
      if (observerArmed) {
        V35CheckpointObserverHook.arm(value.checkpoints);
        V35SourceAttributionObserver.arm(runId, value.instance,
            String.valueOf(value.seed), label.cliAlias());
        V35SourceAttributionObserver.attach(problem);
        V35SourceAttributionObserver.openLedgerFiles(
            value.output.toAbsolutePath().normalize().getParent().toFile());
        // Frozen context defaults to disabled; enabling it only affects what
        // current() returns to pure observers (P25E diagnostic precedent).
        V35EvaluationSourceContext.setEnabled(true);
      }
      V35FairRunner.RunRecord record = V35FairRunner.run(
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner.Mode.V35_FULL_POOL_OFF,
          problem, P8InitialPopulationProvider.copy(initial), value.maxFes, value.seed, true,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
      if (observerArmed) {
        checkpoints = V35CheckpointObserverHook.getRecords();
        terminalDecision = V35CheckpointObserverHook.getTerminalDecisionCsv();
        terminalObserved = V35CheckpointObserverHook.getTerminalObservedCsv();
        terminalObservedFE = V35CheckpointObserverHook.getTerminalObservedFE();
        terminalCounterFE = V35CheckpointObserverHook.getTerminalCounterFE();
        observerErrors = V35CheckpointObserverHook.getErrorCount();
        observerLastError = V35CheckpointObserverHook.getLastError();
        evaluationLedger = V35SourceAttributionObserver.getEvaluationLedgerCsv();
        pddrLedger = V35SourceAttributionObserver.getPddrLedgerCsv();
        pddrRounds = V35SourceAttributionObserver.getPddrRounds();
        ledgerErrors = V35SourceAttributionObserver.getErrorCount();
        ledgerLastError = V35SourceAttributionObserver.getLastError();
        unsetSourceRows = V35SourceAttributionObserver.getUnknownSourceEvents();
        lifecycleRows = V35SourceAttributionObserver.getLifecycleRows();
        observerFronts.putAll(V35SourceAttributionObserver.getCheckpointFronts());
      }
      V35SourceAttributionObserver.closeLedgerWriters();
      copyLedgerFiles(partial);
      ledgerRows = countLedgerFileRows(partial);
      long obsDroppedEvents = observerArmed ? V35SourceAttributionObserver.getDroppedEvents() : 0L;
      long obsUnknownSource = observerArmed ? V35SourceAttributionObserver.getUnknownSourceEvents() : 0L;
      long obsInvalidRows = observerArmed ? V35SourceAttributionObserver.getInvalidObjectiveRows() : 0L;
      long obsDupRows = observerArmed ? V35SourceAttributionObserver.getDuplicateCandidateEventRows() : 0L;
      long obsCapViolations = observerArmed ? V35SourceAttributionObserver.getBoundedCapacityViolations() : 0L;
      long obsB0Captured = observerArmed ? V35SourceAttributionObserver.getB0Captured() : 0L;
      int obsCheckpointCount = observerArmed ? V35SourceAttributionObserver.getCheckpointFronts().size() : 0;
      V35CheckpointObserverHook.disarm();
      sampler.stop();
      V35SourceAttributionObserver.disarm();
      V35EvaluationSourceContext.setEnabled(false);
      long wallNanos = System.nanoTime() - wallStart;
      Budget budget = Budget.classify(value.maxFes, record.getFullEvaluations(),
          record.getDecoderCalls());
      List<String> failures = gate(label, record, v35Hash, budget, configuration);
      if (observerArmed) {
        gateObserver(failures, value.checkpoints, checkpoints, terminalDecision,
            terminalObserved, terminalObservedFE, observerErrors, observerLastError);
        gateLedger(failures, ledgerRows, record.getFullEvaluations(), unsetSourceRows,
            ledgerErrors, ledgerLastError, pddrRounds,
            value(record.getMechanismSummary(), "formalOuterCycles"));
        gateCompleteness(failures,
            obsDroppedEvents, obsUnknownSource, obsInvalidRows,
            obsDupRows, obsCapViolations, obsB0Captured, obsCheckpointCount,
            lifecycleRows, partial);
      }
      String canonical = "repairRunnerVersion=" + VERSION + "\n"
          + "repairFamily=LOCAL_FE_PACING\nsingleKnob=betaMax\n"
          + "observerMode=" + (observerArmed ? "ON" : "OFF") + "\n"
          + "observerSchema=" + V35SourceAttributionObserver.SCHEMA_VERSION + "\n"
          + "telemetryLedgerRows=" + ledgerRows + "\n"
          + "telemetryPddrRounds=" + pddrRounds + "\n"
          + "checkpointTargets=" + joinLongs(value.checkpoints) + "\n"
          + "runId=" + runId + "\nprofileLabel=" + label.name()
          + "\nprofileCliAlias=" + label.cliAlias() + "\nseed=" + value.seed
          + "\ninstance=" + value.instance + "\npopulation=" + POPULATION
          + "\nmaxFEs=" + value.maxFes
          + "\nbudgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\nbudgetProtocolVersion="
          + BUDGET_VERSION + "\nformalJarSha256=" + formalSha
          + "\nexperimentalJarSha256=" + experimentalSha
          + "\nsnapshotSha256=" + sha256(snapshot)
          + "\ninitialPopulationHashV35=" + v35Hash + "\ninitialPopulationHashP8=" + p8Hash
          + "\nprofileSha256=" + profileHash + "\nprofileCanonicalBegin\n" + profileText
          + "profileCanonicalEnd\n";
      V35FairRunner.writeRecord(record, partial, canonical);
      Files.write(partial.resolve("profile.txt"), profileText.getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("profile.sha256"),
          (profileHash + "  profile.txt\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population.sha256"),
          (v35Hash + "  V35\n" + p8Hash + "  P8\n").getBytes(StandardCharsets.UTF_8));
      writeBudget(partial, record, budget, formalSha, experimentalSha);
      writeGate(partial, record, wallNanos, failures, runId, label, observerArmed,
          value.checkpoints, checkpoints.size(), observerErrors, ledgerRows,
          pddrRounds, lifecycleRows, ledgerErrors);
      writePddrObservation(partial, record, label, value.instance, value.seed);
      if (observerArmed) {
        writeCheckpoints(partial, checkpoints, terminalDecision, terminalObserved,
            terminalObservedFE, terminalCounterFE, observerFronts);
      }
      writeMemorySummary(partial, sampler, wallNanos);
      writeManifest(partial);
      move(partial, target);
      moved = true;
      if (!failures.isEmpty()) {
        throw new IllegalStateException("RUN_GATE_FAILED " + join(failures));
      }
      System.out.println("V35_OBSERVER_GATE_COMPLETED profile=" + label.cliAlias()
          + " telemetry=" + (observerArmed ? "ON" : "OFF")
          + " FE=" + record.getFullEvaluations() + " output=" + target);
    } catch (Exception error) {
      sampler.stop();
      V35SourceAttributionObserver.disarm();
      V35CheckpointObserverHook.disarm();
      V35EvaluationSourceContext.setEnabled(false);
      if (!moved && Files.exists(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n")
                .getBytes(StandardCharsets.UTF_8));
        writeManifest(partial);
      }
      throw error;
    }
  }

  private static List<String> gate(V35LocalFePacingRepairProfile.Label label,
      V35FairRunner.RunRecord record, String initialHash, Budget budget,
      V35ProductionConfiguration configuration) {
    List<String> failures = new ArrayList<String>();
    if (!"COMPLETED".equals(record.getStatus())) failures.add("status=" + record.getStatus());
    if (!initialHash.equals(record.getInitialPopulationHash())) {
      failures.add("initialPopulationDrift");
    }
    if (!budget.accepted) failures.add("budget=" + budget.failure);
    if (record.getIllegalSolutions() != 0) failures.add("illegal=" + record.getIllegalSolutions());
    if (record.getDuplicateEvaluations() != 0) {
      failures.add("duplicate=" + record.getDuplicateEvaluations());
    }
    if (record.getPassiveObservedCount() != record.getFullEvaluations()) {
      failures.add("sourceObservationLoss");
    }
    if (record.getFront().isEmpty()) failures.add("emptyFront");
    Set<String> points = new HashSet<String>();
    for (double[] point : record.getFront()) {
      if (point.length != 3) failures.add("objectiveDimension");
      for (double v : point) if (!Double.isFinite(v)) failures.add("nonFiniteFront");
      String fingerprint = point.length < 3 ? "invalid" : point[0] + "|" + point[1] + "|" + point[2];
      if (!points.add(fingerprint)) failures.add("duplicateFrontPoint");
    }
    ZhangBoDecoderTimingSnapshot timing = record.getDecoderTiming();
    if (timing.getSuccessfulDecoderCalls() != record.getFullEvaluations()) {
      failures.add("decoderSnapshotMismatch");
    }
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L
        || timing.getLeftFullRecomputations() != 0L || timing.getRightFullRecomputations() != 0L) {
      failures.add("shiftActivity");
    }
    String summary = record.getMechanismSummary();
    zero(failures, summary, "cfvfRepairs");
    zero(failures, summary, "directionalPoolRequests");
    zero(failures, summary, "directionalPoolFiltered");
    zero(failures, summary, "shadowSamples");
    zero(failures, summary, "shadowEvaluations");
    if (budget.requested >= budget.qPhase) {
      positive(failures, summary, "qgSelections");
      positive(failures, summary, "pddrEvents");
      positive(failures, summary, "cfvfOffspring");
      positive(failures, summary, "qpActions");
      positive(failures, summary, "qpTransitions");
      positive(failures, summary, "archiveInsertions");
      positive(failures, summary, "caTaLiteTest");
      positive(failures, summary, "caTaLiteApply");
      positive(failures, summary, "teacherUses");
      positive(failures, summary, "validityChecks");
      zero(failures, summary, "dominatedTeacherUses");
      positive(failures, summary, "formalLocalFE");
    }
    if (configuration.getLocalFeBudget() == null) failures.add("localFeBudgetMissing");
    return failures;
  }

  /** Observer acceptance gate (ON side only; checkpoints). */
  private static void gateObserver(List<String> failures, long[] targets,
      List<CheckpointRecord> checkpoints, String terminalDecision, String terminalObserved,
      long terminalObservedFE, long observerErrors, String observerLastError) {
    if (observerErrors != 0L) {
      failures.add("observerExecutionErrors=" + observerErrors + ":" + observerLastError);
    }
    if (checkpoints.size() != targets.length) {
      failures.add("checkpointRows=" + checkpoints.size() + " expected=" + targets.length);
      return;
    }
    for (int index = 0; index < targets.length; index++) {
      CheckpointRecord record = checkpoints.get(index);
      if (record.targetFE != targets[index]) {
        failures.add("checkpointTargetMismatch row=" + index);
      }
      if (record.observedFE != record.targetFE || record.overshootFE != 0L) {
        failures.add("checkpointOvershoot target=" + record.targetFE
            + " observed=" + record.observedFE);
      }
      if (record.decisionFrontCsv.indexOf(',') < 0) failures.add("emptyCheckpointDecisionFront");
      if (record.observedFullFrontCsv.indexOf(',') < 0) failures.add("emptyCheckpointObservedFront");
      if (!finite(record.decisionFrontCsv) || !finite(record.observedFullFrontCsv)) {
        failures.add("nonFiniteCheckpointFront");
      }
    }
    if (terminalDecision.indexOf(',') < 0) failures.add("emptyTerminalDecisionFront");
    if (terminalObserved.indexOf(',') < 0) failures.add("emptyTerminalObservedFront");
    if (!finite(terminalDecision) || !finite(terminalObserved)) {
      failures.add("nonFiniteTerminalFront");
    }
    if (terminalObservedFE < 0L) failures.add("terminalObservedFEMissing");
  }

  /** Ledger acceptance gate (diagnostics prereg §3, ON side only). */
  private static void gateLedger(List<String> failures, long ledgerRows, long actualFE,
      long unsetSourceRows, long ledgerErrors, String ledgerLastError, long pddrRounds,
      long expectedOuterCycles) {
    if (ledgerErrors != 0L) {
      failures.add("telemetryLedgerErrors=" + ledgerErrors + ":" + ledgerLastError);
    }
    if (unsetSourceRows != 0L) {
      failures.add("unsetSourceRows=" + unsetSourceRows);
    }
    if (ledgerRows != actualFE) {
      failures.add("ledgerRows=" + ledgerRows + " expected=" + actualFE);
    }
    if (pddrRounds != expectedOuterCycles) {
      failures.add("pddrRounds=" + pddrRounds + " expected=" + expectedOuterCycles);
    }
  }

  private static boolean finite(String csv) {
    for (String line : csv.split("\n")) {
      if (line.isEmpty() || line.startsWith("candidateFingerprint")
          || line.startsWith("observedFE,") || line.startsWith("cycle,")) {
        continue;
      }
      String[] fields = line.split(",");
      for (int index = 1; index < fields.length; index++) {
        if (!Double.isFinite(Double.parseDouble(fields[index]))) return false;
      }
    }
    return true;
  }

  /** Observer completeness gate (task 15). */
  private static void gateCompleteness(List<String> failures,
      long droppedEvents, long unknownSourceEvents, long invalidObjectiveRows,
      long duplicateCandidateEventRows, long boundedCapacityViolations,
      long b0Captured, int checkpointCount, long lifecycleRows, Path partial) {
    if (droppedEvents != 0L) failures.add("droppedEvents=" + droppedEvents);
    if (unknownSourceEvents != 0L) failures.add("unknownSourceEvents=" + unknownSourceEvents);
    if (invalidObjectiveRows != 0L) failures.add("invalidObjectiveRows=" + invalidObjectiveRows);
    if (duplicateCandidateEventRows != 0L) failures.add("duplicateCandidateEventRows=" + duplicateCandidateEventRows);
    if (boundedCapacityViolations != 0L) failures.add("boundedCapacityViolations=" + boundedCapacityViolations);
    if (b0Captured == 0L) failures.add("B0Missing");
    if (checkpointCount < 1) failures.add("noCheckpointsCaptured");
    if (lifecycleRows < 1L) failures.add("lifecycleRows=" + lifecycleRows);
    String ledgerHeader = firstLine(partial.resolve("source-ledger.csv"));
    for (String required : new String[]{"actualFE", "nominalFE", "generation",
        "outerCycle", "qRound", "rawSource", "firstLevelSource"}) {
      if (!headerContains(ledgerHeader, required)) failures.add("ledgerColumnMissing=" + required);
    }
    String lifecycleHeader = firstLine(partial.resolve("source-lifecycle-events.csv"));
    for (String required : new String[]{"actualFE", "nominalFE", "eventType",
        "subjectFingerprint", "relatedFingerprint"}) {
      if (!headerContains(lifecycleHeader, required)) {
        failures.add("lifecycleColumnMissing=" + required);
      }
    }
  }

  private static String firstLine(Path file) {
    if (!Files.isRegularFile(file)) return "";
    try (java.io.BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line = reader.readLine();
      return line == null ? "" : line;
    } catch (IOException error) {
      return "";
    }
  }

  private static boolean headerContains(String header, String field) {
    for (String value : header.split(",")) if (field.equals(value)) return true;
    return false;
  }

  /** Memory/GC summary writer. */
  private static void writeMemorySummary(Path dir, RunnerMemorySampler sampler,
      long wallNanos) throws IOException {
    String NL = "\n";
    StringBuilder text = new StringBuilder();
    text.append("heapUsedPeak=").append(sampler.heapUsedPeak()).append(NL);
    text.append("heapCommittedPeak=").append(sampler.heapCommittedPeak()).append(NL);
    text.append("gcCollectionCount=").append(sampler.gcCollectionCount()).append(NL);
    text.append("gcCollectionTime=").append(sampler.gcCollectionTime()).append(NL);
    text.append("runWallClockNanos=").append(wallNanos).append(NL);
    text.append("samples=").append(sampler.sampleCount()).append(NL);
    Files.write(dir.resolve("memory-summary.properties"),
        text.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Memory/GC sampler (identical on OFF and ON). */
  static final class RunnerMemorySampler implements Runnable {
    private final java.lang.management.MemoryMXBean memory =
        java.lang.management.ManagementFactory.getMemoryMXBean();
    private final List<java.lang.management.GarbageCollectorMXBean> gcs =
        java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
    private volatile boolean running = true;
    private volatile long heapUsedPeak = 0L;
    private volatile long heapCommittedPeak = 0L;
    private volatile long samples = 0L;
    private Thread worker;
    void start() {
      worker = new Thread(this, "observer-memory-sampler");
      worker.setDaemon(true);
      worker.start();
    }
    void stop() {
      running = false;
      try { worker.join(2000L); } catch (InterruptedException ignored) { }
      sample();
    }
    public void run() {
      while (running) {
        sample();
        try { Thread.sleep(100L); } catch (InterruptedException ignored) { return; }
      }
    }
    private void sample() {
      java.lang.management.MemoryUsage usage = memory.getHeapMemoryUsage();
      heapUsedPeak = Math.max(heapUsedPeak, usage.getUsed());
      heapCommittedPeak = Math.max(heapCommittedPeak, usage.getCommitted());
      samples++;
    }
    long heapUsedPeak() { return heapUsedPeak; }
    long heapCommittedPeak() { return heapCommittedPeak; }
    long sampleCount() { return samples; }
    long gcCollectionCount() {
      long total = 0L;
      for (java.lang.management.GarbageCollectorMXBean bean : gcs) total += bean.getCollectionCount();
      return total;
    }
    long gcCollectionTime() {
      long total = 0L;
      for (java.lang.management.GarbageCollectorMXBean bean : gcs) total += bean.getCollectionTime();
      return total;
    }
  }

  /** Copies observer streaming temp files into the run output dir. */
  private static void copyLedgerFiles(Path partial) throws IOException {
    java.io.File ledger = V35SourceAttributionObserver.getLedgerTempFile();
    java.io.File pddr = V35SourceAttributionObserver.getPddrTempFile();
    java.io.File lifecycle = V35SourceAttributionObserver.getLifecycleTempFile();
    if (ledger != null && ledger.exists()) {
      Files.copy(ledger.toPath(), partial.resolve("source-ledger.csv"),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    if (pddr != null && pddr.exists()) {
      Files.copy(pddr.toPath(), partial.resolve("pddr-round-ledger.csv"),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    if (lifecycle != null && lifecycle.exists()) {
      Files.copy(lifecycle.toPath(), partial.resolve("source-lifecycle-events.csv"),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** Counts data rows in the on-disk ledger (excludes headers). */
  private static long countLedgerFileRows(Path partial) {
    java.io.File ledger = partial.resolve("source-ledger.csv").toFile();
    if (!ledger.exists()) return 0L;
    long rows = 0L;
    try (java.io.BufferedReader reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(new java.io.FileInputStream(ledger),
            StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isEmpty() && !line.startsWith("actualFE,")
            && !line.startsWith("cycle,")) {
          rows++;
        }
      }
    } catch (java.io.IOException error) {
      return 0L;
    }
    return rows;
  }

  private static void writeBudget(Path dir, V35FairRunner.RunRecord record, Budget budget,
      String formalSha, String experimentalSha) throws IOException {
    String summary = record.getMechanismSummary();
    long caTa = value(summary, "caTaLiteFE");
    long formalLocal = value(summary, "formalLocalFE");
    String text = "budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\n"
        + "budgetProtocolVersion=" + BUDGET_VERSION + "\nrequestedMaxFE=" + budget.requested
        + "\nactualFE=" + budget.actual + "\ndecoderCalls=" + budget.decoder
        + "\nremainingFE=" + budget.remaining + "\nqPhaseFE=" + budget.qPhase
        + "\nutilizationRate=" + String.format(Locale.ROOT, "%.12f", budget.utilization())
        + "\nterminationKind=" + budget.kind + "\nphaseBoundAccepted=" + budget.accepted
        + "\nphaseBoundFailure=" + budget.failure
        + "\nformalOuterCycles=" + value(summary, "formalOuterCycles")
        + "\nformalQgRounds=" + value(summary, "formalQgRounds")
        + "\nformalLocalFE=" + formalLocal
        + "\ncaTaLiteFE=" + caTa
        + "\ntotalLocalFE=" + (formalLocal + caTa)
        + "\nlocalFeShare=" + String.format(Locale.ROOT, "%.12f",
            budget.actual == 0 ? 0.0 : (double) (formalLocal + caTa) / budget.actual)
        + "\nglobalPhaseFE=" + Math.max(0L, budget.actual - formalLocal - caTa)
        + "\ncfvfOffspring=" + value(summary, "cfvfOffspring")
        + "\nqgSelections=" + value(summary, "qgSelections")
        + "\nqpActions=" + value(summary, "qpActions")
        + "\nformalJarSha256=" + formalSha
        + "\nexperimentalJarSha256=" + experimentalSha + "\n";
    Files.write(dir.resolve("budget-termination.properties"), text.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeGate(Path dir, V35FairRunner.RunRecord record, long wallNanos,
      List<String> failures, String runId, V35LocalFePacingRepairProfile.Label label,
      boolean observerOn, long[] targets, int checkpointRows, long observerErrors,
      long ledgerRows, long pddrRounds, long lifecycleRows, long ledgerErrors)
      throws IOException {
    String text = "runnerVersion=" + VERSION + "\nstatus="
        + (failures.isEmpty() ? "COMPLETED" : "FAILED")
        + "\nrunId=" + runId + "\nprofile=" + label.name()
        + "\nobserverMode=" + (observerOn ? "ON" : "OFF")
        + "\ncheckpointTargets=" + joinLongs(targets)
        + "\ncheckpointRows=" + checkpointRows
        + "\nobserverExecutionErrors=" + observerErrors
        + "\ntelemetryLedgerRows=" + ledgerRows
        + "\ntelemetryPddrRounds=" + pddrRounds
        + "\ntelemetryLifecycleRows=" + lifecycleRows
        + "\ntelemetryLedgerErrors=" + ledgerErrors
        + "\nactualFE=" + record.getFullEvaluations() + "\ndecoderCalls=" + record.getDecoderCalls()
        + "\nfrontSize=" + record.getFront().size() + "\nwallNanos=" + wallNanos
        + "\nfailures=" + (failures.isEmpty() ? "NONE" : join(failures)) + "\n";
    Files.write(dir.resolve("formal-gate.properties"), text.getBytes(StandardCharsets.UTF_8));
  }

  private static void writePddrObservation(Path dir, V35FairRunner.RunRecord record,
      V35LocalFePacingRepairProfile.Label label, String instance, long seed) throws IOException {
    String summary = record.getMechanismSummary();
    String events = record.getCaTaEvents() == null ? "" : record.getCaTaEvents();
    long formalLocalOps = 0L;
    long formalLocalAccepted = 0L;
    long inheritedInsertSwap = 0L;
    long inheritedO1toO9 = 0L;
    for (String line : events.split("\n")) {
      if (!line.startsWith("formalLocal:")) continue;
      formalLocalOps++;
      if (line.contains("accepted=true")) formalLocalAccepted++;
      if (line.contains("op=CRITICAL_SWAP") || line.contains("op=CRITICAL_INSERT")
          || line.contains("op=FACTORY_SWAP") || line.contains("op=FACTORY_INSERT")) {
        inheritedInsertSwap++;
      } else {
        inheritedO1toO9++;
      }
    }
    double minCmax = Double.POSITIVE_INFINITY;
    double minTec = Double.POSITIVE_INFINITY;
    double minTwc = Double.POSITIVE_INFINITY;
    for (double[] point : record.getFront()) {
      minCmax = Math.min(minCmax, point[0]);
      minTec = Math.min(minTec, point[1]);
      minTwc = Math.min(minTwc, point[2]);
    }
    StringBuilder out = new StringBuilder()
        .append("profile=").append(label.name())
        .append("\ninstance=").append(instance)
        .append("\nseed=").append(seed)
        .append("\npddrSelectionMode=GLOBAL_ORIGINAL\n")
        .append("pddrEvents=").append(value(summary, "pddrEvents"))
        .append("\npddrEventStreamHash=").append(hex(summary, "pddrEventStreamHash"))
        .append("\narchiveInsertions=").append(value(summary, "archiveInsertions"))
        .append("\nglobalOffspringFE=").append(value(summary, "cfvfOffspring"))
        .append("\ncaTaTestCalls=").append(value(summary, "caTaLiteTest"))
        .append("\ncaTaApplyCalls=").append(value(summary, "caTaLiteApply"))
        .append("\ncaTaLiteFE=").append(value(summary, "caTaLiteFE"))
        .append("\nformalLocalFE=").append(value(summary, "formalLocalFE"))
        .append("\ninheritedLocalEventOps=").append(formalLocalOps)
        .append("\ninheritedLocalAccepted=").append(formalLocalAccepted)
        .append("\ninheritedLocalInsertSwapOps=").append(inheritedInsertSwap)
        .append("\ninheritedLocalO1O9Ops=").append(inheritedO1toO9)
        .append("\ndecisionFrontSize=").append(record.getFront().size())
        .append("\nobservedFullFrontSize=").append(record.getPassiveArchiveSize())
        .append(String.format(Locale.ROOT, "\nminCmax=%.12f", minCmax))
        .append(String.format(Locale.ROOT, "\nminTEC=%.12f", minTec))
        .append(String.format(Locale.ROOT, "\nminTWC=%.12f", minTwc))
        .append("\npoolLevelAttribution=SOURCE_DIAGNOSTIC_LEDGER\n")
        .append("observationMode=POST_HOC_PARSE_ONLY\n");
    Files.write(dir.resolve("pddr-observation.properties"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Exports all checkpoint/terminal front artifacts (ON mode only). */
  private static void writeCheckpoints(Path dir, List<CheckpointRecord> checkpoints,
      String terminalDecision, String terminalObserved, long terminalObservedFE,
      long terminalCounterFE, Map<Long, Map<String, String>> observerFronts) throws IOException {
    Path checkpointsDir = dir.resolve("checkpoints");
    Files.createDirectory(checkpointsDir);
    StringBuilder registry = new StringBuilder(
        "checkpointTargetFE,checkpointObservedFE,overshootFE,evaluationCounterFE,"
        + "frontType,frontSize\n");
    StringBuilder combined = new StringBuilder(
        "checkpointTargetFE,checkpointObservedFE,overshootFE,frontType,"
        + "candidateFingerprint,Cmax,TEC,TWC\n");
    Map<String, String> b0 = observerFronts.get(0L);
    if (b0 == null || b0.get("decision-front") == null
        || b0.get("observed-full-front") == null) {
      throw new IOException("B0 front missing at export");
    }
    String b0Decision = b0.get("decision-front");
    String b0Observed = b0.get("observed-full-front");
    Files.write(checkpointsDir.resolve("checkpoint-0-decision-front.csv"),
        b0Decision.getBytes(StandardCharsets.UTF_8));
    Files.write(checkpointsDir.resolve("checkpoint-0-observed-full-front.csv"),
        b0Observed.getBytes(StandardCharsets.UTF_8));
    registry.append("0,0,0,0,b0-decision-front,").append(lineCount(b0Decision)).append('\n');
    registry.append("0,0,0,0,b0-observed-full-front,").append(lineCount(b0Observed)).append('\n');
    for (CheckpointRecord record : checkpoints) {
      registry.append(record.targetFE).append(',').append(record.observedFE).append(',')
          .append(record.overshootFE).append(',').append(record.counterFE)
          .append(",checkpoint-decision-front,").append(lineCount(record.decisionFrontCsv))
          .append('\n');
      registry.append(record.targetFE).append(',').append(record.observedFE).append(',')
          .append(record.overshootFE).append(',').append(record.counterFE)
          .append(",checkpoint-observed-full-front,").append(lineCount(record.observedFullFrontCsv))
          .append('\n');
      Files.write(checkpointsDir.resolve(
              "checkpoint-" + record.targetFE + "-decision-front.csv"),
          record.decisionFrontCsv.getBytes(StandardCharsets.UTF_8));
      Files.write(checkpointsDir.resolve(
              "checkpoint-" + record.targetFE + "-observed-full-front.csv"),
          record.observedFullFrontCsv.getBytes(StandardCharsets.UTF_8));
      combined.append(combinedRows(record.targetFE, record.observedFE, record.overshootFE,
          "checkpoint-decision-front", record.decisionFrontCsv));
      combined.append(combinedRows(record.targetFE, record.observedFE, record.overshootFE,
          "checkpoint-observed-full-front", record.observedFullFrontCsv));
    }
    registry.append(terminalObservedFE).append(',').append(terminalObservedFE).append(",0,")
        .append(terminalCounterFE).append(",terminal-decision-front,")
        .append(lineCount(terminalDecision)).append('\n');
    registry.append(terminalObservedFE).append(',').append(terminalObservedFE).append(",0,")
        .append(terminalCounterFE).append(",terminal-observed-full-front,")
        .append(lineCount(terminalObserved)).append('\n');
    Files.write(checkpointsDir.resolve("checkpoint-registry.csv"),
        registry.toString().getBytes(StandardCharsets.UTF_8));
    combined.append(combinedRows(terminalObservedFE, terminalObservedFE, 0L,
        "terminal-decision-front", terminalDecision));
    combined.append(combinedRows(terminalObservedFE, terminalObservedFE, 0L,
        "terminal-observed-full-front", terminalObserved));
    Files.write(dir.resolve("checkpoint-fronts.csv"),
        combined.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Exports the evaluation-ordered source ledger and the merge-pool/PDDR
   *  ledger (ON mode only). */
  private static void writeSourceLedgers(Path dir, String evaluationLedger,
      String pddrLedger) throws IOException {
    Files.write(dir.resolve("source-ledger.csv"),
        evaluationLedger.getBytes(StandardCharsets.UTF_8));
    Files.write(dir.resolve("pddr-round-ledger.csv"),
        pddrLedger.getBytes(StandardCharsets.UTF_8));
  }

  private static String combinedRows(long targetFE, long observedFE, long overshootFE,
      String frontType, String frontCsv) {
    StringBuilder out = new StringBuilder();
    String[] lines = frontCsv.split("\n");
    for (String line : lines) {
      if (line.isEmpty() || line.startsWith("candidateFingerprint")) continue;
      String[] fields = line.split(",");
      out.append(targetFE).append(',').append(observedFE).append(',').append(overshootFE)
          .append(',').append(frontType).append(',').append(fields[0]).append(',')
          .append(fields[1]).append(',').append(fields[2]).append(',').append(fields[3])
          .append('\n');
    }
    return out.toString();
  }

  private static int lineCount(String frontCsv) {
    int count = 0;
    for (String line : frontCsv.split("\n")) {
      if (!line.isEmpty() && !line.startsWith("candidateFingerprint")
          && !line.startsWith("Cmax,TEC,TWC")) count++;
    }
    return count;
  }

  private static String joinLongs(long[] values) {
    StringBuilder out = new StringBuilder();
    for (long value : values) {
      if (out.length() > 0) out.append(',');
      out.append(value);
    }
    return out.toString();
  }

  private static void writeManifest(Path root) throws Exception {
    Files.deleteIfExists(root.resolve("evidence-sha256.tsv"));
    Map<String, String> rows = new TreeMap<String, String>();
    try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
      java.util.Iterator<Path> iterator = stream.filter(Files::isRegularFile).iterator();
      while (iterator.hasNext()) {
        Path file = iterator.next();
        rows.put(root.relativize(file).toString().replace('\\', '/'), sha256(file));
      }
    }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> row : rows.entrySet()) {
      text.append(row.getKey()).append('\t').append(row.getValue()).append('\n');
    }
    Files.write(root.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static Path requireRegular(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    if (!Files.isRegularFile(normalized)) {
      throw new IllegalArgumentException("missing required file: " + normalized);
    }
    return normalized;
  }

  private static Properties load(Path path) throws IOException {
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(path)) {
      properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
    return properties;
  }

  private static String required(Properties p, String key) {
    String value = p.getProperty(key);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("missing binding key=" + key);
    }
    return value.trim();
  }

  private static void requireEquals(String field, String expected, String actual) {
    if (!expected.equalsIgnoreCase(actual)) {
      throw new IllegalStateException(field + " expected=" + expected + " actual=" + actual);
    }
  }

  private static long value(String text, String key) {
    Matcher matcher = Pattern.compile(
        String.format(Locale.ROOT, NUMBER.pattern(), Pattern.quote(key)))
        .matcher(text == null ? "" : text);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
  }

  private static String hex(String text, String key) {
    Matcher matcher = Pattern.compile(
        String.format(Locale.ROOT, NUMBER.pattern().replace("\\d+", "[0-9a-fA-F]+"), Pattern.quote(key)))
        .matcher(text == null ? "" : text);
    return matcher.find() ? matcher.group(1) : "UNKNOWN";
  }

  private static void positive(List<String> failures, String text, String key) {
    long n = value(text, key);
    if (n <= 0) failures.add(key + "=" + n + " expected>0");
  }

  private static void zero(List<String> failures, String text, String key) {
    long n = value(text, key);
    if (n != 0) failures.add(key + "=" + n + " expected=0");
  }

  private static String join(List<String> values) {
    StringBuilder out = new StringBuilder();
    for (String value : values) {
      if (out.length() > 0) out.append(';');
      out.append(value);
    }
    return out.toString();
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException error) {
      Files.move(source, target);
    }
  }

  private static String sha256(String text) throws Exception {
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path file) throws Exception {
    return sha256(Files.readAllBytes(file));
  }

  private static String sha256(byte[] bytes) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
    return out.toString();
  }

  /** PHASE_CONSISTENT_BUDGET_TERMINATION classifier (identical to the formal arm runner). */
  static final class Budget {
    final long requested, actual, decoder, remaining, qPhase;
    final String kind, failure;
    final boolean accepted;
    private Budget(long requested, long actual, long decoder, String kind, String failure) {
      this.requested = requested;
      this.actual = actual;
      this.decoder = decoder;
      this.remaining = requested - actual;
      this.qPhase = (long) POPULATION * Q_TIMES;
      this.kind = kind;
      this.failure = failure;
      this.accepted = "NONE".equals(failure);
    }
    static Budget classify(long requested, long actual, long decoder) {
      if (requested <= 0) return new Budget(requested, actual, decoder, "INVALID", "REQUESTED_NON_POSITIVE");
      if (actual <= 0) return new Budget(requested, actual, decoder, "INVALID", "ACTUAL_NON_POSITIVE");
      if (decoder != actual) return new Budget(requested, actual, decoder, "INVALID", "DECODER_MISMATCH");
      if (actual > requested) return new Budget(requested, actual, decoder, "INVALID", "OVER_BUDGET");
      long remaining = requested - actual;
      if (remaining >= (long) POPULATION * Q_TIMES) {
        return new Budget(requested, actual, decoder, "INVALID", "TAIL_TOO_LARGE");
      }
      return new Budget(requested, actual, decoder,
          remaining == 0 ? "EXACT_MAX_FE" : "PHASE_CONSISTENT_TAIL_STOP", "NONE");
    }
    double utilization() { return requested <= 0 ? 0.0 : (double) actual / requested; }
  }

  /** Parsed CLI; the six permitted flags plus the telemetry toggle and targets. */
  static final class Arguments {
    final String instance;
    final long seed;
    final String profile;
    final int maxFes;
    final Path snapshot;
    final Path output;
    final boolean telemetryOn;
    final long[] checkpoints;

    private Arguments(String instance, long seed, String profile, int maxFes,
        Path snapshot, Path output, boolean telemetryOn, long[] checkpoints) {
      this.instance = instance;
      this.seed = seed;
      this.profile = profile;
      this.maxFes = maxFes;
      this.snapshot = snapshot;
      this.output = output;
      this.telemetryOn = telemetryOn;
      this.checkpoints = checkpoints;
    }

    static Arguments parse(String[] args) {
      String instance = null;
      Long seed = null;
      String profile = null;
      Integer maxFes = null;
      Path snapshot = null;
      Path output = null;
      Boolean telemetryOn = Boolean.FALSE;
      long[] checkpoints = DEFAULT_CHECKPOINTS;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        String flag = args[index];
        String next = args[index + 1];
        if ("--instance".equals(flag)) instance = next;
        else if ("--seed".equals(flag)) seed = Long.parseLong(next);
        else if ("--profile".equals(flag)) profile = next;
        else if ("--max-fes".equals(flag)) maxFes = Integer.parseInt(next);
        else if ("--snapshot".equals(flag)) snapshot = Paths.get(next);
        else if ("--output".equals(flag)) output = Paths.get(next);
        else if ("--telemetry".equals(flag)) {
          if ("ON".equalsIgnoreCase(next)) telemetryOn = Boolean.TRUE;
          else if ("OFF".equalsIgnoreCase(next)) telemetryOn = Boolean.FALSE;
          else throw usage();
        } else if ("--checkpoints".equals(flag)) {
          String[] parts = next.split(",");
          long[] values = new long[parts.length];
          for (int i = 0; i < parts.length; i++) values[i] = Long.parseLong(parts[i].trim());
          checkpoints = values;
        } else throw usage();
      }
      if (instance == null || seed == null || profile == null || maxFes == null
          || snapshot == null || output == null) {
        throw usage();
      }
      return new Arguments(instance, seed, profile, maxFes, snapshot, output,
          telemetryOn, checkpoints);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "usage: --instance <name> --seed <long> --profile REF_A4|C0|C1|C2|C3 "
              + "--max-fes <int> --snapshot <path> --output <path> "
              + "[--telemetry OFF|ON] [--checkpoints <csv>]");
    }
  }
}
