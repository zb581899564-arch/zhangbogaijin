package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;

/** The v3.5 context is exactly (subSwarm role, bottleneck): 4 x 6 = 24. */
public final class V35CaTaContext implements Serializable {
  private static final long serialVersionUID = 1L;
  private final V35SubSwarmRole role;
  private final V35Bottleneck bottleneck;

  public V35CaTaContext(V35SubSwarmRole role, V35Bottleneck bottleneck) {
    if (role == null || bottleneck == null) throw new IllegalArgumentException("context fields cannot be null");
    this.role = role;
    this.bottleneck = bottleneck;
  }
  public V35SubSwarmRole getRole() { return role; }
  public V35Bottleneck getBottleneck() { return bottleneck; }
  public static int contextCount() { return V35SubSwarmRole.values().length * V35Bottleneck.values().length; }
  public boolean allows(V35MacroNeighborhood action) {
    if (action == null) return false;
    switch (bottleneck) {
      case SEQ: return action == V35MacroNeighborhood.N1 || action == V35MacroNeighborhood.N3;
      case MAC: return action == V35MacroNeighborhood.N2 || action == V35MacroNeighborhood.N4;
      case WOR: return action == V35MacroNeighborhood.N2 || action == V35MacroNeighborhood.N4
          || action == V35MacroNeighborhood.N5;
      case SET: return action == V35MacroNeighborhood.N3 || action == V35MacroNeighborhood.N4;
      case FAT: return action == V35MacroNeighborhood.N4 || action == V35MacroNeighborhood.N5;
      case BAL: return true;
      default: return false;
    }
  }
}
