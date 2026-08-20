package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EAlgorithmResult;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EOfficialJMetalEngine;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EPaperAuthorEngine;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** One-algorithm/one-JVM entry for the corrected P25E paper comparison. */
public final class ZhangBoV35P25ECorrectedComparisonRunner {
  public static final String VERSION = "v35-p25e-faithful-paper-comparison-v1";
  public static final long SEED = 20260822L;
  public static final int POPULATION = 100;
  public static final int MAX_FES = 50000;

  public enum Algorithm {
    ZHANGBO_A4("ZHANGBO_CURRENT"),
    HMOPSO_QGS_F("ZHANGBO_CURRENT"),
    HMOPSO_QLS_F("PAPER_AUTHOR_SOURCE"),
    MOPSO_F("PAPER_AUTHOR_SOURCE"),
    MOPSODS_DE_F("PAPER_AUTHOR_SOURCE"),
    MOHEADE_F("PAPER_AUTHOR_SOURCE"),
    NSGA_II_F("OFFICIAL_JMETAL_CORE"),
    SPEA2_F("OFFICIAL_JMETAL_CORE");
    private final String sourceKind;
    Algorithm(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceKind() { return sourceKind; }
  }

  private ZhangBoV35P25ECorrectedComparisonRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    run(value.algorithm, value.projectRoot, value.output, POPULATION, value.maxFEs, value.seed,
        value.instance);
  }

  static Path runForTest(Algorithm algorithm, Path projectRoot, Path output,
      int population, int maxFEs, long seed) throws Exception {
    return run(algorithm, projectRoot, output, population, maxFEs, seed, "20_2_3_1");
  }

  private static Path run(Algorithm algorithm, Path projectRoot, Path output,
      int population, int maxFEs, long seed, String instanceName) throws Exception {
    if (algorithm == null || population != 100 || maxFEs < population) {
      throw new IllegalArgumentException("P25E requires Table 9 population=100 and a valid budget");
    }
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP/" + instanceName + ".txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance);
    requireFile(extension.resolve(instanceName + ".setup.txt"));
    requireFile(fatigue.resolve(instanceName + ".fatigue.txt"));
    staticIdentityGate(javaProject, algorithm);

    Path target = output.toAbsolutePath().normalize().resolve("runs/seed-" + seed)
        .resolve(algorithm.name());
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    Path partial = target.getParent().resolve(".partial-" + algorithm + '-' + System.nanoTime());
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

      Outcome outcome;
      // FC-6A.1: enable the pure-observation PDDR composition audit for the
      // ZhangBoMOHPSOQ-based arms (QGS baseline and A4) so every environmental
      // selection round logs its pool's score-class composition.
      boolean zhangboArm = algorithm == Algorithm.ZHANGBO_A4
          || algorithm == Algorithm.HMOPSO_QGS_F;
      if (zhangboArm) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
            .setEnabled(true);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
            .reset();
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
            .setSeed(seed);
        // FC-6A.2: 174.44 反事实探针（20-job seed22；FC-5.2 record 655 三元组）。
        if ("20_2_3_1".equals(instanceName) && seed == 20260822L) {
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
              .setRegionProbe(174.43665028596877, 11123.472680537456,
                  15044.462631959621);
        }
      }
      if (algorithm == Algorithm.ZHANGBO_A4 || algorithm == Algorithm.HMOPSO_QGS_F) {
        V35FairRunner.Mode mode = algorithm == Algorithm.ZHANGBO_A4
            ? V35FairRunner.Mode.V35_FULL_POOL_OFF : V35FairRunner.Mode.V35_BASELINE;
        V35FairRunner.RunRecord record = V35FairRunner.run(mode, problem,
            P8InitialPopulationProvider.copy(initial), maxFEs, seed, false,
            V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), true);
        if (!"COMPLETED".equals(record.getStatus())) {
          throw new IllegalStateException(record.getStopReason());
        }
        outcome = new Outcome(record.getFullEvaluations(), record.getAlgorithmRunNanos(),
            record.getFront(), algorithm.sourceKind,
            "org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ",
            record.getMechanismSummary(), 0);
        V35FairRunner.writeRecord(record, partial, configurationText(javaProject, algorithm,
            seed, population, maxFEs, initialHash, instance, extension, fatigue, outcome));
      } else {
        V35ComparisonProblemAdapter.ObjectiveView view = isOfficial(algorithm)
            ? V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE
            : V35ComparisonProblemAdapter.ObjectiveView.AUTHOR_SEVEN_SLOT;
        V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(problem,
            P8InitialPopulationProvider.copy(initial), view, maxFEs);
        V35P25EAlgorithmResult record = isOfficial(algorithm)
            ? V35P25EOfficialJMetalEngine.run(official(algorithm), adapter,
                population, maxFEs, seed)
            : V35P25EPaperAuthorEngine.run(author(algorithm), adapter,
                population, maxFEs, seed);
        outcome = new Outcome(record.getEvaluations(), record.getRunNanos(), record.getFront(),
            record.getSourceKind(), record.getImplementationClass(),
            record.getIdentityEvidence(), adapter.getRepresentationRepairs());
        Files.write(partial.resolve("configuration.txt"), configurationText(javaProject,
            algorithm, seed, population, maxFEs, initialHash, instance, extension, fatigue,
            outcome).getBytes(StandardCharsets.UTF_8));
        writeFront(partial.resolve("front.csv"), outcome.front);
      }

      hardGate(outcome, problem.getDecoderTimingSnapshot(), maxFEs);
      String diagText = "";
      if (zhangboArm) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
            .current().finish(outcome.front);
        diagText = "\n"
            + org.uma.jmetal.algorithm.multiobjective.mypso.v35
                .V35Fc6BpPddrDiagnosticAudit.current().fc6DiagText();
      }
      appendStatus(partial.resolve("status.properties"), algorithm, seed, initialHash,
          outcome, problem.getDecoderTimingSnapshot());
      Files.write(partial.resolve("algorithm-identity.txt"),
          identityText(javaProject, algorithm, outcome).getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("mechanism-summary.txt"),
          (outcome.identityEvidence + diagText + "\n").getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-record.csv"), (
          "runId,seed,algorithm,sourceKind,status,FE,frontSize,initialPopulationHash,runNanos,representationRepairs\n"
              + "seed-" + seed + '-' + algorithm + ',' + seed + ',' + algorithm + ','
              + outcome.sourceKind + ",COMPLETED," + outcome.evaluations + ','
              + outcome.front.size() + ',' + initialHash + ',' + outcome.runNanos + ','
              + outcome.representationRepairs + "\n").getBytes(StandardCharsets.UTF_8));
      writeHashes(partial);
      move(partial, target);
      System.out.println("P25E_COMPLETED algorithm=" + algorithm + " seed=" + seed
          + " FE=" + outcome.evaluations + " front=" + outcome.front.size());
      return target;
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

  private static void hardGate(Outcome outcome, ZhangBoDecoderTimingSnapshot timing,
      int maxFEs) {
    if (outcome.evaluations <= 0 || outcome.evaluations > maxFEs) {
      throw new IllegalStateException("FE_OUT_OF_BOUNDS=" + outcome.evaluations);
    }
    if (timing.getSuccessfulDecoderCalls() != outcome.evaluations) {
      throw new IllegalStateException("DECODER_FE_MISMATCH="
          + timing.getSuccessfulDecoderCalls() + '/' + outcome.evaluations);
    }
    if (timing.getLeftShiftNanos() != 0L || timing.getRightShiftNanos() != 0L) {
      throw new IllegalStateException("SHIFT_NOT_FROZEN");
    }
    if (outcome.front.isEmpty()) throw new IllegalStateException("EMPTY_FRONT");
    for (double[] point : outcome.front) {
      if (point.length != 3) throw new IllegalStateException("OBJECTIVE_ADAPTER_NOT_016");
      for (double value : point) if (!Double.isFinite(value)) {
        throw new IllegalStateException("NON_FINITE_FRONT");
      }
    }
  }

  private static String configurationText(Path javaProject, Algorithm algorithm, long seed,
      int population, int maxFEs, String initialHash, Path instance, Path extension,
      Path fatigue, Outcome outcome) throws Exception {
    return "p25eVersion=" + VERSION + "\nalgorithm=" + algorithm + "\nsourceKind="
        + outcome.sourceKind + "\nimplementationClass=" + outcome.implementationClass
        + "\nseed=" + seed + "\ninstance=" + instance.getFileName().toString().replace(".txt", "")
        + "\npopulation=" + population
        + "\nmaxFEs=" + maxFEs + "\ndecoderMode=FM3\nshiftMode=NONE"
        + "\nfamilyMode=DEGENERATE_SINGLE_FAMILY\nsetupMode=SEQUENCE_INDEPENDENT"
        + "\nobjectiveAdapter=0,1,6\nsharedProblemOnly=true"
        + "\nsearchMechanismsIndependent=true\ninitialPopulationHash=" + initialHash
        + "\ninstanceFileSha256=" + sha256(instance)
        + "\nsutExtensionFileSha256=" + sha256(extension.resolve(
            instance.getFileName().toString().replace(".txt", ".setup.txt")))
        + "\nfatigueParameterFileSha256=" + sha256(fatigue.resolve(
            instance.getFileName().toString().replace(".txt", ".fatigue.txt")))
        + "\nlegacyP25DExcluded=true\nqmoea=PENDING_SOURCE_VERIFICATION"
        + "\ntable9Parameters=" + table9(algorithm)
        + "\nactualSourceSha256=" + actualSourceSha(javaProject, algorithm) + "\n";
  }

  private static String identityText(Path javaProject, Algorithm algorithm, Outcome outcome)
      throws Exception {
    return "paperLabel=" + algorithm + "\nsourceKind=" + outcome.sourceKind
        + "\nimplementationClass=" + outcome.implementationClass
        + "\nactualSourceSha256=" + actualSourceSha(javaProject, algorithm)
        + "\nadaptationWhitelist=Problem,Solution,initialPopulation,randomSource,FE,logging"
        + "\nforbiddenEnhancements=ZhangBoBaselineUpdater,CFVF,Qp,DSCR,CA-TA-Lite,directionalTeacherPool"
        + "\nidentityEvidence=" + outcome.identityEvidence + "\n";
  }

  private static void appendStatus(Path path, Algorithm algorithm, long seed,
      String initialHash, Outcome outcome, ZhangBoDecoderTimingSnapshot timing)
      throws IOException {
    String text = "p25eStatus=COMPLETED\np25eAlgorithm=" + algorithm + "\np25eSeed=" + seed
        + "\np25eSourceKind=" + outcome.sourceKind + "\np25eFullEvaluations="
        + outcome.evaluations + "\np25eDecoderCalls=" + timing.getSuccessfulDecoderCalls()
        + "\np25eInitialPopulationHash=" + initialHash + "\np25eRepresentationRepairs="
        + outcome.representationRepairs + "\n";
    Files.write(path, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
  }

  private static String table9(Algorithm algorithm) {
    if (algorithm == Algorithm.NSGA_II_F || algorithm == Algorithm.SPEA2_F) {
      return V35P25EOfficialJMetalEngine.canonicalParameters(official(algorithm));
    }
    if (algorithm == Algorithm.HMOPSO_QLS_F || algorithm == Algorithm.MOPSO_F
        || algorithm == Algorithm.MOPSODS_DE_F || algorithm == Algorithm.MOHEADE_F) {
      return V35P25EPaperAuthorEngine.canonicalParameters(author(algorithm));
    }
    return "Table9FormalHmopsoQgs;population=100;QTimes=50;LSTimes=30";
  }

  private static boolean isOfficial(Algorithm value) {
    return value == Algorithm.NSGA_II_F || value == Algorithm.SPEA2_F;
  }
  private static V35P25EOfficialJMetalEngine.Algorithm official(Algorithm value) {
    return value == Algorithm.NSGA_II_F
        ? V35P25EOfficialJMetalEngine.Algorithm.NSGA_II_F
        : V35P25EOfficialJMetalEngine.Algorithm.SPEA2_F;
  }
  private static V35P25EPaperAuthorEngine.AlgorithmKind author(Algorithm value) {
    return V35P25EPaperAuthorEngine.AlgorithmKind.valueOf(value.name());
  }

  private static void staticIdentityGate(Path javaProject, Algorithm algorithm)
      throws IOException {
    if (algorithm == Algorithm.ZHANGBO_A4 || algorithm == Algorithm.HMOPSO_QGS_F) return;
    String text = new String(Files.readAllBytes(actualSource(javaProject, algorithm)),
        StandardCharsets.UTF_8);
    String[] forbidden = {"V35P25DComparativeEngine", "ZhangBoBaselineUpdater",
        "ZhangBoCfvf", "ZhangBoQp", "V35Dscr", "V35CaTaLite",
        "DirectionalTeacherPool"};
    for (String token : forbidden) if (text.contains(token)) {
      throw new IllegalStateException(algorithm + " forbidden enhancement reference=" + token);
    }
  }

  private static Path actualSource(Path javaProject, Algorithm algorithm) {
    Path root = javaProject.resolve("jmetal-algorithm/src/main/java");
    if (algorithm == Algorithm.NSGA_II_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58NSGAII.java");
    if (algorithm == Algorithm.SPEA2_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58SPEA2.java");
    if (algorithm == Algorithm.MOHEADE_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mymohea/P25EAuthorMOHEADE.java");
    if (algorithm == Algorithm.HMOPSO_QLS_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSODivSubDE.java");
    if (algorithm == Algorithm.MOPSODS_DE_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSODivSub.java");
    if (algorithm == Algorithm.MOPSO_F) return root.resolve(
        "org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSO.java");
    return root.resolve("org/uma/jmetal/algorithm/multiobjective/mypso/ZhangBoMOHPSOQ.java");
  }
  private static String actualSourceSha(Path javaProject, Algorithm algorithm) throws Exception {
    return sha256(actualSource(javaProject, algorithm));
  }

  private static void writeFront(Path path, List<double[]> front) throws IOException {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1])
        .append(',').append(point[2]).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
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
    Files.write(directory.resolve("evidence-sha256.tsv"),
        out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static final class Outcome {
    private final int evaluations; private final long runNanos;
    private final List<double[]> front; private final String sourceKind;
    private final String implementationClass; private final String identityEvidence;
    private final int representationRepairs;
    private Outcome(int evaluations, long runNanos, List<double[]> front, String sourceKind,
        String implementationClass, String identityEvidence, int representationRepairs) {
      this.evaluations = evaluations; this.runNanos = runNanos; this.front = front;
      this.sourceKind = sourceKind; this.implementationClass = implementationClass;
      this.identityEvidence = identityEvidence; this.representationRepairs = representationRepairs;
    }
  }
  private static final class Arguments {
    private Algorithm algorithm; private Path projectRoot; private Path output;
    private long seed = SEED;
    private int maxFEs = MAX_FES;
    private String instance = "20_2_3_1";
    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--algorithm".equals(args[index])) value.algorithm = Algorithm.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else if ("--seed".equals(args[index])) value.seed = Long.parseLong(args[index + 1]);
        else if ("--max-fes".equals(args[index])) value.maxFEs = Integer.parseInt(args[index + 1]);
        else if ("--instance".equals(args[index])) value.instance = args[index + 1];
        else throw usage();
      }
      if (value.algorithm == null || value.projectRoot == null || value.output == null) throw usage();
      return value;
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --algorithm <P25E algorithm> "
          + "--project-root <path> --output <path> [--seed <long>] [--max-fes <int>] "
          + "[--instance <name>]");
    }
  }
}
