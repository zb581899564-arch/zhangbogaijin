package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DecodeOptions;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DecodeResult;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.OperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.OriginalDhhfspDecoder;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;

/**
 * Paper-evidence runner for the immutable Chapter-4 illustrative instance I1.
 *
 * <p>This runner decodes one explicit chromosome.  It never starts PSO and it
 * never invokes the author-diagnostic problem.  Scientific inputs are constants;
 * the command line can only select an output location.</p>
 */
public final class ZhangBoCanonicalExampleRunner {
  private static final long SEED = 20260808L;
  private static final String INSTANCE_FILE = "10_2_2_1.txt";
  private static final String VERSION = "pre-validation-audited-20260810";

  private ZhangBoCanonicalExampleRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    Path projectRoot = parsed.projectRoot.toAbsolutePath().normalize();
    Path output = parsed.output.toAbsolutePath().normalize();
    requireEmptyOrMissing(output);
    Files.createDirectories(output);
    run(projectRoot, output);
    System.out.println("CANONICAL_EXAMPLE_COMPLETED output=" + output);
  }

  static void run(Path projectRoot, Path output) throws Exception {
    Path bridgeRoot = projectRoot.resolve("java-jmetal58/p8-bridge/v1");
    Path instancePath = bridgeRoot.resolve("EADHFSP").resolve(INSTANCE_FILE);
    Path extensionDirectory = bridgeRoot.resolve("instance-extensions/v1");
    Path fatigueDirectory = bridgeRoot.resolve("fatigue-parameters/v1");
    requireFile(instancePath);
    requireFile(extensionDirectory.resolve("10_2_2_1.setup.txt"));
    requireFile(fatigueDirectory.resolve("10_2_2_1.fatigue.txt"));

    Path freeze = output.resolve("00_freeze");
    Path input = output.resolve("01_input");
    Path fm3Directory = output.resolve("02_decoder_fm3");
    Path manual = output.resolve("03_manual_validation");
    Path fm0Directory = output.resolve("04_fm0_regression");
    Path evolution = output.resolve("05_one_particle_evolution");
    Path local = output.resolve("06_local_search");
    Path selection = output.resolve("07_environment_selection");
    Path figures = output.resolve("08_figures");
    for (Path directory : Arrays.asList(freeze, input, fm3Directory, manual,
        fm0Directory, evolution, local, selection, figures)) {
      Files.createDirectories(directory);
    }

    copyFrozenInput(bridgeRoot, input);
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    writeX0(fixture, input);

    ZhangBoCanonicalProductionProblem fm3 = ZhangBoCanonicalProblemLoader.load(
        instancePath, ProductionDecodeMode.FM3, SEED, extensionDirectory, fatigueDirectory);
    ZhangBoCanonicalProductionProblem fm0 = ZhangBoCanonicalProblemLoader.load(
        instancePath, ProductionDecodeMode.CANONICAL_NO_FATIGUE, SEED,
        extensionDirectory, fatigueDirectory);
    writeInputTables(fm3.getInstance(), fm3.getParameters(), input);

    DhhfspFourVectorSolution x0Fm3 = x0(fixture, ProductionDecodeMode.FM3);
    DhhfspFourVectorSolution x0Fm0 = x0(fixture, ProductionDecodeMode.CANONICAL_NO_FATIGUE);
    fm3.evaluate(x0Fm3);
    fm0.evaluate(x0Fm0);
    ZhangBoFatigueEvaluationResult fm3Result = result(x0Fm3);
    ZhangBoFatigueEvaluationResult fm0Result = result(x0Fm0);
    writeDecode(fm3Result, fm3.getInstance(), fm3.getParameters(), fm3Directory);
    writeDecode(fm0Result, fm0.getInstance(), fm0.getParameters(), fm0Directory);
    writeRegression(fm0Result, fm3Result, fm0Directory.resolve("fm0_vs_fm3.csv"));

    DecodeResult paper = new OriginalDhhfspDecoder().decode(
        fixture.getInstance(), fixture.createSolution(), DecodeOptions.deterministic(SEED));
    Files.write(fm0Directory.resolve("p3_published_initial.csv"),
        paper.getInitial().operationsCsv().getBytes(StandardCharsets.UTF_8));
    Files.write(fm0Directory.resolve("p3_oracle_scope.txt"),
        ("semanticTag=published_baseline\nphase=initial_append_only\n"
            + "fineTuneCompared=false\nrightShiftCompared=false\n"
            + "seed=" + SEED + "\n").getBytes(StandardCharsets.UTF_8));

    String manifest = "schemaVersion=1\n"
        + "versionId=" + VERSION + "\n"
        + "instance=I1\nseed=" + SEED + "\n"
        + "jobs=10\nstages=2\nfactories=2\n"
        + "mainMode=FM3\nregressionMode=CANONICAL_NO_FATIGUE\n"
        + "sourceResource=" + Chapter4GoldenFixture.RESOURCE + "\n"
        + "instanceSha256=" + fm3.getInstance().getInstanceSha256() + "\n"
        + "instanceExtensionSha256=" + fm3.getInstance().getInstanceExtensionSha256() + "\n"
        + "fatigueParametersSha256=" + fm3.getParameters().getConfigurationSha256() + "\n"
        + "x0Sha256=" + sha256(input.resolve("X0-zero-based.csv")) + "\n"
        + "fm3TraceSha256=" + sha256(fm3Directory.resolve("program_trace.csv")) + "\n"
        + "fm0TraceSha256=" + sha256(fm0Directory.resolve("program_trace.csv")) + "\n"
        + "manualValidationStatus=PENDING_INDEPENDENT_RECONSTRUCTION\n"
        + "evolutionTraceStatus=PENDING\n";
    Files.write(output.resolve("manifest.properties"), manifest.getBytes(StandardCharsets.UTF_8));
    Files.write(freeze.resolve("environment.txt"), environment().getBytes(StandardCharsets.UTF_8));
    Files.write(manual.resolve("README.md"),
        ("# Independent manual validation\n\nThis directory is populated by the independent "
            + "formula reconstruction tool.  It must not import or invoke the Java decoder.\n")
            .getBytes(StandardCharsets.UTF_8));
  }

  private static DhhfspFourVectorSolution x0(
      Chapter4GoldenFixture fixture, ProductionDecodeMode mode) {
    DhhfspFourVectorSolution source = fixture.createSolution();
    return new DhhfspFourVectorSolution(
        source.getJobSequence(), source.getFactoryAssignments(), source.getMachineAssignments(),
        source.getWorkerAssignments(), mode.getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
  }

  private static ZhangBoFatigueEvaluationResult result(DhhfspFourVectorSolution solution) {
    Object value = solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    if (!(value instanceof ZhangBoFatigueEvaluationResult)) {
      throw new IllegalStateException("Missing canonical decode result");
    }
    return (ZhangBoFatigueEvaluationResult) value;
  }

  private static void writeDecode(
      ZhangBoFatigueEvaluationResult result, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, Path directory) throws IOException {
    StringBuilder trace = new StringBuilder();
    trace.append("sequence,job,stage,factory,machine,worker,predecessorCompletion,")
        .append("machineAvailableBefore,workerAvailableBefore,start,recoveryDuration,")
        .append("fatigueBeforeRecovery,fatigueAtStart,ST,SUT,machineSpeed,machinePower,")
        .append("workerEfficiency,workerCost,lambda,muCurrentStage,recoveryMu,r,delta,")
        .append("baseProcessing,baseSetup,baseAT,fatigueMultiplier,actualProcessing,")
        .append("actualSetup,actualAT,end,fatigueAfter,energy,cost,safeThresholdExceeded\n");
    int[][] previousStage = new int[instance.getFactories()][];
    for (int factory = 0; factory < instance.getFactories(); factory++) {
      previousStage[factory] = new int[instance.getWorkerCount(factory)];
      Arrays.fill(previousStage[factory], -1);
    }
    for (ZhangBoFatigueOperationRecord op : result.getOperations()) {
      int recoveryStage = previousStage[op.factory][op.worker] < 0
          ? op.stage : previousStage[op.factory][op.worker];
      trace.append(op.sequence).append(',').append(op.job).append(',').append(op.stage)
          .append(',').append(op.factory).append(',').append(op.machine).append(',')
          .append(op.worker).append(',').append(number(op.predecessorCompletion)).append(',')
          .append(number(op.machineAvailableBefore)).append(',')
          .append(number(op.workerAvailableBefore)).append(',').append(number(op.start)).append(',')
          .append(number(op.recoveryDuration)).append(',')
          .append(number(op.fatigueBeforeRecovery)).append(',')
          .append(number(op.fatigueAtStart)).append(',')
          .append(instance.getStandardTime(op.job, op.stage)).append(',')
          .append(instance.getStandardSetupTime(op.job, op.stage)).append(',')
          .append(number(instance.getMachineSpeed(op.factory, op.stage, op.machine))).append(',')
          .append(instance.getMachinePower(op.factory, op.stage, op.machine)).append(',')
          .append(number(instance.getWorkerEfficiency(op.factory, op.worker))).append(',')
          .append(instance.getWorkerCost(op.factory, op.worker)).append(',')
          .append(number(parameters.getLambda(op.factory, op.worker, op.stage))).append(',')
          .append(number(parameters.getMu(op.factory, op.worker, op.stage))).append(',')
          .append(number(parameters.getMu(op.factory, op.worker, recoveryStage))).append(',')
          .append(number(parameters.getMaximumIncrease(op.stage))).append(',')
          .append(number(parameters.getDelta(op.factory, op.worker, op.stage))).append(',')
          .append(number(op.baseProcessingDuration)).append(',')
          .append(number(op.baseSetupDuration)).append(',').append(number(op.baseDuration))
          .append(',').append(number(op.durationMultiplier)).append(',')
          .append(number(op.actualProcessingDuration)).append(',')
          .append(number(op.actualSetupDuration)).append(',')
          .append(number(op.actualDuration)).append(',').append(number(op.end)).append(',')
          .append(number(op.fatigueAfter)).append(',').append(number(op.energy)).append(',')
          .append(number(op.cost)).append(',').append(op.safeThresholdExceeded).append('\n');
      previousStage[op.factory][op.worker] = op.stage;
    }
    Files.write(directory.resolve("program_trace.csv"),
        trace.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("canonical_result.txt"), result.toCanonicalUtf8());
    writeObjectives(result, directory.resolve("objective_breakdown.csv"));
    writeJobCompletions(result, instance.getJobs(), instance.getStages(),
        directory.resolve("job_final_completion.csv"));
    writeOperationBreakdowns(result, directory);
  }

  private static void writeObjectives(
      ZhangBoFatigueEvaluationResult result, Path path) throws IOException {
    double[] objective = result.getObjectives();
    ZhangBoFatigueMetrics metrics = result.getMetrics();
    String text = "metric,value\n"
        + "Cmax," + number(objective[0]) + "\nTEC," + number(objective[1])
        + "\nTWC," + number(objective[6])
        + "\nFmax," + number(metrics.maximumFatigue)
        + "\nFavg," + number(metrics.averageEventFatigue)
        + "\nFE," + number(metrics.fatigueExcessIntegral)
        + "\nVarFw," + number(metrics.workerFatigueVarianceAtMakespan)
        + "\nhighFatigueRatio," + number(metrics.highFatigueTimeRatio)
        + "\nlongestContinuousWork," + number(metrics.longestContinuousWork)
        + "\ntotalNaturalRecovery," + number(metrics.totalNaturalRecovery)
        + "\nsafeEvents," + metrics.safeThresholdEventCount + "\n";
    Files.write(path, text.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeJobCompletions(
      ZhangBoFatigueEvaluationResult result, int jobs, int stages, Path path) throws IOException {
    double[] completion = new double[jobs];
    for (ZhangBoFatigueOperationRecord op : result.getOperations()) {
      if (op.stage == stages - 1) completion[op.job] = op.end;
    }
    StringBuilder out = new StringBuilder("job,finalCompletion\n");
    for (int job = 0; job < jobs; job++) {
      out.append(job).append(',').append(number(completion[job])).append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeOperationBreakdowns(
      ZhangBoFatigueEvaluationResult result, Path directory) throws IOException {
    StringBuilder energy = new StringBuilder(
        "sequence,factory,stage,machine,job,start,end,actualDuration,energy\n");
    StringBuilder cost = new StringBuilder(
        "sequence,factory,worker,job,start,end,actualDuration,cost\n");
    for (ZhangBoFatigueOperationRecord op : result.getOperations()) {
      energy.append(op.sequence).append(',').append(op.factory).append(',').append(op.stage)
          .append(',').append(op.machine).append(',').append(op.job).append(',')
          .append(number(op.start)).append(',').append(number(op.end)).append(',')
          .append(number(op.actualDuration)).append(',').append(number(op.energy)).append('\n');
      cost.append(op.sequence).append(',').append(op.factory).append(',').append(op.worker)
          .append(',').append(op.job).append(',').append(number(op.start)).append(',')
          .append(number(op.end)).append(',').append(number(op.actualDuration)).append(',')
          .append(number(op.cost)).append('\n');
    }
    Files.write(directory.resolve("energy_breakdown.csv"),
        energy.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("worker_cost_breakdown.csv"),
        cost.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeRegression(
      ZhangBoFatigueEvaluationResult fm0, ZhangBoFatigueEvaluationResult fm3, Path path)
      throws IOException {
    double[] a = fm0.getObjectives();
    double[] b = fm3.getObjectives();
    String text = "metric,FM0,FM3,FM3_minus_FM0\n"
        + row("Cmax", a[0], b[0]) + row("TEC", a[1], b[1])
        + row("TWC", a[6], b[6])
        + row("Fmax", fm0.getMetrics().maximumFatigue, fm3.getMetrics().maximumFatigue)
        + row("FE", fm0.getMetrics().fatigueExcessIntegral,
            fm3.getMetrics().fatigueExcessIntegral);
    Files.write(path, text.getBytes(StandardCharsets.UTF_8));
  }

  private static String row(String name, double fm0, double fm3) {
    return name + ',' + number(fm0) + ',' + number(fm3) + ',' + number(fm3 - fm0) + "\n";
  }

  private static void writeInputTables(
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters, Path input)
      throws IOException {
    StringBuilder jobs = new StringBuilder("job,stage,ST,SUT\n");
    for (int job = 0; job < instance.getJobs(); job++) {
      for (int stage = 0; stage < instance.getStages(); stage++) {
        jobs.append(job).append(',').append(stage).append(',')
            .append(instance.getStandardTime(job, stage)).append(',')
            .append(instance.getStandardSetupTime(job, stage)).append('\n');
      }
    }
    Files.write(input.resolve("job_stage_data.csv"),
        jobs.toString().getBytes(StandardCharsets.UTF_8));

    StringBuilder machines = new StringBuilder("factory,stage,machine,speed,power\n");
    for (int factory = 0; factory < instance.getFactories(); factory++) {
      for (int stage = 0; stage < instance.getStages(); stage++) {
        for (int machine = 0; machine < instance.getMachineCount(factory, stage); machine++) {
          machines.append(factory).append(',').append(stage).append(',').append(machine)
              .append(',').append(number(instance.getMachineSpeed(factory, stage, machine)))
              .append(',').append(instance.getMachinePower(factory, stage, machine)).append('\n');
        }
      }
    }
    Files.write(input.resolve("machine_data.csv"),
        machines.toString().getBytes(StandardCharsets.UTF_8));

    StringBuilder workers = new StringBuilder(
        "factory,stage,worker,efficiency,cost,lambda,mu,r,delta,eligible\n");
    for (int factory = 0; factory < instance.getFactories(); factory++) {
      for (int stage = 0; stage < instance.getStages(); stage++) {
        for (int worker = 0; worker < instance.getWorkerCount(factory); worker++) {
          workers.append(factory).append(',').append(stage).append(',').append(worker)
              .append(',').append(number(instance.getWorkerEfficiency(factory, worker)))
              .append(',').append(instance.getWorkerCost(factory, worker))
              .append(',').append(number(parameters.getLambda(factory, worker, stage)))
              .append(',').append(number(parameters.getMu(factory, worker, stage)))
              .append(',').append(number(parameters.getMaximumIncrease(stage)))
              .append(',').append(number(parameters.getDelta(factory, worker, stage)))
              .append(',').append(instance.isWorkerEligible(factory, stage, worker)).append('\n');
        }
      }
    }
    Files.write(input.resolve("worker_fatigue_data.csv"),
        workers.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeX0(Chapter4GoldenFixture fixture, Path input) throws IOException {
    DhhfspFourVectorSolution solution = fixture.createSolution();
    String zero = "vector,values\nJS,\"" + join(solution.getJobSequence()) + "\"\nFA,\""
        + join(solution.getFactoryAssignments()) + "\"\nMA,\""
        + join(solution.getMachineAssignments()) + "\"\nWA,\""
        + join(solution.getWorkerAssignments()) + "\"\n";
    String one = "vector,values\nJS,\"" + join(fixture.getPublishedJobSequence())
        + "\"\nFA,\"" + join(fixture.getPublishedFactoryAssignments())
        + "\"\nMA,\"" + join(fixture.getPublishedMachineAssignments())
        + "\"\nWA,\"" + join(fixture.getPublishedWorkerAssignments()) + "\"\n";
    Files.write(input.resolve("X0-zero-based.csv"), zero.getBytes(StandardCharsets.UTF_8));
    Files.write(input.resolve("X0-paper-one-based.csv"), one.getBytes(StandardCharsets.UTF_8));
  }

  private static String join(List<Integer> values) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) result.append(',');
      result.append(values.get(index));
    }
    return result.toString();
  }

  private static void copyFrozenInput(Path bridgeRoot, Path input) throws IOException {
    copy(bridgeRoot.resolve("bridge-manifest.txt"), input.resolve("bridge-manifest.txt"));
    copy(bridgeRoot.resolve("EADHFSP/10_2_2_1.txt"), input.resolve("10_2_2_1.txt"));
    copy(bridgeRoot.resolve("instance-extensions/v1/10_2_2_1.setup.txt"),
        input.resolve("10_2_2_1.setup.txt"));
    copy(bridgeRoot.resolve("fatigue-parameters/v1/10_2_2_1.fatigue.txt"),
        input.resolve("10_2_2_1.fatigue.txt"));
    try (InputStream stream = P8GoldenAuthorCompatibilityBridge.class.getResourceAsStream(
        Chapter4GoldenFixture.RESOURCE)) {
      if (stream == null) throw new IOException("Missing " + Chapter4GoldenFixture.RESOURCE);
      Files.copy(stream, input.resolve("eswa-2026-130934-golden.properties"));
    }
  }

  private static void copy(Path source, Path target) throws IOException {
    requireFile(source);
    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
  }

  private static String environment() {
    return "versionId=" + VERSION + "\njava.version=" + System.getProperty("java.version")
        + "\njava.vendor=" + System.getProperty("java.vendor")
        + "\nos.name=" + System.getProperty("os.name")
        + "\nos.arch=" + System.getProperty("os.arch")
        + "\njmetal.version=5.8\nseed=" + SEED + "\n";
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }

  private static String sha256(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      try (InputStream input = Files.newInputStream(path)) {
        for (int read; (read = input.read(buffer)) >= 0; ) digest.update(buffer, 0, read);
      }
      StringBuilder result = new StringBuilder();
      for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Missing file: " + path);
  }

  private static void requireEmptyOrMissing(Path path) throws IOException {
    if (!Files.exists(path)) return;
    if (!Files.isDirectory(path)) throw new IllegalArgumentException("Output is not a directory: " + path);
    try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
      if (children.iterator().hasNext()) {
        throw new IllegalArgumentException("Output directory is not empty: " + path);
      }
    }
  }

  private static final class Arguments {
    private final Path projectRoot;
    private final Path output;

    private Arguments(Path projectRoot, Path output) {
      this.projectRoot = projectRoot;
      this.output = output;
    }

    private static Arguments parse(String[] args) {
      Path projectRoot = null;
      Path output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--project-root".equals(args[index])) projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (projectRoot == null || output == null) throw usage();
      return new Arguments(projectRoot, output);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "Usage: --project-root <ZhangBo project root> --output <empty directory>");
    }
  }
}
