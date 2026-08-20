package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class ZhangBoV35P25CBalOpenRunnerTest {
  @Test
  public void completesNineShortRunsWithCommonInitialPopulationsAndUnifiedReport()
      throws Exception {
    Path root = javaProject();
    Path output = Files.createTempDirectory("v35-p25c-test");
    for (int slot = 1; slot <= 3; slot++) {
      String initial = null;
      for (ZhangBoV35P25CBalOpenRunner.Arm arm
          : ZhangBoV35P25CBalOpenRunner.Arm.values()) {
        Path run = ZhangBoV35P25CBalOpenRunner.runForTest(slot, arm, root, output, 10, 2000);
        String value = Files.readAllLines(run.resolve("initial-population.sha256"),
            StandardCharsets.UTF_8).get(0).split("\\s+")[0];
        if (initial == null) initial = value; else assertEquals(initial, value);
        String configuration = new String(Files.readAllBytes(run.resolve("configuration.txt")),
            StandardCharsets.UTF_8);
        assertTrue(configuration.contains("strictPressureMask=false"));
        assertTrue(configuration.contains("actualBottleneck=BAL"));
        assertTrue(configuration.contains("shadowEnabled=false"));
        assertTrue(configuration.contains("shiftMode=NONE"));
      }
    }
    Path report = output.resolve("report");
    ZhangBoV35P25CBalOpenReportRunner.Decision decision =
        ZhangBoV35P25CBalOpenReportRunner.generate(output, report);
    assertTrue(Files.isRegularFile(report.resolve("reference-front.csv")));
    assertTrue(Files.isRegularFile(report.resolve("per-seed-metrics.csv")));
    assertFalse(decision == null);
  }

  @Test
  public void usesThreeFreshSeeds() {
    assertEquals(20260819L, ZhangBoV35P25CBalOpenRunner.approvedSeed(1));
    assertEquals(20260821L, ZhangBoV35P25CBalOpenRunner.approvedSeed(3));
    assertTrue(ZhangBoV35P25CBalOpenRunner.approvedSeed(1) > 20260818L);
  }

  private static Path javaProject() {
    Path cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("EADHFSP"))) return cwd;
    if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("EADHFSP"))) {
      return cwd.getParent();
    }
    if (Files.isDirectory(cwd.resolve("java-jmetal58/EADHFSP"))) {
      return cwd.resolve("java-jmetal58");
    }
    throw new IllegalStateException("cannot locate java project from " + cwd);
  }
}
