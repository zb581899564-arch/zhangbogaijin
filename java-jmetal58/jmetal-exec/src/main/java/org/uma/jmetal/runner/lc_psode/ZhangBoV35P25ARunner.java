package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** Scope-locked V35-P25A runner: one approved seed slot and one arm per JVM. */
public final class ZhangBoV35P25ARunner {
  public static final String VERSION = "v35-p25a-main-variant-gate-v1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 500000;
  private static final String INSTANCE = "20_2_3_1";
  private static final long[] SEEDS = {
      20260809L, 20260810L, 20260811L, 20260812L, 20260813L
  };
  private static final DateTimeFormatter FAILURE_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
  private static final Pattern NUMBER = Pattern.compile("(?:^|[,|])%s=(-?\\d+)(?:$|[,|])");

  public enum Arm {
    A0(V35AblationRegistry.Rung.A0_BASELINE),
    A4(V35AblationRegistry.Rung.A4_CA_TA_LITE),
    A5(V35AblationRegistry.Rung.A5_FULL);

    private final V35AblationRegistry.Rung rung;
    Arm(V35AblationRegistry.Rung rung) { this.rung = rung; }
    public V35AblationRegistry.Rung getRung() { return rung; }
  }

  private ZhangBoV35P25ARunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    run(parsed.seedSlot, parsed.arm, parsed.projectRoot, parsed.output, POPULATION, MAX_FES);
  }

  /** Package-private short-budget hook. Scientific CLI parameters remain immutable. */
  static Path runForTest(int seedSlot, Arm arm, Path projectRoot, Path output,
      int population, int maxFEs) throws Exception {
    return run(seedSlot, arm, projectRoot, output, population, maxFEs);
  }

  private static Path run(int seedSlot, Arm arm, Path projectRoot, Path output,
      int population, int maxFEs) throws Exception {
    if (arm == null || population <= 0 || maxFEs < population) {
      throw new IllegalArgumentException("invalid arm/population/budget");
    }
    long seed = approvedSeed(seedSlot);
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP/20_2_3_1.txt");
    Path extensionDirectory = javaProject.resolve("instance-extensions/v1");
    Path fatigueDirectory = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance, "instance");
    requireFile(extensionDirectory.resolve("20_2_3_1.setup.txt"), "SUT extension");
    requireFile(fatigueDirectory.resolve("20_2_3_1.fatigue.txt"), "fatigue parameters");

    String runId = "seed-" + seed + "-" + arm;
    Path seedDirectory = output.toAbsolutePath().normalize().resolve("runs/seed-" + seed);
    Files.createDirectories(seedDirectory);
    Path completed = seedDirectory.resolve(arm.name());
    if (Files.exists(completed)) {
      throw new IllegalStateException("result exists; refusing overwrite: " + completed);
    }
    Path partial = seedDirectory.resolve(".partial-" + arm + "-" + System.nanoTime());
    Files.createDirectory(partial);
    appendConsole(partial, "START version=" + VERSION + " runId=" + runId
        + " seedSlot=" + seedSlot + " seed=" + seed + " arm=" + arm
        + " population=" + population + " maxFEs=" + maxFEs);

    try {
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instance, ProductionDecodeMode.FM3, seed, extensionDirectory, fatigueDirectory,
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < population; index++) initial.add(problem.createSolution());
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  initial-four-vector-population\n").getBytes(StandardCharsets.UTF_8));

      V35ProductionConfiguration configuration = V35AblationRegistry.configFor(
          arm.rung, seed, population, maxFEs);
      String configurationText = "p25aVersion=" + VERSION + "\n"
          + "runId=" + runId + "\nseedSlot=" + seedSlot + "\nseed=" + seed + "\n"
          + "arm=" + arm + "\ninstance=" + INSTANCE + "\npopulation=" + population + "\n"
          + "maxFEs=" + maxFEs + "\ndecoderMode=FM3\n"
          + "familyMode=DEGENERATE_SINGLE_FAMILY\nsetupMode=SEQUENCE_INDEPENDENT\n"
          + "shiftMode=NONE\nobjectiveAdapter=0,1,6\n"
          + "instanceFileSha256=" + sha256(instance) + "\n"
          + "sutExtensionFileSha256="
          + sha256(extensionDirectory.resolve("20_2_3_1.setup.txt")) + "\n"
          + "fatigueParameterFileSha256="
          + sha256(fatigueDirectory.resolve("20_2_3_1.fatigue.txt")) + "\n"
          + "ablationRung=" + arm.rung.getLabel() + "\n"
          + "mechanismVectorHash=" + configuration.configurationHash() + "\n"
          + "p24RevisionSourceManifestSha256=" + optionalSha(
              project.resolve("docs/evidence/V35-P24.1/source-sha256.csv")) + "\n"
          + "v35ConfigurationBegin\n" + configuration.canonicalText()
          + "v35ConfigurationEnd\n";

      V35FairRunner.RunRecord record = V35FairRunner.run(arm.rung.getMode(), problem,
          P8InitialPopulationProvider.copy(initial), maxFEs, seed);
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("mechanism-summary.txt"),
          (record.getMechanismSummary() + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-record.csv"), runRecordCsv(runId, seedSlot, seed, arm,
          initialHash, record).getBytes(StandardCharsets.UTF_8));
      String failure = hardGate(arm, record, population, maxFEs);
      if (failure != null) {
        appendConsole(partial, "FAILED hardGate=" + failure);
        writeEvidenceHashes(partial);
        Path failed = seedDirectory.resolve("failed-" + arm + "-"
            + FAILURE_TIME.format(LocalDateTime.now()));
        move(partial, failed);
        throw new IllegalStateException(failure + "; evidence=" + failed);
      }
      appendConsole(partial, "COMPLETED FE=" + record.getFullEvaluations()
          + " frontSize=" + record.getFront().size() + " initialHash=" + initialHash);
      writeEvidenceHashes(partial);
      move(partial, completed);
      System.out.println("V35_P25A_RUN_COMPLETED runId=" + runId + " output=" + completed
          + " FE=" + record.getFullEvaluations() + " front=" + record.getFront().size());
      return completed;
    } catch (Exception error) {
      if (Files.exists(partial)) {
        appendConsole(partial, "EXCEPTION " + error.getClass().getName() + ": "
            + String.valueOf(error.getMessage()));
        writeEvidenceHashes(partial);
        Path failed = seedDirectory.resolve("failed-" + arm + "-"
            + FAILURE_TIME.format(LocalDateTime.now()));
        if (!Files.exists(failed)) move(partial, failed);
      }
      throw error;
    }
  }

  static String hardGate(Arm arm, V35FairRunner.RunRecord record,
      int population, int maxFEs) {
    if (!"COMPLETED".equals(record.getStatus())) return "RUN_FAILED=" + record.getStopReason();
    if (record.getFullEvaluations() < population || record.getFullEvaluations() > maxFEs) {
      return "FE_OUT_OF_RANGE=" + record.getFullEvaluations();
    }
    if (record.getFront().isEmpty()) return "EMPTY_FRONT";
    for (double[] point : record.getFront()) for (double value : point) {
      if (!Double.isFinite(value)) return "NON_FINITE_FRONT";
    }
    ZhangBoDecoderTimingSnapshot timing = record.getDecoderTiming();
    if (timing.getSuccessfulDecoderCalls() != record.getFullEvaluations()) {
      return "DECODER_FE_MISMATCH=" + timing.getSuccessfulDecoderCalls()
          + "/" + record.getFullEvaluations();
    }
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L
        || timing.getLeftFullRecomputations() != 0L || timing.getRightFullRecomputations() != 0L) {
      return "SHIFT_NOT_FROZEN";
    }
    String summary = record.getMechanismSummary();
    if (value(summary, "cfvfRepairs") != 0L) return "CFVF_REPAIR_NONZERO";
    if (value(summary, "qgSelections") <= 0L || value(summary, "pddrEvents") <= 0L) {
      return "QG_OR_PDDR_MISSING";
    }
    if (arm == Arm.A0) {
      if (value(summary, "formalQgRounds") <= 0L
          || value(summary, "baselineUpdateEvents") <= 0L
          || value(summary, "fixedNeighborhoodEvents") <= 0L
          || value(summary, "formalLocalFE") <= 0L) return "BASELINE_MECHANISM_MISSING";
      if (value(summary, "cfvfOffspring") != 0L || value(summary, "qpActions") != 0L
          || value(summary, "archiveInsertions") != 0L || value(summary, "caTaLiteFE") != 0L
          || value(summary, "directionalPoolRequests") != 0L
          || value(summary, "directionalPoolFiltered") != 0L) return "BASELINE_INNOVATION_LEAK";
    } else {
      if (value(summary, "cfvfOffspring") <= 0L || value(summary, "qpActions") <= 0L
          || value(summary, "archiveInsertions") <= 0L || value(summary, "caTaLiteTest") <= 0L
          || value(summary, "caTaLiteApply") <= 0L || value(summary, "dualQP") <= 0L
          || value(summary, "dualQG") <= 0L || dscrValue(summary, "teacherUses") <= 0L
          || dscrValue(summary, "dominatedTeacherUses") != 0L) {
        return "FULL_CHAIN_MISSING_OR_DSCR_UNSAFE";
      }
      if (arm == Arm.A4 && (value(summary, "directionalPoolRequests") != 0L
          || value(summary, "directionalPoolFiltered") != 0L)) return "A4_POOL_LEAK";
      if (arm == Arm.A5 && (value(summary, "directionalPoolRequests") <= 0L
          || (population >= POPULATION && value(summary, "directionalPoolFiltered") <= 0L))) {
        return "A5_DIRECTIONAL_POOL_NOT_TRIGGERED";
      }
    }
    return null;
  }

  static long value(String summary, String key) {
    Pattern pattern = Pattern.compile(String.format(Locale.ROOT, NUMBER.pattern(),
        Pattern.quote(key)));
    Matcher matcher = pattern.matcher(summary == null ? "" : summary);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : Long.MIN_VALUE;
  }

  static long dscrValue(String summary, String key) { return value(summary, key); }

  private static String runRecordCsv(String runId, int slot, long seed, Arm arm,
      String initialHash, V35FairRunner.RunRecord record) {
    return "runId,seedSlot,seed,arm,status,FE,frontSize,initialPopulationHash,algorithmRunNanos\n"
        + runId + ',' + slot + ',' + seed + ',' + arm + ',' + record.getStatus() + ','
        + record.getFullEvaluations() + ',' + record.getFront().size() + ',' + initialHash + ','
        + record.getAlgorithmRunNanos() + "\n";
  }

  static long approvedSeed(int slot) {
    if (slot < 1 || slot > SEEDS.length) throw new IllegalArgumentException("seed-slot must be 1..5");
    return SEEDS[slot - 1];
  }

  private static void requireFile(Path path, String label) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException(label + " missing: " + path);
  }

  private static String optionalSha(Path path) throws Exception {
    return Files.isRegularFile(path) ? sha256(path) : "NOT_STAGED";
  }

  private static void appendConsole(Path directory, String line) throws IOException {
    Files.write(directory.resolve("console.log"),
        (line + "\n").getBytes(StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
  }

  private static void writeEvidenceHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    java.util.stream.Stream<Path> walk = Files.walk(directory);
    try {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try { hashes.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    } finally { walk.close(); }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"),
        text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static void move(Path source, Path target) throws IOException {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }

  private static final class Arguments {
    private final int seedSlot;
    private final Arm arm;
    private final Path projectRoot;
    private final Path output;
    private Arguments(int seedSlot, Arm arm, Path projectRoot, Path output) {
      this.seedSlot = seedSlot; this.arm = arm; this.projectRoot = projectRoot; this.output = output;
    }
    private static Arguments parse(String[] args) {
      Integer slot = null; Arm arm = null; Path project = null; Path output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--seed-slot".equals(args[index])) slot = Integer.valueOf(args[index + 1]);
        else if ("--arm".equals(args[index])) arm = Arm.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) project = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (slot == null || arm == null || project == null || output == null) throw usage();
      approvedSeed(slot);
      return new Arguments(slot, arm, project, output);
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --seed-slot 1..5 --arm A0|A4|A5 "
          + "--project-root <path> --output <path>");
    }
  }
}
