package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable solution attribute identifying a personal-memory lineage. */
public final class ZhangBoLineageTag implements Serializable {
  private static final long serialVersionUID = 1L;
  private final long lineageId;
  private final long parentLineageId;

  public ZhangBoLineageTag(long lineageId, long parentLineageId) {
    if (lineageId < 0L) throw new IllegalArgumentException("lineageId must be nonnegative");
    this.lineageId = lineageId;
    this.parentLineageId = parentLineageId;
  }

  public long getLineageId() { return lineageId; }
  public long getParentLineageId() { return parentLineageId; }

  public String toCanonicalText() {
    return "lineageId=" + lineageId + ",parentLineageId=" + parentLineageId;
  }
}
