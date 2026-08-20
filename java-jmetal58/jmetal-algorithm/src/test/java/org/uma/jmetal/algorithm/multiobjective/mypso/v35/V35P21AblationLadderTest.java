package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
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
 * V35-P21 closure: the six-rung ablation ladder A0(baseline) -> A1(+DSCR) ->
 * A2(+CFVF) -> A3(+Q-pbest) -> A4(+CA-TA-Lite) -> A5(+directional teacher pool),
 * one seed (20260808), 500k FE per rung on 20_2_3_1, plus I1 5k link arms for
 * the two newly added rungs (A2/A3) to prove the mechanisms actually fire.
 * Adjacent rungs must differ in exactly one switch (the registry's added
 * switch); every arm shares one controlled initial population.  The FULL-minus-
 * DSCR cell is forbidden (runtime fallback to legacy CA-TA) and documented in
 * the report.  Diagnostic only: one seed, no statistics, no formal claims.
 */
public class V35P21AblationLadderTest {
  private static final long SEED = 20260808L;
  private static final int BUDGET = 500000;
  private static final int POPULATION = 100;
  private static final int LINK_POPULATION = 10;
  private static final int LINK_BUDGET = 5000;
  private static final int MIN_ACCEPTABLE_FE = 495000;

  @Test(timeout = 2400000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void sixRungLadderWithI1LinkArms() throws Exception {
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
    Path evidence = root.resolve("docs/evidence/V35-P21");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    // --- 20_2_3_1 ladder: six arms on one controlled start -----------------
    System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", javaProject.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        javaProject.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
        seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
        ProductionDecodeMode.FM3, SEED);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) initial.add(seedProblem.createSolution());

    Map<String, V35FairRunner.RunRecord> ladder = new TreeMap<>();
    Map<String, String> ladderConfigs = new TreeMap<>();
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      V35FairRunner.RunRecord record = V35FairRunner.run(rung.getMode(), newProblem(20, 2, 3),
          initial, BUDGET, SEED);
      assertEquals("COMPLETED " + rung.getLabel(), "COMPLETED", record.getStatus());
      ladder.put(rung.getLabel(), record);
      ladderConfigs.put(rung.getLabel(),
          V35AblationRegistry.configFor(rung, SEED, POPULATION, BUDGET).canonicalText());
    }

    // Controlled start: all six arms share one initial population hash.
    String ladderHash = ladder.get("A0-baseline").getInitialPopulationHash();
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      assertEquals("same controlled start " + rung.getLabel(), ladderHash,
          ladder.get(rung.getLabel()).getInitialPopulationHash());
    }

    // Adjacent-rung discipline: exactly the expected switch line differs.
    for (int i = 1; i < V35AblationRegistry.LADDER.size(); i++) {
      V35AblationRegistry.Rung lower = V35AblationRegistry.LADDER.get(i - 1);
      V35AblationRegistry.Rung upper = V35AblationRegistry.LADDER.get(i);
      List<String> differing = V35AblationRegistry.differingSwitchKeys(
          ladderConfigs.get(lower.getLabel()), ladderConfigs.get(upper.getLabel()));
      assertEquals(lower.getLabel() + " -> " + upper.getLabel()
          + " must differ only in " + upper.getAddedSwitch(), Arrays.asList(upper.getAddedSwitch()),
          differing);
    }

    // Mechanism fingerprints per rung.
    assertArm(ladder.get("A0-baseline"), 0, 0, 0, 0, -1);
    assertArm(ladder.get("A1-dscr"), 0, 0, 0, 1, 0);
    assertArm(ladder.get("A2-cfvf"), 1, 0, 0, 1, 0);
    assertArm(ladder.get("A3-qp"), 1, 0, 1, 1, 0);
    assertArm(ladder.get("A4-catalite"), 1, 1, 1, 1, 0);
    assertArm(ladder.get("A5-full"), 1, 1, 1, 1, 1);

    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      V35FairRunner.RunRecord record = ladder.get(rung.getLabel());
      assertTrue(rung.getLabel() + " FE closure: " + record.getFullEvaluations(),
          record.getFullEvaluations() >= MIN_ACCEPTABLE_FE);
      // Acceptance-review hard gate: the budget is an upper bound too.  A2/A3
      // previously overflowed to 500100 via whole-swarm re-evaluation.
      assertTrue(rung.getLabel() + " FE must not exceed budget: " + record.getFullEvaluations(),
          record.getFullEvaluations() <= BUDGET);
      assertTrue(rung.getLabel() + " Qg rounds > 0",
          summaryLong(record, "formalQgRounds") > 0L);
      V35FairRunner.writeRecord(record, runs.resolve(rung.getLabel() + "-500k-20_2_3_1"),
          ladderConfigs.get(rung.getLabel()));
    }

    // --- I1 link arms: A2/A3 at 5k, proving the new rungs fire on I1 -------
    System.setProperty("dhfsp.data.dir", bridge.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", bridge.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        bridge.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW linkSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(10, 2, 2, 1);
    ZhangBoCanonicalProductionProblem linkSeedProblem = ZhangBoV35ProblemFactory.create(
        linkSource.getFatigueInstanceData(), linkSource.getFatigueParameters(),
        ProductionDecodeMode.FM3, SEED);
    List<PermutationSolution<Integer>> linkInitial = new ArrayList<>();
    for (int i = 0; i < LINK_POPULATION; i++) linkInitial.add(linkSeedProblem.createSolution());

    V35FairRunner.RunRecord linkA2 = V35FairRunner.run(V35FairRunner.Mode.V35_A2,
        newProblem(10, 2, 2), linkInitial, LINK_BUDGET, SEED);
    V35FairRunner.RunRecord linkA3 = V35FairRunner.run(V35FairRunner.Mode.V35_A3,
        newProblem(10, 2, 2), linkInitial, LINK_BUDGET, SEED);
    assertEquals("COMPLETED link A2", "COMPLETED", linkA2.getStatus());
    assertEquals("COMPLETED link A3", "COMPLETED", linkA3.getStatus());
    assertEquals("same controlled start link arms", linkA2.getInitialPopulationHash(),
        linkA3.getInitialPopulationHash());
    assertTrue("link A2 FE <= budget: " + linkA2.getFullEvaluations(),
        linkA2.getFullEvaluations() <= LINK_BUDGET);
    assertTrue("link A3 FE <= budget: " + linkA3.getFullEvaluations(),
        linkA3.getFullEvaluations() <= LINK_BUDGET);
    assertTrue("link A2 CFVF fires", summaryLong(linkA2, "cfvfOffspring") > 0L);
    assertTrue("link A3 CFVF fires", summaryLong(linkA3, "cfvfOffspring") > 0L);
    assertEquals("link A2 qp off", 0L, summaryLong(linkA2, "archiveInsertions"));
    assertTrue("link A3 qp fires", summaryLong(linkA3, "archiveInsertions") > 0L);
    assertTrue("link A2 DSCR teacher uses > 0", dscrLong(linkA2, "teacherUses") > 0L);
    assertTrue("link A3 DSCR teacher uses > 0", dscrLong(linkA3, "teacherUses") > 0L);
    V35FairRunner.writeRecord(linkA2, runs.resolve("link-A2-5k-I1-10_2_2_1"),
        V35AblationRegistry.configFor(V35AblationRegistry.Rung.A2_CFVF, SEED, LINK_POPULATION,
            LINK_BUDGET).canonicalText());
    V35FairRunner.writeRecord(linkA3, runs.resolve("link-A3-5k-I1-10_2_2_1"),
        V35AblationRegistry.configFor(V35AblationRegistry.Rung.A3_QP, SEED, LINK_POPULATION,
            LINK_BUDGET).canonicalText());

    // --- Metrics: ladder table + pooled-reference HV -----------------------
    List<double[]> pooled = new ArrayList<>();
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      pooled.addAll(ladder.get(rung.getLabel()).getFront());
    }
    P8MetricCalculator.Metrics unionMetrics = P8MetricCalculator.calculate(pooled, pooled);

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,frontSize,minCmax,minTEC,minTWC,cfvfOffspring,caTaLiteFE,"
        + "archiveInsertions,dscrTeacherUses,dscrDominatedTeacherUses,formalQgRounds,"
        + "algorithmRunSeconds\n");
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      csv.append(armMetrics(rung.getLabel(), ladder.get(rung.getLabel()))).append('\n');
    }
    Files.write(evidence.resolve("ABLATION_LADDER_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    StringBuilder hvCsv = new StringBuilder();
    hvCsv.append("rung,hv,hvShare,marginalDelta,igd,cPrevOverNext,cNextOverPrev,"
        + "nondominatedCount\n");
    double previousHv = Double.NaN;
    List<double[]> previousFront = null;
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      List<double[]> front = ladder.get(rung.getLabel()).getFront();
      P8MetricCalculator.Metrics metrics = P8MetricCalculator.calculate(front, pooled);
      double marginal = previousHv != previousHv ? Double.NaN : metrics.hv - previousHv;
      double cPrevOverNext = Double.NaN;
      double cNextOverPrev = Double.NaN;
      if (previousFront != null) {
        P8MetricCalculator.Metrics adjacent = P8MetricCalculator.calculate(previousFront, front);
        cPrevOverNext = adjacent.cForward;
        cNextOverPrev = adjacent.cReverse;
      }
      hvCsv.append(String.format(Locale.ROOT,
          "%s,%f,%f,%f,%f,%f,%f,%d\n",
          rung.getLabel(), metrics.hv, metrics.hv / unionMetrics.hv, marginal, metrics.igd,
          cPrevOverNext, cNextOverPrev, metrics.nondominatedCount));
      previousHv = metrics.hv;
      previousFront = front;
    }
    Files.write(evidence.resolve("ABLATION_HV_METRICS.csv"),
        hvCsv.toString().getBytes(StandardCharsets.UTF_8));

    writeReport(evidence, ladder, unionMetrics, ladderHash, linkA2, linkA3);

    // SHA-256 manifest over every evidence file (excluding the previous manifest,
    // which would make the file self-referential and unverifiable).
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

  /** Fingerprint expectations: cfvfOn, caTaLiteOn, qpOn(archive), dscrOn, poolOn. */
  private static void assertArm(V35FairRunner.RunRecord record, int cfvfOn, int caTaLiteOn,
      int qpOn, int dscrOn, int poolOn) {
    long cfvf = summaryLong(record, "cfvfOffspring");
    long caTaLite = summaryLong(record, "caTaLiteFE");
    long archive = summaryLong(record, "archiveInsertions");
    long teacherUses = dscrLong(record, "teacherUses");
    long dominated = dscrLong(record, "dominatedTeacherUses");
    if (cfvfOn == 0) assertEquals("cfvf off", 0L, cfvf); else assertTrue("cfvf on: " + cfvf, cfvf > 0L);
    if (caTaLiteOn == 0) assertEquals("caTaLite off", 0L, caTaLite); else assertTrue("caTaLite on: " + caTaLite, caTaLite > 0L);
    if (qpOn == 0) assertEquals("qp archive off", 0L, archive); else assertTrue("qp archive on: " + archive, archive > 0L);
    if (dscrOn == 0) assertTrue("dscr disabled", teacherUses < 0L); else {
      assertTrue("dscr teacher uses > 0: " + teacherUses, teacherUses > 0L);
      assertEquals("dscr dominated == 0", 0L, dominated);
    }
    // poolOn is only expressible through the FULL-vs-POOL_OFF adjacency proof,
    // asserted via the exact-difference discipline above.
  }

  private static String armMetrics(String label, V35FairRunner.RunRecord record) {
    double minCmax = Double.POSITIVE_INFINITY, minTEC = Double.POSITIVE_INFINITY,
        minTWC = Double.POSITIVE_INFINITY;
    for (double[] value : record.getFront()) {
      minCmax = Math.min(minCmax, value[0]);
      minTEC = Math.min(minTEC, value[1]);
      minTWC = Math.min(minTWC, value[2]);
    }
    return String.format(Locale.ROOT,
        "%s,%s,%d,%d,%f,%f,%f,%d,%d,%d,%d,%d,%d,%.3f",
        label, record.getStatus(), record.getFullEvaluations(), record.getFront().size(),
        minCmax, minTEC, minTWC, summaryLong(record, "cfvfOffspring"),
        summaryLong(record, "caTaLiteFE"), summaryLong(record, "archiveInsertions"),
        dscrLong(record, "teacherUses"), dscrLong(record, "dominatedTeacherUses"),
        summaryLong(record, "formalQgRounds"), record.getAlgorithmRunNanos() / 1e9);
  }

  private static void writeReport(Path evidence, Map<String, V35FairRunner.RunRecord> ladder,
      P8MetricCalculator.Metrics unionMetrics, String ladderHash,
      V35FairRunner.RunRecord linkA2, V35FairRunner.RunRecord linkA3) throws Exception {
    StringBuilder report = new StringBuilder();
    report.append("# V35-P21 算法树消融梯子证据\n\n");
    report.append("诊断性证据：单 seed ").append(SEED).append("，20_2_3_1 六梯级各 500k FE，")
        .append("I1 10_2_2_1 链路臂（A2/A3）各 5k FE。无统计、无正式结论。\n\n");
    report.append("## 梯子定义\n\n");
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      report.append("- ").append(rung.getLabel()).append("：").append(rung.getDescription())
          .append("；相邻新增开关 = ").append(rung.getAddedSwitch() == null ? "无（基准）" : rung.getAddedSwitch())
          .append("\n");
    }
    report.append("\n## 受控起点\n\n同一初始种群哈希：`").append(ladderHash).append("`（六臂一致）。\n\n");
    report.append("## 逐臂状态\n\n");
    report.append("| 臂 | 状态 | FE | 前沿大小 | minCmax | minTEC | minTWC |\n");
    report.append("|---|---|---|---|---|---|---|\n");
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      V35FairRunner.RunRecord record = ladder.get(rung.getLabel());
      double[] min = minOf(record);
      report.append("| ").append(rung.getLabel()).append(" | ").append(record.getStatus())
          .append(" | ").append(record.getFullEvaluations()).append(" | ")
          .append(record.getFront().size()).append(" | ").append(min[0]).append(" | ")
          .append(min[1]).append(" | ").append(min[2]).append(" |\n");
    }
    report.append("\n池化参考（六前沿并集）：非支配解 ")
        .append(unionMetrics.nondominatedCount).append(" 个。\n\n");
    report.append("## 禁止格\n\n").append(V35AblationRegistry.forbiddenFullMinusDscrNote())
        .append("\n\n");
    report.append("## I1 链路臂\n\nA2 5k：CFVF offspring=")
        .append(summaryLong(linkA2, "cfvfOffspring")).append("，DSCR teacherUses=")
        .append(dscrLong(linkA2, "teacherUses")).append("；A3 5k：archiveInsertions=")
        .append(summaryLong(linkA3, "archiveInsertions")).append("，DSCR teacherUses=")
        .append(dscrLong(linkA3, "teacherUses")).append("。\n\n");
    report.append("## 数据文件\n\n- `ABLATION_LADDER_METRICS.csv`：逐臂机制计数与极值\n");
    report.append("- `ABLATION_HV_METRICS.csv`：池化参考 HV、边际差、IGD、相邻覆盖\n");
    report.append("- `runs/`：每臂 configuration.txt / front.csv / 审计与 DSCR 文件\n");
    report.append("\n## FE 收口登记\n\n");
    for (V35AblationRegistry.Rung rung : V35AblationRegistry.LADDER) {
      int fe = ladder.get(rung.getLabel()).getFullEvaluations();
      report.append("- ").append(rung.getLabel()).append("：").append(fe);
      if (fe != BUDGET) {
        report.append("（预算 ").append(BUDGET).append("，差 ").append(fe - BUDGET).append("）");
      }
      report.append('\n');
    }
    report.append("\nA2/A3 曾溢出 +100 的原因（验收整改更正，2026-08-13）：正式基线循环先给全局后代打")
        .append("预评价标记、局部搜索后再调 evaluateSwarm；但标记只在\"局部搜索启用或结构化基线更新\"")
        .append("时被尊重，CFVF 更新模式（A2/A3）落入无条件整群重评分支——每外层周期重复评价整群")
        .append("（500k 下 18×100=1800 次，I1 下 2×10=20 次），末批整群越过预算 100。已修复为")
        .append("\"正式基线循环启用即尊重标记\"并增加 FE 上界硬门（<= 预算），修复后各臂收口 <= 预算；")
        .append("原\"critical-factory 检查粒度\"判断作废。\n");
    Files.write(evidence.resolve("V35_P21_REPORT.md"),
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(int jobs, int stages, int factories)
      throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        jobs, stages, factories, 1);
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
