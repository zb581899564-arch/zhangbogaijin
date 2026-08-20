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
 * V35-FC-5: the Cmax lifecycle audit is pure observation.
 *
 * <p>Acceptance: (a) the audit collects a non-empty four-layer funnel, G1 conditional
 * GIR counts and lineage records on a 20000 FE run; (b) repeated runs from the same
 * frozen initial population produce byte-identical fronts and FE counts — the audit
 * never enters any decision path.</p>
 */
public class V35Fc5CmaxLifecycleAuditTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int REPLAY_FES = 20000;

  @Test(timeout = 600000)
  public void auditCollectsFourLayersAndIsBehaviourNeutral() throws Exception {
    String hash1 = runReplay();
    String hash2 = runReplay();
    assertEquals("front hash must be identical across replays (audit is pure observation)",
        hash1, hash2);
    assertEquals("FE must be identical across replays",
        lastFe1, lastFe2);
    V35CmaxLifecycleAudit audit = lastAudit;
    assertNotNull("audit must be attached", audit);
    assertFalse("funnel must have at least one cycle row", audit.cycleRows().isEmpty());
    V35CmaxLifecycleAudit.CycleRow first = audit.cycleRows().get(0);
    assertTrue("cycle 1 must observe a finite bestCmax after Q rounds",
        Double.isFinite(first.bestCmaxAfterQRounds));
    assertTrue("cycle 1 must observe a finite bestCmax final",
        Double.isFinite(first.bestCmaxFinal));
    assertFalse("G1 conditional GIR must have observed offspring",
        audit.summaryText().contains("no offspring observed"));
    assertFalse("lineage records must be non-empty", audit.lineageRecords().isEmpty());
    String summary = audit.summaryText();
    assertTrue("summary must contain the funnel header", summary.contains("--funnel"));
    assertTrue("summary must contain G1 conditional GIR",
        summary.contains("cmaxImproved"));
  }

  private long lastFe1 = -1L;
  private long lastFe2 = -1L;
  private V35CmaxLifecycleAudit lastAudit;

  private String runReplay() throws Exception {
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
    lastAudit = algorithm.getV35CmaxLifecycleAudit();
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
    else lastFe2 = fe;
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