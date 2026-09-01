package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 2,000-FE behavior gate for passive observation ON/OFF. */
@Ignore("V3.1 permits only the final A4/100_5_3_1/20260901/50000 validation")
public class V35ObservationOnOffEquivalenceTest {
  private static final long SEED = 20260822L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 2_000;

  @Test(timeout = 180000)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void passiveObservationOnOffHasIdenticalSearchTrace() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-algorithm".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    Path instance = root.resolve("EADHFSP/20_2_3_1.txt");
    Path extension = root.resolve("instance-extensions/v1");
    Path fatigue = root.resolve("fatigue-parameters/v1");
    ZhangBoCanonicalProductionProblem problemOff = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, SEED, extension, fatigue, ZhangBoShiftConfiguration.none());
    ZhangBoCanonicalProductionProblem problemOn = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, SEED, extension, fatigue, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problemOff.createSolution());
    V35FinalAblationProfile.Arm arm = V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE;
    V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
        arm, SEED, POPULATION, BUDGET);
    V35FairRunner.RunRecord off = V35FairRunner.run(V35FairRunner.Mode.V35_A3,
        (Problem) problemOff, initial, BUDGET, SEED, false,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
    V35FairRunner.RunRecord on = V35FairRunner.run(V35FairRunner.Mode.V35_A3,
        (Problem) problemOn, initial, BUDGET, SEED, true,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);

    // RunRecord already contains an immutable run-end snapshot.  Exporting
    // that snapshot is deliberately performed only after the ON run returns;
    // the OFF run therefore acts as the no-sidecar control.
    Path sidecar = Files.createTempDirectory("v35-observation-equivalence-");
    try {
      V35FairRunner.writeRecord(on, sidecar, "testSidecar=true\n");
      assertTrue(Files.isRegularFile(sidecar.resolve("qp-summary.properties")));
      assertTrue(Files.isRegularFile(sidecar.resolve("lineage-summary.properties")));
      assertTrue(Files.isRegularFile(sidecar.resolve("dual-q-summary.properties")));
      assertTrue(new String(Files.readAllBytes(sidecar.resolve("qp-summary.properties")),
          java.nio.charset.StandardCharsets.UTF_8).contains("eventCapture=EMPTY\n"));
    } finally {
      deleteTree(sidecar);
    }

    assertEquals("COMPLETED", off.getStatus());
    assertEquals("COMPLETED", on.getStatus());
    assertEquals(off.getInitialPopulationHash(), on.getInitialPopulationHash());
    // The formal phase guard may stop at the initial population when the next
    // complete phase would exceed the 2,000-FE cap; the equivalence gate is
    // therefore bounded-budget, not an exact-FE assertion.
    assertTrue(off.getFullEvaluations() > 0 && off.getFullEvaluations() <= BUDGET);
    assertTrue(on.getFullEvaluations() > 0 && on.getFullEvaluations() <= BUDGET);
    assertEquals(off.getFullEvaluations(), on.getFullEvaluations());
    assertEquals(off.getEvaluationTraceHash(), on.getEvaluationTraceHash());
    assertEquals(frontText(off.getFront()), frontText(on.getFront()));
    assertEquals(off.getCmaxAudit().recordsCsv(), on.getCmaxAudit().recordsCsv());
    assertEquals(off.getObservationEvidence().getQpActionStatistics(),
        on.getObservationEvidence().getQpActionStatistics());
    assertEquals(off.getObservationEvidence().getQpTableHash(),
        on.getObservationEvidence().getQpTableHash());
    assertEquals(off.getObservationEvidence().getQpEventCount(),
        on.getObservationEvidence().getQpEventCount());
    assertEquals(off.getObservationEvidence().getQpEventStreamHash(),
        on.getObservationEvidence().getQpEventStreamHash());
    assertEquals(off.getObservationEvidence().getQpEvents(),
        on.getObservationEvidence().getQpEvents());
    assertEquals(off.getObservationEvidence().getLineageEventCount(),
        on.getObservationEvidence().getLineageEventCount());
    assertEquals(off.getObservationEvidence().getLineageEventStreamHash(),
        on.getObservationEvidence().getLineageEventStreamHash());
    assertEquals(off.getObservationEvidence().getLineageEvents(),
        on.getObservationEvidence().getLineageEvents());
    assertEquals(off.getObservationEvidence().getDualQEventCount(),
        on.getObservationEvidence().getDualQEventCount());
    assertEquals(off.getObservationEvidence().getDualQEventStreamHash(),
        on.getObservationEvidence().getDualQEventStreamHash());
    assertEquals(off.getObservationEvidence().getDualQEvents(),
        on.getObservationEvidence().getDualQEvents());
    assertEquals(off.getObservationEvidence().getDualQWarmup(),
        on.getObservationEvidence().getDualQWarmup());
    assertEquals(off.getObservationEvidence().getDualQP(),
        on.getObservationEvidence().getDualQP());
    assertEquals(off.getObservationEvidence().getDualQG(),
        on.getObservationEvidence().getDualQG());
  }

  private static void deleteTree(Path root) throws IOException {
    if (!Files.exists(root)) return;
    try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException error) {
          throw new RuntimeException(error);
        }
      });
    }
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder();
    for (double[] point : front) {
      text.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return text.toString();
  }
}
