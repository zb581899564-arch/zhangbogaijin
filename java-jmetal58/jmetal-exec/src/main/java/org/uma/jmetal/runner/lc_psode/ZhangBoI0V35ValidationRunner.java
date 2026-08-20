package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/** Runs the frozen five-job I0-v35 particle once through the formal FM3 decoder. */
public final class ZhangBoI0V35ValidationRunner {
  private static final long SEED = 20260808L;

  private ZhangBoI0V35ValidationRunner() { }

  public static void main(String[] args) throws Exception {
    Path output = output(args).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      try (java.util.stream.Stream<Path> children = Files.list(output)) {
        if (children.findAny().isPresent()) {
          throw new IllegalArgumentException("Output directory must be empty: " + output);
        }
      }
    }
    Files.createDirectories(output);
    run(output);
    System.out.println("I0_V35_FM3_COMPLETED output=" + output);
  }

  static void run(Path output) throws Exception {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = parameters(instance.getInstanceSha256());
    ZhangBoCanonicalProductionProblem problem =
        ZhangBoV35ProblemFactory.fm3(instance, parameters, SEED);
    DhhfspFourVectorSolution solution = particle();
    problem.evaluate(solution);
    ZhangBoFatigueEvaluationResult result =
        (ZhangBoFatigueEvaluationResult) solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    if (result == null) throw new IllegalStateException("Missing FM3 decode result");
    if (result.getShiftSummary() != null) {
      throw new IllegalStateException("I0-v35 formal validation must use ShiftMode.NONE");
    }
    writeTrace(result, instance, parameters, output.resolve("java_trace.csv"));
    writeObjectives(result, output.resolve("java_objectives.csv"));
    Files.write(output.resolve("canonical_result.txt"), result.toCanonicalUtf8());
    String manifest = "schemaVersion=1\ninstanceId=I0-v35\nseed=" + SEED
        + "\ndecoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY"
        + "\nsetupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE"
        + "\noperations=" + result.getOperations().size()
        + "\nevaluations=" + problem.getEvaluationCounter().getSuccessfulEvaluations()
        + "\ntraceSha256=" + sha256(output.resolve("java_trace.csv"))
        + "\nobjectivesSha256=" + sha256(output.resolve("java_objectives.csv")) + "\n";
    Files.write(output.resolve("manifest.properties"), manifest.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeTrace(
      ZhangBoFatigueEvaluationResult result, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, Path path) throws Exception {
    int[][] previousStage = new int[instance.getFactories()][];
    for (int factory = 0; factory < instance.getFactories(); factory++) {
      previousStage[factory] = new int[instance.getWorkerCount(factory)];
      Arrays.fill(previousStage[factory], -1);
    }
    StringBuilder out = new StringBuilder();
    out.append("sequence,job,stage,factory,machine,worker,predecessorCompletion,")
        .append("machineAvailableBefore,workerAvailableBefore,start,recoveryDuration,")
        .append("fatigueBeforeRecovery,fatigueAtStart,ST,SUT,machineSpeed,machinePower,")
        .append("workerEfficiency,workerCost,lambda,muCurrentStage,recoveryMu,r,delta,")
        .append("baseProcessing,baseSetup,baseAT,fatigueMultiplier,actualProcessing,")
        .append("actualSetup,actualAT,end,fatigueAfter,machineIdle,energy,cost,")
        .append("safeThresholdExceeded\n");
    for (ZhangBoFatigueOperationRecord op : result.getOperations()) {
      int recoveryStage = previousStage[op.factory][op.worker] < 0
          ? op.stage : previousStage[op.factory][op.worker];
      double machineIdle = op.energy
          - op.actualDuration * instance.getMachinePower(op.factory, op.stage, op.machine);
      out.append(op.sequence + 1).append(',').append(op.job + 1).append(',')
          .append(op.stage + 1).append(',').append(op.factory + 1).append(',')
          .append(op.machine + 1).append(',').append(op.worker + 1).append(',')
          .append(number(op.predecessorCompletion)).append(',')
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
          .append(number(op.baseSetupDuration)).append(',').append(number(op.baseDuration)).append(',')
          .append(number(op.durationMultiplier)).append(',')
          .append(number(op.actualProcessingDuration)).append(',')
          .append(number(op.actualSetupDuration)).append(',')
          .append(number(op.actualDuration)).append(',').append(number(op.end)).append(',')
          .append(number(op.fatigueAfter)).append(',').append(number(machineIdle)).append(',')
          .append(number(op.energy)).append(',').append(number(op.cost)).append(',')
          .append(op.safeThresholdExceeded).append('\n');
      previousStage[op.factory][op.worker] = op.stage;
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeObjectives(ZhangBoFatigueEvaluationResult result, Path path)
      throws Exception {
    double[] objective = result.getObjectives();
    ZhangBoFatigueMetrics metrics = result.getMetrics();
    String out = "metric,value\n"
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
    Files.write(path, out.getBytes(StandardCharsets.UTF_8));
  }

  private static ZhangBoFatigueInstanceData instance() {
    String instanceSha = "88efa7c0ef6bb1b5f434f592e6ace627ebf1a63cbfabcd5a630894a167e5c9af";
    String extensionSha = sha256Utf8(
        "schemaVersion=1\ninstanceId=I0-v35\nsetupMode=SEQUENCE_INDEPENDENT\n"
            + "sut=2,1;1,2;2,1;1,2;1,1\n");
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        instanceSha, 5, 2,
        new int[][] {{2, 1}, {1, 2}, {2, 1}, {1, 2}, {1, 1}}, extensionSha);
    return new ZhangBoFatigueInstanceData(instanceSha, 5, 2, 2,
        new int[][] {{2, 2}, {2, 2}},
        new double[][][] {{{1.00, 1.25}, {1.00, 1.25}},
            {{1.00, 1.20}, {1.10, 1.25}}},
        new int[][][] {{{6, 8}, {7, 9}}, {{6, 7}, {7, 8}}},
        new int[][] {{10, 6}, {6, 8}, {8, 5}, {7, 9}, {5, 7}},
        new int[] {4, 4},
        new double[][] {{1.00, 1.00, 1.00, 1.20}, {1.00, 1.10, 1.00, 1.15}},
        new int[][] {{10, 11, 10, 12}, {10, 11, 10, 12}}, extension);
  }

  private static ZhangBoFatigueParameters parameters(String instanceSha) {
    String configurationSha = sha256Utf8(
        "schemaVersion=1\ninstanceId=I0-v35\nseed=20260808\n"
            + "lambda=0.020,0.025,0.020,0.020;0.020,0.020,0.018,0.022;"
            + "0.021,0.026,0.020,0.020;0.020,0.020,0.019,0.023\n"
            + "mu=0.050,0.040,0.050,0.050;0.050,0.050,0.060,0.050;"
            + "0.050,0.040,0.050,0.050;0.050,0.050,0.060,0.050\n"
            + "r=0.30,0.30\nFwarn=0.80\nFsafe=0.90\n");
    return new ZhangBoFatigueParameters(instanceSha,
        new double[][][] {
            {{0.020, 0.025, 0.020, 0.020}, {0.020, 0.020, 0.018, 0.022}},
            {{0.021, 0.026, 0.020, 0.020}, {0.020, 0.020, 0.019, 0.023}}},
        new double[][][] {
            {{0.050, 0.040, 0.050, 0.050}, {0.050, 0.050, 0.060, 0.050}},
            {{0.050, 0.040, 0.050, 0.050}, {0.050, 0.050, 0.060, 0.050}}},
        new double[] {0.30, 0.30}, 0.80, 0.90, configurationSha);
  }

  private static DhhfspFourVectorSolution particle() {
    List<Integer> js = Arrays.asList(2, 0, 1, 3, 4);
    List<Integer> fa = Arrays.asList(0, 1, 1, 0, 0);
    List<Integer> ma = Arrays.asList(0, 0, 0, 0, 1);
    List<Integer> wa = Arrays.asList(0, 0, 0, 1, 1);
    return new DhhfspFourVectorSolution(js, fa, ma, wa,
        ProductionDecodeMode.FM3.getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
  }

  private static Path output(String[] args) {
    if (args.length != 2 || !"--output".equals(args[0])) {
      throw new IllegalArgumentException("Usage: --output <directory>");
    }
    return Paths.get(args[1]);
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }

  private static String sha256(Path path) throws Exception {
    byte[] bytes = Files.readAllBytes(path);
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format(Locale.ROOT, "%02x", value & 0xff));
    return out.toString();
  }

  private static String sha256Utf8(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder(64);
      for (byte current : digest) out.append(String.format(Locale.ROOT, "%02X", current & 0xff));
      return out.toString();
    } catch (Exception exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

}
