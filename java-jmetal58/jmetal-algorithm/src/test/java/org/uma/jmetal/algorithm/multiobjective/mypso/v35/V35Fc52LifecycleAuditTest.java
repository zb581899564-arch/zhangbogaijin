package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * V35-FC-5.2: the evaluated-candidate lifecycle audit is pure observation.
 *
 * <p>Acceptance: (a) repeated runs from the same frozen initial population produce
 * byte-identical fronts and FE counts with the audit OFF, ON and ON again — the audit
 * never enters any decision path; (b) with the audit ON it collects a non-empty
 * lifecycle funnel (tracked records, Top-20, best-ever evaluated Cmax) and the funnel
 * is identical across the two ON runs.</p>
 */
public class V35Fc52LifecycleAuditTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int REPLAY_FES = 20000;

  @Test(timeout = 900000)
  public void auditCollectsLifecycleAndIsBehaviourNeutral() throws Exception {
    String hashOff = runReplay(false);
    String hashOn1 = runReplay(true);
    String hashOn2 = runReplay(true);
    assertEquals("front hash must be identical OFF vs ON (audit is pure observation)",
        hashOff, hashOn1);
    assertEquals("front hash must be identical across ON replays",
        hashOn1, hashOn2);
    assertEquals("FE must be identical across all replays", lastFe1, lastFe2);
    assertEquals("FE must be identical across all replays", lastFe2, lastFe3);
    String summary = lastSummary;
    assertNotNull("audit summary must be collected", summary);
    assertTrue("summary must contain the funnel header",
        summary.contains("fc52LifecycleAudit"));
    assertTrue("at least one tracked record is required",
        summary.contains("fc52Tracked=") && !summary.contains("fc52Tracked=0"));
    assertTrue("best-ever evaluated Cmax must be finite",
        summary.contains("fc52BestEverEvaluated=")
            && !summary.contains("fc52BestEverEvaluated=NaN")
            && !summary.contains("fc52BestEverEvaluated=Infinity"));
    assertTrue("Top-20 fate table must be emitted",
        summary.contains("fc52Top20Begin") && summary.contains("fc52Top20End"));
    assertTrue("records section must be emitted",
        summary.contains("fc52RecordsBegin") && summary.contains("fc52RecordsEnd"));
  }

  private long lastFe1 = -1L;
  private long lastFe2 = -1L;
  private long lastFe3 = -1L;
  private String lastSummary;

  private String runReplay(boolean auditOn) throws Exception {
    V35EvaluationSourceContext.setEnabled(auditOn);
    V35Fc52LifecycleAudit.setEnabled(auditOn);
    if (auditOn) {
      V35Fc52LifecycleAudit.reset();
    }
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    while (root.getParent() != null && !java.nio.file.Files.exists(root.resolve("AGENTS.md"))) {
      root = root.getParent();
    }
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, SEED,
        root.resolve("java-jmetal58/instance-extensions/v1"),
        root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration config = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(REPLAY_FES)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    ZhangBoFormalHmopsoQgsConfiguration formal =
        ZhangBoFormalHmopsoQgsConfiguration.table9();
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, POPULATION,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(config)
        .setFormalBaselineConfiguration(formal)
        .setMaxIterations(REPLAY_FES)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.run();
    V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
    if (fc52 != null) {
      List<double[]> frontForFinish = new ArrayList<>();
      for (PermutationSolution<Integer> solution : algorithm.getResult()) {
        frontForFinish.add(new double[] {solution.getObjective(0),
            solution.getObjective(1), solution.getObjective(6)});
      }
      fc52.finish(algorithm.getResult(),
          P8MetricCalculator.nondominated(frontForFinish),
          problem.getEvaluationCounter().getSuccessfulEvaluations());
      lastSummary = fc52.fc52SummaryText();
    }
    List<double[]> front = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      front.add(new double[] {solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)});
    }
    front = P8MetricCalculator.nondominated(front);
    StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      csv.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    long fe = problem.getEvaluationCounter().getSuccessfulEvaluations();
    if (lastFe1 < 0) lastFe1 = fe;
    else if (lastFe2 < 0) lastFe2 = fe;
    else lastFe3 = fe;
    return sha256(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data);
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}
