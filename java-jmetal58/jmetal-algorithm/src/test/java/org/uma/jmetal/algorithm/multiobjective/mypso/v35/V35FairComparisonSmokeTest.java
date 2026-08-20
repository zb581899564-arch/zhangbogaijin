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

/** One controlled 20k-FE comparison; diagnostic only, not a formal experiment. */
public class V35FairComparisonSmokeTest {
  @Test(timeout = 300000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void runBaselineAndFullOnSameStart() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null && "jmetal-algorithm".equals(project.getFileName().toString())) project = project.getParent();
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) project = project.getParent();
    Path javaProject = Files.isDirectory(project.resolve("java-jmetal58"))
        ? project.resolve("java-jmetal58") : project;
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
        (Problem) baselineProblem, initial, 20000, 20260808L);
    V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) fullProblem, initial, 20000, 20260808L);
    // Single-variable DSCR pairing: both runs retain original Qg and differ
    // only in the DSCR sanitation switch.
    ZhangBoEDHHFSPW sourceC = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceD = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem qg0Problem = ZhangBoV35ProblemFactory.create(
        sourceC.getFatigueInstanceData(), sourceC.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem qg1Problem = ZhangBoV35ProblemFactory.create(
        sourceD.getFatigueInstanceData(), sourceD.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    V35FairRunner.RunRecord qg0 = V35FairRunner.run(V35FairRunner.Mode.V35_QG0,
        (Problem) qg0Problem, initial, 20000, 20260808L);
    V35FairRunner.RunRecord qg1 = V35FairRunner.run(V35FairRunner.Mode.V35_QG1,
        (Problem) qg1Problem, initial, 20000, 20260808L);
    System.out.println("V35_COMPARE baseline=" + baseline.getStatus() + ",fe=" + baseline.getFullEvaluations()
        + ",full=" + full.getStatus() + ",fe=" + full.getFullEvaluations());
    assertEquals(baseline.getInitialPopulationHash(), full.getInitialPopulationHash());
    assertEquals(qg0.getInitialPopulationHash(), qg1.getInitialPopulationHash());
    assertEquals(baseline.getInitialPopulationHash(), qg0.getInitialPopulationHash());
    assertEquals("COMPLETED", baseline.getStatus());
    assertEquals(full.getStopReason(), "COMPLETED", full.getStatus());
    assertTrue(baseline.getFullEvaluations() <= 20000);
    assertTrue(full.getFullEvaluations() <= 20000);
    assertFalse(baseline.getFront().isEmpty());
    assertFalse(full.getFront().isEmpty());
    assertEquals("COMPLETED", qg0.getStatus());
    assertEquals("COMPLETED", qg1.getStatus());
    assertTrue("QG1 must expose real teacher uses: " + qg1.getMechanismSummary(),
        qg1.getMechanismSummary().contains("teacherUses=")
            && !qg1.getDscrTeacherUses().trim().equals(
                "decisionCycle,generation,FE,group,teacherId,teacherObjectives,dominated,dominatorCount"));
    assertTrue("QG1 DSCR gate must remain zero: " + qg1.getMechanismSummary(),
        qg1.getMechanismSummary().contains("dominatedTeacherUses=0"));
    // Dual-Q block freezing must actually alternate: the FULL run has to reach
    // the G-block (Qg learns again after warmup) inside a 20k budget.
    assertTrue("G-block never executed: " + full.getMechanismSummary(),
        dualQPhaseCount(full, "dualQG") > 0L);
    assertTrue("P-block missing: " + full.getMechanismSummary(),
        dualQPhaseCount(full, "dualQP") > 0L);
    Path evidence = project.resolve("docs/evidence/V35-P9");
    Files.createDirectories(evidence);
    String report = "instance=20_2_3_1\nseed=20260808\npopulation=100\nmaxEvaluations=20000\n"
        + "baselineStatus=" + baseline.getStatus() + "\nbaselineFE=" + baseline.getFullEvaluations()
        + "\nbaselineInitialHash=" + baseline.getInitialPopulationHash() + "\n"
        + "baselineFrontSize=" + baseline.getFront().size() + "\n"
        + "baselineMin=" + minima(baseline.getFront()) + "\n"
        + "baselineMechanisms=" + baseline.getMechanismSummary() + "\n"
        + "fullStatus=" + full.getStatus() + "\nfullFE=" + full.getFullEvaluations()
        + "\nfullInitialHash=" + full.getInitialPopulationHash() + "\n"
        + "fullFrontSize=" + full.getFront().size() + "\n"
        + "fullMin=" + minima(full.getFront()) + "\n"
        + "fullMechanisms=" + full.getMechanismSummary() + "\n"
        + "qg0Status=" + qg0.getStatus() + "\nqg0FE=" + qg0.getFullEvaluations()
        + "\nqg0Mechanisms=" + qg0.getMechanismSummary() + "\n"
        + "qg1Status=" + qg1.getStatus() + "\nqg1FE=" + qg1.getFullEvaluations()
        + "\nqg1Mechanisms=" + qg1.getMechanismSummary() + "\n";
    Files.write(evidence.resolve("V35_FAIR_COMPARISON_20K_20260808.txt"),
        report.getBytes(StandardCharsets.UTF_8));
    String canonicalConfig = "semanticVersion=v35-dscr-cata-deterministic-cost-v1\n"
        + "decoder=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nseed=20260808\n"
        + "maxEvaluations=20000\n";
    V35FairRunner.writeRecord(baseline, evidence.resolve("runs/baseline-20260808"), canonicalConfig);
    V35FairRunner.writeRecord(full, evidence.resolve("runs/full-20260808"), canonicalConfig);
    V35FairRunner.writeRecord(qg0, evidence.resolve("runs/qg0-20260808"), canonicalConfig);
    V35FairRunner.writeRecord(qg1, evidence.resolve("runs/qg1-20260808"), canonicalConfig);
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
    double cmax = Double.POSITIVE_INFINITY, tec = Double.POSITIVE_INFINITY,
        twc = Double.POSITIVE_INFINITY;
    for (double[] value : front) {
      cmax = Math.min(cmax, value[0]);
      tec = Math.min(tec, value[1]);
      twc = Math.min(twc, value[2]);
    }
    return cmax + "," + tec + "," + twc;
  }
}
