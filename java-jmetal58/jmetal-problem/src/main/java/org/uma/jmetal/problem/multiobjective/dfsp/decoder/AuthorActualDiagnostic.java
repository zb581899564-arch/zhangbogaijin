package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.EDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Diagnostic bridge to the unchanged EDHHFSPW path; never used by the production problem. */
public final class AuthorActualDiagnostic {
  private static final List<String> KNOWN_DIFFERENCES = Collections.unmodifiableList(Arrays.asList(
      "active duration uses ST + 10% ST instead of published SUT",
      "active calculation does not honor stage-one MA/WA directly",
      "active worker array defaults to worker index 0",
      "default solution construction depends on 150_8_5_1.txt",
      "author problem exposes seven objective slots and stores TWC in slot 6",
      "default solution construction uses unseeded random sources",
      "active evaluate path contains no published right-shift phase"));

  public AuthorActualObservation observe(
      EDHHFSPW problem, PermutationSolution<Integer> solution) {
    if (problem == null || solution == null) {
      throw new IllegalArgumentException("problem and solution must not be null");
    }
    String before = encodingText(solution);
    Throwable failure = null;
    try {
      problem.evaluate(solution);
    } catch (Throwable throwable) {
      failure = throwable;
    }
    boolean mutated = !before.equals(encodingText(solution));
    return new AuthorActualObservation(
        failure == null, solution.getObjectives(), failure, mutated, KNOWN_DIFFERENCES);
  }

  public List<String> getKnownDifferences() {
    return KNOWN_DIFFERENCES;
  }

  private static String encodingText(PermutationSolution<Integer> solution) {
    List<Object> values = new ArrayList<>();
    values.add(new ArrayList<>(solution.getVariables()));
    values.add(new ArrayList<>(solution.getVariablesid()));
    values.add(new ArrayList<>(solution.getVariablesworker()));
    values.add(solution.getAttribute("machine"));
    return values.toString();
  }
}
