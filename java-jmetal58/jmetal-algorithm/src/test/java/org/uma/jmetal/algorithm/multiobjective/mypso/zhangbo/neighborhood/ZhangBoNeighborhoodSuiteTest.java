package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

public class ZhangBoNeighborhoodSuiteTest {
  private ZhangBoFatigueInstanceData instance;
  private ZhangBoFatigueParameters parameters;
  private ZhangBoFatigueEvaluator evaluator;
  private TestSolution source;

  @Before
  public void setUp() {
    String sha = repeat('A', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 4, 2, new int[][] {{9, 4}, {8, 5}, {7, 6}, {6, 7}}, repeat('B', 64));
    instance = new ZhangBoFatigueInstanceData(sha, 4, 2, 1,
        new int[][] {{2, 2}},
        new double[][][] {{{1.0, 1.5}, {1.0, 1.3}}},
        new int[][][] {{{8, 11}, {7, 10}}},
        new int[][] {{20, 12}, {18, 14}, {16, 16}, {14, 18}},
        new int[] {4}, new double[][] {{1.0, 1.3, 1.0, 1.2}},
        new int[][] {{10, 13, 10, 12}}, extension);
    double[][][] lambda = {{{0.08, 0.08, 0.08, 0.08}, {0.08, 0.08, 0.08, 0.08}}};
    double[][][] mu = {{{0.05, 0.05, 0.05, 0.05}, {0.05, 0.05, 0.05, 0.05}}};
    parameters = new ZhangBoFatigueParameters(sha, lambda, mu,
        new double[] {0.30, 0.30}, 0.20, 0.90, repeat('C', 64));
    evaluator = new ZhangBoFatigueEvaluator();
    source = new TestSolution(
        Arrays.asList(0, 1, 2, 3), Arrays.asList(0, 0, 0, 0),
        Arrays.asList(0, 0, 1, 1), Arrays.asList(0, 0, 0, 0));
    evaluate(source);
  }

  @Test
  public void allNeighborhoodsRespectCapsAndDoNotMutateInput() {
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    String original = fingerprint(source);
    for (ZhangBoNeighborhoodId id : ZhangBoNeighborhoodId.values()) {
      ZhangBoCountingEvaluationGateway gateway = new ZhangBoCountingEvaluationGateway(this::evaluate);
      ZhangBoNeighborhoodRequest request = new ZhangBoNeighborhoodRequest(
          source, instance, parameters, 0, ZhangBoSubSwarm.G4_BALANCED,
          20260808L, ZhangBoFatigueFocus.FMAX);
      ZhangBoNeighborhoodResult result = suite.apply(id, request, gateway);
      assertEquals(original, fingerprint(source));
      assertTrue(result.getGeneratedCandidates() <= ZhangBoNeighborhoodConfiguration.cap(id));
      assertEquals(gateway.getEvaluationCount(), result.getCompleteEvaluations());
      if (result.isApplicable()) {
        assertNotNull(result.getSelected());
        assertTrue(result.getCompleteEvaluations() > 0);
      } else {
        assertEquals(0, result.getGeneratedCandidates());
        if (id != ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW) {
          assertEquals(0, result.getCompleteEvaluations());
        }
      }
    }
  }

  @Test
  public void jsMovesKeepResourceBundlesAttachedToJobs() {
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    Map<Integer, String> before = resourcesByJob(source);
    for (ZhangBoNeighborhoodId id : Arrays.asList(
        ZhangBoNeighborhoodId.O1_JS_INSERT,
        ZhangBoNeighborhoodId.O2_JS_REVERSE,
        ZhangBoNeighborhoodId.O3_JS_SWAP,
        ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK)) {
      ZhangBoNeighborhoodResult result = suite.apply(id,
          new ZhangBoNeighborhoodRequest(source, instance, parameters, 0,
              ZhangBoSubSwarm.G1_CMAX, 20260808L, ZhangBoFatigueFocus.FE),
          new ZhangBoCountingEvaluationGateway(this::evaluate));
      if (result.isApplicable()) assertEquals(before, resourcesByJob(result.getSelected()));
    }
  }

  @Test
  public void o11UsesFixedSutAndO12ChangesMachineAndWorkerTogether() {
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    ZhangBoNeighborhoodResult o11 = suite.apply(
        ZhangBoNeighborhoodId.O11_FATIGUE_WORKER_REASSIGNMENT,
        request(ZhangBoSubSwarm.G3_TWC, ZhangBoFatigueFocus.FMAX),
        new ZhangBoCountingEvaluationGateway(this::evaluate));
    assertTrue(o11.isApplicable());
    assertTrue(o11.getDiagnostics().toString().contains("setupComponentEnabled=true"));

    ZhangBoNeighborhoodResult o12 = suite.apply(
        ZhangBoNeighborhoodId.O12_JOINT_MACHINE_WORKER,
        request(ZhangBoSubSwarm.G2_TEC, ZhangBoFatigueFocus.FE),
        new ZhangBoCountingEvaluationGateway(this::evaluate));
    assertTrue(o12.isApplicable());
    assertTrue(o12.getDiagnostics().toString().contains("weights=[0.15, 0.55, 0.15, 0.15]"));
    assertFalse(fingerprint(source).equals(fingerprint(o12.getSelected())));
  }

  @Test
  public void deterministicCallsAreByteStableApartFromWallClock() {
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    ZhangBoNeighborhoodResult first = suite.apply(ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK,
        request(ZhangBoSubSwarm.G1_CMAX, ZhangBoFatigueFocus.FMAX),
        new ZhangBoCountingEvaluationGateway(this::evaluate));
    ZhangBoNeighborhoodResult second = suite.apply(ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK,
        request(ZhangBoSubSwarm.G1_CMAX, ZhangBoFatigueFocus.FMAX),
        new ZhangBoCountingEvaluationGateway(this::evaluate));
    assertEquals(first.isApplicable(), second.isApplicable());
    assertEquals(first.getReason(), second.getReason());
    assertEquals(first.getGeneratedCandidates(), second.getGeneratedCandidates());
    assertEquals(first.getCompleteEvaluations(), second.getCompleteEvaluations());
    assertEquals(first.getDiagnostics(), second.getDiagnostics());
    if (first.isApplicable()) assertEquals(fingerprint(first.getSelected()), fingerprint(second.getSelected()));
  }

  @Test
  public void goldenAndRealInstanceAuditMatricesStayWithinOneHundredFourEvaluations()
      throws Exception {
    AuditFixture golden = goldenFixture();
    int goldenEvaluations = auditMatrix(golden.source, golden.instance, golden.parameters,
        solution -> evaluate(golden.evaluator, golden.instance, golden.parameters, solution));
    assertTrue(goldenEvaluations <= 104);

    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName().toString().equals("jmetal-algorithm")) project = project.getParent();
    String previousData = System.getProperty("dhfsp.data.dir");
    String previousFatigue = System.getProperty("dhfsp.fatigue.dir");
    String previousExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", project.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", project.resolve("fatigue-parameters/v1").toString());
      System.setProperty("dhfsp.instance.extension.dir",
          project.resolve("instance-extensions/v1").toString());
      JMetalRandom.getInstance().setSeed(20260808L);
      ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(20, 2, 3, 1);
      PermutationSolution<Integer> real = problem.createSolution();
      @SuppressWarnings("unchecked")
      List<Integer> realMachines = (List<Integer>) real.getAttribute("machine");
      for (int position = 0; position < real.getNumberOfVariables(); position++) {
        int factory = real.getVariableValueid(position);
        realMachines.set(position,
            position % problem.getFatigueInstanceData().getMachineCount(factory, 0));
        int[] eligible = problem.getFatigueInstanceData().getEligibleWorkers(factory, 0);
        real.setVariableValueworker(position, eligible[position % eligible.length]);
      }
      problem.evaluate(real);
      int realEvaluations = auditMatrix(real, problem.getFatigueInstanceData(),
          problem.getFatigueParameters(), problem::evaluate);
      assertTrue(realEvaluations <= 104);
      System.out.println("P7.1_AUDIT goldenEvaluations=" + goldenEvaluations
          + ",realEvaluations=" + realEvaluations + ",budget=104");
    } finally {
      restore("dhfsp.data.dir", previousData);
      restore("dhfsp.fatigue.dir", previousFatigue);
      restore("dhfsp.instance.extension.dir", previousExtension);
    }
  }

  @Test
  public void oneHundredFixedSeedCallsReplayAndAnotherSeedCanChangeJsMove() {
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    String expected = null;
    for (int repetition = 0; repetition < 100; repetition++) {
      ZhangBoNeighborhoodResult result = suite.apply(ZhangBoNeighborhoodId.O1_JS_INSERT,
          request(ZhangBoSubSwarm.G1_CMAX, ZhangBoFatigueFocus.FMAX),
          new ZhangBoCountingEvaluationGateway(this::evaluate));
      String actual = fingerprint(result.getSelected()) + "|" + result.getDiagnostics();
      if (expected == null) expected = actual;
      else assertEquals(expected, actual);
    }
    boolean changed = false;
    for (long seed = 1; seed <= 50 && !changed; seed++) {
      ZhangBoNeighborhoodResult result = suite.apply(ZhangBoNeighborhoodId.O1_JS_INSERT,
          new ZhangBoNeighborhoodRequest(source, instance, parameters, 0,
              ZhangBoSubSwarm.G1_CMAX, seed, ZhangBoFatigueFocus.FMAX),
          new ZhangBoCountingEvaluationGateway(this::evaluate));
      changed = !expected.startsWith(fingerprint(result.getSelected()) + "|");
    }
    assertTrue(changed);
  }

  private ZhangBoNeighborhoodRequest request(ZhangBoSubSwarm group, ZhangBoFatigueFocus focus) {
    return new ZhangBoNeighborhoodRequest(source, instance, parameters, 0, group, 20260808L, focus);
  }

  private void evaluate(PermutationSolution<Integer> solution) {
    evaluate(evaluator, instance, parameters, solution);
  }

  private static void evaluate(ZhangBoFatigueEvaluator evaluator,
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution) {
    ZhangBoFatigueEvaluationResult result = evaluator.evaluate(instance, parameters, solution);
    double[] objectives = result.getObjectives();
    for (int i = 0; i < objectives.length; i++) solution.setObjective(i, objectives[i]);
    solution.setAttribute(ZhangBoFatigueEvaluationResult.class, result);
  }

  private static int auditMatrix(PermutationSolution<Integer> source,
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      java.util.function.Consumer<PermutationSolution<Integer>> evaluator) {
    int total = 0;
    ZhangBoNeighborhoodSuite suite = new ZhangBoNeighborhoodSuite();
    for (ZhangBoSubSwarm group : ZhangBoSubSwarm.values()) {
      for (ZhangBoNeighborhoodId id : ZhangBoNeighborhoodId.values()) {
        ZhangBoCountingEvaluationGateway gateway = new ZhangBoCountingEvaluationGateway(evaluator);
        ZhangBoNeighborhoodResult result = suite.apply(id,
            new ZhangBoNeighborhoodRequest(source, instance, parameters, 0, group,
                20260808L, id == ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW
                    && group == ZhangBoSubSwarm.G2_TEC
                    ? ZhangBoFatigueFocus.FE : ZhangBoFatigueFocus.FMAX), gateway);
        assertEquals(gateway.getEvaluationCount(), result.getCompleteEvaluations());
        total += gateway.getEvaluationCount();
      }
    }
    return total;
  }

  private static AuditFixture goldenFixture() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance sourceInstance = fixture.getInstance();
    int jobs = sourceInstance.getNumberOfJobs();
    int stages = sourceInstance.getNumberOfStages();
    int factories = sourceInstance.getNumberOfFactories();
    int[][] machineCounts = new int[factories][stages];
    double[][][] machineSpeeds = new double[factories][stages][];
    int[][][] powers = new int[factories][stages][];
    int[] workerCounts = new int[factories];
    double[][] workerEfficiencies = new double[factories][];
    int[][] workerCosts = new int[factories][];
    for (int factory = 0; factory < factories; factory++) {
      List<Double> efficiencies = new ArrayList<>();
      List<Integer> costs = new ArrayList<>();
      for (int stage = 0; stage < stages; stage++) {
        machineCounts[factory][stage] = sourceInstance.getMachineCount(factory, stage);
        machineSpeeds[factory][stage] = sourceInstance.getMachineSpeeds(factory, stage);
        double[] sourcePowers = sourceInstance.getMachineEnergyPerUnit(factory, stage);
        powers[factory][stage] = new int[sourcePowers.length];
        for (int index = 0; index < sourcePowers.length; index++) {
          powers[factory][stage][index] = (int) sourcePowers[index];
        }
        double[] stageEfficiencies = sourceInstance.getWorkerEfficiencies(factory, stage);
        double[] stageCosts = sourceInstance.getWorkerCostPerUnit(factory, stage);
        for (int index = 0; index < stageEfficiencies.length; index++) {
          efficiencies.add(stageEfficiencies[index]);
          costs.add((int) stageCosts[index]);
        }
      }
      workerCounts[factory] = efficiencies.size();
      workerEfficiencies[factory] = new double[efficiencies.size()];
      workerCosts[factory] = new int[costs.size()];
      for (int index = 0; index < efficiencies.size(); index++) {
        workerEfficiencies[factory][index] = efficiencies.get(index);
        workerCosts[factory][index] = costs.get(index);
      }
    }
    int[][] processing = new int[jobs][stages];
    int[][] setup = new int[jobs][stages];
    for (int job = 0; job < jobs; job++) {
      for (int stage = 0; stage < stages; stage++) {
        processing[job][stage] = (int) sourceInstance.getStandardProcessingTime(stage, job);
        setup[job][stage] = (int) sourceInstance.getStandardSetupTime(stage, job);
      }
    }
    String sha = repeat('D', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, jobs, stages, setup, repeat('E', 64));
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(
        sha, jobs, stages, factories, machineCounts, machineSpeeds, powers, processing,
        workerCounts, workerEfficiencies, workerCosts, extension);
    double[][][] lambda = new double[factories][stages][];
    double[][][] mu = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        lambda[factory][stage] = new double[workerCounts[factory]];
        mu[factory][stage] = new double[workerCounts[factory]];
        Arrays.fill(lambda[factory][stage], 0.03);
        Arrays.fill(mu[factory][stage], 0.05);
      }
    }
    ZhangBoFatigueParameters parameters = new ZhangBoFatigueParameters(
        sha, lambda, mu, new double[] {0.30, 0.30}, 0.80, 0.90, repeat('F', 64));
    DhhfspFourVectorSolution golden = fixture.createSolution();
    TestSolution solution = new TestSolution(golden.getVariables(), golden.getVariablesid(),
        golden.getMachineAssignments(), golden.getVariablesworker());
    ZhangBoFatigueEvaluator evaluator = new ZhangBoFatigueEvaluator();
    evaluate(evaluator, instance, parameters, solution);
    return new AuditFixture(solution, instance, parameters, evaluator);
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key);
    else System.setProperty(key, value);
  }

  private static Map<Integer, String> resourcesByJob(PermutationSolution<Integer> solution) {
    Map<Integer, String> result = new HashMap<>();
    @SuppressWarnings("unchecked")
    List<Integer> machines = (List<Integer>) solution.getAttribute("machine");
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      result.put(solution.getVariableValue(position), solution.getVariableValueid(position) + ":"
          + machines.get(position) + ":" + solution.getVariableValueworker(position));
    }
    return result;
  }

  private static String fingerprint(PermutationSolution<Integer> solution) {
    return solution.getVariables() + "|" + solution.getVariablesid() + "|"
        + solution.getAttribute("machine") + "|" + solution.getVariablesworker();
  }

  private static String repeat(char value, int count) {
    char[] result = new char[count];
    Arrays.fill(result, value);
    return new String(result);
  }

  private static final class TestSolution implements PermutationSolution<Integer> {
    private static final long serialVersionUID = 1L;
    private final List<Integer> js;
    private final List<Integer> fa;
    private final List<Integer> wa;
    private final double[] objectives;
    private final Map<Object, Object> attributes;

    TestSolution(List<Integer> js, List<Integer> fa, List<Integer> ma, List<Integer> wa) {
      this.js = new ArrayList<>(js);
      this.fa = new ArrayList<>(fa);
      this.wa = new ArrayList<>(wa);
      this.objectives = new double[7];
      this.attributes = new HashMap<>();
      this.attributes.put("machine", new ArrayList<>(ma));
    }

    private TestSolution(TestSolution source) {
      this.js = new ArrayList<>(source.js);
      this.fa = new ArrayList<>(source.fa);
      this.wa = new ArrayList<>(source.wa);
      this.objectives = source.objectives.clone();
      this.attributes = new HashMap<>(source.attributes);
      @SuppressWarnings("unchecked")
      List<Integer> machines = (List<Integer>) source.attributes.get("machine");
      this.attributes.put("machine", new ArrayList<>(machines));
    }

    @Override public void setObjective(int index, double value) { objectives[index] = value; }
    @Override public double getObjective(int index) { return objectives[index]; }
    @Override public double[] getObjectives() { return objectives; }
    @Override public Integer getVariableValue(int index) { return js.get(index); }
    @Override public List<Integer> getVariables() { return js; }
    @Override public void setVariableValue(int index, Integer value) { js.set(index, value); }
    @Override public String getVariableValueString(int index) { return String.valueOf(js.get(index)); }
    @Override public int getNumberOfVariables() { return js.size(); }
    @Override public int getNumberOfObjectives() { return objectives.length; }
    @Override public Solution<Integer> copy() { return new TestSolution(this); }
    @Override public void setAttribute(Object id, Object value) { attributes.put(id, value); }
    @Override public Object getAttribute(Object id) { return attributes.get(id); }
    @Override public Map<Object, Object> getAttributes() { return attributes; }
    @Override public Integer getVariableValueid(int index) { return fa.get(index); }
    @Override public List<Integer> getVariablesid() { return fa; }
    @Override public void setVariableValueid(int index, Integer value) { fa.set(index, value); }
    @Override public int getNumberOfVariablesid() { return fa.size(); }
    @Override public List<Integer> getVariablesworker() { return wa; }
    @Override public int getNumberOfVariablesworker() { return wa.size(); }
    @Override public void setVariableValueworker(int index, Integer value) { wa.set(index, value); }
    @Override public Integer getVariableValueworker(int index) { return wa.get(index); }
  }

  private static final class AuditFixture {
    final TestSolution source;
    final ZhangBoFatigueInstanceData instance;
    final ZhangBoFatigueParameters parameters;
    final ZhangBoFatigueEvaluator evaluator;

    AuditFixture(TestSolution source, ZhangBoFatigueInstanceData instance,
        ZhangBoFatigueParameters parameters, ZhangBoFatigueEvaluator evaluator) {
      this.source = source;
      this.instance = instance;
      this.parameters = parameters;
      this.evaluator = evaluator;
    }
  }
}
