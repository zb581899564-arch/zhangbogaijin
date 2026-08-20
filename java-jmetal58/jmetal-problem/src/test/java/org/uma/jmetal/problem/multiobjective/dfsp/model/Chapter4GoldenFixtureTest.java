package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class Chapter4GoldenFixtureTest {
  private static final double EPSILON = 0.0;

  @Test
  public void shouldLoadEveryPublishedTableValue() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance instance = fixture.getInstance();

    assertEquals(10, instance.getNumberOfJobs());
    assertEquals(2, instance.getNumberOfStages());
    assertEquals(2, instance.getNumberOfFactories());
    assertArrayEquals(new double[] {10,16,12,9,10,8,13,14,16,9},
        instance.getStandardProcessingTimes(0), EPSILON);
    assertArrayEquals(new double[] {8,11,12,14,16,15,18,12,10,9},
        instance.getStandardProcessingTimes(1), EPSILON);
    assertArrayEquals(new double[] {1,3,2,1,2,2,2,2,3,2},
        instance.getStandardSetupTimes(0), EPSILON);
    assertArrayEquals(new double[] {1,2,3,3,3,2,1,2,1,1},
        instance.getStandardSetupTimes(1), EPSILON);

    assertArrayEquals(new double[] {1,1.2,1.3}, instance.getMachineSpeeds(0,0), EPSILON);
    assertArrayEquals(new double[] {1.2,1.1}, instance.getMachineSpeeds(0,1), EPSILON);
    assertArrayEquals(new double[] {1,1.1}, instance.getMachineSpeeds(1,0), EPSILON);
    assertArrayEquals(new double[] {1,1.2,0.9}, instance.getMachineSpeeds(1,1), EPSILON);
    assertArrayEquals(new double[] {7,10,12}, instance.getMachineEnergyPerUnit(0,0), EPSILON);
    assertArrayEquals(new double[] {9,8}, instance.getMachineEnergyPerUnit(0,1), EPSILON);
    assertArrayEquals(new double[] {7,8}, instance.getMachineEnergyPerUnit(1,0), EPSILON);
    assertArrayEquals(new double[] {7,8,6}, instance.getMachineEnergyPerUnit(1,1), EPSILON);

    assertArrayEquals(new double[] {1.2,1.0}, instance.getWorkerEfficiencies(0,0), EPSILON);
    assertArrayEquals(new double[] {0.9,1.1}, instance.getWorkerEfficiencies(0,1), EPSILON);
    assertArrayEquals(new double[] {1.1,1.2}, instance.getWorkerEfficiencies(1,0), EPSILON);
    assertArrayEquals(new double[] {1.2,0.9}, instance.getWorkerEfficiencies(1,1), EPSILON);
    assertArrayEquals(new double[] {12,10}, instance.getWorkerCostPerUnit(0,0), EPSILON);
    assertArrayEquals(new double[] {9,11}, instance.getWorkerCostPerUnit(0,1), EPSILON);
    assertArrayEquals(new double[] {11,12}, instance.getWorkerCostPerUnit(1,0), EPSILON);
    assertArrayEquals(new double[] {12,9}, instance.getWorkerCostPerUnit(1,1), EPSILON);
  }

  @Test
  public void shouldKeepPublishedAndRuntimeIndexBasesExplicit() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    assertEquals(1, fixture.getSourceIndexBase());
    assertEquals(Arrays.asList(6,10,5,4,7,2,8,3,1,9), fixture.getPublishedJobSequence());
    assertEquals(Arrays.asList(2,1,2,1,1,1,2,2,2,1), fixture.getPublishedFactoryAssignments());
    assertEquals(Arrays.asList(2,2,2,1,1,3,1,1,2,2), fixture.getPublishedMachineAssignments());
    assertEquals(Arrays.asList(2,2,1,2,1,2,1,2,2,1), fixture.getPublishedWorkerAssignments());

    DhhfspFourVectorSolution solution = fixture.createSolution();
    assertEquals(Arrays.asList(5,9,4,3,6,1,7,2,0,8), solution.getJobSequence());
    assertEquals(Arrays.asList(1,0,1,0,0,0,1,1,1,0), solution.getFactoryAssignments());
    assertEquals(Arrays.asList(1,1,1,0,0,2,0,0,1,1), solution.getMachineAssignments());
    assertEquals(Arrays.asList(1,1,0,1,0,1,0,1,1,0), solution.getWorkerAssignments());
    DhhfspEncodingValidator.validateOrThrow(solution, fixture.getInstance());
  }

  @Test
  public void shouldProvideBidirectionalJobPositionMapping() {
    DhhfspFourVectorSolution solution = Chapter4GoldenFixture.load().createSolution();
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      assertEquals(position, solution.positionOfJob(solution.jobAtPosition(position)));
    }
    expectIllegalArgument(() -> solution.jobAtPosition(-1), "JS position -1");
    expectIllegalArgument(() -> solution.jobAtPosition(10), "JS position 10");
    expectIllegalArgument(() -> solution.positionOfJob(10), "does not contain job 10");
  }

  @Test
  public void shouldDefensivelyCopyInstanceArrays() {
    DhhfspInstance instance = Chapter4GoldenFixture.load().getInstance();
    double[] speeds = instance.getMachineSpeeds(0, 0);
    speeds[0] = 999;
    assertNotEquals(999.0, instance.getMachineSpeeds(0, 0)[0], EPSILON);
  }

  @Test
  public void shouldRejectEveryEncodingViolationWithLocation() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance instance = fixture.getInstance();

    DhhfspFourVectorSolution duplicate = fixture.createSolution();
    duplicate.setVariableValue(1, duplicate.getVariableValue(0));
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(duplicate, instance), "JS position 1");

    DhhfspFourVectorSolution shortFa = createWith(
        fixture.createSolution(), "FA", Arrays.asList(1,0));
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(shortFa, instance), "FA length 2");

    DhhfspFourVectorSolution nullJs = fixture.createSolution();
    nullJs.setVariableValue(0, null);
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(nullJs, instance), "JS position 0 contains null");

    DhhfspFourVectorSolution badFa = fixture.createSolution();
    badFa.setVariableValueid(0, 2);
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(badFa, instance),
        "FA position 0 has value 2");

    DhhfspFourVectorSolution badMa = fixture.createSolution();
    badMa.setMachineAssignment(0, 2);
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(badMa, instance),
        "MA position 0 has value 2");

    DhhfspFourVectorSolution badWa = fixture.createSolution();
    badWa.setVariableValueworker(0, 2);
    expectIllegalArgument(
        () -> DhhfspEncodingValidator.validateOrThrow(badWa, instance),
        "WA position 0 has value 2");
  }

  private static DhhfspFourVectorSolution createWith(
      DhhfspFourVectorSolution source, String vector, List<Integer> replacement) {
    List<Integer> js = new ArrayList<>(source.getJobSequence());
    List<Integer> fa = new ArrayList<>(source.getFactoryAssignments());
    List<Integer> ma = new ArrayList<>(source.getMachineAssignments());
    List<Integer> wa = new ArrayList<>(source.getWorkerAssignments());
    if ("FA".equals(vector)) {
      fa = replacement;
    }
    return new DhhfspFourVectorSolution(js, fa, ma, wa, source.getSemanticTag());
  }

  private static void expectIllegalArgument(Runnable action, String expectedText) {
    try {
      action.run();
    } catch (IllegalArgumentException exception) {
      org.junit.Assert.assertTrue(
          "Expected message containing: " + expectedText + ", actual: " + exception.getMessage(),
          exception.getMessage().contains(expectedText));
      return;
    }
    org.junit.Assert.fail("Expected IllegalArgumentException containing: " + expectedText);
  }
}
