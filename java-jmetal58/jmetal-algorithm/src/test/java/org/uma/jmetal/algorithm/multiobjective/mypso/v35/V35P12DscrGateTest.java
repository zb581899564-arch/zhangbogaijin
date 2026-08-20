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
 * V35-P12 formal gate: DTUR=0 AND no post-action override, verified on real
 * FULL runs (DSCR active) on two instances:
 *   1. 20_2_3_1, 100 particles, 20 000 FE
 *   2. I1 10_2_2_1, 10 particles, 5 000 FE
 * For each run: every recorded teacher use must be nondominated
 * (dominated=false, dominatorCount=0), the DTUR summary must be 0, and every
 * DSCR replacement in the event log must have been flagged stale.
 */
public class V35P12DscrGateTest {
  private static final long SEED = 20260808L;

  private static final class ArmSpec {
    final String label;
    final int jobs;
    final int stages;
    final int factories;
    final int population;
    final int budget;
    ArmSpec(String label, int jobs, int stages, int factories, int population, int budget) {
      this.label = label; this.jobs = jobs; this.stages = stages;
      this.factories = factories; this.population = population; this.budget = budget;
    }
  }

  @Test(timeout = 900000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void dscrGateHoldsOnBothInstances() throws Exception {
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

    ArmSpec arm20k = new ArmSpec("full-20k-20_2_3_1", 20, 2, 3, 100, 20000);
    ArmSpec armI1 = new ArmSpec("full-5k-I1-10_2_2_1", 10, 2, 2, 10, 5000);

    Path evidence = root.resolve("docs/evidence/V35-P12");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    Map<String, GateMetrics> metrics = new TreeMap<>();
    for (ArmSpec arm : Arrays.asList(arm20k, armI1)) {
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
          newProblem(arm), initial, arm.budget, SEED);
      assertEquals("COMPLETED " + arm.label, "COMPLETED", record.getStatus());

      GateMetrics gate = GateMetrics.from(record);
      // Terminal gate: DTUR must be exactly zero and defined.
      assertTrue("dturDefined " + arm.label + ": " + record.getMechanismSummary(),
          record.getMechanismSummary().contains("dturDefined=true"));
      assertEquals("dominatedTeacherUses must be 0 " + arm.label,
          0L, gate.dominatedTeacherUses);
      assertEquals("dtur must be 0 " + arm.label, 0.0, gate.dtur, 0.0);
      // Per-use gate: every recorded teacher use must be clean.
      assertEquals("all teacher-use rows clean " + arm.label, gate.useRows, gate.cleanUseRows);
      assertTrue("teacher uses must be recorded " + arm.label, gate.useRows > 0);
      // Ledger invariant: every replacement in the event log must be stale.
      assertEquals("all replaced rows stale " + arm.label, gate.replacedRows, gate.staleReplacedRows);
      // CSV agrees with the summary counters (ledger completeness).
      assertEquals("use rows == teacherUses " + arm.label, gate.teacherUses, gate.useRows);
      assertEquals("event rows == validityChecks " + arm.label,
          gate.validityChecks, gate.eventRows);
      assertEquals("replaced rows == replacements " + arm.label,
          gate.replacements, gate.replacedRows);

      V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
          .seed(SEED).populationSize(arm.population).maxEvaluations(arm.budget)
          .decoderMode(ProductionDecodeMode.FM3)
          .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
          .directionalTeacherPool(true).teacherPoolSize(10).build();
      V35FairRunner.writeRecord(record, runs.resolve(arm.label), configuration.canonicalText());
      metrics.put(arm.label, gate);
    }

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,teacherUses,dominatedTeacherUses,dtur,dturDefined,"
        + "validityChecks,replacements,scrr,useRows,cleanUseRows,eventRows,"
        + "replacedRows,staleReplacedRows,frontSize,minCmax\n");
    for (Map.Entry<String, GateMetrics> entry : metrics.entrySet()) {
      csv.append(entry.getValue().csv(entry.getKey())).append('\n');
    }
    Files.write(evidence.resolve("GATE_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // SHA-256 manifest over every evidence file.
    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("GATE_METRICS.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("GATE_METRICS.csv")));
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

  /** Gate metrics extracted from a FULL run record. */
  private static final class GateMetrics {
    final String status;
    final int fullEvaluations;
    final long teacherUses;
    final long dominatedTeacherUses;
    final double dtur;
    final boolean dturDefined;
    final long validityChecks;
    final long replacements;
    final double scrr;
    final int useRows;
    final int cleanUseRows;
    final int eventRows;
    final int replacedRows;
    final int staleReplacedRows;
    final int frontSize;
    final double minCmax;

    GateMetrics(String status, int fullEvaluations, long teacherUses,
        long dominatedTeacherUses, double dtur, boolean dturDefined,
        long validityChecks, long replacements, double scrr, int useRows,
        int cleanUseRows, int eventRows, int replacedRows, int staleReplacedRows,
        int frontSize, double minCmax) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.teacherUses = teacherUses; this.dominatedTeacherUses = dominatedTeacherUses;
      this.dtur = dtur; this.dturDefined = dturDefined;
      this.validityChecks = validityChecks; this.replacements = replacements;
      this.scrr = scrr; this.useRows = useRows; this.cleanUseRows = cleanUseRows;
      this.eventRows = eventRows; this.replacedRows = replacedRows;
      this.staleReplacedRows = staleReplacedRows;
      this.frontSize = frontSize; this.minCmax = minCmax;
    }

    static GateMetrics from(V35FairRunner.RunRecord record) {
      double minCmax = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) minCmax = Math.min(minCmax, value[0]);
      long teacherUses = dscrLong(record, "teacherUses");
      long dominatedTeacherUses = dscrLong(record, "dominatedTeacherUses");
      long validityChecks = dscrLong(record, "validityChecks");
      long replacements = dscrLong(record, "replacements");
      double dtur = dscrDouble(record, "dtur");
      double scrr = dscrDouble(record, "scrr");
      boolean dturDefined = record.getMechanismSummary().contains("dturDefined=true");

      int useRows = 0, cleanUseRows = 0;
      for (String line : record.getDscrTeacherUses().split("\n")) {
        if (line.isEmpty() || line.startsWith("decisionCycle,")) continue;
        // The teacher fingerprint embeds commas, so only the tail is indexable:
        // the last two fields are dominated, dominatorCount.
        String[] columns = line.split(",", -1);
        useRows++;
        if ("false".equals(columns[columns.length - 2])
            && "0".equals(columns[columns.length - 1])) {
          cleanUseRows++;
        }
      }
      int eventRows = 0, replacedRows = 0, staleReplacedRows = 0;
      // Objectives fields are semicolon-joined triples; fingerprints embed commas,
      // so the fingerprint columns are reconstructed by scanning between the two
      // objective fields instead of by fixed index.
      java.util.regex.Pattern objectives = java.util.regex.Pattern.compile(
          "-?[0-9]+(\\.[0-9]+)?;-?[0-9]+(\\.[0-9]+)?;-?[0-9]+(\\.[0-9]+)?");
      for (String line : record.getDscrEvents().split("\n")) {
        if (line.isEmpty() || line.startsWith("decisionCycle,")) continue;
        String[] columns = line.split(",", -1);
        eventRows++;
        int beforeObjectives = 5;
        while (beforeObjectives < columns.length
            && !objectives.matcher(columns[beforeObjectives]).matches()) {
          beforeObjectives++;
        }
        if (beforeObjectives + 3 >= columns.length) continue;
        String oldLeaderId = join(columns, 5, beforeObjectives);
        String stale = columns[beforeObjectives + 2];
        int afterObjectives = beforeObjectives + 3;
        while (afterObjectives < columns.length
            && !objectives.matcher(columns[afterObjectives]).matches()) {
          afterObjectives++;
        }
        String replacementId = join(columns, beforeObjectives + 3, afterObjectives);
        boolean replaced = !oldLeaderId.equals(replacementId);
        if (replaced) {
          replacedRows++;
          if ("true".equals(stale)) staleReplacedRows++;
        }
      }
      return new GateMetrics(record.getStatus(), record.getFullEvaluations(),
          teacherUses, dominatedTeacherUses, dtur, dturDefined, validityChecks,
          replacements, scrr, useRows, cleanUseRows, eventRows, replacedRows,
          staleReplacedRows, record.getFront().size(), minCmax);
    }

    private static String join(String[] columns, int from, int to) {
      StringBuilder out = new StringBuilder();
      for (int index = from; index < to; index++) {
        if (out.length() > 0) out.append(',');
        out.append(columns[index]);
      }
      return out.toString();
    }

    private static String dscrBlock(V35FairRunner.RunRecord record) {
      String summary = record.getMechanismSummary();
      String marker = "dscr=";
      int start = summary.indexOf(marker);
      if (start < 0) return "";
      start += marker.length();
      int end = summary.indexOf(",algorithmRunNanos=", start);
      if (end < 0) end = summary.length();
      return summary.substring(start, end);
    }

    private static long dscrLong(V35FairRunner.RunRecord record, String key) {
      String block = dscrBlock(record);
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

    private static double dscrDouble(V35FairRunner.RunRecord record, String key) {
      String block = dscrBlock(record);
      String field = key + "=";
      int index = block.indexOf('|' + field);
      if (index < 0) {
        if (!block.startsWith(field)) return Double.NaN;
        index = 0;
      } else {
        index++;
      }
      int valueStart = index + field.length();
      int valueEnd = block.indexOf('|', valueStart);
      if (valueEnd < 0) valueEnd = block.length();
      return Double.parseDouble(block.substring(valueStart, valueEnd));
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%d,%d,%f,%s,%d,%d,%f,%d,%d,%d,%d,%d,%d,%f",
          label, status, fullEvaluations, teacherUses, dominatedTeacherUses, dtur,
          dturDefined, validityChecks, replacements, scrr, useRows, cleanUseRows,
          eventRows, replacedRows, staleReplacedRows, frontSize, minCmax);
    }
  }
}
