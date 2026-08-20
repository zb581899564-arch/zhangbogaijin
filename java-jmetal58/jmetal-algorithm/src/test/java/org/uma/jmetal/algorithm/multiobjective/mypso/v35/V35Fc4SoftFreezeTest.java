package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * V35-FC-4: contribution-gated soft freeze.  (a) {@code rho=0} is the hard
 * freeze byte-for-byte (canonical text and validation).  (b) The scaled-alpha
 * TD update is exactly the hard update scaled by {@code rho} on a zeroed
 * table.  (c) Integration: with {@code rho=0.3} the frozen Qg accumulates
 * strictly more TD updates than the hard freeze at equal budget, and the
 * run still closes its FE budget.
 */
public class V35Fc4SoftFreezeTest {
  private static final long SEED = 20260808L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 20000;

  @Test public void rhoZeroIsTheHardFreezeByteForByte() {
    assertEquals(ZhangBoDualQCoordinationConfiguration.blockFrozen().toCanonicalText(),
        ZhangBoDualQCoordinationConfiguration
            .blockFrozenSoftFreeze(0.10, 5, 5, 0.0).toCanonicalText());
    assertEquals(0.0,
        ZhangBoDualQCoordinationConfiguration.blockFrozen().getSoftFreezeRho(), 0.0);
    assertTrue(ZhangBoDualQCoordinationConfiguration
        .blockFrozenSoftFreeze(0.10, 5, 5, 0.3).toCanonicalText()
        .contains("dualQ.softFreezeRho=0.3"));
    try {
      ZhangBoDualQCoordinationConfiguration.blockFrozenSoftFreeze(0.10, 5, 5, 1.5);
      throw new AssertionError("rho must be bounded to [0,1]");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test public void scaledAlphaUpdateIsTheRhoScaledHardUpdate() {
    List<PermutationSolution<Integer>> before = Arrays.<PermutationSolution<Integer>>asList(
        solution(10, 20, 30), solution(20, 40, 60));
    List<PermutationSolution<Integer>> after = Arrays.<PermutationSolution<Integer>>asList(
        solution(8, 18, 27), solution(18, 36, 54));
    ZhangBoQgController hard = new ZhangBoQgController(
        new JavaRandomGenerator(20260808L), 1.0, 1.0, 0.8);
    ZhangBoQgController soft = new ZhangBoQgController(
        new JavaRandomGenerator(20260808L), 1.0, 1.0, 0.8);
    ZhangBoQgController.Selection hardSelection =
        hard.select(ZhangBoSubSwarm.G1_CMAX, before);
    ZhangBoQgController.Selection softSelection =
        soft.select(ZhangBoSubSwarm.G1_CMAX, before);
    hard.settle(hardSelection, before, after);
    soft.settleWithScaledAlpha(softSelection, before, after, 0.3);
    double hardValue = hard.getTable(ZhangBoSubSwarm.G1_CMAX)
        [hardSelection.getState()][hardSelection.getAction()];
    double softValue = soft.getTable(ZhangBoSubSwarm.G1_CMAX)
        [softSelection.getState()][softSelection.getAction()];
    assertEquals(0.3 * hardValue, softValue, 1e-12);
    assertEquals(1L, soft.getSoftTdUpdateCount());
    try {
      soft.settleWithScaledAlpha(softSelection, before, after, 0.0);
      throw new AssertionError("scale must be strictly positive");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test(timeout = 600000) public void softFreezeRaisesFrozenQgLearningAtEqualBudget()
      throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    RunOutcome hard = runA4(project, ZhangBoDualQCoordinationConfiguration.blockFrozen());
    RunOutcome soft = runA4(project,
        ZhangBoDualQCoordinationConfiguration.blockFrozenSoftFreeze(0.10, 5, 5, 0.3));
    assertTrue("soft freeze must raise the frozen-side Qg TD count ("
        + soft.qgTdUpdates + " vs " + hard.qgTdUpdates + ")",
        soft.qgTdUpdates > hard.qgTdUpdates);
    assertTrue("both arms must respect the budget",
        hard.fullEvaluations <= BUDGET && soft.fullEvaluations <= BUDGET);
    assertTrue("soft arm must keep real Qg learning (>= the hard count)",
        soft.qgTdUpdates >= hard.qgTdUpdates);
  }

  private static final class RunOutcome {
    final long fullEvaluations;
    final long qgTdUpdates;
    RunOutcome(long fullEvaluations, long qgTdUpdates) {
      this.fullEvaluations = fullEvaluations;
      this.qgTdUpdates = qgTdUpdates;
    }
  }

  private RunOutcome runA4(Path root, ZhangBoDualQCoordinationConfiguration coordination) throws Exception {
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, SEED,
        root.resolve("java-jmetal58/instance-extensions/v1"),
        root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration config = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(BUDGET)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10)
        .dualQCoordination(coordination).build();
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, POPULATION,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(config)
        .setFormalBaselineConfiguration(ZhangBoFormalHmopsoQgsConfiguration.table9())
        .setMaxIterations(BUDGET)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.run();
    return new RunOutcome(problem.getEvaluationCounter().getSuccessfulEvaluations(),
        algorithm.getQgTdUpdateCount());
  }

  private static PermutationSolution<Integer> solution(double cmax, double tec, double twc) {
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 11);
    solution.setObjective(0, cmax);
    solution.setObjective(1, tec);
    solution.setObjective(6, twc);
    return solution;
  }
}
