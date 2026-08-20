package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/** Unified-reference diagnostic report for the three-seed BAL/full-mask gate. */
public final class ZhangBoV35P25CBalOpenReportRunner {
  private static final double EPS = 1.0e-12;

  public enum Decision { A4_PREFERRED_SIGNAL, A5_PREFERRED_SIGNAL, STOP_REVIEW }

  private ZhangBoV35P25CBalOpenReportRunner() { }

  public static void main(String[] args) throws Exception {
    Path runs = null, output = null;
    for (int index = 0; index < args.length; index += 2) {
      if (index + 1 >= args.length) throw usage();
      if ("--runs-root".equals(args[index])) runs = Paths.get(args[index + 1]);
      else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
      else throw usage();
    }
    if (runs == null || output == null) throw usage();
    generate(runs, output);
  }

  static Decision generate(Path runsRoot, Path output) throws Exception {
    Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> runs = load(runsRoot);
    List<double[]> union = new ArrayList<>();
    for (Map<ZhangBoV35P25CBalOpenRunner.Arm, Run> arms : runs.values()) {
      for (Run run : arms.values()) union.addAll(run.front);
    }
    List<double[]> reference = P8MetricCalculator.nondominated(union);
    for (Map<ZhangBoV35P25CBalOpenRunner.Arm, Run> arms : runs.values()) {
      for (Run run : arms.values()) run.metrics = P8MetricCalculator.calculate(run.front, reference);
    }
    Summary summary = summarize(runs);
    Files.createDirectories(output);
    writeReference(output.resolve("reference-front.csv"), reference);
    writePerSeed(output.resolve("per-seed-metrics.csv"), runs);
    writeArmSummary(output.resolve("arm-summary.csv"), runs);
    Files.write(output.resolve("decision.properties"), properties(summary)
        .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("V35_P25C_BAL_OPEN_REPORT.md"), report(summary, reference.size())
        .getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    System.out.println("V35_P25C_REPORT_COMPLETED decision=" + summary.decision
        + " reference=" + reference.size() + " output=" + output);
    return summary.decision;
  }

  private static Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> load(Path root)
      throws Exception {
    Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> result = new TreeMap<>();
    for (int slot = 1; slot <= 3; slot++) {
      long seed = ZhangBoV35P25CBalOpenRunner.approvedSeed(slot);
      Map<ZhangBoV35P25CBalOpenRunner.Arm, Run> arms =
          new EnumMap<>(ZhangBoV35P25CBalOpenRunner.Arm.class);
      for (ZhangBoV35P25CBalOpenRunner.Arm arm : ZhangBoV35P25CBalOpenRunner.Arm.values()) {
        Path directory = root.toAbsolutePath().normalize().resolve("runs/seed-" + seed)
            .resolve(arm.name());
        verifyHashes(directory);
        Map<String, String> status = keyValues(directory.resolve("status.properties"));
        if (!"COMPLETED".equals(status.get("status"))) {
          throw new IllegalStateException("incomplete run: " + directory);
        }
        Run run = new Run(); run.seed = seed; run.arm = arm; run.directory = directory;
        run.fe = Integer.parseInt(status.get("fullEvaluations"));
        run.nanos = Long.parseLong(status.get("algorithmRunNanos"));
        run.initialHash = firstToken(directory.resolve("initial-population.sha256"));
        run.front = front(directory.resolve("front.csv"));
        run.mechanism = new String(Files.readAllBytes(directory.resolve("mechanism-summary.txt")),
            StandardCharsets.UTF_8);
        arms.put(arm, run);
      }
      String initial = arms.get(ZhangBoV35P25CBalOpenRunner.Arm.A0).initialHash;
      for (Run run : arms.values()) if (!initial.equals(run.initialHash)) {
        throw new IllegalStateException("initial population mismatch at seed " + seed);
      }
      result.put(seed, arms);
    }
    return result;
  }

  private static Summary summarize(
      Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> runs) {
    Summary out = new Summary();
    List<Double> a4Coverage = new ArrayList<>(), a5Coverage = new ArrayList<>();
    List<Double> a5A4Coverage = new ArrayList<>(), a5A4Hv = new ArrayList<>();
    for (Map<ZhangBoV35P25CBalOpenRunner.Arm, Run> arms : runs.values()) {
      Run a0 = arms.get(ZhangBoV35P25CBalOpenRunner.Arm.A0);
      Run a4 = arms.get(ZhangBoV35P25CBalOpenRunner.Arm.A4);
      Run a5 = arms.get(ZhangBoV35P25CBalOpenRunner.Arm.A5);
      if (a4.metrics.hv > a0.metrics.hv + EPS) out.a4Wins++;
      if (a5.metrics.hv > a0.metrics.hv + EPS) out.a5Wins++;
      if (a5.metrics.hv + EPS >= a4.metrics.hv) out.a5WinTie++;
      a4Coverage.add(coverageAdvantage(a4.front, a0.front));
      a5Coverage.add(coverageAdvantage(a5.front, a0.front));
      a5A4Coverage.add(coverageAdvantage(a5.front, a4.front));
      a5A4Hv.add(a5.metrics.hv - a4.metrics.hv);
    }
    out.a4Coverage = median(a4Coverage); out.a5Coverage = median(a5Coverage);
    out.a5A4Coverage = median(a5A4Coverage); out.a5A4Hv = median(a5A4Hv);
    boolean a4Signal = out.a4Wins >= 2 && out.a4Coverage > 0.0;
    boolean a5Signal = out.a5Wins >= 2 && out.a5Coverage > 0.0;
    boolean poolPass = out.a5WinTie >= 2 && out.a5A4Hv >= -EPS && out.a5A4Coverage >= -EPS;
    out.decision = a5Signal && poolPass ? Decision.A5_PREFERRED_SIGNAL
        : a4Signal ? Decision.A4_PREFERRED_SIGNAL
        : a5Signal ? Decision.A5_PREFERRED_SIGNAL : Decision.STOP_REVIEW;
    return out;
  }

  private static void writePerSeed(Path path,
      Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> runs) throws Exception {
    StringBuilder out = new StringBuilder("seed,arm,FE,frontSize,HV,IGD,Spacing,minCmax,minTEC,minTWC,"
        + "coverageAdvantageVsA0,coverageAdvantageVsA4,wallSeconds,cfvfOffspring,qpActions,"
        + "archiveInsertions,caTaLiteTest,caTaLiteApply,directionalPoolRequests\n");
    for (Map.Entry<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> entry : runs.entrySet()) {
      Run a0 = entry.getValue().get(ZhangBoV35P25CBalOpenRunner.Arm.A0);
      Run a4 = entry.getValue().get(ZhangBoV35P25CBalOpenRunner.Arm.A4);
      for (ZhangBoV35P25CBalOpenRunner.Arm arm : ZhangBoV35P25CBalOpenRunner.Arm.values()) {
        Run run = entry.getValue().get(arm);
        out.append(String.format(Locale.ROOT,
            "%d,%s,%d,%d,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.6f,%d,%d,%d,%d,%d,%d\n",
            run.seed, run.arm, run.fe, run.front.size(), run.metrics.hv, run.metrics.igd,
            run.metrics.spacing, minimum(run.front, 0), minimum(run.front, 1), minimum(run.front, 2),
            coverageAdvantage(run.front, a0.front), coverageAdvantage(run.front, a4.front),
            run.nanos / 1.0e9, value(run, "cfvfOffspring"), value(run, "qpActions"),
            value(run, "archiveInsertions"), value(run, "caTaLiteTest"),
            value(run, "caTaLiteApply"), value(run, "directionalPoolRequests")));
      }
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeArmSummary(Path path,
      Map<Long, Map<ZhangBoV35P25CBalOpenRunner.Arm, Run>> runs) throws Exception {
    StringBuilder out = new StringBuilder("arm,medianHV,medianIGD,medianSpacing,medianWallSeconds\n");
    for (ZhangBoV35P25CBalOpenRunner.Arm arm : ZhangBoV35P25CBalOpenRunner.Arm.values()) {
      List<Double> hv = new ArrayList<>(), igd = new ArrayList<>(), spacing = new ArrayList<>(), time = new ArrayList<>();
      for (Map<ZhangBoV35P25CBalOpenRunner.Arm, Run> row : runs.values()) {
        Run run = row.get(arm); hv.add(run.metrics.hv); igd.add(run.metrics.igd);
        spacing.add(run.metrics.spacing); time.add(run.nanos / 1.0e9);
      }
      out.append(String.format(Locale.ROOT, "%s,%.12f,%.12f,%.12f,%.6f\n", arm,
          median(hv), median(igd), median(spacing), median(time)));
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String properties(Summary s) {
    return "decision=" + s.decision + "\na4HvWinsVsA0=" + s.a4Wins + "/3\n"
        + "a5HvWinsVsA0=" + s.a5Wins + "/3\na5HvWinOrTieVsA4=" + s.a5WinTie + "/3\n"
        + "a4MedianCoverageAdvantageVsA0=" + s.a4Coverage + "\n"
        + "a5MedianCoverageAdvantageVsA0=" + s.a5Coverage + "\n"
        + "a5MedianHvDeltaVsA4=" + s.a5A4Hv + "\n"
        + "a5MedianCoverageAdvantageVsA4=" + s.a5A4Coverage + "\n"
        + "diagnosticOnly=true\nformalMatrixStarted=false\n";
  }

  private static String report(Summary s, int referenceSize) {
    return "# V35-P25C BAL全开放三Seed 100k验证\n\n"
        + "统一参考前沿由A0/A4/A5全部9条运行结束后一次性构造，规模为**" + referenceSize
        + "**。本报告只判断当前安全语义是否存在可重复信号，不作显著性结论。\n\n"
        + "## 诊断判定\n\n**" + s.decision + "**\n\n"
        + "- A4相对A0的HV胜出：`" + s.a4Wins + "/3`；覆盖优势中位数：`" + s.a4Coverage + "`\n"
        + "- A5相对A0的HV胜出：`" + s.a5Wins + "/3`；覆盖优势中位数：`" + s.a5Coverage + "`\n"
        + "- A5相对A4的HV胜/平：`" + s.a5WinTie + "/3`；HV中位差：`" + s.a5A4Hv
        + "`；覆盖优势中位数：`" + s.a5A4Coverage + "`\n\n"
        + "固定语义：FM3、ShiftMode=NONE、单族、序列无关SUT、压力分类仅诊断、"
        + "BAL回退、N1-N5全开放、shadow关闭。\n";
  }

  private static long value(Run run, String key) {
    long value = ZhangBoV35P25ARunner.value(run.mechanism, key);
    return value == Long.MIN_VALUE ? 0L : value;
  }

  private static double coverageAdvantage(List<double[]> left, List<double[]> right) {
    return coverage(left, right) - coverage(right, left);
  }
  private static double coverage(List<double[]> left, List<double[]> right) {
    int count = 0;
    for (double[] target : right) for (double[] candidate : left) {
      if (P8MetricCalculator.dominates(candidate, target) || equal(candidate, target)) {
        count++; break;
      }
    }
    return right.isEmpty() ? 0.0 : ((double) count) / right.size();
  }
  private static boolean equal(double[] a, double[] b) {
    for (int i = 0; i < 3; i++) if (Math.abs(a[i] - b[i]) > EPS) return false;
    return true;
  }
  private static double minimum(List<double[]> front, int objective) {
    double min = Double.POSITIVE_INFINITY;
    for (double[] point : front) min = Math.min(min, point[objective]);
    return min;
  }
  private static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<>(values); Collections.sort(sorted);
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 1 ? sorted.get(middle)
        : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
  }

  private static List<double[]> front(Path path) throws Exception {
    List<double[]> out = new ArrayList<>(); List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) if (!lines.get(index).trim().isEmpty()) {
      String[] f = lines.get(index).split(",");
      out.add(new double[] {Double.parseDouble(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2])});
    }
    if (out.isEmpty()) throw new IllegalStateException("empty front: " + path);
    return out;
  }
  private static Map<String, String> keyValues(Path path) throws Exception {
    Map<String, String> out = new HashMap<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      int at = line.indexOf('='); if (at > 0) out.put(line.substring(0, at), line.substring(at + 1));
    }
    return out;
  }
  private static String firstToken(Path path) throws Exception {
    return Files.readAllLines(path, StandardCharsets.UTF_8).get(0).trim().split("\\s+")[0];
  }
  private static void verifyHashes(Path directory) throws Exception {
    for (String line : Files.readAllLines(directory.resolve("evidence-sha256.tsv"), StandardCharsets.UTF_8)) {
      if (line.startsWith("path\t") || line.trim().isEmpty()) continue;
      String[] f = line.split("\t", 2); Path file = directory.resolve(f[0]);
      if (f.length != 2 || !Files.isRegularFile(file) || !f[1].equals(sha256(file))) {
        throw new IllegalStateException("evidence hash mismatch: " + file);
      }
    }
  }
  private static void writeReference(Path path, List<double[]> front) throws Exception {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static void writeHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try { hashes.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
        catch (Exception error) { throw new RuntimeException(error); }
      });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) out.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }
  private static IllegalArgumentException usage() {
    return new IllegalArgumentException("Usage: --runs-root <path> --output <path>");
  }
  private static final class Run {
    long seed, nanos; int fe; String initialHash, mechanism; Path directory;
    ZhangBoV35P25CBalOpenRunner.Arm arm; List<double[]> front; P8MetricCalculator.Metrics metrics;
  }
  private static final class Summary {
    int a4Wins, a5Wins, a5WinTie; double a4Coverage, a5Coverage, a5A4Coverage, a5A4Hv; Decision decision;
  }
}
