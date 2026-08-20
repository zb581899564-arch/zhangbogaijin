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
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * V35-P13-P16 evidence: two FULL runs (20k on 20_2_3_1, 5k on I1 10_2_2_1) with
 * the v35Lite decision stream kept in full (ring-buffer capacity raised) so the
 * Test/Apply/Re-test reasons and the runtime mask legality can be verified line
 * by line.  N4/N5 accepted lines prove the upstream natural-recovery gate
 * (accepted = recoveryGain && role acceptance) held for those candidates.
 */
public class V35P13P16CaTaLiteEvidenceTest {
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
  public void cataLiteEvidenceHoldsOnBothInstances() throws Exception {
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

    Path evidence = root.resolve("docs/evidence/V35-P13");
    Path runs = evidence.resolve("runs");
    Files.createDirectories(runs);

    Map<String, LiteMetrics> metrics = new TreeMap<>();
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
      // Keep the full CA-TA event stream for this evidence run: the default
      // 4096-entry ring buffer is flooded by the formal local-search lines.
      System.setProperty("zhangbo.events.capacity", "131072");

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

      LiteMetrics lite = LiteMetrics.from(record);
      // The three-phase machinery must all fire at runtime.
      assertTrue("TEST decisions: " + arm.label, lite.testDecisions > 0);
      assertTrue("APPLY decisions: " + arm.label, lite.applyDecisions > 0);
      assertTrue("CA-TA-Lite FE: " + arm.label, lite.caTaLiteFE > 0);
      // Every decision line is legal for its context mask.
      assertEquals("mask legality " + arm.label, lite.decisions, lite.legalDecisions);
      // The action stream agrees with the summary counters.
      assertEquals("action lines == test+apply calls " + arm.label,
          lite.caTaTestCalls + lite.caTaApplyCalls, lite.actionLines);
      metrics.put(arm.label, lite);

      V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
          .seed(SEED).populationSize(arm.population).maxEvaluations(arm.budget)
          .decoderMode(ProductionDecodeMode.FM3)
          .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
          .directionalTeacherPool(true).teacherPoolSize(10).build();
      V35FairRunner.writeRecord(record, runs.resolve(arm.label), configuration.canonicalText());
    }
    // At least one re-test trigger must fire in the 20k arm (unit tests pin the
    // triggers themselves; this is the runtime occurrence evidence).
    assertTrue("20k arm must exhibit a re-test trigger: " + metrics.get(arm20k.label).reasons,
        metrics.get(arm20k.label).reasons.contains("CONSECUTIVE_APPLY_FAILURE_RETEST")
            || metrics.get(arm20k.label).reasons.contains("APPLY_HORIZON_COMPLETE_TEST"));

    StringBuilder csv = new StringBuilder();
    csv.append("arm,status,FE,caTaTestCalls,caTaApplyCalls,caTaLiteFE,decisions,"
        + "testDecisions,applyDecisions,legalDecisions,actionLines,reasons,macrosSeen,"
        + "n4Accepted,n5Accepted,frontSize,minCmax\n");
    for (Map.Entry<String, LiteMetrics> entry : metrics.entrySet()) {
      csv.append(entry.getValue().csv(entry.getKey())).append('\n');
    }
    Files.write(evidence.resolve("CATA_LITE_METRICS.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // SHA-256 manifest over every evidence file.
    Map<String, String> hashes = new TreeMap<>();
    hashes.put(root.relativize(evidence.resolve("CATA_LITE_METRICS.csv")).toString()
        .replace('\\', '/'), sha256(evidence.resolve("CATA_LITE_METRICS.csv")));
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

  private static long summaryLong(V35FairRunner.RunRecord record, String key) {
    String summary = record.getMechanismSummary();
    String marker = key + "=";
    int index = summary.indexOf(marker);
    if (index < 0) return -1L;
    int end = summary.indexOf(',', index);
    if (end < 0) end = summary.length();
    return Long.parseLong(summary.substring(index + marker.length(), end));
  }

  /** Runtime CA-TA-Lite metrics parsed from the full v35Lite decision stream. */
  private static final class LiteMetrics {
    final String status;
    final int fullEvaluations;
    final long caTaTestCalls;
    final long caTaApplyCalls;
    final long caTaLiteFE;
    final int decisions;
    final int testDecisions;
    final int applyDecisions;
    final int legalDecisions;
    final int actionLines;
    final Set<String> reasons;
    final Set<String> macrosSeen;
    final int n4Accepted;
    final int n5Accepted;
    final int frontSize;
    final double minCmax;

    LiteMetrics(String status, int fullEvaluations, long caTaTestCalls,
        long caTaApplyCalls, long caTaLiteFE, int decisions, int testDecisions,
        int applyDecisions, int legalDecisions, int actionLines, Set<String> reasons,
        Set<String> macrosSeen, int n4Accepted, int n5Accepted, int frontSize,
        double minCmax) {
      this.status = status; this.fullEvaluations = fullEvaluations;
      this.caTaTestCalls = caTaTestCalls; this.caTaApplyCalls = caTaApplyCalls;
      this.caTaLiteFE = caTaLiteFE; this.decisions = decisions;
      this.testDecisions = testDecisions; this.applyDecisions = applyDecisions;
      this.legalDecisions = legalDecisions; this.actionLines = actionLines;
      this.reasons = reasons; this.macrosSeen = macrosSeen;
      this.n4Accepted = n4Accepted; this.n5Accepted = n5Accepted;
      this.frontSize = frontSize; this.minCmax = minCmax;
    }

    static LiteMetrics from(V35FairRunner.RunRecord record) {
      int decisions = 0, testDecisions = 0, applyDecisions = 0, legalDecisions = 0;
      int actionLines = 0, n4Accepted = 0, n5Accepted = 0;
      Set<String> reasons = new TreeSet<>();
      Set<String> macrosSeen = new TreeSet<>();
      for (String line : record.getCaTaEvents().split("\n")) {
        if (line.startsWith("v35Lite:generation=")) {
          decisions++;
          String context = field(line, "context=", ',');
          String reason = field(line, "reason=", ',');
          boolean test = Boolean.parseBoolean(field(line, "test=", ','));
          if (test) testDecisions++; else applyDecisions++;
          reasons.add(reason);
          int actionsStart = line.indexOf("actions=[") + "actions=[".length();
          int actionsEnd = line.indexOf(']', actionsStart);
          String[] actions = line.substring(actionsStart, actionsEnd).split(", ");
          String bottleneck = context.substring(context.indexOf('|') + 1);
          String role = context.substring(0, context.indexOf('|'));
          boolean legal = true;
          V35CaTaContext gate = new V35CaTaContext(
              V35SubSwarmRole.valueOf(role), V35Bottleneck.valueOf(bottleneck));
          for (String action : actions) {
            if (!gate.allows(V35MacroNeighborhood.valueOf(action))) legal = false;
          }
          if (legal) legalDecisions++;
        } else if (line.startsWith("v35Lite:action=")) {
          actionLines++;
          String action = field(line, "v35Lite:action=", ',');
          String accepted = field(line, "accepted=", ',');
          macrosSeen.add(action);
          if (accepted.equals("true")) {
            if (action.equals("N4")) n4Accepted++;
            if (action.equals("N5")) n5Accepted++;
          }
        }
      }
      double minCmax = Double.POSITIVE_INFINITY;
      for (double[] value : record.getFront()) minCmax = Math.min(minCmax, value[0]);
      return new LiteMetrics(record.getStatus(), record.getFullEvaluations(),
          summaryLong(record, "caTaLiteTest"), summaryLong(record, "caTaLiteApply"),
          summaryLong(record, "caTaLiteFE"), decisions, testDecisions, applyDecisions,
          legalDecisions, actionLines, reasons, macrosSeen, n4Accepted, n5Accepted,
          record.getFront().size(), minCmax);
    }

    private static String field(String line, String marker, char terminator) {
      int start = line.indexOf(marker);
      if (start < 0) return "";
      start += marker.length();
      int end = line.indexOf(terminator, start);
      return end < 0 ? line.substring(start) : line.substring(start, end);
    }

    String csv(String label) {
      return String.format(Locale.ROOT,
          "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%d,%d,%d,%f",
          label, status, fullEvaluations, caTaTestCalls, caTaApplyCalls, caTaLiteFE,
          decisions, testDecisions, applyDecisions, legalDecisions, actionLines,
          String.join("+", reasons), String.join("+", macrosSeen), n4Accepted,
          n5Accepted, frontSize, minCmax);
    }
  }
}
