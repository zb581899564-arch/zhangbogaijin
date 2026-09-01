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
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** Snapshot-bound external A0--A4 launcher.  It never creates an initial solution. */
public final class ZhangBoV35FormalAblationArmRunner {
  public static final String VERSION = "v35-formal-a0-a4-external-runner-v1";
  public static final String PLAN_SCHEMA = "v35-final-a0-a4-run-plan-v2";
  public static final String BUDGET_VERSION = "v35-phase-consistent-budget-v1";
  private static final int POPULATION = 100;
  private static final int Q_TIMES = 50;
  private static final Pattern NUMBER = Pattern.compile("(?:^|[,|])%s=(-?\\d+)(?:$|[,|])");

  private ZhangBoV35FormalAblationArmRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 4 || !"--plan".equals(args[0]) || !"--output".equals(args[2])) {
      throw new IllegalArgumentException("usage: --plan <properties> --output <directory>");
    }
    execute(Paths.get(args[1]), Paths.get(args[3]));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static void execute(Path planPath, Path output) throws Exception {
    Properties plan = load(planPath);
    requireEquals("schema", PLAN_SCHEMA, required(plan, "schema"));
    String purpose = required(plan, "purpose");
    if (!"SMOKE".equals(purpose) && !"GATE3".equals(purpose)
        && !"LAUNCHER_ACCEPTANCE".equals(purpose) && !"FORMAL".equals(purpose)) {
      throw new IllegalArgumentException("unsupported purpose=" + purpose);
    }
    V35FinalAblationProfile.Arm arm = arm(required(plan, "arm"));
    long seed = Long.parseLong(required(plan, "seed"));
    int population = Integer.parseInt(required(plan, "population"));
    int maxFes = Integer.parseInt(required(plan, "maxFEs"));
    if (population != POPULATION) throw new IllegalArgumentException("population must be 100");

    Path frozenJar = file(plan, "frozenJar");
    requireEquals("frozenJarSha256", required(plan, "frozenJarSha256"), sha256(frozenJar));
    Path instance = file(plan, "instancePath");
    Path setup = file(plan, "setupPath");
    Path fatigueFile = file(plan, "fatiguePath");
    requireEquals("instanceSha256", required(plan, "instanceSha256"), sha256(instance));
    requireEquals("setupFileSha256", required(plan, "setupFileSha256"), sha256(setup));
    requireEquals("fatigueFileSha256", required(plan, "fatigueFileSha256"), sha256(fatigueFile));
    Path setupDirectory = setup.getParent();
    Path fatigueDirectory = fatigueFile.getParent();

    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, seed, setupDirectory, fatigueDirectory,
        ZhangBoShiftConfiguration.none());
    requireEquals("runtimeInstanceSha256", required(plan, "instanceSha256"),
        problem.getInstance().getInstanceSha256());
    requireEquals("runtimeSetupSha256", required(plan, "setupConfigurationSha256"),
        problem.getInstance().getInstanceExtensionSha256());
    requireEquals("runtimeFatigueSha256", required(plan, "fatigueConfigurationSha256"),
        problem.getParameters().getConfigurationSha256());

    Path snapshot = file(plan, "snapshotPath");
    requireEquals("snapshotSha256", required(plan, "snapshotSha256"), sha256(snapshot));
    List<PermutationSolution<Integer>> initial =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, problem);
    if (initial.size() != population) throw new IllegalStateException("snapshot population mismatch");
    String v35Hash = V35FairRunner.initialHash(initial);
    String p8Hash = P8InitialPopulationProvider.sha256(initial);
    requireEquals("initialPopulationHashV35", required(plan, "initialPopulationHashV35"), v35Hash);
    requireEquals("initialPopulationHashP8", required(plan, "initialPopulationHashP8"), p8Hash);
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("snapshot load consumed FE");
    }

    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        arm, seed, population, maxFes);
    V35FinalAblationProfile.validate(arm, configuration);
    String profileText = V35FinalAblationProfile.canonicalTextFor(arm, seed, population, maxFes);
    String profileHash = V35FinalAblationProfile.configurationHashFor(arm, seed, population, maxFes);
    requireEquals("armProfileSha256", required(plan, "armProfileSha256"), profileHash);
    requireEquals("runtimeConfigurationSha256", required(plan, "runtimeConfigurationSha256"),
        configuration.configurationHash());

    Path target = output.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    Path partial = target.resolveSibling(".partial-" + target.getFileName() + "-" + System.nanoTime());
    Files.createDirectory(partial);
    boolean moved = false;
    try {
      long wallStart = System.nanoTime();
      V35FairRunner.RunRecord record = V35FairRunner.run(arm.getMode(), problem,
          P8InitialPopulationProvider.copy(initial), maxFes, seed, true,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
      long wallNanos = System.nanoTime() - wallStart;
      Budget budget = Budget.classify(maxFes, record.getFullEvaluations(), record.getDecoderCalls());
      List<String> failures = gate(arm, record, v35Hash, budget,
          "FORMAL".equals(purpose) || "LAUNCHER_ACCEPTANCE".equals(purpose));
      String canonical = "externalRunnerVersion=" + VERSION + "\n"
          + "planSchema=" + PLAN_SCHEMA + "\npurpose=" + purpose + "\nrunId="
          + required(plan, "runId") + "\narm=" + arm.getLabel() + "\nseed=" + seed
          + "\npopulation=" + population + "\nmaxFEs=" + maxFes
          + "\nbudgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\nbudgetProtocolVersion="
          + BUDGET_VERSION + "\nfrozenJarSha256=" + required(plan, "frozenJarSha256")
          + "\nsnapshotSha256=" + required(plan, "snapshotSha256")
          + "\ninitialPopulationHashV35=" + v35Hash + "\ninitialPopulationHashP8=" + p8Hash
          + "\nproblemConfigurationSha256=" + required(plan, "problemConfigurationSha256")
          + "\nprofileSha256=" + profileHash + "\nprofileCanonicalBegin\n" + profileText
          + "profileCanonicalEnd\n";
      V35FairRunner.writeRecord(record, partial, canonical);
      Files.write(partial.resolve("profile.txt"), profileText.getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("profile.sha256"), (profileHash + "  profile.txt\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population.sha256"),
          (v35Hash + "  V35\n" + p8Hash + "  P8\n").getBytes(StandardCharsets.UTF_8));
      writeBudget(partial, plan, record, budget);
      writeGate(partial, plan, arm, record, wallNanos, failures);
      writeManifest(partial);
      move(partial, target);
      moved = true;
      if (!failures.isEmpty()) throw new IllegalStateException("RUN_GATE_FAILED " + join(failures));
      System.out.println("V35_FORMAL_A0_A4_ARM_COMPLETED arm=" + arm.getLabel()
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

  private static List<String> gate(V35FinalAblationProfile.Arm arm,
      V35FairRunner.RunRecord record, String initialHash, Budget budget, boolean strict99) {
    List<String> failures = new ArrayList<String>();
    if (!"COMPLETED".equals(record.getStatus())) failures.add("status=" + record.getStatus());
    if (!initialHash.equals(record.getInitialPopulationHash())) failures.add("initialPopulationDrift");
    if (!budget.accepted) failures.add("budget=" + budget.failure);
    if (strict99 && budget.utilization() <= 0.99) failures.add("utilizationRate<=0.99");
    if (record.getIllegalSolutions() != 0) failures.add("illegal=" + record.getIllegalSolutions());
    if (record.getDuplicateEvaluations() != 0) failures.add("duplicate=" + record.getDuplicateEvaluations());
    if (record.getPassiveObservedCount() != record.getFullEvaluations()) failures.add("sourceObservationLoss");
    if (record.getFront().isEmpty()) failures.add("emptyFront");
    Set<String> points = new HashSet<String>();
    for (double[] point : record.getFront()) {
      if (point.length != 3) failures.add("objectiveDimension");
      for (double value : point) if (!Double.isFinite(value)) failures.add("nonFiniteFront");
      String fingerprint = point.length < 3 ? "invalid" : point[0] + "|" + point[1] + "|" + point[2];
      if (!points.add(fingerprint)) failures.add("duplicateFrontPoint");
    }
    ZhangBoDecoderTimingSnapshot timing = record.getDecoderTiming();
    if (timing.getSuccessfulDecoderCalls() != record.getFullEvaluations()) failures.add("decoderSnapshotMismatch");
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L
        || timing.getLeftFullRecomputations() != 0L || timing.getRightFullRecomputations() != 0L) {
      failures.add("shiftActivity");
    }
    String summary = record.getMechanismSummary();
    zero(failures, summary, "cfvfRepairs"); zero(failures, summary, "directionalPoolRequests");
    zero(failures, summary, "directionalPoolFiltered"); zero(failures, summary, "shadowSamples");
    zero(failures, summary, "shadowEvaluations");
    // A 2k wiring smoke is intentionally shorter than one 5k atomic Q phase.
    // Mechanism activation is therefore gated only once at least one complete
    // Q phase fits; the smoke still proves snapshot, decoder and output wiring.
    if (budget.requested >= budget.qPhase) {
      positive(failures, summary, "qgSelections"); positive(failures, summary, "pddrEvents");
      expected(failures, summary, "baselineUpdateEvents", !arm.isCfvfEnabled());
      expected(failures, summary, "cfvfOffspring", arm.isCfvfEnabled());
      expected(failures, summary, "qpActions", arm.isQpEnabled());
      expected(failures, summary, "qpTransitions", arm.isQpEnabled());
      expected(failures, summary, "archiveInsertions", arm.isQpEnabled());
      expected(failures, summary, "caTaLiteTest", arm.isCaTaLiteEnabled());
      expected(failures, summary, "caTaLiteApply", arm.isCaTaLiteEnabled());
      if (arm.isDscrEnabled()) {
        positive(failures, summary, "teacherUses"); positive(failures, summary, "validityChecks");
        zero(failures, summary, "dominatedTeacherUses");
      } else {
        zero(failures, summary, "teacherUses"); zero(failures, summary, "validityChecks");
        zero(failures, summary, "replacements");
      }
    }
    return failures;
  }

  private static void writeBudget(Path dir, Properties plan, V35FairRunner.RunRecord record,
      Budget budget) throws IOException {
    String summary = record.getMechanismSummary();
    String text = "budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION\n"
        + "budgetProtocolVersion=" + BUDGET_VERSION + "\nrequestedMaxFE=" + budget.requested
        + "\nactualFE=" + budget.actual + "\ndecoderCalls=" + budget.decoder
        + "\nremainingFE=" + budget.remaining + "\nqPhaseFE=" + budget.qPhase
        + "\nutilizationRate=" + String.format(Locale.ROOT, "%.12f", budget.utilization())
        + "\nterminationKind=" + budget.kind + "\nphaseBoundAccepted=" + budget.accepted
        + "\nphaseBoundFailure=" + budget.failure
        + "\nformalOuterCycles=" + value(summary, "formalOuterCycles")
        + "\nformalQgRounds=" + value(summary, "formalQgRounds")
        + "\nfrozenJarSha256=" + required(plan, "frozenJarSha256")
        + "\narmProfileSha256=" + required(plan, "armProfileSha256")
        + "\nsnapshotSha256=" + required(plan, "snapshotSha256")
        + "\nproblemConfigurationSha256=" + required(plan, "problemConfigurationSha256") + "\n";
    Files.write(dir.resolve("budget-termination.properties"), text.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeGate(Path dir, Properties plan, V35FinalAblationProfile.Arm arm,
      V35FairRunner.RunRecord record, long wallNanos, List<String> failures) throws IOException {
    String text = "runnerVersion=" + VERSION + "\nstatus=" + (failures.isEmpty() ? "COMPLETED" : "FAILED")
        + "\nrunId=" + required(plan, "runId") + "\narm=" + arm.getLabel()
        + "\nactualFE=" + record.getFullEvaluations() + "\ndecoderCalls=" + record.getDecoderCalls()
        + "\nfrontSize=" + record.getFront().size() + "\nwallNanos=" + wallNanos
        + "\nlauncherAcceptanceOnly=" + required(plan, "launcherAcceptanceOnly")
        + "\nincludedInFormalStatistics=" + required(plan, "includedInFormalStatistics")
        + "\nincludedInReferenceFront=" + required(plan, "includedInReferenceFront")
        + "\nfailures=" + (failures.isEmpty() ? "NONE" : join(failures)) + "\n";
    Files.write(dir.resolve("formal-gate.properties"), text.getBytes(StandardCharsets.UTF_8));
    String provenance = "frozenJarSha256=" + required(plan, "frozenJarSha256")
        + "\narmProfileSha256=" + required(plan, "armProfileSha256")
        + "\nsnapshotSha256=" + required(plan, "snapshotSha256")
        + "\ninitialPopulationHashV35=" + required(plan, "initialPopulationHashV35")
        + "\ninitialPopulationHashP8=" + required(plan, "initialPopulationHashP8")
        + "\ninstanceSha256=" + required(plan, "instanceSha256")
        + "\nsetupFileSha256=" + required(plan, "setupFileSha256")
        + "\nfatigueFileSha256=" + required(plan, "fatigueFileSha256")
        + "\nsetupConfigurationSha256=" + required(plan, "setupConfigurationSha256")
        + "\nfatigueConfigurationSha256=" + required(plan, "fatigueConfigurationSha256")
        + "\nproblemConfigurationSha256=" + required(plan, "problemConfigurationSha256") + "\n";
    Files.write(dir.resolve("provenance.properties"), provenance.getBytes(StandardCharsets.UTF_8));
  }

  private static void expected(List<String> failures, String text, String key, boolean enabled) {
    if (enabled) positive(failures, text, key); else zero(failures, text, key);
  }
  private static void positive(List<String> failures, String text, String key) {
    long n = value(text, key); if (n <= 0) failures.add(key + "=" + n + " expected>0");
  }
  private static void zero(List<String> failures, String text, String key) {
    long n = value(text, key); if (n != 0) failures.add(key + "=" + n + " expected=0");
  }
  private static long value(String text, String key) {
    Matcher matcher = Pattern.compile(String.format(Locale.ROOT, NUMBER.pattern(), Pattern.quote(key)))
        .matcher(text == null ? "" : text);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
  }
  private static V35FinalAblationProfile.Arm arm(String label) {
    for (V35FinalAblationProfile.Arm value : V35FinalAblationProfile.Arm.values()) {
      if (value.getLabel().equals(label)) return value;
    }
    throw new IllegalArgumentException("arm must be A0,A1,A2,A3,A4: " + label);
  }
  private static Properties load(Path path) throws IOException {
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(path)) {
      properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
    return properties;
  }
  private static String required(Properties p, String key) {
    String value = p.getProperty(key); if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("missing plan key=" + key);
    } return value.trim();
  }
  private static Path file(Properties p, String key) {
    Path path = Paths.get(required(p, key)).toAbsolutePath().normalize();
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing file " + key + "=" + path);
    return path;
  }
  private static void requireEquals(String field, String expected, String actual) {
    if (!expected.equalsIgnoreCase(actual)) throw new IllegalStateException(field + " expected=" + expected + " actual=" + actual);
  }
  private static String join(List<String> values) {
    StringBuilder out = new StringBuilder(); for (String value : values) { if (out.length() > 0) out.append(';'); out.append(value); } return out.toString();
  }
  private static void move(Path source, Path target) throws IOException {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }
  private static String sha256(Path file) throws Exception {
    return sha256(Files.readAllBytes(file));
  }
  private static String sha256(byte[] bytes) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff)); return out.toString();
  }
  private static void writeManifest(Path root) throws Exception {
    Files.deleteIfExists(root.resolve("evidence-sha256.tsv"));
    Map<String, String> rows = new TreeMap<String, String>();
    try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
      java.util.Iterator<Path> iterator = stream.filter(Files::isRegularFile).iterator();
      while (iterator.hasNext()) { Path file = iterator.next(); rows.put(root.relativize(file).toString().replace('\\', '/'), sha256(file)); }
    }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> row : rows.entrySet()) text.append(row.getKey()).append('\t').append(row.getValue()).append('\n');
    Files.write(root.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  static final class Budget {
    final long requested, actual, decoder, remaining, qPhase; final String kind, failure; final boolean accepted;
    private Budget(long requested, long actual, long decoder, String kind, String failure) {
      this.requested=requested; this.actual=actual; this.decoder=decoder; this.remaining=requested-actual;
      this.qPhase=(long) POPULATION*Q_TIMES; this.kind=kind; this.failure=failure; this.accepted="NONE".equals(failure);
    }
    static Budget classify(long requested, long actual, long decoder) {
      if (requested<=0) return new Budget(requested,actual,decoder,"INVALID","REQUESTED_NON_POSITIVE");
      if (actual<=0) return new Budget(requested,actual,decoder,"INVALID","ACTUAL_NON_POSITIVE");
      if (decoder!=actual) return new Budget(requested,actual,decoder,"INVALID","DECODER_MISMATCH");
      if (actual>requested) return new Budget(requested,actual,decoder,"INVALID","OVER_BUDGET");
      long remaining=requested-actual; if (remaining>=(long)POPULATION*Q_TIMES) return new Budget(requested,actual,decoder,"INVALID","TAIL_TOO_LARGE");
      return new Budget(requested,actual,decoder,remaining==0?"EXACT_MAX_FE":"PHASE_CONSISTENT_TAIL_STOP","NONE");
    }
    double utilization() { return requested<=0 ? 0.0 : (double)actual/requested; }
  }
}
