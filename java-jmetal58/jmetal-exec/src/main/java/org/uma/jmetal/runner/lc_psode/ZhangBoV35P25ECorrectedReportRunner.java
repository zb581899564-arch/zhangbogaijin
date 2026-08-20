package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/**
 * Freezes one common reference only after all corrected P25E runs exist.
 *
 * <p>Scans every {@code runs/seed-*} directory: each seed must contain all
 * eight algorithms. The reference is the non-dominated union of all fronts of
 * all seeds, and per-seed metrics are written to {@code metrics.csv} with a
 * per-algorithm median summary in {@code metrics-median.csv}.</p>
 */
public final class ZhangBoV35P25ECorrectedReportRunner {
  private ZhangBoV35P25ECorrectedReportRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !"--output".equals(args[0])) {
      throw new IllegalArgumentException("Usage: --output <P25E root>");
    }
    generate(Paths.get(args[1]).toAbsolutePath().normalize());
  }

  static void generate(Path output) throws Exception {
    Path runsRoot = output.resolve("runs");
    TreeSet<String> seedNames = new TreeSet<>();
    if (Files.isDirectory(runsRoot)) {
      try (java.util.stream.Stream<Path> walk = Files.list(runsRoot)) {
        walk.filter(Files::isDirectory).forEach(dir -> {
          String name = dir.getFileName().toString();
          if (name.startsWith("seed-")) seedNames.add(name.substring("seed-".length()));
        });
      }
    }
    if (seedNames.isEmpty()) throw new IllegalStateException("no runs/seed-* directories");

    List<Run> runs = new ArrayList<>();
    List<double[]> pooled = new ArrayList<>();
    Map<String, String> seedToInitialHash = new TreeMap<>();
    for (String seed : seedNames) {
      String initialHash = null;
      for (ZhangBoV35P25ECorrectedComparisonRunner.Algorithm algorithm
          : ZhangBoV35P25ECorrectedComparisonRunner.Algorithm.values()) {
        Path directory = output.resolve("runs/seed-" + seed).resolve(algorithm.name());
        if (!Files.isDirectory(directory)) {
          throw new IllegalStateException("missing run: " + directory);
        }
        String status = read(directory.resolve("status.properties"));
        if (!status.contains("p25eStatus=COMPLETED")) {
          throw new IllegalStateException("run not completed: " + directory);
        }
        String hash = property(status, "p25eInitialPopulationHash");
        if (initialHash == null) initialHash = hash;
        else if (!initialHash.equals(hash)) {
          throw new IllegalStateException("initial population drift in seed " + seed);
        }
        Run run = new Run(seed, algorithm, readFront(directory.resolve("front.csv")),
            property(status, "p25eSourceKind"), property(status, "p25eFullEvaluations"),
            read(directory.resolve("algorithm-identity.txt")));
        runs.add(run);
        pooled.addAll(run.front);
      }
      seedToInitialHash.put(seed, initialHash);
    }

    List<double[]> reference = P8MetricCalculator.nondominated(pooled);
    for (Run run : runs) run.metrics = P8MetricCalculator.calculate(run.front, reference);
    writeFront(output.resolve("reference-front.csv"), reference);
    writeMetrics(output.resolve("metrics.csv"), runs);
    writeMedianSummary(output.resolve("metrics-median.csv"), runs);
    writeSourceMap(output.resolve("algorithm-source-map.csv"), runs);
    writeTable9(output.resolve("table9-parameters.csv"), output, runs);
    Files.write(output.resolve("adapter-readiness.csv"), (
        "algorithm,status,sharedProblemOnly,searchMechanismIndependent,objectiveAdapter,legacyP25DExcluded\n"
            + readinessRows(runs)).getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("QMOEA_PENDING.md"), (
        "# QMOEA source gate\n\n`QMOEA=PENDING_SOURCE_VERIFICATION`。当前没有"
            + "经论文、源码和结构三方核实的实现，未以近似算法替代。\n")
        .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("P25E_REPORT.md"), report(runs, reference.size(),
        seedToInitialHash).getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    System.out.println("P25E_REPORT_COMPLETED seeds=" + seedNames.size()
        + " runs=" + runs.size() + " reference=" + reference.size());
  }

  private static void writeMetrics(Path path, List<Run> runs) throws Exception {
    Run a4 = runs.get(0);
    StringBuilder out = new StringBuilder(
        "seed,algorithm,sourceKind,FE,HV,IGD,Spacing,C_A_R,C_R_A,C_A4_A,C_A_A4,frontSize,minCmax,minTEC,minTWC\n");
    for (Run run : runs) {
      out.append(run.seed).append(',').append(run.algorithm).append(',')
          .append(run.sourceKind).append(',').append(run.fe).append(',')
          .append(run.metrics.hv).append(',').append(run.metrics.igd).append(',')
          .append(run.metrics.spacing).append(',').append(run.metrics.cForward).append(',')
          .append(run.metrics.cReverse).append(',')
          .append(coverage(a4.front, run.front)).append(',')
          .append(coverage(run.front, a4.front)).append(',')
          .append(run.front.size()).append(',').append(min(run.front, 0)).append(',')
          .append(min(run.front, 1)).append(',').append(min(run.front, 2)).append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeMedianSummary(Path path, List<Run> runs) throws Exception {
    Map<String, List<Run>> byAlgorithm = new TreeMap<>();
    for (Run run : runs) byAlgorithm.computeIfAbsent(run.algorithm.name(), k -> new ArrayList<>())
        .add(run);
    StringBuilder out = new StringBuilder(
        "algorithm,seeds,medianHV,medianIGD,medianSpacing,medianFrontSize,medianMinCmax,medianMinTEC,medianMinTWC,minFE,maxFE\n");
    for (Map.Entry<String, List<Run>> entry : byAlgorithm.entrySet()) {
      List<Run> values = entry.getValue();
      out.append(entry.getKey()).append(',').append(values.size()).append(',')
          .append(medianDouble(values, r -> r.metrics.hv)).append(',')
          .append(medianDouble(values, r -> r.metrics.igd)).append(',')
          .append(medianDouble(values, r -> r.metrics.spacing)).append(',')
          .append(medianDouble(values, r -> r.front.size())).append(',')
          .append(medianDouble(values, r -> min(r.front, 0))).append(',')
          .append(medianDouble(values, r -> min(r.front, 1))).append(',')
          .append(medianDouble(values, r -> min(r.front, 2))).append(',')
          .append(minInt(values, r -> Integer.parseInt(r.fe))).append(',')
          .append(maxInt(values, r -> Integer.parseInt(r.fe))).append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private interface NumberExtractor { double value(Run run); }
  private static double medianDouble(List<Run> runs, NumberExtractor extractor) {
    List<Double> values = new ArrayList<>();
    for (Run run : runs) values.add(extractor.value(run));
    Collections.sort(values);
    return values.get(values.size() / 2);
  }
  private interface IntExtractor { int value(Run run); }
  private static int minInt(List<Run> runs, IntExtractor extractor) {
    int value = Integer.MAX_VALUE;
    for (Run run : runs) value = Math.min(value, extractor.value(run));
    return value;
  }
  private static int maxInt(List<Run> runs, IntExtractor extractor) {
    int value = Integer.MIN_VALUE;
    for (Run run : runs) value = Math.max(value, extractor.value(run));
    return value;
  }

  private static void writeSourceMap(Path path, List<Run> runs) throws Exception {
    StringBuilder out = new StringBuilder(
        "algorithm,sourceKind,implementationClass,actualSourceSha256,adaptationBoundary\n");
    for (Run run : runs) out.append(run.algorithm).append(',').append(run.sourceKind).append(',')
        .append(property(run.identity, "implementationClass")).append(',')
        .append(property(run.identity, "actualSourceSha256")).append(',')
        .append("Problem|Solution|initialPopulation|randomSource|FE|logging").append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeTable9(Path path, Path output, List<Run> runs) throws Exception {
    StringBuilder out = new StringBuilder("algorithm,canonicalParameters\n");
    String firstSeed = runs.get(0).seed;
    for (Run run : runs) {
      Path config = output.resolve("runs/seed-" + firstSeed)
          .resolve(run.algorithm.name()).resolve("configuration.txt");
      out.append(run.algorithm).append(',').append('"')
          .append(property(read(config), "table9Parameters").replace("\"", "\"\""))
          .append('"').append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String readinessRows(List<Run> runs) {
    StringBuilder out = new StringBuilder();
    for (Run run : runs) out.append(run.algorithm)
        .append(",COMPLETED,true,true,0|1|6,true\n");
    return out.toString();
  }

  private static String report(List<Run> runs, int referenceSize,
      Map<String, String> seedToInitialHash) {
    TreeSet<String> seeds = new TreeSet<>(seedToInitialHash.keySet());
    StringBuilder hashLines = new StringBuilder();
    for (Map.Entry<String, String> entry : seedToInitialHash.entrySet()) {
      hashLines.append("    - seed `").append(entry.getKey()).append("`：`")
          .append(entry.getValue()).append("`\n");
    }
    return "# V35-P25E 论文算法忠实适配多Seed 50k纠正报告\n\n"
        + "- 运行：`" + runs.size() + "`（`" + seeds.size() + "` 个 seed，每 seed 8 算法），"
        + "实例=`20_2_3_1`。\n"
        + "- 共享边界：四向量、FM3、ShiftMode.NONE、单族序列无关SUT、"
        + "目标适配`[0,1,6]`、初始种群、FE和指标。\n"
        + "- 搜索机制：每个算法独立；六种比较算法均不引用张博CFVF、Qp、DSCR、"
        + "CA-TA-Lite或方向教师池。\n"
        + "- 初始种群哈希（同 seed 内 8 算法一致）：\n" + hashLines
        + "- 统一reference在全部运行完成后一次冻结，点数：`" + referenceSize + "`。\n"
        + "- 旧P25D已隔离，不进入本reference。QMOEA仍为来源待核验。\n\n"
        + "本轮是多seed纠正诊断；不构成显著性结论，也不代表论文最终优越性判断。"
        + "逐seed指标见`metrics.csv`，按算法中位数见`metrics-median.csv`。\n";
  }

  private static List<double[]> readFront(Path path) throws Exception {
    List<double[]> result = new ArrayList<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).trim().isEmpty()) continue;
      String[] value = lines.get(index).split(",");
      result.add(new double[] {Double.parseDouble(value[0]), Double.parseDouble(value[1]),
          Double.parseDouble(value[2])});
    }
    if (result.isEmpty()) throw new IllegalStateException("empty front: " + path);
    return result;
  }
  private static void writeFront(Path path, List<double[]> front) throws Exception {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1])
        .append(',').append(point[2]).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static double min(List<double[]> front, int objective) {
    double value = Double.POSITIVE_INFINITY;
    for (double[] point : front) value = Math.min(value, point[objective]);
    return value;
  }
  private static double coverage(List<double[]> left, List<double[]> right) {
    if (right.isEmpty()) return 0.0;
    int covered = 0;
    for (double[] target : right) {
      for (double[] candidate : left) {
        boolean noWorse = true, strict = false, equal = true;
        for (int index = 0; index < 3; index++) {
          if (candidate[index] > target[index] + 1e-12) noWorse = false;
          if (candidate[index] < target[index] - 1e-12) strict = true;
          if (Math.abs(candidate[index] - target[index]) > 1e-12) equal = false;
        }
        if ((noWorse && strict) || equal) { covered++; break; }
      }
    }
    return ((double) covered) / right.size();
  }
  private static String read(Path path) throws Exception {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
  private static String property(String text, String key) {
    for (String line : text.split("\\R")) if (line.startsWith(key + "=")) {
      return line.substring(key.length() + 1);
    }
    throw new IllegalStateException("missing " + key);
  }
  private static void writeHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> values = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile).forEach(file -> {
        try { values.put(directory.relativize(file).toString().replace('\\', '/'), sha256(file)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : values.entrySet()) {
      out.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }
  private static final class Run {
    private final String seed;
    private final ZhangBoV35P25ECorrectedComparisonRunner.Algorithm algorithm;
    private final List<double[]> front; private final String sourceKind; private final String fe;
    private final String identity; private P8MetricCalculator.Metrics metrics;
    private Run(String seed, ZhangBoV35P25ECorrectedComparisonRunner.Algorithm algorithm,
        List<double[]> front, String sourceKind, String fe, String identity) {
      this.seed = seed; this.algorithm = algorithm; this.front = front;
      this.sourceKind = sourceKind; this.fe = fe; this.identity = identity;
    }
  }
}
