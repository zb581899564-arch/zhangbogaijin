package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class ZhangBoV35P25DRunnerTest {
  @Test
  public void allEightAdaptersCloseTheShortBudgetAndShareTheInitialPopulation()
      throws Exception {
    Path output = Files.createTempDirectory("v35-p25d-test");
    String expectedHash = null;
    for (ZhangBoV35P25DRunner.Algorithm algorithm
        : ZhangBoV35P25DRunner.Algorithm.values()) {
      Path run = ZhangBoV35P25DRunner.runForTest(1, algorithm, javaProject(), output,
          10, 2000);
      String hash = Files.readAllLines(run.resolve("initial-population.sha256"),
          StandardCharsets.UTF_8).get(0).split("\\s+")[0];
      if (expectedHash == null) expectedHash = hash; else assertEquals(expectedHash, hash);
      String status = new String(Files.readAllBytes(run.resolve("status.properties")),
          StandardCharsets.UTF_8);
      assertTrue(algorithm + " did not close FE", status.contains("fullEvaluations=2000"));
      assertTrue(Files.size(run.resolve("front.csv")) > "Cmax,TEC,TWC\n".length());
    }
  }

  @Test
  public void freezesFiveFreshSeeds() {
    assertEquals(20260822L, ZhangBoV35P25DRunner.approvedSeed(1));
    assertEquals(20260826L, ZhangBoV35P25DRunner.approvedSeed(5));
  }

  private static Path javaProject() {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("EADHFSP"))) return cwd;
    if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("EADHFSP"))) {
      return cwd.getParent();
    }
    if (Files.isDirectory(cwd.resolve("java-jmetal58/EADHFSP"))) return cwd.resolve("java-jmetal58");
    throw new IllegalStateException("cannot locate java project from " + cwd);
  }
}
