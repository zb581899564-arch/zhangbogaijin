package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable per-lineage archive state. */
public final class ZhangBoLineageMemory {
  private final long lineageId;
  private final long parentLineageId;
  private final int generation;
  private final long archiveVersion;
  private final int noArchiveUpdateCount;
  private final ZhangBoSubSwarm subSwarm;
  private final List<ZhangBoArchiveEntry> entries;

  public ZhangBoLineageMemory(
      long lineageId, long parentLineageId, int generation, long archiveVersion,
      int noArchiveUpdateCount, ZhangBoSubSwarm subSwarm,
      List<ZhangBoArchiveEntry> entries) {
    if (lineageId < 0L || subSwarm == null || entries == null || entries.isEmpty()) {
      throw new IllegalArgumentException("Invalid lineage memory");
    }
    this.lineageId = lineageId;
    this.parentLineageId = parentLineageId;
    this.generation = generation;
    this.archiveVersion = archiveVersion;
    this.noArchiveUpdateCount = noArchiveUpdateCount;
    this.subSwarm = subSwarm;
    this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
  }

  public long getLineageId() { return lineageId; }
  public long getParentLineageId() { return parentLineageId; }
  public int getGeneration() { return generation; }
  public long getArchiveVersion() { return archiveVersion; }
  public int getNoArchiveUpdateCount() { return noArchiveUpdateCount; }
  public ZhangBoSubSwarm getSubSwarm() { return subSwarm; }
  public List<ZhangBoArchiveEntry> getEntries() { return new ArrayList<>(entries); }
}
