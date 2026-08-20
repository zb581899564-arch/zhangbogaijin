package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.FourVectorOperators;
import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.HmopsoQgsConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Contract tests for the replayable structured author baseline updater. */
public class ZhangBoBaselineUpdaterTest {
  @Test
  public void injectedSeedReplaysAndDifferentSeedChangesEvents() {
    String first = run(20260808L);
    assertEquals(first, run(20260808L));
    assertNotEquals(first, run(20260809L));
  }

  @Test
  public void positionAlignedResourceVectorsRemainLegalAfterJsPso() {
    ZhangBoTestPermutationSolution current = solution(
        new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0},
        new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});
    ZhangBoTestPermutationSolution pbest = solution(
        new int[] {2, 0, 3, 1}, new int[] {1, 0, 1, 0},
        new int[] {1, 0, 1, 0}, new int[] {1, 0, 1, 0});
    ZhangBoTestPermutationSolution gbest = solution(
        new int[] {3, 2, 1, 0}, new int[] {0, 1, 0, 1},
        new int[] {0, 1, 0, 1}, new int[] {0, 1, 0, 1});
    ZhangBoBaselineUpdater.Result result = new ZhangBoBaselineUpdater().update(
        current, pbest, gbest, domain(), 0.8, 1.0, 0.0, 0.0, 0.0,
        new JavaRandomGenerator(7L));
    PermutationSolution<Integer> offspring = result.getSolution();
    @SuppressWarnings("unchecked")
    List<Integer> machines = (List<Integer>) offspring.getAttribute("machine");
    for (int position = 0; position < offspring.getNumberOfVariables(); position++) {
      int factory = offspring.getVariableValueid(position);
      assertTrue(domain().isFactoryValid(factory));
      assertTrue(domain().isMachineValid(factory, machines.get(position)));
      assertTrue(domain().isWorkerValid(factory,
          offspring.getVariableValueworker(position)));
    }
    // The published baseline is position aligned: JS changes may reassign the resource
    // package at a position to another job.  CFVF is the separate identity-aligned path.
    assertEquals(offspring.getNumberOfVariables(), machines.size());
    assertTrue(result.getEvents().toString().contains("pso:JS"));
  }

  @Test
  public void structuredBaselineMatchesTheP4Fig5Fig6OracleForIdenticalRandomEvents() {
    for (long seed : new long[] {7L, 20260808L, 20260809L}) {
      DhhfspFourVectorSolution current = paperSolution(
          new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0},
          new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});
      DhhfspFourVectorSolution pbest = paperSolution(
          new int[] {2, 0, 3, 1}, new int[] {1, 0, 1, 0},
          new int[] {1, 0, 1, 0}, new int[] {1, 0, 1, 0});
      DhhfspFourVectorSolution gbest = paperSolution(
          new int[] {3, 2, 1, 0}, new int[] {0, 1, 0, 1},
          new int[] {0, 1, 0, 1}, new int[] {0, 1, 0, 1});
      HmopsoQgsConfiguration configuration =
          HmopsoQgsConfiguration.publishedTable9(seed);
      DhhfspFourVectorSolution oracle = new FourVectorOperators(
          paperInstance(), new JavaRandomGenerator(seed)).update(
              current, pbest, gbest, configuration);
      ZhangBoBaselineUpdater.Result runtime = new ZhangBoBaselineUpdater().update(
          current, pbest, gbest, domain(),
          configuration.getRandomCoefficientUpperBound(),
          configuration.getCrossoverProbabilities()[0],
          configuration.getCrossoverProbabilities()[1],
          configuration.getCrossoverProbabilities()[2],
          configuration.getMutationProbabilities()[0],
          configuration.getMutationProbabilities()[1],
          configuration.getMutationProbabilities()[2],
          new JavaRandomGenerator(seed));
      assertEquals(oracle.getJobSequence(), runtime.getSolution().getVariables());
      assertEquals(oracle.getFactoryAssignments(), runtime.getSolution().getVariablesid());
      assertEquals(oracle.getMachineAssignments(),
          ZhangBoMachineVectorSupport.copy(runtime.getSolution(), 4));
      assertEquals(oracle.getWorkerAssignments(), runtime.getSolution().getVariablesworker());
    }
  }

  private static String run(long seed) {
    ZhangBoBaselineUpdater.Result result = new ZhangBoBaselineUpdater().update(
        solution(new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0},
            new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0}),
        solution(new int[] {2, 0, 3, 1}, new int[] {1, 0, 1, 0},
            new int[] {1, 0, 1, 0}, new int[] {1, 0, 1, 0}),
        solution(new int[] {3, 2, 1, 0}, new int[] {0, 1, 0, 1},
            new int[] {0, 1, 0, 1}, new int[] {0, 1, 0, 1}),
        domain(), 0.8, 0.7, 0.2, 0.2, 0.2,
        new JavaRandomGenerator(seed));
    return result.getSolution().getVariables() + "|"
        + result.getSolution().getVariablesid() + "|"
        + result.getSolution().getAttribute("machine") + "|"
        + result.getSolution().getVariablesworker() + "|" + result.getEvents();
  }

  private static ZhangBoTestPermutationSolution solution(
      int[] jobs, int[] factories, int[] machines, int[] workers) {
    return new ZhangBoTestPermutationSolution(jobs, factories, machines, workers,
        new int[] {9, 8, 7, 6}, 10.0, 20.0, 30.0);
  }

  private static DhhfspFourVectorSolution paperSolution(
      int[] jobs, int[] factories, int[] machines, int[] workers) {
    return new DhhfspFourVectorSolution(integers(jobs), integers(factories),
        integers(machines), integers(workers), "published_baseline", 7);
  }

  private static DhhfspInstance paperInstance() {
    return new DhhfspInstance(4, 1, 2,
        new double[][] {{1, 1, 1, 1}}, new double[][] {{1, 1, 1, 1}},
        new double[][][] {{{1, 1}}, {{1, 1}}},
        new double[][][] {{{1, 1}}, {{1, 1}}},
        new double[][][] {{{1, 1}}, {{1, 1}}},
        new double[][][] {{{1, 1}}, {{1, 1}}});
  }

  private static List<Integer> integers(int[] values) {
    java.util.ArrayList<Integer> result = new java.util.ArrayList<>();
    for (int value : values) result.add(value);
    return result;
  }

  private static ZhangBoResourceDomain domain() {
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(
        repeat('C', 64), 4, 1, 2,
        new int[][] {{2}, {2}},
        new double[][][] {{{1.0, 1.0}}, {{1.0, 1.0}}},
        new int[][][] {{{1, 1}}, {{1, 1}}},
        new int[][] {{1}, {1}, {1}, {1}}, new int[] {2, 2},
        new double[][] {{1.0, 1.0}, {1.0, 1.0}},
        new int[][] {{10, 10}, {10, 10}});
    return new ZhangBoResourceDomain(instance);
  }

  private static String repeat(char value, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, value);
    return new String(chars);
  }
}
