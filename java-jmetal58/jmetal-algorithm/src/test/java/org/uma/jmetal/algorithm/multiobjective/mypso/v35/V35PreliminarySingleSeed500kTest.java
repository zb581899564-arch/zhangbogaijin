package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Map;
import org.junit.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * User-requested preliminary peek (single seed, one small instance, the real
 * experiment budget of 500 000 FE): the author's algorithm fair baseline
 * (V35_BASELINE: original Qg, structured baseline updater, inherited local
 * search — the chapter-4 semantics minus the v3.5 innovations) versus the
 * v3.5 FULL algorithm, on the same controlled start.
 * Diagnostic only: one seed, no statistics, no formal claims.
 */
public class V35PreliminarySingleSeed500kTest {
  private static final long SEED = 20260808L;
  private static final int BUDGET = 500000;
  private static final int POPULATION = 100;

  @Test(timeout = 1200000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void baselineVersusFullOnSingleSeed500k() throws Exception {
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

    V35FairRunner.RunRecord baseline = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        newProblem(20, 2, 3), initial, BUDGET, SEED);
    V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        newProblem(20, 2, 3), initial, BUDGET, SEED);
    assertEquals("COMPLETED baseline", "COMPLETED", baseline.getStatus());
    assertEquals("COMPLETED full", "COMPLETED", full.getStatus());
    assertEquals("controlled start", baseline.getInitialPopulationHash(),
        full.getInitialPopulationHash());
    assertTrue("baseline FE <= budget: " + baseline.getFullEvaluations(),
        baseline.getFullEvaluations() <= BUDGET);
    assertTrue("full FE <= budget: " + full.getFullEvaluations(),
        full.getFullEvaluations() <= BUDGET);

    ArmMetrics baselineMetrics = ArmMetrics.from(baseline);
    ArmMetrics fullMetrics = ArmMetrics.from(full);
    CrossMetrics cross = CrossMetrics.of(baseline.getFront(), full.getFront());

    Path evidence = root.resolve("docs/evidence/V35-PR");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);
    V35FairRunner.writeRecord(baseline, runs.resolve("baseline-500k-20_2_3_1"),
        canonicalText(false));
    V35FairRunner.writeRecord(full, runs.resolve("full-500k-20_2_3_1"),
        canonicalText(true));

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,frontSize,minCmax,minTEC,minTWC,formalOuterCycles,"
        + "formalQgRounds,cfvfOffspring,caTaLiteFE,formalLocalFE,dscrTeacherUses,"
        + "archiveInsertions\n");
    csv.append(baselineMetrics.csv("baseline")).append('\n');
    csv.append(fullMetrics.csv("full")).append('\n');
    csv.append(cross.csv()).append('\n');
    Files.write(evidence.resolve("PRELIMINARY_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    writeManifest(root, evidence);
  }

  /** Direction check on two further small instances (different stage/factory mixes). */
  @Test(timeout = 1800000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void additionalSmallInstances20_2_4_1And20_5_3_1() throws Exception {
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
    System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", javaProject.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        javaProject.resolve("instance-extensions/v1").toString());

    Path evidence = root.resolve("docs/evidence/V35-PR");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    int[][] instances = {{20, 2, 4, 1}, {20, 5, 3, 1}};
    List<String> rows = new ArrayList<>();
    rows.add("arm,status,FE,frontSize,minCmax,minTEC,minTWC,formalOuterCycles,"
        + "formalQgRounds,cfvfOffspring,caTaLiteFE,formalLocalFE,dscrTeacherUses,"
        + "archiveInsertions");
    for (int[] instance : instances) {
      int jobs = instance[0];
      int stages = instance[1];
      int factories = instance[2];
      String tag = jobs + "_" + stages + "_" + factories + "_1";
      ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          jobs, stages, factories, 1);
      ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
          seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
          ProductionDecodeMode.FM3, SEED);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int i = 0; i < POPULATION; i++) initial.add(seedProblem.createSolution());

      V35FairRunner.RunRecord baseline = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
          newProblem(jobs, stages, factories), initial, BUDGET, SEED);
      V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
          newProblem(jobs, stages, factories), initial, BUDGET, SEED);
      assertEquals("COMPLETED baseline " + tag, "COMPLETED", baseline.getStatus());
      assertEquals("COMPLETED full " + tag, "COMPLETED", full.getStatus());
      assertEquals("controlled start " + tag, baseline.getInitialPopulationHash(),
          full.getInitialPopulationHash());

      ArmMetrics baselineMetrics = ArmMetrics.from(baseline);
      ArmMetrics fullMetrics = ArmMetrics.from(full);
      CrossMetrics cross = CrossMetrics.of(baseline.getFront(), full.getFront());
      V35FairRunner.writeRecord(baseline, runs.resolve("baseline-500k-" + tag),
          canonicalText(false));
      V35FairRunner.writeRecord(full, runs.resolve("full-500k-" + tag),
          canonicalText(true));
      rows.add(baselineMetrics.csv("baseline-" + tag));
      rows.add(fullMetrics.csv("full-" + tag));
      rows.add(cross.row("cross-" + tag));
    }
    Files.write(evidence.resolve("ADDITIONAL_INSTANCES_METRICS.csv"),
        String.join("\n", rows).getBytes(StandardCharsets.UTF_8));
    writeManifest(root, evidence);
  }

  /** Regenerates the SHA-256 manifest over the whole V35-PR evidence directory. */
  private static void writeManifest(Path root, Path evidence) throws Exception {
    Map<String, String> hashes = new TreeMap<>();
    java.util.stream.Stream<Path> walk = Files.walk(evidence);
    walk.filter(Files::isRegularFile)
        .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
        .forEach(path -> {
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(int jobs, int stages, int factories)
      throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        jobs, stages, factories, 1);
    return ZhangBoV35ProblemFactory.create(source.getFatigueInstanceData(),
        source.getFatigueParameters(), ProductionDecodeMode.FM3, SEED);
  }

  private static String canonicalText(boolean full) {
    return V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(BUDGET)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(full).cfvf(full).qg(true).qp(full).caTaLite(full)
        .directionalTeacherPool(full).teacherPoolSize(10).build().canonicalText();
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
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
    String block = summary.substring(start, end);
    String field = key + "=";
    int index = block.indexOf('|' + field);
    if (index < 0) {
      if (!block.startsWith(field)) return -1L;
      index = 0;
    } else {
      index++;
    }
    int valueStart = index + field.length();
    int valueEnd = block.indexOf('|', valueStart);
    if (valueEnd < 0) valueEnd = block.length();
    return Long.parseLong(block.substring(valueStart, valueEnd));
  }

  private static final class ArmMetrics {
    final String status;
    final int fullEvaluations;
    final int frontSize;
    final double minCmax;
    final double minTEC;
    final double minTWC;
    final long formalOuterCycles;
    final long formalQgRounds;
    final long cfvfOffspring;
    final long caTaLiteFE;
    final long formalLocalFE;
    final long dscrTeacherUses;
    final long archiveInsertions;

    ArmMetrics(String status, int fullEvaluations, int frontSize, double minCmax,
        double minTEC, double minTWC, long formalOuterCycles, long formalQgRounds,
        long cfvfOffspring, long caTaLiteFE, long formalLocalFE, long dscrTeacherUses,
        long archiveInsertions) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.frontSize = frontSize; this.minCmax = minCmax; this.minTEC = minTEC;
      this.minTWC = minTWC; this.formalOuterCycles = formalOuterCycles;
      this.formalQgRounds = formalQgRounds; this.cfvfOffspring = cfvfOffspring;
      this.caTaLiteFE = caTaLiteFE; this.formalLocalFE = formalLocalFE;
      this.dscrTeacherUses = dscrTeacherUses; this.archiveInsertions = archiveInsertions;
    }

    static ArmMetrics from(V35FairRunner.RunRecord record) {
      double minCmax = Double.POSITIVE_INFINITY, minTEC = Double.POSITIVE_INFINITY,
          minTWC = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) {
        minCmax = Math.min(minCmax, value[0]);
        minTEC = Math.min(minTEC, value[1]);
        minTWC = Math.min(minTWC, value[2]);
      }
      return new ArmMetrics(record.getStatus(), record.getFullEvaluations(),
          record.getFront().size(), minCmax, minTEC, minTWC,
          summaryLong(record, "formalOuterCycles"), summaryLong(record, "formalQgRounds"),
          summaryLong(record, "cfvfOffspring"), summaryLong(record, "caTaLiteFE"),
          summaryLong(record, "formalLocalFE"), dscrLong(record, "teacherUses"),
          summaryLong(record, "archiveInsertions"));
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%d,%f,%f,%f,%d,%d,%d,%d,%d,%d,%d",
          label, status, fullEvaluations, frontSize, minCmax, minTEC, minTWC,
          formalOuterCycles, formalQgRounds, cfvfOffspring, caTaLiteFE,
          formalLocalFE, dscrTeacherUses, archiveInsertions);
    }
  }

  /** Mutual strict-dominance and union-front statistics between the two fronts. */
  private static final class CrossMetrics {
    final int baselinePointsDominatedByFull;
    final int fullPointsDominatedByBaseline;
    final int unionSize;
    final int fullContribution;
    final int baselineContribution;

    CrossMetrics(int baselinePointsDominatedByFull, int fullPointsDominatedByBaseline,
        int unionSize, int fullContribution, int baselineContribution) {
      this.baselinePointsDominatedByFull = baselinePointsDominatedByFull;
      this.fullPointsDominatedByBaseline = fullPointsDominatedByBaseline;
      this.unionSize = unionSize;
      this.fullContribution = fullContribution;
      this.baselineContribution = baselineContribution;
    }

    static CrossMetrics of(List<double[]> baseline, List<double[]> full) {
      int baselineDominatedByFull = 0;
      for (double[] point : baseline) {
        if (dominatedByAny(point, full)) baselineDominatedByFull++;
      }
      int fullDominatedByBaseline = 0;
      for (double[] point : full) {
        if (dominatedByAny(point, baseline)) fullDominatedByBaseline++;
      }
      List<double[]> union = new ArrayList<>();
      for (double[] point : baseline) {
        if (!dominatedByAny(point, full)) union.add(point);
      }
      int fullContribution = 0;
      for (double[] point : full) {
        if (!dominatedByAny(point, baseline)) {
          union.add(point);
          fullContribution++;
        }
      }
      return new CrossMetrics(baselineDominatedByFull, fullDominatedByBaseline,
          union.size(), fullContribution, union.size() - fullContribution);
    }

    private static boolean dominatedByAny(double[] point, List<double[]> others) {
      for (double[] other : others) {
        boolean strict = false;
        boolean betterOrEqual = true;
        for (int index = 0; index < 3; index++) {
          if (other[index] > point[index]) { betterOrEqual = false; break; }
          if (other[index] < point[index]) strict = true;
        }
        if (betterOrEqual && strict) return true;
      }
      return false;
    }

    String csv() {
      return String.format(Locale.ROOT,
          "cross,baselineDominatedByFull=%d,fullDominatedByBaseline=%d,unionSize=%d,"
              + "fullContribution=%d,baselineContribution=%d",
          baselinePointsDominatedByFull, fullPointsDominatedByBaseline, unionSize,
          fullContribution, baselineContribution);
    }

    String row(String label) {
      return String.format(Locale.ROOT,
          "%s,%d,%d,%d,%d,%d",
          label, baselinePointsDominatedByFull, fullPointsDominatedByBaseline,
          unionSize, fullContribution, baselineContribution);
    }
  }
}
