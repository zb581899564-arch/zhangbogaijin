package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** 2000-FE Batch-0 for all five approved seed slots and all three P25A arms. */
public class ZhangBoV35P25ARunnerTest {

  @Test(timeout = 900000)
  public void fifteenShortRunsShareStartsAndFreezeOneReference() throws Exception {
    Path root = projectRoot();
    Path output = Files.createTempDirectory("v35-p25a-smoke-");
    Set<String> runIds = new HashSet<>();
    Set<String> seedHashes = new HashSet<>();
    Set<String> a5ActionLogs = new HashSet<>();
    for (int slot = 1; slot <= 5; slot++) {
      String shared = null;
      for (ZhangBoV35P25ARunner.Arm arm : ZhangBoV35P25ARunner.Arm.values()) {
        // Table 9 uses Q_Times=50. Ten particles are the supported explanatory
        // size and allow complete formal cycles inside 2000 FE.  At this size
        // the pool path must be requested; the production 100-particle gate
        // additionally requires actual top-k filtering.
        Path run = ZhangBoV35P25ARunner.runForTest(slot, arm, root, output, 10, 2000);
        String hash = Files.readAllLines(run.resolve("initial-population.sha256"),
            StandardCharsets.UTF_8).get(0).split("\\s+")[0];
        if (shared == null) shared = hash; else assertEquals("same seed must share start", shared, hash);
        assertTrue("unique runId", runIds.add(slot + ":" + arm));
        String status = new String(Files.readAllBytes(run.resolve("status.properties")),
            StandardCharsets.UTF_8);
        assertTrue(status.contains("status=COMPLETED"));
        assertTrue(status.contains("fullEvaluations="));
        if (arm == ZhangBoV35P25ARunner.Arm.A5) {
          a5ActionLogs.add(sha256(Files.readAllBytes(run.resolve("ca-ta-lite-events.log"))));
        }
      }
      assertTrue("different seed must have a new initial population", seedHashes.add(shared));
    }
    assertEquals(15, runIds.size());
    assertEquals(5, seedHashes.size());
    assertTrue("different seeds must change a recorded random action stream",
        a5ActionLogs.size() > 1);
    Path report = output.resolve("report");
    ZhangBoV35P25AReportRunner.Decision decision =
        ZhangBoV35P25AReportRunner.generate(output, report);
    assertTrue(Files.isRegularFile(report.resolve("reference-front.csv")));
    assertTrue(Files.isRegularFile(report.resolve("per-seed-metrics.csv")));
    assertTrue(Files.isRegularFile(report.resolve("evidence-sha256.tsv")));
    assertTrue(decision == ZhangBoV35P25AReportRunner.Decision.A4_MAIN
        || decision == ZhangBoV35P25AReportRunner.Decision.A5_FULL_MAIN
        || decision == ZhangBoV35P25AReportRunner.Decision.STOP_REVIEW);
  }

  @Test public void approvedSeedsAndArmMechanismsAreScopeLocked() {
    assertEquals(20260809L, ZhangBoV35P25ARunner.approvedSeed(1));
    assertEquals(20260813L, ZhangBoV35P25ARunner.approvedSeed(5));
    assertNotEquals(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry.configFor(
            ZhangBoV35P25ARunner.Arm.A4.getRung(), 20260809L, 100, 500000)
            .configurationHash(),
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry.configFor(
            ZhangBoV35P25ARunner.Arm.A5.getRung(), 20260809L, 100, 500000)
            .configurationHash());
    String a0 = org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry.configFor(
        ZhangBoV35P25ARunner.Arm.A0.getRung(), 20260809L, 100, 500000).canonicalText();
    String a4 = org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry.configFor(
        ZhangBoV35P25ARunner.Arm.A4.getRung(), 20260809L, 100, 500000).canonicalText();
    String a5 = org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry.configFor(
        ZhangBoV35P25ARunner.Arm.A5.getRung(), 20260809L, 100, 500000).canonicalText();
    assertEquals(java.util.Arrays.asList("dscr", "cfvf", "qp", "caTaLite"),
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry
            .differingSwitchKeys(a0, a4));
    assertEquals(java.util.Arrays.asList("directionalTeacherPool"),
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35AblationRegistry
            .differingSwitchKeys(a4, a5));
  }

  private static String sha256(byte[] bytes) throws Exception {
    byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static Path projectRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current.getParent() != null && !Files.exists(current.resolve("AGENTS.md"))) {
      current = current.getParent();
    }
    if (!Files.exists(current.resolve("AGENTS.md"))) throw new IllegalStateException("project root not found");
    return current;
  }
}
