package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/** Freezes one cross-arm/cross-seed engineering reference and decides the P25A main variant. */
public final class ZhangBoV35P25AReportRunner {
  private static final double EPS = 1.0e-12;

  private ZhangBoV35P25AReportRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    generate(parsed.runsRoot, parsed.output);
  }

  static Decision generate(Path runsRoot, Path output) throws Exception {
    List<Run> runs = load(runsRoot.toAbsolutePath().normalize());
    if (runs.size() != 15) throw new IllegalStateException("P25A requires exactly 15 runs: " + runs.size());
    Map<Long, Map<ZhangBoV35P25ARunner.Arm, Run>> bySeed = new TreeMap<>();
    for (Run run : runs) {
      Map<ZhangBoV35P25ARunner.Arm, Run> arms = bySeed.get(run.seed);
      if (arms == null) { arms = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class); bySeed.put(run.seed, arms); }
      if (arms.put(run.arm, run) != null) throw new IllegalStateException("duplicate run " + run.seed + "/" + run.arm);
    }
    if (bySeed.size() != 5) throw new IllegalStateException("P25A requires five seeds");
    for (int slot = 1; slot <= 5; slot++) {
      long seed = ZhangBoV35P25ARunner.approvedSeed(slot);
      Map<ZhangBoV35P25ARunner.Arm, Run> arms = bySeed.get(seed);
      if (arms == null || arms.size() != 3) throw new IllegalStateException("missing arms for seed " + seed);
      String hash = arms.get(ZhangBoV35P25ARunner.Arm.A0).initialHash;
      for (Run run : arms.values()) if (!hash.equals(run.initialHash)) {
        throw new IllegalStateException("initial population mismatch for seed " + seed);
      }
    }

    List<double[]> pooled = new ArrayList<>();
    for (Run run : runs) pooled.addAll(run.front);
    List<double[]> reference = P8MetricCalculator.nondominated(pooled);
    for (Run run : runs) run.metrics = P8MetricCalculator.calculate(run.front, reference);

    Files.createDirectories(output);
    writeReference(output.resolve("reference-front.csv"), reference);
    writePerSeed(output.resolve("per-seed-metrics.csv"), bySeed);
    Summary summary = summarize(bySeed);
    writeSummary(output.resolve("arm-summary.csv"), summary);
    Files.write(output.resolve("decision.properties"), decisionProperties(summary)
        .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("V35_P25A_REPORT.md"), report(summary, reference.size())
        .getBytes(StandardCharsets.UTF_8));
    writeRunAudit(output.resolve("run-evidence-audit.csv"), runs);
    writeHashes(output);
    System.out.println("V35_P25A_REPORT_COMPLETED decision=" + summary.decision
        + " referenceSize=" + reference.size() + " output=" + output);
    return summary.decision;
  }

  public enum Decision { A4_MAIN, A5_FULL_MAIN, STOP_REVIEW }

  private static Summary summarize(Map<Long, Map<ZhangBoV35P25ARunner.Arm, Run>> bySeed) {
    Summary result = new Summary();
    for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
      result.hv.put(arm, new ArrayList<Double>());
      result.igd.put(arm, new ArrayList<Double>());
      result.spacing.put(arm, new ArrayList<Double>());
    }
    List<Double> a5MinusA4Hv = new ArrayList<>();
    List<Double> a5MinusA4Coverage = new ArrayList<>();
    Map<ZhangBoV35P25ARunner.Arm, Integer> winsVsA0 = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class);
    Map<ZhangBoV35P25ARunner.Arm, List<Double>> coverageVsA0 = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class);
    for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
      winsVsA0.put(arm, 0); coverageVsA0.put(arm, new ArrayList<Double>());
    }
    int a5WinOrTie = 0;
    for (Map<ZhangBoV35P25ARunner.Arm, Run> arms : bySeed.values()) {
      Run a0 = arms.get(ZhangBoV35P25ARunner.Arm.A0);
      Run a4 = arms.get(ZhangBoV35P25ARunner.Arm.A4);
      Run a5 = arms.get(ZhangBoV35P25ARunner.Arm.A5);
      for (Run run : arms.values()) {
        result.hv.get(run.arm).add(run.metrics.hv);
        result.igd.get(run.arm).add(run.metrics.igd);
        result.spacing.get(run.arm).add(run.metrics.spacing);
      }
      if (a5.metrics.hv + EPS >= a4.metrics.hv) a5WinOrTie++;
      a5MinusA4Hv.add(a5.metrics.hv - a4.metrics.hv);
      a5MinusA4Coverage.add(coverageAdvantage(a5.front, a4.front));
      for (ZhangBoV35P25ARunner.Arm arm : new ZhangBoV35P25ARunner.Arm[] {
          ZhangBoV35P25ARunner.Arm.A4, ZhangBoV35P25ARunner.Arm.A5}) {
        Run candidate = arms.get(arm);
        if (candidate.metrics.hv > a0.metrics.hv + EPS) {
          winsVsA0.put(arm, winsVsA0.get(arm) + 1);
        }
        coverageVsA0.get(arm).add(coverageAdvantage(candidate.front, a0.front));
      }
    }
    result.a5WinOrTieVsA4 = a5WinOrTie;
    result.a5MedianHvDeltaVsA4 = median(a5MinusA4Hv);
    result.a5MedianCoverageAdvantageVsA4 = median(a5MinusA4Coverage);
    result.a4HvWinsVsA0 = winsVsA0.get(ZhangBoV35P25ARunner.Arm.A4);
    result.a5HvWinsVsA0 = winsVsA0.get(ZhangBoV35P25ARunner.Arm.A5);
    result.a4MedianCoverageAdvantageVsA0 = median(coverageVsA0.get(ZhangBoV35P25ARunner.Arm.A4));
    result.a5MedianCoverageAdvantageVsA0 = median(coverageVsA0.get(ZhangBoV35P25ARunner.Arm.A5));
    boolean a4Signal = result.a4HvWinsVsA0 >= 4 && result.a4MedianCoverageAdvantageVsA0 > 0.0;
    boolean a5Signal = result.a5HvWinsVsA0 >= 4 && result.a5MedianCoverageAdvantageVsA0 > 0.0;
    boolean poolPass = result.a5WinOrTieVsA4 >= 3 && result.a5MedianHvDeltaVsA4 >= -EPS
        && result.a5MedianCoverageAdvantageVsA4 >= -EPS;
    if (!a4Signal && !a5Signal) result.decision = Decision.STOP_REVIEW;
    else if (poolPass && a5Signal) result.decision = Decision.A5_FULL_MAIN;
    else if (a4Signal) result.decision = Decision.A4_MAIN;
    else result.decision = Decision.A5_FULL_MAIN;
    return result;
  }

  private static void writePerSeed(Path path,
      Map<Long, Map<ZhangBoV35P25ARunner.Arm, Run>> bySeed) throws Exception {
    StringBuilder out = new StringBuilder("seed,arm,FE,frontSize,HV,IGD,Spacing,minCmax,minTEC,minTWC,"
        + "C_arm_over_A0,C_A0_over_arm,C_arm_over_A4,C_A4_over_arm,wallSeconds\n");
    for (Map.Entry<Long, Map<ZhangBoV35P25ARunner.Arm, Run>> entry : bySeed.entrySet()) {
      Run a0 = entry.getValue().get(ZhangBoV35P25ARunner.Arm.A0);
      Run a4 = entry.getValue().get(ZhangBoV35P25ARunner.Arm.A4);
      for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
        Run run = entry.getValue().get(arm);
        out.append(String.format(Locale.ROOT, "%d,%s,%d,%d,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,"
                + "%.12f,%.12f,%.12f,%.12f,%.6f\n",
            entry.getKey(), arm, run.fe, run.front.size(), run.metrics.hv, run.metrics.igd,
            run.metrics.spacing, minimum(run.front, 0), minimum(run.front, 1), minimum(run.front, 2),
            coverage(run.front, a0.front), coverage(a0.front, run.front),
            coverage(run.front, a4.front), coverage(a4.front, run.front),
            run.algorithmRunNanos / 1.0e9));
      }
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeSummary(Path path, Summary summary) throws Exception {
    StringBuilder out = new StringBuilder("arm,medianHV,medianIGD,medianSpacing,hvWinsVsA0,medianCoverageAdvantageVsA0\n");
    for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
      int wins = arm == ZhangBoV35P25ARunner.Arm.A4 ? summary.a4HvWinsVsA0
          : arm == ZhangBoV35P25ARunner.Arm.A5 ? summary.a5HvWinsVsA0 : 0;
      double coverage = arm == ZhangBoV35P25ARunner.Arm.A4 ? summary.a4MedianCoverageAdvantageVsA0
          : arm == ZhangBoV35P25ARunner.Arm.A5 ? summary.a5MedianCoverageAdvantageVsA0 : 0.0;
      out.append(String.format(Locale.ROOT, "%s,%.12f,%.12f,%.12f,%d,%.12f\n", arm,
          median(summary.hv.get(arm)), median(summary.igd.get(arm)),
          median(summary.spacing.get(arm)), wins, coverage));
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String decisionProperties(Summary summary) {
    return "decision=" + summary.decision + "\n"
        + "a5HvWinOrTieVsA4=" + summary.a5WinOrTieVsA4 + "/5\n"
        + "a5MedianHvDeltaVsA4=" + summary.a5MedianHvDeltaVsA4 + "\n"
        + "a5MedianCoverageAdvantageVsA4=" + summary.a5MedianCoverageAdvantageVsA4 + "\n"
        + "a4HvWinsVsA0=" + summary.a4HvWinsVsA0 + "/5\n"
        + "a4MedianCoverageAdvantageVsA0=" + summary.a4MedianCoverageAdvantageVsA0 + "\n"
        + "a5HvWinsVsA0=" + summary.a5HvWinsVsA0 + "/5\n"
        + "a5MedianCoverageAdvantageVsA0=" + summary.a5MedianCoverageAdvantageVsA0 + "\n"
        + "formalMatrixStarted=false\nsampledReproductionAccepted=false\n"
        + "fullReproductionAccepted=false\n";
  }

  private static String report(Summary s, int referenceSize) {
    return "# V35-P25A 五 Seed 主版本判定\n\n"
        + "本报告使用 `20_2_3_1`、seed `20260809..20260813`、每臂 500000 FE。"
        + "15 个最终前沿全部完成后才一次性冻结统一工程参考前沿；归一化 HV 参考点为 "
        + "`(1.1,1.1,1.1)`。参考前沿规模：**" + referenceSize + "**。\n\n"
        + "## 判定\n\n**" + s.decision + "**\n\n"
        + "- A5 对 A4 的 HV 胜/平：`" + s.a5WinOrTieVsA4 + "/5`\n"
        + "- A5−A4 HV 中位差：`" + s.a5MedianHvDeltaVsA4 + "`\n"
        + "- A5 对 A4 覆盖优势中位数：`" + s.a5MedianCoverageAdvantageVsA4 + "`\n"
        + "- A4 对 A0：HV 胜 `" + s.a4HvWinsVsA0 + "/5`，覆盖优势中位数 `"
        + s.a4MedianCoverageAdvantageVsA0 + "`\n"
        + "- A5 对 A0：HV 胜 `" + s.a5HvWinsVsA0 + "/5`，覆盖优势中位数 `"
        + s.a5MedianCoverageAdvantageVsA0 + "`\n\n"
        + "这是主版本工程门，不是显著性检验或论文最终数字；不会自动启动20次正式实验，"
        + "也不会自动修改方向教师池。\n";
  }

  private static List<Run> load(Path root) throws Exception {
    List<Run> result = new ArrayList<>();
    for (int slot = 1; slot <= 5; slot++) {
      long seed = ZhangBoV35P25ARunner.approvedSeed(slot);
      for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
        Path directory = root.resolve("runs/seed-" + seed).resolve(arm.name());
        if (!Files.isDirectory(directory)) throw new IllegalStateException("missing run " + directory);
        verifyManifest(directory);
        Map<String, String> status = keyValues(directory.resolve("status.properties"));
        if (!"COMPLETED".equals(status.get("status"))) throw new IllegalStateException("incomplete run " + directory);
        Run run = new Run(); run.seed = seed; run.arm = arm; run.directory = directory;
        run.fe = Integer.parseInt(status.get("fullEvaluations"));
        run.algorithmRunNanos = Long.parseLong(status.get("algorithmRunNanos"));
        run.initialHash = firstToken(directory.resolve("initial-population.sha256"));
        run.front = front(directory.resolve("front.csv"));
        result.add(run);
      }
    }
    return result;
  }

  private static void verifyManifest(Path directory) throws Exception {
    List<String> lines = Files.readAllLines(directory.resolve("evidence-sha256.tsv"), StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).trim().isEmpty()) continue;
      String[] fields = lines.get(index).split("\\t", 2);
      if (fields.length != 2) throw new IllegalStateException("bad manifest line: " + lines.get(index));
      Path file = directory.resolve(fields[0]);
      if (!Files.isRegularFile(file) || !fields[1].equals(sha256(file))) {
        throw new IllegalStateException("run evidence hash mismatch: " + file);
      }
    }
  }

  private static void writeReference(Path path, List<double[]> front) throws Exception {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeRunAudit(Path path, List<Run> runs) throws Exception {
    StringBuilder out = new StringBuilder("seed,arm,runDirectory,evidenceManifestSha256\n");
    for (Run run : runs) out.append(run.seed).append(',').append(run.arm).append(',')
        .append(run.directory).append(',').append(sha256(run.directory.resolve("evidence-sha256.tsv"))).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static Map<String, String> keyValues(Path path) throws Exception {
    Map<String, String> values = new HashMap<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      int separator = line.indexOf('=');
      if (separator > 0) values.put(line.substring(0, separator), line.substring(separator + 1));
    }
    return values;
  }

  private static String firstToken(Path path) throws Exception {
    return Files.readAllLines(path, StandardCharsets.UTF_8).get(0).trim().split("\\s+")[0];
  }

  private static List<double[]> front(Path path) throws Exception {
    List<double[]> result = new ArrayList<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).trim().isEmpty()) continue;
      String[] fields = lines.get(index).split(",");
      result.add(new double[] {Double.parseDouble(fields[0]), Double.parseDouble(fields[1]),
          Double.parseDouble(fields[2])});
    }
    if (result.isEmpty()) throw new IllegalStateException("empty front: " + path);
    return result;
  }

  private static double coverageAdvantage(List<double[]> left, List<double[]> right) {
    return coverage(left, right) - coverage(right, left);
  }

  private static double coverage(List<double[]> left, List<double[]> right) {
    int covered = 0;
    for (double[] target : right) {
      for (double[] candidate : left) {
        if (P8MetricCalculator.dominates(candidate, target) || equal(candidate, target)) {
          covered++; break;
        }
      }
    }
    return right.isEmpty() ? 0.0 : ((double) covered) / right.size();
  }

  private static boolean equal(double[] left, double[] right) {
    for (int index = 0; index < 3; index++) if (Math.abs(left[index] - right[index]) > EPS) return false;
    return true;
  }

  private static double minimum(List<double[]> values, int objective) {
    double result = Double.POSITIVE_INFINITY;
    for (double[] point : values) result = Math.min(result, point[objective]);
    return result;
  }

  private static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<>(values); Collections.sort(sorted);
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 1 ? sorted.get(middle)
        : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
  }

  private static void writeHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    java.util.stream.Stream<Path> walk = Files.walk(directory);
    try {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try { hashes.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    } finally { walk.close(); }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) out.append(entry.getKey())
        .append('\t').append(entry.getValue()).append('\n');
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static final class Run {
    private long seed; private ZhangBoV35P25ARunner.Arm arm; private int fe;
    private long algorithmRunNanos; private String initialHash; private Path directory;
    private List<double[]> front; private P8MetricCalculator.Metrics metrics;
  }

  private static final class Summary {
    private final Map<ZhangBoV35P25ARunner.Arm, List<Double>> hv = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class);
    private final Map<ZhangBoV35P25ARunner.Arm, List<Double>> igd = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class);
    private final Map<ZhangBoV35P25ARunner.Arm, List<Double>> spacing = new EnumMap<>(ZhangBoV35P25ARunner.Arm.class);
    private int a5WinOrTieVsA4, a4HvWinsVsA0, a5HvWinsVsA0;
    private double a5MedianHvDeltaVsA4, a5MedianCoverageAdvantageVsA4;
    private double a4MedianCoverageAdvantageVsA0, a5MedianCoverageAdvantageVsA0;
    private Decision decision;
  }

  private static final class Arguments {
    private final Path runsRoot, output;
    private Arguments(Path runsRoot, Path output) { this.runsRoot = runsRoot; this.output = output; }
    private static Arguments parse(String[] args) {
      Path runs = null, output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--runs-root".equals(args[index])) runs = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (runs == null || output == null) throw usage();
      return new Arguments(runs, output);
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --runs-root <path> --output <path>");
    }
  }
}
