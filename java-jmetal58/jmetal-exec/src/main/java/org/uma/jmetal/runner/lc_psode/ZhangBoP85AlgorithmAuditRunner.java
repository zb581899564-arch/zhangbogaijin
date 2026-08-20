package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MatrixKind;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunStatus;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNeighborhoodCandidateGateway;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Scope-locked P8.5 audit runner.  It performs switch exposure, deterministic replay and the
 * approved 20k FULL/B1 smoke only; it cannot start a 500k or formal statistical matrix.
 */
public final class ZhangBoP85AlgorithmAuditRunner {
  private static final long SEED = 20260808L;
  private static final int SWITCH_FES = 2000;
  private static final int SMOKE_FES = 20000;

  private ZhangBoP85AlgorithmAuditRunner() { }

  private enum Phase { SWITCHES, REPLAY, SMOKE20K, ALL }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    Path project = parsed.projectRoot.toAbsolutePath().normalize();
    Path javaRoot = project.resolve("java-jmetal58");
    if (!Files.isDirectory(javaRoot.resolve("EADHFSP"))) {
      throw new IllegalArgumentException("Not a ZhangBo project root: " + project);
    }
    Files.createDirectories(parsed.output);
    if (parsed.phase == Phase.SWITCHES || parsed.phase == Phase.ALL) {
      runSwitchAudit(project, parsed.output.resolve("switches-34x2000"));
    }
    if (parsed.phase == Phase.REPLAY || parsed.phase == Phase.ALL) {
      runReplayAudit(project, parsed.output.resolve("replay-3x2000"));
    }
    if (parsed.phase == Phase.SMOKE20K || parsed.phase == Phase.ALL) {
      run20kSmoke(project, parsed.output.resolve("smoke-20k"));
    }
  }

  private static void runSwitchAudit(Path project, Path output) throws Exception {
    requireEmpty(output);
    GoldenBinding binding = goldenBinding(project);
    List<P8ExperimentSpec> specs = P8ExperimentRegistry.currentMatrix();
    P8ExperimentRegistry.assertCurrentMatrix(specs);
    ZhangBoP9FormalParameters parameters = ZhangBoP9FormalParameters.engineering(SEED, SWITCH_FES);
    String expectedInitial = null;
    Map<String, String> mechanismHashOwners = new LinkedHashMap<>();
    List<String> failures = new ArrayList<>();
    StringBuilder aliases = new StringBuilder("aliasLabel,sourceLabel,mechanismHash\n");
    StringBuilder csv = new StringBuilder("label,matrix,status,reason,mechanismHash,"
        + "initialHash,FE,frontSize,baselineUpdates,cfvfOffspring,pddrEvents,archiveInsertions,"
        + "lineageEvents,qgSelections,qpActions,dualQEvents,fixedEvents,cataTest,cataApply,"
        + "formalOuter,formalQRounds,formalCriticalSwap,formalCriticalInsert,formalO1O9\n");
    for (P8ExperimentSpec source : specs) {
      String owner = mechanismHashOwners.get(source.getMechanismVectorHash());
      if (owner == null) mechanismHashOwners.put(source.getMechanismVectorHash(), source.getLabel());
      else aliases.append(source.getLabel()).append(',').append(owner).append(',')
          .append(source.getMechanismVectorHash()).append('\n');
      P8ExperimentSpec spec = resized(source, SWITCH_FES, "p8.5-switch-audit");
      ZhangBoCanonicalProductionProblem problem = binding.problem(source, SEED);
      List<PermutationSolution<Integer>> initial = initial(problem, spec.getPopulationSize());
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      if (expectedInitial == null) expectedInitial = initialHash;
      else if (!expectedInitial.equals(initialHash)) {
        failures.add(source.getLabel() + ": initial population drift");
      }
      ZhangBoP9FormalRunResult result = ZhangBoP9FormalExecutor.execute(source.getLabel(),
          source.getLabel(), spec, parameters, binding.name, binding.instanceSha256,
          problem, P8InitialPopulationProvider.copy(initial));
      String failure = switchFailure(source, result, parameters);
      if (failure != null) failures.add(source.getLabel() + ": " + failure);
      Files.write(output.resolve(source.getLabel() + "-mechanism-summary.txt"),
          result.mechanismSummary.getBytes(StandardCharsets.UTF_8));
      csv.append(source.getLabel()).append(',').append(source.getMatrix()).append(',')
          .append(failure == null ? "PASS" : "FAIL").append(',')
          .append(quoted(failure == null ? "" : failure)).append(',')
          .append(source.getMechanismVectorHash()).append(',').append(initialHash).append(',')
          .append(result.record.getFullEvaluations()).append(',')
          .append(result.record.getFront().size()).append(',')
          .append(result.baselineUpdateEvents).append(',').append(result.cfvfOffspring).append(',')
          .append(result.pddrEvents).append(',').append(result.archiveInsertions).append(',')
          .append(result.lineageEvents).append(',').append(result.qgSelections).append(',')
          .append(result.qpActions).append(',').append(result.dualQEvents).append(',')
          .append(result.fixedNeighborhoodEvents).append(',').append(result.caTaTestCalls)
          .append(',').append(result.caTaApplyCalls).append(',')
          .append(result.formalOuterCycles).append(',').append(result.formalQgRounds).append(',')
          .append(result.formalCriticalFactorySwaps).append(',')
          .append(result.formalCriticalFactoryInserts).append(',')
          .append(result.formalO1O9Evaluations).append('\n');
    }
    Files.write(output.resolve("switch-audit.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("exact-aliases.csv"),
        aliases.toString().getBytes(StandardCharsets.UTF_8));
    String summary = "schema=zhangbo-p8.5-switch-audit-v1\nlabels=" + specs.size()
        + "\nseed=" + SEED + "\npopulation=100\nmaxFEs=" + SWITCH_FES
        + "\ninitialPopulationSha256=" + expectedInitial
        + "\nuniqueMechanismHashes=" + mechanismHashOwners.size()
        + "\nexactAliasLabels=" + (specs.size() - mechanismHashOwners.size())
        + "\nfailures=" + failures.size() + "\n"
        + (failures.isEmpty() ? "" : String.join("\n", failures) + "\n");
    Files.write(output.resolve("SUMMARY.txt"), summary.getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    if (!failures.isEmpty()) throw new IllegalStateException("P8.5 switch audit failed\n" + summary);
    System.out.println("P8_5_SWITCH_AUDIT_COMPLETED labels=" + specs.size());
  }

  private static void runReplayAudit(Path project, Path output) throws Exception {
    requireEmpty(output);
    GoldenBinding binding = goldenBinding(project);
    ZhangBoP9FormalParameters parameters = ZhangBoP9FormalParameters.engineering(SEED, SWITCH_FES);
    StringBuilder csv = new StringBuilder(
        "label,run,initialHash,FE,frontHash,qgHash,qpHash,mechanismSummaryHash\n");
    for (String label : Arrays.asList("B1", "FULL")) {
      P8ExperimentSpec source = P8ExperimentRegistry.find(label);
      String expected = null;
      for (int run = 1; run <= 3; run++) {
        ZhangBoCanonicalProductionProblem problem = binding.problem(source, SEED);
        List<PermutationSolution<Integer>> initial = initial(problem, source.getPopulationSize());
        P8ExperimentSpec spec = resized(source, SWITCH_FES, "p8.5-replay-audit");
        ZhangBoP9FormalRunResult result = ZhangBoP9FormalExecutor.execute(label, label,
            spec, parameters, binding.name, binding.instanceSha256, problem,
            P8InitialPopulationProvider.copy(initial), deterministicClock());
        String failure = switchFailure(source, result, parameters);
        if (failure != null) throw new IllegalStateException(label + " replay gate: " + failure);
        String signature = result.record.getInitialPopulationSha256() + '|'
            + result.record.getFullEvaluations() + '|' + frontHash(result.record.getFront()) + '|'
            + result.qgTableHash + '|' + result.qpTableHash + '|'
            + sha256(result.mechanismSummary);
        if (expected == null) expected = signature;
        else if (!expected.equals(signature)) {
          throw new IllegalStateException(label + " replay signature drift at run " + run);
        }
        csv.append(label).append(',').append(run).append(',')
            .append(result.record.getInitialPopulationSha256()).append(',')
            .append(result.record.getFullEvaluations()).append(',')
            .append(frontHash(result.record.getFront())).append(',')
            .append(result.qgTableHash).append(',').append(result.qpTableHash).append(',')
            .append(sha256(result.mechanismSummary)).append('\n');
      }
    }
    Files.write(output.resolve("replay-audit.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("SUMMARY.txt"),
        "schema=zhangbo-p8.5-replay-v1\nlabels=B1,FULL\nrunsPerLabel=3\nreplay=true\n"
            .getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    System.out.println("P8_5_REPLAY_COMPLETED");
  }

  private static void run20kSmoke(Path project, Path output) throws Exception {
    requireEmpty(output);
    Path javaRoot = project.resolve("java-jmetal58");
    Path instance = javaRoot.resolve("EADHFSP/20_2_3_1.txt");
    Path extensions = javaRoot.resolve("instance-extensions/v1");
    Path fatigue = javaRoot.resolve("fatigue-parameters/v1");
    String instanceHash = sha256(Files.readAllBytes(instance));
    ZhangBoP9FormalParameters parameters = ZhangBoP9FormalParameters.formalAudit(SEED, SMOKE_FES);
    String expectedInitial = null;
    StringBuilder csv = new StringBuilder("label,status,reason,initialHash,FE,frontSize,frontHash,"
        + "baselineUpdates,cfvfOffspring,pddrEvents,archiveInsertions,qgSelections,qpActions,"
        + "fixedEvents,cataTest,cataApply,formalOuter,formalQRounds,criticalSwap,criticalInsert,o1o9\n");
    for (String label : Arrays.asList("FULL", "B1")) {
      P8ExperimentSpec source = P8ExperimentRegistry.find(label);
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
          ProductionDecodeMode.FM3, SEED, extensions, fatigue,
          source.getAblationProfile().getShiftConfiguration());
      List<PermutationSolution<Integer>> initial = initial(problem, source.getPopulationSize());
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      if (expectedInitial == null) expectedInitial = initialHash;
      else if (!expectedInitial.equals(initialHash)) {
        throw new IllegalStateException("20k initial population drift");
      }
      P8ExperimentSpec spec = resized(source, SMOKE_FES, "p8.5-20k-smoke");
      ZhangBoP9FormalRunResult result = ZhangBoP9FormalExecutor.execute(label, label, spec,
          parameters, "20_2_3_1", instanceHash, problem,
          P8InitialPopulationProvider.copy(initial));
      String failure = switchFailure(source, result, parameters);
      if (failure != null) throw new IllegalStateException(label + " 20k gate: " + failure);
      Files.write(output.resolve(label + "-mechanism-summary.txt"),
          result.mechanismSummary.getBytes(StandardCharsets.UTF_8));
      writeFront(output.resolve(label + "-front.csv"), result.record.getFront());
      csv.append(label).append(",PASS,,").append(initialHash).append(',')
          .append(result.record.getFullEvaluations()).append(',')
          .append(result.record.getFront().size()).append(',')
          .append(frontHash(result.record.getFront())).append(',')
          .append(result.baselineUpdateEvents).append(',').append(result.cfvfOffspring).append(',')
          .append(result.pddrEvents).append(',').append(result.archiveInsertions).append(',')
          .append(result.qgSelections).append(',').append(result.qpActions).append(',')
          .append(result.fixedNeighborhoodEvents).append(',').append(result.caTaTestCalls)
          .append(',').append(result.caTaApplyCalls).append(',')
          .append(result.formalOuterCycles).append(',').append(result.formalQgRounds).append(',')
          .append(result.formalCriticalFactorySwaps).append(',')
          .append(result.formalCriticalFactoryInserts).append(',')
          .append(result.formalO1O9Evaluations).append('\n');
    }
    Files.write(output.resolve("smoke-20k.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("SUMMARY.txt"),
        ("schema=zhangbo-p8.5-20k-v1\nseed=" + SEED + "\npopulation=100\nmaxFEs="
            + SMOKE_FES + "\ninitialPopulationSha256=" + expectedInitial
            + "\nsharedFM3=true\nsharedShiftMode=LEFT_RIGHT\nformalMatrix=false\n")
            .getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    System.out.println("P8_5_20K_COMPLETED");
  }

  private static String switchFailure(P8ExperimentSpec spec,
      ZhangBoP9FormalRunResult result, ZhangBoP9FormalParameters parameters) {
    if (result.record.getStatus() != P8RunStatus.COMPLETED) return result.record.getReason();
    if (result.record.getFullEvaluations() < parameters.getPopulation()
        || result.record.getFullEvaluations() > parameters.getMaxFEs()) return "FE_OUT_OF_RANGE";
    if (result.record.getFront().isEmpty()) return "EMPTY_FRONT";
    if (result.record.getIllegalSolutions() != 0) return "ILLEGAL_SOLUTIONS";
    if (result.record.getCfvfRepairs() != 0) return "CFVF_REPAIRS";
    P8AblationProfile profile = spec.getAblationProfile();
    if ((result.qgSelections > 0) != profile.isQgEnabled()) return "QG_SWITCH_MISMATCH";
    if ((result.pddrEvents > 0) != profile.isEvaluatedPddrEnabled()) return "PDDR_SWITCH_MISMATCH";
    if ((result.archiveInsertions > 0) != profile.isLineageArchiveEnabled()) {
      return "ARCHIVE_SWITCH_MISMATCH";
    }
    if ((result.qpActions > 0) != profile.isQpEnabled()) return "QP_SWITCH_MISMATCH";
    if (profile.getResourceFlightMode() == P8AblationProfile.ResourceFlightMode.BASELINE_GA) {
      if (result.baselineUpdateEvents <= 0 || result.cfvfOffspring != 0) {
        return "BASELINE_GA_SWITCH_MISMATCH";
      }
    } else if (profile.isCfvfFamily()) {
      if (result.cfvfOffspring <= 0 || result.baselineUpdateEvents != 0) {
        return "RESOURCE_FLIGHT_SWITCH_MISMATCH";
      }
    }
    boolean formalControl = spec.getMatrix() == P8MatrixKind.FULL
        && ("B0".equals(spec.getLabel()) || "B1".equals(spec.getLabel()));
    if (formalControl) {
      if (result.formalOuterCycles <= 0
          || result.formalQgRounds != result.formalOuterCycles * parameters.getQTimes()
          || result.formalCriticalFactorySwaps <= 0
          || result.formalCriticalFactoryInserts <= 0
          || result.formalO1O9Evaluations <= 0) return "FORMAL_BASELINE_RUNTIME_MISMATCH";
    } else if (result.formalOuterCycles != 0 || result.formalQgRounds != 0) {
      return "FORMAL_BASELINE_LEAK";
    }
    if (profile.isCaTaEnabled()) {
      if (result.caTaTestCalls <= 0 || result.caTaApplyCalls <= 0) return "CATA_SWITCH_MISMATCH";
    } else if (result.caTaTestCalls != 0 || result.caTaApplyCalls != 0) {
      return "CATA_SWITCH_LEAK";
    }
    if (profile.isLocalSearchEnabled() && !profile.isCaTaEnabled()
        && !formalControl && result.fixedNeighborhoodEvents <= 0) {
      return "FIXED_VNS_SWITCH_MISMATCH";
    }
    return null;
  }

  private static P8ExperimentSpec resized(P8ExperimentSpec source, int maxFEs, String suffix) {
    return new P8ExperimentSpec(source.getMatrix(), source.getLabel(), source.getMechanism(),
        source.getConfigurationKey() + '-' + suffix, source.getAblationProfile(),
        P8RunStatus.COMPLETED, suffix, source.getPopulationSize(), maxFEs,
        source.getPhysicalSubswarmSizes());
  }

  private static List<PermutationSolution<Integer>> initial(
      ZhangBoCanonicalProductionProblem problem, int size) {
    List<PermutationSolution<Integer>> result = new ArrayList<>(size);
    for (int index = 0; index < size; index++) result.add(problem.createSolution());
    return result;
  }

  private static GoldenBinding goldenBinding(Path project) throws Exception {
    Path bridgeRoot = project.resolve("java-jmetal58/p8-bridge/v1");
    P8GoldenAuthorCompatibilityBridge.Manifest bridge =
        P8GoldenAuthorCompatibilityBridge.materialize(bridgeRoot);
    return new GoldenBinding("chapter4-golden-author-bridge",
        bridge.root.resolve("EADHFSP/10_2_2_1.txt"),
        bridge.root.resolve("instance-extensions/v1"),
        bridge.root.resolve("fatigue-parameters/v1"), bridge.instanceSha256);
  }

  private static final class GoldenBinding {
    private final String name;
    private final Path instance;
    private final Path extensions;
    private final Path fatigue;
    private final String instanceSha256;
    private GoldenBinding(String name, Path instance, Path extensions, Path fatigue,
        String instanceSha256) {
      this.name = name;
      this.instance = instance;
      this.extensions = extensions;
      this.fatigue = fatigue;
      this.instanceSha256 = instanceSha256;
    }
    private ZhangBoCanonicalProductionProblem problem(P8ExperimentSpec spec, long seed)
        throws Exception {
      return ZhangBoCanonicalProblemLoader.load(instance,
          productionMode(spec.getAblationProfile().getDecoderMode()), seed,
          extensions, fatigue, spec.getAblationProfile().getShiftConfiguration());
    }
  }

  private static ProductionDecodeMode productionMode(P8AblationProfile.DecoderMode mode) {
    switch (mode) {
      case DETERMINISTIC_CANONICAL:
      case CORRECTED_NO_FATIGUE: return ProductionDecodeMode.CANONICAL_NO_FATIGUE;
      case ACCUMULATION_ONLY: return ProductionDecodeMode.FM1;
      case ACCUMULATION_RECOVERY: return ProductionDecodeMode.FM2;
      case FATIGUE_AWARE_SELECTION: return ProductionDecodeMode.FM3;
      default: throw new IllegalArgumentException("Diagnostic decoder forbidden: " + mode);
    }
  }

  private static void writeFront(Path path, List<double[]> values) throws IOException {
    StringBuilder text = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] value : values) text.append(number(value[0])).append(',')
        .append(number(value[1])).append(',').append(number(value[2])).append('\n');
    Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String frontHash(List<double[]> values) {
    List<String> rows = new ArrayList<>();
    for (double[] value : values) rows.add(number(value[0]) + ',' + number(value[1])
        + ',' + number(value[2]));
    java.util.Collections.sort(rows);
    return sha256(String.join("\n", rows) + "\n");
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }

  private static ZhangBoNeighborhoodCandidateGateway.NanoClock deterministicClock() {
    return new ZhangBoNeighborhoodCandidateGateway.NanoClock() {
      private long value;
      @Override public long nanoTime() { value += 1000L; return value; }
    };
  }

  private static String quoted(String value) {
    return '"' + value.replace("\"", "\"\"") + '"';
  }

  private static void requireEmpty(Path directory) throws IOException {
    Files.createDirectories(directory);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      if (stream.iterator().hasNext()) throw new IllegalArgumentException(
          "Audit output must be empty: " + directory);
    }
  }

  private static void writeHashes(Path directory) throws IOException {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path path : stream) if (Files.isRegularFile(path)
          && !"evidence-sha256.tsv".equals(path.getFileName().toString())) files.add(path);
    }
    java.util.Collections.sort(files);
    StringBuilder text = new StringBuilder("sha256\tfile\n");
    for (Path path : files) text.append(sha256(Files.readAllBytes(path))).append('\t')
        .append(path.getFileName()).append('\n');
    Files.write(directory.resolve("evidence-sha256.tsv"),
        text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws IOException {
    return sha256(Files.readAllBytes(path));
  }

  private static String sha256(String value) {
    return sha256(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(byte[] value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder result = new StringBuilder();
      for (byte item : digest) result.append(String.format("%02x", item & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static final class Arguments {
    private final Phase phase;
    private final Path projectRoot;
    private final Path output;
    private Arguments(Phase phase, Path projectRoot, Path output) {
      this.phase = phase;
      this.projectRoot = projectRoot;
      this.output = output;
    }
    private static Arguments parse(String[] args) {
      Phase phase = null;
      Path project = null;
      Path output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--phase".equals(args[index])) phase = Phase.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) project = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (phase == null || project == null || output == null) throw usage();
      return new Arguments(phase, project, output.toAbsolutePath().normalize());
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "Usage: --phase SWITCHES|REPLAY|SMOKE20K|ALL --project-root <path> --output <path>");
    }
  }
}
