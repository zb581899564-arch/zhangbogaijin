package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Stream;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35PersonalLeaderMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35QpSettlementPolicy;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEventLog;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-A3-D minimal causal decomposition launcher.  This runner is deliberately
 * separate from the frozen A0-A4 profiles: it only diagnoses the bundled A3
 * additions and cannot be used as a formal experiment launcher.
 */
public final class ZhangBoV35A2A3DecompositionRunner {
  public static final String VERSION = "v35-a2-a3-decomposition-q1-tie-v3";
  public static final String INSTANCE = "20_2_3_1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 50_000;
  private static final long[] APPROVED_SEEDS = {20260822L, 20260823L, 20260824L};

  public enum Arm {
    D0_A2_CONTROL(V35FairRunner.Mode.V35_A2),
    D1_PA_DIRECTIONAL(V35FairRunner.Mode.V35_DIAG_PA_DIRECTIONAL),
    Q0_QP_OBSERVE_ONLY(V35FairRunner.Mode.V35_DIAG_QP_OBSERVE_ONLY),
    Q1_QP_DIRECTIONAL_TIE(V35FairRunner.Mode.V35_DIAG_QP_DIRECTIONAL_TIE),
    D2_QP_SYNCHRONOUS(V35FairRunner.Mode.V35_DIAG_QP_SYNCHRONOUS),
    D3_A3_BLOCK_FROZEN(V35FairRunner.Mode.V35_A3);

    private final V35FairRunner.Mode mode;
    Arm(V35FairRunner.Mode mode) { this.mode = mode; }
    V35FairRunner.Mode mode() { return mode; }
  }

  private ZhangBoV35A2A3DecompositionRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    Path output = run(arguments);
    System.out.println("V35_A2_A3_DECOMPOSITION_COMPLETED output=" + output);
  }

  static Path runForTest(Path projectRoot, Path output, long seed, Arm arm) throws Exception {
    return run(new Arguments(projectRoot.toAbsolutePath().normalize(), output.toAbsolutePath().normalize(),
        seed, arm, 2_000));
  }

  @SuppressWarnings("unchecked")
  private static Path run(Arguments arguments) throws Exception {
    Path root = arguments.projectRoot;
    Path javaProject = root.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP").resolve(INSTANCE + ".txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    Path snapshot = root.resolve("docs/evidence/V35-FORMAL-MANIFEST/initial-populations")
        .resolve(INSTANCE).resolve("seed-" + arguments.seed + ".fourvec");
    requireFile(instance);
    requireFile(extension.resolve(INSTANCE + ".setup.txt"));
    requireFile(fatigue.resolve(INSTANCE + ".fatigue.txt"));
    requireFile(snapshot);
    if (Files.exists(arguments.output)) throw new IllegalStateException("refusing overwrite: " + arguments.output);
    if (arguments.output.getParent() == null) throw new IllegalArgumentException("output must have a parent");
    Files.createDirectories(arguments.output.getParent());
    Path partial = arguments.output.resolveSibling(
        ".partial-" + arguments.output.getFileName() + "-" + System.nanoTime());
    Files.createDirectories(partial);

    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    String oldCapture = System.getProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY);
    try {
      System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", fatigue.toString());
      System.setProperty("dhfsp.instance.extension.dir", extension.toString());
      // Full controller traces are diagnostic-only; this affects retained evidence,
      // never a random draw, score, FE, or action decision.
      System.setProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY, "true");

      ZhangBoCanonicalProductionProblem snapshotProblem = load(instance, extension, fatigue, arguments.seed);
      List<PermutationSolution<Integer>> initial =
          ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, snapshotProblem);
      if (initial.size() != POPULATION) throw new IllegalStateException("population drift");
      String initialHash = V35FairRunner.initialHash(initial);
      String p8Hash = P8InitialPopulationProvider.sha256(initial);
      V35ProductionConfiguration configuration = configurationFor(arguments.arm, arguments.seed,
          arguments.maxFes);
      requireContract(arguments.arm, configuration, arguments.seed, arguments.maxFes);

      ZhangBoCanonicalProductionProblem problem = load(instance, extension, fatigue, arguments.seed);
      V35FairRunner.RunRecord record = V35FairRunner.runA2A3Diagnostic(arguments.arm.mode(), problem,
          P8InitialPopulationProvider.copy(initial), arguments.maxFes, arguments.seed,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration);
      String profileText = profileText(arguments.arm, arguments.seed, arguments.maxFes, configuration);
      String profileHash = sha256(profileText);
      String configurationText = "runnerVersion=" + VERSION + "\n"
          + "diagnosticKind=V35_A2_TO_A3_MINIMAL_CAUSAL_DECOMPOSITION\n"
          + "preRegistered=true\n"
          + "instance=" + INSTANCE + "\narm=" + arguments.arm + "\nseed=" + arguments.seed + "\n"
          + "population=" + POPULATION + "\nmaxFEs=" + arguments.maxFes + "\n"
          + "initialPopulationHash=" + initialHash + "\ninitialPopulationP8Sha256=" + p8Hash + "\n"
          + "sourceRunId=" + sourceRunId(arguments.arm, arguments.seed) + "\n"
          + "rerunReason=" + rerunReason(arguments.arm) + "\n"
          + "profileSha256=" + profileHash + "\nprofileCanonicalBegin\n" + profileText
          + "profileCanonicalEnd\n";
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("pre-registration.txt"), preRegistration(arguments.arm,
          arguments.seed, arguments.maxFes, profileHash).getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population.sha256"),
          ("initialPopulationHash=" + initialHash + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population-p8.sha256"),
          (p8Hash + "  p8-four-vector-population\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("profile.sha256"),
          (profileHash + "  canonical-profile\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("telemetry-contract.properties"), (
          "fullControllerCapture=true\n"
          + "personalLeaderAuditEvents=" + record.getA2A3PersonalLeaderAuditEvents() + "\n"
          + "qpActionActive=" + configuration.isQpEnabled() + "\n"
          + "qpSettlementPolicy=" + configuration.getQpSettlementPolicy() + "\n"
          + "qpGreedyTiePolicy=" + (configuration.getQpConfiguration() == null
              ? ZhangBoQpConfiguration.GreedyTiePolicy.FIRST_VALID
              : configuration.getQpConfiguration().getGreedyTiePolicy()) + "\n"
          + "qpTdLearningDisabled=" + (configuration.getQpSettlementPolicy()
              == V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES) + "\n"
          + "diagnosticObservationOnly=true\nrandomStreamUnchanged=true\nfeUnchanged=true\n")
          .getBytes(StandardCharsets.UTF_8));
      requireCompleted(record, arguments.maxFes);
      writeEvidenceHashes(partial);
      move(partial, arguments.output);
      return arguments.output;
    } catch (Exception error) {
      if (Files.isDirectory(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
      }
      throw error;
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
      restore(ZhangBoEventLog.FULL_CAPTURE_PROPERTY, oldCapture);
    }
  }

  private static V35ProductionConfiguration configurationFor(Arm arm, long seed, int maxFes) {
    if (arm == Arm.D0_A2_CONTROL || arm == Arm.D3_A3_BLOCK_FROZEN) {
      V35FinalAblationProfile.Arm profileArm = arm == Arm.D0_A2_CONTROL
          ? V35FinalAblationProfile.Arm.A2_CFVF
          : V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE;
      return V35FinalAblationProfile.configurationFor(profileArm, seed, POPULATION, maxFes);
    }
    V35ProductionConfiguration.Builder builder = V35ProductionConfiguration.builder()
        .seed(seed).populationSize(POPULATION).maxEvaluations(maxFes)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).caTaLite(false)
        .directionalTeacherPool(false).teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED);
    if (arm == Arm.D1_PA_DIRECTIONAL) {
      return builder.qp(false).personalLeaderMode(V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL).build();
    }
    if (arm == Arm.D2_QP_SYNCHRONOUS) {
      return builder.qp(true).personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
          .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous()).build();
    }
    if (arm == Arm.Q0_QP_OBSERVE_ONLY) {
      return builder.qp(true).personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
          .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous())
          .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
    }
    if (arm == Arm.Q1_QP_DIRECTIONAL_TIE) {
      return builder.qp(true).personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
          .dualQCoordination(ZhangBoDualQCoordinationConfiguration.synchronous())
          .qpConfiguration(ZhangBoQpConfiguration.diagnosticDirectionalGreedyTie())
          .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
    }
    throw new IllegalArgumentException("Unsupported decomposition arm: " + arm);
  }

  private static void requireContract(Arm arm, V35ProductionConfiguration configuration,
      long seed, int maxFes) {
    if (configuration.getSeed() != seed || configuration.getPopulationSize() != POPULATION
        || configuration.getMaxEvaluations() != maxFes || configuration.getDecoderMode() != ProductionDecodeMode.FM3
        || configuration.getPddrSelectionMode() != PddrSelectionMode.GLOBAL_ORIGINAL
        || configuration.getLocalSearchOrder() != V35LocalSearchOrder.CATA_THEN_INHERITED
        || configuration.isCaTaLiteEnabled() || configuration.isDirectionalTeacherPoolEnabled()) {
      throw new IllegalArgumentException("diagnostic arm violates the frozen common boundary");
    }
    if (arm == Arm.D0_A2_CONTROL && (configuration.isLineageArchiveEnabled() || configuration.isQpEnabled())) {
      throw new IllegalArgumentException("D0 must be the exact A2 control");
    }
    if (arm == Arm.D1_PA_DIRECTIONAL && (configuration.isQpEnabled()
        || configuration.getPersonalLeaderMode() != V35PersonalLeaderMode.ARCHIVE_DIRECTIONAL
        || configuration.getDualQCoordination() != null)) {
      throw new IllegalArgumentException("D1 must enable only the directional personal archive");
    }
    if (arm == Arm.D2_QP_SYNCHRONOUS && (!configuration.isQpEnabled()
        || configuration.getPersonalLeaderMode() != V35PersonalLeaderMode.QP_FOUR_ACTIONS
        || configuration.getDualQCoordination() == null
        || configuration.getDualQCoordination().isBlockFrozen())) {
      throw new IllegalArgumentException("D2 must use synchronous Qg/Qp without a frozen block");
    }
    if (arm == Arm.Q0_QP_OBSERVE_ONLY && (!configuration.isQpEnabled()
        || configuration.getPersonalLeaderMode() != V35PersonalLeaderMode.QP_FOUR_ACTIONS
        || configuration.getDualQCoordination() == null
        || configuration.getDualQCoordination().isBlockFrozen()
        || configuration.getQpSettlementPolicy()
            != V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES)) {
      throw new IllegalArgumentException(
          "Q0 must use active four-action Qp with synchronous observe-only settlement");
    }
    if (arm == Arm.Q1_QP_DIRECTIONAL_TIE && (!configuration.isQpEnabled()
        || configuration.getPersonalLeaderMode() != V35PersonalLeaderMode.QP_FOUR_ACTIONS
        || configuration.getDualQCoordination() == null
        || configuration.getDualQCoordination().isBlockFrozen()
        || configuration.getQpSettlementPolicy()
            != V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES
        || configuration.getQpConfiguration() == null
        || configuration.getQpConfiguration().getGreedyTiePolicy()
            != ZhangBoQpConfiguration.GreedyTiePolicy.DIRECTIONAL_IF_TIED)) {
      throw new IllegalArgumentException(
          "Q1 must differ from Q0 only through the diagnostic directional greedy-tie policy");
    }
    if (arm == Arm.D3_A3_BLOCK_FROZEN) {
      V35FinalAblationProfile.validate(V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE, configuration);
      if (configuration.getDualQCoordination() == null
          || !configuration.getDualQCoordination().isBlockFrozen()
          || configuration.getDualQCoordination().getSoftFreezeRho() != 0.0) {
        throw new IllegalArgumentException("D3 must preserve the P5/G5 hard-freeze schedule");
      }
    }
  }

  private static String profileText(Arm arm, long seed, int maxFes,
      V35ProductionConfiguration configuration) {
    return "diagnosticArm=" + arm + "\n"
        + "diagnosticSemantics=LEGACY_UNCLIPPED_QP_DIRECTION_REWARD\n"
        + "configurationBegin\n" + configuration.canonicalText() + "configurationEnd\n";
  }

  private static String preRegistration(Arm arm, long seed, int maxFes, String profileHash) {
    return "workPackage=" + (arm == Arm.Q1_QP_DIRECTIONAL_TIE ? "V35-A3-D3" : "V35-A3-D")
        + "\narm=" + arm + "\ninstance=" + INSTANCE + "\nseed=" + seed
        + "\npopulation=" + POPULATION + "\nmaxFEs=" + maxFes + "\nprofileSha256=" + profileHash
        + "\ncommonBoundary=FM3;ShiftMode.NONE;singleFamily;sequenceIndependentSUT;"
        + "mixture=20/40/20/20;PDDR=GLOBAL_ORIGINAL;LS=CATA_THEN_INHERITED;teacherPool=OFF\n"
        + "notDoe=true\nnotFormalAblation=true\nformalJarModified=false\n"
        + "causalPairing=" + causalPairing(arm) + "\n";
  }

  private static void requireCompleted(V35FairRunner.RunRecord record, int maxFes) {
    boolean formalDiagnosticBudget = maxFes == MAX_FES;
    if (!"COMPLETED".equals(record.getStatus()) || record.getFront().isEmpty()
        || record.getIllegalSolutions() != 0 || record.getDuplicateEvaluations() != 0
        || record.getFullEvaluations() <= 0 || record.getFullEvaluations() > maxFes
        || (formalDiagnosticBudget && record.getFullEvaluations() != maxFes)
        || record.getDecoderCalls() != record.getFullEvaluations()) {
      throw new IllegalStateException("diagnostic run failed acceptance: " + record.getStatus());
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension,
      Path fatigue, long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed,
        extension, fatigue, ZhangBoShiftConfiguration.none());
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing file: " + path);
  }
  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key); else System.setProperty(key, value);
  }
  private static boolean approved(long seed) {
    for (long value : APPROVED_SEEDS) if (value == seed) return true;
    return false;
  }
  private static String sourceRunId(Arm arm, long seed) {
    if (arm == Arm.Q0_QP_OBSERVE_ONLY) {
      return "D1_PA_DIRECTIONAL@" + seed + ";D2_QP_SYNCHRONOUS@" + seed;
    }
    if (arm == Arm.Q1_QP_DIRECTIONAL_TIE) {
      return "Q0_QP_OBSERVE_ONLY@" + seed + ";D1_PA_DIRECTIONAL@" + seed;
    }
    return "V35-A2-A3-CAUSAL-AUDIT";
  }
  private static String rerunReason(Arm arm) {
    if (arm == Arm.Q0_QP_OBSERVE_ONLY) return "NEW_MINIMAL_QP_SETTLEMENT_CAUSAL_ARM";
    if (arm == Arm.Q1_QP_DIRECTIONAL_TIE) return "NEW_MINIMAL_QP_COLD_START_TIE_CAUSAL_ARM";
    return "UNIFORM_CAUSAL_TELEMETRY_NOT_AN_INDEPENDENT_SAMPLE";
  }
  private static String causalPairing(Arm arm) {
    if (arm == Arm.Q0_QP_OBSERVE_ONLY) {
      return "D1_TO_Q0=QP_ACTION_POLICY;Q0_TO_D2=QP_TD_REWARD";
    }
    if (arm == Arm.Q1_QP_DIRECTIONAL_TIE) {
      return "Q0_TO_Q1=QP_COLD_START_GREEDY_TIE_ONLY;Q1_TO_D1=DIRECTIONAL_RECOVERY_CHECK";
    }
    return "D0_TO_D1=PERSONAL_ARCHIVE;D1_TO_D2=QP_ACTION_AND_TD;D2_TO_D3=BLOCK_FREEZE";
  }

  private static void writeEvidenceHashes(Path directory) throws Exception {
    StringBuilder out = new StringBuilder("sha256\tbytes\tpath\n");
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
          .sorted().forEach(path -> {
            try {
              out.append(sha256(path)).append('\t').append(Files.size(path)).append('\t')
                  .append(directory.relativize(path).toString().replace('\\', '/')).append('\n');
            } catch (Exception error) { throw new HashingFailure(error); }
          });
    } catch (HashingFailure error) { throw error.cause; }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (java.io.InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
    }
    return hex(digest.digest());
  }
  private static String sha256(String text) throws Exception {
    return hex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
  }
  private static String hex(byte[] value) {
    StringBuilder out = new StringBuilder();
    for (byte item : value) out.append(String.format("%02x", item & 0xff));
    return out.toString();
  }
  private static void move(Path partial, Path completed) throws Exception {
    try { Files.move(partial, completed, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(partial, completed); }
  }
  private static final class HashingFailure extends RuntimeException {
    private final Exception cause;
    private HashingFailure(Exception cause) { this.cause = cause; }
  }

  private static final class Arguments {
    private final Path projectRoot;
    private final Path output;
    private final long seed;
    private final Arm arm;
    private final int maxFes;
    private Arguments(Path projectRoot, Path output, long seed, Arm arm, int maxFes) {
      this.projectRoot = projectRoot; this.output = output; this.seed = seed; this.arm = arm; this.maxFes = maxFes;
    }
    private static Arguments parse(String[] args) {
      String project = null, output = null, armText = null;
      Long seed = null;
      boolean preflight = false;
      for (int index = 0; index < args.length; index++) {
        String key = args[index];
        if ("--preflight".equals(key)) {
          preflight = true;
          continue;
        }
        if (index + 1 >= args.length) throw new IllegalArgumentException("missing option value");
        String value = args[++index];
        if ("--project-root".equals(key)) project = value;
        else if ("--output".equals(key)) output = value;
        else if ("--seed".equals(key)) seed = Long.valueOf(value);
        else if ("--arm".equals(key)) armText = value;
        else throw new IllegalArgumentException("unknown option: " + key);
      }
      if (project == null || output == null || seed == null || armText == null) {
        throw new IllegalArgumentException("required: --project-root --output --seed --arm");
      }
      if (!approved(seed)) throw new IllegalArgumentException("seed must be 20260822, 20260823, or 20260824");
      Arm arm;
      try { arm = Arm.valueOf(armText); }
      catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid arm: " + armText, error); }
      return new Arguments(Paths.get(project).toAbsolutePath().normalize(),
          Paths.get(output).toAbsolutePath().normalize(), seed, arm,
          preflight ? 2_000 : MAX_FES);
    }
  }
}
