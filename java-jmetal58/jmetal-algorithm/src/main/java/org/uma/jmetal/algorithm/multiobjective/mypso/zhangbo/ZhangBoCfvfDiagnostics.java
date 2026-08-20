package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable per-offspring CFVF diagnostics. */
public final class ZhangBoCfvfDiagnostics implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int jsHamming;
  private final int faHamming;
  private final int maHamming;
  private final int waHamming;
  private final int pbestInherited;
  private final int gbestInherited;
  private final int pbestConflictWins;
  private final int gbestConflictWins;
  private final int repairs;
  private final Map<ZhangBoResourceAction.Kind, Integer> kindCounts;
  private final Map<ZhangBoResourceAction.Source, Integer> sourceCounts;
  /** V35-FC-5 GIR audit: Kind x Source cross counts ("KIND:SOURCE" keys). */
  private final Map<String, Integer> crossCounts;
  private final List<String> events;

  ZhangBoCfvfDiagnostics(
      int jsHamming, int faHamming, int maHamming, int waHamming,
      int pbestInherited, int gbestInherited,
      int pbestConflictWins, int gbestConflictWins, int repairs,
      Map<ZhangBoResourceAction.Kind, Integer> kindCounts,
      Map<ZhangBoResourceAction.Source, Integer> sourceCounts,
      Map<String, Integer> crossCounts,
      List<String> events) {
    this.jsHamming = jsHamming;
    this.faHamming = faHamming;
    this.maHamming = maHamming;
    this.waHamming = waHamming;
    this.pbestInherited = pbestInherited;
    this.gbestInherited = gbestInherited;
    this.pbestConflictWins = pbestConflictWins;
    this.gbestConflictWins = gbestConflictWins;
    this.repairs = repairs;
    this.kindCounts = Collections.unmodifiableMap(new EnumMap<>(kindCounts));
    this.sourceCounts = Collections.unmodifiableMap(new EnumMap<>(sourceCounts));
    this.crossCounts = Collections.unmodifiableMap(new TreeMap<>(
        crossCounts == null ? java.util.Collections.<String, Integer>emptyMap() : crossCounts));
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
  }

  public int getJsHamming() { return jsHamming; }
  public int getFaHamming() { return faHamming; }
  public int getMaHamming() { return maHamming; }
  public int getWaHamming() { return waHamming; }
  public int getPbestInherited() { return pbestInherited; }
  public int getGbestInherited() { return gbestInherited; }
  public int getPbestConflictWins() { return pbestConflictWins; }
  public int getGbestConflictWins() { return gbestConflictWins; }
  public int getRepairs() { return repairs; }
  public Map<ZhangBoResourceAction.Kind, Integer> getKindCounts() { return kindCounts; }
  public Map<ZhangBoResourceAction.Source, Integer> getSourceCounts() { return sourceCounts; }
  public Map<String, Integer> getCrossCounts() { return crossCounts; }
  public List<String> getEvents() { return events; }

  public String toCanonicalText() {
    StringBuilder builder = new StringBuilder();
    builder.append("hamming=").append(jsHamming).append(',').append(faHamming).append(',')
        .append(maHamming).append(',').append(waHamming).append('\n');
    builder.append("inherit=").append(pbestInherited).append(',').append(gbestInherited).append('\n');
    builder.append("conflictWins=").append(pbestConflictWins).append(',').append(gbestConflictWins).append('\n');
    builder.append("repairs=").append(repairs).append('\n');
    for (ZhangBoResourceAction.Kind kind : ZhangBoResourceAction.Kind.values()) {
      builder.append("kind.").append(kind).append('=').append(value(kindCounts, kind)).append('\n');
    }
    for (ZhangBoResourceAction.Source source : ZhangBoResourceAction.Source.values()) {
      builder.append("source.").append(source).append('=').append(value(sourceCounts, source)).append('\n');
    }
    for (Map.Entry<String, Integer> cross : crossCounts.entrySet()) {
      builder.append("cross.").append(cross.getKey()).append('=').append(cross.getValue()).append('\n');
    }
    for (String event : events) builder.append("event=").append(event).append('\n');
    return builder.toString();
  }

  private static <E extends Enum<E>> int value(Map<E, Integer> values, E key) {
    Integer value = values.get(key);
    return value == null ? 0 : value;
  }
}
