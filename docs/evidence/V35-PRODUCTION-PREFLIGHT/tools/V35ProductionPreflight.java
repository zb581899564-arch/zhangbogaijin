import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * External Stage2 diagnostic runner.  This file is deliberately kept under
 * evidence rather than project source: it calls the frozen public APIs but
 * neither changes their algorithm logic nor supplies alternate parameters.
 */
public final class V35ProductionPreflight {
  private static final String VERSION = "v35-phase-consistent-budget-v1";
  private static final int POPULATION = 100;
  private static final int Q_TIMES = 50;
  private static final long DEFAULT_SEED = 20260828L;
  private static final String INSTANCE_NAME = "20_2_3_1";
  private static final Pattern NUMBER = Pattern.compile("(?:^|[,|])%s=(-?\\d+)(?:$|[,|])");

  private V35ProductionPreflight() { }

  /** Maps immutable paper labels to descriptive Java enum constants. */
  static V35FinalAblationProfile.Arm armForLabel(String label) {
    for (V35FinalAblationProfile.Arm candidate : V35FinalAblationProfile.Arm.values()) {
      if (candidate.getLabel().equals(label)) return candidate;
    }
    throw new IllegalArgumentException("--arm must be one of A0,A1,A2,A3,A4: " + label);
  }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    run(parsed);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void run(Arguments parsed) throws Exception {
    if ("PREFLIGHT".equals(parsed.purpose)
        && parsed.maxFes != 50000 && parsed.maxFes != 100000) {
      throw new IllegalArgumentException("preflight max-fes must be 50000 or documented fallback 100000");
    }
    if ("THROUGHPUT".equals(parsed.purpose)
        && parsed.maxFes != 20000 && parsed.maxFes != 50000) {
      throw new IllegalArgumentException("throughput max-fes must be 20000 or 50000");
    }
    Path projectRoot = parsed.projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(projectRoot.resolve("EADHFSP"))
        ? projectRoot : projectRoot.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP").resolve(INSTANCE_NAME + ".txt");
    Path extensionDirectory = javaProject.resolve("instance-extensions/v1");
    Path fatigueDirectory = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance, "instance");
    requireFile(extensionDirectory.resolve(INSTANCE_NAME + ".setup.txt"), "SUT extension");
    requireFile(fatigueDirectory.resolve(INSTANCE_NAME + ".fatigue.txt"), "fatigue parameters");

    Path destination = parsed.output.toAbsolutePath().normalize();
    if (Files.exists(destination)) {
      throw new IllegalStateException("refusing to overwrite evidence: " + destination);
    }
    Files.createDirectories(destination.getParent());
    Path partial = destination.resolveSibling(".partial-" + destination.getFileName() + "-"
        + System.nanoTime());
    Files.createDirectories(partial);
    boolean moved = false;
    try {
      ZhangBoCanonicalProductionProblem seedProblem = load(instance, extensionDirectory,
          fatigueDirectory, parsed.seed);
      List<PermutationSolution<Integer>> initial = new ArrayList<PermutationSolution<Integer>>();
      for (int index = 0; index < POPULATION; index++) initial.add(seedProblem.createSolution());
      String initialHash = V35FairRunner.initialHash(initial);
      V35ProductionConfiguration config = V35FinalAblationProfile.configurationFor(parsed.arm,
          parsed.seed, POPULATION, parsed.maxFes);
      V35FinalAblationProfile.validate(parsed.arm, config);
      String profileText = V35FinalAblationProfile.canonicalTextFor(parsed.arm, parsed.seed,
          POPULATION, parsed.maxFes);
      String profileHash = V35FinalAblationProfile.configurationHashFor(parsed.arm, parsed.seed,
          POPULATION, parsed.maxFes);
      ZhangBoCanonicalProductionProblem problem = load(instance, extensionDirectory,
          fatigueDirectory, parsed.seed);
      long wallStart = System.nanoTime();
      V35FairRunner.RunRecord record = V35FairRunner.run(parsed.arm.getMode(), problem,
          P8InitialPopulationProvider.copy(initial), parsed.maxFes, parsed.seed, true,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, config);
      long wallNanos = System.nanoTime() - wallStart;

      String configuration = canonicalRunText(parsed, instance, extensionDirectory, fatigueDirectory,
          initialHash, profileHash, profileText);
      V35FairRunner.writeRecord(record, partial, configuration);
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  initial-four-vector-population\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("profile.sha256"),
          (profileHash + "  canonical-profile\n").getBytes(StandardCharsets.UTF_8));

      BudgetTermination budget = BudgetTermination.classify(parsed.maxFes,
          record.getFullEvaluations(), record.getDecoderCalls(), POPULATION, Q_TIMES);
      List<String> failures = hardGate(parsed.arm, record, initialHash, budget);
      writeGateEvidence(partial, parsed, record, wallNanos, budget, failures);
      writeHashes(partial);
      move(partial, destination);
      moved = true;
      if (!failures.isEmpty()) {
        throw new IllegalStateException("PRECHECK_FAILED " + join(failures, ";")
            + " evidence=" + destination);
      }
      System.out.println("V35_STAGE2_PREFLIGHT_COMPLETED arm=" + parsed.arm.getLabel()
          + " FE=" + record.getFullEvaluations() + " output=" + destination);
    } catch (Exception error) {
      if (!moved && Files.exists(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + String.valueOf(error.getMessage()) + "\n")
                .getBytes(StandardCharsets.UTF_8));
        writeHashes(partial);
        move(partial, destination);
      }
      throw error;
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension, Path fatigue,
      long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed, extension,
        fatigue, ZhangBoShiftConfiguration.none());
  }

  private static String canonicalRunText(Arguments parsed, Path instance, Path extension,
      Path fatigue, String initialHash, String profileHash, String profileText) throws Exception {
    return "preflightVersion=" + VERSION + "\n"
        + "evidenceClass=diagnostic_not_formal_statistical_run\n"
        + "purpose=" + parsed.purpose + "\n"
        + "arm=" + parsed.arm.getLabel() + "\n"
        + "instance=" + INSTANCE_NAME + "\n"
        + "seed=" + parsed.seed + "\n"
        + "population=" + POPULATION + "\n"
        + "requestedFE=" + parsed.maxFes + "\n"
        + "budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\n"
        + "budgetProtocolVersion=" + VERSION + "\n"
        + "qTimes=" + Q_TIMES + "\n"
        + "qPhaseFE=" + (POPULATION * Q_TIMES) + "\n"
        + "decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n"
        + "pddrSelectionMode=GLOBAL_ORIGINAL\n"
        + "localSearchOrder=CATA_THEN_INHERITED\n"
        + "directionalTeacherPool=false\n"
        + "objectives=0,1,6\n"
        + "freezeJarSha256=" + parsed.jarSha256 + "\n"
        + "freezeBinding=" + parsed.freezeBinding + "\n"
        + "instanceSha256=" + sha256(instance) + "\n"
        + "sutExtensionSha256=" + sha256(extension.resolve(INSTANCE_NAME + ".setup.txt")) + "\n"
        + "fatigueParametersSha256=" + sha256(fatigue.resolve(INSTANCE_NAME + ".fatigue.txt")) + "\n"
        + "initialPopulationHash=" + initialHash + "\n"
        + "profileSha256=" + profileHash + "\n"
        + "profileCanonicalBegin\n" + profileText + "profileCanonicalEnd\n";
  }

  private static List<String> hardGate(V35FinalAblationProfile.Arm arm,
      V35FairRunner.RunRecord record, String initialHash, BudgetTermination budget) {
    List<String> failures = new ArrayList<String>();
    if (!"COMPLETED".equals(record.getStatus())) add(failures, "status=" + record.getStatus());
    if (!initialHash.equals(record.getInitialPopulationHash())) add(failures, "initialPopulationDrift");
    if (!budget.isAccepted()) add(failures, "budget=" + budget.getFailureReason());
    if (record.getDecoderCalls() != record.getFullEvaluations()) {
      add(failures, "decoderCalls=" + record.getDecoderCalls() + " actualFE="
          + record.getFullEvaluations());
    }
    if (record.getIllegalSolutions() != 0) add(failures, "illegal=" + record.getIllegalSolutions());
    if (record.getDuplicateEvaluations() != 0) add(failures, "duplicate="
        + record.getDuplicateEvaluations());
    if (record.getFront().isEmpty()) add(failures, "emptyFront");
    for (double[] point : record.getFront()) {
      for (double objective : point) {
        if (!Double.isFinite(objective)) add(failures, "nonFiniteObjective");
      }
    }
    ZhangBoDecoderTimingSnapshot timing = record.getDecoderTiming();
    if (timing.getSuccessfulDecoderCalls() != record.getFullEvaluations()) {
      add(failures, "decoderSnapshotMismatch");
    }
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L
        || timing.getLeftFullRecomputations() != 0L || timing.getRightFullRecomputations() != 0L) {
      add(failures, "shiftNotFrozen");
    }

    String summary = record.getMechanismSummary();
    positive(failures, summary, "qgSelections");
    positive(failures, summary, "pddrEvents");
    zero(failures, summary, "cfvfRepairs");
    zero(failures, summary, "directionalPoolRequests");
    zero(failures, summary, "directionalPoolFiltered");
    zero(failures, summary, "shadowSamples");
    zero(failures, summary, "shadowEvaluations");
    if (arm.isDscrEnabled()) {
      positive(failures, summary, "teacherUses");
      positive(failures, summary, "validityChecks");
      zero(failures, summary, "dominatedTeacherUses");
    } else {
      zero(failures, summary, "teacherUses");
      zero(failures, summary, "validityChecks");
      zero(failures, summary, "replacements");
    }
    expected(failures, summary, "baselineUpdateEvents", !arm.isCfvfEnabled());
    expected(failures, summary, "cfvfOffspring", arm.isCfvfEnabled());
    expected(failures, summary, "qpActions", arm.isQpEnabled());
    expected(failures, summary, "qpTransitions", arm.isQpEnabled());
    expected(failures, summary, "archiveInsertions", arm.isQpEnabled());
    expected(failures, summary, "dualQP", arm.isQpEnabled());
    expected(failures, summary, "dualQG", arm.isQpEnabled());
    expected(failures, summary, "caTaLiteTest", arm.isCaTaLiteEnabled());
    expected(failures, summary, "caTaLiteApply", arm.isCaTaLiteEnabled());
    if (!arm.isCaTaLiteEnabled()) zero(failures, summary, "caTaLiteFE");
    if (arm.isCaTaLiteEnabled()) positive(failures, summary, "formalLocalFE");
    return failures;
  }

  private static void expected(List<String> failures, String text, String key, boolean enabled) {
    if (enabled) positive(failures, text, key); else zero(failures, text, key);
  }

  private static void positive(List<String> failures, String text, String key) {
    long value = summaryValue(text, key);
    if (value <= 0) add(failures, key + "=" + value + " expected>0");
  }

  private static void zero(List<String> failures, String text, String key) {
    long value = summaryValue(text, key);
    if (value != 0) add(failures, key + "=" + value + " expected=0");
  }

  private static long summaryValue(String text, String key) {
    Pattern pattern = Pattern.compile(String.format(Locale.ROOT, NUMBER.pattern(),
        Pattern.quote(key)));
    Matcher matcher = pattern.matcher(text == null ? "" : text);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
  }

  private static void writeGateEvidence(Path directory, Arguments parsed,
      V35FairRunner.RunRecord record, long wallNanos, BudgetTermination budget,
      List<String> failures) throws IOException {
    String status = failures.isEmpty() ? "PASS" : "FAIL";
    StringBuilder gate = new StringBuilder()
        .append("preflightVersion=").append(VERSION).append('\n')
        .append("gateStatus=").append(status).append('\n')
        .append("arm=").append(parsed.arm.getLabel()).append('\n')
        .append("requestedFE=").append(parsed.maxFes).append('\n')
        .append("actualFE=").append(record.getFullEvaluations()).append('\n')
        .append("decoderCalls=").append(record.getDecoderCalls()).append('\n')
        .append("illegalSolutions=").append(record.getIllegalSolutions()).append('\n')
        .append("duplicateEvaluations=").append(record.getDuplicateEvaluations()).append('\n')
        .append("frontSize=").append(record.getFront().size()).append('\n')
        .append("wallNanos=").append(wallNanos).append('\n')
        .append("algorithmRunNanos=").append(record.getAlgorithmRunNanos()).append('\n')
        .append("freezeJarSha256=").append(parsed.jarSha256).append('\n')
        .append("freezeBinding=").append(parsed.freezeBinding).append('\n')
        .append("failures=").append(failures.isEmpty() ? "NONE" : join(failures, ";")).append('\n');
    Files.write(directory.resolve("preflight-gate.properties"),
        gate.toString().getBytes(StandardCharsets.UTF_8));
    String formalSummary = record.getMechanismSummary();
    String budgetText = new StringBuilder()
        .append("budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\n")
        .append("budgetProtocolVersion=").append(VERSION).append('\n')
        .append("requestedMaxFE=").append(budget.getRequestedMaxFE()).append('\n')
        .append("actualFE=").append(budget.getActualFE()).append('\n')
        .append("decoderCalls=").append(budget.getDecoderCalls()).append('\n')
        .append("remainingFE=").append(budget.getRemainingFE()).append('\n')
        .append("population=").append(budget.getPopulation()).append('\n')
        .append("qTimes=").append(budget.getQTimes()).append('\n')
        .append("qPhaseFE=").append(budget.getQPhaseFE()).append('\n')
        .append("utilizationRate=").append(String.format(Locale.ROOT, "%.12f",
            budget.getUtilizationRate())).append('\n')
        .append("terminationKind=").append(budget.getTerminationKind()).append('\n')
        .append("phaseBoundAccepted=").append(budget.isAccepted()).append('\n')
        .append("phaseBoundFailure=").append(budget.getFailureReason()).append('\n')
        .append("formalOuterCycles=").append(summaryValue(formalSummary, "formalOuterCycles"))
        .append('\n')
        .append("formalQgRounds=").append(summaryValue(formalSummary, "formalQgRounds"))
        .append('\n')
        .append("freezeJarSha256=").append(parsed.jarSha256).append('\n')
        .append("freezeBinding=").append(parsed.freezeBinding).append('\n').toString();
    Files.write(directory.resolve("budget-termination.properties"),
        budgetText.getBytes(StandardCharsets.UTF_8));
    String summary = formalSummary;
    StringBuilder csv = new StringBuilder("arm,status,requestedFE,actualFE,decoderCalls,illegal,duplicate,"
        + "qgSelections,pddrEvents,teacherUses,cfvfOffspring,qpActions,archiveInsertions,dualQP,dualQG,"
        + "caTaLiteTest,caTaLiteApply,formalLocalFE,cfvfRepairs,directionalPoolRequests,remainingFE,qPhaseFE,"
        + "utilizationRate,terminationKind,wallNanos,failures\n");
    csv.append(parsed.arm.getLabel()).append(',').append(status).append(',').append(parsed.maxFes)
        .append(',').append(record.getFullEvaluations()).append(',').append(record.getDecoderCalls())
        .append(',').append(record.getIllegalSolutions()).append(',').append(record.getDuplicateEvaluations());
    String[] keys = {"qgSelections", "pddrEvents", "teacherUses", "cfvfOffspring", "qpActions",
        "archiveInsertions", "dualQP", "dualQG", "caTaLiteTest", "caTaLiteApply", "formalLocalFE",
        "cfvfRepairs", "directionalPoolRequests"};
    for (String key : keys) csv.append(',').append(summaryValue(summary, key));
    csv.append(',').append(budget.getRemainingFE()).append(',').append(budget.getQPhaseFE())
        .append(',').append(String.format(Locale.ROOT, "%.12f", budget.getUtilizationRate()))
        .append(',').append(budget.getTerminationKind())
        .append(',').append(wallNanos).append(',').append(failures.isEmpty() ? "NONE" : quote(join(failures, ";")))
        .append('\n');
    Files.write(directory.resolve("preflight-summary.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String quote(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }
  private static String join(List<String> values, String separator) {
    StringBuilder result = new StringBuilder();
    for (String value : values) { if (result.length() > 0) result.append(separator); result.append(value); }
    return result.toString();
  }
  private static void add(List<String> failures, String message) {
    if (!failures.contains(message)) failures.add(message);
  }
  private static void requireFile(Path path, String label) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException(label + " missing: " + path);
  }
  private static void move(Path source, Path target) throws IOException {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (java.nio.file.AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }
  private static void writeHashes(Path root) throws Exception {
    Files.deleteIfExists(root.resolve("evidence-sha256.tsv"));
    Map<String, String> entries = new TreeMap<String, String>();
    java.util.stream.Stream<Path> stream = Files.walk(root);
    try {
      java.util.Iterator<Path> iterator = stream.filter(Files::isRegularFile).iterator();
      while (iterator.hasNext()) {
        Path file = iterator.next();
        entries.put(root.relativize(file).toString().replace('\\', '/'), sha256(file));
      }
    } finally { stream.close(); }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : entries.entrySet()) {
      text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(root.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path file) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
    StringBuilder text = new StringBuilder();
    for (byte value : digest) text.append(String.format(Locale.ROOT, "%02x", value & 0xff));
    return text.toString();
  }

  /**
   * Pure post-run classification for the frozen phase-based search.  It is
   * intentionally evaluated only after V35FairRunner.run returns, so enabling
   * this audit cannot influence algorithm state, random draws, FE, or fronts.
   */
  static final class BudgetTermination {
    private final long requestedMaxFE;
    private final long actualFE;
    private final long decoderCalls;
    private final long remainingFE;
    private final int population;
    private final int qTimes;
    private final long qPhaseFE;
    private final String terminationKind;
    private final String failureReason;

    private BudgetTermination(long requestedMaxFE, long actualFE, long decoderCalls,
        int population, int qTimes, String terminationKind, String failureReason) {
      this.requestedMaxFE = requestedMaxFE;
      this.actualFE = actualFE;
      this.decoderCalls = decoderCalls;
      this.remainingFE = requestedMaxFE - actualFE;
      this.population = population;
      this.qTimes = qTimes;
      this.qPhaseFE = (long) population * qTimes;
      this.terminationKind = terminationKind;
      this.failureReason = failureReason;
    }

    static BudgetTermination classify(long requestedMaxFE, long actualFE, long decoderCalls,
        int population, int qTimes) {
      if (requestedMaxFE <= 0) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "REQUESTED_MAX_FE_NON_POSITIVE");
      if (population <= 0 || qTimes <= 0) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "Q_PHASE_CONFIGURATION_NON_POSITIVE");
      if (actualFE <= 0) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "ZERO_OR_NEGATIVE_ACTUAL_FE");
      if (decoderCalls != actualFE) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "DECODER_CALLS_DO_NOT_CLOSE");
      if (actualFE > requestedMaxFE) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "OVER_MAX_FE");
      long remaining = requestedMaxFE - actualFE;
      long qPhaseFE = (long) population * qTimes;
      if (remaining == 0L) return new BudgetTermination(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "EXACT_MAX_FE", "NONE");
      if (remaining >= qPhaseFE) return invalid(requestedMaxFE, actualFE, decoderCalls,
          population, qTimes, "TAIL_NOT_SHORTER_THAN_Q_PHASE");
      return new BudgetTermination(requestedMaxFE, actualFE, decoderCalls, population, qTimes,
          "PHASE_CONSISTENT_TAIL_STOP", "NONE");
    }

    private static BudgetTermination invalid(long requestedMaxFE, long actualFE, long decoderCalls,
        int population, int qTimes, String failureReason) {
      return new BudgetTermination(requestedMaxFE, actualFE, decoderCalls, population, qTimes,
          "INVALID", failureReason);
    }

    boolean isAccepted() { return "NONE".equals(failureReason); }
    long getRequestedMaxFE() { return requestedMaxFE; }
    long getActualFE() { return actualFE; }
    long getDecoderCalls() { return decoderCalls; }
    long getRemainingFE() { return remainingFE; }
    int getPopulation() { return population; }
    int getQTimes() { return qTimes; }
    long getQPhaseFE() { return qPhaseFE; }
    double getUtilizationRate() { return requestedMaxFE <= 0 ? 0.0 : (double) actualFE / requestedMaxFE; }
    String getTerminationKind() { return terminationKind; }
    String getFailureReason() { return failureReason; }
  }

  private static final class Arguments {
    private Path projectRoot;
    private Path output;
    private V35FinalAblationProfile.Arm arm;
    private long seed = DEFAULT_SEED;
    private int maxFes = 50000;
    private String jarSha256 = "UNBOUND";
    private String freezeBinding = "CANDIDATE_JAR_NOT_PRODUCTION_EVIDENCE";
    private String purpose = "PREFLIGHT";
    private static Arguments parse(String[] args) {
      Arguments result = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw new IllegalArgumentException("missing value for " + args[index]);
        String key = args[index]; String value = args[index + 1];
        if ("--project-root".equals(key)) result.projectRoot = Paths.get(value);
        else if ("--output".equals(key)) result.output = Paths.get(value);
        else if ("--arm".equals(key)) result.arm = armForLabel(value);
        else if ("--seed".equals(key)) result.seed = Long.parseLong(value);
        else if ("--max-fes".equals(key)) result.maxFes = Integer.parseInt(value);
        else if ("--jar-sha256".equals(key)) result.jarSha256 = value;
        else if ("--freeze-binding".equals(key)) result.freezeBinding = value;
        else if ("--purpose".equals(key)) result.purpose = value;
        else throw new IllegalArgumentException("unknown option " + key);
      }
      if (result.projectRoot == null || result.output == null || result.arm == null) {
        throw new IllegalArgumentException("--project-root --output --arm are required");
      }
      if (!"PREFLIGHT".equals(result.purpose) && !"THROUGHPUT".equals(result.purpose)) {
        throw new IllegalArgumentException("--purpose must be PREFLIGHT or THROUGHPUT");
      }
      return result;
    }

  }
}
