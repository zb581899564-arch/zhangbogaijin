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
 * V35-P11 closure: QG0/QG1 single-variable pairing plus the I1 mechanism-chain
 * recheck.  Two pairs on the same controlled start:
 *   1. 20_2_3_1, 100 particles, 20 000 FE  (the engineering pairing)
 *   2. I1 10_2_2_1, 10 particles, 5 000 FE (the I1 chain recheck)
 * For each pair both arms share one initial population and differ only in the
 * DSCR switch; the Qg chain must execute in both, QG1 must hold DTUR=0, and the
 * teacher lifecycle columns must be populated (6750-type recheck).
 */
public class V35P11QgPairClosureTest {
  private static final long SEED = 20260808L;

  private static final class PairSpec {
    final String label;
    final int jobs;
    final int stages;
    final int factories;
    final int population;
    final int budget;
    PairSpec(String label, int jobs, int stages, int factories, int population, int budget) {
      this.label = label; this.jobs = jobs; this.stages = stages;
      this.factories = factories; this.population = population; this.budget = budget;
    }
  }

  @Test(timeout = 900000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void closeQgPairingWithI1ChainRecheck() throws Exception {
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

    PairSpec pair20k = new PairSpec("20k-20_2_3_1", 20, 2, 3, 100, 20000);
    PairSpec pairI1 = new PairSpec("5k-I1-10_2_2_1", 10, 2, 2, 10, 5000);

    Path evidence = root.resolve("docs/evidence/V35-P11");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    Map<String, ArmMetrics> metrics = new TreeMap<>();
    for (PairSpec pair : Arrays.asList(pair20k, pairI1)) {
      Path dataDir = pair.label.startsWith("5k-I1") ? bridge.resolve("EADHFSP") : javaProject.resolve("EADHFSP");
      Path fatigueDir = pair.label.startsWith("5k-I1") ? bridge.resolve("fatigue-parameters/v1")
          : javaProject.resolve("fatigue-parameters/v1");
      Path extensionDir = pair.label.startsWith("5k-I1") ? bridge.resolve("instance-extensions/v1")
          : javaProject.resolve("instance-extensions/v1");
      System.setProperty("dhfsp.data.dir", dataDir.toString());
      System.setProperty("dhfsp.fatigue.dir", fatigueDir.toString());
      System.setProperty("dhfsp.instance.extension.dir", extensionDir.toString());

      ZhangBoEDHHFSPW seedSource = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          pair.jobs, pair.stages, pair.factories, 1);
      ZhangBoCanonicalProductionProblem seedProblem = ZhangBoV35ProblemFactory.create(
          seedSource.getFatigueInstanceData(), seedSource.getFatigueParameters(),
          ProductionDecodeMode.FM3, SEED);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int i = 0; i < pair.population; i++) initial.add(seedProblem.createSolution());

      V35FairRunner.RunRecord qg0 = V35FairRunner.run(V35FairRunner.Mode.V35_QG0,
          newProblem(pair), initial, pair.budget, SEED);
      V35FairRunner.RunRecord qg1 = V35FairRunner.run(V35FairRunner.Mode.V35_QG1,
          newProblem(pair), initial, pair.budget, SEED);

      assertEquals("COMPLETED qg0 " + pair.label, "COMPLETED", qg0.getStatus());
      assertEquals("COMPLETED qg1 " + pair.label, "COMPLETED", qg1.getStatus());
      assertEquals("same controlled start " + pair.label,
          qg0.getInitialPopulationHash(), qg1.getInitialPopulationHash());

      // Qg chain executes in both arms.
      assertTrue("QG0 Qg rounds > 0: " + qg0.getMechanismSummary(),
          summaryLong(qg0, "formalQgRounds") > 0L);
      assertTrue("QG1 Qg rounds > 0: " + qg1.getMechanismSummary(),
          summaryLong(qg1, "formalQgRounds") > 0L);

      // Single-variable discipline: the two configurations differ only in dscr.
      assertEquals("configs must differ only in dscr " + pair.label,
          canonicalText(pair, false), canonicalText(pair, true));
      assertTrue(qg0.getMechanismSummary().contains("dscr=disabled"));
      assertTrue(qg1.getMechanismSummary().contains("schema=v35-dscr-metrics-v2"));

      // QG1 DSCR chain: real teacher uses with the dominance gate held.
      ArmMetrics qg0Metrics = ArmMetrics.from(qg0, pair.label + "-qg0");
      ArmMetrics qg1Metrics = ArmMetrics.from(qg1, pair.label + "-qg1");
      assertTrue("QG1 must have teacher uses: " + qg1.getMechanismSummary(),
          qg1Metrics.teacherUses > 0);
      assertTrue("QG1 DTUR gate must hold: " + qg1.getMechanismSummary(),
          qg1Metrics.dominatedTeacherUses == 0);
      assertTrue("QG1 teacher lifecycle columns must be populated: " + pair.label,
          qg1Metrics.auditRecords > 0 && qg1Metrics.lifecycleColumnsPresent);

      V35FairRunner.writeRecord(qg0, runs.resolve(pair.label + "-qg0"),
          canonicalText(pair, false));
      V35FairRunner.writeRecord(qg1, runs.resolve(pair.label + "-qg1"),
          canonicalText(pair, true));
      metrics.put(pair.label + "-qg0", qg0Metrics);
      metrics.put(pair.label + "-qg1", qg1Metrics);
    }

    // Metrics CSV.
    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,frontSize,minCmax,minTEC,minTWC,auditRecords,"
        + "socialTaughtRecords,personalOnlyRecords,lastRecordFE,lastRecordSocialUses,"
        + "lastRecordPersonalUses,teacherUses,dominatedTeacherUses,validityChecks,"
        + "replacements,scrr,formalQgRounds,p6EventsTotal\n");
    for (Map.Entry<String, ArmMetrics> entry : metrics.entrySet()) {
      csv.append(entry.getValue().csv(entry.getKey())).append('\n');
    }
    Files.write(evidence.resolve("QG_PAIR_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // SHA-256 manifest over every evidence file.
    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("QG_PAIR_METRICS.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("QG_PAIR_METRICS.csv")));
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

  /** Canonical configuration text with the dscr flag fixed, for the single-variable proof. */
  private static String canonicalText(PairSpec pair, boolean dscr) {
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(pair.population).maxEvaluations(pair.budget)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(dscr).cfvf(false).qg(true).qp(false).caTaLite(false)
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    String text = configuration.canonicalText();
    return text.replace("dscr=" + dscr + '\n', "dscr=FIXED\n");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ZhangBoCanonicalProductionProblem newProblem(PairSpec pair) throws Exception {
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        pair.jobs, pair.stages, pair.factories, 1);
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

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  /** Per-arm metrics including the teacher lifecycle (6750-type) fields. */
  private static final class ArmMetrics {
    final String status;
    final int fullEvaluations;
    final int frontSize;
    final double minCmax;
    final double minTEC;
    final double minTWC;
    final int auditRecords;
    final int socialTaughtRecords;
    final int personalOnlyRecords;
    final long lastRecordFE;
    final long lastRecordSocialUses;
    final long lastRecordPersonalUses;
    final boolean lifecycleColumnsPresent;
    final long teacherUses;
    final long dominatedTeacherUses;
    final long validityChecks;
    final long replacements;
    final double scrr;
    final long formalQgRounds;
    final long p6EventsTotal;

    ArmMetrics(String status, int fullEvaluations, int frontSize, double minCmax,
        double minTEC, double minTWC, int auditRecords, int socialTaughtRecords,
        int personalOnlyRecords, long lastRecordFE, long lastRecordSocialUses,
        long lastRecordPersonalUses, boolean lifecycleColumnsPresent, long teacherUses,
        long dominatedTeacherUses, long validityChecks, long replacements, double scrr,
        long formalQgRounds, long p6EventsTotal) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.frontSize = frontSize; this.minCmax = minCmax; this.minTEC = minTEC;
      this.minTWC = minTWC; this.auditRecords = auditRecords;
      this.socialTaughtRecords = socialTaughtRecords;
      this.personalOnlyRecords = personalOnlyRecords; this.lastRecordFE = lastRecordFE;
      this.lastRecordSocialUses = lastRecordSocialUses;
      this.lastRecordPersonalUses = lastRecordPersonalUses;
      this.lifecycleColumnsPresent = lifecycleColumnsPresent;
      this.teacherUses = teacherUses; this.dominatedTeacherUses = dominatedTeacherUses;
      this.validityChecks = validityChecks; this.replacements = replacements;
      this.scrr = scrr; this.formalQgRounds = formalQgRounds;
      this.p6EventsTotal = p6EventsTotal;
    }

    static ArmMetrics from(V35FairRunner.RunRecord record, String label) {
      double minCmax = Double.POSITIVE_INFINITY, minTEC = Double.POSITIVE_INFINITY,
          minTWC = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) {
        minCmax = Math.min(minCmax, value[0]);
        minTEC = Math.min(minTEC, value[1]);
        minTWC = Math.min(minTWC, value[2]);
      }
      String records = record.getCmaxAudit() == null ? "" : record.getCmaxAudit().recordsCsv();
      boolean lifecycleColumnsPresent = records.contains("g1SocialTeacherParticleUses");
      int auditRecords = 0, socialTaught = 0, personalOnly = 0;
      long lastFE = -1L, lastSocial = 0L, lastPersonal = 0L;
      for (String line : records.split("\n")) {
        if (line.isEmpty() || line.startsWith("candidateId,")) continue;
        String[] columns = line.split(",", -1);
        auditRecords++;
        long social = Long.parseLong(columns[18]);
        long personal = Long.parseLong(columns[20]);
        if (social > 0) socialTaught++;
        if (personal > 0 && social == 0) personalOnly++;
        long evaluation = Long.parseLong(columns[5]);
        if (evaluation >= lastFE) {
          lastFE = evaluation; lastSocial = social; lastPersonal = personal;
        }
      }
      return new ArmMetrics(record.getStatus(), record.getFullEvaluations(),
          record.getFront().size(), minCmax, minTEC, minTWC, auditRecords, socialTaught,
          personalOnly, lastFE, lastSocial, lastPersonal, lifecycleColumnsPresent,
          dscrLong(record, "teacherUses"), dscrLong(record, "dominatedTeacherUses"),
          dscrLong(record, "validityChecks"), dscrLong(record, "replacements"),
          dscrDouble(record, "scrr"), summaryLong(record, "formalQgRounds"),
          summaryLong(record, "p6EventsTotal"));
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

    private static double dscrDouble(V35FairRunner.RunRecord record, String key) {
      String summary = record.getMechanismSummary();
      String marker = "dscr=";
      int start = summary.indexOf(marker);
      if (start < 0) return Double.NaN;
      start += marker.length();
      int end = summary.indexOf(",algorithmRunNanos=", start);
      if (end < 0) end = summary.length();
      String nested = summary.substring(start, end);
      String field = key + "=";
      int index = nested.indexOf('|' + field);
      if (index < 0) {
        index = nested.indexOf(field);
        if (index != 0) return Double.NaN;
      } else {
        index++;
      }
      int valueStart = index + field.length();
      int valueEnd = nested.indexOf('|', valueStart);
      if (valueEnd < 0) valueEnd = nested.length();
      return Double.parseDouble(nested.substring(valueStart, valueEnd));
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%d,%f,%f,%f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%f,%d,%d",
          label, status, fullEvaluations, frontSize, minCmax, minTEC, minTWC,
          auditRecords, socialTaughtRecords, personalOnlyRecords, lastRecordFE,
          lastRecordSocialUses, lastRecordPersonalUses, teacherUses,
          dominatedTeacherUses, validityChecks, replacements, scrr,
          formalQgRounds, p6EventsTotal);
    }
  }
}
