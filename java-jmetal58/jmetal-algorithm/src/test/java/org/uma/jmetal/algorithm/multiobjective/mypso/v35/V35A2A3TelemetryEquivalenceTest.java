package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/** Proves that A2-to-A3 causal telemetry is a pure observer. */
public class V35A2A3TelemetryEquivalenceTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  // The phase guard may safely stop at the initial population; behavior equivalence is the unit gate.
  private static final int BUDGET = 2_000;

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void diagnosticTelemetryDoesNotChangeA3AtPhaseBoundedBudget() throws Exception {
    java.nio.file.Path root = java.nio.file.Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    java.nio.file.Path instance = root.resolve("EADHFSP/20_2_3_1.txt");
    java.nio.file.Path extension = root.resolve("instance-extensions/v1");
    java.nio.file.Path fatigue = root.resolve("fatigue-parameters/v1");
    ZhangBoCanonicalProductionProblem controlProblem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, SEED, extension, fatigue, ZhangBoShiftConfiguration.none());
    ZhangBoCanonicalProductionProblem observedProblem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, SEED, extension, fatigue, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(controlProblem.createSolution());
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE, SEED, POPULATION, BUDGET);

    V35FairRunner.RunRecord control = V35FairRunner.run(V35FairRunner.Mode.V35_A3,
        (Problem) controlProblem, initial, BUDGET, SEED, false,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
    V35FairRunner.RunRecord observed = V35FairRunner.runA2A3Diagnostic(V35FairRunner.Mode.V35_A3,
        (Problem) observedProblem, initial, BUDGET, SEED,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration);

    assertEquals("COMPLETED", control.getStatus());
    assertEquals("COMPLETED", observed.getStatus());
    assertEquals(control.getInitialPopulationHash(), observed.getInitialPopulationHash());
    assertEquals(control.getFullEvaluations(), observed.getFullEvaluations());
    assertEquals(control.getEvaluationTraceHash(), observed.getEvaluationTraceHash());
    assertEquals(frontText(control.getFront()), frontText(observed.getFront()));
    assertEquals(control.getObservationEvidence().getQpTableHash(),
        observed.getObservationEvidence().getQpTableHash());
    assertEquals(control.getObservationEvidence().getQpEventStreamHash(),
        observed.getObservationEvidence().getQpEventStreamHash());
    assertEquals(control.getObservationEvidence().getDualQEventStreamHash(),
        observed.getObservationEvidence().getDualQEventStreamHash());
    assertEquals("generation,FE,group,branchId,lineageId,source,action,mask,archiveSize,"
        + "selectedPbestFingerprint,fallback\n", observed.getA2A3PersonalLeaderAuditCsv());
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void observeOnlyQpUsesActionsAndArchiveButDoesNotCreateTdTransitions() throws Exception {
    final int observeBudget = 6_000;
    java.nio.file.Path root = java.nio.file.Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(observeBudget)
        .decoderMode(ProductionDecodeMode.FM3).qg(true).dscr(true).cfvf(true).qp(true)
        .caTaLite(false).directionalTeacherPool(false)
        .pddrSelectionMode(org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED)
        .personalLeaderMode(V35PersonalLeaderMode.QP_FOUR_ACTIONS)
        .dualQCoordination(org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration.synchronous())
        .qpSettlementPolicy(V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES).build();
    V35FairRunner.RunRecord record = V35FairRunner.runA2A3Diagnostic(
        V35FairRunner.Mode.V35_DIAG_QP_OBSERVE_ONLY, (Problem) problem, initial, observeBudget,
        SEED, V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration);
    assertEquals("COMPLETED", record.getStatus());
    assertTrue(record.getFullEvaluations() > POPULATION);
    assertEquals(record.getFullEvaluations(), record.getDecoderCalls());
    assertTrue(record.getObservationEvidence().getQpEventCount() > 0L);
    assertEquals(0L, record.getObservationEvidence().getQpTrainedTransitions());
    assertTrue(record.getObservationEvidence().getQpFrozenObservations() > 0L);
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder();
    for (double[] point : front) {
      text.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return text.toString();
  }
}
