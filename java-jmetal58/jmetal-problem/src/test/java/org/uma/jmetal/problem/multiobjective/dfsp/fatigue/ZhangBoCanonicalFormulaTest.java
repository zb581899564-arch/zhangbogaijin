package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/** Hand-checkable PT/SET/AT, multiplier, accumulation and delta formulas. */
public class ZhangBoCanonicalFormulaTest {
  @Test
  public void firstOperationMatchesPtSetAtAndDeltaByDefinition() {
    String sha = repeat('E', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 1, 1, new int[][] {{4}}, repeat('F', 64));
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(sha, 1, 1, 1,
        new int[][] {{1}}, new double[][][] {{{2.0}}}, new int[][][] {{{7}}},
        new int[][] {{10}}, new int[] {1}, new double[][] {{1.25}}, new int[][] {{3}}, extension);
    double lambda = 0.02;
    ZhangBoFatigueParameters parameters = new ZhangBoFatigueParameters(sha,
        new double[][][] {{{lambda}}}, new double[][][] {{{0.05}}},
        new double[] {0.30}, 0.80, 0.90, "");
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        Arrays.asList(0), Arrays.asList(0), Arrays.asList(0), Arrays.asList(0),
        ProductionDecodeMode.FM1.getSemanticTag(), 7);
    ZhangBoFatigueOperationRecord record = new ZhangBoFatigueEvaluator().evaluate(
        instance, parameters, solution, ZhangBoFatigueEvaluationMode.ACCUMULATION_ONLY)
        .getOperations().get(0);
    double pt = 10.0 / (2.0 * 1.25);
    double set = 4.0 / 1.25;
    double at = pt + set;
    assertEquals(pt, record.baseProcessingDuration, 0.0);
    assertEquals(set, record.baseSetupDuration, 0.0);
    assertEquals(at, record.baseDuration, 0.0);
    assertEquals(1.0 + 0.30 / Math.log(2.0) * Math.log1p(0.0), record.durationMultiplier, 0.0);
    assertEquals(at, record.actualDuration, 0.0);
    assertEquals(0.30 / (lambda * Math.log(2.0)), parameters.getDelta(0, 0, 0), 1.0e-12);
  }

  private static String repeat(char value, int count) {
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }
}
