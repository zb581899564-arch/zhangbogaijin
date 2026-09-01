package org.uma.jmetal.runner.lc_psode;

import java.io.InputStream;
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
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * FC5-T's only replay launcher.  It is intentionally unable to alter search
 * semantics: it merely attaches the observer ledger to one of the frozen A0,
 * A2, or A4 arms and requires the original four-vector snapshot as input.
 */
public final class ZhangBoV35Fc5TransferRunner {
  public static final String VERSION = "FC5_100JOB_TRANSFER_V1";
  public static final int POPULATION = 100;

  private ZhangBoV35Fc5TransferRunner() { }

  public enum Arm {
    A0_BASELINE(V35FairRunner.Mode.V35_BASELINE, V35FinalAblationProfile.Arm.A0_BASELINE),
    A2_CFVF(V35FairRunner.Mode.V35_A2, V35FinalAblationProfile.Arm.A2_CFVF),
    A4_BUDGET_AWARE_CATA(V35FairRunner.Mode.V35_FULL_POOL_OFF,
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA);

    private final V35FairRunner.Mode mode;
    private final V35FinalAblationProfile.Arm profileArm;
    Arm(V35FairRunner.Mode mode, V35FinalAblationProfile.Arm profileArm) {
      this.mode = mode; this.profileArm = profileArm;
    }
  }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    Path output = run(arguments);
    System.out.println("FC5_TRANSFER_TELEMETRY_COMPLETED output=" + output);
  }

  @SuppressWarnings("unchecked")
  static Path run(Arguments arguments) throws Exception {
    requireApprovedCase(arguments.instanceName, arguments.seed, arguments.arm);
    Path root = arguments.projectRoot;
    Path javaProject = root.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP").resolve(arguments.instanceName + ".txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance); requireFile(extension.resolve(arguments.instanceName + ".setup.txt"));
    requireFile(fatigue.resolve(arguments.instanceName + ".fatigue.txt"));
    requireFile(arguments.snapshot);
    if (Files.exists(arguments.output)) throw new IllegalStateException("refusing overwrite: " + arguments.output);
    if (arguments.output.getParent() == null) throw new IllegalArgumentException("output must have a parent");
    Files.createDirectories(arguments.output.getParent());
    Path partial = arguments.output.resolveSibling(".partial-"
        + arguments.output.getFileName() + "-" + System.nanoTime());
    Files.createDirectories(partial);
    try {
      ZhangBoCanonicalProductionProblem snapshotProblem = load(instance, extension, fatigue, arguments.seed);
      List<PermutationSolution<Integer>> initial =
          ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(arguments.snapshot, snapshotProblem);
      if (initial.size() != POPULATION) throw new IllegalStateException("snapshot population drift");
      String v35InitialHash = V35FairRunner.initialHash(initial);
      String p8InitialHash = P8InitialPopulationProvider.sha256(initial);
      V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
          arguments.arm.profileArm, arguments.seed, POPULATION, arguments.maxFes);
      V35FinalAblationProfile.validate(arguments.arm.profileArm, configuration);
      requireFrozenBoundary(configuration);

      ZhangBoCanonicalProductionProblem problem = load(instance, extension, fatigue, arguments.seed);
      V35FairRunner.RunRecord record = V35FairRunner.runFc5TransferDiagnostic(arguments.arm.mode,
          problem, P8InitialPopulationProvider.copy(initial), arguments.maxFes, arguments.seed,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration);
      requireAccepted(record, arguments.maxFes);
      String profileText = configuration.canonicalText();
      String profileHash = sha256(profileText.getBytes(StandardCharsets.UTF_8));
      String sourceRun = sourceRunId(arguments.instanceName, arguments.seed, arguments.arm);
      String configurationText = "runnerVersion=" + VERSION + "\n"
          + "diagnosticKind=FC5_100JOB_TRANSFER_OBSERVER_ONLY\npreRegistered=true\n"
          + "instance=" + arguments.instanceName + "\nseed=" + arguments.seed + "\narm="
          + arguments.arm + "\npopulation=" + POPULATION + "\nmaxFEs=" + arguments.maxFes + "\n"
          + "sourceRunId=" + sourceRun + "\nsnapshotSha256=" + sha256(arguments.snapshot) + "\n"
          + "initialPopulationHashV35=" + v35InitialHash + "\ninitialPopulationHashP8="
          + p8InitialHash + "\nprofileSha256=" + profileHash + "\n"
          + "observerVersion=" + VERSION + "\nobserverOnly=true\n"
          + "globalOriginalPddr=true\nprofileCanonicalBegin\n" + profileText + "profileCanonicalEnd\n";
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("telemetry-contract.properties"), (
          "observerOnly=true\nsearchSemanticsChanged=false\n"
          + "pddrMode=GLOBAL_ORIGINAL\ncfvf=" + configuration.isCfvfEnabled() + "\n"
          + "qp=" + configuration.isQpEnabled() + "\n"
          + "dualQ=" + (configuration.getDualQCoordination() != null) + "\n"
          + "caTaLite=" + configuration.isCaTaLiteEnabled() + "\n"
          + "actualFE=" + record.getFullEvaluations() + "\ndecoderCalls=" + record.getDecoderCalls() + "\n"
          + "normalization=behavioral-equivalence-not-raw-csv-bytes\n")
          .getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population.sha256"),
          ("v35=" + v35InitialHash + "\np8=" + p8InitialHash + "\nsnapshot="
              + sha256(arguments.snapshot) + "\n").getBytes(StandardCharsets.UTF_8));
      writeHashes(partial);
      move(partial, arguments.output);
      return arguments.output;
    } catch (Exception error) {
      if (Files.isDirectory(partial)) Files.write(partial.resolve("failure.txt"),
          (error.getClass().getName() + ": " + error.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
      throw error;
    }
  }

  private static void requireFrozenBoundary(V35ProductionConfiguration configuration) {
    if (configuration.getDecoderMode() != ProductionDecodeMode.FM3
        || configuration.getPddrSelectionMode()
            != org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode.GLOBAL_ORIGINAL
        || configuration.isDirectionalTeacherPoolEnabled()) {
      throw new IllegalArgumentException("FC5-T replay violates the frozen common boundary");
    }
  }

  private static void requireAccepted(V35FairRunner.RunRecord record, int maxFes) {
    if (!"COMPLETED".equals(record.getStatus()) || record.getFront().isEmpty()
        || record.getIllegalSolutions() != 0 || record.getDuplicateEvaluations() != 0
        || record.getFullEvaluations() <= 0 || record.getFullEvaluations() > maxFes
        || record.getDecoderCalls() != record.getFullEvaluations()
        || record.getFc5TransferSummary().isEmpty()) {
      throw new IllegalStateException("FC5-T telemetry acceptance failed: " + record.getStatus());
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension,
      Path fatigue, long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed,
        extension, fatigue, ZhangBoShiftConfiguration.none());
  }

  private static void requireApprovedCase(String instance, long seed, Arm arm) {
    boolean a0a2Case = "100_2_5_1".equals(instance) || "100_8_3_1".equals(instance);
    boolean a2a4Case = "100_2_4_1".equals(instance) || "100_5_3_1".equals(instance);
    boolean a0a2Seed = seed >= 20260911L && seed <= 20260915L;
    boolean a2a4Seed = seed >= 20260901L && seed <= 20260905L;
    if ((a0a2Case && a0a2Seed && arm != Arm.A4_BUDGET_AWARE_CATA)
        || (a2a4Case && a2a4Seed && arm != Arm.A0_BASELINE)) return;
    throw new IllegalArgumentException("instance/seed/arm is outside FC5-T's pre-registered contrast set");
  }

  private static String sourceRunId(String instance, long seed, Arm arm) {
    String campaign = ("100_2_5_1".equals(instance) || "100_8_3_1".equals(instance))
        ? "V35A2FINAL" : "V35A2A4";
    String label = arm == Arm.A0_BASELINE ? "A0" : arm == Arm.A2_CFVF ? "A2" : "A4";
    return campaign + "-" + instance + "-" + seed + "-" + label;
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing file: " + path);
  }
  private static void writeHashes(Path directory) throws Exception {
    StringBuilder out = new StringBuilder("sha256\tbytes\tpath\n");
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> !"evidence-sha256.tsv".equals(path.getFileName().toString()))
          .sorted().forEach(path -> {
            try { out.append(sha256(path)).append('\t').append(Files.size(path)).append('\t')
                .append(directory.relativize(path).toString().replace('\\', '/')).append('\n'); }
            catch (Exception error) { throw new HashingFailure(error); }
          });
    } catch (HashingFailure error) { throw error.cause; }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192]; int count;
      while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
    }
    return hex(digest.digest());
  }
  private static String sha256(byte[] data) throws Exception {
    return hex(MessageDigest.getInstance("SHA-256").digest(data));
  }
  private static String hex(byte[] data) {
    StringBuilder out = new StringBuilder();
    for (byte value : data) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }
  private static void move(Path partial, Path output) throws Exception {
    try { Files.move(partial, output, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(partial, output); }
  }
  private static final class HashingFailure extends RuntimeException {
    private final Exception cause;
    private HashingFailure(Exception cause) { this.cause = cause; }
  }
  static final class Arguments {
    private final Path projectRoot, output, snapshot;
    private final String instanceName;
    private final long seed;
    private final Arm arm;
    private final int maxFes;
    private Arguments(Path projectRoot, Path output, Path snapshot, String instanceName, long seed,
        Arm arm, int maxFes) {
      this.projectRoot = projectRoot; this.output = output; this.snapshot = snapshot;
      this.instanceName = instanceName; this.seed = seed; this.arm = arm; this.maxFes = maxFes;
    }
    private static Arguments parse(String[] args) {
      String project = null, output = null, snapshot = null, instance = null, arm = null, budget = null;
      Long seed = null;
      for (int i = 0; i < args.length; i++) {
        if (i + 1 >= args.length) throw new IllegalArgumentException("missing option value: " + args[i]);
        String key = args[i], value = args[++i];
        if ("--project-root".equals(key)) project = value;
        else if ("--output".equals(key)) output = value;
        else if ("--snapshot".equals(key)) snapshot = value;
        else if ("--instance".equals(key)) instance = value;
        else if ("--seed".equals(key)) seed = Long.valueOf(value);
        else if ("--arm".equals(key)) arm = value;
        else if ("--max-fes".equals(key)) budget = value;
        else throw new IllegalArgumentException("unknown option: " + key);
      }
      if (project == null || output == null || snapshot == null || instance == null || seed == null || arm == null
          || budget == null) throw new IllegalArgumentException("required: --project-root --output --snapshot --instance --seed --arm --max-fes");
      int max = Integer.parseInt(budget);
      if (max != 2_000 && max != 50_000 && max != 100_000 && max != 250_000 && max != 500_000)
        throw new IllegalArgumentException("max-fes must be 2000, 50000, 100000, 250000 or 500000");
      Arm parsed;
      try { parsed = Arm.valueOf(arm); }
      catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid arm: " + arm, error); }
      return new Arguments(Paths.get(project).toAbsolutePath().normalize(),
          Paths.get(output).toAbsolutePath().normalize(), Paths.get(snapshot).toAbsolutePath().normalize(),
          instance, seed.longValue(), parsed, max);
    }
  }
}
