package org.uma.jmetal.runner.lc_psode;

import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCfvfUpdater;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZhangBoP6IntegrationSmokeTest {
  private static final int JOBS = 20;
  private static final int STAGES = 2;
  private static final int FACTORIES = 3;
  private static final int PROBLEM_ID = 1;
  private static final int SWARM = 100;

  @Test
  public void cfvfChainClosesExactlyAtTwoThousandEvaluationsWithoutRepair() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoMOHPSOQ algorithm = builder(problem, 2000)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 20260808L))
          .build();

      algorithm.run();

      assertEquals(2000L, algorithm.getFullEvaluationCount());
      assertEquals(1900L, algorithm.getCfvfOffspringCount());
      assertEquals(0L, algorithm.getCfvfRepairCount());
      assertTrue(algorithm.getCfvfInitializationCorrections() > 0L);
      assertFalse(algorithm.getZhangBoP6Events().isEmpty());
      assertTrue(algorithm.getQgCanonicalText().contains("G1_CMAX.q0="));
      printTrace("P6.1", algorithm);
      ZhangBoResourceDomain domain = new ZhangBoResourceDomain(problem.getFatigueInstanceData());
      List<PermutationSolution<Integer>> result = algorithm.getResult();
      assertFalse(result.isEmpty());
      for (PermutationSolution<Integer> solution : result) {
        ZhangBoCfvfUpdater.validate(solution, domain, "smokeResult");
      }
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void originalQgRunsAsAnIndependentAuthorUpdateAblation() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoMOHPSOQ algorithm = builder(problem, 200)
          .setGlobalSearchConfiguration(ZhangBoGlobalSearchConfiguration.originalQg(0.4))
          .build();
      algorithm.run();
      assertEquals(200L, algorithm.getFullEvaluationCount());
      assertEquals(0L, algorithm.getCfvfOffspringCount());
      assertTrue(algorithm.getQgCanonicalText().contains("event="));
      printTrace("P6.0", algorithm);
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void evaluatedPddrRunsAfterEvaluationAndClosesAtTwoThousandFes() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(problem);
      ZhangBoMOHPSOQ algorithm = builder(problem, 2000)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvfEvaluatedPddr(
                  0.4, 20260808L))
          .build();
      algorithm.run();

      assertEquals(2000L, algorithm.getFullEvaluationCount());
      assertEquals(1900L, algorithm.getCfvfOffspringCount());
      assertEquals(1900L, algorithm.getEvaluatedPddrSelections());
      assertEquals(0L, algorithm.getCfvfRepairCount());
      assertEquals(19, algorithm.getZhangBoPddrEvents().size());
      assertTrue(algorithm.getZhangBoLineageMemories().isEmpty());
      printPddrTrace("P6.1.1", algorithm);
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void lineageArchiveIsAByteStableShadowOfEvaluatedPddr() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW pddrProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(pddrProblem);
      ZhangBoMOHPSOQ pddr = builder(pddrProblem, 2000)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvfEvaluatedPddr(
                  0.4, 20260808L))
          .build();
      ZhangBoEDHHFSPW archiveProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoMOHPSOQ archive = builder(archiveProblem, 2000)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvfAndLineageArchive(
                  0.4, 20260808L))
          .build();

      JMetalRandom.getInstance().setSeed(20260808L);
      pddr.run();
      JMetalRandom.getInstance().setSeed(20260808L);
      archive.run();

      // Lineage IDs are trace metadata introduced only by the shadow archive.  Removing that
      // metadata must leave the complete search behavior and random-event stream unchanged.
      assertCanonicalEquals("CFVF", withoutLineageMetadata(
              String.join("\n", pddr.getZhangBoP6Events())), withoutLineageMetadata(
              String.join("\n", archive.getZhangBoP6Events())));
      assertCanonicalEquals("PDDR", withoutLineageMetadata(
              String.join("\n", pddr.getZhangBoPddrEvents())), withoutLineageMetadata(
              String.join("\n", archive.getZhangBoPddrEvents())));
      assertCanonicalEquals("Qg", pddr.getQgCanonicalText(), archive.getQgCanonicalText());
      assertEquals(resultText(pddr), resultText(archive));
      assertEquals(pddr.getFullEvaluationCount(), archive.getFullEvaluationCount());
      assertEquals(2000L, archive.getFullEvaluationCount());
      assertEquals(1900L, archive.getEvaluatedPddrSelections());
      assertEquals(0L, archive.getCfvfRepairCount());
      Map<Long, org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory>
          memories = archive.getZhangBoLineageMemories();
      assertEquals(SWARM, memories.size());
      for (org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory memory
          : memories.values()) {
        assertTrue(memory.getEntries().size() >= 1);
        assertTrue(memory.getEntries().size() <= 6);
      }
      printPddrTrace("P6.2", archive);
      System.out.println("P6.2_LINEAGE_SUMMARY lineages=" + memories.size()
          + ",splits=" + archive.getZhangBoLineageSplitCount()
          + ",deletions=" + archive.getZhangBoLineageDeletionCount()
          + ",migrations=" + archive.getZhangBoLineageMigrationCount()
          + ",insertions=" + archive.getZhangBoArchiveInsertionCount()
          + ",dominatedRemoved=" + archive.getZhangBoArchiveDominatedRemovalCount()
          + ",duplicatesRemoved=" + archive.getZhangBoArchiveDuplicateRemovalCount()
          + ",truncatedRemoved=" + archive.getZhangBoArchiveTruncationCount());
      Map<Integer, Integer> sizeDistribution = new TreeMap<>();
      for (org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory memory
          : memories.values()) {
        int size = memory.getEntries().size();
        Integer count = sizeDistribution.get(size);
        sizeDistribution.put(size, count == null ? 1 : count + 1);
      }
      System.out.println("P6.2_ARCHIVE_SIZE_DISTRIBUTION " + sizeDistribution);
      List<String> lineageEvents = archive.getZhangBoLineageEvents();
      for (int index = 0; index < Math.min(20, lineageEvents.size()); index++) {
        System.out.println("P6.2_LINEAGE_EVENT[" + index + "]=" + lineageEvents.get(index));
      }
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void qpBestUsesTheLineageArchiveAndClosesAtTwoThousandFes() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(problem);
      ZhangBoMOHPSOQ algorithm = builder(problem, 2000)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveAndQp(
                  0.4, 20260808L))
          .build();

      JMetalRandom.getInstance().setSeed(20260808L);
      algorithm.run();

      assertEquals(2000L, algorithm.getFullEvaluationCount());
      assertEquals(1900L, algorithm.getCfvfOffspringCount());
      assertEquals(1900L, algorithm.getEvaluatedPddrSelections());
      assertEquals(0L, algorithm.getCfvfRepairCount());
      assertFalse(algorithm.getQpEvents().isEmpty());
      assertTrue(algorithm.getQpCanonicalText().contains("G1_CMAX.q0="));
      long actions = 0L;
      for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
        actions += algorithm.getQpActionCount(action);
        assertTrue("Expected Qp action " + action, algorithm.getQpActionCount(action) > 0L);
      }
      assertEquals(1900L, actions);
      assertTrue(algorithm.getQpPbestSwitches() > 0L);
      assertEquals(SWARM, algorithm.getZhangBoLineageMemories().size());
      for (org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory memory
          : algorithm.getZhangBoLineageMemories().values()) {
        assertTrue(memory.getEntries().size() >= 1);
        assertTrue(memory.getEntries().size() <= 6);
      }
      System.out.println("P6.3_QP_SUMMARY evaluations=" + algorithm.getFullEvaluationCount()
          + ",events=" + algorithm.getQpEvents().size()
          + ",switches=" + algorithm.getQpPbestSwitches()
          + ",KEEP=" + algorithm.getQpActionCount(ZhangBoQpAction.KEEP)
          + ",DIRECTIONAL=" + algorithm.getQpActionCount(ZhangBoQpAction.DIRECTIONAL)
          + ",EPSILON=" + algorithm.getQpActionCount(ZhangBoQpAction.EPSILON)
          + ",COMPLEMENTARY=" + algorithm.getQpActionCount(ZhangBoQpAction.COMPLEMENTARY));
      for (int index = 0; index < Math.min(20, algorithm.getQpEvents().size()); index++) {
        System.out.println("P6.3_QP_EVENT[" + index + "]=" + algorithm.getQpEvents().get(index));
      }
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void blockFrozenDualQUsesOneWarmupTenPAndEightGGenerations() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(problem);
      ZhangBoMOHPSOQ algorithm = builder(problem, 2000)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration
                  .originalQgWithCfvfLineageArchiveQpBlockFrozen(0.4, 20260808L))
          .build();

      JMetalRandom.getInstance().setSeed(20260808L);
      algorithm.run();

      assertEquals(2000L, algorithm.getFullEvaluationCount());
      assertEquals(1900L, algorithm.getCfvfOffspringCount());
      assertEquals(1900L, algorithm.getEvaluatedPddrSelections());
      assertEquals(0L, algorithm.getCfvfRepairCount());
      assertEquals(1L, algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.WARMUP));
      assertEquals(10L, algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.P_BLOCK));
      assertEquals(8L, algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.G_BLOCK));
      assertEquals(76L, algorithm.getQgSelectionCount());
      assertEquals(36L, algorithm.getQgTdUpdateCount());
      assertEquals(1800L, algorithm.getQpExecutedActionCount());
      assertEquals(1000L, algorithm.getQpTrainedTransitionCount());
      assertEquals(19, algorithm.getDualQCoordinationEvents().size());
      for (String event : algorithm.getDualQCoordinationEvents()) {
        if (event.contains("phase=P_BLOCK")) assertFrozenHash(event, "Qg");
        if (event.contains("phase=G_BLOCK") || event.contains("phase=WARMUP")) {
          assertFrozenHash(event, "Qp");
        }
      }
      assertEquals(SWARM, algorithm.getZhangBoLineageMemories().size());
      System.out.println("P6.4_DUAL_Q_SUMMARY evaluations=" + algorithm.getFullEvaluationCount()
          + ",warmup=1,P=10,G=8,QgActions=" + algorithm.getQgSelectionCount()
          + ",QgTd=" + algorithm.getQgTdUpdateCount()
          + ",QpActions=" + algorithm.getQpExecutedActionCount()
          + ",QpTransitions=" + algorithm.getQpTrainedTransitionCount()
          + ",QgHash=" + algorithm.getQgTableHash()
          + ",QpHash=" + algorithm.getQpTableHash());
      for (String event : algorithm.getDualQCoordinationEvents()) {
        System.out.println("P6.4_PHASE_EVENT=" + event);
      }
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void blockFrozenDualQTraceReplaysThreeTimesAndSeedChangesAnAction() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(source);
      String expected = null;
      for (int run = 0; run < 3; run++) {
        String trace = runBlockFrozen(initial, 20260808L);
        if (expected == null) expected = trace;
        else assertEquals(expected, trace);
      }
      assertFalse(expected.equals(runBlockFrozen(initial, 20260809L)));
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test
  public void qpOwnedTraceIsByteStableAcrossThreeExplicitInitialSwarmRuns() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      ZhangBoEDHHFSPW sourceProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      List<PermutationSolution<Integer>> initial = createInitial(sourceProblem);
      String expectedQp = null;
      String expectedResult = null;
      for (int run = 0; run < 3; run++) {
        ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
            JOBS, STAGES, FACTORIES, PROBLEM_ID);
        ZhangBoMOHPSOQ algorithm = builder(problem, 600)
            .setInitialSwarmOverride(initial)
            .setGlobalSearchConfiguration(
                ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveAndQp(
                    0.4, 20260808L))
            .build();
        JMetalRandom.getInstance().setSeed(20260808L);
        algorithm.run();
        if (run == 0) {
          expectedQp = algorithm.getQpCanonicalText();
          expectedResult = resultText(algorithm);
        } else {
          assertEquals(expectedQp, algorithm.getQpCanonicalText());
          assertEquals(expectedResult, resultText(algorithm));
        }
        assertEquals(600L, algorithm.getFullEvaluationCount());
      }

      ZhangBoEDHHFSPW differentProblem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoMOHPSOQ different = builder(differentProblem, 600)
          .setInitialSwarmOverride(initial)
          .setGlobalSearchConfiguration(
              ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveAndQp(
                  0.4, 20260809L))
          .build();
      JMetalRandom.getInstance().setSeed(20260808L);
      different.run();
      assertFalse(expectedQp.equals(different.getQpCanonicalText()));
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void p6RejectsTheNonFatigueAuthorProblem() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      ZhangBoEDHHFSPW problem = new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      builder(problem, 100)
          .setGlobalSearchConfiguration(ZhangBoGlobalSearchConfiguration.originalQg(0.4))
          .build();
    } finally {
      restore("dhfsp.data.dir", previousData);
    }
  }

  @Test
  public void builderDefaultsRemainAuthorActiveAndAuthorUpdate() throws Exception {
    String previousData = System.getProperty("dhfsp.data.dir");
    try {
      Path project = Paths.get("..").toAbsolutePath().normalize();
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      ZhangBoEDHHFSPW problem = new ZhangBoEDHHFSPW(JOBS, STAGES, FACTORIES, PROBLEM_ID);
      ZhangBoMOHPSOQ algorithm = builder(problem, 100).build();
      assertEquals(ZhangBoGlobalSearchConfiguration.GlobalLeaderMode.AUTHOR_ACTIVE,
          algorithm.getGlobalSearchConfiguration().getGlobalLeaderMode());
      assertEquals(ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.AUTHOR_UPDATE,
          algorithm.getGlobalSearchConfiguration().getParticleUpdateMode());
      assertEquals("disabled\n", algorithm.getQgCanonicalText());
    } finally {
      restore("dhfsp.data.dir", previousData);
    }
  }

  private static ZhangBoMOHPSOQBuilder builder(ZhangBoEDHHFSPW problem, int evaluations) {
    return new ZhangBoMOHPSOQBuilder(problem, SWARM, FACTORIES, 0.0, 0.8, 0.9, 40.0)
        .setMaxIterations(evaluations)
        .setSwarmSize(SWARM)
        .setRand_k(0.4)
        .setCrossoverRate(0.2)
        .setMutationRate(0.06)
        .setCrossoverRates4worker(0.4)
        .setCrossoverRates4machine(0.3)
        .setMutationRate4worker(0.1)
        .setMutationRate4machine(0.25)
        .setLocalSearch(40);
  }

  private static void assertCanonicalEquals(String label, String expected, String actual) {
    if (expected.equals(actual)) return;
    String[] expectedLines = expected.split("\\n", -1);
    String[] actualLines = actual.split("\\n", -1);
    int limit = Math.min(expectedLines.length, actualLines.length);
    for (int index = 0; index < limit; index++) {
      if (!expectedLines[index].equals(actualLines[index])) {
        throw new AssertionError(label + " differs at line " + index
            + ", previous=<" + (index == 0 ? "" : expectedLines[index - 1]) + ">"
            + ": expected=<" + expectedLines[index] + "> actual=<"
            + actualLines[index] + ">");
      }
    }
    throw new AssertionError(label + " line count differs: expected="
        + expectedLines.length + ", actual=" + actualLines.length);
  }

  private static String withoutLineageMetadata(String text) {
    return text.replaceAll("lineage=-?[0-9]+", "lineage=<metadata>")
        .replaceAll("branch=-?[0-9]+", "branch=<metadata>");
  }

  private static void assertFrozenHash(String event, String controller) {
    String before = field(event, controller + "HashBefore");
    String after = field(event, controller + "HashAfter");
    assertEquals(controller + " table changed in " + event, before, after);
  }

  private static String field(String event, String name) {
    String prefix = name + "=";
    for (String item : event.split(",")) {
      if (item.startsWith(prefix)) return item.substring(prefix.length());
    }
    throw new AssertionError("Missing " + name + " in " + event);
  }

  private static List<PermutationSolution<Integer>> createInitial(ZhangBoEDHHFSPW problem) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (int index = 0; index < SWARM; index++) result.add(problem.createSolution());
    return result;
  }

  private static String runBlockFrozen(
      List<PermutationSolution<Integer>> initial, long seed) throws Exception {
    ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        JOBS, STAGES, FACTORIES, PROBLEM_ID);
    ZhangBoMOHPSOQ algorithm = builder(problem, 2000)
        .setInitialSwarmOverride(initial)
        .setGlobalSearchConfiguration(
            ZhangBoGlobalSearchConfiguration.originalQgWithCfvfLineageArchiveQpBlockFrozen(
                0.4, seed))
        .build();
    JMetalRandom.getInstance().setSeed(20260808L);
    algorithm.run();
    return String.join("\n", algorithm.getDualQCoordinationEvents()) + "\n"
        + algorithm.getQgCanonicalText() + algorithm.getQpCanonicalText()
        + resultText(algorithm);
  }

  private static String resultText(ZhangBoMOHPSOQ algorithm) {
    List<String> values = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      values.add(solution.getVariables() + "|" + solution.getVariablesid() + "|"
          + solution.getAttribute("machine") + "|" + solution.getVariablesworker() + "|"
          + solution.getObjective(0) + ',' + solution.getObjective(1) + ','
          + solution.getObjective(6));
    }
    Collections.sort(values);
    return values.toString();
  }

  private static void printPddrTrace(String phase, ZhangBoMOHPSOQ algorithm) {
    System.out.println(phase + "_PDDR_SUMMARY evaluations=" + algorithm.getFullEvaluationCount()
        + ",selections=" + algorithm.getEvaluatedPddrSelections()
        + ",events=" + algorithm.getZhangBoPddrEvents().size());
    List<String> events = algorithm.getZhangBoPddrEvents();
    for (int index = 0; index < Math.min(20, events.size()); index++) {
      System.out.println(phase + "_PDDR_EVENT[" + index + "]=" + events.get(index));
    }
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key);
    else System.setProperty(key, value);
  }

  private static void printTrace(String phase, ZhangBoMOHPSOQ algorithm) {
    List<String> events = algorithm.getZhangBoP6Events();
    System.out.println(phase + "_TRACE_SUMMARY evaluations=" + algorithm.getFullEvaluationCount()
        + ",cfvfOffspring=" + algorithm.getCfvfOffspringCount()
        + ",repairs=" + algorithm.getCfvfRepairCount()
        + ",initializationCorrections=" + algorithm.getCfvfInitializationCorrections()
        + ",events=" + events.size());
    System.out.println(phase + "_Q_FINAL_BEGIN");
    System.out.print(algorithm.getQgCanonicalText());
    System.out.println(phase + "_Q_FINAL_END");
    int sampleSize = Math.min(20, events.size());
    for (int index = 0; index < sampleSize; index++) {
      System.out.println(phase + "_EVENT[" + index + "]=" + events.get(index));
    }
  }
}
