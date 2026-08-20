package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable list of concrete schedule violations. */
public final class ScheduleValidationReport implements Serializable {
  private static final long serialVersionUID = 1L;
  private final List<String> violations;

  public ScheduleValidationReport(List<String> violations) {
    this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
  }

  public boolean isValid() { return violations.isEmpty(); }
  public List<String> getViolations() { return violations; }

  public void throwIfInvalid() {
    if (!isValid()) {
      throw new IllegalStateException("Invalid schedule: " + violations);
    }
  }
}
