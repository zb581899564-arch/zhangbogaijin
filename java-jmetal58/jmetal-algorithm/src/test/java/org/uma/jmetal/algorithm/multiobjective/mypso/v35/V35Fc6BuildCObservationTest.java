package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * V35-FC-6A-POST / Build-C2: the BP-PDDR stability diagnostic audit is pure
 * observation and deterministic.
 *
 * <p>Acceptance: (a) repeated replays from the same frozen initial population
 * produce byte-identical fronts and FE counts with the audit OFF, ON and ON
 * again; (b) with the audit ON it collects a non-empty, deterministic
 * {@code fc6Diag} block covering rounds (counterfactual), rescue events,
 * per-cycle geometry and exposures.</p>
 */
public class V35Fc6BuildCObservationTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int REPLAY_FES = 20000;

  @Test(timeout = 900000)
  public void diagCollectsAndIsBehaviourNeutral() throws Exception {
    String hashOff = runReplay(false);
    String hashOn1 = runReplay(true);
    String hashOn2 = runReplay(true);
    assertEquals("front hash must be identical OFF vs ON (audit is pure observation)",
        hashOff, hashOn1);
    assertEquals("front hash must be identical across ON replays",
        hashOn1, hashOn2);
    assertEquals("FE must be identical across all replays", lastFe1, lastFe2);
    assertEquals("FE must be identical across all replays", lastFe2, lastFe3);
    assertEquals("collected FE must match the algorithm counter", lastFe3, lastCollectedFe);

    String diag = lastDiagText;
    assertNotNull("diag text must be collected", diag);
    assertTrue("diag block must be bracketed",
        diag.contains("fc6DiagBegin\n") && diag.contains("fc6DiagEnd\n"));
    assertTrue("rounds section must be emitted",
        diag.contains("fc6diagRoundBegin\n") && diag.contains("fc6diagRoundEnd\n"));
    assertTrue("rescue section must be emitted",
        diag.contains("fc6diagRescueBegin\n") && diag.contains("fc6diagRescueEnd\n"));
    assertTrue("exposure section must be emitted",
        diag.contains("fc6diagExposureBegin\n") && diag.contains("fc6diagExposureEnd\n"));
    assertTrue("per-cycle geometry must be non-empty",
        diag.contains("fc6diagCycleBegin\n") && diag.contains("fc6diagCycleEnd\n")
            && countLines(diag, "fc6diagCycle ") > 0);
    assertTrue("at least one PDDR round must be observed",
        containsValue(diag, "fc6diagRounds=") > 0);
    assertTrue("rescue/displacement totals must be present and numeric",
        diag.contains("fc6diagActualRescues=") && diag.contains("fc6diagDisplacements="));
    assertTrue("FC-6A.1 composition section must be emitted",
        diag.contains("fc6diagCompBegin\n") && diag.contains("fc6diagCompEnd\n")
            && diag.contains("fc6diagCompSummary rounds=") && countLines(diag, "fc6diagComp ") > 0);
    assertTrue("FC-6A.2 region section must be emitted",
        diag.contains("fc6diagRegionBegin\n") && diag.contains("fc6diagRegionEnd\n")
            && diag.contains("fc6diagRegionSummary rounds=")
            && countLines(diag, "fc6diagRegion ") > 0);
    assertTrue("FC-6A.2 region record must carry all region/rejection fields",
        diag.contains("g1Lt1=") && diag.contains("g2Lt1=") && diag.contains("g3Lt1=")
            && diag.contains("g4Lt1=") && diag.contains("ovfLt1=")
            && diag.contains("rejG1=") && diag.contains("rejOvf=")
            && diag.contains("absorbable="));
    assertTrue("FC-6A.2 probe must be absent when not configured",
        !diag.contains("fc6diagProbeBegin"));
  }

  private long lastFe1 = -1L;
  private long lastFe2 = -1L;
  private long lastFe3 = -1L;
  private long lastCollectedFe = -1L;
  private String lastDiagText;

  private String runReplay(boolean auditOn) throws Exception {
    V35EvaluationSourceContext.setEnabled(auditOn);
    V35Fc52LifecycleAudit.setEnabled(auditOn);
    V35Fc6BpPddrDiagnosticAudit.setEnabled(auditOn);
    if (auditOn) {
      V35Fc52LifecycleAudit.reset();
      V35Fc6BpPddrDiagnosticAudit.reset();
      V35Fc6BpPddrDiagnosticAudit.setSeed(SEED);
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
    List<double[]> frontForFinish = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      frontForFinish.add(new double[] {solution.getObjective(0),
          solution.getObjective(1), solution.getObjective(6)});
    }
    frontForFinish = P8MetricCalculator.nondominated(frontForFinish);
    long fe = problem.getEvaluationCounter().getSuccessfulEvaluations();
    V35Fc6BpPddrDiagnosticAudit diag = V35Fc6BpPddrDiagnosticAudit.current();
    if (diag != null) {
      diag.finish(frontForFinish);
      lastDiagText = diag.fc6DiagText();
      lastCollectedFe = fe;
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
    if (lastFe1 < 0) lastFe1 = fe;
    else if (lastFe2 < 0) lastFe2 = fe;
    else lastFe3 = fe;
    return sha256(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static int countLines(String text, String prefix) {
    int count = 0;
    for (String line : text.split("\n")) {
      if (line.startsWith(prefix)) {
        count++;
      }
    }
    return count;
  }

  private static long containsValue(String text, String key) {
    Pattern pattern = Pattern.compile(Pattern.quote(key) + "(\\d+)");
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : -1L;
  }

  private static String sha256(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data);
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}