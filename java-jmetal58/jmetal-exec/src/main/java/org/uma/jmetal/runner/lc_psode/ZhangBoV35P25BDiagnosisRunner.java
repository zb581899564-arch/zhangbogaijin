package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DiagnosisThresholdSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** Scope-locked calibration/held-out runner for V35-P25B pressure diagnosis. */
public final class ZhangBoV35P25BDiagnosisRunner {
  public static final String VERSION = "v35-p25b-pressure-diagnosis-v1";
  private static final long[] CALIBRATION = {20260814L, 20260815L, 20260816L};
  private static final long[] HELDOUT = {20260817L, 20260818L};

  public enum Phase { CALIBRATION, SELECT, HELDOUT, REPORT }
  public enum Instance { I1, E20 }
  public enum Arm { A4, A5 }

  private ZhangBoV35P25BDiagnosisRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    switch (value.phase) {
      case CALIBRATION:
        run(value, false);
        break;
      case SELECT:
        select(value.output);
        break;
      case HELDOUT:
        run(value, true);
        break;
      case REPORT:
        report(value.output);
        break;
      default:
        throw new IllegalStateException("unsupported phase");
    }
  }

  static Path runForTest(Phase phase, Instance instance, Arm arm, long seed,
      Path projectRoot, Path output, int population, int budget,
      V35BottleneckDiagnosisConfiguration diagnosis) throws Exception {
    return runOne(phase, instance, arm, seed, projectRoot, output, population, budget, diagnosis);
  }

  private static void run(Arguments value, boolean heldout) throws Exception {
    if (value.instance == null || value.arm == null || value.seed == null) throw usage();
    requireSeed(value.seed, heldout ? HELDOUT : CALIBRATION);
    if (!heldout && value.arm != Arm.A4) {
      throw new IllegalArgumentException("calibration is fixed to A4");
    }
    V35BottleneckDiagnosisConfiguration diagnosis = heldout
        ? confidenceFrom(value.output) : V35BottleneckDiagnosisConfiguration.calibrationAudit();
    int population = value.instance == Instance.I1 ? 10 : 100;
    int budget = value.instance == Instance.I1 ? 5000 : 20000;
    Path result = runOne(value.phase, value.instance, value.arm, value.seed,
        value.projectRoot, value.output, population, budget, diagnosis);
    System.out.println("V35_P25B_RUN_COMPLETED output=" + result);
  }

  private static Path runOne(Phase phase, Instance instance, Arm arm, long seed,
      Path projectRoot, Path output, int population, int budget,
      V35BottleneckDiagnosisConfiguration diagnosis) throws Exception {
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path root = instance == Instance.I1 ? javaProject.resolve("p8-bridge/v1") : javaProject;
    String name = instance == Instance.I1 ? "10_2_2_1" : "20_2_3_1";
    Path instanceFile = root.resolve("EADHFSP/" + name + ".txt");
    Path extension = root.resolve("instance-extensions/v1");
    Path fatigue = root.resolve("fatigue-parameters/v1");
    requireFile(instanceFile);
    requireFile(extension.resolve(name + ".setup.txt"));
    requireFile(fatigue.resolve(name + ".fatigue.txt"));
    Path directory = output.toAbsolutePath().normalize().resolve("runs")
        .resolve(phase.name().toLowerCase()).resolve("seed-" + seed)
        .resolve(instance.name()).resolve(arm.name());
    if (Files.exists(directory)) throw new IllegalStateException("refusing overwrite: " + directory);
    Files.createDirectories(directory);

    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instanceFile, ProductionDecodeMode.FM3, seed, extension, fatigue,
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < population; index++) initial.add(problem.createSolution());
    V35FairRunner.Mode mode = arm == Arm.A4
        ? V35FairRunner.Mode.V35_FULL_POOL_OFF : V35FairRunner.Mode.V35_FULL;
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .seed(seed).populationSize(population).maxEvaluations(budget)
        .decoderMode(ProductionDecodeMode.FM3).dscr(true).cfvf(true).qg(true).qp(true)
        .caTaLite(true).directionalTeacherPool(arm == Arm.A5).teacherPoolSize(10)
        .bottleneckDiagnosis(diagnosis).build();
    V35FairRunner.RunRecord record = V35FairRunner.run(mode, problem,
        P8InitialPopulationProvider.copy(initial), budget, seed, false, diagnosis);
    String config = "p25bVersion=" + VERSION + "\nphase=" + phase + "\ninstance=" + name
        + "\narm=" + arm + "\nseed=" + seed + "\npopulation=" + population
        + "\nmaxFEs=" + budget + "\nshadowFeExcludedFromMain=true\n"
        + "mechanismVectorHash=" + configuration.configurationHash() + "\n"
        + configuration.canonicalText();
    V35FairRunner.writeRecord(record, directory, config);
    Files.write(directory.resolve("initial-population.sha256"),
        (P8InitialPopulationProvider.sha256(initial) + "\n").getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("run-record.csv"), (
        "phase,instance,arm,seed,status,mainFE,shadowFE,shadowSamples,frontSize\n"
            + phase + "," + name + "," + arm + "," + seed + "," + record.getStatus()
            + "," + record.getFullEvaluations() + "," + record.getShadowEvaluations()
            + "," + record.getShadowSamples() + "," + record.getFront().size() + "\n")
        .getBytes(StandardCharsets.UTF_8));
    if (!"COMPLETED".equals(record.getStatus()) || record.getFullEvaluations() > budget
        || record.getShadowSamples() <= 0L || record.getShadowEvaluations() <= 0) {
      throw new IllegalStateException("P25B run gate failed: " + record.getStopReason());
    }
    writeHashes(directory);
    return directory;
  }

  private static void select(Path output) throws Exception {
    String probes = collect(output.resolve("runs/calibration"));
    V35DiagnosisThresholdSelector.Selection selection =
        new V35DiagnosisThresholdSelector().select(probes);
    Files.createDirectories(output);
    Files.write(output.resolve("threshold-candidates.csv"),
        selection.candidatesCsv().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("threshold-selection.csv"),
        selection.selectionCsv().getBytes(StandardCharsets.UTF_8));
    Properties properties = new Properties();
    properties.setProperty("diagnosisVersion", V35BottleneckDiagnosisConfiguration.VERSION);
    properties.setProperty("status", selection.hasFrozenThresholds() ? "CALIBRATION_SELECTED" : "NO_FEASIBLE_THRESHOLD");
    if (selection.hasFrozenThresholds()) {
      properties.setProperty("tauAbs", String.valueOf(selection.getSelected().getTauAbs()));
      properties.setProperty("tauGap", String.valueOf(selection.getSelected().getTauGap()));
    }
    java.io.Writer writer = Files.newBufferedWriter(output.resolve("threshold-selection.properties"),
        StandardCharsets.UTF_8);
    try { properties.store(writer, "V35-P25B calibration selection"); }
    finally { writer.close(); }
    if (!selection.hasFrozenThresholds()) {
      throw new IllegalStateException("no threshold satisfies coverage/missed-positive gates");
    }
    writeHashes(output);
  }

  private static void report(Path output) throws Exception {
    V35BottleneckDiagnosisConfiguration confidence = confidenceFrom(output);
    Path heldoutRoot = output.resolve("runs/heldout");
    String root = collect(heldoutRoot);
    V35DiagnosisThresholdSelector.Candidate combined = new V35DiagnosisThresholdSelector()
        .validate(root, confidence.getTauAbs(), confidence.getTauGap());
    boolean pass = combined.getCoverage() + 1.0e-12 >= 0.10
        && combined.getMissedPositiveBestRate() <= 0.05 + 1.0e-12;
    StringBuilder heldout = new StringBuilder(
        "run,tauAbs,tauGap,coverage,missedPositiveBestRate,meanRegret,p95Regret,pass\n");
    List<Path> probeFiles = probeFiles(heldoutRoot);
    for (Path probe : probeFiles) {
      V35DiagnosisThresholdSelector.Candidate value = new V35DiagnosisThresholdSelector()
          .validate(new String(Files.readAllBytes(probe), StandardCharsets.UTF_8),
              confidence.getTauAbs(), confidence.getTauGap());
      boolean runPass = value.getCoverage() + 1.0e-12 >= 0.10
          && value.getMissedPositiveBestRate() <= 0.05 + 1.0e-12;
      pass &= runPass;
      heldout.append(heldoutRoot.relativize(probe.getParent()).toString().replace('\\', '/'))
          .append(',').append(value.getTauAbs()).append(',').append(value.getTauGap())
          .append(',').append(value.getCoverage()).append(',')
          .append(value.getMissedPositiveBestRate()).append(',').append(value.getMeanRegret())
          .append(',').append(value.getP95Regret()).append(',').append(runPass).append('\n');
    }
    heldout.append("ALL,").append(combined.getTauAbs()).append(',').append(combined.getTauGap())
        .append(',').append(combined.getCoverage()).append(',')
        .append(combined.getMissedPositiveBestRate()).append(',').append(combined.getMeanRegret())
        .append(',').append(combined.getP95Regret()).append(',').append(pass).append('\n');
    Files.write(output.resolve("heldout-validation.csv"),
        heldout.toString().getBytes(StandardCharsets.UTF_8));
    String report = "# V35-P25B Diagnosis Audit Report\n\n"
        + "- semantics: `" + V35BottleneckDiagnosisConfiguration.VERSION + "`\n"
        + "- held-out gate: **" + (pass ? "PASS" : "FAIL") + "**\n"
        + "- strict coverage: " + combined.getCoverage() + "\n"
        + "- missed positive best rate: " + combined.getMissedPositiveBestRate() + "\n"
        + "- p95 diagnostic regret: " + combined.getP95Regret() + "\n\n"
        + (pass ? "Thresholds may be frozen in P24.2; no 500000 FE run was started.\n"
            : "Thresholds are not frozen. Formal path remains BAL with N1-N5 open.\n");
    Files.write(output.resolve("DIAGNOSIS_AUDIT_REPORT.md"), report.getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    if (!pass) throw new IllegalStateException("held-out diagnosis gate failed");
  }

  private static V35BottleneckDiagnosisConfiguration confidenceFrom(Path output) throws Exception {
    Properties properties = new Properties();
    java.io.Reader reader = Files.newBufferedReader(
        output.resolve("threshold-selection.properties"), StandardCharsets.UTF_8);
    try { properties.load(reader); } finally { reader.close(); }
    if (!"CALIBRATION_SELECTED".equals(properties.getProperty("status"))) {
      throw new IllegalStateException("thresholds have not passed calibration");
    }
    return V35BottleneckDiagnosisConfiguration.confidence(
        Double.parseDouble(properties.getProperty("tauAbs")),
        Double.parseDouble(properties.getProperty("tauGap")), true);
  }

  private static String collect(Path root) throws Exception {
    if (!Files.isDirectory(root)) throw new IllegalArgumentException("evidence directory missing: " + root);
    List<Path> files = probeFiles(root);
    StringBuilder out = new StringBuilder();
    boolean header = false;
    long nextSample = 1L;
    for (Path file : files) {
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      Map<String, Long> sampleIds = new TreeMap<>();
      for (int index = 0; index < lines.size(); index++) {
        if (index == 0) {
          if (!header) { out.append(lines.get(index)).append('\n'); header = true; }
        } else if (!lines.get(index).trim().isEmpty()) {
          int comma = lines.get(index).indexOf(',');
          if (comma <= 0) throw new IllegalArgumentException("malformed shadow row: " + file);
          String local = lines.get(index).substring(0, comma);
          Long global = sampleIds.get(local);
          if (global == null) {
            global = nextSample++;
            sampleIds.put(local, global);
          }
          out.append(global).append(lines.get(index).substring(comma)).append('\n');
        }
      }
    }
    if (!header) throw new IllegalStateException("no shadow probes found under " + root);
    return out.toString();
  }

  private static List<Path> probeFiles(Path root) throws Exception {
    List<Path> files = new ArrayList<>();
    Stream<Path> walk = Files.walk(root);
    try { walk.filter(path -> path.getFileName().toString().equals("shadow-probes.csv"))
        .forEach(files::add); } finally { walk.close(); }
    java.util.Collections.sort(files);
    return files;
  }

  private static void requireSeed(long seed, long[] allowed) {
    for (long value : allowed) if (value == seed) return;
    throw new IllegalArgumentException("seed is outside the approved phase");
  }
  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("required file missing: " + path);
  }
  private static void writeHashes(Path root) throws Exception {
    Map<String, String> values = new TreeMap<>();
    Path manifest = root.resolve("evidence-sha256.tsv");
    Files.deleteIfExists(manifest);
    Stream<Path> walk = Files.walk(root);
    try { walk.filter(Files::isRegularFile).forEach(path -> {
      try { values.put(root.relativize(path).toString().replace('\\', '/'), sha256(path)); }
      catch (Exception error) { throw new RuntimeException(error); }
    }); } finally { walk.close(); }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> value : values.entrySet()) {
      out.append(value.getKey()).append('\t').append(value.getValue()).append('\n');
    }
    Files.write(manifest, out.toString().getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static final class Arguments {
    final Phase phase; final Instance instance; final Arm arm; final Long seed;
    final Path projectRoot; final Path output;
    private Arguments(Phase phase, Instance instance, Arm arm, Long seed,
        Path projectRoot, Path output) {
      this.phase = phase; this.instance = instance; this.arm = arm;
      this.seed = seed; this.projectRoot = projectRoot; this.output = output;
    }
    static Arguments parse(String[] args) {
      Phase phase = null; Instance instance = null; Arm arm = null; Long seed = null;
      Path project = null; Path output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--phase".equals(args[index])) phase = Phase.valueOf(args[index + 1]);
        else if ("--instance".equals(args[index])) instance = Instance.valueOf(args[index + 1]);
        else if ("--arm".equals(args[index])) arm = Arm.valueOf(args[index + 1]);
        else if ("--seed".equals(args[index])) seed = Long.valueOf(args[index + 1]);
        else if ("--project-root".equals(args[index])) project = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (phase == null || output == null) throw usage();
      if (phase == Phase.CALIBRATION || phase == Phase.HELDOUT) {
        if (project == null || instance == null || arm == null || seed == null) throw usage();
      }
      return new Arguments(phase, instance, arm, seed, project, output);
    }
  }
  private static IllegalArgumentException usage() {
    return new IllegalArgumentException("Usage: --phase CALIBRATION|SELECT|HELDOUT|REPORT "
        + "[--instance I1|E20 --arm A4|A5 --seed approved] "
        + "[--project-root <path>] --output <path>");
  }
}
