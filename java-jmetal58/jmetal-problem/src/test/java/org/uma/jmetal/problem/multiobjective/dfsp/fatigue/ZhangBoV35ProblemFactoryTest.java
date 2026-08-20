package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;

import static org.junit.Assert.assertEquals;

public class ZhangBoV35ProblemFactoryTest {
  @Test
  public void formalFactoryAlwaysBindsNoShiftAndDegenerateSetup() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = parameters(instance.getInstanceSha256());
    ZhangBoCanonicalProductionProblem problem = ZhangBoV35ProblemFactory.create(
        instance, parameters, ProductionDecodeMode.FM3, 20260808L);
    assertEquals(ZhangBoShiftMode.NONE, problem.getShiftConfiguration().getMode());
    org.junit.Assert.assertTrue(problem.getSetupModel().isFormalDegenerate());
  }

  @Test(expected = IllegalArgumentException.class)
  public void authorDiagnosticCannotEnterFormalFactory() {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoV35ProblemFactory.create(instance, parameters(instance.getInstanceSha256()),
        ProductionDecodeMode.AUTHOR_DIAGNOSTIC, 20260808L);
  }

  private static ZhangBoFatigueInstanceData instance() {
    String sha = repeat('C', 64);
    ZhangBoInstanceExtension extension = new ZhangBoInstanceExtension(
        sha, 2, 1, new int[][] {{1}, {2}}, repeat('D', 64));
    return new ZhangBoFatigueInstanceData(sha, 2, 1, 1,
        new int[][] {{1}}, new double[][][] {{{1.0}}}, new int[][][] {{{1}}},
        new int[][] {{10}, {10}}, new int[] {1}, new double[][] {{1.0}},
        new int[][] {{10}}, extension);
  }

  private static ZhangBoFatigueParameters parameters(String sha) {
    return new ZhangBoFatigueParameters(sha,
        new double[][][] {{{0.02}}}, new double[][][] {{{0.05}}},
        new double[] {0.30}, 0.80, 0.90, "");
  }

  private static String repeat(char value, int count) {
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }
}
