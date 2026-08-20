package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

public class ZhangBoP9SingleComparisonRunnerTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void formalParametersExactlyMatchTable9Contract() {
    ZhangBoP9FormalParameters p = ZhangBoP9FormalParameters.formal();
    assertEquals(100, p.getPopulation());
    assertEquals(500000, p.getMaxFEs());
    assertEquals(20260808L, p.getSeed());
    assertTrue(Arrays.equals(new int[] {20, 40, 20, 20}, p.getPhysicalSubswarmSizes()));
    assertEquals(0.6, p.getRandUpperBound(), 0.0);
    assertEquals(0.2, p.getFaCrossover(), 0.0);
    assertEquals(0.5, p.getMaCrossover(), 0.0);
    assertEquals(0.5, p.getWaCrossover(), 0.0);
    assertEquals(0.08, p.getFaMutation(), 0.0);
    assertEquals(0.15, p.getMaMutation(), 0.0);
    assertEquals(0.25, p.getWaMutation(), 0.0);
    assertEquals(50, p.getQTimes());
    assertEquals(30, p.getLocalSearchTimes());
    assertEquals(0.8, p.getGamma(), 0.0);
    assertEquals(0.8, p.getEpsilon(), 0.0);
    assertFalse(p.canonicalText().contains("randUpperBound=0.4"));
    assertFalse(p.canonicalText().contains("localSearchTimes=40"));
  }

  @Test
  public void fullAndBaselineUseFm3ButHaveDifferentMechanismVectors() {
    P8ExperimentSpec full = P8ExperimentRegistry.find("FULL");
    P8ExperimentSpec baseline = P8ExperimentRegistry.find("B1");
    assertEquals(P8AblationProfile.DecoderMode.FATIGUE_AWARE_SELECTION,
        full.getAblationProfile().getDecoderMode());
    assertEquals(P8AblationProfile.DecoderMode.FATIGUE_AWARE_SELECTION,
        baseline.getAblationProfile().getDecoderMode());
    assertNotEquals(full.getMechanismVectorHash(), baseline.getMechanismVectorHash());
    assertTrue(full.getAblationProfile().isCfvfFamily());
    assertFalse(baseline.getAblationProfile().isCfvfFamily());
  }

  @Test
  public void fiveAdditionalSeedSlotsAreFixedAndCannotDrift() {
    for (int slot = 1; slot <= 5; slot++) {
      long expected = 20260808L + slot;
      assertEquals(expected, ZhangBoP9FiveSeedRunner.approvedSeed(slot));
      ZhangBoP9FormalParameters p =
          ZhangBoP9FormalParameters.engineering(expected, 2000);
      assertEquals(expected, p.getSeed());
      assertEquals(2, p.getQTimes());
      assertEquals(1, p.getLocalSearchTimes());
      assertTrue(p.canonicalText().contains("seed=" + expected));
      assertTrue(p.canonicalText().contains("formalBaseline.qTimes=2"));
      assertTrue(p.canonicalText().contains("formalBaseline.localSearchTimes=1"));
    }
    try {
      ZhangBoP9FiveSeedRunner.approvedSeed(0);
      fail("slot zero must be rejected");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("1..5"));
    }
  }

  @Test
  public void baselineRefusesToStartWithoutCompletedFull() throws Exception {
    Path output = temporary.newFolder("missing-full").toPath();
    try {
      ZhangBoP9SingleComparisonRunner.runPhase(
          ZhangBoP9SingleComparisonRunner.Phase.HMOPSO_QGS_F,
          projectRoot(), output, ZhangBoP9FormalParameters.engineering(2000));
      fail("baseline must require FULL");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("Required completed run is missing"));
    }
  }

  @Test
  public void engineeringPipelineRunsFullThenBaselineThenReport() throws Exception {
    Path output = temporary.newFolder("pipeline").toPath();
    ZhangBoP9FormalParameters p = ZhangBoP9FormalParameters.engineering(2000);
    Path project = projectRoot();
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.FULL, project, output, p);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.HMOPSO_QGS_F, project, output, p);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.REPORT, project, output, p);

    Path full = output.resolve("ZHANGBO-FULL-20260808/status.properties");
    Path baseline = output.resolve("HMOPSO-QGS-F-20260808/status.properties");
    Path report = output.resolve("comparison/P9_SINGLE_COMPARISON_REPORT.md");
    Path fullTiming = output.resolve("ZHANGBO-FULL-20260808/decoder-timing.csv");
    Path baseTiming = output.resolve("HMOPSO-QGS-F-20260808/decoder-timing.csv");
    Path timingComparison = output.resolve("comparison/decoder-timing-comparison.csv");
    Path timingReport = output.resolve("comparison/DECODER_TIMING_REPORT.md");
    assertTrue(Files.isRegularFile(full));
    assertTrue(Files.isRegularFile(baseline));
    assertTrue(Files.isRegularFile(report));
    assertTrue(Files.isRegularFile(fullTiming));
    assertTrue(Files.isRegularFile(baseTiming));
    assertTrue(Files.isRegularFile(timingComparison));
    assertTrue(Files.isRegularFile(timingReport));
    String fullText = new String(Files.readAllBytes(full), StandardCharsets.UTF_8);
    String baseText = new String(Files.readAllBytes(baseline), StandardCharsets.UTF_8);
    assertTrue(fullText.contains("status=COMPLETED"));
    assertTrue(fullText.matches("(?s).*cfvfOffspring=[1-9][0-9]*.*"));
    assertTrue(fullText.matches("(?s).*qpActions=[1-9][0-9]*.*"));
    assertTrue(fullText.matches("(?s).*archiveInsertions=[1-9][0-9]*.*"));
    assertTrue(fullText.matches("(?s).*caTaTestCalls=[1-9][0-9]*.*"));
    assertTrue(fullText.matches("(?s).*caTaApplyCalls=[1-9][0-9]*.*"));
    assertTrue(fullText.contains("formalOuterCycles=0"));
    assertTrue(fullText.contains("formalQgRounds=0"));
    assertTrue(baseText.contains("status=COMPLETED"));
    assertTrue(baseText.contains("qpActions=0"));
    assertTrue(baseText.contains("archiveInsertions=0"));
    assertTrue(baseText.contains("caTaTestCalls=0"));
    assertTrue(baseText.contains("caTaApplyCalls=0"));
    assertTrue(baseText.matches("(?s).*formalOuterCycles=[1-9][0-9]*.*"));
    assertEquals(Long.parseLong(value(baseText, "formalOuterCycles")) * 2L,
        Long.parseLong(value(baseText, "formalQgRounds")));
    assertTrue(baseText.matches("(?s).*formalCriticalFactorySwaps=[1-9][0-9]*.*"));
    assertTrue(baseText.matches("(?s).*formalCriticalFactoryInserts=[1-9][0-9]*.*"));
    assertTrue(baseText.matches("(?s).*formalO1O9Evaluations=[1-9][0-9]*.*"));
    assertEquals(value(fullText, "initialPopulationSha256"),
        value(baseText, "initialPopulationSha256"));
    assertEquals("LEFT_RIGHT", value(fullText, "shiftMode"));
    assertEquals(value(fullText, "shiftMode"), value(baseText, "shiftMode"));
    assertEquals(value(fullText, "shiftConfigurationSha256"),
        value(baseText, "shiftConfigurationSha256"));
    assertDecoderTimingClosed(fullText);
    assertDecoderTimingClosed(baseText);
    String timingHeader = new String(Files.readAllBytes(fullTiming), StandardCharsets.UTF_8);
    assertTrue(timingHeader.startsWith("algorithm,seed,fullEvaluations,"));
    assertTrue(timingHeader.contains("leftFullRecomputations"));
    assertTrue(timingHeader.contains("avgRightMicrosPerRecomputation"));
  }

  @Test
  public void threeObjectiveMetricSanityUsesFrozenReference() {
    List<double[]> reference = Arrays.asList(
        new double[] {1.0, 3.0, 3.0},
        new double[] {2.0, 2.0, 2.0},
        new double[] {3.0, 1.0, 1.0});
    P8MetricCalculator.Metrics exact = P8MetricCalculator.calculate(reference, reference);
    P8MetricCalculator.Metrics weak = P8MetricCalculator.calculate(
        Arrays.asList(new double[] {3.0, 3.0, 3.0}), reference);
    assertEquals(0.0, exact.igd, 1e-12);
    assertTrue(exact.hv > weak.hv);
    assertTrue(weak.igd > exact.igd);
  }

  private static Path projectRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current != null && !Files.isDirectory(current.resolve("EADHFSP"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("Cannot locate java-jmetal58 root");
    return current;
  }

  private static String value(String text, String key) {
    String prefix = key + "=";
    for (String line : text.split("\\r?\\n")) {
      if (line.startsWith(prefix)) return line.substring(prefix.length());
    }
    throw new AssertionError("Missing key " + key);
  }

  private static void assertDecoderTimingClosed(String status) {
    long evaluations = Long.parseLong(value(status, "fullEvaluations"));
    long calls = Long.parseLong(value(status, "successfulDecoderCalls"));
    long algorithm = Long.parseLong(value(status, "algorithmRunNanos"));
    long decoder = Long.parseLong(value(status, "decoderTotalNanos"));
    assertEquals(evaluations, calls);
    assertTrue(Long.parseLong(value(status, "baseDecodeNanos")) > 0L);
    assertTrue(Long.parseLong(value(status, "leftShiftNanos")) > 0L);
    assertTrue(Long.parseLong(value(status, "rightShiftNanos")) > 0L);
    assertTrue(algorithm >= decoder);
    assertTrue(Long.parseLong(value(status, "leftFullRecomputations")) >= 0L);
    assertTrue(Long.parseLong(value(status, "rightFullRecomputations")) >= 0L);
  }
}
