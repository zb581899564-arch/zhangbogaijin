package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class P8MetricCalculatorTest {
  @Test
  public void filtersStrictDominanceAndDuplicatesStably() {
    List<double[]> front = P8MetricCalculator.nondominated(Arrays.asList(
        p(1, 1, 1), p(2, 2, 2), p(1, 1, 1), p(0, 3, 3)));
    assertEquals(2, front.size());
    assertEquals(0.0, front.get(0)[0], 0.0);
    assertEquals(1.0, front.get(1)[0], 0.0);
  }

  @Test
  public void identicalApproximationHasZeroIgdAndFullCoverage() {
    List<double[]> front = Arrays.asList(p(1, 3, 2), p(2, 1, 3), p(3, 2, 1));
    P8MetricCalculator.Metrics metrics = P8MetricCalculator.calculate(front, front);
    assertEquals(0.0, metrics.igd, 1e-12);
    assertEquals(1.0, metrics.cForward, 1e-12);
    assertEquals(1.0, metrics.cReverse, 1e-12);
    assertEquals(3, metrics.nondominatedCount);
    assertTrue(metrics.hv > 0.0);
  }

  @Test
  public void singleNormalizedPointUsesTheDeclaredReferenceMargin() {
    List<double[]> approximation = new ArrayList<>();
    approximation.add(p(5, 5, 5));
    P8MetricCalculator.Metrics metrics = P8MetricCalculator.calculate(
        approximation, approximation);
    assertEquals(1.331, metrics.hv, 1e-12);
    assertEquals(0.0, metrics.spacing, 1e-12);
  }

  private static double[] p(double a, double b, double c) {
    return new double[] {a, b, c};
  }
}
