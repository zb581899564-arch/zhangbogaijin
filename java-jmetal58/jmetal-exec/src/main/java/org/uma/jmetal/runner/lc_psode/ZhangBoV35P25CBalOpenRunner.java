package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** Three-seed 100k diagnostic for the fail-closed BAL/full-mask V35 semantics. */
public final class ZhangBoV35P25CBalOpenRunner {
  public static final String VERSION = "v35-p25c-bal-open-100k-v1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 100000;
  private static final long[] SEEDS = {20260819L, 20260820L, 20260821L};

  public enum Arm {
    A0(V35AblationRegistry.Rung.A0_BASELINE),
    A4(V35AblationRegistry.Rung.A4_CA_TA_LITE),
    A5(V35AblationRegistry.Rung.A5_FULL);

    private final V35AblationRegistry.Rung rung;
    Arm(V35AblationRegistry.Rung rung) { this.rung = rung; }
    public V35AblationRegistry.Rung getRung() { return rung; }
  }

  private ZhangBoV35P25CBalOpenRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    run(value.seedSlot, value.arm, value.projectRoot, value.output, POPULATION, MAX_FES);
  }

  static Path runForTest(int seedSlot, Arm arm, Path projectRoot, Path output,
      int population, int maxFEs) throws Exception {
    return run(seedSlot, arm, projectRoot, output, population, maxFEs);
  }

  private static Path run(int seedSlot, Arm arm, Path projectRoot, Path output,
      int population, int maxFEs) throws Exception {
    long seed = approvedSeed(seedSlot);
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP/20_2_3_1.txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance); requireFile(extension.resolve("20_2_3_1.setup.txt"));
    requireFile(fatigue.resolve("20_2_3_1.fatigue.txt"));

    Path seedDirectory = output.toAbsolutePath().normalize().resolve("runs/seed-" + seed);
    Path completed = seedDirectory.resolve(arm.name());
    if (Files.exists(completed)) throw new IllegalStateException("refusing overwrite: " + completed);
    Files.createDirectories(seedDirectory);
    Path partial = seedDirectory.resolve(".partial-" + arm + "-" + System.nanoTime());
    Files.createDirectory(partial);
    try {
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instance, ProductionDecodeMode.FM3, seed, extension, fatigue,
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < population; index++) initial.add(problem.createSolution());
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  initial-four-vector-population\n").getBytes(StandardCharsets.UTF_8));

      V35BottleneckDiagnosisConfiguration diagnosis =
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow();
      V35ProductionConfiguration configuration = V35AblationRegistry.configFor(
          arm.rung, seed, population, maxFEs);
      if (!diagnosis.canonicalText().equals(
          configuration.getBottleneckDiagnosis().canonicalText())) {
        throw new IllegalStateException("registry is not bound to BAL/full-mask diagnosis");
      }
      String runId = "seed-" + seed + "-" + arm;
      String configurationText = "p25cVersion=" + VERSION + "\nrunId=" + runId
          + "\nseedSlot=" + seedSlot + "\nseed=" + seed + "\narm=" + arm
          + "\ninstance=20_2_3_1\npopulation=" + population + "\nmaxFEs=" + maxFEs
          + "\ndecoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY"
          + "\nsetupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectiveAdapter=0,1,6"
          + "\npressureClassifier=diagnostic_only\nstrictPressureMask=false"
          + "\nactualBottleneck=BAL\nactionMask=N1|N2|N3|N4|N5\nshadowEnabled=false"
          + "\ninstanceFileSha256=" + sha256(instance)
          + "\nsutExtensionFileSha256=" + sha256(extension.resolve("20_2_3_1.setup.txt"))
          + "\nfatigueParameterFileSha256=" + sha256(fatigue.resolve("20_2_3_1.fatigue.txt"))
          + "\nmechanismVectorHash=" + configuration.configurationHash()
          + "\nv35ConfigurationBegin\n" + configuration.canonicalText()
          + "v35ConfigurationEnd\n";
      V35FairRunner.RunRecord record = V35FairRunner.run(arm.rung.getMode(), problem,
          P8InitialPopulationProvider.copy(initial), maxFEs, seed, false, diagnosis);
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("mechanism-summary.txt"),
          (record.getMechanismSummary() + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-record.csv"), (
          "runId,seedSlot,seed,arm,status,FE,frontSize,initialPopulationHash,algorithmRunNanos\n"
              + runId + ',' + seedSlot + ',' + seed + ',' + arm + ',' + record.getStatus()
              + ',' + record.getFullEvaluations() + ',' + record.getFront().size() + ','
              + initialHash + ',' + record.getAlgorithmRunNanos() + "\n")
          .getBytes(StandardCharsets.UTF_8));
      String gate = ZhangBoV35P25ARunner.hardGate(
          ZhangBoV35P25ARunner.Arm.valueOf(arm.name()), record, population, maxFEs);
      if (gate != null) throw new IllegalStateException(gate);
      if (record.getShadowSamples() != 0L || record.getShadowEvaluations() != 0L) {
        throw new IllegalStateException("shadow leaked into BAL verification");
      }
      writeHashes(partial);
      move(partial, completed);
      System.out.println("V35_P25C_RUN_COMPLETED seed=" + seed + " arm=" + arm
          + " FE=" + record.getFullEvaluations() + " front=" + record.getFront().size()
          + " output=" + completed);
      return completed;
    } catch (Exception error) {
      if (Files.exists(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n")
                .getBytes(StandardCharsets.UTF_8));
        writeHashes(partial);
      }
      throw error;
    }
  }

  static long approvedSeed(int slot) {
    if (slot < 1 || slot > SEEDS.length) throw new IllegalArgumentException("seed-slot must be 1..3");
    return SEEDS[slot - 1];
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing input: " + path);
  }

  private static void move(Path source, Path target) throws IOException {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }

  private static void writeHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try { hashes.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      out.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static final class Arguments {
    private int seedSlot; private Arm arm; private Path projectRoot; private Path output;
    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--seed-slot".equals(args[index])) value.seedSlot = Integer.parseInt(args[index + 1]);
        else if ("--arm".equals(args[index])) value.arm = Arm.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (value.seedSlot == 0 || value.arm == null || value.projectRoot == null
          || value.output == null) throw usage();
      approvedSeed(value.seedSlot);
      return value;
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --seed-slot 1..3 --arm A0|A4|A5 "
          + "--project-root <path> --output <path>");
    }
  }
}
