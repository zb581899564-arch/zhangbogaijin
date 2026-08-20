package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.util.Arrays;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/** Input-only screening gate for the five-job hand-calculation particle. */
public class ZhangBoI0ParticleScreeningTest {
  @Test
  public void frozenI0V2ParticleHasOneHonestLeftAndOneHonestRightShiftEvent() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = parameters(instance.getInstanceSha256());
    DhhfspFourVectorSolution particle = new DhhfspFourVectorSolution(
        Arrays.asList(2, 0, 1, 3, 4), Arrays.asList(0, 1, 1, 0, 0),
        Arrays.asList(0, 0, 0, 0, 1), Arrays.asList(0, 0, 0, 1, 1),
        ProductionDecodeMode.FM3.getSemanticTag(), 7);
    ZhangBoFatigueEvaluationResult result = new ZhangBoFatigueEvaluator().evaluate(
        instance, parameters, particle,
        ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
        ZhangBoShiftConfiguration.formalLeftRight().withFullTrace(false));
    ZhangBoShiftSummary summary = result.getShiftSummary();
    assertNotNull(summary);
    assertEquals(6, summary.getLeftCandidates());
    assertEquals(1, summary.getLeftAccepted());
    assertEquals(41, summary.getRightCandidates());
    assertEquals(1, summary.getRightAccepted());
    assertEquals(42, summary.getInternalPropagations());
  }

  private static ZhangBoFatigueInstanceData instance() {
    String sha = repeat('A', 64);
    int[][] setup = new int[][] {{2, 1}, {1, 2}, {2, 1}, {1, 2}, {1, 1}};
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 5, 2, setup, repeat('B', 64));
    return new ZhangBoFatigueInstanceData(sha, 5, 2, 2,
        new int[][] {{2, 2}, {2, 2}},
        new double[][][] {{{1.00, 1.25}, {1.00, 1.25}},
            {{1.00, 1.20}, {1.10, 1.25}}},
        new int[][][] {{{6, 8}, {7, 9}}, {{6, 7}, {7, 8}}},
        new int[][] {{10, 6}, {6, 8}, {8, 5}, {7, 9}, {5, 7}},
        new int[] {4, 4},
        new double[][] {{1.00, 1.00, 1.00, 1.20}, {1.00, 1.10, 1.00, 1.15}},
        new int[][] {{10, 11, 10, 12}, {10, 11, 10, 12}}, extension);
  }

  private static ZhangBoFatigueParameters parameters(String sha) {
    return new ZhangBoFatigueParameters(sha,
        new double[][][] {
            {{0.020, 0.025, 0.020, 0.020}, {0.020, 0.020, 0.018, 0.022}},
            {{0.021, 0.026, 0.020, 0.020}, {0.020, 0.020, 0.019, 0.023}}},
        new double[][][] {
            {{0.050, 0.040, 0.050, 0.050}, {0.050, 0.050, 0.060, 0.050}},
            {{0.050, 0.040, 0.050, 0.050}, {0.050, 0.050, 0.060, 0.050}}},
        new double[] {0.30, 0.30}, 0.80, 0.90, repeat('C', 64));
  }

  private static String repeat(char value, int count) {
    char[] values = new char[count];
    Arrays.fill(values, value);
    return new String(values);
  }

}
