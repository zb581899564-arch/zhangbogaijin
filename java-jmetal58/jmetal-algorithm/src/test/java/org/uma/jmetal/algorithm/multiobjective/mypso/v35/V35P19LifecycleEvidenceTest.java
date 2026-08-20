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
 * V35-P19 closure evidence: one FULL 20k run on 20_2_3_1 whose Cmax lifecycle
 * master table must exhibit the complete Generation -> Admission -> Survival ->
 * Exploitation funnel.  With the finish() sweep, no record may remain PENDING;
 * the last-record (6750-class) teacher lifecycle is captured for the report.
 */
public class V35P19LifecycleEvidenceTest {
  private static final long SEED = 20260808L;

  @Test(timeout = 900000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void lifecycleFunnelClosesWithoutPending() throws Exception {
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
    for (int i = 0; i < 100; i++) initial.add(seedProblem.createSolution());

    V35FairRunner.RunRecord record = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        newProblem(), initial, 20000, SEED);
    assertEquals("COMPLETED", "COMPLETED", record.getStatus());
    assertNotNull(record.getCmaxAudit());

    LifecycleMetrics metrics = LifecycleMetrics.from(record);
    // Four-stage funnel, fully resolved.
    assertTrue("records must exist", metrics.records > 0);
    assertEquals("all records generated", metrics.records, metrics.generatedRows);
    assertEquals("admitted == enteredCandidateSet on every record",
        metrics.records, metrics.admittedConsistentRows);
    assertEquals("no PENDING after finish() sweep", 0, metrics.pendingRows);
    assertTrue("resolvedPendingByFinish must be reported",
        record.getCmaxAudit().summaryText().contains("resolvedPendingByFinish="));
    // Survival stage actually exercised.
    assertTrue("at least one PDDR-retained record", metrics.pddrRetainedRows > 0);
    assertTrue("at least one global-archive record", metrics.globalArchiveRows > 0);
    // Exploitation stage columns present (values are captured in the report).
    assertTrue("teacher lifecycle columns present",
        record.getCmaxAudit().recordsCsv().contains("g1SocialTeacherParticleUses"));

    Path evidence = root.resolve("docs/evidence/V35-P19");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(100).maxEvaluations(20000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .directionalTeacherPool(true).teacherPoolSize(10).build();
    V35FairRunner.writeRecord(record, runs.resolve("full-20k-20_2_3_1"),
        configuration.canonicalText());

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,records,generatedRows,admittedConsistentRows,pendingRows,"
        + "notSelectedRows,yesRows,noRows,pddrRetainedRows,personalArchiveRows,"
        + "globalArchiveRows,lastRecordFE,lastRecordCmax,lastRecordSocialUses,"
        + "lastRecordPersonalUses,lastRecordFirstTeacherFE,resolvedPendingByFinish,"
        + "frontSize,minCmax\n");
    csv.append(metrics.csv("full-20k-20_2_3_1")).append('\n');
    Files.write(evidence.resolve("LIFECYCLE_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("LIFECYCLE_METRICS.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("LIFECYCLE_METRICS.csv")));
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
  private static ZhangBoCanonicalProductionProblem newProblem() throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
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

  /** Lifecycle funnel metrics parsed from the audit master table. */
  private static final class LifecycleMetrics {
    final String status;
    final int fullEvaluations;
    final int records;
    final int generatedRows;
    final int admittedConsistentRows;
    final int pendingRows;
    final int notSelectedRows;
    final int yesRows;
    final int noRows;
    final int pddrRetainedRows;
    final int personalArchiveRows;
    final int globalArchiveRows;
    final long lastRecordFE;
    final double lastRecordCmax;
    final long lastRecordSocialUses;
    final long lastRecordPersonalUses;
    final long lastRecordFirstTeacherFE;
    final long resolvedPendingByFinish;
    final int frontSize;
    final double minCmax;

    LifecycleMetrics(String status, int fullEvaluations, int records, int generatedRows,
        int admittedConsistentRows, int pendingRows, int notSelectedRows, int yesRows,
        int noRows, int pddrRetainedRows, int personalArchiveRows, int globalArchiveRows,
        long lastRecordFE, double lastRecordCmax, long lastRecordSocialUses,
        long lastRecordPersonalUses, long lastRecordFirstTeacherFE,
        long resolvedPendingByFinish, int frontSize, double minCmax) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.records = records; this.generatedRows = generatedRows;
      this.admittedConsistentRows = admittedConsistentRows;
      this.pendingRows = pendingRows; this.notSelectedRows = notSelectedRows;
      this.yesRows = yesRows; this.noRows = noRows;
      this.pddrRetainedRows = pddrRetainedRows;
      this.personalArchiveRows = personalArchiveRows;
      this.globalArchiveRows = globalArchiveRows;
      this.lastRecordFE = lastRecordFE; this.lastRecordCmax = lastRecordCmax;
      this.lastRecordSocialUses = lastRecordSocialUses;
      this.lastRecordPersonalUses = lastRecordPersonalUses;
      this.lastRecordFirstTeacherFE = lastRecordFirstTeacherFE;
      this.resolvedPendingByFinish = resolvedPendingByFinish;
      this.frontSize = frontSize; this.minCmax = minCmax;
    }

    static LifecycleMetrics from(V35FairRunner.RunRecord record) {
      int records = 0, generatedRows = 0, admittedConsistentRows = 0, pendingRows = 0;
      int notSelectedRows = 0, yesRows = 0, noRows = 0, pddrRetainedRows = 0;
      int personalArchiveRows = 0, globalArchiveRows = 0;
      long lastFE = -1L, lastSocial = 0L, lastPersonal = 0L, lastFirstTeacherFE = -1L;
      double lastCmax = Double.NaN;
      for (String line : record.getCmaxAudit().recordsCsv().split("\n")) {
        if (line.isEmpty() || line.startsWith("candidateId,")) continue;
        String[] columns = line.split(",", -1);
        records++;
        if ("true".equals(columns[3])) generatedRows++;
        if (columns[4].equals(columns[13])) admittedConsistentRows++;
        String survival = columns[17];
        if ("PENDING".equals(survival)) pendingRows++;
        else if ("NOT_SELECTED".equals(survival)) notSelectedRows++;
        else if ("YES".equals(survival)) yesRows++;
        else if ("NO".equals(survival)) noRows++;
        if ("true".equals(columns[14])) pddrRetainedRows++;
        if ("true".equals(columns[15])) personalArchiveRows++;
        if ("true".equals(columns[16])) globalArchiveRows++;
        long evaluation = Long.parseLong(columns[5]);
        if (evaluation >= lastFE) {
          lastFE = evaluation;
          lastCmax = Double.parseDouble(columns[7]);
          lastSocial = Long.parseLong(columns[18]);
          lastPersonal = Long.parseLong(columns[20]);
          lastFirstTeacherFE = columns[22].isEmpty() ? -1L : Long.parseLong(columns[22]);
        }
      }
      double minCmax = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) minCmax = Math.min(minCmax, value[0]);
      long resolved = record.getCmaxAudit().getResolvedPendingByFinish();
      return new LifecycleMetrics(record.getStatus(), record.getFullEvaluations(),
          records, generatedRows, admittedConsistentRows, pendingRows, notSelectedRows,
          yesRows, noRows, pddrRetainedRows, personalArchiveRows, globalArchiveRows,
          lastFE, lastCmax, lastSocial, lastPersonal, lastFirstTeacherFE, resolved,
          record.getFront().size(), minCmax);
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%f,%d,%d,%d,%d,%d,%f",
          label, status, fullEvaluations, records, generatedRows,
          admittedConsistentRows, pendingRows, notSelectedRows, yesRows, noRows,
          pddrRetainedRows, personalArchiveRows, globalArchiveRows, lastRecordFE,
          lastRecordCmax, lastRecordSocialUses, lastRecordPersonalUses,
          lastRecordFirstTeacherFE, resolvedPendingByFinish, frontSize, minCmax);
    }
  }
}
