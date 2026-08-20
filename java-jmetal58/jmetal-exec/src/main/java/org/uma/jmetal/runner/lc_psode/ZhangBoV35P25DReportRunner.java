package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/** Builds the one frozen reference only after all forty P25D runs completed. */
public final class ZhangBoV35P25DReportRunner {
  private ZhangBoV35P25DReportRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !"--output".equals(args[0])) {
      throw new IllegalArgumentException("Usage: --output <P25D root>");
    }
    generate(Paths.get(args[1]).toAbsolutePath().normalize());
  }

  static void generate(Path output) throws Exception {
    List<Run> runs = new ArrayList<>();
    List<double[]> pooled = new ArrayList<>();
    for (int slot = 1; slot <= 5; slot++) {
      long seed = ZhangBoV35P25DRunner.approvedSeed(slot);
      for (ZhangBoV35P25DRunner.Algorithm algorithm
          : ZhangBoV35P25DRunner.Algorithm.values()) {
        Path directory = output.resolve("runs/seed-" + seed).resolve(algorithm.name());
        if (!Files.isDirectory(directory)) throw new IllegalStateException("missing run: " + directory);
        String status = new String(Files.readAllBytes(directory.resolve("status.properties")),
            StandardCharsets.UTF_8);
        if (!status.contains("status=COMPLETED") || !status.contains("fullEvaluations=50000")) {
          throw new IllegalStateException("run not complete: " + directory);
        }
        List<double[]> front = readFront(directory.resolve("front.csv"));
        Run run = new Run(seed, algorithm, front); runs.add(run); pooled.addAll(front);
      }
    }
    List<double[]> reference = P8MetricCalculator.nondominated(pooled);
    for (Run run : runs) run.metrics = P8MetricCalculator.calculate(run.front, reference);
    writeFront(output.resolve("reference-front.csv"), reference);
    writeMetrics(output.resolve("metrics.csv"), runs);
    writeSummary(output.resolve("stability-summary.csv"), runs);
    Files.write(output.resolve("PILOT_REPORT.md"), report(runs, reference.size())
        .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("QMOEA_PENDING.md"), (
        "# QMOEA source gate\n\nQMOEA未进入本轮运行。当前工作区没有能够由论文、来源"
            + "和结构三方证明的QMOEA实现；禁止以MOHPSOQ或其他近似类替代。"
            + "状态：`PENDING_SOURCE_VERIFICATION`。\n").getBytes(StandardCharsets.UTF_8));
    System.out.println("P25D_REPORT_COMPLETED runs=" + runs.size() + " reference=" + reference.size());
  }

  private static void writeMetrics(Path path, List<Run> runs) throws Exception {
    StringBuilder out = new StringBuilder("algorithm,seed,HV,IGD,Spacing,C_A_R,C_R_A,frontSize,minCmax,minTEC,minTWC\n");
    for (Run run : runs) out.append(run.algorithm).append(',').append(run.seed).append(',')
        .append(run.metrics.hv).append(',').append(run.metrics.igd).append(',')
        .append(run.metrics.spacing).append(',').append(run.metrics.cForward).append(',')
        .append(run.metrics.cReverse).append(',').append(run.front.size()).append(',')
        .append(min(run.front, 0)).append(',').append(min(run.front, 1)).append(',')
        .append(min(run.front, 2)).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeSummary(Path path, List<Run> runs) throws Exception {
    Map<ZhangBoV35P25DRunner.Algorithm, List<Run>> groups = new EnumMap<>(ZhangBoV35P25DRunner.Algorithm.class);
    for (ZhangBoV35P25DRunner.Algorithm algorithm : ZhangBoV35P25DRunner.Algorithm.values()) {
      groups.put(algorithm, new ArrayList<Run>());
    }
    for (Run run : runs) groups.get(run.algorithm).add(run);
    StringBuilder out = new StringBuilder("algorithm,medianHV,iqrHV,meanHV,stdHV,medianIGD,iqrIGD,medianCmax,medianTEC,medianTWC\n");
    for (Map.Entry<ZhangBoV35P25DRunner.Algorithm, List<Run>> entry : groups.entrySet()) {
      List<Double> hv = values(entry.getValue(), 0); List<Double> igd = values(entry.getValue(), 1);
      out.append(entry.getKey()).append(',').append(median(hv)).append(',')
          .append(percentile(hv, .75) - percentile(hv, .25)).append(',')
          .append(mean(hv)).append(',').append(std(hv)).append(',').append(median(igd))
          .append(',').append(percentile(igd, .75) - percentile(igd, .25)).append(',')
          .append(median(minima(entry.getValue(), 0))).append(',')
          .append(median(minima(entry.getValue(), 1))).append(',')
          .append(median(minima(entry.getValue(), 2))).append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String report(List<Run> runs, int referenceSize) {
    return "# V35-P25D 八算法五Seed 50k FE先导报告\n\n"
        + "- 运行数：`" + runs.size() + "/40`。\n"
        + "- 实例：`20_2_3_1`；FM3；单一产品族；序列无关SUT；ShiftMode.NONE。\n"
        + "- 统一经验reference在全部40次结束后一次构造，点数：`" + referenceSize + "`。\n"
        + "- `QMOEA`因来源未闭合未运行；没有使用近似算法冒充。\n"
        + "- 本轮只有5个seed，只用于初步稳定性诊断，不构成显著性或论文最终结论。\n\n"
        + "逐seed结果见`metrics.csv`，五seed汇总见`stability-summary.csv`。\n";
  }

  private static List<double[]> readFront(Path path) throws Exception {
    List<double[]> result = new ArrayList<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).trim().isEmpty()) continue;
      String[] values = lines.get(index).split(",");
      result.add(new double[] {Double.parseDouble(values[0]), Double.parseDouble(values[1]),
          Double.parseDouble(values[2])});
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

  private static List<Double> values(List<Run> runs, int field) {
    List<Double> values = new ArrayList<>();
    for (Run run : runs) values.add(field == 0 ? run.metrics.hv : run.metrics.igd);
    Collections.sort(values); return values;
  }
  private static List<Double> minima(List<Run> runs, int objective) {
    List<Double> values = new ArrayList<>();
    for (Run run : runs) values.add(min(run.front, objective));
    Collections.sort(values); return values;
  }
  private static double min(List<double[]> front, int objective) {
    double value = Double.POSITIVE_INFINITY;
    for (double[] point : front) value = Math.min(value, point[objective]);
    return value;
  }
  private static double mean(List<Double> values) { double sum=0; for(double v:values)sum+=v; return sum/values.size(); }
  private static double std(List<Double> values) { double m=mean(values),s=0; for(double v:values)s+=(v-m)*(v-m); return Math.sqrt(s/values.size()); }
  private static double median(List<Double> values) { return percentile(values, .5); }
  private static double percentile(List<Double> values, double p) {
    if (values.isEmpty()) return Double.NaN;
    double position = p * (values.size() - 1); int left=(int)Math.floor(position), right=(int)Math.ceil(position);
    return values.get(left) + (values.get(right)-values.get(left))*(position-left);
  }

  private static final class Run {
    private final long seed; private final ZhangBoV35P25DRunner.Algorithm algorithm;
    private final List<double[]> front; private P8MetricCalculator.Metrics metrics;
    private Run(long seed, ZhangBoV35P25DRunner.Algorithm algorithm, List<double[]> front) {
      this.seed=seed; this.algorithm=algorithm; this.front=front;
    }
  }
}
