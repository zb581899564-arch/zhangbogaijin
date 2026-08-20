package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Captured outcome of invoking the unchanged author implementation. */
public final class AuthorActualObservation implements Serializable {
  private static final long serialVersionUID = 1L;
  private final boolean successful;
  private final double[] objectives;
  private final String failureClass;
  private final String failureMessage;
  private final boolean encodingMutated;
  private final List<String> knownDifferences;

  AuthorActualObservation(
      boolean successful, double[] objectives, Throwable failure,
      boolean encodingMutated, List<String> knownDifferences) {
    this.successful = successful;
    this.objectives = objectives == null ? new double[0] : objectives.clone();
    this.failureClass = failure == null ? "" : failure.getClass().getName();
    this.failureMessage = failure == null || failure.getMessage() == null
        ? "" : failure.getMessage();
    this.encodingMutated = encodingMutated;
    this.knownDifferences = Collections.unmodifiableList(new ArrayList<>(knownDifferences));
  }

  public boolean isSuccessful() { return successful; }
  public double[] getObjectives() { return objectives.clone(); }
  public String getFailureClass() { return failureClass; }
  public String getFailureMessage() { return failureMessage; }
  public boolean isEncodingMutated() { return encodingMutated; }
  public List<String> getKnownDifferences() { return knownDifferences; }
}
