package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.solution.PermutationSolution;

/** Proves that installing the ND0 observation hook does not alter A4 search behaviour. */
public class V35ArchiveControlEquivalenceTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void nd0MatchesUninstrumentedA4AtTwoThousandEvaluations() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null
        && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        root.resolve("instance-extensions/v1").toString());

    final long seed = 20260808L;
    final int populationSize = 10;
    final int maxEvaluations = 2000;
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem seedProblem = newProblem(source, seed);
    ZhangBoCanonicalProductionProblem plainProblem = newProblem(source, seed);
    ZhangBoCanonicalProductionProblem observedProblem = newProblem(source, seed);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < populationSize; index++) {
      initial.add(seedProblem.createSolution());
    }
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA,
        seed, populationSize, maxEvaluations);

    V35FairRunner.RunRecord plain = V35FairRunner.run(
        V35FairRunner.Mode.V35_FULL_POOL_OFF, (Problem) plainProblem,
        copy(initial), maxEvaluations, seed, true,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
    V35FairRunner.RunRecord observed = V35ArchiveExperimentRunner.run(
        V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL,
        (Problem) observedProblem, copy(initial), maxEvaluations, seed);

    assertEquals("COMPLETED", plain.getStatus());
    assertEquals("COMPLETED", observed.getStatus());
    assertEquals(plain.getInitialPopulationHash(), observed.getInitialPopulationHash());
    assertEquals(plain.getFullEvaluations(), observed.getFullEvaluations());
    assertEquals(plain.getEvaluationTraceHash(), observed.getEvaluationTraceHash());
    assertEquals(frontText(plain.getFront()), frontText(observed.getFront()));
    assertEquals(mechanismCore(plain.getMechanismSummary()),
        mechanismCore(observed.getMechanismSummary()));
    assertNotNull(observed.getArchiveExperimentArtifacts());
    assertTrue(observed.getArchiveExperimentArtifacts()
        .isDecisionEqualsObservedAfterExactDedup());
    Path evidence = temporaryFolder.newFolder("nd0-record").toPath();
    V35ArchiveExperimentRunner.writeRecord(observed,
        V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL,
        evidence, seed, populationSize, maxEvaluations);
    assertTrue(Files.isRegularFile(evidence.resolve("decision-front.csv")));
    assertTrue(Files.isRegularFile(evidence.resolve("observed-full-front.csv")));
    assertTrue(Files.isRegularFile(evidence.resolve("representative-front-k30.csv")));
    assertTrue(Files.isRegularFile(evidence.resolve("sensitivity-front-k25.csv")));
    assertTrue(Files.isRegularFile(evidence.resolve("sensitivity-front-k50.csv")));
    assertTrue(Files.isRegularFile(evidence.resolve("front-kind-registry.csv")));
  }

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void a3RunEndObservationExportIsDeterministicAndReadOnly() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null
        && "jmetal-algorithm".equals(root.getFileName().toString())) root = root.getParent();
    System.setProperty("dhfsp.data.dir", root.resolve("EADHFSP").toString());
    System.setProperty("dhfsp.fatigue.dir", root.resolve("fatigue-parameters/v1").toString());
    System.setProperty("dhfsp.instance.extension.dir",
        root.resolve("instance-extensions/v1").toString());

    final long seed = 20260822L;
    final int populationSize = 10;
    final int maxEvaluations = 2000;
    ZhangBoEDHHFSPW source = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
    ZhangBoCanonicalProductionProblem seedProblem = newProblem(source, seed);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < populationSize; index++) initial.add(seedProblem.createSolution());
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE,
        seed, populationSize, maxEvaluations);

    V35FairRunner.RunRecord first = V35FairRunner.run(
        V35FairRunner.Mode.V35_A3, (Problem) newProblem(source, seed), copy(initial),
        maxEvaluations, seed, true, V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(),
        false, configuration);
    V35FairRunner.RunRecord replay = V35FairRunner.run(
        V35FairRunner.Mode.V35_A3, (Problem) newProblem(source, seed), copy(initial),
        maxEvaluations, seed, true, V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(),
        false, configuration);

    assertEquals("COMPLETED", first.getStatus());
    assertEquals(first.getInitialPopulationHash(), replay.getInitialPopulationHash());
    assertEquals(first.getFullEvaluations(), replay.getFullEvaluations());
    assertEquals(first.getEvaluationTraceHash(), replay.getEvaluationTraceHash());
    assertEquals(frontText(first.getFront()), frontText(replay.getFront()));
    assertEquals(mechanismCore(first.getMechanismSummary()),
        mechanismCore(replay.getMechanismSummary()));
    assertNotNull(first.getObservationEvidence());
    assertTrue(first.getObservationEvidence().getQpEventCount() > 0L);
    assertEquals(first.getObservationEvidence().getQpEventStreamHash(),
        replay.getObservationEvidence().getQpEventStreamHash());
    assertEquals(first.getObservationEvidence().getLineageEventStreamHash(),
        replay.getObservationEvidence().getLineageEventStreamHash());
    assertEquals(first.getObservationEvidence().getDualQEventStreamHash(),
        replay.getObservationEvidence().getDualQEventStreamHash());

    String evaluationHash = first.getEvaluationTraceHash();
    String frontHash = frontText(first.getFront());
    String qpHash = first.getObservationEvidence().getQpEventStreamHash();
    Path evidence = temporaryFolder.newFolder("a3-observation-record").toPath();
    V35FairRunner.writeRecord(first, evidence, "diagnosticObservation=true\n");
    assertEquals(evaluationHash, first.getEvaluationTraceHash());
    assertEquals(frontHash, frontText(first.getFront()));
    assertEquals(qpHash, first.getObservationEvidence().getQpEventStreamHash());
    assertTrue(Files.isRegularFile(evidence.resolve("qp-events.log")));
    assertTrue(Files.isRegularFile(evidence.resolve("lineage-events.log")));
    assertTrue(Files.isRegularFile(evidence.resolve("dual-q-events.log")));
  }

  private static ZhangBoCanonicalProductionProblem newProblem(
      ZhangBoEDHHFSPW source, long seed) {
    return ZhangBoV35ProblemFactory.create(source.getFatigueInstanceData(),
        source.getFatigueParameters(), ProductionDecodeMode.FM3, seed);
  }

  @SuppressWarnings("unchecked")
  private static List<PermutationSolution<Integer>> copy(
      List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) {
      result.add((PermutationSolution<Integer>) value.copy());
    }
    return result;
  }

  private static String frontText(List<double[]> values) {
    StringBuilder result = new StringBuilder();
    for (double[] value : values) {
      result.append(value[0]).append(',').append(value[1]).append(',')
          .append(value[2]).append('\n');
    }
    return result.toString();
  }

  private static String mechanismCore(String value) {
    int timing = value.indexOf(",algorithmRunNanos=");
    return timing < 0 ? value : value.substring(0, timing);
  }
}
