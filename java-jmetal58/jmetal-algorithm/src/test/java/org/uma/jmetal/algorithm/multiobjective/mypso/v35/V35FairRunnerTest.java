package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

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

public class V35FairRunnerTest {
  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void baselineAndFullShareControlledStart() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", root.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem canonical = ZhangBoV35ProblemFactory.create(
        source.getFatigueInstanceData(), source.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < 10; i++) initial.add(canonical.createSolution());
    V35FairRunner.RunRecord baseline = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        (Problem) canonical, initial, 2000, 20260808L);
    V35FairRunner.RunRecord full = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) canonical, initial, 2000, 20260808L);
    assertEquals(baseline.getInitialPopulationHash(), full.getInitialPopulationHash());
    assertFalse(baseline.getFront().isEmpty());
    assertFalse(full.getFront().isEmpty());
    assertNotNull(full.getCmaxAudit());
    assertFalse(full.getCmaxAudit().getCheckpoints().isEmpty());
    assertTrue(full.getCmaxAudit().recordsCsv().contains("candidateId,parentId,lineageId"));
    assertTrue(full.getDscrEvents().contains("decisionCycle,generation,FE,group,cacheType"));
    assertTrue(full.getDscrTeacherUses().contains("teacherId,teacherObjectives,dominated"));
    assertTrue(full.getMechanismSummary().contains("dturDefined=true"));
    assertTrue(full.getMechanismSummary().contains("dominatedTeacherUses=0"));
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void formalBaselineReplaysWithSameSeed() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", root.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW sourceA = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceB = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem problemA = ZhangBoV35ProblemFactory.create(
        sourceA.getFatigueInstanceData(), sourceA.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem problemB = ZhangBoV35ProblemFactory.create(
        sourceB.getFatigueInstanceData(), sourceB.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    List<PermutationSolution<Integer>> initialA = new ArrayList<>();
    List<PermutationSolution<Integer>> initialB = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      initialA.add(problemA.createSolution());
      initialB.add(problemB.createSolution());
    }
    V35FairRunner.RunRecord first = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        (Problem) problemA, initialA, 500, 20260808L);
    V35FairRunner.RunRecord second = V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        (Problem) problemB, initialB, 500, 20260808L);
    assertEquals(first.getInitialPopulationHash(), second.getInitialPopulationHash());
    assertEquals(first.getFullEvaluations(), second.getFullEvaluations());
    assertEquals(frontText(first.getFront()), frontText(second.getFront()));
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void formalFullReplaysWithSameSeed() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", root.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW sourceA = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceB = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem problemA = ZhangBoV35ProblemFactory.create(
        sourceA.getFatigueInstanceData(), sourceA.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem problemB = ZhangBoV35ProblemFactory.create(
        sourceB.getFatigueInstanceData(), sourceB.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    List<PermutationSolution<Integer>> initialA = new ArrayList<>();
    List<PermutationSolution<Integer>> initialB = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      initialA.add(problemA.createSolution());
      initialB.add(problemB.createSolution());
    }
    V35FairRunner.RunRecord first = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) problemA, initialA, 500, 20260808L);
    V35FairRunner.RunRecord second = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) problemB, initialB, 500, 20260808L);
    assertEquals(first.getInitialPopulationHash(), second.getInitialPopulationHash());
    assertEquals(first.getFullEvaluations(), second.getFullEvaluations());
    assertEquals(frontText(first.getFront()), frontText(second.getFront()));
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void fullIsNotAffectedByPriorBaselineInSameJvm() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", root.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW sourceA = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceB = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceC = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem fullProblemA = ZhangBoV35ProblemFactory.create(
        sourceA.getFatigueInstanceData(), sourceA.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem baselineProblem = ZhangBoV35ProblemFactory.create(
        sourceB.getFatigueInstanceData(), sourceB.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    ZhangBoCanonicalProductionProblem fullProblemB = ZhangBoV35ProblemFactory.create(
        sourceC.getFatigueInstanceData(), sourceC.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);
    List<PermutationSolution<Integer>> initialA = new ArrayList<>();
    List<PermutationSolution<Integer>> initialB = new ArrayList<>();
    List<PermutationSolution<Integer>> initialC = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      initialA.add(fullProblemA.createSolution());
      initialB.add(baselineProblem.createSolution());
      initialC.add(fullProblemB.createSolution());
    }
    final int sequenceIsolationBudget = 20000;
    V35FairRunner.RunRecord fullBefore = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) fullProblemA, initialA, sequenceIsolationBudget, 20260808L);
    V35FairRunner.run(V35FairRunner.Mode.V35_BASELINE,
        (Problem) baselineProblem, initialB, sequenceIsolationBudget, 20260808L);
    V35FairRunner.RunRecord fullAfter = V35FairRunner.run(V35FairRunner.Mode.V35_FULL,
        (Problem) fullProblemB, initialC, sequenceIsolationBudget, 20260808L);
    assertEquals(fullBefore.getStopReason(), "COMPLETED", fullBefore.getStatus());
    assertEquals(fullAfter.getStopReason(), "COMPLETED", fullAfter.getStatus());
    assertEquals(fullBefore.getInitialPopulationHash(), fullAfter.getInitialPopulationHash());
    assertEquals(fullBefore.getFullEvaluations(), fullAfter.getFullEvaluations());
    assertEquals(frontText(fullBefore.getFront()), frontText(fullAfter.getFront()));
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void shadowAuditUsesSeparateBudgetAndDoesNotChangeMainSearch() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir", root.resolve("instance-extensions/v1").toString());
    ZhangBoEDHHFSPW sourceA = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoEDHHFSPW sourceB = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem problemA = ZhangBoV35ProblemFactory.create(
        sourceA.getFatigueInstanceData(), sourceA.getFatigueParameters(), ProductionDecodeMode.FM3, 20260814L);
    ZhangBoCanonicalProductionProblem problemB = ZhangBoV35ProblemFactory.create(
        sourceB.getFatigueInstanceData(), sourceB.getFatigueParameters(), ProductionDecodeMode.FM3, 20260814L);
    List<PermutationSolution<Integer>> initialA = new ArrayList<>();
    List<PermutationSolution<Integer>> initialB = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      initialA.add(problemA.createSolution());
      initialB.add(problemB.createSolution());
    }
    V35FairRunner.RunRecord off = V35FairRunner.run(V35FairRunner.Mode.V35_FULL_POOL_OFF,
        (Problem) problemA, initialA, 1000, 20260814L, false,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow());
    V35FairRunner.RunRecord on = V35FairRunner.run(V35FairRunner.Mode.V35_FULL_POOL_OFF,
        (Problem) problemB, initialB, 1000, 20260814L, false,
        V35BottleneckDiagnosisConfiguration.calibrationAudit());
    assertEquals("COMPLETED", off.getStatus());
    assertEquals(on.getStopReason(), "COMPLETED", on.getStatus());
    assertEquals(off.getInitialPopulationHash(), on.getInitialPopulationHash());
    assertEquals(off.getFullEvaluations(), on.getFullEvaluations());
    assertEquals(frontText(off.getFront()), frontText(on.getFront()));
    assertEquals(off.getCaTaEvents(), on.getCaTaEvents());
    assertTrue(on.getShadowSamples() > 0L);
    assertTrue(on.getShadowEvaluations() > 0);
    assertTrue(on.getShadowProbes().startsWith(V35ShadowDiagnosisAudit.HEADER));
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder();
    for (double[] point : front) {
      text.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return text.toString();
  }
}
