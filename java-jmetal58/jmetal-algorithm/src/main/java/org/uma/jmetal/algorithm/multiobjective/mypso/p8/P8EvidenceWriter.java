package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Writes deterministic UTF-8 P8 CSV evidence and pooled engineering reference fronts. */
public final class P8EvidenceWriter {
  private P8EvidenceWriter() { }

  public static void write(Path directory, List<P8ExperimentSpec> specs,
      List<P8RunRecord> records) throws IOException {
    validateP8V3(specs, records);
    Files.createDirectories(directory);
    Files.createDirectories(directory.resolve("runs"));
    Files.createDirectories(directory.resolve("reference-fronts"));
    writeRegistry(directory.resolve("matrix-registry.csv"), specs);
    writeMechanismVectors(directory.resolve("mechanism-vectors.csv"), specs);
    writePairings(directory.resolve("ablation-pairings.csv"), specs);
    writeControls(directory.resolve("control-points.csv"), specs);
    writeUnsupported(directory.resolve("unsupported-combinations.csv"), specs);
    writeRuns(directory.resolve("run-records.csv"), records);
    Map<String, List<double[]>> references = pooledReferences(records);
    for (Map.Entry<String, List<double[]>> entry : references.entrySet()) {
      writeFront(directory.resolve("reference-fronts").resolve(safe(entry.getKey()) + ".csv"),
          entry.getValue());
    }
    for (P8RunRecord record : records) {
      Path runDirectory = directory.resolve("runs").resolve(safe(record.getInstance()));
      Files.createDirectories(runDirectory);
      String stem = safe(record.getMatrix() + "-" + record.getLabel() + "-" + record.getSeed());
      writeFront(runDirectory.resolve(stem + "-front.csv"), record.getFront());
      write(runDirectory.resolve(stem + "-configuration.txt"), record.getConfigurationText());
    }
    writeMetrics(directory.resolve("metrics.csv"), records, references);
    writeReport(directory.resolve("P8_REPORT.md"), specs, records, references);
    writeSha256Manifest(directory);
  }

  private static void writeMechanismVectors(Path path, List<P8ExperimentSpec> specs)
      throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("schemaVersion,semanticTag,matrix,label,profileSha256,canonicalBaseline,decoder,randomness,resourceFlight,resourceInertia,legalExploration,qg,evaluatedPddr,lineageArchive,personalLeader,blockFrozenDualQ,vns");
    for (P8ExperimentSpec spec : specs) {
      P8AblationProfile p = spec.getAblationProfile();
      lines.add(csv(P8AblationProfile.VERSION) + ',' + csv(p.getSemanticTag()) + ','
          + csv(spec.getMatrix()) + ',' + csv(spec.getLabel()) + ','
          + csv(p.mechanismVectorHash()) + ',' + p.isCanonicalBaseline() + ','
          + csv(p.getDecoderMode()) + ',' + csv(p.getRandomnessMode()) + ','
          + csv(p.getResourceFlightMode()) + ',' + p.hasResourceInertia() + ','
          + p.hasLegalExploration() + ',' + p.isQgEnabled() + ','
          + p.isEvaluatedPddrEnabled() + ',' + p.isLineageArchiveEnabled() + ','
          + csv(p.getPersonalLeaderMode()) + ',' + p.isBlockFrozenDualQ() + ','
          + csv(p.getVnsMode()));
    }
    write(path, join(lines));
  }

  private static void writePairings(Path path, List<P8ExperimentSpec> specs)
      throws IOException {
    String[][] pairs = {
        {"B0", "B1", "fatigue decoder"},
        {"FM0", "FM1", "fatigue accumulation and duration feedback"},
        {"FM1", "FM2", "natural recovery"},
        {"FM2", "FM3", "fatigue-aware worker selection"},
        {"B1", "B2", "CFVF bundle"},
        {"B2", "B3", "lineage archive"},
        {"B3", "B4", "Q-pbest"},
        {"B4", "B5", "block-frozen dual Q"},
        {"B5", "B6", "fixed O1-O13"},
        {"B6", "B7", "contextual Test-and-Apply without FAT"},
        {"B7", "FULL", "FAT bottleneck context"},
        {"FV0", "FV1", "FA leader update"},
        {"FV1", "FV2", "independent MA/WA flight"},
        {"FV2", "FV3", "coupled FMW/MW/M/W actions"},
        {"FV3", "FV4", "remove resource inertia"},
        {"FV3", "FV5", "remove legal exploration"},
        {"QP0", "QP1", "lineage archive directional pbest"},
        {"QP1", "QP2", "four-policy random pbest"},
        {"QP2", "QP3", "Q-pbest"},
        {"QP3", "QP4", "synchronous Q-gbest"},
        {"QP4", "QP5", "block freezing"},
        {"QP5", "QP6", "complete CFVF"},
        {"V0", "V1", "O10-O13"},
        {"V1", "V2", "Need-aware factory selection"},
        {"V2", "V3", "context-free Test-and-Apply"},
        {"V3", "V4", "context"},
        {"V4", "V5", "cost credit"},
        {"V5", "V-Full", "FAT context"}
    };
    Map<String, P8ExperimentSpec> byLabel = new LinkedHashMap<>();
    for (P8ExperimentSpec spec : specs) byLabel.put(spec.getLabel(), spec);
    List<String> lines = new ArrayList<>();
    lines.add("fromLabel,toLabel,intendedChange,actualDifferenceKeys");
    for (String[] pair : pairs) {
      P8ExperimentSpec from = byLabel.get(pair[0]);
      P8ExperimentSpec to = byLabel.get(pair[1]);
      if (from == null || to == null) continue;
      lines.add(csv(pair[0]) + ',' + csv(pair[1]) + ',' + csv(pair[2]) + ','
          + csv(from.getAblationProfile().differenceKeys(to.getAblationProfile())));
    }
    write(path, join(lines));
  }

  private static void writeControls(Path path, List<P8ExperimentSpec> specs) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("label,mechanism,profileSha256");
    for (P8ExperimentSpec spec : specs) {
      if (spec.getMatrix() != P8MatrixKind.CONTROL) continue;
      lines.add(csv(spec.getLabel()) + ',' + csv(spec.getMechanism()) + ','
          + csv(sha256(spec.getAblationProfile().canonicalText()
          .getBytes(StandardCharsets.UTF_8))));
    }
    write(path, join(lines));
  }

  private static void writeRegistry(Path path, List<P8ExperimentSpec> specs) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("schemaVersion,semanticTag,matrix,label,mechanism,configurationKey,registryStatus,reason,population,maxFEs,physicalSubswarmSizes,frontEligible,mechanismVectorHash");
    for (P8ExperimentSpec spec : specs) {
      lines.add(csv(P8AblationProfile.VERSION) + ',' + csv(spec.getSemanticTag()) + ','
          + csv(spec.getMatrix()) + ',' + csv(spec.getLabel()) + ',' + csv(spec.getMechanism())
          + ',' + csv(spec.getConfigurationKey()) + ',' + csv(spec.getStatus()) + ','
          + csv(spec.getReason()) + ',' + spec.getPopulationSize() + ',' + spec.getMaxFEs()
          + ',' + csv(java.util.Arrays.toString(spec.getPhysicalSubswarmSizes())) + ','
          + spec.isFrontEligible() + ',' + csv(spec.getMechanismVectorHash()));
    }
    write(path, join(lines));
  }

  private static void writeUnsupported(Path path, List<P8ExperimentSpec> specs) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("matrix,label,mechanism,reason");
    for (P8ExperimentSpec spec : specs) if (spec.getStatus() == P8RunStatus.NOT_EXPOSED) {
      lines.add(csv(spec.getMatrix()) + ',' + csv(spec.getLabel()) + ','
          + csv(spec.getMechanism()) + ',' + csv(spec.getReason()));
    }
    write(path, join(lines));
  }

  private static void writeRuns(Path path, List<P8RunRecord> records) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("runId,sourceRunId,reused,instance,instanceSha256,matrix,label,seed,status,reason,configurationSha256,mechanismVectorHash,initialPopulationSha256,fullEvaluations,wallClockMillis,cpuNanos,cfvfRepairs,caTaEvaluations,illegalSolutions,Fmax,Favg,FE,VarFw,highFatigueRatio,longestContinuousWork,totalNaturalRecovery,loadImbalance,frontSize");
    for (P8RunRecord record : sorted(records)) {
      lines.add(csv(record.getRunId()) + ',' + csv(record.getSourceRunId()) + ','
          + record.isReused() + ',' + csv(record.getInstance()) + ','
          + csv(record.getInstanceSha256()) + ','
          + csv(record.getMatrix()) + ',' + csv(record.getLabel()) + ',' + record.getSeed()
          + ',' + record.getStatus() + ',' + csv(record.getReason()) + ','
          + csv(record.getConfigurationSha256()) + ',' + csv(record.getMechanismVectorHash()) + ','
          + csv(record.getInitialPopulationSha256())
          + ',' + record.getFullEvaluations() + ',' + record.getWallClockMillis() + ','
          + record.getCpuNanos() + ',' + record.getCfvfRepairs() + ','
          + record.getCaTaEvaluations() + ',' + record.getIllegalSolutions() + ','
          + number(record.getFmax()) + ',' + number(record.getFavg()) + ','
          + number(record.getFatigueExcess()) + ',' + number(record.getWorkerFatigueVariance())
          + ',' + number(record.getHighFatigueRatio()) + ','
          + number(record.getLongestContinuousWork()) + ','
          + number(record.getTotalNaturalRecovery()) + ','
          + number(record.getLoadImbalance()) + ',' + record.getFront().size());
    }
    write(path, join(lines));
  }

  private static void writeReport(Path path, List<P8ExperimentSpec> specs,
      List<P8RunRecord> records, Map<String, List<double[]>> references) throws IOException {
    int official = 0;
    int controls = 0;
    for (P8ExperimentSpec spec : specs) {
      if (spec.getMatrix() == P8MatrixKind.CONTROL) controls++; else official++;
    }
    int completed = 0;
    int failed = 0;
    int reused = 0;
    long maximumFe = 0L;
    long minimumFe = Long.MAX_VALUE;
    long illegal = 0L;
    long repairs = 0L;
    long localEvaluations = 0L;
    for (P8RunRecord record : records) {
      if (record.getStatus() == P8RunStatus.COMPLETED) completed++;
      if (record.getStatus() == P8RunStatus.FAILED) failed++;
      if (record.isReused()) reused++;
      maximumFe = Math.max(maximumFe, record.getFullEvaluations());
      minimumFe = Math.min(minimumFe, record.getFullEvaluations());
      illegal += record.getIllegalSolutions();
      repairs += record.getCfvfRepairs();
      localEvaluations += record.getCaTaEvaluations();
    }
    boolean accepted = specs.size() == 34 && records.size() == 204
        && completed == 204 && failed == 0 && maximumFe <= 2000L
        && illegal == 0L && repairs == 0L;
    StringBuilder text = new StringBuilder();
    text.append("# P8 集成、消融与工程验收报告（校正版）\n\n")
        .append("- 语义版本：`").append(P8AblationProfile.VERSION).append("`\n")
        .append("- 正式标签：").append(official).append("；旧控制点：已归档，不计入\n")
        .append("- 标签级运行记录：").append(records.size()).append("；完成：")
        .append(completed).append("；失败：").append(failed).append("\n")
        .append("- 精确配置复用记录：").append(reused).append("\n")
        .append("- 单次最大完整评价数：").append(maximumFe)
        .append("；最小完整评价数：").append(minimumFe).append("\n")
        .append("- 非法解总数：").append(illegal).append("；异常修复总数：")
        .append(repairs).append("；局部候选完整评价记录：")
        .append(localEvaluations).append("\n")
        .append("- 工程参考前沿实例数：").append(references.size()).append("\n\n")
        .append("## 五组正式消融\n\n")
        .append("1. `FV0–FV-Full`（7项）：比较规范资源更新、FA引导、独立资源更新、耦合FMW动作及惯性/探索删项。\n")
        .append("2. `FM0–FM3`（4项）：在同一deterministic_canonical骨架上依次比较规范无疲劳、疲劳累积、自然恢复和疲劳感知选工。\n")
        .append("3. `QP0–QP6`（7项）：比较单pbest、容量6档案、随机四策略、Q-pbest、同步Qg、分块冻结和完整CFVF。\n")
        .append("4. `V0–V-Full`（7项）：比较O1–O9、O10–O13、Need工厂选择、Test-and-Apply、上下文、代价信用和FAT上下文。\n")
        .append("5. `B0–FULL`（9项）：从deterministic_canonical规范基线逐层叠加疲劳、CFVF、谱系档案、Q-pbest、双Q、邻域和CA-TA。\n\n")
        .append("`A0_AUTHOR_DIAGNOSTIC`仅用于作者缺陷诊断，不进入矩阵或参考前沿；旧`B0R/B0C/B1Q/B2P`属于P8-v2历史证据。\n\n")
        .append("## 工程验收\n\n")
        .append("- `integration_engineering_validated=").append(accepted).append("`\n")
        .append("- `ablation_engineering_validated=").append(accepted).append("`\n")
        .append("- `sampled_reproduction_accepted=false`\n")
        .append("- `full_reproduction_accepted=false`\n\n")
        .append("## 结论边界\n\n")
        .append("本报告只证明校正后的开关、桥接、预算和小规模消融矩阵工程闭合。")
        .append("参考前沿由本轮已完成运行合并后冻结，不是理论真值。")
        .append("B0/FM0使用`deterministic_canonical`、实例SUT、显式第一阶段MA/WA、原Qg/PDDR/O1-O9；作者未控路径仅作A0诊断。")
        .append("本轮未执行500000 FE、显著性检验或P9正式实验，")
        .append("因此不得称为论文完整复现。\n");
    write(path, text.toString());
  }

  private static void writeMetrics(Path path, List<P8RunRecord> records,
      Map<String, List<double[]>> references) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("instance,matrix,label,seed,status,HV,IGD,Spacing,C_A_R,C_R_A,nondominatedCount,minCmax,minTEC,minTWC");
    for (P8RunRecord record : sorted(records)) {
      if (record.getStatus() != P8RunStatus.COMPLETED || record.getFront().isEmpty()) {
        lines.add(csv(record.getInstance()) + ',' + csv(record.getMatrix()) + ','
            + csv(record.getLabel()) + ',' + record.getSeed() + ',' + record.getStatus()
            + ",NA,NA,NA,NA,NA,0,NA,NA,NA");
        continue;
      }
      P8MetricCalculator.Metrics metrics = P8MetricCalculator.calculate(
          record.getFront(), references.get(record.getInstance()));
      double[] minima = minima(record.getFront());
      lines.add(csv(record.getInstance()) + ',' + csv(record.getMatrix()) + ','
          + csv(record.getLabel()) + ',' + record.getSeed() + ',' + record.getStatus()
          + ',' + number(metrics.hv) + ',' + number(metrics.igd) + ','
          + number(metrics.spacing) + ',' + number(metrics.cForward) + ','
          + number(metrics.cReverse) + ',' + metrics.nondominatedCount + ','
          + number(minima[0]) + ',' + number(minima[1]) + ',' + number(minima[2]));
    }
    write(path, join(lines));
  }

  private static Map<String, List<double[]>> pooledReferences(List<P8RunRecord> records) {
    Map<String, List<double[]>> pooled = new LinkedHashMap<>();
    for (P8RunRecord record : records) {
      if (record.getStatus() != P8RunStatus.COMPLETED
          || "DIAGNOSTIC".equals(record.getMatrix())
          || record.getLabel().startsWith("A0")) continue;
      if (!pooled.containsKey(record.getInstance())) pooled.put(record.getInstance(), new ArrayList<double[]>());
      pooled.get(record.getInstance()).addAll(record.getFront());
    }
    for (Map.Entry<String, List<double[]>> entry : pooled.entrySet()) {
      entry.setValue(P8MetricCalculator.nondominated(entry.getValue()));
    }
    return pooled;
  }

  private static void writeFront(Path path, List<double[]> front) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("Cmax,TEC,TWC");
    for (double[] point : front) {
      lines.add(number(point[0]) + ',' + number(point[1]) + ',' + number(point[2]));
    }
    write(path, join(lines));
  }

  private static List<P8RunRecord> sorted(List<P8RunRecord> records) {
    List<P8RunRecord> sorted = new ArrayList<>(records);
    Collections.sort(sorted, new Comparator<P8RunRecord>() {
      @Override public int compare(P8RunRecord a, P8RunRecord b) {
        int value = a.getInstance().compareTo(b.getInstance());
        if (value != 0) return value;
        value = a.getMatrix().compareTo(b.getMatrix());
        if (value != 0) return value;
        value = a.getLabel().compareTo(b.getLabel());
        if (value != 0) return value;
        return Long.compare(a.getSeed(), b.getSeed());
      }
    });
    return sorted;
  }

  private static double[] minima(List<double[]> front) {
    double[] result = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    for (double[] point : front) for (int i = 0; i < 3; i++) result[i] = Math.min(result[i], point[i]);
    return result;
  }

  private static String safe(String value) { return value.replaceAll("[^A-Za-z0-9._-]", "_"); }

  private static String csv(Object value) {
    String text = String.valueOf(value).replace("\"", "\"\"");
    return '"' + text + '"';
  }

  private static String number(double value) {
    return Double.isNaN(value) || Double.isInfinite(value)
        ? "NA" : String.format(Locale.ROOT, "%.17g", value);
  }

  private static String join(List<String> lines) {
    StringBuilder result = new StringBuilder();
    for (String line : lines) result.append(line).append('\n');
    return result.toString();
  }

  private static void validateP8V3(List<P8ExperimentSpec> specs,
      List<P8RunRecord> records) {
    if (specs == null || records == null) throw new IllegalArgumentException("P8 evidence inputs null");
    P8ExperimentRegistry.assertCurrentMatrix(specs);
    if (records.size() != 204) {
      throw new IllegalArgumentException("Current shift evidence requires exactly 204 label records");
    }
    Map<String, P8ExperimentSpec> byLabel = new HashMap<>();
    for (P8ExperimentSpec spec : specs) byLabel.put(spec.getLabel(), spec);
    Set<String> runIds = new HashSet<>();
    Map<String, Integer> labelCounts = new HashMap<>();
    Map<String, P8RunRecord> recordsById = new HashMap<>();
    for (P8RunRecord record : records) {
      if (record == null || record.getLabel().startsWith("A0")
          || "DIAGNOSTIC".equals(record.getMatrix())
          || record.getStatus() == P8RunStatus.DIAGNOSTIC_ONLY) {
        throw new IllegalArgumentException("A0 diagnostic cannot enter current shift evidence");
      }
      if (!runIds.add(record.getRunId())) {
        throw new IllegalArgumentException("Duplicate current-shift runId: " + record.getRunId());
      }
      recordsById.put(record.getRunId(), record);
      P8ExperimentSpec spec = byLabel.get(record.getLabel());
      if (spec == null) throw new IllegalArgumentException("Unknown current-shift label: " + record.getLabel());
      Integer previous = labelCounts.get(record.getLabel());
      labelCounts.put(record.getLabel(), previous == null ? 1 : previous + 1);
      if (record.getStatus() == P8RunStatus.COMPLETED
          && !spec.getMechanismVectorHash().equals(record.getMechanismVectorHash())) {
        throw new IllegalArgumentException("Run mechanism hash does not match registry: "
          + record.getRunId());
      }
    }
    for (P8ExperimentSpec spec : specs) {
      Integer count = labelCounts.get(spec.getLabel());
      if (count == null || count != 6) {
        throw new IllegalArgumentException("Current shift label must have 2 instances x 3 seeds: "
            + spec.getLabel() + " count=" + count);
      }
    }
    for (P8RunRecord record : records) {
      if (!record.isReused()) continue;
      P8RunRecord source = recordsById.get(record.getSourceRunId());
      if (source == null || source.getStatus() != P8RunStatus.COMPLETED
          || !source.getMechanismVectorHash().equals(record.getMechanismVectorHash())) {
        throw new IllegalArgumentException("Reused P8 record lost its exact source hash: "
            + record.getRunId());
      }
    }
  }

  private static void write(Path path, String text) throws IOException {
    Files.write(path, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
  }

  private static void writeSha256Manifest(Path directory) throws IOException {
    Path manifest = directory.resolve("evidence-sha256.tsv");
    List<Path> files = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(directory)) {
      stream.filter(Files::isRegularFile)
          .filter(path -> !path.equals(manifest))
          // The runner's redirected console log is still open while this manifest is written.
          .filter(path -> !path.getFileName().toString().equals("full-matrix.log"))
          .forEach(files::add);
    }
    Collections.sort(files);
    List<String> lines = new ArrayList<>();
    lines.add("sha256\trelativePath");
    for (Path file : files) {
      lines.add(sha256(Files.readAllBytes(file)) + '\t'
          + directory.relativize(file).toString().replace('\\', '/'));
    }
    write(manifest, join(lines));
  }

  private static String sha256(byte[] input) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
      StringBuilder text = new StringBuilder();
      for (byte value : digest) text.append(String.format(Locale.ROOT, "%02x", value & 0xff));
      return text.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
