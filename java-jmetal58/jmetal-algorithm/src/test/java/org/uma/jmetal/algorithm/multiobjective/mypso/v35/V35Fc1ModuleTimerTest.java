package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * V35-FC-TIME-1: the module timer is pure observation.
 *
 * <p>Acceptance: (a) with profiling enabled, the sum of recorded module
 * nanoseconds explains most of the wall-clock run time (>= 80% after the
 * instrumentation's own overhead); (b) enabling profiling must reproduce the
 * exact same final nondominated front hash and FE count as profiling
 * disabled — i.e. the timer never enters any decision path.</p>
 */
public class V35Fc1ModuleTimerTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int REPLAY_FES = 20000;

  @Test(timeout = 600000)
  public void moduleCoverageExplainsRuntime() throws Exception {
    V35ModuleTimer.setEnabled(true);
    V35ModuleTimer.reset();
    try {
      long start = System.nanoTime();
      runReplay(false);
      long wall = System.nanoTime() - start;
      Map<String, long[]> stats = V35ModuleTimer.snapshot();
      long total = 0L;
      for (long[] s : stats.values()) total += s[1];
      assertTrue("module timer must have recorded samples, totalNanos was " + total,
          total > 0L);
      double ratio = (double) total / wall;
      assertTrue("module sum should explain >= 80% of wall clock, was "
          + String.format("%.1f%%", 100.0 * ratio) + " (decoded FE="
          + lastFrontHash + ")", ratio >= 0.80);
      // The big three must all have been recorded.
      assertTrue("DECODE module must be recorded",
          stats.containsKey(V35ModuleTimer.DECODE));
      assertTrue("CFVF module must be recorded",
          stats.containsKey(V35ModuleTimer.CFVF));
      assertTrue("Qg module must be recorded",
          stats.containsKey(V35ModuleTimer.QG));
      // Per-cycle lines must have been captured.
      assertFalse("per-cycle lines must be captured",
          lastPerCycleLines.isEmpty());
      assertTrue("per-cycle header must carry fe and archiveSize",
          lastPerCycleLines.get(0).contains("fe=")
              && lastPerCycleLines.get(0).contains("archiveSize="));
    } finally {
      V35ModuleTimer.setEnabled(false);
      V35ModuleTimer.reset();
    }
  }

  @Test(timeout = 600000)
  public void profilingMustNotChangeBehaviour() throws Exception {
    V35ModuleTimer.setEnabled(false);
    String plain = runReplay(false);
    V35ModuleTimer.setEnabled(true);
    V35ModuleTimer.reset();
    String profiled = runReplay(false);
    V35ModuleTimer.setEnabled(false);
    V35ModuleTimer.reset();
    assertEquals("front hash must be identical with profiling on/off", plain, profiled);
    assertEquals("FE count must be identical with profiling on/off",
        lastFrontFeProfiled, lastFrontFePlain);
  }

  private String lastFrontHash;
  private long lastFrontFePlain = -1L;
  private long lastFrontFeProfiled = -1L;
  private List<String> lastPerCycleLines = new ArrayList<>();

  private String runReplay(boolean unused) throws Exception {
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
    if (V35ModuleTimer.isEnabled()) {
      lastPerCycleLines = algorithm.v35ModulePerCycleLines();
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
    lastFrontHash = sha256(csv.toString().getBytes(StandardCharsets.UTF_8));
    long fe = problem.getEvaluationCounter().getSuccessfulEvaluations();
    if (V35ModuleTimer.isEnabled()) lastFrontFeProfiled = fe;
    else lastFrontFePlain = fe;
    return lastFrontHash;
  }

  private static String sha256(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data);
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}