package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.List;

/**
 * Deterministic lineage-archive personal-leader selector with no Qp table,
 * reward, mask, or random source.  It is intentionally separate from the
 * four-action Qp selector so causal diagnostics do not instantiate Qp merely
 * to obtain the directional control point.
 */
public final class ZhangBoDirectionalArchiveSelector {
  private ZhangBoDirectionalArchiveSelector() { }

  public static ZhangBoArchiveEntry select(
      List<ZhangBoArchiveEntry> entries, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    if (entries == null || entries.isEmpty() || group == null || bounds == null) {
      throw new IllegalArgumentException("Archive directional inputs cannot be null or empty");
    }
    ZhangBoArchiveEntry best = entries.get(0);
    for (int index = 1; index < entries.size(); index++) {
      ZhangBoArchiveEntry candidate = entries.get(index);
      int comparison = Double.compare(
          ZhangBoSubSwarmSemantics.archivePhi(candidate, group, bounds),
          ZhangBoSubSwarmSemantics.archivePhi(best, group, bounds));
      if (comparison < 0 || (comparison == 0
          && candidate.getFingerprint().compareTo(best.getFingerprint()) < 0)) {
        best = candidate;
      }
    }
    return best;
  }
}
