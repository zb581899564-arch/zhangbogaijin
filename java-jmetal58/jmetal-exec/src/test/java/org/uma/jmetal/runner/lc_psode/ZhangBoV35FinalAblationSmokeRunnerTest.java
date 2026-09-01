package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/** The required bounded 2k smoke; this never exercises a formal budget. */
public class ZhangBoV35FinalAblationSmokeRunnerTest {
  @Test(timeout = 360000)
  public void completesA0ToA4WithOneInstanceSeedAndInitialPopulation() throws Exception {
    Path output = Files.createTempDirectory("v35-final-a0-a4-smoke").resolve("completed");
    Path completed = ZhangBoV35FinalAblationSmokeRunner.runForTest(javaProject(), output,
        ZhangBoV35FinalAblationSmokeRunner.POPULATION,
        ZhangBoV35FinalAblationSmokeRunner.MAX_FES,
        ZhangBoV35FinalAblationSmokeRunner.SEED);
    String csv = new String(Files.readAllBytes(completed.resolve("smoke-summary.csv")),
        StandardCharsets.UTF_8);
    assertTrue(csv.contains("A0,COMPLETED"));
    assertTrue(csv.contains("A1,COMPLETED"));
    assertTrue(csv.contains("A2,COMPLETED"));
    assertTrue(csv.contains("A3,COMPLETED"));
    assertTrue(csv.contains("A4,COMPLETED"));
    assertTrue(new String(Files.readAllBytes(completed.resolve("SMOKE_CHECKS.md")),
        StandardCharsets.UTF_8).contains("| A4 | PASS |"));
    String initial = Files.readAllLines(completed.resolve("initial-population.sha256"),
        StandardCharsets.UTF_8).get(0).split("\\s+")[0];
    for (String arm : new String[]{"A0", "A1", "A2", "A3", "A4"}) {
      String configuration = new String(Files.readAllBytes(completed.resolve("arms")
          .resolve(arm).resolve("configuration.txt")), StandardCharsets.UTF_8);
      assertTrue(configuration.contains("initialPopulationHash=" + initial));
      assertTrue(configuration.contains("shiftMode=NONE"));
    }
  }

  @Test
  public void refusesAnyBudgetOtherThanTheRequired2000FeSmoke() throws Exception {
    try {
      ZhangBoV35FinalAblationSmokeRunner.runForTest(javaProject(),
          Files.createTempDirectory("v35-final-a0-a4-refuse"), 10, 500000, 20260822L);
      fail("formal budget must be rejected");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("formal runs are intentionally refused"));
    }
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
