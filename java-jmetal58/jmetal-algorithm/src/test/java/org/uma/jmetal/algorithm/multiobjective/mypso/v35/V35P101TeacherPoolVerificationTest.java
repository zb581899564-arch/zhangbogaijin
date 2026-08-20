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
 * V35-P10.1 verification: directional top-k teacher pool.
 * Three arms on the same controlled start (instance 20_2_3_1, seed 20260808,
 * population 100, FM3, ShiftMode.NONE):
 *   1. FULL pool-ON  20k  (the improvement)
 *   2. FULL pool-OFF 20k  (must replay the pre-P10.1 FULL evidence bit for bit)
 *   3. FULL pool-ON  100k (audit run: teacher lag, CFVF records, G1 degeneration)
 */
public class V35P101TeacherPoolVerificationTest {
  private static final long SEED = 20260808L;
  private static final String HISTORIC_INITIAL_HASH =
      "07311d31f51e6a71efcbf70435bf8924c02cb8be302023ddeed7f86c2ebca01b";

  private static final class Arm {
    final String label;
    final V35FairRunner.Mode mode;
    final int budget;
    final boolean pool;
    Arm(String label, V35FairRunner.Mode mode, int budget, boolean pool) {
      this.label = label; this.mode = mode; this.budget = budget; this.pool = pool;
    }
  }

  @Test(timeout = 900000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void verifyPoolOnOffAnd100kAudit() throws Exception {
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

    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
        source.getFatigueInstanceData(), source.getFatigueParameters(),
        ProductionDecodeMode.FM3, SEED);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < 100; i++) initial.add(seedProblem.createSolution());

    Path evidence = root.resolve("docs/evidence/V35-P10.1");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    Arm on20k = new Arm("full-20k-pool-on", V35FairRunner.Mode.V35_FULL, 20000, true);
    Arm off20k = new Arm("full-20k-pool-off", V35FairRunner.Mode.V35_FULL_POOL_OFF, 20000, false);
    Arm on100k = new Arm("full-100k-pool-on", V35FairRunner.Mode.V35_FULL, 100000, true);
    List<Arm> arms = Arrays.asList(on20k, off20k, on100k);

    Map<String, Metrics> metrics = new TreeMap<>();
    for (Arm arm : arms) {
      V35FairRunner.RunRecord record = V35FairRunner.run(arm.mode, newProblem(source),
          initial, arm.budget, SEED);
      assertEquals("COMPLETED: " + arm.label, "COMPLETED", record.getStatus());
      assertEquals("controlled start: " + arm.label,
          HISTORIC_INITIAL_HASH, record.getInitialPopulationHash());
      V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
          .seed(SEED).populationSize(initial.size()).maxEvaluations(arm.budget)
          .decoderMode(ProductionDecodeMode.FM3)
          .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
          .directionalTeacherPool(arm.pool).teacherPoolSize(10).build();
      V35FairRunner.writeRecord(record, runs.resolve(arm.label), configuration.canonicalText());
      metrics.put(arm.label, Metrics.from(record));
    }

    // Behaviour isolation: pool OFF must replay the pre-P10.1 FULL front bit for bit.
    Path historicFront = root.resolve("docs/evidence/V35-P10/runs/full-20k/front.csv");
    assertTrue("historic front missing: " + historicFront, Files.exists(historicFront));
    String historic = new String(Files.readAllBytes(historicFront), StandardCharsets.UTF_8);
    assertEquals("pool OFF must replay the pre-P10.1 FULL front bit for bit",
        historic, metrics.get(off20k.label).frontCsv);

    // The pool must actually change the trajectory relative to the legacy front.
    assertFalse("pool ON must diverge from the legacy trajectory",
        historic.equals(metrics.get(on20k.label).frontCsv));

    StringBuilder report = new StringBuilder();
    report.append("instance=20_2_3_1\nseed=").append(SEED)
        .append("\npopulation=100\ndecoder=FM3\nshiftMode=NONE\n")
        .append("directionalTeacherPoolK=10 (FULL arms only)\n")
        .append("poolOffReplaysPreP101FrontBitForBit=true\n\n");
    report.append("arm,budget,frontSize,minCmax,minTEC,minTWC,auditRecords,"
        + "cfvfNewRecords,o1o9Records,taughtRecords,maxTeacherLagFE,meanTeacherLagFE,"
        + "finalBestCmaxGlobal,finalCurrentBestCmaxG1,minCurrentBestCmaxG1\n");
    for (String label : new String[]{on20k.label, off20k.label, on100k.label}) {
      report.append(metrics.get(label).csv(label)).append('\n');
    }
    Files.write(evidence.resolve("IMPROVEMENT_COMPARISON.csv"),
        report.toString().getBytes(StandardCharsets.UTF_8));

    // SHA-256 manifest over every evidence file.
    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("IMPROVEMENT_COMPARISON.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("IMPROVEMENT_COMPARISON.csv")));
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
  private static ZhangBoCanonicalProductionProblem newProblem(ZhangBoEDHHFSPW source) throws Exception {
    ZhangBoEDHHFSPW copy = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    return ZhangBoV35ProblemFactory.create(copy.getFatigueInstanceData(),
        copy.getFatigueParameters(), ProductionDecodeMode.FM3, SEED);
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  /** Compact per-arm metrics extracted from the run record. */
  private static final class Metrics {
    final String frontCsv;
    final int frontSize;
    final double minCmax;
    final double minTEC;
    final double minTWC;
    final int auditRecords;
    final int cfvfNewRecords;
    final int o1o9Records;
    final int taughtRecords;
    final long maxTeacherLagFE;
    final double meanTeacherLagFE;
    final double finalBestCmaxGlobal;
    final double finalCurrentBestCmaxG1;
    final double minCurrentBestCmaxG1;

    Metrics(String frontCsv, int frontSize, double minCmax, double minTEC, double minTWC,
        int auditRecords, int cfvfNewRecords, int o1o9Records, int taughtRecords,
        long maxTeacherLagFE, double meanTeacherLagFE, double finalBestCmaxGlobal,
        double finalCurrentBestCmaxG1, double minCurrentBestCmaxG1) {
      this.frontCsv = frontCsv; this.frontSize = frontSize;
      this.minCmax = minCmax; this.minTEC = minTEC; this.minTWC = minTWC;
      this.auditRecords = auditRecords; this.cfvfNewRecords = cfvfNewRecords;
      this.o1o9Records = o1o9Records; this.taughtRecords = taughtRecords;
      this.maxTeacherLagFE = maxTeacherLagFE; this.meanTeacherLagFE = meanTeacherLagFE;
      this.finalBestCmaxGlobal = finalBestCmaxGlobal;
      this.finalCurrentBestCmaxG1 = finalCurrentBestCmaxG1;
      this.minCurrentBestCmaxG1 = minCurrentBestCmaxG1;
    }

    static Metrics from(V35FairRunner.RunRecord record) {
      StringBuilder front = new StringBuilder("Cmax,TEC,TWC\n");
      double minCmax = Double.POSITIVE_INFINITY, minTEC = Double.POSITIVE_INFINITY,
          minTWC = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) {
        front.append(value[0]).append(',').append(value[1]).append(',').append(value[2]).append('\n');
        minCmax = Math.min(minCmax, value[0]);
        minTEC = Math.min(minTEC, value[1]);
        minTWC = Math.min(minTWC, value[2]);
      }
      String records = record.getCmaxAudit() == null ? "" : record.getCmaxAudit().recordsCsv();
      int auditRecords = 0, cfvf = 0, o1o9 = 0, taught = 0;
      long maxLag = 0, lagSum = 0;
      for (String line : records.split("\n")) {
        if (line.isEmpty() || line.startsWith("candidateId,")) continue;
        String[] columns = line.split(",", -1);
        auditRecords++;
        String mechanism = columns[11];
        String operator = columns[12];
        if ("CFVF".equals(mechanism) || "CFVF".equals(operator)) cfvf++;
        if ("O1_O9".equals(operator) || "FIXED_VNS".equals(mechanism)) o1o9++;
        long evaluation = Long.parseLong(columns[5]);
        String firstTeacherFE = columns[22];
        if (!firstTeacherFE.isEmpty()) {
          long lag = Long.parseLong(firstTeacherFE) - evaluation;
          taught++;
          maxLag = Math.max(maxLag, lag);
          lagSum += lag;
        }
      }
      String curves = record.getCmaxAudit() == null ? "" : record.getCmaxAudit().curvesCsv();
      double finalBest = Double.NaN, finalCurrent = Double.NaN, minCurrent = Double.POSITIVE_INFINITY;
      for (String line : curves.split("\n")) {
        if (line.isEmpty() || line.startsWith("fe,")) continue;
        String[] columns = line.split(",", -1);
        finalBest = Double.parseDouble(columns[1]);
        finalCurrent = Double.parseDouble(columns[3]);
        minCurrent = Math.min(minCurrent, finalCurrent);
      }
      return new Metrics(front.toString(), record.getFront().size(), minCmax, minTEC, minTWC,
          auditRecords, cfvf, o1o9, taught, maxLag,
          taught == 0 ? Double.NaN : (double) lagSum / taught,
          finalBest, finalCurrent, minCurrent);
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%d,%f,%f,%f,%d,%d,%d,%d,%d,%f,%f,%f,%f",
          label, frontSize, minCmax, minTEC, minTWC, auditRecords, cfvfNewRecords,
          o1o9Records, taughtRecords, maxTeacherLagFE, meanTeacherLagFE,
          finalBestCmaxGlobal, finalCurrentBestCmaxG1, minCurrentBestCmaxG1);
    }
  }
}
