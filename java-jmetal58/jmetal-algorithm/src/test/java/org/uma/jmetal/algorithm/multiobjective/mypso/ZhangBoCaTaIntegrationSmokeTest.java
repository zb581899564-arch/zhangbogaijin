package org.uma.jmetal.algorithm.multiobjective.mypso;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNeighborhoodCandidateGateway;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/** P7.2 real 20-job, 2-stage, 3-factory CA-TA smoke with a 2,000-FE cap. */
public class ZhangBoCaTaIntegrationSmokeTest {
  @Test(timeout = 180000)
  public void cataPathKeepsEvaluationBudgetClosed() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir",
          project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      JMetalRandom.getInstance().setSeed(20260808L);
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      ZhangBoGlobalSearchConfiguration configuration =
          ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveQpBlockFrozenCaTa(
              0.4, 20260808L);
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQ(
          3, 0.4, 0.2, 0.4, problem, new SequentialSolutionListEvaluator<PermutationSolution<Integer>>(),
          100, 2000, 15, 55, 15, 15, 0.0, 0.8, 0.9, 50.0,
          0.4, 0.3, 0.1, 0.25, 1, configuration);
      algorithm.run();
      System.out.println("P7.2_SMOKE fullFE=" + algorithm.getFullEvaluationCount()
          + ",caTaFE=" + algorithm.getCaTaFullEvaluations()
          + ",testCalls=" + algorithm.getCaTaTestCalls()
          + ",applyCalls=" + algorithm.getCaTaApplyCalls()
          + ",events=" + algorithm.getCaTaEvents().size());
      assertTrue("CA-TA must not exceed MaxFEs", algorithm.getFullEvaluationCount() <= 2000L);
      assertTrue("initial population must be evaluated", algorithm.getFullEvaluationCount() >= 100L);
      assertTrue("CA-TA must have a closed gateway", algorithm.getCaTaFullEvaluations()
          <= algorithm.getFullEvaluationCount());
      assertTrue("ordinary non-profile CA-TA must execute complete evaluations",
          algorithm.getCaTaFullEvaluations() > 0L);
      assertTrue("ordinary non-profile CA-TA must enter Test/Apply",
          algorithm.getCaTaTestCalls() + algorithm.getCaTaApplyCalls() > 0L);
      assertTrue("ordinary non-profile CA-TA must emit events",
          algorithm.getCaTaEvents().size() > 0);
      assertTrue("semantic mapping must be present", algorithm.getSubSwarmRoleMappingHash().length() == 64);
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  @Test(timeout = 180000)
  public void fixedInitialPopulationReplaysCaTaEvents() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir",
          project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      JMetalRandom.getInstance().setSeed(20260808L);
      ZhangBoEDHHFSPW legacy = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      ZhangBoCanonicalProductionProblem problem = new ZhangBoCanonicalProductionProblem(
          legacy.getFatigueInstanceData(), legacy.getFatigueParameters(),
          ProductionDecodeMode.FM3, 20260808L);
      @SuppressWarnings("unchecked")
      Problem<PermutationSolution<Integer>> typedProblem =
          (Problem<PermutationSolution<Integer>>) (Problem<?>) problem;
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < 100; index++) initial.add(problem.createSolution());
      ZhangBoGlobalSearchConfiguration configuration =
          P8ExperimentRegistry.configurationFor(P8ExperimentRegistry.find("FULL"), 20260808L);
      ZhangBoMOHPSOQ first = algorithm(typedProblem, configuration, 400);
      first.setInitialSwarmOverride(initial);
      first.setCaTaNanoClock(deterministicClock());
      first.run();
      ZhangBoMOHPSOQ second = algorithm(typedProblem, configuration, 400);
      second.setInitialSwarmOverride(initial);
      second.setCaTaNanoClock(deterministicClock());
      second.run();
      ZhangBoMOHPSOQ audited = algorithm(typedProblem, configuration, 400);
      audited.setInitialSwarmOverride(initial);
      audited.setCaTaNanoClock(deterministicClock());
      audited.setCmaxAudit(new ZhangBoCmaxAudit(100L));
      audited.run();
      assertEquals(first.getFullEvaluationCount(), second.getFullEvaluationCount());
      assertEquals(first.getFullEvaluationCount(), audited.getFullEvaluationCount());
      assertTrue(first.getCaTaFullEvaluations() > 0L);
      assertTrue(first.getCaTaTestCalls() + first.getCaTaApplyCalls() > 0L);
      assertTrue(first.getCaTaEvents().size() > 0);
      assertEquals(first.getCaTaEvents(), second.getCaTaEvents());
      assertEquals("observation-only audit must not change CA-TA events",
          first.getCaTaEvents(), audited.getCaTaEvents());
      Set<String> governedAttempts = new HashSet<>();
      for (String event : first.getCaTaEvents()) {
        if (!event.contains(",attemptSeed=")) continue;
        String key = caTaAttemptKey(event);
        assertTrue("a governed CA-TA candidate must not be evaluated twice: " + key,
            governedAttempts.add(key));
      }
      assertTrue("the replay must contain governed CA-TA candidate attempts",
          !governedAttempts.isEmpty());
      assertEquals(first.getQgTableHash(), second.getQgTableHash());
      assertEquals(first.getQpTableHash(), second.getQpTableHash());
      assertEquals(first.getQgTableHash(), audited.getQgTableHash());
      assertEquals(first.getQpTableHash(), audited.getQpTableHash());
      assertEquals("observation-only audit must not change the final front",
          resultFingerprint(first.getResult()), resultFingerprint(audited.getResult()));
      assertTrue("deterministic timing must include preview plus full evaluation",
          first.getCaTaStatisticsCanonicalText()
              .contains("averageWallClockNanos=2000.0"));
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  @Test(timeout = 180000)
  public void canonicalB0RunsStructuredBaselineQgPddrAndO1O9() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir",
          project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      JMetalRandom.getInstance().setSeed(20260808L);
      ZhangBoEDHHFSPW legacy = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      ZhangBoCanonicalProductionProblem canonical = new ZhangBoCanonicalProductionProblem(
          legacy.getFatigueInstanceData(), legacy.getFatigueParameters(),
          ProductionDecodeMode.CANONICAL_NO_FATIGUE, 20260808L);
      @SuppressWarnings({"rawtypes", "unchecked"})
      Problem<PermutationSolution<Integer>> problem = (Problem) canonical;
      ZhangBoGlobalSearchConfiguration configuration =
          P8ExperimentRegistry.configurationFor(P8ExperimentRegistry.find("B0"), 20260808L);
      assertTrue(configuration.isStructuredBaselineEnabled());
      assertTrue(configuration.isQgEnabled());
      assertTrue(configuration.isEvaluatedPddrEnabled());
      assertTrue(configuration.isLocalSearchEnabled());
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQ(
          3, 0.4, 0.2, 0.4, problem,
          new SequentialSolutionListEvaluator<PermutationSolution<Integer>>(),
          100, 500, 15, 55, 15, 15, 0.0, 0.8, 0.9, 50.0,
          0.4, 0.3, 0.1, 0.25, 1, configuration);
      algorithm.run();
      assertTrue("structured baseline updater must emit events",
          algorithm.getZhangBoP6Events().toString().contains(":baseline="));
      assertTrue("original Qg must be selected", algorithm.getQgSelectionCount() > 0L);
      assertTrue("evaluated PDDR must run", algorithm.getZhangBoPddrEvents().size() > 0);
      assertTrue("fixed O1-O9 path must be entered",
          algorithm.getCaTaEvents().toString().contains("fixedId="));
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  @Test(timeout = 180000)
  public void formalB0ConsumesConfiguredQAndLocalLoopsAtRuntime() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir",
          project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      ZhangBoEDHHFSPW legacy = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      ZhangBoCanonicalProductionProblem canonical = new ZhangBoCanonicalProductionProblem(
          legacy.getFatigueInstanceData(), legacy.getFatigueParameters(),
          ProductionDecodeMode.CANONICAL_NO_FATIGUE, 20260808L);
      @SuppressWarnings({"rawtypes", "unchecked"})
      Problem<PermutationSolution<Integer>> problem = (Problem) canonical;
      ZhangBoFormalHmopsoQgsConfiguration formal =
          ZhangBoFormalHmopsoQgsConfiguration.engineeringAudit();
      try {
        new ZhangBoMOHPSOQBuilder(problem, 100, 3, 0.0, 0.8, 0.8, 2.0)
            .setMaxIterations(1400).setSwarmSize(100)
            .setPhysicalSubswarmSizes(20, 40, 20, 20)
            .setGlobalSearchConfiguration(
                ZhangBoGlobalSearchConfiguration.forP8(
                    P8ExperimentRegistry.find("B0").getAblationProfile(), 20260808L, 0.6))
            .setFormalBaselineConfiguration(formal)
            .setRand_k(0.4)
            .build();
        fail("runtime parameter drift must be rejected");
      } catch (IllegalArgumentException expected) {
        assertTrue(expected.getMessage().contains("randomCoefficientUpperBound runtime=0.4"));
      }
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, 100, 3,
          0.0, 0.8, 0.8, 2.0)
          .setMaxIterations(1400).setSwarmSize(100)
          .setPhysicalSubswarmSizes(20, 40, 20, 20)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.forP8(
                  P8ExperimentRegistry.find("B0").getAblationProfile(), 20260808L, 0.6))
          .setFormalBaselineConfiguration(formal)
          .build();
      algorithm.run();
      assertEquals(formal.sha256(), algorithm.getFormalBaselineConfiguration().sha256());
      assertEquals(2L, algorithm.getFormalBaselineQgRounds());
      assertEquals(1L, algorithm.getFormalBaselineOuterCycles());
      assertEquals(100L, algorithm.getFormalCriticalFactorySwapEvaluations());
      assertEquals(100L, algorithm.getFormalCriticalFactoryInsertEvaluations());
      assertTrue("formal O1-O9 must consume the remaining complete evaluations",
          algorithm.getFormalOriginalNeighborhoodEvaluations() > 0L);
      assertTrue(algorithm.getFormalOriginalNeighborhoodEvaluations() > 0L);
      assertEquals(100L + 200L
              + algorithm.getFormalCriticalFactorySwapEvaluations()
              + algorithm.getFormalCriticalFactoryInsertEvaluations()
              + algorithm.getFormalOriginalNeighborhoodEvaluations(),
          algorithm.getFullEvaluationCount());
      assertTrue(algorithm.getFullEvaluationCount() <= 1400L);
      assertEquals(8L, algorithm.getQgSelectionCount());
      assertEquals(8L, algorithm.getQgTdUpdateCount());
      assertTrue(algorithm.getZhangBoPddrEventCount() > 0L);
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  private static ZhangBoMOHPSOQ algorithm(Problem<PermutationSolution<Integer>> problem,
      ZhangBoGlobalSearchConfiguration configuration, int maxEvaluations) {
    return new ZhangBoMOHPSOQ(
        3, 0.4, 0.2, 0.4, problem,
        new SequentialSolutionListEvaluator<PermutationSolution<Integer>>(),
        100, maxEvaluations, 15, 55, 15, 15, 0.0, 0.8, 0.9, 50.0,
        0.4, 0.3, 0.1, 0.25, 1, configuration);
  }

  private static ZhangBoNeighborhoodCandidateGateway.NanoClock deterministicClock() {
    return new ZhangBoNeighborhoodCandidateGateway.NanoClock() {
      private long value;
      @Override public long nanoTime() {
        value += 1000L;
        return value;
      }
    };
  }

  private static List<String> resultFingerprint(
      List<PermutationSolution<Integer>> solutions) {
    List<String> result = new ArrayList<>();
    for (PermutationSolution<Integer> solution : solutions) {
      result.add(ZhangBoQgController.fingerprint(solution) + '|'
          + solution.getObjective(0) + '|' + solution.getObjective(1) + '|'
          + solution.getObjective(6));
    }
    java.util.Collections.sort(result);
    return result;
  }

  private static String caTaAttemptKey(String event) {
    return field(event, "generation=") + '|' + field(event, "slot=") + '|'
        + field(event, "lineage=") + '|' + field(event, "id=") + '|'
        + field(event, "contextEpoch=") + '|' + field(event, "callOrdinal=") + '|'
        + field(event, "repetition=") + '|' + field(event, "attemptSeed=");
  }

  private static String field(String event, String token) {
    int start = event.indexOf(token);
    if (start < 0) return "MISSING";
    start += token.length();
    int end = event.indexOf(',', start);
    return end < 0 ? event.substring(start) : event.substring(start, end);
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key);
    else System.setProperty(key, value);
  }
}
