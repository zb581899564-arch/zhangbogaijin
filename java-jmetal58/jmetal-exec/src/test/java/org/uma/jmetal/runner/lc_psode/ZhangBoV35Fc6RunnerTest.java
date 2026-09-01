package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6LocalCandidateAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** FC-6A.3 configuration/ledger smoke.  A partial Q phase is sufficient to
 * prove that the explicit GLOBAL_ORIGINAL boundary reaches the real selector. */
public class ZhangBoV35Fc6RunnerTest {
  @Test(timeout = 900000)
  public void currentOrderWritesExplicitGlobalOriginalAuditAtTwoThousandFe() throws Exception {
    Path output = Files.createTempDirectory("v35-fc6-2k-");
    Path run = ZhangBoV35Fc6Runner.runForTest(
        ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT, "20_2_3_1", 20260822L,
        projectRoot(), output, 100, 2000);
    String configuration = new String(Files.readAllBytes(run.resolve("configuration.txt")),
        StandardCharsets.UTF_8);
    String status = new String(Files.readAllBytes(run.resolve("status.properties")),
        StandardCharsets.UTF_8);
    assertTrue(configuration.contains("pddrSelectionMode=GLOBAL_ORIGINAL"));
    assertTrue(configuration.contains("localSearchOrder=CATA_THEN_INHERITED"));
    assertTrue(!configuration.contains("BP_RESERVED_LEGACY"));
    assertTrue(status.contains("status=COMPLETED"));
    assertTrue(Files.size(run.resolve("local-candidate-ledger.csv")) > 80L);
    assertTrue(Files.isRegularFile(run.resolve("merge-pool-ledger.csv")));
    assertTrue(Files.isRegularFile(run.resolve("selector-call-chain.txt")));
    assertTrue(Files.isRegularFile(run.resolve("evidence-sha256.tsv")));
  }

  @Test public void phasesFreezeOnlyThePermittedBoundary() {
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration current =
        ZhangBoV35Fc6Runner.configurationFor(ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT,
            20260822L, 100, 500000);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration swapped =
        ZhangBoV35Fc6Runner.configurationFor(ZhangBoV35Fc6Runner.Phase.ORDER_SWAP,
            20260822L, 100, 500000);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration regional =
        ZhangBoV35Fc6Runner.configurationFor(ZhangBoV35Fc6Runner.Phase.REGION_AWARE,
            20260822L, 100, 500000);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration regionalSwap =
        ZhangBoV35Fc6Runner.configurationFor(ZhangBoV35Fc6Runner.Phase.REGION_AWARE_SWAP,
            20260822L, 100, 500000);
    assertEquals(PddrSelectionMode.GLOBAL_ORIGINAL, current.getPddrSelectionMode());
    assertEquals(V35LocalSearchOrder.CATA_THEN_INHERITED, current.getLocalSearchOrder());
    assertEquals(PddrSelectionMode.GLOBAL_ORIGINAL, swapped.getPddrSelectionMode());
    assertEquals(V35LocalSearchOrder.INHERITED_THEN_CATA, swapped.getLocalSearchOrder());
    assertEquals(PddrSelectionMode.REGION_AWARE, regional.getPddrSelectionMode());
    assertEquals(V35LocalSearchOrder.CATA_THEN_INHERITED, regional.getLocalSearchOrder());
    assertEquals(PddrSelectionMode.REGION_AWARE, regionalSwap.getPddrSelectionMode());
    assertEquals(V35LocalSearchOrder.INHERITED_THEN_CATA, regionalSwap.getLocalSearchOrder());
  }

  @Test(timeout = 900000)
  public void orderSwapAndRegionAwareReachTheRealLocalAndSelectionPaths() throws Exception {
    Path output = Files.createTempDirectory("v35-fc6-6k-");
    Path swapped = ZhangBoV35Fc6Runner.runForTest(
        ZhangBoV35Fc6Runner.Phase.ORDER_SWAP, "20_2_3_1", 20260823L,
        projectRoot(), output, 100, 6000);
    Path regional = ZhangBoV35Fc6Runner.runForTest(
        ZhangBoV35Fc6Runner.Phase.REGION_AWARE, "20_2_3_1", 20260823L,
        projectRoot(), output, 100, 20000);
    String swappedSummary = new String(Files.readAllBytes(swapped.resolve("mechanism-summary.txt")),
        StandardCharsets.UTF_8);
    String regionalLedger = new String(Files.readAllBytes(regional.resolve("local-candidate-ledger.csv")),
        StandardCharsets.UTF_8);
    assertTrue(swappedSummary.contains("formalLocalFE="));
    assertTrue(regionalLedger.contains("regionAssignments"));
    assertTrue(regionalLedger.contains("G1_CMAX="));
    assertEquals(sumCsvColumn(regionalLedger, 2), sumCsvColumn(regionalLedger, 8));
  }

  @Test(timeout = 900000)
  public void orderReportFreezesOneReferenceOnlyAfterAllSixRunsExist() throws Exception {
    Path output = Files.createTempDirectory("v35-fc6-report-");
    for (long seed : new long[] {20260822L, 20260823L, 20260824L}) {
      ZhangBoV35Fc6Runner.runForTest(ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT,
          "20_2_3_1", seed, projectRoot(), output, 100, 2000);
      ZhangBoV35Fc6Runner.runForTest(ZhangBoV35Fc6Runner.Phase.ORDER_SWAP,
          "20_2_3_1", seed, projectRoot(), output, 100, 2000);
    }
    Path report = output.resolve("report");
    ZhangBoV35Fc6ReportRunner.Decision decision = ZhangBoV35Fc6ReportRunner.generate(
        ZhangBoV35Fc6ReportRunner.Kind.ORDER, "20_2_3_1", output, report);
    assertTrue(decision == ZhangBoV35Fc6ReportRunner.Decision.ORDER_SWAP
        || decision == ZhangBoV35Fc6ReportRunner.Decision.CURRENT_RETAINED);
    assertTrue(Files.isRegularFile(report.resolve("reference-front.csv")));
    assertTrue(Files.isRegularFile(report.resolve("per-seed-metrics.csv")));
    assertTrue(Files.isRegularFile(report.resolve("local-search-ledger-summary.csv")));
    assertTrue(Files.isRegularFile(report.resolve("evidence-sha256.tsv")));
  }

  @Test(timeout = 900000)
  public void observationLedgerDoesNotChangeFc6ActionsTablesFrontOrFe() throws Exception {
    Replay off = replay(false);
    Replay on = replay(true);
    assertEquals(off.front, on.front);
    assertEquals(off.fe, on.fe);
    assertEquals(off.p6Hash, on.p6Hash);
    assertEquals(off.pddrHash, on.pddrHash);
    assertEquals(off.qgHash, on.qgHash);
    assertEquals(off.qpHash, on.qpHash);
    assertEquals(off.qgTable, on.qgTable);
    assertEquals(off.qpTable, on.qpTable);
    assertTrue(on.ledgerCycles > 0);
  }

  private static Replay replay(boolean enabled) throws Exception {
    long seed = 20260822L;
    Path root = projectRoot();
    Path javaProject = root.resolve("java-jmetal58");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        javaProject.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, seed,
        javaProject.resolve("instance-extensions/v1"), javaProject.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<PermutationSolution<Integer>>();
    for (int index = 0; index < 100; index++) initial.add(problem.createSolution());
    V35Fc6LocalCandidateAudit.setEnabled(enabled);
    V35Fc6LocalCandidateAudit.reset();
    try {
      V35FairRunner.RunRecord record = V35FairRunner.run(V35FairRunner.Mode.V35_FULL_POOL_OFF,
          problem, P8InitialPopulationProvider.copy(initial), 2000, seed, false,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), true,
          ZhangBoV35Fc6Runner.configurationFor(ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT,
              seed, 100, 2000));
      Replay result = new Replay();
      result.fe = record.getFullEvaluations();
      result.front = frontText(record.getFront());
      result.p6Hash = field(record.getMechanismSummary(), "p6EventStreamHash");
      result.pddrHash = field(record.getMechanismSummary(), "pddrEventStreamHash");
      result.qgHash = field(record.getMechanismSummary(), "qgEventStreamHash");
      result.qpHash = field(record.getMechanismSummary(), "qpEventStreamHash");
      result.qgTable = field(record.getMechanismSummary(), "qgTableHash");
      result.qpTable = field(record.getMechanismSummary(), "qpTableHash");
      V35Fc6LocalCandidateAudit audit = V35Fc6LocalCandidateAudit.current();
      result.ledgerCycles = audit == null ? 0 : audit.getRecordedCycleCount();
      return result;
    } finally {
      V35Fc6LocalCandidateAudit.setEnabled(false);
    }
  }

  private static String field(String summary, String key) {
    java.util.regex.Matcher match = java.util.regex.Pattern.compile("(?:^|,)" + key + "=([^,]+)")
        .matcher(summary);
    if (!match.find()) throw new AssertionError("field absent: " + key);
    return match.group(1);
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder();
    for (double[] value : front) text.append(value[0]).append(',').append(value[1]).append(',')
        .append(value[2]).append('\n');
    return text.toString();
  }

  private static final class Replay {
    int fe; int ledgerCycles; String front, p6Hash, pddrHash, qgHash, qpHash, qgTable, qpTable;
  }

  private static long sumCsvColumn(String csv, int column) {
    String[] lines = csv.split("\\r?\\n");
    long total = 0L;
    for (int index = 1; index < lines.length; index++) {
      if (lines[index].trim().isEmpty()) continue;
      String[] fields = lines[index].split(",", 11);
      total += Long.parseLong(fields[column]);
    }
    return total;
  }

  private static Path projectRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current.getParent() != null && !Files.exists(current.resolve("AGENTS.md"))) {
      current = current.getParent();
    }
    if (!Files.exists(current.resolve("AGENTS.md"))) {
      throw new IllegalStateException("project root not found");
    }
    return current;
  }
}
