package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCfvfDiagnostics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;

/**
 * V35-FC-5: a read-only CFVF gene-inheritance-rate (GIR) audit.  Aggregates,
 * per sub-swarm group, per four-vector dimension and per teacher source, how
 * many inherited modifications CFVF actually applied; keeps the most recent
 * modification fingerprint per lineage branch so Cmax-record attribution
 * (RecordContribution) can later be aligned by branch and evaluation
 * ordinal.  Observing never consumes randomness, evaluations, or influences
 * any decision: attaching it must leave the run's front byte-identical.
 *
 * <p>Known granularity limit (registered honestly): the JS dimension is
 * exchange-sequence driven and its diagnostics only expose aggregate
 * inheritance counters, so JS is reported through the per-offspring
 * pbest/gbest inheritance counts rather than a Kind cross table.</p>
 */
public final class V35CfvfGirAudit implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Kind -> affected first-stage vectors. */
  private static String[] vectorsOf(ZhangBoResourceAction.Kind kind) {
    switch (kind) {
      case FMW: return new String[] {"FA", "MA", "WA"};
      case MW: return new String[] {"MA", "WA"};
      case M: return new String[] {"MA"};
      default: return new String[] {"WA"};
    }
  }

  private final Map<String, Long> gir = new TreeMap<>();
  private final Map<String, Long> jsInherit = new TreeMap<>();
  private final Map<Long, String> lastModificationByBranch =
      new LinkedHashMap<Long, String>() {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
          return size() > 4096;
        }
      };
  private long observations;

  /** Records one CFVF offspring's diagnostics. Read-only with respect to behaviour. */
  public void observe(ZhangBoSubSwarm group, ZhangBoCfvfDiagnostics diagnostics,
      long branchId, long evaluationOrdinal, long generation) {
    if (group == null || diagnostics == null) return;
    observations++;
    String groupKey = group.name();
    StringBuilder fingerprint = new StringBuilder();
    for (Map.Entry<String, Integer> cross : diagnostics.getCrossCounts().entrySet()) {
      String key = cross.getKey();
      int separator = key.indexOf(':');
      ZhangBoResourceAction.Kind kind =
          ZhangBoResourceAction.Kind.valueOf(key.substring(0, separator));
      ZhangBoResourceAction.Source source =
          ZhangBoResourceAction.Source.valueOf(key.substring(separator + 1));
      String sourceClass = sourceClass(source);
      if (sourceClass == null) continue;
      for (String vector : vectorsOf(kind)) {
        merge(gir, groupKey + "|" + vector + "|" + sourceClass, cross.getValue());
        if (fingerprint.length() > 0) fingerprint.append(';');
        fingerprint.append(vector).append('=').append(sourceClass)
            .append('x').append(cross.getValue());
      }
    }
    if (diagnostics.getJsHamming() > 0) {
      if (diagnostics.getPbestInherited() > 0) {
        merge(jsInherit, groupKey + "|JS|PBEST", diagnostics.getJsHamming());
      }
      if (diagnostics.getGbestInherited() > 0) {
        merge(jsInherit, groupKey + "|JS|GBEST", diagnostics.getJsHamming());
      }
    }
    if (fingerprint.length() == 0 && diagnostics.getJsHamming() == 0) return;
    lastModificationByBranch.put(branchId, "fe=" + evaluationOrdinal
        + ",generation=" + generation + ",branch=" + branchId
        + (fingerprint.length() == 0 ? ",jsOnly" : ",resources=" + fingerprint));
  }

  /** RecordContribution lookup: the most recent CFVF modification of a branch. */
  public String lastModificationOf(long branchId) {
    return lastModificationByBranch.get(branchId);
  }

  public long getObservations() { return observations; }

  /** Canonical, sorted audit summary for evidence files. */
  public String summaryText() {
    StringBuilder out = new StringBuilder();
    out.append("girObservations=").append(observations).append('\n');
    for (Map.Entry<String, Long> entry : gir.entrySet()) {
      out.append("gir.").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    for (Map.Entry<String, Long> entry : jsInherit.entrySet()) {
      out.append("gir.").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    out.append("girTrackedBranches=").append(lastModificationByBranch.size()).append('\n');
    return out.toString();
  }

  private static String sourceClass(ZhangBoResourceAction.Source source) {
    switch (source) {
      case PBEST: return "PBEST";
      case GBEST: return "GBEST";
      case BOTH: return "BOTH";
      default: return null;
    }
  }

  private static void merge(Map<String, Long> target, String key, long delta) {
    Long current = target.get(key);
    target.put(key, (current == null ? 0L : current.longValue()) + delta);
  }
}
