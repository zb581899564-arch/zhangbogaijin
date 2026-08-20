package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/** Larger v3.5 diagnostic comparison; not a formal statistical experiment. */
public class V35FairComparison100kSmokeTest {
  @Test(timeout = 600000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void run100kSameStart() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) project = project.getParent();
    Path javaProject = project.resolve("java-jmetal58");
    System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", javaProject.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", javaProject.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW sourceA = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceB = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem baselineProblem = ZhangBoV35ProblemFactory.create(
        sourceA.getFatigueInstanceData(), sourceA.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem fullProblem = ZhangBoV35ProblemFactory.create(
        sourceB.getFatigueInstanceData(), sourceB.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < 100; i++) initial.add(baselineProblem.createSolution());
    V35FairRunner.RunRecord baseline = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        (Problem) baselineProblem, initial, 100000, 20260808L);
    V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) fullProblem, initial, 100000, 20260808L);
    assertEquals("COMPLETED", baseline.getStatus());
    assertEquals("COMPLETED", full.getStatus());
    assertTrue(baseline.getFullEvaluations() > 0 && baseline.getFullEvaluations() <= 100000);
    assertTrue(full.getFullEvaluations() > 0 && full.getFullEvaluations() <= 100000);
    assertEquals(baseline.getInitialPopulationHash(), full.getInitialPopulationHash());
    assertFalse(baseline.getFront().isEmpty());
    assertFalse(full.getFront().isEmpty());
    // Dual-Q block freezing must actually alternate: the FULL run has to reach
    // the G-block (Qg learns again after warmup) inside a 100k budget.
    assertTrue("G-block never executed: " + full.getMechanismSummary(),
        dualQPhaseCount(full, "dualQG") > 0L);
    assertTrue("P-block missing: " + full.getMechanismSummary(),
        dualQPhaseCount(full, "dualQP") > 0L);
    Path evidence = project.resolve("docs/evidence/V35-P9");
    Files.createDirectories(evidence);
    String text = "instance=20_2_3_1\nseed=20260808\npopulation=100\nmaxEvaluations=100000\n"
        + record("baseline", baseline) + record("full", full);
    Files.write(evidence.resolve("V35_FAIR_COMPARISON_100K_20260808.txt"),
        text.getBytes(StandardCharsets.UTF_8));
  }

  private static String record(String label, V35FairRunner.RunRecord record) {
    return label + "Status=" + record.getStatus() + "\n"
        + label + "FE=" + record.getFullEvaluations() + "\n"
        + label + "InitialHash=" + record.getInitialPopulationHash() + "\n"
        + label + "FrontSize=" + record.getFront().size() + "\n"
        + label + "Min=" + minima(record.getFront()) + "\n"
        + label + "AlgorithmRunNanos=" + record.getAlgorithmRunNanos() + "\n"
        + label + "DecoderTiming=" + record.getDecoderTiming().getSuccessfulDecoderCalls()
        + ",base=" + record.getDecoderTiming().getBaseDecodeNanos()
        + ",left=" + record.getDecoderTiming().getLeftShiftNanos()
        + ",right=" + record.getDecoderTiming().getRightShiftNanos()
        + ",total=" + record.getDecoderTiming().getDecoderTotalNanos()
        + ",leftRecomputations=" + record.getDecoderTiming().getLeftFullRecomputations()
        + ",rightRecomputations=" + record.getDecoderTiming().getRightFullRecomputations() + "\n"
        + label + "Mechanisms=" + record.getMechanismSummary() + "\n";
  }

  private static long dualQPhaseCount(V35FairRunner.RunRecord record, String key) {
    String summary = record.getMechanismSummary();
    String marker = key + "=";
    int index = summary.indexOf(marker);
    if (index < 0) return -1L;
    int end = summary.indexOf(',', index);
    if (end < 0) end = summary.length();
    return Long.parseLong(summary.substring(index + marker.length(), end));
  }

  private static String minima(List<double[]> front) {
    double cmax = Double.POSITIVE_INFINITY, tec = Double.POSITIVE_INFINITY, twc = Double.POSITIVE_INFINITY;
    for (double[] value : front) { cmax = Math.min(cmax, value[0]); tec = Math.min(tec, value[1]); twc = Math.min(twc, value[2]); }
    return cmax + "," + tec + "," + twc;
  }
}
