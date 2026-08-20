package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class DhhfspFourVectorSolutionTest {
  @Test
  @SuppressWarnings("unchecked")
  public void shouldDeepCopyOwnedMutableState() {
    DhhfspFourVectorSolution original = Chapter4GoldenFixture.load().createSolution();
    original.setObjective(0, 12.5);
    original.setAttribute("trace", new ArrayList<>(Arrays.asList("a", "b")));

    DhhfspFourVectorSolution copy = original.copy();
    copy.setVariableValue(0, 0);
    copy.setVariableValueid(0, 0);
    copy.setMachineAssignment(0, 0);
    copy.setVariableValueworker(0, 0);
    copy.setObjective(0, 99.0);
    ((List<String>) copy.getAttribute("trace")).set(0, "changed");
    copy.setAttribute("new", "value");

    assertEquals(Integer.valueOf(5), original.getVariableValue(0));
    assertEquals(Integer.valueOf(1), original.getVariableValueid(0));
    assertEquals(Integer.valueOf(1), original.getMachineAssignment(0));
    assertEquals(Integer.valueOf(1), original.getVariableValueworker(0));
    assertEquals(12.5, original.getObjective(0), 0.0);
    assertEquals("a", ((List<String>) original.getAttribute("trace")).get(0));
    assertEquals(null, original.getAttribute("new"));
    assertNotSame(original.getJobSequence(), copy.getJobSequence());
    assertNotSame(original.getFactoryAssignments(), copy.getFactoryAssignments());
    assertNotSame(original.getMachineAssignments(), copy.getMachineAssignments());
    assertNotSame(original.getWorkerAssignments(), copy.getWorkerAssignments());
    assertNotSame(original.getAttributes(), copy.getAttributes());
  }

  @Test
  public void shouldRoundTripBothIndexBasesCanonically() {
    DhhfspFourVectorSolution solution = Chapter4GoldenFixture.load().createSolution();

    String oneBased = DhhfspEncodingCodec.serialize(solution, 1);
    assertEquals(
        "schemaVersion=1\n"
            + "semanticTag=published_baseline\n"
            + "indexBase=1\n"
            + "JS=6,10,5,4,7,2,8,3,1,9\n"
            + "FA=2,1,2,1,1,1,2,2,2,1\n"
            + "MA=2,2,2,1,1,3,1,1,2,2\n"
            + "WA=2,2,1,2,1,2,1,2,2,1\n",
        oneBased);
    DhhfspFourVectorSolution parsedOneBased = DhhfspEncodingCodec.deserialize(oneBased);
    assertEquals(solution, parsedOneBased);

    String zeroBased = DhhfspEncodingCodec.serialize(solution, 0);
    DhhfspFourVectorSolution parsedZeroBased = DhhfspEncodingCodec.deserialize(zeroBased);
    assertEquals(solution, parsedZeroBased);
    parsedZeroBased.setVariableValue(0, 0);
    assertEquals(Integer.valueOf(5), solution.getVariableValue(0));
  }

  @Test
  public void shouldRejectMalformedSerializedEncodings() {
    String valid = DhhfspEncodingCodec.serialize(Chapter4GoldenFixture.load().createSolution(), 1);
    expectIllegalArgument(
        () -> DhhfspEncodingCodec.deserialize(valid + "JS=1,2\n"), "duplicates key JS");
    expectIllegalArgument(
        () -> DhhfspEncodingCodec.deserialize(valid.replace("schemaVersion=1", "schemaVersion=2")),
        "Unsupported schemaVersion: 2");
    expectIllegalArgument(
        () -> DhhfspEncodingCodec.deserialize(valid.replace("WA=2,2,1,2,1,2,1,2,2,1\n", "")),
        "missing=[WA]");
    expectIllegalArgument(
        () -> DhhfspEncodingCodec.deserialize(valid + "extra=value\n"), "unknown=[extra]");
    expectIllegalArgument(
        () -> DhhfspEncodingCodec.deserialize(valid.replace("indexBase=1", "indexBase=3")),
        "indexBase must be 0 or 1");
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
