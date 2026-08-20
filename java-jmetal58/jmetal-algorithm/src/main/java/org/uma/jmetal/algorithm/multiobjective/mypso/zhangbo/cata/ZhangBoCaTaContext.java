package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.io.Serializable;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;

/** Immutable CA-TA state bucket: role, budget phase, stagnation and bottleneck. */
public final class ZhangBoCaTaContext implements Serializable {
  private static final long serialVersionUID = 1L;
  private final ZhangBoSubSwarm subSwarm;
  private final ZhangBoCaTaPhase phase;
  private final boolean stagnated;
  private final ZhangBoBottleneck bottleneck;

  public ZhangBoCaTaContext(
      ZhangBoSubSwarm subSwarm, ZhangBoCaTaPhase phase,
      boolean stagnated, ZhangBoBottleneck bottleneck) {
    if (subSwarm == null || phase == null || bottleneck == null) {
      throw new IllegalArgumentException("CA-TA context fields cannot be null");
    }
    this.subSwarm = subSwarm;
    this.phase = phase;
    this.stagnated = stagnated;
    this.bottleneck = bottleneck;
  }

  public ZhangBoSubSwarm getSubSwarm() { return subSwarm; }
  public ZhangBoCaTaPhase getPhase() { return phase; }
  public boolean isStagnated() { return stagnated; }
  public ZhangBoBottleneck getBottleneck() { return bottleneck; }

  public String toCanonicalKey() {
    return subSwarm + "|" + phase + "|s=" + (stagnated ? 1 : 0) + "|" + bottleneck;
  }

  @Override public String toString() { return toCanonicalKey(); }
  @Override public int hashCode() { return toCanonicalKey().hashCode(); }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ZhangBoCaTaContext)) return false;
    ZhangBoCaTaContext value = (ZhangBoCaTaContext) other;
    return subSwarm == value.subSwarm && phase == value.phase
        && stagnated == value.stagnated && bottleneck == value.bottleneck;
  }
}
