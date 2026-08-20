package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * V35-P22 closure: 10-job multi-instance pilot.  Three 10-job instances
 * (10_2_2_1 from the p8 bridge, 10_2_3_1 and 10_3_2_1 from the generated pilot
 * set) each run baseline vs FULL at one seed (20260808), 500k FE, 100
 * particles, shared controlled start.  Mechanism-level acceptance only
 * (Qg rounds, DSCR, CFVF, CA-TA-Lite, FE closure, audit COMPLETED) plus
 * directional sanity; explicitly not statistics.
 */
public class V35P22MultiInstancePilotTest {
  private static final long SEED = 20260808L;
  private static final int BUDGET = 500000;
  private static final int POPULATION = 100;
  private static final int MIN_ACCEPTABLE_FE = 495000;

  private static final class PilotSpec {
    final String name;
    final int jobs;
    final int stages;
    final int factories;
    final Path dataDir;
    final Path fatigueDir;
    final Path extensionDir;
    final String source;
    PilotSpec(String name, int jobs, int stages, int factories,
        Path dataDir, Path fatigueDir, Path extensionDir, String source) {
      this.name = name; this.jobs = jobs; this.stages = stages;
      this.factories = factories; this.dataDir = dataDir;
      this.fatigueDir = fatigueDir; this.extensionDir = extensionDir;
      this.source = source;
    }
  }

  @Test(timeout = 2400000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void multiInstancePilot() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    final Path root = project;
    Path javaProject = root.resolve("java-jmetal58");
    Path bridge = javaProject.resolve("p8-bridge/v1");
    Path pilot = javaProject.resolve("EADHFSP-pilot");
    new V35PilotInstanceGeneratorTest().generatePilotInstances();

    Path evidence = root.resolve("docs/evidence/V35-P22");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    List<PilotSpec> specs = new ArrayList<>();
    specs.add(new PilotSpec("10_2_2_1", 10, 2, 2, bridge.resolve("EADHFSP"),
        bridge.resolve("fatigue-parameters/v1"), bridge.resolve("instance-extensions/v1"),
        "p8-bridge"));
    specs.add(new PilotSpec("10_2_3_1", 10, 2, 3, pilot.resolve("EADHFSP"),
        pilot.resolve("fatigue-parameters/v1"), pilot.resolve("instance-extensions/v1"),
        "generated"));
    specs.add(new PilotSpec("10_3_2_1", 10, 3, 2, pilot.resolve("EADHFSP"),
        pilot.resolve("fatigue-parameters/v1"), pilot.resolve("instance-extensions/v1"),
        "generated"));

    Map<String, V35FairRunner.RunRecord> records = new TreeMap<>();
    Map<String, String> configTexts = new TreeMap<>();
    for (PilotSpec spec : specs) {
      System.setProperty("dhfsp.data.dir", spec.dataDir.toString());
      System.setProperty("dhfsp.fatigue.dir", spec.fatigueDir.toString());
      System.setProperty("dhfsp.instance.extension.dir", spec.extensionDir.toString());
      ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          spec.jobs, spec.stages, spec.factories, 1);
      ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
          seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
          ProductionDecodeMode.FM3, SEED);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int i = 0; i < POPULATION; i++) initial.add(seedProblem.createSolution());

      V35FairRunner.RunRecord baseline = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
          newProblem(spec), initial, BUDGET, SEED);
      V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
          newProblem(spec), initial, BUDGET, SEED);
      assertEquals("COMPLETED baseline " + spec.name, "COMPLETED", baseline.getStatus());
      assertEquals("COMPLETED full " + spec.name, "COMPLETED", full.getStatus());
      assertEquals("same controlled start " + spec.name, baseline.getInitialPopulationHash(),
          full.getInitialPopulationHash());
      assertTrue("baseline FE closure " + spec.name + ": " + baseline.getFullEvaluations(),
          baseline.getFullEvaluations() >= MIN_ACCEPTABLE_FE);
      assertTrue("full FE closure " + spec.name + ": " + full.getFullEvaluations(),
          full.getFullEvaluations() >= MIN_ACCEPTABLE_FE);
      assertTrue("baseline FE must not exceed budget " + spec.name + ": "
          + baseline.getFullEvaluations(), baseline.getFullEvaluations() <= BUDGET);
      assertTrue("full FE must not exceed budget " + spec.name + ": " + full.getFullEvaluations(),
          full.getFullEvaluations() <= BUDGET);

      // Mechanism fingerprints: baseline zeros, FULL all-active.
      assertEquals("baseline cfvf off " + spec.name, 0L,
          summaryLong(baseline, "cfvfOffspring"));
      assertEquals("baseline caTaLite off " + spec.name, 0L,
          summaryLong(baseline, "caTaLiteFE"));
      assertTrue("baseline Qg rounds > 0 " + spec.name,
          summaryLong(baseline, "formalQgRounds") > 0L);
      assertTrue("full cfvf on " + spec.name, summaryLong(full, "cfvfOffspring") > 0L);
      assertTrue("full caTaLite on " + spec.name, summaryLong(full, "caTaLiteFE") > 0L);
      assertTrue("full DSCR teacher uses > 0 " + spec.name, dscrLong(full, "teacherUses") > 0L);
      assertEquals("full DTUR gate holds " + spec.name, 0L,
          dscrLong(full, "dominatedTeacherUses"));
      assertTrue("full archive insertions > 0 " + spec.name,
          summaryLong(full, "archiveInsertions") > 0L);

      V35FairRunner.writeRecord(baseline, runs.resolve("baseline-500k-" + spec.name),
          baselineConfigText(spec));
      V35FairRunner.writeRecord(full, runs.resolve("full-500k-" + spec.name),
          fullConfigText(spec));
      records.put(spec.name + "-baseline", baseline);
      records.put(spec.name + "-full", full);
      configTexts.put(spec.name, fullConfigText(spec));
    }

    // Metrics CSV.
    StringBuilder csv = new StringBuilder();
    csv.append("instance,arm,status,FE,frontSize,minCmax,minTEC,minTWC,cfvfOffspring,"
        + "caTaLiteFE,archiveInsertions,dscrTeacherUses,dscrDominated,formalQgRounds,"
        + "algorithmRunSeconds\n");
    for (PilotSpec spec : specs) {
      csv.append(armMetrics(spec.name, "baseline", records.get(spec.name + "-baseline")))
          .append('\n');
      csv.append(armMetrics(spec.name, "full", records.get(spec.name + "-full")))
          .append('\n');
    }
    Files.write(evidence.resolve("PILOT_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // HV per instance against the pooled union.
    StringBuilder hvCsv = new StringBuilder();
    hvCsv.append("instance,hvBaseline,hvFull,hvRatio,hvUnion,hvFullShare,hvBaselineShare,"
        + "cBaselineOverFull,cFullOverBaseline,igdBaseline,igdFull,"
        + "unionNondominated,baseNondominated,fullNondominated\n");
    for (PilotSpec spec : specs) {
      List<double[]> baselineFront = records.get(spec.name + "-baseline").getFront();
      List<double[]> fullFront = records.get(spec.name + "-full").getFront();
      List<double[]> union = new ArrayList<>();
      union.addAll(baselineFront);
      union.addAll(fullFront);
      P8MetricCalculator.Metrics base = P8MetricCalculator.calculate(baselineFront, union);
      P8MetricCalculator.Metrics ful = P8MetricCalculator.calculate(fullFront, union);
      P8MetricCalculator.Metrics unionMetrics = P8MetricCalculator.calculate(union, union);
      P8MetricCalculator.Metrics baseVsFull = P8MetricCalculator.calculate(baselineFront, fullFront);
      hvCsv.append(String.format(Locale.ROOT,
          "%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%d,%d\n",
          spec.name, base.hv, ful.hv, ful.hv / base.hv, unionMetrics.hv,
          ful.hv / unionMetrics.hv, base.hv / unionMetrics.hv,
          baseVsFull.cForward, baseVsFull.cReverse, base.igd, ful.igd,
          unionMetrics.nondominatedCount, base.nondominatedCount, ful.nondominatedCount));
    }
    Files.write(evidence.resolve("PILOT_HV_METRICS.csv"),
        hvCsv.toString().getBytes(StandardCharsets.UTF_8));

    writeReport(evidence, specs, records);

    Files.deleteIfExists(evidence.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    java.util.stream.Stream<Path> walk = Files.walk(evidence);
    walk.filter(Files::isRegularFile).forEach(path -> {
      try {
        hashes.put(root.relativize(path).toString().replace('\\', '/'), sha256(path));
      } catch (Exception error) {
        throw new RuntimeException(error);
      }
    });
    walk.close();
    StringBuilder manifest = new StringBuilder();
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      manifest.append(entry.getValue()).append("  ").append(entry.getKey()).append('\n');
    }
    Files.write(evidence.resolve("evidence-sha256.tsv"),
        manifest.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeReport(Path evidence, List<PilotSpec> specs,
      Map<String, V35FairRunner.RunRecord> records) throws Exception {
    StringBuilder report = new StringBuilder();
    report.append("# V35-P22 10 工件多实例 pilot 证据\n\n");
    report.append("诊断性证据：单 seed ").append(SEED).append("，三个 10 工件实例各 baseline/FULL ")
        .append(BUDGET).append(" FE。机制级验收 + 方向 sanity，无统计、无正式结论。\n\n");
    report.append("## 实例\n\n");
    for (PilotSpec spec : specs) {
      report.append("- ").append(spec.name).append("：").append(spec.jobs).append(" 工件、")
          .append(spec.stages).append(" 阶段、").append(spec.factories).append(" 工厂");
      if ("p8-bridge".equals(spec.source)) {
        report.append("（p8 桥接既有实例，来源 `p8-bridge/v1`）\n");
      } else {
        report.append("（V35PilotInstanceGeneratorTest 生成，来源 `EADHFSP-pilot`）\n");
      }
    }
    report.append("\n## 逐臂状态\n\n");
    report.append("| 实例 | 臂 | 状态 | FE | 前沿大小 | minCmax | minTEC | minTWC |\n");
    report.append("|---|---|---|---|---|---|---|---|\n");
    for (PilotSpec spec : specs) {
      for (String arm : new String[]{"baseline", "full"}) {
        V35FairRunner.RunRecord record = records.get(spec.name + "-" + arm);
        double[] min = minOf(record);
        report.append("| ").append(spec.name).append(" | ").append(arm).append(" | ")
            .append(record.getStatus()).append(" | ").append(record.getFullEvaluations())
            .append(" | ").append(record.getFront().size()).append(" | ").append(min[0])
            .append(" | ").append(min[1]).append(" | ").append(min[2]).append(" |\n");
      }
    }
    report.append("\n机制级验收：三实例 FULL 均 CFVF>0、CA-TA-Lite>0、DSCR teacherUses>0 且 DTUR=0、")
        .append("档案插入>0；baseline 均零；双臂 FE 收口 ≥495000。\n\n");
    report.append("## 数据文件\n\n- `PILOT_METRICS.csv`：逐臂机制计数与极值\n");
    report.append("- `PILOT_HV_METRICS.csv`：池化参考 HV/IGD/覆盖\n");
    report.append("- `runs/`：每臂 configuration.txt / front.csv / 审计与 DSCR 文件\n");
    Files.write(evidence.resolve("V35_P22_REPORT.md"),
        report.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static double[] minOf(V35FairRunner.RunRecord record) {
    double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    for (double[] value : record.getFront()) {
      min[0] = Math.min(min[0], value[0]);
      min[1] = Math.min(min[1], value[1]);
      min[2] = Math.min(min[2], value[2]);
    }
    return min;
  }

  private static String armMetrics(String instance, String arm, V35FairRunner.RunRecord record) {
    double[] min = minOf(record);
    return String.format(Locale.ROOT,
        "%s,%s,%s,%d,%d,%f,%f,%f,%d,%d,%d,%d,%d,%d,%.3f",
        instance, arm, record.getStatus(), record.getFullEvaluations(),
        record.getFront().size(), min[0], min[1], min[2],
        summaryLong(record, "cfvfOffspring"), summaryLong(record, "caTaLiteFE"),
        summaryLong(record, "archiveInsertions"), dscrLong(record, "teacherUses"),
        dscrLong(record, "dominatedTeacherUses"), summaryLong(record, "formalQgRounds"),
        record.getAlgorithmRunNanos() / 1e9);
  }

  private static String baselineConfigText(PilotSpec spec) {
    return V35AblationRegistry.configFor(V35AblationRegistry.Rung.A0_BASELINE, SEED,
        POPULATION, BUDGET).canonicalText();
  }

  private static String fullConfigText(PilotSpec spec) {
    return V35AblationRegistry.configFor(V35AblationRegistry.Rung.A5_FULL, SEED,
        POPULATION, BUDGET).canonicalText();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(PilotSpec spec) throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        spec.jobs, spec.stages, spec.factories, 1);
    return ZhangBoV35ProblemFactory.create(source.getFatigueInstanceData(),
        source.getFatigueParameters(), ProductionDecodeMode.FM3, SEED);
  }

  private static long summaryLong(V35FairRunner.RunRecord record, String key) {
    String summary = record.getMechanismSummary();
    String marker = key + "=";
    int index = summary.indexOf(marker);
    if (index < 0) return -1L;
    int end = summary.indexOf(',', index);
    if (end < 0) end = summary.length();
    return Long.parseLong(summary.substring(index + marker.length(), end));
  }

  private static long dscrLong(V35FairRunner.RunRecord record, String key) {
    String summary = record.getMechanismSummary();
    String marker = "dscr=";
    int start = summary.indexOf(marker);
    if (start < 0) return -1L;
    start += marker.length();
    int end = summary.indexOf(",algorithmRunNanos=", start);
    if (end < 0) end = summary.length();
    String nested = summary.substring(start, end);
    String field = key + "=";
    int index = nested.indexOf('|' + field);
    if (index < 0) {
      index = nested.indexOf(field);
      if (index != 0) return -1L;
    } else {
      index++;
    }
    int valueStart = index + field.length();
    int valueEnd = nested.indexOf('|', valueStart);
    if (valueEnd < 0) valueEnd = nested.length();
    return Long.parseLong(nested.substring(valueStart, valueEnd));
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }
}
