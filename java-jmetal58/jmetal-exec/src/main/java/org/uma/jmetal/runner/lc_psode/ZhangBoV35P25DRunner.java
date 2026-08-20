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
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35P25DComparativeEngine;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** One-algorithm/one-seed process entry for the P25D 50k comparison pilot. */
public final class ZhangBoV35P25DRunner {
  public static final String VERSION = "v35-p25d-eight-algorithm-50k-pilot-v1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 50000;
  private static final long[] SEEDS = {
      20260822L, 20260823L, 20260824L, 20260825L, 20260826L
  };

  public enum Algorithm {
    ZHANGBO_A4(null),
    HMOPSO_QGS_F(null),
    HMOPSO_QLS_F(V35P25DComparativeEngine.Algorithm.HMOPSO_QLS_F),
    MOPSO_F(V35P25DComparativeEngine.Algorithm.MOPSO_F),
    MOPSODS_DE_F(V35P25DComparativeEngine.Algorithm.MOPSODS_DE_F),
    MOHEADE_F(V35P25DComparativeEngine.Algorithm.MOHEADE_F),
    NSGA_II_F(V35P25DComparativeEngine.Algorithm.NSGA_II_F),
    SPEA2_F(V35P25DComparativeEngine.Algorithm.SPEA2_F);
    private final V35P25DComparativeEngine.Algorithm comparative;
    Algorithm(V35P25DComparativeEngine.Algorithm comparative) { this.comparative = comparative; }
  }

  private ZhangBoV35P25DRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    run(value.seedSlot, value.algorithm, value.projectRoot, value.output, POPULATION, MAX_FES);
  }

  static Path runForTest(int seedSlot, Algorithm algorithm, Path projectRoot, Path output,
      int population, int maxFEs) throws Exception {
    return run(seedSlot, algorithm, projectRoot, output, population, maxFEs);
  }

  private static Path run(int seedSlot, Algorithm algorithm, Path projectRoot, Path output,
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
    Path completed = seedDirectory.resolve(algorithm.name());
    if (Files.exists(completed)) throw new IllegalStateException("refusing overwrite: " + completed);
    Files.createDirectories(seedDirectory);
    Path partial = seedDirectory.resolve(".partial-" + algorithm + '-' + System.nanoTime());
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
      long start = System.nanoTime();
      String status; String stopReason; String mechanism; List<double[]> front;
      int evaluations; long runNanos;
      if (algorithm == Algorithm.ZHANGBO_A4 || algorithm == Algorithm.HMOPSO_QGS_F) {
        V35FairRunner.Mode mode = algorithm == Algorithm.ZHANGBO_A4
            ? V35FairRunner.Mode.V35_FULL_POOL_OFF : V35FairRunner.Mode.V35_BASELINE;
        V35FairRunner.RunRecord record = V35FairRunner.run(mode, problem,
            P8InitialPopulationProvider.copy(initial), maxFEs, seed, false,
            V35BottleneckDiagnosisConfiguration.fullMaskNoShadow());
        status = record.getStatus(); stopReason = record.getStopReason();
        mechanism = record.getMechanismSummary(); front = record.getFront();
        evaluations = record.getFullEvaluations(); runNanos = record.getAlgorithmRunNanos();
        V35FairRunner.writeRecord(record, partial, configurationText(algorithm, seedSlot, seed,
            population, maxFEs, initialHash, instance, extension, fatigue, null));
        String armGate = ZhangBoV35P25ARunner.hardGate(
            algorithm == Algorithm.ZHANGBO_A4 ? ZhangBoV35P25ARunner.Arm.A4
                : ZhangBoV35P25ARunner.Arm.A0, record, population, maxFEs);
        if (armGate != null) throw new IllegalStateException(armGate);
      } else {
        V35P25DComparativeEngine.Result record = V35P25DComparativeEngine.run(
            algorithm.comparative, problem, P8InitialPopulationProvider.copy(initial),
            maxFEs, seed);
        status = "COMPLETED"; stopReason = "EXACT_BUDGET";
        mechanism = record.mechanismSummary(); front = record.getFront();
        evaluations = record.getEvaluations(); runNanos = record.getRunNanos();
        Files.write(partial.resolve("configuration.txt"), configurationText(algorithm,
            seedSlot, seed, population, maxFEs, initialHash, instance, extension, fatigue,
            algorithm.comparative.canonicalParameters()).getBytes(StandardCharsets.UTF_8));
        writeFront(partial.resolve("front.csv"), front);
        ZhangBoDecoderTimingSnapshot timing = problem.getDecoderTimingSnapshot();
        Files.write(partial.resolve("status.properties"), (
            "status=COMPLETED\nalgorithm=" + algorithm + "\nfullEvaluations=" + evaluations
            + "\ninitialPopulationHash=" + initialHash + "\nstopReason=" + stopReason
            + "\nmechanismSummary=" + mechanism + "\nalgorithmRunNanos=" + runNanos
            + "\ndecoderCalls=" + timing.getSuccessfulDecoderCalls() + "\n")
            .getBytes(StandardCharsets.UTF_8));
      }
      hardGate(status, evaluations, maxFEs, front, problem.getDecoderTimingSnapshot());
      Files.write(partial.resolve("mechanism-summary.txt"),
          (mechanism + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-record.csv"), (
          "runId,seedSlot,seed,algorithm,status,FE,frontSize,initialPopulationHash,algorithmRunNanos\n"
              + "seed-" + seed + '-' + algorithm + ',' + seedSlot + ',' + seed + ','
              + algorithm + ',' + status + ',' + evaluations + ',' + front.size() + ','
              + initialHash + ',' + runNanos + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("console.log"), (
          "COMPLETED version=" + VERSION + " algorithm=" + algorithm + " seed=" + seed
              + " FE=" + evaluations + " elapsedNanos=" + (System.nanoTime() - start) + "\n")
          .getBytes(StandardCharsets.UTF_8));
      writeHashes(partial); move(partial, completed);
      System.out.println("P25D_COMPLETED algorithm=" + algorithm + " seed=" + seed
          + " FE=" + evaluations + " front=" + front.size() + " output=" + completed);
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

  private static void hardGate(String status, int evaluations, int maxFEs,
      List<double[]> front, ZhangBoDecoderTimingSnapshot timing) {
    if (!"COMPLETED".equals(status)) throw new IllegalStateException("RUN_FAILED");
    if (evaluations != maxFEs) throw new IllegalStateException("FE_NOT_EXACT=" + evaluations);
    if (timing.getSuccessfulDecoderCalls() != evaluations) {
      throw new IllegalStateException("DECODER_FE_MISMATCH="
          + timing.getSuccessfulDecoderCalls() + '/' + evaluations);
    }
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L) {
      throw new IllegalStateException("SHIFT_NOT_FROZEN");
    }
    if (front == null || front.isEmpty()) throw new IllegalStateException("EMPTY_FRONT");
    for (double[] point : front) for (double value : point) {
      if (!Double.isFinite(value)) throw new IllegalStateException("NON_FINITE_FRONT");
    }
  }

  private static String configurationText(Algorithm algorithm, int slot, long seed,
      int population, int maxFEs, String initialHash, Path instance, Path extension,
      Path fatigue, String adapterParameters) throws Exception {
    return "p25dVersion=" + VERSION + "\nalgorithm=" + algorithm + "\nseedSlot=" + slot
        + "\nseed=" + seed + "\ninstance=20_2_3_1\npopulation=" + population
        + "\nmaxFEs=" + maxFEs + "\ndecoderMode=FM3\nshiftMode=NONE"
        + "\nfamilyMode=DEGENERATE_SINGLE_FAMILY\nsetupMode=SEQUENCE_INDEPENDENT"
        + "\nobjectiveAdapter=0,1,6\ninitialPopulationHash=" + initialHash
        + "\ninstanceFileSha256=" + sha256(instance)
        + "\nsutExtensionFileSha256=" + sha256(extension.resolve("20_2_3_1.setup.txt"))
        + "\nfatigueParameterFileSha256=" + sha256(fatigue.resolve("20_2_3_1.fatigue.txt"))
        + "\nmaxFEsPaper=500000\nmaxFEsPilotApproved=50000"
        + "\nadapterSemantics=" + (algorithm == Algorithm.ZHANGBO_A4
            || algorithm == Algorithm.HMOPSO_QGS_F ? "V35_CANONICAL" : "STRUCTURED_PAPER_FAMILY_REWRITE")
        + "\nadapterParametersBegin\n" + (adapterParameters == null ? "TABLE9_V35\n" : adapterParameters)
        + "adapterParametersEnd\n";
  }

  private static void writeFront(Path path, List<double[]> front) throws IOException {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1])
        .append(',').append(point[2]).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  static long approvedSeed(int slot) {
    if (slot < 1 || slot > SEEDS.length) throw new IllegalArgumentException("seed-slot must be 1..5");
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
    Map<String, String> values = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try { values.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : values.entrySet()) {
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
    private int seedSlot; private Algorithm algorithm; private Path projectRoot; private Path output;
    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--seed-slot".equals(args[index])) value.seedSlot = Integer.parseInt(args[index + 1]);
        else if ("--algorithm".equals(args[index])) value.algorithm = Algorithm.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (value.seedSlot == 0 || value.algorithm == null || value.projectRoot == null
          || value.output == null) throw usage();
      approvedSeed(value.seedSlot); return value;
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --seed-slot 1..5 --algorithm <P25D algorithm> "
          + "--project-root <path> --output <path>");
    }
  }
}
