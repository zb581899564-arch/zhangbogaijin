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

/** FC5-T's observer must not alter the frozen A2 path. */
public class V35Fc5TransferTelemetryEquivalenceTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 2_000;

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void transferTelemetryIsBehaviorallyNeutral() throws Exception {
    java.nio.file.Path root = java.nio.file.Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    ZhangBoCanonicalProductionProblem controlProblem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    ZhangBoCanonicalProductionProblem observedProblem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("EADHFSP/20_2_3_1.txt"), ProductionDecodeMode.FM3, SEED,
        root.resolve("instance-extensions/v1"), root.resolve("fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<PermutationSolution<Integer>>();
    for (int index = 0; index < POPULATION; index++) initial.add(controlProblem.createSolution());
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A2_CFVF, SEED, POPULATION, BUDGET);

    V35FairRunner.RunRecord control = V35FairRunner.run(V35FairRunner.Mode.V35_A2,
        (Problem) controlProblem, initial, BUDGET, SEED, false,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
    V35FairRunner.RunRecord observed = V35FairRunner.runFc5TransferDiagnostic(
        V35FairRunner.Mode.V35_A2, (Problem) observedProblem, initial, BUDGET, SEED,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration);

    assertEquals("COMPLETED", control.getStatus());
    assertEquals("COMPLETED", observed.getStatus());
    assertEquals(control.getInitialPopulationHash(), observed.getInitialPopulationHash());
    assertEquals(control.getFullEvaluations(), observed.getFullEvaluations());
    assertEquals(control.getDecoderCalls(), observed.getDecoderCalls());
    assertEquals(control.getEvaluationTraceHash(), observed.getEvaluationTraceHash());
    assertEquals(frontText(control.getFront()), frontText(observed.getFront()));
    assertEquals(control.getObservationEvidence().getQpEventStreamHash(),
        observed.getObservationEvidence().getQpEventStreamHash());
    assertTrue(observed.getFc5TransferMergeRoundsCsv().startsWith(
        "seed,cycle,fe,Nmerge,Nunique,Nnd,Roverflow\n"));
    assertTrue(observed.getFc5TransferSummary().contains("schema=FC5_100JOB_TRANSFER_V1"));
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder();
    for (double[] point : front) {
      text.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return text.toString();
  }
}
