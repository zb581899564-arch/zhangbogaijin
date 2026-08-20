package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;

/** Auditable scheduling or random decision. */
public final class DecisionEvent implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String phase;
  private final String kind;
  private final String key;
  private final String value;

  public DecisionEvent(String phase, String kind, String key, String value) {
    this.phase = phase;
    this.kind = kind;
    this.key = key;
    this.value = value;
  }

  public String getPhase() { return phase; }
  public String getKind() { return kind; }
  public String getKey() { return key; }
  public String getValue() { return value; }

  public String toCanonicalText() {
    return phase + "|" + kind + "|" + key + "|" + value;
  }
}
