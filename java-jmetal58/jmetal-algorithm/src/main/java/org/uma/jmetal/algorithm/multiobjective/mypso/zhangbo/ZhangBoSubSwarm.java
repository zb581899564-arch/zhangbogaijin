package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

/** Canonical subgroup roles used by the ZhangBo innovation path. */
public enum ZhangBoSubSwarm {
  G1_CMAX(0, 0),
  G2_TEC(1, 1),
  G3_TWC(2, 6),
  G4_BALANCED(3, -1);

  private final int semanticId;
  private final int objectiveIndex;

  ZhangBoSubSwarm(int semanticId, int objectiveIndex) {
    this.semanticId = semanticId;
    this.objectiveIndex = objectiveIndex;
  }

  public int getSemanticId() { return semanticId; }
  public boolean isBoundary() { return objectiveIndex >= 0; }
  public int getObjectiveIndex() { return objectiveIndex; }

  public static ZhangBoSubSwarm fromSemanticId(int semanticId) {
    for (ZhangBoSubSwarm role : values()) {
      if (role.semanticId == semanticId) return role;
    }
    throw new IllegalArgumentException("Unknown subgroup semanticId=" + semanticId);
  }
}
