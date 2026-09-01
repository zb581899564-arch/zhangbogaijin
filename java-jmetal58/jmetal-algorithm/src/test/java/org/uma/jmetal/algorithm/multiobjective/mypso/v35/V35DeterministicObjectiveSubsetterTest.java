package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class V35DeterministicObjectiveSubsetterTest {
  @Test
  public void preservesThreeExtremesAndExactCapacity() {
    List<double[]> points = points(80);
    List<double[]> selected = V35DeterministicObjectiveSubsetter.selectPoints(points, 30);
    assertEquals(30, selected.size());
    assertTrue(contains(selected, 0.0, 100.0, 100.0));
    assertTrue(contains(selected, 100.0, 0.0, 100.0));
    assertTrue(contains(selected, 100.0, 100.0, 0.0));
  }

  @Test
  public void reversedInputProducesByteIdenticalCsv() {
    List<double[]> points = points(80);
    List<double[]> reversed = new ArrayList<>(points);
    Collections.reverse(reversed);
    String first = V35DeterministicObjectiveSubsetter.pointsToCsv(
        V35DeterministicObjectiveSubsetter.selectPoints(points, 25));
    String second = V35DeterministicObjectiveSubsetter.pointsToCsv(
        V35DeterministicObjectiveSubsetter.selectPoints(reversed, 25));
    assertEquals(first, second);
  }

  @Test
  public void exactObjectiveDuplicatesAreRemovedAndDegenerateRangesAreSafe() {
    List<double[]> points = Arrays.asList(new double[] {1, 5, 9}, new double[] {1, 5, 9},
        new double[] {1, 4, 8}, new double[] {1, 3, 7}, new double[] {1, 2, 6});
    List<double[]> selected = V35DeterministicObjectiveSubsetter.selectPoints(points, 50);
    assertEquals(4, selected.size());
    assertEquals(4, new java.util.HashSet<String>(keys(selected)).size());
  }

  @Test
  public void registeredCapacitiesAreDeterministic() {
    for (int capacity : new int[] {25, 30, 50, 100, 200}) {
      List<double[]> source = points(260);
      assertEquals(capacity,
          V35DeterministicObjectiveSubsetter.selectPoints(source, capacity).size());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void capacityBelowThreeIsRejectedRatherThanBreakingExtremeRetention() {
    V35DeterministicObjectiveSubsetter.selectPoints(points(10), 2);
  }

  @Test
  public void frontKindsPreventPresentationAndSensitivitySetsFromMainMetrics() {
    assertTrue(V35FrontKind.DECISION_FRONT.isMainMetricEligible());
    assertTrue(V35FrontKind.DECISION_FRONT.isReferenceEligible());
    assertFalse(V35FrontKind.OBSERVED_FULL_FRONT.isMainMetricEligible());
    assertFalse(V35FrontKind.REPRESENTATIVE_FRONT_K30.isMainMetricEligible());
    assertFalse(V35FrontKind.SENSITIVITY_FRONT_K25.isReferenceEligible());
    try {
      V35FrontKind.REPRESENTATIVE_FRONT_K30.requireMainMetricEligible();
      fail("K30 must be rejected for main metrics");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("forbidden"));
    }
    try {
      V35ArchiveMetricInputGate.requireReferenceFreezeInput(
          V35FrontKind.DECISION_FRONT, false);
      fail("PFref must not freeze before all registered runs complete");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("every registered run"));
    }
    V35ArchiveMetricInputGate.requireReferenceFreezeInput(
        V35FrontKind.DECISION_FRONT, true);
  }

  private static List<double[]> points(int count) {
    List<double[]> result = new ArrayList<>();
    result.add(new double[] {0, 100, 100});
    result.add(new double[] {100, 0, 100});
    result.add(new double[] {100, 100, 0});
    for (int index = 0; index < count - 3; index++) {
      result.add(new double[] {10 + index, 90 - index * 0.3, 50 + (index % 11)});
    }
    return result;
  }

  private static boolean contains(List<double[]> values, double a, double b, double c) {
    for (double[] value : values) if (value[0] == a && value[1] == b && value[2] == c) return true;
    return false;
  }

  private static List<String> keys(List<double[]> values) {
    List<String> result = new ArrayList<>();
    for (double[] value : values) result.add(Arrays.toString(value));
    return result;
  }
}
