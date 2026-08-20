package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

public class ZhangBoIncrementalParetoArchiveTest {
  @Test
  public void incrementalUpdateMatchesLegacyStableBatchScan() {
    Random random = new Random(20260808L);
    for (int round = 0; round < 100; round++) {
      List<PermutationSolution<Integer>> source = new ArrayList<>();
      for (int index = 0; index < 80; index++) {
        source.add(solution(index,
            random.nextInt(12), random.nextInt(12), random.nextInt(12)));
      }
      List<PermutationSolution<Integer>> legacy = copies(source);
      legacyScan(legacy);
      List<PermutationSolution<Integer>> incremental = new ArrayList<>();
      for (PermutationSolution<Integer> value : source) {
        ZhangBoIncrementalParetoArchive.add(incremental,
            (PermutationSolution<Integer>) value.copy());
      }
      assertEquals(signatures(legacy), signatures(incremental));
    }
  }

  private static DhhfspFourVectorSolution solution(int id, double a, double b, double c) {
    DhhfspFourVectorSolution value = new DhhfspFourVectorSolution(
        Arrays.asList(id), Arrays.asList(0), Arrays.asList(0), Arrays.asList(0),
        "deterministic_canonical", 7);
    value.setObjective(0, a); value.setObjective(1, b); value.setObjective(6, c);
    return value;
  }

  private static List<PermutationSolution<Integer>> copies(
      List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) {
      result.add((PermutationSolution<Integer>) value.copy());
    }
    return result;
  }

  private static void legacyScan(List<PermutationSolution<Integer>> values) {
    for (int i = 0; i < values.size(); i++) {
      for (int j = i + 1; j < values.size(); j++) {
        if (ZhangBoIncrementalParetoArchive.weaklyDominates(values.get(i), values.get(j))) {
          values.remove(j--);
        }
      }
      for (int j = i + 1; j < values.size(); j++) {
        if (ZhangBoIncrementalParetoArchive.weaklyDominates(values.get(j), values.get(i))) {
          values.remove(i--); break;
        }
      }
    }
  }

  private static List<String> signatures(List<PermutationSolution<Integer>> values) {
    List<String> result = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) {
      result.add(value.getVariableValue(0) + ":" + value.getObjective(0) + ":"
          + value.getObjective(1) + ":" + value.getObjective(6));
    }
    return result;
  }
}
