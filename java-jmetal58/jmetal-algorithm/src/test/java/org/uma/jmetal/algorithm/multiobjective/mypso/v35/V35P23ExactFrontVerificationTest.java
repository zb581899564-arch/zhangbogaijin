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
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * V35-P23 closure: exact-front verification on 3/5-job instances.  The exact
 * four-vector space is enumerated exhaustively (3_2_2_1: 3,072 decodes;
 * 5_2_2_1: 3,932,160 decodes) through the production FM3 evaluate, and the
 * baseline/FULL 500k single-seed fronts are measured against that exact
 * reference (IGD, C-metric).  Cross-validation: no algorithm solution may
 * strictly dominate an exact-front point, which would signal an enumerator or
 * decoding-口径 bug.  Diagnostic only; one seed, no statistics.
 */
public class V35P23ExactFrontVerificationTest {
  private static final long SEED = 20260808L;
  private static final int BUDGET = 500000;
  private static final int POPULATION = 100;
  private static final int MIN_ACCEPTABLE_FE = 495000;

  @Test(timeout = 2400000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void exactFrontVerification() throws Exception {
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
    Path pilot = javaProject.resolve("EADHFSP-pilot");
    new V35PilotInstanceGeneratorTest().generatePilotInstances();

    Path evidence = root.resolve("docs/evidence/V35-P23");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    System.setProperty("dhfsp.data.dir", pilot.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", pilot.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        pilot.resolve("instance-extensions/v1").toString());

    StringBuilder csv = new StringBuilder();
    csv.append("instance,exactFrontSize,enumeratedDecodes,arm,status,FE,armFrontSize,"
        + "igd,cForwardOverExact,cExactOverArm,minCmax,minTEC,minTWC,"
        + "armSolutionsDominatingExact,algorithmRunSeconds\n");
    for (String name : new String[]{"3_2_2_1", "5_2_2_1"}) {
      String[] parts = name.split("_");
      int jobs = Integer.parseInt(parts[0]);
      int stages = Integer.parseInt(parts[1]);
      int factories = Integer.parseInt(parts[2]);

      // Exact enumeration on a dedicated problem instance (own evaluation counter).
      ZhangBoCanonicalProductionProblem exactProblem = newProblem(jobs, stages, factories);
      V35ExactFrontEnumerator enumerator = new V35ExactFrontEnumerator(exactProblem);
      List<double[]> exactFront = enumerator.enumerate();
      assertTrue("exact front non-empty " + name, !exactFront.isEmpty());
      writeFront(exactFront, evidence.resolve("exact-front-" + name + ".csv"));

      // Decode determinism cross-check: evaluation order must not change objectives.
      verifyDecodeDeterminism(jobs, stages, factories);

      // Controlled-start arms on fresh problems.
      ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          jobs, stages, factories, 1);
      ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
          seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
          ProductionDecodeMode.FM3, SEED);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int i = 0; i < POPULATION; i++) initial.add(seedProblem.createSolution());

      for (String arm : new String[]{"baseline", "full"}) {
        V35FairRunner.Mode mode = arm.equals("baseline")
            ? V35FairRunner.Mode.V35_BASELINE : V35FairRunner.Mode.V35_FULL;
        V35FairRunner.RunRecord record = V35FairRunner.run(mode, newProblem(jobs, stages, factories),
            initial, BUDGET, SEED);
        assertEquals("COMPLETED " + arm + " " + name, "COMPLETED", record.getStatus());
        assertTrue(arm + " FE closure " + name + ": " + record.getFullEvaluations(),
            record.getFullEvaluations() >= MIN_ACCEPTABLE_FE);
        assertTrue(arm + " FE must not exceed budget " + name + ": "
            + record.getFullEvaluations(), record.getFullEvaluations() <= BUDGET);
        assertEquals("same controlled start " + arm + " " + name,
            initialHash(initial), record.getInitialPopulationHash());

        // Cross-validation: no arm point may strictly dominate an exact point.
        int dominating = 0;
        for (double[] armPoint : record.getFront()) {
          for (double[] exactPoint : exactFront) {
            if (V35ExactFrontEnumerator.dominates(armPoint, exactPoint)) dominating++;
          }
        }
        assertEquals("algorithm front must never dominate the exact front " + arm + " " + name,
            0, dominating);

        P8MetricCalculator.Metrics metrics = P8MetricCalculator.calculate(record.getFront(),
            exactFront);
        double[] min = minOf(record);
        csv.append(String.format(Locale.ROOT,
            "%s,%d,%d,%s,%s,%d,%d,%f,%f,%f,%f,%f,%f,%d,%.3f\n",
            name, exactFront.size(), enumerator.getEvaluatedCount(), arm, record.getStatus(),
            record.getFullEvaluations(), record.getFront().size(), metrics.igd,
            metrics.cForward, metrics.cReverse, min[0], min[1], min[2], dominating,
            record.getAlgorithmRunNanos() / 1e9));

        V35FairRunner.writeRecord(record, runs.resolve(arm + "-500k-" + name),
            V35AblationRegistry.configFor(arm.equals("baseline")
                ? V35AblationRegistry.Rung.A0_BASELINE : V35AblationRegistry.Rung.A5_FULL,
                SEED, POPULATION, BUDGET).canonicalText());
      }
    }
    Files.write(evidence.resolve("EXACT_FRONT_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    writeReport(evidence);

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

  /** Order-independence check: same solutions in a different order give the same objectives. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void verifyDecodeDeterminism(int jobs, int stages, int factories)
      throws Exception {
    ZhangBoCanonicalProductionProblem first = newProblem(jobs, stages, factories);
    ZhangBoCanonicalProductionProblem second = newProblem(jobs, stages, factories);
    List<DhhfspFourVectorSolution> a = new ArrayList<>();
    List<DhhfspFourVectorSolution> b = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      a.add((DhhfspFourVectorSolution) first.createSolution());
      b.add((DhhfspFourVectorSolution) second.createSolution());
    }
    first.evaluate(a.get(0)); first.evaluate(a.get(1)); first.evaluate(a.get(2));
    second.evaluate(b.get(2)); second.evaluate(b.get(0)); second.evaluate(b.get(1));
    for (int i = 0; i < 3; i++) {
      assertEquals("decode determinism obj0 " + i, a.get(i).getObjective(0),
          b.get(i).getObjective(0), 1e-12);
      assertEquals("decode determinism obj1 " + i, a.get(i).getObjective(1),
          b.get(i).getObjective(1), 1e-12);
      assertEquals("decode determinism obj6 " + i, a.get(i).getObjective(6),
          b.get(i).getObjective(6), 1e-12);
    }
  }

  private static void writeFront(List<double[]> front, Path path) throws Exception {
    StringBuilder text = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      text.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeReport(Path evidence) throws Exception {
    StringBuilder report = new StringBuilder();
    report.append("# V35-P23 3/5 工件精确前沿核验证据\n\n");
    report.append("诊断性证据：穷举四向量全空间（3_2_2_1：3,072；5_2_2_1：3,932,160 解码），")
        .append("与 baseline/FULL 单 seed ").append(BUDGET).append(" FE 前沿对比（IGD/C 指标）。")
        .append("交叉验证：算法前沿任何解不得严格支配精确前沿解。无统计、无正式结论。\n\n");
    report.append("## 数据文件\n\n- `EXACT_FRONT_METRICS.csv`：精确前沿规模、穷举解码数、双臂 IGD/C\n");
    report.append("- `exact-front-3_2_2_1.csv` / `exact-front-5_2_2_1.csv`：精确前沿\n");
    report.append("- `runs/`：各臂 configuration.txt / front.csv / 审计文件\n");
    Files.write(evidence.resolve("V35_P23_REPORT.md"),
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

  private static String initialHash(List<PermutationSolution<Integer>> population) {
    return V35FairRunner.initialHash(population);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(int jobs, int stages, int factories)
      throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        jobs, stages, factories, 1);
    return ZhangBoV35ProblemFactory.create(source.getFatigueInstanceData(),
        source.getFatigueParameters(), ProductionDecodeMode.FM3, SEED);
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }
}
