package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable per-generation branch identity for subgroup duplicates of one lineage. */
public final class ZhangBoQpBranchTag implements Serializable {
  private static final long serialVersionUID = 1L;
  private final long branchId;
  private final long lineageId;

  public ZhangBoQpBranchTag(long branchId, long lineageId) {
    if (branchId < 0L || lineageId < 0L) throw new IllegalArgumentException("Invalid Qp branch tag");
    this.branchId = branchId;
    this.lineageId = lineageId;
  }

  public long getBranchId() { return branchId; }
  public long getLineageId() { return lineageId; }
}
