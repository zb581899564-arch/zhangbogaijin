package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Aggregates the original seed and five additional seeds without significance claims. */
public final class ZhangBoP9SixSeedSummaryRunner {
  private static final long[] SEEDS = {
      20260808L, 20260809L, 20260810L, 20260811L, 20260812L, 20260813L
  };

  private ZhangBoP9SixSeedSummaryRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    if (Files.exists(parsed.output)) {
      throw new IllegalStateException("Summary output already exists: " + parsed.output);
    }
    Files.createDirectories(parsed.output);
    List<SeedResult> results = new ArrayList<>();
    for (long seed : SEEDS) {
      Path root = seed == 20260808L
          ? parsed.existingSeedRoot : parsed.additionalRoot.resolve("seed-" + seed);
      results.add(read(seed, root));
    }
    Files.write(parsed.output.resolve("per-seed-comparison.csv"), csv(results)
        .getBytes(StandardCharsets.UTF_8));
    String classification = classification(results);
    Files.write(parsed.output.resolve("P9_SIX_SEED_STABILITY_REPORT.md"),
        report(results, classification).getBytes(StandardCharsets.UTF_8));
    Files.write(parsed.output.resolve("status.properties"),
        ("schema=zhangbo-p9-six-seed-v1\nstatus=COMPLETED\nclassification="
            + classification + "\nseedCount=6\n").getBytes(StandardCharsets.UTF_8));
    writeHashes(parsed.output);
    System.out.println("P9_SIX_SEED_REPORT_COMPLETED classification=" + classification
        + " output=" + parsed.output);
  }

  private static SeedResult read(long seed, Path root) throws IOException {
    Path fullDir = root.resolve("ZHANGBO-FULL-" + seed);
    Path baseDir = root.resolve("HMOPSO-QGS-F-" + seed);
    Map<String, String> full = properties(fullDir.resolve("status.properties"));
    Map<String, String> base = properties(baseDir.resolve("status.properties"));
    Map<String, String> comparison = properties(root.resolve("comparison/status.properties"));
    require("FULL status", full.get("status"), "COMPLETED");
    require("baseline status", base.get("status"), "COMPLETED");
    require("comparison status", comparison.get("status"), "COMPLETED");
    require("initial population", full.get("initialPopulationSha256"),
        base.get("initialPopulationSha256"));
    require("instance", full.get("instanceSha256"), base.get("instanceSha256"));
    require("SUT", full.get("instanceExtensionSha256"), base.get("instanceExtensionSha256"));
    require("fatigue", full.get("fatigueParametersSha256"),
        base.get("fatigueParametersSha256"));
    List<String> metrics = Files.readAllLines(root.resolve("comparison/metrics.csv"),
        StandardCharsets.UTF_8);
    if (metrics.size() != 3) throw new IOException("Expected two metric rows: " + root);
    Metric fm = Metric.parse(metrics.get(1), "ZHANGBO-FULL");
    Metric bm = Metric.parse(metrics.get(2), "HMOPSO-QGS-F");
    return new SeedResult(seed, comparison.get("signal"), fm, bm,
        number(full, "fatigueExcess"), number(base, "fatigueExcess"),
        number(full, "highFatigueRatio"), number(base, "highFatigueRatio"),
        number(full, "wallClockMillis"), number(base, "wallClockMillis"),
        full.get("initialPopulationSha256"));
  }

  private static String csv(List<SeedResult> values) {
    StringBuilder csv = new StringBuilder("seed,signal,initialPopulationSha256,"
        + "fullMinCmax,baseMinCmax,fullMinTEC,baseMinTEC,fullMinTWC,baseMinTWC,"
        + "C_full_base,C_base_full,fullHV,baseHV,fullIGD,baseIGD,"
        + "fullFatigueExcess,baseFatigueExcess,fullHighFatigueRatio,"
        + "baseHighFatigueRatio,fullWallMillis,baseWallMillis\n");
    for (SeedResult value : values) {
      csv.append(value.seed).append(',').append(value.signal).append(',')
          .append(value.initialHash).append(',').append(n(value.full.minCmax)).append(',')
          .append(n(value.base.minCmax)).append(',').append(n(value.full.minTec)).append(',')
          .append(n(value.base.minTec)).append(',').append(n(value.full.minTwc)).append(',')
          .append(n(value.base.minTwc)).append(',').append(n(value.full.coverage)).append(',')
          .append(n(value.base.coverage)).append(',').append(n(value.full.hv)).append(',')
          .append(n(value.base.hv)).append(',').append(n(value.full.igd)).append(',')
          .append(n(value.base.igd)).append(',').append(n(value.fullFatigueExcess)).append(',')
          .append(n(value.baseFatigueExcess)).append(',')
          .append(n(value.fullHighFatigueRatio)).append(',')
          .append(n(value.baseHighFatigueRatio)).append(',').append(n(value.fullWallMillis))
          .append(',').append(n(value.baseWallMillis)).append('\n');
    }
    return csv.toString();
  }

  private static String classification(List<SeedResult> values) {
    int promising = 0, regression = 0, threeObjectiveWins = 0;
    for (SeedResult value : values) {
      if ("PROMISING_SIGNAL".equals(value.signal)) promising++;
      if ("REGRESSION_SIGNAL".equals(value.signal)) regression++;
      if (value.full.minCmax < value.base.minCmax
          && value.full.minTec < value.base.minTec
          && value.full.minTwc < value.base.minTwc) threeObjectiveWins++;
    }
    if (promising >= 4 && threeObjectiveWins >= 4 && regression == 0) {
      return "CONSISTENT_PROMISING_SIGNAL";
    }
    if (regression >= 4) return "CONSISTENT_REGRESSION_SIGNAL";
    return "MIXED_OR_INCONCLUSIVE";
  }

  private static String report(List<SeedResult> values, String classification) {
    int promising = countSignal(values, "PROMISING_SIGNAL");
    int inconclusive = countSignal(values, "INCONCLUSIVE");
    int regression = countSignal(values, "REGRESSION_SIGNAL");
    int cmaxWins = wins(values, 0), tecWins = wins(values, 1), twcWins = wins(values, 2);
    int fatigueWins = 0, highFatigueWins = 0;
    List<Double> cFullBase = new ArrayList<>(), cBaseFull = new ArrayList<>();
    List<Double> cmaxDelta = new ArrayList<>(), tecDelta = new ArrayList<>(),
        twcDelta = new ArrayList<>(), runtimeRatio = new ArrayList<>();
    for (SeedResult value : values) {
      if (value.fullFatigueExcess < value.baseFatigueExcess) fatigueWins++;
      if (value.fullHighFatigueRatio < value.baseHighFatigueRatio) highFatigueWins++;
      cFullBase.add(value.full.coverage);
      cBaseFull.add(value.base.coverage);
      cmaxDelta.add(relative(value.full.minCmax, value.base.minCmax));
      tecDelta.add(relative(value.full.minTec, value.base.minTec));
      twcDelta.add(relative(value.full.minTwc, value.base.minTwc));
      runtimeRatio.add(value.fullWallMillis / value.baseWallMillis);
    }
    StringBuilder table = new StringBuilder(
        "|seed|信号|C(FULL,BASE)|C(BASE,FULL)|Cmax变化|TEC变化|TWC变化|\n"
            + "|---:|---|---:|---:|---:|---:|---:|\n");
    for (SeedResult value : values) {
      table.append('|').append(value.seed).append('|').append(value.signal).append('|')
          .append(n(value.full.coverage)).append('|').append(n(value.base.coverage)).append('|')
          .append(percent(relative(value.full.minCmax, value.base.minCmax))).append('|')
          .append(percent(relative(value.full.minTec, value.base.minTec))).append('|')
          .append(percent(relative(value.full.minTwc, value.base.minTwc))).append("|\n");
    }
    return "# P9六seed稳定性诊断\n\n"
        + "- 汇总结论：`" + classification + "`。\n"
        + "- seed：`20260808..20260813`，每个seed均为同实例、同FM3问题、"
        + "同初始种群配对的FULL与HMOPSO-QGS-F比较。\n"
        + "- 单seed信号计数：PROMISING=" + promising + "，INCONCLUSIVE="
        + inconclusive + "，REGRESSION=" + regression + "。\n"
        + "- FULL最小值胜出次数：Cmax=" + cmaxWins + "/6，TEC=" + tecWins
        + "/6，TWC=" + twcWins + "/6。\n"
        + "- 疲劳超阈积分胜出=" + fatigueWins + "/6；高疲劳比例胜出="
        + highFatigueWins + "/6。\n\n" + table + "\n"
        + "六seed中位数：`C(FULL,BASE)=" + n(median(cFullBase))
        + "`，`C(BASE,FULL)=" + n(median(cBaseFull)) + "`；FULL相对基线的"
        + "最小Cmax/TEC/TWC变化分别为`" + percent(median(cmaxDelta)) + " / "
        + percent(median(tecDelta)) + " / " + percent(median(twcDelta))
        + "`；wall-clock倍率中位数为`" + n(median(runtimeRatio)) + "x`。\n\n"
        + "该汇总只判断信号是否跨seed保持，不进行显著性检验，也不启动20次矩阵。"
        + "`sampled_reproduction_accepted=false`，`full_reproduction_accepted=false`。\n";
  }

  private static int countSignal(List<SeedResult> values, String signal) {
    int count = 0;
    for (SeedResult value : values) if (signal.equals(value.signal)) count++;
    return count;
  }

  private static int wins(List<SeedResult> values, int objective) {
    int count = 0;
    for (SeedResult value : values) {
      double left = objective == 0 ? value.full.minCmax
          : objective == 1 ? value.full.minTec : value.full.minTwc;
      double right = objective == 0 ? value.base.minCmax
          : objective == 1 ? value.base.minTec : value.base.minTwc;
      if (left < right) count++;
    }
    return count;
  }

  private static double relative(double full, double base) {
    return (full - base) / base;
  }

  private static double median(List<Double> source) {
    List<Double> values = new ArrayList<>(source);
    Collections.sort(values);
    int middle = values.size() / 2;
    return values.size() % 2 == 0
        ? (values.get(middle - 1) + values.get(middle)) / 2.0 : values.get(middle);
  }

  private static Map<String, String> properties(Path path) throws IOException {
    Map<String, String> values = new LinkedHashMap<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      if (line.isEmpty()) continue;
      int equals = line.indexOf('=');
      if (equals <= 0) throw new IOException("Invalid properties line: " + line);
      values.put(line.substring(0, equals), line.substring(equals + 1));
    }
    return values;
  }

  private static double number(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null) throw new IllegalStateException("Missing key " + key);
    return Double.parseDouble(value);
  }

  private static void require(String name, String actual, String expected) {
    if (actual == null || !actual.equals(expected)) {
      throw new IllegalStateException(name + " mismatch: " + actual + " != " + expected);
    }
  }

  private static String n(double value) {
    return String.format(Locale.ROOT, "%.12g", value);
  }

  private static String percent(double value) {
    return String.format(Locale.ROOT, "%+.3f%%", value * 100.0);
  }

  private static void writeHashes(Path output) throws IOException {
    List<Path> paths = new ArrayList<>();
    try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(output)) {
      for (Path path : stream) {
        if (Files.isRegularFile(path)
            && !"evidence-sha256.tsv".equals(path.getFileName().toString())) paths.add(path);
      }
    }
    Collections.sort(paths);
    StringBuilder text = new StringBuilder("sha256\tbytes\tfile\n");
    for (Path path : paths) {
      text.append(sha256(path)).append('\t').append(Files.size(path)).append('\t')
          .append(path.getFileName()).append('\n');
    }
    Files.write(output.resolve("evidence-sha256.tsv"),
        text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws IOException {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static final class Metric {
    private final double hv, igd, coverage, minCmax, minTec, minTwc;
    private Metric(double hv, double igd, double coverage,
        double minCmax, double minTec, double minTwc) {
      this.hv = hv; this.igd = igd; this.coverage = coverage;
      this.minCmax = minCmax; this.minTec = minTec; this.minTwc = minTwc;
    }
    private static Metric parse(String line, String expectedName) {
      String[] values = line.split(",", -1);
      if (values.length != 13 || !expectedName.equals(values[0])) {
        throw new IllegalArgumentException("Invalid metric row: " + line);
      }
      return new Metric(Double.parseDouble(values[2]), Double.parseDouble(values[3]),
          Double.parseDouble(values[5]), Double.parseDouble(values[7]),
          Double.parseDouble(values[8]), Double.parseDouble(values[9]));
    }
  }

  private static final class SeedResult {
    private final long seed;
    private final String signal, initialHash;
    private final Metric full, base;
    private final double fullFatigueExcess, baseFatigueExcess;
    private final double fullHighFatigueRatio, baseHighFatigueRatio;
    private final double fullWallMillis, baseWallMillis;
    private SeedResult(long seed, String signal, Metric full, Metric base,
        double fullFatigueExcess, double baseFatigueExcess,
        double fullHighFatigueRatio, double baseHighFatigueRatio,
        double fullWallMillis, double baseWallMillis, String initialHash) {
      this.seed = seed; this.signal = signal; this.full = full; this.base = base;
      this.fullFatigueExcess = fullFatigueExcess;
      this.baseFatigueExcess = baseFatigueExcess;
      this.fullHighFatigueRatio = fullHighFatigueRatio;
      this.baseHighFatigueRatio = baseHighFatigueRatio;
      this.fullWallMillis = fullWallMillis; this.baseWallMillis = baseWallMillis;
      this.initialHash = initialHash;
    }
  }

  private static final class Arguments {
    private final Path existingSeedRoot, additionalRoot, output;
    private Arguments(Path existingSeedRoot, Path additionalRoot, Path output) {
      this.existingSeedRoot = existingSeedRoot;
      this.additionalRoot = additionalRoot;
      this.output = output;
    }
    private static Arguments parse(String[] args) {
      Path existing = null, additional = null, output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        Path value = Paths.get(args[index + 1]).toAbsolutePath().normalize();
        if ("--existing-seed-root".equals(args[index])) existing = value;
        else if ("--additional-root".equals(args[index])) additional = value;
        else if ("--output".equals(args[index])) output = value;
        else throw usage();
      }
      if (existing == null || additional == null || output == null) throw usage();
      return new Arguments(existing, additional, output);
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --existing-seed-root <path> "
          + "--additional-root <path> --output <path>");
    }
  }
}
