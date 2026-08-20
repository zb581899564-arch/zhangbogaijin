package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ZhangBoV35P25ECorrectedComparisonRunnerTest {
  @Test
  public void allEightAlgorithmsShareOnlyProblemAndInitialPopulationAtTwoK()
      throws Exception {
    Path output = Files.createTempDirectory("v35-p25e-test");
    String expectedHash = null;
    Set<String> sources = new HashSet<>();
    for (ZhangBoV35P25ECorrectedComparisonRunner.Algorithm algorithm
        : ZhangBoV35P25ECorrectedComparisonRunner.Algorithm.values()) {
      Path run = ZhangBoV35P25ECorrectedComparisonRunner.runForTest(
          algorithm, javaProject(), output, 100, 2000, 20260822L);
      String hash = Files.readAllLines(run.resolve("initial-population.sha256"),
          StandardCharsets.UTF_8).get(0).split("\\s+")[0];
      if (expectedHash == null) expectedHash = hash; else assertEquals(expectedHash, hash);
      String status = read(run.resolve("status.properties"));
      assertTrue(algorithm + " status", status.contains("p25eStatus=COMPLETED"));
      int evaluations = integerProperty(status, "p25eFullEvaluations");
      assertTrue(evaluations > 0 && evaluations <= 2000);
      assertEquals(evaluations, integerProperty(status, "p25eDecoderCalls"));
      assertTrue(Files.size(run.resolve("front.csv")) > "Cmax,TEC,TWC\n".length());
      String configuration = read(run.resolve("configuration.txt"));
      assertTrue(configuration.contains("objectiveAdapter=0,1,6"));
      assertTrue(configuration.contains("shiftMode=NONE"));
      assertTrue(configuration.contains("searchMechanismsIndependent=true"));
      String identity = read(run.resolve("algorithm-identity.txt"));
      assertFalse(identity.contains("READY_STRUCTURED_ADAPTER"));
      sources.add(property(identity, "sourceKind"));
    }
    assertTrue(sources.contains("ZHANGBO_CURRENT"));
    assertTrue(sources.contains("PAPER_AUTHOR_SOURCE"));
    assertTrue(sources.contains("OFFICIAL_JMETAL_CORE"));
    ZhangBoV35P25ECorrectedReportRunner.generate(output);
    assertTrue(Files.size(output.resolve("reference-front.csv")) > 20L);
    assertTrue(read(output.resolve("P25E_REPORT.md")).contains("旧P25D已隔离"));
  }

  @Test
  public void isolatedComparisonCoresDoNotReferenceImprovementModules() throws Exception {
    String[] relative = {
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSO.java",
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSODivSub.java",
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/P25EAuthorMOPSODivSubDE.java",
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mymohea/P25EAuthorMOHEADE.java",
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58NSGAII.java",
        "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58SPEA2.java"
    };
    String[] forbidden = {"V35P25DComparativeEngine", "ZhangBoBaselineUpdater",
        "ZhangBoCfvf", "ZhangBoQp", "V35Dscr", "V35CaTaLite",
        "DirectionalTeacherPool"};
    for (String path : relative) {
      String source = read(javaProject().resolve(path));
      for (String token : forbidden) assertFalse(path + " contains " + token,
          source.contains(token));
    }
  }

  private static int integerProperty(String text, String key) {
    return Integer.parseInt(property(text, key));
  }
  private static String property(String text, String key) {
    for (String line : text.split("\\R")) {
      if (line.startsWith(key + "=")) return line.substring(key.length() + 1);
    }
    throw new AssertionError("missing " + key);
  }
  private static String read(Path path) throws Exception {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
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
