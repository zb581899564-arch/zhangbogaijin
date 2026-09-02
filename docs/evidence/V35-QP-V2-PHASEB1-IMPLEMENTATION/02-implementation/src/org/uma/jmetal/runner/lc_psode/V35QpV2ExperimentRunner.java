package org.uma.jmetal.runner.lc_psode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFePacingRepairProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalArchiveConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpTopKConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpV2Profile;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpV2TelemetrySink;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * Snapshot-bound standalone runner for Qp-v2 Phase B1 Candidate A investigation.
 *
 * <p>CLI: {@code --instance <name> --seed <long> --profile REF_A4|QP_V2_K1|QP_V2_K2|QP_V2_K3|QP_V2_K4
 * --max-fes <int> --snapshot <path> --output <path> [--telemetry OFF|ON]}.
 */
public final class V35QpV2ExperimentRunner {

  public static final String VERSION = "v35-qp-v2-experiment-runner-v1";
  public static final String BUDGET_VERSION = "v35-phase-consistent-budget-v1";
  private static final int POPULATION = 100;

  private V35QpV2ExperimentRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    execute(parsed);
  }

  public static void execute(Arguments args) throws Exception {
    V35QpV2Profile profile = V35QpV2Profile.fromString(args.profile);
    V35QpTopKConfiguration topKConfig = profile.toConfiguration();

    Path outputDir = args.output.toAbsolutePath().normalize();
    if (Files.exists(outputDir)) {
      throw new IllegalStateException("Refusing overwrite: " + outputDir);
    }
    Files.createDirectories(outputDir.getParent());
    Path partialDir = outputDir.resolveSibling(".partial-" + outputDir.getFileName().toString() + "-" + System.nanoTime());
    Files.createDirectory(partialDir);

    Path instance = findPath(args.instance,
        "inputs/java-jmetal58/EADHFSP/" + args.instance + ".txt",
        "EADHFSP/" + args.instance + ".txt",
        "../../inputs/java-jmetal58/EADHFSP/" + args.instance + ".txt",
        "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/08-observer-v5-schema-correction/02-local-tests/gate-workdir/inputs/java-jmetal58/EADHFSP/" + args.instance + ".txt",
        "java-jmetal58/EADHFSP/" + args.instance + ".txt");
    Path setup = findPath(args.instance + ".setup.txt",
        "inputs/java-jmetal58/instance-extensions/v1/" + args.instance + ".setup.txt",
        "instance-extensions/" + args.instance + ".setup.txt",
        "../../inputs/java-jmetal58/instance-extensions/v1/" + args.instance + ".setup.txt",
        "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/08-observer-v5-schema-correction/02-local-tests/gate-workdir/inputs/java-jmetal58/instance-extensions/v1/" + args.instance + ".setup.txt",
        "java-jmetal58/instance-extensions/v1/" + args.instance + ".setup.txt");
    Path fatigue = findPath(args.instance + ".fatigue.txt",
        "inputs/java-jmetal58/fatigue-parameters/v1/" + args.instance + ".fatigue.txt",
        "fatigue-parameters/" + args.instance + ".fatigue.txt",
        "../../inputs/java-jmetal58/fatigue-parameters/v1/" + args.instance + ".fatigue.txt",
        "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/08-observer-v5-schema-correction/02-local-tests/gate-workdir/inputs/java-jmetal58/fatigue-parameters/v1/" + args.instance + ".fatigue.txt",
        "java-jmetal58/fatigue-parameters/v1/" + args.instance + ".fatigue.txt");

    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, args.seed, setup.getParent(), fatigue.getParent(),
        ZhangBoShiftConfiguration.none());

    Path snapshot = args.snapshot.toAbsolutePath().normalize();
    if (!Files.exists(snapshot)) {
      throw new IllegalStateException("Snapshot not found: " + snapshot);
    }
    List<PermutationSolution<Integer>> initial =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, problem);
    if (initial.size() != POPULATION) {
      throw new IllegalStateException("Snapshot population size mismatch: " + initial.size() + ", expected: " + POPULATION);
    }
    String v35Hash = V35FairRunner.initialHash(initial);
    String p8Hash = P8InitialPopulationProvider.sha256(initial);

    V35QpV2TelemetrySink telemetrySink = new V35QpV2TelemetrySink();

    V35ProductionConfiguration configuration = V35LocalFePacingRepairProfile.configurationFor(
        V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN, args.seed, POPULATION, args.maxFes);

    long qpSeed = args.seed ^ 0x515042455354L;
    ZhangBoQpController qpController = new ZhangBoQpController(
        ZhangBoQpConfiguration.standard(),
        ZhangBoPersonalArchiveConfiguration.standard(),
        new JavaRandomGenerator(qpSeed), qpSeed,
        false, topKConfig);

    if ("ON".equalsIgnoreCase(args.telemetry)) {
      qpController.setTelemetrySink(telemetrySink);
    }

    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, initial.size(),
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(configuration)
        .setMaxIterations(args.maxFes)
        .setInitialSwarmOverride(initial)
        .build();

    algorithm.setCustomQpController(qpController);

    long wallStart = System.nanoTime();
    algorithm.run();
    long wallNanos = System.nanoTime() - wallStart;

    List<PermutationSolution<Integer>> result = algorithm.getResult();
    long actualFE = problem.getEvaluationCounter().getSuccessfulEvaluations();

    // Write front.csv (canonical format: Cmax;TEC;TWC)
    Path frontCsv = partialDir.resolve("front.csv");
    writeFrontCsv(frontCsv, result);

    // Write telemetry if enabled
    Path telemetryCsv = partialDir.resolve("qp-pool-telemetry.csv");
    if ("ON".equalsIgnoreCase(args.telemetry)) {
      writeTelemetryCsv(telemetryCsv, telemetrySink.getEvents());
    }

    // Write properties
    Path summaryProps = partialDir.resolve("summary.properties");
    Properties props = new Properties();
    props.setProperty("runnerVersion", VERSION);
    props.setProperty("budgetProtocol", BUDGET_VERSION);
    props.setProperty("instance", args.instance);
    props.setProperty("seed", String.valueOf(args.seed));
    props.setProperty("profile", profile.getProfileName());
    props.setProperty("qpPoolK", String.valueOf(topKConfig.getK()));
    props.setProperty("topKEnabled", String.valueOf(topKConfig.isEnabled()));
    props.setProperty("requestedFEs", String.valueOf(args.maxFes));
    props.setProperty("actualFEs", String.valueOf(actualFE));
    props.setProperty("frontSize", String.valueOf(result.size()));
    props.setProperty("wallClockNanos", String.valueOf(wallNanos));
    props.setProperty("initialPopulationHashV35", v35Hash);
    props.setProperty("initialPopulationHashP8", p8Hash);
    props.setProperty("snapshotSha256", sha256(snapshot));
    props.setProperty("frontSha256", sha256(frontCsv));
    props.setProperty("totalQpSelections", String.valueOf(telemetrySink.getTotalQpSelections()));
    props.setProperty("poolSizeGe2Selections", String.valueOf(telemetrySink.getPoolSizeGe2Selections()));
    props.setProperty("nonCanonicalSelections", String.valueOf(telemetrySink.getNonCanonicalSelections()));
    props.setProperty("totalExtraRngDraws", String.valueOf(telemetrySink.getTotalExtraRngDraws()));

    try (BufferedWriter w = Files.newBufferedWriter(summaryProps, StandardCharsets.UTF_8)) {
      props.store(w, "Qp-v2 Phase B1 Run Summary");
    }

    // Atomic move
    try {
      Files.move(partialDir, outputDir, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(partialDir, outputDir);
    }
  }

  private static Path findPath(String name, String... candidates) {
    for (String c : candidates) {
      Path p = Paths.get(c);
      if (Files.exists(p)) return p.toAbsolutePath().normalize();
    }
    throw new IllegalArgumentException("Could not locate file for " + name);
  }

  private static void writeFrontCsv(Path path, List<PermutationSolution<Integer>> solutions) throws IOException {
    List<PermutationSolution<Integer>> sorted = new ArrayList<PermutationSolution<Integer>>(solutions);
    Collections.sort(sorted, new Comparator<PermutationSolution<Integer>>() {
      @Override
      public int compare(PermutationSolution<Integer> o1, PermutationSolution<Integer> o2) {
        int c0 = Double.compare(o1.getObjective(0), o2.getObjective(0));
        if (c0 != 0) return c0;
        int c1 = Double.compare(o1.getObjective(1), o2.getObjective(1));
        if (c1 != 0) return c1;
        return Double.compare(o1.getObjective(6), o2.getObjective(6));
      }
    });

    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
      out.println("Cmax;TEC;TWC");
      for (PermutationSolution<Integer> sol : sorted) {
        out.printf(Locale.US, "%.6f;%.6f;%.6f\n", sol.getObjective(0), sol.getObjective(1), sol.getObjective(6));
      }
    }
  }

  private static void writeTelemetryCsv(Path path, List<V35QpV2TelemetrySink.QpPoolSelectionEvent> events) throws IOException {
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
      out.println("actualFE,outerCycle,qRound,lineageId,group,action,mask,archiveSize,qpPoolK,qpPoolSize,qpPoolIndex,qpSelectedIsCanonical,drewExtraRng,selectedTeacherFingerprint,canonicalTeacherFingerprint");
      for (V35QpV2TelemetrySink.QpPoolSelectionEvent e : events) {
        out.printf(Locale.US, "%d,%d,%d,%d,%s,%s,\"%s\",%d,%d,%d,%d,%b,%b,%s,%s\n",
            e.actualFE, e.outerCycle, e.qRound, e.lineageId, e.group, e.action, e.mask,
            e.archiveSize, e.qpPoolK, e.qpPoolSize, e.qpPoolIndex, e.qpSelectedIsCanonical,
            e.drewExtraRng, e.selectedTeacherFingerprint, e.canonicalTeacherFingerprint);
      }
    }
  }

  private static String sha256(Path file) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] bytes = Files.readAllBytes(file);
    byte[] hash = md.digest(bytes);
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static final class Arguments {
    public String instance;
    public long seed;
    public String profile;
    public int maxFes;
    public Path snapshot;
    public Path output;
    public String telemetry = "ON";

    public static Arguments parse(String[] args) {
      Arguments res = new Arguments();
      for (int i = 0; i < args.length; i++) {
        String flag = args[i];
        if ("--instance".equals(flag) && i + 1 < args.length) res.instance = args[++i];
        else if ("--seed".equals(flag) && i + 1 < args.length) res.seed = Long.parseLong(args[++i]);
        else if ("--profile".equals(flag) && i + 1 < args.length) res.profile = args[++i];
        else if ("--max-fes".equals(flag) && i + 1 < args.length) res.maxFes = Integer.parseInt(args[++i]);
        else if ("--snapshot".equals(flag) && i + 1 < args.length) res.snapshot = Paths.get(args[++i]);
        else if ("--output".equals(flag) && i + 1 < args.length) res.output = Paths.get(args[++i]);
        else if ("--telemetry".equals(flag) && i + 1 < args.length) res.telemetry = args[++i];
      }
      if (res.instance == null || res.profile == null || res.snapshot == null || res.output == null) {
        throw new IllegalArgumentException("Missing required arguments");
      }
      return res;
    }
  }
}
