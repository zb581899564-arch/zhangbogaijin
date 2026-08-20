package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

/** Three boundary groups and one PDDR-FF center group. */
public enum SubSwarm {
  G1_CMAX(0), G2_TEC(1), G3_TWC(2), G4_CENTER(-1);

  private final int objective;
  SubSwarm(int objective) { this.objective = objective; }
  public int getObjective() { return objective; }
  public boolean isBoundary() { return objective >= 0; }
}
