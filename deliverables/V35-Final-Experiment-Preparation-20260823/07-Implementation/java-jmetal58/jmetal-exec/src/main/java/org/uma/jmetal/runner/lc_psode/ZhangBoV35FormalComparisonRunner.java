package org.uma.jmetal.runner.lc_psode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixture;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Fail-closed, single-arm launcher for the future V35 versus HMOPSO-QGS-F
 * formal comparison. It deliberately has no scheduler and never creates a
 * reference front. Each invocation handles one arm in one JVM only after all
 * external freeze gates, source hashes and fairness inputs have been checked.
 */
public final class ZhangBoV35FormalComparisonRunner {
  public static final String VERSION = "v35-formal-comparison-gate-v1";
  private static final String PLAN_SCHEMA = "v35-formal-comparison-plan-v1";
  private static final String A0 = "HMOPSO_QGS_F";
  private static final String MAIN = "V35_MAIN";

  private ZhangBoV35FormalComparisonRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    Readiness readiness = validate(arguments.plan);
    writeReadiness(arguments.report, readiness);
    if (!readiness.isReady()) {
      System.out.println("V35_FORMAL_COMPARISON_BLOCKED blockers=" + readiness.getBlockers().size());
      if (arguments.execute) throw new IllegalStateException("formal comparison is not ready");
      return;
    }
    if (!arguments.execute) {
      System.out.println("V35_FORMAL_COMPARISON_READY_NOT_EXECUTED");
      return;
    }
    execute(readiness.getPlan(), arguments.arm, arguments.output);
  }

  /** Public for a no-FE readiness test. A non-empty blocker list is a pass for a blocked plan. */
  public static Readiness validate(Path planFile) throws Exception {
    Plan plan = Plan.load(planFile);
    List<String> blockers = new ArrayList<>();
    required(plan, blockers, "schema", PLAN_SCHEMA);
    required(plan, blockers, "campaign", "V35_VS_HMOPSO_QGS_F");
    required(plan, blockers, "execution.authorized", "true");
    required(plan, blockers, "gate.fc8.champion", "APPROVED");
    required(plan, blockers, "gate.exp1.mainVariantFrozen", "true");
    required(plan, blockers, "gate.formalMatrixAuthorized", "true");
    required(plan, blockers, "formal.seedListFrozen", "true");
    required(plan, blockers, "formal.algorithmSetFrozen", "true");
    required(plan, blockers, "run.id", null);
    required(plan, blockers, "run.instance", null);
    required(plan, blockers, "run.seed", null);
    required(plan, blockers, "run.initialPopulationHash.v35", null);
    required(plan, blockers, "run.initialPopulationHash.p8", null);
    required(plan, blockers, "run.fairnessContractSha256", null);
    required(plan, blockers, "run.population", "100");
    required(plan, blockers, "run.maxFEs", "500000");
    required(plan, blockers, "run.decoderMode", "FM3");
    required(plan, blockers, "run.familyMode", "DEGENERATE_SINGLE_FAMILY");
    required(plan, blockers, "run.setupMode", "SEQUENCE_INDEPENDENT");
    required(plan, blockers, "run.shiftMode", "NONE");
    required(plan, blockers, "run.objectives", "0,1,6");
    required(plan, blockers, "run.pddrSelectionMode", "GLOBAL_ORIGINAL");
    required(plan, blockers, "run.localSearchOrder", "CATA_THEN_INHERITED");
    required(plan, blockers, "run.pressureMode", "BAL_FULL_OPEN");
    required(plan, blockers, "run.directionalTeacherPool", "false");
    required(plan, blockers, "run.softFreezeRho", "0");
    required(plan, blockers, "run.subSwarmMixture", "20/40/20/20");
    required(plan, blockers, "run.a0DualQ", "NONE");
    required(plan, blockers, "run.mainDualQ", "BLOCK_FROZEN:0.10:5:5");

    requireSha(plan, blockers, "run.initialPopulationHash.v35");
    requireSha(plan, blockers, "run.initialPopulationHash.p8");
    requireSha(plan, blockers, "run.fairnessContractSha256");
    requireLong(plan, blockers, "run.seed");

    Path seedList = plan.path("formal.seedList", blockers);
    validateSeedList(seedList, plan, blockers);
    Path roster = plan.path("formal.algorithmRoster", blockers);
    validateRoster(roster, plan, blockers);
    validateHashBoundFile(plan, "formal.sourceManifest", "formal.sourceManifestSha256", blockers);
    validateHashBoundFile(plan, "run.instancePath", "run.instanceSha256", blockers);
    validateHashBoundFile(plan, "run.sutExtensionPath", "run.sutExtensionSha256", blockers);
    validateHashBoundFile(plan, "run.fatigueParameterPath", "run.fatigueParameterSha256", blockers);
    validateDirectory(plan, "run.sutExtensionDirectory", blockers);
    validateDirectory(plan, "run.fatigueParameterDirectory", blockers);
    validateConfig(plan, A0, V35FairRunner.Mode.V35_BASELINE, blockers);
    validateConfig(plan, MAIN, V35FairRunner.Mode.V35_FULL_POOL_OFF, blockers);
    return new Readiness(plan, blockers);
  }

  private static void execute(Plan plan, String arm, Path output) throws Exception {
    if (!A0.equals(arm) && !MAIN.equals(arm)) {
      throw new IllegalArgumentException("--arm must be " + A0 + " or " + MAIN);
    }
    Path target = output.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    Path partial = target.getParent().resolve(".partial-" + arm + '-' + System.nanoTime());
    Files.createDirectory(partial);
    try {
      long seed = Long.parseLong(plan.value("run.seed"));
      int population = Integer.parseInt(plan.value("run.population"));
      int maxFes = Integer.parseInt(plan.value("run.maxFEs"));
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          plan.path("run.instancePath", new ArrayList<String>()), ProductionDecodeMode.FM3, seed,
          plan.path("run.sutExtensionDirectory", new ArrayList<String>()),
          plan.path("run.fatigueParameterDirectory", new ArrayList<String>()),
          ZhangBoShiftConfiguration.none());
      verifyProblemProvenance(plan, problem);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < population; index++) initial.add(problem.createSolution());
      String v35InitialHash = V35FairRunner.initialHash(initial);
      String p8InitialHash = P8InitialPopulationProvider.sha256(initial);
      requireEquals("run.initialPopulationHash.v35", plan.value("run.initialPopulationHash.v35"),
          v35InitialHash);
      requireEquals("run.initialPopulationHash.p8", plan.value("run.initialPopulationHash.p8"),
          p8InitialHash);
      String fairness = fairnessContract(plan, v35InitialHash, p8InitialHash,
          problem.getInstance().getInstanceSha256(), problem.getInstance().getInstanceExtensionSha256(),
          problem.getParameters().getConfigurationSha256());
      requireEquals("run.fairnessContractSha256", plan.value("run.fairnessContractSha256"), fairness);
      V35ProductionConfiguration configuration = runtimeConfiguration(plan, arm, seed, population, maxFes);
      Properties frozen = loadProperties(plan.path("arm." + arm + ".config", new ArrayList<String>()));
      requireEquals("runtime.configuration.sha256", frozen.getProperty("runtime.configuration.sha256"),
          configuration.configurationHash());
      V35FairRunner.Mode mode = A0.equals(arm)
          ? V35FairRunner.Mode.V35_BASELINE : V35FairRunner.Mode.V35_FULL_POOL_OFF;
      V35FairRunner.RunRecord record = V35FairRunner.run(mode, problem, copy(initial), maxFes, seed,
          true, V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
      postRunGate(record, maxFes, arm);
      String canonical = new String(Files.readAllBytes(plan.path("arm." + arm + ".config",
          new ArrayList<String>())), StandardCharsets.UTF_8)
          + "formalRunnerVersion=" + VERSION + '\n'
          + "formalRunId=" + plan.value("run.id") + '\n'
          + "fairnessContractSha256=" + fairness + '\n'
          + "initialPopulationHashV35=" + v35InitialHash + '\n'
          + "initialPopulationHashP8=" + p8InitialHash + '\n';
      V35FairRunner.writeRecord(record, partial, canonical);
      Files.write(partial.resolve("formal-gate.properties"), (
          "formalRunnerVersion=" + VERSION + "\nstatus=COMPLETED\narm=" + arm
          + "\nrunId=" + plan.value("run.id") + "\nfairnessContractSha256=" + fairness
          + "\ninitialPopulationHashV35=" + v35InitialHash
          + "\ninitialPopulationHashP8=" + p8InitialHash + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("provenance.properties"), provenance(plan, problem).getBytes(StandardCharsets.UTF_8));
      writeManifest(partial);
      move(partial, target);
      System.out.println("V35_FORMAL_COMPARISON_ARM_COMPLETED arm=" + arm + " runId="
          + plan.value("run.id") + " FE=" + record.getFullEvaluations());
    } catch (Exception error) {
      Files.write(partial.resolve("failure.txt"), (error.getClass().getName() + ": "
          + error.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
      writeManifest(partial);
      throw error;
    }
  }

  private static V35ProductionConfiguration runtimeConfiguration(Plan plan, String arm,
      long seed, int population, int maxFes) {
    return runtimeConfigurationForTest(arm, seed, population, maxFes);
  }

  /** Package-visible only for the no-FE fairness configuration regression. */
  static V35ProductionConfiguration runtimeConfigurationForTest(String arm,
      long seed, int population, int maxFes) {
    boolean main = MAIN.equals(arm);
    V35ProductionConfiguration.Builder builder = V35ProductionConfiguration.builder()
        .seed(seed).populationSize(population).maxEvaluations(maxFes)
        .decoderMode(ProductionDecodeMode.FM3)
        .qg(true).dscr(main).cfvf(main).qp(main).caTaLite(main)
        .directionalTeacherPool(false).teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED)
        .subSwarmMixture(V35SubSwarmMixture.BASELINE);
    // Dual-Q is part of the A4 candidate only.  Supplying even an inactive
    // coordination object to A0 would contradict its declared fair-baseline
    // mechanism profile.
    if (main) {
      builder.dualQCoordination(ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 5, 5));
      builder.localFeBudget(V35LocalFeBudgetConfiguration.of(0.25, 0.65));
    }
    return builder.build();
  }

  private static void verifyProblemProvenance(Plan plan, ZhangBoCanonicalProductionProblem problem) {
    requireEquals("run.instanceSha256", plan.value("run.instanceSha256"),
        problem.getInstance().getInstanceSha256());
    requireEquals("run.sutExtensionSha256", plan.value("run.sutExtensionSha256"),
        problem.getInstance().getInstanceExtensionSha256());
    requireEquals("run.fatigueParameterSha256", plan.value("run.fatigueParameterSha256"),
        problem.getParameters().getConfigurationSha256());
  }

  private static void postRunGate(V35FairRunner.RunRecord record, int maxFes, String arm) {
    if (!"COMPLETED".equals(record.getStatus())) throw new IllegalStateException("RUN_STATUS=" + record.getStatus());
    // A legal tail boundary may safely stop before MaxFEs rather than create
    // a partial Q batch/local window.  Preserve the actual FE; do not pretend
    // it reached the nominal cap.
    if (record.getFullEvaluations() <= 0 || record.getFullEvaluations() > maxFes
        || record.getDecoderCalls() != record.getFullEvaluations()) {
      throw new IllegalStateException("FE_DECODER_CLOSURE=" + record.getFullEvaluations() + '/' + record.getDecoderCalls());
    }
    if (record.getIllegalSolutions() != 0 || record.getDuplicateEvaluations() != 0) {
      throw new IllegalStateException("ILLEGAL_OR_DUPLICATE=" + record.getIllegalSolutions()
          + '/' + record.getDuplicateEvaluations());
    }
    if (record.getDecoderTiming().getLeftShiftNanos() != 0L
        || record.getDecoderTiming().getRightShiftNanos() != 0L
        || record.getDecoderTiming().getLeftAccepted() != 0L
        || record.getDecoderTiming().getRightAccepted() != 0L) {
      throw new IllegalStateException("SHIFT_ACTIVITY_DETECTED");
    }
    if (record.getFront().isEmpty()) throw new IllegalStateException("EMPTY_FRONT");
    Set<String> points = new HashSet<>();
    for (double[] point : record.getFront()) {
      if (point.length != 3) throw new IllegalStateException("OBJECTIVE_DIMENSION");
      for (double value : point) if (!Double.isFinite(value)) throw new IllegalStateException("NON_FINITE_FRONT");
      String id = Long.toHexString(Double.doubleToLongBits(point[0])) + ':'
          + Long.toHexString(Double.doubleToLongBits(point[1])) + ':'
          + Long.toHexString(Double.doubleToLongBits(point[2]));
      if (!points.add(id)) throw new IllegalStateException("DUPLICATE_FRONT_POINT");
    }
    String summary = record.getMechanismSummary();
    requirePositive(summary, "qgSelections");
    requirePositive(summary, "pddrEvents");
    if (A0.equals(arm)) {
      requirePositive(summary, "baselineUpdateEvents");
      requirePositive(summary, "fixedNeighborhoodEvents");
    } else {
      requirePositive(summary, "cfvfOffspring");
      requirePositive(summary, "qpActions");
      requirePositive(summary, "archiveInsertions");
      requirePositive(summary, "caTaLiteTest");
      requirePositive(summary, "caTaLiteApply");
      if (!summary.contains("dominatedTeacherUses=0")) throw new IllegalStateException("DTUR_NONZERO");
    }
  }

  private static void validateConfig(Plan plan, String arm, V35FairRunner.Mode mode,
      List<String> blockers) throws Exception {
    String prefix = "arm." + arm + '.';
    Path configFile = plan.path(prefix + "config", blockers);
    validateHashBoundFile(plan, prefix + "config", prefix + "configFileSha256", blockers);
    if (configFile == null || !Files.isRegularFile(configFile)) return;
    Properties config = loadProperties(configFile);
    required(config, blockers, prefix + "frozen", "true", "frozen");
    required(config, blockers, prefix + "algorithmRole", arm, "algorithmRole");
    required(config, blockers, prefix + "runnerMode", mode.name(), "runnerMode");
    required(config, blockers, prefix + "decoderMode", "FM3", "decoderMode");
    required(config, blockers, prefix + "familyMode", "DEGENERATE_SINGLE_FAMILY", "familyMode");
    required(config, blockers, prefix + "setupMode", "SEQUENCE_INDEPENDENT", "setupMode");
    required(config, blockers, prefix + "shiftMode", "NONE", "shiftMode");
    required(config, blockers, prefix + "objectives", "0,1,6", "objectives");
    required(config, blockers, prefix + "pddrSelectionMode", "GLOBAL_ORIGINAL", "pddrSelectionMode");
    required(config, blockers, prefix + "localSearchOrder", "CATA_THEN_INHERITED", "localSearchOrder");
    required(config, blockers, prefix + "pressureMode", "BAL_FULL_OPEN", "pressureMode");
    required(config, blockers, prefix + "directionalTeacherPool", "false", "directionalTeacherPool");
    required(config, blockers, prefix + "softFreezeRho", "0", "softFreezeRho");
    required(config, blockers, prefix + "subSwarmMixture", "20/40/20/20", "subSwarmMixture");
    if (A0.equals(arm)) {
      required(config, blockers, prefix + "mechanismProfile", "A0_HMOPSO_QGS_F_FAIR_ADAPTATION", "mechanismProfile");
      required(config, blockers, prefix + "localFeBudget", "NONE", "localFeBudget");
    } else {
      required(config, blockers, prefix + "mechanismProfile", "A4_PACING_CANDIDATE", "mechanismProfile");
      required(config, blockers, prefix + "localFeBudget", "0.250000/0.650000", "localFeBudget");
      required(config, blockers, prefix + "dualQ", "BLOCK_FROZEN:0.10:5:5", "dualQ");
    }
    String runtimeHash = config.getProperty("runtime.configuration.sha256", "");
    if (!sha(runtimeHash)) blockers.add(prefix + "runtime.configuration.sha256 is not frozen");
  }

  private static void validateSeedList(Path path, Plan plan, List<String> blockers) throws Exception {
    if (path == null || !Files.isRegularFile(path)) return;
    validateHashBoundFile(plan, "formal.seedList", "formal.seedListSha256", blockers);
    Properties values = loadProperties(path);
    required(values, blockers, "formal.seedList.frozen", "true", "frozen");
    List<String> seeds = new ArrayList<>();
    for (String name : values.stringPropertyNames()) if (name.startsWith("seed.")) seeds.add(values.getProperty(name));
    Collections.sort(seeds);
    if (seeds.size() != 20 || new HashSet<String>(seeds).size() != 20) {
      blockers.add("formal seed list must contain exactly 20 unique seed.N entries");
    }
    if (seeds.size() == 20 && !seeds.contains(plan.value("run.seed"))) {
      blockers.add("run.seed is absent from the frozen 20-seed list");
    }
  }

  private static void validateRoster(Path path, Plan plan, List<String> blockers) throws Exception {
    if (path == null || !Files.isRegularFile(path)) return;
    validateHashBoundFile(plan, "formal.algorithmRoster", "formal.algorithmRosterSha256", blockers);
    Properties values = loadProperties(path);
    required(values, blockers, "formal.algorithmRoster.frozen", "true", "frozen");
    required(values, blockers, "formal.algorithmRoster." + A0, "V35_BASELINE", A0);
    String main = values.getProperty("V35_MAIN", "");
    if (unfrozen(main)) blockers.add("formal algorithm roster has no frozen V35_MAIN variant");
  }

  private static String fairnessContract(Plan plan, String v35InitialHash, String p8InitialHash,
      String instanceHash, String sutHash, String fatigueHash) {
    String text = "schema=v35-formal-fairness-contract-v1\n"
        + "instance=" + plan.value("run.instance") + '\n'
        + "instanceSha256=" + instanceHash + '\n'
        + "sutExtensionSha256=" + sutHash + '\n'
        + "fatigueParameterSha256=" + fatigueHash + '\n'
        + "seed=" + plan.value("run.seed") + '\n'
        + "initialPopulationHashV35=" + v35InitialHash + '\n'
        + "initialPopulationHashP8=" + p8InitialHash + '\n'
        + "population=100\nmaxFEs=500000\ndecoder=FM3\n"
        + "family=DEGENERATE_SINGLE_FAMILY\nsetup=SEQUENCE_INDEPENDENT\nshift=NONE\n"
        + "objectives=0,1,6\npddr=GLOBAL_ORIGINAL\nlocalSearchOrder=CATA_THEN_INHERITED\n"
        + "pressure=BAL_FULL_OPEN\nmixture=20/40/20/20\n";
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String provenance(Plan plan, ZhangBoCanonicalProductionProblem problem) {
    return "instanceId=" + plan.value("run.instance") + '\n'
        + "instanceSha256=" + problem.getInstance().getInstanceSha256() + '\n'
        + "sutExtensionSha256=" + problem.getInstance().getInstanceExtensionSha256() + '\n'
        + "fatigueParameterSha256=" + problem.getParameters().getConfigurationSha256() + '\n'
        + "sourceManifestSha256=" + plan.value("formal.sourceManifestSha256") + '\n'
        + "semanticLabel=deterministic_canonical\n"
        + "baselineLabel=HMOPSO-QGS-F fair adaptation; not Li Mingzhe original algorithm\n";
  }

  private static void validateHashBoundFile(Plan plan, String pathKey, String hashKey,
      List<String> blockers) throws Exception {
    Path file = plan.path(pathKey, blockers);
    String expected = plan.value(hashKey);
    if (file == null || !Files.isRegularFile(file)) return;
    if (!sha(expected)) { blockers.add(hashKey + " is not a frozen SHA-256"); return; }
    String actual = sha256(Files.readAllBytes(file));
    if (!expected.equalsIgnoreCase(actual)) blockers.add(hashKey + " mismatch");
  }

  private static void validateDirectory(Plan plan, String key, List<String> blockers) {
    Path directory = plan.path(key, blockers);
    if (directory != null && !Files.isDirectory(directory)) blockers.add(key + " is not a directory");
  }

  private static void required(Plan plan, List<String> blockers, String key, String expected) {
    String actual = plan.value(key);
    if (expected == null) {
      if (unfrozen(actual)) blockers.add(key + " is not frozen");
    } else if (!expected.equals(actual)) blockers.add(key + " expected=" + expected + " actual=" + actual);
  }
  private static void required(Properties values, List<String> blockers, String label,
      String expected, String property) {
    String actual = values.getProperty(property, "");
    if (!expected.equals(actual)) blockers.add(label + " expected=" + expected + " actual=" + actual);
  }
  private static void requireSha(Plan plan, List<String> blockers, String key) {
    if (!sha(plan.value(key))) blockers.add(key + " is not a frozen SHA-256");
  }
  private static void requireLong(Plan plan, List<String> blockers, String key) {
    try { Long.parseLong(plan.value(key)); } catch (Exception error) { blockers.add(key + " is not a long"); }
  }
  private static boolean unfrozen(String value) {
    return value == null || value.trim().isEmpty() || value.contains("UNFROZEN") || value.contains("TODO");
  }
  private static boolean sha(String value) { return value != null && value.matches("(?i)[0-9a-f]{64}"); }
  private static void requireEquals(String name, String expected, String actual) {
    if (expected == null || !expected.equalsIgnoreCase(actual)) {
      throw new IllegalStateException(name + " mismatch expected=" + expected + " actual=" + actual);
    }
  }
  private static void requirePositive(String summary, String key) {
    String marker = key + '='; int begin = summary.indexOf(marker);
    int end = begin < 0 ? -1 : summary.indexOf(',', begin);
    if (begin < 0) throw new IllegalStateException("missing mechanism=" + key);
    if (end < 0) end = summary.length();
    long value = Long.parseLong(summary.substring(begin + marker.length(), end));
    if (value <= 0L) throw new IllegalStateException("mechanism not triggered=" + key);
  }
  private static List<PermutationSolution<Integer>> copy(List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> copy = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) copy.add((PermutationSolution<Integer>) value.copy());
    return copy;
  }
  private static Properties loadProperties(Path path) throws Exception {
    Properties values = new Properties();
    try (InputStream input = Files.newInputStream(path)) { values.load(input); }
    return values;
  }
  private static void writeReadiness(Path report, Readiness readiness) throws Exception {
    StringBuilder out = new StringBuilder("# V35 Formal Comparison Readiness\n\n")
        .append("runnerVersion=").append(VERSION).append("\n\n")
        .append("status=").append(readiness.isReady() ? "READY" : "BLOCKED").append("\n\n");
    if (readiness.isReady()) out.append("All freeze, fairness and provenance gates passed. No run was started by this validation.\n");
    else {
      out.append("## Blocking conditions\n\n");
      for (String blocker : readiness.getBlockers()) out.append("- ").append(blocker).append('\n');
    }
    Files.createDirectories(report.toAbsolutePath().normalize().getParent());
    Files.write(report, out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static void writeManifest(Path root) throws Exception {
    TreeMap<String, String> rows = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
          .forEach(path -> { try { rows.put(root.relativize(path).toString().replace('\\', '/'), sha256(Files.readAllBytes(path))); }
            catch (Exception error) { throw new RuntimeException(error); } });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (java.util.Map.Entry<String, String> row : rows.entrySet()) out.append(row.getKey()).append('\t').append(row.getValue()).append('\n');
    Files.write(root.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static void move(Path source, Path target) throws Exception {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }
  private static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (Exception error) { throw new IllegalStateException(error); }
  }

  public static final class Readiness {
    private final Plan plan; private final List<String> blockers;
    private Readiness(Plan plan, List<String> blockers) { this.plan = plan; this.blockers = new ArrayList<>(blockers); }
    public boolean isReady() { return blockers.isEmpty(); }
    public List<String> getBlockers() { return Collections.unmodifiableList(blockers); }
    private Plan getPlan() { return plan; }
  }
  private static final class Plan {
    private final Path file; private final Properties values;
    private Plan(Path file, Properties values) { this.file = file; this.values = values; }
    private static Plan load(Path path) throws Exception { return new Plan(path.toAbsolutePath().normalize(), loadProperties(path)); }
    private String value(String key) { return values.getProperty(key, "").trim(); }
    private Path path(String key, List<String> blockers) {
      String raw = value(key);
      if (unfrozen(raw)) { blockers.add(key + " is not frozen"); return null; }
      Path result = Paths.get(raw); if (!result.isAbsolute()) result = file.getParent().resolve(result);
      result = result.normalize(); if (!Files.exists(result)) blockers.add(key + " missing=" + result);
      return result;
    }
  }
  private static final class Arguments {
    private Path plan; private Path report; private Path output; private String arm; private boolean execute;
    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index++) {
        if ("--plan".equals(args[index]) && index + 1 < args.length) value.plan = Paths.get(args[++index]);
        else if ("--readiness-report".equals(args[index]) && index + 1 < args.length) value.report = Paths.get(args[++index]);
        else if ("--execute".equals(args[index])) value.execute = true;
        else if ("--arm".equals(args[index]) && index + 1 < args.length) value.arm = args[++index];
        else if ("--output".equals(args[index]) && index + 1 < args.length) value.output = Paths.get(args[++index]);
        else throw usage();
      }
      if (value.plan == null || value.report == null) throw usage();
      if (value.execute && (value.arm == null || value.output == null)) throw usage();
      return value;
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --plan <properties> --readiness-report <md> [--execute --arm HMOPSO_QGS_F|V35_MAIN --output <new-directory>]");
    }
  }
}
