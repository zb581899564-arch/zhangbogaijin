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
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Fixed-scope local A2/A3 diagnostic launcher.
 *
 * <p>The instance, population and budget are intentionally not configurable:
 * this entry point cannot accidentally become a formal-matrix launcher.  One
 * invocation runs one arm/seed in one JVM.  Controller events are exported by
 * {@link V35FairRunner#writeRecord(V35FairRunner.RunRecord, Path, String)}
 * after the algorithm returns.</p>
 */
public final class ZhangBoV35A2A3DiagnosticRunner {
  public static final String VERSION = "v35-a2-a3-qp-reward-diagnostic-v2";
  public static final String INSTANCE = "20_2_3_1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 50_000;
  private static final long[] APPROVED_SEEDS = {20260822L, 20260823L, 20260824L};

  private ZhangBoV35A2A3DiagnosticRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    Path output = run(arguments);
    System.out.println("V35_A2_A3_DIAGNOSTIC_COMPLETED output=" + output);
  }

  static Path runForTest(Path projectRoot, Path output, long seed, String arm) throws Exception {
    return run(new Arguments(projectRoot.toAbsolutePath().normalize(), output.toAbsolutePath().normalize(),
        seed, arm, 2000));
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
    if (Files.exists(arguments.output)) {
      throw new IllegalStateException("refusing overwrite: " + arguments.output);
    }
    if (arguments.output.getParent() == null) {
      throw new IllegalArgumentException("output must have a parent directory");
    }
    Files.createDirectories(arguments.output.getParent());
    Path partial = arguments.output.resolveSibling(
        ".partial-" + arguments.output.getFileName() + "-" + System.nanoTime());
    Files.createDirectories(partial);

    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", fatigue.toString());
      System.setProperty("dhfsp.instance.extension.dir", extension.toString());

      ZhangBoCanonicalProductionProblem snapshotProblem = load(instance, extension, fatigue,
          arguments.seed);
      List<PermutationSolution<Integer>> initial =
          ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, snapshotProblem);
      if (initial.size() != POPULATION) throw new IllegalStateException("population drift");
      String initialHash = V35FairRunner.initialHash(initial);
      String p8InitialHash = P8InitialPopulationProvider.sha256(initial);

      V35FinalAblationProfile.Arm profileArm = profileArm(arguments.arm);
      V35FairRunner.Mode mode = profileArm == V35FinalAblationProfile.Arm.A2_CFVF
          ? V35FairRunner.Mode.V35_A2 : V35FairRunner.Mode.V35_A3;
      V35ProductionConfiguration configuration = configurationFor(
          arguments.arm, profileArm, arguments.seed, arguments.maxFes);
      V35FinalAblationProfile.validate(profileArm, configuration);
      ZhangBoCanonicalProductionProblem problem = load(instance, extension, fatigue, arguments.seed);
      V35FairRunner.RunRecord record = V35FairRunner.run(mode, problem,
          P8InitialPopulationProvider.copy(initial), arguments.maxFes, arguments.seed, true,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);

      String configurationText = "runnerVersion=" + VERSION + "\n"
          + "instance=" + INSTANCE + "\n"
          + "arm=" + arguments.arm + "\n"
          + "seed=" + arguments.seed + "\n"
          + "population=" + POPULATION + "\n"
          + "maxFEs=" + arguments.maxFes + "\n"
          + "initialPopulationHash=" + initialHash + "\n"
          + "initialPopulationP8Sha256=" + p8InitialHash + "\n"
          + "profileSha256=" + profileHash(arguments.arm, profileArm,
              arguments.seed, arguments.maxFes, configuration) + "\n"
          + "profileCanonicalBegin\n"
          + profileText(arguments.arm, profileArm, arguments.seed, arguments.maxFes, configuration)
          + "profileCanonicalEnd\n";
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("initial-population.sha256"),
          ("initialPopulationHash=" + initialHash + "\n")
              .getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("initial-population-p8.sha256"),
          (p8InitialHash + "  p8-four-vector-population\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("profile.sha256"),
          (profileHash(arguments.arm, profileArm, arguments.seed, arguments.maxFes, configuration)
              + "  canonical-profile\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-scope.txt"),
          ("remoteRun=false\ninstance=" + INSTANCE + "\npopulation=" + POPULATION
              + "\nmaxFEs=" + arguments.maxFes + "\narm=" + arguments.arm + "\nseed="
          + arguments.seed + "\npostRunObservationOnly=true\nrandomStreamUnchanged=true\n")
              .getBytes(StandardCharsets.UTF_8));
      if (!"COMPLETED".equals(record.getStatus())) {
        throw new IllegalStateException("run status=" + record.getStatus());
      }
      writeEvidenceHashes(partial);
      move(partial, arguments.output);
      return arguments.output;
    } catch (Exception error) {
      if (Files.isDirectory(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n")
                .getBytes(StandardCharsets.UTF_8));
      }
      throw error;
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension,
      Path fatigue, long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed,
        extension, fatigue, ZhangBoShiftConfiguration.none());
  }

  private static V35FinalAblationProfile.Arm profileArm(String arm) {
    if ("A2".equals(arm)) return V35FinalAblationProfile.Arm.A2_CFVF;
    if ("A3".equals(arm) || "A3_CLIPPED".equals(arm)) {
      return V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE;
    }
    throw new IllegalArgumentException("arm must be A2, A3, or A3_CLIPPED");
  }

  private static V35ProductionConfiguration configurationFor(String arm,
      V35FinalAblationProfile.Arm profileArm, long seed, int maxFes) {
    if (!"A3_CLIPPED".equals(arm)) {
      return V35FinalAblationProfile.configurationFor(profileArm, seed, POPULATION, maxFes);
    }
    return V35ProductionConfiguration.builder()
        .seed(seed).populationSize(POPULATION).maxEvaluations(maxFes)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(false)
        .directionalTeacherPool(false).teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED)
        .dualQCoordination(ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 5, 5))
        .qpConfiguration(ZhangBoQpConfiguration.v35ClippedDirection())
        .build();
  }

  private static String profileText(String arm, V35FinalAblationProfile.Arm profileArm,
      long seed, int maxFes, V35ProductionConfiguration configuration) {
    String base = V35FinalAblationProfile.canonicalTextFor(profileArm, seed, POPULATION, maxFes);
    if (!"A3_CLIPPED".equals(arm)) return base;
    return base + "diagnosticOverride=QP_DIRECTION_REWARD_CLIP_MINUS1_PLUS1\n"
        + "diagnosticConfigurationBegin\n" + configuration.canonicalText()
        + "diagnosticConfigurationEnd\n";
  }

  private static String profileHash(String arm, V35FinalAblationProfile.Arm profileArm,
      long seed, int maxFes, V35ProductionConfiguration configuration) throws Exception {
    return sha256(profileText(arm, profileArm, seed, maxFes, configuration));
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing file: " + path);
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key); else System.setProperty(key, value);
  }

  private static void writeEvidenceHashes(Path directory) throws Exception {
    StringBuilder out = new StringBuilder("sha256\tbytes\tpath\n");
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
          .sorted()
          .forEach(path -> {
            try {
              out.append(sha256(path)).append('\t')
                  .append(Files.size(path)).append('\t')
                  .append(directory.relativize(path).toString().replace('\\', '/')).append('\n');
            } catch (Exception error) {
              throw new HashingFailure(error);
            }
          });
    } catch (HashingFailure error) {
      throw error.cause;
    }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (java.io.InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) >= 0) {
        if (count > 0) digest.update(buffer, 0, count);
      }
    }
    StringBuilder out = new StringBuilder();
    for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  private static String sha256(String text) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(text.getBytes(StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder();
    for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  private static void move(Path partial, Path completed) throws Exception {
    try {
      Files.move(partial, completed, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException error) {
      Files.move(partial, completed);
    }
  }

  private static final class HashingFailure extends RuntimeException {
    private final Exception cause;
    private HashingFailure(Exception cause) { this.cause = cause; }
  }

  private static boolean approved(long seed) {
    for (long value : APPROVED_SEEDS) if (value == seed) return true;
    return false;
  }

  private static final class Arguments {
    private final Path projectRoot;
    private final Path output;
    private final long seed;
    private final String arm;
    private final int maxFes;

    private Arguments(Path projectRoot, Path output, long seed, String arm, int maxFes) {
      this.projectRoot = projectRoot;
      this.output = output;
      this.seed = seed;
      this.arm = arm;
      this.maxFes = maxFes;
    }

    private static Arguments parse(String[] args) {
      String project = null;
      String output = null;
      Long seed = null;
      String arm = null;
      for (int index = 0; index < args.length; index++) {
        if (index + 1 >= args.length) throw new IllegalArgumentException("missing option value");
        String key = args[index++];
        String value = args[index];
        switch (key) {
          case "--project-root": project = value; break;
          case "--output": output = value; break;
          case "--seed": seed = Long.valueOf(value); break;
          case "--arm": arm = value; break;
          default: throw new IllegalArgumentException("unknown option: " + key);
        }
      }
      if (project == null || output == null || seed == null || arm == null) {
        throw new IllegalArgumentException("required: --project-root --output --seed --arm");
      }
      if (!approved(seed)) throw new IllegalArgumentException("seed must be 20260822, 20260823, or 20260824");
      profileArm(arm);
      return new Arguments(Paths.get(project).toAbsolutePath().normalize(),
          Paths.get(output).toAbsolutePath().normalize(), seed, arm, MAX_FES);
    }
  }
}
