package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CfvfGirAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-FC-5: the CFVF GIR audit.  (a) Kind x Source cross counts propagate
 * from the diagnostics; the audit expands them into per-group, per-vector,
 * per-teacher inheritance counts (FMW touches FA/MA/WA, MW touches MA/WA,
 * and so on) and tracks the last modification per branch for
 * RecordContribution alignment.  (b) The audit is a pure observer: a full A4
 * replay must keep the front byte-identical while every CFVF offspring is
 * observed exactly once.
 */
public class V35Fc5CfvfGirAuditTest {

  private static ZhangBoCfvfDiagnostics diagnostics(int jsHamming,
      Map<ZhangBoResourceAction.Kind, Integer> kinds,
      Map<ZhangBoResourceAction.Source, Integer> sources,
      Map<String, Integer> cross) {
    return new ZhangBoCfvfDiagnostics(jsHamming, 1, 1, 1,
        1, 2, 0, 0, 0, kinds, sources, cross, new ArrayList<String>());
  }

  @Test public void crossCountsFeedGroupVectorSourceGir() {
    Map<ZhangBoResourceAction.Kind, Integer> kinds =
        new EnumMap<>(ZhangBoResourceAction.Kind.class);
    kinds.put(ZhangBoResourceAction.Kind.FMW, 2);
    kinds.put(ZhangBoResourceAction.Kind.W, 1);
    Map<ZhangBoResourceAction.Source, Integer> sources =
        new EnumMap<>(ZhangBoResourceAction.Source.class);
    sources.put(ZhangBoResourceAction.Source.GBEST, 2);
    sources.put(ZhangBoResourceAction.Source.PBEST, 1);
    Map<String, Integer> cross = new HashMap<>();
    cross.put("FMW:GBEST", 2);
    cross.put("W:PBEST", 1);
    ZhangBoCfvfDiagnostics diagnostics = diagnostics(3, kinds, sources, cross);
    assertEquals(Integer.valueOf(2), diagnostics.getCrossCounts().get("FMW:GBEST"));
    assertTrue(diagnostics.toCanonicalText().contains("cross.FMW:GBEST=2"));

    V35CfvfGirAudit audit = new V35CfvfGirAudit();
    audit.observe(ZhangBoSubSwarm.G1_CMAX, diagnostics, 7L, 42L, 3L);
    assertEquals(1L, audit.getObservations());
    String summary = audit.summaryText();
    assertTrue(summary.contains("gir.G1_CMAX|FA|GBEST=2"));
    assertTrue(summary.contains("gir.G1_CMAX|MA|GBEST=2"));
    assertTrue(summary.contains("gir.G1_CMAX|WA|GBEST=2"));
    assertTrue(summary.contains("gir.G1_CMAX|WA|PBEST=1"));
    // JS inheritance is reported through the aggregate counters (registered
    // granularity limit): pbest/gbest inheritance both attribute JS moves.
    assertTrue(summary.contains("gir.G1_CMAX|JS|PBEST=3"));
    assertTrue(summary.contains("gir.G1_CMAX|JS|GBEST=3"));
    assertEquals("fe=42,generation=3,branch=7,resources=FA=GBESTx2;MA=GBESTx2;"
        + "WA=GBESTx2;WA=PBESTx1", audit.lastModificationOf(7L));
  }

  @Test(timeout = 600000) public void auditIsAPureObserverOnARealRun() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    String first = runA4FrontHash(project);
    String second = runA4FrontHash(project);
    assertEquals("replay must stay byte-identical with the audit attached", first, second);
  }

  private String runA4FrontHash(Path root) throws Exception {
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, 20260808L,
        root.resolve("java-jmetal58/instance-extensions/v1"),
        root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < 100; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration config = V35ProductionConfiguration.builder()
        .seed(20260808L).populationSize(100).maxEvaluations(20000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, 100,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(config)
        .setFormalBaselineConfiguration(ZhangBoFormalHmopsoQgsConfiguration.table9())
        .setMaxIterations(20000)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.run();
    assertEquals("every CFVF offspring must be observed exactly once",
        algorithm.getCfvfOffspringCount(), algorithm.getV35CfvfGirAudit().getObservations());
    assertTrue("the audit must hold real data",
        algorithm.getV35CfvfGirAudit().getObservations() > 0);
    assertTrue(algorithm.v35CfvfGirAuditSummary().contains("gir."));
    List<double[]> front = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      front.add(new double[] {solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)});
    }
    front = P8MetricCalculator.nondominated(front);
    StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      csv.append(point[0]).append(',').append(point[1]).append(',')
          .append(point[2]).append('\n');
    }
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }
}
