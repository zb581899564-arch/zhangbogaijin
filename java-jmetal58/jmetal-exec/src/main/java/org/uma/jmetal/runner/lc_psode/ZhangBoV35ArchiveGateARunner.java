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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ArchiveExperimentArtifacts;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ArchiveExperimentProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ArchiveExperimentRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/** Fail-closed ND0 Gate-A runner.  It cannot expose ND1--ND4. */
public final class ZhangBoV35ArchiveGateARunner {
  public static final String VERSION = "v35-nd0-gate-a-v1";

  private ZhangBoV35ArchiveGateARunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    run(value.projectRoot, value.output, value.instance, value.seed);
  }

  static Path runForTest(Path projectRoot, Path output, String instance, long seed)
      throws Exception {
    return run(projectRoot, output, instance, seed);
  }

  private static Path run(Path projectRoot, Path output, String requestedInstance, long seed)
      throws Exception {
    Spec spec = Spec.of(requestedInstance, seed);
    Path root = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(root.resolve("EADHFSP"))
        ? root : root.resolve("java-jmetal58");
    Path dataRoot = spec.instance.equals("10_2_2_1")
        ? javaProject.resolve("p8-bridge/v1") : javaProject;
    Path instance = dataRoot.resolve("EADHFSP/" + spec.instance + ".txt");
    Path extension = dataRoot.resolve("instance-extensions/v1");
    Path fatigue = dataRoot.resolve("fatigue-parameters/v1");
    require(instance);
    require(extension.resolve(spec.instance + ".setup.txt"));
    require(fatigue.resolve(spec.instance + ".fatigue.txt"));

    Path completed = output.toAbsolutePath().normalize();
    if (Files.exists(completed)) throw new IllegalStateException("refusing overwrite: " + completed);
    Files.createDirectories(completed.getParent());
    Path partial = completed.resolveSibling(".partial-" + completed.getFileName() + "-"
        + System.nanoTime());
    Files.createDirectories(partial);
    try {
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instance, ProductionDecodeMode.FM3, seed, extension, fatigue,
          ZhangBoShiftConfiguration.none());
      JMetalRandom.getInstance().setSeed(seed);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < spec.population; index++) initial.add(problem.createSolution());
      String initialHash = V35FairRunner.initialHash(initial);
      V35FairRunner.RunRecord record = V35ArchiveExperimentRunner.runGateAudit(problem,
          P8InitialPopulationProvider.copy(initial), spec.maxEvaluations, seed);
      verify(record, initialHash, spec);
      String configuration = "gateARunnerVersion=" + VERSION + '\n'
          + "gate=ND0_PURE_OBSERVATION\narchiveMode=UNBOUNDED_FULL\n"
          + "instance=" + spec.instance + "\nseed=" + seed + "\npopulation="
          + spec.population + "\nmaxFEs=" + spec.maxEvaluations + "\n"
          + "decoderMode=FM3\nshiftMode=NONE\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
          + "setupMode=SEQUENCE_INDEPENDENT\ninitialPopulationHash=" + initialHash + '\n';
      V35ArchiveExperimentRunner.writeRecord(record,
          V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL,
          partial, seed, spec.population, spec.maxEvaluations);
      Files.write(partial.resolve("gate-a-run-configuration.txt"),
          configuration.getBytes(StandardCharsets.UTF_8));
      writeGateSummary(partial, record, spec);
      writeBehaviorHashes(partial, record);
      writeHashes(partial);
      move(partial, completed);
      String gate = record.getArchiveExperimentArtifacts()
          .isDecisionEqualsObservedAfterExactDedup() ? "PASSED" : "BLOCKED";
      System.out.println("V35_ARCHIVE_GATE_A_" + gate + " instance=" + spec.instance
          + " seed=" + seed + " FE=" + record.getFullEvaluations()
          + " output=" + completed);
      return completed;
    } catch (Exception error) {
      Files.write(partial.resolve("failure.txt"),
          (error.getClass().getName() + ": " + error.getMessage() + '\n')
              .getBytes(StandardCharsets.UTF_8));
      writeHashes(partial);
      throw error;
    }
  }

  private static void verify(V35FairRunner.RunRecord record, String initialHash, Spec spec) {
    require("COMPLETED".equals(record.getStatus()), "status=" + record.getStatus());
    require(initialHash.equals(record.getInitialPopulationHash()), "initial population drift");
    require(record.getFullEvaluations() > 0
        && record.getFullEvaluations() <= spec.maxEvaluations, "FE outside budget");
    require(record.getDecoderCalls() == record.getFullEvaluations(), "decoder/FE mismatch");
    require(record.getIllegalSolutions() == 0, "illegal solution observed");
    require(record.getDuplicateEvaluations() == 0, "duplicate evaluation observed");
    require(!record.getFront().isEmpty(), "empty decision front");
    V35ArchiveExperimentArtifacts artifacts = record.getArchiveExperimentArtifacts();
    require(artifacts != null, "missing archive artifacts");
    require(record.getPassiveObservedCount() == record.getFullEvaluations(),
        "passive observation count does not close against FE");
  }

  private static void writeGateSummary(Path output, V35FairRunner.RunRecord record, Spec spec)
      throws IOException {
    V35ArchiveExperimentArtifacts artifacts = record.getArchiveExperimentArtifacts();
    String summary = artifacts.getAuditSummary();
    double archiveNanos = number(summary, "archiveUpdateNanos")
        + number(summary, "teacherPipelineNanos");
    double archivePercent = record.getAlgorithmRunNanos() == 0L ? Double.NaN
        : archiveNanos / record.getAlgorithmRunNanos();
    List<Double> regrets = teacherRegrets(artifacts.getAuditEventsCsv());
    double median = percentile(regrets, 0.50);
    double p95 = percentile(regrets, 0.95);
    String result = "schema=" + VERSION + '\n'
        + "instance=" + spec.instance + "\nseed=" + spec.seed + '\n'
        + "status=" + record.getStatus() + "\nfullEvaluations=" + record.getFullEvaluations()
        + "\ndecoderCalls=" + record.getDecoderCalls() + '\n'
        + "archiveGateA="
        + (artifacts.isDecisionEqualsObservedAfterExactDedup() ? "PASSED" : "BLOCKED") + '\n'
        + "decisionEqualsObservedAfterExactDedup="
        + artifacts.isDecisionEqualsObservedAfterExactDedup() + '\n'
        + "nearDuplicateRate0_1Pct=" + number(summary, "nearDuplicateRate0_1Pct") + '\n'
        + "archiveAndTeacherTimePercent=" + archivePercent + '\n'
        + "teacherDirectionalRegretMedian=" + median + '\n'
        + "teacherDirectionalRegretP95=" + p95 + '\n'
        + "teacherRegretSampleCount=" + regrets.size() + '\n'
        + "nd1Nd4Started=false\n";
    Files.write(output.resolve("gate-a-result.properties"),
        result.getBytes(StandardCharsets.UTF_8));
  }

  private static List<Double> teacherRegrets(String csv) {
    List<Double> values = new ArrayList<>();
    for (String line : csv.split("\\r?\\n")) {
      if (!line.startsWith("TEACHER,")) continue;
      String[] fields = line.split(",");
      if (fields.length < 27) continue;
      try {
        double value = Double.parseDouble(fields[fields.length - 8]);
        if (Double.isFinite(value)) values.add(value);
      } catch (NumberFormatException ignored) { }
    }
    Collections.sort(values);
    return values;
  }

  private static double percentile(List<Double> values, double probability) {
    if (values.isEmpty()) return Double.NaN;
    int index = (int) Math.ceil(probability * values.size()) - 1;
    return values.get(Math.max(0, Math.min(index, values.size() - 1)));
  }

  private static double number(String properties, String key) {
    String marker = key + '=';
    for (String line : properties.split("\\r?\\n")) {
      if (line.startsWith(marker)) return Double.parseDouble(line.substring(marker.length()));
    }
    return Double.NaN;
  }

  private static void writeBehaviorHashes(Path output, V35FairRunner.RunRecord record)
      throws Exception {
    String summary = record.getMechanismSummary();
    String front = new String(Files.readAllBytes(output.resolve("front.csv")),
        StandardCharsets.UTF_8);
    String text = "initialPopulationHash=" + record.getInitialPopulationHash() + '\n'
        + "evaluationTraceHash=" + record.getEvaluationTraceHash() + '\n'
        + "frontHash=" + sha256(front.getBytes(StandardCharsets.UTF_8)) + '\n'
        + "p6EventStreamHash=" + field(summary, "p6EventStreamHash") + '\n'
        + "pddrEventStreamHash=" + field(summary, "pddrEventStreamHash") + '\n'
        + "qgEventStreamHash=" + field(summary, "qgEventStreamHash") + '\n'
        + "qgTableHash=" + field(summary, "qgTableHash") + '\n'
        + "qpEventStreamHash=" + field(summary, "qpEventStreamHash") + '\n'
        + "qpTableHash=" + field(summary, "qpTableHash") + '\n'
        + "caTaEventStreamHash=" + field(summary, "caTaEventStreamHash") + '\n'
        + "fullEvaluations=" + record.getFullEvaluations() + '\n';
    Files.write(output.resolve("behavior-hashes.properties"),
        text.getBytes(StandardCharsets.UTF_8));
  }

  private static String field(String summary, String key) {
    String marker = key + '=';
    int start = summary.indexOf(marker);
    if (start < 0) return "NOT_AVAILABLE";
    start += marker.length();
    int end = summary.indexOf(',', start);
    return summary.substring(start, end < 0 ? summary.length() : end);
  }

  private static void writeHashes(Path directory) throws Exception {
    List<Path> files = new ArrayList<>();
    Files.walk(directory).filter(Files::isRegularFile)
        .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
        .forEach(files::add);
    Collections.sort(files);
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Path file : files) {
      out.append(directory.relativize(file).toString().replace('\\', '/')).append('\t')
          .append(sha256(Files.readAllBytes(file))).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"),
        out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(byte[] value) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
    StringBuilder out = new StringBuilder();
    for (byte item : digest) out.append(String.format("%02x", item & 0xff));
    return out.toString();
  }

  private static void move(Path source, Path destination) throws IOException {
    try {
      Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ignored) {
      Files.move(source, destination);
    }
  }

  private static void require(Path file) {
    if (!Files.isRegularFile(file)) throw new IllegalArgumentException("missing file: " + file);
  }

  private static void require(boolean value, String message) {
    if (!value) throw new IllegalStateException(message);
  }

  private static final class Spec {
    private final String instance;
    private final long seed;
    private final int population;
    private final int maxEvaluations;
    private Spec(String instance, long seed, int population, int maxEvaluations) {
      this.instance = instance; this.seed = seed; this.population = population;
      this.maxEvaluations = maxEvaluations;
    }
    private static Spec of(String value, long seed) {
      String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
      if ("I1".equals(normalized) || "10_2_2_1".equals(normalized)) {
        if (seed != 20260808L) throw new IllegalArgumentException("I1 Gate A seed is 20260808");
        return new Spec("10_2_2_1", seed, 10, 5000);
      }
      if ("20_2_3_1".equals(normalized)) {
        if (seed < 20260822L || seed > 20260824L) {
          throw new IllegalArgumentException("20_2_3_1 Gate A seeds are 20260822..20260824");
        }
        return new Spec("20_2_3_1", seed, 100, 20000);
      }
      throw new IllegalArgumentException("Gate A supports only I1 or 20_2_3_1");
    }
  }

  private static final class Arguments {
    private Path projectRoot = Paths.get(".");
    private Path output;
    private String instance;
    private long seed = Long.MIN_VALUE;
    private static Arguments parse(String[] args) {
      Arguments result = new Arguments();
      for (int index = 0; index < args.length; index++) {
        String argument = args[index];
        if ("--project-root".equals(argument)) result.projectRoot = Paths.get(args[++index]);
        else if ("--output".equals(argument)) result.output = Paths.get(args[++index]);
        else if ("--instance".equals(argument)) result.instance = args[++index];
        else if ("--seed".equals(argument)) result.seed = Long.parseLong(args[++index]);
        else throw new IllegalArgumentException("unknown argument: " + argument);
      }
      if (result.output == null || result.instance == null || result.seed == Long.MIN_VALUE) {
        throw new IllegalArgumentException(
            "usage: --project-root <path> --output <path> --instance I1|20_2_3_1 --seed <seed>");
      }
      return result;
    }
  }
}
