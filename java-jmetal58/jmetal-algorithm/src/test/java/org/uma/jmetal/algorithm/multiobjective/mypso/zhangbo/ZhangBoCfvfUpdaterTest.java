package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoCfvfUpdaterTest {
  @Test
  public void fourResourceActionsFollowJobIdentityAcrossDifferentJsOrders() {
    ZhangBoTestPermutationSolution current = solution(
        new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0},
        new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});
    ZhangBoTestPermutationSolution pbest = solution(
        new int[] {2, 0, 3, 1},
        // position-aligned values: J2=M, J0=FMW, J3=W, J1=MW
        new int[] {0, 1, 0, 0}, new int[] {1, 1, 0, 1}, new int[] {0, 1, 1, 1});
    ZhangBoTestPermutationSolution gbest = solution(
        new int[] {3, 2, 1, 0}, new int[] {0, 0, 0, 0},
        new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});
    String currentBefore = current.vectors();
    String pbestBefore = pbest.vectors();
    String gbestBefore = gbest.vectors();

    ZhangBoCfvfResult result = new ZhangBoCfvfUpdater().update(current, pbest, gbest,
        ZhangBoResourceVelocity.EMPTY, domain(),
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 20260808L),
        new ZhangBoScriptedRandom(
            new double[] {1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0},
            new int[] {0, 0}));

    PermutationSolution<Integer> offspring = result.getSolution();
    assertResource(offspring, 0, 1, 1, 1);
    assertResource(offspring, 1, 0, 1, 1);
    assertResource(offspring, 2, 0, 1, 0);
    assertResource(offspring, 3, 0, 0, 1);
    Map<ZhangBoResourceAction.Kind, Integer> counts = result.getDiagnostics().getKindCounts();
    assertEquals(Integer.valueOf(1), counts.get(ZhangBoResourceAction.Kind.FMW));
    assertEquals(Integer.valueOf(1), counts.get(ZhangBoResourceAction.Kind.MW));
    assertEquals(Integer.valueOf(1), counts.get(ZhangBoResourceAction.Kind.M));
    assertEquals(Integer.valueOf(1), counts.get(ZhangBoResourceAction.Kind.W));
    assertEquals(0, result.getDiagnostics().getRepairs());
    assertEquals(Arrays.asList(9, 8, 7, 6),
        offspring.getVariablesworker().subList(4, 8));
    assertEquals(currentBefore, current.vectors());
    assertEquals(pbestBefore, pbest.vectors());
    assertEquals(gbestBefore, gbest.vectors());
  }

  @Test
  public void fixedInputAndSeedReplayEventsAndDifferentSeedChangesAnEvent() {
    String baseline = run(20260808L);
    for (int repetition = 0; repetition < 100; repetition++) {
      assertEquals(baseline, run(20260808L));
    }
    assertNotEquals(baseline, run(20260809L));
  }

  @Test
  public void repairIsOnlyAStableExceptionalSafetyNet() {
    ZhangBoTestPermutationSolution invalid = solution(
        new int[] {0, 1, 2, 3}, new int[] {7, 0, 0, 0},
        new int[] {7, 0, 0, 0}, new int[] {7, 0, 0, 0});
    List<String> events = new ArrayList<>();
    assertEquals(3, ZhangBoCfvfUpdater.repairForSafety(invalid, domain(), events));
    ZhangBoCfvfUpdater.validate(invalid, domain(), "repaired");
    assertResource(invalid, 0, 0, 0, 0);
    assertTrue(events.get(0).contains("vector=FA"));
  }

  @Test
  public void inertiaExplorationAndJsChannelsAreIndependentlyObservable() {
    ZhangBoTestPermutationSolution one = new ZhangBoTestPermutationSolution(
        new int[] {0}, new int[] {0}, new int[] {0}, new int[] {0},
        new int[] {9}, 10, 20, 30);
    ZhangBoResourceAction old = new ZhangBoResourceAction(0, ZhangBoResourceAction.Kind.M,
        ZhangBoResourceAction.Source.GBEST, 0, 1, 0);
    ZhangBoCfvfResult inertia = new ZhangBoCfvfUpdater().update(one, one, one,
        new ZhangBoResourceVelocity(Arrays.asList(old)), domainOneJob(),
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 1L),
        new ZhangBoScriptedRandom(new double[] {0.0, 0.0, 0.0, 1.0}, new int[] {0, 0}));
    assertResource(inertia.getSolution(), 0, 0, 1, 0);
    assertEquals(Integer.valueOf(1),
        inertia.getDiagnostics().getSourceCounts().get(ZhangBoResourceAction.Source.INERTIA));

    ZhangBoCfvfResult exploration = new ZhangBoCfvfUpdater().update(one, one, one,
        ZhangBoResourceVelocity.EMPTY, domainOneJob(),
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 1L),
        new ZhangBoScriptedRandom(new double[] {0.0, 0.0, 0.0},
            new int[] {0, 0, 0, 0, 0}));
    assertResource(exploration.getSolution(), 0, 0, 1, 0);
    assertEquals(Integer.valueOf(1),
        exploration.getDiagnostics().getSourceCounts().get(ZhangBoResourceAction.Source.EXPLORE));

    ZhangBoTestPermutationSolution two = new ZhangBoTestPermutationSolution(
        new int[] {0, 1}, new int[] {0, 0}, new int[] {0, 0}, new int[] {0, 0},
        new int[] {9, 8}, 10, 20, 30);
    ZhangBoCfvfResult js = new ZhangBoCfvfUpdater().update(two, two, two,
        ZhangBoResourceVelocity.EMPTY, domain(),
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, 1L),
        new ZhangBoScriptedRandom(new double[] {0.0, 0.0, 1.0}, new int[] {0, 1}));
    assertEquals(Arrays.asList(1, 0), js.getSolution().getVariables());
    assertEquals(2, js.getDiagnostics().getJsHamming());
  }

  @Test
  public void equalGranularityConflictsCanChooseEitherSideIncludingZeroWeights() {
    ZhangBoResourceAction personal = new ZhangBoResourceAction(0, ZhangBoResourceAction.Kind.M,
        ZhangBoResourceAction.Source.PBEST, 0, 1, 0);
    ZhangBoResourceAction social = new ZhangBoResourceAction(0, ZhangBoResourceAction.Kind.M,
        ZhangBoResourceAction.Source.GBEST, 0, 2, 0);
    assertEquals(ZhangBoResourceAction.Source.PBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personal, social, 0.4, 0.4,
            new ZhangBoScriptedRandom(new double[] {0.1}, new int[0])).getSource());
    assertEquals(ZhangBoResourceAction.Source.GBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personal, social, 0.4, 0.4,
            new ZhangBoScriptedRandom(new double[] {0.9}, new int[0])).getSource());
    assertEquals(ZhangBoResourceAction.Source.PBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personal, social, 0.0, 0.0,
            new ZhangBoScriptedRandom(new double[] {0.49}, new int[0])).getSource());
    assertEquals(ZhangBoResourceAction.Source.GBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personal, social, 0.0, 0.0,
            new ZhangBoScriptedRandom(new double[] {0.51}, new int[0])).getSource());

    ZhangBoResourceAction socialFmw = new ZhangBoResourceAction(0,
        ZhangBoResourceAction.Kind.FMW, ZhangBoResourceAction.Source.GBEST, 1, 0, 0);
    assertEquals(ZhangBoResourceAction.Source.GBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personal, socialFmw, 0.4, 0.4,
            new ZhangBoScriptedRandom(new double[] {0.0}, new int[0])).getSource());
    ZhangBoResourceAction personalFmw = new ZhangBoResourceAction(0,
        ZhangBoResourceAction.Kind.FMW, ZhangBoResourceAction.Source.PBEST, 1, 0, 0);
    assertEquals(ZhangBoResourceAction.Source.PBEST,
        ZhangBoCfvfUpdater.resolveLeadershipForPolicyTest(personalFmw, social, 0.4, 0.4,
            new ZhangBoScriptedRandom(new double[] {1.0}, new int[0])).getSource());
  }

  private static String run(long seed) {
    ZhangBoTestPermutationSolution current = solution(
        new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0},
        new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});
    ZhangBoTestPermutationSolution leader = solution(
        new int[] {2, 0, 3, 1}, new int[] {0, 1, 0, 0},
        new int[] {1, 1, 0, 1}, new int[] {0, 1, 1, 1});
    ZhangBoCfvfResult result = new ZhangBoCfvfUpdater().update(current, leader, current,
        ZhangBoResourceVelocity.EMPTY, domain(),
        ZhangBoGlobalSearchConfiguration.originalQgWithCfvf(0.4, seed),
        new JavaRandomGenerator(seed));
    return ZhangBoQgController.fingerprint(result.getSolution()) + "\n"
        + result.getDiagnostics().toCanonicalText();
  }

  private static ZhangBoTestPermutationSolution solution(
      int[] jobs, int[] factories, int[] machines, int[] workers) {
    return new ZhangBoTestPermutationSolution(jobs, factories, machines, workers,
        new int[] {9, 8, 7, 6}, 10.0, 20.0, 30.0);
  }

  private static ZhangBoResourceDomain domain() {
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(
        repeat('A', 64), 4, 1, 2,
        new int[][] {{2}, {2}},
        new double[][][] {{{1.0, 1.0}}, {{1.0, 1.0}}},
        new int[][][] {{{1, 1}}, {{1, 1}}},
        new int[][] {{1}, {1}, {1}, {1}},
        new int[] {2, 2},
        new double[][] {{1.0, 1.0}, {1.0, 1.0}},
        new int[][] {{10, 10}, {10, 10}});
    return new ZhangBoResourceDomain(instance);
  }

  private static ZhangBoResourceDomain domainOneJob() {
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(
        repeat('B', 64), 1, 1, 2,
        new int[][] {{2}, {2}},
        new double[][][] {{{1.0, 1.0}}, {{1.0, 1.0}}},
        new int[][][] {{{1, 1}}, {{1, 1}}},
        new int[][] {{1}}, new int[] {2, 2},
        new double[][] {{1.0, 1.0}, {1.0, 1.0}},
        new int[][] {{10, 10}, {10, 10}});
    return new ZhangBoResourceDomain(instance);
  }

  private static void assertResource(
      PermutationSolution<Integer> solution, int job, int factory, int machine, int worker) {
    int position = solution.getVariables().indexOf(job);
    assertEquals(factory, (int) solution.getVariableValueid(position));
    assertEquals(machine, (int) ((List<Integer>) solution.getAttribute("machine")).get(position));
    assertEquals(worker, (int) solution.getVariableValueworker(position));
  }

  private static String repeat(char value, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, value);
    return new String(chars);
  }
}
