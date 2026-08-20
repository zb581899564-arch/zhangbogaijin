package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC-2: the dynamic local-FE budget scheduler.  Unit pins for the beta
 * schedule and the per-cycle hard budget, the archive-stability contract
 * (a null budget must not change the frozen configuration hash), and one
 * 20000 FE integration contrast on 20_2_3_1: with pacing active the local
 * share must fall inside the beta band while the legacy null configuration
 * keeps its LS=30 behaviour.
 */
public class V35Fc2LocalFePacingTest {
  private static final long SEED = 20260808L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 20000;
  /** configurationHash of the FC-0 A4-PREFINAL archive (no local budget). */
  private static final String PREFINAL_HASH =
      "116393b4e074c1918e1f0983adf32c9312ba439e9a8f99a7436ebf30d79b6e76";

  @Test public void betaScheduleFollowsTheQuadraticRamp() {
    V35LocalFeBudgetConfiguration budget = V35LocalFeBudgetConfiguration.of(0.25, 0.65);
    assertEquals(0.25, budget.betaAt(0.0), 1e-12);
    assertEquals(0.65, budget.betaAt(1.0), 1e-12);
    assertEquals(0.35, budget.betaAt(0.5), 1e-12);
    assertEquals(0.25, budget.betaAt(-3.0), 1e-12);
    assertEquals(0.65, budget.betaAt(7.0), 1e-12);
  }

  @Test public void perCycleBudgetMatchesTheSharedFormula() {
    V35LocalFeBudgetConfiguration budget = V35LocalFeBudgetConfiguration.of(0.25, 0.65);
    // u=0 -> B_L = floor(0.25/0.75 * 5000) = 1666
    assertEquals(1666L, budget.localBudgetFor(0.0, 5000L));
    // u=1 -> B_L = floor(0.65/0.35 * 5000) = 9285
    assertEquals(9285L, budget.localBudgetFor(1.0, 5000L));
    assertEquals(0L, budget.localBudgetFor(0.5, 0L));
  }

  @Test public void invalidRangesAreRejected() {
    double[][] bad = {{0.0, 0.5}, {0.3, 0.3}, {0.6, 0.4}, {0.2, 1.0}, {-0.1, 0.5}};
    for (double[] range : bad) {
      try {
        V35LocalFeBudgetConfiguration.of(range[0], range[1]);
        throw new AssertionError("must reject " + range[0] + "/" + range[1]);
      } catch (IllegalArgumentException expected) {
        // expected
      }
    }
  }

  @Test public void nullBudgetKeepsThePrefinalArchiveHash() {
    V35ProductionConfiguration archived = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(500000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    assertEquals(PREFINAL_HASH, archived.configurationHash());
    V35ProductionConfiguration paced = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(500000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10)
        .localFeBudget(V35LocalFeBudgetConfiguration.of(0.25, 0.65)).build();
    assertNotEquals(PREFINAL_HASH, paced.configurationHash());
    assertTrue(paced.canonicalText().contains("localFeBudget.betaMin=0.250000"));
  }

  @Test(timeout = 600000) public void pacingCapsTheLocalShareOnARealRun() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    RunResult legacy = runA4(project, null);
    RunResult paced = runA4(project, V35LocalFeBudgetConfiguration.of(0.25, 0.65));
    assertEquals(20000, legacy.fullEvaluations);
    assertTrue("legacy LS=30 local share must stay heavy, got " + legacy.localFraction,
        legacy.localFraction > 0.6);
    assertTrue("paced run must respect the total budget, got " + paced.fullEvaluations,
        paced.fullEvaluations <= BUDGET);
    assertTrue("paced local share must fall inside the beta band, got "
        + paced.localFraction, paced.localFraction >= 0.2 && paced.localFraction <= 0.7);
    assertTrue("paced run must keep real search activity (multiple outer cycles), got "
        + paced.outerCycles, paced.outerCycles >= 2);
  }

  private static final class RunResult {
    final long fullEvaluations;
    final double localFraction;
    final long outerCycles;
    RunResult(long fullEvaluations, double localFraction, long outerCycles) {
      this.fullEvaluations = fullEvaluations;
      this.localFraction = localFraction;
      this.outerCycles = outerCycles;
    }
  }

  private RunResult runA4(Path root, V35LocalFeBudgetConfiguration budget) throws Exception {
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, SEED,
        root.resolve("java-jmetal58/instance-extensions/v1"),
        root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration.Builder builder = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(BUDGET)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10);
    if (budget != null) builder.localFeBudget(budget);
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, POPULATION,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(builder.build())
        .setFormalBaselineConfiguration(ZhangBoFormalHmopsoQgsConfiguration.table9())
        .setMaxIterations(BUDGET)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.run();
    long localFe = algorithm.getFormalCriticalFactorySwapEvaluations()
        + algorithm.getFormalCriticalFactoryInsertEvaluations()
        + algorithm.getFormalOriginalNeighborhoodEvaluations()
        + algorithm.getCaTaTestCalls() + algorithm.getCaTaApplyCalls();
    long total = problem.getEvaluationCounter().getSuccessfulEvaluations();
    return new RunResult(total, total == 0 ? 0.0 : (double) localFe / total,
        algorithm.getFormalBaselineOuterCycles());
  }
}
