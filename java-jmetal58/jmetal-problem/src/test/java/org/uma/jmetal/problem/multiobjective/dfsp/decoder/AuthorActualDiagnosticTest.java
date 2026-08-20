package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.EDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.solution.Solution;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthorActualDiagnosticTest {
  @Test
  public void shouldInvokeUnchangedAuthorPathOnlyThroughDiagnosticBoundary() throws IOException {
    String previous = System.getProperty("dhfsp.data.dir");
    try {
      System.setProperty("dhfsp.data.dir",
          Paths.get("..", "EADHFSP").toAbsolutePath().normalize().toString());
      EDHHFSPW authorProblem = new EDHHFSPW(20, 2, 3, 1);
      LegacyDiagnosticSolution solution = new LegacyDiagnosticSolution(20, 2);
      AuthorActualObservation observation =
          new AuthorActualDiagnostic().observe(authorProblem, solution);

      assertEquals(7, observation.getObjectives().length);
      assertArrayEquals(new double[] {
          331.4505494505495, 13984.724228152796, 0.0, 2.0, 2.0, 1.0,
          10929.527472527472}, observation.getObjectives(), 1.0e-9);
      assertEquals(7, observation.getKnownDifferences().size());
      assertTrue(observation.getKnownDifferences().toString().contains("10% ST"));
      assertTrue(observation.getKnownDifferences().toString().contains("slot 6"));
      assertTrue("Unexpected author_actual failure: " + observation.getFailureClass()
          + ": " + observation.getFailureMessage(), observation.isSuccessful());
      assertFalse(observation.isEncodingMutated());
    } finally {
      if (previous == null) System.clearProperty("dhfsp.data.dir");
      else System.setProperty("dhfsp.data.dir", previous);
    }
  }

  private static final class LegacyDiagnosticSolution
      implements PermutationSolution<Integer> {
    private static final long serialVersionUID = 1L;
    private final List<Integer> jobs;
    private final List<Integer> factories;
    private final List<Integer> workers;
    private final double[] objectives;
    private final Map<Object, Object> attributes;

    LegacyDiagnosticSolution(int jobCount, int stages) {
      jobs = new ArrayList<>();
      factories = new ArrayList<>();
      workers = new ArrayList<>();
      objectives = new double[7];
      attributes = new HashMap<>();
      List<Integer> machines = new ArrayList<>();
      for (int job = 0; job < jobCount; job++) {
        jobs.add(job);
        factories.add(job % 3);
      }
      for (int index = 0; index < jobCount * stages; index++) {
        workers.add(0);
        machines.add(0);
      }
      attributes.put("machine", machines);
    }

    private LegacyDiagnosticSolution(LegacyDiagnosticSolution source) {
      jobs = new ArrayList<>(source.jobs);
      factories = new ArrayList<>(source.factories);
      workers = new ArrayList<>(source.workers);
      objectives = source.objectives.clone();
      attributes = new HashMap<>(source.attributes);
      Object machines = source.attributes.get("machine");
      if (machines instanceof List) {
        attributes.put("machine", new ArrayList<>((List<?>) machines));
      }
    }

    @Override public void setObjective(int index, double value) { objectives[index] = value; }
    @Override public double getObjective(int index) { return objectives[index]; }
    @Override public double[] getObjectives() { return objectives; }
    @Override public Integer getVariableValue(int index) { return jobs.get(index); }
    @Override public List<Integer> getVariables() { return jobs; }
    @Override public void setVariableValue(int index, Integer value) { jobs.set(index, value); }
    @Override public String getVariableValueString(int index) { return jobs.get(index).toString(); }
    @Override public int getNumberOfVariables() { return jobs.size(); }
    @Override public int getNumberOfObjectives() { return objectives.length; }
    @Override public Solution<Integer> copy() { return new LegacyDiagnosticSolution(this); }
    @Override public void setAttribute(Object id, Object value) { attributes.put(id, value); }
    @Override public Object getAttribute(Object id) { return attributes.get(id); }
    @Override public Map<Object, Object> getAttributes() { return attributes; }
    @Override public Integer getVariableValueid(int index) { return factories.get(index); }
    @Override public List<Integer> getVariablesid() { return factories; }
    @Override public void setVariableValueid(int index, Integer value) { factories.set(index, value); }
    @Override public int getNumberOfVariablesid() { return factories.size(); }
    @Override public List<Integer> getVariablesworker() { return workers; }
    @Override public int getNumberOfVariablesworker() { return workers.size(); }
    @Override public void setVariableValueworker(int index, Integer value) { workers.set(index, value); }
    @Override public Integer getVariableValueworker(int index) { return workers.get(index); }
  }
}
