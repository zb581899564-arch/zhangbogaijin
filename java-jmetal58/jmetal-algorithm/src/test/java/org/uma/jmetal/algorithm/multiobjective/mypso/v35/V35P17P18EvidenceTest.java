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
 * V35-P17/P18 evidence:
 *   - read-only isolation proof: the same FULL run with and without the passive
 *     archive must produce bit-identical fronts (the archive is a pure bypass);
 *   - the archive export must be pairwise nondominated and observe every
 *     successful evaluation;
 *   - the three-objective best-ever fields must be present in the audit output.
 */
public class V35P17P18EvidenceTest {
  private static final long SEED = 20260808L;

  private static final class ArmSpec {
    final String label;
    final int jobs;
    final int stages;
    final int factories;
    final int population;
    final int budget;
    final boolean passive;
    ArmSpec(String label, int jobs, int stages, int factories, int population,
        int budget, boolean passive) {
      this.label = label; this.jobs = jobs; this.stages = stages;
      this.factories = factories; this.population = population;
      this.budget = budget; this.passive = passive;
    }
  }

  @Test(timeout = 900000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void passiveIsolationAndBestEverEvidence() throws Exception {
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

    ArmSpec with20k = new ArmSpec("full-20k-with", 20, 2, 3, 100, 20000, true);
    ArmSpec without20k = new ArmSpec("full-20k-without", 20, 2, 3, 100, 20000, false);
    ArmSpec withI1 = new ArmSpec("full-5k-I1-with", 10, 2, 2, 10, 5000, true);

    Path evidence = root.resolve("docs/evidence/V35-P17");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    Map<String, ArmMetrics> metrics = new TreeMap<>();
    for (ArmSpec arm : Arrays.asList(with20k, without20k, withI1)) {
      boolean i1 = arm.label.startsWith("full-5k-I1");
      Path dataDir = i1 ? bridge.resolve("EADHFSP") : javaProject.resolve("EADHFSP");
      Path fatigueDir = i1 ? bridge.resolve("fatigue-parameters/v1")
          : javaProject.resolve("fatigue-parameters/v1");
      Path extensionDir = i1 ? bridge.resolve("instance-extensions/v1")
          : javaProject.resolve("instance-extensions/v1");
      System.setProperty("dhfsp.data.dir", dataDir.toString());
      System.setProperty("dhfsp.fatigue.dir", fatigueDir.toString());
      System.setProperty("dhfsp.instance.extension.dir", extensionDir.toString());

      ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          arm.jobs, arm.stages, arm.factories, 1);
      ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
          seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
          ProductionDecodeMode.FM3, SEED);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int i = 0; i < arm.population; i++) initial.add(seedProblem.createSolution());

      V35FairRunner.RunRecord record = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
          newProblem(arm), initial, arm.budget, SEED, arm.passive);
      assertEquals("COMPLETED " + arm.label, "COMPLETED", record.getStatus());

      ArmMetrics metricsForArm = ArmMetrics.from(record, arm.passive);
      if (arm.passive) {
        // The passive archive observes every successful evaluation and stays
        // pairwise nondominated.
        assertEquals("observedCount == fullEvaluations " + arm.label,
            record.getFullEvaluations(), metricsForArm.observedCount);
        assertEquals("archive pairwise nondominated " + arm.label,
            metricsForArm.rows, metricsForArm.nondominatedRows);
        assertTrue("archive non-empty " + arm.label, metricsForArm.rows > 0);
      }
      // Three-objective best-ever present in the audit output.
      assertTrue("best-ever summary " + arm.label,
          record.getCmaxAudit().summaryText().contains("bestTECGenerated=")
              && record.getCmaxAudit().summaryText().contains("bestTWCGenerated="));
      assertTrue("best-ever sources " + arm.label,
          !record.getCmaxAudit().getBestCmaxSource().isEmpty()
              && !record.getCmaxAudit().getBestTECSource().isEmpty()
              && !record.getCmaxAudit().getBestTWCSource().isEmpty());
      assertTrue("best-ever curves " + arm.label,
          record.getCmaxAudit().curvesCsv().contains("bestTECGlobal,bestTWCGlobal"));

      V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
          .seed(SEED).populationSize(arm.population).maxEvaluations(arm.budget)
          .decoderMode(ProductionDecodeMode.FM3)
          .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
          .directionalTeacherPool(true).teacherPoolSize(10).build();
      V35FairRunner.writeRecord(record, runs.resolve(arm.label), configuration.canonicalText());
      metrics.put(arm.label, metricsForArm);
    }

    // Read-only isolation proof: with vs without the archive the front is
    // bit-identical, so the archive is a pure bypass that never feeds the search.
    assertEquals("passive archive must not change the front (bit-identical)",
        metrics.get(with20k.label).frontCsv, metrics.get(without20k.label).frontCsv);

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,passive,observedCount,archiveSize,rows,nondominatedRows,"
        + "bestCmax,bestTEC,bestTWC,bestTECGlobal,bestTWCGlobal,frontSize,minCmax\n");
    for (String label : new String[]{with20k.label, without20k.label, withI1.label}) {
      csv.append(metrics.get(label).csv(label)).append('\n');
    }
    Files.write(evidence.resolve("PASSIVE_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // Best-ever metrics into the V35-P18 evidence directory.
    Path p18 = root.resolve("docs/evidence/V35-P18");
    Files.createDirectories(p18);
    StringBuilder bestEver = new StringBuilder();
    bestEver.append("arm,bestCmaxGenerated,bestTECGenerated,bestTWCGenerated,"
        + "bestCmaxGlobal,bestTECGlobal,bestTWCGlobal,bestCmaxSource,bestTECSource,"
        + "bestTWCSource\n");
    for (String label : new String[]{with20k.label, withI1.label}) {
      bestEver.append(metrics.get(label).bestEverCsv(label)).append('\n');
    }
    Files.write(p18.resolve("BEST_EVER_METRICS.csv"),
        bestEver.toString().getBytes(StandardCharsets.UTF_8));

    // SHA-256 manifest over every P17 evidence file.
    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("PASSIVE_METRICS.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("PASSIVE_METRICS.csv")));
    java.util.stream.Stream<Path> walk = Files.walk(runs);
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(ArmSpec arm) throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        arm.jobs, arm.stages, arm.factories, 1);
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

  private static final class ArmMetrics {
    final String status;
    final int fullEvaluations;
    final long observedCount;
    final int archiveSize;
    final int rows;
    final int nondominatedRows;
    final double bestTEC;
    final double bestTWC;
    final double bestTECGlobal;
    final double bestTWCGlobal;
    final String bestCmaxSource;
    final String bestTECSource;
    final String bestTWCSource;
    final String frontCsv;
    final int frontSize;
    final double minCmax;

    ArmMetrics(String status, int fullEvaluations, long observedCount, int archiveSize,
        int rows, int nondominatedRows, double bestTEC, double bestTWC,
        double bestTECGlobal, double bestTWCGlobal, String bestCmaxSource,
        String bestTECSource, String bestTWCSource, String frontCsv, int frontSize,
        double minCmax) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.observedCount = observedCount; this.archiveSize = archiveSize;
      this.rows = rows; this.nondominatedRows = nondominatedRows;
      this.bestTEC = bestTEC; this.bestTWC = bestTWC;
      this.bestTECGlobal = bestTECGlobal; this.bestTWCGlobal = bestTWCGlobal;
      this.bestCmaxSource = bestCmaxSource; this.bestTECSource = bestTECSource;
      this.bestTWCSource = bestTWCSource;
      this.frontCsv = frontCsv; this.frontSize = frontSize; this.minCmax = minCmax;
    }

    static ArmMetrics from(V35FairRunner.RunRecord record, boolean passive) {
      int rows = 0, nondominatedRows = 0;
      List<double[]> points = new ArrayList<>();
      if (passive) {
        for (String line : record.getPassiveArchiveCsv().split("\n")) {
          if (line.isEmpty() || line.startsWith("Cmax,")) continue;
          String[] columns = line.split(",", -1);
          points.add(new double[]{Double.parseDouble(columns[0]),
              Double.parseDouble(columns[1]), Double.parseDouble(columns[2])});
          rows++;
        }
        for (int left = 0; left < points.size(); left++) {
          boolean dominated = false;
          for (int right = 0; right < points.size() && !dominated; right++) {
            if (left == right) continue;
            double[] a = points.get(left);
            double[] b = points.get(right);
            boolean strict = false;
            boolean betterOrEqual = true;
            for (int index = 0; index < 3; index++) {
              if (b[index] > a[index]) { betterOrEqual = false; break; }
              if (b[index] < a[index]) strict = true;
            }
            if (betterOrEqual && strict) dominated = true;
          }
          if (!dominated) nondominatedRows++;
        }
      }
      StringBuilder front = new StringBuilder("Cmax,TEC,TWC\n");
      double minCmax = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) {
        front.append(value[0]).append(',').append(value[1]).append(',').append(value[2]).append('\n');
        minCmax = Math.min(minCmax, value[0]);
      }
      return new ArmMetrics(record.getStatus(), record.getFullEvaluations(),
          record.getPassiveObservedCount(), record.getPassiveArchiveSize(),
          rows, nondominatedRows, record.getCmaxAudit().getBestTEC(),
          record.getCmaxAudit().getBestTWC(),
          record.getCmaxAudit().getCurrentTECGlobal(),
          record.getCmaxAudit().getCurrentTWCGlobal(),
          record.getCmaxAudit().getBestCmaxSource(),
          record.getCmaxAudit().getBestTECSource(),
          record.getCmaxAudit().getBestTWCSource(),
          front.toString(), record.getFront().size(), minCmax);
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%s,%d,%d,%d,%d,%f,%f,%f,%f,%d,%f",
          label, status, fullEvaluations, observedCount > 0 ? "true" : "false",
          observedCount, archiveSize, rows, nondominatedRows, bestTEC, bestTWC,
          bestTECGlobal, bestTWCGlobal, frontSize, minCmax);
    }

    String bestEverCsv(String label) {
      return String.format(Locale.ROOT,
          "%s,%f,%f,%f,%f,%f,%f,%s,%s,%s",
          label, Double.NaN, bestTEC, bestTWC, Double.NaN, bestTECGlobal,
          bestTWCGlobal, bestCmaxSource, bestTECSource, bestTWCSource);
    }
  }
}
