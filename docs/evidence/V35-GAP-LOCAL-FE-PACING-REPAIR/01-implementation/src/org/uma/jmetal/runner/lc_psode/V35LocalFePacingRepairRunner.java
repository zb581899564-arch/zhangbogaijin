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
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFePacingRepairProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-GAP-LOCAL-FE-PACING-REPAIR-V1 snapshot-bound experimental runner.
 *
 * <p>CLI (exactly six flags, nothing else):
 * {@code --instance <name> --seed <long> --profile REF_A4|C0|C1|C2|C3
 * --max-fes <int> --snapshot <path> --output <path>}. Scientific parameters are
 * frozen inside {@link V35LocalFePacingRepairProfile}; no CLI override exists.
 * Instance/SUT/fatigue files resolve from the fixed layout under the working
 * directory ({@code inputs/java-jmetal58/...}) and every artifact hash is
 * verified against the pre-registered binding file
 * {@code bindings/<instance>.binding.properties} before any evaluation.</p>
 *
 * <p>The runner never creates an initial solution and never modifies the
 * frozen formal jar or any algorithm behavior: REF_A4 runs the frozen formal
 * A4 path; C0--C3 differ only in the local-FE budget betaMax. All observation
 * (PDDR bypass, FE accounting) is post-hoc parsing of the run record.</p>
 */
public final class V35LocalFePacingRepairRunner {
  public static final String VERSION = "v35-local-fe-pacing-repair-runner-v1";
  public static final String BUDGET_VERSION = "v35-phase-consistent-budget-v1";
  private static final int POPULATION = 100;
  private static final int Q_TIMES = 50;
  private static final Pattern NUMBER = Pattern.compile("(?:^|[,|])%s=(-?\\d+)(?:$|[,|])");

  private V35LocalFePacingRepairRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    execute(value);
  }

  static void execute(Arguments value) throws Exception {
    V35LocalFePacingRepairProfile.Label label =
        V35LocalFePacingRepairProfile.fromCli(value.profile);
    // Fixed input layout, anchored at the working directory.
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
        "jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar"));
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
    String runId = "GAPL20K-" + label.cliAlias() + "-" + value.instance + "-" + value.seed;

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

    // Snapshot-bound initial population; the formal path must not create solutions.
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
    // Runtime read-back of the knob (gate item: four-level betaMax actually read).
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
    try {
      long wallStart = System.nanoTime();
      V35FairRunner.RunRecord record = V35FairRunner.run(
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner.Mode.V35_FULL_POOL_OFF,
          problem, P8InitialPopulationProvider.copy(initial), value.maxFes, value.seed, true,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
      long wallNanos = System.nanoTime() - wallStart;
      Budget budget = Budget.classify(value.maxFes, record.getFullEvaluations(),
          record.getDecoderCalls());
      List<String> failures = gate(label, record, v35Hash, budget, configuration);
      String canonical = "repairRunnerVersion=" + VERSION + "\n"
          + "repairFamily=LOCAL_FE_PACING\nsingleKnob=betaMax\n"
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
      writeGate(partial, record, wallNanos, failures, runId, label);
      writePddrObservation(partial, record, label, value.instance, value.seed);
      writeManifest(partial);
      move(partial, target);
      moved = true;
      if (!failures.isEmpty()) {
        throw new IllegalStateException("RUN_GATE_FAILED " + join(failures));
      }
      System.out.println("V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=" + label.cliAlias()
          + " FE=" + record.getFullEvaluations() + " output=" + target);
    } catch (Exception error) {
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
    // PDDR must remain GLOBAL_ORIGINAL with the frozen beta axis.
    if (configuration.getLocalFeBudget() == null) failures.add("localFeBudgetMissing");
    return failures;
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
      List<String> failures, String runId, V35LocalFePacingRepairProfile.Label label)
      throws IOException {
    String text = "runnerVersion=" + VERSION + "\nstatus="
        + (failures.isEmpty() ? "COMPLETED" : "FAILED")
        + "\nrunId=" + runId + "\nprofile=" + label.name()
        + "\nactualFE=" + record.getFullEvaluations() + "\ndecoderCalls=" + record.getDecoderCalls()
        + "\nfrontSize=" + record.getFront().size() + "\nwallNanos=" + wallNanos
        + "\nfailures=" + (failures.isEmpty() ? "NONE" : join(failures)) + "\n";
    Files.write(dir.resolve("formal-gate.properties"), text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * PDDR bypass observation (post-hoc, zero in-algorithm telemetry): per-source
   * FE accounting from the mechanism summary and the CA-TA/inherited event
   * stream, plus PDDR round/survivor counts and front geometry. Pool-level
   * per-candidate PDDR attribution is not exported by the frozen jar; the
   * field poolLevelAttribution records that limitation explicitly.
   */
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
        .append("\npoolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR\n")
        .append("observationMode=POST_HOC_PARSE_ONLY\n");
    Files.write(dir.resolve("pddr-observation.properties"), out.toString().getBytes(StandardCharsets.UTF_8));
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

  /** Extracts a hex digest field (e.g. pddrEventStreamHash) from the mechanism summary. */
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

  /** Parsed CLI; exactly the six permitted flags. */
  static final class Arguments {
    final String instance;
    final long seed;
    final String profile;
    final int maxFes;
    final Path snapshot;
    final Path output;

    private Arguments(String instance, long seed, String profile, int maxFes,
        Path snapshot, Path output) {
      this.instance = instance;
      this.seed = seed;
      this.profile = profile;
      this.maxFes = maxFes;
      this.snapshot = snapshot;
      this.output = output;
    }

    static Arguments parse(String[] args) {
      String instance = null;
      Long seed = null;
      String profile = null;
      Integer maxFes = null;
      Path snapshot = null;
      Path output = null;
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
        else throw usage();
      }
      if (instance == null || seed == null || profile == null || maxFes == null
          || snapshot == null || output == null) {
        throw usage();
      }
      return new Arguments(instance, seed, profile, maxFes, snapshot, output);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "usage: --instance <name> --seed <long> --profile REF_A4|C0|C1|C2|C3 "
              + "--max-fes <int> --snapshot <path> --output <path>");
    }
  }
}
