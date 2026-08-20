package org.uma.jmetal.runner.lc_psode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunRecord;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunStatus;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;
import org.uma.jmetal.solution.PermutationSolution;

/** Scope-locked FULL -> HMOPSO-QGS-F -> REPORT runner for P9. */
public final class ZhangBoP9SingleComparisonRunner {
  private static final String INSTANCE_NAME = "20_2_3_1";
  private static final String INSTANCE_FILE = "20_2_3_1.txt";
  private static final String EXTENSION_FILE = "20_2_3_1.setup.txt";
  private static final String FATIGUE_FILE = "20_2_3_1.fatigue.txt";
  private static final DateTimeFormatter ATTEMPT_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

  enum Phase { FULL, HMOPSO_QGS_F, REPORT }

  private ZhangBoP9SingleComparisonRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    runPhase(parsed.phase, parsed.projectRoot, parsed.output,
        ZhangBoP9FormalParameters.formal());
  }

  /** Package-private entry for the 2000-FE Batch-0 test only. */
  static void runPhase(Phase phase, Path projectRoot, Path output,
      ZhangBoP9FormalParameters parameters) throws Exception {
    requireProject(projectRoot);
    Files.createDirectories(output);
    if (phase == Phase.REPORT) {
      runReport(output, parameters);
    } else {
      runAlgorithm(phase, projectRoot, output, parameters);
    }
  }

  private static void runAlgorithm(Phase phase, Path projectRoot, Path output,
      ZhangBoP9FormalParameters parameters) throws Exception {
    String alias = alias(phase);
    String sourceLabel = phase == Phase.FULL ? "FULL" : "B1";
    Path finalDirectory = output.resolve(alias + "-" + parameters.getSeed());
    if (Files.exists(finalDirectory)) {
      throw new IllegalStateException("P9 result already exists; refusing overwrite: "
          + finalDirectory);
    }
    Path fullDirectory = output.resolve("ZHANGBO-FULL-" + parameters.getSeed());
    if (phase == Phase.HMOPSO_QGS_F) requireFullGate(fullDirectory, parameters);

    String attempt = ATTEMPT_TIME.format(LocalDateTime.now());
    Path partial = output.resolve(".partial-" + alias + "-" + parameters.getSeed()
        + "-" + attempt);
    Files.createDirectory(partial);
    try {
      Path instancePath = projectRoot.resolve("EADHFSP").resolve(INSTANCE_FILE);
      Path extensionDirectory = projectRoot.resolve("instance-extensions/v1");
      Path fatigueDirectory = projectRoot.resolve("fatigue-parameters/v1");
      Path extensionPath = extensionDirectory.resolve(EXTENSION_FILE);
      Path fatiguePath = fatigueDirectory.resolve(FATIGUE_FILE);
      String instanceHash = sha256(instancePath);
      String extensionHash = sha256(extensionPath);
      String fatigueHash = sha256(fatiguePath);

      P8ExperimentSpec source = P8ExperimentRegistry.find(sourceLabel);
      if (source.getAblationProfile().getDecoderMode()
          != org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.DecoderMode
          .FATIGUE_AWARE_SELECTION) {
        throw new IllegalStateException(sourceLabel + " is not the FM3 comparison profile");
      }
      P8ExperimentSpec spec = new P8ExperimentSpec(source.getMatrix(), alias,
          source.getMechanism(), source.getConfigurationKey() + "-p9-single",
          source.getAblationProfile(), P8RunStatus.COMPLETED,
          "P9 single 500000-FE diagnostic comparison", parameters.getPopulation(),
          parameters.getMaxFEs(), parameters.getPhysicalSubswarmSizes());
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instancePath, ProductionDecodeMode.FM3, parameters.getSeed(),
          extensionDirectory, fatigueDirectory,
          source.getAblationProfile().getShiftConfiguration());
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < parameters.getPopulation(); index++) {
        initial.add(problem.createSolution());
      }
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      if (phase == Phase.HMOPSO_QGS_F) {
        Map<String, String> fullStatus = readKeyValues(fullDirectory.resolve("status.properties"));
        requireEqual("initial population", fullStatus.get("initialPopulationSha256"), initialHash);
        requireEqual("instance", fullStatus.get("instanceSha256"), instanceHash);
        requireEqual("SUT extension", fullStatus.get("instanceExtensionSha256"), extensionHash);
        requireEqual("fatigue manifest", fullStatus.get("fatigueParametersSha256"), fatigueHash);
      }
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  initial-four-vector-population\n")
              .getBytes(StandardCharsets.UTF_8));
      appendConsole(partial, "START phase=" + phase + " alias=" + alias
          + " seed=" + parameters.getSeed() + " maxFEs=" + parameters.getMaxFEs());

      ZhangBoP9FormalRunResult result = ZhangBoP9FormalExecutor.execute(alias,
          sourceLabel, spec, parameters, INSTANCE_NAME, instanceHash, problem,
          P8InitialPopulationProvider.copy(initial));
      String gateFailure = gateFailure(phase, result, parameters);
      writeRunArtifacts(partial, phase, sourceLabel, parameters, result,
          instanceHash, extensionHash, fatigueHash,
          gateFailure == null ? "COMPLETED" : "FAILED",
          gateFailure == null ? result.record.getReason() : gateFailure);
      if (gateFailure != null) {
        appendConsole(partial, "FAILED hardGate=" + gateFailure);
        writeEvidenceHashes(partial);
        Path failed = output.resolve("failed-" + alias + "-" + parameters.getSeed()
            + "-" + attempt);
        moveDirectory(partial, failed);
        throw new IllegalStateException(gateFailure + "; evidence=" + failed);
      }
      appendConsole(partial, "COMPLETED fe=" + result.record.getFullEvaluations()
          + " front=" + result.record.getFront().size());
      writeEvidenceHashes(partial);
      moveDirectory(partial, finalDirectory);
      System.out.println("P9_PHASE_COMPLETED phase=" + phase + " output=" + finalDirectory
          + " fe=" + result.record.getFullEvaluations());
    } catch (Exception exception) {
      if (Files.exists(partial)) {
        appendConsole(partial, "EXCEPTION " + exception.getClass().getName() + ": "
            + String.valueOf(exception.getMessage()));
        writeEvidenceHashes(partial);
        Path failed = output.resolve("failed-" + alias + "-" + parameters.getSeed()
            + "-" + attempt);
        if (!Files.exists(failed)) moveDirectory(partial, failed);
      }
      throw exception;
    }
  }

  private static String gateFailure(Phase phase, ZhangBoP9FormalRunResult result,
      ZhangBoP9FormalParameters parameters) {
    P8RunRecord record = result.record;
    if (record.getStatus() != P8RunStatus.COMPLETED) {
      return "RUN_NOT_COMPLETED: " + record.getReason();
    }
    if (record.getFullEvaluations() < parameters.getPopulation()
        || record.getFullEvaluations() > parameters.getMaxFEs()) {
      return "FE_OUT_OF_RANGE=" + record.getFullEvaluations();
    }
    if (record.getFront().isEmpty()) return "EMPTY_FRONT";
    if (!finite(record.getFront())) return "NON_FINITE_FRONT";
    if (record.getIllegalSolutions() != 0) {
      return "ILLEGAL_SOLUTIONS=" + record.getIllegalSolutions();
    }
    if (record.getCfvfRepairs() != 0) return "CFVF_REPAIRS=" + record.getCfvfRepairs();
    ZhangBoDecoderTimingSnapshot timing = result.decoderTiming;
    if (timing.getSuccessfulDecoderCalls() != record.getFullEvaluations()) {
      return "DECODER_FE_MISMATCH decoder=" + timing.getSuccessfulDecoderCalls()
          + " fe=" + record.getFullEvaluations();
    }
    if (timing.getBaseDecodeNanos() <= 0L || timing.getLeftShiftNanos() <= 0L
        || timing.getRightShiftNanos() <= 0L) {
      return "DECODER_TIMING_MISSING base=" + timing.getBaseDecodeNanos()
          + " left=" + timing.getLeftShiftNanos()
          + " right=" + timing.getRightShiftNanos();
    }
    if (result.algorithmRunNanos < timing.getDecoderTotalNanos()) {
      return "DECODER_TIME_EXCEEDS_ALGORITHM decoder=" + timing.getDecoderTotalNanos()
          + " algorithm=" + result.algorithmRunNanos;
    }
    if (timing.getInternalPropagations()
        != timing.getLeftFullRecomputations() + timing.getRightFullRecomputations()) {
      return "DECODER_PROPAGATION_COUNT_MISMATCH";
    }
    if (result.qgSelections <= 0 || result.pddrEvents <= 0) {
      return "BASELINE_MECHANISM_MISSING qg=" + result.qgSelections
          + " pddr=" + result.pddrEvents;
    }
    if (phase == Phase.FULL) {
      if (result.cfvfOffspring <= 0 || result.qpActions <= 0
          || result.archiveInsertions <= 0 || result.caTaTestCalls <= 0
          || result.caTaApplyCalls <= 0) {
        return "FULL_MECHANISM_MISSING cfvf=" + result.cfvfOffspring
            + " qp=" + result.qpActions + " archive=" + result.archiveInsertions
            + " test=" + result.caTaTestCalls + " apply=" + result.caTaApplyCalls;
      }
      if (result.formalOuterCycles != 0 || result.formalQgRounds != 0) {
        return "FULL_FORMAL_BASELINE_LOOP_LEAK outer=" + result.formalOuterCycles
            + " qRounds=" + result.formalQgRounds;
      }
    } else {
      if (result.baselineUpdateEvents <= 0 || result.fixedNeighborhoodEvents <= 0) {
        return "BASELINE_OPERATOR_MISSING update=" + result.baselineUpdateEvents
            + " fixedO1O9=" + result.fixedNeighborhoodEvents;
      }
      if (result.qpActions != 0 || result.archiveInsertions != 0
          || result.caTaTestCalls != 0 || result.caTaApplyCalls != 0) {
        return "BASELINE_INNOVATION_LEAK qp=" + result.qpActions
            + " archive=" + result.archiveInsertions + " test="
            + result.caTaTestCalls + " apply=" + result.caTaApplyCalls;
      }
      if (result.formalOuterCycles <= 0
          || result.formalQgRounds
              != result.formalOuterCycles * parameters.getQTimes()
          || result.formalCriticalFactorySwaps <= 0
          || result.formalCriticalFactoryInserts <= 0
          || result.formalO1O9Evaluations <= 0) {
        return "FORMAL_BASELINE_RUNTIME_MISMATCH outer=" + result.formalOuterCycles
            + " qRounds=" + result.formalQgRounds
            + " qTimes=" + parameters.getQTimes()
            + " criticalSwap=" + result.formalCriticalFactorySwaps
            + " criticalInsert=" + result.formalCriticalFactoryInserts
            + " o1o9=" + result.formalO1O9Evaluations;
      }
    }
    return null;
  }

  private static void writeRunArtifacts(Path directory, Phase phase, String sourceLabel,
      ZhangBoP9FormalParameters parameters, ZhangBoP9FormalRunResult result,
      String instanceHash, String extensionHash, String fatigueHash,
      String finalStatus, String finalReason) throws IOException {
    P8RunRecord record = result.record;
    Files.write(directory.resolve("configuration.txt"),
        record.getConfigurationText().getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("mechanism-summary.txt"),
        result.mechanismSummary.getBytes(StandardCharsets.UTF_8));
    writeFront(directory.resolve("front.csv"), record.getFront());
    Files.write(directory.resolve("run-record.csv"), runRecordCsv(record)
        .getBytes(StandardCharsets.UTF_8));
    Files.write(directory.resolve("decoder-timing.csv"), decoderTimingCsv(result)
        .getBytes(StandardCharsets.UTF_8));
    Map<String, String> status = new LinkedHashMap<>();
    status.put("schema", "zhangbo-p9-status-v2-decoder-timing");
    status.put("phase", phase.name());
    status.put("algorithmAlias", alias(phase));
    status.put("sourceP8Label", sourceLabel);
    status.put("status", finalStatus);
    status.put("reason", finalReason);
    status.put("seed", Long.toString(parameters.getSeed()));
    status.put("population", Integer.toString(parameters.getPopulation()));
    status.put("maxFEs", Integer.toString(parameters.getMaxFEs()));
    status.put("fullEvaluations", Long.toString(record.getFullEvaluations()));
    status.put("wallClockMillis", Long.toString(record.getWallClockMillis()));
    status.put("cpuNanos", Long.toString(record.getCpuNanos()));
    status.put("algorithmRunNanos", Long.toString(result.algorithmRunNanos));
    status.put("experimentWallNanos", Long.toString(result.experimentWallNanos));
    status.put("successfulDecoderCalls",
        Long.toString(result.decoderTiming.getSuccessfulDecoderCalls()));
    status.put("baseDecodeNanos",
        Long.toString(result.decoderTiming.getBaseDecodeNanos()));
    status.put("leftShiftNanos",
        Long.toString(result.decoderTiming.getLeftShiftNanos()));
    status.put("rightShiftNanos",
        Long.toString(result.decoderTiming.getRightShiftNanos()));
    status.put("decoderTotalNanos",
        Long.toString(result.decoderTiming.getDecoderTotalNanos()));
    status.put("decoderFrameworkOverheadNanos",
        Long.toString(result.decoderTiming.getDecoderFrameworkOverheadNanos()));
    status.put("leftFullRecomputations",
        Long.toString(result.decoderTiming.getLeftFullRecomputations()));
    status.put("rightFullRecomputations",
        Long.toString(result.decoderTiming.getRightFullRecomputations()));
    status.put("leftAccepted", Long.toString(result.decoderTiming.getLeftAccepted()));
    status.put("rightAccepted", Long.toString(result.decoderTiming.getRightAccepted()));
    status.put("configurationSha256", record.getConfigurationSha256());
    status.put("formalParameterSha256", parameters.sha256());
    status.put("mechanismVectorHash", result.mechanismVectorHash);
    P8ExperimentSpec sourceSpec = P8ExperimentRegistry.find(sourceLabel);
    String shiftText = sourceSpec.getAblationProfile().getShiftConfiguration().toCanonicalText();
    status.put("shiftMode", sourceSpec.getAblationProfile().getShiftMode().name());
    status.put("shiftConfigurationSha256", sha256Text(shiftText));
    status.put("initialPopulationSha256", record.getInitialPopulationSha256());
    status.put("instanceSha256", instanceHash);
    status.put("instanceExtensionSha256", extensionHash);
    status.put("fatigueParametersSha256", fatigueHash);
    status.put("frontSha256", sha256(directory.resolve("front.csv")));
    status.put("frontSize", Integer.toString(record.getFront().size()));
    status.put("illegalSolutions", Integer.toString(record.getIllegalSolutions()));
    status.put("cfvfRepairs", Long.toString(record.getCfvfRepairs()));
    status.put("cfvfOffspring", Long.toString(result.cfvfOffspring));
    status.put("pddrEvents", Long.toString(result.pddrEvents));
    status.put("baselineUpdateEvents", Long.toString(result.baselineUpdateEvents));
    status.put("fixedNeighborhoodEvents", Long.toString(result.fixedNeighborhoodEvents));
    status.put("archiveInsertions", Long.toString(result.archiveInsertions));
    status.put("qgSelections", Long.toString(result.qgSelections));
    status.put("qpActions", Long.toString(result.qpActions));
    status.put("caTaTestCalls", Long.toString(result.caTaTestCalls));
    status.put("caTaApplyCalls", Long.toString(result.caTaApplyCalls));
    status.put("formalOuterCycles", Long.toString(result.formalOuterCycles));
    status.put("formalQgRounds", Long.toString(result.formalQgRounds));
    status.put("formalCriticalFactorySwaps",
        Long.toString(result.formalCriticalFactorySwaps));
    status.put("formalCriticalFactoryInserts",
        Long.toString(result.formalCriticalFactoryInserts));
    status.put("formalO1O9Evaluations", Long.toString(result.formalO1O9Evaluations));
    status.put("localFullEvaluations", Long.toString(record.getCaTaEvaluations()));
    status.put("Fmax", number(record.getFmax()));
    status.put("Favg", number(record.getFavg()));
    status.put("fatigueExcess", number(record.getFatigueExcess()));
    status.put("workerFatigueVariance", number(record.getWorkerFatigueVariance()));
    status.put("highFatigueRatio", number(record.getHighFatigueRatio()));
    status.put("longestContinuousWork", number(record.getLongestContinuousWork()));
    status.put("totalNaturalRecovery", number(record.getTotalNaturalRecovery()));
    status.put("loadImbalance", number(record.getLoadImbalance()));
    writeKeyValues(directory.resolve("status.properties"), status);
  }

  private static void runReport(Path output, ZhangBoP9FormalParameters parameters)
      throws Exception {
    Path fullDir = output.resolve("ZHANGBO-FULL-" + parameters.getSeed());
    Path baseDir = output.resolve("HMOPSO-QGS-F-" + parameters.getSeed());
    Map<String, String> fullStatus = requireCompleted(fullDir);
    Map<String, String> baseStatus = requireCompleted(baseDir);
    for (String key : Arrays.asList("initialPopulationSha256", "instanceSha256",
        "instanceExtensionSha256", "fatigueParametersSha256", "formalParameterSha256",
        "shiftMode", "shiftConfigurationSha256")) {
      requireEqual(key, fullStatus.get(key), baseStatus.get(key));
    }
    List<double[]> full = readFront(fullDir.resolve("front.csv"));
    List<double[]> baseline = readFront(baseDir.resolve("front.csv"));
    List<double[]> union = new ArrayList<>();
    union.addAll(full);
    union.addAll(baseline);
    List<double[]> reference = P8MetricCalculator.nondominated(union);
    P8MetricCalculator.Metrics fullMetrics = P8MetricCalculator.calculate(full, reference);
    P8MetricCalculator.Metrics baseMetrics = P8MetricCalculator.calculate(baseline, reference);
    double cFullBase = coverage(full, baseline);
    double cBaseFull = coverage(baseline, full);
    double[] fullMin = minima(full), baseMin = minima(baseline);
    String signal = signal(cFullBase, cBaseFull, fullMin, baseMin);

    Path finalDir = output.resolve("comparison");
    if (Files.exists(finalDir)) {
      throw new IllegalStateException("Comparison already exists; refusing overwrite: " + finalDir);
    }
    Path partial = output.resolve(".partial-comparison-" + ATTEMPT_TIME.format(LocalDateTime.now()));
    Files.createDirectory(partial);
    writeFront(partial.resolve("reference-front.csv"), reference);
    String metrics = "algorithm,frontSize,HV,IGD,Spacing,C_against_other,other_C_against_self,"
        + "minCmax,minTEC,minTWC,maxCmax,maxTEC,maxTWC\n"
        + metricRow("ZHANGBO-FULL", full, fullMetrics, cFullBase, cBaseFull)
        + metricRow("HMOPSO-QGS-F", baseline, baseMetrics, cBaseFull, cFullBase);
    Files.write(partial.resolve("metrics.csv"), metrics.getBytes(StandardCharsets.UTF_8));
    Files.write(partial.resolve("decoder-timing-comparison.csv"),
        decoderTimingComparisonCsv(fullStatus, baseStatus).getBytes(StandardCharsets.UTF_8));
    Files.write(partial.resolve("DECODER_TIMING_REPORT.md"),
        decoderTimingReport(fullStatus, baseStatus).getBytes(StandardCharsets.UTF_8));
    String report = report(signal, fullStatus, baseStatus, full, baseline,
        fullMetrics, baseMetrics, cFullBase, cBaseFull, reference.size());
    Files.write(partial.resolve("P9_SINGLE_COMPARISON_REPORT.md"),
        report.getBytes(StandardCharsets.UTF_8));
    Map<String, String> status = new LinkedHashMap<>();
    status.put("schema", "zhangbo-p9-comparison-v2-decoder-timing");
    status.put("status", "COMPLETED");
    status.put("signal", signal);
    status.put("sameInitialPopulation", "true");
    status.put("seed", Long.toString(parameters.getSeed()));
    status.put("referenceFrontSize", Integer.toString(reference.size()));
    writeKeyValues(partial.resolve("status.properties"), status);
    writeEvidenceHashes(partial);
    moveDirectory(partial, finalDir);
    System.out.println("P9_REPORT_COMPLETED signal=" + signal + " output=" + finalDir);
  }

  private static String report(String signal, Map<String, String> fullStatus,
      Map<String, String> baseStatus, List<double[]> full, List<double[]> baseline,
      P8MetricCalculator.Metrics fm, P8MetricCalculator.Metrics bm,
      double cFullBase, double cBaseFull, int referenceSize) {
    double[] fmin = minima(full), fmax = maxima(full);
    double[] bmin = minima(baseline), bmax = maxima(baseline);
    return "# P9两算法单次500000 FE诊断比较\n\n"
        + "- 结论标签：`" + signal + "`。这是单实例、单seed证据信号，不是显著性或论文优越性结论。\n"
        + "- 实例：`20_2_3_1`；seed：`20260808`；种群：`100`；预算上限：`500000 FE`。\n"
        + "- 两算法使用相同FM3疲劳问题、SUT、疲劳参数和初始四向量种群。\n"
        + "- 临时参考前沿为两次运行结果并集的非支配集合，共`" + referenceSize + "`点，仅供本次诊断。\n\n"
        + "|算法|FE|前沿点|Cmax范围|TEC范围|TWC范围|HV|IGD|Spacing|\n"
        + "|---|---:|---:|---:|---:|---:|---:|---:|---:|\n"
        + row("ZHANGBO-FULL", fullStatus, full.size(), fmin, fmax, fm)
        + row("HMOPSO-QGS-F", baseStatus, baseline.size(), bmin, bmax, bm)
        + "\n双向覆盖：`C(FULL,BASE)=" + number(cFullBase)
        + "`，`C(BASE,FULL)=" + number(cBaseFull) + "`。\n\n"
        + "疲劳诊断（最终非支配集聚合）：\n\n"
        + "|算法|Fmax|Favg|FE|Var(Fw)|高疲劳比例|最长连续工作|自然恢复|负载不均衡|\n"
        + "|---|---:|---:|---:|---:|---:|---:|---:|---:|\n"
        + fatigueRow("ZHANGBO-FULL", fullStatus)
        + fatigueRow("HMOPSO-QGS-F", baseStatus)
        + "\n机制硬门：FULL的CFVF、Qg/Qp、谱系档案和CA-TA Test/Apply均已触发；"
        + "HMOPSO-QGS-F仅保留原Qg、评价后PDDR、规范资源GA和固定O1–O9。\n\n"
        + "Decoder分阶段耗时及左右移完整重传播开销见"
        + "`decoder-timing-comparison.csv`和`DECODER_TIMING_REPORT.md`。\n\n"
        + "`sampled_reproduction_accepted=false`；`full_reproduction_accepted=false`；"
        + "`formal_20_run_matrix_started=false`；`ablation_started=false`。\n";
  }

  private static String decoderTimingComparisonCsv(
      Map<String, String> full, Map<String, String> baseline) {
    String header = "algorithm,fullEvaluations,successfulDecoderCalls,algorithmRunNanos,"
        + "experimentWallNanos,baseDecodeNanos,leftShiftNanos,rightShiftNanos,"
        + "decoderTotalNanos,decoderFrameworkOverheadNanos,searchControlNanos,"
        + "algorithmExcludingShiftNanos,leftFullRecomputations,rightFullRecomputations,"
        + "leftAccepted,rightAccepted,baseDecodePercent,leftShiftPercent,rightShiftPercent,"
        + "decoderTotalPercent,searchControlPercent,avgLeftRecomputationsPerFE,"
        + "avgRightRecomputationsPerFE,avgLeftMicrosPerRecomputation,"
        + "avgRightMicrosPerRecomputation\n";
    return header + decoderTimingComparisonRow("ZHANGBO-FULL", full)
        + decoderTimingComparisonRow("HMOPSO-QGS-F", baseline);
  }

  private static String decoderTimingComparisonRow(
      String algorithm, Map<String, String> status) {
    long fe = longValue(status, "fullEvaluations");
    long calls = longValue(status, "successfulDecoderCalls");
    long run = longValue(status, "algorithmRunNanos");
    long base = longValue(status, "baseDecodeNanos");
    long left = longValue(status, "leftShiftNanos");
    long right = longValue(status, "rightShiftNanos");
    long decoder = longValue(status, "decoderTotalNanos");
    long leftCount = longValue(status, "leftFullRecomputations");
    long rightCount = longValue(status, "rightFullRecomputations");
    return algorithm + ',' + fe + ',' + calls + ',' + run + ','
        + longValue(status, "experimentWallNanos") + ',' + base + ',' + left + ','
        + right + ',' + decoder + ',' + longValue(status, "decoderFrameworkOverheadNanos")
        + ',' + (run - decoder) + ',' + (run - left - right) + ',' + leftCount + ','
        + rightCount + ',' + longValue(status, "leftAccepted") + ','
        + longValue(status, "rightAccepted") + ',' + number(percent(base, run)) + ','
        + number(percent(left, run)) + ',' + number(percent(right, run)) + ','
        + number(percent(decoder, run)) + ',' + number(percent(run - decoder, run)) + ','
        + number(calls == 0L ? 0.0 : ((double) leftCount) / calls) + ','
        + number(calls == 0L ? 0.0 : ((double) rightCount) / calls) + ','
        + number(averageMicros(left, leftCount)) + ','
        + number(averageMicros(right, rightCount)) + '\n';
  }

  private static String decoderTimingReport(
      Map<String, String> full, Map<String, String> baseline) {
    return "# Decoder分阶段耗时报告\n\n"
        + "计时口径为单线程单调墙钟时间。`algorithmRunNanos`只包围"
        + "`algorithm.run()`；基础解码、FCLS和FCRS统计覆盖全部完整FE。\n\n"
        + "|算法|算法总耗时(s)|基础解码(s)|FCLS(s)|FCRS(s)|Decoder占比|"
        + "去除Decoder搜索控制(s)|左/右完整重算|\n"
        + "|---|---:|---:|---:|---:|---:|---:|---:|\n"
        + decoderTimingReportRow("ZHANGBO-FULL", full)
        + decoderTimingReportRow("HMOPSO-QGS-F", baseline)
        + "\n`algorithmExcludingShiftNanos`表示保留基础解码、但扣除FCLS/FCRS的"
        + "实测算法时间；它是开销分解，不是关闭移位后的反事实运行结果。\n";
  }

  private static String decoderTimingReportRow(
      String algorithm, Map<String, String> status) {
    long run = longValue(status, "algorithmRunNanos");
    long decoder = longValue(status, "decoderTotalNanos");
    return "|" + algorithm + "|" + seconds(run) + "|"
        + seconds(longValue(status, "baseDecodeNanos")) + "|"
        + seconds(longValue(status, "leftShiftNanos")) + "|"
        + seconds(longValue(status, "rightShiftNanos")) + "|"
        + number(percent(decoder, run)) + "%|" + seconds(run - decoder) + "|"
        + longValue(status, "leftFullRecomputations") + "/"
        + longValue(status, "rightFullRecomputations") + "|\n";
  }

  private static String seconds(long nanos) {
    return String.format(Locale.ROOT, "%.6f", nanos / 1000000000.0);
  }

  private static String row(String name, Map<String, String> status, int size,
      double[] min, double[] max, P8MetricCalculator.Metrics metrics) {
    return "|" + name + "|" + status.get("fullEvaluations") + "|" + size + "|"
        + range(min[0], max[0]) + "|" + range(min[1], max[1]) + "|"
        + range(min[2], max[2]) + "|" + number(metrics.hv) + "|"
        + number(metrics.igd) + "|" + number(metrics.spacing) + "|\n";
  }

  private static String fatigueRow(String name, Map<String, String> status) {
    return "|" + name + "|" + value(status, "Fmax") + "|" + value(status, "Favg")
        + "|" + value(status, "fatigueExcess") + "|"
        + value(status, "workerFatigueVariance") + "|"
        + value(status, "highFatigueRatio") + "|"
        + value(status, "longestContinuousWork") + "|"
        + value(status, "totalNaturalRecovery") + "|"
        + value(status, "loadImbalance") + "|\n";
  }

  private static String metricRow(String name, List<double[]> front,
      P8MetricCalculator.Metrics metrics, double forward, double reverse) {
    double[] min = minima(front), max = maxima(front);
    return name + ',' + front.size() + ',' + number(metrics.hv) + ','
        + number(metrics.igd) + ',' + number(metrics.spacing) + ',' + number(forward)
        + ',' + number(reverse) + ',' + number(min[0]) + ',' + number(min[1]) + ','
        + number(min[2]) + ',' + number(max[0]) + ',' + number(max[1]) + ','
        + number(max[2]) + '\n';
  }

  private static String signal(double cFullBase, double cBaseFull,
      double[] fullMin, double[] baseMin) {
    int fullNoWorse = noWorse(fullMin, baseMin);
    int baseNoWorse = noWorse(baseMin, fullMin);
    if (cFullBase > cBaseFull + 1e-12 && fullNoWorse >= 2) return "PROMISING_SIGNAL";
    if (cBaseFull > cFullBase + 1e-12 && baseNoWorse >= 2) return "REGRESSION_SIGNAL";
    return "INCONCLUSIVE";
  }

  private static int noWorse(double[] left, double[] right) {
    int result = 0;
    for (int index = 0; index < 3; index++) {
      if (left[index] <= right[index] + 1e-12) result++;
    }
    return result;
  }

  private static double coverage(List<double[]> left, List<double[]> right) {
    int covered = 0;
    for (double[] target : right) {
      for (double[] candidate : left) {
        if (P8MetricCalculator.dominates(candidate, target) || equal(candidate, target)) {
          covered++;
          break;
        }
      }
    }
    return right.isEmpty() ? 0.0 : ((double) covered) / right.size();
  }

  private static boolean equal(double[] left, double[] right) {
    for (int index = 0; index < 3; index++) {
      if (Math.abs(left[index] - right[index]) > 1e-12) return false;
    }
    return true;
  }

  private static void requireFullGate(Path fullDirectory,
      ZhangBoP9FormalParameters parameters) throws IOException {
    Map<String, String> status = requireCompleted(fullDirectory);
    requireEqual("FULL seed", status.get("seed"), Long.toString(parameters.getSeed()));
    requireEqual("FULL population", status.get("population"),
        Integer.toString(parameters.getPopulation()));
    requireEqual("FULL budget", status.get("maxFEs"),
        Integer.toString(parameters.getMaxFEs()));
    if (longValue(status, "cfvfOffspring") <= 0 || longValue(status, "qgSelections") <= 0
        || longValue(status, "qpActions") <= 0 || longValue(status, "archiveInsertions") <= 0
        || longValue(status, "caTaTestCalls") <= 0
        || longValue(status, "caTaApplyCalls") <= 0) {
      throw new IllegalStateException("FULL status exists but mechanism hard gate is incomplete");
    }
  }

  private static Map<String, String> requireCompleted(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new IllegalStateException("Required completed run is missing: " + directory);
    }
    Map<String, String> status = readKeyValues(directory.resolve("status.properties"));
    requireEqual("run status", status.get("status"), "COMPLETED");
    String frontHash = sha256(directory.resolve("front.csv"));
    requireEqual("front hash", status.get("frontSha256"), frontHash);
    return status;
  }

  private static void writeFront(Path path, List<double[]> front) throws IOException {
    StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      csv.append(number(point[0])).append(',').append(number(point[1])).append(',')
          .append(number(point[2])).append('\n');
    }
    Files.write(path, csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static List<double[]> readFront(Path path) throws IOException {
    List<double[]> result = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line = reader.readLine();
      if (!"Cmax,TEC,TWC".equals(line)) throw new IOException("Invalid front header: " + path);
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] values = line.split(",", -1);
        if (values.length != 3) throw new IOException("Invalid front row: " + line);
        double[] point = {Double.parseDouble(values[0]), Double.parseDouble(values[1]),
            Double.parseDouble(values[2])};
        if (!finite(Arrays.asList(point))) throw new IOException("Non-finite front row");
        result.add(point);
      }
    }
    if (result.isEmpty()) throw new IOException("Empty front: " + path);
    return result;
  }

  private static String runRecordCsv(P8RunRecord record) {
    double[] min = minima(record.getFront()), max = maxima(record.getFront());
    return "algorithm,status,seed,fullEvaluations,wallClockMillis,cpuNanos,frontSize,"
        + "minCmax,minTEC,minTWC,maxCmax,maxTEC,maxTWC,cfvfRepairs,"
        + "localFullEvaluations,illegalSolutions,configurationSha256,initialPopulationSha256,reason\n"
        + csv(record.getLabel()) + ',' + record.getStatus() + ',' + record.getSeed() + ','
        + record.getFullEvaluations() + ',' + record.getWallClockMillis() + ','
        + record.getCpuNanos() + ',' + record.getFront().size() + ',' + number(min[0])
        + ',' + number(min[1]) + ',' + number(min[2]) + ',' + number(max[0]) + ','
        + number(max[1]) + ',' + number(max[2]) + ',' + record.getCfvfRepairs() + ','
        + record.getCaTaEvaluations() + ',' + record.getIllegalSolutions() + ','
        + record.getConfigurationSha256() + ',' + record.getInitialPopulationSha256()
        + ',' + csv(record.getReason()) + '\n';
  }

  private static String decoderTimingCsv(ZhangBoP9FormalRunResult result) {
    ZhangBoDecoderTimingSnapshot timing = result.decoderTiming;
    long algorithm = result.algorithmRunNanos;
    long decoder = timing.getDecoderTotalNanos();
    long left = timing.getLeftShiftNanos();
    long right = timing.getRightShiftNanos();
    long searchControl = algorithm - decoder;
    long excludingShift = algorithm - left - right;
    String header = "algorithm,seed,fullEvaluations,successfulDecoderCalls,"
        + "algorithmRunNanos,experimentWallNanos,algorithmCpuNanos,baseDecodeNanos,"
        + "leftShiftNanos,rightShiftNanos,decoderTotalNanos,"
        + "decoderFrameworkOverheadNanos,searchControlNanos,algorithmExcludingShiftNanos,"
        + "leftFullRecomputations,rightFullRecomputations,leftAccepted,rightAccepted,"
        + "baseDecodePercent,leftShiftPercent,rightShiftPercent,decoderTotalPercent,"
        + "searchControlPercent,avgBaseDecodeMicrosPerFE,avgLeftShiftMicrosPerFE,"
        + "avgRightShiftMicrosPerFE,avgLeftMicrosPerRecomputation,"
        + "avgRightMicrosPerRecomputation\n";
    return header + csv(result.record.getLabel()) + ',' + result.record.getSeed() + ','
        + result.record.getFullEvaluations() + ',' + timing.getSuccessfulDecoderCalls() + ','
        + algorithm + ',' + result.experimentWallNanos + ',' + result.record.getCpuNanos() + ','
        + timing.getBaseDecodeNanos() + ',' + left + ',' + right + ',' + decoder + ','
        + timing.getDecoderFrameworkOverheadNanos() + ',' + searchControl + ','
        + excludingShift + ',' + timing.getLeftFullRecomputations() + ','
        + timing.getRightFullRecomputations() + ',' + timing.getLeftAccepted() + ','
        + timing.getRightAccepted() + ',' + number(percent(timing.getBaseDecodeNanos(), algorithm))
        + ',' + number(percent(left, algorithm)) + ',' + number(percent(right, algorithm))
        + ',' + number(percent(decoder, algorithm)) + ','
        + number(percent(searchControl, algorithm)) + ','
        + number(averageMicros(timing.getBaseDecodeNanos(), timing.getSuccessfulDecoderCalls()))
        + ',' + number(averageMicros(left, timing.getSuccessfulDecoderCalls()))
        + ',' + number(averageMicros(right, timing.getSuccessfulDecoderCalls()))
        + ',' + number(averageMicros(left, timing.getLeftFullRecomputations()))
        + ',' + number(averageMicros(right, timing.getRightFullRecomputations())) + '\n';
  }

  private static double percent(long part, long total) {
    return total <= 0L ? 0.0 : 100.0 * part / total;
  }

  private static double averageMicros(long nanos, long count) {
    return count <= 0L ? 0.0 : ((double) nanos) / count / 1000.0;
  }

  private static void writeKeyValues(Path path, Map<String, String> values) throws IOException {
    StringBuilder text = new StringBuilder();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      text.append(entry.getKey()).append('=').append(escape(entry.getValue())).append('\n');
    }
    Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static Map<String, String> readKeyValues(Path path) throws IOException {
    Map<String, String> result = new LinkedHashMap<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      if (line.isEmpty()) continue;
      int equals = line.indexOf('=');
      if (equals <= 0) throw new IOException("Invalid status line: " + line);
      String key = line.substring(0, equals);
      if (result.containsKey(key)) throw new IOException("Duplicate status key: " + key);
      result.put(key, unescape(line.substring(equals + 1)));
    }
    return result;
  }

  private static String escape(String value) {
    return String.valueOf(value).replace("\\", "\\\\").replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  private static String unescape(String value) {
    StringBuilder result = new StringBuilder();
    boolean escaped = false;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (!escaped && current == '\\') { escaped = true; continue; }
      if (escaped) {
        if (current == 'n') result.append('\n');
        else if (current == 'r') result.append('\r');
        else result.append(current);
        escaped = false;
      } else result.append(current);
    }
    if (escaped) result.append('\\');
    return result.toString();
  }

  private static void appendConsole(Path directory, String line) throws IOException {
    Files.write(directory.resolve("console.log"),
        (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
  }

  private static void writeEvidenceHashes(Path directory) throws IOException {
    List<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path path : stream) {
        if (Files.isRegularFile(path)
            && !"evidence-sha256.tsv".equals(path.getFileName().toString())) paths.add(path);
      }
    }
    paths.sort((left, right) -> left.getFileName().toString()
        .compareTo(right.getFileName().toString()));
    StringBuilder text = new StringBuilder("sha256\tbytes\tfile\n");
    for (Path path : paths) {
      text.append(sha256(path)).append('\t').append(Files.size(path)).append('\t')
          .append(path.getFileName()).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"),
        text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void moveDirectory(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target);
    }
  }

  private static double[] minima(List<double[]> front) {
    double[] result = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY};
    for (double[] point : front) for (int index = 0; index < 3; index++) {
      result[index] = Math.min(result[index], point[index]);
    }
    return result;
  }

  private static double[] maxima(List<double[]> front) {
    double[] result = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY};
    for (double[] point : front) for (int index = 0; index < 3; index++) {
      result[index] = Math.max(result[index], point[index]);
    }
    return result;
  }

  private static boolean finite(List<double[]> front) {
    for (double[] point : front) for (double value : point) if (!Double.isFinite(value)) return false;
    return true;
  }

  private static void requireProject(Path root) {
    if (root == null || !Files.isRegularFile(root.resolve("EADHFSP").resolve(INSTANCE_FILE))
        || !Files.isRegularFile(root.resolve("instance-extensions/v1").resolve(EXTENSION_FILE))
        || !Files.isRegularFile(root.resolve("fatigue-parameters/v1").resolve(FATIGUE_FILE))) {
      throw new IllegalArgumentException("Invalid P9 project root: " + root);
    }
  }

  private static String alias(Phase phase) {
    if (phase == Phase.FULL) return "ZHANGBO-FULL";
    if (phase == Phase.HMOPSO_QGS_F) return "HMOPSO-QGS-F";
    return "REPORT";
  }

  private static long longValue(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null) throw new IllegalStateException("Missing status key: " + key);
    return Long.parseLong(value);
  }

  private static String value(Map<String, String> values, String key) {
    String value = values.get(key);
    return value == null ? "NaN" : value;
  }

  private static void requireEqual(String name, String actual, String expected) {
    if (actual == null || !actual.equals(expected)) {
      throw new IllegalStateException(name + " mismatch: " + actual + " != " + expected);
    }
  }

  private static String range(double min, double max) {
    return number(min) + "–" + number(max);
  }

  private static String number(double value) {
    return Double.isFinite(value) ? String.format(Locale.ROOT, "%.12g", value) : "NaN";
  }

  private static String csv(String value) {
    return '"' + String.valueOf(value).replace("\"", "\"\"") + '"';
  }

  private static String sha256(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[65536];
      try (java.io.InputStream input = Files.newInputStream(path)) {
        int read;
        while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
      }
      StringBuilder result = new StringBuilder();
      for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static String sha256Text(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
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
        else if ("--project-root".equals(args[index])) {
          project = Paths.get(args[index + 1]).toAbsolutePath().normalize();
        } else if ("--output".equals(args[index])) {
          output = Paths.get(args[index + 1]).toAbsolutePath().normalize();
        } else throw usage();
      }
      if (phase == null || project == null || output == null) throw usage();
      return new Arguments(phase, project, output);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --phase FULL|HMOPSO_QGS_F|REPORT "
          + "--project-root <path> --output <path>");
    }
  }
}
